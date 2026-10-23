package org.ecorefs.ipfs;

import io.ipfs.api.IPFS;
import io.ipfs.api.KeyInfo;
import io.ipfs.api.MerkleNode;
import io.ipfs.api.NamedStreamable;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceImpl;

import java.io.OutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An XMI resource that can also publish its content on IPFS.
 *
 * <p>{@link #save(Map)} is the normal EMF save: it writes the XMI to the location
 * of the resource URI, for example a local file. {@link #saveIPFS(Map)} publishes
 * the content on the IPFS node instead and gives the resource the URI of the
 * published version, {@code ipfs://<CID>} in CID mode or {@code ipns://<name>} in
 * IPNS mode. Content under a CID cannot change, so after a publication a normal
 * save needs a writable URI again, set with {@link #setURI(URI)}.
 * {@link #saveIPFSCascaded(Map)} publishes the resource together with every
 * resource that refers to it, in dependency order.
 */
public class IPFSResourceImpl extends XMIResourceImpl {
    private final IPFS ipfs;

    /**
     * Stores the IPNS key name once derived, so it remains stable across
     * multiple saves even after the URI is mutated to ipns://keyHash.
     */
    private String ipnsKeyName;

    /**
     * Creates a resource that publishes through the given IPFS client.
     *
     * @param uri  the URI of the resource: a local location for normal saves, or a
     *             placeholder such as {@code ipfs://pending} before the first publication
     * @param ipfs the client of the IPFS node
     */
    public IPFSResourceImpl(URI uri, IPFS ipfs) {
        super(uri);
        this.ipfs = ipfs;
    }

    // EcoreFS begin: save() is the normal EMF save, and saveIPFS() publishes to IPFS
    // @Override
    // public void save(Map<?, ?> options) throws IOException {
    /**
     * Publishes the content of the resource on the IPFS node. The XMI goes to a
     * temporary file and from there to the node, which returns the CID. In CID mode
     * the resource URI becomes {@code ipfs://<CID>}; in IPNS mode the CID is
     * published under the IPNS key of the resource, and the URI becomes
     * {@code ipns://<key>}. Resources that refer to this resource keep the old CID
     * until they are published again, for example by {@link #saveIPFSCascaded(Map)}.
     *
     * @param options the save options, including {@link IPFSResourceOptions#REFERENCE_MODE}
     *                and {@link IPFSResourceOptions#LOCATION_UPDATERS}, or null
     * @throws IOException when the serialization or the publication fails
     */
    public void saveIPFS(Map<?, ?> options) throws IOException {
    // EcoreFS end
        ReferenceMode mode = ReferenceMode.CID;
        if (options != null && options.containsKey(IPFSResourceOptions.REFERENCE_MODE)) {
            mode = (ReferenceMode) options.get(IPFSResourceOptions.REFERENCE_MODE);
        }
        // EcoreFS begin: location attributes take the current URIs of the resources they name
        updateLocationAttributes(options);
        // EcoreFS end

        String cid;
        Path tempFile = Files.createTempFile("ecorefs-ipfs-save-", ".xmi");
        try {
            // 1. Serialize standard EMF model XMI to a temporary file to avoid
            // buffering very large models entirely in heap memory.
            try (OutputStream output = Files.newOutputStream(tempFile)) {
                super.doSave(output, options);
            }

            // 2. Upload the serialized file natively to IPFS content-addressed storage
            NamedStreamable.FileWrapper fileWrapper = new NamedStreamable.FileWrapper(tempFile.toFile());
            MerkleNode addResult = ipfs.add(fileWrapper).get(0);
            cid = addResult.hash.toBase58();
        } finally {
            Files.deleteIfExists(tempFile);
        }

        if (mode == ReferenceMode.IPNS) {
            // 3a. IPNS mode: publish a mutable pointer to the new CID
            String keyName = getOrDeriveKeyName();
            try {
                ensureKeyExists(keyName);
                publishIpnsName(cid, keyName);

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

    // EcoreFS begin: saveIPFSCascaded() runs the cascade save of IPFSModelPersister
    /**
     * Publishes the resource on IPFS together with every resource of the resource
     * set that refers to it, directly or through other resources. The resources are
     * published in dependency order, so every stored reference names the current
     * CID of its target, unless the references form a cycle. In IPNS mode only this
     * resource is published, because references by IPNS name stay valid.
     *
     * @param options the save options, as for {@link #saveIPFS(Map)}
     * @throws IOException when one of the publications fails
     */
    public void saveIPFSCascaded(Map<?, ?> options) throws IOException {
        IPFSModelPersister.cascadeSave(this, options);
    }
    // EcoreFS end

    // EcoreFS begin: location attributes take the current URIs of the resources they name
    /**
     * Lets every updater of the save options write the current URIs of the named
     * resources into the location attributes of this resource, so the stored
     * locations name the same versions as the stored references.
     *
     * @param options the save options with the updaters, or null
     */
    private void updateLocationAttributes(Map<?, ?> options) {
        for (LocationAttributeUpdater updater : IPFSResourceOptions.locationUpdaters(options)) {
            updater.updateLocations(this);
        }
    }
    // EcoreFS end

    private void publishIpnsName(String cid, String keyName) throws IOException {
        String endpoint = ipfs.protocol + "://" + ipfs.host + ":" + ipfs.port
                + "/api/v0/name/publish?arg="
                + URLEncoder.encode("/ipfs/" + cid, StandardCharsets.UTF_8)
                + "&key=" + URLEncoder.encode(keyName, StandardCharsets.UTF_8)
                + "&allow-offline=true";
        HttpURLConnection connection = (HttpURLConnection) new URL(endpoint).openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setConnectTimeout(30_000);
        connection.setReadTimeout(120_000);
        connection.connect();

        try (InputStream input = connection.getInputStream()) {
            input.readAllBytes();
        } finally {
            connection.disconnect();
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
        String endpoint = ipfs.protocol + "://" + ipfs.host + ":" + ipfs.port + "/api/v0/key/list?l=true";
        HttpURLConnection connection = (HttpURLConnection) new URL(endpoint).openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.connect();

        try (InputStream input = connection.getInputStream()) {
            String body = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            String escapedName = Pattern.quote(keyName);
            Pattern nameFirst = Pattern.compile("\\{[^{}]*\"Name\":\"" + escapedName + "\"[^{}]*\"Id\":\"([^\"]+)\"[^{}]*\\}");
            Matcher matcher = nameFirst.matcher(body);
            boolean matched = matcher.find();
            if (!matched) {
                Pattern idFirst = Pattern.compile("\\{[^{}]*\"Id\":\"([^\"]+)\"[^{}]*\"Name\":\"" + escapedName + "\"[^{}]*\\}");
                matcher = idFirst.matcher(body);
                matched = matcher.find();
            }
            if (!matched) {
                throw new IOException("IPNS key '" + keyName + "' not found in key/list response");
            }
            return matcher.group(1);
        } finally {
            connection.disconnect();
        }
    }
}
