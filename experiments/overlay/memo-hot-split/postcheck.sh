#!/bin/bash
set -euo pipefail
cd "$(dirname "$0")/../../.."
exec 8>../.rdf4j-heavy-jvm.lock; flock 8
D=experiments/overlay/memo-hot-split
cmp core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/NativeValueMemo.java "$D/NativeValueMemo.candidate.java"
bash experiments/overlay/lifetime/mvn.sh -o -pl core/sail/lmdb -Pquick install > maven-build.log 2>&1
cp maven-build.log "$D/postchange-install.log"
for pair in false-true false-false true-true true-false; do
 mode=${pair%-*}; probe=${pair#*-}
 bash experiments/overlay/lifetime/mvn.sh -o -pl core/sail/lmdb -Dtest=NativeValueMemoTest,NativeMemoPolicyTest,NativeMemoEntryMetadataTest -Drdf4j.lmdb.nativeValueMemo.entryMetadata="$mode" -Drdf4j.lmdb.nativeValueMemo.testProbe="$probe" -DforkCount=1 surefire:test > "$D/postchange-$pair-tests.log" 2>&1
 cat core/sail/lmdb/target/surefire-reports/*Native*Memo*.txt > "$D/postchange-$pair-reports.txt"
done
bash experiments/overlay/lifetime/mvn.sh -o -pl core/sail/lmdb '-Dtest=LmdbMapGrowthReplayLifecycleTest#initiallyCompiledMemoIsRecreatedAcrossUnobservedMapGrowth+replayRestoresCallerContextWhenReopeningFails+compiledGroupedDistinctIsRecheckedAcrossMapGrowth' -Drdf4j.lmdb.nativeValueMemo.entryMetadata=false -DforkCount=1 -Drdf4j.lmdb.test.maxHeap=2G -Drdf4j.lmdb.nativeValueMemo.enabled=true -Drdf4j.lmdb.nativeValueMemo.testProbe=true -Drdf4j.lmdb.nativeLazyResults.enabled=false -Drdf4j.lmdb.nativeQueryEngine.enabled=true -Drdf4j.lmdb.janinoCodegen.enabled=true -Drdf4j.lmdb.janinoCodegen.synchronous=true -Drdf4j.lmdb.janinoCodegen.thresholdRows=0 -Drdf4j.lmdb.kernelInterpreter.enabled=false surefire:test > "$D/postchange-replay-tests.log" 2>&1
cp core/sail/lmdb/target/surefire-reports/org.eclipse.rdf4j.sail.lmdb.LmdbMapGrowthReplayLifecycleTest.txt "$D/postchange-replay-report.txt"
