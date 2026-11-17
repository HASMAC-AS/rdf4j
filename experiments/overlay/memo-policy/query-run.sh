#!/bin/bash
set -euo pipefail
ROOT=$(cd "$(dirname "$0")/../../.." && pwd)
cd "$ROOT"
variant=$1; name=$2; groups=${3:-8000}
D=experiments/overlay/lifetime
JAVA=experiments/overlay/tools/jdk-25.0.5+8/bin/java
O=experiments/overlay/memo-policy
case "$variant" in
 initial_adaptive) ADMISSION=initial; CAPACITY=adaptive ;;
 always_adaptive) ADMISSION=always; CAPACITY=adaptive ;;
 initial_fixed) ADMISSION=initial; CAPACITY=fixed4096 ;;
 always_fixed) ADMISSION=always; CAPACITY=fixed4096 ;;
 *) echo "Unknown memo variant $variant" >&2; exit 2 ;;
esac
API="experiments/overlay/compact-snapshots/hash-api-classes"
LMDB="experiments/overlay/memo-policy/candidate-lmdb-classes"
EXPECTED=org.eclipse.rdf4j.sail.features.CompactBindingSetSnapshot

CP="experiments/overlay/compact-snapshot-benchmark/harness:$API:$LMDB:$(cat "$D/classpath.txt")"
EXTRA=("-Drdf4j.sail.compactResultSnapshots=true" "-Drdf4j.lmdb.nativeValueMemo.admission=$ADMISSION" "-Drdf4j.lmdb.nativeValueMemo.capacity=$CAPACITY")
if [[ -n "${SNAPSHOT_FLAG:-}" ]]; then EXTRA+=("-D${SNAPSHOT_FLAG}=true"); fi
exec 9>experiments/overlay/native-benchmark.lock; flock 9
python3 experiments/overlay/main-comparison/timed.py "$JAVA" --enable-native-access=ALL-UNNAMED -XX:FlightRecorderOptions=stackdepth=128 -Xms2g -Xmx5g -Dlogback.configurationFile=experiments/overlay/main-comparison/logback.xml "${EXTRA[@]}" -Dprofile.writer=${WRITER:-false} -Dprofile.record=${RECORD:-false} -Dprofile.file="$O/$name" -Dsnapshot.expectedClass="$EXPECTED" -Dcomparison.exact=${EXACT:-false} -Dcomparison.narrow=false -Dcomparison.requireNoSpill=true -Drdf4j.lmdb.nativeDistinct.kernelGrouping=true -Drdf4j.lmdb.nativeValueMemo.enabled=true -Drdf4j.lmdb.repository.partialReady=true -Drdf4j.lmdb.nativeDistinct.widePrimitive=true -Drdf4j.lmdb.expressionDomain.bulkIdRecordIo=true -Drdf4j.lmdb.expressionDomain.reuseMembershipMissSlot=true -Drdf4j.lmdb.queryMemory.maxBytes=2684354560 -Drdf4j.lmdb.queryMemory.perQueryMaxBytes=671088640 -Doverlay.benchmark.fixture="$D/fixture-$groups" -Doverlay.benchmark.warmups=${WARMUPS:-0} -cp "$CP" org.eclipse.rdf4j.sail.lmdb.evaluation.ExperimentalProfileProbe "$groups" 10 ${ITERATIONS:-1} true > "$O/$name.log" 2>&1
