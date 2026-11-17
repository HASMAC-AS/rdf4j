#!/bin/bash
set -euo pipefail
exec 9>../native-benchmark.lock
flock 9
JAVA=../tools/jdk-25.0.5+8/bin/java
CP="core/sail/lmdb/target/test-classes:experiments/overlay/typed-scalar-classes:$(cat experiments/overlay/classpath.txt)"
i=0
for typed in false true true false; do
 i=$((i+1))
 "$JAVA" --enable-native-access=ALL-UNNAMED -Xms2g -Xmx3g -Doverlay.benchmark.valueCacheSize=1024 -Drdf4j.lmdb.valueOverlay.typedMaterialization=$typed -cp "$CP" org.eclipse.rdf4j.sail.lmdb.evaluation.OverlayOptionalQueryBenchmark 100 10 8 true > experiments/overlay/scalar-abba-$i-$typed.log 2>&1
done
