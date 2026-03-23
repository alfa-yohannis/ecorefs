# MoDisco JDT Reverse Engineering

This project imports Eclipse Java projects from a checkout such as `eval/datasets/eclipse.jdt.core` and exports one MoDisco Java model (`.xmi`) per imported Java project.

## What it does

- Scans a repository for Eclipse `.project` files
- Imports the discovered projects into the headless Eclipse workspace used by Tycho
- Runs MoDisco's `DiscoverJavaModelFromJavaProject` discoverer for each Java project
- Writes the resulting models to an output directory

## Layout

- `org.ecorefs.modisco.jdt.reverse`: reusable exporter service
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
