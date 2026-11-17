#!/bin/bash
set -euo pipefail
exec 9>../native-benchmark.lock; flock 9
JAVA=../tools/jdk-25.0.5+8/bin/java
CP="experiments/overlay/probe-abba-test-classes:experiments/overlay/probe-segmented-classes:$(cat experiments/overlay/classpath.txt)"
run=0
for reuse in false true true false; do
 run=$((run + 1))
 stem="experiments/overlay/probe-abba-$run-$reuse"
 python3 experiments/overlay/time-probe-jvm.py "$stem.json" "$JAVA" --enable-native-access=ALL-UNNAMED -Xms2g -Xmx5g "-Xlog:gc:file=$stem.gc.log:time,uptime,level,tags" -Drdf4j.lmdb.nativeValueMemo.enabled=true -Drdf4j.lmdb.repository.partialReady=true -Drdf4j.lmdb.nativeDistinct.widePrimitive=true -Drdf4j.lmdb.expressionDomain.bulkIdRecordIo=true -Drdf4j.lmdb.expressionDomain.reuseMembershipMissSlot="$reuse" -Doverlay.benchmark.fixture=../fixture-eight-million -Doverlay.benchmark.warmups=0 -Doverlay.benchmark.expectedBulkRecords=8000000 -cp "$CP" org.eclipse.rdf4j.sail.lmdb.evaluation.SpillProbeReuseQueryProbe 8000 10 1 true > "$stem.log" 2>&1
 python3 experiments/overlay/summarize-probe-abba.py
 done
