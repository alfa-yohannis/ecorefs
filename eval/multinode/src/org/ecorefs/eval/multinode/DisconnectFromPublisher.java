package org.ecorefs.eval.multinode;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Policy of the dht topology: the fetcher closes its connections to the
 * publisher before the fetch, so the fetch has to find the publisher through a
 * provider lookup.
 *
 * <p>A request leaves activity behind, such as a provider lookup that keeps
 * running and dials the providers it finds. In the validation runs mn-b
 * connected to mn-a again 0.6 s and 1.6 s after a fetch over wan50. Closing
 * those connections as they came back did not help: when a close met a dial in
 * progress, libp2p put mn-a into its dial backoff, and the next fetch waited
 * about a minute for the periodic provider search of Bitswap. The policy
 * therefore lets the activity end first. It waits until three seconds have
 * passed since the last request of the fetcher, closes the connections once,
 * and checks half a second later that none has come back.
 */
public final class DisconnectFromPublisher implements ConnectionPolicy {

    /** Time after the last request of a fetcher before its connections are closed. */
    private static final long QUIET_NANOS = 3_000_000_000L;

    /** Time between the close and the check that no connection came back. */
    private static final long CHECK_DELAY_MILLIS = 500;

    /** Number of closes before the fetch starts anyway; the row then records the connection. */
    private static final int ATTEMPTS = 5;

    /** End of the last request per fetcher name, in System.nanoTime. */
    private final Map<String, Long> lastRequestEnds = new HashMap<>();

    /**
     * Waits for the activity of earlier requests to end, then closes the
     * connections of the fetcher to the publisher.
     *
     * @param fetcher         the node that fetches next
     * @param publisherPeerId the peer ID of the publisher
     * @return true when a connection to the publisher exists when the fetch starts
     * @throws IOException when the fetcher rejects a request or the wait is interrupted
     */
    @Override
    public boolean prepare(KuboClient fetcher, String publisherPeerId) throws IOException {
        for (int attempt = 0; attempt < ATTEMPTS; attempt++) {
            waitUntilQuiet(fetcher);
            fetcher.disconnect(publisherPeerId);
            pause(CHECK_DELAY_MILLIS);
            if (!fetcher.isConnectedTo(publisherPeerId)) {
                return false;
            }
            // A connection came back, so some activity is still running: wait again
            finished(fetcher);
        }
        return fetcher.isConnectedTo(publisherPeerId);
    }

    /**
     * Records the end of a request of the fetcher.
     *
     * @param fetcher the node that finished a request
     */
    @Override
    public void finished(KuboClient fetcher) {
        lastRequestEnds.put(fetcher.name(), System.nanoTime());
    }

    /**
     * Waits until the quiet time has passed since the last request of the fetcher.
     *
     * @param fetcher the node that fetches next
     * @throws IOException when the wait is interrupted
     */
    private void waitUntilQuiet(KuboClient fetcher) throws IOException {
        Long lastRequestEnd = lastRequestEnds.get(fetcher.name());
        if (lastRequestEnd == null) {
            return;
        }
        long remaining = QUIET_NANOS - (System.nanoTime() - lastRequestEnd);
        if (remaining > 0) {
            pause(remaining / 1_000_000 + 1);
        }
    }

    /**
     * Waits for a number of milliseconds.
     *
     * @param millis the time to wait
     * @throws IOException when the thread is interrupted during the wait
     */
    private static void pause(long millis) throws IOException {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while waiting for the fetcher to go quiet", interrupted);
        }
    }
}
