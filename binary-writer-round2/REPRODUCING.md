# Reproducing the binary writer measurements

Run these commands from a Git checkout containing the candidate production writer and the complete binary-writer-round2 directory. Use a matching RDF4J 6.2.0-SNAPSHOT source tree, JDK 25, Maven 3.9 or newer, Python 3, and the platform's LWJGL/LMDB native libraries. The commands below use Bash; the recorded environment was Linux x86-64.

The harness compares:

- **published:** the standalone writer copied from commit 741b9389538e80214075692439a8cd25a8a9ae84, with its class and constructor renamed.
- **current:** the production BinaryQueryResultWriter.java currently present in this checkout, compiled directly by the harness.
- **baseline:** the separately supplied original upstream writer from 5434e51194e6c2e1aa745e5d65a4c998918d6073. This is available for historical comparisons, but is not the round-two comparison baseline.

Simple or fully qualified writer class names can also select experimental classes. Results from another revision of main are a new comparison; record that revision and do not present them as measurements of the pinned sources.

## 1. Select the toolchain and checkout-local dependency cache

```sh
export JAVA_HOME="/absolute/path/to/jdk-25"
export PATH="$JAVA_HOME/bin:$PATH"
export RDF4J_BENCH_ROOT="$(pwd -P)"
export RDF4J_BENCH_RUN="reproduce-$(date -u +%Y%m%dT%H%M%SZ)"

"$JAVA_HOME/bin/java" -version
mvn -version
git rev-parse HEAD
```

Replace the JDK path. Keep the same shell for subsequent commands. The timestamp prefix keeps local results separate from the supplied evidence. Run builds, tests, and timing measurements sequentially. Check the JVM startup messages for inherited Java options that could override the runner's heap or processor settings.

The runner currently requires artifacts beneath the checkout's .m2_repo directory, RDF4J version 6.2.0-SNAPSHOT, and JMH 1.37. Merely generating a classpath against a different Maven cache is insufficient: bench.py also locates three RDF4J jars and the JMH annotation processor at these checkout-local paths. The following commands consistently select that cache.

Follow the repository's [AGENTS.md](../AGENTS.md): start with a **full root clean install**, and repeat it before direct/manual test or verify runs. A module-only prerequisite install does not replace this initial step. Define this shell helper to retain the required flags and build-log filter:

```sh
set -o pipefail
rdf4j_clean_install() {
  mvn -B -ntp -Dmaven.compiler.showWarnings=false \
    -Dmaven.repo.local="$RDF4J_BENCH_ROOT/.m2_repo" "$@" \
    -Pquick clean install 2>&1 \
    | tee maven-build.log \
    | awk '
        /\[WARNING\]/ { next }
        /\[ERROR\]/ { print; next }
        /Reactor Summary/ { summary=1 }
        summary { print }
      '
}

rdf4j_clean_install -T 1C -o
```

Stop if the build fails. **Try offline first.** If and only if it fails because a dependency or plugin is missing, repeat the same command once without -o to fetch it, then return offline. For another build failure, AGENTS.md requires retrying without parallelism:

```sh
# Only when offline resolution reports a missing dependency or plugin:
rdf4j_clean_install -T 1C

# For another build failure, retry the offline build without -T 1C:
rdf4j_clean_install -o
```

These are conditional recovery commands, not two additional mandatory builds. Keep the complete maven-build.log; copy it to a uniquely named evidence file before another build if retaining each run. Allow at least 60 seconds when invoking this build through a tool with a timeout.

The root quick profile sets skipTests and skipITs, while retaining test compilation and test jars required by dependencies. Do not replace it with maven.test.skip=true: the LMDB dependency graph includes test artifacts. Use -o on subsequent Maven commands as shown. If a later command reports a missing dependency/plugin, rerun that command once without -o and then return offline; a test or compilation failure is not a reason to enable network resolution.

The configuration backing these instructions is in the [root POM](../pom.xml) and [LMDB POM](../core/sail/lmdb/pom.xml): Java 25, JMH 1.37, and dependency-plugin 3.11.0 are pinned; the LMDB module declares repository-sail, jmh-core, and jmh-generator-annprocess in test scope. Its runtime dependencies include the supported LWJGL native classifiers.

## 2. Regenerate the absolute classpath

The supplied dependency-classpath.txt contains paths from the recording machine. Regenerate it after copying or moving the checkout:

```sh
mvn -o -Dmaven.repo.local="$RDF4J_BENCH_ROOT/.m2_repo" \
  -pl core/sail/lmdb \
  org.apache.maven.plugins:maven-dependency-plugin:3.11.0:build-classpath \
  -DincludeScope=test \
  -Dmdep.regenerateFile=true \
  -Dmdep.outputFile="$RDF4J_BENCH_ROOT/binary-writer-round2/harness/dependency-classpath.txt"
```

The test scope is intentional: it supplies the repository query fixture and JMH classes. The absolute output path prevents Maven from placing the file under the selected module. The regenerateFile option forces replacement of a copied classpath even when it already exists. These parameter names are supported by the locally inspected 3.11.0 plugin descriptor.

The runner adds the matching binary-result, HTTP-client, and LMDB module jars itself, removes duplicate paths, and rejects missing files. Regeneration fixes path portability; it does not make incompatible snapshot jars interchangeable. Build them from the same checkout.

## 3. Compile and preserve source identity

```sh
python3 binary-writer-round2/harness/bench.py compile

cp binary-writer-round2/harness/compiled-source-identity.json \
  "binary-writer-round2/$RDF4J_BENCH_RUN-source-identity.json"
```

Compilation includes the supplied standalone writer sources, both benchmark classes, the profiler, and the actual production writer. JMH annotation processing creates the benchmark metadata. The resulting harness/classes directory precedes dependency jars at runtime, so the compiled production writer takes precedence over an installed copy.

The identity JSON records the checkout HEAD, Java version, full resolved classpath, both baseline revisions, and SHA-256 hashes of every compiled source. HEAD alone is insufficient when sources are modified. Verify that the files still match the recorded compilation:

```sh
python3 - <<'PY'
import hashlib
import json
from pathlib import Path

root = Path.cwd()
identity = json.loads(
    (root / "binary-writer-round2/harness/compiled-source-identity.json").read_text()
)
for name, expected in identity["sources"].items():
    actual = hashlib.sha256((root / name).read_bytes()).hexdigest()
    if actual != expected:
        raise SystemExit("Source changed since compilation: " + name)
missing = [name for name in identity["classpath"] if not Path(name).is_file()]
if missing:
    raise SystemExit("Missing classpath entries: " + repr(missing))
print("All compiled source hashes and classpath entries match.")
print("Checkout HEAD:", identity["checkout_head"])
print("Published baseline:", identity["published_baseline"])
PY
```

If both pinned commits are available in the local Git history, verify the standalone baseline copies too:

```sh
python3 - <<'PY'
from pathlib import Path
import subprocess

path = "core/queryresultio/binary/src/main/java/org/eclipse/rdf4j/query/resultio/binary/BinaryQueryResultWriter.java"
for commit, name in [
    ("741b9389538e80214075692439a8cd25a8a9ae84", "PublishedBinaryQueryResultWriter"),
    ("5434e51194e6c2e1aa745e5d65a4c998918d6073", "OriginalBinaryQueryResultWriter"),
]:
    expected = subprocess.check_output(["git", "show", commit + ":" + path], text=True)
    actual = Path("binary-writer-round2/harness/" + name + ".java").read_text()
    if actual.replace(name, "BinaryQueryResultWriter") != expected:
        raise SystemExit("Baseline source mismatch: " + name)
    print(name, "matches", commit, "after reversing the class rename.")
PY
```

Each subsequent compile replaces compiled-source-identity.json. Preserve a fresh copy alongside each result set when changing sources, and use a new run prefix. Keep the complete JSON results, logs, and command JSON files produced by the runner.

## 4. Validate complete output

Explicitly select published/current. The large benchmark's smoke main retains the historical baseline/current default, which is not the desired round-two selection.

```sh
BINARY_WRITER_SMOKE_NAME="$RDF4J_BENCH_RUN-smoke" \
python3 binary-writer-round2/harness/bench.py smoke \
  published,current \
  ascii,unicode,repeat_identity,repeat_equal_lmdb,unique_lmdb,wide,long,long_unicode,long_latin1,long_cjk,long_emoji,long_sparse,medium,medium_unicode,query \
  memory,heartbeat stable,churn

BINARY_WRITER_SMOKE_NAME="$RDF4J_BENCH_RUN-small-smoke" \
python3 binary-writer-round2/harness/bench.py small-smoke \
  published,current 0,1,8 memory,heartbeat
```

The large smoke run parses each complete document and compares every term. Real-query validation checks every expected subject/value combination exactly once while allowing query order to vary. The small smoke run additionally compares exact output bytes across implementations and sinks for each row count. Successful selections print roundtrip=PASS; small smoke marks the first byte sequence REFERENCE and later matching sequences PASS.

Run the binary module's verify phase and the two focused server tests separately, repeating the required root clean install before each direct/manual verify command:

```sh
rdf4j_clean_install -T 1C -o
mvn -o -Dmaven.repo.local="$RDF4J_BENCH_ROOT/.m2_repo" \
  -Pquick -DskipTests=false \
  -pl core/queryresultio/binary verify 2>&1 \
  | tee "binary-writer-round2/$RDF4J_BENCH_RUN-binary-verify.log"

python3 scripts/agent-evidence.py \
  --command "mvn -o -Dmaven.repo.local=$RDF4J_BENCH_ROOT/.m2_repo -Pquick -DskipTests=false -pl core/queryresultio/binary verify" \
  --log "binary-writer-round2/$RDF4J_BENCH_RUN-binary-verify.log" \
  core/queryresultio/binary/target/surefire-reports \
  > initial-evidence.txt
cp initial-evidence.txt "binary-writer-round2/$RDF4J_BENCH_RUN-initial-evidence.txt"
cp -R core/queryresultio/binary/target/surefire-reports \
  "binary-writer-round2/$RDF4J_BENCH_RUN-binary-surefire-reports"
```

Inspect the verify result before continuing. The evidence capture preserves the first report before the next root clean install removes target directories. Then run the focused server selection:

```sh
rdf4j_clean_install -T 1C -o
mvn -o -Dmaven.repo.local="$RDF4J_BENCH_ROOT/.m2_repo" \
  -Pquick -DskipTests=false -pl tools/server-spring \
  '-Dtest=TupleQueryResultViewTest#testRender_BinaryTupleTimeoutAfterRowsPreservesTypedTerminalError+testRender_BinaryCircuitBreakerAfterRowsPreservesItsReason' \
  verify 2>&1 \
  | tee "binary-writer-round2/$RDF4J_BENCH_RUN-server-verify.log"
cp -R tools/server-spring/target/surefire-reports \
  "binary-writer-round2/$RDF4J_BENCH_RUN-server-surefire-reports"
```

The explicit -DskipTests=false enables unit tests despite the quick profile; skipITs remains enabled. Neither verify command uses -am or -q. The selected server methods check a typed terminal timeout error and preservation of the circuit breaker's reason after rows have already been emitted. The [server POM](../tools/server-spring/pom.xml) resolves the writer through installed dependencies, which the preceding root install refreshes.

The commands preserve the verify logs, Surefire reports, and AGENTS.md's initial-evidence.txt. Preserve any target/failsafe-reports produced as well. Stop on build or test failure. If production or harness sources change during validation, recompile the harness and preserve a new source identity before timing.

## 5. Measure time and allocation without JFR

Run one command at a time on an otherwise idle machine, after builds and tests have finished. The runner uses one benchmark thread, fail-on-error, a 512 MiB initial and 1 GiB maximum heap, ActiveProcessorCount=2, and native access.

### Matched final comparisons

Use [run_final.py](harness/run_final.py) for the final sequential comparison. Its arguments are mode followed by an optional fresh output prefix:

```sh
python3 binary-writer-round2/harness/run_final.py heartbeat "$RDF4J_BENCH_RUN"

python3 binary-writer-round2/harness/run_final.py small "$RDF4J_BENCH_RUN"
```

Heartbeat mode runs each of these workloads in a separate JMH invocation, waiting for it to finish before starting the next:

ascii, unicode, repeat_identity, repeat_equal_lmdb, unique_lmdb, wide, medium, medium_unicode, long, long_unicode, long_latin1, long_cjk, long_emoji, long_sparse, query.

Each workload compares published/current with stable namespaces and the heartbeat sink. The requested implementation order alternates by workload: published,current first, current,published next, and so on. Both implementations receive **two forks, five one-second warmup iterations, five one-second measurement iterations, and the GC profiler**. Keep the actual execution order in the logs with the results.

Small mode applies the same fork/warmup/measurement settings to 0-, 1-, and 8-row documents with the **memory sink**, requesting current,published order. It reports per-document results. The small smoke command above checks both sink types; small final mode deliberately measures memory only.

The helper verifies compiled source hashes before execution and after each completed comparison, and saves a mode-specific source-identity snapshot. Supply a fresh prefix containing only letters, digits, underscores, or hyphens. It refuses an existing identity snapshot or result JSON rather than overwriting those results. The default prefix is final, so use your reproduction prefix to keep supplied evidence intact. After an interrupted run, choose a new prefix; retain the partial run separately.

Heartbeat outputs use names such as PREFIX-ascii.json; small mode writes PREFIX-small.json. Each also has its runner log and command JSON. Identity snapshots are named PREFIX-heartbeat-source-identity.json and PREFIX-small-source-identity.json.

### Additional controls and individual reruns

The underlying bench.py remains available when a specific control is needed. These direct commands do not provide run_final.py's existing-result checks, so keep their output names unique.

Memory-sink control, with the same two-fork settings:

```sh
python3 binary-writer-round2/harness/bench.py jmh "$RDF4J_BENCH_RUN-memory" -- \
  -p implementation=published,current -p namespaceLayout=stable -p sink=memory \
  -p workload=ascii,unicode,long,long_unicode \
  -wi 5 -w 1s -i 5 -r 1s -f 2 -prof gc
```

Optional small-document heartbeat control, distinct from the final memory result:

```sh
python3 binary-writer-round2/harness/bench.py small-jmh "$RDF4J_BENCH_RUN-small-heartbeat" -- \
  -p implementation=published,current -p rows=0,1,8 -p sink=heartbeat \
  -wi 5 -w 1s -i 5 -r 1s -f 2 -prof gc
```

| Harness | One invocation | Reported average time | gc.alloc.rate.norm |
| --- | --- | --- | --- |
| BinaryWriterBenchmark | One complete 4,096-row document | ns/row | B/row |
| SmallDocumentBenchmark | One complete 0-, 1-, or 8-row document | ns/document | B/document |

The large benchmark's OperationsPerInvocation(4096) already performs the normalization. Do not divide its results again. Small documents have no such annotation; even the zero-row case is one complete document. Both benchmarks include construction of a fresh writer, while their actual output buffers are reused and pre-grown.

The heartbeat sink uses the real synchronized payload wrapper but no heartbeat thread or network connection. The query workload measures query evaluation plus serialization. Large Unicode fixtures differ in byte size; compare implementations within the same workload. See [the methodology review](methodology-review.md) for exact payload sizes and scope. Preserve inconclusive results and inspect per-fork variability before deciding whether longer warmup or a separately reported reversed-order recheck is necessary.

## 6. Collect restricted JFR profiles separately

The supplied [InProcessJfrProfiler](harness/src/org/eclipse/rdf4j/query/resultio/binary/benchmark/InProcessJfrProfiler.java) starts recording at the first measurement iteration and stops after the last. It retains the JDK profile settings for these six event types:

- jdk.ExecutionSample
- jdk.NativeMethodSample
- jdk.ObjectAllocationSample
- jdk.GarbageCollection
- jdk.CPULoad
- jdk.ThreadCPULoad

It explicitly disables other event types found in the profile configuration or currently registered with FlightRecorder. Always inspect the actual recording inventory before publishing; configuration is not a substitute for that check.

Use an explicit round-two output directory. The profiler's fallback directory retains the round-one name, and its recording names expect the large benchmark's workload parameter.

```sh
python3 binary-writer-round2/harness/bench.py jmh "$RDF4J_BENCH_RUN-profiles" -- \
  -p implementation=published,current -p namespaceLayout=stable -p sink=heartbeat \
  -p workload=ascii,long_unicode,query \
  -wi 5 -w 1s -i 5 -r 2s -f 1 \
  -prof "org.eclipse.rdf4j.query.resultio.binary.benchmark.InProcessJfrProfiler:$RDF4J_BENCH_ROOT/binary-writer-round2/$RDF4J_BENCH_RUN-jfr"
```

The log prints each recording's complete path. Select one actual file, inspect event types/counts, and export only the CPU/allocation events used by the summarizer:

```sh
export RDF4J_PROFILE_FILE="/absolute/path/to/the/recording-printed-in-the-log.jfr"

"$JAVA_HOME/bin/jfr" summary "$RDF4J_PROFILE_FILE"

"$JAVA_HOME/bin/jfr" print --json --stack-depth 128 \
  --events jdk.ExecutionSample,jdk.ObjectAllocationSample \
  "$RDF4J_PROFILE_FILE" > "$RDF4J_PROFILE_FILE.events.json"

python3 binary-writer-round2/harness/summarize_jfr.py \
  "$RDF4J_PROFILE_FILE.events.json"
```

For the inventory, expect the six allowed event types plus JFR structural metadata/checkpoints; investigate other nonzero event types before sharing the recording. The summarizer writes adjacent summary JSON and Markdown. Explicit stack depth avoids the JFR print command's shallower display default.

Use these profiles for CPU and allocation attribution. Their instrumented timings are separate from the unprofiled comparison results. Inclusive stack percentages overlap, and sampled allocation weight estimates represented bytes rather than an exact object count.

## Allocation sampling boundary correction

Starting JFR only at measurement time can assign pre-recording cumulative allocation weight to the first allocation sample on each thread. Absolute B/row comes from the separate JMH GC-profiler runs. Preserve unadjusted JFR summaries and generate the separate first-event sensitivity reports after timing:

```sh
python3 binary-writer-round2/harness/steady_profiles.py \
  binary-writer-round2/your-recording-directory/*.events.json
```

Replace the directory with the actual recording directory. This omits the first allocation event per recorded thread ID, records every omitted weight/site/time/stack, and leaves CPU events unchanged. It produces separate `*.steady-summary.json` and Markdown files; it does not overwrite raw summaries. The omission also removes an unknown amount of valid post-start allocation, so the result is an approximate distribution, not exact accounting. The [allocation investigation](query-allocation-analysis.md) gives the pinned JDK sources, checks and limits.
