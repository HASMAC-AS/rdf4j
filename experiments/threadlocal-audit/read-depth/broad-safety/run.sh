#!/usr/bin/env bash
set -euo pipefail
ROOT=$(cd "$(dirname "$0")/../../.." && pwd)
# script lives one directory below read-depth
ROOT=$(cd "$ROOT/.." && pwd)
cd "$ROOT"
ARM=${1:?a or ad}; SELECTION=${2:?selection}; FAILURES=${3:?failure list}
CP=$(cat "experiments/threadlocal-audit/read-depth/cp_${ARM}.txt")
exec flock experiments/overlay/native-benchmark.lock timeout --signal=TERM --kill-after=15s 12m ../toolchain/jdk-25.0.5+8/bin/java -Xmx4g --enable-native-access=ALL-UNNAMED -javaagent:"$ROOT/.m2_repo/org/mockito/mockito-core/5.24.0/mockito-core-5.24.0.jar" --source 25 -cp "$CP" experiments/threadlocal-audit/read-depth/broad-safety/BroadSafety.java "$SELECTION" "$FAILURES"
