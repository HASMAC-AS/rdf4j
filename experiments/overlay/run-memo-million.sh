#!/bin/bash
set -euo pipefail
exec 9>../native-benchmark.lock
flock 9
JAVA=../tools/jdk-25.0.5+8/bin/java
CP="core/sail/lmdb/target/test-classes:experiments/overlay/memo-combined-classes:$(cat experiments/overlay/classpath.txt)"
i=0
for pair in 'false false' 'true false' 'false true' 'true true' 'true true' 'false true' 'true false' 'false false'; do
 read ready wide <<< "$pair";i=$((i+1))
 "$JAVA" --enable-native-access=ALL-UNNAMED -Xms2g -Xmx4g -Drdf4j.lmdb.nativeValueMemo.enabled=true -Drdf4j.lmdb.repository.partialReady=$ready -Drdf4j.lmdb.nativeDistinct.widePrimitive=$wide -cp "$CP" org.eclipse.rdf4j.sail.lmdb.evaluation.OverlayOptionalQueryBenchmark 1000 10 4 true > experiments/overlay/memo-million-$i-$ready-$wide.log 2>&1
done
