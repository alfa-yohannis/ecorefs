#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
WORKSPACE_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
REPO_ROOT="${1:-/data2/projects/ecorefs/eval/datasets/eclipse.jdt.core}"
OUTPUT_DIR="${2:-${REPO_ROOT}/modisco-java-models}"
PROJECT_NAME="${3:-}"

export JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-25-openjdk-amd64}"
export PATH="${JAVA_HOME}/bin:${PATH}"
export MAVEN_OPTS="${MAVEN_OPTS:-} -Djdk.xml.maxGeneralEntitySizeLimit=0 -Djdk.xml.totalEntitySizeLimit=0 -Djdk.xml.entityExpansionLimit=0"
MVN_BIN="${MVN_BIN:-${WORKSPACE_ROOT}/.tools/apache-maven-3.9.10/bin/mvn}"

if [[ ! -x "${MVN_BIN}" ]]; then
  MVN_BIN="mvn"
fi

TEST_WORK_DIR="${SCRIPT_DIR}/org.ecorefs.modisco.jdt.reverse.tests/target/work"
if [[ -d "${TEST_WORK_DIR}" ]]; then
  mv "${TEST_WORK_DIR}" "${TEST_WORK_DIR}.stale.$(date +%s)"
fi

MVN_ARGS=(
  -f "${SCRIPT_DIR}/pom.xml"
  -Dmaven.repo.local="${WORKSPACE_ROOT}/.m2-repo"
  "-Dmodisco.repo=${REPO_ROOT}"
  "-Dmodisco.output=${OUTPUT_DIR}"
  verify
)

if [[ -n "${PROJECT_NAME}" ]]; then
  MVN_ARGS+=("-Dmodisco.project=${PROJECT_NAME}")
fi

"${MVN_BIN}" "${MVN_ARGS[@]}"
