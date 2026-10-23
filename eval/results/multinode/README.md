# Multi-node results

Results of the multi-node experiment in `eval/multinode` (see the README there for the method). The full run writes four files into this folder:

| File | Content |
|---|---|
| `metrics.csv` | one row per measured operation |
| `summary.md` | medians and interquartile ranges, written by `summarize.py` |
| `environment.txt` | settings of the run and versions of the host, Docker, Kubo, Java and the jars |
| `run.log` | everything the run printed |

The full run has not been made yet. Start it on a quiet machine with

```sh
bash eval/multinode/run.sh
```

`validation/` holds the last quick run (`run.sh --quick`, run `20261023-095500`) and `probe-large-models.csv`, one iteration of the three largest models used to estimate the duration.

## Columns of metrics.csv

| Column | Meaning |
|---|---|
| `run` | identifier of the run, the start time |
| `topology` | `peered` or `dht` |
| `profile` | `none`, `lan`, `wan50` or `wan100` |
| `workload` | `models`, `bpmn` or `ipns` |
| `label` | number of elements, path of the BPMN file below `eval/datasets/bpmn`, or `attempts=<n>` for IPNS (resolutions until mn-b saw the new CID) |
| `bytes` | size of the content in bytes |
| `iteration` | measured iteration, starting at 1 |
| `operation` | `save_a`, `cat_b`, `load_c`, `add_a`, `publish_a`, `resolve_b`, `visible_b` or `visible_b_timeout` |
| `milliseconds` | duration, three decimals |
| `connected_to_publisher` | whether the fetcher held a connection to mn-a when the operation started; empty for operations on mn-a |

## Validation on 2026-10-23

The quick run covered both topologies with the profiles `none` and `wan50`: 1 warm-up and 3 measured iterations for the models of 100 and 1,000 elements, 1 warm-up and 3 measured BPMN files, and 1 warm-up and 3 measured IPNS publications. It took 4.5 minutes, wrote 132 rows and removed the nodes and their volumes at the end. Every fetch and resolution in the peered topology started with a connection to mn-a (36 of 36 rows), and none in the dht topology did (0 of 36).

Medians in milliseconds:

| | peered, none | peered, wan50 | dht, none | dht, wan50 |
|---|---:|---:|---:|---:|
| model of 100 elements, `cat_b` | 23.4 | 221.2 | 1,008 | 1,710 |
| model of 100 elements, `load_c` | 29.5 | 252.6 | 1,034 | 1,688 |
| model of 1,000 elements, `cat_b` | 24.9 | 334.4 | 1,029 | 1,818 |
| BPMN file, `cat_b` | 28.1 | 231.7 | 1,030 | 1,673 |
| IPNS `publish_a` | 6.2 | 512.8 | 6.8 | 229.9 |
| IPNS `resolve_b` | 1.9 | 103.2 | 4.7 | 606.3 |
| IPNS `visible_b` | 8.0 | 618.9 | 11.5 | 839.8 |

Saving these small models on mn-a takes 11 to 20 ms in every profile, since the announcement of the root CID runs in the background. In the dht topology every fetch takes at least one second: Bitswap first asks the connected peers and starts the provider lookup only after `Internal.Bitswap.ProviderSearchDelay`, 1 s by default. The first resolution on mn-b always returned the new CID.

The probe ran one iteration without warm-up of the models of 10,000, 100,000 and 500,000 elements, with a heap of 2 GB. Its dht row ran before the final connection policy. For the models the earlier policy gave the same cold fetches, because mn-b and mn-c take turns. Durations in seconds for the model of 500,000 elements (39.6 MB):

| | `save_a` | `cat_b` | `load_c` |
|---|---:|---:|---:|
| peered, none | 0.5 | 0.4 | 1.0 |
| peered, lan | 0.6 | 0.7 | 1.2 |
| peered, wan50 | 0.5 | 9.5 | 9.8 |
| peered, wan100 | 0.5 | 17.5 | 18.7 |
| dht, wan100 | 0.7 | 26.9 | 22.7 |

## Duration of the full run

The estimate comes from the probe and the validation. Per topology and profile it adds 33 iterations of the five models, 33 BPMN files, 33 IPNS publications and about a minute to start the nodes and the JVM. In the dht topology each fetch adds the provider search delay, the lookup, a new connection, and the wait of the connection policy (0.5 s after the close, and up to 3 s after the last request of the same node).

| | none | lan | wan50 | wan100 |
|---|---:|---:|---:|---:|
| peered | 3 min | 3 min | 18 min | 31 min |
| dht | 18 min | 20 min | 40 min | 65 min |

In total about 3 h 20 min. Since the fetches of the largest model vary by several seconds, plan for 3 to 4.5 hours. Without `wan100` (`PROFILES="none lan wan50"`) the run takes about 1 h 45 min. The full run uses a fixed heap of 8 GB like the RQ3 benchmark; the probe loaded the largest model with 2 GB, so `HEAP=4g` also works if memory is short.
