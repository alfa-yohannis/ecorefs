# RQ1 on real BPMN models

These results answer the RQ1 questions of the paper on real models instead of an example program. Does a model saved through the IPFS layer come back unchanged? Does the same model always receive the same CID? How do identical files and whole collections behave on a node? `eval/bpmn-rq1/run.sh` produced every file here on 2026-10-23; `summary.md` holds all tables.

## Setup

- **Models.** The 11,533 BPMN files of the five collections in `eval/datasets/bpmn-original`, as downloaded and before the cleaning. `eval/datasets/bpmn/README.md` describes the collections. 11,486 files load into a model, 515 of them with load errors such as unresolved references; 47 files yield no model at all.
- **Software.** The core plugin of this repository with its own dependencies: EMF Ecore 2.23.0, EMF XMI 2.16.0, EMF Common 2.21.0, the Java IPFS client 1.4.3 and the Eclipse BPMN 2.0 metamodel 1.5.1. Code compiled with javac 17 and run on OpenJDK 21.0.9.
- **Nodes.** Each step used its own Kubo 0.40.1 container, offline and with an empty repository, reachable only on 127.0.0.1.
- **Safety.** The resource sets read IPFS only through the node of the step and read files only inside the dataset and the temporary folder. Every other URI is refused, external DTDs and entities are switched off, and the experiment never follows a reference into another file.

## Persistence paths

Each model went through seven paths, each with a fresh load of the file and a fresh resource set for every save and every load.

| Column prefix | Save | Load |
|---|---|---|
| `xmi_local` | plain XMI to a local file | XMI resource from the file |
| `xmi_ipfs` | `IPFSResourceImpl.saveIPFS` with default options, as the core plugin publishes | `IPFSResourceImpl` by CID through `IPFSURIHandlerImpl` |
| `emd_local`, `emd_ipfs` | as above, with `XMLResource.OPTION_EXTENDED_META_DATA` | as above, with the same option |
| `bpmn_local` | BPMN 2.0 XML through the BPMN resource of Eclipse to a local file | BPMN resource from the file |
| `bpmn_ipfs` | the same BPMN 2.0 XML, added to the node as the modeler publishes a file | BPMN resource by CID through `IPFSURIHandlerImpl` |
| `bpmn_ipfs_numbered` | as `bpmn_ipfs`, after every object without an id received one in document order (`Task_1`, `Task_2`, ...) | as `bpmn_ipfs` |

Before a save, the references that point into the same file are resolved, because the BPMN resource loads every QName reference as a proxy. References into other files stay proxies and are never loaded.

**Comparison.** The loaded model is compared with the saved model. The comparison walks both containment trees and checks the class, the set state and the value of every stored feature, including the entries of feature maps, which hold the vendor extensions. A cross-reference must lead to the matching object, or, for a target in another file, name the same file and fragment. The first difference is recorded with a category:

- `extension-attribute`: an attribute of a vendor namespace.
- `feature-map`: other feature map entries, mostly vendor extension elements.
- `dangling-reference`: a reference to an id that exists nowhere in the file.
- `duplicate-id`: a reference to an id that the file declares twice.
- `reference`: other references.
- `value`: attribute values.
- `content`: objects that are missing or extra.

**Determinism.** A second fresh load of the same file is saved through the same path, and the two CIDs are compared; local paths compare SHA-256 digests.

## Results

| Path | Save succeeds | Loaded model equals saved model | Second save gives the same identifier |
|---|---:|---:|---:|
| Plain XMI, local file | 5,505 (47.9%) | 2,585 (22.5%) | 5,505 of 5,505 |
| Plain XMI, IPFS | 5,505 (47.9%) | 2,585 (22.5%) | 5,505 of 5,505 |
| XMI with extended metadata, local file | 11,484 (100.0%) | 0 | 11,484 of 11,484 |
| XMI with extended metadata, IPFS | 11,484 (100.0%) | 0 | 11,484 of 11,484 |
| BPMN 2.0 XML, local file | 11,484 (100.0%) | 10,587 (92.2%) | 1,850 of 11,484 (16.1%) |
| BPMN 2.0 XML, IPFS | 11,484 (100.0%) | 10,587 (92.2%) | 1,850 of 11,484 (16.1%) |
| BPMN 2.0 XML, IPFS, numbered ids | 11,484 (100.0%) | 10,587 (92.2%) | 11,484 of 11,484 |

Percentages are of the 11,486 models that loaded.

**The IPFS layer adds no failure of its own.** For every one of the 11,486 models, each local path and its IPFS counterpart gave the same outcome. The first difference was the same text for all models on both XMI pairs. On the BPMN pair it matched for 99.2%, and the other 0.8% differ only in the random UUIDs that the save writes into the path of the difference.

**Plain XMI cannot store vendor extensions.**
- 5,981 saves (52.1%) end in a `NullPointerException` in `XMLSaveImpl`, because the model holds vendor extension elements in a feature map and plain XMI has no extended metadata to write them.
- Of the saves that succeed, 2,554 models lose their vendor attributes on load (for example `camunda:type` or `drools:packageName`, reported as `Feature 'class' not found` and similar), and 364 lose other feature map entries.
- 2,585 models (22.5%) round-trip unchanged.

**Extended metadata does not help.** With `OPTION_EXTENDED_META_DATA`, the save succeeds for all models but the 2 that fail on every path, yet no model loads back. The save unwraps the document root of the BPMN model and writes `definitions` as the root element. The XMI loader of EMF creates a document root only for packages that it creates on demand, so it does not recognize that element ("Feature 'definitions' not found") and reads the rest of the file as unrelated objects.

**BPMN 2.0 XML round-trips 92.2% of the models.** The rest change in the BPMN serializer of Eclipse:
- **Dangling references (535 models, 4.7%).** A reference names an id that exists nowhere in the file, as `structureRef="String"` does in 386 item definitions. The first load keeps such a reference as a proxy, but the reload of the saved file reports it as unresolved and leaves it unset. In one checked example the original file binds the BPMN namespace as default namespace and the saved file does not.
- **Other references (252 models).**
  - In 121 models the diagram plane loses the element it shows (`bpmnElement`). In one checked example the save writes the process id with the `tns:` prefix and the reload leaves the reference unset.
  - In 82 models an input set lists more output sets after the reload than before, for example 4 instead of 2.
  - In the rest a participant, an input or output set, a shape or a label loses its target.
- **Mixed content and objects.** 75 models change mixed content: documentation text loses entries, or a vendor element comes back under another namespace. 21 models lose objects, 20 of them because the saved file declares a namespace prefix (`_0`) that the reload rejects. 12 saved files do not load.

The local BPMN path shows the same differences, so they come from the BPMN serializer and not from IPFS.

**BPMN 2.0 XML on IPFS is not deterministic.** All 9,634 models with at least one object without an id received a new CID on the second save, and none of the 1,850 models without such objects did. `Bpmn2ResourceImpl.prepareSave` gives every object without an id a fresh `EcoreUtil.generateUUID()`. With ids numbered in document order before the save, as the BPMN2 Modeler numbers new elements (`ModelUtil.setID`), every model received the same CID twice. The XMI path never changed its CID.

**Deduplication.** One add per file on a fresh node:
- 11,533 files gave 10,393 distinct CIDs, so 1,140 files had the CID of an earlier file. The distinct contents hold 197.8 MB of the 207.4 MB.
- Kubo reported a repository growth of 207.1 MB.
- On disk, the block folder took 269.1 MB, against 234.7 MB for a plain copy of all files in 4 KiB blocks. Every block is a file of its own, so the block store needs more space on disk than the duplicates save.
- The adds took 69.4 s, 166 models per second.

**One directory per collection.** Adding each collection as one IPFS directory published the same 11,533 files in 11.6 s on another fresh node, about 1,000 models per second, six times the rate of one add per model.

**Times.** The medians in `summary.md` (an IPFS save about 10 ms slower than a local save, a load about 1 ms slower) come from five virtual machines running in parallel. They are indicative only; the RQ3 benchmark measures times on a quiet machine.

## Files

| File | Content |
|---|---|
| `summary.md` | every table of the run |
| `roundtrip.tsv.gz` | one row per file with the outcome, first difference, determinism check and CID of each path, for the four collections whose terms allow redistribution; Camunda bpmn-for-research appears in the aggregates only |

The raw tables, including the per-file times and the deduplication log, stay in `eval/bpmn-rq1/raw`, which git ignores.

## Rerunning

```bash
bash eval/bpmn-rq1/run.sh compile
bash eval/bpmn-rq1/run.sh roundtrip     # about one hour, five virtual machines in parallel
bash eval/bpmn-rq1/run.sh statistics
bash eval/bpmn-rq1/run.sh dedup
bash eval/bpmn-rq1/run.sh directory
bash eval/bpmn-rq1/run.sh summary
```

Set `TEMPORARY` to a folder on a fast local disk for the local saves; the run on 2026-10-23 used an ext4 folder.
