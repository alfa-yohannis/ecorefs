# RQ1 driver: round trip of real BPMN models

The driver saves every original BPMN file of `eval/datasets/bpmn-original` through seven persistence paths, loads it back, compares it with the saved model and saves it a second time to check that the same model receives the same identifier. It also adds every file to a fresh node, one add per file and one directory per collection. `eval/results/bpmn-rq1/README.md` describes the setup and the results.

## Running

```bash
bash eval/bpmn-rq1/run.sh compile      # javac 17 over src and eval/bpmn-common/src
bash eval/bpmn-rq1/run.sh roundtrip    # node on 5101, one virtual machine per collection
bash eval/bpmn-rq1/run.sh statistics   # recounts the model properties
bash eval/bpmn-rq1/run.sh dedup        # node on 5102
bash eval/bpmn-rq1/run.sh directory    # node on 5106
bash eval/bpmn-rq1/run.sh summary      # writes eval/results/bpmn-rq1
```

`roundtrip 20` limits the run to 20 files per collection. Each step starts its own Kubo 0.40.1 container in offline mode, reachable only on 127.0.0.1, and removes it at the end. The jars come from `~/.m2`, the same versions as the pom of the core plugin. Raw tables go to `raw/` and class files to `build/`, both ignored by git.

## Classes

Package `org.ecorefs.eval.rq1`:

| Class | Role |
|---|---|
| `Rq1Main` | entry point with the commands `roundtrip`, `statistics`, `dedup` and `directory` |
| `RoundTripRun`, `ModelRoundTrip` | the loop over the files of one collection, with a time limit per file, and the row of one file |
| `PathTrial` | save, load, comparison and second save of one file on one path |
| `PersistencePath` | the common type of the paths: `LocalXmiPath`, `IpfsXmiPath`, `LocalBpmnXmlPath`, `IpfsBpmnXmlPath` |
| `ModelComparer` | the structural comparison and the categories of differences |
| `DocumentOrderIds` | numbered ids for objects without one, before the BPMN save |
| `ModelStatistics`, `StatisticsRun` | size, objects without id, vendor entries and external proxies of a model |
| `DeduplicationRun`, `DirectoryPublicationRun`, `DirectoryTree` | the two publication runs |

The classes shared with the RQ2 driver live in `eval/bpmn-common/src`, package `org.ecorefs.eval.bpmn`:
- the restricted resource sets and URI handlers;
- the loader of source files;
- the Kubo client;
- the removal of vendor extensions.
