#!/bin/bash
set -euo pipefail
ROOT=$(cd "$(dirname "$0")/../../.." && pwd)
cd "$ROOT"
D=experiments/overlay/memo-policy
JAVA=experiments/overlay/tools/jdk-25.0.5+8/bin/java
CP="$D/harness:$D/candidate-lmdb-classes:$(cat experiments/overlay/lifetime/classpath.txt)"
exec 9>experiments/overlay/native-benchmark.lock
flock 9
for round in 0 1; do
  arms=(initial:adaptive always:adaptive always:fixed4096 initial:fixed4096)
  if [[ $round == 1 ]]; then arms=(initial:fixed4096 always:fixed4096 always:adaptive initial:adaptive); fi
  for trace in early-hot late-hot hot-unique; do
    for arm in "${arms[@]}"; do
      admission=${arm%:*}; capacity=${arm#*:}
      "$JAVA" -Xms1g -Xmx1g -Drdf4j.lmdb.nativeValueMemo.admission="$admission" -Drdf4j.lmdb.nativeValueMemo.capacity="$capacity" -cp "$CP" org.eclipse.rdf4j.sail.lmdb.evaluation.MemoPhaseProbe "$trace" > "$D/phase-${round}-${trace}-${admission}-${capacity}.log" 2>&1
    done
  done
done
