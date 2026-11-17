# Binary query writer benchmark harness

`BinaryWriterBenchmark` runs the actual writer classes, with one complete 4,096-row binary document per invocation. `@OperationsPerInvocation(4096)` reports time and `gc.alloc.rate.norm` **per row**, not per document. A writer is newly constructed for each document. Its constructor is looked up once per trial and invoked reflectively once per document; no reflection occurs inside row or value loops.

## Fixtures

| Workload | Input |
| --- | --- |
| `ascii` | Eight columns: IRI, plain literal, integer literal, language literal, blank node, IRI, decimal literal, custom-datatype literal; values change each row. |
| `unicode` | Same shape, with Norwegian, Japanese, and supplementary Unicode in literal labels and a directed language literal. |
| `repeat_identity` | Eight initialized `LmdbLiteral` columns; runs of 32 rows reuse exactly the same value objects. |
| `repeat_equal_lmdb` | Same lexical values, native IDs, and runs as `repeat_identity`, but every cell has a distinct initialized `LmdbLiteral` object. |
| `unique_lmdb` | Eight initialized `LmdbLiteral` columns with distinct labels and canonical IDs in every row. |
| `wide` | 32 columns, repeating the eight-column ASCII type pattern with distinct column-specific values. |
| `long` | Eight columns; one changing 8,192-character ASCII literal per row, with seven small mixed-type values. |
| `query` | A real LMDB store, native evaluation enabled: 512 subjects with six fixed attributes and eight `v` values, yielding exactly 4,096 eight-column query rows. Query evaluation and serialization are both measured; loading and query preparation are outside timing. |

The three synthetic LMDB literal workloads use an explicitly canonical revision stub and already initialized payloads. They isolate native equality cost and do not measure a database. `query` is the separate real-database workload. All synthetic rows are built during trial setup. A reusable `ByteArrayOutputStream` is reset for each document and pre-grown during setup. The measured method consumes the backing array, four sampled output bytes, and output size. This measures real in-memory serialization; it does not measure socket or servlet costs.

`namespaceLayout=stable` is the default for JMH and smoke validation. In `ascii`, `unicode`, `wide`, and `long`, it constructs subject and kind IRIs as `urn:binary-benchmark:subject:<row>-<column>` and `urn:binary-benchmark:kind:<mod17>-<column>`. These mixed documents use four namespaces: subject, kind, XSD, and the custom datatype namespace. Select `-p namespaceLayout=churn` to reproduce the original fixture exactly, with a colon before the column instead of a hyphen. RDF4J splits URNs at the last colon, so that layout produces 4,096 subject namespaces, 17 kind namespaces, XSD, and the custom datatype namespace: **4,115 namespaces per document**. Namespace layout is selected while constructing the fixture, outside timing. It does not change the synthetic LMDB literal workloads or the real query.

The original `isolated-screen` measurements and initial profiles used the churn fixture before this parameter existed. Their results remain a namespace-churn stress case; they must not be labeled stable. [The methodology review](../methodology-review.md) records the original fixture and artifact hashes. Use explicit `-p namespaceLayout=stable` in canonical comparison commands and `-p namespaceLayout=churn` when reproducing that screening workload.

The default `sink=memory` writes directly to that buffer. Select `-p sink=heartbeat` to include the actual `QueryResponseHeartbeat` payload stream used by server integration. The harness calls `start(writer)`, asserts that binary format declines whitespace scheduling, and calls `complete()` after each document. There is no heartbeat thread or periodic output, but every serializer write passes through the real synchronized payload path. This tests whether batching writes removes wrapper overhead.

## Reproduce with `bench.py`

Run the following from the RDF4J repository root containing the candidate production source and this evidence directory. Requirements are a JDK 25 installation, Maven, Python 3, and the platform's supported LWJGL LMDB native library. The recorded measurements used Linux x86-64. Set `JAVA_HOME` to your own JDK installation:

```sh
export JAVA_HOME="/absolute/path/to/jdk-25"
export PATH="$JAVA_HOME/bin:$PATH"
BENCH_ROOT="$(pwd -P)"
"$JAVA_HOME/bin/java" -version
```

### Build matching dependencies and generate the classpath

Install the matching reactor snapshots into this checkout's `.m2_repo`. The `quick` profile skips test execution while retaining test compilation and test artifacts needed by the dependency graph. This is a build prerequisite, not a test run:

```sh
mvn -o -Dmaven.repo.local="$BENCH_ROOT/.m2_repo" -Pquick \
  -pl core/sail/lmdb,core/queryresultio/binary,core/http/client -am install
```

Generate the LMDB module's **test-scope** dependency classpath. That scope includes the repository fixture dependencies and JMH 1.37, including its annotation processor. An absolute output path ensures Maven writes into this harness directory rather than beneath the selected module:

```sh
mvn -o -Dmaven.repo.local="$BENCH_ROOT/.m2_repo" \
  -pl core/sail/lmdb dependency:build-classpath \
  -DincludeScope=test \
  -Dmdep.outputFile="$BENCH_ROOT/binary-writer-evidence/harness/dependency-classpath.txt"
```

These commands use offline mode for a populated cache. If `.m2_repo` lacks dependencies or build plugins, repeat the affected command without `-o` to download them, then return to offline mode. The delivered source and evidence do not include a ready-to-run Maven cache or compiled benchmark binaries. `dependency-classpath.txt` contains absolute paths for the machine that generated it; regenerate it locally, including after moving the checkout. Avoid mixing snapshots from another source tree. `bench.py` currently expects this revision's `6.2.0-SNAPSHOT` RDF4J artifacts and JMH 1.37.

The runtime needs `rdf4j-queryresultio-binary`, `rdf4j-repository-sail`, `rdf4j-sail-lmdb`, `rdf4j-http-client`, and their transitive dependencies. `bench.py` adds the installed binary-writer, HTTP-client, and LMDB jars to the generated classpath, checks that every entry exists, and places the separately compiled harness classes first.

### Compile and validate the harness

The supplied `OriginalBinaryQueryResultWriter` is the fixed upstream source with only its public class and constructor renamed. The `current` implementation is compiled directly from the actual production `BinaryQueryResultWriter.java`; the generator never replaces production. If experiment sources need regeneration, run the optional first command, which derives them from `harness/original/BinaryQueryResultWriter.java`:

```sh
# Optional: regenerate the isolated experiment classes from the immutable baseline.
python3 binary-writer-evidence/harness/generate_variants.py
python3 binary-writer-evidence/harness/bench.py compile
python3 binary-writer-evidence/harness/bench.py smoke \
  baseline,current \
  ascii,unicode,repeat_identity,repeat_equal_lmdb,unique_lmdb,wide,long,query \
  memory,heartbeat stable,churn
```

Compilation runs JMH's annotation processor and writes `harness/compiled-source-identity.json` with the Java version, source hashes, Git HEAD, and resolved classpath. Smoke validation parses each complete document and compares every synthetic row. For the real query, it checks every term and all 4,096 expected subject/value pairs exactly once, allowing the query's row order to differ. Every successful selection prints `roundtrip=PASS`, namespace layout, row count, and byte count. Optional smoke arguments are CSV lists of implementations, workloads, sinks, and namespace layouts, in that order; the fourth defaults to `stable`.

The short experiment names `columns`, `ascii`, `reusable`, `utf8`, `datatype`, `buffered`, and `combined` select their corresponding writer classes. Additional classes can be selected by simple or fully qualified class name; they must implement `TupleQueryResultWriter` and expose an `OutputStream` constructor. Baseline/current comparisons do not depend on choosing an experimental class.

### Run the recorded comparisons

Run each command separately on an otherwise idle machine. `bench.py jmh NAME` writes `NAME.json`, `NAME.log`, and `NAME-command.json` under `binary-writer-evidence/`. The commands below use the investigation's report names; use new names in your own working copy when retaining the supplied results.

The runner supplies one benchmark thread, fail-on-error behavior, native access, a 512 MiB initial and 1 GiB maximum heap, `ActiveProcessorCount=2`, JFR stack depth 128, `DebugNonSafepoints`, and the WARN-level logging configuration. Each comparison adds the GC profiler to measure allocation alongside time. The complete heartbeat matrix uses five one-second warmup iterations and five one-second measurement iterations in each of two forks:

```sh
python3 binary-writer-evidence/harness/bench.py jmh final-heartbeat -- \
  -p implementation=baseline,current \
  -p namespaceLayout=stable -p sink=heartbeat \
  -p workload=ascii,unicode,repeat_identity,repeat_equal_lmdb,unique_lmdb,wide,long,query \
  -wi 5 -w 1s -i 5 -r 1s -f 2 -prof gc
```

The separately preserved query recheck requests the opposite implementation order and extends warmup to ten one-second iterations, followed by eight one-second measurements in each of two forks. Retain both the original query measurements and this recheck when interpreting the result. The JMH log records actual execution order:

```sh
python3 binary-writer-evidence/harness/bench.py jmh final-query-recheck -- \
  -p implementation=current,baseline \
  -p namespaceLayout=stable -p sink=heartbeat -p workload=query \
  -wi 10 -w 1s -i 8 -r 1s -f 2 -prof gc
```

The memory-only control omits the heartbeat payload wrapper and measures the ASCII and distinct-but-equal LMDB literal fixtures:

```sh
python3 binary-writer-evidence/harness/bench.py jmh final-memory -- \
  -p implementation=current,baseline \
  -p namespaceLayout=stable -p sink=memory \
  -p workload=ascii,repeat_equal_lmdb \
  -wi 5 -w 1s -i 5 -r 1s -f 2 -prof gc
```

The `long` workload writes over 32 MiB per invocation. Time and allocation remain normalized per row. Preserve the source identity file, full commands, raw JMH JSON, and per-fork results; distinguish stable namespaces from the original churn screening fixture.

### Collect separate restricted JFR profiles

Compile the current profiler with the harness, then select `InProcessJfrProfiler` in a separate run. This example profiles the mixed ASCII, Unicode, and equality-heavy cases. It deliberately has a different result name from the timing comparisons:

```sh
python3 binary-writer-evidence/harness/bench.py jmh filtered-profiles -- \
  -p implementation=baseline,current \
  -p namespaceLayout=stable -p sink=heartbeat \
  -p workload=ascii,unicode,repeat_equal_lmdb \
  -wi 5 -w 1s -i 5 -r 2s -f 1 \
  -prof "org.eclipse.rdf4j.query.resultio.binary.benchmark.InProcessJfrProfiler:$BENCH_ROOT/binary-writer-evidence/jfr-filtered"
```

Use these recordings for CPU/allocation attribution; keep their timings separate from the comparison tables. The following section documents the profiler's event allowlist, actual-recording inspection, and conversion to summaries.

## Summarize JFR CPU and allocation samples

`InProcessJfrProfiler` is an optional JMH internal profiler for environments where a separate `jcmd` process cannot attach to the fork. Compile its Java source with the harness and select it using:

```sh
-prof org.eclipse.rdf4j.query.resultio.binary.benchmark.InProcessJfrProfiler:/absolute/profile/directory
```

It uses the benchmark JVM's JFR API and derives a restricted settings map from `Configuration.getConfiguration("profile")`. The exact allowed events are:

- `jdk.ExecutionSample`
- `jdk.NativeMethodSample`
- `jdk.ObjectAllocationSample`
- `jdk.GarbageCollection`
- `jdk.CPULoad`
- `jdk.ThreadCPULoad`

Their profile settings, including sampling periods, allocation throttle, and stack-trace settings, are retained unchanged. Every other event listed in the profile configuration or currently registered with `FlightRecorder` receives an explicit `enabled=false` setting before `new Recording(settings)` is started. This excludes environment variables, system/security properties, process command lines, and unrelated diagnostic events. Explicit disabling matters because omitted Java-defined events can otherwise inherit an enabled default. In particular, `jdk.InitialEnvironmentVariable`, `jdk.InitialSystemProperty`, `jdk.InitialSecurityProperty`, `jdk.SecurityPropertyModification`, `jdk.SystemProcess`, `jdk.ProcessStart`, and `jdk.JVMInformation` are excluded.

This harness does not define arbitrary custom JFR events. Final recordings must nevertheless be inspected for their actual event types before publication, rather than assuming configuration alone proves their contents. Initial recordings made before this restriction remain private; public profile artifacts use fresh restricted recordings and summaries limited to the intended profiling events.

Recording starts before the first measurement iteration and stops after the last, excluding preceding warmup iterations. Each recording name includes implementation, workload, sink, timestamp, PID, and a unique suffix; JMH reports its final path as a text result. Use this profiling run separately from unprofiled timing comparisons.

Convert the recording with a sufficiently deep displayed stack. The `jfr print` default is only five frames, independently of how deeply the recording captured stacks:

```sh
jfr print --json --stack-depth 128 \
  --events jdk.ExecutionSample,jdk.ObjectAllocationSample \
  binary-writer-evidence/profile.jfr > binary-writer-evidence/profile.events.json
python3 binary-writer-evidence/harness/summarize_jfr.py \
  binary-writer-evidence/profile.events.json
```

The script writes `profile.events.summary.json` and `profile.events.summary.md`. `--json`, `--markdown`, and `--top` customize destinations and Markdown table length. It streams the standard JDK JSON envelope instead of loading all events into memory and requires only Python's standard library. The JSON retains all aggregated methods and their JVM descriptors; the Markdown defaults to the top 25.

Selection is a benchmark-thread regex match **or** a writer/benchmark method anywhere in the stack. Override `--thread-regex` and `--stack-regex` if profiling a different harness. Reports include both all-event and selected-event denominators, thread names, missing/truncated stack counts, CPU leaf and inclusive methods, allocation weights by class, allocation leaf and inclusive methods, and allocation class/leaf pairs. Recursion contributes only once to each inclusive method per event. Overloaded methods remain distinct.

Execution-sample fractions estimate where selected CPU samples occurred; they are not wall-clock percentages. Allocation event `weight` estimates bytes represented by sampled objects; event counts are not allocated-object counts. Inclusive percentages overlap. Setup and warmup are included if present in the input recording, and the default filter does not include arbitrary asynchronous query worker threads. The report states these limits explicitly.

The small attribution checks can run without any Java process or profile:

```sh
python3 binary-writer-evidence/harness/test_summarize_jfr.py
```
