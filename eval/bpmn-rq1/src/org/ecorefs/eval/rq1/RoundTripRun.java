package org.ecorefs.eval.rq1;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.ecorefs.eval.bpmn.Failures;
import org.ecorefs.eval.bpmn.SourceModelLoader;

/**
 * Runs the round trip over every file of one collection and writes one row per
 * file. Each file runs on a worker thread with a time limit. A file that runs past
 * the limit is recorded as TIMEOUT, and the run continues on a new worker.
 */
final class RoundTripRun {

    private final ModelList models;
    private final SourceModelLoader loader;
    private final List<PersistencePath> paths;
    private final long timeoutSeconds;
    private ExecutorService worker = newWorker();

    /**
     * Prepares the run.
     *
     * @param models         the files of the collection
     * @param loader         the loader of source files
     * @param paths          the persistence paths
     * @param timeoutSeconds the time limit per file
     */
    RoundTripRun(ModelList models, SourceModelLoader loader, List<PersistencePath> paths, long timeoutSeconds) {
        this.models = models;
        this.loader = loader;
        this.paths = paths;
        this.timeoutSeconds = timeoutSeconds;
    }

    /**
     * Runs every file and writes the table.
     *
     * @param output the TSV file
     * @throws IOException when the table cannot be written
     */
    void run(Path output) throws IOException {
        try (PrintWriter table = new PrintWriter(Files.newBufferedWriter(output, StandardCharsets.UTF_8))) {
            table.println(String.join("\t", header()));
            int done = 0;
            for (Path file : models.files) {
                table.println(String.join("\t", runOne(file)));
                done++;
                if (done % 100 == 0) {
                    table.flush();
                    System.err.println(models.collection + ": " + done + "/" + models.files.size());
                }
            }
        } finally {
            worker.shutdownNow();
        }
    }

    /**
     * Runs the round trip of one file within the time limit.
     *
     * @param file the BPMN file
     * @return the row of the file
     */
    private List<String> runOne(Path file) {
        String relativePath = models.relativePath(file);
        ModelRoundTrip roundTrip = new ModelRoundTrip(models.collection, relativePath, file, loader, paths);
        Future<List<String>> result = worker.submit(roundTrip);
        try {
            return result.get(timeoutSeconds, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            result.cancel(true);
            worker.shutdownNow();
            worker = newWorker();
            return ModelRoundTrip.unfinishedRow(models.collection, relativePath,
                    "TIMEOUT after " + timeoutSeconds + " s", paths.size());
        } catch (Exception e) {
            return ModelRoundTrip.unfinishedRow(models.collection, relativePath, "CRASH " + Failures.describe(e),
                    paths.size());
        }
    }

    /**
     * Returns the column names of the table.
     *
     * @return the model columns followed by the columns of each path
     */
    private List<String> header() {
        List<String> columns = new ArrayList<>(ModelRoundTrip.MODEL_COLUMNS);
        for (PersistencePath path : paths) {
            for (String suffix : PathTrial.COLUMNS) {
                columns.add(path.name() + "_" + suffix);
            }
        }
        return columns;
    }

    /**
     * Creates a worker with one daemon thread.
     *
     * @return the worker
     */
    private static ExecutorService newWorker() {
        return Executors.newSingleThreadExecutor(new DaemonThreadFactory());
    }
}
