#!/bin/bash
set -euo pipefail
CP="core/sail/lmdb/target/test-classes:core/sail/lmdb/target/classes:$(cat experiments/overlay/classpath.txt)"
i=0
for grouped in false true true false; do
 i=$((i+1)); stem="experiments/overlay/kernel-grouping-adjacency-8m-$i-$grouped"
 (
 exec 9>../native-benchmark.lock; flock 9
 ../tools/jdk-25.0.5+8/bin/java --enable-native-access=ALL-UNNAMED -Xms2g -Xmx5g -Drdf4j.lmdb.nativeDistinct.kernelGrouping="$grouped" -Doverlay.benchmark.expectedGrouping="$grouped" -Drdf4j.lmdb.nativeValueMemo.enabled=true -Drdf4j.lmdb.repository.partialReady=true -Drdf4j.lmdb.nativeDistinct.widePrimitive=true -Drdf4j.lmdb.expressionDomain.bulkIdRecordIo=true -Drdf4j.lmdb.expressionDomain.reuseMembershipMissSlot=true -Drdf4j.lmdb.queryMemory.maxBytes=2684354560 -Drdf4j.lmdb.queryMemory.perQueryMaxBytes=671088640 -Doverlay.benchmark.expectedChecksum=-3441391656945048864 -Doverlay.benchmark.fixture=../fixture-eight-million -Doverlay.benchmark.warmups=0 -cp "$CP" org.eclipse.rdf4j.sail.lmdb.evaluation.KernelGroupingQueryProbe 8000 10 1 true > "$stem.log" 2>&1
 )
done
