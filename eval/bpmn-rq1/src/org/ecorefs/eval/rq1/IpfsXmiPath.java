package org.ecorefs.eval.rq1;

import java.io.IOException;
import java.util.Map;

import org.ecorefs.eval.bpmn.ExperimentResourceSets;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;

/**
 * Saves the model through {@code IPFSResourceImpl} of the core plugin, which writes
 * XMI and adds it to the node, and loads it back by CID through the IPFS resource
 * and URI handler. The placeholder URI {@code ipfs://pending} is the one the tests
 * of the core plugin use.
 */
final class IpfsXmiPath extends PersistencePath {

    private static final URI PLACEHOLDER = URI.createURI("ipfs://pending");

    private final boolean extendedMetadata;

    /**
     * Creates the path.
     *
     * @param name             the column prefix in the result table
     * @param resourceSets     the builder of the resource sets
     * @param extendedMetadata whether save and load use extended metadata
     */
    IpfsXmiPath(String name, ExperimentResourceSets resourceSets, boolean extendedMetadata) {
        super(name, resourceSets);
        this.extendedMetadata = extendedMetadata;
    }

    /**
     * Moves the content of the BPMN resource into an IPFS resource and saves it.
     *
     * @param source the freshly loaded BPMN resource
     * @return the outcome with the CID that the save gave the resource
     * @throws IOException when the node refuses the content
     */
    @Override
    SavedModel save(Resource source) throws IOException {
        Resource target = resourceSets.forIpfsXmi().createResource(PLACEHOLDER);
        target.getContents().addAll(source.getContents());
        long start = System.nanoTime();
        target.save(ExperimentResourceSets.xmiSaveOptions(extendedMetadata));
        long saveNanos = System.nanoTime() - start;
        return new SavedModel(target, target.getURI(), target.getURI().authority(), saveNanos);
    }

    /**
     * Creates an IPFS resource for the CID in a fresh resource set.
     *
     * @param saved the outcome of the save
     * @return the resource, not yet loaded
     */
    @Override
    Resource createLoadTarget(SavedModel saved) {
        return resourceSets.forIpfsXmi().createResource(saved.location);
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
}
