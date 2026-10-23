package org.ecorefs.eval.multinode;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Locale;

/**
 * Appends one row per measured operation to a CSV file. Several runs of the
 * harness, one per topology and delay profile, write into the same file, so the
 * writer adds the header only to a new file.
 */
public final class ResultWriter implements AutoCloseable {

    /** Header of the result file. */
    static final String HEADER =
            "run,topology,profile,workload,label,bytes,iteration,operation,milliseconds,connected_to_publisher";

    /** Writer of the open result file. */
    private final BufferedWriter writer;

    /** Identifier of the run, shared by every row of one start of the harness. */
    private final String run;

    /** Topology of the network during the run. */
    private final String topology;

    /** Delay profile of the network during the run. */
    private final String profile;

    /**
     * Opens the result file for appending.
     *
     * @param file     the CSV file
     * @param run      identifier of the run
     * @param topology topology of the network
     * @param profile  delay profile of the network
     * @throws IOException when the file cannot be opened
     */
    public ResultWriter(Path file, String run, String topology, String profile) throws IOException {
        Files.createDirectories(file.toAbsolutePath().getParent());
        boolean isNew = !Files.exists(file) || Files.size(file) == 0;
        this.writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        this.run = run;
        this.topology = topology;
        this.profile = profile;
        if (isNew) {
            writer.write(HEADER);
            writer.newLine();
        }
    }

    /**
     * Writes one measured operation and flushes, so an interrupted run keeps
     * every row written so far.
     *
     * @param workload              the workload, such as models
     * @param label                 the model size or the file of the row
     * @param bytes                 the size of the content in bytes
     * @param iteration             the measured iteration, starting at 1
     * @param operation             the operation, such as cat_b
     * @param nanoseconds           the duration of the operation
     * @param connectedToPublisher  whether the acting node held a connection to
     *                              the publisher when the operation started, or
     *                              null when the question does not apply
     * @throws IOException when the file cannot be written
     */
    public void record(String workload, String label, long bytes, int iteration, String operation,
            long nanoseconds, Boolean connectedToPublisher) throws IOException {
        String connected = connectedToPublisher == null ? "" : connectedToPublisher.toString();
        writer.write(String.format(Locale.ROOT, "%s,%s,%s,%s,%s,%d,%d,%s,%.3f,%s", run, topology, profile, workload,
                label.replace(',', ';'), bytes, iteration, operation, nanoseconds / 1_000_000.0, connected));
        writer.newLine();
        writer.flush();
    }

    /**
     * Closes the result file.
     *
     * @throws IOException when the file cannot be closed
     */
    @Override
    public void close() throws IOException {
        writer.close();
    }
}
