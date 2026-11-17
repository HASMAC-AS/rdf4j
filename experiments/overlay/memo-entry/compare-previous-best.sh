#!/bin/bash
set -euo pipefail
cd "$(dirname "$0")/../../.."
exec 8>../.rdf4j-heavy-jvm.lock; flock 8
D=experiments/overlay/workbench-e2e
python3 experiments/overlay/memo-entry/summarize.py > experiments/overlay/memo-entry/metadata-summary.json
for trial in 1-baseline 2-on 3-on 4-baseline; do
 mode=${trial#*-}
 python3 "$D/run.py" combined_hash on "memo-best-abba-$trial" --size 1000000 --transport on --memo-metadata "$mode"
done
python3 "$D/run.py" combined_hash on memo-entry-profile-on --size 1000000 --transport on --memo-metadata on --profile
