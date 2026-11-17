#!/bin/bash
# Execute only after the coordinator grants an uncontended validation window.
set -euo pipefail
ROOT=$(cd "$(dirname "$0")/../../.." && pwd)
cd "$ROOT"
D=experiments/overlay/memo-policy
selection='LmdbMapGrowthReplayLifecycleTest,LmdbReadAttemptCancellationTest,RepositoryValueIterationTest,RepositoryPartialReadyTest,LmdbRepositoryValueExposureTest,LmdbExternalValueBoundaryTest,ValueResolutionScopeActivationTest,LmdbValueMaterializingCancellationTest,LmdbReplayExposureContractTest,LmdbStandaloneQueryReplayTest,NativeLazyResultReplayTest,LmdbNativeReplayWorkerContextTest'
exec 9>experiments/overlay/native-benchmark.lock
flock 9
# No compilation; frozen candidate remains immutable. Verify target bytecode is the tested candidate.
cmp core/sail/lmdb/target/classes/org/eclipse/rdf4j/sail/lmdb/evaluation/NativeValueMemo.class "$D/candidate-lmdb-classes/org/eclipse/rdf4j/sail/lmdb/evaluation/NativeValueMemo.class"
for arm in initial:fixed4096 always:adaptive always:fixed4096; do
  admission=${arm%:*}; capacity=${arm#*:}; name="lifecycle-${admission}-${capacity}"
  mkdir -p "$D/$name-reports"
  set +e
  experiments/overlay/lifetime/mvn.sh -o -pl core/sail/lmdb -Dtest="$selection" \
    -Drdf4j.lmdb.nativeValueMemo.enabled=true \
    -Drdf4j.lmdb.nativeValueMemo.testProbe=true \
    -Drdf4j.lmdb.nativeValueMemo.admission="$admission" \
    -Drdf4j.lmdb.nativeValueMemo.capacity="$capacity" \
    -Drdf4j.lmdb.repository.partialReady=true \
    surefire:test > "$D/$name.log" 2>&1
  result=$?
  set -e
  for class in ${selection//,/ }; do
    cp "core/sail/lmdb/target/surefire-reports/org.eclipse.rdf4j.sail.lmdb.$class.txt" "$D/$name-reports/" 2>/dev/null || \
    cp "core/sail/lmdb/target/surefire-reports/org.eclipse.rdf4j.sail.lmdb.evaluation.$class.txt" "$D/$name-reports/"
  done
  if [[ $result != 0 ]]; then echo "Failed $arm; preserved reports; stopping."; exit "$result"; fi
 done
