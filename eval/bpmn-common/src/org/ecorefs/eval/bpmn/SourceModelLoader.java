package org.ecorefs.eval.bpmn;

import java.io.IOException;
import java.nio.file.Path;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;

/**
 * Loads a model file of the dataset with the BPMN 2.0 resource of Eclipse, the way
 * the modeler reads a file, and resolves the references that point into the same
 * file. A file whose load reports errors but still yields content counts as
 * loaded, as in the load check of the dataset.
 */
public final class SourceModelLoader {

    private final ExperimentResourceSets resourceSets;

    /**
     * Creates the loader.
     *
     * @param resourceSets the builder of the resource sets
     */
    public SourceModelLoader(ExperimentResourceSets resourceSets) {
        this.resourceSets = resourceSets;
    }

    /**
     * Loads one file in a fresh resource set.
     *
     * @param modelFile the BPMN file
     * @return the loaded resource, possibly with errors
     * @throws IOException when the load yields no content
     */
    public Resource load(Path modelFile) throws IOException {
        Resource resource = resourceSets.forBpmnFiles()
                .createResource(URI.createFileURI(modelFile.toAbsolutePath().toString()));
        try {
            resource.load(ExperimentResourceSets.loadOptions(false));
        } catch (IOException | RuntimeException e) {
            if (resource.getContents().isEmpty()) {
                throw e;
            }
        }
        LocalProxyResolver.resolveLocalProxies(resource);
        return resource;
    }
}
