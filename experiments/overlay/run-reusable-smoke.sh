#!/bin/bash
set -euo pipefail
exec 9>../native-benchmark.lock;flock 9
JAVA=../tools/jdk-25.0.5+8/bin/java
CP="core/sail/lmdb/target/test-classes:experiments/overlay/memo-combined-classes:$(cat experiments/overlay/classpath.txt)"
for i in 1 2;do
 "$JAVA" --enable-native-access=ALL-UNNAMED -Xms512m -Xmx2g -Drdf4j.lmdb.nativeValueMemo.enabled=true -Doverlay.benchmark.fixture=../fixture-reuse-smoke -Doverlay.benchmark.warmups=0 -cp "$CP" org.eclipse.rdf4j.sail.lmdb.evaluation.OverlayOptionalQueryBenchmark 2 3 1 true > experiments/overlay/reuse-smoke-$i.log 2>&1
done
