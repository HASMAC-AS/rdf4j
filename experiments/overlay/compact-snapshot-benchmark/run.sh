#!/bin/bash
set -euo pipefail
ROOT=$(cd "$(dirname "$0")/../../.." && pwd)
cd "$ROOT"
variant=$1; name=$2; groups=${3:-8000}
D=experiments/overlay/lifetime
JAVA=experiments/overlay/tools/jdk-25.0.5+8/bin/java
O=experiments/overlay/compact-snapshot-benchmark
case "$variant" in
 baseline) API="$D/original-api-classes"; LMDB="$D/combined-classes"; EXPECTED=org.eclipse.rdf4j.query.impl.MapBindingSet ;;
 pre_lifetime) API="$D/original-api-classes"; LMDB="$D/baseline-classes"; EXPECTED=org.eclipse.rdf4j.query.impl.MapBindingSet ;;
 candidate) API="$O/candidate-api-classes"; LMDB="$D/combined-classes"; EXPECTED=org.eclipse.rdf4j.sail.features.CompactBindingSetSnapshot ;;
 hash_candidate) API="experiments/overlay/compact-snapshots/hash-api-classes"; LMDB="$D/combined-classes"; EXPECTED=org.eclipse.rdf4j.sail.features.CompactBindingSetSnapshot ;;
 compact_only) API="$O/candidate-api-classes"; LMDB="$D/baseline-classes"; EXPECTED=org.eclipse.rdf4j.sail.features.CompactBindingSetSnapshot ;;
 *) echo "Unknown variant $variant" >&2; exit 2 ;;
esac
CP="$O/harness:$API:$LMDB:$(cat "$D/classpath.txt")"
EXTRA=()
if [[ -n "${SNAPSHOT_FLAG:-}" ]]; then EXTRA+=("-D${SNAPSHOT_FLAG}=true"); fi
exec 9>experiments/overlay/native-benchmark.lock; flock 9
python3 experiments/overlay/main-comparison/timed.py "$JAVA" --enable-native-access=ALL-UNNAMED -XX:FlightRecorderOptions=stackdepth=128 -Xms2g -Xmx5g -Dlogback.configurationFile=experiments/overlay/main-comparison/logback.xml "${EXTRA[@]}" -Dprofile.writer=${WRITER:-false} -Dprofile.record=${RECORD:-false} -Dprofile.file="$O/$name" -Dsnapshot.expectedClass="$EXPECTED" -Dcomparison.exact=${EXACT:-false} -Dcomparison.narrow=false -Dcomparison.requireNoSpill=true -Drdf4j.lmdb.nativeDistinct.kernelGrouping=true -Drdf4j.lmdb.nativeValueMemo.enabled=true -Drdf4j.lmdb.repository.partialReady=true -Drdf4j.lmdb.nativeDistinct.widePrimitive=true -Drdf4j.lmdb.expressionDomain.bulkIdRecordIo=true -Drdf4j.lmdb.expressionDomain.reuseMembershipMissSlot=true -Drdf4j.lmdb.queryMemory.maxBytes=2684354560 -Drdf4j.lmdb.queryMemory.perQueryMaxBytes=671088640 -Doverlay.benchmark.fixture="$D/fixture-$groups" -Doverlay.benchmark.warmups=${WARMUPS:-0} -cp "$CP" org.eclipse.rdf4j.sail.lmdb.evaluation.ExperimentalProfileProbe "$groups" 10 ${ITERATIONS:-1} true > "$O/$name.log" 2>&1
