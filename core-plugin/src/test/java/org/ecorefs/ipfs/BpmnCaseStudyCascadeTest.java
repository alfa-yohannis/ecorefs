package org.ecorefs.ipfs;

import io.ipfs.api.IPFS;
import org.eclipse.bpmn2.Bpmn2Package;
import org.eclipse.bpmn2.Collaboration;
import org.eclipse.bpmn2.Definitions;
import org.eclipse.bpmn2.DocumentRoot;
import org.eclipse.bpmn2.Import;
import org.eclipse.bpmn2.Participant;
import org.eclipse.bpmn2.RootElement;
import org.eclipse.bpmn2.util.Bpmn2ResourceFactoryImpl;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Runs the BPMN case study of the paper against the local IPFS node and reports
 * the content identifiers before and after a cascade save.
 *
 * The case study has three resources. The payment process stands alone, the
 * order process imports the payment process, and the collaboration refers to
 * both processes through participants and message flows. The files name each
 * other through BPMN imports. A change to the payment process therefore has to
 * give a new identifier to all three resources, and every reference and every
 * import location has to carry the new identifier of the target.
 */
public class BpmnCaseStudyCascadeTest {

    /** Multiaddress of the local Kubo node. */
    private static final String IPFS_ADDRESS = "/ip4/127.0.0.1/tcp/5001";

    /** Directory with the three models of the case study. */
    private static final String CASE_STUDY_DIRECTORY = "/data2/projects/ecorefs/examples/case-study/";

    /** Updater that writes the URIs of the imported models into the BPMN import locations. */
    private static final LocationAttributeUpdater BPMN_IMPORTS = new NamespaceImportLocations(
            Bpmn2Package.Literals.IMPORT__NAMESPACE,
            Bpmn2Package.Literals.IMPORT__LOCATION,
            Bpmn2Package.Literals.DEFINITIONS__TARGET_NAMESPACE);

    /** Save options of every save: CID mode, with import locations that follow the identifiers. */
    private static final Map<String, Object> SAVE_OPTIONS =
            Collections.singletonMap(IPFSResourceOptions.LOCATION_UPDATERS, Collections.singletonList(BPMN_IMPORTS));

    /** Client for the HTTP application programming interface of the local node. */
    private static IPFS ipfs;

    /**
     * Connects to the local IPFS node and loads the BPMN 2.0 metamodel, so the
     * references between the models become typed references of EMF.
     */
    @BeforeAll
    public static void setUp() {
        ipfs = new IPFS(IPFS_ADDRESS);
        Bpmn2Package.eINSTANCE.getName();
    }

    /**
     * Builds a resource set that reads BPMN files and writes resources on IPFS.
     *
     * @return the configured resource set
     */
    private ResourceSet createResourceSet() {
        ResourceSet resourceSet = new ResourceSetImpl();
        resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap()
                .put("bpmn", new Bpmn2ResourceFactoryImpl());
        IPFSResourceFactoryImpl factory = new IPFSResourceFactoryImpl(ipfs);
        resourceSet.getResourceFactoryRegistry().getProtocolToFactoryMap().put("ipfs", factory);
        resourceSet.getURIConverter().getURIHandlers().add(0, new IPFSURIHandlerImpl(ipfs));
        return resourceSet;
    }

    /**
     * Loads one model of the case study and resolves its references. The BPMN
     * loader keeps references into the diagram, such as the participant of a
     * pool shape, as proxies with the URI of the local file. Resolved now, they
     * move into the IPFS resource together with their targets, so the stored
     * model holds no local file path and its identifier does not depend on where
     * the repository lies.
     *
     * @param resourceSet the resource set of the load
     * @param fileName    file name inside the case study directory
     * @return the loaded resource
     */
    private Resource loadCaseStudyModel(ResourceSet resourceSet, String fileName) {
        Resource resource = resourceSet.getResource(URI.createFileURI(CASE_STUDY_DIRECTORY + fileName), true);
        EcoreUtil.resolveAll(resource);
        return resource;
    }

    /**
     * Returns the definitions element of a loaded BPMN resource.
     *
     * @param resource the loaded resource
     * @return the definitions element
     */
    private Definitions findDefinitions(Resource resource) {
        EObject root = resource.getContents().get(0);
        if (root instanceof DocumentRoot) {
            return ((DocumentRoot) root).getDefinitions();
        }
        return (Definitions) root;
    }

    /**
     * Returns the process with the given identifier.
     *
     * @param definitions the definitions element of one model
     * @param processId   identifier of the process
     * @return the process, or null when no process carries the identifier
     */
    private org.eclipse.bpmn2.Process findProcess(Definitions definitions, String processId) {
        for (RootElement element : definitions.getRootElements()) {
            if (element instanceof org.eclipse.bpmn2.Process && processId.equals(element.getId())) {
                return (org.eclipse.bpmn2.Process) element;
            }
        }
        return null;
    }

    /**
     * Returns the first collaboration of a model.
     *
     * @param definitions the definitions element of one model
     * @return the collaboration, or null when the model holds none
     */
    private Collaboration findCollaboration(Definitions definitions) {
        for (RootElement element : definitions.getRootElements()) {
            if (element instanceof Collaboration) {
                return (Collaboration) element;
            }
        }
        return null;
    }

    /**
     * Returns the participant with the given identifier.
     *
     * @param collaboration the collaboration of the case study
     * @param participantId identifier of the participant
     * @return the participant, or null when no participant carries the identifier
     */
    private Participant findParticipant(Collaboration collaboration, String participantId) {
        for (Participant participant : collaboration.getParticipants()) {
            if (participantId.equals(participant.getId())) {
                return participant;
            }
        }
        return null;
    }

    /**
     * Tells whether definitions import the model with a namespace.
     *
     * @param definitions the importing definitions
     * @param namespace   the target namespace of the imported model
     * @return true when one import of the definitions names the namespace
     */
    private boolean importsNamespace(Definitions definitions, String namespace) {
        for (Import anImport : definitions.getImports()) {
            if (namespace.equals(anImport.getNamespace())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Moves the content of a loaded file into a new IPFS resource.
     *
     * @param resourceSet    the resource set of the new resource
     * @param placeholderUri the URI before the first save
     * @param loaded         the resource loaded from the file
     * @return the new IPFS resource
     */
    private Resource createIpfsResource(ResourceSet resourceSet, String placeholderUri, Resource loaded) {
        Resource ipfsResource = resourceSet.createResource(URI.createURI(placeholderUri));
        ipfsResource.getContents().addAll(loaded.getContents());
        return ipfsResource;
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
     * Publishes the three models of the case study, changes the payment process
     * and checks the identifiers and the references after the cascade save.
     *
     * @throws Exception when a load, a save or a read fails
     */
    @Test
    public void cascadesTheThreeModelsOfTheCaseStudy() throws Exception {
        ResourceSet resourceSet = createResourceSet();
        Resource paymentFile = loadCaseStudyModel(resourceSet, "payment-process.bpmn");
        Resource orderFile = loadCaseStudyModel(resourceSet, "order-process.bpmn");
        Resource collaborationFile = loadCaseStudyModel(resourceSet, "collaboration.bpmn");

        org.eclipse.bpmn2.Process paymentProcess =
                findProcess(findDefinitions(paymentFile), "PaymentProcess");
        org.eclipse.bpmn2.Process orderProcess =
                findProcess(findDefinitions(orderFile), "OrderFulfillmentProcess");
        Collaboration collaboration = findCollaboration(findDefinitions(collaborationFile));
        assertNotNull(paymentProcess, "The payment process has to load");
        assertNotNull(orderProcess, "The order process has to load");
        assertNotNull(collaboration, "The collaboration has to load");

        // The files refer to each other through BPMN imports, so the loaded
        // references already lead to the processes of the other files.
        assertTrue(importsNamespace(findDefinitions(orderFile), findDefinitions(paymentFile).getTargetNamespace()),
                "The order process has to import payment-process.bpmn");
        assertSame(orderProcess, findParticipant(collaboration, "Participant_Seller").getProcessRef(),
                "The seller has to refer to the order process of order-process.bpmn");
        assertSame(paymentProcess, findParticipant(collaboration, "Participant_PaymentProvider").getProcessRef(),
                "The payment provider has to refer to the payment process of payment-process.bpmn");

        Resource payment = createIpfsResource(resourceSet, "ipfs://pending-payment", paymentFile);
        Resource order = createIpfsResource(resourceSet, "ipfs://pending-order", orderFile);
        Resource collaborationResource =
                createIpfsResource(resourceSet, "ipfs://pending-collaboration", collaborationFile);

        payment.save(SAVE_OPTIONS);
        order.save(SAVE_OPTIONS);
        collaborationResource.save(SAVE_OPTIONS);
        System.out.println("[case study] before the change");
        System.out.println("[case study]   payment-process.bpmn " + payment.getURI());
        System.out.println("[case study]   order-process.bpmn " + order.getURI());
        System.out.println("[case study]   collaboration.bpmn " + collaborationResource.getURI());

        paymentProcess.setName("Payment Process, second version");
        long start = System.nanoTime();
        IPFSModelPersister.cascadeSave(payment, SAVE_OPTIONS);
        double elapsedMilliseconds = (System.nanoTime() - start) / 1_000_000.0;

        System.out.printf("[case study] after the cascade save, %.2f ms%n", elapsedMilliseconds);
        System.out.println("[case study]   payment-process.bpmn " + payment.getURI());
        System.out.println("[case study]   order-process.bpmn " + order.getURI());
        System.out.println("[case study]   collaboration.bpmn " + collaborationResource.getURI());

        String storedOrder = readStoredContent(order);
        String storedCollaboration = readStoredContent(collaborationResource);
        assertTrue(storedCollaboration.contains(href(order)),
                "The references of the collaboration have to carry the new identifier of the order process");
        assertTrue(storedCollaboration.contains(href(payment)),
                "The references of the collaboration have to carry the new identifier of the payment process");
        assertTrue(storedOrder.contains(location(payment)),
                "The import of the order process has to name the new identifier of the payment process");
        assertTrue(storedCollaboration.contains(location(order)),
                "The imports of the collaboration have to name the new identifier of the order process");
        assertTrue(storedCollaboration.contains(location(payment)),
                "The imports of the collaboration have to name the new identifier of the payment process");
    }

    /**
     * Returns the start of a stored reference into a resource, as XMI writes it.
     *
     * @param target the referenced resource
     * @return the text {@code href="<uri>#}
     */
    private String href(Resource target) {
        return "href=\"" + target.getURI() + "#";
    }

    /**
     * Returns a stored import location that names a resource, as XMI writes it.
     *
     * @param target the imported resource
     * @return the text {@code location="<uri>"}
     */
    private String location(Resource target) {
        return "location=\"" + target.getURI() + "\"";
    }
}
