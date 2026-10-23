package org.ecorefs.eval.baselines;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;

/**
 * The local XMI path of the paper: a save to a file on the local disk and a load
 * of the file in a fresh resource set.
 */
public final class XmiFileStore implements ModelStore {

    /** Folder that receives the XMI files. */
    private final Path directory;

    /**
     * Creates the store.
     *
     * @param workDirectory the work folder of the run
     */
    public XmiFileStore(Path workDirectory) {
        this.directory = workDirectory.resolve("xmi");
    }

    /**
     * Returns the name of the store.
     *
     * @return {@code xmi}
     */
    @Override
    public String name() {
        return "xmi";
    }

    /**
     * Creates the folder of the files.
     *
     * @throws Exception when the folder cannot be created
     */
    @Override
    public void start() throws Exception {
        Files.createDirectories(directory);
    }

    /**
     * Needs no preparation per model size.
     *
     * @param elementCount number of elements of the models that follow
     */
    @Override
    public void prepareSize(int elementCount) {
        // Every iteration writes its own file.
    }

    /**
     * Saves the copy to a new file, loads the file in a fresh resource set and
     * visits the loaded model.
     *
     * @param content   the root objects of the copy
     * @param iteration number of the iteration
     * @param times     receives the phases save, load, visit, write and read
     * @throws Exception when the save or the load fails
     */
    @Override
    public void measure(Collection<EObject> content, int iteration, PhaseTimes times) throws Exception {
        Path file = Files.createTempFile(directory, "model_", ".xmi");
        Resource target = BenchmarkModels.createXmiResourceSet().createResource(URI.createFileURI(file.toString()));
        target.getContents().addAll(content);

        long start = System.nanoTime();
        target.save(Collections.emptyMap());
        times.recordSince("save", start);
        times.recordSince("write", start);

        start = System.nanoTime();
        Resource loaded = BenchmarkModels.createXmiResourceSet().getResource(URI.createFileURI(file.toString()), true);
        loaded.getContents();
        times.recordSince("load", start);
        times.recordSince("read", start);

        start = System.nanoTime();
        VisitSummary visit = ModelTraversal.visitAll(loaded);
        times.recordSince("visit", start);
        times.recordVisit(visit);

        Files.deleteIfExists(file);
    }

    /**
     * Needs no cleanup per model size.
     */
    @Override
    public void finishSize() {
        // The files are deleted after every iteration.
    }

    /**
     * Needs no services.
     */
    @Override
    public void stop() {
        // Nothing runs in the background.
    }
}
