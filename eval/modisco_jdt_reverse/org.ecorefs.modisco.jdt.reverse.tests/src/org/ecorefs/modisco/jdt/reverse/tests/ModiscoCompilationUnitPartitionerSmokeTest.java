package org.ecorefs.modisco.jdt.reverse.tests;

import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.ecorefs.modisco.jdt.reverse.ModiscoCompilationUnitPartitioner;
import org.ecorefs.modisco.jdt.reverse.ModiscoCompilationUnitPartitioner.PartitionResult;
import org.ecorefs.modisco.jdt.reverse.ModiscoJavaModelExporter;
import org.ecorefs.modisco.jdt.reverse.ModiscoJavaModelLoader;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

public class ModiscoCompilationUnitPartitionerSmokeTest {

  private static final String SAMPLE_PROJECT_NAME = "sample-java-project";

  @Before
  @After
  public void resetWorkspaceProject() throws Exception {
    final IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(SAMPLE_PROJECT_NAME);
    if (project.exists()) {
      project.delete(true, new NullProgressMonitor());
    }
  }

  @Test
  public void partitionsBundledSampleModelByCompilationUnit() throws Exception {
    final Path sampleWorkspace = materializeSampleWorkspace();
    final Path exportedModelDirectory = Files.createTempDirectory("modisco-partitioner-source");
    final List<Path> exportedModels = new ModiscoJavaModelExporter().exportJavaProjects(
        sampleWorkspace,
        exportedModelDirectory,
        SAMPLE_PROJECT_NAME,
        new NullProgressMonitor());
    Assert.assertEquals(1, exportedModels.size());

    final Path mergedModelPath = exportedModels.get(0);
    final Path outputDirectory = Files.createTempDirectory("modisco-cu-partitioned-parent").resolve("partitioned");
    final PartitionResult result = new ModiscoCompilationUnitPartitioner().partitionByCompilationUnit(
        mergedModelPath,
        outputDirectory);

    Assert.assertTrue(Files.exists(result.rootModelPath()));
    Assert.assertTrue(Files.exists(outputDirectory.resolve("ROOT_MODEL.txt")));
    Assert.assertTrue(Files.exists(outputDirectory.resolve("resources-manifest.csv")));
    Assert.assertTrue(Files.exists(outputDirectory.resolve("version-manifest.json")));
    Assert.assertTrue(Files.exists(outputDirectory.resolve("component-summary.csv")));
    Assert.assertTrue(Files.exists(outputDirectory.resolve("component-dependencies.csv")));
    Assert.assertTrue(result.compilationUnitResources() >= 1);

    final ResourceSet mergedResourceSet = ModiscoModelTestSupport.newResourceSet();
    ModiscoJavaModelLoader.loadModel(mergedModelPath, mergedResourceSet);
    final long mergedElements = ModiscoModelTestSupport.countUniqueElements(mergedResourceSet);

    final ResourceSet partitionedResourceSet = ModiscoModelTestSupport.newResourceSet();
    ModiscoJavaModelLoader.loadModel(result.rootModelPath(), partitionedResourceSet);
    final int loadedExtraResources = ModiscoModelTestSupport.loadAdditionalPartitionResources(result.rootModelPath(), partitionedResourceSet);
    final long partitionedElements = ModiscoModelTestSupport.countUniqueElements(partitionedResourceSet);

    Assert.assertTrue(loadedExtraResources >= 1);
    Assert.assertEquals(mergedElements, partitionedElements);
  }

  private Path materializeSampleWorkspace() throws Exception {
    final Path templateRoot = resolveBundlePath("sample-workspace-template");
    final Path workspaceRoot = Files.createTempDirectory("modisco-partitioner-workspace");

    Files.walkFileTree(templateRoot, new SimpleFileVisitor<>() {
      @Override
      public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs)
          throws java.io.IOException {
        final Path relative = templateRoot.relativize(dir);
        Files.createDirectories(workspaceRoot.resolve(relative));
        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs)
          throws java.io.IOException {
        final Path relative = templateRoot.relativize(file);
        Path target = workspaceRoot.resolve(relative);
        if ("project-description.xml".equals(file.getFileName().toString())) {
          target = target.getParent().resolve(".project");
        } else if ("classpath.xml".equals(file.getFileName().toString())) {
          target = target.getParent().resolve(".classpath");
        }
        Files.copy(file, target, StandardCopyOption.REPLACE_EXISTING);
        return FileVisitResult.CONTINUE;
      }
    });

    return workspaceRoot;
  }

  private Path resolveBundlePath(final String bundleRelativePath) throws Exception {
    final Bundle bundle = FrameworkUtil.getBundle(getClass());
    final URL entry = bundle.getEntry(bundleRelativePath);
    return Path.of(FileLocator.toFileURL(entry).toURI());
  }
}
