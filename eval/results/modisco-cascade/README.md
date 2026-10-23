# Cascade on the partitioned MoDisco model

This analysis asks what a cascade save in CID mode would do to the partitioned MoDisco model of the case study. That model has 6,934 resources: the root model, 6,930 compilation units and three shared resources in `aux`. `eval/modisco-cascade/resource_graph.py` reads every resource of `eval/datasets/modisco-java-models-by-repo/all-repos-merged-by-compilation-unit` and collects its serialized cross-references: every `href` that names another resource. An edge from r to q means that r stores the location of q, so in CID mode r would store the CID of q and need a new save after every change of q. The analysis needs no EMF load, no node and no save.

`resource-graph.md` and `resource-graph.json` hold the numbers of the run on 2026-10-23.

## Results

| Measure | Value |
|---|---:|
| resources | 6,934 |
| edges between resources | 92,184 |
| serialized hrefs into other resources | 2,393,902 |
| strongly connected components | 1 |
| resources in the largest component | 6,934 |
| edges between compilation units of different components | 0 |

**The whole model is one cycle.** The root model refers to every compilation unit: its model element lists all 6,930 in `compilationUnits`, and its packages list the usages in the compilation units. The compilation units refer back to the root model, for example to their package, and to the shared resources. The shared resources in turn refer to the compilation units, `aux/orphan-types.xmi` alone through 153,741 hrefs. Every resource therefore reaches every other one, and the affected set of any change holds all 6,934 resources. A change to a test class that no other compilation unit references affects the same 6,934 resources as a change to the root model. By Proposition 3 of the paper, no sequence of saves in CID mode makes such a model consistent. `IPFSModelPublishTool` refuses a CID publication of a model with a cycle for this reason.

**The layered publication needs no cascade.** The publication of the case study puts each component into one IPFS directory. Inside the published directories, the resources refer to each other by relative paths, not by CIDs. A change to one compilation unit therefore gives new CIDs only to these objects:
- the file of the compilation unit;
- every directory from its folder up to the component root;
- the component manifest;
- the project version manifest.

That is 9 to 11 objects for the compilation units in the table of `resource-graph.md`, plus one new IPNS record, instead of 6,934 saves of 1.04 GiB.

**The manifests see none of this.** The component manifests list 92 dependencies, all from components to the four shared resources, and no compilation unit refers to a compilation unit of another component. A dependency lookup from the manifests therefore predicts that a change stays inside its component and the shared resources, while the serialized references put every resource into one cycle through the root model.

## Rerunning

```bash
python3 -I eval/modisco-cascade/resource_graph.py \
  eval/datasets/modisco-java-models-by-repo/all-repos-merged-by-compilation-unit \
  eval/results/modisco-cascade
```

The run takes about 20 seconds. Hrefs are percent-decoded before they are resolved, because folder names such as `console tests` appear encoded.
