package org.ecorefs.eval.baselines;

import io.ipfs.api.IPFS;
import org.ecorefs.ipfs.IPFSResourceFactoryImpl;
import org.ecorefs.ipfs.IPFSResourceImpl;
import org.ecorefs.ipfs.IPFSURIHandlerImpl;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;

import java.util.Collection;
import java.util.Collections;

/**
 * The IPFS path of the paper: a save through {@code IPFSResourceImpl} to a Kubo
 * node and a load by CID in a fresh resource set.
 */
public final class IpfsStore implements ModelStore {

    /** Multiaddress of the Kubo API, for example {@code /ip4/127.0.0.1/tcp/5103}. */
    private final String apiAddress;

    /** Client of the Kubo API, created when the store starts. */
    private IPFS ipfs;

    /**
     * Creates the store.
     *
     * @param apiAddress multiaddress of the Kubo API
     */
    public IpfsStore(String apiAddress) {
        this.apiAddress = apiAddress;
    }

    /**
     * Returns the name of the store.
     *
     * @return {@code ipfs}
     */
    @Override
    public String name() {
        return "ipfs";
    }

    /**
     * Connects to the Kubo node.
     *
     * @throws Exception when the node does not answer
     */
    @Override
    public void start() throws Exception {
        ipfs = new IPFS(apiAddress);
        ipfs.version();
    }

    /**
     * Needs no preparation per model size.
     *
     * @param elementCount number of elements of the models that follow
     */
    @Override
    public void prepareSize(int elementCount) {
        // Every save receives its own CID.
    }

    /**
     * Saves the copy to the node, loads the copy by CID in a fresh resource set
     * and visits the loaded model.
     *
     * @param content   the root objects of the copy
     * @param iteration number of the iteration
     * @param times     receives the phases save, load, visit, write and read
     * @throws Exception when the save or the load fails
     */
    @Override
    public void measure(Collection<EObject> content, int iteration, PhaseTimes times) throws Exception {
        IPFSResourceImpl target = (IPFSResourceImpl) createResourceSet().createResource(URI.createURI("ipfs://pending"));
        target.getContents().addAll(content);

        long start = System.nanoTime();
        target.saveIPFS(Collections.emptyMap());
        times.recordSince("save", start);
        times.recordSince("write", start);

        start = System.nanoTime();
        Resource loaded = createResourceSet().createResource(target.getURI());
        loaded.load(Collections.emptyMap());
        loaded.getContents();
        times.recordSince("load", start);
        times.recordSince("read", start);

        start = System.nanoTime();
        VisitSummary visit = ModelTraversal.visitAll(loaded);
        times.recordSince("visit", start);
        times.recordVisit(visit);
    }

    /**
     * Builds a resource set with the IPFS factory and the IPFS URI handler.
     *
     * @return the configured resource set
     */
    private ResourceSet createResourceSet() {
        ResourceSet resourceSet = new ResourceSetImpl();
        resourceSet.getResourceFactoryRegistry().getProtocolToFactoryMap().put("ipfs", new IPFSResourceFactoryImpl(ipfs));
        resourceSet.getURIConverter().getURIHandlers().add(0, new IPFSURIHandlerImpl(ipfs));
        return resourceSet;
    }

    /**
     * Needs no cleanup per model size.
     */
    @Override
    public void finishSize() {
        // Content stays in the node until the container is removed.
    }

    /**
     * Needs no shutdown; the run script removes the container.
     */
    @Override
    public void stop() {
        // The client keeps no open connection.
    }
}
