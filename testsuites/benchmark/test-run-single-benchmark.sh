#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd -- "${SCRIPT_DIR}/../.." && pwd)"
SCRIPT="${REPO_ROOT}/scripts/run-single-benchmark.sh"

set +e
OUTPUT="$(bash "${SCRIPT}" --dry-run --module testsuites/benchmark --class org.eclipse.rdf4j.benchmark.ReasoningBenchmark --method forwardChainingSchemaCachingRDFSInferencer 2>&1)"
STATUS=$?
set -e

echo "${OUTPUT}"

if [[ ${STATUS} -ne 0 ]]; then
        exit ${STATUS}
fi

if [[ "${OUTPUT}" != *"mvn -pl testsuites/benchmark -am -P benchmarks -DskipTests package"* ]]; then
        echo "Expected Maven command not found in output" >&2
        exit 1
fi

if [[ "${OUTPUT}" != *"ReasoningBenchmark"*"forwardChainingSchemaCachingRDFSInferencer"* ]]; then
        echo "Expected benchmark method not found in output" >&2
        exit 1
fi

set +e
JFR_OUTPUT="$(bash "${SCRIPT}" --dry-run --module testsuites/benchmark --class org.eclipse.rdf4j.benchmark.ReasoningBenchmark --method forwardChainingSchemaCachingRDFSInferencer --enable-jfr --param dataset=cache 2>&1)"
JFR_STATUS=$?
set -e

echo "${JFR_OUTPUT}"

if [[ ${JFR_STATUS} -ne 0 ]]; then
        exit ${JFR_STATUS}
fi

if [[ "${JFR_OUTPUT}" != *"JFR profiling enabled:"* ]]; then
        echo "Expected JFR guidance banner when profiling is enabled" >&2
        exit 1
fi

EXPECTED_JFR_PATH="testsuites/benchmark/target/ReasoningBenchmark.forwardChainingSchemaCachingRDFSInferencer.jfr"
if [[ "${JFR_OUTPUT}" != *"${EXPECTED_JFR_PATH}"* ]]; then
        echo "Expected JFR banner to include the recording destination" >&2
        exit 1
fi

if [[ "${JFR_OUTPUT}" != *"-wi 0"* ]]; then
        echo "Expected JFR run to disable warmup iterations" >&2
        exit 1
fi

if [[ "${JFR_OUTPUT}" != *"-i 3"* ]]; then
        echo "Expected JFR run to force 3 measurement iterations" >&2
        exit 1
fi

if [[ "${JFR_OUTPUT}" != *"-r 10s"* ]]; then
        echo "Expected JFR run to set measurement time to 10 seconds" >&2
        exit 1
fi

if [[ "${JFR_OUTPUT}" != *"-f 1"* ]]; then
        echo "Expected JFR run to enforce a single fork" >&2
        exit 1
fi

if [[ "${JFR_OUTPUT}" != *"-p dataset=cache"* ]]; then
        echo "Expected JFR run to lock parameters to a single value" >&2
        exit 1
fi

if [[ "${JFR_OUTPUT}" != *"jfr-cpu-settings.jfc"* ]]; then
        echo "Expected JFR run to enable JFR profiling" >&2
        exit 1
fi

if [[ "${JFR_OUTPUT}" != *"-XX:FlightRecorderOptions=stackdepth=256"* ]]; then
        echo "Expected JFR run to increase stack depth for profiling" >&2
        exit 1
fi

if [[ "${JFR_OUTPUT}" != *"testsuites/benchmark/target/ReasoningBenchmark.forwardChainingSchemaCachingRDFSInferencer.jfr"* ]]; then
        echo "Expected JFR run to emit recording into the module target directory" >&2
        exit 1
fi

set +e
PARAM_OUTPUT="$(bash "${SCRIPT}" --dry-run --module testsuites/benchmark --class org.eclipse.rdf4j.benchmark.ReasoningBenchmark --method forwardChainingSchemaCachingRDFSInferencer --param testParam=value 2>&1)"
PARAM_STATUS=$?
set -e

echo "${PARAM_OUTPUT}" 

if [[ ${PARAM_STATUS} -ne 0 ]]; then
        exit ${PARAM_STATUS}
fi

if [[ "${PARAM_OUTPUT}" != *"-p testParam=value"* ]]; then
        echo "Expected param override to be passed to JMH" >&2
        exit 1
fi

set +e
JFR_CPU_OUTPUT="$(bash "${SCRIPT}" --dry-run --module testsuites/benchmark --class org.eclipse.rdf4j.benchmark.ReasoningBenchmark --method forwardChainingSchemaCachingRDFSInferencer --enable-jfr --enable-jfr-cpu-times --param dataset=cache 2>&1)"
JFR_CPU_STATUS=$?
set -e

echo "${JFR_CPU_OUTPUT}"

if [[ ${JFR_CPU_STATUS} -ne 0 ]]; then
        exit ${JFR_CPU_STATUS}
fi

if [[ "${JFR_CPU_OUTPUT}" != *"enableThreadCpuTime=true"* ]] || [[ "${JFR_CPU_OUTPUT}" != *"enableProcessCpuTime=true"* ]]; then
        echo "Expected CPU time options to be appended when requested" >&2
        exit 1
fi

set +e
INSUFFICIENT_PARAMS_OUTPUT="$(bash "${SCRIPT}" --dry-run --module testsuites/benchmark --class org.eclipse.rdf4j.benchmark.QueryOrderBenchmark --method selectAll --enable-jfr --param limit=10 2>&1)"
INSUFFICIENT_PARAMS_STATUS=$?
set -e

echo "${INSUFFICIENT_PARAMS_OUTPUT}"

if [[ ${INSUFFICIENT_PARAMS_STATUS} -eq 0 ]]; then
        echo "Expected insufficient param overrides to fail when profiling" >&2
        exit 1
fi

if [[ "${INSUFFICIENT_PARAMS_OUTPUT}" != *"declares 3 @Param"* ]]; then
        echo "Expected error to mention the benchmark's parameter count" >&2
        exit 1
fi

exit 0
