package org.ecorefs.ipfs;

import io.ipfs.api.IPFS;
import io.ipfs.multihash.Multihash;
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
        String scheme = uri.scheme();
        return "ipfs".equals(scheme) || "ipns".equals(scheme);
    }

    @Override
    public InputStream createInputStream(URI uri, Map<?, ?> options) throws IOException {
        String cid;

        if ("ipns".equals(uri.scheme())) {
            // IPNS: resolve the mutable name to its current CID first
            String ipnsKey = uri.authority();
            if (ipnsKey == null) ipnsKey = uri.opaquePart();

            // Strip fragment if present
            if (ipnsKey.contains("#")) {
                ipnsKey = ipnsKey.substring(0, ipnsKey.indexOf('#'));
            }

            try {
                String resolved = ipfs.name.resolve(Multihash.fromBase58(ipnsKey));
                // Resolution returns "/ipfs/<CID>" — extract just the CID
                cid = resolved.replace("/ipfs/", "");
            } catch (Exception e) {
                throw new IOException("IPNS resolution failed for key '" + ipnsKey + "': " + e.getMessage(), e);
            }
        } else {
            // IPFS: direct CID access
            cid = uri.authority();
            if (cid == null) cid = uri.opaquePart();

            // Strip fragment if present
            if (cid.contains("#")) {
                cid = cid.substring(0, cid.indexOf('#'));
            }
        }

        byte[] content = ipfs.cat(Multihash.fromBase58(cid));
        return new ByteArrayInputStream(content);
    }

    @Override
    public OutputStream createOutputStream(URI uri, Map<?, ?> options) throws IOException {
        throw new UnsupportedOperationException(
                "IPFS is content-addressed. Output streams are handled natively via IPFSResourceImpl.save(), "
                        + "generating a new CID, rather than piping to an existing URI.");
    }
}
