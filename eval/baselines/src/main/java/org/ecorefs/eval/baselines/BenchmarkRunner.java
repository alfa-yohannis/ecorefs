package org.ecorefs.eval.baselines;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Runs the benchmark. For every model size, every iteration measures every store
 * one after the other, so all stores see the same state of the machine in the
 * same iteration and the series stay paired, as the XMI and IPFS series of the
 * paper are. Every store receives its own fresh copy of the model, made outside
 * the measured region, and a garbage collection runs after every measurement.
 */
public final class BenchmarkRunner {

    /** The settings of the run. */
    private final BenchmarkSettings settings;

    /** The generated models and the copies per iteration. */
    private final BenchmarkModels models = new BenchmarkModels();

    /** The measured stores, in the order of the settings. */
    private final List<ModelStore> stores = new ArrayList<>();

    /**
     * Creates the runner and the stores.
     *
     * @param settings the settings of the run
     */
    public BenchmarkRunner(BenchmarkSettings settings) {
        this.settings = settings;
        Path work = settings.workDirectory();
        for (String name : settings.stores()) {
            stores.add(StoreCatalog.create(name, settings, work));
        }
    }

    /**
     * Runs every model size and writes the result files.
     *
     * @throws Exception when a store fails
     */
    public void run() throws Exception {
        Files.createDirectories(settings.workDirectory());
        String description = String.format("Run %s: sizes %s, %d warm-up and %d measured iterations, stores %s.",
                settings.label(), settings.sizes(), settings.warmupIterations(), settings.measuredIterations(),
                settings.stores());
        try (ResultLog log = new ResultLog(settings.resultDirectory(), settings.label(), description)) {
            startStores();
            try {
                for (int size : settings.sizes()) {
                    runSize(size, log);
                }
            } finally {
                stopStores();
            }
            log.writeSummary(settings.sizes());
        }
    }

    /**
     * Starts the services of every store.
     *
     * @throws Exception when a service does not start
     */
    private void startStores() throws Exception {
        for (ModelStore store : stores) {
            store.start();
        }
    }

    /**
     * Stops the services of every store, also after a failure.
     */
    private void stopStores() {
        for (ModelStore store : stores) {
            store.stop();
        }
    }

    /**
     * Runs the warm-up and measured iterations of one model size.
     *
     * @param size the number of elements
     * @param log  the result log
     * @throws Exception when a store fails
     */
    private void runSize(int size, ResultLog log) throws Exception {
        long start = System.nanoTime();
        Resource source = models.loadSource(settings.modelDirectory(), size);
        for (ModelStore store : stores) {
            store.prepareSize(size);
        }
        int total = settings.warmupIterations() + settings.measuredIterations();
        for (int iteration = 0; iteration < total; iteration++) {
            runIteration(source, size, iteration, log);
        }
        for (ModelStore store : stores) {
            store.finishSize();
        }
        System.out.printf("size %,d done in %.1f s%n", size, (System.nanoTime() - start) / 1e9);
    }

    /**
     * Measures every store once, checks that the loaded model equals the saved
     * copy and records the result unless the iteration warms up.
     *
     * @param source    the loaded source model
     * @param size      the model size as named in the settings
     * @param iteration the number of the iteration, starting at zero
     * @param log       the result log
     * @throws Exception when a store fails or returns another model
     */
    private void runIteration(Resource source, int size, int iteration, ResultLog log) throws Exception {
        boolean warmup = iteration < settings.warmupIterations();
        for (ModelStore store : stores) {
            Collection<EObject> content = models.copyForIteration(source, iteration);
            VisitSummary saved = ModelTraversal.visitAll(content);
            PhaseTimes times = new PhaseTimes();
            store.measure(content, iteration, times);
            System.gc();
            checkLoadedModel(store, saved, times.visit());
            if (!warmup) {
                log.record(store.name(), size, iteration - settings.warmupIterations() + 1, times);
            }
        }
    }

    /**
     * Stops the run when the loaded model differs from the saved copy in its
     * element count or in its identifiers.
     *
     * @param store  the measured store
     * @param saved  the visit of the copy before the save
     * @param loaded the visit of the loaded model
     * @throws IllegalStateException when the visits differ
     */
    private static void checkLoadedModel(ModelStore store, VisitSummary saved, VisitSummary loaded) {
        if (!saved.equals(loaded)) {
            throw new IllegalStateException(String.format("%s saved %s but loaded %s", store.name(), saved, loaded));
        }
    }
}
