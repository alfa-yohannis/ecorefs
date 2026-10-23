#!/usr/bin/env bash
# Compiles the multi-node harness with javac. The jars come from the local Maven
# cache, read only, and the core jar from core-plugin/target. build/lib keeps a
# copy of each jar, so a later rebuild of core-plugin does not change a run that
# is in progress, and build/jars.sha256 records which jars the run used.
set -euo pipefail

here="$(cd "$(dirname "$0")" && pwd)"
repo="$(cd "$here/../.." && pwd)"
m2="${M2_REPOSITORY:-$HOME/.m2/repository}"
core="${ECOREFS_CORE_JAR:-$repo/core-plugin/target/ecorefs-ipfs-core-1.0-SNAPSHOT.jar}"

jars=(
  "$core"
  "$m2/org/eclipse/emf/org.eclipse.emf.ecore/2.23.0/org.eclipse.emf.ecore-2.23.0.jar"
  "$m2/org/eclipse/emf/org.eclipse.emf.ecore.xmi/2.16.0/org.eclipse.emf.ecore.xmi-2.16.0.jar"
  "$m2/org/eclipse/emf/org.eclipse.emf.common/2.21.0/org.eclipse.emf.common-2.21.0.jar"
  "$m2/com/github/ipfs/java-ipfs-http-client/v1.4.3/java-ipfs-http-client-v1.4.3.jar"
  "$m2/com/github/multiformats/java-multiaddr/v1.4.12/java-multiaddr-v1.4.12.jar"
  "$m2/com/github/multiformats/java-multihash/v1.3.4/java-multihash-v1.3.4.jar"
  "$m2/com/github/multiformats/java-multibase/v1.1.1/java-multibase-v1.1.1.jar"
  "$m2/com/github/ipld/java-cid/v1.3.7/java-cid-v1.3.7.jar"
  "$m2/com/google/code/gson/gson/2.10.1/gson-2.10.1.jar"
)

rm -rf "$here/build"
mkdir -p "$here/build/lib" "$here/build/classes"
for jar in "${jars[@]}"; do
  if [[ ! -f "$jar" ]]; then
    echo "Missing jar: $jar" >&2
    echo "Build core-plugin first (mvn -q -f core-plugin/pom.xml package -DskipTests)." >&2
    exit 1
  fi
  cp "$jar" "$here/build/lib/"
done

mapfile -t sources < <(find "$here/src" -name '*.java' | sort)
javac --release 17 -encoding UTF-8 -Xlint:all,-processing -d "$here/build/classes" \
  -cp "$here/build/lib/*" "${sources[@]}"
(cd "$here/build/lib" && sha256sum ./*.jar > ../jars.sha256)
echo "Compiled ${#sources[@]} classes into $here/build/classes"
