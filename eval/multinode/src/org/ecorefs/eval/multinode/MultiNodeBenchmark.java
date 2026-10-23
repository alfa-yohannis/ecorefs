package org.ecorefs.eval.multinode;

import java.io.IOException;

/**
 * Entry point of the multi-node experiment. One start measures one topology
 * under one delay profile; run.sh sets the topology and the profile of the
 * network before each start and passes them as options, so the result rows
 * carry them.
 *
 * <pre>
 * java -cp ... org.ecorefs.eval.multinode.MultiNodeBenchmark --run 20261023 \
 *     --topology peered --profile wan50 --workloads models,bpmn,ipns
 * </pre>
 */
public final class MultiNodeBenchmark {

    /**
     * Hides the constructor of the entry point.
     */
    private MultiNodeBenchmark() {
    }

    /**
     * Runs the selected workloads against the experiment network.
     *
     * @param args options of the form --name value, see {@link BenchmarkSettings}
     * @throws IOException when a node or the result file fails
     */
    public static void main(String[] args) throws IOException {
        BenchmarkSettings settings = BenchmarkSettings.parse(args);
        KuboClient publisher = new KuboClient("mn-a", settings.host(), settings.port("a"));
        KuboClient fetcherB = new KuboClient("mn-b", settings.host(), settings.port("b"));
        KuboClient fetcherC = new KuboClient("mn-c", settings.host(), settings.port("c"));
        ConnectionPolicy policy = createPolicy(settings.topology());

        System.out.printf("run %s, topology %s, profile %s%n", settings.run(), settings.topology(),
                settings.profile());
        try (ResultWriter writer = new ResultWriter(settings.output(), settings.run(), settings.topology(),
                settings.profile())) {
            BenchmarkContext context = new BenchmarkContext(settings, publisher, fetcherB, fetcherC, policy, writer,
                    publisher.peerId());
            runWorkloads(context);
        }
    }

    /**
     * Runs the workloads that the settings include, in a fixed order.
     *
     * @param context shared state of the run
     * @throws IOException when a node or the result file fails
     */
    private static void runWorkloads(BenchmarkContext context) throws IOException {
        BenchmarkSettings settings = context.settings();
        if (settings.includes("models")) {
            new GeneratedModelWorkload(context, new GeneratedModels(settings.modelsDirectory())).run();
        }
        if (settings.includes("bpmn")) {
            new BpmnFileWorkload(context, BpmnSample.read(settings.bpmnSample(), settings.bpmnRoot())).run();
        }
        if (settings.includes("ipns")) {
            new IpnsWorkload(context).run();
        }
    }

    /**
     * Chooses the connection policy of a topology.
     *
     * @param topology peered or dht
     * @return the policy
     * @throws IllegalArgumentException when the topology is unknown
     */
    private static ConnectionPolicy createPolicy(String topology) {
        if ("peered".equals(topology)) {
            return new KeepConnections();
        }
        if ("dht".equals(topology)) {
            return new DisconnectFromPublisher();
        }
        throw new IllegalArgumentException("Unknown topology: " + topology);
    }
}
