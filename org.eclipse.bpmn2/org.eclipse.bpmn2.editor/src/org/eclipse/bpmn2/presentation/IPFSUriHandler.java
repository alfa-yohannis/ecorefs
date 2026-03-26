package org.eclipse.bpmn2.presentation;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.impl.URIHandlerImpl;

/**
 * EMF URI handler for ipfs:// and ipns:// schemes.
 * Resolves content via the local Kubo daemon HTTP API.
 *
 * The API base URL is read from (in priority order):
 *   1. System property  org.eclipse.bpmn2.ipfs.apiUrl
 *   2. Environment var  BPMN2_IPFS_API_URL
 *   3. Default          http://127.0.0.1:5001
 */
// EcoreFS begin: URI handler for ipfs:// and ipns:// in the BPMN2 generated editor
class IPFSUriHandler extends URIHandlerImpl {

	static final String API_URL_PROPERTY = "org.eclipse.bpmn2.ipfs.apiUrl"; //$NON-NLS-1$
	static final String API_URL_ENV      = "BPMN2_IPFS_API_URL"; //$NON-NLS-1$
	static final String DEFAULT_API_URL  = "http://127.0.0.1:5001"; //$NON-NLS-1$

	@Override
	public boolean canHandle(URI uri) {
		String scheme = uri.scheme();
		return "ipfs".equals(scheme) || "ipns".equals(scheme); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Override
	public InputStream createInputStream(URI uri, Map<?, ?> options) throws IOException {
		String cid;

		if ("ipns".equals(uri.scheme())) { //$NON-NLS-1$
			String ipnsKey = uri.authority();
			if (ipnsKey == null || ipnsKey.isEmpty()) {
				ipnsKey = uri.opaquePart();
			}
			cid = resolveIpns(ipnsKey);
		} else {
			cid = uri.authority();
			if (cid == null || cid.isEmpty()) {
				cid = uri.opaquePart();
			}
		}

		if (cid != null && cid.contains("#")) { //$NON-NLS-1$
			cid = cid.substring(0, cid.indexOf('#'));
		}

		return new ByteArrayInputStream(cat(cid));
	}

	@Override
	public OutputStream createOutputStream(URI uri, Map<?, ?> options) throws IOException {
		throw new UnsupportedOperationException(
				"ipfs:// and ipns:// URIs are read-only in the BPMN2 editor."); //$NON-NLS-1$
	}

	private String resolveIpns(String ipnsKey) throws IOException {
		String endpoint = apiBase() + "/api/v0/name/resolve?arg=" //$NON-NLS-1$
				+ URLEncoder.encode(ipnsKey, StandardCharsets.UTF_8);
		HttpURLConnection conn = post(endpoint);
		try (InputStream in = responseStream(conn)) {
			String body = new String(in.readAllBytes(), StandardCharsets.UTF_8);
			String marker = "\"Path\":\""; //$NON-NLS-1$
			int start = body.indexOf(marker);
			if (start < 0) {
				throw new IOException("Unexpected IPNS resolve response: " + body); //$NON-NLS-1$
			}
			start += marker.length();
			int end = body.indexOf('"', start);
			if (end < 0) {
				throw new IOException("Unexpected IPNS resolve response: " + body); //$NON-NLS-1$
			}
			return body.substring(start, end).replace("/ipfs/", ""); //$NON-NLS-1$ //$NON-NLS-2$
		} finally {
			conn.disconnect();
		}
	}

	private byte[] cat(String cid) throws IOException {
		String endpoint = apiBase() + "/api/v0/cat?arg=" //$NON-NLS-1$
				+ URLEncoder.encode(cid, StandardCharsets.UTF_8);
		HttpURLConnection conn = post(endpoint);
		try (InputStream in = responseStream(conn)) {
			return in.readAllBytes();
		} finally {
			conn.disconnect();
		}
	}

	private static HttpURLConnection post(String endpoint) throws IOException {
		HttpURLConnection conn = (HttpURLConnection) new URL(endpoint).openConnection();
		conn.setRequestMethod("POST"); //$NON-NLS-1$
		conn.setDoOutput(true);
		conn.setConnectTimeout(30_000);
		conn.setReadTimeout(60_000);
		conn.connect();
		return conn;
	}

	private static InputStream responseStream(HttpURLConnection conn) throws IOException {
		int status = conn.getResponseCode();
		if (status >= 200 && status < 300) {
			return conn.getInputStream();
		}
		InputStream err = conn.getErrorStream();
		String body = err != null
				? new String(err.readAllBytes(), StandardCharsets.UTF_8)
				: "(no error body)"; //$NON-NLS-1$
		throw new IOException("Kubo API returned HTTP " + status + ": " + body); //$NON-NLS-1$ //$NON-NLS-2$
	}

	private static String apiBase() {
		String url = System.getProperty(API_URL_PROPERTY);
		if (url == null || url.trim().isEmpty()) {
			url = System.getenv(API_URL_ENV);
		}
		if (url == null || url.trim().isEmpty()) {
			url = DEFAULT_API_URL;
		}
		url = url.trim();
		if (url.endsWith("/")) { //$NON-NLS-1$
			url = url.substring(0, url.length() - 1);
		}
		return url;
	}
}
// EcoreFS end: URI handler for ipfs:// and ipns:// in the BPMN2 generated editor
