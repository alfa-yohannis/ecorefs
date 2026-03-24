package org.ecorefs.ipfs;

import io.ipfs.api.IPFS;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

public final class IPFSModelPublishTool {

    private IPFSModelPublishTool() {
    }

    public static void main(String[] args) throws Exception {
        Arguments parsed = Arguments.parse(args);
        registerEPackage(parsed.epackageClassName());

        IPFS ipfs = new IPFS(parsed.ipfsApi());
        if (parsed.kind() == PublishKind.SINGLE) {
            PublishResult result = publishSingle(parsed.sourcePath(), parsed.referenceMode(), ipfs);
            writeSingleReport(parsed.reportPath(), parsed.sourcePath(), result);
            System.out.println("source=" + parsed.sourcePath());
            System.out.println("uri=" + result.uri());
            System.out.println("resolvedCid=" + result.resolvedCid());
            return;
        }

        PartitionAnalysis analysis;
        if (parsed.referenceMode() == ReferenceMode.CID) {
            log("analyzing partitioned graph for CID feasibility...");
            analysis = analyzePartitionedGraph(parsed.sourcePath());
            if (analysis.hasCycles()) {
                throw new IllegalStateException(
                        "Partitioned graph has cyclic cross-resource dependencies and cannot be made CID-consistent "
                                + "with the core-plugin cascade algorithm. Example cycle: " + analysis.exampleCycle());
            }
        } else {
            analysis = new PartitionAnalysis(true, "analysis skipped for IPNS publication");
        }

        PartitionPublishResult result = publishPartitioned(parsed.sourcePath(), parsed.referenceMode(), ipfs);
        writePartitionedReport(parsed.reportPath(), parsed.sourcePath(), result, analysis);
        System.out.println("root=" + parsed.sourcePath());
        System.out.println("rootUri=" + result.rootUri());
        System.out.println("rootResolvedCid=" + result.rootResolvedCid());
        System.out.println("resourceCount=" + result.resources().size());
    }

    private static PublishResult publishSingle(Path sourcePath, ReferenceMode mode, IPFS ipfs) throws Exception {
        log("loading single-resource model from " + sourcePath);
        ResourceSet sourceResourceSet = newFileResourceSet();
        Resource source = sourceResourceSet.getResource(fileUri(sourcePath), true);
        EcoreUtil.resolveAll(sourceResourceSet);

        log("copying model into an IPFS-backed resource");
        ResourceSet targetResourceSet = newIpfsResourceSet(ipfs);
        Resource target = targetResourceSet.createResource(URI.createURI("ipfs://merged-model"));
        target.getContents().addAll(EcoreUtil.copyAll(source.getContents()));
        log("saving single resource via core-plugin in " + mode + " mode");
        target.save(saveOptions(mode));

        return new PublishResult(target.getURI().toString(), resolveUriToCid(target.getURI(), ipfs));
    }

    private static PartitionPublishResult publishPartitioned(Path rootModelPath, ReferenceMode mode, IPFS ipfs) throws Exception {
        log("loading partitioned root model from " + rootModelPath);
        ResourceSet sourceResourceSet = newFileResourceSet();
        Resource sourceRoot = sourceResourceSet.getResource(fileUri(rootModelPath), true);
        int additionalResources = loadAdditionalPartitionResources(rootModelPath, sourceResourceSet);
        log("loaded root plus " + additionalResources + " sibling resources");
        log("resolving cross-resource references...");
        EcoreUtil.resolveAll(sourceResourceSet);

        List<Resource> sourceResources = new ArrayList<>(sourceResourceSet.getResources());
        sourceResources.sort(Comparator.comparing(resource -> resource.getURI().toString()));
        log("copying " + sourceResources.size() + " resources into an IPFS-backed resource set");

        ResourceSet targetResourceSet = newIpfsResourceSet(ipfs);
        Map<Resource, Resource> sourceToTarget = new LinkedHashMap<>();
        Map<Resource, String> sourceToRelativePath = new LinkedHashMap<>();
        EcoreUtil.Copier copier = new EcoreUtil.Copier(true, true);

        for (int index = 0; index < sourceResources.size(); index++) {
            Resource source = sourceResources.get(index);
            String relativePath = relativePath(rootModelPath.getParent(), Path.of(source.getURI().toFileString()));
            sourceToRelativePath.put(source, relativePath);

            String pendingName = sanitizePendingName(relativePath);
            String scheme = mode == ReferenceMode.IPNS ? "ipns" : "ipfs";
            Resource target = targetResourceSet.createResource(URI.createURI(scheme + "://" + pendingName));
            sourceToTarget.put(source, target);

            for (EObject root : source.getContents()) {
                target.getContents().add(copier.copy(root));
            }

            if (index == 0 || (index + 1) % 500 == 0) {
                log("copied resources: " + (index + 1) + "/" + sourceResources.size() + " current=" + relativePath);
            }
        }
        copier.copyReferences();
        log("copy references complete");

        List<PublishedResource> publishedResources = new ArrayList<>();
        int saved = 0;
        for (Map.Entry<Resource, Resource> entry : sourceToTarget.entrySet()) {
            Resource target = entry.getValue();
            target.save(saveOptions(mode));
            publishedResources.add(new PublishedResource(
                    sourceToRelativePath.get(entry.getKey()),
                    target.getURI().toString(),
                    resolveUriToCid(target.getURI(), ipfs)));
            saved++;
            if (saved == 1 || saved % 500 == 0) {
                log("saved resources: " + saved + "/" + sourceToTarget.size()
                        + " current=" + sourceToRelativePath.get(entry.getKey()));
            }
        }

        Resource targetRoot = sourceToTarget.get(sourceRoot);
        publishedResources.sort(Comparator.comparing(PublishedResource::relativePath));
        return new PartitionPublishResult(targetRoot.getURI().toString(), resolveUriToCid(targetRoot.getURI(), ipfs), publishedResources);
    }

    private static PartitionAnalysis analyzePartitionedGraph(Path rootModelPath) throws Exception {
        ResourceSet resourceSet = newFileResourceSet();
        resourceSet.getResource(fileUri(rootModelPath), true);
        loadAdditionalPartitionResources(rootModelPath, resourceSet);
        EcoreUtil.resolveAll(resourceSet);

        Map<Resource, Set<Resource>> outgoing = new LinkedHashMap<>();
        for (Resource resource : resourceSet.getResources()) {
            outgoing.put(resource, referencedResources(resource));
        }

        List<Resource> cycle = findCycle(outgoing);
        return new PartitionAnalysis(!cycle.isEmpty(), describeCycle(cycle, rootModelPath.getParent()));
    }

    private static Set<Resource> referencedResources(Resource resource) {
        Set<Resource> references = new LinkedHashSet<>();
        for (EObject root : resource.getContents()) {
            collectReferencedResources(root, references);
        }
        references.remove(resource);
        return references;
    }

    private static void collectReferencedResources(EObject object, Set<Resource> references) {
        for (EObject referenced : object.eCrossReferences()) {
            if (referenced != null && referenced.eResource() != null) {
                references.add(referenced.eResource());
            }
        }

        TreeIterator<EObject> descendants = object.eAllContents();
        while (descendants.hasNext()) {
            EObject next = descendants.next();
            for (EObject referenced : next.eCrossReferences()) {
                if (referenced != null && referenced.eResource() != null) {
                    references.add(referenced.eResource());
                }
            }
        }
    }

    private static List<Resource> findCycle(Map<Resource, Set<Resource>> outgoing) {
        Set<Resource> visiting = new HashSet<>();
        Set<Resource> visited = new HashSet<>();
        ArrayDeque<Resource> stack = new ArrayDeque<>();

        for (Resource resource : outgoing.keySet()) {
            List<Resource> cycle = dfs(resource, outgoing, visiting, visited, stack);
            if (!cycle.isEmpty()) {
                return cycle;
            }
        }
        return Collections.emptyList();
    }

    private static List<Resource> dfs(
            Resource current,
            Map<Resource, Set<Resource>> outgoing,
            Set<Resource> visiting,
            Set<Resource> visited,
            ArrayDeque<Resource> stack) {
        if (visited.contains(current)) {
            return Collections.emptyList();
        }
        if (visiting.contains(current)) {
            return sliceCycle(stack, current);
        }

        visiting.add(current);
        stack.addLast(current);
        for (Resource next : outgoing.getOrDefault(current, Collections.emptySet())) {
            List<Resource> cycle = dfs(next, outgoing, visiting, visited, stack);
            if (!cycle.isEmpty()) {
                return cycle;
            }
        }
        stack.removeLast();
        visiting.remove(current);
        visited.add(current);
        return Collections.emptyList();
    }

    private static List<Resource> sliceCycle(ArrayDeque<Resource> stack, Resource repeated) {
        List<Resource> cycle = new ArrayList<>();
        boolean collecting = false;
        for (Resource resource : stack) {
            if (resource == repeated) {
                collecting = true;
            }
            if (collecting) {
                cycle.add(resource);
            }
        }
        cycle.add(repeated);
        return cycle;
    }

    private static String describeCycle(List<Resource> cycle, Path baseDirectory) {
        if (cycle.isEmpty()) {
            return "";
        }
        List<String> parts = new ArrayList<>();
        for (Resource resource : cycle) {
            parts.add(relativePath(baseDirectory, Path.of(resource.getURI().toFileString())));
        }
        return String.join(" -> ", parts);
    }

    private static Map<String, Object> saveOptions(ReferenceMode mode) {
        Map<String, Object> options = new HashMap<>();
        options.put(IPFSResourceOptions.REFERENCE_MODE, mode);
        return options;
    }

    private static ResourceSet newFileResourceSet() {
        ResourceSet resourceSet = new ResourceSetImpl();
        resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap()
                .put(Resource.Factory.Registry.DEFAULT_EXTENSION, new XMIResourceFactoryImpl());
        resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap()
                .put("xmi", new XMIResourceFactoryImpl());
        return resourceSet;
    }

    private static ResourceSet newIpfsResourceSet(IPFS ipfs) {
        ResourceSet resourceSet = newFileResourceSet();
        IPFSResourceFactoryImpl factory = new IPFSResourceFactoryImpl(ipfs);
        resourceSet.getResourceFactoryRegistry().getProtocolToFactoryMap().put("ipfs", factory);
        resourceSet.getResourceFactoryRegistry().getProtocolToFactoryMap().put("ipns", factory);
        resourceSet.getURIConverter().getURIHandlers().add(0, new IPFSURIHandlerImpl(ipfs));
        return resourceSet;
    }

    private static void registerEPackage(String epackageClassName) throws Exception {
        Class<?> packageClass = Class.forName(epackageClassName);
        Object instance = packageClass.getField("eINSTANCE").get(null);
        String nsUri = (String) packageClass.getField("eNS_URI").get(null);
        if (!(instance instanceof EPackage)) {
            throw new IllegalArgumentException("Not an EPackage class: " + epackageClassName);
        }
        EPackage.Registry.INSTANCE.put(nsUri, instance);
    }

    private static int loadAdditionalPartitionResources(Path rootModelPath, ResourceSet resourceSet) throws Exception {
        Path parent = rootModelPath.getParent();
        if (parent == null) {
            return 0;
        }

        List<Path> additionalResources = new ArrayList<>();
        collectXmis(parent.resolve("compilation-units"), additionalResources);
        collectXmis(parent.resolve("aux"), additionalResources);
        additionalResources.sort(Comparator.naturalOrder());

        for (Path path : additionalResources) {
            resourceSet.getResource(fileUri(path), true);
        }
        return additionalResources.size();
    }

    private static void collectXmis(Path directory, Collection<Path> sink) throws Exception {
        if (!Files.isDirectory(directory)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(directory)) {
            paths.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".xmi"))
                    .forEach(sink::add);
        }
    }

    private static URI fileUri(Path path) {
        return URI.createFileURI(path.toAbsolutePath().normalize().toString());
    }

    private static String relativePath(Path baseDirectory, Path target) {
        Path normalizedBase = Objects.requireNonNull(baseDirectory, "baseDirectory").toAbsolutePath().normalize();
        Path normalizedTarget = target.toAbsolutePath().normalize();
        return normalizedBase.relativize(normalizedTarget).toString().replace('\\', '/');
    }

    private static String sanitizePendingName(String value) {
        return value.replace('\\', '/').replaceAll("[^a-zA-Z0-9._/-]", "_").replace('/', '_');
    }

    private static String resolveUriToCid(URI uri, IPFS ipfs) throws Exception {
        if ("ipfs".equals(uri.scheme())) {
            return authorityOrOpaque(uri);
        }
        if ("ipns".equals(uri.scheme())) {
            String resolved = resolveIpnsPath(ipfs, authorityOrOpaque(uri));
            return resolved.replace("/ipfs/", "");
        }
        throw new IllegalArgumentException("Unsupported URI scheme: " + uri);
    }

    private static String resolveIpnsPath(IPFS ipfs, String ipnsKey) throws IOException {
        String endpoint = ipfs.protocol + "://" + ipfs.host + ":" + ipfs.port
                + "/api/v0/name/resolve?arg=" + URLEncoder.encode(ipnsKey, StandardCharsets.UTF_8);
        HttpURLConnection connection = (HttpURLConnection) new URL(endpoint).openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.connect();

        try (java.io.InputStream input = connection.getInputStream()) {
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

    private static String authorityOrOpaque(URI uri) {
        String value = uri.authority();
        if (value == null || value.isEmpty()) {
            value = uri.opaquePart();
        }
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException("Cannot extract authority/opaque part from URI: " + uri);
        }
        return value;
    }

    private static void writeSingleReport(Path reportPath, Path sourcePath, PublishResult result) throws IOException {
        List<String> lines = List.of(
                "# Core Plugin IPFS Publication",
                "",
                "- Source: `" + sourcePath.toAbsolutePath().normalize() + "`",
                "- URI: `" + result.uri() + "`",
                "- Resolved CID: `" + result.resolvedCid() + "`");
        Files.write(reportPath, lines, StandardCharsets.UTF_8);
    }

    private static void writePartitionedReport(
            Path reportPath,
            Path rootModelPath,
            PartitionPublishResult result,
            PartitionAnalysis analysis) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add("# Core Plugin IPFS Publication");
        lines.add("");
        lines.add("- Root model: `" + rootModelPath.toAbsolutePath().normalize() + "`");
        lines.add("- Root URI: `" + result.rootUri() + "`");
        lines.add("- Root resolved CID: `" + result.rootResolvedCid() + "`");
        lines.add("- Published resources: `" + result.resources().size() + "`");
        lines.add("- Cyclic graph detected: `" + analysis.hasCycles() + "`");
        if (analysis.hasCycles()) {
            lines.add("- Example cycle: `" + analysis.exampleCycle() + "`");
        }
        lines.add("");
        lines.add("## Resource Map");
        lines.add("");
        lines.add("| Relative Path | URI | Resolved CID |");
        lines.add("| --- | --- | --- |");
        for (PublishedResource resource : result.resources()) {
            lines.add("| `" + resource.relativePath() + "` | `" + resource.uri() + "` | `" + resource.resolvedCid() + "` |");
        }
        Files.write(reportPath, lines, StandardCharsets.UTF_8);
    }

    private static void log(String message) {
        System.out.println("[ipfs-core-publish] " + message);
        System.out.flush();
    }

    private enum PublishKind {
        SINGLE,
        PARTITIONED
    }

    private static final class PublishResult {
        private final String uri;
        private final String resolvedCid;

        private PublishResult(String uri, String resolvedCid) {
            this.uri = uri;
            this.resolvedCid = resolvedCid;
        }

        private String uri() {
            return uri;
        }

        private String resolvedCid() {
            return resolvedCid;
        }
    }

    private static final class PublishedResource {
        private final String relativePath;
        private final String uri;
        private final String resolvedCid;

        private PublishedResource(String relativePath, String uri, String resolvedCid) {
            this.relativePath = relativePath;
            this.uri = uri;
            this.resolvedCid = resolvedCid;
        }

        private String relativePath() {
            return relativePath;
        }

        private String uri() {
            return uri;
        }

        private String resolvedCid() {
            return resolvedCid;
        }
    }

    private static final class PartitionPublishResult {
        private final String rootUri;
        private final String rootResolvedCid;
        private final List<PublishedResource> resources;

        private PartitionPublishResult(String rootUri, String rootResolvedCid, List<PublishedResource> resources) {
            this.rootUri = rootUri;
            this.rootResolvedCid = rootResolvedCid;
            this.resources = resources;
        }

        private String rootUri() {
            return rootUri;
        }

        private String rootResolvedCid() {
            return rootResolvedCid;
        }

        private List<PublishedResource> resources() {
            return resources;
        }
    }

    private static final class PartitionAnalysis {
        private final boolean hasCycles;
        private final String exampleCycle;

        private PartitionAnalysis(boolean hasCycles, String exampleCycle) {
            this.hasCycles = hasCycles;
            this.exampleCycle = exampleCycle;
        }

        private boolean hasCycles() {
            return hasCycles;
        }

        private String exampleCycle() {
            return exampleCycle;
        }
    }

    private static final class Arguments {
        private final PublishKind kind;
        private final Path sourcePath;
        private final ReferenceMode referenceMode;
        private final String ipfsApi;
        private final String epackageClassName;
        private final Path reportPath;

        private Arguments(
                PublishKind kind,
                Path sourcePath,
                ReferenceMode referenceMode,
                String ipfsApi,
                String epackageClassName,
                Path reportPath) {
            this.kind = kind;
            this.sourcePath = sourcePath;
            this.referenceMode = referenceMode;
            this.ipfsApi = ipfsApi;
            this.epackageClassName = epackageClassName;
            this.reportPath = reportPath;
        }

        private PublishKind kind() {
            return kind;
        }

        private Path sourcePath() {
            return sourcePath;
        }

        private ReferenceMode referenceMode() {
            return referenceMode;
        }

        private String ipfsApi() {
            return ipfsApi;
        }

        private String epackageClassName() {
            return epackageClassName;
        }

        private Path reportPath() {
            return reportPath;
        }

        private static Arguments parse(String[] args) {
            if (args.length < 5) {
                throw new IllegalArgumentException(
                        "Usage: <single|partitioned> <sourcePath> <cid|ipns> <epackageClassName> <reportPath> [ipfsApi]");
            }

            PublishKind kind;
            String kindValue = args[0].toLowerCase(Locale.ROOT);
            if ("single".equals(kindValue)) {
                kind = PublishKind.SINGLE;
            } else if ("partitioned".equals(kindValue)) {
                kind = PublishKind.PARTITIONED;
            } else {
                throw new IllegalArgumentException("Unknown kind: " + args[0]);
            }

            ReferenceMode mode;
            String modeValue = args[2].toLowerCase(Locale.ROOT);
            if ("cid".equals(modeValue)) {
                mode = ReferenceMode.CID;
            } else if ("ipns".equals(modeValue)) {
                mode = ReferenceMode.IPNS;
            } else {
                throw new IllegalArgumentException("Unknown reference mode: " + args[2]);
            }

            String ipfsApi = args.length >= 6 ? args[5] : "/ip4/127.0.0.1/tcp/5001";
            return new Arguments(
                    kind,
                    Path.of(args[1]).toAbsolutePath().normalize(),
                    mode,
                    ipfsApi,
                    args[3],
                    Path.of(args[4]).toAbsolutePath().normalize());
        }
    }
}
