package org.ecorefs.modisco.jdt.reverse.tests;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.compare.Comparison;
import org.eclipse.emf.compare.Diff;
import org.eclipse.emf.compare.EMFCompare;
import org.eclipse.emf.compare.scope.DefaultComparisonScope;
import org.eclipse.emf.compare.scope.IComparisonScope;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.ecorefs.modisco.jdt.reverse.ModiscoJavaModelLoader;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;

public class ModiscoEmfCompareSnapshotTest {

  @Test
  public void mergedSnapshotMatchesByteForByteWhenConfigured() throws Exception {
    final Path localMerged = configuredPath("modisco.merged");
    final Path fetchedMerged = configuredPath("modisco.merged.snapshot");

    log("merged-byte-compare: local=" + localMerged);
    log("merged-byte-compare: snapshot=" + fetchedMerged);

    Assert.assertEquals("Merged snapshot should match byte-for-byte",
        -1L, Files.mismatch(localMerged, fetchedMerged));
    log("merged-byte-compare: no differences");
  }

  @Test
  public void emfCompareFindsNoDifferencesForPartitionedSnapshotWhenConfigured() throws Exception {
    Assume.assumeTrue(!hasConfiguredValue("modisco.component.id"));

    final Path localRoot = configuredPath("modisco.fragmented.root");
    final Path snapshotRoot = configuredPath("modisco.fragmented.snapshot.root");

    log("fragmented-compare: localRoot=" + localRoot);
    log("fragmented-compare: snapshotRoot=" + snapshotRoot);

    ModiscoJavaModelLoader.relaxXmlParserLimits();

    final ResourceSet localSet = ModiscoModelTestSupport.newResourceSet();
    localSet.getResource(URI.createFileURI(localRoot.toString()), true);
    final int localExtraResources = ModiscoModelTestSupport.loadAdditionalPartitionResources(localRoot, localSet);

    final ResourceSet snapshotSet = ModiscoModelTestSupport.newResourceSet();
    snapshotSet.getResource(URI.createFileURI(snapshotRoot.toString()), true);
    final int snapshotExtraResources = ModiscoModelTestSupport.loadAdditionalPartitionResources(snapshotRoot, snapshotSet);

    log("fragmented-compare: localExtraResources=" + localExtraResources
        + ", snapshotExtraResources=" + snapshotExtraResources);

    normalizeResourceUris(localSet, localRoot.getParent());
    normalizeResourceUris(snapshotSet, snapshotRoot.getParent());

    final Map<String, Resource> localResources = indexedResources(localSet);
    final Map<String, Resource> snapshotResources = indexedResources(snapshotSet);

    Assert.assertEquals("Partitioned snapshots should contain the same resource set",
        localResources.keySet(), snapshotResources.keySet());

    final EMFCompare emfCompare = EMFCompare.builder().build();
    final List<String> resourcePaths = new ArrayList<>(localResources.keySet());
    resourcePaths.sort(Comparator.naturalOrder());

    int compared = 0;
    for (final String resourcePath : resourcePaths) {
      compared++;
      if (compared == 1 || compared % 500 == 0) {
        log("fragmented-compare: comparing resource " + compared + "/" + resourcePaths.size()
            + " path=" + resourcePath);
      }

      final Resource localResource = localResources.get(resourcePath);
      final Resource snapshotResource = snapshotResources.get(resourcePath);

      final IComparisonScope scope = new DefaultComparisonScope(localResource, snapshotResource, null);
      final Comparison comparison = emfCompare.compare(scope);
      final List<Diff> differences = comparison.getDifferences();
      if (!differences.isEmpty()) {
        Assert.fail("EMF Compare found " + differences.size() + " differences for " + resourcePath
            + "; first difference=" + differences.get(0));
      }
    }

    log("fragmented-compare: comparedResources=" + resourcePaths.size() + " with no differences");
  }

  @Test
  public void emfCompareFindsNoDifferencesForConfiguredComponentSnapshotWhenConfigured() throws Exception {
    final Path localRoot = configuredPath("modisco.fragmented.root");
    final Path snapshotRoot = configuredPath("modisco.fragmented.snapshot.root");
    final List<String> componentIds = configuredValues("modisco.component.id");
    final List<ComponentCompareResult> results = new ArrayList<>();
    final EMFCompare emfCompare = EMFCompare.builder().build();

    log("component-compare: localRoot=" + localRoot);
    log("component-compare: snapshotRoot=" + snapshotRoot);
    log("component-compare: componentCount=" + componentIds.size()
        + ", componentIds=" + String.join(", ", componentIds));

    Assert.assertEquals("Partitioned root should match byte-for-byte for component-scoped comparison",
        -1L, Files.mismatch(localRoot, snapshotRoot));

    for (final String componentId : componentIds) {
      final ComponentCompareResult result = compareComponent(localRoot, snapshotRoot, componentId, emfCompare);
      results.add(result);
      log("component-compare: componentId=" + result.componentId()
          + ", comparedResources=" + result.comparedResources()
          + ", compareMs=" + result.compareMillis()
          + ", status=OK");
    }

    writeComponentReportsIfConfigured(results);

    long totalResources = 0L;
    long totalCompareMillis = 0L;
    for (final ComponentCompareResult result : results) {
      totalResources += result.comparedResources();
      totalCompareMillis += result.compareMillis();
    }
    log("component-compare: totalComponents=" + results.size()
        + ", totalComparedResources=" + totalResources
        + ", totalCompareMs=" + totalCompareMillis
        + ", status=OK");
  }

  private static ComponentCompareResult compareComponent(
      final Path localRoot,
      final Path snapshotRoot,
      final String componentId,
      final EMFCompare emfCompare) throws Exception {
    final long compareStart = System.nanoTime();
    log("component-compare: componentId=" + componentId);

    final Path localManifest = componentManifestPath(localRoot, componentId);
    final Path snapshotManifest = componentManifestPath(snapshotRoot, componentId);
    Assert.assertTrue("Missing local component manifest: " + localManifest, Files.exists(localManifest));
    Assert.assertTrue("Missing snapshot component manifest: " + snapshotManifest, Files.exists(snapshotManifest));
    Assert.assertEquals("Component manifest should match byte-for-byte",
        -1L, Files.mismatch(localManifest, snapshotManifest));

    final Path localComponentDirectory = localManifest.getParent();
    final Path snapshotComponentDirectory = snapshotManifest.getParent();
    final long localXmiBytes = totalXmiBytes(localComponentDirectory);
    final long snapshotXmiBytes = totalXmiBytes(snapshotComponentDirectory);

    final Map<String, Resource> localResources =
        loadCanonicalComponentResources(localComponentDirectory, localRoot.getParent());
    final Map<String, Resource> snapshotResources =
        loadCanonicalComponentResources(snapshotComponentDirectory, snapshotRoot.getParent());

    log("component-compare: localSubsetResources=" + localResources.size()
        + ", snapshotSubsetResources=" + snapshotResources.size()
        + ", localXmiBytes=" + localXmiBytes
        + ", snapshotXmiBytes=" + snapshotXmiBytes);

    Assert.assertEquals("Component snapshots should contain the same scoped resource set",
        localResources.keySet(), snapshotResources.keySet());

    final List<String> resourcePaths = new ArrayList<>(localResources.keySet());
    resourcePaths.sort(Comparator.naturalOrder());

    int compared = 0;
    for (final String resourcePath : resourcePaths) {
      compared++;
      if (compared == 1 || compared % 100 == 0) {
        log("component-compare: comparing resource " + compared + "/" + resourcePaths.size()
            + " path=" + resourcePath);
      }

      final Resource localResource = localResources.get(resourcePath);
      final Resource snapshotResource = snapshotResources.get(resourcePath);

      final IComparisonScope scope = new DefaultComparisonScope(localResource, snapshotResource, null);
      final Comparison comparison = emfCompare.compare(scope);
      final List<Diff> differences = comparison.getDifferences();
      if (!differences.isEmpty()) {
        Assert.fail("EMF Compare found " + differences.size() + " differences for " + resourcePath
            + "; first difference=" + differences.get(0));
      }
    }

    return new ComponentCompareResult(
        componentId,
        resourcePaths.size(),
        localXmiBytes,
        snapshotXmiBytes,
        nanosToMillis(System.nanoTime() - compareStart));
  }

  private static Map<String, Resource> loadCanonicalComponentResources(
      final Path componentDirectory,
      final Path partitionRoot) throws Exception {
    final List<Path> resourceFiles = new ArrayList<>();
    try (Stream<Path> paths = Files.walk(componentDirectory)) {
      paths.filter(Files::isRegularFile)
          .filter(path -> path.getFileName().toString().endsWith(".xmi"))
          .forEach(resourceFiles::add);
    }
    resourceFiles.sort(Comparator.naturalOrder());

    final ResourceSet resourceSet = ModiscoModelTestSupport.newResourceSet();
    final Map<String, Resource> resources = new LinkedHashMap<>();
    for (final Path resourceFile : resourceFiles) {
      final String relativePath = partitionRoot.relativize(resourceFile).toString().replace('\\', '/');
      final URI compareUri = URI.createURI("compare:/partitioned/" + relativePath);
      final Resource resource = resourceSet.createResource(compareUri);
      try (var input = Files.newInputStream(resourceFile)) {
        resource.load(input, null);
      }
      Assert.assertFalse("Expected at least one root object in " + compareUri,
          resource.getContents().isEmpty());
      resources.put(compareUri.toString(), resource);
    }
    return resources;
  }

  private static long totalXmiBytes(final Path componentDirectory) throws Exception {
    long total = 0L;
    try (Stream<Path> paths = Files.walk(componentDirectory)) {
      for (final Path path : (Iterable<Path>) paths.filter(Files::isRegularFile)
          .filter(candidate -> candidate.getFileName().toString().endsWith(".xmi"))::iterator) {
        total += Files.size(path);
      }
    }
    return total;
  }

  private static void normalizeResourceUris(final ResourceSet resourceSet, final Path rootDirectory) {
    EcoreUtil.resolveAll(resourceSet);
    for (final Resource resource : resourceSet.getResources()) {
      if (resource.getURI() == null || !resource.getURI().isFile()) {
        continue;
      }

      final Path absolutePath = Path.of(resource.getURI().toFileString()).toAbsolutePath().normalize();
      final String relativePath = rootDirectory.relativize(absolutePath).toString().replace('\\', '/');
      resource.setURI(URI.createURI("compare:/partitioned/" + relativePath));
    }
  }

  private static Map<String, Resource> indexedResources(final ResourceSet resourceSet) {
    final Map<String, Resource> resources = new LinkedHashMap<>();
    for (final Resource resource : resourceSet.getResources()) {
      if (resource.getURI() == null) {
        continue;
      }
      resources.put(resource.getURI().toString(), resource);
    }
    return resources;
  }

  private static Path componentManifestPath(final Path rootModelPath, final String componentId) {
    return rootModelPath.getParent()
        .resolve("compilation-units")
        .resolve(componentId)
        .resolve("component-manifest.json");
  }

  private static void writeComponentReportsIfConfigured(final List<ComponentCompareResult> results) throws IOException {
    if (!hasConfiguredValue("modisco.component.report.dir")) {
      return;
    }

    final Path reportDirectory = Path.of(configuredValue("modisco.component.report.dir"))
        .toAbsolutePath()
        .normalize();
    Files.createDirectories(reportDirectory);

    final Path csvPath = reportDirectory.resolve("component-compare-stats.csv");
    final Path mdPath = reportDirectory.resolve("component-compare-summary.md");

    final StringBuilder csv = new StringBuilder();
    csv.append("component_id,compared_resources,local_xmi_bytes,snapshot_xmi_bytes,compare_ms\n");

    long totalResources = 0L;
    long totalLocalBytes = 0L;
    long totalSnapshotBytes = 0L;
    long totalCompareMillis = 0L;
    for (final ComponentCompareResult result : results) {
      csv.append(csvEscape(result.componentId())).append(',')
          .append(result.comparedResources()).append(',')
          .append(result.localXmiBytes()).append(',')
          .append(result.snapshotXmiBytes()).append(',')
          .append(result.compareMillis()).append('\n');

      totalResources += result.comparedResources();
      totalLocalBytes += result.localXmiBytes();
      totalSnapshotBytes += result.snapshotXmiBytes();
      totalCompareMillis += result.compareMillis();
    }

    final StringBuilder md = new StringBuilder();
    md.append("# Component Compare Summary\n\n");
    md.append("- Components compared: ").append(results.size()).append('\n');
    md.append("- Compared resources: ").append(totalResources).append('\n');
    md.append("- Local XMI bytes: ").append(totalLocalBytes).append(" (")
        .append(humanSize(totalLocalBytes)).append(")\n");
    md.append("- Snapshot XMI bytes: ").append(totalSnapshotBytes).append(" (")
        .append(humanSize(totalSnapshotBytes)).append(")\n");
    md.append("- Total compare time: ").append(totalCompareMillis).append(" ms\n\n");
    md.append("| Component | Resources (count) | Local Size (bytes) | Snapshot Size (bytes) | Compare Time (ms) |\n");
    md.append("|---|---:|---:|---:|---:|\n");
    for (final ComponentCompareResult result : results) {
      md.append("| ").append(result.componentId())
          .append(" | ").append(result.comparedResources())
          .append(" | ").append(result.localXmiBytes())
          .append(" | ").append(result.snapshotXmiBytes())
          .append(" | ").append(result.compareMillis())
          .append(" |\n");
    }

    Files.writeString(csvPath, csv.toString(),
        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
    Files.writeString(mdPath, md.toString(),
        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
    log("component-compare: wrote reports to " + reportDirectory);
  }

  private static Path configuredPath(final String propertyName) {
    final String value = configuredValue(propertyName);
    final Path path = Path.of(value).toAbsolutePath().normalize();
    Assert.assertTrue("Missing configured path for " + propertyName + ": " + path, Files.exists(path));
    return path;
  }

  private static String configuredValue(final String propertyName) {
    final String value = System.getProperty(propertyName);
    Assume.assumeTrue(value != null && !value.isBlank() && !"null".equalsIgnoreCase(value));
    return value;
  }

  private static boolean hasConfiguredValue(final String propertyName) {
    final String value = System.getProperty(propertyName);
    return value != null && !value.isBlank() && !"null".equalsIgnoreCase(value);
  }

  private static List<String> configuredValues(final String propertyName) {
    final String value = configuredValue(propertyName);
    final List<String> values = Arrays.stream(value.split(","))
        .map(String::trim)
        .filter(candidate -> !candidate.isEmpty())
        .toList();
    Assert.assertFalse("Expected at least one configured value for " + propertyName, values.isEmpty());
    return values;
  }

  private static long nanosToMillis(final long nanos) {
    return nanos / 1_000_000L;
  }

  private static String humanSize(final long bytes) {
    final double kib = 1024.0;
    final double mib = kib * 1024.0;
    if (bytes >= mib) {
      return String.format("%.2f MiB", bytes / mib);
    }
    if (bytes >= kib) {
      return String.format("%.2f KiB", bytes / kib);
    }
    return bytes + " bytes";
  }

  private static String csvEscape(final String value) {
    return '"' + value.replace("\"", "\"\"") + '"';
  }

  private static void log(final String message) {
    System.out.println("[emf-compare] " + message);
    System.out.flush();
  }

  private record ComponentCompareResult(
      String componentId,
      int comparedResources,
      long localXmiBytes,
      long snapshotXmiBytes,
      long compareMillis) {
  }
}
