package org.ecorefs.eval.rq1;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.ecorefs.eval.bpmn.ExperimentResourceSets;
import org.ecorefs.eval.bpmn.Digests;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;

/**
 * Saves the model as BPMN 2.0 XML with the resource of Eclipse into a file in the
 * temporary folder: the paired local baseline of {@link IpfsBpmnXmlPath}.
 */
final class LocalBpmnXmlPath extends PersistencePath {

    private final Path temporaryFolder;
    private long savedFiles;

    /**
     * Creates the path.
     *
     * @param name            the column prefix in the result table
     * @param resourceSets    the builder of the resource sets
     * @param temporaryFolder the folder for the BPMN files
     */
    LocalBpmnXmlPath(String name, ExperimentResourceSets resourceSets, Path temporaryFolder) {
        super(name, resourceSets);
        this.temporaryFolder = temporaryFolder;
    }

    /**
     * Saves the BPMN resource into a new file. The resource keeps its URI, so the
     * save writes the same text as a save in place.
     *
     * @param source the freshly loaded BPMN resource
     * @return the outcome with the SHA-256 digest of the file
     * @throws IOException when the file cannot be written
     */
    @Override
    SavedModel save(Resource source) throws IOException {
        Path file = temporaryFolder.resolve("model-" + (++savedFiles) + ".bpmn");
        try {
            long start = System.nanoTime();
            try (OutputStream output = Files.newOutputStream(file)) {
                source.save(output, null);
            }
            long saveNanos = System.nanoTime() - start;
            return new SavedModel(source, URI.createFileURI(file.toString()), Digests.sha256(Files.readAllBytes(file)),
                    saveNanos);
        } catch (IOException | RuntimeException e) {
            Files.deleteIfExists(file);
            throw e;
        }
    }

    /**
     * Creates a BPMN resource for the saved file in a fresh resource set.
     *
     * @param saved the outcome of the save
     * @return the resource, not yet loaded
     */
    @Override
    Resource createLoadTarget(SavedModel saved) {
        return resourceSets.forBpmnFiles().createResource(saved.location);
    }

    /**
     * Returns the secure load options.
     *
     * @return the options
     */
    @Override
    Map<Object, Object> loadOptions() {
        return ExperimentResourceSets.loadOptions(false);
    }

    /**
     * Deletes the BPMN file.
     *
     * @param saved the outcome of the save
     */
    @Override
    void discard(SavedModel saved) {
        super.discard(saved);
        try {
            Files.deleteIfExists(Path.of(saved.location.toFileString()));
        } catch (IOException e) {
            System.err.println("Could not delete " + saved.location + ": " + e.getMessage());
        }
    }
}
