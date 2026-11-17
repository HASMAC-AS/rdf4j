#!/bin/bash
set -euo pipefail
cd "$(dirname "$0")/../../.."
exec 8>../.rdf4j-heavy-jvm.lock; flock 8
P=experiments/overlay/memo-hot-split
for pair in false-true false-false true-true true-false; do grep -q 'BUILD SUCCESS' "$P/postchange-$pair-tests.log"; done
grep -q "Tests run: 3, Failures: 0, Errors: 0, Skipped: 0" "$P/postchange-replay-report.txt"
if [ -e "$P/candidate-classes" ]; then echo 'Refusing frozen artifact overwrite'; exit 1; fi
cp -a experiments/overlay/lifetime/combined-classes "$P/candidate-classes"
experiments/overlay/tools/jdk-25.0.5+8/bin/javac -cp "$P/candidate-classes:experiments/overlay/workbench-e2e/runtime/BOOT-INF/lib/*" -d "$P/candidate-classes" "$P/NativeValueMemo.candidate.java"
python3 "$P/run-split.py" combined_hash on hot-split-smoke --size 100 --validate --transport on --hot-split
for trial in 1-baseline 2-split 3-split 4-baseline; do
 args=(); if [[ "$trial" == *split ]]; then args+=(--hot-split); fi
 python3 "$P/run-split.py" combined_hash on "hot-split-abba-$trial" --size 1000000 --transport on "${args[@]}"
done
python3 "$P/run-split.py" combined_hash on hot-split-profile --size 1000000 --transport on --hot-split --jit --profile
