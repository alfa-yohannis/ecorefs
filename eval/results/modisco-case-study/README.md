# MoDisco case study reports

JUnit reports of the case study on the MoDisco models of the Eclipse Java development tools. Each file holds the summary of one test and the console output of that test. The files were copied out of `eval/modisco_jdt_reverse/org.ecorefs.modisco.jdt.reverse.tests/target/surefire-reports`, which the build directory ignores.

| File | Test | What the test shows |
| --- | --- | --- |
| `merged-model-load.txt` | `ModiscoMergedModelLoadTest` | Loads the merged model of 1.06 GiB and navigates into the model |
| `partitioned-model-load.txt` | `ModiscoFragmentedModelLoadTest` | Loads the partitioned root model and resolves cross-references into the fragments |
| `traversal-benchmark.txt` | `ModiscoTraversalBenchmarkTest` | Load and traversal times of both forms, with the element count of each |
| `emf-compare-snapshot.txt` | `ModiscoEmfCompareSnapshotTest` | EMF Compare between local components and copies fetched from IPFS |
| `component-manifest.txt` | `ModiscoComponentManifestExternalModelTest` | Component manifests of the partitioned model |
| `partitioner.txt` | `ModiscoCompilationUnitPartitionerExternalModelTest` | Split of the merged model into one resource per compilation unit |

The byte-level checks against the local Kubo node live in `eval/modisco_jdt_reverse/MANUAL-CHECKS.md`, together with the expected results `MERGED_BAFY_MATCH`, `PROJECT_VERSION_MANIFEST_MATCH` and `PARTITIONED_ROOT_MATCH`.
