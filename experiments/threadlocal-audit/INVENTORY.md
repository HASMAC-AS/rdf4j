# Query-path ThreadLocal inventory

## Scope and evidence

This is a source-backed inventory, not a profile or a claim that all entries run on every query. It covers repository-owned declarations in production Java under `core/` and `tools/`, including regular evaluation, native interpreted execution, Janino-generated execution, callbacks, value decoding, diagnostics and optimizer work. Static frequencies below describe call-site placement, not measured invocation counts. A pull means one `hasNext`, `next`, `remove`, or close call at the named wrapper; nested wrappers can multiply that cost. A candidate row can be rejected and never become an output row.

The appendix records every detected ThreadLocal declaration and its direct get/set/remove sites in the declaring file. It intentionally includes cold and write-only entries so that exclusions are explicit. ThreadLocalRandom is listed separately: it uses per-thread random state, not a repository-owned ThreadLocal map lookup. Third-party implementation internals (JDK, Janino, logging, HTTP, XML and spatial libraries) are not exhaustively audited by this source scan. An absent repository declaration does not establish zero transitive ThreadLocal activity.

Line numbers are for the pinned base, before cancellation-propagation edits. Re-run the searches against later revisions. This document changes no production code, so the inventory itself is a Routine C investigation.

## Actual query paths and likely frequency

### Shared repository/Sail entry and lifetime

`SailTupleQuery.evaluate` / `SailBooleanQuery.evaluate` / `SailGraphQuery.evaluate` establish a BlankNodeAllocationScope around opening evaluation. `AbstractSailConnection` admits operations using its `operationThread` holder and `CancellationSignal.current`, combines connection lifetime with ambient lineage, and uses `swapCurrent` / `restoreCurrent` on entry/exit. `resultLineage()` captures lineage for returned results. This is operation/pull-boundary context management, not kernel candidate polling. The signal's CURRENT holds a reusable Slot; swap/restore mutate the slot after `get`, rather than performing ThreadLocal.set/remove per pull. Counting only ThreadLocal.set would miss those lookups.

`LmdbStoreConnection` adds read exposure and replay semantics: `internalReadScopeDepth`, `activeReadExposureScope`, and `readThreadRegistration` occur around observed result pulls and nested reads. `ReadExposureScope` and `CapturedReadContext` capture `CancellationSignal.current()` in their field initializers; the worker-admission path also resolves it. `pendingEvaluationReplayPlan` is an evaluation-open handoff; `explainTupleExprCapture` is explanation/open work; `namespaceReadDepth` is namespace-read nesting, not every tuple slot. These must be separated from cancellation polling itself. Substituting a native token for a lifecycle signal is not automatically equivalent: shutdown, connection ownership, replay and map-growth admissions have their own scope.

`RepositoryValueIteration.SOURCE_PULL` is installed/restored around source `hasNext`/`next`, and consulted by `ownsSourcePull`; it distinguishes reads inside an existing source pull from later external lazy-value access. This is per pull when that wrapper is present. `ValueResolutionScope.ACTIVE` is installed/restored for resolution scope activation and queried from ValueStore; it preserves read-revision/reader ownership, including detached values. It is not merely an on/off cancellation flag.

`LmdbSailStore.activeReadView` and `activeReadResourceContext` carry scoped views/resources into lower-level access; `callbackAdmission` handles callback admission, and `replayRetirementOwner` tracks retirement ownership. These may run per scope, callback or store operation, including nested statement reads inside a query. Writer/publication/none-mutation fields are adjacent infrastructure, not proven per-row read-query cost.

`ValueStore.readContext` contains borrowed transaction/revision and read-barrier state. Lookups occur in resolution and decoding paths and may therefore scale with values rather than queries. The implementation already groups multiple items in one reusable holder, but repeated accesses remain (e.g. consecutive accesses near borrowed-writer checks). A query-native materializer can potentially pass that context explicitly, but its validity depends on the active transaction and nested callback scope. `safeAllocationResize`, `safeRetirementReservationResize` and `standaloneWriterAdmissions` are resize/write admission controls; do not count their declarations as normal tuple-read work.

### Regular evaluation and expressions

`QueryContextIteration` is not a universal regular-LMDB wrapper: the production construction found in this checkout is `LuceneSailConnection.evaluate` (417), after an opening begin/end scope (409–413). When present it calls `QueryContext.begin()` and `end()` around each hasNext/next/remove/close. Begin reads and installs; end removes and restores the previous query. `AbstractSpinFunction` can reach `QueryContext.getQueryContext()` to obtain a query preparer. This is an actual ambient query-context use, distinct from cancellation.

`AdaptiveFilterEvaluationStep.Execution.call` gets CURRENT, sets this execution, runs the wrapped operation, and restores the previous reference with set. Its `ScopedIteration` surrounds hasNext/next/close, and evaluate also opens a scope; its `workSource` wrapper consults CURRENT for statement opens and counts. This is per pull plus per source-open/count when adaptive regions exist, not unconditional for all strategies. It carries filter-region telemetry and original-site semantics.

`BlankNodeAllocationScope.ACTIVE` resolves the active query's blank-node allocator at strategy/context construction and query entry. Its per-scope `currentSolution` is installed/restored for solution evaluation, and is read for labeled blank-node allocation and solution snapshots. `ExtensionIterator` is one concrete per-solution caller. `FilterIterator`, order key handling, SERVICE and SPIN can participate. Removing it must preserve per-solution BNODE identity, repeated evaluation, ordering and nested calls.

`DefaultEvaluationStrategy.compilationDepth` is read during recursive tuple/value expression precompile and removed when depth returns to zero. This is compilation-tree frequency, not per produced row. `QueryEvaluationUtility.ACTIVE_SAFETY_SNAPSHOT`, `QueryModelNormalizerOptimizer.CURRENT_ANALYSIS`, and `SketchBasedJoinEstimator.optimizationScope` support planning and repeated optimizer analysis; their get counts scale with planning calls/nodes rather than normal result pulls. Dynamic/repeated subplan compilation can make them occur during outer query execution.

Expression-dependent caches are real query-path uses: `QueryEvaluationUtil.CALENDAR_VALUE_CACHE` on calendar comparison conversion, `ValueComparator.CALENDAR_SORT_KEY_CACHE` for date sort keys, `DecimalFormat.FORMATS` per formatting-function call, `GeoSupport.READER` per WKT parse, `AbstractLiteral.DATATYPE_FACTORY` per calendar literal construction/parsing, and LMDB `inlined.Dates.DATATYPE_FACTORY` in date conversion. These preserve reusable non-thread-safe library objects or cached parsed values. Their frequency depends on expressions and data types (sorting comparisons may exceed row count). They should not be removed simply because cancellation is expensive.

`AbstractParserQuery.LEGACY_HOOK_OWNER` prevents duplicate timeout ownership when invoking an overridden legacy max-query-time hook. `enforceMaxQueryTime` reads it only if a positive timeout is configured (67); `applyMaxQueryTimeHook` gets/sets/restores it (141–149) only after the ClassValue override check accepts a custom hook. The default hook path bypasses that scope. This is query-result setup/hook dispatch, not per-row cancellation polling.

### Native interpreted and Janino execution

`NativeCancellationToken.CURRENT` is read by `forSource`, `current`, `checkCurrent`, and `enter`, then restored by Scope.close. `forSource` first gets the execution context's explicit token, then can select a same-evaluation scoped child so nested operators inherit local cancellation. Ordinary `isCancellationRequested` follows instance fields, parents and thread interruption; it is not a CURRENT lookup on every poll. Its interrupted/cancelled path can call `QueryExecutionContext.consumeInterruptionOwner` or `currentOwnsCancellation`, which do consult worker replay context. Any replacement must preserve local operator stop versus enclosing query cancellation and worker attribution.

The important indirect scan path is `LmdbRecordIterator.next`/seek → `pollCancellationAtEntry` (only while its candidate tick is zero), `pollCancellationCandidate` (every 1024 candidate ticks), or `pollCancellationAtExhaustion` → `NativeLmdbQuerySource.checkpointCancellation` → **both** `NativeCancellationToken.checkCurrent()` and `LmdbNativeProbeDeadline.poll(0)`. At the pinned base the source helper is line 48, iterator helper calls are 857/863/868, and token CURRENT.get is line 204. These ThreadLocal reads exist even though the iterator itself declares no ThreadLocal. `TripleStore.exactIndexCancellation` (3981–3990) also calls replay safepoint plus this source helper on each supplier invocation; its invocation frequency is owned by the exact-index aggregation loop, not necessarily one per emitted tuple.

`LmdbNativeTypeMatrix.pollCancellation` (269–274) calls checkCurrent every 1024 ticks, and its worker loops additionally call it once per claimed/attempted morsel or range (1027, 1378, 1672, 4389). Parallel workers explicitly inherit the token but still consult its ambient CURRENT in these helpers. This is a concrete place to distinguish explicit-token capture cost from repeated checkpoint lookup, without changing polling cadence.

`LmdbNativeProbeDeadline.CURRENT` is separate from terminal cancellation. `poll(tick)` returns early unless `(tick & 1023) == 0`, then gets CURRENT, even with no active probe. Callers that pass zero always do the lookup. `LmdbNativeKernelExecution.KernelRowCursor` has both boundary `poll(0)` and buffer-position polls. `currentKernelCancellation` / `currentAggregateKernelCancellation` capture the ambient deadline into a KernelCancellation object before compiled/interpreted execution. Workers inherit a captured deadline; OWNED_ATTEMPTS retains probe resources until readers/tasks settle. The generated kernel can poll its explicit object without CURRENT, while enclosing cursor and fallback loops still use the ambient helper. Probe capacity exhaustion and timeout are intentionally different outcomes.

`LmdbFusedSipFactorizedRuntime.CURRENT` carries a factorized SIP session (runtime lookup/reuse context). `LmdbNativeKernelExecution` wraps the fused runtime row cursor with attach/restore on next and close, not only query open. Helper `currentOrNull`/current session accesses can occur inside execution. This is a strong candidate for explicit session ownership along cursor/kernel paths; nested subplans must keep their intended shared versus child session behavior.

`NativeExecutionContext.BORROWED_PRODUCER_OWNERS` supports worker context capture and borrowed producer activation. `NativeExecutionContext` task owner's `current` associates cleanup/worker admission with the active task. `initializingNativeStates` is a per-thread recursion/cycle guard around shared-state creation, not a general per-row table. Its checks scale with state creation or lookup entry, and active owner checks can consult BORROWED_PRODUCER_OWNERS.

`LmdbNativeTypeMatrix.LinkageMorsel.REUSABLE` borrows reusable scratch on morsel open; it is not per tuple by declaration alone. Type-matrix code also resolves `NativeCancellationToken.current()` at several operation starts. `LmdbNativeValueCodec.UTF8_SCRATCH` gets a reusable byte buffer for eligible strings up to 64 KiB and sets it on growth; it is value-encoding-dependent. `LmdbSyntheticValueSource.ACTIVE_EVALUATION` is a source scope needed for synthetic/fallback evaluation, not evidence of the same frequency in a real LMDB-source benchmark.

`LmdbNativeEvaluationStrategy.KERNEL_SUBPLAN`, `scopedValuePositions`, and `scopedUniformValues` propagate compilation/subplan hints and temporarily suppress forcing; `LmdbNativeDispatchLoop.ACTIVE_FORCING` carries selected strategy while dispatching. `NativeGraphFusion.SEMANTIC_BOUNDARIES` and `LmdbNativeAggregateCompiler.SEMANTIC_SUBQUERY_COMPILE_STACK` are semantic recursion/compilation guards. They can execute on nested dynamic subplans, but should not be treated as per-result cost without counters.

### Tracing, explain, workers and remote execution

`NativeExecutionPhaseEvent` has three ThreadLocals for evaluation/cancellation/input owner. Important: access is guarded by the JFR EventType enablement in its entry helpers, so disabled tracing often returns before a get. When enabled, spans and input scopes bind/restore at stage boundaries and nested work. Sampling a profile with this event enabled must be distinguished from ordinary disabled execution.

`LmdbNativeAttemptMetrics.DECLINE_CAPTURE` / DECLINE_ROUTE record attempts and decline reasons during dispatch/compilation; `LmdbNativeParallelAggregation.LAST_STRATEGY_LABEL` is diagnostic route metadata. `QueryExplanationContext.CURRENT` carries preview/analyze state; `SlowQueryContextHolder.CONTEXT` carries slow-query metadata. They can be consulted even if no output is emitted, but do not all run once per row.

`QueryExecutionContext.CURRENT` tracks HTTP/query checkpoints; REPLAY tracks active safepoints/deferrals; EXTERNAL_VALUE_INITIALIZER protects exposure; INSTALLED_REPLAY_CONTEXT records worker ownership and installed replay scope. `captureReplayContext` explicitly captures state for pre-existing workers. `ReplayCheckpoint` already caches the creating thread's ReplayState and uses direct state on that same thread, falling back to REPLAY.get on a different thread. Thus global replacement with one cached query value would regress existing cross-thread behavior. External-value initialization can be per binding set/value; safepoint calls can be per pull or loop stride. `QueryRequestContext` and `QueryExplanationRequestContext` are HTTP request scopes, typically per request/open, but nested requests must restore previous values.

`AsyncIteratorWorker.workerThread` is bound for the worker runnable and checked during close to avoid self-join. It is worker-lifecycle rather than one lookup for every returned row. Federation `QueryPlanLog.queryPlan` holds per-thread plan text; `TaskWrapper` mentions ThreadLocal cleanup as a contract but declares none itself.

### Explicitly colder or neighboring entries

`SailDatasetSnapshot.CURRENT` is activated during snapshot-scoped operations and queried when dataset resources are acquired; subquery/statement-open frequency depends on execution shape. `SailWriteContinuation.SCOPES`, `StreamingWritePreflight.CURRENT`, `Changeset.continuationReadLocks`, `SailModelCleanup.CURRENT`, LMDB `WriterTransaction.invocation`, `LmdbDirectAdjacencyStore.logicalCommitBatchDepth`, ValueStore resize/write flags, and bulk scheduler/stager locals are write, cleanup or loading mechanisms. They matter if query functions/callbacks trigger those operations, but are not established hot read-query paths. `TripleStore` per-thread cursor pool is touched when cursors are acquired/returned, which can repeat inside joins; prepared sort workspace and SPO-existence scratch are largely mutation paths. NativeStore WAL locals are another storage engine's write/checksum infrastructure. Startup diagnostics are observer scope, not normal evaluation.

### Verified indirect dependency: LWJGL MemoryStack

Bytecode inspection of the resolved `org.lwjgl:lwjgl:3.4.2` confirms `MemoryStack.stackGet()` executes `TLS.get()`; `stackPush()` calls `stackGet().push()`. `close()` operates on that stack instance rather than performing another stackGet. Evidence command: `javap -p -c -classpath .m2_repo/org/lwjgl/lwjgl/3.4.2/lwjgl-3.4.2.jar org.lwjgl.system.MemoryStack`. This adds a dependency-owned ThreadLocal to the 86 repository declarations; the latter count must never be described as all reachable thread locals.

In `LmdbRecordIterator`, stackPush at pinned lines 258 and 820 wraps native cursor allocation only when a pooled cursor is unavailable, during construction or cursor reopen. It is **not per next()/candidate** in that class. Repeated short probes can nevertheless make cursor acquisition scale with outer rows. `TripleStore` uses it for transaction/index/catalog work, exact count preparation (3854), context count (4048), total stats (4077), and SPO-existence probe (4099); these have different per-open/probe/planning frequencies, not one uniform per-query count. Its `perThread` cursor pool is a separate repository-owned ThreadLocal.

The following complete lexical callsite list for LMDB production source covers qualified and statically imported stackPush/stackGet calls (including write/cold callers). Passing an existing stack instance to lower helpers avoids another stackGet; static convenience allocators in dependency wrappers may add indirect accesses not enumerated here. Remaining JDK/logging/Janino/XML/spatial dependency internals are explicitly unresolved, not asserted absent.

- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LegacyStoreDiscovery.java`: 122, 247, 267, 280, 295, 370, 383, 399, 1661, 1810.
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbContextIdIterator.java`: 88.
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbRecordIterator.java`: 258, 820.
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbUtil.java`: 206, 230, 255.
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/PersistentSet.java`: 72, 138, 230, 275.
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/PersistentSetFactory.java`: 119.
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/PreparedBulkStore.java`: 350.
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/RetiredValueIdStore.java`: 198, 252, 290, 338.
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/TermOwnerGraphVerifier.java`: 46, 109.
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/TripleStore.java`: 684, 888, 1161, 1204, 1265, 1312, 2342, 2419, 2429, 2457, 2520, 2601, 2651, 2706, 2741, 3040, 3079, 3119, 3151, 3173, 3854, 4048, 4077, 4099, 4450, 4525, 4792, 4852, 4958, 5874, 6003, 6045, 6445, 7000, 7130, 7271, 7382, 7473, 8031.
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/TxnManager.java`: 673, 744, 1120, 1157.
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/TxnRecordCache.java`: 88, 213, 229, 259, 270, 315, 404.
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/ValueStore.java`: 404, 419, 1522, 1620, 2183, 2564, 2601, 2638, 2781, 3042, 3108, 3161, 3265, 3501, 3599, 3659, 3711, 3743, 4117, 4142, 4174, 4303, 4478, 5541, 6031, 6636, 8661, 8730, 10052, 10169, 10330, 10606, 10614, 10696, 11819, 12179, 12208, 13120, 14139, 14311, 14762, 14904, 15026.
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/estimate/LmdbDataFile.java`: 194, 300, 415.

## Prioritization and measurement contract

Do not assume every declaration is overhead on the measured route, nor infer the user's observed 5–10% slowdown from this inventory. First count dynamic accesses under ordinary, interpreted-native and compiled-native routing with feature flags and callback behavior pinned. Instrumentation must be separate from latency forks; counters can change inlining and contention.

The first candidates are repeated wrapper binding/restore, loop-level probe-deadline lookup, replay/exposure lookup, and value resolution context lookup. Preserve boundary fallback for API paths with no explicit context. For each candidate record the get/set/remove counts removed per query, poll frequency unchanged, nested/cross-thread correctness, first-row latency, window/full consumption, allocations and cancellation acknowledgement/cleanup latency. Record tracing enabled/disabled and synthetic versus real LMDB source separately. Compile/optimizer/date/format/geospatial caches need dedicated workloads rather than extrapolation from a wide synthetic DISTINCT query.

## Reproducibility and limitations

Search basis: `rg -n 'ThreadLocal|InheritableThreadLocal|ThreadLocalRandom' core tools --glob '*.java' --glob '!**/src/test/**'`. The appendix scans all production Java, associates field names with their declaring file, and lists direct field.get/set/remove expressions. It includes local generic declarations too. It is not a Java type-resolved call graph: indirect access through helper methods is described above; aliases, method references or third-party internals require additional review. Source ownership means a ThreadLocal must not be replaced with a plain shared field on a concurrently used connection/strategy without proving invocation ownership.

## Pinned revision

`ba74f4782e95e386653a6b1c7f6b0969e09b8f12`.

## Declaration and exact direct-access appendix

Detected 86 ThreadLocal declarations. Numbers are source line numbers; method names are the actual ThreadLocal operation.

- `core/common/iterator/src/main/java/org/eclipse/rdf4j/common/iteration/CancellationSignal.java`: **`CURRENT`**, declaration 35; accesses: 127 get, 136 get, 144 get.
- `core/http/client/src/main/java/org/eclipse/rdf4j/http/client/QueryExecutionContext.java`: **`CURRENT`**, declaration 43; accesses: 58 get, 61 set, 64 get, 69 remove, 71 set, 77 get, 91 get, 303 get.
- `core/http/client/src/main/java/org/eclipse/rdf4j/http/client/QueryExecutionContext.java`: **`REPLAY`**, declaration 45; accesses: 229 get, 244 get, 254 get, 607 get.
- `core/http/client/src/main/java/org/eclipse/rdf4j/http/client/QueryExecutionContext.java`: **`EXTERNAL_VALUE_INITIALIZER`**, declaration 46; accesses: 138 get, 141 set, 143 get, 151 get, 156 get, 165 get, 177 get, 193 get, 464 get, 474 get, 526 get, 539 remove, 541 set.
- `core/http/client/src/main/java/org/eclipse/rdf4j/http/client/QueryExecutionContext.java`: **`INSTALLED_REPLAY_CONTEXT`**, declaration 47; accesses: 216 get, 223 get, 463 get, 472 set, 497 remove, 499 set, 531 get.
- `core/http/client/src/main/java/org/eclipse/rdf4j/http/client/QueryExplanationRequestContext.java`: **`CURRENT`**, declaration 22; accesses: 34 get, 36 set, 38 get, 43 remove, 45 set, 51 get.
- `core/http/client/src/main/java/org/eclipse/rdf4j/http/client/QueryRequestContext.java`: **`CURRENT`**, declaration 22; accesses: 33 get, 35 set, 37 get, 42 remove, 44 set, 50 get.
- `core/model-api/src/main/java/org/eclipse/rdf4j/model/base/AbstractLiteral.java`: **`DATATYPE_FACTORY`**, declaration 966; accesses: 1002 get, 1011 get.
- `core/query/src/main/java/org/eclipse/rdf4j/query/explanation/QueryExplanationContext.java`: **`CURRENT`**, declaration 29; accesses: 38 get, 45 set, 57 get, 64 get, 78 get, 100 get, 105 get, 112 remove, 114 set.
- `core/query/src/main/java/org/eclipse/rdf4j/query/impl/BlankNodeAllocationScope.java`: **`ACTIVE`**, declaration 45; accesses: 106 get, 112 get, 113 set, 130 remove, 132 set.
- `core/query/src/main/java/org/eclipse/rdf4j/query/impl/BlankNodeAllocationScope.java`: **`currentSolution`**, declaration 59; accesses: 368 get, 369 set, 388 get, 389 set, 406 remove, 408 set, 429 get, 451 get.
- `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/QueryContext.java`: **`queryContext`**, declaration 29; accesses: 32 get, 47 get, 48 set, 64 remove, 66 set.
- `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/function/math/DecimalFormat.java`: **`FORMATS`**, declaration 35; accesses: 72 get.
- `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/impl/DefaultEvaluationStrategy.java`: **`compilationDepth`**, declaration 235; accesses: 663 get, 672 remove, 1526 get, 1535 remove.
- `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/impl/evaluationsteps/AdaptiveFilterEvaluationStep.java`: **`CURRENT`**, declaration 87; accesses: 328 get, 1033 get, 1034 set, 1043 set, 1124 get, 1148 get.
- `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/iterator/AsyncIteratorWorker.java`: **`workerThread`**, declaration 24; accesses: 35 get, 36 set, 41 remove, 43 set, 108 get, 111 remove.
- `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/QueryModelNormalizerOptimizer.java`: **`CURRENT_ANALYSIS`**, declaration 58; accesses: 66 get, 67 set, 73 remove, 75 set, 282 get, 304 get, 504 get.
- `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/sketch/SketchBasedJoinEstimator.java`: **`optimizationScope`**, declaration 1280; accesses: 1494 get, 1497 set, 1512 remove, 1519 get, 1675 get, 4297 get, 4320 get, 4410 get, 5008 get, 5944 get, 5959 get, 7220 get, 7246 get, 7265 get, 7416 get.
- `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/util/QueryEvaluationUtil.java`: **`CALENDAR_VALUE_CACHE`**, declaration 57; accesses: 538 get.
- `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/util/QueryEvaluationUtility.java`: **`ACTIVE_SAFETY_SNAPSHOT`**, declaration 204; accesses: 214 get, 252 get, 258 set, 262 remove, 283 get, 290 set, 294 remove, 296 set, 308 get, 315 set, 319 set, 347 get, 371 get, 378 get, 635 get.
- `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/util/ValueComparator.java`: **`CALENDAR_SORT_KEY_CACHE`**, declaration 44; accesses: 167 get.
- `core/queryalgebra/geosparql/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/function/geosparql/ext/GeoSupport.java`: **`READER`**, declaration 59; accesses: 98 get.
- `core/queryparser/api/src/main/java/org/eclipse/rdf4j/query/impl/AbstractParserQuery.java`: **`LEGACY_HOOK_OWNER`**, declaration 34; accesses: 67 get, 141 get, 142 set, 147 remove, 149 set.
- `core/sail/api/src/main/java/org/eclipse/rdf4j/sail/helpers/AbstractSailConnection.java`: **`operationThread`**, declaration 120; accesses: 257 get, 313 get, 392 get, 413 get, 423 get, 450 get, 613 get.
- `core/sail/api/src/main/java/org/eclipse/rdf4j/sail/helpers/SlowQueryContextHolder.java`: **`CONTEXT`**, declaration 64; accesses: 70 get, 85 get, 86 set, 92 remove, 94 set.
- `core/sail/base/src/main/java/org/eclipse/rdf4j/sail/base/Changeset.java`: **`continuationReadLocks`**, declaration 2634; accesses: 2637 get, 2641 get, 2649 remove, 2654 get.
- `core/sail/base/src/main/java/org/eclipse/rdf4j/sail/base/SailDatasetSnapshot.java`: **`CURRENT`**, declaration 32; accesses: 39 get, 126 get, 128 set, 138 remove, 140 set.
- `core/sail/base/src/main/java/org/eclipse/rdf4j/sail/base/SailModelCleanup.java`: **`CURRENT`**, declaration 20; accesses: 26 get, 34 get, 42 get, 47 get, 52 get, 57 get, 247 get.
- `core/sail/base/src/main/java/org/eclipse/rdf4j/sail/base/SailWriteContinuation.java`: **`SCOPES`**, declaration 30; accesses: 54 get, 57 set, 65 get, 71 get, 190 get.
- `core/sail/base/src/main/java/org/eclipse/rdf4j/sail/base/StreamingWritePreflight.java`: **`CURRENT`**, declaration 39; accesses: 55 get, 64 get, 70 get, 77 get, 82 get, 104 get, 131 get, 134 get, 167 get, 212 set, 334 get, 357 get, 396 get, 421 get, 445 get, 454 get, 802 set.
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbDirectAdjacencyStore.java`: **`logicalCommitBatchDepth`**, declaration 157; accesses: 201 get, 394 get, 405 get, 411 set, 411 get, 416 get, 418 remove, 421 set.
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbEvaluationStatistics.java`: **`optimizationCostScope`**, declaration 77; accesses: 166 get, 169 set, 182 get, 188 remove, 614 get.
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbSailStore.java`: **`callbackAdmission`**, declaration 625; accesses: 722 get, 2156 get, 2158 set, 2168 remove, 2170 set, 7291 get.
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbSailStore.java`: **`replayRetirementOwner`**, declaration 2639; accesses: 884 get, 5170 get, 5171 set, 5176 remove, 5178 set, 6587 get, 7292 get.
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbSailStore.java`: **`noneMutationReadOwner`**, declaration 4160; accesses: 4165 get, 4166 set, 4167 set, 7292 get, 10167 get, 10175 get.
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbSailStore.java`: **`publicationContext`**, declaration 4248; accesses: 3132 get, 3197 get, 4527 get, 4581 set, 5360 get, 5655 get, 5659 get, 5717 get, 5830 get, 5837 get, 5948 get, 6017 get, 6069 get, 6244 get, 6272 get, 6325 set, 6330 get, 6382 set, 6490 get, 6495 get, 6503 get, 6511 get, 7293 get, 7654 get, 7668 get, 7700 get, 7783 get, 7890 get, 7979 get, 9003 get, 9426 get, 10435 get, 10664 get, 10705 get.
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbSailStore.java`: **`activeReadView`**, declaration 4249; accesses: 766 get, 1024 get, 4851 get, 4853 remove, 4855 set, 4945 get, 4964 set, 4968 get, 4970 remove, 4972 set, 4996 get, 5011 get, 5046 set, 5059 get, 5061 remove, 5063 set, 5086 get, 5088 remove, 5090 set, 5115 get, 5128 set, 6730 get, 7283 get, 15184 get, 15272 get.
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbSailStore.java`: **`activeReadResourceContext`**, declaration 4250; accesses: 4891 get, 4896 remove, 4898 set, 4904 get, 4907 set, 4912 get, 7284 get.
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbSailStore.java`: **`activeWriterOwner`**, declaration 4306; accesses: 4463 get, 4464 set, 4473 set, 6284 get, 6342 get, 6491 get.
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbStartupDiagnostics.java`: **`OBSERVER`**, declaration 28; accesses: 34 get.
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbStoreConnection.java`: **`internalReadScopeDepth`**, declaration 165; accesses: 795 get, 1389 get, 1535 get, 1970 get, 2612 get, 2656 get, 2657 set, 2666 remove, 2668 set, 2872 get, 2883 set, 2895 remove, 2897 set, 3498 get, 3504 set, 3541 remove, 3543 set.
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbStoreConnection.java`: **`namespaceReadDepth`**, declaration 167; accesses: 715 get, 785 set, 785 get, 789 set, 789 get, 896 set, 896 get, 900 set, 900 get.
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbStoreConnection.java`: **`activeReadExposureScope`**, declaration 168; accesses: 733 get, 940 get, 1333 get, 1374 get, 1779 get, 1789 get, 1803 set, 1824 get, 1826 remove, 1828 set, 1870 get, 1911 get, 1923 remove, 1925 set, 2616 get, 2619 get.
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbStoreConnection.java`: **`pendingEvaluationReplayPlan`**, declaration 169; accesses: 816 get, 820 remove, 1051 set.
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbStoreConnection.java`: **`explainTupleExprCapture`**, declaration 170; accesses: 333 get, 335 set, 343 remove, 345 set, 352 get, 997 get.
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbStoreConnection.java`: **`readThreadRegistration`**, declaration 177; accesses: 1616 get, 1634 get, 2972 get, 2994 get, 3270 get, 3474 get.
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/RepositoryValueIteration.java`: **`SOURCE_PULL`**, declaration 48; accesses: 182 get, 186 get, 187 set, 196 get, 197 set, 207 set.
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/TripleStore.java`: **`spoExistenceCursor`**, declaration 328; accesses: 4182 get, 4288 remove.
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/TripleStore.java`: **`preparedSortWorkspace`**, declaration 565; accesses: 1417 get, 1437 get, 1769 get, 1812 get.
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/TripleStore.java`: **`perThread`**, declaration 4305; accesses: 4324 get, 4368 get.
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/ValueResolutionScope.java`: **`ACTIVE`**, declaration 28; accesses: 60 get, 110 get, 111 set, 113 set, 117 get, 121 set, 123 set.
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/ValueStore.java`: **`safeAllocationResize`**, declaration 1125; accesses: 7424 get, 7458 set, 7462 set.
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/ValueStore.java`: **`safeRetirementReservationResize`**, declaration 1126; accesses: 7428 get, 13578 set, 13587 remove.
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/ValueStore.java`: **`standaloneWriterAdmissions`**, declaration 1167; accesses: 3771 get, 3773 set, 3793 set, 3801 get, 3803 remove, 3805 set, 7841 get, 7862 get.
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/ValueStore.java`: **`readContext`**, declaration 1255; accesses: 4614 get, 4980 get, 5041 get, 5344 get, 5470 get, 5471 get, 5497 get, 5532 get, 7267 get, 7336 get, 7386 get, 7756 get, 11793 get, 11898 get, 12337 get, 15539 get, 16128 get, 16171 get.
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/WriterTransaction.java`: **`invocation`**, declaration 24; accesses: 41 get, 51 get, 52 set, 56 set.
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/bulk/BulkTaskScheduler.java`: **`CURRENT_SCHEDULER`**, declaration 44; accesses: 74 get, 115 get, 225 get, 297 get, 331 get, 498 set, 502 remove, 594 get, 639 get, 678 get, 764 get, 823 get, 920 get, 1117 get, 1571 get.
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/bulk/BulkTaskScheduler.java`: **`CURRENT_FILE_DESCRIPTORS`**, declaration 45; accesses: 72 get, 218 get, 233 set, 256 get, 265 set, 277 get, 310 get, 342 get, 364 get, 456 get, 462 set, 467 get, 480 get, 503 remove, 1790 get, 1802 remove, 1804 set, 1858 get, 2251 get, 2252 set, 2259 remove, 2261 set.
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/bulk/BulkTaskScheduler.java`: **`TASK_DEPTH`**, declaration 46; accesses: 504 remove, 1554 get, 1558 set, 1558 get, 1562 get, 1563 set.
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/bulk/ParallelStager.java`: **`shard`**, declaration 119; accesses: 200 get.
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbFusedSipFactorizedRuntime.java`: **`CURRENT`**, declaration 36; accesses: 53 get, 66 get, 73 get, 77 get, 82 get, 100 set, 115 remove, 117 set.
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativeAggregateCompiler.java`: **`SEMANTIC_SUBQUERY_COMPILE_STACK`**, declaration 46; accesses: 187 get, 198 remove.
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativeAttemptMetrics.java`: **`DECLINE_CAPTURE`**, declaration 246; accesses: 273 get, 275 remove, 277 set, 287 get, 310 get.
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativeAttemptMetrics.java`: **`DECLINE_ROUTE`**, declaration 247; accesses: 254 get, 255 set, 260 remove, 262 set, 283 get.
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativeDispatchLoop.java`: **`ACTIVE_FORCING`**, declaration 369; accesses: 373 get, 377 get, 378 set, 382 set.
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativeEvaluationStrategy.java`: **`KERNEL_SUBPLAN`**, declaration 66; accesses: 234 get, 246 get, 262 get, 263 set, 269 set, 439 get.
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativeEvaluationStrategy.java`: **`scopedValuePositions`**, declaration 81; accesses: 86 get, 94 get, 95 set, 100 remove, 102 set, 108 get, 109 set.
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativeEvaluationStrategy.java`: **`scopedUniformValues`**, declaration 83; accesses: 418 get, 424 remove, 431 set, 445 get, 446 set, 451 remove, 453 set.
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativeParallelAggregation.java`: **`LAST_STRATEGY_LABEL`**, declaration 338; accesses: 285 set, 289 set, 341 get, 342 remove.
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativeProbeDeadline.java`: **`CURRENT`**, declaration 42; accesses: 61 get, 62 set, 174 get, 175 set, 180 get, 191 get, 211 get, 230 get, 251 get, 321 remove, 323 set.
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativeProbeDeadline.java`: **`OWNED_ATTEMPTS`**, declaration 43; accesses: 63 get, 72 get, 73 set, 139 remove, 141 set.
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativeTypeMatrix.java`: **`REUSABLE`**, declaration 2595; accesses: 2633 get.
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativeValueCodec.java`: **`UTF8_SCRATCH`**, declaration 57; accesses: 594 get, 601 set.
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbSyntheticValueSource.java`: **`ACTIVE_EVALUATION`**, declaration 55; accesses: 111 get, 126 get, 159 get, 170 get, 185 get, 261 get, 263 set, 270 get, 288 remove, 290 set.
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/NativeCancellationToken.java`: **`CURRENT`**, declaration 24; accesses: 63 get, 72 get, 76 set, 95 remove, 97 set, 195 get, 204 get.
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/NativeExecutionContext.java`: **`BORROWED_PRODUCER_OWNERS`**, declaration 66; accesses: 74 get, 141 get, 158 set, 167 remove, 169 set, 1017 get.
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/NativeExecutionContext.java`: **`current`**, declaration 182; accesses: 213 get, 214 set, 219 get, 228 get, 249 get, 342 remove, 344 set.
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/NativeExecutionContext.java`: **`initializingNativeStates`**, declaration 395; accesses: 656 get, 720 remove.
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/NativeExecutionPhaseEvent.java`: **`CURRENT_EVALUATION`**, declaration 32; accesses: 245 get, 249 set, 275 remove, 277 set, 290 get, 300 set, 398 remove, 400 set.
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/NativeExecutionPhaseEvent.java`: **`CURRENT_CANCELLATION`**, declaration 33; accesses: 67 get, 85 get, 246 get, 250 set, 291 get, 301 set, 308 remove, 310 set.
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/NativeExecutionPhaseEvent.java`: **`CURRENT_INPUT`**, declaration 34; accesses: 111 get, 122 get, 133 get, 187 get, 248 set, 270 remove, 272 set.
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/NativeGraphFusion.java`: **`SEMANTIC_BOUNDARIES`**, declaration 40; accesses: 49 get, 174 get, 175 set, 180 remove, 182 set.
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/inlined/Dates.java`: **`DATATYPE_FACTORY`**, declaration 90; accesses: 62 get, 281 get.
- `core/sail/nativerdf/src/main/java/org/eclipse/rdf4j/sail/nativerdf/ValueStore.java`: **`walPendingLsn`**, declaration 146; accesses: 1164 get, 1168 set, 1310 get, 1312 set.
- `core/sail/nativerdf/src/main/java/org/eclipse/rdf4j/sail/nativerdf/ValueStore.java`: **`CRC32C_HOLDER`**, declaration 1381; accesses: 1357 get.
- `tools/federation/src/main/java/org/eclipse/rdf4j/federated/monitoring/QueryPlanLog.java`: **`queryPlan`**, declaration 28; accesses: 31 get, 35 set.

## ThreadLocalRandom appendix (distinct mechanism)

- `core/model-api/src/main/java/org/eclipse/rdf4j/model/base/AbstractValueFactory.java:61`: `private final AtomicLong nodeID = new AtomicLong(ThreadLocalRandom.current().nextLong());`
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/AdaptiveSlotCache.java:146`: `if (!(c instanceof IsolatedHotCell) && ThreadLocalRandom.current().nextInt(64) == 0) {`
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/AdaptiveSlotCache.java:154`: `int index = firstEmpty >= 0 ? firstEmpty : base + ThreadLocalRandom.current().nextInt(4) * wayStride;`
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/AdaptiveSlotCache.java:193`: `int index = firstEmpty >= 0 ? firstEmpty : base + ThreadLocalRandom.current().nextInt(4) * wayStride;`
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/CompactSlotCache.java:115`: `int index = firstEmpty >= 0 ? firstEmpty : base + ThreadLocalRandom.current().nextInt(4) * wayStride;`
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbFilterSelectivityStats.java:602`: `int replacementIndex = ThreadLocalRandom.current().nextInt(eligibleRows);`
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/MemoizingSlotCache.java:137`: `int index = firstEmpty >= 0 ? firstEmpty : base + ThreadLocalRandom.current().nextInt(4) * wayStride;`
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/MemoizingSlotCache.java:175`: `int index = firstEmpty >= 0 ? firstEmpty : base + ThreadLocalRandom.current().nextInt(4) * wayStride;`
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/SlotCasCache.java:118`: `int index = firstEmpty >= 0 ? firstEmpty : base + ThreadLocalRandom.current().nextInt(4) * wayStride;`
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/SlotCellCache.java:118`: `int index = firstEmpty >= 0 ? firstEmpty : base + ThreadLocalRandom.current().nextInt(4) * wayStride;`
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/SlotSnapshotCache.java:115`: `int index = firstEmpty >= 0 ? firstEmpty : base + ThreadLocalRandom.current().nextInt(4) * wayStride;`
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativeCostObservation.java:42`: `this(learner, key, expectedNanos, System::nanoTime, ThreadLocalRandom.current().nextLong());`
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/NativeExpressionDomain.java:352`: `ThreadLocalRandom.current().nextLong());`
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/NativeExpressionDomain.java:398`: `this(width, ledger, cancellation, spillFiles, ThreadLocalRandom.current().nextLong());`
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/NativeRuntimeValueSpill.java:43`: `private final long seed = ThreadLocalRandom.current().nextLong();`
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/NativeRuntimeValueTable.java:887`: `final long seed = ThreadLocalRandom.current().nextLong();`
