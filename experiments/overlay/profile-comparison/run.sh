#!/bin/bash
set -euo pipefail
cd "$(dirname "$0")/.."
variant=$1; writer=$2; name=$3
CLASS=ProfileComparisonBenchmark
if [[ $variant == main ]]; then
 CP="profile-comparison/main-classes:rdf4j-main-baseline/core/sail/lmdb/target/classes:$(cat main-comparison/main-classpath.txt)"
else
 CLASS=ExperimentalProfileProbe
 CP="profile-comparison/exp-classes:main-comparison/exp-production:$(cat main-comparison/exp-classpath.txt)"
fi
exec 9>native-benchmark.lock; flock 9
python3 main-comparison/timed.py tools/jdk-25.0.5+8/bin/java --enable-native-access=ALL-UNNAMED -XX:FlightRecorderOptions=stackdepth=128 -Xms2g -Xmx5g -Dlogback.configurationFile=main-comparison/logback.xml -Dprofile.writer=$writer -Dprofile.record=${RECORD:-true} -Dprofile.file=profile-comparison/$name -Dcomparison.narrow=false -Drdf4j.lmdb.nativeDistinct.kernelGrouping=true -Drdf4j.lmdb.nativeValueMemo.enabled=true -Drdf4j.lmdb.repository.partialReady=true -Drdf4j.lmdb.nativeDistinct.widePrimitive=true -Drdf4j.lmdb.expressionDomain.bulkIdRecordIo=true -Drdf4j.lmdb.expressionDomain.reuseMembershipMissSlot=true -Drdf4j.lmdb.queryMemory.maxBytes=2684354560 -Drdf4j.lmdb.queryMemory.perQueryMaxBytes=671088640 -Doverlay.benchmark.fixture=main-comparison/fixture-$variant -Doverlay.benchmark.warmups=0 -cp "$CP" org.eclipse.rdf4j.sail.lmdb.evaluation.$CLASS 8000 10 1 true >profile-comparison/$name.log 2>&1
