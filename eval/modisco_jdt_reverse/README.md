# MoDisco JDT Reverse Engineering

This project imports Eclipse Java projects from a checkout such as `eval/datasets/eclipse.jdt.core` and exports one MoDisco Java model (`.xmi`) per imported Java project. It also includes an EMF-native partitioner that rewrites a merged MoDisco model into a small root model plus one resource per compilation unit, then emits plugin/module-level version manifests over those resources.

## What it does

- Scans a repository for Eclipse `.project` files
- Imports the discovered projects into the headless Eclipse workspace used by Tycho
- Runs MoDisco's `DiscoverJavaModelFromJavaProject` discoverer for each Java project
- Writes the resulting models to an output directory

## Layout

- `org.ecorefs.modisco.jdt.reverse`: reusable exporter + partitioner services
- `org.ecorefs.modisco.jdt.reverse.tests`: smoke test plus an opt-in integration test for a real checkout
- `run-jdt-discovery.sh`: convenience wrapper for the real JDT checkout

## Requirements

- Java 21-compatible runtime.
  On this machine, the wrapper works best with `/usr/lib/jvm/java-25-openjdk-amd64` because the packaged Java 21 install is missing `lib/ct.sym`, which the imported Eclipse workspace build touches.
- Maven 3.9+.
  This workspace includes a local copy at `/data2/projects/ecorefs/.tools/apache-maven-3.9.10/bin/mvn`.
- Network access on the first build so Tycho can resolve Eclipse + MoDisco bundles from the Eclipse release repository

## Run it against the cloned JDT repository

```bash
cd /data2/projects/ecorefs/eval/modisco_jdt_reverse
./run-jdt-discovery.sh \
  /data2/projects/ecorefs/eval/datasets/eclipse.jdt.core \
  /data2/projects/ecorefs/eval/datasets/eclipse.jdt.core/modisco-java-models \
  org.eclipse.jdt.core
```

Arguments:

1. repository root to scan for Eclipse projects
2. output directory for generated `.xmi` files
3. optional project name filter, for example `org.eclipse.jdt.core`

If the third argument is omitted, the exporter processes every imported Java project found in the checkout.

## Direct Maven invocation

```bash
export JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64
export PATH="$JAVA_HOME/bin:$PATH"
export MAVEN_OPTS="${MAVEN_OPTS:-} -Djdk.xml.maxGeneralEntitySizeLimit=0 -Djdk.xml.totalEntitySizeLimit=0 -Djdk.xml.entityExpansionLimit=0"

/data2/projects/ecorefs/.tools/apache-maven-3.9.10/bin/mvn \
  -f /data2/projects/ecorefs/eval/modisco_jdt_reverse/pom.xml \
  -Dmaven.repo.local=/data2/projects/ecorefs/.m2-repo \
  -Dmodisco.repo=/data2/projects/ecorefs/eval/datasets/eclipse.jdt.core \
  -Dmodisco.output=/data2/projects/ecorefs/eval/datasets/eclipse.jdt.core/modisco-java-models \
  -Dmodisco.project=org.eclipse.jdt.core \
  verify
```

The regular smoke test always runs. The external repository export only runs when `-Dmodisco.repo=...` is supplied.

## Partition a merged model by compilation unit

This command takes the merged model and writes an EMF-native partitioned directory. The partitioner now also generates a root `version-manifest.json` plus plugin/module `component-manifest.json` files beside the partitioned XMIs:

```bash
export JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64
export PATH="$JAVA_HOME/bin:$PATH"
export MAVEN_OPTS="${MAVEN_OPTS:-} -Djdk.xml.maxGeneralEntitySizeLimit=0 -Djdk.xml.totalEntitySizeLimit=0 -Djdk.xml.entityExpansionLimit=0"

/data2/projects/ecorefs/.tools/apache-maven-3.9.10/bin/mvn \
  -f /data2/projects/ecorefs/eval/modisco_jdt_reverse/pom.xml \
  -Dmaven.repo.local=/data2/projects/ecorefs/.m2-repo \
  -Dmodisco.merged=/data2/projects/ecorefs/eval/datasets/modisco-java-models-by-repo/all-repos-merged.xmi \
  -Dmodisco.partition.output=/data2/projects/ecorefs/eval/datasets/modisco-java-models-by-repo/all-repos-merged-by-compilation-unit \
  -Dtest=ModiscoCompilationUnitPartitionerExternalModelTest \
  verify
```

The output directory must be absent or empty before the run starts.

## Generate plugin/module version manifests for an existing partitioned model

If the partitioned directory already exists, this command regenerates the component manifests in place:

```bash
export JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64
export PATH="$JAVA_HOME/bin:$PATH"
export MAVEN_OPTS="${MAVEN_OPTS:-} -Djdk.xml.maxGeneralEntitySizeLimit=0 -Djdk.xml.totalEntitySizeLimit=0 -Djdk.xml.entityExpansionLimit=0"

/data2/projects/ecorefs/.tools/apache-maven-3.9.10/bin/mvn \
  -f /data2/projects/ecorefs/eval/modisco_jdt_reverse/pom.xml \
  -Dmaven.repo.local=/data2/projects/ecorefs/.m2-repo \
  -Dmodisco.fragmented.root=/data2/projects/ecorefs/eval/datasets/modisco-java-models-by-repo/all-repos-merged-by-compilation-unit/all-repos-merged.root.xmi \
  -Dtest=ModiscoComponentManifestExternalModelTest \
  verify
```

## Load a model from Java

The core bundle now includes [ModiscoJavaModelLoader.java](./org.ecorefs.modisco.jdt.reverse/src/org/ecorefs/modisco/jdt/reverse/ModiscoJavaModelLoader.java), which can load either the single merged model or the marked root model from the partitioned directory.

Programmatic usage:

```java
Path modelPath = Path.of("/data2/projects/ecorefs/eval/datasets/modisco-java-models-by-repo/all-repos-merged.xmi");

ResourceSet resourceSet = ModiscoJavaModelLoader.newResourceSet();
ModiscoJavaModelLoader.relaxXmlParserLimits();

Model model = ModiscoJavaModelLoader.loadModel(modelPath, resourceSet);
EObject firstTopLevel = ModiscoJavaModelLoader.resolveFirstTopLevel(model, resourceSet);
```

The `main(String[] args)` entry point in that class prints a tiny summary for the supplied model path. For the very large merged model, the XML limit relaxation is important.

## Test the loader with Maven

This command runs the verified merged-model loader test:

```bash
export JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64
export PATH="$JAVA_HOME/bin:$PATH"
export MAVEN_OPTS="${MAVEN_OPTS:-} -Djdk.xml.maxGeneralEntitySizeLimit=0 -Djdk.xml.totalEntitySizeLimit=0 -Djdk.xml.entityExpansionLimit=0"

/data2/projects/ecorefs/.tools/apache-maven-3.9.10/bin/mvn \
  -f /data2/projects/ecorefs/eval/modisco_jdt_reverse/pom.xml \
  -Dmaven.repo.local=/data2/projects/ecorefs/.m2-repo \
  -Dmodisco.merged=/data2/projects/ecorefs/eval/datasets/modisco-java-models-by-repo/all-repos-merged.xmi \
  -Dtest=ModiscoMergedModelLoadTest \
  verify
```

That test loads the model, navigates to a contained element, and resolves a sampled cross-reference.

This command runs the partitioned-directory root-model loader test:

```bash
export JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64
export PATH="$JAVA_HOME/bin:$PATH"
export MAVEN_OPTS="${MAVEN_OPTS:-} -Djdk.xml.maxGeneralEntitySizeLimit=0 -Djdk.xml.totalEntitySizeLimit=0 -Djdk.xml.entityExpansionLimit=0"

/data2/projects/ecorefs/.tools/apache-maven-3.9.10/bin/mvn \
  -f /data2/projects/ecorefs/eval/modisco_jdt_reverse/pom.xml \
  -Dmaven.repo.local=/data2/projects/ecorefs/.m2-repo \
  -Dmodisco.fragmented.root=/data2/projects/ecorefs/eval/datasets/modisco-java-models-by-repo/all-repos-merged-by-compilation-unit/all-repos-merged.root.xmi \
  -Dtest=ModiscoFragmentedModelLoadTest \
  verify
```

That test loads the marked root model from the partitioned directory, resolves a compilation-unit resource, navigates into a top-level type in that resource, and verifies descendant traversal plus cross-reference resolution.

This command benchmarks logical whole-model traversal for the merged model versus the partitioned resource set:

```bash
export JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64
export PATH="$JAVA_HOME/bin:$PATH"
export MAVEN_OPTS="${MAVEN_OPTS:-} -Djdk.xml.maxGeneralEntitySizeLimit=0 -Djdk.xml.totalEntitySizeLimit=0 -Djdk.xml.entityExpansionLimit=0"

/data2/projects/ecorefs/.tools/apache-maven-3.9.10/bin/mvn \
  -f /data2/projects/ecorefs/eval/modisco_jdt_reverse/pom.xml \
  -Dmaven.repo.local=/data2/projects/ecorefs/.m2-repo \
  -Dmodisco.merged=/data2/projects/ecorefs/eval/datasets/modisco-java-models-by-repo/all-repos-merged.xmi \
  -Dmodisco.fragmented.root=/data2/projects/ecorefs/eval/datasets/modisco-java-models-by-repo/all-repos-merged-by-compilation-unit/all-repos-merged.root.xmi \
  -Dtest=ModiscoTraversalBenchmarkTest \
  verify
```

## Partitioned directory layout

The directory `eval/datasets/modisco-java-models-by-repo/all-repos-merged-by-compilation-unit` contains:

- `all-repos-merged.root.xmi`: small root model with shared package structure plus references to partitioned resources
- `version-manifest.json`: project-level version manifest next to the root model
- `project-head.json`: mutable local record of the current published IPNS head
- `project-version-manifest.json`: published version manifest with resolved CIDs
- `component-summary.csv`: component inventory with resource counts
- `component-dependencies.csv`: cross-component dependency edges
- `component-manifests.md`: short notes for the plugin/module manifest layer
- `compilation-units/`: one `.xmi` resource per Java compilation unit
- `compilation-units/<repo>/<plugin>/component-manifest.json`: one manifest per plugin/module component, next to its `.xmi` files
- `aux/`: shared auxiliary resources such as orphan types and unresolved items
- `ROOT_MODEL.txt`: marker file naming the root model
- `resources-manifest.csv`: emitted resource inventory
- `README.md`: short generated description

The logical model is the whole `ResourceSet`, not just the root file by itself. The tests count logical elements by EMF URI across all loaded resources so the partitioned view can be compared fairly with the single merged file.

For distributed publication, the intended layering is:

- compilation units remain the immutable internal resources
- plugin/module manifests become the independently versioned component units
- `version-manifest.json` becomes the single mutable project head target, for example behind one `IPNS` name
- `project-head.json` is the local sidecar that records the mutable IPNS head and its current target
