#!/bin/bash
set -euo pipefail
exec 9>../native-benchmark.lock; flock 9
experiments/overlay/mvn.sh -o -pl core/sail/lmdb -DforkCount=1 -Drdf4j.lmdb.test.maxHeap=2G -Drdf4j.lmdb.nativeValueMemo.enabled=true -Drdf4j.lmdb.nativeValueMemo.testProbe=true -Drdf4j.lmdb.nativeLazyResults.enabled=false -Drdf4j.lmdb.nativeQueryEngine.enabled=true -Drdf4j.lmdb.janinoCodegen.enabled=true -Drdf4j.lmdb.janinoCodegen.synchronous=true -Drdf4j.lmdb.janinoCodegen.thresholdRows=0 -Drdf4j.lmdb.kernelInterpreter.enabled=false -Dtest=LmdbMapGrowthReplayLifecycleTest#compiledGroupedDistinctIsRecheckedAcrossMapGrowth surefire:test > experiments/overlay/grouping-adjacency-replay-final.log 2>&1
