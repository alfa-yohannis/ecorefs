# Fixed cost of a save through IPFS

The RQ3 benchmark found that a save through IPFS costs a nearly constant extra time for small models, and that the extra time differed between machines. This probe finds where that time goes. It times one `add` of a generated model to a fresh offline Kubo 0.40.1 node in several ways:

| Variant | What it changes |
|---|---|
| `library` | The call of `IPFSResourceImpl.saveIPFS`: `IPFS.add` of the Java IPFS client 1.4.3, which sends `Expect: 100-continue` and writes the multipart body in chunks. |
| `library` with `-Dhttp.keepAlive=false` | A new TCP connection for every request. |
| `single-write` | One request with a fixed length that sends the whole body in one write. |
| `single-write-no-pin` | The same request with `pin=false`, so the node does not record a pin for the file. |
| `curl`, with and without the pin | A client outside the JVM. |
| node on tmpfs | A second node whose repository lies in memory, where a synchronous write returns at once. |

`dd` with `oflag=dsync` measures how long one synchronous 4 KiB write takes in the home folder, which lies on the same disk as the Docker data on the measured machines.

## Run

```sh
bash eval/rq3-add-probe/run.sh                 # writes eval/results/rq3-add-probe/<host>.txt
JAVAC_HOME=/usr/lib/jvm/java-17-openjdk-amd64 bash eval/rq3-add-probe/run.sh   # when the Java 21 runtime has no javac
```

Requirements: Docker, curl, a Java 21 runtime (`JAVA_HOME`, default `/usr/lib/jvm/java-21-openjdk-amd64`), javac for Java 17 or later, the generated models in `eval/custom_generator/output`, and the five client jars named in `run.sh` in `~/.m2/repository`, which a build of `core-plugin` downloads. The script compiles the probe into `build/`, starts the two nodes on 127.0.0.1:5199 and 127.0.0.1:5200 (`PORT` moves them), and removes the nodes at the end. Each variant runs three warm-up adds and thirty measured adds of `model_100.xmi` and of `model_10000.xmi`.

## Results

`eval/results/rq3-add-probe/<host>.txt` holds the output of each machine: the times of every measured add, their mean and median, and the datastore configuration of the node.
