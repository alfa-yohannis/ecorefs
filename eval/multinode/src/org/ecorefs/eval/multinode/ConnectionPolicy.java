package org.ecorefs.eval.multinode;

import java.io.IOException;

/**
 * Decides what happens to the connection between a fetcher and the publisher
 * before each fetch. In the peered topology every node keeps its connections,
 * so a fetcher asks the publisher directly. In the dht topology the fetcher
 * closes its connections to the publisher once the activity of its earlier
 * requests has ended, so the fetch has to find the publisher through provider
 * records first. The harness calls the policy outside the timers.
 */
public interface ConnectionPolicy {

    /**
     * Prepares a fetcher for the next fetch.
     *
     * @param fetcher         the node that fetches next
     * @param publisherPeerId the peer ID of the node that added the content
     * @return true when the fetcher still holds a direct connection to the
     *         publisher when the fetch starts
     * @throws IOException when a node rejects a request
     */
    boolean prepare(KuboClient fetcher, String publisherPeerId) throws IOException;

    /**
     * Tells the policy that a fetcher finished a request, such as a fetch or an
     * IPNS resolution, whose activity may go on for a while.
     *
     * @param fetcher the node that finished the request
     */
    void finished(KuboClient fetcher);
}
