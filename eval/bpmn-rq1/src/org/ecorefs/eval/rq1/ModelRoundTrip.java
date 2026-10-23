package org.ecorefs.eval.rq1;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

import org.ecorefs.eval.bpmn.Failures;
import org.ecorefs.eval.bpmn.SourceModelLoader;
import org.eclipse.emf.ecore.resource.Resource;

/**
 * Runs every persistence path on one model file and returns one row of the result
 * table. The row starts with the file, the outcome of the source load and the
 * counts of {@link ModelStatistics}, followed by the columns of each path.
 */
final class ModelRoundTrip implements Callable<List<String>> {

    /** Columns of the row before the columns of the paths. */
    static final List<String> MODEL_COLUMNS = List.of("collection", "path", "bytes", "source_load", "objects",
            "flow_elements", "missing_ids", "extension_entries", "external_proxies");

    private final String collection;
    private final String relativePath;
    private final Path modelFile;
    private final SourceModelLoader loader;
    private final List<PersistencePath> paths;

    /**
     * Prepares the round trip of one file.
     *
     * @param collection   the name of the collection
     * @param relativePath the path of the file inside the collection
     * @param modelFile    the BPMN file
     * @param loader       the loader of source files
     * @param paths        the persistence paths to run
     */
    ModelRoundTrip(String collection, String relativePath, Path modelFile, SourceModelLoader loader,
            List<PersistencePath> paths) {
        this.collection = collection;
        this.relativePath = relativePath;
        this.modelFile = modelFile;
        this.loader = loader;
        this.paths = paths;
    }

    /**
     * Loads the file once for the counts, then runs each path.
     *
     * @return the row of the result table
     * @throws Exception when the size of the file cannot be read
     */
    @Override
    public List<String> call() throws Exception {
        List<String> row = new ArrayList<>(List.of(collection, relativePath, Long.toString(Files.size(modelFile))));
        Resource source;
        try {
            source = loader.load(modelFile);
        } catch (Exception | StackOverflowError e) {
            row.add("FAIL " + Failures.describe(e));
            return padded(row);
        }
        row.add(source.getErrors().isEmpty() ? "OK" : "ERRORS " + source.getErrors().size());
        row.addAll(List.of(ModelStatistics.of(source).columns()));
        source.unload();
        for (PersistencePath path : paths) {
            row.addAll(new PathTrial(path, loader, modelFile).run());
        }
        return row;
    }

    /**
     * Fills the columns of a row whose source did not load.
     *
     * @param row the columns so far
     * @return the row with a dash in every remaining column
     */
    private List<String> padded(List<String> row) {
        int width = MODEL_COLUMNS.size() + paths.size() * PathTrial.COLUMNS.size();
        row.addAll(Collections.nCopies(width - row.size(), "-"));
        return row;
    }

    /**
     * Returns a row for a file whose round trip did not finish in time or crashed.
     *
     * @param collection   the name of the collection
     * @param relativePath the path of the file inside the collection
     * @param status       the text for the source load column
     * @param pathCount    the number of persistence paths
     * @return the row
     */
    static List<String> unfinishedRow(String collection, String relativePath, String status, int pathCount) {
        List<String> row = new ArrayList<>(List.of(collection, relativePath, "-", status));
        int width = MODEL_COLUMNS.size() + pathCount * PathTrial.COLUMNS.size();
        row.addAll(Collections.nCopies(width - row.size(), "-"));
        return row;
    }
}
