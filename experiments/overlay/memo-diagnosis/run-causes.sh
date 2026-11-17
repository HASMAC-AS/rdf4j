#!/bin/bash
set -euo pipefail
cd "$(dirname "$0")/../../.."
exec 8>../.rdf4j-heavy-jvm.lock; flock 8
P=experiments/overlay/memo-diagnosis
if [ -e "$P/diagnostic-classes" ]; then echo 'Refusing frozen directory overwrite'; exit 1; fi
cp -a experiments/overlay/lifetime/combined-classes "$P/diagnostic-classes"
experiments/overlay/tools/jdk-25.0.5+8/bin/javac -cp "$P/diagnostic-classes:experiments/overlay/workbench-e2e/runtime/BOOT-INF/lib/*" -d "$P/diagnostic-classes" "$P/source/org/eclipse/rdf4j/sail/lmdb/evaluation/NativeValueMemo.java"
python3 "$P/run-diagnostic.py" combined_hash on miss-causes-basic --size 100 --validate --transport on
python3 "$P/run-diagnostic.py" combined_hash on miss-causes-seen --size 100 --validate --transport on --seen
