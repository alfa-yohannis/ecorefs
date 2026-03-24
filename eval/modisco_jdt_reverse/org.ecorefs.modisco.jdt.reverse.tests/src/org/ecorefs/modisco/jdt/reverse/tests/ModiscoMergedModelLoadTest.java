package org.ecorefs.modisco.jdt.reverse.tests;

import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
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

public class ModiscoMergedModelLoadTest {

  @Test
  public void loadsConfiguredMergedModelWhenRequested() throws Exception {
    final String mergedProperty = System.getProperty("modisco.merged");
    Assume.assumeTrue(mergedProperty != null && !mergedProperty.isBlank());

    final Path mergedModelPath = Path.of(mergedProperty).toAbsolutePath().normalize();
    Assert.assertTrue(Files.exists(mergedModelPath));
    log("merged model: " + mergedModelPath);
    log("loading merged model... size=" + humanSize(Files.size(mergedModelPath)) + ", this may take some time");

    final ResourceSet resourceSet = new ResourceSetImpl();
    resourceSet.getPackageRegistry().put(JavaPackage.eNS_URI, JavaPackage.eINSTANCE);
    resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap()
        .put(Resource.Factory.Registry.DEFAULT_EXTENSION, new XMIResourceFactoryImpl());
    resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap()
        .put("xmi", new XMIResourceFactoryImpl());

    final Resource resource = resourceSet.getResource(URI.createFileURI(mergedModelPath.toString()), true);
    Assert.assertNotNull(resource);
    Assert.assertEquals(1, resource.getContents().size());

    final EObject root = resource.getContents().get(0);
    Assert.assertTrue(root instanceof Model);

    final Model javaModel = (Model) root;
    Assert.assertEquals("all-repos-merged", javaModel.getName());
    Assert.assertFalse(javaModel.eContents().isEmpty());
    log("root loaded: name=" + javaModel.getName() + ", topLevelEntries=" + javaModel.eContents().size());

    final EObject firstDescendant = firstDescendantOf(resource);
    Assert.assertNotNull(firstDescendant);
    Assert.assertSame(resource, firstDescendant.eResource());
    Assert.assertNotNull(firstDescendant.eClass().getName());
    Assert.assertFalse(firstDescendant.eClass().getName().isBlank());
    log("first descendant: " + describe(firstDescendant));

    final EObject xrefSource = firstObjectWithCrossReference(resource, 50_000);
    Assert.assertNotNull("Expected at least one cross-reference in the merged model", xrefSource);
    log("xref source: " + describe(xrefSource));

    EObject referenced = xrefSource.eCrossReferences().get(0);
    if (referenced.eIsProxy()) {
      referenced = EcoreUtil.resolve(referenced, resourceSet);
    }

    Assert.assertNotNull(referenced);
    Assert.assertFalse("Expected the sampled cross-reference to resolve", referenced.eIsProxy());
    Assert.assertSame(resource, referenced.eResource());
    Assert.assertNotNull(referenced.eClass().getName());
    Assert.assertFalse(referenced.eClass().getName().isBlank());
    log("resolved target: " + describe(referenced));
  }

  private static EObject firstDescendantOf(final Resource resource) {
    final TreeIterator<EObject> contents = resource.getAllContents();
    return contents.hasNext() ? contents.next() : null;
  }

  private static EObject firstObjectWithCrossReference(final Resource resource, final int maxScan) {
    final TreeIterator<EObject> contents = resource.getAllContents();
    int scanned = 0;
    while (contents.hasNext() && scanned < maxScan) {
      final EObject candidate = contents.next();
      if (scanned == 0 || scanned % 10_000 == 0) {
        log("scan progress: scanned=" + scanned + " current=" + describe(candidate));
      }
      if (!candidate.eCrossReferences().isEmpty()) {
        return candidate;
      }
      scanned++;
    }
    return null;
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
    System.out.println("[merged-test] " + message);
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
}
