package org.ecorefs.eval.baselines;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Durations of the phases of one measured iteration, in nanoseconds and in the
 * order in which the phases ran, and what the visit of the loaded model saw,
 * which the runner checks against the saved copy.
 */
public final class PhaseTimes {

    /** Duration of every phase, keyed by the phase name. */
    private final Map<String, Long> durations = new LinkedHashMap<>();

    /** What the visit of the loaded model saw, or null before the visit. */
    private VisitSummary visit;

    /**
     * Records the duration of one phase.
     *
     * @param phase       the name of the phase, for example {@code push}
     * @param nanoseconds the measured duration
     */
    public void record(String phase, long nanoseconds) {
        durations.put(phase, nanoseconds);
    }

    /**
     * Records the time elapsed since a start time taken with
     * {@link System#nanoTime()}.
     *
     * @param phase the name of the phase
     * @param start the value of {@link System#nanoTime()} at the start of the phase
     */
    public void recordSince(String phase, long start) {
        record(phase, System.nanoTime() - start);
    }

    /**
     * Returns the recorded durations.
     *
     * @return an unmodifiable view, in the order of recording
     */
    public Map<String, Long> durations() {
        return Collections.unmodifiableMap(durations);
    }

    /**
     * Records what the visit of the loaded model saw.
     *
     * @param summary the element count and the identifier sum of the loaded model
     */
    public void recordVisit(VisitSummary summary) {
        visit = summary;
    }

    /**
     * Returns what the visit of the loaded model saw.
     *
     * @return the summary of the visit, or null when no visit ran
     */
    public VisitSummary visit() {
        return visit;
    }
}
