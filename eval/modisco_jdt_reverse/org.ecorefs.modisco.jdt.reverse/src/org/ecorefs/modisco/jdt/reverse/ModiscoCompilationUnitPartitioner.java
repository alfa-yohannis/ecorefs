package org.ecorefs.modisco.jdt.reverse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.xmi.XMLResource;
import org.eclipse.modisco.java.AbstractTypeDeclaration;
import org.eclipse.modisco.java.Archive;
import org.eclipse.modisco.java.ClassFile;
import org.eclipse.modisco.java.CompilationUnit;
import org.eclipse.modisco.java.Model;
import org.eclipse.modisco.java.Package;
import org.eclipse.modisco.java.UnresolvedItem;

public final class ModiscoCompilationUnitPartitioner {

  private static final String ROOT_FILENAME = "all-repos-merged.root.xmi";

  public PartitionResult partitionByCompilationUnit(final Path mergedModelPath, final Path outputDirectory) throws Exception {
    Objects.requireNonNull(mergedModelPath, "mergedModelPath");
    Objects.requireNonNull(outputDirectory, "outputDirectory");

    final Path normalizedModelPath = mergedModelPath.toAbsolutePath().normalize();
    final Path normalizedOutputDirectory = outputDirectory.toAbsolutePath().normalize();
    if (!Files.isRegularFile(normalizedModelPath)) {
      throw new IllegalArgumentException("Merged model does not exist: " + normalizedModelPath);
    }

    if (Files.exists(normalizedOutputDirectory) && directoryHasEntries(normalizedOutputDirectory)) {
      throw new IllegalArgumentException("Output directory must be empty or absent: " + normalizedOutputDirectory);
    }
    Files.createDirectories(normalizedOutputDirectory);

    ModiscoJavaModelLoader.relaxXmlParserLimits();
    log("loading merged model... path=" + normalizedModelPath + ", size=" + humanSize(Files.size(normalizedModelPath)));

    final ResourceSet resourceSet = ModiscoJavaModelLoader.newResourceSet();
    final Model model = ModiscoJavaModelLoader.loadModel(normalizedModelPath, resourceSet);
    final Resource rootResource = model.eResource();
    if (rootResource == null) {
      throw new IllegalStateException("Merged model has no backing EMF resource");
    }

    final List<CompilationUnit> compilationUnits = sortedCompilationUnits(model.getCompilationUnits());
    final Path commonCompilationUnitPrefix = commonPrefixOfCompilationUnitPaths(compilationUnits);
    log("loaded model: name=" + model.getName() + ", packages=" + model.getOwnedElements().size()
        + ", compilationUnits=" + compilationUnits.size()
        + ", orphanTypes=" + model.getOrphanTypes().size()
        + ", unresolvedItems=" + model.getUnresolvedItems().size());
    if (commonCompilationUnitPrefix != null) {
      log("compilation-unit path prefix: " + commonCompilationUnitPrefix);
    }

    rootResource.setURI(fileUri(normalizedOutputDirectory.resolve(ROOT_FILENAME)));
    final List<String> manifestLines = new ArrayList<>();
    manifestLines.add("kind,relative_path,label,count");
    final Set<Path> usedRelativePaths = new LinkedHashSet<>();

    final Map<EObject, Resource> assignedResources = new IdentityHashMap<>();
    for (int index = 0; index < compilationUnits.size(); index++) {
      final CompilationUnit compilationUnit = compilationUnits.get(index);
      final Path relativePath = uniqueCompilationUnitResourcePath(compilationUnit, commonCompilationUnitPrefix, index + 1, usedRelativePaths);
      final Resource compilationUnitResource = resourceSet.createResource(fileUri(normalizedOutputDirectory.resolve(relativePath)));
      compilationUnitResource.getContents().add(compilationUnit);
      assignedResources.put(compilationUnit, compilationUnitResource);

      int exportedTypes = 0;
      for (final AbstractTypeDeclaration type : compilationUnit.getTypes()) {
        if (type == null || assignedResources.containsKey(type)) {
          continue;
        }
        compilationUnitResource.getContents().add(type);
        assignedResources.put(type, compilationUnitResource);
        exportedTypes++;
      }

      manifestLines.add(csvRow(
          "compilation-unit",
          relativePath,
          labelForCompilationUnit(compilationUnit),
          Integer.toString(exportedTypes + 1)));

      if (index == 0 || (index + 1) % 500 == 0) {
        log("assigned compilation units: " + (index + 1) + "/" + compilationUnits.size()
            + " current=" + relativePath);
      }
    }

    final List<AbstractTypeDeclaration> unassignedPackageTypes = collectUnassignedPackageTypes(model, assignedResources);
    if (!unassignedPackageTypes.isEmpty()) {
      final Path relativePath = Path.of("aux").resolve("unassigned-package-types.xmi");
      final Resource resource = resourceSet.createResource(fileUri(normalizedOutputDirectory.resolve(relativePath)));
      for (final AbstractTypeDeclaration type : unassignedPackageTypes) {
        resource.getContents().add(type);
        assignedResources.put(type, resource);
      }
      manifestLines.add(csvRow("aux-package-types", relativePath, "unassigned package-owned types",
          Integer.toString(unassignedPackageTypes.size())));
      log("assigned untracked package-owned types: count=" + unassignedPackageTypes.size());
    }

    writeAuxResource(resourceSet, normalizedOutputDirectory, manifestLines, "aux", "orphan-types.xmi", "orphan-types", "orphan types", model.getOrphanTypes());
    writeAuxResource(resourceSet, normalizedOutputDirectory, manifestLines, "aux", "unresolved-items.xmi", "unresolved-items", "unresolved items", model.getUnresolvedItems());
    writeAuxResource(resourceSet, normalizedOutputDirectory, manifestLines, "aux", "class-files.xmi", "class-files", "class files", model.getClassFiles());
    writeAuxResource(resourceSet, normalizedOutputDirectory, manifestLines, "aux", "archives.xmi", "archives", "archives", model.getArchives());

    log("saving " + resourceSet.getResources().size() + " EMF resources...");
    saveAllResources(resourceSet);

    final ModiscoComponentManifestGenerator.ManifestGenerationResult manifestResult =
        new ModiscoComponentManifestGenerator().generateForLoadedPartitionedModel(
            normalizedOutputDirectory.resolve(ROOT_FILENAME),
            resourceSet,
            model);

    Files.write(normalizedOutputDirectory.resolve("ROOT_MODEL.txt"), List.of(ROOT_FILENAME), StandardCharsets.UTF_8);
    Files.write(normalizedOutputDirectory.resolve("resources-manifest.csv"), manifestLines, StandardCharsets.UTF_8);
    Files.write(
        normalizedOutputDirectory.resolve("README.md"),
        readmeLines(model, manifestLines.size() - 1, manifestResult.componentCount()),
        StandardCharsets.UTF_8);

    final PartitionResult result = new PartitionResult(
        normalizedOutputDirectory.resolve(ROOT_FILENAME),
        compilationUnits.size(),
        manifestLines.size() - 1,
        resourceSet.getResources().size());
    log("done: rootModel=" + result.rootModelPath()
        + ", compilationUnitResources=" + result.compilationUnitResources()
        + ", manifestEntries=" + result.manifestEntries()
        + ", totalResources=" + result.totalResources());
    return result;
  }

  private static void writeAuxResource(
      final ResourceSet resourceSet,
      final Path outputDirectory,
      final List<String> manifestLines,
      final String directory,
      final String filename,
      final String kind,
      final String label,
      final Collection<? extends EObject> contents) {
    if (contents.isEmpty()) {
      return;
    }

    final Path relativePath = Path.of(directory).resolve(filename);
    final Resource resource = resourceSet.createResource(fileUri(outputDirectory.resolve(relativePath)));
    for (final EObject object : contents) {
      resource.getContents().add(object);
    }
    manifestLines.add(csvRow(kind, relativePath, label, Integer.toString(contents.size())));
  }

  private static List<String> readmeLines(final Model model, final int manifestEntries, final int componentCount) {
    return List.of(
        "# Compilation-Unit Partitioned MoDisco Model",
        "",
        "- Root model: `" + ROOT_FILENAME + "`",
        "- Model name: `" + model.getName() + "`",
        "- Resource entries: `" + manifestEntries + "`",
        "- Plugin/module components: `" + componentCount + "`",
        "- Layout: one authoritative root/package resource plus one EMF resource per compilation unit",
        "- Publication manifests: `version-manifest.json` at the root plus one `component-manifest.json` per plugin/module directory",
        "",
        "See `resources-manifest.csv` for the emitted resource list and `version-manifest.json` for the project-level manifest."
    );
  }

  private static void saveAllResources(final ResourceSet resourceSet) throws IOException {
    final List<Resource> resources = new ArrayList<>(resourceSet.getResources());
    resources.sort(Comparator.comparing(resource -> resource.getURI().toString()));

    final Map<Object, Object> saveOptions = new HashMap<>();
    saveOptions.put(XMLResource.OPTION_SCHEMA_LOCATION, Boolean.TRUE);
    saveOptions.put(XMLResource.OPTION_PROCESS_DANGLING_HREF, XMLResource.OPTION_PROCESS_DANGLING_HREF_DISCARD);

    for (final Resource resource : resources) {
      final URI uri = resource.getURI();
      if (uri != null && uri.isFile()) {
        Files.createDirectories(Path.of(uri.toFileString()).getParent());
      }
      resource.save(saveOptions);
    }
  }

  private static List<AbstractTypeDeclaration> collectUnassignedPackageTypes(
      final Model model,
      final Map<EObject, Resource> assignedResources) {
    final List<AbstractTypeDeclaration> leftovers = new ArrayList<>();
    for (final Package rootPackage : model.getOwnedElements()) {
      collectUnassignedPackageTypes(rootPackage, assignedResources, leftovers);
    }
    leftovers.sort(Comparator.comparing(ModiscoCompilationUnitPartitioner::labelForType));
    return leftovers;
  }

  private static void collectUnassignedPackageTypes(
      final Package current,
      final Map<EObject, Resource> assignedResources,
      final List<AbstractTypeDeclaration> leftovers) {
    for (final AbstractTypeDeclaration type : current.getOwnedElements()) {
      if (!assignedResources.containsKey(type)) {
        leftovers.add(type);
      }
    }
    for (final Package child : current.getOwnedPackages()) {
      collectUnassignedPackageTypes(child, assignedResources, leftovers);
    }
  }

  private static List<CompilationUnit> sortedCompilationUnits(final List<CompilationUnit> units) {
    final List<CompilationUnit> sorted = new ArrayList<>(units);
    sorted.sort(Comparator.comparing(ModiscoCompilationUnitPartitioner::compilationUnitSortKey));
    return sorted;
  }

  private static String compilationUnitSortKey(final CompilationUnit compilationUnit) {
    final String originalFilePath = normalize(compilationUnit.getOriginalFilePath());
    if (originalFilePath != null) {
      return originalFilePath;
    }
    return normalize(compilationUnit.getName()) == null ? "" : compilationUnit.getName();
  }

  private static Path commonPrefixOfCompilationUnitPaths(final List<CompilationUnit> units) {
    Path common = null;
    for (final CompilationUnit unit : units) {
      final String originalFilePath = normalize(unit.getOriginalFilePath());
      if (originalFilePath == null) {
        continue;
      }

      final Path current = Paths.get(originalFilePath).toAbsolutePath().normalize();
      if (common == null) {
        common = current.getParent();
      } else {
        common = commonPath(common, current.getParent());
      }
      if (common == null) {
        return null;
      }
    }
    return common;
  }

  private static Path commonPath(final Path left, final Path right) {
    if (left == null || right == null) {
      return null;
    }
    if (left.getRoot() == null ? right.getRoot() != null : !left.getRoot().equals(right.getRoot())) {
      return null;
    }

    final int max = Math.min(left.getNameCount(), right.getNameCount());
    Path common = left.getRoot();
    for (int index = 0; index < max; index++) {
      if (!left.getName(index).equals(right.getName(index))) {
        break;
      }
      common = common == null ? left.getName(index) : common.resolve(left.getName(index));
    }
    return common;
  }

  private static Path uniqueCompilationUnitResourcePath(
      final CompilationUnit compilationUnit,
      final Path commonPrefix,
      final int seed,
      final Set<Path> usedRelativePaths) {
    Path candidate = compilationUnitResourcePath(compilationUnit, commonPrefix, seed);
    int duplicateIndex = 1;
    while (usedRelativePaths.contains(candidate)) {
      candidate = appendSuffix(candidate, duplicateIndex++);
    }
    usedRelativePaths.add(candidate);
    return candidate;
  }

  private static Path appendSuffix(final Path candidate, final int index) {
    final String filename = candidate.getFileName().toString();
    final int extensionIndex = filename.lastIndexOf('.');
    final String base = extensionIndex >= 0 ? filename.substring(0, extensionIndex) : filename;
    final String extension = extensionIndex >= 0 ? filename.substring(extensionIndex) : "";
    return candidate.getParent().resolve(base + "__" + index + extension);
  }

  private static Path compilationUnitResourcePath(
      final CompilationUnit compilationUnit,
      final Path commonPrefix,
      final int fallbackIndex) {
    final String originalFilePath = normalize(compilationUnit.getOriginalFilePath());
    if (originalFilePath != null) {
      final Path originalPath = Paths.get(originalFilePath).toAbsolutePath().normalize();
      Path relativePath = originalPath.getRoot() == null ? originalPath : originalPath.subpath(0, originalPath.getNameCount());
      if (commonPrefix != null && originalPath.startsWith(commonPrefix)) {
        relativePath = commonPrefix.relativize(originalPath);
      }
      final Path normalizedRelativePath = replaceExtension(relativePath, "xmi");
      return Path.of("compilation-units").resolve(sanitizeRelativePath(normalizedRelativePath));
    }

    final String packageName = normalize(compilationUnit.getPackage() == null ? null : compilationUnit.getPackage().getName());
    final String unitName = normalize(compilationUnit.getName());
    final Path fallback = packageName == null
        ? Path.of("compilation-unit-" + fallbackIndex + ".xmi")
        : Path.of(packageName.replace('.', '/')).resolve(unitName == null ? "compilation-unit-" + fallbackIndex + ".xmi" : unitName + ".xmi");
    return Path.of("compilation-units").resolve(sanitizeRelativePath(fallback));
  }

  private static Path sanitizeRelativePath(final Path candidate) {
    Path sanitized = Path.of("");
    for (final Path segment : candidate) {
      String value = segment.toString();
      value = value.replace(':', '_');
      if (value.isBlank()) {
        value = "_";
      }
      sanitized = sanitized.resolve(value);
    }
    return sanitized;
  }

  private static Path replaceExtension(final Path path, final String newExtension) {
    final Path parent = path.getParent();
    final String fileName = path.getFileName().toString();
    final int extensionIndex = fileName.lastIndexOf('.');
    final String baseName = extensionIndex >= 0 ? fileName.substring(0, extensionIndex) : fileName;
    final String newName = baseName + "." + newExtension;
    return parent == null ? Path.of(newName) : parent.resolve(newName);
  }

  private static String labelForCompilationUnit(final CompilationUnit compilationUnit) {
    final String filePath = normalize(compilationUnit.getOriginalFilePath());
    if (filePath != null) {
      return filePath;
    }
    final String name = normalize(compilationUnit.getName());
    return name == null ? "compilation-unit" : name;
  }

  private static String labelForType(final AbstractTypeDeclaration type) {
    final String name = normalize(type.getName());
    return name == null ? type.eClass().getName() : name;
  }

  private static String csvRow(final String kind, final Path relativePath, final String label, final String count) {
    return csv(kind) + "," + csv(relativePath.toString()) + "," + csv(label) + "," + csv(count);
  }

  private static String csv(final String value) {
    if (value == null) {
      return "";
    }
    final String escaped = value.replace("\"", "\"\"");
    return "\"" + escaped + "\"";
  }

  private static URI fileUri(final Path path) {
    return URI.createFileURI(path.toAbsolutePath().normalize().toString());
  }

  private static boolean directoryHasEntries(final Path directory) throws IOException {
    if (!Files.isDirectory(directory)) {
      return false;
    }
    try (var entries = Files.list(directory)) {
      return entries.findAny().isPresent();
    }
  }

  private static String normalize(final String value) {
    if (value == null) {
      return null;
    }
    final String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  private static void log(final String message) {
    System.out.println("[cu-partitioner] " + message);
    System.out.flush();
  }

  private static String humanSize(final long bytes) {
    final double kib = 1024.0;
    final double mib = kib * 1024.0;
    final double gib = mib * 1024.0;

    if (bytes >= gib) {
      return String.format("%.2f GiB (%d bytes)", bytes / gib, bytes);
    }
    if (bytes >= mib) {
      return String.format("%.2f MiB (%d bytes)", bytes / mib, bytes);
    }
    if (bytes >= kib) {
      return String.format("%.2f KiB (%d bytes)", bytes / kib, bytes);
    }
    return bytes + " bytes";
  }

  public record PartitionResult(
      Path rootModelPath,
      int compilationUnitResources,
      int manifestEntries,
      int totalResources) {
  }
}
