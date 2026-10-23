# RQ2 on real BPMN models

These results test the cascade save of the core plugin on reference structures from real models instead of the hand-made resources of the unit tests. They check the three propositions of Section 5.4 of the paper on every change:
- Proposition 1: the resources that need a new save are exactly those that reach the changed resource.
- Proposition 2: the dependency order leaves every reference current.
- Proposition 3: a cycle keeps an outdated CID.

`eval/bpmn-rq2/run.sh` produced the files on 2026-10-23; `summary.md` holds every table.

## Setup

- **Models.** The cleaned collections in `eval/datasets/bpmn`, so every file loads without errors. Vendor extensions are removed before a model is published, because plain XMI cannot store them (see `eval/results/bpmn-rq1`). They carry no reference between the resources.
- **Software and nodes.** As for RQ1: the core plugin with its own dependencies, javac 17, OpenJDK 21.0.9, and for each experiment its own Kubo 0.40.1 container, offline, with an empty repository and reachable only on 127.0.0.1.
- **Saves.** Every save goes through `IPFSResourceImpl` in CID mode, and every change runs `IPFSModelPersister.cascadeSave`. The first publication saves the resources in dependency order.

Each change renames one object and runs one cascade save. Three checks follow:

- **New CIDs match.** The resources that received a new CID are exactly the affected set A(t): the changed resource and every resource with a path to it. The experiment computes A(t) with its own scan of the stored references, independent of the dependency graph of the core plugin.
- **Consistent.** The stored bytes of every resource are read back from the node by the current CID, and every `ipfs://` URI in them names the current CID of its target.
- **Loads fully.** A fresh resource set loads the top resource by CID and resolves every proxy through the node. Every reachable resource must load under its current CID, with no proxy left and no load error, and the loaded copy of the changed object must carry the new name.

## Split of multi-pool models

The 1,855 models whose collaboration has two or more pools with a process were split into:
- one resource per process;
- one resource for the collaborations and the diagrams;
- one resource for the other root elements, such as messages, signals and item definitions, when there are any.

Moving the elements keeps every reference, so a participant now refers to the resource of its process, a message flow to the nodes of two processes, and a diagram shape to the element it draws. The first change renames the first process; the second renames the first named element of the shared resource.

| Step | Cycle | Models | New CIDs match | Consistent | Loads fully |
|---|---|---:|---:|---:|---:|
| First publication | no | 1,749 | | 1,749 (100%) | |
| Rename of a process | no | 1,748 | 1,748 (100%) | 1,748 (100%) | 1,743 (99.7%) |
| Rename in the shared resource | no | 487 | 487 (100%) | 487 (100%) | 484 (99.4%) |
| First publication | yes | 106 | | 0 | |
| Rename of a process | yes | 106 | 100 (94.3%) | 0 | 0 |
| Rename in the shared resource | yes | 26 | 25 (96.2%) | 0 | 0 |

The loaded copy carried the new name in every acyclic change whose load reached the changed resource. In eight models no participant or shape refers to the first process, so a load from the collaboration does not reach it, and its cascade gave only the process itself a new CID.

**Acyclic splits behave as the propositions state.** Every change gave new CIDs to exactly the affected set and left every stored reference current.

**106 split models (5.7%) contain a reference cycle.** The cycles have two causes:
- **Flows that cross pools (67 models).** A sequence flow, a data association or a boundary event crosses two pools, which BPMN forbids but some modelers drew, so two processes refer to each other.
- **References back to the collaboration (39 models).** A process refers to the collaboration, through an association to an element of the collaboration or through the standard attribute `definitionalCollaborationRef`, while the collaboration refers to the process.

The first publication of these models was never consistent, and no cascade made it consistent, as Proposition 3 predicts. Inside a cycle, a save can also write the same bytes as an earlier save and keep its CID. That is why the new CIDs fall short of the affected set in 7 changes.

**Five acyclic splits load with a proxy left.** In four of them, a sequence flow or a data association crosses two pools in one direction only. In the Eclipse BPMN 2.0 metamodel, references such as `SequenceFlow.targetRef` and `DataAssociation.sourceRef` do not resolve proxies. EMF stores them as `ipfs://` URIs, but after a load they stay proxies. Neither IPFS nor the cascade causes this: the same holds for local files once such a reference crosses two resources.

**Time.** A cascade took a median of 13.6 ms for two new CIDs and 51.5 ms for six; `summary.md` lists all sizes. The cascade time grows with the number of new CIDs at about 7 to 9 ms per resource.

## Call graphs between files

The cleaning of the dataset removed 616 `calledElement` values that name a process in another file. `resolve_called_elements.py` found the defining file for 421 of them by rules that need exactly one candidate (a unique id in the collection, a unique candidate in the folder of the caller, a file named after the id, or the same test variant). It skipped the other 195:
- 22 are expressions;
- 39 have no defining file;
- 134 have several candidates.

The 421 calls connect 538 files in 168 components: 99 components have two files, and the largest has 31 files, where 30 files call the same process. Each file became one IPFS resource, and each call activity refers to the called process through `calledElementRef`. Every file of every component was then renamed in turn, with one cascade save per change.

| Cycle | Changes | New CIDs match | Consistent | Loads fully |
|---|---:|---:|---:|---:|
| no | 536 | 536 (100%) | 536 (100%) | 536 (100%) |
| yes | 2 | 1 | 0 | 0 |

The one cyclic component holds two files of the Bonita export in the MIWG suite that call each other.

| Resources with a new CID | Changes | Median (ms) | Median per new CID (ms) |
|---:|---:|---:|---:|
| 1 | 332 | 8.2 | 8.2 |
| 2 | 129 | 15.4 | 7.7 |
| 3 | 45 | 23.7 | 7.9 |
| 4 | 10 | 33.0 | 8.3 |
| 6 | 8 | 46.5 | 7.8 |
| 17 | 1 | 135.7 | 8.0 |
| 31 | 1 | 679.2 | 21.9 |

The chains are short: the deepest has four edges above the changed file (five files), and the largest fan-in is 30. A cascade costs about 8 ms per resource with a new CID up to 17 resources. The single change with 31 new CIDs took 679 ms; in an earlier run of the same experiment it took 312 ms.

## Files

| File | Content |
|---|---|
| `summary.md` | every table of the run |
| `split.tsv.gz` | one row per split model, for the collections whose terms allow redistribution |
| `calls.tsv.gz` | one row per change of the call graphs |

The model lists and the raw tables stay in `eval/bpmn-rq2/raw`, which git ignores, because the list of multi-pool models names files of Camunda bpmn-for-research.

## Rerunning

```bash
bash eval/bpmn-rq2/run.sh compile
bash eval/bpmn-rq2/run.sh lists      # needs the cleaning logs of eval/datasets/bpmn
bash eval/bpmn-rq2/run.sh split      # about three minutes
bash eval/bpmn-rq2/run.sh calls      # about half a minute
bash eval/bpmn-rq2/run.sh summary
```
