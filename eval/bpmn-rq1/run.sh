#!/usr/bin/env bash
# RQ1 on real BPMN models: round trip, determinism, deduplication and directory
# publication over the original files of the five collections in
# eval/datasets/bpmn-original. Each step starts its own Kubo node in offline mode
# with an empty repository, on a port of its own, and removes the node afterwards.
#
#   eval/bpmn-rq1/run.sh compile
#   eval/bpmn-rq1/run.sh roundtrip [limit]   # one JVM per collection, node on 5101
#   eval/bpmn-rq1/run.sh statistics          # recounts the model properties, node on 5101
#   eval/bpmn-rq1/run.sh dedup               # one add per file, node on 5102
#   eval/bpmn-rq1/run.sh directory           # one add per collection, node on 5106
#   eval/bpmn-rq1/run.sh summary             # writes eval/results/bpmn-rq1
set -euo pipefail

HERE="$(cd "$(dirname "$0")" && pwd)"
ECOREFS="$(cd "$HERE/../.." && pwd)"
DATASET="${DATASET:-$ECOREFS/eval/datasets/bpmn-original}"
RAW="$HERE/raw"
BUILD="$HERE/build"
# Local saves go here; a folder on a fast local disk keeps the local paths honest.
TEMPORARY="${TEMPORARY:-$BUILD/tmp}"
RESULTS="$ECOREFS/eval/results/bpmn-rq1"
M2="${M2:-$HOME/.m2/repository}"
JAVAC="${JAVAC:-/usr/lib/jvm/java-17-openjdk-amd64/bin/javac}"
JAVA="${JAVA:-/usr/lib/jvm/java-21-openjdk-amd64/bin/java}"
KUBO_IMAGE=ipfs/kubo:v0.40.1
COLLECTIONS=(camunda-bpmn-for-research omg-miwg-test-suite jbpm-tests flowable-tests camunda7-tests)

# The core plugin and the dependencies of its pom, with the BPMN 2.0 metamodel
# that the core tests use.
CLASSPATH="$ECOREFS/core-plugin/target/ecorefs-ipfs-core-1.0-SNAPSHOT.jar"
for jar in \
  com/github/ipfs/java-ipfs-http-client/v1.4.3/java-ipfs-http-client-v1.4.3.jar \
  com/github/multiformats/java-multihash/v1.3.4/java-multihash-v1.3.4.jar \
  com/github/multiformats/java-multibase/v1.1.1/java-multibase-v1.1.1.jar \
  com/github/multiformats/java-multiaddr/v1.4.12/java-multiaddr-v1.4.12.jar \
  com/github/ipld/java-cid/v1.3.7/java-cid-v1.3.7.jar \
  org/eclipse/emf/org.eclipse.emf.ecore/2.23.0/org.eclipse.emf.ecore-2.23.0.jar \
  org/eclipse/emf/org.eclipse.emf.ecore.xmi/2.16.0/org.eclipse.emf.ecore.xmi-2.16.0.jar \
  org/eclipse/emf/org.eclipse.emf.common/2.21.0/org.eclipse.emf.common-2.21.0.jar \
  org/eclipse/bpmn2/org.eclipse.bpmn2/1.5.1/org.eclipse.bpmn2-1.5.1.jar; do
  CLASSPATH="$CLASSPATH:$M2/$jar"
done

start_node() {
  local name=$1 port=$2
  docker rm -f -v "$name" > /dev/null 2>&1 || true
  docker run -d --name "$name" -p "127.0.0.1:$port:5001" "$KUBO_IMAGE" daemon --offline > /dev/null
  until curl -sf -X POST "http://127.0.0.1:$port/api/v0/id" > /dev/null; do sleep 1; done
  echo "node $name on port $port: $(curl -s -X POST "http://127.0.0.1:$port/api/v0/version" | tr -d '\n')"
}

stop_node() {
  docker rm -f -v "$1" > /dev/null
}

make_lists() {
  mkdir -p "$RAW/lists"
  for collection in "${COLLECTIONS[@]}"; do
    find "$DATASET/$collection" -type f \( -name '*.bpmn' -o -name '*.bpmn2' -o -name '*.bpmn20.xml' \) \
      -not -path '*/.git/*' | LC_ALL=C sort > "$RAW/lists/$collection.lst"
  done
  wc -l "$RAW"/lists/*.lst
}

collection_arguments() {
  for collection in "${COLLECTIONS[@]}"; do
    printf '%s=%s ' "$collection" "$RAW/lists/$collection.lst"
  done
}

case "${1:-}" in
  compile)
    rm -rf "$BUILD/classes"
    mkdir -p "$BUILD/classes"
    "$JAVAC" --release 17 -Xlint:all -Xlint:-serial -cp "$CLASSPATH" -d "$BUILD/classes" \
      $(find "$HERE/src" "$ECOREFS/eval/bpmn-common/src" -name '*.java')
    ;;
  roundtrip)
    limit="${2:-0}"
    make_lists
    mkdir -p "$RAW/roundtrip"
    start_node ecorefs-eval-rq1 5101
    for collection in "${COLLECTIONS[@]}"; do
      "$JAVA" -Xmx1g -cp "$CLASSPATH:$BUILD/classes" org.ecorefs.eval.rq1.Rq1Main roundtrip \
        /ip4/127.0.0.1/tcp/5101 "$DATASET" "$TEMPORARY/$collection" "$collection" \
        "$RAW/lists/$collection.lst" "$RAW/roundtrip/$collection.tsv" 120 "$limit" \
        2> "$RAW/roundtrip/$collection.stderr" &
    done
    wait
    stop_node ecorefs-eval-rq1
    ;;
  statistics)
    make_lists
    mkdir -p "$RAW/statistics"
    start_node ecorefs-eval-rq1 5101
    for collection in "${COLLECTIONS[@]}"; do
      "$JAVA" -Xmx1g -cp "$CLASSPATH:$BUILD/classes" org.ecorefs.eval.rq1.Rq1Main statistics \
        /ip4/127.0.0.1/tcp/5101 "$DATASET" "$collection" "$RAW/lists/$collection.lst" \
        "$RAW/statistics/$collection.tsv" &
    done
    wait
    stop_node ecorefs-eval-rq1
    ;;
  dedup)
    make_lists
    start_node ecorefs-eval-rq1-dedup 5102
    "$JAVA" -Xmx1g -cp "$CLASSPATH:$BUILD/classes" org.ecorefs.eval.rq1.Rq1Main dedup \
      /ip4/127.0.0.1/tcp/5102 "$DATASET" "$RAW/dedup.tsv" $(collection_arguments)
    {
      echo "datastore_apparent_bytes	$(docker exec ecorefs-eval-rq1-dedup du -sb /data/ipfs | cut -f1)"
      echo "datastore_allocated_kib	$(docker exec ecorefs-eval-rq1-dedup du -sk /data/ipfs | cut -f1)"
      echo "blocks_apparent_bytes	$(docker exec ecorefs-eval-rq1-dedup du -sb /data/ipfs/blocks | cut -f1)"
      echo "blocks_allocated_kib	$(docker exec ecorefs-eval-rq1-dedup du -sk /data/ipfs/blocks | cut -f1)"
    } >> "$RAW/dedup.tsv.repo"
    stop_node ecorefs-eval-rq1-dedup
    ;;
  directory)
    make_lists
    start_node ecorefs-eval-rq1-dir 5106
    "$JAVA" -Xmx2g -cp "$CLASSPATH:$BUILD/classes" org.ecorefs.eval.rq1.Rq1Main directory \
      /ip4/127.0.0.1/tcp/5106 "$DATASET" "$RAW/directory.tsv" $(collection_arguments)
    stop_node ecorefs-eval-rq1-dir
    ;;
  summary)
    mkdir -p "$RESULTS"
    python3 -I "$HERE/summarize.py" "$RAW" "$RESULTS"
    ;;
  *)
    sed -n '2,14p' "$0"
    exit 2
    ;;
esac
