package org.ecorefs.modisco.jdt.reverse.tests;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.ecorefs.modisco.jdt.reverse.ModiscoJavaModelLoader;

final class ModiscoModelTestSupport {

  private ModiscoModelTestSupport() {
  }

  static ResourceSet newResourceSet() {
    return ModiscoJavaModelLoader.newResourceSet();
  }

  static int loadAdditionalPartitionResources(final Path rootModelPath, final ResourceSet resourceSet) throws Exception {
    final Path parent = rootModelPath.getParent();
    if (parent == null) {
      return 0;
    }

    final List<Path> additionalResources = new ArrayList<>();
    collectXmis(parent.resolve("compilation-units"), additionalResources);
    collectXmis(parent.resolve("aux"), additionalResources);
    additionalResources.sort(Comparator.naturalOrder());

    for (final Path resourcePath : additionalResources) {
      resourceSet.getResource(URI.createFileURI(resourcePath.toAbsolutePath().normalize().toString()), true);
    }
    return additionalResources.size();
  }

  static int loadComponentPartitionResources(
      final Path rootModelPath,
      final ResourceSet resourceSet,
      final String componentId) throws Exception {
    final Path parent = rootModelPath.getParent();
    if (parent == null) {
      return 0;
    }

    final List<Path> componentResources = new ArrayList<>();
    collectXmis(parent.resolve("aux"), componentResources);
    collectXmis(parent.resolve("compilation-units").resolve(componentId), componentResources);
    componentResources.sort(Comparator.naturalOrder());

    for (final Path resourcePath : componentResources) {
      resourceSet.getResource(URI.createFileURI(resourcePath.toAbsolutePath().normalize().toString()), true);
    }
    return componentResources.size();
  }

  static long countUniqueElements(final ResourceSet resourceSet) {
    EcoreUtil.resolveAll(resourceSet);
    final Set<String> visitedUris = new LinkedHashSet<>();
    long count = 0;
    for (final Resource resource : resourceSet.getResources()) {
      for (final EObject root : resource.getContents()) {
        count += countUnique(root, visitedUris);
      }
    }
    return count;
  }

  private static long countUnique(final EObject root, final Set<String> visitedUris) {
    long count = visitedUris.add(logicalUri(root)) ? 1 : 0;
    final TreeIterator<EObject> contents = root.eAllContents();
    while (contents.hasNext()) {
      final EObject next = contents.next();
      if (visitedUris.add(logicalUri(next))) {
        count++;
      }
    }
    return count;
  }

  private static String logicalUri(final EObject object) {
    return EcoreUtil.getURI(object).toString();
  }

  private static void collectXmis(final Path directory, final List<Path> sink) throws Exception {
    if (!Files.isDirectory(directory)) {
      return;
    }
    try (Stream<Path> paths = Files.walk(directory)) {
      paths.filter(Files::isRegularFile)
          .filter(path -> path.getFileName().toString().endsWith(".xmi"))
          .forEach(sink::add);
    }
  }
}
