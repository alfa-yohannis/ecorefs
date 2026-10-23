package org.ecorefs.eval.bpmn;

import org.ecorefs.ipfs.IPFSURIHandlerImpl;
import org.eclipse.emf.common.util.URI;

import io.ipfs.api.IPFS;

/**
 * The URI handler of the core plugin, limited to {@code ipfs} and {@code ipns}
 * URIs. The core handler also reads {@code http} and {@code https} URIs, which the
 * experiment must not follow when a model from the dataset names a web address.
 */
public final class IpfsOnlyURIHandler extends IPFSURIHandlerImpl {

    /**
     * Creates the handler for the node of the experiment.
     *
     * @param ipfs the client of the Kubo node
     */
    public IpfsOnlyURIHandler(IPFS ipfs) {
        super(ipfs);
    }

    /**
     * Accepts only content-addressed and IPNS URIs.
     *
     * @param uri the URI to read
     * @return true for the {@code ipfs} and {@code ipns} schemes
     */
    @Override
    public boolean canHandle(URI uri) {
        return "ipfs".equals(uri.scheme()) || "ipns".equals(uri.scheme());
    }
}
