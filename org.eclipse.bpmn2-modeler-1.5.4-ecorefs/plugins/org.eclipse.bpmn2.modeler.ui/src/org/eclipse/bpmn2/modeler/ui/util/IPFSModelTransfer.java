package org.eclipse.bpmn2.modeler.ui.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Minimal Kubo HTTP client used by the BPMN2 Modeler UI actions.
 *
 * <p>
 * This intentionally keeps BPMN editing local and only moves model bytes
 * between the local file system and IPFS/IPNS on demand.
 * </p>
 */
// EcoreFS begin: UI-driven Kubo transfer helper for BPMN model import/export
public final class IPFSModelTransfer {

	public static final String API_URL_PROPERTY = "org.eclipse.bpmn2.modeler.ipfs.apiUrl"; //$NON-NLS-1$
	public static final String API_URL_ENV = "BPMN2_MODELER_IPFS_API_URL"; //$NON-NLS-1$
	public static final String DEFAULT_API_URL = "http://127.0.0.1:5001"; //$NON-NLS-1$

	private static final Pattern CIDV0_PATTERN = Pattern.compile("^Qm[1-9A-HJ-NP-Za-km-z]{44}$"); //$NON-NLS-1$
	private static final Pattern CIDV1_PATTERN = Pattern.compile("^b[a-z2-7]{20,}$"); //$NON-NLS-1$
	private static final Pattern IPNS_KEY_PATTERN = Pattern.compile("^k[0-9a-z]{20,}$"); //$NON-NLS-1$
	private static final Pattern HASH_PATTERN = Pattern.compile("\"Hash\"\\s*:\\s*\"([^\"]+)\""); //$NON-NLS-1$
	private static final Pattern NAME_PATTERN = Pattern.compile("\"Name\"\\s*:\\s*\"([^\"]+)\""); //$NON-NLS-1$
	private static final Pattern VALUE_PATTERN = Pattern.compile("\"Value\"\\s*:\\s*\"([^\"]+)\""); //$NON-NLS-1$

	private IPFSModelTransfer() {
	}

	public static PublicationResult publish(File modelFile) throws IOException {
		return publish(modelFile, null, null, null);
	}

	public static PublicationResult publish(File modelFile, String apiUrl) throws IOException {
		return publish(modelFile, apiUrl, null, null);
	}

	public static PublicationResult publish(File modelFile, String apiUrl, String publishMode, String ipnsKeyName)
			throws IOException {
		String normalizedApiBase = normalizeApiBase(apiUrl);
		String cid = add(modelFile, normalizedApiBase);
		String ipfsUri = "ipfs://" + cid; //$NON-NLS-1$
		String ipfsPath = "/ipfs/" + cid; //$NON-NLS-1$
		String gatewayUrl = createGatewayUrl(ipfsPath, normalizedApiBase);

		if (publishMode == null || publishMode.trim().isEmpty() || "cid".equalsIgnoreCase(publishMode)) { //$NON-NLS-1$
			return new PublicationResult(cid, ipfsUri, ipfsPath, gatewayUrl, null, null, null, null);
		}

		if (!"ipns".equalsIgnoreCase(publishMode)) { //$NON-NLS-1$
			throw new IllegalArgumentException("Unsupported publish mode: " + publishMode); //$NON-NLS-1$
		}
		if (ipnsKeyName == null || ipnsKeyName.trim().isEmpty()) {
			throw new IllegalArgumentException("An IPNS publish key name is required for IPNS mode."); //$NON-NLS-1$
		}

		PublishedName publishedName = publishName(cid, ipnsKeyName.trim(), normalizedApiBase);
		String ipnsUri = "ipns://" + publishedName.name; //$NON-NLS-1$
		String ipnsPath = "/ipns/" + publishedName.name; //$NON-NLS-1$
		String ipnsGatewayUrl = createGatewayUrl(ipnsPath, normalizedApiBase);
		return new PublicationResult(cid, ipfsUri, ipfsPath, gatewayUrl, publishedName.name, ipnsUri, ipnsPath,
				ipnsGatewayUrl);
	}

	public static File downloadToTempModel(String reference) throws IOException {
		return downloadToTempModel(reference, null);
	}

	public static File downloadToTempModel(String reference, String apiUrl) throws IOException {
		byte[] bytes = downloadBytes(reference, apiUrl);
		File tempFile = File.createTempFile(deriveTempPrefix(reference), ".bpmn"); //$NON-NLS-1$
		try (OutputStream output = new FileOutputStream(tempFile)) {
			output.write(bytes);
		}
		return tempFile;
	}

	public static byte[] downloadBytes(String reference) throws IOException {
		return downloadBytes(reference, null);
	}

	public static byte[] downloadBytes(String reference, String apiUrl) throws IOException {
		String normalizedReference = normalizeReference(reference);
		return read(normalizedReference, normalizeApiBase(apiUrl));
	}

	public static boolean isSupportedReference(String reference) {
		try {
			normalizeReference(reference);
			return true;
		} catch (IllegalArgumentException e) {
			return false;
		}
	}

	public static String normalizeReference(String reference) {
		if (reference == null) {
			throw new IllegalArgumentException("Reference must not be empty."); //$NON-NLS-1$
		}

		String trimmed = reference.trim();
		if (trimmed.isEmpty()) {
			throw new IllegalArgumentException("Reference must not be empty."); //$NON-NLS-1$
		}
		if (trimmed.startsWith("/ipfs/") || trimmed.startsWith("/ipns/")) { //$NON-NLS-1$ //$NON-NLS-2$
			return trimmed;
		}
		if (trimmed.startsWith("ipfs://")) { //$NON-NLS-1$
			return "/ipfs/" + trimmed.substring("ipfs://".length()); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (trimmed.startsWith("ipns://")) { //$NON-NLS-1$
			return "/ipns/" + trimmed.substring("ipns://".length()); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (CIDV0_PATTERN.matcher(trimmed).matches() || CIDV1_PATTERN.matcher(trimmed).matches()) {
			return "/ipfs/" + trimmed; //$NON-NLS-1$
		}
		if (IPNS_KEY_PATTERN.matcher(trimmed).matches()) {
			return "/ipns/" + trimmed; //$NON-NLS-1$
		}

		throw new IllegalArgumentException(
				"Enter a CID, /ipfs/... path, ipfs://... URI, or IPNS reference."); //$NON-NLS-1$
	}

	private static byte[] read(String reference, String apiUrl) throws IOException {
		HttpURLConnection connection = openApiConnection(apiUrl, "cat?arg=" + encode(reference)); //$NON-NLS-1$
		try (InputStream input = getResponseStream(connection)) {
			return readAllBytes(input);
		} finally {
			connection.disconnect();
		}
	}

	private static String add(File modelFile, String apiUrl) throws IOException {
		HttpURLConnection connection = openApiConnection(apiUrl, "add?pin=true&cid-version=1"); //$NON-NLS-1$
		String boundary = "---------------------------BPMN2Modeler" + System.currentTimeMillis(); //$NON-NLS-1$
		connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary); //$NON-NLS-1$ //$NON-NLS-2$
		connection.setDoOutput(true);
		connection.setChunkedStreamingMode(16 * 1024);

		try (OutputStream output = connection.getOutputStream()) {
			writeAscii(output, "--" + boundary + "\r\n"); //$NON-NLS-1$ //$NON-NLS-2$
			writeAscii(output,
					"Content-Disposition: form-data; name=\"file\"; filename=\"" + modelFile.getName() + "\"\r\n"); //$NON-NLS-1$ //$NON-NLS-2$
			writeAscii(output, "Content-Type: application/octet-stream\r\n\r\n"); //$NON-NLS-1$
			Files.copy(modelFile.toPath(), output);
			writeAscii(output, "\r\n--" + boundary + "--\r\n"); //$NON-NLS-1$ //$NON-NLS-2$
		}

		try (InputStream input = getResponseStream(connection)) {
			String response = new String(readAllBytes(input), StandardCharsets.UTF_8);
			Matcher matcher = HASH_PATTERN.matcher(response);
			String cid = null;
			while (matcher.find()) {
				cid = matcher.group(1);
			}
			if (cid == null || cid.isEmpty()) {
				throw new IOException("Could not extract a CID from the Kubo add response."); //$NON-NLS-1$
			}
			return cid;
		} finally {
			connection.disconnect();
		}
	}

	private static PublishedName publishName(String cid, String keyName, String apiUrl) throws IOException {
		String ipfsPath = "/ipfs/" + cid; //$NON-NLS-1$
		HttpURLConnection connection = openApiConnection(apiUrl, "name/publish?allow-offline=true&arg=" //$NON-NLS-1$
				+ encode(ipfsPath) + "&key=" + encode(keyName)); //$NON-NLS-1$
		try (InputStream input = getResponseStream(connection)) {
			String response = new String(readAllBytes(input), StandardCharsets.UTF_8);
			Matcher nameMatcher = NAME_PATTERN.matcher(response);
			Matcher valueMatcher = VALUE_PATTERN.matcher(response);
			String name = null;
			String value = null;
			while (nameMatcher.find()) {
				name = nameMatcher.group(1);
			}
			while (valueMatcher.find()) {
				value = valueMatcher.group(1);
			}
			if (name == null || name.isEmpty()) {
				throw new IOException("Could not extract an IPNS name from the Kubo publish response."); //$NON-NLS-1$
			}
			return new PublishedName(name, value);
		} finally {
			connection.disconnect();
		}
	}

	private static HttpURLConnection openApiConnection(String apiUrl, String commandAndQuery) throws IOException {
		URL url = new URL(apiUrl + "/api/v0/" + commandAndQuery); //$NON-NLS-1$
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod("POST"); //$NON-NLS-1$
		connection.setConnectTimeout(30_000);
		connection.setReadTimeout(60_000);
		connection.setDoInput(true);
		return connection;
	}

	private static InputStream getResponseStream(HttpURLConnection connection) throws IOException {
		int status = connection.getResponseCode();
		if (status >= 200 && status < 300) {
			return connection.getInputStream();
		}

		InputStream errorStream = connection.getErrorStream();
		if (errorStream == null) {
			throw new IOException("Kubo request failed with HTTP " + status + "."); //$NON-NLS-1$ //$NON-NLS-2$
		}

		String error = new String(readAllBytes(errorStream), StandardCharsets.UTF_8);
		throw new IOException("Kubo request failed with HTTP " + status + ": " + error); //$NON-NLS-1$ //$NON-NLS-2$
	}

	private static String normalizeApiBase(String configured) {
		if (configured == null || configured.trim().isEmpty()) {
			configured = System.getProperty(API_URL_PROPERTY);
		}
		if (configured == null || configured.trim().isEmpty()) {
			configured = System.getenv(API_URL_ENV);
		}
		if (configured == null || configured.trim().isEmpty()) {
			configured = DEFAULT_API_URL;
		}
		if (configured.endsWith("/")) { //$NON-NLS-1$
			return configured.substring(0, configured.length() - 1);
		}
		return configured.trim();
	}

	private static String createGatewayUrl(String path, String apiUrl) {
		String gatewayBase = apiUrl.replace(":5001", ":8080"); //$NON-NLS-1$ //$NON-NLS-2$
		return gatewayBase + path;
	}

	private static String deriveTempPrefix(String reference) {
		String prefix = reference == null ? "ipfs-model" : reference.trim(); //$NON-NLS-1$
		prefix = prefix.replaceAll("^[^a-zA-Z0-9]+", ""); //$NON-NLS-1$ //$NON-NLS-2$
		prefix = prefix.replaceAll("[^a-zA-Z0-9._-]", "_"); //$NON-NLS-1$ //$NON-NLS-2$
		if (prefix.length() < 3) {
			prefix = "ipfs-model"; //$NON-NLS-1$
		}
		if (prefix.length() > 48) {
			prefix = prefix.substring(0, 48);
		}
		return prefix;
	}

	private static byte[] readAllBytes(InputStream input) throws IOException {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		byte[] buffer = new byte[16 * 1024];
		int read = 0;
		while ((read = input.read(buffer)) != -1) {
			output.write(buffer, 0, read);
		}
		return output.toByteArray();
	}

	private static void writeAscii(OutputStream output, String text) throws IOException {
		output.write(text.getBytes(StandardCharsets.UTF_8));
	}

	private static String encode(String value) {
		return URLEncoder.encode(value, StandardCharsets.UTF_8);
	}

	public static final class PublicationResult {
		private final String cid;
		private final String ipfsUri;
		private final String ipfsPath;
		private final String gatewayUrl;
		private final String ipnsName;
		private final String ipnsUri;
		private final String ipnsPath;
		private final String ipnsGatewayUrl;

		public PublicationResult(String cid, String ipfsUri, String ipfsPath, String gatewayUrl, String ipnsName,
				String ipnsUri, String ipnsPath, String ipnsGatewayUrl) {
			this.cid = cid;
			this.ipfsUri = ipfsUri;
			this.ipfsPath = ipfsPath;
			this.gatewayUrl = gatewayUrl;
			this.ipnsName = ipnsName;
			this.ipnsUri = ipnsUri;
			this.ipnsPath = ipnsPath;
			this.ipnsGatewayUrl = ipnsGatewayUrl;
		}

		public String getCid() {
			return cid;
		}

		public String getIpfsUri() {
			return ipfsUri;
		}

		public String getIpfsPath() {
			return ipfsPath;
		}

		public String getGatewayUrl() {
			return gatewayUrl;
		}

		public boolean hasIpnsPublication() {
			return ipnsName != null && !ipnsName.isEmpty();
		}

		public String getIpnsName() {
			return ipnsName;
		}

		public String getIpnsUri() {
			return ipnsUri;
		}

		public String getIpnsPath() {
			return ipnsPath;
		}

		public String getIpnsGatewayUrl() {
			return ipnsGatewayUrl;
		}
	}

	private static final class PublishedName {
		private final String name;
		@SuppressWarnings("unused")
		private final String value;

		private PublishedName(String name, String value) {
			this.name = name;
			this.value = value;
		}
	}
}
// EcoreFS end: UI-driven Kubo transfer helper for BPMN model import/export
