#!/bin/bash
set -euo pipefail
exec 9>../native-benchmark.lock; flock 9
JAVA=../tools/jdk-25.0.5+8/bin/java
"$JAVA" --enable-native-access=ALL-UNNAMED -Xms2g -Xmx5g -XX:StartFlightRecording=filename=experiments/overlay/spill-default-eight-million.jfr,settings=experiments/overlay/spill-profile-4ms.jfc,dumponexit=true -Drdf4j.lmdb.nativeValueMemo.enabled=true -Drdf4j.lmdb.repository.partialReady=true -Drdf4j.lmdb.nativeDistinct.widePrimitive=true -Doverlay.benchmark.fixture=../fixture-eight-million -Doverlay.benchmark.warmups=0 -cp "core/sail/lmdb/target/test-classes:experiments/overlay/budgeted-wide-final-classes:$(cat experiments/overlay/classpath.txt)" org.eclipse.rdf4j.sail.lmdb.evaluation.OverlayOptionalQueryBenchmark 8000 10 1 true > experiments/overlay/spill-default-eight-million-profile.log 2>&1
../tools/jdk-25.0.5+8/bin/jfr print --json --events jdk.ExecutionSample experiments/overlay/spill-default-eight-million.jfr > experiments/overlay/spill-default-eight-million-samples.json
../tools/jdk-25.0.5+8/bin/jfr print --json --events jdk.NativeMethodSample experiments/overlay/spill-default-eight-million.jfr > experiments/overlay/spill-default-eight-million-native-samples.json
