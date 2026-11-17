#!/bin/bash
set -euo pipefail
JAVA=../tools/jdk-25.0.5+8/bin/java
POST=${POST_REVIEW_CLASSES:?Supply frozen postreview production directory}
TEST=experiments/overlay/review-abba-test-classes
# Caller freezes shared harness/helper and production snapshots before starting.
i=0
for mode in pre post post pre; do
 i=$((i+1))
 classes=experiments/overlay/probe-segmented-classes
 if [ "$mode" = post ]; then classes="$POST"; fi
 stem="experiments/overlay/review-spill-abba-$i-$mode"
 (
  exec 9>../native-benchmark.lock; flock 9
  python3 experiments/overlay/time-probe-jvm.py "$stem.json" "$JAVA" --enable-native-access=ALL-UNNAMED -Xms2g -Xmx5g "-Xlog:gc:file=$stem.gc.log:time,uptime,level,tags" -Drdf4j.lmdb.nativeValueMemo.enabled=true -Drdf4j.lmdb.repository.partialReady=true -Drdf4j.lmdb.nativeDistinct.widePrimitive=true -Drdf4j.lmdb.expressionDomain.bulkIdRecordIo=true -Drdf4j.lmdb.expressionDomain.reuseMembershipMissSlot=true -Doverlay.benchmark.expectedChecksum=-3441391656945048864 -Doverlay.benchmark.fixture=../fixture-eight-million -Doverlay.benchmark.warmups=0 -cp "$TEST:$classes:$(cat experiments/overlay/classpath.txt)" org.eclipse.rdf4j.sail.lmdb.evaluation.OverlayOptionalQueryBenchmark 8000 10 1 true > "$stem.log" 2>&1
 )
 # Allow queued regression shards to acquire the shared lock between forks.
 sleep 1
done
