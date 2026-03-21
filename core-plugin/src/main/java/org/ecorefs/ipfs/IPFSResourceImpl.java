package org.ecorefs.ipfs;

import io.ipfs.api.IPFS;
import io.ipfs.api.MerkleNode;
import io.ipfs.api.NamedStreamable;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceImpl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

public class IPFSResourceImpl extends XMIResourceImpl {
    private final IPFS ipfs;

    public IPFSResourceImpl(URI uri, IPFS ipfs) {
        super(uri);
        this.ipfs = ipfs;
    }

    @Override
    public void save(Map<?, ?> options) throws IOException {
        // 1. Serialize standard EMF model XMI to in-memory byte array
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        super.doSave(baos, options);
        byte[] content = baos.toByteArray();

        // 2. Upload file natively to IPFS content-addressed storage
        NamedStreamable.ByteArrayWrapper fileWrapper = new NamedStreamable.ByteArrayWrapper("model.xmi", content);
        MerkleNode addResult = ipfs.add(fileWrapper).get(0);
        String cid = addResult.hash.toBase58();

        // 3. Immutability shift: Update the Resource's URI to reflect the new mathematical address
        this.setURI(URI.createURI("ipfs://" + cid));
    }
}
