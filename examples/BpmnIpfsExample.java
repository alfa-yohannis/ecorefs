package examples;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.ecorefs.ipfs.IPFSResourceFactoryImpl;
import org.ecorefs.ipfs.IPFSResourceImpl;
import org.ecorefs.ipfs.IPFSURIHandlerImpl;
import org.eclipse.bpmn2.Bpmn2Factory;
import org.eclipse.bpmn2.Bpmn2Package;
import org.eclipse.bpmn2.Collaboration;
import org.eclipse.bpmn2.Definitions;
import org.eclipse.bpmn2.DocumentRoot;
import org.eclipse.bpmn2.Participant;
import org.eclipse.bpmn2.Process;
import org.eclipse.bpmn2.RootElement;
import org.eclipse.bpmn2.util.Bpmn2ResourceFactoryImpl;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;

import io.ipfs.api.IPFS;
import io.ipfs.multihash.Multihash;

/**
 * Shows the three saves of an IPFS resource on two BPMN models of
 * {@code examples/bpmn}. The loan model is saved as a local working copy with
 * {@code save}, then published with {@code saveIPFS}. The order model gains a
 * collaboration whose second participant runs the loan process, so the order
 * model refers to the loan model by CID. A change to the loan model followed by
 * {@code saveIPFSCascaded} publishes both models again, and a load of the order
 * model by CID in a fresh resource set resolves the reference through IPFS.
 *
 * <p>Run it from the root of the repository with a Kubo node on port 5001:
 * {@code bash examples/run-bpmn-ipfs-example.sh}, or pass another API address
 * as the first argument.
 */
public final class BpmnIpfsExample {

    private static final String LOAN_MODEL = "examples/bpmn/loan-application.bpmn";
    private static final String ORDER_MODEL = "examples/bpmn/order-fulfillment.bpmn";
    private static final String DEFAULT_NODE = "/ip4/127.0.0.1/tcp/5001";

    /**
     * Prevents instances; the class is a program.
     */
    private BpmnIpfsExample() {
    }

    /**
     * Runs the example.
     *
     * @param arguments the API address of the Kubo node, optional
     * @throws Exception when a load, a save or a publication fails
     */
    public static void main(String[] arguments) throws Exception {
        IPFS ipfs = new IPFS(arguments.length > 0 ? arguments[0] : DEFAULT_NODE);
        Bpmn2Package.eINSTANCE.getName();
        Path outputFolder = Files.createDirectories(Path.of("examples/out"));
        ResourceSet resourceSet = createResourceSet(ipfs);

        URI workingCopy = URI.createFileURI(outputFolder.resolve("loan-application.xmi").toAbsolutePath().toString());
        IPFSResourceImpl loan = copyIntoIpfsResource(resourceSet, LOAN_MODEL, workingCopy, ipfs);
        loan.save(null);
        System.out.println("save()              wrote the loan model to " + loan.getURI());
        loan.saveIPFS(null);
        System.out.println("saveIPFS()          published the loan model as " + loan.getURI());

        IPFSResourceImpl order = copyIntoIpfsResource(resourceSet, ORDER_MODEL, URI.createURI("ipfs://pending-order"),
                ipfs);
        Participant loanParticipant = addCollaboration(order, firstProcess(loan));
        order.saveIPFS(null);
        System.out.println("saveIPFS()          published the order model as " + order.getURI());
        System.out.println("                    " + describeStoredReference(ipfs, order, loan));

        firstProcess(loan).setName("Loan Application Process, second version");
        loan.saveIPFSCascaded(null);
        System.out.println("saveIPFSCascaded()  published the changed loan model as " + loan.getURI());
        System.out.println("                    and the order model again as " + order.getURI());
        System.out.println("                    " + describeStoredReference(ipfs, order, loan));

        Resource loadedOrder = createResourceSet(ipfs).getResource(order.getURI(), true);
        Participant loaded = (Participant) loadedOrder.getEObject(loanParticipant.getId());
        Process calledProcess = loaded.getProcessRef();
        System.out.println("load by CID         the participant \"" + loaded.getName() + "\" runs \""
                + calledProcess.getName() + "\" from " + calledProcess.eResource().getURI());
    }

    /**
     * Creates a resource set that reads BPMN files and IPFS resources. The IPFS
     * URI handler goes first, so the handler receives every {@code ipfs} and
     * {@code ipns} URI.
     *
     * @param ipfs the client of the node
     * @return the resource set
     */
    private static ResourceSet createResourceSet(IPFS ipfs) {
        ResourceSet resourceSet = new ResourceSetImpl();
        IPFSResourceFactoryImpl ipfsFactory = new IPFSResourceFactoryImpl(ipfs);
        resourceSet.getResourceFactoryRegistry().getProtocolToFactoryMap().put("ipfs", ipfsFactory);
        resourceSet.getResourceFactoryRegistry().getProtocolToFactoryMap().put("ipns", ipfsFactory);
        resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put("bpmn", new Bpmn2ResourceFactoryImpl());
        resourceSet.getURIConverter().getURIHandlers().add(0, new IPFSURIHandlerImpl(ipfs));
        return resourceSet;
    }

    /**
     * Loads a BPMN file and moves its content into a new IPFS resource of the same
     * resource set. The references inside the file are resolved first, so the
     * moved content refers to its own objects and not to the file.
     *
     * @param resourceSet the resource set of both resources
     * @param bpmnFile    the path of the BPMN file
     * @param uri         the URI of the new resource: a local working copy or a placeholder
     * @param ipfs        the client of the node
     * @return the IPFS resource with the content of the file
     */
    private static IPFSResourceImpl copyIntoIpfsResource(ResourceSet resourceSet, String bpmnFile, URI uri,
            IPFS ipfs) {
        Resource file = resourceSet.getResource(URI.createFileURI(Path.of(bpmnFile).toAbsolutePath().toString()),
                true);
        EcoreUtil.resolveAll(file);
        IPFSResourceImpl resource = new IPFSResourceImpl(uri, ipfs);
        resourceSet.getResources().add(resource);
        resource.getContents().addAll(file.getContents());
        return resource;
    }

    /**
     * Adds a collaboration to the order model with two participants: one for the
     * order process and one for the loan process of the other resource. The second
     * participant is the reference across the two resources.
     *
     * @param order       the resource of the order model
     * @param loanProcess the process of the loan model
     * @return the participant that refers to the loan process
     */
    private static Participant addCollaboration(Resource order, Process loanProcess) {
        Collaboration collaboration = Bpmn2Factory.eINSTANCE.createCollaboration();
        collaboration.setId("Collaboration_OrderAndLoan");
        collaboration.getParticipants().add(createParticipant("Participant_Shop", "Shop", firstProcess(order)));
        Participant bank = createParticipant("Participant_Bank", "Bank", loanProcess);
        collaboration.getParticipants().add(bank);
        definitionsOf(order).getRootElements().add(collaboration);
        return bank;
    }

    /**
     * Creates a participant that runs a process.
     *
     * @param id      the identifier of the participant
     * @param name    the name of the participant
     * @param process the process of the participant
     * @return the participant
     */
    private static Participant createParticipant(String id, String name, Process process) {
        Participant participant = Bpmn2Factory.eINSTANCE.createParticipant();
        participant.setId(id);
        participant.setName(name);
        participant.setProcessRef(process);
        return participant;
    }

    /**
     * Reads the stored bytes of a resource back from the node and tells whether
     * they name the current CID of a target resource.
     *
     * @param ipfs   the client of the node
     * @param holder the published resource that holds the reference
     * @param target the published resource that the reference names
     * @return a sentence for the console
     * @throws Exception when the node does not return the content
     */
    private static String describeStoredReference(IPFS ipfs, Resource holder, Resource target) throws Exception {
        byte[] stored = ipfs.cat(Multihash.fromBase58(holder.getURI().authority()));
        String text = new String(stored, StandardCharsets.UTF_8);
        String targetUri = target.getURI().toString();
        return text.contains(targetUri) ? "the stored order model names the current " + targetUri
                : "the stored order model names an older version of the loan model";
    }

    /**
     * Returns the first process of a BPMN resource.
     *
     * @param resource the resource
     * @return the process
     */
    private static Process firstProcess(Resource resource) {
        for (RootElement element : definitionsOf(resource).getRootElements()) {
            if (element instanceof Process) {
                return (Process) element;
            }
        }
        throw new IllegalStateException("No process in " + resource.getURI());
    }

    /**
     * Returns the definitions element of a BPMN resource.
     *
     * @param resource the resource
     * @return the definitions
     */
    private static Definitions definitionsOf(Resource resource) {
        EObject root = resource.getContents().get(0);
        return root instanceof DocumentRoot ? ((DocumentRoot) root).getDefinitions() : (Definitions) root;
    }
}
