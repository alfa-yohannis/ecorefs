package org.ecorefs.eval.multinode;

import java.io.IOException;

/**
 * Policy of the peered topology: the fetcher keeps every connection, and the
 * policy only reports whether the fetcher is connected to the publisher.
 */
public final class KeepConnections implements ConnectionPolicy {

    /**
     * Leaves the connections as they are.
     *
     * @param fetcher         the node that fetches next
     * @param publisherPeerId the peer ID of the publisher
     * @return true when the fetcher holds a connection to the publisher
     * @throws IOException when the fetcher rejects the request
     */
    @Override
    public boolean prepare(KuboClient fetcher, String publisherPeerId) throws IOException {
        return fetcher.isConnectedTo(publisherPeerId);
    }

    /**
     * Ignores the end of a request, since this policy never closes a connection.
     *
     * @param fetcher the node that finished the request
     */
    @Override
    public void finished(KuboClient fetcher) {
        // Nothing to track: the connections stay as they are
    }
}
