package org.ecorefs.eval.rq1;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.ecorefs.eval.bpmn.ExperimentResourceSets;
import org.ecorefs.eval.bpmn.Digests;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;

/**
 * Saves the model as an XMI file in the temporary folder: the local path with the
 * same serialization as {@code IPFSResourceImpl}, and therefore the paired
 * baseline of {@link IpfsXmiPath}.
 */
final class LocalXmiPath extends PersistencePath {

    private final boolean extendedMetadata;
    private final Path temporaryFolder;
    private long savedFiles;

    /**
     * Creates the path.
     *
     * @param name             the column prefix in the result table
     * @param resourceSets     the builder of the resource sets
     * @param extendedMetadata whether save and load use extended metadata
     * @param temporaryFolder  the folder for the XMI files
     */
    LocalXmiPath(String name, ExperimentResourceSets resourceSets, boolean extendedMetadata, Path temporaryFolder) {
        super(name, resourceSets);
        this.extendedMetadata = extendedMetadata;
        this.temporaryFolder = temporaryFolder;
    }

    /**
     * Moves the content of the BPMN resource into an XMI resource and saves it to a file.
     *
     * @param source the freshly loaded BPMN resource
     * @return the outcome with the SHA-256 digest of the file
     * @throws IOException when the file cannot be written
     */
    @Override
    SavedModel save(Resource source) throws IOException {
        Path file = temporaryFolder.resolve("model-" + (++savedFiles) + ".xmi");
        Resource target = resourceSets.forXmiFiles().createResource(URI.createFileURI(file.toString()));
        target.getContents().addAll(source.getContents());
        try {
            long start = System.nanoTime();
            target.save(ExperimentResourceSets.xmiSaveOptions(extendedMetadata));
            long saveNanos = System.nanoTime() - start;
            return new SavedModel(target, target.getURI(), Digests.sha256(Files.readAllBytes(file)), saveNanos);
        } catch (IOException | RuntimeException e) {
            Files.deleteIfExists(file);
            throw e;
        }
    }

    /**
     * Creates an XMI resource for the saved file in a fresh resource set.
     *
     * @param saved the outcome of the save
     * @return the resource, not yet loaded
     */
    @Override
    Resource createLoadTarget(SavedModel saved) {
        return resourceSets.forXmiFiles().createResource(saved.location);
    }

    /**
     * Returns the secure load options, with extended metadata when the save used them.
     *
     * @return the options
     */
    @Override
    Map<Object, Object> loadOptions() {
        return ExperimentResourceSets.loadOptions(extendedMetadata);
    }

    /**
     * Deletes the XMI file.
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
