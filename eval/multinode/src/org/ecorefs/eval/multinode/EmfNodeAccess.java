package org.ecorefs.eval.multinode;

import io.ipfs.api.IPFS;
import org.ecorefs.ipfs.IPFSResourceFactoryImpl;
import org.ecorefs.ipfs.IPFSResourceImpl;
import org.ecorefs.ipfs.IPFSURIHandlerImpl;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

/**
 * Saves and loads EMF models through the persistence layer of ecorefs against
 * one node of the experiment network. A save uses IPFSResourceImpl as in the
 * paper; a load runs in a fresh resource set, so the load reads the content
 * through IPFSURIHandlerImpl and the node, never from memory.
 */
public final class EmfNodeAccess {

    /** Client of the Java IPFS library, bound to the API of one node. */
    private final IPFS ipfs;

    /**
     * Creates the access for one node.
     *
     * @param node the node that serves the saves and the loads
     */
    public EmfNodeAccess(KuboClient node) {
        this.ipfs = new IPFS(node.apiMultiaddress());
    }

    /**
     * Saves model content as a new IPFS resource.
     *
     * @param content the root objects of the model, which move into the resource
     * @return the URI of the saved resource, ipfs:// followed by the CID
     * @throws IOException when the save fails
     */
    public URI save(Collection<EObject> content) throws IOException {
        IPFSResourceImpl resource = (IPFSResourceImpl) createResourceSet().createResource(URI.createURI("ipfs://pending"));
        resource.getContents().addAll(content);
        resource.saveIPFS(Collections.emptyMap());
        return resource.getURI();
    }

    /**
     * Loads a resource by URI in a fresh resource set.
     *
     * @param uri the URI of the resource, ipfs:// followed by the CID
     * @return the number of root objects of the loaded resource
     * @throws IOException when the load fails
     */
    public int load(URI uri) throws IOException {
        Resource resource = createResourceSet().createResource(uri);
        resource.load(Collections.emptyMap());
        return resource.getContents().size();
    }

    /**
     * Builds a resource set that reads and writes resources on the node.
     *
     * @return the configured resource set
     */
    private ResourceSet createResourceSet() {
        ResourceSet resourceSet = new ResourceSetImpl();
        resourceSet.getResourceFactoryRegistry().getProtocolToFactoryMap()
                .put("ipfs", new IPFSResourceFactoryImpl(ipfs));
        resourceSet.getURIConverter().getURIHandlers().add(0, new IPFSURIHandlerImpl(ipfs));
        return resourceSet;
    }
}
