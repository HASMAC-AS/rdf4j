#!/bin/bash
set -euo pipefail
exec 9>../native-benchmark.lock; flock 9
route=${1:-irKernel}; groups=${2:-20}; budget=${3:-67108864}; stem=${4:-ordered-small-ir}; checksum=${5:-}; warmups=${6:-0}; iterations=${7:-1}
CP="experiments/overlay/probe-segmented-classes:core/sail/lmdb/target/test-classes:$(cat experiments/overlay/classpath.txt)"
mkdir -p experiments/overlay/ordered-distinct-test-classes
../tools/jdk-25.0.5+8/bin/javac -cp "$CP" -d experiments/overlay/ordered-distinct-test-classes core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/evaluation/OverlayOptionalQueryBenchmark.java
fixture=../fixture-ordered-$groups
if [ "$groups" = 20 ]; then fixture=../fixture-ordered-small; fi
if [ "$groups" = 8000 ]; then fixture=../fixture-eight-million; fi
extra=();
if [ -n "${PACKED_VECTOR_ROWS:-}" ]; then extra+=("-Drdf4j.lmdb.packedFtree.vectorRows=$PACKED_VECTOR_ROWS"); fi
if [ -n "${PACKED_ROOT_ROWS:-}" ]; then extra+=("-Drdf4j.lmdb.packedFtree.rootBatchRows=$PACKED_ROOT_ROWS"); fi
if [ -n "${ORDERED_JFR:-}" ]; then extra+=("-XX:StartFlightRecording=filename=$ORDERED_JFR,settings=profile,dumponexit=true"); fi; if [ -n "$checksum" ]; then extra+=("-Doverlay.benchmark.expectedChecksum=$checksum"); fi
../tools/jdk-25.0.5+8/bin/java --enable-native-access=ALL-UNNAMED -Xms2g -Xmx5g -Drdf4j.lmdb.nativeValueMemo.enabled=true -Drdf4j.lmdb.repository.partialReady=true -Drdf4j.lmdb.nativeDistinct.widePrimitive=true -Drdf4j.lmdb.expressionDomain.bulkIdRecordIo=true -Drdf4j.lmdb.expressionDomain.reuseMembershipMissSlot=true -Drdf4j.lmdb.queryMemory.maxBytes=2684354560 -Drdf4j.lmdb.queryMemory.perQueryMaxBytes="$budget" -Doverlay.benchmark.route="$route" -Doverlay.benchmark.fixture="$fixture" -Doverlay.benchmark.warmups="$warmups" "${extra[@]}" -cp "experiments/overlay/ordered-distinct-test-classes:$CP" org.eclipse.rdf4j.sail.lmdb.evaluation.OverlayOptionalQueryBenchmark "$groups" 10 "$iterations" true > "experiments/overlay/$stem.log" 2>&1
