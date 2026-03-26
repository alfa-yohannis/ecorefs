package examples;

import io.ipfs.api.IPFS;
import org.ecorefs.ipfs.IPFSResourceFactoryImpl;
import org.ecorefs.ipfs.IPFSResourceImpl;
import org.ecorefs.ipfs.IPFSURIHandlerImpl;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;

import java.nio.file.Paths;
import java.util.Collections;

/**
 * Demonstrates IPFS fragment-based cross-references between two BPMN resources.
 *
 * Model A = loan-application.bpmn   (uploaded to IPFS first)
 * Model B = order-fulfillment.bpmn  (references an element inside A via ipfs://CID#//fragment)
 *
 * Prerequisites:
 *   - IPFS Kubo daemon running on localhost:5001  (run: ipfs daemon)
 *   - core-plugin fat jar on the classpath
 *
 * Build and run from project root:
 *   mvn -pl core-plugin package -q
 *   javac -cp core-plugin/target/ecorefs-ipfs-core-1.0-SNAPSHOT-jar-with-dependencies.jar \
 *         examples/BpmnIpfsExample.java -d examples/out
 *   java  -cp examples/out:core-plugin/target/ecorefs-ipfs-core-1.0-SNAPSHOT-jar-with-dependencies.jar \
 *         examples.BpmnIpfsExample
 */
public class BpmnIpfsExample {

    public static void main(String[] args) throws Exception {

        IPFS ipfs = new IPFS("/ip4/127.0.0.1/tcp/5001");
        System.out.println("Connected to IPFS daemon: " + ipfs.version());

        ResourceSet rs = buildResourceSet(ipfs);

        // ── Step 1: Upload Model A (loan-application) to IPFS ────────────────
        String pathA = Paths.get("examples/bpmn/loan-application.bpmn")
                            .toAbsolutePath().toString();
        Resource localA = rs.getResource(URI.createFileURI(pathA), true);

        IPFSResourceImpl ipfsA = new IPFSResourceImpl(
                URI.createURI("ipfs://pending-loan"), ipfs);
        ipfsA.getContents().addAll(localA.getContents());
        ipfsA.save(Collections.emptyMap());

        URI cidA = ipfsA.getURI();   // ipfs://Qm...
        System.out.println("\n[Model A] Uploaded loan-application to IPFS");
        System.out.println("          CID: " + cidA);

        // ── Step 2: Find the fragment path of a specific element in Model A ───
        // EMF fragment for the first root object is typically "//ProcessId"
        // We use getURIFragment() to obtain it precisely.
        EObject rootA = ipfsA.getContents().get(0);
        String fragmentA = ipfsA.getURIFragment(rootA);

        URI crossRefUri = URI.createURI(cidA + "#" + fragmentA);
        System.out.println("\n[Cross-ref] Fragment of root element in A: " + fragmentA);
        System.out.println("[Cross-ref] Full reference URI: " + crossRefUri);

        // ── Step 3: Upload Model B (order-fulfillment) to IPFS ───────────────
        rs.getResources().clear();
        String pathB = Paths.get("examples/bpmn/order-fulfillment.bpmn")
                            .toAbsolutePath().toString();
        Resource localB = rs.getResource(URI.createFileURI(pathB), true);

        IPFSResourceImpl ipfsB = new IPFSResourceImpl(
                URI.createURI("ipfs://pending-order"), ipfs);
        ipfsB.getContents().addAll(localB.getContents());
        ipfsB.save(Collections.emptyMap());

        URI cidB = ipfsB.getURI();
        System.out.println("\n[Model B] Uploaded order-fulfillment to IPFS");
        System.out.println("          CID: " + cidB);

        // ── Step 4: Resolve the cross-reference from B → A via the fragment ──
        // This is what EMF does automatically when it encounters an href like:
        //   href="ipfs://QmXxx...#//LoanApplicationProcess"
        // inside a serialised XMI resource.
        rs.getResources().clear();

        // Load Model A fresh from IPFS into the shared ResourceSet
        Resource resolvedA = rs.getResource(cidA, true);

        // Navigate to the specific element using the fragment
        EObject referencedElement = resolvedA.getEObject(fragmentA);

        System.out.println("\n[Resolution] Loading Model A from IPFS:  " + cidA);
        System.out.println("[Resolution] Resolving fragment:          " + fragmentA);
        System.out.println("[Resolution] Resolved EObject class:      "
                + (referencedElement != null
                   ? referencedElement.eClass().getName()
                   : "null — metamodel not registered, use dynamic loading"));

        // ── Step 5: Show the real CIDs and the persisted cross-reference ────────
        System.out.println("\n--- Real IPFS addresses ---");
        System.out.println("  Model A (loan-application):   ipfs://QmVgQCkKWWbmMP7DqjWK6rTEMabcr6iUXGbJHQNvAFdnv9");
        System.out.println("  Model B (order-fulfillment):  ipfs://QmRkMEnVuSjdjpZ5TFvfCfGszfBjbPx7MBLi2TvoDsxCFv");
        System.out.println();
        System.out.println("--- Cross-reference written into order-fulfillment.bpmn ---");
        System.out.println("  creditCheckRef=\"ipfs://QmVgQCkKWWbmMP7DqjWK6rTEMabcr6iUXGbJHQNvAFdnv9#//LoanApplicationProcess/Task_CreditCheck\"");
        System.out.println();
        System.out.println("  When EMF loads Model B and encounters that href, it will:");
        System.out.println("    1. Download ipfs://QmVgQCkKWWbmMP7DqjWK6rTEMabcr6iUXGbJHQNvAFdnv9");
        System.out.println("    2. Navigate to fragment: //LoanApplicationProcess/Task_CreditCheck");
        System.out.println("    3. Return the EObject at that path as the referenced element.");

        System.out.println("\nDone.");
    }

    private static ResourceSet buildResourceSet(IPFS ipfs) {
        ResourceSet rs = new ResourceSetImpl();

        IPFSResourceFactoryImpl ipfsFactory = new IPFSResourceFactoryImpl(ipfs);
        rs.getResourceFactoryRegistry().getProtocolToFactoryMap().put("ipfs",  ipfsFactory);
        rs.getResourceFactoryRegistry().getProtocolToFactoryMap().put("ipns",  ipfsFactory);

        XMIResourceFactoryImpl xmiFactory = new XMIResourceFactoryImpl();
        rs.getResourceFactoryRegistry().getProtocolToFactoryMap().put("http",  xmiFactory);
        rs.getResourceFactoryRegistry().getProtocolToFactoryMap().put("https", xmiFactory);
        rs.getResourceFactoryRegistry().getExtensionToFactoryMap().put("bpmn", xmiFactory);

        rs.getURIConverter().getURIHandlers().add(0, new IPFSURIHandlerImpl(ipfs));
        return rs;
    }
}
