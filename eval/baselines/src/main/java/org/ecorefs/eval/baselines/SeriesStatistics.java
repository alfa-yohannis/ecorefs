package org.ecorefs.eval.baselines;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Mean, standard deviation and median of one series of durations in
 * milliseconds.
 */
public final class SeriesStatistics {

    /** The measured durations in milliseconds. */
    private final List<Double> values = new ArrayList<>();

    /**
     * Adds one measured duration.
     *
     * @param milliseconds the duration
     */
    public void add(double milliseconds) {
        values.add(milliseconds);
    }

    /**
     * Returns the number of values.
     *
     * @return the count
     */
    public int count() {
        return values.size();
    }

    /**
     * Returns the arithmetic mean.
     *
     * @return the mean, or zero for an empty series
     */
    public double mean() {
        if (values.isEmpty()) {
            return 0.0;
        }
        double sum = 0.0;
        for (double value : values) {
            sum += value;
        }
        return sum / values.size();
    }

    /**
     * Returns the sample standard deviation.
     *
     * @return the standard deviation, or zero for fewer than two values
     */
    public double standardDeviation() {
        if (values.size() < 2) {
            return 0.0;
        }
        double average = mean();
        double sumOfSquares = 0.0;
        for (double value : values) {
            sumOfSquares += (value - average) * (value - average);
        }
        return Math.sqrt(sumOfSquares / (values.size() - 1));
    }

    /**
     * Returns the median.
     *
     * @return the median, or zero for an empty series
     */
    public double median() {
        if (values.isEmpty()) {
            return 0.0;
        }
        List<Double> sorted = new ArrayList<>(values);
        Collections.sort(sorted);
        int middle = sorted.size() / 2;
        if (sorted.size() % 2 == 0) {
            return (sorted.get(middle - 1) + sorted.get(middle)) / 2.0;
        }
        return sorted.get(middle);
    }
}
