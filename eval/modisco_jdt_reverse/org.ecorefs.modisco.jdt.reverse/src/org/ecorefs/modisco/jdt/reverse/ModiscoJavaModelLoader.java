package org.ecorefs.modisco.jdt.reverse;

import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.modisco.java.Model;
import org.eclipse.modisco.java.emf.JavaPackage;

public final class ModiscoJavaModelLoader {

  private ModiscoJavaModelLoader() {
  }

  public static void main(final String[] args) throws Exception {
    if (args.length != 1) {
      System.err.println("Usage: ModiscoJavaModelLoader <model.xmi>");
      System.exit(2);
    }

    relaxXmlParserLimits();

    final Path modelPath = Path.of(args[0]).toAbsolutePath().normalize();
    System.out.println("loading model... path=" + modelPath + ", size=" + humanSize(Files.size(modelPath)) + ", this may take some time");
    System.out.flush();
    final ResourceSet resourceSet = newResourceSet();
    final Model model = loadModel(modelPath, resourceSet);

    System.out.println("model=" + model.getName());
    System.out.println("topLevelElements=" + model.eContents().size());

    final EObject firstTopLevel = resolveFirstTopLevel(model, resourceSet);
    if (firstTopLevel != null) {
      System.out.println("firstTopLevelType=" + firstTopLevel.eClass().getName());
      System.out.println("firstTopLevelResource=" + firstTopLevel.eResource().getURI());
    }
  }

  public static void relaxXmlParserLimits() {
    System.setProperty("jdk.xml.maxGeneralEntitySizeLimit", "0");
    System.setProperty("jdk.xml.totalEntitySizeLimit", "0");
    System.setProperty("jdk.xml.entityExpansionLimit", "0");
  }

  public static ResourceSet newResourceSet() {
    final ResourceSet resourceSet = new ResourceSetImpl();
    resourceSet.getPackageRegistry().put(JavaPackage.eNS_URI, JavaPackage.eINSTANCE);
    resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap()
        .put(Resource.Factory.Registry.DEFAULT_EXTENSION, new XMIResourceFactoryImpl());
    resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap()
        .put("xmi", new XMIResourceFactoryImpl());
    return resourceSet;
  }

  public static Model loadModel(final Path modelPath, final ResourceSet resourceSet) {
    if (modelPath == null) {
      throw new IllegalArgumentException("modelPath must not be null");
    }
    if (resourceSet == null) {
      throw new IllegalArgumentException("resourceSet must not be null");
    }

    final Path normalizedPath = modelPath.toAbsolutePath().normalize();
    if (!Files.exists(normalizedPath)) {
      throw new IllegalArgumentException("Model path does not exist: " + normalizedPath);
    }

    final Resource resource = resourceSet.getResource(URI.createFileURI(normalizedPath.toString()), true);
    if (resource == null || resource.getContents().isEmpty()) {
      throw new IllegalStateException("No root object was loaded from " + normalizedPath);
    }

    final EObject root = resource.getContents().get(0);
    if (!(root instanceof Model)) {
      throw new IllegalStateException("Expected a MoDisco java:Model root but got " + root.eClass().getName());
    }

    return (Model) root;
  }

  public static EObject resolveFirstTopLevel(final Model model, final ResourceSet resourceSet) {
    if (model == null) {
      throw new IllegalArgumentException("model must not be null");
    }
    if (resourceSet == null) {
      throw new IllegalArgumentException("resourceSet must not be null");
    }
    if (model.eContents().isEmpty()) {
      return null;
    }

    EObject firstTopLevel = model.eContents().get(0);
    if (firstTopLevel.eIsProxy()) {
      firstTopLevel = EcoreUtil.resolve(firstTopLevel, resourceSet);
    }
    return firstTopLevel;
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
