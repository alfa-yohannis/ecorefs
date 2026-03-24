#!/usr/bin/env bash
set -euo pipefail

# EcoreFS begin: standalone runner for the BPMN2 Modeler IPFS UI self-tests
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
BUILD_DIR="$SCRIPT_DIR/build"

mkdir -p "$BUILD_DIR"

javac -d "$BUILD_DIR" \
  "$REPO_ROOT/plugins/org.eclipse.bpmn2.modeler.ui/src/org/eclipse/bpmn2/modeler/ui/util/IPFSModelTransfer.java" \
  "$SCRIPT_DIR/src/org/eclipse/bpmn2/modeler/ui/tests/IPFSModelTransferSelfTest.java"

java -cp "$BUILD_DIR" org.eclipse.bpmn2.modeler.ui.tests.IPFSModelTransferSelfTest "$REPO_ROOT" "$@"
# EcoreFS end: standalone runner for the BPMN2 Modeler IPFS UI self-tests
