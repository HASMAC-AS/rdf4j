#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd -- "${SCRIPT_DIR}/../.." && pwd)"
SCRIPT="${REPO_ROOT}/scripts/run-single-benchmark-docker.sh"

set +e
OUTPUT="$(RDF4J_JMH_DOCKER_IMAGE=maven:3.9.11-eclipse-temurin-25 bash "${SCRIPT}" --dry-run --module testsuites/benchmark --class org.eclipse.rdf4j.benchmark.ReasoningBenchmark --method forwardChainingSchemaCachingRDFSInferencer 2>&1)"
STATUS=$?
set -e

echo "${OUTPUT}"

if [[ ${STATUS} -ne 0 ]]; then
        exit ${STATUS}
fi

if [[ "${OUTPUT}" != *"docker run --rm"* ]]; then
        echo "Expected docker invocation in dry-run output" >&2
        exit 1
fi

if [[ "${OUTPUT}" != *"-v ${REPO_ROOT}:/workspace"* ]]; then
        echo "Expected repository mount in dry-run output" >&2
        exit 1
fi

if [[ "${OUTPUT}" != *"MAVEN_OPTS=-Dmaven.repo.local=/workspace/.m2_repo_linux_j25"* ]]; then
        echo "Expected Maven local repository override in dry-run output" >&2
        exit 1
fi

if [[ "${OUTPUT}" != *"MAVEN_CONFIG=/tmp/home/.m2"* ]]; then
        echo "Expected Maven config override in dry-run output" >&2
        exit 1
fi

if [[ "${OUTPUT}" != *"scripts/run-single-benchmark.sh"* ]]; then
        echo "Expected inner benchmark helper invocation in dry-run output" >&2
        exit 1
fi

if [[ "${OUTPUT}" != *"bash -c"* ]]; then
        echo "Expected wrapper to avoid login-shell execution" >&2
        exit 1
fi

if [[ "${OUTPUT}" != *"mkdir\\ -p\\ \\\"\\\$HOME\\\"\\ \\\"\\\$HOME/.m2\\\""* ]]; then
        echo "Expected wrapper to initialize HOME-backed Maven directories" >&2
        exit 1
fi

if [[ "${OUTPUT}" != *"--dry-run --module testsuites/benchmark"* ]]; then
        echo "Expected wrapper to pass through benchmark arguments" >&2
        exit 1
fi

if [[ "${OUTPUT}" != *"--enable-jfr"* || "${OUTPUT}" != *"--enable-jfr-cpu-times"* ]]; then
        echo "Expected wrapper to force JFR CPU time profiling" >&2
        exit 1
fi

set +e
SHORTHAND_OUTPUT="$(RDF4J_JMH_DOCKER_IMAGE=maven:3.9.11-eclipse-temurin-25 bash "${SCRIPT}" --dry-run org.eclipse.rdf4j.sail.lmdb.benchmark.ThemeQueryBenchmark.executeQuery 2>&1)"
SHORTHAND_STATUS=$?
set -e

echo "${SHORTHAND_OUTPUT}"

if [[ ${SHORTHAND_STATUS} -ne 0 ]]; then
        exit ${SHORTHAND_STATUS}
fi

if [[ "${SHORTHAND_OUTPUT}" != *"--module core/sail/lmdb"* ]]; then
        echo "Expected shorthand benchmark id to resolve the LMDB module" >&2
        exit 1
fi

if [[ "${SHORTHAND_OUTPUT}" != *"--class org.eclipse.rdf4j.sail.lmdb.benchmark.ThemeQueryBenchmark"* ]]; then
        echo "Expected shorthand benchmark id to resolve the benchmark class" >&2
        exit 1
fi

if [[ "${SHORTHAND_OUTPUT}" != *"--method executeQuery"* ]]; then
        echo "Expected shorthand benchmark id to resolve the benchmark method" >&2
        exit 1
fi

assert_flexible_params() {
        local description="$1"
        shift
        local flexible_output
        local flexible_status

        set +e
        flexible_output="$(RDF4J_JMH_DOCKER_IMAGE=maven:3.9.11-eclipse-temurin-25 bash "${SCRIPT}" --dry-run org.eclipse.rdf4j.sail.lmdb.benchmark.ThemeQueryBenchmark.executeQuery "$@" 2>&1)"
        flexible_status=$?
        set -e

        echo "${flexible_output}"

        if [[ ${flexible_status} -ne 0 ]]; then
                echo "Expected flexible parser case to succeed: ${description}" >&2
                exit ${flexible_status}
        fi

        if [[ "${flexible_output}" != *"--param themeName=MEDICAL_RECORDS"* ]]; then
                echo "Expected themeName param for case: ${description}" >&2
                exit 1
        fi

        if [[ "${flexible_output}" != *"--param z_queryIndex=0"* ]]; then
                echo "Expected z_queryIndex param for case: ${description}" >&2
                exit 1
        fi
}

assert_flexible_params "equals with punctuation" themeName = MEDICAL_RECORDS, z_queryIndex = 0
assert_flexible_params "colon form" themeName:MEDICAL_RECORDS z_queryIndex:0
assert_flexible_params "name value pairs" themeName MEDICAL_RECORDS z_queryIndex 0
assert_flexible_params "positional value order" MEDICAL_RECORDS 0
assert_flexible_params "reverse positional order" 0 MEDICAL_RECORDS

exit 0
