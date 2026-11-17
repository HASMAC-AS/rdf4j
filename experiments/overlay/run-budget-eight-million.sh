#!/bin/bash
set -euo pipefail
exec 9>../native-benchmark.lock; flock 9
JAVA=../tools/jdk-25.0.5+8/bin/java
for mode in generous default; do
 options=()
 if [ "$mode" = generous ]; then options=(-Drdf4j.lmdb.queryMemory.maxBytes=4294967296 -Drdf4j.lmdb.queryMemory.perQueryMaxBytes=3758096384); fi
 "$JAVA" --enable-native-access=ALL-UNNAMED -Xms2g -Xmx5g -Drdf4j.lmdb.nativeValueMemo.enabled=true -Drdf4j.lmdb.repository.partialReady=true -Drdf4j.lmdb.nativeDistinct.widePrimitive=true "${options[@]}" -Doverlay.benchmark.fixture=../fixture-eight-million -Doverlay.benchmark.warmups=0 -cp "core/sail/lmdb/target/test-classes:experiments/overlay/budgeted-wide-final-classes:$(cat experiments/overlay/classpath.txt)" org.eclipse.rdf4j.sail.lmdb.evaluation.OverlayOptionalQueryBenchmark 8000 10 1 true > experiments/overlay/budget-eight-million-$mode.log 2>&1
done
