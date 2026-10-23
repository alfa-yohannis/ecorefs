package org.ecorefs.eval.rq1;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.ecorefs.eval.bpmn.Failures;
import org.ecorefs.eval.bpmn.SourceModelLoader;
import org.eclipse.emf.ecore.resource.Resource;

/**
 * Counts the properties of every model of one collection without storing
 * anything: the columns of {@link ModelStatistics} for each file. The run serves
 * to recount the statistics of an earlier round trip after a change of the
 * counting rules.
 */
final class StatisticsRun {

    private final ModelList models;
    private final SourceModelLoader loader;

    /**
     * Prepares the run.
     *
     * @param models the files of the collection
     * @param loader the loader of source files
     */
    StatisticsRun(ModelList models, SourceModelLoader loader) {
        this.models = models;
        this.loader = loader;
    }

    /**
     * Loads every file and writes one row per file.
     *
     * @param output the TSV file
     * @throws IOException when the table cannot be written
     */
    void run(Path output) throws IOException {
        try (PrintWriter table = new PrintWriter(Files.newBufferedWriter(output, StandardCharsets.UTF_8))) {
            table.println(String.join("\t", ModelRoundTrip.MODEL_COLUMNS.subList(0, 2)) + "\tsource_load\t"
                    + String.join("\t", ModelRoundTrip.MODEL_COLUMNS.subList(4, ModelRoundTrip.MODEL_COLUMNS.size())));
            for (Path file : models.files) {
                table.println(String.join("\t", rowOf(file)));
            }
        }
    }

    /**
     * Loads one file and counts its properties.
     *
     * @param file the BPMN file
     * @return the row of the file
     */
    private List<String> rowOf(Path file) {
        List<String> row = new ArrayList<>(List.of(models.collection, models.relativePath(file)));
        try {
            Resource source = loader.load(file);
            row.add(source.getErrors().isEmpty() ? "OK" : "ERRORS " + source.getErrors().size());
            row.addAll(List.of(ModelStatistics.of(source).columns()));
            source.unload();
        } catch (Exception | StackOverflowError e) {
            row.add("FAIL " + Failures.describe(e));
            row.addAll(List.of("-", "-", "-", "-", "-"));
        }
        return row;
    }
}
