package org.ecorefs.ipfs;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Set;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceImpl;
import org.junit.jupiter.api.Test;

/**
 * Checks the dependency scan of the cascade with cross-resource containment,
 * where a container keeps a child that another resource stores. The MoDisco
 * partition uses this for the compilation units of a model.
 *
 * <p>
 * Two cases matter. A container stores the URI of a child in another
 * resource, so the container resource depends on the child resource. And the
 * references of such a child belong to the resource that stores the child,
 * not to the resource of its container. The tests build the resources in
 * memory with a small dynamic metamodel and need no IPFS node.
 * </p>
 */
public class CrossResourceContainmentTest {

    private final EClass node = createNodeClass();
    private final EReference children = (EReference) node.getEStructuralFeature("children");
    private final EReference peer = (EReference) node.getEStructuralFeature("peer");

    /**
     * A container resource depends on the resource that stores one of its
     * children, so a change to the child resource reaches the container.
     */
    @Test
    public void countsAChildStoredInAnotherResource() {
        ResourceSet resourceSet = new ResourceSetImpl();
        Resource containerResource = addResource(resourceSet, "ipfs://QmContainer");
        Resource childResource = addResource(resourceSet, "ipfs://QmChild");
        EObject container = addRoot(containerResource);
        EObject child = addChild(container);
        childResource.getContents().add(child);

        ResourceDependencyGraph graph = ResourceDependencyGraph.of(resourceSet);

        assertEquals(Set.of(childResource), graph.getReferencedResources(containerResource));
        assertEquals(List.of(childResource, containerResource), graph.planSaveOrder(childResource));
    }

    /**
     * A reference of a child that another resource stores belongs to that
     * resource. The container resource reaches the referenced resource only
     * through the child resource, and the save order follows that chain.
     */
    @Test
    public void creditsAReferenceToTheResourceThatStoresTheObject() {
        ResourceSet resourceSet = new ResourceSetImpl();
        Resource containerResource = addResource(resourceSet, "ipfs://QmContainer");
        Resource childResource = addResource(resourceSet, "ipfs://QmChild");
        Resource targetResource = addResource(resourceSet, "ipfs://QmTarget");
        EObject container = addRoot(containerResource);
        EObject child = addChild(container);
        childResource.getContents().add(child);
        child.eSet(peer, addRoot(targetResource));

        ResourceDependencyGraph graph = ResourceDependencyGraph.of(resourceSet);

        assertEquals(Set.of(childResource), graph.getReferencedResources(containerResource));
        assertEquals(Set.of(targetResource), graph.getReferencedResources(childResource));
        assertEquals(List.of(targetResource, childResource, containerResource),
                graph.planSaveOrder(targetResource));
    }

    /**
     * A child that stays in the resource of its container adds no edge.
     */
    @Test
    public void ignoresAChildInTheSameResource() {
        ResourceSet resourceSet = new ResourceSetImpl();
        Resource resource = addResource(resourceSet, "ipfs://QmSingle");
        addChild(addRoot(resource));

        ResourceDependencyGraph graph = ResourceDependencyGraph.of(resourceSet);

        assertEquals(Set.of(), graph.getReferencedResources(resource));
    }

    /**
     * Creates the metamodel of the tests: a node with contained children,
     * which may live in other resources, and one plain reference to a peer.
     *
     * @return the node class
     */
    private static EClass createNodeClass() {
        EcoreFactory factory = EcoreFactory.eINSTANCE;
        EPackage ePackage = factory.createEPackage();
        ePackage.setName("containment");
        ePackage.setNsPrefix("containment");
        ePackage.setNsURI("http://ecorefs.org/test/containment");
        EClass nodeClass = factory.createEClass();
        nodeClass.setName("Node");
        ePackage.getEClassifiers().add(nodeClass);

        EReference childrenReference = factory.createEReference();
        childrenReference.setName("children");
        childrenReference.setEType(nodeClass);
        childrenReference.setContainment(true);
        childrenReference.setResolveProxies(true);
        childrenReference.setUpperBound(-1);
        nodeClass.getEStructuralFeatures().add(childrenReference);

        EReference peerReference = factory.createEReference();
        peerReference.setName("peer");
        peerReference.setEType(nodeClass);
        nodeClass.getEStructuralFeatures().add(peerReference);
        return nodeClass;
    }

    /**
     * Adds an empty resource to the resource set.
     *
     * @param resourceSet the resource set
     * @param uri         the URI of the resource
     * @return the resource
     */
    private static Resource addResource(ResourceSet resourceSet, String uri) {
        Resource resource = new XMIResourceImpl(URI.createURI(uri));
        resourceSet.getResources().add(resource);
        return resource;
    }

    /**
     * Adds a node as a root object of a resource.
     *
     * @param resource the resource
     * @return the new node
     */
    private EObject addRoot(Resource resource) {
        EObject root = EcoreUtil.create(node);
        resource.getContents().add(root);
        return root;
    }

    /**
     * Adds a child node to the children of a node.
     *
     * @param parent the node that contains the child
     * @return the new child
     */
    @SuppressWarnings("unchecked")
    private EObject addChild(EObject parent) {
        EObject child = EcoreUtil.create(node);
        ((List<EObject>) parent.eGet(children)).add(child);
        return child;
    }
}
