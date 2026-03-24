package org.ecorefs.modisco.jdt.reverse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;

import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.modisco.java.Model;

public final class ModiscoComponentManifestGenerator {

  private static final String VERSION_MANIFEST_FILENAME = "version-manifest.json";
  private static final String COMPONENT_SUMMARY_FILENAME = "component-summary.csv";
  private static final String COMPONENT_DEPENDENCIES_FILENAME = "component-dependencies.csv";
  private static final String README_FILENAME = "component-manifests.md";
  private static final String COMPONENT_MANIFEST_FILENAME = "component-manifest.json";
  private static final String ROOT_KIND = "root-model";
  private static final String COMPILATION_UNIT_KIND = "compilation-unit";
  private static final String AUX_KIND = "aux";
  private static final String OTHER_KIND = "other";

  public ManifestGenerationResult generateForPartitionedModel(final Path rootModelPath) throws Exception {
    final Path normalizedRootModelPath = normalizeExistingFile(rootModelPath, "rootModelPath");
    final Path partitionRoot = requirePartitionRoot(normalizedRootModelPath);

    ModiscoJavaModelLoader.relaxXmlParserLimits();
    log("loading partitioned model for component manifests... root=" + normalizedRootModelPath);

    final ResourceSet resourceSet = ModiscoJavaModelLoader.newResourceSet();
    final Model model = ModiscoJavaModelLoader.loadModel(normalizedRootModelPath, resourceSet);
    final int loadedAdditionalResources = loadAdditionalPartitionResources(partitionRoot, resourceSet);
    log("loaded root plus " + loadedAdditionalResources + " sibling resources");

    return generateForLoadedPartitionedModel(normalizedRootModelPath, resourceSet, model);
  }

  public ManifestGenerationResult generateForLoadedPartitionedModel(
      final Path rootModelPath,
      final ResourceSet resourceSet,
      final Model model) throws Exception {
    Objects.requireNonNull(resourceSet, "resourceSet");
    Objects.requireNonNull(model, "model");

    final Path normalizedRootModelPath = normalizeExistingFile(rootModelPath, "rootModelPath");
    final Path partitionRoot = requirePartitionRoot(normalizedRootModelPath);

    final List<ResourceDescriptor> descriptors = describeResources(resourceSet, partitionRoot, normalizedRootModelPath);
    final Map<Resource, ResourceDescriptor> descriptorByResource = new IdentityHashMap<>();
    final Map<String, ComponentInfo> components = new LinkedHashMap<>();
    final List<SharedResourceInfo> sharedResources = new ArrayList<>();

    for (final ResourceDescriptor descriptor : descriptors) {
      descriptorByResource.put(descriptor.resource(), descriptor);
      if (descriptor.componentId() != null) {
        final ComponentInfo component = components.computeIfAbsent(
            descriptor.componentId(),
            ignored -> new ComponentInfo(descriptor.componentId(), descriptor.repoName(), descriptor.moduleName()));
        component.resourcePaths().add(descriptor.relativePath());
      } else {
        sharedResources.add(new SharedResourceInfo(descriptor.kind(), descriptor.relativePath()));
      }
    }

    log("grouped resources: components=" + components.size()
        + ", sharedResources=" + sharedResources.size()
        + ", totalResources=" + descriptors.size());

    log("resolving cross-resource references for dependency analysis...");
    EcoreUtil.resolveAll(resourceSet);

    final List<ResourceDescriptor> componentDescriptors = new ArrayList<>();
    for (final ResourceDescriptor descriptor : descriptors) {
      if (descriptor.componentId() != null) {
        componentDescriptors.add(descriptor);
      }
    }
    componentDescriptors.sort(Comparator.comparing(ResourceDescriptor::relativePath));

    for (int index = 0; index < componentDescriptors.size(); index++) {
      final ResourceDescriptor descriptor = componentDescriptors.get(index);
      final ComponentInfo component = components.get(descriptor.componentId());
      collectDependencies(descriptor, descriptorByResource, component);

      if (index == 0 || (index + 1) % 500 == 0) {
        log("scanned component resources: " + (index + 1) + "/" + componentDescriptors.size()
            + " current=" + descriptor.relativePath());
      }
    }

    final List<ComponentInfo> sortedComponents = new ArrayList<>(components.values());
    sortedComponents.sort(Comparator.comparing(ComponentInfo::componentId));
    sharedResources.sort(Comparator.comparing(SharedResourceInfo::relativePath));

    writeComponentManifests(partitionRoot, sortedComponents);
    writeVersionManifest(partitionRoot, normalizedRootModelPath, model, sortedComponents, sharedResources);
    writeComponentSummaryCsv(partitionRoot, sortedComponents);
    final int dependencyEdges = writeComponentDependenciesCsv(partitionRoot, sortedComponents);
    writeReadme(partitionRoot, model, sortedComponents.size(), sharedResources.size(), dependencyEdges);

    final ManifestGenerationResult result = new ManifestGenerationResult(
        partitionRoot.resolve(VERSION_MANIFEST_FILENAME),
        sortedComponents.size(),
        sharedResources.size(),
        dependencyEdges,
        descriptors.size());
    log("done: versionManifest=" + result.versionManifestPath()
        + ", components=" + result.componentCount()
        + ", sharedResources=" + result.sharedResourceCount()
        + ", dependencyEdges=" + result.dependencyEdgeCount()
        + ", totalResources=" + result.totalResourceCount());
    return result;
  }

  private static void collectDependencies(
      final ResourceDescriptor descriptor,
      final Map<Resource, ResourceDescriptor> descriptorByResource,
      final ComponentInfo component) {
    final Resource resource = descriptor.resource();
    for (final EObject root : resource.getContents()) {
      collectDependencies(root, descriptor, descriptorByResource, component);
    }
  }

  private static void collectDependencies(
      final EObject object,
      final ResourceDescriptor sourceDescriptor,
      final Map<Resource, ResourceDescriptor> descriptorByResource,
      final ComponentInfo component) {
    countCrossReferences(object, sourceDescriptor, descriptorByResource, component);
    final TreeIterator<EObject> contents = object.eAllContents();
    while (contents.hasNext()) {
      countCrossReferences(contents.next(), sourceDescriptor, descriptorByResource, component);
    }
  }

  private static void countCrossReferences(
      final EObject source,
      final ResourceDescriptor sourceDescriptor,
      final Map<Resource, ResourceDescriptor> descriptorByResource,
      final ComponentInfo component) {
    for (final EObject target : source.eCrossReferences()) {
      if (target == null || target.eResource() == null) {
        continue;
      }

      final ResourceDescriptor targetDescriptor = descriptorByResource.get(target.eResource());
      if (targetDescriptor == null) {
        continue;
      }

      if (Objects.equals(sourceDescriptor.componentId(), targetDescriptor.componentId())) {
        continue;
      }

      if (targetDescriptor.componentId() != null) {
        component.outgoingComponentReferences().merge(targetDescriptor.componentId(), 1, Integer::sum);
      } else {
        component.outgoingSharedReferences().merge(targetDescriptor.relativePath(), 1, Integer::sum);
      }
    }
  }

  private static void writeComponentManifests(
      final Path partitionRoot,
      final List<ComponentInfo> components) throws IOException {
    for (final ComponentInfo component : components) {
      final Path manifestPath = partitionRoot.resolve(componentManifestRelativePath(component));
      Files.createDirectories(manifestPath.getParent());
      Files.write(manifestPath, componentManifestLines(component), StandardCharsets.UTF_8);
    }
  }

  private static void writeVersionManifest(
      final Path partitionRoot,
      final Path rootModelPath,
      final Model model,
      final List<ComponentInfo> components,
      final List<SharedResourceInfo> sharedResources) throws IOException {
    Files.write(
        partitionRoot.resolve(VERSION_MANIFEST_FILENAME),
        versionManifestLines(partitionRoot, rootModelPath, model, components, sharedResources),
        StandardCharsets.UTF_8);
  }

  private static void writeComponentSummaryCsv(
      final Path partitionRoot,
      final List<ComponentInfo> components) throws IOException {
    final List<String> lines = new ArrayList<>();
    lines.add(csvRow(
        "component_id",
        "repo",
        "module",
        "resource_count",
        "outgoing_component_count",
        "outgoing_component_xrefs",
        "shared_resource_count",
        "shared_resource_xrefs",
        "manifest_path"));

    for (final ComponentInfo component : components) {
      lines.add(csvRow(
          component.componentId(),
          component.repoName(),
          component.moduleName(),
          Integer.toString(component.resourcePaths().size()),
          Integer.toString(component.outgoingComponentReferences().size()),
          Integer.toString(sumValues(component.outgoingComponentReferences())),
          Integer.toString(component.outgoingSharedReferences().size()),
          Integer.toString(sumValues(component.outgoingSharedReferences())),
          componentManifestRelativePath(component)));
    }

    Files.write(partitionRoot.resolve(COMPONENT_SUMMARY_FILENAME), lines, StandardCharsets.UTF_8);
  }

  private static int writeComponentDependenciesCsv(
      final Path partitionRoot,
      final List<ComponentInfo> components) throws IOException {
    final List<String> lines = new ArrayList<>();
    lines.add(csvRow("source_component", "target_kind", "target_id", "cross_reference_count"));

    int edges = 0;
    for (final ComponentInfo component : components) {
      for (final Map.Entry<String, Integer> dependency : component.outgoingComponentReferences().entrySet()) {
        lines.add(csvRow(component.componentId(), "component", dependency.getKey(), Integer.toString(dependency.getValue())));
        edges++;
      }
      for (final Map.Entry<String, Integer> dependency : component.outgoingSharedReferences().entrySet()) {
        lines.add(csvRow(component.componentId(), "shared-resource", dependency.getKey(), Integer.toString(dependency.getValue())));
        edges++;
      }
    }

    Files.write(partitionRoot.resolve(COMPONENT_DEPENDENCIES_FILENAME), lines, StandardCharsets.UTF_8);
    return edges;
  }

  private static void writeReadme(
      final Path partitionRoot,
      final Model model,
      final int componentCount,
      final int sharedResourceCount,
      final int dependencyEdges) throws IOException {
    final List<String> lines = List.of(
        "# Component Version Manifests",
        "",
        "This directory lifts the compilation-unit partition into plugin/module-level version units.",
        "",
        "- Model name: `" + model.getName() + "`",
        "- Version head manifest: `" + VERSION_MANIFEST_FILENAME + "`",
        "- Component count: `" + componentCount + "`",
        "- Shared resource count: `" + sharedResourceCount + "`",
        "- Dependency edges: `" + dependencyEdges + "`",
        "",
        "Intended publication layout:",
        "",
        "- root manifest next to the root XMI",
        "- component manifests next to the component XMI files",
        "- immutable resource fragments by CID",
        "- one component manifest per plugin/module",
        "- one version manifest as the mutable project head target"
    );
    Files.write(partitionRoot.resolve(README_FILENAME), lines, StandardCharsets.UTF_8);
  }

  private static List<String> componentManifestLines(final ComponentInfo component) {
    final List<String> lines = new ArrayList<>();
    lines.add("{");
    lines.add("  \"schemaVersion\": 1,");
    lines.add("  \"manifestKind\": \"modisco-component-manifest\",");
    lines.add("  \"componentId\": \"" + json(component.componentId()) + "\",");
    lines.add("  \"repo\": \"" + json(component.repoName()) + "\",");
    lines.add("  \"module\": \"" + json(component.moduleName()) + "\",");
    lines.add("  \"resourceCount\": " + component.resourcePaths().size() + ",");
    lines.add("  \"resources\": [");
    appendStringArray(lines, component.resourcePaths(), 4);
    lines.add("  ],");
    lines.add("  \"dependencies\": [");
    appendDependencyArray(lines, component.outgoingComponentReferences(), "componentId", 4);
    lines.add("  ],");
    lines.add("  \"sharedResourceDependencies\": [");
    appendDependencyArray(lines, component.outgoingSharedReferences(), "path", 4);
    lines.add("  ]");
    lines.add("}");
    return lines;
  }

  private static List<String> versionManifestLines(
      final Path partitionRoot,
      final Path rootModelPath,
      final Model model,
      final List<ComponentInfo> components,
      final List<SharedResourceInfo> sharedResources) {
    final List<String> lines = new ArrayList<>();
    lines.add("{");
    lines.add("  \"schemaVersion\": 1,");
    lines.add("  \"manifestKind\": \"modisco-project-version-manifest\",");
    lines.add("  \"modelName\": \"" + json(model.getName()) + "\",");
    lines.add("  \"rootModel\": \"" + json(partitionRoot.relativize(rootModelPath).toString().replace('\\', '/')) + "\",");
    lines.add("  \"componentStrategy\": \"plugin-module over compilation-unit resources\",");
    lines.add("  \"sharedResources\": [");
    appendSharedResources(lines, sharedResources);
    lines.add("  ],");
    lines.add("  \"components\": [");
    appendComponents(lines, components);
    lines.add("  ]");
    lines.add("}");
    return lines;
  }

  private static void appendStringArray(final List<String> lines, final List<String> values, final int indent) {
    for (int index = 0; index < values.size(); index++) {
      final String suffix = index + 1 == values.size() ? "" : ",";
      lines.add(spaces(indent) + "\"" + json(values.get(index)) + "\"" + suffix);
    }
  }

  private static void appendDependencyArray(
      final List<String> lines,
      final Map<String, Integer> dependencies,
      final String idField,
      final int indent) {
    final List<Map.Entry<String, Integer>> entries = new ArrayList<>(dependencies.entrySet());
    entries.sort(Map.Entry.comparingByKey());
    for (int index = 0; index < entries.size(); index++) {
      final Map.Entry<String, Integer> entry = entries.get(index);
      final String suffix = index + 1 == entries.size() ? "" : ",";
      lines.add(spaces(indent) + "{ \"" + idField + "\": \"" + json(entry.getKey())
          + "\", \"crossReferenceCount\": " + entry.getValue() + " }" + suffix);
    }
  }

  private static void appendSharedResources(final List<String> lines, final List<SharedResourceInfo> sharedResources) {
    for (int index = 0; index < sharedResources.size(); index++) {
      final SharedResourceInfo shared = sharedResources.get(index);
      final String suffix = index + 1 == sharedResources.size() ? "" : ",";
      lines.add("    { \"kind\": \"" + json(shared.kind()) + "\", \"path\": \"" + json(shared.relativePath()) + "\" }" + suffix);
    }
  }

  private static void appendComponents(final List<String> lines, final List<ComponentInfo> components) {
    for (int index = 0; index < components.size(); index++) {
      final ComponentInfo component = components.get(index);
      final String suffix = index + 1 == components.size() ? "" : ",";
      lines.add("    {");
      lines.add("      \"componentId\": \"" + json(component.componentId()) + "\",");
      lines.add("      \"repo\": \"" + json(component.repoName()) + "\",");
      lines.add("      \"module\": \"" + json(component.moduleName()) + "\",");
      lines.add("      \"manifest\": \"" + json(componentManifestRelativePath(component)) + "\",");
      lines.add("      \"resourceCount\": " + component.resourcePaths().size() + ",");
      lines.add("      \"dependencyCount\": " + component.outgoingComponentReferences().size());
      lines.add("    }" + suffix);
    }
  }

  private static List<ResourceDescriptor> describeResources(
      final ResourceSet resourceSet,
      final Path partitionRoot,
      final Path rootModelPath) {
    final List<ResourceDescriptor> descriptors = new ArrayList<>();
    final String normalizedRootRelativePath = partitionRoot.relativize(rootModelPath).toString().replace('\\', '/');

    for (final Resource resource : resourceSet.getResources()) {
      if (resource.getURI() == null || !resource.getURI().isFile()) {
        continue;
      }

      final Path absolutePath = Path.of(resource.getURI().toFileString()).toAbsolutePath().normalize();
      final String relativePath = partitionRoot.relativize(absolutePath).toString().replace('\\', '/');
      final String kind = classifyResourceKind(relativePath, normalizedRootRelativePath);
      final ComponentCoordinates coordinates = componentCoordinates(relativePath);
      descriptors.add(new ResourceDescriptor(
          resource,
          relativePath,
          kind,
          coordinates == null ? null : coordinates.componentId(),
          coordinates == null ? null : coordinates.repoName(),
          coordinates == null ? null : coordinates.moduleName()));
    }

    descriptors.sort(Comparator.comparing(ResourceDescriptor::relativePath));
    return descriptors;
  }

  private static String classifyResourceKind(final String relativePath, final String rootRelativePath) {
    if (relativePath.equals(rootRelativePath)) {
      return ROOT_KIND;
    }
    if (relativePath.startsWith("compilation-units/")) {
      return COMPILATION_UNIT_KIND;
    }
    if (relativePath.startsWith("aux/")) {
      return AUX_KIND;
    }
    return OTHER_KIND;
  }

  private static ComponentCoordinates componentCoordinates(final String relativePath) {
    final Path path = Path.of(relativePath);
    if (!"compilation-units".equals(path.getName(0).toString())) {
      return null;
    }

    if (path.getNameCount() == 2) {
      return new ComponentCoordinates("_local/default-component", "_local", "default-component");
    }
    if (path.getNameCount() < 3) {
      return null;
    }

    final String repoName = path.getName(1).toString();
    final String moduleName = path.getName(2).toString();
    return new ComponentCoordinates(repoName + "/" + moduleName, repoName, moduleName);
  }

  private static int loadAdditionalPartitionResources(final Path partitionRoot, final ResourceSet resourceSet) throws Exception {
    final List<Path> additionalResources = new ArrayList<>();
    collectXmis(partitionRoot.resolve("compilation-units"), additionalResources);
    collectXmis(partitionRoot.resolve("aux"), additionalResources);
    additionalResources.sort(Comparator.naturalOrder());

    for (final Path resourcePath : additionalResources) {
      resourceSet.getResource(org.eclipse.emf.common.util.URI.createFileURI(resourcePath.toAbsolutePath().normalize().toString()), true);
    }
    return additionalResources.size();
  }

  private static void collectXmis(final Path directory, final Collection<Path> sink) throws Exception {
    if (!Files.isDirectory(directory)) {
      return;
    }
    try (Stream<Path> paths = Files.walk(directory)) {
      paths.filter(Files::isRegularFile)
          .filter(path -> path.getFileName().toString().endsWith(".xmi"))
          .forEach(sink::add);
    }
  }

  private static Path normalizeExistingFile(final Path path, final String name) {
    Objects.requireNonNull(path, name);
    final Path normalized = path.toAbsolutePath().normalize();
    if (!Files.isRegularFile(normalized)) {
      throw new IllegalArgumentException("Missing file: " + normalized);
    }
    return normalized;
  }

  private static Path requirePartitionRoot(final Path rootModelPath) {
    final Path parent = rootModelPath.getParent();
    if (parent == null) {
      throw new IllegalArgumentException("Root model has no parent directory: " + rootModelPath);
    }
    return parent;
  }

  private static int sumValues(final Map<String, Integer> values) {
    int sum = 0;
    for (final int value : values.values()) {
      sum += value;
    }
    return sum;
  }

  private static String componentManifestRelativePath(final ComponentInfo component) {
    return "compilation-units/" + component.repoName() + "/" + component.moduleName() + "/" + COMPONENT_MANIFEST_FILENAME;
  }

  private static String spaces(final int count) {
    return " ".repeat(Math.max(0, count));
  }

  private static String csvRow(final String... values) {
    final List<String> escaped = new ArrayList<>(values.length);
    for (final String value : values) {
      final String safe = value == null ? "" : value.replace("\"", "\"\"");
      escaped.add("\"" + safe + "\"");
    }
    return String.join(",", escaped);
  }

  private static String json(final String value) {
    if (value == null) {
      return "";
    }

    final StringBuilder builder = new StringBuilder(value.length() + 8);
    for (int index = 0; index < value.length(); index++) {
      final char current = value.charAt(index);
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

  private static void log(final String message) {
    System.out.println("[component-manifests] " + message);
    System.out.flush();
  }

  private static final class ComponentInfo {
    private final String componentId;
    private final String repoName;
    private final String moduleName;
    private final List<String> resourcePaths = new ArrayList<>();
    private final Map<String, Integer> outgoingComponentReferences = new TreeMap<>();
    private final Map<String, Integer> outgoingSharedReferences = new TreeMap<>();

    private ComponentInfo(final String componentId, final String repoName, final String moduleName) {
      this.componentId = componentId;
      this.repoName = repoName;
      this.moduleName = moduleName;
    }

    private String componentId() {
      return componentId;
    }

    private String repoName() {
      return repoName;
    }

    private String moduleName() {
      return moduleName;
    }

    private List<String> resourcePaths() {
      return resourcePaths;
    }

    private Map<String, Integer> outgoingComponentReferences() {
      return outgoingComponentReferences;
    }

    private Map<String, Integer> outgoingSharedReferences() {
      return outgoingSharedReferences;
    }
  }

  private record ComponentCoordinates(String componentId, String repoName, String moduleName) {
  }

  private record ResourceDescriptor(
      Resource resource,
      String relativePath,
      String kind,
      String componentId,
      String repoName,
      String moduleName) {
  }

  private record SharedResourceInfo(String kind, String relativePath) {
  }

  public record ManifestGenerationResult(
      Path versionManifestPath,
      int componentCount,
      int sharedResourceCount,
      int dependencyEdgeCount,
      int totalResourceCount) {
  }
}
