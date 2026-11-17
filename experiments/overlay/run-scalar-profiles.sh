#!/bin/bash
set -euo pipefail
exec 9>../native-benchmark.lock
flock 9
JAVA=../tools/jdk-25.0.5+8/bin/java
CP="core/sail/lmdb/target/test-classes:experiments/overlay/typed-scalar-classes:$(cat experiments/overlay/classpath.txt)"
for typed in false true; do
 "$JAVA" --enable-native-access=ALL-UNNAMED -Xms2g -Xmx3g -Doverlay.benchmark.valueCacheSize=1024 -Drdf4j.lmdb.valueOverlay.typedMaterialization=$typed -XX:StartFlightRecording=filename=experiments/overlay/scalar-profile-$typed.jfr,settings=experiments/overlay/profile-2ms.jfc,dumponexit=true -cp "$CP" org.eclipse.rdf4j.sail.lmdb.evaluation.OverlayOptionalQueryBenchmark 100 10 8 true > experiments/overlay/scalar-profile-$typed.log 2>&1
 ../tools/jdk-25.0.5+8/bin/jfr print --json --events jdk.ExecutionSample experiments/overlay/scalar-profile-$typed.jfr > experiments/overlay/scalar-profile-$typed-samples.json
done
