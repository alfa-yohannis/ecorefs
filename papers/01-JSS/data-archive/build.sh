#!/usr/bin/env bash
# Builds the research data archive of the JSS paper "Towards Decentralized Model
# Persistence" for upload to Zenodo. The archive goes to $OUT, outside the
# repository, and holds the MoDisco models, the generated models of the timed
# benchmarks, the BPMN collections whose terms allow redistribution, the
# evaluation results, and the source code of the three repositories at their
# current commits.
#
#   papers/01-JSS/data-archive/build.sh
#
# MODELER, METAMODEL and OUT override the default locations.
set -euo pipefail

HERE="$(cd "$(dirname "$0")" && pwd)"
ECOREFS="$(cd "$HERE/../../.." && pwd)"
MODELER="${MODELER:-/data2/projects/org.eclipse.bpmn2-modeler-1.5.4-ecorefs}"
METAMODEL="${METAMODEL:-/data2/projects/org.eclipse.bpmn2-ecorefs}"
OUT="${OUT:-/data2/projects/ecorefs-data-archive}"
DATASETS="$ECOREFS/eval/datasets"
MODISCO="modisco-java-models-by-repo"
# Camunda bpmn-for-research is left out: its terms allow research use but say
# nothing about redistribution. The README cites it by repository and commit.
SHAREABLE_COLLECTIONS="omg-miwg-test-suite jbpm-tests flowable-tests camunda7-tests"

mkdir -p "$OUT"
rm -f "$OUT"/*.tar.gz "$OUT/SHA256SUMS" "$OUT/README.md"

echo "MoDisco models of Eclipse JDT"
tar -C "$DATASETS" -czf "$OUT/modisco-jdt-models.tar.gz" \
  "$MODISCO/eclipse.jdt.core" "$MODISCO/eclipse.jdt.core.binaries" \
  "$MODISCO/eclipse.jdt.debug" "$MODISCO/eclipse.jdt.ui" \
  "$MODISCO/all-repos-merged.xmi" "$MODISCO/all-repos-merged-by-compilation-unit" \
  "$MODISCO/ipfs-cids.md" "$MODISCO/aggregate-metrics-breakdown.csv" "$MODISCO/aggregate-metrics-summary.md"

echo "Generated models of the timed benchmarks"
tar -C "$ECOREFS/eval/custom_generator" -czf "$OUT/generated-models.tar.gz" output

echo "BPMN collections"
bpmn_paths=(bpmn/README.md bpmn/_load-check/summary.md bpmn/_load-check/tools bpmn/_load-check/clean/removed-files.tsv)
for collection in $SHAREABLE_COLLECTIONS; do
  bpmn_paths+=("bpmn/$collection" "bpmn/_load-check/clean/$collection.changes.tsv" "bpmn/_load-check/$collection.classified.tsv")
done
tar -C "$DATASETS" --exclude=.git -czf "$OUT/bpmn-collections.tar.gz" "${bpmn_paths[@]}"

echo "Evaluation results"
tar -C "$ECOREFS" -czf "$OUT/evaluation-results.tar.gz" eval/results

echo "Source code"
git -C "$ECOREFS" archive --format=tar.gz --prefix=ecorefs/ -o "$OUT/ecorefs-source.tar.gz" HEAD
git -C "$MODELER" archive --format=tar.gz --prefix=org.eclipse.bpmn2-modeler-1.5.4-ecorefs/ -o "$OUT/bpmn2-modeler-ecorefs-source.tar.gz" HEAD
git -C "$METAMODEL" archive --format=tar.gz --prefix=org.eclipse.bpmn2-ecorefs/ -o "$OUT/bpmn2-metamodel-ecorefs-source.tar.gz" HEAD

cp "$HERE/README.md" "$OUT/README.md"
{
  echo
  echo "## Commits in this archive"
  echo
  echo "- ecorefs: $(git -C "$ECOREFS" rev-parse HEAD)"
  echo "- org.eclipse.bpmn2-modeler-1.5.4-ecorefs: $(git -C "$MODELER" rev-parse HEAD)"
  echo "- org.eclipse.bpmn2-ecorefs: $(git -C "$METAMODEL" rev-parse HEAD)"
} >> "$OUT/README.md"

(cd "$OUT" && sha256sum README.md ./*.tar.gz > SHA256SUMS)
ls -l "$OUT"
