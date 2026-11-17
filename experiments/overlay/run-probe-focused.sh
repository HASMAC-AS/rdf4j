#!/bin/bash
set -euo pipefail
exec 9>../native-benchmark.lock; flock 9
JAVAC=../tools/jdk-25.0.5+8/bin/javac
CP="core/sail/lmdb/target/classes:$(cat experiments/overlay/classpath.txt)"
$JAVAC -cp "$CP" -d core/sail/lmdb/target/classes core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/NativeExpressionDomain.java
$JAVAC -cp "$CP" -d core/sail/lmdb/target/test-classes core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/evaluation/NativeMembership{BulkIo,ProbeReuse}Test.java
experiments/overlay/mvn.sh -o -pl core/sail/lmdb surefire:test -Dtest=NativeMembershipProbeReuseTest,NativeMembershipBulkIoTest,NativeExpressionDomainFailureTest,NativeRowTermDomainExecutionTest -Drdf4j.lmdb.expressionDomain.bulkIdRecordIo="${2:-true}" -Drdf4j.lmdb.expressionDomain.reuseMembershipMissSlot="${1:-true}"
