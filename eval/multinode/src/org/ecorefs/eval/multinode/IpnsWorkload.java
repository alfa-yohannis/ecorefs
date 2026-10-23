package org.ecorefs.eval.multinode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Measures IPNS across nodes. Each iteration adds a small new file on the
 * publisher and publishes its CID under one key. mn-b then resolves the name
 * without its cache, and repeats the resolution until the name returns the new
 * CID. The run records three durations: the publication on mn-a, the first
 * resolution on mn-b, and the time from the start of the publication until
 * mn-b sees the new CID. The connection policy prepares mn-b before the
 * publication, so its wait stays out of all three durations.
 */
public final class IpnsWorkload {

    /** Name of the IPNS key on the publisher. */
    private static final String KEY_NAME = "ecorefs-multinode";

    /** Longest wait for mn-b to see a new publication before the iteration counts as a timeout. */
    private static final long VISIBILITY_LIMIT_NANOS = 300_000_000_000L;

    /** Shared state of the run. */
    private final BenchmarkContext context;

    /**
     * Creates the workload.
     *
     * @param context shared state of the run
     */
    public IpnsWorkload(BenchmarkContext context) {
        this.context = context;
    }

    /**
     * Runs the warm-up and measured publications.
     *
     * @throws IOException when a node or the result file fails
     */
    public void run() throws IOException {
        context.publisher().ensureKey(KEY_NAME);
        Durations publishes = new Durations("publish_a");
        Durations resolves = new Durations("resolve_b");
        Durations visibility = new Durations("visible_b");
        int warmup = context.settings().warmup();
        int total = warmup + context.settings().iterations();
        for (int iteration = 0; iteration < total; iteration++) {
            boolean measured = iteration >= warmup;
            runOnePublication(iteration, measured ? iteration - warmup + 1 : 0, publishes, resolves, visibility);
        }
        System.out.printf("ipns: %s, %s, %s%n", publishes.describe(), resolves.describe(), visibility.describe());
    }

    /**
     * Publishes one new CID and waits until mn-b resolves the name to it.
     *
     * @param iteration         the iteration, starting at 0
     * @param measuredIteration the measured iteration starting at 1, or 0 for a warm-up
     * @param publishes         durations of the publications
     * @param resolves          durations of the first resolutions
     * @param visibility        durations until mn-b saw the new CID
     * @throws IOException when a node or the result file fails
     */
    private void runOnePublication(int iteration, int measuredIteration, Durations publishes, Durations resolves,
            Durations visibility) throws IOException {
        byte[] content = ("ecorefs multinode IPNS " + context.uniqueLabel() + " " + iteration)
                .getBytes(StandardCharsets.UTF_8);
        String cid = context.publisher().add(content, "ipns-" + iteration + ".txt");
        String expected = "/ipfs/" + cid;
        boolean connected = context.policy().prepare(context.fetcherB(), context.publisherPeerId());

        long start = System.nanoTime();
        String name = context.publisher().publish(cid, KEY_NAME);
        long publish = System.nanoTime() - start;

        long resolveStart = System.nanoTime();
        String path = context.fetcherB().resolve(name);
        long resolve = System.nanoTime() - resolveStart;

        int attempts = 1;
        while (!expected.equals(path) && System.nanoTime() - start < VISIBILITY_LIMIT_NANOS) {
            path = context.fetcherB().resolve(name);
            attempts++;
        }
        long visible = System.nanoTime() - start;
        context.policy().finished(context.fetcherB());
        if (measuredIteration == 0) {
            return;
        }
        String operation = expected.equals(path) ? "visible_b" : "visible_b_timeout";
        ResultWriter writer = context.writer();
        writer.record("ipns", "attempts=" + attempts, content.length, measuredIteration, "publish_a", publish, null);
        writer.record("ipns", "attempts=" + attempts, content.length, measuredIteration, "resolve_b", resolve,
                connected);
        writer.record("ipns", "attempts=" + attempts, content.length, measuredIteration, operation, visible, null);
        publishes.add(publish);
        resolves.add(resolve);
        visibility.add(visible);
    }
}
