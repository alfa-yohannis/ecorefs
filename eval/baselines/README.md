# Baselines for the RQ3 benchmark

This harness measures the generated models of the RQ3 benchmark (100 to 500,000 elements, `eval/custom_generator/output`) with three more stores next to the local XMI file and IPFS, for the item "Baselines on the same models" in `papers/01-JSS/TODO.md` (section 3). In every iteration each store gets its own copy of the same model, stamped with the iteration number, and the stores run one after the other. The series are therefore paired, as the XMI and IPFS series of the paper are.

## Start

```sh
bash eval/baselines/run.sh validate   # sizes 100 to 10,000, 1 warm-up and 3 measured iterations, 2 GB heap
bash eval/baselines/run.sh final      # sizes 100 to 500,000, 3 warm-up and 30 measured iterations, 8 GB heap
```

Options after the mode go to the harness and override the defaults of the mode, for example `--stores xmi,git --sizes 100,1000`. Environment variables: `BASELINE_LABEL` names the result files, `BASELINE_WORK` moves the work folder, `BASELINE_JAVA_HOME` selects the JDK and `MAVEN` the Maven binary. The final mode refuses to start when its CSV file exists.

`run.sh` installs `core-plugin/target/ecorefs-ipfs-core-1.0-SNAPSHOT.jar` into the Maven repository `.m2-eval` and builds the harness with that repository only. It then starts a fresh Kubo 0.40.1 node in offline mode (container `ecorefs-eval-baselines`, API on 127.0.0.1:5103), writes the environment file and runs one JVM. At the end, also after a failure or Ctrl-C, it removes the container, unmounts the shares and stops the SSH server and the Git daemon. No step needs sudo.

Requirements: Docker, the JDK 17 in `/usr/lib/jvm/java-17-openjdk-amd64`, `git`, `sshfs` and `fusermount3`, `/usr/sbin/sshd` and `ssh-keygen`, the built core plug-in and the generated models. The work data stays in `~/.cache/ecorefs-baselines` on the ext4 system disk, the file system of the temporary XMI files in the paper.

## Stores

| Store | Write | Read |
|---|---|---|
| `xmi` | save to an XMI file | load the file in a fresh resource set |
| `ipfs` | save through `IPFSResourceImpl` to the local Kubo node | load by CID in a fresh resource set |
| `git` | save the XMI file into a working tree, `git add` and `git commit`, `git push` to a bare repository behind `git daemon` | `git clone --depth 1` into a fresh folder, load the file |
| `share` | save the XMI file through one `sshfs` mount | load the file through a second mount of the same folder |
| `neoemf` | create a MapDB database, copy every element into it, commit and close | open the database |

After the read, every store visits each element of the loaded model and reads its identifier. The runner compares the element count and the sum of the identifiers with the saved copy, so a store that returns an incomplete model or the model of an earlier iteration stops the run.

Git uses the command line client 2.43, the usual way to work with Git. The daemon is a separate process that serves the bare repositories over `git://` on 127.0.0.1:9419 with pushes enabled. The working tree keeps its history over the iterations of one model size, as a modeling project would. `gc.auto` is 0 and `receive.autogc` is false, so no background `git gc` falls into a measurement. The clone is shallow, because a reader only needs the current model.

The network share is `sshfs` 3.7.3 against an OpenSSH 9.6 server. NFS and SMB mounts need root rights on this machine, while `sshfs` mounts through FUSE as the current user. The server runs as the user on 127.0.0.1:2222 with its own host key, configuration and client key, and offers only `internal-sftp`. The share is mounted twice, for a writing and a reading client, so a read cannot come from what the writing mount keeps in memory. Both mounts run with `dir_cache=no`, and every file name occurs once per run.

NeoEMF 2.0.0 uses the embedded MapDB 3.0.7 back end with the `indices` mapping and no caches. In a pilot run with 10,000 elements on a busy machine, the `arrays` and `lists` mappings and the NeoEMF caches were all slower. NeoEMF loads lazily: the read only opens the database, and the visit fetches the elements, so the read and the visit together compare with a full XMI load.

The released NeoEMF 2.0.0 depends on Chronicle Map 3.17.1 with Chronicle Core 2.17.6, which stops on Java 17 (it looks for the hidden field `AccessibleObject.override`) and on Java 21 (it calls `FileChannelImpl.unmap0`, which Java 21 no longer has). The pom imports the Chronicle BOM 2.23.123 (Chronicle Map 3.23.5, Chronicle Core 2.23.35, affinity 3.23.3), which keeps the API that NeoEMF calls and runs on Java 17 when `run.sh` opens the JDK packages that FST and Chronicle reach into. Java 21 still fails with these versions, and Chronicle Map compiles classes with `javac` at run time, which the Java 21 runtime of this machine lacks. All stores therefore run on the JDK 17. The pom leaves out `chronicle-analytics`, and `run.sh` sets `chronicle.analytics.disable`, so Chronicle sends no usage data.

`NeoEmfModelCopier` binds every new NeoEMF object to the database resource before the object joins its container, so NeoEMF writes it straight into MapDB. An unbound object first goes through the transient Chronicle maps of NeoEMF, which NeoEMF sizes for 65,536 entries; in a pilot run that detour took about 2 s per copy of 1,000 elements instead of 35 to 80 ms. Since NeoEMF runs on Java 17, CDO was not needed.

## Phases

The CSV file has one row per store, size, measured iteration and phase, in milliseconds:

- `save`: the EMF save (for `neoemf`: create the database, copy, commit and close);
- `commit` and `push` (Git only);
- `write`: from the start of the save until the model is stored and, for Git, pushed;
- `clone` (Git only) and `load`, a load in a fresh resource set;
- `read`: from the start of the transfer until the load returns;
- `visit`: one pass over every element of the loaded model;
- `read+visit`: the sum of the two, computed by the harness.

## Files

`BaselineBenchmark` is the entry point, `BenchmarkSettings` reads the options, `BenchmarkRunner` runs the iterations and `ResultLog` writes the results. Each store implements `ModelStore`: `XmiFileStore`, `IpfsStore`, `GitStore` with `GitDaemon`, `NetworkShareStore` with `SshServer` and `SshfsMount`, and `NeoEmfStore` with `NeoEmfModelCopier`. `log4j2-baselines.xml` keeps NeoEMF from logging every save and load inside the measured region.

## Duration

The validation took 48 s on 2026-10-23. The final run takes about an hour by an estimate from the validation and from the RQ3 times of the large models; most of it goes to NeoEMF at 500,000 elements.

## Limits

All servers run on the same machine and talk over the loopback interface, so the Git and share times contain no network delay, as the IPFS times of RQ3 contain none. The IPFS read comes from the node that saved the model, while the Git and share reads go through a second client. `eval/multinode` measures fetches between IPFS nodes with network delay. The paper's RQ3 run used OpenJDK 21.0.9 and this harness uses the JDK 17, so the baselines should be compared with the XMI and IPFS columns of the same run.
