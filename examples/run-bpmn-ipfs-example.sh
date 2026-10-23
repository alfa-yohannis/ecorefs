#!/usr/bin/env bash
# Compiles and runs examples/BpmnIpfsExample.java against a Kubo node.
#
#   bash examples/run-bpmn-ipfs-example.sh [api-multiaddress]
#
# The default node is /ip4/127.0.0.1/tcp/5001, the first node of docker/kubo.
# The core plugin has to be built first (mvn -f core-plugin/pom.xml package);
# the other jars come from ~/.m2, in the versions of the pom of the core plugin.
set -euo pipefail

ECOREFS="$(cd "$(dirname "$0")/.." && pwd)"
M2="${M2:-$HOME/.m2/repository}"
JAVAC="${JAVAC:-/usr/lib/jvm/java-17-openjdk-amd64/bin/javac}"
JAVA="${JAVA:-java}"

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

cd "$ECOREFS"
mkdir -p examples/out/classes
"$JAVAC" --release 11 -cp "$CLASSPATH" -d examples/out/classes examples/BpmnIpfsExample.java
"$JAVA" -cp "$CLASSPATH:examples/out/classes" examples.BpmnIpfsExample "$@"
