# Research data of "Towards Decentralized Model Persistence"

This archive holds the data, the evaluation results and the source code behind the paper "Towards Decentralized Model Persistence" by Alfa Yohannis (Pradita University), submitted to the Journal of Systems and Software. The paper stores models of the Eclipse Modeling Framework (EMF) on the InterPlanetary File System (IPFS), where a model is identified by a content identifier (CID) computed from its content.

## Files

| File | Content | Terms |
|---|---|---|
| `modisco-jdt-models.tar.gz` | MoDisco models of four repositories of the Eclipse Java development tools (JDT): the 39 XMI files (1.04 GiB), the merged model, the merged model partitioned into 6,933 resources, the published CIDs (`ipfs-cids.md`) and the size metrics | Eclipse Public License 2.0, the license of Eclipse JDT, from which the models are derived |
| `generated-models.tar.gz` | The five generated models of the timed benchmarks (RQ3, the baselines and the multi-node run), with 100 to 500,000 elements | MIT, as the ecorefs repository |
| `bpmn-collections.tar.gz` | BPMN 2.0 models of the OMG MIWG test suite and of the test resources of jBPM, Flowable and Camunda 7, cleaned so that every file loads into the Eclipse BPMN2 metamodel; the change log of every edit, the list of removed files, the per-file load results, and the tools that fetched, cleaned and checked the models | Each collection keeps its license file: CC BY 3.0 for MIWG, Apache 2.0 for the engine tests |
| `evaluation-results.tar.gz` | The result files of the paper: the measurements and summaries of the RQ3 benchmark, the baselines, the multi-node run and the add probe, each with the machine and the versions of its run, the MoDisco case study reports, and the experiments on real BPMN models | MIT, as the ecorefs repository |
| `ecorefs-source.tar.gz` | The ecorefs repository: the EMF persistence layer for IPFS, its tests, the case study, the evaluation code and the paper sources | MIT |
| `bpmn2-modeler-ecorefs-source.tar.gz` | The Eclipse BPMN2 Modeler 1.5.4 with the IPFS commands | Eclipse Public License 1.0, as upstream |
| `bpmn2-metamodel-ecorefs-source.tar.gz` | The Eclipse BPMN 2.0 metamodel with the read-only IPFS URI handler of its tree editor | Eclipse Public License 2.0, as upstream |
| `SHA256SUMS` | SHA-256 checksums of the files above | |

The MoDisco models were discovered from these commits of https://github.com/eclipse-jdt: `eclipse.jdt.core` fb1844c934, `eclipse.jdt.core.binaries` bf66653, `eclipse.jdt.debug` da7bd866e and `eclipse.jdt.ui` 0a44971ca7.

The files are also on the public repositories https://github.com/alfa-yohannis/ecorefs, https://github.com/alfa-yohannis/org.eclipse.bpmn2-modeler-1.5.4-ecorefs and https://github.com/alfa-yohannis/org.eclipse.bpmn2-ecorefs, except the datasets, which the ecorefs repository does not track.

## Data not included

The paper also uses the Camunda bpmn-for-research collection (https://github.com/camunda/bpmn-for-research, commit `1416f6f`, 3,739 files). Its terms allow academic and research use but do not mention redistribution, so the archive leaves the files out. `bpmn/_load-check/tools/fetch.sh` clones the collection; checking out commit `1416f6f` gives the files of the paper, and `clean.sh` repeats the cleaning. The archive keeps only aggregate numbers about the collection.

## Reproducing the results

The README files inside `ecorefs-source.tar.gz` describe the setup: Java 11 or newer and Maven for the persistence layer, Java 17 for the Eclipse tools, and Kubo 0.40.1 in Docker as a private network of local nodes. The test classes of `core-plugin` and the programs in `eval` produce the result files. The timed benchmarks ran on an HP Envy laptop, and the baselines and the multi-node run need Linux for sshfs and `tc netem`. The same content always receives the same CID, so a new run of the BPMN case study on the same files gives the CIDs printed in the paper.
