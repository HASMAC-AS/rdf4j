#!/bin/bash
set -euo pipefail
ROOT=$(cd "$(dirname "$0")/.." && pwd); cd "$ROOT"
variant=$1; narrow=$2; name=$3
CLASS=MainComparisonBenchmark
if [[ $variant == main ]]; then
 CP="main-comparison/main-classes:rdf4j-main-baseline/core/sail/lmdb/target/classes:$(cat main-comparison/main-classpath.txt)"
else
 CLASS=ExperimentalComparisonProbe
 CP="main-comparison/exp-classes:main-comparison/exp-production:$(cat main-comparison/exp-classpath.txt)"
fi
exec 9>native-benchmark.lock; flock 9
python3 main-comparison/timed.py tools/jdk-25.0.5+8/bin/java --enable-native-access=ALL-UNNAMED -Xms2g -Xmx5g -Dlogback.configurationFile=main-comparison/logback.xml -Xlog:gc:file=main-comparison/$name-gc.log -Dcomparison.exact=${EXACT:-false} -Dcomparison.narrow=$narrow -Dcomparison.requireNoSpill=$narrow -Drdf4j.lmdb.nativeDistinct.kernelGrouping=true -Drdf4j.lmdb.nativeValueMemo.enabled=true -Drdf4j.lmdb.repository.partialReady=true -Drdf4j.lmdb.nativeDistinct.widePrimitive=true -Drdf4j.lmdb.expressionDomain.bulkIdRecordIo=true -Drdf4j.lmdb.expressionDomain.reuseMembershipMissSlot=true -Drdf4j.lmdb.queryMemory.maxBytes=2684354560 -Drdf4j.lmdb.queryMemory.perQueryMaxBytes=671088640 -Doverlay.benchmark.fixture=main-comparison/fixture-$variant -Doverlay.benchmark.warmups=${WARMUPS:-0} -cp "$CP" org.eclipse.rdf4j.sail.lmdb.evaluation.$CLASS 8000 10 ${RUNS:-1} true >main-comparison/$name.log 2>&1
