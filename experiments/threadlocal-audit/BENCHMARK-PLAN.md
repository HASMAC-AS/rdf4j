# Fresh matched ThreadLocal benchmark plan

**Execution update:** Implemented and completed; see `RESULTS.md` for authoritative actual protocol/results. Actual interpreted forced route is `irKernelInterpreted`, not `irKernel`; six projected slots can still admit the budgeted-wide table (threshold four), so admission is reported rather than assumed absent. Generic collection sync threshold is explicitly zero in all arms to guarantee heap-only generic collections. Initial 3-warm/5-measure pilot was rejected; accepted series uses 20 warmups/10 measurements across 8 forks per route. The following retains the original design rationale, not a second completed experiment.

Status: source-inspected design, not executed. Base is `ba74f4782e95e386653a6b1c7f6b0969e09b8f12`, **not main**. No earlier frozen binaries survived recovery. Historical overlay numbers are not this environment's baseline. This plan introduces no production changes and runs no Maven or benchmark while `native_token_path` owns the measurement window.

## Reuse and limitations

Use `core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/evaluation/OverlayOptionalQueryBenchmark.java` as the primary harness donor. It already supplies:

- Deterministic LMDB fixture with groups × fanout³ rows, nested OPTIONAL/unbound suffixes, Unicode/language literals, reopen and adjacency/overlay readiness checks.
- Reusable fixture marker and cardinality verification, native route observer, compiled binds, lexical row checksum, memo counters, query-memory and spill counters, query-thread allocations, warmup and per-run time.
- Single JVM per treatment, easy to freeze with production classes.

It is **not usable unchanged for three regimes**: it accepts only `irKernel`/`orderedDistinct`, always enables native+Janino synchronously and disables the interpreter, and requires compiled binds for irKernel. It measures connection open + prepare + evaluate + full lexical consumption + close together. It does not expose first-row latency, interpreted binds, regular strategy evidence, a narrowed projection, a strict no-spill assertion, or independently validated expected checksum.

`BenchmarkRouteTelemetry` is reusable for IR planned/opened/rows and chunk/source counters, but lacks interpreted/compiled bind separation. Read those package-private counters from a harness in the evaluation package. `ThreeTierParityBenchmark`/`ThreeTierRegime` provide useful fixture ideas and the important fresh-JVM rule, but their LMDB/ADJACENCY/JANINO ladder keeps native enabled in all three and changes adjacency availability. That is **not** regular/interpreted/compiled isolation. `JaninoCeilingBenchmark` compares hand-written fused kernels and row counts, not a matched end-to-end query checksum; do not substitute it.

Old `experiments/overlay/run-*.sh` assume vanished `../tools` and frozen class directories. Reuse their ABBA ordering, not their classpaths or historical hashes. Workbench harness is a second-stage confirmation, not the initial cancellation-loop isolator.

## Minimal harness changes (proposed, not yet implemented)

Add `ThreadLocalQueryBenchmark` in the evaluation test package, borrowing the fixture/query builder and result checks. Keep one shared harness binary for A and B; avoid different consumption work across variants.

1. Accept `audit.regime=regular|interpreted|janino` before any engine class initializes. Regular: native flag false, config native evaluation false, no forced native strategy. Interpreted: native/config true, force `irKernel`, Janino false, interpreter true. Janino: native/config true, force `irKernel`, Janino true + synchronous true + thresholdRows=0, interpreter false. Do not permit silent fallback to another native route. Keep overlay, adjacency, parallelism and all non-treatment flags identical.
2. Retain 30-slot original query as `audit.shape=wide`. Add `narrow`: keep the entire same WHERE but project `?s ?label ?seed ?n0 ?n1 ?n2`. Including all three member identities retains the expected groups × fanout³ cardinality; reduce projected/materialized width without changing fixture. Recompute checksums; never reuse wide's checksum for narrow. Optimizer may change the physical plan, so record it separately, not as a pure width microbenchmark.
3. Capture per-run `COMPILED_BINDS`, `INTERPRETED_BINDS`, selected route, `BenchmarkRouteTelemetry`, memo, spill/migration/budget and loaded class code-source URLs. For regular additionally assert the actual evaluation strategy class/factory and native-disable settings in an untimed diagnostic, plus zero native opens/binds during the measured interval. Empty route observer alone does not prove regular execution.
4. Record `prepareNanos`, `evaluateReturnNanos`, `firstFullyMaterializedRowNanos`, `first1000RowsNanos`, `fullConsumeNanos`, `closeNanos`, total including close, row count and checksum. Query timeout/transaction mode identical. First row means all projected lexical values consumed, not only result object returned. Keep query parse/prepare-inclusive and preprepared steady execution separate.
5. Sum hash of all projected values in fixed binding order as existing harness does. Add independent regular preflight validation, full RDF-term identity (kind, lexical form, datatype, language, unbound), and a second independent order-insensitive checksum or sorted canonical output digest for the small fixture. Run strict semantic validation untimed. Never assert unordered query row order. Store expected results in a manifest keyed by fixture+query SHA; compare every timed execution to that reference.
6. Assert zero native spill CREATED, zero migration, and returned ledger reservation. Native spill counters do not cover generic collections. In a separate no-spill proof fork, trace query-time file creations/writes with stack information (JFR FileWrite threshold=0 or equivalent supported tracing), inspect the actual regular DISTINCT implementation/factory, and record spill paths/counters used there. Distinguish legitimate LMDB data reads/writes before timing. Do not infer no spill just because files are removed before shutdown. If regular spills, reduce groups equally or use the non-DISTINCT projection control, and rerun all regimes.
7. Keep counters/logs outside timed loop when possible. Time each major phase with a bounded number of nanoTime calls. Do not add nanoTime or atomics per row solely to measure ThreadLocal costs.

## Fixtures and cells

Start fixture **100 groups, fanout 10 = 100,000 output rows**; same generator supplies 26,700 triples (3/group + 3 arms × [10 edges + 78 value triples] per group). Use 20 groups for correctness smoke and 100 for first profiling. Reuse a fixture copy in one process at a time; do not let two LMDB processes open it concurrently. Build/readiness is outside timing, and each fresh JVM reopens the persisted store. Reopened JVM means cold Java state, not necessarily cold OS disk cache; do not call it cold disk.

Primary matrix: A baseline versus B explicit-context implementation × regular/interpreted/Janino × narrow/wide at 100 groups. Default-sized native cap explicitly pinned to **671088640 bytes (640 MiB)**, with global limit recorded and held constant. First preflight must prove no spill on all six cells; do not merely assume 100k fits. Preserve current best combination flags equally in A and B, recording their exact values and defaults from the actual baseline manifest rather than guessing from old filenames. Particularly record memo/admission/capacity, partial-ready, compact snapshot, grouping, typed materialization, wide DISTINCT, spill I/O and adjacency mode. Keep parallel/IR parallel disabled in the first matrix, then separately validate workers.

Add a real **cursor-backed scan** control with direct adjacency disabled, non-DISTINCT `SELECT ?s ?p ?o WHERE { ?s ?p ?o }`, exact row count equal to stored triples, all terms consumed, and no aggregation that could replace scanning with catalog metadata. This directly exercises LmdbRecordIterator checkpoints. Add selective-filter and short nested-probe controls only after primary matrix; rejected candidates and frequent source opens are distinct frequency regimes. These controls are not the same workload as wide OPTIONAL.

The current type-matrix optimization must also get a **route-verified TypeMatrix/co-scan cell**. A generic irKernel label does not establish TypeMatrix ran. Reuse the query/fixture from `LmdbNativeTypeMatrixCancellationTest` or corpus type-matrix benchmark, assert its native compile/selection and worker/morsel counters before attributing speedup. If this cell cannot run compiled IR, report N/A for that route rather than forcing an unrelated plan. Profiled route coverage must match the changed call sites.

## Build/freeze discipline and command templates

Commands below are a recipe for the owner after tests/lock release; proposed `audit.*` options require the harness implementation above. No benchmark has run from this document.

1. Freeze baseline from a separate worktree at ba74f478, and candidate at its exact commit, using JDK `../toolchain/jdk-25.0.5+8` and Maven wrapper `experiments/threadlocal-audit/mvn.sh`. Complete module dependencies once, then module-only test compilation. Do not overwrite the active worker's target directories or `.m2_repo` during its tests. Parent coordinates build window. No `-am` or `-q` for tests.
2. Capture ALL runtime dependency jars in immutable A/B manifests, not just lmdb classes layered over moving shared targets. Common/iterator, repository/Sail/API and evaluation classes can also change. Use identical third-party jars and JDK; log SHA-256, exact git diff, command, CPU/memory and JVM flags. Verify class origins for CancellationSignal, NativeCancellationToken, LmdbRecordIterator, NativeCancellationCheckpoint, QueryExecutionContext and the harness at startup.
3. Generate test dependency classpath with module-only Maven (owner may use an already valid classpath):

```sh
experiments/threadlocal-audit/mvn.sh -pl core/sail/lmdb test-compile -DskipTests
experiments/threadlocal-audit/mvn.sh -pl core/sail/lmdb dependency:build-classpath \
  -DincludeScope=test -Dmdep.outputFile="$PWD/experiments/threadlocal-audit/dependencies.txt"
```

4. Canonical lock agreed with owner, **one path for every worker**. Older scripts use inconsistent lock paths; do not run them unchanged. Example fork after fixture and manifest are prepared:

```sh
JAVA=../toolchain/jdk-25.0.5+8/bin/java
# Set LOCK to the parent's canonical absolute lock path; CP_A/CP_B to frozen manifests.
# AUDIT_COMMON_FLAGS is an identical recorded array of best-combination flags.
(
  flock 9
  "$JAVA" --enable-native-access=ALL-UNNAMED -Xms2g -Xmx3g \
    -Drdf4j.lmdb.queryMemory.perQueryMaxBytes=671088640 \
    -Djava.io.tmpdir="$RUN_TMP" \
    -Daudit.regime=interpreted -Daudit.shape=narrow \
    -Daudit.fixture="$FIXTURE" -Daudit.warmups=5 \
    "${AUDIT_COMMON_FLAGS[@]}" -cp "$CP_A" \
    org.eclipse.rdf4j.sail.lmdb.evaluation.ThreadLocalQueryBenchmark 100 10 10 true \
    > "$RUN_LOG" 2>&1
) 9>"$LOCK"
```

Build the candidate and baseline once, freeze, then **for each regime/shape** run fresh-JVM A B B A B A A B order (four independent forks each), with five warmups and ten measurements initially. Increase warmup only before the final series if compilation/GC trends persist; do not discard slow measured samples ad hoc. Do not interleave other builds/profilers. Reverse order across regime/shape blocks or randomize a prespecified seed. Store fork-level summaries, not 40 in-JVM runs treated as 40 independent environments. Report median, range and paired/fork-level uncertainty; do not claim a win smaller than variability. Throughput = rows / timed seconds for a sequential query, not concurrent QPS. Concurrency and p95 request latency need a separate load experiment with enough samples.

## ThreadLocal versus polling attribution

Three separate instruments, never collapsed into one percentage:

- **Clean timings**: normal A versus B, exactly the same cancellation checks, interrupt checks, deadline checks and mask/stride. Record time to first row and full consumption. If poll cadence changed, comparison no longer isolates lookup/binding.
- **Access-count diagnostic builds**: at known helpers count CURRENT/Slot.get, set, remove, capture, restore, checkpoint calls, actual token checks and candidate ticks. Attribute per open/pull/row/morsel/value to INVENTORY.md. Use thread-owned primitive counter objects passed explicitly or locals aggregated at end; do not add new ThreadLocal or global contended atomics per event. Report instrumentation overhead separately, and never use these runs for latency claims. Add LWJGL stackPush counts through caller instrumentation, not assumptions that every JNI call uses it.
- **Sampling profile forks**: record JFR CPU/allocation with identical settings for A/B after readiness/warmup, separate from clean timings. Preserve compiled route counters and no-spill proof. JIT can inline ThreadLocal.get, so absence of that frame does not prove no lookups; correlate helper frames, access counts and generated/inlined code evidence where available. Sample share is not wall-time percentage. Use a small standalone matched microbenchmark for ambient-get+check vs already-captured-token.check vs boundary capture/restore to isolate mechanisms, with same live mutable token and an observable result. A diagnostic no-poll microbenchmark is not a deployable optimization and cannot justify weaker cancellation.

Safety results alongside speed: already-cancelled query, later cancellation, timeout, nested callback token shadowing, worker propagation, replay/map growth, early close, and cancellation acknowledgement/cleanup latency under scan load. Query cancellation must remain correct even if the noncancelled microbenchmark gets faster.

## Workbench follow-up

After a proven winning combination, rebuild/freeze a fresh server/Workbench runtime and adapt `experiments/overlay/workbench-e2e/{WorkbenchBenchmarkServer.java,run.py,client.mjs}`. Old extracted Boot jar/class snapshots are not available baseline evidence. Keep binary-writer PR version identical across A/B (or use a separately labeled factorial writer experiment). Assert actual binary response, compiled/interpreted counters, row content and no spill; collect first NDJSON row, window completion, terminal record and EOF. A narrow no-spill query must use matching query text on server and client validator. Report **Workbench HTTP/NDJSON** only, not browser paint/IndexedDB. Build and validate the transport harness separately before timing.
