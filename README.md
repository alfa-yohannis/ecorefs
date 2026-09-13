# ecorefs

EMF Ecore XMI implementation for IPFS.

ecorefs stores Eclipse Modeling Framework (EMF) models on the InterPlanetary File System (IPFS). A model is serialized as XMI and added to a local IPFS node (Kubo). Each save returns a content identifier (CID). Models load back by CID or by IPNS name, and cross-resource references can point to either.

The project is research on decentralized model persistence for model-driven engineering (MDE). The working title is *Towards Decentralized Model Persistence*.

## Contents

- [Research questions](#research-questions)
- [Publication plan](#publication-plan)
- [To do](#to-do)
- [Architecture](#architecture)
- [Evaluation code and results](#evaluation-code-and-results)
- [Running the tests](#running-the-tests)
- [Comparison with other persistence approaches](#comparison-with-other-persistence-approaches)
- [Related repositories](#related-repositories)

## Research questions

In EMF, a URI such as `file://` or `platform://` names a location, and the content at that location can change. In IPFS, the address is computed from the content, so every modification produces a new address. The research asks three questions about the consequences for model persistence.

### RQ1: Architecture

> What are the architectural challenges of extending model persistence frameworks to support content-addressed decentralized storage, and how can they be addressed?

EMF's `Resource` and `ResourceSet` assume location-based URIs. The URI stays stable while the content may change. Content-addressed storage inverts the relation, so models need a different way to be identified, stored and retrieved.

Sub-questions:

- How should `ipfs://` URIs map onto EMF's URI handling (`URIConverter`, `Resource.Factory.Registry`)?
- What is the lifecycle of a content-addressed resource: create, save (receive a CID), modify, save again (new CID)?
- How can content-addressed resources coexist with file-based resources in the same `ResourceSet`?

Evaluation plan:

| Method | Details |
| --- | --- |
| Proof of concept | Build `IPFSResource`, `IPFSResourceFactory` and `IPFSURIHandler` as an EMF extension |
| Functional validation | Save and load round trips of Ecore models over IPFS |
| Compatibility test | One `ResourceSet` holding both `XMIResource` (local) and `IPFSResource` (IPFS) |
| Generalizability | Which challenges are specific to EMF and which apply to other modeling frameworks |
| Success criteria | `save()`, `load()` and proxy resolution work over `ipfs://` URIs |

### RQ2: Cross-resource references

> How can cross-resource references be maintained and resolved in an immutable, content-addressed environment where resource identifiers change on every modification?

File-based persistence uses location paths for cross-resource references, for example `href="../metamodel.ecore#//Person"`. On IPFS, a resource gets a new CID whenever the resource changes. For independently developed resources, the new CID acts as version pinning:

```text
Resource A --href="ipfs://QmB_v1#//Person"--> Resource B (v1)

B is modified, and B_v2 gets a new CID.
A still points to B_v1, the version A was built and validated against.
B_v1 stays retrievable while at least one IPFS node stores it.
```

Dependency management works the same way. Maven versions, npm lockfiles, Go modules and Git submodules pin exact versions, and an upgrade is a deliberate action. The research question is therefore how to support deliberate reference upgrades while keeping models reproducible.

Sub-questions:

- How does content addressing pin versions for independently developed model resources?
- Which strategies support deliberate upgrades, when a developer wants a newer version of a referenced resource?
- How do EMF proxy resolution and lazy loading behave over `ipfs://` URIs with immutable references?
- Can an IPFS directory, with related resources under one root CID and relative paths, simplify references between resources developed together?

Reference management strategies:

| Strategy | Mechanism | Benefit | Drawback |
| --- | --- | --- | --- |
| Direct CID references | `href="ipfs://QmB..."` pins an exact version | Full reproducibility | Upgrades are manual |
| IPFS directories | Relative paths inside one directory with one root CID | Natural for resources developed together | Any internal change creates a new root CID |
| IPNS indirection | `href="ipns://k51..."` is a mutable pointer to the latest version | Always resolves to the latest version | Loses immutability, and resolution is slower |
| Manifest or registry | A registry document maps logical names to CIDs | Flexible | The registry itself needs management |

Location-based and content-addressed storage side by side:

| | Location-based (`file://`) | Content-addressed (`ipfs://`) |
| --- | --- | --- |
| Address | Stable | Changes with the content |
| Content | Mutable | Immutable |
| References | May break or point to changed content | Pinned to one version |
| Versioning | None built in | Every save creates a new version (CID) |

Evaluation plan:

| Method | Details |
| --- | --- |
| Multi-resource scenario | A metamodel (`.ecore`) and 3 to 5 instance models (`.xmi`) with cross-references, all persisted to IPFS |
| Round-trip validation | Save all, retrieve by CID, and check that all cross-references resolve |
| Version-pin test | Modify one resource and save it (new CID), then check that referencing resources still resolve to the original version |
| Intentional upgrade test | Point a reference to the new version, save, and check the updated resolution |
| Proxy resolution test | Lazy loading of referenced EObjects across `ipfs://` URIs |
| Strategy comparison | Direct CIDs, IPFS directories and IPNS in the scenarios above |
| Success criteria | References stay stable by default, deliberate upgrades work, and each strategy has a clear recommended use |

### RQ3: Performance

> What is the performance overhead of content-addressed model persistence, and under what conditions is it practical for MDE toolchains?

IPFS adds network communication, content hashing and API calls. MDE tools need to know the cost and the model sizes at which the cost stays acceptable.

Metrics:

| Metric | What it captures |
| --- | --- |
| Save latency (ms) | Time from `resource.save()` to the returned CID |
| Load latency (ms) | Time from `resource.load()` with a CID to a fully resolved EObject tree |
| Throughput | Models saved or loaded per second |
| Scalability | Growth of latency with model size (number of EObjects, depth of the containment tree) |

Evaluation plan:

| Method | Details |
| --- | --- |
| Benchmark protocol | Models with 100, 1K, 10K and 100K EObjects, generated from one representative Ecore metamodel |
| Baselines | `XMIResource` on the local file system and on an NFS or network drive |
| Environment | Local IPFS node (Kubo) in Docker, to isolate network variability |
| Repetitions | 30 runs per configuration, reported as mean and standard deviation |
| Statistical test | Wilcoxon signed-rank test or paired t-test |
| Practicality threshold | The model size at which IPFS takes more than 5 times as long as local I/O |
| Success criteria | A clear performance profile and guidance on when content-addressed persistence is practical |

The expected result is an IPFS latency curve above the file system curve, growing with model size. The practicality threshold marks the size where IPFS stops being practical.

### Summary

| RQ | Focus | Contribution type |
| --- | --- | --- |
| RQ1 | Architecture: the challenges and how to address them | Design and implementation |
| RQ2 | Cross-references: how immutable CIDs affect references and which strategies manage them | Conceptual and empirical |
| RQ3 | Performance: the conditions under which content-addressed persistence is practical | Quantitative benchmarking |

In one sentence, the research identifies the architectural challenges of content-addressed model persistence (RQ1), shows that immutable references pin versions and describes strategies for managing them (RQ2), and measures the performance limits for practical use (RQ3).

## Publication plan

RQ2 is considered the main new contribution and targets the MODELS conference. RQ1 and RQ3 complete an extended journal version for a Q1 journal such as Software and Systems Modeling (SoSyM).

## To do

Open work for the SoSyM manuscript in `papers/01-SoSym/paper/main.tex` and for the code, most significant first. Every item marked here also appears as a `\todo` note in the manuscript, except where noted.

The first five items come from a review of the current draft. The draft reads as a solid tool and case study paper, and these five items address what a reviewer would raise first.

- [ ] Measure against a remote IPFS node. Every number in the paper comes from one local Kubo node on one laptop, so a paper about decentralized persistence never measures the decentralized part. Two setups are needed. A Kubo node on a rented machine, reached through an SSH tunnel on port 5001, gives the cost of the network round trip per `add` and `cat`. A second node on another network, which has never seen the content, gives the cost of a fetch through the distributed hash table, plus the publish and resolve times of an IPNS name.
- [ ] Compare against CDO, NeoEMF and a Git-based store on the same models. The paper argues properties against the alternatives without measuring any of them.
- [ ] Evaluate the cascade save on a large dependency graph. The partitioned MoDisco model has 6,933 resources in 26 components with 92 dependency edges, while the merged file holds no cross-resource references at all. Change one element in a compilation-unit resource, call `cascadeSave`, and record the number of re-saved resources and the time, for a leaf that nothing references, for a resource inside a component that others depend on, and for a shared resource in `aux`. Watch the cross-reference scan, because `EcoreUtil.CrossReferencer.find` walks the whole resource set and a traversal of that model already takes 380 s. A variant that reads the dependencies from the component manifests would be the comparison.
- [ ] State which challenges are specific to EMF and which apply to other modeling frameworks. The generalizability sub-question of RQ1 is open, and an answer raises the conceptual contribution beyond one framework.
- [ ] Evaluate the tool support with a realistic editing workflow or with users. The two Eclipse tools are described but never tried by anyone outside the project.

- [x] Repeat the XMI load benchmark with a fresh `ResourceSet`. Every load now runs in a fresh resource set, the copy of the model sits outside the measured region, and each iteration stamps the copy, so every iteration stores new content.
- [x] Check `cascadeSave` when two reference paths reach the changed resource. `ResourceDependencyGraph` now orders the affected resources by their dependencies, and `CascadeReferenceIntegrityTest` checks a chain, a diamond and a chain of four resources.
- [x] Run the BPMN cascade case study with a live Kubo node and record the real CIDs before and after the cascade save. `BpmnCaseStudyCascadeTest` loads the three BPMN files with the Eclipse BPMN 2.0 metamodel, publishes them, changes the payment process and checks every rewritten reference. The figure in the paper carries the CIDs of that run.
- [x] Record the measurement environment: Intel Core i7-1165G7, 16 GB memory, Ubuntu 24.04.3 with Linux 6.8.0, OpenJDK 21.0.9, Kubo 0.40.1 in Docker 29.8.0, EMF 2.23.0, java-ipfs-http-client 1.4.3, JUnit 5.10.0, Maven 3.8.7.
- [x] Extend the benchmark to larger models with 30 runs per size and a statistical test. The run covers 100 to 500,000 elements, and `eval/analyse_benchmark.py` writes the summary with a Wilcoxon signed-rank test. A shared network file system as a further baseline stays open and is named as such in the paper.
- [x] Test proxy resolution across `ipfs://` references. `ProxyResolutionTest` loads a referring resource in a fresh resource set, checks that the reference starts as a proxy and checks the object after the resolution.
- [x] Assert the rewritten reference in the cascade test and measure chains longer than two resources. The chain of four resources took 120 ms for four saves.
- [ ] Build the modeler with Tycho. The BPMN2 update site in the build file is no longer available, and `releases/2022-06` carries the same bundles.
- [ ] Report the output of `BpmnIpfsExample` and of the modeler self-tests against a running Kubo node.
- [x] Attach the MoDisco test reports in `eval/results/modisco-case-study`, together with the byte-level check results and the measured load and traversal times.
- [ ] Archive the MoDisco dataset (1.04 GiB) and the published CIDs under a DOI for the data availability statement.
- [ ] Refactor the IPFS Java code to the project rules: object-oriented structure, no nested functions, intelligible names, an effective comment on every class and method, and `// EcoreFS begin` and `// EcoreFS end` markers around additions to existing files. This item is not in the manuscript.
- [x] Write the declarations: funding (self-funded), competing interests, ethics approval, consent for publication, author contribution and the Springer statement on the use of large language models (in the Research method section).
- [x] Fill in the affiliation: Department of Informatics, Pradita University, Kabupaten Tangerang, Banten, Indonesia.
- [ ] Remove every `\todo` note from `main.tex` before submission.

## Architecture

### Core plugin

`core-plugin/` is a Java 11 Maven library. The library adds IPFS persistence to EMF and talks to a Kubo daemon on port 5001 through `java-ipfs-http-client`.

| Class | Role |
| --- | --- |
| `IPFSResourceImpl` | Extends `XMIResourceImpl`. A save writes the XMI to a temporary file, adds the file to Kubo, and sets the resource URI to `ipfs://<CID>`. In IPNS mode, the save publishes the CID under an IPNS key and sets the URI to `ipns://<key id>`. |
| `IPFSResourceFactoryImpl` | Creates `IPFSResourceImpl` instances for `ipfs://` URIs. |
| `IPFSURIHandlerImpl` | Added to the URI converter of a `ResourceSet`. The handler reads `ipfs://`, `ipns://`, `http://` and `https://` URIs during `load()`. |
| `IPFSModelPersister` | Provides `cascadeSave(Resource, Map)`, described below. |
| `ReferenceMode`, `IPFSResourceOptions` | The save option `IPFSResourceOptions.REFERENCE_MODE` selects `ReferenceMode.CID` (default) or `ReferenceMode.IPNS`. |

Two command-line tools, `IPFSModelPublishTool` and `IPFSVersionManifestPublisher`, publish larger partitioned models. The MoDisco evaluation uses them.

### Cascade save and reference modes

`resource.save()` saves one resource and gives the resource a new CID. Resources that still reference the old CID stay unchanged. `IPFSModelPersister.cascadeSave()` performs a deliberate upgrade, similar to a Git commit that updates every tree above a changed file:

1. Save the changed resource. The save assigns the new CID to the resource URI.
2. Use `EcoreUtil.CrossReferencer` to find every resource in the `ResourceSet` with a reference into the saved resource.
3. Save each referencing resource the same way. The rewritten `href`s point to the new CID, because EMF serializes a reference with the current URI of the target.

A set of already saved resources prevents endless loops when resources reference each other.

| Mode | Behavior | Trade-off |
| --- | --- | --- |
| CID (default) | Immutable references, and the cascade updates referencing resources | Full integrity, plus the cost of the cascade |
| IPNS | Mutable name pointers, and no cascade is needed | No cascade, but no immutability |

## Evaluation code and results

| Path | Purpose |
| --- | --- |
| `eval/custom_generator` (`ecorefs.generator.ModelGenerator`) | Generates EMF component trees with 100, 1k, 10k, 100k and 500k objects for latency tests |
| `core-plugin/src/test/java/org/ecorefs/ipfs/FastPerformanceBenchmarkTest.java` | RQ3 benchmark: XMI file saves and loads against IPFS for 100 to 10,000 elements, with 3 warm-up and 10 measured iterations |
| `core-plugin/src/test/java/org/ecorefs/ipfs/MultiFileCascadeTest.java` | RQ2 test: builds two resources A and B at runtime, with A referring to B, runs `cascadeSave(B)` and records the latency |
| `eval/modisco_jdt_reverse/` | MoDisco models of Eclipse JDT, partitioning and IPFS publication (see its [README](eval/modisco_jdt_reverse/README.md)) |

Results:

- `eval/results/comprehensive_benchmark_results.md`: summary of the benchmark runs (mean, median and standard deviation)
- `eval/results/fast_test_metrics.csv` and `eval/results/cascade_metrics.csv`: raw timings, written locally and ignored by git

## Running the tests

The tests need a local Kubo node on port 5001:

```bash
docker run -d --name ipfs_node -p 5001:5001 -p 8080:8080 -p 4001:4001 ipfs/kubo:latest
```

The core plugin compiles for Java 11 (`maven.compiler.source` is 11) and uses `java-ipfs-http-client` v1.4.3 from JitPack. Run Maven with JDK 11 or newer, for example 21 or 25. Java 8 does not work.

The tests create their metamodels at runtime with `EcoreFactory` (for example `factory.createEPackage()`), so no generated model classes or `.genmodel` files are needed. Cross-resource tests use non-containment references.

## Comparison with other persistence approaches

The table compares ecorefs with XMI files, CDO (Connected Data Objects) and NeoEMF.

| Property | XMI file | CDO | NeoEMF | ecorefs (IPFS) |
| --- | --- | --- | --- | --- |
| Decentralized | No | No | No | Yes |
| Content integrity check | No | No | No | Yes |
| New version on every save | No | Partly (transactions) | No | Yes |
| Reproducible snapshots | No | No | No | Yes |
| Works without a central server | Yes | No | No | Yes |
| Location-independent addresses | No | No | No | Yes |
| Built-in deduplication | No | No | No | Yes |
| Lazy loading | No | Yes | Yes | No |
| Query support | No | Yes (OCL) | Yes (graph database) | No |
| Mutable references | Not applicable | Yes | Yes | Optional (IPNS) |

### Decentralization

XMI files live on a local or shared file system. CDO needs a running CDO server with a relational database, and NeoEMF needs a running graph database such as Neo4j. ecorefs uses the IPFS peer-to-peer network. No central server has to run, and any IPFS node with the content can serve the models.

### Content integrity

A CID is a cryptographic hash of the content. Changing a single byte of a model changes the CID, so a client can check that downloaded content matches the CID. XMI files, CDO and NeoEMF rely on the file system, the server or the database engine for integrity.

### Versioning and reproducible snapshots

Every `save()` produces a new CID. Each CID is a fixed snapshot of the model at the time of the save, and creating the snapshot needs no transaction management or separate version control. A save does not record a link to the previous version, so the version history has to be tracked separately, for example in a manifest.

Anyone with a CID can later retrieve and verify the exact model content, as long as some IPFS node still stores the content. The property helps research reproducibility and audits. An XMI file can be changed on disk, a CDO server can be reconfigured or shut down, and a NeoEMF database can be altered.

### Location-independent addresses and deduplication

The same content added with the same IPFS settings produces the same CID, independent of where or when the content is added. Organizations can share models without coordinating namespaces, and identical content can be deduplicated.

### Circular references

`cascadeSave` handles circular dependencies (A referring to B and B referring to A) with its set of already saved resources. CDO and NeoEMF do not face the problem, because their references use mutable identifiers on the server.

### Limitations

| Limitation | Detail |
| --- | --- |
| No lazy loading | A resource is loaded into memory as a whole, unlike in CDO and NeoEMF |
| No query support | No built-in OCL or graph query engine |
| IPNS resolution latency | IPNS publish and resolve use the DHT and can take seconds longer than direct CID access |
| Cascade cost | In CID mode, a change to a leaf resource causes saves along the whole dependency chain |
| Circular references | With references in both directions, one direction always points to an outdated CID. Content addressing makes the behavior unavoidable. |

CDO and NeoEMF remain the better choice for performance and queries. The IPFS approach offers different properties of the storage design: decentralization, integrity checks, fixed versions and reproducibility. The comparison rests on these design properties, and no benchmark against CDO or NeoEMF has been run.

## Related repositories

The BPMN2 work lives in separate repositories:

- [org.eclipse.bpmn2-modeler-1.5.4-ecorefs](https://github.com/alfa-yohannis/org.eclipse.bpmn2-modeler-1.5.4-ecorefs): Eclipse BPMN2 Modeler 1.5.4 with commands to open and publish BPMN models on IPFS
- [org.eclipse.bpmn2-ecorefs](https://github.com/alfa-yohannis/org.eclipse.bpmn2-ecorefs): Eclipse BPMN 2.0 Metamodel with a generated tree editor that reads `ipfs://` and `ipns://` URIs
