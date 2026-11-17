#!/bin/bash
set -euo pipefail
cd "$(dirname "$0")/../../.."
exec 9>../.rdf4j-heavy-jvm.lock; flock 9
D=experiments/overlay/memo-entry
for pair in false-false true-false false-true true-true; do
 mode=${pair%-*}; probe=${pair#*-}; goal=surefire:test
 if [ "$pair" = false-false ]; then goal=test; fi
 bash experiments/overlay/lifetime/mvn.sh -o -pl core/sail/lmdb -Dtest=NativeValueMemoTest,NativeMemoPolicyTest,NativeMemoEntryMetadataTest -Drdf4j.lmdb.nativeValueMemo.entryMetadata="$mode" -Drdf4j.lmdb.nativeValueMemo.testProbe="$probe" -DforkCount=1 "$goal" > "$D/final-$pair-tests.log" 2>&1
 cat core/sail/lmdb/target/surefire-reports/*Native*Memo*.txt > "$D/final-$pair-reports.txt"
done
