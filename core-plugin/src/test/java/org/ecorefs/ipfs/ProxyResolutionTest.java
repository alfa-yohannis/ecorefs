package org.ecorefs.ipfs;

import io.ipfs.api.IPFS;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.InternalEList;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks that EMF resolves a reference across two resources on IPFS.
 *
 * The test saves a target resource, saves a referring resource, and then loads
 * the referring resource in a fresh resource set. The reference of the loaded
 * copy starts as a proxy, because the target resource is not in the new
 * resource set. The first access to the reference has to fetch the target from
 * the local node and return the object behind the proxy.
 */
public class ProxyResolutionTest {

    /** Multiaddress of the local Kubo node. */
    private static final String IPFS_ADDRESS = "/ip4/127.0.0.1/tcp/5001";

    /** Namespace of the metamodel that the test creates at runtime. */
    private static final String PROXY_NAMESPACE = "http://proxy/1.0";

    /** Client for the HTTP application programming interface of the local node. */
    private static IPFS ipfs;

    /** Metamodel of the test, created at runtime. */
    private static EPackage proxyPackage;

    /** The single class of the metamodel. */
    private static EClass nodeClass;

    /** Non-containment reference between nodes of different resources. */
    private static EReference referencesFeature;

    /**
     * Connects to the local IPFS node and creates the metamodel of the test.
     */
    @BeforeAll
    public static void setUp() {
        ipfs = new IPFS(IPFS_ADDRESS);

        EcoreFactory factory = EcoreFactory.eINSTANCE;
        proxyPackage = factory.createEPackage();
        proxyPackage.setName("proxy");
        proxyPackage.setNsPrefix("prx");
        proxyPackage.setNsURI(PROXY_NAMESPACE);

        nodeClass = factory.createEClass();
        nodeClass.setName("Node");

        EAttribute identifierAttribute = factory.createEAttribute();
        identifierAttribute.setName("id");
        identifierAttribute.setEType(EcorePackage.eINSTANCE.getEString());
        nodeClass.getEStructuralFeatures().add(identifierAttribute);

        referencesFeature = factory.createEReference();
        referencesFeature.setName("references");
        referencesFeature.setEType(nodeClass);
        referencesFeature.setUpperBound(-1);
        referencesFeature.setContainment(false);
        nodeClass.getEStructuralFeatures().add(referencesFeature);

        proxyPackage.getEClassifiers().add(nodeClass);
        EPackage.Registry.INSTANCE.put(proxyPackage.getNsURI(), proxyPackage);
    }

    /**
     * Builds a resource set that reads and writes resources on IPFS.
     *
     * @return a resource set with the IPFS factory and the IPFS URI handler
     */
    private ResourceSet createIpfsResourceSet() {
        ResourceSet resourceSet = new ResourceSetImpl();
        IPFSResourceFactoryImpl factory = new IPFSResourceFactoryImpl(ipfs);
        resourceSet.getResourceFactoryRegistry().getProtocolToFactoryMap().put("ipfs", factory);
        resourceSet.getURIConverter().getURIHandlers().add(0, new IPFSURIHandlerImpl(ipfs));
        return resourceSet;
    }

    /**
     * Creates one resource with a single node inside.
     *
     * @param resourceSet    the resource set of the new resource
     * @param placeholderUri the URI before the first save
     * @param nodeIdentifier the identifier of the node inside the resource
     * @return the new resource
     */
    private Resource createResourceWithNode(ResourceSet resourceSet, String placeholderUri, String nodeIdentifier) {
        Resource resource = resourceSet.createResource(URI.createURI(placeholderUri));
        EObject node = proxyPackage.getEFactoryInstance().create(nodeClass);
        node.eSet(nodeClass.getEStructuralFeature("id"), nodeIdentifier);
        resource.getContents().add(node);
        return resource;
    }

    /**
     * Reads the first reference of the node inside a resource.
     *
     * @param resource the resource with the referring node
     * @param resolve  true to resolve a proxy, false to keep the proxy. A
     *                 resolving list of EMF resolves on every read, so the
     *                 unresolved read goes through the internal list.
     * @return the referenced object
     */
    @SuppressWarnings("unchecked")
    private EObject firstReference(Resource resource, boolean resolve) {
        EObject node = resource.getContents().get(0);
        List<EObject> references = (List<EObject>) node.eGet(referencesFeature, resolve);
        if (resolve) {
            return references.get(0);
        }
        return ((InternalEList<EObject>) references).basicGet(0);
    }

    /**
     * Saves two resources on IPFS, loads the referring resource in a fresh
     * resource set and resolves the reference across the two resources.
     *
     * @throws Exception when a save or a load fails
     */
    @Test
    @SuppressWarnings("unchecked")
    public void resolvesAProxyAcrossTwoResourcesOnIpfs() throws Exception {
        ResourceSet writerSet = createIpfsResourceSet();
        Resource target = createResourceWithNode(writerSet, "ipfs://pendingProxyTarget", "proxy-target");
        Resource referrer = createResourceWithNode(writerSet, "ipfs://pendingProxyReferrer", "proxy-referrer");
        EObject targetNode = target.getContents().get(0);
        EObject referrerNode = referrer.getContents().get(0);
        ((List<EObject>) referrerNode.eGet(referencesFeature)).add(targetNode);

        target.save(Collections.emptyMap());
        referrer.save(Collections.emptyMap());
        System.out.println("[proxy] target " + target.getURI());
        System.out.println("[proxy] referrer " + referrer.getURI());

        ResourceSet readerSet = createIpfsResourceSet();
        Resource loadedReferrer = readerSet.getResource(referrer.getURI(), true);
        EObject unresolved = firstReference(loadedReferrer, false);
        assertTrue(unresolved.eIsProxy(), "The reference has to start as a proxy in a fresh resource set");

        EObject resolved = firstReference(loadedReferrer, true);
        assertNotNull(resolved, "The proxy has to resolve");
        assertFalse(resolved.eIsProxy(), "The resolved object cannot stay a proxy");
        assertEquals("proxy-target", resolved.eGet(nodeClass.getEStructuralFeature("id")),
                "The resolved object has to carry the identifier of the target node");
        assertEquals(target.getURI().toString(), resolved.eResource().getURI().toString(),
                "The resolved object has to come from the target resource on IPFS");
        System.out.println("[proxy] resolved " + resolved.eResource().getURI() + " with id "
                + resolved.eGet(nodeClass.getEStructuralFeature("id")));
    }
}
