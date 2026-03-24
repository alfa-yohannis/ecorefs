package org.ecorefs.modisco.jdt.reverse.tests;

import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.modisco.java.Model;
import org.eclipse.modisco.java.emf.JavaPackage;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;

public class ModiscoTraversalBenchmarkTest {

  @Test
  public void benchmarksMergedAndFragmentedTraversalWhenConfigured() throws Exception {
    final Path mergedModelPath = configuredPath("modisco.merged");
    final Path fragmentedRootPath = configuredPath("modisco.fragmented.root");

    final TraversalRun merged = benchmark("merged", mergedModelPath);
    final TraversalRun fragmented = benchmark("fragmented", fragmentedRootPath);

    log("comparison: mergedElements=" + merged.elementCount()
        + ", fragmentedElements=" + fragmented.elementCount()
        + ", mergedLoadMs=" + merged.loadMillis()
        + ", fragmentedLoadMs=" + fragmented.loadMillis()
        + ", mergedTraverseMs=" + merged.traverseMillis()
        + ", fragmentedTraverseMs=" + fragmented.traverseMillis());

    Assert.assertEquals("Merged and partitioned resource-set traversals should expose the same graph size",
        merged.elementCount(), fragmented.elementCount());
  }

  private static TraversalRun benchmark(final String label, final Path modelPath) throws Exception {
    log(label + ": loading model " + modelPath + " size=" + humanSize(Files.size(modelPath)));

    final ResourceSet resourceSet = newResourceSet();
    final long loadStart = System.nanoTime();
    final Resource resource = resourceSet.getResource(URI.createFileURI(modelPath.toString()), true);
    final long loadEnd = System.nanoTime();

    Assert.assertNotNull(resource);
    Assert.assertEquals(1, resource.getContents().size());
    Assert.assertTrue(resource.getContents().get(0) instanceof Model);

    final Model model = (Model) resource.getContents().get(0);
    final int extraResources = ModiscoModelTestSupport.loadAdditionalPartitionResources(modelPath, resourceSet);
    log(label + ": load done topLevelEntries=" + model.eContents().size()
        + ", loadedResources=" + resourceSet.getResources().size()
        + ", extraResources=" + extraResources
        + ", loadMs=" + nanosToMillis(loadEnd - loadStart));

    final long traverseStart = System.nanoTime();
    final long elementCount = ModiscoModelTestSupport.countUniqueElements(resourceSet);
    final long traverseEnd = System.nanoTime();

    final TraversalRun run = new TraversalRun(
        label,
        elementCount,
        nanosToMillis(loadEnd - loadStart),
        nanosToMillis(traverseEnd - traverseStart),
        resourceSet.getResources().size());

    log(label + ": traversal done elements=" + run.elementCount()
        + ", traverseMs=" + run.traverseMillis()
        + ", loadedResources=" + run.loadedResources());
    return run;
  }

  private static Path configuredPath(final String propertyName) {
    final String value = System.getProperty(propertyName);
    Assume.assumeTrue(value != null && !value.isBlank());

    final Path path = Path.of(value).toAbsolutePath().normalize();
    Assert.assertTrue(Files.exists(path));
    return path;
  }

  private static ResourceSet newResourceSet() {
    final ResourceSet resourceSet = new ResourceSetImpl();
    resourceSet.getPackageRegistry().put(JavaPackage.eNS_URI, JavaPackage.eINSTANCE);
    resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap()
        .put(Resource.Factory.Registry.DEFAULT_EXTENSION, new XMIResourceFactoryImpl());
    resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap()
        .put("xmi", new XMIResourceFactoryImpl());
    return resourceSet;
  }

  private static long nanosToMillis(final long nanos) {
    return nanos / 1_000_000L;
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

  private static void log(final String message) {
    System.out.println("[traversal-bench] " + message);
    System.out.flush();
  }

  private record TraversalRun(
      String label,
      long elementCount,
      long loadMillis,
      long traverseMillis,
      int loadedResources) {
  }
}
