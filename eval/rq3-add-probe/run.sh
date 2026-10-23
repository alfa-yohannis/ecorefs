#!/usr/bin/env bash
# Locates the fixed cost of a save through IPFS (RQ3). The probe times one add
# of a generated model to a fresh offline Kubo node in several ways:
#   - through the Java IPFS client, as IPFSResourceImpl.saveIPFS adds the file,
#     with and without HTTP keep-alive;
#   - through one request that sends the whole body in one write, with and
#     without the pin that Kubo records for an added file;
#   - with curl, with and without the pin;
#   - against a second node whose repository lies on tmpfs.
# dd measures the time of a synchronous 4 KiB write in the home folder.
#
#   bash eval/rq3-add-probe/run.sh [output file]
#
# The output goes to eval/results/rq3-add-probe/<host>.txt by default. JAVA_HOME
# selects the runtime (default OpenJDK 21), JAVAC_HOME the compiler (default
# JAVA_HOME, which needs javac), and PORT the first of two API ports (default
# 5199). The jars come from the local Maven cache, read only. The nodes run in
# their own containers and are removed at the end.
set -euo pipefail

here="$(cd "$(dirname "$0")" && pwd)"
repo="$(cd "$here/../.." && pwd)"
out="${1:-$repo/eval/results/rq3-add-probe/$(hostname).txt}"
java_home="${JAVA_HOME:-/usr/lib/jvm/java-21-openjdk-amd64}"
javac_home="${JAVAC_HOME:-$java_home}"
m2="${M2_REPOSITORY:-$HOME/.m2/repository}"
port="${PORT:-5199}"
memory_port=$((port + 1))
models="$repo/eval/custom_generator/output"
disk_node=ecorefs-add-probe
memory_node=ecorefs-add-probe-tmpfs
probe=org.ecorefs.eval.addprobe.AddLatencyProbe

jars=(
  "$m2/com/github/ipfs/java-ipfs-http-client/v1.4.3/java-ipfs-http-client-v1.4.3.jar"
  "$m2/com/github/multiformats/java-multiaddr/v1.4.12/java-multiaddr-v1.4.12.jar"
  "$m2/com/github/multiformats/java-multihash/v1.3.4/java-multihash-v1.3.4.jar"
  "$m2/com/github/multiformats/java-multibase/v1.1.1/java-multibase-v1.1.1.jar"
  "$m2/com/github/ipld/java-cid/v1.3.7/java-cid-v1.3.7.jar"
)
classpath="$here/build/classes"
for jar in "${jars[@]}"; do
  if [[ ! -f "$jar" ]]; then
    echo "Missing jar: $jar (build core-plugin once to fill the Maven cache)" >&2
    exit 1
  fi
  classpath+=":$jar"
done

# Removes the nodes of the probe.
remove_nodes() {
  docker rm -f "$disk_node" "$memory_node" >/dev/null 2>&1 || true
}
trap remove_nodes EXIT

# Waits until the API of a node answers, then a little longer for the start.
wait_for_node() {
  for _ in $(seq 60); do
    curl -s -X POST "http://127.0.0.1:$1/api/v0/version" >/dev/null && break
    sleep 1
  done
  sleep 5
}

# Times 30 adds with curl after 3 warm-up adds and prints the median and the
# times in ms. The third argument says whether the node pins the file.
time_curl() {
  local file="$1" api_port="$2" pin="$3" times=""
  for iteration in $(seq 33); do
    local seconds
    seconds="$(curl -s -o /dev/null -w '%{time_total}' -F "file=@$file" \
      "http://127.0.0.1:$api_port/api/v0/add?pin=$pin")"
    if [[ "$iteration" -gt 3 ]]; then
      times+="$(awk -v s="$seconds" 'BEGIN {printf "%.3f ", s * 1000}')"
    fi
  done
  echo "curl pin=$pin port=$api_port file=$(basename "$file") median=$(echo "$times" | tr ' ' '\n' \
    | grep . | sort -n | awk '{a[NR]=$1} END {printf "%.3f", (a[15]+a[16])/2}') ms"
  echo "  times: $times"
}

# Runs the probe class with the given Java options and arguments.
run_probe() {
  "$java_home/bin/java" "$@"
}

# Measures every variant and prints the results.
measure() {
  echo "host: $(hostname), $(uname -sr), $(lscpu | sed -n 's/^Model name: *//p')"
  echo "java: $("$java_home/bin/java" -version 2>&1 | head -n 1), kubo image: ipfs/kubo:v0.40.1"
  echo "commit: $(git -C "$repo" rev-parse HEAD), date: $(date -Is)"
  echo "docker root: $(docker info --format '{{.DockerRootDir}}') on" \
    "$(df --output=source "$(docker info --format '{{.DockerRootDir}}')" | tail -1)," \
    "home on $(df --output=source "$HOME" | tail -1)"
  echo "== 200 synchronous 4 KiB writes in the home folder"
  dd if=/dev/zero of="$HOME/.ecorefs-dsync-probe" bs=4k count=200 oflag=dsync 2>&1 | tail -1
  rm -f "$HOME/.ecorefs-dsync-probe"

  remove_nodes
  docker run -d --name "$disk_node" -p "127.0.0.1:$port:5001" ipfs/kubo:v0.40.1 daemon --offline >/dev/null
  docker run -d --name "$memory_node" --tmpfs /data/ipfs -p "127.0.0.1:$memory_port:5001" \
    ipfs/kubo:v0.40.1 daemon --offline >/dev/null
  wait_for_node "$port"
  wait_for_node "$memory_port"
  echo "datastore of the disk node: $(docker exec "$disk_node" ipfs config Datastore.Spec | tr -d ' \n')"

  for size in 100 10000; do
    local file="$models/model_$size.xmi"
    echo "== model_$size.xmi, node with the repository on disk"
    run_probe -cp "$classpath" "$probe" library "$file" "$port"
    run_probe -Dhttp.keepAlive=false -cp "$classpath" "$probe" library "$file" "$port"
    run_probe -cp "$classpath" "$probe" single-write "$file" "$port"
    run_probe -cp "$classpath" "$probe" single-write-no-pin "$file" "$port"
    time_curl "$file" "$port" true
    time_curl "$file" "$port" false
    echo "== model_$size.xmi, node with the repository on tmpfs"
    run_probe -cp "$classpath" "$probe" library "$file" "$memory_port"
    time_curl "$file" "$memory_port" true
  done
}

rm -rf "$here/build"
mkdir -p "$here/build/classes" "$(dirname "$out")"
mapfile -t sources < <(find "$here/src" -name '*.java' | sort)
"$javac_home/bin/javac" --release 17 -encoding UTF-8 -Xlint:all -d "$here/build/classes" \
  -cp "$classpath" "${sources[@]}"
measure | tee "$out"
