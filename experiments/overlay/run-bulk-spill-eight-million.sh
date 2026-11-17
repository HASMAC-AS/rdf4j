#!/bin/bash
set -euo pipefail
exec 9>../native-benchmark.lock; flock 9
JAVA=../tools/jdk-25.0.5+8/bin/java
for mode in flat segmented; do
 "$JAVA" --enable-native-access=ALL-UNNAMED -Xms2g -Xmx5g -Drdf4j.lmdb.nativeValueMemo.enabled=true -Drdf4j.lmdb.repository.partialReady=true -Drdf4j.lmdb.nativeDistinct.widePrimitive=true -Drdf4j.lmdb.expressionDomain.bulkIdRecordIo=true -Doverlay.benchmark.fixture=../fixture-eight-million -Doverlay.benchmark.warmups=0 -Doverlay.benchmark.expectedBulkRecords=8000000 -cp "core/sail/lmdb/target/test-classes:experiments/overlay/bulk-$mode-classes:$(cat experiments/overlay/classpath.txt)" org.eclipse.rdf4j.sail.lmdb.evaluation.BulkSpillQueryProbe 8000 10 1 true > experiments/overlay/bulk-eight-million-$mode.log 2>&1
done
