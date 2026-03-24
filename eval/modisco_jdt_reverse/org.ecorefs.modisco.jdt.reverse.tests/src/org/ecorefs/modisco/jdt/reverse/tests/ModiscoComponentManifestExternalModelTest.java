package org.ecorefs.modisco.jdt.reverse.tests;

import java.nio.file.Files;
import java.nio.file.Path;

import org.ecorefs.modisco.jdt.reverse.ModiscoComponentManifestGenerator;
import org.ecorefs.modisco.jdt.reverse.ModiscoComponentManifestGenerator.ManifestGenerationResult;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;

public class ModiscoComponentManifestExternalModelTest {

  @Test
  public void generatesPluginModuleManifestsForConfiguredPartitionedModel() throws Exception {
    final String fragmentedRootProperty = System.getProperty("modisco.fragmented.root");
    Assume.assumeTrue(fragmentedRootProperty != null && !fragmentedRootProperty.isBlank());

    final Path rootModelPath = Path.of(fragmentedRootProperty).toAbsolutePath().normalize();
    final ManifestGenerationResult result = new ModiscoComponentManifestGenerator().generateForPartitionedModel(rootModelPath);

    Assert.assertTrue(Files.exists(result.versionManifestPath()));
    Assert.assertTrue(Files.exists(rootModelPath.getParent().resolve("component-summary.csv")));
    Assert.assertTrue(Files.exists(rootModelPath.getParent().resolve("component-dependencies.csv")));
    Assert.assertTrue(result.componentCount() >= 1);
  }
}
