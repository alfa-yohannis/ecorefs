package org.ecorefs.ipfs;

import io.ipfs.api.IPFS;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.impl.URIHandlerImpl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
                String resolved = resolveIpnsPath(ipnsKey);
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

        byte[] content = catContent(cid);
        return new ByteArrayInputStream(content);
    }

    private String resolveIpnsPath(String ipnsKey) throws IOException {
        String endpoint = apiEndpoint("name/resolve", ipnsKey);
        HttpURLConnection connection = (HttpURLConnection) new URL(endpoint).openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.connect();

        try (InputStream input = connection.getInputStream()) {
            String body = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            String marker = "\"Path\":\"";
            int start = body.indexOf(marker);
            if (start < 0) {
                throw new IOException("Unexpected IPNS resolve response: " + body);
            }
            start += marker.length();
            int end = body.indexOf('"', start);
            if (end < 0) {
                throw new IOException("Unexpected IPNS resolve response: " + body);
            }
            return body.substring(start, end);
        } finally {
            connection.disconnect();
        }
    }

    private byte[] catContent(String cid) throws IOException {
        String endpoint = apiEndpoint("cat", cid);
        HttpURLConnection connection = (HttpURLConnection) new URL(endpoint).openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.connect();

        try (InputStream input = connection.getInputStream()) {
            return input.readAllBytes();
        } finally {
            connection.disconnect();
        }
    }

    private String apiEndpoint(String command, String argument) {
        return ipfs.protocol + "://" + ipfs.host + ":" + ipfs.port
                + "/api/v0/" + command + "?arg=" + URLEncoder.encode(argument, StandardCharsets.UTF_8);
    }

    @Override
    public OutputStream createOutputStream(URI uri, Map<?, ?> options) throws IOException {
        throw new UnsupportedOperationException(
                "IPFS is content-addressed. Output streams are handled natively via IPFSResourceImpl.save(), "
                        + "generating a new CID, rather than piping to an existing URI.");
    }
}
