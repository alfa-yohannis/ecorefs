package org.ecorefs.eval.rq1;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

import org.ecorefs.eval.bpmn.ExperimentResourceSets;
import org.ecorefs.eval.bpmn.KuboNode;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;

/**
 * Saves the model as BPMN 2.0 XML and adds the bytes to the node, as the IPFS
 * commands of the modeler publish a file, then loads the content by CID with the
 * BPMN 2.0 resource and the IPFS URI handler of the core plugin. With numbered
 * identifiers, every object without an identifier first receives one in document
 * order, so the BPMN resource assigns no random UUIDs during the save.
 */
final class IpfsBpmnXmlPath extends PersistencePath {

    private final KuboNode node;
    private final boolean numberIds;

    /**
     * Creates the path.
     *
     * @param name         the column prefix in the result table
     * @param resourceSets the builder of the resource sets
     * @param node         the node of the experiment
     * @param numberIds    whether objects without an identifier are numbered before the save
     */
    IpfsBpmnXmlPath(String name, ExperimentResourceSets resourceSets, KuboNode node, boolean numberIds) {
        super(name, resourceSets);
        this.node = node;
        this.numberIds = numberIds;
    }

    /**
     * Serializes the BPMN resource and adds the bytes to the node.
     *
     * @param source the freshly loaded BPMN resource
     * @return the outcome with the CID of the bytes
     * @throws IOException when the node refuses the content
     */
    @Override
    SavedModel save(Resource source) throws IOException {
        if (numberIds) {
            DocumentOrderIds.assign(source);
        }
        long start = System.nanoTime();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        source.save(bytes, null);
        String cid = node.add(bytes.toByteArray());
        long saveNanos = System.nanoTime() - start;
        return new SavedModel(source, URI.createURI("ipfs://" + cid), cid, saveNanos);
    }

    /**
     * Creates a BPMN resource for the CID in a fresh resource set.
     *
     * @param saved the outcome of the save
     * @return the resource, not yet loaded
     */
    @Override
    Resource createLoadTarget(SavedModel saved) {
        return resourceSets.forIpfsBpmnXml().createResource(saved.location);
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
}
