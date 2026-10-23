package org.ecorefs.eval.baselines;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;

/**
 * A Git repository as the store. Every iteration saves the model as XMI into a
 * working tree, commits the file, pushes the commit to a bare repository behind
 * a {@code git daemon}, makes a shallow clone into a fresh folder and loads the
 * model from the clone. The working tree keeps its history across the
 * iterations of one model size, as a modeling project would. Automatic garbage
 * collection is off, so a background {@code git gc} never falls into a
 * measurement.
 */
public final class GitStore implements ModelStore {

    /** Name of the model file in the working tree. */
    private static final String MODEL_FILE = "model.xmi";

    /** Name of the branch. */
    private static final String BRANCH = "main";

    /** Folder of the bare repositories. */
    private final Path remoteDirectory;

    /** Folder of the working trees. */
    private final Path workDirectory;

    /** Folder of the clones. */
    private final Path cloneDirectory;

    /** The daemon that serves the bare repositories. */
    private final GitDaemon daemon;

    /** Working tree of the current model size. */
    private Path workingTree;

    /** URL of the bare repository of the current model size. */
    private String remoteUrl;

    /** Bare repository of the current model size. */
    private Path bareRepository;

    /** Number of elements of the current model size. */
    private int elementCount;

    /**
     * Creates the store.
     *
     * @param runDirectory the work folder of the run
     * @param daemonPort   port of the {@code git daemon}
     */
    public GitStore(Path runDirectory, int daemonPort) {
        Path root = runDirectory.resolve("git");
        this.remoteDirectory = root.resolve("remote");
        this.workDirectory = root.resolve("work");
        this.cloneDirectory = root.resolve("clones");
        this.daemon = new GitDaemon(remoteDirectory, daemonPort);
    }

    /**
     * Returns the name of the store.
     *
     * @return {@code git}
     */
    @Override
    public String name() {
        return "git";
    }

    /**
     * Creates the folders and starts the daemon.
     *
     * @throws Exception when the daemon does not start
     */
    @Override
    public void start() throws Exception {
        Files.createDirectories(workDirectory);
        Files.createDirectories(cloneDirectory);
        daemon.start();
    }

    /**
     * Creates a fresh bare repository and a fresh working tree for one model size.
     *
     * @param elementCount number of elements of the models that follow
     * @throws Exception when a Git command fails
     */
    @Override
    public void prepareSize(int elementCount) throws Exception {
        this.elementCount = elementCount;
        String repositoryName = "model-" + elementCount + ".git";
        bareRepository = remoteDirectory.resolve(repositoryName);
        FileTrees.deleteRecursively(bareRepository);
        ExternalCommand.run(remoteDirectory, "git", "init", "-q", "--bare", "-b", BRANCH, repositoryName);
        ExternalCommand.run(bareRepository, "git", "config", "gc.auto", "0");
        ExternalCommand.run(bareRepository, "git", "config", "receive.autogc", "false");
        remoteUrl = daemon.urlOf(repositoryName);

        workingTree = workDirectory.resolve("model-" + elementCount);
        FileTrees.deleteRecursively(workingTree);
        Files.createDirectories(workingTree);
        ExternalCommand.run(workingTree, "git", "init", "-q", "-b", BRANCH);
        ExternalCommand.run(workingTree, "git", "config", "user.name", "ecorefs benchmark");
        ExternalCommand.run(workingTree, "git", "config", "user.email", "benchmark@ecorefs.invalid");
        ExternalCommand.run(workingTree, "git", "config", "gc.auto", "0");
        ExternalCommand.run(workingTree, "git", "remote", "add", "origin", remoteUrl);
    }

    /**
     * Saves, commits, pushes, clones, loads and visits one copy of the model.
     *
     * @param content   the root objects of the copy
     * @param iteration number of the iteration
     * @param times     receives the phases save, commit, push, clone, load, visit,
     *                  write (save, commit and push) and read (clone and load)
     * @throws Exception when a save, a Git command or a load fails
     */
    @Override
    public void measure(Collection<EObject> content, int iteration, PhaseTimes times) throws Exception {
        Path modelFile = workingTree.resolve(MODEL_FILE);
        Resource target = BenchmarkModels.createXmiResourceSet().createResource(URI.createFileURI(modelFile.toString()));
        target.getContents().addAll(content);

        long writeStart = System.nanoTime();
        target.save(Collections.emptyMap());
        times.recordSince("save", writeStart);

        long start = System.nanoTime();
        ExternalCommand.run(workingTree, "git", "add", MODEL_FILE);
        ExternalCommand.run(workingTree, "git", "commit", "-q", "-m", "iteration " + iteration);
        times.recordSince("commit", start);

        start = System.nanoTime();
        ExternalCommand.run(workingTree, "git", "push", "-q", "origin", BRANCH);
        times.recordSince("push", start);
        times.recordSince("write", writeStart);

        Path clone = cloneDirectory.resolve("clone-" + elementCount + "-" + iteration);
        long readStart = System.nanoTime();
        ExternalCommand.run(cloneDirectory, "git", "clone", "-q", "--depth", "1", "--branch", BRANCH,
                remoteUrl, clone.toString());
        times.recordSince("clone", readStart);

        start = System.nanoTime();
        Resource loaded = BenchmarkModels.createXmiResourceSet()
                .getResource(URI.createFileURI(clone.resolve(MODEL_FILE).toString()), true);
        loaded.getContents();
        times.recordSince("load", start);
        times.recordSince("read", readStart);

        start = System.nanoTime();
        VisitSummary visit = ModelTraversal.visitAll(loaded);
        times.recordSince("visit", start);
        times.recordVisit(visit);

        FileTrees.deleteRecursively(clone);
    }

    /**
     * Reports the size of the bare repository and removes the repositories of
     * the model size.
     *
     * @throws Exception when a folder cannot be removed
     */
    @Override
    public void finishSize() throws Exception {
        System.out.printf("git: bare repository of %d elements holds %d bytes after the run%n",
                elementCount, FileTrees.sizeOf(bareRepository));
        FileTrees.deleteRecursively(workingTree);
        FileTrees.deleteRecursively(bareRepository);
    }

    /**
     * Stops the daemon.
     */
    @Override
    public void stop() {
        daemon.stop();
    }
}
