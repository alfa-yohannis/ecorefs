package org.ecorefs.modisco.jdt.reverse.tests;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.ecorefs.modisco.jdt.reverse.ModiscoJavaModelExporter;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;

public class ModiscoJavaModelExporterExternalRepoTest {

  @Test
  public void exportsConfiguredRepositoryWhenRequested() throws Exception {
    final String repositoryProperty = System.getProperty("modisco.repo");
    Assume.assumeTrue(repositoryProperty != null && !repositoryProperty.isBlank());

    final Path repositoryRoot = Path.of(repositoryProperty).toAbsolutePath().normalize();
    final String outputProperty = System.getProperty("modisco.output");
    final Path outputDirectory = outputProperty == null || outputProperty.isBlank()
        ? repositoryRoot.resolve("modisco-java-models")
        : Path.of(outputProperty).toAbsolutePath().normalize();
    final String projectFilter = System.getProperty("modisco.project");

    final List<Path> exportedModels = new ModiscoJavaModelExporter().exportJavaProjects(
        repositoryRoot,
        outputDirectory,
        projectFilter,
        new NullProgressMonitor());

    Assert.assertFalse(exportedModels.isEmpty());
    for (final Path exportedModel : exportedModels) {
      Assert.assertTrue(Files.exists(exportedModel));
    }
  }
}
