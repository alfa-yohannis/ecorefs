#!/usr/bin/env bash
# Runs the baseline benchmark (Git, sshfs share, NeoEMF next to local XMI and
# IPFS) on the generated models of the RQ3 benchmark.
#
#   eval/baselines/run.sh validate   sizes 100 to 10,000, 1 warm-up, 3 measured, 2 GB heap
#   eval/baselines/run.sh final      sizes 100 to 500,000, 3 warm-up, 30 measured, 8 GB heap
#
# Further options go to BaselineBenchmark and override the mode defaults, for
# example: eval/baselines/run.sh validate --stores xmi,neoemf --sizes 100
#
# The script installs the current core plug-in jar into the Maven repository
# .m2-eval, builds the harness with that repository, starts a fresh offline
# Kubo node in the container ecorefs-eval-baselines on 127.0.0.1:5103, and
# removes the node, the mounts and the servers at the end. Repositories, mounts
# and databases live in BASELINE_WORK (default ~/.cache/ecorefs-baselines, an
# ext4 folder); results go to eval/results/baselines/<label>.*, where
# BASELINE_LABEL overrides the label. BASELINE_JAVA_HOME selects the JDK
# (default OpenJDK 17).
set -euo pipefail

mode="${1:-}"
shift || true

here="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repository="$(cd "$here/../.." && pwd)"
maven="${MAVEN:-$repository/.tools/apache-maven-3.9.10/bin/mvn}"
maven_repository="$repository/.m2-eval"
core_jar="$repository/core-plugin/target/ecorefs-ipfs-core-1.0-SNAPSHOT.jar"
# NeoEMF runs on the JDK 17 here: Chronicle 2.23 stops on Java 21, which no
# longer has FileChannelImpl.unmap0, and Chronicle Map compiles classes with
# javac at run time, which the Java 21 runtime of this machine does not include.
java_home="${BASELINE_JAVA_HOME:-/usr/lib/jvm/java-17-openjdk-amd64}"
build_java_home="${BUILD_JAVA_HOME:-$java_home}"
work="${BASELINE_WORK:-$HOME/.cache/ecorefs-baselines}"
results="$repository/eval/results/baselines"
container="ecorefs-eval-baselines"
api_port=5103

# NeoEMF 2.0.0 serializes values with FST 2.57 and keeps transient objects in
# Chronicle Map, and both reach into JDK internals. Java 17 and later deny that
# access unless the packages are opened to the class path. The system
# properties of the java command keep Chronicle from reporting usage over the
# network and turn off message lookups in the old Log4j 2.11 of NeoEMF.
opens=()
for package in java.lang java.lang.reflect java.lang.invoke java.io java.math java.net java.nio \
        java.text java.time java.util java.util.concurrent java.util.concurrent.atomic \
        java.util.regex sun.nio.ch jdk.internal.misc jdk.internal.ref; do
    opens+=(--add-opens "java.base/$package=ALL-UNNAMED")
done
opens+=(--add-opens java.sql/java.sql=ALL-UNNAMED --add-exports jdk.unsupported/sun.misc=ALL-UNNAMED)

case "$mode" in
    validate)
        label="${BASELINE_LABEL:-validation}"
        heap=(-Xms2g -Xmx2g)
        defaults=(--sizes 100,1000,10000 --warmup 1 --iterations 3)
        ;;
    final)
        label="${BASELINE_LABEL:-final}"
        heap=(-Xms8g -Xmx8g)
        defaults=(--sizes 100,1000,10000,100000,500000 --warmup 3 --iterations 30)
        if [ -e "$results/$label.csv" ]; then
            echo "$results/$label.csv exists; move it away or set BASELINE_LABEL." >&2
            exit 1
        fi
        ;;
    *)
        echo "usage: $0 validate|final [BaselineBenchmark options]" >&2
        exit 2
        ;;
esac

# Removes everything the run started, also after a failure or Ctrl-C. The
# patterns name the work folder of this script, so no other process matches.
clean_up() {
    fusermount3 -u -z "$work/share/writer" 2>/dev/null || true
    fusermount3 -u -z "$work/share/reader" 2>/dev/null || true
    pkill -f -- "--base-path=$work/git/remote" 2>/dev/null || true
    pkill -f -- "-f $work/share/server/sshd_config" 2>/dev/null || true
    docker rm -f "$container" >/dev/null 2>&1 || true
}
trap clean_up EXIT

# Deletes the folders that the stores create, and nothing else in the work folder.
remove_work_data() {
    rm -rf "$work/xmi" "$work/git" "$work/share" "$work/neoemf"
}

start_node() {
    docker rm -f "$container" >/dev/null 2>&1 || true
    docker run -d --name "$container" -p "127.0.0.1:$api_port:5001" \
        ipfs/kubo:v0.40.1 daemon --offline >/dev/null
    for _ in $(seq 1 60); do
        if curl -sf -X POST "http://127.0.0.1:$api_port/api/v0/version" >/dev/null; then
            return 0
        fi
        sleep 1
    done
    echo "The Kubo node did not start." >&2
    return 1
}

record_environment() {
    {
        echo "date: $(date -Iseconds)"
        echo "repository: $(git -C "$repository" rev-parse --short HEAD)" \
            "($(git -C "$repository" status --porcelain | wc -l) changed or new paths)"
        echo "core plug-in jar: $(sha256sum "$core_jar" | cut -c1-16)"
        echo "kernel: $(uname -srm)"
        lscpu | grep -E '^(Model name|CPU\(s\)|Thread\(s\) per core|CPU max MHz):'
        free -h | head -2
        echo "load average at start: $(cut -d' ' -f1-3 /proc/loadavg)"
        echo "work folder: $work ($(df -T "$work" | awk 'NR == 2 {print $2}'))"
        "$java_home/bin/java" -version 2>&1
        git --version
        sshfs --version 2>&1
        ssh -V 2>&1
        echo "kubo: $(docker exec "$container" ipfs version)"
        echo "docker: $(docker version --format '{{.Server.Version}}')"
    } > "$results/$label-environment.txt"
}

if [ ! -f "$core_jar" ]; then
    echo "$core_jar is missing; build the core plug-in first." >&2
    exit 1
fi
echo "Installing the core plug-in and building the harness"
JAVA_HOME="$build_java_home" "$maven" -q -f "$here/pom.xml" -Dmaven.repo.local="$maven_repository" \
    install:install-file -Dfile="$core_jar" -DpomFile="$repository/core-plugin/pom.xml"
JAVA_HOME="$build_java_home" "$maven" -q -f "$here/pom.xml" -Dmaven.repo.local="$maven_repository" package

clean_up
remove_work_data
mkdir -p "$work" "$results"
start_node
record_environment

echo "Running $mode, results in $results/$label.csv"
"$java_home/bin/java" "${heap[@]}" "${opens[@]}" \
    -Dchronicle.analytics.disable=true -Dlog4j2.formatMsgNoLookups=true \
    -Dlog4j.configurationFile="$here/log4j2-baselines.xml" \
    -cp "$here/target/ecorefs-baselines-1.0-SNAPSHOT.jar:$here/target/lib/*" \
    org.ecorefs.eval.baselines.BaselineBenchmark \
    "${defaults[@]}" --label "$label" --work "$work" --results "$results" \
    --ipfs "/ip4/127.0.0.1/tcp/$api_port" "$@" 2>&1 | tee "$results/$label.log"

clean_up
remove_work_data
