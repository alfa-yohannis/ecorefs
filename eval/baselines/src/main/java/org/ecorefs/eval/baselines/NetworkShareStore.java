package org.ecorefs.eval.baselines;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;

/**
 * A shared network file system as the store: XMI files on {@code sshfs} mounts
 * of a folder that a separate SSH server process offers on the loopback
 * interface. The share is mounted twice, once for the writing client and once
 * for the reading client, so a load cannot reuse what the writing mount keeps
 * in memory, as with two machines that share a folder.
 */
public final class NetworkShareStore implements ModelStore {

    /** The SSH server behind the share. */
    private final SshServer server;

    /** The mount of the client that saves. */
    private final SshfsMount writerMount;

    /** The mount of the client that loads. */
    private final SshfsMount readerMount;

    /** Number of elements of the current model size. */
    private int elementCount;

    /**
     * Creates the store.
     *
     * @param runDirectory the work folder of the run
     * @param sshPort      port of the SSH server
     */
    public NetworkShareStore(Path runDirectory, int sshPort) {
        Path root = runDirectory.resolve("share");
        Path exported = root.resolve("exported");
        this.server = new SshServer(root.resolve("server"), sshPort);
        this.writerMount = new SshfsMount(server, exported, root.resolve("writer"));
        this.readerMount = new SshfsMount(server, exported, root.resolve("reader"));
    }

    /**
     * Returns the name of the store.
     *
     * @return {@code share}
     */
    @Override
    public String name() {
        return "share";
    }

    /**
     * Starts the SSH server and mounts the share for both clients.
     *
     * @throws Exception when the server or a mount fails
     */
    @Override
    public void start() throws Exception {
        server.start();
        writerMount.mount();
        readerMount.mount();
    }

    /**
     * Remembers the model size for the file names. Every file name is used once
     * per run, so no attribute that the kernel caches for a mount can describe
     * an older file of the same name.
     *
     * @param elementCount number of elements of the models that follow
     */
    @Override
    public void prepareSize(int elementCount) {
        this.elementCount = elementCount;
    }

    /**
     * Saves the copy to a new file through the writing mount, loads the file
     * through the reading mount in a fresh resource set and visits the loaded
     * model.
     *
     * @param content   the root objects of the copy
     * @param iteration number of the iteration
     * @param times     receives the phases save, load, visit, write and read
     * @throws Exception when the save or the load fails
     */
    @Override
    public void measure(Collection<EObject> content, int iteration, PhaseTimes times) throws Exception {
        String fileName = "model-" + elementCount + "-" + iteration + ".xmi";
        Path writtenFile = writerMount.mountPoint().resolve(fileName);
        Resource target = BenchmarkModels.createXmiResourceSet()
                .createResource(URI.createFileURI(writtenFile.toString()));
        target.getContents().addAll(content);

        long start = System.nanoTime();
        target.save(Collections.emptyMap());
        times.recordSince("save", start);
        times.recordSince("write", start);

        Path readFile = readerMount.mountPoint().resolve(fileName);
        start = System.nanoTime();
        Resource loaded = BenchmarkModels.createXmiResourceSet()
                .getResource(URI.createFileURI(readFile.toString()), true);
        loaded.getContents();
        times.recordSince("load", start);
        times.recordSince("read", start);

        start = System.nanoTime();
        VisitSummary visit = ModelTraversal.visitAll(loaded);
        times.recordSince("visit", start);
        times.recordVisit(visit);

        Files.deleteIfExists(writtenFile);
    }

    /**
     * Needs no cleanup per model size.
     */
    @Override
    public void finishSize() {
        // The files are deleted after every iteration.
    }

    /**
     * Unmounts both clients and stops the server.
     */
    @Override
    public void stop() {
        readerMount.unmount();
        writerMount.unmount();
        server.stop();
    }
}
