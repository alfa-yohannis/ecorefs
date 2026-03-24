#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<'EOF'
Usage:
  fetch-component-snapshot-from-ipfs.sh DESTINATION_DIR COMPONENT_ID [COMPONENT_ID ...]

Example:
  fetch-component-snapshot-from-ipfs.sh \
    /tmp/modisco_component_snapshot_small \
    eclipse.jdt.debug/org.eclipse.jdt.launching.javaagent

Environment overrides:
  IPFS_CONTAINER            Docker container name running Kubo (default: ipfs_node)
  MODISCO_PROJECT_MANIFEST  Published manifest JSON to read (default: current project-version-manifest.json)

What it fetches:
  - the published root model file all-repos-merged.root.xmi
  - one published component directory per COMPONENT_ID

The resulting directory matches the structure expected by
ModiscoEmfCompareSnapshotTest's component-scoped comparison.
EOF
}

if [[ $# -lt 2 ]]; then
  usage >&2
  exit 1
fi

destination_dir=$(python3 -c 'import pathlib,sys; print(pathlib.Path(sys.argv[1]).expanduser().resolve())' "$1")
shift

ipfs_container=${IPFS_CONTAINER:-ipfs_node}
project_manifest=${MODISCO_PROJECT_MANIFEST:-/data2/projects/ecorefs/eval/datasets/modisco-java-models-by-repo/all-repos-merged-by-compilation-unit/project-version-manifest.json}

if [[ ! -f "$project_manifest" ]]; then
  echo "Missing project manifest: $project_manifest" >&2
  exit 1
fi

if [[ -d "$destination_dir" ]] && find "$destination_dir" -mindepth 1 -print -quit | grep -q .; then
  echo "Destination directory must be empty: $destination_dir" >&2
  exit 1
fi

mkdir -p "$destination_dir/compilation-units"

docker start "$ipfs_container" >/dev/null

manifest_rows=$(
  python3 - "$project_manifest" "$@" <<'PY'
import json
import sys

manifest_path = sys.argv[1]
requested = sys.argv[2:]

with open(manifest_path, "r", encoding="utf-8") as handle:
    manifest = json.load(handle)

components = {component["componentId"]: component["componentRootCid"]
              for component in manifest["components"]}

print("ROOT\t" + manifest["rootModel"]["cid"])
for component_id in requested:
    if component_id not in components:
        print("MISSING\t" + component_id, file=sys.stderr)
        sys.exit(2)
    print("COMPONENT\t" + component_id + "\t" + components[component_id])
PY
)

root_cid=
while IFS=$'\t' read -r kind field1 field2; do
  if [[ "$kind" == "ROOT" ]]; then
    root_cid=$field1
  fi
done <<<"$manifest_rows"

if [[ -z "${root_cid:-}" ]]; then
  echo "Could not determine root CID from $project_manifest" >&2
  exit 1
fi

echo "[snapshot-fetch] destination: $destination_dir"
echo "[snapshot-fetch] manifest: $project_manifest"
echo "[snapshot-fetch] container: $ipfs_container"
echo "[snapshot-fetch] root CID: $root_cid"

docker exec "$ipfs_container" ipfs cat "$root_cid" > "$destination_dir/all-repos-merged.root.xmi"

while IFS=$'\t' read -r kind field1 field2; do
  if [[ "$kind" != "COMPONENT" ]]; then
    continue
  fi

  component_id=$field1
  component_root_cid=$field2
  target_dir="$destination_dir/compilation-units/$component_id"
  container_tmp="/tmp/ecorefs-ipfs-fetch-$$-$RANDOM"

  mkdir -p "$target_dir"
  echo "[snapshot-fetch] component: $component_id"
  echo "[snapshot-fetch]   root CID: $component_root_cid"
  docker exec "$ipfs_container" sh -lc \
    "rm -rf '$container_tmp' && mkdir -p '$container_tmp' && ipfs get '$component_root_cid' -o '$container_tmp/component' >/dev/null && tar -C '$container_tmp/component' -cf - . && rm -rf '$container_tmp'" \
    | tar -xf - -C "$target_dir"
done <<<"$manifest_rows"

echo "[snapshot-fetch] snapshot ready: $destination_dir"
