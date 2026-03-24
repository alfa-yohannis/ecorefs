package org.ecorefs.modisco.jdt.reverse.tests;

import java.nio.file.Files;
import java.nio.file.Path;

import org.ecorefs.modisco.jdt.reverse.ModiscoCompilationUnitPartitioner;
import org.ecorefs.modisco.jdt.reverse.ModiscoCompilationUnitPartitioner.PartitionResult;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;

public class ModiscoCompilationUnitPartitionerExternalModelTest {

  @Test
  public void partitionsConfiguredMergedModelWhenRequested() throws Exception {
    final String mergedProperty = System.getProperty("modisco.merged");
    Assume.assumeTrue(mergedProperty != null && !mergedProperty.isBlank());

    final String outputProperty = System.getProperty("modisco.partition.output");
    Assume.assumeTrue(outputProperty != null && !outputProperty.isBlank());

    final Path mergedModelPath = Path.of(mergedProperty).toAbsolutePath().normalize();
    final Path outputDirectory = Path.of(outputProperty).toAbsolutePath().normalize();

    final PartitionResult result = new ModiscoCompilationUnitPartitioner().partitionByCompilationUnit(
        mergedModelPath,
        outputDirectory);

    Assert.assertTrue(Files.exists(result.rootModelPath()));
    Assert.assertTrue(Files.exists(outputDirectory.resolve("ROOT_MODEL.txt")));
    Assert.assertTrue(Files.exists(outputDirectory.resolve("resources-manifest.csv")));
    Assert.assertTrue(Files.exists(outputDirectory.resolve("version-manifest.json")));
    Assert.assertTrue(result.compilationUnitResources() >= 1);
  }
}
