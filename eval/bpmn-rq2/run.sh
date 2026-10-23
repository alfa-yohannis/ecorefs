#!/usr/bin/env bash
# RQ2 on real BPMN models: cascade saves on split multi-pool models and on the
# call graphs between files, over the cleaned collections in eval/datasets/bpmn.
# Each step starts its own Kubo node in offline mode with an empty repository and
# removes it afterwards.
#
#   eval/bpmn-rq2/run.sh compile
#   eval/bpmn-rq2/run.sh lists           # raw/multi-pool-models.tsv and raw/called-element-links.tsv
#   eval/bpmn-rq2/run.sh split [limit]   # node on port 5104
#   eval/bpmn-rq2/run.sh calls           # node on port 5105
#   eval/bpmn-rq2/run.sh summary         # writes eval/results/bpmn-rq2
set -euo pipefail

HERE="$(cd "$(dirname "$0")" && pwd)"
ECOREFS="$(cd "$HERE/../.." && pwd)"
DATASET="${DATASET:-$ECOREFS/eval/datasets/bpmn}"
RAW="$HERE/raw"
BUILD="$HERE/build"
RESULTS="$ECOREFS/eval/results/bpmn-rq2"
M2="${M2:-$HOME/.m2/repository}"
JAVAC="${JAVAC:-/usr/lib/jvm/java-17-openjdk-amd64/bin/javac}"
JAVA="${JAVA:-/usr/lib/jvm/java-21-openjdk-amd64/bin/java}"
KUBO_IMAGE=ipfs/kubo:v0.40.1

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
}

stop_node() {
  docker rm -f -v "$1" > /dev/null
}

run_driver() {
  "$JAVA" -Xmx2g -cp "$CLASSPATH:$BUILD/classes" org.ecorefs.eval.rq2.Rq2Main "$@"
}

case "${1:-}" in
  compile)
    rm -rf "$BUILD/classes"
    mkdir -p "$BUILD/classes"
    "$JAVAC" --release 17 -Xlint:all -Xlint:-serial -cp "$CLASSPATH" -d "$BUILD/classes" \
      $(find "$HERE/src" "$ECOREFS/eval/bpmn-common/src" -name '*.java')
    ;;
  lists)
    mkdir -p "$RAW"
    python3 -I "$HERE/list_multi_pool_models.py" "$DATASET" "$RAW"
    python3 -I "$HERE/resolve_called_elements.py" "$DATASET" "$RAW"
    wc -l "$RAW"/*.tsv
    ;;
  split)
    start_node ecorefs-eval-rq2-split 5104
    run_driver split /ip4/127.0.0.1/tcp/5104 "$DATASET" "$RAW/multi-pool-models.tsv" "$RAW/split.tsv" "${2:-0}"
    stop_node ecorefs-eval-rq2-split
    ;;
  calls)
    start_node ecorefs-eval-rq2-calls 5105
    run_driver calls /ip4/127.0.0.1/tcp/5105 "$DATASET" "$RAW/called-element-links.tsv" "$RAW/calls.tsv"
    stop_node ecorefs-eval-rq2-calls
    ;;
  summary)
    mkdir -p "$RESULTS"
    python3 -I "$HERE/summarize.py" "$RAW" "$RESULTS"
    ;;
  *)
    sed -n '2,12p' "$0"
    exit 2
    ;;
esac
