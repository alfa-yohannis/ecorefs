package org.ecorefs.eval.baselines;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

/**
 * A {@code git daemon} that serves the bare repositories of the benchmark over
 * the Git protocol on the loopback interface. The daemon runs as a separate
 * process, so a push and a clone cross a process and a network boundary, as
 * they would with a remote repository.
 */
public final class GitDaemon {

    /** Longest wait for the daemon to accept connections, in milliseconds. */
    private static final long START_TIMEOUT_MILLIS = 10_000;

    /** Folder with the bare repositories. */
    private final Path baseDirectory;

    /** Port of the daemon. */
    private final int port;

    /** The running daemon, or null before the start. */
    private Process process;

    /**
     * Creates the daemon without starting the daemon.
     *
     * @param baseDirectory folder with the bare repositories
     * @param port          port of the daemon on 127.0.0.1
     */
    public GitDaemon(Path baseDirectory, int port) {
        this.baseDirectory = baseDirectory;
        this.port = port;
    }

    /**
     * Starts the daemon with pushes enabled and waits until the daemon listens.
     *
     * @throws Exception when the daemon does not start
     */
    public void start() throws Exception {
        Files.createDirectories(baseDirectory);
        Path logFile = baseDirectory.resolveSibling("git-daemon.log");
        process = ExternalCommand.start(baseDirectory, logFile, "git", "daemon", "--reuseaddr",
                "--listen=127.0.0.1", "--port=" + port, "--base-path=" + baseDirectory, "--export-all",
                "--enable=receive-pack", "--informative-errors", baseDirectory.toString());
        ServicePort.awaitOpen("127.0.0.1", port, START_TIMEOUT_MILLIS);
    }

    /**
     * Returns the Git URL of a bare repository below the base folder.
     *
     * @param repositoryName the folder name of the bare repository
     * @return the URL, for example {@code git://127.0.0.1:9419/model-100.git}
     */
    public String urlOf(String repositoryName) {
        return "git://127.0.0.1:" + port + "/" + repositoryName;
    }

    /**
     * Stops the daemon.
     */
    public void stop() {
        if (process == null) {
            return;
        }
        process.destroy();
        try {
            process.waitFor(5, TimeUnit.SECONDS);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
        process.destroyForcibly();
    }
}
