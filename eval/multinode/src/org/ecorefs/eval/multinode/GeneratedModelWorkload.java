package org.ecorefs.eval.multinode;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;

import java.io.IOException;
import java.util.Collection;

/**
 * Fetches the generated models of the RQ3 benchmark across nodes. Each iteration
 * saves a stamped copy through IPFSResourceImpl on the publisher, then reads the
 * new CID on mn-b with cat and loads it on mn-c through the EMF resource of
 * ecorefs in a fresh resource set. The stamp gives every copy a new root CID,
 * but it changes only the first block, so each fetcher removes its blocks with
 * a garbage collection right after its fetch. A fetch therefore starts with no
 * block of the model on the fetcher, and with the publisher as the only node
 * that holds the model.
 */
public final class GeneratedModelWorkload {

    /** Shared state of the run. */
    private final BenchmarkContext context;

    /** The generated models. */
    private final GeneratedModels models;

    /** EMF access to the publisher, for the saves. */
    private final EmfNodeAccess publisherEmf;

    /** EMF access to mn-c, for the loads. */
    private final EmfNodeAccess loaderEmf;

    /**
     * Creates the workload.
     *
     * @param context shared state of the run
     * @param models  the generated models
     */
    public GeneratedModelWorkload(BenchmarkContext context, GeneratedModels models) {
        this.context = context;
        this.models = models;
        this.publisherEmf = new EmfNodeAccess(context.publisher());
        this.loaderEmf = new EmfNodeAccess(context.fetcherC());
    }

    /**
     * Runs every model size of the settings.
     *
     * @throws IOException when a node or the result file fails
     */
    public void run() throws IOException {
        for (int elements : context.settings().sizes()) {
            runOneSize(elements);
        }
    }

    /**
     * Runs the warm-up and measured iterations of one model size.
     *
     * @param elements the number of elements of the model
     * @throws IOException when a node or the result file fails
     */
    private void runOneSize(int elements) throws IOException {
        Resource source = models.load(elements);
        Durations saves = new Durations("save_a");
        Durations cats = new Durations("cat_b");
        Durations loads = new Durations("load_c");
        int warmup = context.settings().warmup();
        int total = warmup + context.settings().iterations();
        for (int iteration = 0; iteration < total; iteration++) {
            boolean measured = iteration >= warmup;
            runOneIteration(source, elements, iteration, measured ? iteration - warmup + 1 : 0, saves, cats, loads);
        }
        System.out.printf("models %d: %s, %s, %s%n", elements, saves.describe(), cats.describe(), loads.describe());
    }

    /**
     * Saves one stamped copy on the publisher and fetches it on both fetchers.
     *
     * @param source            the loaded model
     * @param elements          the number of elements of the model
     * @param iteration         the iteration, starting at 0, which selects the stamp
     * @param measuredIteration the measured iteration starting at 1, or 0 for a warm-up
     * @param saves             durations of the saves
     * @param cats              durations of the raw fetches
     * @param loads             durations of the EMF loads
     * @throws IOException when a node or the result file fails
     */
    private void runOneIteration(Resource source, int elements, int iteration, int measuredIteration,
            Durations saves, Durations cats, Durations loads) throws IOException {
        Collection<EObject> copy = models.stampedCopy(source, context.stamp(iteration));

        long start = System.nanoTime();
        URI uri = publisherEmf.save(copy);
        long save = System.nanoTime() - start;
        String cid = uri.authority();

        boolean bConnected = context.policy().prepare(context.fetcherB(), context.publisherPeerId());
        start = System.nanoTime();
        byte[] fetched = context.fetcherB().cat(cid);
        long cat = System.nanoTime() - start;
        context.policy().finished(context.fetcherB());
        context.fetcherB().collectGarbage();

        boolean cConnected = context.policy().prepare(context.fetcherC(), context.publisherPeerId());
        start = System.nanoTime();
        loaderEmf.load(uri);
        long load = System.nanoTime() - start;
        context.policy().finished(context.fetcherC());
        context.fetcherC().collectGarbage();

        // A collection between the iterations keeps the copies of this iteration
        // out of the next measurement, as in the benchmark of the paper.
        System.gc();
        if (measuredIteration == 0) {
            return;
        }
        String label = String.valueOf(elements);
        ResultWriter writer = context.writer();
        writer.record("models", label, fetched.length, measuredIteration, "save_a", save, null);
        writer.record("models", label, fetched.length, measuredIteration, "cat_b", cat, bConnected);
        writer.record("models", label, fetched.length, measuredIteration, "load_c", load, cConnected);
        saves.add(save);
        cats.add(cat);
        loads.add(load);
    }
}
