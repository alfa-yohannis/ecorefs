package org.ecorefs.eval.addprobe;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

/**
 * Times one add of a file to a Kubo node, with warm-up iterations as in the RQ3
 * benchmark. The probe prints the time of every measured iteration and the mean
 * and median, so implementations of {@link AddRequest} can be compared on the
 * same file and node.
 *
 * <pre>
 * java -cp &lt;class path&gt; org.ecorefs.eval.addprobe.AddLatencyProbe \
 *     library|single-write|single-write-no-pin &lt;file&gt; &lt;port&gt; [iterations]
 * </pre>
 */
public final class AddLatencyProbe {

    /** Iterations that warm up the virtual machine and the connection. */
    private static final int WARMUP_ITERATIONS = 3;

    /** Nanoseconds in one millisecond. */
    private static final double NANOS_PER_MILLISECOND = 1_000_000.0;

    /** Prevents instances, because the probe only runs through {@link #main}. */
    private AddLatencyProbe() {
    }

    /**
     * Runs the probe.
     *
     * @param arguments the implementation, the file, the API port and optionally the number of iterations
     * @throws IOException when a request fails
     */
    public static void main(String[] arguments) throws IOException {
        AddRequest request = createRequest(arguments[0], Integer.parseInt(arguments[2]));
        Path file = Paths.get(arguments[1]);
        int iterations = arguments.length > 3 ? Integer.parseInt(arguments[3]) : 30;
        double[] times = new double[iterations];
        String cid = "";
        for (int iteration = 0; iteration < WARMUP_ITERATIONS + iterations; iteration++) {
            long start = System.nanoTime();
            cid = request.add(file);
            long elapsed = System.nanoTime() - start;
            if (iteration >= WARMUP_ITERATIONS) {
                times[iteration - WARMUP_ITERATIONS] = elapsed / NANOS_PER_MILLISECOND;
            }
        }
        printSummary(request.name(), file, cid, times);
    }

    /**
     * Creates the implementation that the first argument names.
     *
     * @param name {@code library}, {@code single-write} or {@code single-write-no-pin}
     * @param port port of the HTTP API of the node
     * @return the implementation
     * @throws IOException when the URL of the node is malformed
     */
    private static AddRequest createRequest(String name, int port) throws IOException {
        if ("single-write".equals(name)) {
            return new SingleWriteAddRequest("http://127.0.0.1:" + port, true);
        }
        if ("single-write-no-pin".equals(name)) {
            return new SingleWriteAddRequest("http://127.0.0.1:" + port, false);
        }
        return new LibraryAddRequest("/ip4/127.0.0.1/tcp/" + port);
    }

    /**
     * Prints every measured time, then the mean and the median.
     *
     * @param name  name of the implementation
     * @param file  the added file
     * @param cid   the answer of the last add, which shows that the node stored the file
     * @param times the measured times in milliseconds
     */
    private static void printSummary(String name, Path file, String cid, double[] times) {
        double sum = 0.0;
        StringBuilder series = new StringBuilder();
        for (double time : times) {
            sum += time;
            series.append(String.format("%.3f ", time));
        }
        double[] sorted = times.clone();
        Arrays.sort(sorted);
        int middle = sorted.length / 2;
        double median = sorted.length % 2 == 0 ? (sorted[middle - 1] + sorted[middle]) / 2.0 : sorted[middle];
        System.out.printf("%s keepAlive=%s file=%s mean=%.3f median=%.3f ms%n", name,
                System.getProperty("http.keepAlive", "true"), file.getFileName(), sum / times.length, median);
        System.out.println("  times: " + series.toString().trim());
        System.out.println("  last answer: " + cid);
    }
}
