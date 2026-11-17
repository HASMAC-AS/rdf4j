#!/bin/bash
set -euo pipefail
exec 9>../native-benchmark.lock
flock 9
JAVA=../tools/jdk-25.0.5+8/bin/java
i=0
for classes in experiments/overlay/typed-vector-classes experiments/overlay/typed-vector-classes experiments/overlay/typed-vector-classes experiments/overlay/typed-vector-classes; do
 i=$((i+1))
 CP="core/sail/lmdb/target/test-classes:$classes:$(cat experiments/overlay/classpath.txt)"
 "$JAVA" --enable-native-access=ALL-UNNAMED -Xms2g -Xmx2g -Drdf4j.lmdb.valueOverlay.typedMaterialization=$([ "$i" = 1 ] || [ "$i" = 4 ] && echo false || echo true) -cp "$CP" org.eclipse.rdf4j.sail.lmdb.ValueStoreOverlayMaterializationBenchmark true 12 512 1024 5000 > experiments/overlay/vector-baseline-$i.log 2>&1
done
