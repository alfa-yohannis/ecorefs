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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks that a cascade save leaves every reference pointing at the current
 * content identifier of the target resource.
 *
 * The tests cover three shapes of the reference graph: a chain of two
 * resources, a diamond where two reference paths reach the changed resource,
 * and a chain of four resources. Every check reads the serialized resource back
 * from the local IPFS node, so the check sees the stored bytes instead of the
 * objects in memory.
 */
public class CascadeReferenceIntegrityTest {

    /** Multiaddress of the local Kubo node. */
    private static final String IPFS_ADDRESS = "/ip4/127.0.0.1/tcp/5001";

    /** Namespace of the metamodel that the tests create at runtime. */
    private static final String CASCADE_NAMESPACE = "http://cascade/1.0";

    /** Client for the HTTP application programming interface of the local node. */
    private static IPFS ipfs;

    /** Metamodel of the tests, created at runtime. */
    private static EPackage cascadePackage;

    /** The single class of the metamodel. */
    private static EClass nodeClass;

    /** Non-containment reference between nodes of different resources. */
    private static EReference referencesFeature;

    /**
     * Connects to the local IPFS node and creates the metamodel of the tests, so
     * the tests need no generated Java classes.
     */
    @BeforeAll
    public static void setUp() {
        ipfs = new IPFS(IPFS_ADDRESS);

        EcoreFactory factory = EcoreFactory.eINSTANCE;
        cascadePackage = factory.createEPackage();
        cascadePackage.setName("cascade");
        cascadePackage.setNsPrefix("cas");
        cascadePackage.setNsURI(CASCADE_NAMESPACE);

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

        cascadePackage.getEClassifiers().add(nodeClass);
        EPackage.Registry.INSTANCE.put(cascadePackage.getNsURI(), cascadePackage);
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
        resourceSet.getResourceFactoryRegistry().getProtocolToFactoryMap().put("ipns", factory);
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
        EObject node = cascadePackage.getEFactoryInstance().create(nodeClass);
        node.eSet(nodeClass.getEStructuralFeature("id"), nodeIdentifier);
        resource.getContents().add(node);
        return resource;
    }

    /**
     * Adds a reference from the node of one resource to the node of another
     * resource.
     *
     * @param source the resource that receives the reference
     * @param target the resource with the referenced node
     */
    @SuppressWarnings("unchecked")
    private void addReference(Resource source, Resource target) {
        EObject sourceNode = source.getContents().get(0);
        EObject targetNode = target.getContents().get(0);
        ((List<EObject>) sourceNode.eGet(referencesFeature)).add(targetNode);
    }

    /**
     * Changes the identifier of the node inside a resource, so the next save
     * produces new content and a new content identifier.
     *
     * @param resource      the resource to change
     * @param newIdentifier the new identifier of the node
     */
    private void changeNodeIdentifier(Resource resource, String newIdentifier) {
        EObject node = resource.getContents().get(0);
        node.eSet(nodeClass.getEStructuralFeature("id"), newIdentifier);
    }

    /**
     * Reads the stored bytes of a resource back from the local IPFS node.
     *
     * @param resource the resource with an IPFS URI
     * @return the serialized resource as text
     * @throws IOException when the content cannot be fetched
     */
    private String readStoredContent(Resource resource) throws IOException {
        IPFSURIHandlerImpl handler = new IPFSURIHandlerImpl(ipfs);
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (InputStream input = handler.createInputStream(resource.getURI(), Collections.emptyMap())) {
            byte[] chunk = new byte[8192];
            int read = input.read(chunk);
            while (read > 0) {
                buffer.write(chunk, 0, read);
                read = input.read(chunk);
            }
        }
        return new String(buffer.toByteArray(), StandardCharsets.UTF_8);
    }

    /**
     * Saves a chain of two resources, changes the target and checks that the
     * referencing resource carries the new content identifier afterwards.
     *
     * @throws Exception when a save or a read fails
     */
    @Test
    public void rewritesTheReferenceOfATwoResourceChain() throws Exception {
        ResourceSet resourceSet = createIpfsResourceSet();
        Resource target = createResourceWithNode(resourceSet, "ipfs://pendingTarget", "target");
        Resource referrer = createResourceWithNode(resourceSet, "ipfs://pendingReferrer", "referrer");
        addReference(referrer, target);

        target.save(Collections.emptyMap());
        referrer.save(Collections.emptyMap());

        changeNodeIdentifier(target, "target-version-2");
        IPFSModelPersister.cascadeSave(target, Collections.emptyMap());

        String storedReferrer = readStoredContent(referrer);
        System.out.println("[two resources] target " + target.getURI());
        System.out.println("[two resources] referrer " + referrer.getURI());
        assertTrue(storedReferrer.contains(target.getURI().toString()),
                "The stored referrer has to carry the new content identifier of the target");
    }

    /**
     * Saves a diamond where two reference paths reach the changed resource, and
     * checks that the top resource carries the current content identifier of both
     * targets after one cascade save.
     *
     * @throws Exception when a save or a read fails
     */
    @Test
    public void rewritesEveryPathOfADiamond() throws Exception {
        ResourceSet resourceSet = createIpfsResourceSet();
        Resource payment = createResourceWithNode(resourceSet, "ipfs://pendingPayment", "payment");
        Resource order = createResourceWithNode(resourceSet, "ipfs://pendingOrder", "order");
        Resource collaboration = createResourceWithNode(resourceSet, "ipfs://pendingCollaboration", "collaboration");
        addReference(order, payment);
        addReference(collaboration, order);
        addReference(collaboration, payment);

        payment.save(Collections.emptyMap());
        order.save(Collections.emptyMap());
        collaboration.save(Collections.emptyMap());

        changeNodeIdentifier(payment, "payment-version-2");
        IPFSModelPersister.cascadeSave(payment, Collections.emptyMap());

        String storedCollaboration = readStoredContent(collaboration);
        System.out.println("[diamond] payment " + payment.getURI());
        System.out.println("[diamond] order " + order.getURI());
        System.out.println("[diamond] collaboration " + collaboration.getURI());
        assertTrue(storedCollaboration.contains(payment.getURI().toString()),
                "The collaboration has to carry the new content identifier of the payment process");
        assertTrue(storedCollaboration.contains(order.getURI().toString()),
                "The collaboration has to carry the new content identifier of the order process");
    }

    /**
     * Saves a chain of four resources, changes the last resource and checks every
     * rewritten reference. The test also prints the time of the cascade save.
     *
     * @throws Exception when a save or a read fails
     */
    @Test
    public void rewritesEveryReferenceOfALongerChain() throws Exception {
        ResourceSet resourceSet = createIpfsResourceSet();
        Resource fourth = createResourceWithNode(resourceSet, "ipfs://pendingFourth", "fourth");
        Resource third = createResourceWithNode(resourceSet, "ipfs://pendingThird", "third");
        Resource second = createResourceWithNode(resourceSet, "ipfs://pendingSecond", "second");
        Resource first = createResourceWithNode(resourceSet, "ipfs://pendingFirst", "first");
        addReference(third, fourth);
        addReference(second, third);
        addReference(first, second);

        fourth.save(Collections.emptyMap());
        third.save(Collections.emptyMap());
        second.save(Collections.emptyMap());
        first.save(Collections.emptyMap());

        changeNodeIdentifier(fourth, "fourth-version-2");
        long start = System.nanoTime();
        IPFSModelPersister.cascadeSave(fourth, Collections.emptyMap());
        double elapsedMilliseconds = (System.nanoTime() - start) / 1_000_000.0;

        String storedThird = readStoredContent(third);
        String storedSecond = readStoredContent(second);
        String storedFirst = readStoredContent(first);
        System.out.printf("[chain of four] cascade save took %.2f ms%n", elapsedMilliseconds);
        System.out.println("[chain of four] fourth " + fourth.getURI());
        System.out.println("[chain of four] third " + third.getURI());
        System.out.println("[chain of four] second " + second.getURI());
        System.out.println("[chain of four] first " + first.getURI());
        assertTrue(storedThird.contains(fourth.getURI().toString()),
                "The third resource has to carry the new content identifier of the fourth resource");
        assertTrue(storedSecond.contains(third.getURI().toString()),
                "The second resource has to carry the new content identifier of the third resource");
        assertTrue(storedFirst.contains(second.getURI().toString()),
                "The first resource has to carry the new content identifier of the second resource");
    }
}
