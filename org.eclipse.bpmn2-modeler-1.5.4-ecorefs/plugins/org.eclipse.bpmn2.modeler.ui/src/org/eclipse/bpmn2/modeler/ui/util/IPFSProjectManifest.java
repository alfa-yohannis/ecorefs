package org.eclipse.bpmn2.modeler.ui.util;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// EcoreFS begin: lightweight JSON manifest for publishing and restoring multi-file BPMN projects via IPFS
public final class IPFSProjectManifest {

	private static final String KIND = "bpmn-project-manifest"; //$NON-NLS-1$
	private static final Pattern KIND_PATTERN = Pattern.compile("\"kind\"\\s*:\\s*\"([^\"]+)\""); //$NON-NLS-1$
	private static final Pattern ROOT_MODEL_PATTERN = Pattern.compile("\"rootModel\"\\s*:\\s*\"([^\"]+)\""); //$NON-NLS-1$
	private static final Pattern PROJECT_NAME_PATTERN = Pattern.compile("\"projectName\"\\s*:\\s*\"([^\"]+)\""); //$NON-NLS-1$
	private static final Pattern MODEL_PATTERN = Pattern.compile(
			"\\{\\s*\"path\"\\s*:\\s*\"([^\"]+)\"\\s*,\\s*\"cid\"\\s*:\\s*\"([^\"]+)\"\\s*\\}"); //$NON-NLS-1$

	private final String projectName;
	private final String rootModel;
	private final List<ModelEntry> models;

	public IPFSProjectManifest(String projectName, String rootModel, List<ModelEntry> models) {
		if (rootModel == null || rootModel.trim().isEmpty()) {
			throw new IllegalArgumentException("A root model path is required."); //$NON-NLS-1$
		}
		if (models == null || models.isEmpty()) {
			throw new IllegalArgumentException("At least one BPMN model entry is required."); //$NON-NLS-1$
		}
		this.projectName = projectName == null ? "" : projectName.trim(); //$NON-NLS-1$
		this.rootModel = rootModel;
		this.models = Collections.unmodifiableList(new ArrayList<ModelEntry>(models));
	}

	public String getProjectName() {
		return projectName;
	}

	public String getRootModel() {
		return rootModel;
	}

	public List<ModelEntry> getModels() {
		return models;
	}

	public String toJson() {
		StringBuilder json = new StringBuilder();
		json.append("{\n"); //$NON-NLS-1$
		json.append("  \"kind\": \"").append(KIND).append("\",\n"); //$NON-NLS-1$ //$NON-NLS-2$
		json.append("  \"version\": 1,\n"); //$NON-NLS-1$
		json.append("  \"projectName\": \"").append(escape(projectName)).append("\",\n"); //$NON-NLS-1$ //$NON-NLS-2$
		json.append("  \"createdAt\": \"").append(Instant.now().toString()).append("\",\n"); //$NON-NLS-1$ //$NON-NLS-2$
		json.append("  \"rootModel\": \"").append(escape(rootModel)).append("\",\n"); //$NON-NLS-1$ //$NON-NLS-2$
		json.append("  \"models\": [\n"); //$NON-NLS-1$
		for (int i = 0; i < models.size(); i++) {
			ModelEntry entry = models.get(i);
			json.append("    { \"path\": \"").append(escape(entry.getPath())).append("\", \"cid\": \"") //$NON-NLS-1$ //$NON-NLS-2$
					.append(escape(entry.getCid())).append("\" }"); //$NON-NLS-1$
			if (i + 1 < models.size()) {
				json.append(',');
			}
			json.append('\n');
		}
		json.append("  ]\n"); //$NON-NLS-1$
		json.append("}\n"); //$NON-NLS-1$
		return json.toString();
	}

	public static IPFSProjectManifest fromJson(String json) {
		if (json == null || json.trim().isEmpty()) {
			throw new IllegalArgumentException("The BPMN project manifest is empty."); //$NON-NLS-1$
		}

		String kind = extractRequired(json, KIND_PATTERN, "kind"); //$NON-NLS-1$
		if (!KIND.equals(kind)) {
			throw new IllegalArgumentException("Unsupported BPMN project manifest kind: " + kind); //$NON-NLS-1$
		}

		String projectName = extractOptional(json, PROJECT_NAME_PATTERN);
		String rootModel = extractRequired(json, ROOT_MODEL_PATTERN, "rootModel"); //$NON-NLS-1$
		List<ModelEntry> models = new ArrayList<ModelEntry>();
		Matcher matcher = MODEL_PATTERN.matcher(json);
		while (matcher.find()) {
			models.add(new ModelEntry(unescape(matcher.group(1)), unescape(matcher.group(2))));
		}
		if (models.isEmpty()) {
			throw new IllegalArgumentException("The BPMN project manifest does not contain any models."); //$NON-NLS-1$
		}
		return new IPFSProjectManifest(unescape(projectName), unescape(rootModel), models);
	}

	private static String extractRequired(String json, Pattern pattern, String fieldName) {
		String value = extractOptional(json, pattern);
		if (value == null || value.trim().isEmpty()) {
			throw new IllegalArgumentException("Missing required BPMN project manifest field: " + fieldName); //$NON-NLS-1$
		}
		return value;
	}

	private static String extractOptional(String json, Pattern pattern) {
		Matcher matcher = pattern.matcher(json);
		if (matcher.find()) {
			return matcher.group(1);
		}
		return null;
	}

	private static String escape(String value) {
		if (value == null) {
			return ""; //$NON-NLS-1$
		}
		return value.replace("\\", "\\\\").replace("\"", "\\\""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
	}

	private static String unescape(String value) {
		if (value == null) {
			return ""; //$NON-NLS-1$
		}
		return value.replace("\\\"", "\"").replace("\\\\", "\\"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
	}

	public static final class ModelEntry {
		private final String path;
		private final String cid;

		public ModelEntry(String path, String cid) {
			if (path == null || path.trim().isEmpty()) {
				throw new IllegalArgumentException("A manifest model path is required."); //$NON-NLS-1$
			}
			if (cid == null || cid.trim().isEmpty()) {
				throw new IllegalArgumentException("A manifest model CID is required."); //$NON-NLS-1$
			}
			this.path = path;
			this.cid = cid;
		}

		public String getPath() {
			return path;
		}

		public String getCid() {
			return cid;
		}
	}
}
// EcoreFS end: lightweight JSON manifest for publishing and restoring multi-file BPMN projects via IPFS
