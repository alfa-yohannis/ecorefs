# Towards Decentralized Model Persistence
## Research Questions & Evaluation Plan

---

## RQ1: What are the architectural challenges of extending model persistence frameworks to support content-addressed decentralized storage, and how can they be addressed?

### Motivation
EMF's `Resource`/`ResourceSet` architecture assumes **location-based** URIs (`file://`, `platform://`), where the URI is a stable address and the content behind it may change. Content-addressed storage (IPFS) inverts this: the **address is derived from the content**, so every modification produces a new URI (CID). This fundamentally challenges the existing persistence paradigm and requires rethinking how models are identified, stored, and retrieved.

### Sub-questions
- How should `ipfs://` URIs be mapped to EMF's URI handling (`URIConverter`, `Resource.Factory.Registry`)?
- What is the lifecycle of a content-addressed model resource: create → save (receive CID) → modify → save again (new CID)?
- How can content-addressed resources coexist with traditional file-based resources in the same `ResourceSet`?

### Evaluation
| Method | Details |
|---|---|
| **Proof-of-concept** | Build `IPFSResource`, `IPFSResourceFactory`, and `IPFSURIHandler` as an EMF extension |
| **Functional validation** | Demonstrate save/load round-trips of Ecore models over IPFS |
| **Compatibility test** | Mixed `ResourceSet` with both `XMIResource` (local) and `IPFSResource` (IPFS) working together |
| **Generalizability discussion** | Analyse which architectural challenges are EMF-specific vs. generalizable to other modeling frameworks |
| **Success criteria** | All standard EMF operations (`save()`, `load()`, proxy resolution) work transparently over `ipfs://` URIs |

---

## RQ2: How can cross-resource references be maintained and resolved in an immutable, content-addressed environment where resource identifiers change on every modification?

### Motivation
In file-based persistence, cross-resource references use location-based paths (e.g., `href="../metamodel.ecore#//Person"`). In content-addressed storage, every resource is **identified by a CID that changes on modification**. At first glance, this appears problematic — but for **independently developed resources**, it is actually a **built-in version-pinning mechanism**:

```
Resource A ──href="ipfs://QmB_v1#//Person"──▶ Resource B (v1)

When B is modified → B_v2 (new CID) is created
A still correctly points to B_v1 — the version A was built and validated against
B_v1 remains permanently available on IPFS
```

This mirrors how modern dependency management works (Maven, npm lockfiles, Go modules, Git submodules) — dependencies are pinned to exact versions, and upgrading is a deliberate action. The research question therefore shifts from *"how to keep references from breaking"* to *"how to support intentional reference evolution while preserving reproducibility."*

### Sub-questions
- How does content-addressed referencing provide inherent version-pinning for independently developed model resources?
- What strategies exist for managing **intentional reference upgrades** (i.e., when a developer wants to adopt a newer version of a referenced resource)?
- How does EMF's proxy resolution and lazy loading mechanism behave over `ipfs://` URIs with immutable references?
- Can a **directory-based approach** (grouping related resources under a single IPFS directory with relative paths) simplify reference management for co-developed resources?

### Reference Management Strategies

| Strategy | Mechanism | Trade-off |
|---|---|---|
| **Direct CID references** | `href="ipfs://QmB..."` — pin to exact version | ✅ Full reproducibility · ❌ Manual upgrade |
| **IPFS directories** | Relative paths within a directory; one root CID | ✅ Natural for co-developed resources · ❌ Root CID changes on any internal change |
| **IPNS indirection** | `href="ipns://k51..."` — mutable pointer to latest | ✅ Always resolves to latest · ❌ Loses immutability, slower resolution |
| **Manifest/registry** | Logical names mapped to CIDs in a registry document | ✅ Flexible · ❌ Registry itself needs management |

### The Core Insight

```
┌─────────────────────────────────────────────────────────────────┐
│  Location-based (file://)     Content-addressed (ipfs://)       │
│  ─────────────────────────    ──────────────────────────────    │
│  Address is STABLE            Address CHANGES with content      │
│  Content is MUTABLE           Content is IMMUTABLE              │
│  References may silently      References are version-pinned     │
│    break on content change      by design — never break         │
│  No built-in versioning       Every save = new version (CID)    │
└─────────────────────────────────────────────────────────────────┘
```

### Evaluation
| Method | Details |
|---|---|
| **Multi-resource scenario** | Metamodel (`.ecore`) + 3–5 instance models (`.xmi`) with cross-references, all persisted to IPFS |
| **Round-trip validation** | Save all → retrieve by CID → verify all cross-references resolve correctly |
| **Version-pin test** | Modify one resource → re-save (new CID) → verify referencing resources still resolve to the original version |
| **Intentional upgrade test** | Update a reference to point to a new version → re-save → verify updated resolution |
| **Proxy resolution test** | Lazy loading of referenced EObjects across `ipfs://` URIs |
| **Strategy comparison** | Compare direct CID, IPFS directories, and IPNS across the scenarios above |
| **Success criteria** | Cross-references are stable by default; intentional upgrades work cleanly; a clear recommendation for which strategy suits which use case |

---

## RQ3: What is the performance overhead of content-addressed model persistence, and under what conditions is it practical for MDE toolchains?

### Motivation
Content-addressed storage introduces network communication, content hashing, and API overhead. For practical adoption in MDE toolchains, the performance cost must be quantified and the boundary conditions understood — not just *"how slow is it?"* but *"when is it acceptable, and when is it not?"*

### Variables to Measure
| Metric | What It Captures |
|---|---|
| **Save latency** (ms) | Time from `resource.save()` to CID returned |
| **Load latency** (ms) | Time from `resource.load()` with CID to fully resolved EObject tree |
| **Throughput** | Models saved/loaded per second |
| **Scalability** | How latency grows with model size (number of EObjects / depth of containment tree) |

### Evaluation
| Method | Details |
|---|---|
| **Benchmarking protocol** | Models of increasing size: 100, 1K, 10K, 100K EObjects (generated from a representative Ecore metamodel) |
| **Baselines** | Compare against: (1) `XMIResource` to local filesystem, (2) `XMIResource` to NFS/network drive |
| **Environment** | Local IPFS node (Kubo) via Docker — isolates network variability |
| **Repetitions** | 30 runs per configuration, report mean ± std dev |
| **Statistical test** | Wilcoxon signed-rank test or paired t-test for significance |
| **Practicality threshold** | Identify the model size at which IPFS overhead becomes impractical (> 5× of local I/O) |
| **Success criteria** | Clear characterisation of performance profile with actionable guidance on when content-addressed persistence is practical |

### Expected Results Shape

```
Latency (ms)
    │
    │          ╱ IPFS
    │        ╱
    │      ╱
    │    ╱──── File system
    │  ╱
    │╱
    └──────────────────── Model size (EObjects)
                    ▲
                    │ Practicality threshold
```

---

## Summary

| RQ | Focus | Contribution Type |
|---|---|---|
| **RQ1** | Architecture — what are the challenges and how do we address them? | Design & implementation |
| **RQ2** | Cross-references — how do immutable CIDs affect references, and what strategies manage them? | Conceptual & empirical |
| **RQ3** | Performance — under what conditions is it practical? | Quantitative benchmarking |

> **Research narrative:**
> *We identified the architectural challenges of content-addressed model persistence (RQ1), discovered that immutable references provide built-in version-pinning and characterised strategies for managing them (RQ2), and empirically determined the performance boundaries for practical adoption (RQ3).*
