package org.ecorefs.eval.baselines;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Mounts a folder of an SSH server with {@code sshfs}, a file system in user
 * space over SFTP. NFS and SMB mounts need root rights on this machine, while
 * {@code sshfs} runs as the current user through FUSE.
 */
public final class SshfsMount {

    /** The server that offers the folder. */
    private final SshServer server;

    /** The folder on the server side. */
    private final Path remoteDirectory;

    /** The local mount point. */
    private final Path mountPoint;

    /** Whether the folder is mounted. */
    private boolean mounted;

    /**
     * Creates the mount without mounting.
     *
     * @param server          the SSH server
     * @param remoteDirectory the folder to mount
     * @param mountPoint      the local mount point
     */
    public SshfsMount(SshServer server, Path remoteDirectory, Path mountPoint) {
        this.server = server;
        this.remoteDirectory = remoteDirectory;
        this.mountPoint = mountPoint;
    }

    /**
     * Mounts the folder. The directory cache is off, so a load after a save always
     * asks the server, and the kernel page cache stays off as in the defaults of
     * {@code sshfs} 3.
     *
     * @throws Exception when {@code sshfs} fails
     */
    public void mount() throws Exception {
        Files.createDirectories(remoteDirectory);
        Files.createDirectories(mountPoint);
        ExternalCommand.run(mountPoint.getParent(), "sshfs",
                "-p", Integer.toString(server.port()),
                "-o", "IdentityFile=" + server.clientKey(),
                "-o", "StrictHostKeyChecking=no",
                "-o", "UserKnownHostsFile=/dev/null",
                "-o", "BatchMode=yes",
                "-o", "dir_cache=no",
                "-o", "LogLevel=ERROR",
                "127.0.0.1:" + remoteDirectory, mountPoint.toString());
        mounted = true;
    }

    /**
     * Returns the local mount point.
     *
     * @return the folder where the share appears
     */
    public Path mountPoint() {
        return mountPoint;
    }

    /**
     * Unmounts the folder.
     */
    public void unmount() {
        if (!mounted) {
            return;
        }
        try {
            ExternalCommand.run(mountPoint.getParent(), "fusermount3", "-u", mountPoint.toString());
            mounted = false;
        } catch (Exception failure) {
            System.err.println("Unmount failed: " + failure.getMessage());
        }
    }
}
