# Ecore IPFS Persistence Context & Architecture
> **Purpose**: This document provides a highly detailed technical summary of the customized EMF IPFS storage plugin and evaluation suite constructed during the project. It explicitly serves as the brain-dump context for future agents inheriting this codebase.

## 1. Core Objective
The core objective of this project was to establish a functional, decentralized storage paradigm for the Eclipse Modeling Framework (EMF) utilizing **IPFS (InterPlanetary File System)**. This enables immutable version-pinning (Git-like history) for Model-Driven Engineering (MDE) artifacts. Specifically, the project addresses two major research goals:
* **RQ2**: Managing cross-resource graph dependencies dynamically across immutable CID boundaries (`cascadeSave`).
* **RQ3**: Evaluating the pragmatic performance thresholds (Save/Load latency limitations) of IPFS vs File IO.

## 2. Technical Architecture & File Map

### A. The Core IPFS Plugin (`/data2/projects/ecorefs/core-plugin/`)
A pure Java 11 Maven plugin exposing the EMF abstraction layer over IPFS HTTP operations.
- `org.ecorefs.ipfs.IPFSResourceImpl` & `IPFSResourceFactoryImpl`: Direct overrides of the standard EMF `XMIResourceImpl`. These classes natively write outgoing XMI content into memory buffers and proxy them recursively to an active IPFS Kubo daemon (port 5001) using the `java-ipfs-http-client` dependency.
- `org.ecorefs.ipfs.IPFSURIHandlerImpl`: Injected centrally into the active `ResourceSet` so that `ipfs://<CID>` strings are automatically trapped and handled by the IPFS HTTP stack during `.load()`.
- `org.ecorefs.ipfs.IPFSModelPersister`: The crown jewel answering RQ2. It features the `cascadeSave(Resource)` algorithm. Because IPFS CIDs are content-addressed and immutable, changing a leaf node file changes its CID. The cascade algorithm natively sweeps the system utilizing `EcoreUtil.CrossReferencer`, identifies all Root parents holding an `href` to the modified Child, actively rewrites those XML `href`s to point to the newly generated `ipfs://CID_Child`, and forces the parent to save resulting in a mathematically perfect `CID_Root`.

### B. Benchmarking and Metrics Dataset (`/data2/projects/ecorefs/eval/`)
- `ecorefs/generator/ModelGenerator.java`: A bespoke Java application designed to algorithmically build and stream dynamically allocated EMF component trees (100, 1k, 10k, 100k, 500k objects) strictly for latency evaluation.
- `FastPerformanceBenchmarkTest.java`: The core Junit test for **RQ3**, comparing standard disk `XMI` writing vs the HTTP `IPFS` engine over **10 measured iterations** mapped onto sizes `100` -> `10,000`.
- `MultiFileCascadeTest.java`: The core architecture test for **RQ2**. Instantiates a `pendingA -> pendingB` 2-file graph structure entirely in runtime, fires `cascadeSave(ResB)`, and evaluates if `pendingA` morphs into an absolute `ipfs://<CID_Root>`. Also generates CSV latency metrics mathematically verifying the tree traversal is **virtually 0ms**.

### C. Data & Outputs
The mathematically compiled multi-iteration results (Mean, Median, and Standard Deviation mapping across IPFS bounds) have been exported natively locally formats and graphs:
- Metrics CSV Output: `/data2/projects/ecorefs/eval/results/fast_test_metrics.csv`
- Cascade Metrics CSV Output: `/data2/projects/ecorefs/eval/results/cascade_metrics.csv`
- Mathematical Summaries & Paper Writeup Data: `/data2/projects/ecorefs/eval/results/comprehensive_benchmark_results.md`

## 3. Important Execution Details for Future Agents
1. **Network Environment**: The unit tests will fatally crash unless a local `ipfs/kubo` Docker container is executing natively over standard port 5001. 
   `docker run -d --name ipfs_node -p 5001:5001 -p 8080:8080 -p 4001:4001 ipfs/kubo:latest`
2. **Java 11/21/25 Runtime**: The Maven configuration (`pom.xml`) utilizes OpenJDK 11 bytecode compatibility mappings (`<maven.compiler.source>11</maven.compiler.source>`), which interfaces specifically with the IPFS JitPack `v1.4.3` client natively. Ensure your execution `$PATH` routes commands through a modern JDK (such as the LTS releases 11/21, or the local system default Java 25) rather than a legacy Java 8 context.
3. **No Generated Classes Needed**: The test layouts construct strict proxy boundary tests (containment = false) directly using raw EMF programmatic factories (`factory.createEPackage()`), entirely avoiding the necessity to inject `.genmodel` static definitions across the testing suites.
