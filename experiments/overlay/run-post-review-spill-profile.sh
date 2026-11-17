#!/bin/bash
set -euo pipefail
exec 9>../native-benchmark.lock; flock 9
JAVA=../tools/jdk-25.0.5+8/bin/java
"$JAVA" --enable-native-access=ALL-UNNAMED -Xms2g -Xmx5g -XX:StartFlightRecording=filename=experiments/overlay/post-review-spill.jfr,settings=experiments/overlay/spill-profile-4ms.jfc,dumponexit=true -Drdf4j.lmdb.nativeValueMemo.enabled=true -Drdf4j.lmdb.repository.partialReady=true -Drdf4j.lmdb.nativeDistinct.widePrimitive=true -Drdf4j.lmdb.expressionDomain.bulkIdRecordIo=true -Drdf4j.lmdb.expressionDomain.reuseMembershipMissSlot=true -Doverlay.benchmark.expectedChecksum=-3441391656945048864 -Doverlay.benchmark.fixture=../fixture-eight-million -Doverlay.benchmark.warmups=0 -cp "experiments/overlay/review-abba-test-classes:experiments/overlay/review-post-classes:$(cat experiments/overlay/classpath.txt)" org.eclipse.rdf4j.sail.lmdb.evaluation.OverlayOptionalQueryBenchmark 8000 10 1 true > experiments/overlay/post-review-spill-profile.log 2>&1
../tools/jdk-25.0.5+8/bin/jfr print --json --events jdk.ExecutionSample experiments/overlay/post-review-spill.jfr > experiments/overlay/post-review-spill-samples.json
../tools/jdk-25.0.5+8/bin/jfr print --json --events jdk.NativeMethodSample experiments/overlay/post-review-spill.jfr > experiments/overlay/post-review-spill-native-samples.json
