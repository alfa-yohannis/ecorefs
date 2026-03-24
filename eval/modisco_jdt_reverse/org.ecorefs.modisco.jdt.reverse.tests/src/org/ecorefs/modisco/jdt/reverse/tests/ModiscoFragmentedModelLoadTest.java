package org.ecorefs.modisco.jdt.reverse.tests;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.modisco.java.AbstractTypeDeclaration;
import org.eclipse.modisco.java.CompilationUnit;
import org.eclipse.modisco.java.Model;
import org.eclipse.modisco.java.emf.JavaPackage;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;

public class ModiscoFragmentedModelLoadTest {

  @Test
  public void loadsFragmentedRootAndNavigatesIntoFragments() throws Exception {
    final Path rootModelPath = configuredRootModelPath();
    log("root model: " + rootModelPath);
    final Path parentDirectory = rootModelPath.getParent();
    final long compilationUnitFragmentCount = countFragmentFiles(parentDirectory.resolve("compilation-units"));
    final long auxiliaryFragmentCount = countFragmentFiles(parentDirectory.resolve("aux"));
    final long fragmentCount = compilationUnitFragmentCount + auxiliaryFragmentCount;
    log("loading fragmented root model... rootSize=" + humanSize(Files.size(rootModelPath))
        + ", compilationUnitFragments=" + compilationUnitFragmentCount
        + ", auxiliaryFragments=" + auxiliaryFragmentCount
        + ", this may take some time");
    Assert.assertTrue("Expected partition resources next to the root model", fragmentCount > 0);

    final ResourceSet resourceSet = newResourceSet();
    final Resource resource = resourceSet.getResource(URI.createFileURI(rootModelPath.toString()), true);
    Assert.assertNotNull(resource);
    Assert.assertEquals(1, resource.getContents().size());

    final EObject root = resource.getContents().get(0);
    Assert.assertTrue(root instanceof Model);

    final Model javaModel = (Model) root;
    Assert.assertEquals("all-repos-merged", javaModel.getName());
    Assert.assertFalse(javaModel.eContents().isEmpty());
    log("root loaded: name=" + javaModel.getName() + ", topLevelEntries=" + javaModel.eContents().size());

    final CompilationUnit sampleUnit = firstCompilationUnitWithTypes(javaModel, resourceSet, 2_000);
    Assert.assertNotNull("Expected at least one compilation-unit fragment", sampleUnit);
    Assert.assertNotNull(sampleUnit.eResource());
    Assert.assertNotNull(sampleUnit.eResource().getURI());
    Assert.assertNotEquals("Expected the sample compilation unit to come from a fragment resource",
        resource.getURI(),
        sampleUnit.eResource().getURI());
    log("sample compilation unit: " + describe(sampleUnit));

    final AbstractTypeDeclaration sampleType = firstResolvedTopLevelType(sampleUnit, resourceSet);
    Assert.assertNotNull("Expected a top-level declaration inside the compilation-unit fragment", sampleType);
    Assert.assertNotNull(sampleType.eResource());
    Assert.assertEquals("Expected the top-level declaration to live in the same fragment resource as its compilation unit",
        sampleUnit.eResource(),
        sampleType.eResource());
    log("sample top-level type: " + describe(sampleType));

    final TraversalStats fragmentTraversal = traverseSubElements(sampleType, resourceSet, 500, 50);
    Assert.assertTrue("Expected to visit descendant sub-elements in the top-level type within the compilation-unit fragment",
        fragmentTraversal.visitedDescendants() >= 50);
    Assert.assertTrue("Expected at least one descendant cross-reference in the compilation-unit fragment",
        fragmentTraversal.encounteredCrossReferences() >= 1);
    Assert.assertTrue("Expected additional fragment resources to be loaded", resourceSet.getResources().size() > 1);
    log("fragment done: visitedDescendants=" + fragmentTraversal.visitedDescendants()
        + ", encounteredCrossReferences=" + fragmentTraversal.encounteredCrossReferences()
        + ", resolvedCrossReferences=" + fragmentTraversal.resolvedCrossReferences()
        + ", loadedResources=" + resourceSet.getResources().size());
  }

  private static Path configuredRootModelPath() {
    final String rootProperty = System.getProperty("modisco.fragmented.root");
    Assume.assumeTrue(rootProperty != null && !rootProperty.isBlank());

    final Path rootModelPath = Path.of(rootProperty).toAbsolutePath().normalize();
    Assert.assertTrue(Files.exists(rootModelPath));
    return rootModelPath;
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

  private static CompilationUnit firstCompilationUnitWithTypes(
      final Model model,
      final ResourceSet resourceSet,
      final int maxScan) {
    int index = 0;
    for (CompilationUnit compilationUnit : model.getCompilationUnits()) {
      if (index == 0 || index % 500 == 0) {
        log("compilationUnitScan index=" + index);
      }
      if (compilationUnit.eIsProxy()) {
        compilationUnit = (CompilationUnit) EcoreUtil.resolve(compilationUnit, resourceSet);
      }
      if (compilationUnit != null && !compilationUnit.eIsProxy() && !compilationUnit.getTypes().isEmpty()) {
        return compilationUnit;
      }
      index++;
      if (index >= maxScan) {
        break;
      }
    }
    return null;
  }

  private static AbstractTypeDeclaration firstResolvedTopLevelType(
      final CompilationUnit compilationUnit,
      final ResourceSet resourceSet) {
    for (AbstractTypeDeclaration type : compilationUnit.getTypes()) {
      if (type.eIsProxy()) {
        type = (AbstractTypeDeclaration) EcoreUtil.resolve(type, resourceSet);
      }
      if (type != null && !type.eIsProxy()) {
        return type;
      }
    }
    return null;
  }

  private static TraversalStats traverseSubElements(
      final EObject root,
      final ResourceSet resourceSet,
      final int maxDescendants,
      final int maxResolvedCrossReferences) {
    final TreeIterator<EObject> contents = root.eAllContents();
    int visitedDescendants = 0;
    int encounteredCrossReferences = 0;
    int resolvedCrossReferences = 0;

    while (contents.hasNext() && visitedDescendants < maxDescendants) {
      final EObject candidate = contents.next();
      visitedDescendants++;

      Assert.assertNotNull(candidate.eResource());
      Assert.assertNotNull(candidate.eClass().getName());
      Assert.assertFalse(candidate.eClass().getName().isBlank());
      if (visitedDescendants == 1 || visitedDescendants % 100 == 0) {
        log("  descendantsVisited=" + visitedDescendants + " current=" + describe(candidate));
      }

      if (resolvedCrossReferences >= maxResolvedCrossReferences) {
        continue;
      }

      for (EObject referenced : candidate.eCrossReferences()) {
        encounteredCrossReferences++;
        if (referenced.eIsProxy()) {
          referenced = EcoreUtil.resolve(referenced, resourceSet);
        }

        Assert.assertNotNull(referenced);
        if (!referenced.eIsProxy()) {
          Assert.assertNotNull(referenced.eResource());
          Assert.assertNotNull(referenced.eClass().getName());
          Assert.assertFalse(referenced.eClass().getName().isBlank());

          resolvedCrossReferences++;
          if (resolvedCrossReferences >= maxResolvedCrossReferences) {
            break;
          }
        }
      }
    }

    return new TraversalStats(visitedDescendants, encounteredCrossReferences, resolvedCrossReferences);
  }

  private static String describe(final EObject object) {
    final StringBuilder description = new StringBuilder(object.eClass().getName());
    final EStructuralFeature nameFeature = object.eClass().getEStructuralFeature("name");
    if (nameFeature != null) {
      final Object nameValue = object.eGet(nameFeature);
      if (nameValue instanceof String && !((String) nameValue).isBlank()) {
        description.append("[name=").append(nameValue).append("]");
      }
    }
    if (object.eResource() != null && object.eResource().getURI() != null) {
      description.append(" @ ").append(object.eResource().getURI());
    }
    return description.toString();
  }

  private static void log(final String message) {
    System.out.println("[fragmented-test] " + message);
    System.out.flush();
  }

  private static long countFragmentFiles(final Path fragmentsDir) throws Exception {
    if (!Files.isDirectory(fragmentsDir)) {
      return 0;
    }
    try (Stream<Path> paths = Files.walk(fragmentsDir)) {
      return paths.filter(Files::isRegularFile).count();
    }
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

  private record TraversalStats(int visitedDescendants, int encounteredCrossReferences, int resolvedCrossReferences) {
  }
}
