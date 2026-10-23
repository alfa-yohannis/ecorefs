package org.ecorefs.eval.multinode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * The real BPMN files of the multi-node run. select_bpmn_sample.py picks the
 * files from the cleaned MIWG and engine collections, spread over the size
 * distribution, and writes their paths into bpmn-sample.tsv. The Camunda
 * research collection stays out, because its terms keep it on an offline node.
 */
public final class BpmnSample {

    /** Paths of the sampled files, in the order of the sample list. */
    private final List<Path> files;

    /** Paths of the sampled files relative to the dataset folder, for the result file. */
    private final List<String> labels;

    /**
     * Creates the sample from a list of files.
     *
     * @param files  the sampled files
     * @param labels the paths relative to the dataset folder
     */
    private BpmnSample(List<Path> files, List<String> labels) {
        this.files = files;
        this.labels = labels;
    }

    /**
     * Reads the sample list. Each line holds a path relative to the dataset
     * folder and the size of the file, separated by a tab.
     *
     * @param sampleList  the file bpmn-sample.tsv
     * @param datasetRoot the folder eval/datasets/bpmn
     * @return the sample
     * @throws IOException when the list cannot be read
     */
    public static BpmnSample read(Path sampleList, Path datasetRoot) throws IOException {
        List<Path> files = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        for (String line : Files.readAllLines(sampleList, StandardCharsets.UTF_8)) {
            if (line.isBlank() || line.startsWith("#")) {
                continue;
            }
            String relative = line.split("\t")[0];
            files.add(datasetRoot.resolve(relative));
            labels.add(relative);
        }
        return new BpmnSample(files, labels);
    }

    /**
     * Returns the path of a sampled file relative to the dataset folder.
     *
     * @param index the position in the sample list
     * @return the relative path
     */
    public String label(int index) {
        return labels.get(index);
    }

    /**
     * Returns the number of sampled files.
     *
     * @return the number of files
     */
    public int size() {
        return files.size();
    }

    /**
     * Returns one sampled file.
     *
     * @param index the position in the sample list
     * @return the path of the file
     */
    public Path file(int index) {
        return files.get(index);
    }

    /**
     * Returns the bytes of a sampled file with a comment appended after the root
     * element. The comment names the run, so every run adds new content and no
     * node holds the content before the fetch. XML allows a comment after the
     * root element, so the file stays a valid BPMN document.
     *
     * @param index the position in the sample list
     * @param run   identifier of the run
     * @return the bytes to add to the publisher
     * @throws IOException when the file cannot be read
     */
    public byte[] uniqueContent(int index, String run) throws IOException {
        byte[] original = Files.readAllBytes(files.get(index));
        byte[] comment = ("\n<!-- ecorefs multinode run " + run + " file " + index + " -->\n")
                .getBytes(StandardCharsets.UTF_8);
        byte[] content = new byte[original.length + comment.length];
        System.arraycopy(original, 0, content, 0, original.length);
        System.arraycopy(comment, 0, content, original.length, comment.length);
        return content;
    }
}
