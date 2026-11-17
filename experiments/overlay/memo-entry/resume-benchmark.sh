#!/bin/bash
set -euo pipefail
cd "$(dirname "$0")/../../.."
exec 8>../.rdf4j-heavy-jvm.lock; flock 8
for pair in false-false true-false false-true true-true; do
 grep -q "BUILD SUCCESS" "experiments/overlay/memo-entry/final-$pair-tests.log" || exit 1
done
grep -q "Tests run: 3, Failures: 0, Errors: 0, Skipped: 0" experiments/overlay/memo-entry/recovered3-replay-report.txt
D=experiments/overlay/workbench-e2e
M=experiments/overlay/transport-mapping/metadata-lmdb-classes
J=experiments/overlay/tools/jdk-25.0.5+8/bin
if [ -e "$M" ]; then echo 'Frozen candidate already exists; refusing overwrite'; exit 1; fi
mkdir -p "$M"
cp -a experiments/overlay/lifetime/combined-classes/. "$M/"
"$J/javac" -cp "$M:$D/runtime/BOOT-INF/lib/*" -d "$M" core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/NativeValueMemo.java
"$J/javac" -cp "$D/runtime/BOOT-INF/classes:$D/runtime/BOOT-INF/lib/*" -d "$D/harness" "$D/WorkbenchBenchmarkServer.java"
for mode in off on; do
 python3 "$D/run.py" combined_hash on "memo-entry-smoke-$mode" --size 100 --validate --transport on --memo-metadata "$mode"
done
for trial in 1-off 2-on 3-on 4-off; do
 mode=${trial#*-}
 python3 "$D/run.py" combined_hash on "memo-entry-abba-$trial" --size 1000000 --transport on --memo-metadata "$mode"
done
