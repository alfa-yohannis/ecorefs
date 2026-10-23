package org.ecorefs.eval.baselines;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * An OpenSSH server that runs as the current user on the loopback interface. The
 * server only offers SFTP to one generated key, so the network share of the
 * benchmark needs no root rights and touches no system configuration.
 */
public final class SshServer {

    /** Longest wait for the server to accept connections, in milliseconds. */
    private static final long START_TIMEOUT_MILLIS = 10_000;

    /** Program of the server. */
    private static final String SSHD = "/usr/sbin/sshd";

    /** Folder with the keys, the configuration and the log of the server. */
    private final Path directory;

    /** Port of the server. */
    private final int port;

    /** The running server, or null before the start. */
    private Process process;

    /**
     * Creates the server without starting the server.
     *
     * @param directory folder for keys, configuration and log
     * @param port      port of the server on 127.0.0.1
     */
    public SshServer(Path directory, int port) {
        this.directory = directory;
        this.port = port;
    }

    /**
     * Generates the keys and the configuration and starts the server.
     *
     * @throws Exception when a key cannot be generated or the server does not start
     */
    public void start() throws Exception {
        Files.createDirectories(directory);
        generateKey("host_key");
        generateKey("client_key");
        Files.copy(directory.resolve("client_key.pub"), directory.resolve("authorized_keys"),
                StandardCopyOption.REPLACE_EXISTING);
        Path configuration = directory.resolve("sshd_config");
        Files.write(configuration, configurationLines());
        process = ExternalCommand.start(directory, directory.resolve("sshd.log"),
                SSHD, "-D", "-e", "-f", configuration.toString());
        ServicePort.awaitOpen("127.0.0.1", port, START_TIMEOUT_MILLIS);
    }

    /**
     * Generates an Ed25519 key pair without a passphrase, unless the key exists.
     *
     * @param name file name of the private key
     * @throws Exception when {@code ssh-keygen} fails
     */
    private void generateKey(String name) throws Exception {
        if (Files.exists(directory.resolve(name))) {
            return;
        }
        ExternalCommand.run(directory, "ssh-keygen", "-q", "-t", "ed25519", "-N", "", "-f", name);
    }

    /**
     * Builds the server configuration: loopback only, key login only, SFTP inside
     * the server process, and no checks that need root rights.
     *
     * @return the lines of {@code sshd_config}
     */
    private List<String> configurationLines() {
        return List.of(
                "Port " + port,
                "ListenAddress 127.0.0.1",
                "HostKey " + directory.resolve("host_key"),
                "PidFile " + directory.resolve("sshd.pid"),
                "AuthorizedKeysFile " + directory.resolve("authorized_keys"),
                "PasswordAuthentication no",
                "KbdInteractiveAuthentication no",
                "UsePAM no",
                "StrictModes no",
                "Subsystem sftp internal-sftp",
                "LogLevel ERROR");
    }

    /**
     * Returns the private key that the server accepts.
     *
     * @return the path of the client key
     */
    public Path clientKey() {
        return directory.resolve("client_key");
    }

    /**
     * Returns the port of the server.
     *
     * @return the port number
     */
    public int port() {
        return port;
    }

    /**
     * Stops the server.
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
