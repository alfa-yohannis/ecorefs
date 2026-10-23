package org.ecorefs.eval.baselines;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Collects the measurements of a run. Every measured phase becomes one row of a
 * CSV file at once, so a crash keeps the rows written so far. The end of the
 * run writes a Markdown summary: tables that compare the write and read times
 * of the stores, then the mean, standard deviation and median of every phase
 * per store and model size.
 */
public final class ResultLog implements AutoCloseable {

    /** Nanoseconds in one millisecond. */
    private static final double NANOS_PER_MILLISECOND = 1_000_000.0;

    /** Phase from the start of the save until the model is stored and shared. */
    private static final String WRITE = "write";

    /** Phase from the start of the transfer until the loaded model is in memory. */
    private static final String READ = "read";

    /** Phase that reads every element of the loaded model. */
    private static final String VISIT = "visit";

    /** Derived phase: read and visit together, a full read for lazy stores too. */
    private static final String READ_AND_VISIT = "read+visit";

    /** Writer of the CSV file. */
    private final PrintWriter rows;

    /** File that receives the summary. */
    private final Path summaryFile;

    /** Statistics per store, size and phase, in the order of first appearance. */
    private final Map<String, SeriesStatistics> series = new LinkedHashMap<>();

    /** Phases per store, in the order of first appearance. */
    private final Map<String, List<String>> phasesOfStore = new LinkedHashMap<>();

    /** Description of the run for the summary header. */
    private final String description;

    /**
     * Opens the result files of a run.
     *
     * @param directory   the result folder
     * @param label       the label of the run
     * @param description the description for the summary header
     * @throws IOException when the files cannot be created
     */
    public ResultLog(Path directory, String label, String description) throws IOException {
        Files.createDirectories(directory);
        this.rows = new PrintWriter(Files.newBufferedWriter(directory.resolve(label + ".csv"), StandardCharsets.UTF_8));
        this.summaryFile = directory.resolve(label + "-summary.md");
        this.description = description;
        rows.println("store,elements,iteration,phase,milliseconds");
        rows.flush();
    }

    /**
     * Records the phases of one measured iteration.
     *
     * @param store     the store name
     * @param elements  the number of elements of the model
     * @param iteration the number of the measured iteration, starting at one
     * @param times     the durations of the phases
     */
    public void record(String store, int elements, int iteration, PhaseTimes times) {
        Map<String, Long> durations = times.durations();
        for (Map.Entry<String, Long> phase : durations.entrySet()) {
            recordPhase(store, elements, iteration, phase.getKey(), phase.getValue());
        }
        if (durations.containsKey(READ) && durations.containsKey(VISIT)) {
            recordPhase(store, elements, iteration, READ_AND_VISIT, durations.get(READ) + durations.get(VISIT));
        }
        rows.flush();
    }

    /**
     * Writes one CSV row and adds the duration to its series.
     *
     * @param store       the store name
     * @param elements    the number of elements of the model
     * @param iteration   the number of the measured iteration
     * @param phase       the phase name
     * @param nanoseconds the duration of the phase
     */
    private void recordPhase(String store, int elements, int iteration, String phase, long nanoseconds) {
        double milliseconds = nanoseconds / NANOS_PER_MILLISECOND;
        rows.printf(Locale.ROOT, "%s,%d,%d,%s,%.3f%n", store, elements, iteration, phase, milliseconds);
        seriesOf(store, elements, phase).add(milliseconds);
    }

    /**
     * Returns the statistics of one series, creating the series when needed.
     *
     * @param store    the store name
     * @param elements the number of elements
     * @param phase    the phase name
     * @return the statistics of the series
     */
    private SeriesStatistics seriesOf(String store, int elements, String phase) {
        List<String> phases = phasesOfStore.get(store);
        if (phases == null) {
            phases = new ArrayList<>();
            phasesOfStore.put(store, phases);
        }
        if (!phases.contains(phase)) {
            phases.add(phase);
        }
        String key = store + "|" + elements + "|" + phase;
        SeriesStatistics statistics = series.get(key);
        if (statistics == null) {
            statistics = new SeriesStatistics();
            series.put(key, statistics);
        }
        return statistics;
    }

    /**
     * Writes the summary: one table per store, with one row per model size and
     * one column per phase, each cell holding mean and standard deviation.
     *
     * @param sizes the model sizes of the run
     * @throws IOException when the summary cannot be written
     */
    public void writeSummary(List<Integer> sizes) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add("# Baseline measurements");
        lines.add("");
        lines.add(description);
        lines.add("");
        lines.add("Each cell gives the mean and the standard deviation in milliseconds; the median is in brackets.");
        addComparisonTable(lines, WRITE, "Write: from the start of the save until the model is stored and shared", sizes);
        addComparisonTable(lines, READ, "Read: from the start of the transfer until the model is loaded", sizes);
        addComparisonTable(lines, READ_AND_VISIT, "Read and visit: read plus a visit of every element", sizes);
        for (Map.Entry<String, List<String>> store : phasesOfStore.entrySet()) {
            addStoreTable(lines, store.getKey(), store.getValue(), sizes);
        }
        Files.write(summaryFile, lines, StandardCharsets.UTF_8);
    }

    /**
     * Adds a table that compares one phase across the stores, with one row per
     * model size and one column per store.
     *
     * @param lines   the summary lines under construction
     * @param phase   the compared phase
     * @param heading the heading of the table
     * @param sizes   the model sizes of the run
     */
    private void addComparisonTable(List<String> lines, String phase, String heading, List<Integer> sizes) {
        List<String> stores = new ArrayList<>(phasesOfStore.keySet());
        lines.add("");
        lines.add("## " + heading);
        lines.add("");
        lines.add("| Elements | " + String.join(" | ", stores) + " |");
        lines.add("|---:|" + "---:|".repeat(stores.size()));
        for (int size : sizes) {
            StringBuilder row = new StringBuilder("| " + String.format(Locale.ROOT, "%,d", size) + " |");
            for (String store : stores) {
                row.append(' ').append(cellOf(store, size, phase)).append(" |");
            }
            lines.add(row.toString());
        }
    }

    /**
     * Adds the table of one store to the summary.
     *
     * @param lines  the summary lines under construction
     * @param store  the store name
     * @param phases the phases of the store
     * @param sizes  the model sizes of the run
     */
    private void addStoreTable(List<String> lines, String store, List<String> phases, List<Integer> sizes) {
        lines.add("");
        lines.add("## Phases of " + store);
        lines.add("");
        lines.add("| Elements | " + String.join(" | ", phases) + " |");
        lines.add("|---:|" + "---:|".repeat(phases.size()));
        for (int size : sizes) {
            lines.add(rowOf(store, size, phases));
        }
    }

    /**
     * Formats one row of a store table.
     *
     * @param store  the store name
     * @param size   the model size
     * @param phases the phases of the store
     * @return the Markdown row
     */
    private String rowOf(String store, int size, List<String> phases) {
        StringBuilder row = new StringBuilder("| " + String.format(Locale.ROOT, "%,d", size) + " |");
        for (String phase : phases) {
            row.append(' ').append(cellOf(store, size, phase)).append(" |");
        }
        return row.toString();
    }

    /**
     * Formats the statistics of one store, size and phase.
     *
     * @param store the store name
     * @param size  the model size
     * @param phase the phase name
     * @return mean, standard deviation and median, or {@code n/a} without data
     */
    private String cellOf(String store, int size, String phase) {
        SeriesStatistics statistics = series.get(store + "|" + size + "|" + phase);
        return statistics == null ? "n/a" : format(statistics);
    }

    /**
     * Formats the statistics of one cell.
     *
     * @param statistics the statistics of a series
     * @return mean, standard deviation and median as text
     */
    private static String format(SeriesStatistics statistics) {
        return String.format(Locale.ROOT, "%.2f ± %.2f (%.2f)", statistics.mean(),
                statistics.standardDeviation(), statistics.median());
    }

    /**
     * Closes the CSV file.
     */
    @Override
    public void close() {
        rows.close();
    }
}
