#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../.."
exec 9>experiments/overlay/native-benchmark.lock; flock 9
CP="$(realpath experiments/threadlocal-audit/harness-classes):$(cat experiments/threadlocal-audit/cp-b.txt)"
JAVA=../toolchain/jdk-25.0.5+8/bin/java
mapfile -t FLAGS < experiments/threadlocal-audit/best-flags.txt
for regime in regular interpreted janino; do
 "$JAVA" --enable-native-access=ALL-UNNAMED -Xms1g -Xmx3g "${FLAGS[@]}" -Daudit.regime=$regime -Doverlay.benchmark.warmups=0 -cp "$CP" org.eclipse.rdf4j.sail.lmdb.evaluation.ThreadLocalQueryBenchmark 2 10 1 true > experiments/threadlocal-audit/smoke-$regime.log 2>&1
done
"$JAVA" --enable-native-access=ALL-UNNAMED -Xms1g -Xmx3g -Daudit.warmups=0 -cp "$CP" org.eclipse.rdf4j.sail.lmdb.evaluation.ThreadLocalCoScanBenchmark 2 1 > experiments/threadlocal-audit/smoke-coscan.log 2>&1
