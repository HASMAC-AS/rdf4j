#!/bin/bash
set -euo pipefail
cd "$(dirname "$0")/../../.."
exec 8>../.rdf4j-heavy-jvm.lock; flock 8
P=experiments/overlay/memo-diagnosis
python3 "$P/run-jit.py" combined_hash on memo-jit-baseline --size 100 --validate --transport on --memo-metadata baseline --profile
python3 "$P/run-jit.py" combined_hash on memo-jit-entry --size 100 --validate --transport on --memo-metadata on --profile
