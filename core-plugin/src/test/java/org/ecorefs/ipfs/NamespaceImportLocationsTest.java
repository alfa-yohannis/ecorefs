package org.ecorefs.ipfs;

import org.eclipse.bpmn2.Bpmn2Factory;
import org.eclipse.bpmn2.Bpmn2Package;
import org.eclipse.bpmn2.Definitions;
import org.eclipse.bpmn2.DocumentRoot;
import org.eclipse.bpmn2.Import;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceImpl;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Checks that BPMN import locations follow the URIs of the imported resources,
 * without an IPFS node. The resources carry the {@code ipfs://} and
 * {@code ipns://} URIs that a save to IPFS leaves on them.
 */
public class NamespaceImportLocationsTest {

    /** Import type of a BPMN 2.0 model. */
    private static final String BPMN_IMPORT_TYPE = "http://www.omg.org/spec/BPMN/20100524/MODEL";

    /** Namespace that the payment model declares. */
    private static final String PAYMENT_NAMESPACE = "http://ecorefs.org/test/payment";

    /** Namespace that the order model declares. */
    private static final String ORDER_NAMESPACE = "http://ecorefs.org/test/order";

    /** Namespace that the collaboration model declares. */
    private static final String COLLABORATION_NAMESPACE = "http://ecorefs.org/test/collaboration";

    /** The updater for BPMN 2.0 imports, as a user of the core plug-in configures it. */
    private final LocationAttributeUpdater bpmnImports = new NamespaceImportLocations(
            Bpmn2Package.Literals.IMPORT__NAMESPACE,
            Bpmn2Package.Literals.IMPORT__LOCATION,
            Bpmn2Package.Literals.DEFINITIONS__TARGET_NAMESPACE);

    /**
     * Adds a resource with one definitions element inside a document root, the
     * shape that a model read from BPMN XML has.
     *
     * @param resourceSet the resource set that receives the resource
     * @param uri         the URI of the resource
     * @param namespace   the target namespace of the definitions
     * @return the definitions element of the new resource
     */
    private Definitions addModel(ResourceSet resourceSet, String uri, String namespace) {
        Resource resource = new XMIResourceImpl(URI.createURI(uri));
        resourceSet.getResources().add(resource);
        DocumentRoot root = Bpmn2Factory.eINSTANCE.createDocumentRoot();
        Definitions definitions = Bpmn2Factory.eINSTANCE.createDefinitions();
        definitions.setTargetNamespace(namespace);
        root.setDefinitions(definitions);
        resource.getContents().add(root);
        return definitions;
    }

    /**
     * Adds an import of a BPMN model to definitions.
     *
     * @param definitions the importing definitions
     * @param namespace   the namespace of the imported model
     * @param location    the location as written in the file
     * @return the new import
     */
    private Import addImport(Definitions definitions, String namespace, String location) {
        Import anImport = Bpmn2Factory.eINSTANCE.createImport();
        anImport.setImportType(BPMN_IMPORT_TYPE);
        anImport.setNamespace(namespace);
        anImport.setLocation(location);
        definitions.getImports().add(anImport);
        return anImport;
    }

    /**
     * An import takes the URI of the resource that declares its namespace, both
     * for a CID and for an IPNS name.
     */
    @Test
    public void writesTheUriOfTheImportedResourceIntoTheLocation() {
        ResourceSet resourceSet = new ResourceSetImpl();
        addModel(resourceSet, "ipfs://QmPayment", PAYMENT_NAMESPACE);
        addModel(resourceSet, "ipns://k51Order", ORDER_NAMESPACE);
        Definitions collaboration = addModel(resourceSet, "ipfs://QmCollaboration", COLLABORATION_NAMESPACE);
        Import paymentImport = addImport(collaboration, PAYMENT_NAMESPACE, "payment-process.bpmn");
        Import orderImport = addImport(collaboration, ORDER_NAMESPACE, "order-process.bpmn");

        bpmnImports.updateLocations(collaboration.eResource());

        assertEquals("ipfs://QmPayment", paymentImport.getLocation());
        assertEquals("ipns://k51Order", orderImport.getLocation());
    }

    /**
     * An import keeps its location when no other resource declares its
     * namespace, or when two resources declare it.
     */
    @Test
    public void keepsTheLocationWhenTheImportedResourceIsMissingOrAmbiguous() {
        ResourceSet resourceSet = new ResourceSetImpl();
        addModel(resourceSet, "ipfs://QmOrderFirst", ORDER_NAMESPACE);
        addModel(resourceSet, "ipfs://QmOrderSecond", ORDER_NAMESPACE);
        Definitions collaboration = addModel(resourceSet, "ipfs://QmCollaboration", COLLABORATION_NAMESPACE);
        Import paymentImport = addImport(collaboration, PAYMENT_NAMESPACE, "payment-process.bpmn");
        Import orderImport = addImport(collaboration, ORDER_NAMESPACE, "order-process.bpmn");

        bpmnImports.updateLocations(collaboration.eResource());

        assertEquals("payment-process.bpmn", paymentImport.getLocation());
        assertEquals("order-process.bpmn", orderImport.getLocation());
    }

    /**
     * The updater reports the imported resources, which the dependency graph
     * uses as edges.
     */
    @Test
    public void reportsTheImportedResources() {
        ResourceSet resourceSet = new ResourceSetImpl();
        Definitions payment = addModel(resourceSet, "ipfs://QmPayment", PAYMENT_NAMESPACE);
        Definitions order = addModel(resourceSet, "ipfs://QmOrder", ORDER_NAMESPACE);
        Definitions collaboration = addModel(resourceSet, "ipfs://QmCollaboration", COLLABORATION_NAMESPACE);
        addImport(collaboration, ORDER_NAMESPACE, "order-process.bpmn");
        addImport(collaboration, PAYMENT_NAMESPACE, "payment-process.bpmn");

        assertEquals(List.of(order.eResource(), payment.eResource()),
                List.copyOf(bpmnImports.findNamedResources(collaboration.eResource())));
    }

    /**
     * A resource that imports another resource without any reference into it
     * still needs a new save when the imported resource changes, because its
     * import location names the old version.
     */
    @Test
    public void letsTheCascadeReachAResourceThatOnlyImports() {
        ResourceSet resourceSet = new ResourceSetImpl();
        Definitions payment = addModel(resourceSet, "ipfs://QmPayment", PAYMENT_NAMESPACE);
        Definitions order = addModel(resourceSet, "ipfs://QmOrder", ORDER_NAMESPACE);
        addImport(order, PAYMENT_NAMESPACE, "payment-process.bpmn");
        Resource paymentResource = payment.eResource();

        ResourceDependencyGraph withImports = ResourceDependencyGraph.of(resourceSet, List.of(bpmnImports));
        ResourceDependencyGraph withoutImports = ResourceDependencyGraph.of(resourceSet);

        assertEquals(List.of(paymentResource, order.eResource()), withImports.planSaveOrder(paymentResource));
        assertEquals(Collections.singletonList(paymentResource), withoutImports.planSaveOrder(paymentResource));
    }
}
