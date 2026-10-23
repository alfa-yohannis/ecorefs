package org.ecorefs.eval.multinode;

import java.util.Random;

/**
 * Everything that the workloads of one run share: the settings, the three nodes
 * with their roles, the connection policy of the topology and the result file.
 */
public final class BenchmarkContext {

    /** Settings of the run. */
    private final BenchmarkSettings settings;

    /** Node that adds and publishes content. */
    private final KuboClient publisher;

    /** Node that fetches raw content and resolves IPNS names. */
    private final KuboClient fetcherB;

    /** Node that fetches content through the EMF resource of ecorefs. */
    private final KuboClient fetcherC;

    /** Policy that prepares a fetcher before each fetch. */
    private final ConnectionPolicy policy;

    /** File that receives the measurements. */
    private final ResultWriter writer;

    /** Peer ID of the publisher. */
    private final String publisherPeerId;

    /** First stamp of the generated models in this run, random so every run stores new content. */
    private final int baseStamp;

    /**
     * Creates the shared state of one run.
     *
     * @param settings        settings of the run
     * @param publisher       node that adds and publishes content
     * @param fetcherB        node that fetches raw content and resolves names
     * @param fetcherC        node that fetches through the EMF resource
     * @param policy          policy of the topology
     * @param writer          the result file
     * @param publisherPeerId peer ID of the publisher
     */
    public BenchmarkContext(BenchmarkSettings settings, KuboClient publisher, KuboClient fetcherB,
            KuboClient fetcherC, ConnectionPolicy policy, ResultWriter writer, String publisherPeerId) {
        this.settings = settings;
        this.publisher = publisher;
        this.fetcherB = fetcherB;
        this.fetcherC = fetcherC;
        this.policy = policy;
        this.writer = writer;
        this.publisherPeerId = publisherPeerId;
        this.baseStamp = new Random().nextInt(1 << 30);
    }

    /**
     * Returns the settings of the run.
     *
     * @return the settings
     */
    public BenchmarkSettings settings() {
        return settings;
    }

    /**
     * Returns the node that adds and publishes content.
     *
     * @return the publisher, mn-a
     */
    public KuboClient publisher() {
        return publisher;
    }

    /**
     * Returns the node that fetches raw content and resolves IPNS names.
     *
     * @return the fetcher mn-b
     */
    public KuboClient fetcherB() {
        return fetcherB;
    }

    /**
     * Returns the node that fetches content through the EMF resource.
     *
     * @return the fetcher mn-c
     */
    public KuboClient fetcherC() {
        return fetcherC;
    }

    /**
     * Returns the policy that prepares a fetcher before each fetch.
     *
     * @return the policy of the topology
     */
    public ConnectionPolicy policy() {
        return policy;
    }

    /**
     * Returns the result file.
     *
     * @return the writer
     */
    public ResultWriter writer() {
        return writer;
    }

    /**
     * Returns the peer ID of the publisher.
     *
     * @return the peer ID
     */
    public String publisherPeerId() {
        return publisherPeerId;
    }

    /**
     * Returns the stamp of one iteration of the generated models.
     *
     * @param iteration the iteration, starting at 0
     * @return a stamp that no earlier run used, with high probability
     */
    public int stamp(int iteration) {
        return baseStamp + iteration;
    }

    /**
     * Returns a label of the run that stays unique across runs, topologies and
     * profiles, for content that has to be new in every run.
     *
     * @return the label
     */
    public String uniqueLabel() {
        return settings.run() + "-" + settings.topology() + "-" + settings.profile() + "-" + baseStamp;
    }
}
