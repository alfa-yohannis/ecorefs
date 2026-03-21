package org.ecorefs.ipfs;

import io.ipfs.api.IPFS;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.impl.URIHandlerImpl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

public class IPFSURIHandlerImpl extends URIHandlerImpl {
    private final IPFS ipfs;

    public IPFSURIHandlerImpl(IPFS ipfs) {
        this.ipfs = ipfs;
    }

    @Override
    public boolean canHandle(URI uri) {
        return "ipfs".equals(uri.scheme());
    }

    @Override
    public InputStream createInputStream(URI uri, Map<?, ?> options) throws IOException {
        String cid = uri.authority();
        if (cid == null) cid = uri.opaquePart();
        
        // Strip out the fragment if present (e.g. ipfs://Qm...#//Person)
        if (cid.contains("#")) {
            cid = cid.substring(0, cid.indexOf('#'));
        }

        byte[] content = ipfs.cat(io.ipfs.multihash.Multihash.fromBase58(cid));
        return new ByteArrayInputStream(content);
    }

    @Override
    public OutputStream createOutputStream(URI uri, Map<?, ?> options) throws IOException {
        throw new UnsupportedOperationException("IPFS is content-addressed. Output streams are handled natively via IPFSResourceImpl.save(), generating a new CID, rather than piping to an existing URI.");
    }
}
