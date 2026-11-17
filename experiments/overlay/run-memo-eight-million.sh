#!/bin/bash
set -euo pipefail
exec 9>../native-benchmark.lock;flock 9
JAVA=../tools/jdk-25.0.5+8/bin/java
CP="core/sail/lmdb/target/test-classes:experiments/overlay/memo-combined-classes:$(cat experiments/overlay/classpath.txt)"
i=0
for pair in 'false false' 'false true' 'true true' 'true true' 'false true' 'false false'; do
 read ready wide <<< "$pair";i=$((i+1))
 "$JAVA" --enable-native-access=ALL-UNNAMED -Xms2g -Xmx5g -Drdf4j.lmdb.nativeValueMemo.enabled=true -Drdf4j.lmdb.repository.partialReady=$ready -Drdf4j.lmdb.nativeDistinct.widePrimitive=$wide -Doverlay.benchmark.fixture=../fixture-eight-million -Doverlay.benchmark.warmups=0 -cp "$CP" org.eclipse.rdf4j.sail.lmdb.evaluation.OverlayOptionalQueryBenchmark 8000 10 1 true > experiments/overlay/memo-eight-million-$i-$ready-$wide.log 2>&1
done
