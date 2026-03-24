# Manual Checks

This document is the operator runbook for the current MoDisco JDT evaluation setup. It focuses on the commands we actually use to validate the generated models and the published IPFS artifacts.

The authoritative current artifacts are:

- merged model: `/data2/projects/ecorefs/eval/datasets/modisco-java-models-by-repo/all-repos-merged.xmi`
- partitioned root model: `/data2/projects/ecorefs/eval/datasets/modisco-java-models-by-repo/all-repos-merged-by-compilation-unit/all-repos-merged.root.xmi`
- local published-head record: `/data2/projects/ecorefs/eval/datasets/modisco-java-models-by-repo/all-repos-merged-by-compilation-unit/project-head.json`
- local published manifest mirror: `/data2/projects/ecorefs/eval/datasets/modisco-java-models-by-repo/all-repos-merged-by-compilation-unit/project-version-manifest.json`

## Environment

Use the same Java, Maven, and XML-limit settings for all Tycho test runs:

```bash
export JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64
export PATH="$JAVA_HOME/bin:$PATH"
export MAVEN=/data2/projects/ecorefs/.tools/apache-maven-3.9.10/bin/mvn
export M2_REPO=/data2/projects/ecorefs/.m2-repo
export MODISCO_ROOT=/data2/projects/ecorefs/eval/datasets/modisco-java-models-by-repo/all-repos-merged-by-compilation-unit
export MERGED_XMI=/data2/projects/ecorefs/eval/datasets/modisco-java-models-by-repo/all-repos-merged.xmi
export PARTITIONED_ROOT=$MODISCO_ROOT/all-repos-merged.root.xmi
export PROJECT_HEAD_JSON=$MODISCO_ROOT/project-head.json
export PROJECT_MANIFEST_JSON=$MODISCO_ROOT/project-version-manifest.json
export MAVEN_OPTS="${MAVEN_OPTS:-} -Djdk.xml.maxGeneralEntitySizeLimit=0 -Djdk.xml.totalEntitySizeLimit=0 -Djdk.xml.entityExpansionLimit=0"
```

The current direct immutable merged CID is:

```bash
export MERGED_IPFS_CID=bafybeidqkvbyfklflf6ttroxehzkqyeci4erwklredfgtqhebtbnaigrie
```

These helpers read the current IPNS and published CIDs from the JSON sidecars instead of hardcoding them:

```bash
export PROJECT_IPNS_NAME=$(python3 -c 'import json,sys; print(json.load(open(sys.argv[1], "r", encoding="utf-8"))["ipns"].removeprefix("ipns://"))' "$PROJECT_HEAD_JSON")
export PROJECT_MANIFEST_CID=$(python3 -c 'import json,sys; print(json.load(open(sys.argv[1], "r", encoding="utf-8"))["currentManifestCid"])' "$PROJECT_HEAD_JSON")
export PROJECT_ROOT_CID=$(python3 -c 'import json,sys; print(json.load(open(sys.argv[1], "r", encoding="utf-8"))["rootModel"]["cid"])' "$PROJECT_MANIFEST_JSON")
```

## Regenerate Models

The generation steps live in the main README:

- export repository models: `README.md`, sections `Run it against the cloned JDT repository` and `Direct Maven invocation`
- partition the merged model: `README.md`, section `Partition a merged model by compilation unit`
- regenerate component manifests: `README.md`, section `Generate plugin/module version manifests for an existing partitioned model`

Those commands remain the authoritative way to rebuild the local evaluation artifacts.

## Kubo Checks

Start the local Kubo container before any IPFS or IPNS check:

```bash
docker start ipfs_node
```

Confirm that the recorded project IPNS resolves to the recorded published manifest CID:

```bash
docker exec ipfs_node ipfs name resolve "$PROJECT_IPNS_NAME"
```

The output should be:

```text
/ipfs/$PROJECT_MANIFEST_CID
```

Check that the direct immutable merged CID matches the local merged file byte-for-byte:

```bash
TMP=$(mktemp)
docker exec ipfs_node ipfs cat "$MERGED_IPFS_CID" > "$TMP"
cmp -s "$TMP" "$MERGED_XMI" && echo MERGED_BAFY_MATCH
rm -f "$TMP"
```

Check that the published project manifest matches the local mirror byte-for-byte:

```bash
TMP=$(mktemp)
docker exec ipfs_node ipfs cat "$PROJECT_MANIFEST_CID" > "$TMP"
cmp -s "$TMP" "$PROJECT_MANIFEST_JSON" && echo PROJECT_VERSION_MANIFEST_MATCH
rm -f "$TMP"
```

Check that the published partitioned root matches the local partitioned root byte-for-byte:

```bash
TMP=$(mktemp)
docker exec ipfs_node ipfs cat "$PROJECT_ROOT_CID" > "$TMP"
cmp -s "$TMP" "$PARTITIONED_ROOT" && echo PARTITIONED_ROOT_MATCH
rm -f "$TMP"
```

## Loader Tests

Merged model load and navigation:

```bash
$MAVEN \
  -f /data2/projects/ecorefs/eval/modisco_jdt_reverse/pom.xml \
  -Dmaven.repo.local="$M2_REPO" \
  -Dmodisco.merged="$MERGED_XMI" \
  -Dtest=ModiscoMergedModelLoadTest \
  verify
```

Partitioned root load, fragment resolution, and descendant traversal:

```bash
$MAVEN \
  -f /data2/projects/ecorefs/eval/modisco_jdt_reverse/pom.xml \
  -Dmaven.repo.local="$M2_REPO" \
  -Dmodisco.fragmented.root="$PARTITIONED_ROOT" \
  -Dtest=ModiscoFragmentedModelLoadTest \
  verify
```

The latest JUnit reports land in:

- `/data2/projects/ecorefs/eval/modisco_jdt_reverse/org.ecorefs.modisco.jdt.reverse.tests/target/surefire-reports/org.ecorefs.modisco.jdt.reverse.tests.ModiscoMergedModelLoadTest.txt`
- `/data2/projects/ecorefs/eval/modisco_jdt_reverse/org.ecorefs.modisco.jdt.reverse.tests/target/surefire-reports/org.ecorefs.modisco.jdt.reverse.tests.ModiscoFragmentedModelLoadTest.txt`

## Traversal Benchmark

This is the full logical-size equivalence check between the merged and partitioned models. It is heavier than the loader tests and can take around 10 minutes on this machine.

```bash
$MAVEN \
  -f /data2/projects/ecorefs/eval/modisco_jdt_reverse/pom.xml \
  -Dmaven.repo.local="$M2_REPO" \
  -Dmodisco.merged="$MERGED_XMI" \
  -Dmodisco.fragmented.root="$PARTITIONED_ROOT" \
  -Dtest=ModiscoTraversalBenchmarkTest \
  verify
```

What to look for in the console:

- merged load + traversal timings
- partitioned load + traversal timings
- final equality line:

```text
comparison: mergedElements=4282694, fragmentedElements=4282694
```

The JUnit report is:

- `/data2/projects/ecorefs/eval/modisco_jdt_reverse/org.ecorefs.modisco.jdt.reverse.tests/target/surefire-reports/org.ecorefs.modisco.jdt.reverse.tests.ModiscoTraversalBenchmarkTest.txt`

## Materialize A Published Component Snapshot

The component-scoped EMF Compare test needs a local filesystem snapshot fetched from Kubo. This helper script recreates the exact directory layout that the test expects:

```bash
cd /data2/projects/ecorefs/eval/modisco_jdt_reverse
rm -rf /tmp/modisco_component_snapshot_small
./fetch-component-snapshot-from-ipfs.sh \
  /tmp/modisco_component_snapshot_small \
  eclipse.jdt.debug/org.eclipse.jdt.launching.javaagent
```

To prepare the larger `2,5,11,26,51,128,350,1066` batch:

```bash
cd /data2/projects/ecorefs/eval/modisco_jdt_reverse
rm -rf /tmp/modisco_component_snapshot_batch
./fetch-component-snapshot-from-ipfs.sh \
  /tmp/modisco_component_snapshot_batch \
  eclipse.jdt.debug/org.eclipse.jdt.launching.javaagent \
  eclipse.jdt.debug/org.eclipse.jdt.launching.macosx \
  eclipse.jdt.debug/org.eclipse.jdt.launching.ui.macosx \
  eclipse.jdt.ui/org.eclipse.jdt.astview \
  eclipse.jdt.ui/org.eclipse.jdt.junit.core \
  eclipse.jdt.debug/org.eclipse.jdt.launching \
  eclipse.jdt.debug/org.eclipse.jdt.debug \
  eclipse.jdt.core/org.eclipse.jdt.core
```

The helper fetches:

- `all-repos-merged.root.xmi` from the published root CID
- one component directory per requested component ID from each `componentRootCid`

## EMF Compare Checks

### Smallest plugin

```bash
$MAVEN \
  -f /data2/projects/ecorefs/eval/modisco_jdt_reverse/pom.xml \
  -Dmaven.repo.local="$M2_REPO" \
  -Dmodisco.fragmented.root="$PARTITIONED_ROOT" \
  -Dmodisco.fragmented.snapshot.root=/tmp/modisco_component_snapshot_small/all-repos-merged.root.xmi \
  -Dmodisco.component.id=eclipse.jdt.debug/org.eclipse.jdt.launching.javaagent \
  -Dmodisco.component.report.dir="$MODISCO_ROOT/component-compare-small-manual" \
  -Dtest=ModiscoEmfCompareSnapshotTest#emfCompareFindsNoDifferencesForConfiguredComponentSnapshotWhenConfigured \
  verify
```

### Scaled plugin ladder: `2,5,11,26,51,128,350,1066`

```bash
$MAVEN \
  -f /data2/projects/ecorefs/eval/modisco_jdt_reverse/pom.xml \
  -Dmaven.repo.local="$M2_REPO" \
  -Dmodisco.fragmented.root="$PARTITIONED_ROOT" \
  -Dmodisco.fragmented.snapshot.root=/tmp/modisco_component_snapshot_batch/all-repos-merged.root.xmi \
  -Dmodisco.component.id=eclipse.jdt.debug/org.eclipse.jdt.launching.javaagent,eclipse.jdt.debug/org.eclipse.jdt.launching.macosx,eclipse.jdt.debug/org.eclipse.jdt.launching.ui.macosx,eclipse.jdt.ui/org.eclipse.jdt.astview,eclipse.jdt.ui/org.eclipse.jdt.junit.core,eclipse.jdt.debug/org.eclipse.jdt.launching,eclipse.jdt.debug/org.eclipse.jdt.debug,eclipse.jdt.core/org.eclipse.jdt.core \
  -Dmodisco.component.report.dir="$MODISCO_ROOT/component-compare-manual-2-5-11-26-51-128-350-1066" \
  -Dtest=ModiscoEmfCompareSnapshotTest#emfCompareFindsNoDifferencesForConfiguredComponentSnapshotWhenConfigured \
  verify
```

The current recheck report generated on this machine is:

- `/data2/projects/ecorefs/eval/datasets/modisco-java-models-by-repo/all-repos-merged-by-compilation-unit/component-compare-recheck-20260324/component-compare-summary.md`
- `/data2/projects/ecorefs/eval/datasets/modisco-java-models-by-repo/all-repos-merged-by-compilation-unit/component-compare-recheck-20260324/component-compare-stats.csv`

What this compare actually checks:

- `all-repos-merged.root.xmi`: byte-for-byte equality
- each selected `component-manifest.json`: byte-for-byte equality
- selected component `.xmi` files: EMF Compare equality

What it intentionally excludes from the component-scoped compare:

- `aux/*`
- non-selected components

That exclusion is deliberate because `root` and `aux` are shared hubs and would dominate every plugin-level compare. Shared artifacts are validated separately with the byte-level Kubo checks above.

The JUnit report for the latest compare run is:

- `/data2/projects/ecorefs/eval/modisco_jdt_reverse/org.ecorefs.modisco.jdt.reverse.tests/target/surefire-reports/org.ecorefs.modisco.jdt.reverse.tests.ModiscoEmfCompareSnapshotTest.txt`

## Current Expected Recheck Results

On the current dataset and publication state, the recheck should show:

- `MERGED_BAFY_MATCH`
- `PROJECT_VERSION_MANIFEST_MATCH`
- `PARTITIONED_ROOT_MATCH`
- merged load test: `Failures: 0, Errors: 0`
- fragmented load test: `Failures: 0, Errors: 0`
- traversal benchmark: `mergedElements=4282694, fragmentedElements=4282694`
- component EMF Compare: no differences for all requested components

The latest successful large compare batch produced:

- components compared: `8`
- compared resources: `1639`
- compared size per side: `252300223` bytes (`240.61 MiB`)
- total EMF Compare time: `186696 ms`

## Notes

- The partitioned model is logically the whole `ResourceSet`, not just the root file by itself.
- The merged model is still faster for whole-model scans.
- The partitioned model is faster to load initially and better suited for selective or component-scoped checks.
- `/tmp` snapshots are disposable. Recreate them with `fetch-component-snapshot-from-ipfs.sh` whenever needed.
