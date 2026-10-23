package org.ecorefs.eval.baselines;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Waits until a server process accepts connections on a local port, so the
 * benchmark never measures the start-up of a service.
 */
public final class ServicePort {

    /** Pause between two connection attempts, in milliseconds. */
    private static final long RETRY_PAUSE_MILLIS = 100;

    /** Timeout of one connection attempt, in milliseconds. */
    private static final int CONNECT_TIMEOUT_MILLIS = 500;

    /** No instances; the class only offers a static helper. */
    private ServicePort() {
    }

    /**
     * Blocks until a connection to the port succeeds.
     *
     * @param host          the host name or address
     * @param port          the port number
     * @param timeoutMillis the longest time to wait
     * @throws IOException          when the port stays closed until the timeout
     * @throws InterruptedException when the waiting thread is interrupted
     */
    public static void awaitOpen(String host, int port, long timeoutMillis) throws IOException, InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMillis;
        while (System.currentTimeMillis() < deadline) {
            if (accepts(host, port)) {
                return;
            }
            Thread.sleep(RETRY_PAUSE_MILLIS);
        }
        throw new IOException("Nothing listens on " + host + ":" + port + " after " + timeoutMillis + " ms");
    }

    /**
     * Tries one connection.
     *
     * @param host the host name or address
     * @param port the port number
     * @return true when the connection succeeds
     */
    private static boolean accepts(String host, int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), CONNECT_TIMEOUT_MILLIS);
            return true;
        } catch (IOException closed) {
            return false;
        }
    }
}
