package org.ecorefs.eval.rq2;

import org.eclipse.bpmn2.Bpmn2Factory;
import org.eclipse.bpmn2.Definitions;
import org.eclipse.bpmn2.DocumentRoot;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;

/**
 * Finds and creates the {@code Definitions} element, the root of every BPMN
 * model, and places one into a new IPFS resource.
 */
final class BpmnDefinitions {

    /**
     * Prevents instances; the class offers static operations only.
     */
    private BpmnDefinitions() {
    }

    /**
     * Returns the definitions of a loaded BPMN resource.
     *
     * @param resource the resource
     * @return the definitions, or null when the resource holds none
     */
    static Definitions of(Resource resource) {
        for (EObject root : resource.getContents()) {
            if (root instanceof DocumentRoot && ((DocumentRoot) root).getDefinitions() != null) {
                return ((DocumentRoot) root).getDefinitions();
            }
            if (root instanceof Definitions) {
                return (Definitions) root;
            }
        }
        return null;
    }

    /**
     * Creates empty definitions for one part of a split model.
     *
     * @param original the definitions of the whole model
     * @param role     the part, such as {@code shared} or {@code process-1}
     * @return the new definitions with the target namespace of the original
     */
    static Definitions createPart(Definitions original, String role) {
        Definitions part = Bpmn2Factory.eINSTANCE.createDefinitions();
        part.setId(original.getId() == null ? role : original.getId() + "-" + role);
        part.setTargetNamespace(original.getTargetNamespace());
        return part;
    }

    /**
     * Places definitions into a new IPFS resource with a placeholder URI of its
     * own. Each placeholder has its own authority, so a reference to a resource
     * that is not yet saved stays an absolute URI that the checks can recognize.
     *
     * @param ipfsResourceSet a resource set whose {@code ipfs} URIs get IPFS resources
     * @param definitions     the definitions
     * @param role            the name of the placeholder
     * @return the new resource
     */
    static Resource placeInNewResource(ResourceSet ipfsResourceSet, Definitions definitions, String role) {
        Resource resource = ipfsResourceSet.createResource(URI.createURI("ipfs://pending-" + role));
        resource.getContents().add(definitions);
        return resource;
    }
}
