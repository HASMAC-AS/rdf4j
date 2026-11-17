#!/bin/bash
set -euo pipefail
cd "$(dirname "$0")/../../.."
exec 8>../.rdf4j-heavy-jvm.lock; flock 8
cp maven-build.log experiments/overlay/memo-hot-split/prior-maven-build.log
bash experiments/overlay/lifetime/mvn.sh -o -T 1C -Pquick install > maven-build.log 2>&1
cp maven-build.log experiments/overlay/memo-hot-split/prechange-install.log
bash experiments/overlay/lifetime/mvn.sh -o -pl core/sail/lmdb -Dtest=NativeValueMemoTest,NativeMemoPolicyTest,NativeMemoEntryMetadataTest -Drdf4j.lmdb.nativeValueMemo.testProbe=true -DforkCount=1 test > experiments/overlay/memo-hot-split/prechange-tests.log 2>&1
cat core/sail/lmdb/target/surefire-reports/*Native*Memo*.txt > experiments/overlay/memo-hot-split/prechange-reports.txt
