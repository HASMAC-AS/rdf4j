#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd -- "${SCRIPT_DIR}/../.." && pwd)"
MODULE="core/sail/lmdb"
JAR_PATH="${REPO_ROOT}/${MODULE}/target/jmh-benchmarks.jar"

cd "${REPO_ROOT}"
mvn -o -Dmaven.repo.local=.m2_repo -pl "${MODULE}" -P benchmarks,quick -DskipTests clean package

set +e
OUTPUT="$(java -jar "${JAR_PATH}" -l 2>&1)"
STATUS=$?
set -e

echo "${OUTPUT}"

if [[ ${STATUS} -ne 0 ]]; then
        exit ${STATUS}
fi

if [[ "${OUTPUT}" != *"ThemeQueryBenchmark.executeQuery"* ]]; then
        echo "Expected LMDB benchmark jar to list ThemeQueryBenchmark.executeQuery" >&2
        exit 1
fi

exit 0
