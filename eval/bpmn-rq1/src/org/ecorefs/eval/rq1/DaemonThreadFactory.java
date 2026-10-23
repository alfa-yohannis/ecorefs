package org.ecorefs.eval.rq1;

import java.util.concurrent.ThreadFactory;

/**
 * Creates the worker thread of a round trip as a daemon thread. A model that runs
 * past its time limit leaves its thread behind, and a daemon thread does not keep
 * the virtual machine alive after the last file.
 */
final class DaemonThreadFactory implements ThreadFactory {

    /**
     * Creates a named daemon thread.
     *
     * @param task the work of the thread
     * @return the thread
     */
    @Override
    public Thread newThread(Runnable task) {
        Thread thread = new Thread(task, "round-trip");
        thread.setDaemon(true);
        return thread;
    }
}
