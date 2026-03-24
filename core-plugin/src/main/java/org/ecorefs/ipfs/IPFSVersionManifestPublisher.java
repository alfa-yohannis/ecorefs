package org.ecorefs.ipfs;

import io.ipfs.api.IPFS;
import io.ipfs.api.MerkleNode;
import io.ipfs.api.NamedStreamable;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public final class IPFSVersionManifestPublisher {

    private static final String SOURCE_VERSION_MANIFEST_FILENAME = "version-manifest.json";
    private static final String PUBLISHED_VERSION_MANIFEST_FILENAME = "project-version-manifest.json";
    private static final String PROJECT_HEAD_FILENAME = "project-head.json";

    private IPFSVersionManifestPublisher() {
    }

    public static void main(String[] args) throws Exception {
        Arguments parsed = Arguments.parse(args);
        IPFS ipfs = new IPFS(parsed.ipfsApi()).timeout(600_000);

        PublishResult result = publish(parsed.partitionRoot(), parsed.headKeyName(), parsed.outputDirectory(), ipfs);
        writeReport(parsed.reportPath(), result);

        System.out.println("partitionRoot=" + parsed.partitionRoot());
        System.out.println("projectManifestCid=" + result.projectManifestCid());
        System.out.println("projectHeadKey=" + result.projectHeadKey());
        System.out.println("projectHeadIpns=" + result.projectHeadIpns());
        System.out.println("resolvedProjectCid=" + result.resolvedProjectCid());
        System.out.println("componentCount=" + result.componentCount());
        System.out.println("sharedResourceCount=" + result.sharedResourceCount());
    }

    private static PublishResult publish(Path partitionRoot, String headKeyName, Path outputDirectory, IPFS ipfs) throws Exception {
        Path normalizedPartitionRoot = requireDirectory(partitionRoot, "partitionRoot");
        Path normalizedOutputDirectory = normalizeOutputDirectory(outputDirectory);

        Path componentSummaryCsv = requireFile(normalizedPartitionRoot.resolve("component-summary.csv"), "component-summary.csv");
        Path componentDependenciesCsv = requireFile(normalizedPartitionRoot.resolve("component-dependencies.csv"), "component-dependencies.csv");
        Path sourceVersionManifest = requireFile(normalizedPartitionRoot.resolve(SOURCE_VERSION_MANIFEST_FILENAME), SOURCE_VERSION_MANIFEST_FILENAME);
        Path rootModelFile = requireFile(normalizedPartitionRoot.resolve("all-repos-merged.root.xmi"), "all-repos-merged.root.xmi");

        String modelName = extractJsonString(sourceVersionManifest, "modelName");
        String rootModelRelativePath = extractJsonString(sourceVersionManifest, "rootModel");
        if (modelName == null || modelName.isEmpty()) {
            modelName = normalizedPartitionRoot.getFileName().toString();
        }
        if (rootModelRelativePath == null || rootModelRelativePath.isEmpty()) {
            rootModelRelativePath = "all-repos-merged.root.xmi";
        }

        Files.createDirectories(normalizedOutputDirectory);

        List<ComponentRecord> components = readComponentSummary(componentSummaryCsv);
        Map<String, List<DependencyRecord>> dependencies = readDependencies(componentDependenciesCsv);
        List<SharedResourceRecord> sharedResources = discoverSharedResources(normalizedPartitionRoot, rootModelRelativePath);

        log("publishing shared resources... count=" + sharedResources.size());
        for (SharedResourceRecord shared : sharedResources) {
            shared.cid = publishFile(ipfs, normalizedPartitionRoot.resolve(shared.relativePath));
            log("shared resource published: " + shared.relativePath + " -> " + shared.cid);
        }

        List<PublishedComponent> publishedComponents = new ArrayList<>();
        for (int index = 0; index < components.size(); index++) {
            ComponentRecord component = components.get(index);
            Path componentDirectory = normalizedPartitionRoot.resolve("compilation-units")
                    .resolve(component.repo)
                    .resolve(component.module);
            Path componentManifestPath = requireFile(
                    normalizedPartitionRoot.resolve(component.sourceManifestPath),
                    "component manifest for " + component.componentId);

            component.componentRootCid = publishDirectory(ipfs, componentDirectory);
            component.componentManifestCid = publishFile(ipfs, componentManifestPath);

            publishedComponents.add(new PublishedComponent(
                    component.componentId,
                    component.repo,
                    component.module,
                    component.resourceCount,
                    component.sourceManifestPath,
                    component.componentManifestCid,
                    component.componentRootCid,
                    dependencies.getOrDefault(component.componentId, Collections.emptyList())));

            log("published component: " + (index + 1) + "/" + components.size()
                    + " " + component.componentId
                    + " rootCid=" + component.componentRootCid
                    + " manifestCid=" + component.componentManifestCid);
        }

        publishedComponents.sort(Comparator.comparing(PublishedComponent::componentId));
        sharedResources.sort(Comparator.comparing(SharedResourceRecord::relativePath));

        Path projectVersionManifestPath = normalizedPartitionRoot.resolve(PUBLISHED_VERSION_MANIFEST_FILENAME);
        Files.write(projectVersionManifestPath,
                projectVersionManifestLines(modelName, rootModelRelativePath, sharedResources, publishedComponents),
                StandardCharsets.UTF_8);
        String projectManifestCid = publishFile(ipfs, projectVersionManifestPath);

        ensureKeyExists(ipfs, headKeyName);
        publishIpnsName(ipfs, projectManifestCid, headKeyName);
        String headKeyId = getKeyId(ipfs, headKeyName);
        String projectHeadIpns = "ipns://" + headKeyId;
        String resolvedProjectCid = resolveIpnsPath(ipfs, headKeyId).replace("/ipfs/", "");
        Path projectHeadPath = normalizedPartitionRoot.resolve(PROJECT_HEAD_FILENAME);
        Files.write(projectHeadPath,
                projectHeadLines(headKeyName, projectHeadIpns, projectManifestCid, PUBLISHED_VERSION_MANIFEST_FILENAME),
                StandardCharsets.UTF_8);

        copyToArchive(projectVersionManifestPath, normalizedOutputDirectory.resolve(PUBLISHED_VERSION_MANIFEST_FILENAME));
        copyToArchive(projectHeadPath, normalizedOutputDirectory.resolve(PROJECT_HEAD_FILENAME));

        log("project version manifest published: cid=" + projectManifestCid);
        log("project head published: key=" + headKeyName + ", ipns=" + projectHeadIpns + ", resolvedCid=" + resolvedProjectCid);

        return new PublishResult(
                normalizedPartitionRoot,
                normalizedOutputDirectory,
                projectVersionManifestPath,
                projectHeadPath,
                projectManifestCid,
                headKeyName,
                projectHeadIpns,
                resolvedProjectCid,
                publishedComponents,
                sharedResources);
    }

    private static List<ComponentRecord> readComponentSummary(Path summaryCsv) throws IOException {
        List<ComponentRecord> components = new ArrayList<>();
        List<String> lines = Files.readAllLines(summaryCsv, StandardCharsets.UTF_8);
        for (int index = 1; index < lines.size(); index++) {
            String line = lines.get(index).trim();
            if (line.isEmpty()) {
                continue;
            }
            List<String> columns = parseCsvRow(line);
            components.add(new ComponentRecord(
                    columns.get(0),
                    columns.get(1),
                    columns.get(2),
                    Integer.parseInt(columns.get(3)),
                    columns.get(8)));
        }
        components.sort(Comparator.comparing(component -> component.componentId));
        return components;
    }

    private static Map<String, List<DependencyRecord>> readDependencies(Path dependenciesCsv) throws IOException {
        Map<String, List<DependencyRecord>> dependencies = new LinkedHashMap<>();
        List<String> lines = Files.readAllLines(dependenciesCsv, StandardCharsets.UTF_8);
        for (int index = 1; index < lines.size(); index++) {
            String line = lines.get(index).trim();
            if (line.isEmpty()) {
                continue;
            }
            List<String> columns = parseCsvRow(line);
            DependencyRecord dependency = new DependencyRecord(columns.get(0), columns.get(1), columns.get(2),
                    Integer.parseInt(columns.get(3)));
            dependencies.computeIfAbsent(dependency.sourceComponentId, ignored -> new ArrayList<>()).add(dependency);
        }
        for (List<DependencyRecord> values : dependencies.values()) {
            values.sort(Comparator.comparing(DependencyRecord::targetKind).thenComparing(DependencyRecord::targetId));
        }
        return dependencies;
    }

    private static List<SharedResourceRecord> discoverSharedResources(Path partitionRoot, String rootModelRelativePath) throws IOException {
        List<SharedResourceRecord> sharedResources = new ArrayList<>();
        sharedResources.add(new SharedResourceRecord("root-model", rootModelRelativePath));

        Path auxDirectory = partitionRoot.resolve("aux");
        if (Files.isDirectory(auxDirectory)) {
            try (Stream<Path> paths = Files.list(auxDirectory)) {
                paths.filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().endsWith(".xmi"))
                        .sorted()
                        .forEach(path -> sharedResources.add(new SharedResourceRecord("aux",
                                partitionRoot.relativize(path.toAbsolutePath().normalize()).toString().replace('\\', '/'))));
            }
        }
        return sharedResources;
    }

    private static String publishFile(IPFS ipfs, Path file) throws IOException {
        MerkleNode node = ipfs.add(new NamedStreamable.FileWrapper(file.toFile())).get(0);
        return node.hash.toBase58();
    }

    private static String publishDirectory(IPFS ipfs, Path directory) throws IOException {
        NamedStreamable directoryStream = toNamedStreamable(directory);
        List<MerkleNode> nodes = ipfs.add(directoryStream);
        String directoryName = directory.getFileName().toString();
        String rootCid = null;
        for (MerkleNode node : nodes) {
            if (node.name.isPresent() && directoryName.equals(node.name.get())) {
                rootCid = node.hash.toBase58();
            }
        }
        if (rootCid == null && !nodes.isEmpty()) {
            rootCid = nodes.get(nodes.size() - 1).hash.toBase58();
        }
        if (rootCid == null) {
            throw new IOException("Directory publish did not return a root CID for " + directory);
        }
        return rootCid;
    }

    private static NamedStreamable toNamedStreamable(Path path) throws IOException {
        if (Files.isRegularFile(path)) {
            return new NamedStreamable.FileWrapper(path.toFile());
        }
        if (!Files.isDirectory(path)) {
            throw new IOException("Not a file or directory: " + path);
        }

        List<Path> children = new ArrayList<>();
        try (Stream<Path> paths = Files.list(path)) {
            paths.sorted().forEach(children::add);
        }

        List<NamedStreamable> namedChildren = new ArrayList<>(children.size());
        for (Path child : children) {
            namedChildren.add(toNamedStreamable(child));
        }
        return new NamedStreamable.DirWrapper(path.getFileName().toString(), namedChildren);
    }

    private static List<String> projectVersionManifestLines(
            String modelName,
            String rootModelRelativePath,
            List<SharedResourceRecord> sharedResources,
            List<PublishedComponent> components) {
        List<String> lines = new ArrayList<>();
        lines.add("{");
        lines.add("  \"schemaVersion\": 1,");
        lines.add("  \"manifestKind\": \"modisco-published-project-version-manifest\",");
        lines.add("  \"modelName\": \"" + json(modelName) + "\",");
        lines.add("  \"publicationStrategy\": \"component-root-cid plus single IPNS version head\",");
        lines.add("  \"rootModel\": { \"path\": \"" + json(rootModelRelativePath) + "\", \"cid\": \"" + json(sharedResources.get(0).cid) + "\" },");
        lines.add("  \"sharedResources\": [");
        for (int index = 0; index < sharedResources.size(); index++) {
            SharedResourceRecord shared = sharedResources.get(index);
            String suffix = index + 1 == sharedResources.size() ? "" : ",";
            lines.add("    { \"kind\": \"" + json(shared.kind)
                    + "\", \"path\": \"" + json(shared.relativePath)
                    + "\", \"cid\": \"" + json(shared.cid) + "\" }" + suffix);
        }
        lines.add("  ],");
        lines.add("  \"components\": [");
        for (int index = 0; index < components.size(); index++) {
            PublishedComponent component = components.get(index);
            String suffix = index + 1 == components.size() ? "" : ",";
            lines.add("    {");
            lines.add("      \"componentId\": \"" + json(component.componentId()) + "\",");
            lines.add("      \"repo\": \"" + json(component.repo()) + "\",");
            lines.add("      \"module\": \"" + json(component.module()) + "\",");
            lines.add("      \"resourceCount\": " + component.resourceCount() + ",");
            lines.add("      \"componentRootCid\": \"" + json(component.componentRootCid()) + "\",");
            lines.add("      \"componentManifestPath\": \"" + json(component.componentManifestPath()) + "\",");
            lines.add("      \"componentManifestCid\": \"" + json(component.componentManifestCid()) + "\",");
            lines.add("      \"dependencyCount\": " + component.dependencies().size());
            lines.add("    }" + suffix);
        }
        lines.add("  ]");
        lines.add("}");
        return lines;
    }

    private static List<String> projectHeadLines(
            String headKeyName,
            String projectHeadIpns,
            String projectManifestCid,
            String projectManifestPath) {
        List<String> lines = new ArrayList<>();
        lines.add("{");
        lines.add("  \"schemaVersion\": 1,");
        lines.add("  \"kind\": \"modisco-project-head\",");
        lines.add("  \"headName\": \"main\",");
        lines.add("  \"keyName\": \"" + json(headKeyName) + "\",");
        lines.add("  \"ipns\": \"" + json(projectHeadIpns) + "\",");
        lines.add("  \"currentManifestCid\": \"" + json(projectManifestCid) + "\",");
        lines.add("  \"currentManifestPath\": \"" + json(projectManifestPath) + "\",");
        lines.add("  \"updatedAtUtc\": \"" + json(Instant.now().toString()) + "\"");
        lines.add("}");
        return lines;
    }

    private static void writeReport(Path reportPath, PublishResult result) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add("# IPFS Version-Head Publication");
        lines.add("");
        lines.add("- Partition root: `" + result.partitionRoot() + "`");
        lines.add("- Output directory: `" + result.outputDirectory() + "`");
        lines.add("- Root-level published manifest: `" + result.projectVersionManifestPath() + "`");
        lines.add("- Root-level project head: `" + result.projectHeadPath() + "`");
        lines.add("- Project manifest CID: `" + result.projectManifestCid() + "`");
        lines.add("- Project head key: `" + result.projectHeadKey() + "`");
        lines.add("- Project head IPNS: `" + result.projectHeadIpns() + "`");
        lines.add("- Resolved project CID: `" + result.resolvedProjectCid() + "`");
        lines.add("- Components published: `" + result.componentCount() + "`");
        lines.add("- Shared resources published: `" + result.sharedResourceCount() + "`");
        lines.add("");
        lines.add("## Components");
        lines.add("");
        lines.add("| Component | Root CID | Manifest CID |");
        lines.add("| --- | --- | --- |");
        for (PublishedComponent component : result.components()) {
            lines.add("| `" + component.componentId() + "` | `" + component.componentRootCid()
                    + "` | `" + component.componentManifestCid() + "` |");
        }
        Files.write(reportPath, lines, StandardCharsets.UTF_8);
    }

    private static List<String> parseCsvRow(String line) {
        List<String> columns = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int index = 0; index < line.length(); index++) {
            char ch = line.charAt(index);
            if (ch == '"') {
                if (inQuotes && index + 1 < line.length() && line.charAt(index + 1) == '"') {
                    current.append('"');
                    index++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (ch == ',' && !inQuotes) {
                columns.add(current.toString());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }
        columns.add(current.toString());
        return columns;
    }

    private static String extractJsonString(Path file, String fieldName) throws IOException {
        String content = Files.readString(file, StandardCharsets.UTF_8);
        Pattern pattern = Pattern.compile("\"" + Pattern.quote(fieldName) + "\"\\s*:\\s*\"([^\"]*)\"");
        Matcher matcher = pattern.matcher(content);
        return matcher.find() ? matcher.group(1) : null;
    }

    private static void ensureKeyExists(IPFS ipfs, String keyName) throws IOException {
        String body = keyListJson(ipfs);
        if (body.contains("\"Name\":\"" + json(keyName) + "\"")) {
            return;
        }
        ipfs.key.gen(keyName, Optional.of("ed25519"), Optional.of("-1"));
    }

    private static String getKeyId(IPFS ipfs, String keyName) throws IOException {
        String body = keyListJson(ipfs);
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
    }

    private static void copyToArchive(Path source, Path target) throws IOException {
        Files.createDirectories(target.getParent());
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
    }

    private static String keyListJson(IPFS ipfs) throws IOException {
        return post(ipfs, "/api/v0/key/list?l=true");
    }

    private static void publishIpnsName(IPFS ipfs, String cid, String keyName) throws IOException {
        post(ipfs,
                "/api/v0/name/publish?arg="
                        + urlEncode("/ipfs/" + cid)
                        + "&key=" + urlEncode(keyName)
                        + "&allow-offline=true");
    }

    private static String resolveIpnsPath(IPFS ipfs, String ipnsKey) throws IOException {
        String body = post(ipfs, "/api/v0/name/resolve?arg=" + urlEncode(ipnsKey));
        Pattern pattern = Pattern.compile("\"Path\":\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(body);
        if (!matcher.find()) {
            throw new IOException("Unexpected IPNS resolve response: " + body);
        }
        return matcher.group(1);
    }

    private static String post(IPFS ipfs, String pathAndQuery) throws IOException {
        String endpoint = ipfs.protocol + "://" + ipfs.host + ":" + ipfs.port + pathAndQuery;
        HttpURLConnection connection = (HttpURLConnection) new URL(endpoint).openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setConnectTimeout(30_000);
        connection.setReadTimeout(0);
        connection.connect();

        try (InputStream input = connection.getInputStream()) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } finally {
            connection.disconnect();
        }
    }

    private static String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static Path requireDirectory(Path path, String name) {
        Objects.requireNonNull(path, name);
        Path normalized = path.toAbsolutePath().normalize();
        if (!Files.isDirectory(normalized)) {
            throw new IllegalArgumentException("Missing directory: " + normalized);
        }
        return normalized;
    }

    private static Path requireFile(Path path, String name) {
        Objects.requireNonNull(path, name);
        Path normalized = path.toAbsolutePath().normalize();
        if (!Files.isRegularFile(normalized)) {
            throw new IllegalArgumentException("Missing file: " + normalized);
        }
        return normalized;
    }

    private static Path normalizeOutputDirectory(Path outputDirectory) throws IOException {
        Objects.requireNonNull(outputDirectory, "outputDirectory");
        Path normalized = outputDirectory.toAbsolutePath().normalize();
        if (Files.exists(normalized)) {
            try (Stream<Path> paths = Files.list(normalized)) {
                if (paths.findAny().isPresent()) {
                    throw new IllegalArgumentException("Output directory must be empty or absent: " + normalized);
                }
            }
        }
        Files.createDirectories(normalized);
        return normalized;
    }

    private static String json(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder(value.length() + 8);
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            switch (current) {
                case '\\':
                    builder.append("\\\\");
                    break;
                case '"':
                    builder.append("\\\"");
                    break;
                case '\n':
                    builder.append("\\n");
                    break;
                case '\r':
                    builder.append("\\r");
                    break;
                case '\t':
                    builder.append("\\t");
                    break;
                default:
                    if (current < 0x20) {
                        builder.append(String.format(Locale.ROOT, "\\u%04x", (int) current));
                    } else {
                        builder.append(current);
                    }
            }
        }
        return builder.toString();
    }

    private static void log(String message) {
        System.out.println("[ipfs-version-publish] " + message);
        System.out.flush();
    }

    private static final class ComponentRecord {
        private final String componentId;
        private final String repo;
        private final String module;
        private final int resourceCount;
        private final String sourceManifestPath;
        private String componentRootCid;
        private String componentManifestCid;

        private ComponentRecord(String componentId, String repo, String module, int resourceCount, String sourceManifestPath) {
            this.componentId = componentId;
            this.repo = repo;
            this.module = module;
            this.resourceCount = resourceCount;
            this.sourceManifestPath = sourceManifestPath;
        }
    }

    private static final class DependencyRecord {
        private final String sourceComponentId;
        private final String targetKind;
        private final String targetId;
        private final int crossReferenceCount;

        private DependencyRecord(String sourceComponentId, String targetKind, String targetId, int crossReferenceCount) {
            this.sourceComponentId = sourceComponentId;
            this.targetKind = targetKind;
            this.targetId = targetId;
            this.crossReferenceCount = crossReferenceCount;
        }

        private String targetKind() {
            return targetKind;
        }

        private String targetId() {
            return targetId;
        }
    }

    private static final class SharedResourceRecord {
        private final String kind;
        private final String relativePath;
        private String cid;

        private SharedResourceRecord(String kind, String relativePath) {
            this.kind = kind;
            this.relativePath = relativePath;
        }

        private String relativePath() {
            return relativePath;
        }
    }

    private static final class PublishedComponent {
        private final String componentId;
        private final String repo;
        private final String module;
        private final int resourceCount;
        private final String componentManifestPath;
        private final String componentManifestCid;
        private final String componentRootCid;
        private final List<DependencyRecord> dependencies;

        private PublishedComponent(
                String componentId,
                String repo,
                String module,
                int resourceCount,
                String componentManifestPath,
                String componentManifestCid,
                String componentRootCid,
                List<DependencyRecord> dependencies) {
            this.componentId = componentId;
            this.repo = repo;
            this.module = module;
            this.resourceCount = resourceCount;
            this.componentManifestPath = componentManifestPath;
            this.componentManifestCid = componentManifestCid;
            this.componentRootCid = componentRootCid;
            this.dependencies = new ArrayList<>(dependencies);
        }

        private String componentId() {
            return componentId;
        }

        private String repo() {
            return repo;
        }

        private String module() {
            return module;
        }

        private int resourceCount() {
            return resourceCount;
        }

        private String componentManifestPath() {
            return componentManifestPath;
        }

        private String componentManifestCid() {
            return componentManifestCid;
        }

        private String componentRootCid() {
            return componentRootCid;
        }

        private List<DependencyRecord> dependencies() {
            return dependencies;
        }
    }

    private static final class PublishResult {
        private final Path partitionRoot;
        private final Path outputDirectory;
        private final Path projectVersionManifestPath;
        private final Path projectHeadPath;
        private final String projectManifestCid;
        private final String projectHeadKey;
        private final String projectHeadIpns;
        private final String resolvedProjectCid;
        private final List<PublishedComponent> components;
        private final List<SharedResourceRecord> sharedResources;

        private PublishResult(
                Path partitionRoot,
                Path outputDirectory,
                Path projectVersionManifestPath,
                Path projectHeadPath,
                String projectManifestCid,
                String projectHeadKey,
                String projectHeadIpns,
                String resolvedProjectCid,
                List<PublishedComponent> components,
                List<SharedResourceRecord> sharedResources) {
            this.partitionRoot = partitionRoot;
            this.outputDirectory = outputDirectory;
            this.projectVersionManifestPath = projectVersionManifestPath;
            this.projectHeadPath = projectHeadPath;
            this.projectManifestCid = projectManifestCid;
            this.projectHeadKey = projectHeadKey;
            this.projectHeadIpns = projectHeadIpns;
            this.resolvedProjectCid = resolvedProjectCid;
            this.components = new ArrayList<>(components);
            this.sharedResources = new ArrayList<>(sharedResources);
        }

        private Path partitionRoot() {
            return partitionRoot;
        }

        private Path outputDirectory() {
            return outputDirectory;
        }

        private Path projectVersionManifestPath() {
            return projectVersionManifestPath;
        }

        private Path projectHeadPath() {
            return projectHeadPath;
        }

        private String projectManifestCid() {
            return projectManifestCid;
        }

        private String projectHeadKey() {
            return projectHeadKey;
        }

        private String projectHeadIpns() {
            return projectHeadIpns;
        }

        private String resolvedProjectCid() {
            return resolvedProjectCid;
        }

        private List<PublishedComponent> components() {
            return components;
        }

        private int componentCount() {
            return components.size();
        }

        private int sharedResourceCount() {
            return sharedResources.size();
        }
    }

    private static final class Arguments {
        private final Path partitionRoot;
        private final String headKeyName;
        private final Path outputDirectory;
        private final Path reportPath;
        private final String ipfsApi;

        private Arguments(Path partitionRoot, String headKeyName, Path outputDirectory, Path reportPath, String ipfsApi) {
            this.partitionRoot = partitionRoot;
            this.headKeyName = headKeyName;
            this.outputDirectory = outputDirectory;
            this.reportPath = reportPath;
            this.ipfsApi = ipfsApi;
        }

        private static Arguments parse(String[] args) {
            if (args.length < 4 || args.length > 5) {
                throw new IllegalArgumentException(
                        "Usage: <partitionRoot> <headKeyName> <outputDirectory> <reportPath> [ipfsApi]");
            }
            String api = args.length == 5 ? args[4] : "/ip4/127.0.0.1/tcp/5001";
            return new Arguments(
                    Path.of(args[0]).toAbsolutePath().normalize(),
                    args[1],
                    Path.of(args[2]).toAbsolutePath().normalize(),
                    Path.of(args[3]).toAbsolutePath().normalize(),
                    api);
        }

        private Path partitionRoot() {
            return partitionRoot;
        }

        private String headKeyName() {
            return headKeyName;
        }

        private Path outputDirectory() {
            return outputDirectory;
        }

        private Path reportPath() {
            return reportPath;
        }

        private String ipfsApi() {
            return ipfsApi;
        }
    }
}
