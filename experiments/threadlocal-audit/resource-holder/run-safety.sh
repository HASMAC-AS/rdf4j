#!/usr/bin/env bash
set -euo pipefail
ROOT=$(cd "$(dirname "$0")/../../.." && pwd)
cd "$ROOT"
ARM=${1:?a or ad}; LAYER=${2:?pristine or candidate store layer}; SELECTION=${3:?selection file}; FAILURES=${4:?failure file}
CP="$ROOT/experiments/threadlocal-audit/resource-holder/test-layer:$LAYER:$(cat experiments/threadlocal-audit/read-depth/cp_${ARM}.txt)"
exec flock experiments/overlay/native-benchmark.lock timeout --signal=TERM --kill-after=15s 12m ../toolchain/jdk-25.0.5+8/bin/java -Xmx4g --enable-native-access=ALL-UNNAMED -javaagent:"$ROOT/.m2_repo/org/mockito/mockito-core/5.24.0/mockito-core-5.24.0.jar" --source 25 -cp "$CP" experiments/threadlocal-audit/resource-holder/ResourceHolderSafety.java "$SELECTION" "$FAILURES"
