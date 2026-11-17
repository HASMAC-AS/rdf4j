#!/bin/bash
set -euo pipefail
exec 9>../native-benchmark.lock; flock 9
../tools/jdk-25.0.5+8/bin/java --enable-native-access=ALL-UNNAMED -Xms2g -Xmx4g -Drdf4j.lmdb.nativeValueMemo.enabled=true -Drdf4j.lmdb.repository.partialReady=true -Drdf4j.lmdb.nativeDistinct.widePrimitive=true -Drdf4j.lmdb.queryMemory.maxBytes=268435456 -Drdf4j.lmdb.queryMemory.perQueryMaxBytes=67108864 -Doverlay.benchmark.fixture=../fixture-budget-million -Doverlay.benchmark.warmups=0 -cp "core/sail/lmdb/target/test-classes:experiments/overlay/budgeted-wide-final-classes:$(cat experiments/overlay/classpath.txt)" org.eclipse.rdf4j.sail.lmdb.evaluation.OverlayOptionalQueryBenchmark 1000 10 1 true > experiments/overlay/budget-spill-million.log 2>&1
