package org.ecorefs.ipfs;

import io.ipfs.api.IPFS;
import io.ipfs.api.KeyInfo;
import io.ipfs.api.MerkleNode;
import io.ipfs.api.NamedStreamable;
import io.ipfs.multihash.Multihash;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceImpl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class IPFSResourceImpl extends XMIResourceImpl {
    private final IPFS ipfs;

    /**
     * Stores the IPNS key name once derived, so it remains stable across
     * multiple saves even after the URI is mutated to ipns://keyHash.
     */
    private String ipnsKeyName;

    public IPFSResourceImpl(URI uri, IPFS ipfs) {
        super(uri);
        this.ipfs = ipfs;
    }

    @Override
    public void save(Map<?, ?> options) throws IOException {
        ReferenceMode mode = ReferenceMode.CID;
        if (options != null && options.containsKey(IPFSResourceOptions.REFERENCE_MODE)) {
            mode = (ReferenceMode) options.get(IPFSResourceOptions.REFERENCE_MODE);
        }

        // 1. Serialize standard EMF model XMI to in-memory byte array
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        super.doSave(baos, options);
        byte[] content = baos.toByteArray();

        // 2. Upload file natively to IPFS content-addressed storage
        NamedStreamable.ByteArrayWrapper fileWrapper = new NamedStreamable.ByteArrayWrapper("model.xmi", content);
        MerkleNode addResult = ipfs.add(fileWrapper).get(0);
        String cid = addResult.hash.toBase58();

        if (mode == ReferenceMode.IPNS) {
            // 3a. IPNS mode: publish a mutable pointer to the new CID
            String keyName = getOrDeriveKeyName();
            try {
                ensureKeyExists(keyName);
                ipfs.name.publish(Multihash.fromBase58(cid), Optional.of(keyName));

                // Resolve the IPNS key hash to use as the stable URI
                String keyHash = getKeyHash(keyName);
                this.setURI(URI.createURI("ipns://" + keyHash));
            } catch (IOException e) {
                throw e;
            } catch (Exception e) {
                throw new IOException("IPNS publish failed for key '" + keyName + "': " + e.getMessage(), e);
            }
        } else {
            // 3b. CID mode (default): immutable content-addressed URI
            this.setURI(URI.createURI("ipfs://" + cid));
        }
    }

    /**
     * Returns the cached IPNS key name, or derives and caches it from the current URI.
     * This ensures the key name stays stable even after the URI is mutated to ipns://keyHash.
     */
    private String getOrDeriveKeyName() {
        if (ipnsKeyName == null) {
            URI uri = getURI();
            String name = uri.authority();
            if (name == null || name.isEmpty()) {
                name = uri.lastSegment();
            }
            if (name == null || name.isEmpty()) {
                name = uri.toString();
            }
            // Sanitize: IPNS key names must be simple strings
            ipnsKeyName = "ecorefs-" + name.replaceAll("[^a-zA-Z0-9_-]", "_");
        }
        return ipnsKeyName;
    }

    /**
     * Ensures an IPNS key with the given name exists. Creates one if it does not.
     */
    private void ensureKeyExists(String keyName) throws IOException {
        List<KeyInfo> keys = ipfs.key.list();
        for (KeyInfo key : keys) {
            if (keyName.equals(key.name)) {
                return; // Key already exists
            }
        }
        // Key does not exist — generate it (ed25519, default size)
        ipfs.key.gen(keyName, Optional.of("ed25519"), Optional.of("-1"));
    }

    /**
     * Retrieves the peer/key hash (Id) for a given IPNS key name.
     */
    private String getKeyHash(String keyName) throws IOException {
        List<KeyInfo> keys = ipfs.key.list();
        for (KeyInfo key : keys) {
            if (keyName.equals(key.name)) {
                return key.id.toBase58();
            }
        }
        throw new IOException("IPNS key '" + keyName + "' not found after generation");
    }
}
