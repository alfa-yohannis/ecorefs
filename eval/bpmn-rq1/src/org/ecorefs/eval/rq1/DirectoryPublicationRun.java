package org.ecorefs.eval.rq1;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.ecorefs.eval.bpmn.KuboNode;

/**
 * Publishes each collection as one IPFS directory in a single add on a fresh node,
 * the alternative to one add per model, and records the time per collection.
 */
final class DirectoryPublicationRun {

    private final KuboNode node;

    /**
     * Prepares the run.
     *
     * @param node a node with an empty repository
     */
    DirectoryPublicationRun(KuboNode node) {
        this.node = node;
    }

    /**
     * Adds every collection as a directory and writes one row per collection.
     *
     * @param collections the files per collection
     * @param output      the TSV file
     * @throws IOException when a file cannot be read or the node refuses the directory
     */
    void run(List<ModelList> collections, Path output) throws IOException {
        try (PrintWriter table = new PrintWriter(Files.newBufferedWriter(output, StandardCharsets.UTF_8))) {
            table.println("collection\tfiles\tbytes\tcid\tadd_ms\trepo_bytes_after\trepo_objects_after");
            for (ModelList models : collections) {
                long bytes = totalSize(models);
                long start = System.nanoTime();
                String cid = node.addDirectory(DirectoryTree.of(models));
                long nanos = System.nanoTime() - start;
                KuboNode.RepositoryStatistics after = node.repositoryStatistics();
                table.println(models.collection + "\t" + models.files.size() + "\t" + bytes + "\t" + cid + "\t"
                        + PathTrial.millis(nanos) + "\t" + after.sizeBytes + "\t" + after.objects);
                table.flush();
            }
        }
    }

    /**
     * Sums the sizes of the files of a collection.
     *
     * @param models the files of the collection
     * @return the total size in bytes
     * @throws IOException when a size cannot be read
     */
    private static long totalSize(ModelList models) throws IOException {
        long bytes = 0;
        for (Path file : models.files) {
            bytes += Files.size(file);
        }
        return bytes;
    }
}
