package org.ecorefs.eval.multinode;

import java.io.IOException;

/**
 * Fetches real BPMN files across nodes. Each file of the sample is added once to
 * the publisher, with a comment that names the run, and read once on mn-b, which
 * then removes the blocks with a garbage collection. The first files of the
 * sample serve as warm-up, so the measured files are the following ones; every
 * file has a different size.
 */
public final class BpmnFileWorkload {

    /** Shared state of the run. */
    private final BenchmarkContext context;

    /** The sampled BPMN files. */
    private final BpmnSample sample;

    /**
     * Creates the workload.
     *
     * @param context shared state of the run
     * @param sample  the sampled BPMN files
     */
    public BpmnFileWorkload(BenchmarkContext context, BpmnSample sample) {
        this.context = context;
        this.sample = sample;
    }

    /**
     * Adds and fetches the warm-up files and the measured files.
     *
     * @throws IOException when a node or the result file fails, or when the
     *                     sample holds too few files
     */
    public void run() throws IOException {
        int warmup = context.settings().warmup();
        int total = warmup + context.settings().bpmnFiles();
        if (sample.size() < total) {
            throw new IOException("The BPMN sample holds " + sample.size() + " files, the run needs " + total);
        }
        Durations adds = new Durations("add_a");
        Durations cats = new Durations("cat_b");
        for (int index = 0; index < total; index++) {
            boolean measured = index >= warmup;
            runOneFile(index, measured ? index - warmup + 1 : 0, adds, cats);
        }
        System.out.printf("bpmn: %s, %s%n", adds.describe(), cats.describe());
    }

    /**
     * Adds one file on the publisher and reads it on mn-b.
     *
     * @param index             the position in the sample
     * @param measuredIteration the measured iteration starting at 1, or 0 for a warm-up
     * @param adds              durations of the adds
     * @param cats              durations of the fetches
     * @throws IOException when a node or the result file fails
     */
    private void runOneFile(int index, int measuredIteration, Durations adds, Durations cats) throws IOException {
        byte[] content = sample.uniqueContent(index, context.uniqueLabel());

        long start = System.nanoTime();
        String cid = context.publisher().add(content, "model-" + index + ".bpmn");
        long add = System.nanoTime() - start;

        boolean connected = context.policy().prepare(context.fetcherB(), context.publisherPeerId());
        start = System.nanoTime();
        byte[] fetched = context.fetcherB().cat(cid);
        long cat = System.nanoTime() - start;
        context.policy().finished(context.fetcherB());
        context.fetcherB().collectGarbage();
        if (fetched.length != content.length) {
            throw new IOException("mn-b returned " + fetched.length + " bytes for " + cid + ", expected "
                    + content.length);
        }
        if (measuredIteration == 0) {
            return;
        }
        ResultWriter writer = context.writer();
        writer.record("bpmn", sample.label(index), content.length, measuredIteration, "add_a", add, null);
        writer.record("bpmn", sample.label(index), content.length, measuredIteration, "cat_b", cat, connected);
        adds.add(add);
        cats.add(cat);
    }
}
