#!/usr/bin/env bash
set -euo pipefail

usage() {
        cat <<USAGE
Usage: $0 --module <modulePath> --class <fullyQualifiedClass> --method <methodName> [options]

Options:
  --dry-run                         Print the Maven and JMH commands without executing them
  --warmup-iterations <number>      Number of warmup iterations (default: 1)
  --measurement-iterations <number> Number of measurement iterations (default: 3)
  --forks <number>                  Number of forks (default: 1)
  --jvm-arg <value>                 Append a JVM argument (can be repeated)
  --jmh-arg <value>                 Append a raw JMH argument (can be repeated)
  --param <name=value>              Lock a JMH @Param to a single value (can be repeated)
  --enable-jfr                      Enable JFR profiling with fixed iteration and timing settings
  --jfr-mode <cpu|alloc>            Choose CPU-only (1ms sampling) or allocation+lock profiling (10ms)
  --enable-jfr-cpu-times            Force CPU time JFR options (requires --enable-jfr)
  --jfr-output <path>               Override the destination file for the JFR recording
  --java-cmd <path>                 Java executable to use (default: java)
  --                                Treat the remaining arguments as raw JMH arguments
USAGE
}

SCRIPT_DIR="$(cd -- "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd -- "${SCRIPT_DIR}/.." && pwd)"
JAVA_CMD="${JAVA_CMD:-java}"

module=""
benchmark_class=""
benchmark_method=""
dry_run=false
warmup_iterations=1
measurement_iterations=3
forks=1
jmh_extra_args=()
jvm_args=()
measurement_time=""
enable_jfr=false
enable_jfr_cpu_times=false
jfr_output=""
warmup_overridden=false
measurement_overridden=false
forks_overridden=false
jfr_notice=""
jfr_profile_mode="cpu"
param_overrides=()
jfr_settings_file=""
benchmark_source_file=""
param_annotation_count=0

while [[ $# -gt 0 ]]; do
        case "$1" in
        --module|-m)
                module="$2"
                shift 2
                ;;
        --class|-c)
                benchmark_class="$2"
                shift 2
                ;;
        --method|-b|--benchmark)
                benchmark_method="$2"
                shift 2
                ;;
        --warmup-iterations)
                warmup_iterations="$2"
                warmup_overridden=true
                shift 2
                ;;
        --measurement-iterations)
                measurement_iterations="$2"
                measurement_overridden=true
                shift 2
                ;;
        --forks)
                forks="$2"
                forks_overridden=true
                shift 2
                ;;
        --jvm-arg)
                jvm_args+=("$2")
                shift 2
                ;;
        --jmh-arg)
                jmh_extra_args+=("$2")
                shift 2
                ;;
        --param)
                param_overrides+=("$2")
                shift 2
                ;;
        --enable-jfr)
                enable_jfr=true
                shift
                ;;
        --jfr-mode)
                jfr_profile_mode="$2"
                shift 2
                ;;
        --enable-jfr-cpu-times)
                enable_jfr_cpu_times=true
                shift
                ;;
        --jfr-output)
                jfr_output="$2"
                shift 2
                ;;
        --java-cmd)
                JAVA_CMD="$2"
                shift 2
                ;;
        --dry-run)
                dry_run=true
                shift
                ;;
        --help|-h)
                usage
                exit 0
                ;;
        --)
                shift
                while [[ $# -gt 0 ]]; do
                        jmh_extra_args+=("$1")
                        shift
                done
                ;;
        *)
                echo "Unknown option: $1" >&2
                usage >&2
                exit 1
                ;;
        esac
done

if [[ -z "${module}" || -z "${benchmark_class}" || -z "${benchmark_method}" ]]; then
        echo "Error: --module, --class, and --method are required." >&2
        usage >&2
        exit 1
fi

if [[ "${jfr_profile_mode}" != "cpu" && "${jfr_profile_mode}" != "alloc" ]]; then
        echo "Error: --jfr-mode must be 'cpu' or 'alloc'." >&2
        exit 1
fi

module_dir="${REPO_ROOT}/${module}"
if [[ ! -d "${module_dir}" ]]; then
        echo "Error: Module directory '${module}' does not exist." >&2
        exit 1
fi

if ${enable_jfr_cpu_times} && ! ${enable_jfr}; then
        echo "Error: --enable-jfr-cpu-times requires --enable-jfr." >&2
        exit 1
fi

locate_benchmark_source() {
        local module_path="$1"
        local class_name="$2"
        local relative_path candidate
        relative_path="${class_name//./\/}.java"
        candidate="$(find "${module_path}/src" -type f -path "*/${relative_path}" | head -n1 || true)"
        if [[ -z "${candidate}" ]]; then
                echo "Error: Unable to locate source file for '${class_name}' under '${module_path}/src'." >&2
                exit 1
        fi
        printf '%s\n' "${candidate}"
}

count_param_annotations() {
        local source_file="$1"
        local count
        count="$( (grep -Eo "[[:space:]]@Param[[:space:]]*\\(" "${source_file}" || true) | wc -l | tr -d ' ' )"
        printf '%s\n' "${count}"
}

validate_param_overrides() {
        if ${enable_jfr} && (( ${#param_overrides[@]} == 0 )); then
                echo "Error: --enable-jfr requires at least one --param override to lock benchmark parameters." >&2
                exit 1
        fi

        if (( param_annotation_count > 0 )) && { ${enable_jfr} || (( ${#param_overrides[@]} > 0 )); }; then
                if (( ${#param_overrides[@]} < param_annotation_count )); then
                        echo "Error: Benchmark '${benchmark_class}' declares ${param_annotation_count} @Param values; provide at least ${param_annotation_count} --param overrides." >&2
                        exit 1
                fi
        fi
}

benchmark_source_file="$(locate_benchmark_source "${module_dir}" "${benchmark_class}")"
param_annotation_count="$(count_param_annotations "${benchmark_source_file}")"
validate_param_overrides

escape_regex() {
        local input="$1"
        input="${input//\\/\\\\}"
        input="${input//./\\.}"
        input="${input//+/\\+}"
        input="${input//\*/\\*}"
        input="${input//\?/\\?}"
        input="${input//^/\\^}"
        input="${input//\$/\\$}"
        input="${input//\[/\\[}"
        input="${input//\]/\\]}"
        input="${input//|/\\|}"
        input="${input//(/\\(}"
        input="${input//)/\\)}"
        printf '%s' "${input}"
}

detect_java_major_version() {
        local version_output version
        if ! version_output="$(${JAVA_CMD} -version 2>&1 | head -n1)"; then
                printf '0\n'
                return
        fi
        version="${version_output%%\"*}"
        version="${version_output#*\"}"
        version="${version%%\"*}"
        printf '%s\n' "${version%%.*}"
}

write_jfr_settings() {
        local mode="$1"
        local destination="$2"
        mkdir -p "$(dirname "${destination}")"
        if [[ "${mode}" == "cpu" ]]; then
                cat >"${destination}" <<'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<configuration version="2.0" label="RDF4J CPU Profiling" description="High frequency CPU sampling without allocation events" provider="Eclipse RDF4J">
    <event name="jdk.ExecutionSample">
      <setting name="enabled">true</setting>
      <setting name="period">1 ms</setting>
      <setting name="stackTraceDepth">256</setting>
    </event>
    <event name="jdk.NativeMethodSample">
      <setting name="enabled">true</setting>
      <setting name="period">1 ms</setting>
      <setting name="stackTraceDepth">256</setting>
    </event>
    <event name="jdk.ObjectAllocationInNewTLAB">
      <setting name="enabled">false</setting>
    </event>
    <event name="jdk.ObjectAllocationOutsideTLAB">
      <setting name="enabled">false</setting>
    </event>
    <event name="jdk.ObjectAllocationSample">
      <setting name="enabled">false</setting>
    </event>
    <event name="jdk.JavaMonitorEnter">
      <setting name="enabled">false</setting>
    </event>
    <event name="jdk.ThreadPark">
      <setting name="enabled">false</setting>
    </event>
</configuration>
EOF
        else
                cat >"${destination}" <<'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<configuration version="2.0" label="RDF4J Allocation Profiling" description="Allocation and lock profiling with moderate sampling" provider="Eclipse RDF4J">
    <event name="jdk.ExecutionSample">
      <setting name="enabled">true</setting>
      <setting name="period">10 ms</setting>
      <setting name="stackTraceDepth">256</setting>
    </event>
    <event name="jdk.NativeMethodSample">
      <setting name="enabled">true</setting>
      <setting name="period">10 ms</setting>
      <setting name="stackTraceDepth">256</setting>
    </event>
    <event name="jdk.ObjectAllocationInNewTLAB">
      <setting name="enabled">true</setting>
      <setting name="stackTrace">true</setting>
    </event>
    <event name="jdk.ObjectAllocationOutsideTLAB">
      <setting name="enabled">true</setting>
      <setting name="stackTrace">true</setting>
    </event>
    <event name="jdk.ObjectAllocationSample">
      <setting name="enabled">true</setting>
      <setting name="throttle">500/s</setting>
      <setting name="stackTrace">true</setting>
    </event>
    <event name="jdk.JavaMonitorEnter">
      <setting name="enabled">true</setting>
      <setting name="threshold">0 ms</setting>
      <setting name="stackTrace">true</setting>
    </event>
    <event name="jdk.ThreadPark">
      <setting name="enabled">true</setting>
      <setting name="stackTrace">true</setting>
    </event>
</configuration>
EOF
        fi
}

configure_jfr_profile() {
        if (( ${#jmh_extra_args[@]} > 0 )); then
                echo "Error: --enable-jfr cannot be combined with additional JMH arguments." >&2
                exit 1
        fi

        if (( ${#param_overrides[@]} == 0 )); then
                echo "Error: --enable-jfr requires at least one --param override to lock benchmark parameters." >&2
                exit 1
        fi

        if ${warmup_overridden} && [[ "${warmup_iterations}" != "0" ]]; then
                echo "Error: --enable-jfr requires 0 warmup iterations." >&2
                exit 1
        fi

        if ${measurement_overridden} && [[ "${measurement_iterations}" != "3" ]]; then
                echo "Error: --enable-jfr requires 3 measurement iterations." >&2
                exit 1
        fi

        if ${forks_overridden} && [[ "${forks}" != "1" ]]; then
                echo "Error: --enable-jfr requires a single fork." >&2
                exit 1
        fi

        warmup_iterations=0
        measurement_iterations=3
        measurement_time="10s"
        forks=1

        if [[ -z "${jfr_output}" ]]; then
            local_class="${benchmark_class##*.}"
            sanitized_class="${local_class//[^A-Za-z0-9_]/_}"
            sanitized_method="${benchmark_method//[^A-Za-z0-9_]/_}"
            jfr_output="${module_dir}/target/${sanitized_class}.${sanitized_method}.jfr"
        elif [[ "${jfr_output}" != /* ]]; then
            jfr_output="${REPO_ROOT}/${jfr_output}"
        fi

        jfr_settings_file="${module_dir}/target/jfr-${jfr_profile_mode}-settings.jfc"
        write_jfr_settings "${jfr_profile_mode}" "${jfr_settings_file}"

        local fr_options="stackdepth=256"
        local detected_java_version
        detected_java_version="$(detect_java_major_version)"
        if ${enable_jfr_cpu_times} || { [[ "$(uname -s)" == "Linux" ]] && [[ "${detected_java_version}" =~ ^[0-9]+$ ]] && (( detected_java_version >= 25 )); }; then
                fr_options+="\,enableThreadCpuTime=true\,enableProcessCpuTime=true"
        fi

        jvm_args+=("-XX:StartFlightRecording=settings=${jfr_settings_file},dumponexit=true,filename=${jfr_output},disk=true")
        jvm_args+=("-XX:FlightRecorderOptions=${fr_options}")

        jfr_notice="JFR profiling enabled: enforcing warmup=0, measurement=3 iterations of 10s, forks=1. Recording will be written to ${jfr_output}. Settings: ${jfr_settings_file}."
}

if ${enable_jfr}; then
        configure_jfr_profile
fi

mvn_cmd=(mvn "-pl" "${module}" "-am" "-P" "benchmarks" "-DskipTests" package)

escaped_class="$(escape_regex "${benchmark_class}")"
escaped_method="$(escape_regex "${benchmark_method}")"
benchmark_pattern="^${escaped_class}\\.${escaped_method}$"

jmh_args=(-wi "${warmup_iterations}" -i "${measurement_iterations}" -f "${forks}")
if [[ -n "${measurement_time}" ]]; then
        jmh_args+=(-r "${measurement_time}")
fi
for arg in "${jvm_args[@]}"; do
        jmh_args+=("-jvmArgsAppend" "${arg}")
done
for param in "${param_overrides[@]}"; do
        jmh_args+=(-p "${param}")
done
for arg in "${jmh_extra_args[@]}"; do
        jmh_args+=("${arg}")
done

find_benchmark_jar() {
        local module_path="$1"
        local require_existing="$2"
        local target_dir="${module_path}/target"
        mapfile -t candidates < <(find "${target_dir}" -maxdepth 2 -type f \( -name '*jmh*.jar' -o -name '*benchmark*.jar' \) 2>/dev/null | sort)
        if [[ ${#candidates[@]} -gt 0 ]]; then
                for jar in "${candidates[@]}"; do
                        if [[ "$(basename "${jar}")" != original-* ]]; then
                                printf '%s\n' "${jar}"
                                return 0
                        fi
                done
                printf '%s\n' "${candidates[0]}"
                return 0
        fi

        if [[ "${require_existing}" == "true" ]]; then
                echo "Error: Unable to locate a benchmark jar in '${target_dir}'." >&2
                exit 1
        fi

        printf '%s\n' "${module_path}/target/jmh.jar"
}

run_jfr_postprocessing() {
        local recording="$1"
        if [[ ! -f "${recording}" ]]; then
                echo "JFR recording '${recording}' not found; skipping post-processing." >&2
                return
        fi

        if ! command -v jfr >/dev/null 2>&1; then
                echo "The 'jfr' tool is not available on PATH; skipping JFR post-processing." >&2
                return
        fi

        local base_dir base_name summary hot_methods exec_samples alloc_events lock_events cpu_load flamegraph
        base_dir="$(dirname "${recording}")"
        base_name="$(basename "${recording}" .jfr)"
        summary="${base_dir}/${base_name}-summary.txt"
        hot_methods="${base_dir}/${base_name}-hot-methods.txt"
        exec_samples="${base_dir}/${base_name}-execution-samples.txt"
        alloc_events="${base_dir}/${base_name}-alloc-events.txt"
        lock_events="${base_dir}/${base_name}-lock-events.txt"
        cpu_load="${base_dir}/${base_name}-cpu-load.txt"
        flamegraph="${base_dir}/${base_name}-flamegraph.txt"

        echo "Post-processing JFR recording at ${recording}" >&2

        jfr summary "${recording}" >"${summary}"
        jfr view hot-methods "${recording}" >"${hot_methods}" || true
        jfr print --events "jdk.ExecutionSample" --stack-depth 64 "${recording}" >"${exec_samples}"
        jfr print --events "jdk.ObjectAllocationInNewTLAB,jdk.ObjectAllocationOutsideTLAB,jdk.ObjectAllocationSample" --stack-depth 64 "${recording}" >"${alloc_events}"
        jfr print --events "jdk.JavaMonitorEnter,jdk.ThreadPark" --stack-depth 64 "${recording}" >"${lock_events}" || true
        jfr print --events "jdk.CPULoad,jdk.ThreadCPULoad" "${recording}" >"${cpu_load}" || true
        jfr view flamegraph "${recording}" >"${flamegraph}" || true

        echo "Generated JFR reports:" >&2
        printf '  %s\n' "${summary}" "${hot_methods}" "${exec_samples}" "${alloc_events}" "${lock_events}" "${cpu_load}" "${flamegraph}" >&2
}

print_command() {
        printf '%q ' "$@"
        printf '\n'
}

if ${dry_run}; then
        if ${enable_jfr}; then
                echo "${jfr_notice}"
        fi
        jar_path="$(find_benchmark_jar "${module_dir}" false)"
        print_command "${mvn_cmd[@]}"
        java_cmd=("${JAVA_CMD}" -jar "${jar_path}" "${jmh_args[@]}" "${benchmark_pattern}")
        print_command "${java_cmd[@]}"
        exit 0
fi

(
        cd "${REPO_ROOT}"
        "${mvn_cmd[@]}"
)

jar_path="$(find_benchmark_jar "${module_dir}" true)"
java_cmd=("${JAVA_CMD}" -jar "${jar_path}" "${jmh_args[@]}" "${benchmark_pattern}")

if ${enable_jfr}; then
        echo "${jfr_notice}"
        mkdir -p "$(dirname "${jfr_output}")"
fi

printf 'Running benchmark with jar %s\n' "${jar_path}"
"${java_cmd[@]}"

if ${enable_jfr}; then
        run_jfr_postprocessing "${jfr_output}"
fi
