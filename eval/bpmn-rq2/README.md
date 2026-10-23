# RQ2 driver: cascade saves on real BPMN structures

The driver runs two experiments on the cleaned collections of `eval/datasets/bpmn` and checks every cascade save against the propositions of the paper. `eval/results/bpmn-rq2/README.md` describes the setup and the results.

- **split**: splits every model with two or more pools into one resource per process, one for the collaborations with the diagrams and one for the other root elements, publishes the parts and renames a process and a shared element.
- **calls**: turns the call links between files into references between IPFS resources, one resource per file, and renames every file of every connected component in turn.

## Running

```bash
bash eval/bpmn-rq2/run.sh compile   # javac 17 over src and eval/bpmn-common/src
bash eval/bpmn-rq2/run.sh lists     # writes raw/multi-pool-models.tsv and raw/called-element-links.tsv
bash eval/bpmn-rq2/run.sh split     # node on 5104
bash eval/bpmn-rq2/run.sh calls     # node on 5105
bash eval/bpmn-rq2/run.sh summary   # writes eval/results/bpmn-rq2
```

`split 30` limits the run to the first 30 models. The lists come from two Python scripts:
- `list_multi_pool_models.py` counts the participants with a process.
- `resolve_called_elements.py` rebuilds the call links that the cleaning removed, from the change logs of the dataset.

## Classes

Package `org.ecorefs.eval.rq2`:

| Class | Role |
|---|---|
| `Rq2Main` | entry point with the commands `split` and `calls` |
| `SplitExperiment`, `SplitModel` | the split of multi-pool models and its row per model |
| `CallGraphExperiment`, `CallLink`, `CallComponents` | the call graphs between files |
| `CascadeTrial`, `NameChange` | one change with one cascade save, and the three checks |
| `ReferenceGraph` | the references between the resources, found by a scan independent of the core plugin |
| `SnapshotCheck` | reads the stored bytes back by CID and classifies every `ipfs://` URI |
| `LoadCheck` | loads the top resource by CID in a fresh resource set and resolves every proxy |
| `CidHistory`, `BpmnDefinitions` | the CIDs each resource had, and the definitions element of a BPMN model |
