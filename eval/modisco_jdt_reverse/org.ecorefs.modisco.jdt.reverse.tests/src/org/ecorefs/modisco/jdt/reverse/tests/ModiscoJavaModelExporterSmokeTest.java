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
import org.ecorefs.modisco.jdt.reverse.ModiscoJavaModelExporter;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

public class ModiscoJavaModelExporterSmokeTest {

  private static final String SAMPLE_PROJECT_NAME = "sample-java-project";
  private final ModiscoJavaModelExporter exporter = new ModiscoJavaModelExporter();

  @Before
  @After
  public void resetWorkspaceProject() throws Exception {
    final IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(SAMPLE_PROJECT_NAME);
    if (project.exists()) {
      project.delete(true, new NullProgressMonitor());
    }
  }

  @Test
  public void exportsModiscoModelForBundledSampleProject() throws Exception {
    final Path sampleWorkspace = materializeSampleWorkspace();
    final Path outputDirectory = Files.createTempDirectory("modisco-smoke-output");

    final List<Path> exportedModels = exporter.exportJavaProjects(
        sampleWorkspace,
        outputDirectory,
        SAMPLE_PROJECT_NAME,
        new NullProgressMonitor());

    Assert.assertEquals(1, exportedModels.size());
    Assert.assertTrue(Files.exists(exportedModels.get(0)));

    final String modelContents = Files.readString(exportedModels.get(0));
    Assert.assertTrue(modelContents.contains("name=\"Hello\""));
    Assert.assertTrue(modelContents.contains("name=\"greet\""));
  }

  private Path materializeSampleWorkspace() throws Exception {
    final Path templateRoot = resolveBundlePath("sample-workspace-template");
    final Path workspaceRoot = Files.createTempDirectory("modisco-smoke-workspace");

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
