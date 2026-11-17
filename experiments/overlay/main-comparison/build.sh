#!/bin/bash
set -euo pipefail
ROOT=$(cd "$(dirname "$0")/.." && pwd)
export JAVA_HOME="$ROOT/tools/jdk-25.0.5+8"
export PATH="$JAVA_HOME/bin:$ROOT/tools/apache-maven-3.9.11/bin:$PATH"
export MAVEN_OPTS=-Xmx2g
cd "$ROOT/rdf4j-main-baseline"
exec 9>../native-benchmark.lock; flock 9
mvn -B -ntp -o -Dmaven.repo.local=.m2_repo -pl core/sail/lmdb -am -Pquick -DskipTests -Dcheckstyle.skip -Dspotless.skip=true -Dformatter.skip=true clean install > ../main-comparison/maven-build.log 2>&1
