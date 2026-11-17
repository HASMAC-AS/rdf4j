#!/bin/bash
set -euo pipefail
cd "$(dirname "$0")/../../.."
exec 8>../.rdf4j-heavy-jvm.lock; flock 8
P=experiments/overlay/memo-hot-split
for pair in false-true false-false true-true true-false; do grep -q 'BUILD SUCCESS' "$P/postchange-$pair-tests.log"; done
grep -q 'Tests run: 3, Failures: 0, Errors: 0, Skipped: 0' "$P/postchange-replay-report.txt"
python3 - <<'PY'
import json,pathlib
r=json.loads(pathlib.Path('experiments/overlay/workbench-e2e/hot-split-smoke.json').read_text());assert r['witnessesPassed'];assert r['checksum']=='495a217169c1db06ff284e3b4d8a765a6cc75b575cc8179fa1e448b09628c301'
for f in pathlib.Path('experiments/overlay/workbench-e2e').glob('hot-split-abba-*.json'):raise SystemExit('Refusing existing timing artifact: '+str(f))
PY
for trial in 1-baseline 2-split 3-split 4-baseline; do
 args=(); if [[ "$trial" == *split ]]; then args+=(--hot-split); fi
 python3 "$P/run-split.py" combined_hash on "hot-split-abba-$trial" --size 1000000 --transport on "${args[@]}"
done
python3 "$P/run-split.py" combined_hash on hot-split-profile --size 1000000 --transport on --hot-split --jit --profile
