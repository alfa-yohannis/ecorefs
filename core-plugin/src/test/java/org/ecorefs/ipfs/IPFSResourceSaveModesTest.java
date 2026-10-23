package org.ecorefs.ipfs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Checks that {@code save} of an IPFS resource is the normal EMF save, which
 * writes to the location of the resource URI, while publishing on IPFS is the
 * separate {@code saveIPFS}. The tests need no IPFS node, because a normal save
 * never contacts the node.
 */
public class IPFSResourceSaveModesTest {

    /**
     * A resource with a file URI writes a plain XMI file and keeps the URI.
     *
     * @param folder a temporary folder of the test
     * @throws Exception when the save or the read fails
     */
    @Test
    public void saveWritesTheModelToTheFileOfTheUri(@TempDir Path folder) throws Exception {
        Path file = folder.resolve("model.xmi");
        URI fileUri = URI.createFileURI(file.toString());
        IPFSResourceImpl resource = new IPFSResourceImpl(fileUri, null);
        resource.getContents().add(namedClass("Order"));

        resource.save(null);

        String stored = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
        assertTrue(stored.contains("name=\"Order\""), "The file has to hold the saved model");
        assertEquals(fileUri, resource.getURI(), "A normal save keeps the URI of the resource");
    }

    /**
     * A normal save to an {@code ipfs} URI fails, because content under a CID cannot
     * be overwritten; the error names {@code saveIPFS} as the way to publish.
     */
    @Test
    public void saveToAnIpfsUriPointsToSaveIpfs() {
        ResourceSet resourceSet = new ResourceSetImpl();
        resourceSet.getURIConverter().getURIHandlers().add(0, new IPFSURIHandlerImpl(null));
        IPFSResourceImpl resource = new IPFSResourceImpl(URI.createURI("ipfs://pending"), null);
        resourceSet.getResources().add(resource);
        resource.getContents().add(namedClass("Payment"));

        UnsupportedOperationException failure =
                assertThrows(UnsupportedOperationException.class, () -> resource.save(null));
        assertTrue(failure.getMessage().contains("saveIPFS"), "The error has to name saveIPFS");
    }

    /**
     * Creates a class with a name, as simple content for a resource.
     *
     * @param name the name of the class
     * @return the class
     */
    private static EClass namedClass(String name) {
        EClass eClass = EcoreFactory.eINSTANCE.createEClass();
        eClass.setName(name);
        return eClass;
    }
}
