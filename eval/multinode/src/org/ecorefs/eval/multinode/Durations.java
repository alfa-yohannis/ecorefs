package org.ecorefs.eval.multinode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Collects the durations of one operation, so the harness can print a median
 * per model size or per workload while it runs. The statistics of the paper
 * come from summarize.py, which reads the result file.
 */
public final class Durations {

    /** Name of the operation, such as cat_b. */
    private final String operation;

    /** Collected durations in nanoseconds. */
    private final List<Long> nanoseconds = new ArrayList<>();

    /**
     * Creates an empty collection for one operation.
     *
     * @param operation the name of the operation
     */
    public Durations(String operation) {
        this.operation = operation;
    }

    /**
     * Adds one duration.
     *
     * @param duration the duration in nanoseconds
     */
    public void add(long duration) {
        nanoseconds.add(duration);
    }

    /**
     * Describes the median of the collected durations.
     *
     * @return text such as "cat_b median 12.345 ms (n=30)"
     */
    public String describe() {
        if (nanoseconds.isEmpty()) {
            return operation + " no data";
        }
        List<Long> sorted = new ArrayList<>(nanoseconds);
        Collections.sort(sorted);
        int middle = sorted.size() / 2;
        double median = sorted.size() % 2 == 0
                ? (sorted.get(middle - 1) + sorted.get(middle)) / 2.0
                : sorted.get(middle);
        return String.format(Locale.ROOT, "%s median %.3f ms (n=%d)", operation, median / 1_000_000.0,
                sorted.size());
    }
}
