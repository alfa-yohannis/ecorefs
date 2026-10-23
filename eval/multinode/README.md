# Multi-node experiment

This harness measures ecorefs across several IPFS nodes on one host, for the item "Measure more than one node" in `papers/01-JSS/TODO.md` (section 3). Four Kubo containers form a private network, and `tc netem` adds the delay of a LAN or a WAN between them. The harness measures two things:

- a fetch by CID on a node that holds no block of the content, as raw bytes and as an EMF load through `IPFSResourceImpl` and `IPFSURIHandlerImpl`;
- an IPNS publication on one node, its resolution on another node without the cache, and the time until the other node sees the new CID.

The nodes never join the global IPFS network. They share a swarm key, have no bootstrap peers, and `LIBP2P_FORCE_PNET` stops a node that has no key.

## Start

```sh
bash eval/multinode/run.sh            # full run, see "Duration"
bash eval/multinode/run.sh --quick    # validation, about five minutes
```

Run it from any folder. `run.sh` writes a swarm key on first use, builds the harness with `build.sh`, and then works through every topology and profile. For each pair it removes the nodes and their volumes, starts four new nodes, sets the peering with `topology.sh`, sets the delay with `netem.sh`, and starts one JVM. At the end it removes the nodes and writes `summary.md`. The full run refuses to start when `eval/results/multinode/metrics.csv` exists, so a second run cannot mix with the first.

Environment variables change the defaults: `TOPOLOGIES="peered"`, `PROFILES="none wan50"`, `HEAP=8g` and `OUT=<folder>`. Options after the mode go to the harness, for example `--sizes 100,1000` or `--workloads models,ipns`.

Requirements: Docker with the Compose plug-in, Java 17, Python 3, the jars named in `build.sh` in `~/.m2` (read only), `core-plugin/target/ecorefs-ipfs-core-1.0-SNAPSHOT.jar`, the generated models in `eval/custom_generator/output`, and the cleaned BPMN collections in `eval/datasets/bpmn`. `netem.sh` builds a small Alpine image with `tc` (`ecorefs-netem:1`) and runs it in the network namespace of each node with `NET_ADMIN`. The host needs the `sch_netem` kernel module, which loads on demand. No step needs sudo.

## Network

| Node | Container | API on the host | DHT | Role |
|---|---|---|---|---|
| mn-a | `ecorefs-mn-a` | 127.0.0.1:5201 | server | publisher: saves, adds and publishes IPNS records |
| mn-b | `ecorefs-mn-b` | 127.0.0.1:5202 | client | raw fetch with `cat`, IPNS resolution |
| mn-c | `ecorefs-mn-c` | 127.0.0.1:5203 | client | EMF load through ecorefs |
| mn-h | `ecorefs-mn-h` | 127.0.0.1:5204 | server | hub in the dht topology |

All four nodes run Kubo 0.40.1 over TCP. `init/010-private-network.sh` configures them on every start; it follows `docker/kubo/init`, but mDNS is off, so the peering alone decides which nodes connect. The publisher and the hub are DHT servers and store the provider and IPNS records. The fetchers are DHT clients, as an editor behind a home router would be, so they hold no records and have to ask a server. Kubo keeps its defaults otherwise: an add announces the root CID in the background (`Import.FastProvideRoot`) and returns without waiting for the announcement. With `Import.FastProvideWait` on, a 40 MB add took 5.6 s instead of 0.09 s over wan100, because the wait grows with the number of blocks.

Topologies (`topology.sh`):

- `peered`: every node keeps a connection to every other node through `Peering.Peers`. A fetcher asks its connected peers first, so it reaches the publisher directly.
- `dht`: mn-a, mn-b and mn-c keep a connection only to the hub mn-h. Before each fetch the fetcher closes its connections to the publisher (`DisconnectFromPublisher`). Once the fetch starts, Bitswap asks the connected peers, none of which holds the content, and starts a provider lookup after `Internal.Bitswap.ProviderSearchDelay` (1 s by default). The fetcher then dials the publisher and fetches. The DHT has two servers, so a lookup needs one round trip; in a large DHT it takes several hops.

The close has to wait, because a request leaves activity behind. After a fetch over wan50, mn-b connected to mn-a again after 0.6 s and 1.6 s, and fetches that started right after a close found mn-a connected and took 0.7 s instead of 1.7 s. Closing each connection as it came back was worse: when a close met a dial in progress, libp2p put mn-a into its dial backoff, and four of five BPMN fetches then took 42 to 61 s, until the periodic provider search of Bitswap. The policy therefore waits until 3 s have passed since the last request of the fetcher, closes the connections once, and checks 0.5 s later that none has come back; otherwise it waits and closes again, up to five times. With this policy every fetch of the validation runs over wan50 took between 1.56 and 1.75 s.

Delay profiles (`netem.sh`), one-way per node:

| Profile | netem setting | Round trip between two nodes |
|---|---|---|
| `none` | no qdisc | about 0.2 ms |
| `lan` | `delay 1ms 0.2ms distribution normal rate 1gbit` | about 2 ms |
| `wan50` | `delay 50ms 10ms distribution normal rate 1gbit` | about 100 ms |
| `wan100` | `delay 100ms 20ms distribution normal rate 1gbit` | about 200 ms |

Every node delays the packets it sends to or from the swarm port 4001, so both directions of a connection carry the delay. The API port stays undelayed: the harness stands for an application next to its own node.

netem draws the delay of each packet on its own, so with jitter alone a packet can overtake the one before it, and TCP takes the reordering for loss. Over wan100 a 40 MB fetch then took 23 s once and 65 s the next time; with a fixed delay of 100 ms it took 13.5 to 19.6 s. The `rate` makes netem send no packet before the previous one, which keeps the order while the delay still varies; the same fetch took 17 to 18 s. At 1 Gbit/s the rate stays above the 425 Mbit/s that a fetch reached with no delay. The queue holds 10,000 packets, so netem drops none.

## Measurements

Every duration is `System.nanoTime` around one call and goes to the CSV in milliseconds with three decimals.

| Workload | Operation | What the timer covers |
|---|---|---|
| `models` | `save_a` | `IPFSResourceImpl.save` of a stamped copy on mn-a; the announcement of the root CID runs in the background |
| `models` | `cat_b` | `cat` of the new CID through the API of mn-b |
| `models` | `load_c` | `Resource.load` of `ipfs://<CID>` in a fresh resource set bound to mn-c |
| `bpmn` | `add_a` | `add` of one BPMN file on mn-a |
| `bpmn` | `cat_b` | `cat` of that CID through mn-b |
| `ipns` | `publish_a` | `name publish` of a new CID on mn-a |
| `ipns` | `resolve_b` | the first `name resolve --nocache` on mn-b |
| `ipns` | `visible_b` | from the start of `publish_a` until mn-b resolves the new CID; `visible_b_timeout` after 300 s |

The generated models are those of the RQ3 benchmark (`eval/custom_generator/output`, 100 to 500,000 elements, 4 KB to 40 MB). Each iteration copies the model in memory and writes a new number into the `id` of the root (a random base per run plus the iteration), so every save has a new root CID. Since the stamp changes only the first block, each fetcher runs a garbage collection of its repository right after its fetch. A fetch therefore starts on a node that holds no block of the model, and mn-a is the only node that holds it. The JVM runs `System.gc()` after each iteration and has a fixed heap (`-Xms` = `-Xmx`).

The BPMN files come from `bpmn-sample.tsv`, which `select_bpmn_sample.py` writes from the MIWG suite and the jBPM, Flowable and Camunda 7 test resources (7,779 files). The 30 measured files sit at evenly spaced size quantiles, from 167 bytes to 907 KB, and 3 warm-up files near the median come first. The harness takes the first `--warmup` lines as warm-up, so the list matches the default of 3; the quick run uses 1 warm-up file and measures the next 3 lines. The harness appends a comment with the run label after the root element, so the content is new in every run, and mn-b removes the blocks after each fetch. The Camunda research collection stays out, because its terms keep it on an offline node.

Per topology and profile the full run has 3 warm-up and 30 measured iterations per model size, 3 warm-up and 30 measured BPMN files, and 3 warm-up and 30 measured IPNS publications. The column `connected_to_publisher` records whether the fetcher held a connection to mn-a when the fetch started. In the validation runs it read `true` for every fetch in the peered topology and `false` for every fetch in the dht topology.

## Duration

See `eval/results/multinode/README.md` for the times of the validation run and the estimate for the full run.

## Limits

- All nodes share the CPU, memory and disk of one host, and the harness runs next to them. The emulated network has delay, jitter and a rate of 1 Gbit/s per node, but no loss and no reordering.
- The DHT has two servers. Every lookup and every IPNS record reaches its target in one hop, which a public DHT does not.
- The background provider of Kubo reports `offline` in this network (`ipfs provide stat`) and announces no blocks. Only the immediate announcement of each root CID (`Import.FastProvideRoot`) reaches the hub: 2.5 s after an add, `routing findprovs` on the hub answered with mn-a in 2 ms. That is enough here, because Bitswap fetches the other blocks from the peer that had the root.
- The API calls of the harness are not delayed. `repo gc` and the connection policy run outside the timers; for IPNS the policy runs before the publication, so `visible_b` does not include it.

## Files

| File | Purpose |
|---|---|
| `compose.yaml` | the four nodes, their volumes and the network `ecorefs-multinode` |
| `init/010-private-network.sh` | node configuration, run by Kubo on every start |
| `new-swarm-key.sh` | writes `swarm.key` (ignored by git) |
| `topology.sh` | sets `Peering.Peers` for `peered` or `dht` and restarts the nodes |
| `netem.sh`, `netem/Dockerfile` | apply or remove a delay profile |
| `build.sh` | compiles the harness into `build/` |
| `run.sh` | runs the experiment and writes the results |
| `select_bpmn_sample.py`, `bpmn-sample.tsv` | the BPMN sample |
| `summarize.py` | turns `metrics.csv` into `summary.md` |
| `src/org/ecorefs/eval/multinode/` | the Java harness; `MultiNodeBenchmark` is the entry point |
