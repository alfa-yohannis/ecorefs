package org.ecorefs.modisco.jdt.reverse.tests;

import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.modisco.java.Model;
import org.eclipse.modisco.java.Package;
import org.eclipse.modisco.java.emf.JavaFactory;
import org.eclipse.modisco.java.emf.JavaPackage;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;

public class EmfCrossResourceContainmentProbeTest {

  @Test
  public void printsHowEmfSerializesDetachedAndContainedRoots() throws Exception {
    final Path tempDir = Files.createTempDirectory("modisco-cross-resource-probe");

    final ResourceSet resourceSet = new ResourceSetImpl();
    resourceSet.getPackageRegistry().put(JavaPackage.eNS_URI, JavaPackage.eINSTANCE);
    resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap()
        .put(Resource.Factory.Registry.DEFAULT_EXTENSION, new XMIResourceFactoryImpl());
    resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap()
        .put("xmi", new XMIResourceFactoryImpl());

    final Model model = JavaFactory.eINSTANCE.createModel();
    model.setName("probe");

    final Package rootPackage = JavaFactory.eINSTANCE.createPackage();
    rootPackage.setName("org");
    model.getOwnedElements().add(rootPackage);

    final Package childPackage = JavaFactory.eINSTANCE.createPackage();
    childPackage.setName("example");
    rootPackage.getOwnedPackages().add(childPackage);

    final Resource rootResource = resourceSet.createResource(URI.createFileURI(tempDir.resolve("root.xmi").toString()));
    rootResource.getContents().add(model);

    final Resource packageResource = resourceSet.createResource(URI.createFileURI(tempDir.resolve("package.xmi").toString()));
    packageResource.getContents().add(rootPackage);

    rootResource.save(null);
    packageResource.save(null);

    final String rootXml = Files.readString(tempDir.resolve("root.xmi"));
    final String packageXml = Files.readString(tempDir.resolve("package.xmi"));

    System.out.println("[cross-resource-probe] rootPackage.eContainer=" + rootPackage.eContainer());
    System.out.println("[cross-resource-probe] rootResource contents=" + rootResource.getContents().size());
    System.out.println("[cross-resource-probe] packageResource contents=" + packageResource.getContents().size());
    System.out.println("[cross-resource-probe] root.xmi=" + rootXml);
    System.out.println("[cross-resource-probe] package.xmi=" + packageXml);
    System.out.flush();

    Assert.assertTrue(Files.exists(tempDir.resolve("root.xmi")));
    Assert.assertTrue(Files.exists(tempDir.resolve("package.xmi")));
  }

  @Test
  public void probesStandaloneFragmentLoadWhenConfigured() throws Exception {
    final String fragmentPathValue = System.getProperty("modisco.fragment.probe");
    Assume.assumeTrue(fragmentPathValue != null && !fragmentPathValue.isBlank());

    final Path fragmentPath = Path.of(fragmentPathValue).toAbsolutePath().normalize();
    Assert.assertTrue(Files.exists(fragmentPath));

    final ResourceSet resourceSet = new ResourceSetImpl();
    resourceSet.getPackageRegistry().put(JavaPackage.eNS_URI, JavaPackage.eINSTANCE);
    resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap()
        .put(Resource.Factory.Registry.DEFAULT_EXTENSION, new XMIResourceFactoryImpl());
    resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap()
        .put("xmi", new XMIResourceFactoryImpl());

    final Resource resource = resourceSet.getResource(URI.createFileURI(fragmentPath.toString()), true);
    Assert.assertEquals(1, resource.getContents().size());

    final EObject root = resource.getContents().get(0);
    long count = 1;
    final TreeIterator<EObject> contents = root.eAllContents();
    while (contents.hasNext()) {
      contents.next();
      count++;
    }

    System.out.println("[cross-resource-probe] fragment=" + fragmentPath);
    System.out.println("[cross-resource-probe] rootType=" + root.eClass().getName());
    System.out.println("[cross-resource-probe] descendants=" + (count - 1));
    System.out.println("[cross-resource-probe] totalWithRoot=" + count);
    System.out.flush();
  }
}
