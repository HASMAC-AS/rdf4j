#!/usr/bin/env bash
set -euo pipefail
ROOT=$(cd "$(dirname "$0")/../.." && pwd)
cd "$ROOT"
exec 9>experiments/overlay/native-benchmark.lock
flock 9
exec experiments/threadlocal-audit/mvn.sh -pl core/sail/lmdb -Dtest=NativeCancellationCheckpointTest,LmdbNativeTypeMatrixCancellationTest,LmdbNativeRowCancellationTest,LmdbNativeOwnedWorkerCancellationRegressionTest,NativeContextClosingIterationCancellationTest,NativeKernelExpansionCancellationTest,LmdbNativeCoordinatorCancellationTest,LmdbRecordIteratorFilteredKeyCancellationTest,LmdbShutdownCancellationTest,LmdbReplayExposureContractTest test
