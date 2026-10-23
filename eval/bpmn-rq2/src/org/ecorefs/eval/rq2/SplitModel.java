package org.ecorefs.eval.rq2;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.bpmn2.Collaboration;
import org.eclipse.bpmn2.Definitions;
import org.eclipse.bpmn2.Process;
import org.eclipse.bpmn2.RootElement;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;

/**
 * A BPMN model split into one resource per process, one resource for the
 * collaborations with the diagrams, and one resource for the other root elements,
 * such as messages, signals and item definitions. Moving the elements keeps every
 * reference, so the references between the parts become references between
 * resources: a participant refers to its process, a message flow to the nodes of
 * two processes, and a diagram shape to the element it draws.
 */
final class SplitModel {

    final List<Resource> resources = new ArrayList<>();
    final List<Resource> processes = new ArrayList<>();
    Resource collaboration;
    Resource shared;

    /**
     * Prevents instances outside {@link #split(Resource, ResourceSet)}.
     */
    private SplitModel() {
    }

    /**
     * Splits a loaded model.
     *
     * @param source          the loaded BPMN resource
     * @param ipfsResourceSet a resource set whose {@code ipfs} URIs get IPFS resources
     * @return the split model
     */
    static SplitModel split(Resource source, ResourceSet ipfsResourceSet) {
        Definitions original = BpmnDefinitions.of(source);
        SplitModel model = new SplitModel();
        Definitions collaborationPart = BpmnDefinitions.createPart(original, "collaboration");
        Definitions sharedPart = BpmnDefinitions.createPart(original, "shared");
        List<Definitions> processParts = new ArrayList<>();
        for (RootElement element : new ArrayList<>(original.getRootElements())) {
            if (element instanceof Process) {
                Definitions processPart = BpmnDefinitions.createPart(original, "process-" + (processParts.size() + 1));
                processPart.getRootElements().add(element);
                processParts.add(processPart);
            } else if (element instanceof Collaboration) {
                collaborationPart.getRootElements().add(element);
            } else {
                sharedPart.getRootElements().add(element);
            }
        }
        collaborationPart.getDiagrams().addAll(original.getDiagrams());
        collaborationPart.getRelationships().addAll(original.getRelationships());
        sharedPart.getImports().addAll(original.getImports());
        sharedPart.getExtensions().addAll(original.getExtensions());
        model.place(ipfsResourceSet, collaborationPart, sharedPart, processParts);
        return model;
    }

    /**
     * Creates the resources of the parts. The shared part gets a resource only
     * when it holds something.
     *
     * @param ipfsResourceSet   the resource set of the IPFS resources
     * @param collaborationPart the definitions with the collaborations and diagrams
     * @param sharedPart        the definitions with the other root elements
     * @param processParts      the definitions with one process each
     */
    private void place(ResourceSet ipfsResourceSet, Definitions collaborationPart, Definitions sharedPart,
            List<Definitions> processParts) {
        collaboration = BpmnDefinitions.placeInNewResource(ipfsResourceSet, collaborationPart, "collaboration");
        resources.add(collaboration);
        for (int index = 0; index < processParts.size(); index++) {
            Resource process = BpmnDefinitions.placeInNewResource(ipfsResourceSet, processParts.get(index),
                    "process-" + (index + 1));
            processes.add(process);
            resources.add(process);
        }
        boolean sharedHoldsSomething = !sharedPart.getRootElements().isEmpty() || !sharedPart.getImports().isEmpty()
                || !sharedPart.getExtensions().isEmpty();
        if (sharedHoldsSomething) {
            shared = BpmnDefinitions.placeInNewResource(ipfsResourceSet, sharedPart, "shared");
            resources.add(shared);
        }
    }
}
