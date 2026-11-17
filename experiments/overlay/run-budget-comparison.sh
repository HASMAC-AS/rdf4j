#!/bin/bash
set -euo pipefail
exec 9>../native-benchmark.lock; flock 9
JAVA=../tools/jdk-25.0.5+8/bin/java
# Freeze both class directories before running. Legacy snapshot predates bounded-wide implementation.
i=0
for mode in legacy budget budget legacy; do
 i=$((i+1)); expected=true; classes=experiments/overlay/budgeted-wide-classes
 if [ "$mode" = legacy ]; then expected=false; classes=experiments/overlay/memo-combined-classes:experiments/overlay/budgeted-wide-classes; fi
 "$JAVA" --enable-native-access=ALL-UNNAMED -Xms2g -Xmx4g -Drdf4j.lmdb.nativeValueMemo.enabled=true -Drdf4j.lmdb.repository.partialReady=true -Drdf4j.lmdb.nativeDistinct.widePrimitive=true -Doverlay.benchmark.expectedBudgetedWide=$expected -Doverlay.benchmark.fixture=../fixture-budget-million -Doverlay.benchmark.warmups=2 -cp "core/sail/lmdb/target/test-classes:$classes:$(cat experiments/overlay/classpath.txt)" org.eclipse.rdf4j.sail.lmdb.evaluation.OverlayOptionalQueryBenchmark 1000 10 3 true > experiments/overlay/budget-comparison-$i-$mode.log 2>&1
done
