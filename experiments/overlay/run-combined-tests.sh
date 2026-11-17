#!/bin/bash
set -euo pipefail
exec 9>../native-benchmark.lock;flock 9
mkdir -p core/sail/lmdb/target/tmp/fork-1
experiments/overlay/mvn.sh -pl core/sail/lmdb -Dtest=RepositoryValueIterationTest,RepositoryPartialReadyTest,LmdbSnapshotIdOverlayTest,LmdbNativeRowDetachmentTest,NativeResultReplayTest,NativeResultReplayStoreTermTest,NativeValueMemoTest,NativePositionConsumerTest,LmdbNativeRuntimeInternerLifetimeTest,NativeLazyResultReplayTest,WideNativeDistinctTrackerTest -DforkCount=1 -Drdf4j.lmdb.test.maxHeap=2G -Drdf4j.lmdb.nativeValueMemo.enabled=true -Drdf4j.lmdb.repository.partialReady=true -Drdf4j.lmdb.nativeDistinct.widePrimitive=true surefire:test > experiments/overlay/memo-combined-regression.log 2>&1
