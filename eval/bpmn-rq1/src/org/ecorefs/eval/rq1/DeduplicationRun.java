package org.ecorefs.eval.rq1;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import org.ecorefs.eval.bpmn.KuboNode;

/**
 * Adds every model file of the dataset to a fresh node, file by file, as the
 * modeler publishes a file, and records the CID and the time of each add. The
 * statistics of the repository before and after show how much the node grew
 * compared with the total size of the files, since identical files receive the
 * same CID and are stored once.
 */
final class DeduplicationRun {

    private final KuboNode node;

    /**
     * Prepares the run.
     *
     * @param node a node with an empty repository
     */
    DeduplicationRun(KuboNode node) {
        this.node = node;
    }

    /**
     * Adds every file and writes the table and the repository statistics.
     *
     * @param collections the files per collection
     * @param output      the TSV file; the statistics go to the same name with {@code .repo}
     * @throws IOException when a file cannot be read or the node refuses it
     */
    void run(List<ModelList> collections, Path output) throws IOException {
        KuboNode.RepositoryStatistics before = node.repositoryStatistics();
        long files = 0;
        long totalBytes = 0;
        long totalNanos = 0;
        try (PrintWriter table = new PrintWriter(Files.newBufferedWriter(output, StandardCharsets.UTF_8))) {
            table.println("collection\tpath\tbytes\tcid\tadd_ms");
            for (ModelList models : collections) {
                for (Path file : models.files) {
                    byte[] bytes = Files.readAllBytes(file);
                    long start = System.nanoTime();
                    String cid = node.add(bytes);
                    long nanos = System.nanoTime() - start;
                    table.println(models.collection + "\t" + models.relativePath(file) + "\t" + bytes.length + "\t"
                            + cid + "\t" + PathTrial.millis(nanos));
                    files++;
                    totalBytes += bytes.length;
                    totalNanos += nanos;
                }
            }
        }
        KuboNode.RepositoryStatistics after = node.repositoryStatistics();
        writeStatistics(Path.of(output + ".repo"), before, after, files, totalBytes, totalNanos);
    }

    /**
     * Writes the repository statistics as key and value lines.
     *
     * @param statisticsFile the output file
     * @param before         the statistics before the first add
     * @param after          the statistics after the last add
     * @param files          the number of added files
     * @param totalBytes     the total size of the added files
     * @param totalNanos     the total time of the adds
     * @throws IOException when the file cannot be written
     */
    private static void writeStatistics(Path statisticsFile, KuboNode.RepositoryStatistics before,
            KuboNode.RepositoryStatistics after, long files, long totalBytes, long totalNanos) throws IOException {
        try (PrintWriter lines = new PrintWriter(Files.newBufferedWriter(statisticsFile, StandardCharsets.UTF_8))) {
            lines.println("files\t" + files);
            lines.println("file_bytes\t" + totalBytes);
            lines.println("add_seconds\t" + String.format(Locale.ROOT, "%.3f", totalNanos / 1e9));
            lines.println("repo_bytes_before\t" + before.sizeBytes);
            lines.println("repo_bytes_after\t" + after.sizeBytes);
            lines.println("repo_objects_before\t" + before.objects);
            lines.println("repo_objects_after\t" + after.objects);
        }
    }
}
