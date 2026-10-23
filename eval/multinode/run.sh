#!/usr/bin/env bash
# Runs the multi-node experiment. For each topology and delay profile the script
# starts four fresh Kubo nodes, sets their peering and the delay, and runs the
# Java harness once. At the end it removes the nodes and their volumes and
# writes summary.md.
#
#   bash eval/multinode/run.sh            full run: peered and dht, profiles none,
#                                         lan, wan50 and wan100, 3 warm-up and 30
#                                         measured iterations, heap 8 GB
#   bash eval/multinode/run.sh --quick    validation: profiles none and wan50,
#                                         1 warm-up and 3 measured iterations,
#                                         models of 100 and 1,000 elements, 2 GB
#
# TOPOLOGIES, PROFILES, HEAP and OUT override the lists, the heap and the result
# folder. Options after the mode go to the harness, for example --sizes 100,1000.
#
# Everything runs inside main, which bash parses as a whole before it starts, so
# an edit of this file during a run does not change the run.
set -euo pipefail

here="$(cd "$(dirname "$0")" && pwd)"
repo="$(cd "$here/../.." && pwd)"
compose=(docker compose --progress quiet -f "$here/compose.yaml")

# Removes the four nodes, their volumes and the network, if they exist.
remove_nodes() {
  "${compose[@]}" down -v --remove-orphans > /dev/null 2>&1 || true
}

# Writes the swarm key, the BPMN sample and the compiled harness when missing.
prepare() {
  if [[ ! -f "$here/swarm.key" ]]; then
    bash "$here/new-swarm-key.sh"
  fi
  if [[ ! -f "$here/bpmn-sample.tsv" ]]; then
    python3 -I "$here/select_bpmn_sample.py"
  fi
  bash "$here/build.sh"
}

# Prints the settings of the run and the versions of everything it uses.
describe_environment() {
  echo "run: $run_id ($mode)"
  echo "started: $(date -Is)"
  echo "topologies: ${topologies[*]}"
  echo "profiles: ${profiles[*]}"
  echo "harness options: ${harness_options[*]:-(defaults: 3 warm-up, 30 measured iterations)}"
  echo "heap: $heap"
  echo "host: $(uname -srmo)"
  echo "cpu: $(lscpu | sed -n 's/^Model name: *//p'), $(nproc) logical CPUs"
  echo "memory: $(free -h | awk '/^Mem:/ {print $2}')"
  echo "docker: $(docker version --format '{{.Server.Version}}'), compose $(docker compose version --short)"
  echo "kubo: $(docker run --rm --entrypoint ipfs ipfs/kubo:v0.40.1 version)"
  echo "java: $(java -version 2>&1 | head -n 1)"
  echo "ecorefs commit: $(git -C "$repo" rev-parse HEAD)$(git -C "$repo" diff --quiet -- core-plugin || echo ' (core-plugin has local changes)')"
  echo "bpmn sample: $(sha256sum "$here/bpmn-sample.tsv" | cut -d' ' -f1)"
  echo "jars:"
  sed 's/^/  /' "$here/build/jars.sha256"
}

# Starts fresh nodes in one topology and profile and runs the harness against them.
measure() {
  local topology="$1" profile="$2"
  echo "== $topology / $profile, $(date -Is)"
  remove_nodes
  "${compose[@]}" up -d --wait
  bash "$here/topology.sh" "$topology"
  bash "$here/netem.sh" "$profile"
  (cd "$repo" && java -Xms"$heap" -Xmx"$heap" -cp "$here/build/classes:$here/build/lib/*" \
    org.ecorefs.eval.multinode.MultiNodeBenchmark --run "$run_id" --topology "$topology" \
    --profile "$profile" --out "$out/metrics.csv" "${harness_options[@]}")
}

main() {
  mode=full
  if [[ "${1:-}" == "--quick" ]]; then
    mode=quick
    shift
  fi

  if [[ "$mode" == quick ]]; then
    read -r -a topologies <<< "${TOPOLOGIES:-peered dht}"
    read -r -a profiles <<< "${PROFILES:-none wan50}"
    heap="${HEAP:-2g}"
    out="${OUT:-$repo/eval/results/multinode/validation}"
    harness_options=(--warmup 1 --iterations 3 --bpmn-files 3 --sizes 100,1000)
    rm -f "$out/metrics.csv" "$out/summary.md" "$out/run.log" "$out/environment.txt"
  else
    read -r -a topologies <<< "${TOPOLOGIES:-peered dht}"
    read -r -a profiles <<< "${PROFILES:-none lan wan50 wan100}"
    heap="${HEAP:-8g}"
    out="${OUT:-$repo/eval/results/multinode}"
    harness_options=()
    if [[ -s "$out/metrics.csv" ]]; then
      echo "$out/metrics.csv exists. Move it away or set OUT to another folder." >&2
      exit 1
    fi
  fi
  harness_options+=("$@")
  run_id="$(date +%Y%m%d-%H%M%S)"
  mkdir -p "$out"
  exec > >(tee -a "$out/run.log") 2>&1
  trap remove_nodes EXIT

  prepare
  describe_environment > "$out/environment.txt"
  cat "$out/environment.txt"
  for topology in "${topologies[@]}"; do
    for profile in "${profiles[@]}"; do
      measure "$topology" "$profile"
    done
  done

  remove_nodes
  python3 -I "$here/summarize.py" "$out/metrics.csv" > "$out/summary.md"
  echo "finished: $(date -Is)" | tee -a "$out/environment.txt"
  echo "Results: $out/metrics.csv, $out/summary.md"
}

main "$@"
