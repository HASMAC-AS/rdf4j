# Next ThreadLocal candidates: source cancellation and dictionary context

Read-only design review, 2026-11-18. Sites refer to the current `ba74f478`-derived production source plus the narrow co-scan checkpoint change. No percentage improvement is inferred. This is a candidate design, not implemented or validated behavior.

## 1. Native source cancellation is not owned by the physical cursor today

### Actual chain and cadence

- `evaluation/NativeLmdbQuerySource.java:48-51`, `checkpointCancellation()`: checks **native query first**, then speculative probe (`poll(0)`). Both currently resolve ambient signals. This priority differs from type-matrix's probe-first helper; do not reuse the new co-scan `poll` blindly.
- `LmdbRecordIterator.java:318-437` (`next`) and `446-562` (`fill`) hold the transaction read lock, synchronize on `txnRef`, validate the snapshot, then use three helpers at `855-868`.
- Entry polling happens when `cancellationPollTick == 0`, not unconditionally on every pull. Candidate polling increments the same tick for **visited native candidates**, including rejected/filtered keys, and checks every 1024. Exhaustion checks unconditionally. Exact-key success and absence have their own candidate/exhaustion paths. The tick survives next/fill calls and is reset on retained logical reopen.
- `TripleStore.java:3981-3991`, `exactIndexCancellation()`: supplier calls `QueryExecutionContext.checkpointReplaySafepoint()`, then native query/probe checks, then checks current-thread interruption and throws `QueryInterruptedException`. This supplier is passed to exact index count/prefix traversal; its invocation count is controlled by estimator traversal, not necessarily one per output row. Preserve replay and interrupt ordering as well as cancellation.

### Physical reuse and concrete counterexample to constructor capture

`TripleStore` opens ordinary/range scans at `4623-4672`, retained iterators at `4680-4688`, and re-aims a retained iterator at `4694-4700`. `LmdbRecordIterator.reset`, beginning at `722`, clears source counters and cancellation tick at `739-742`, changes the pattern/index/bounds, but deliberately does **not** refresh transaction version. Renewal/replay must still validate it. Cursor handles and buffers can survive logical close through `retainOnClose`; `dispose` owns their release.

Two source implementations retain probes: `LmdbSailStore.java:14486-14542` and `16118-16192`; their `NativeProbe.open` closes the previous selection, may choose direct adjacency or LMDB, then creates or resets the retained iterator. These are logical probe boundaries, not necessarily evaluation boundaries. Speculative arbitration may change the effective child/deadline between opens.

Existing test `LmdbRecordIteratorFilteredKeyCancellationTest.retainedResetStartsANewCancellationPollBudget` is an explicit counterexample: create, consume, close and reset the iterator **outside** a cancellation scope, then install an already-cancelled token only around `iterator.next()`. Correct behavior checks it before another native key visit. The parameterized NEXT/FILL filtered-key test likewise creates the iterator before installing its query/probe signal. Capturing once in constructor **or reset** would miss these supported cases.

### Safe direction, in stages

1. Do not change default `RecordIterator` behavior to lifetime capture. The generic API must keep ambient-per-poll semantics unless an explicit owner is supplied.
2. Introduce an explicitly owned source-checkpoint capability at the evaluation/native boundary, with a query-first check and the correct probe reference. The current native token is package-private; a cross-package storage API needs an internal interface/factory, not an unsafe cast or exposure of mutable ThreadLocal internals.
3. `SyntheticValueSource.executionContext()` (`evaluation/LmdbSyntheticValueSource.java:296`) exposes the evaluation context; `NativeExecutionContext.cancellation()` (`436`) owns a root token. This is **not sufficient by itself**: `NativeCancellationToken.forSource` intentionally preserves a matching ambient local child, which can stop a pipeline without cancelling its parent. Probe deadlines belong to attempts and can differ from the query's lifetime. Capture those at an authoritative execution boundary, not on a shared source/plan.
4. Pass the explicit checkpoint through an opt-in native statement/probe opening path. Rebind it on each retained logical open/reset. Keep ordinary callers on the ambient fallback. Absent explicit signals must mean “resolve effective boundary context”, not silently disable cancellation; `inherit(null)` is a no-op.
5. A cheaper local alternative is capture once **per next/fill invocation**, replacing repeated ambient lookup within that invocation only. This preserves installing a scope after iterator creation, but is not automatically beneficial: a one-row next currently often executes *zero* signal lookups, while eager per-pull capture adds two. It is potentially useful for a long filtering scan or large fill. Also verify callbacks cannot intentionally rebind scope for the remainder of a synchronous pull. Do not adopt it without dynamic lookup counts and matched next/fill workloads.
6. For `exactIndexCancellation`, explicitly capture native signals only on a certified owned path. Keep `checkpointReplaySafepoint()` semantics, including replay/map growth and different-thread ownership. A cursor callback used later under a new scope must not blindly retain creation-thread replay state. `QueryExecutionContext` already has a thread-aware ReplayCheckpoint facility; use its actual contract rather than replacing it with a BooleanSupplier snapshot.

### Required tests and measurements before implementation

- Existing real-LMDB filtered-key NEXT/FILL matrix; cancellation and probe expiry after first filtered key, no output required, provenance and cleanup intact.
- Existing retained reset test unchanged; additionally switch A -> B query token and A -> B speculative probe across logical opens, without replacing the physical cursor. Old cancelled A must not cancel B; newly cancelled B must stop before next key visit when initial tick is zero.
- Create outside scope, consume inside; create inside A, consume under B; explicit-owner path under conflicting caller B. Document which path intentionally owns A and which remains ambient.
- Every 1024 candidate cadence unchanged across partial fills, exact-key success/absence, all-filtered scans, exhaustion, repeated calls after close, seekForward and reset.
- Parent versus child-local cancellation, null inherited worker scopes with pre-existing ambient state, pool reuse, cross-thread worker dispatch and scope restoration after exceptions.
- Map resize/replay renewal with correct read lease and prefix bounds; close/commit/shutdown while cancellation is observed; retained cursor dispose only once. Do not trade away snapshot validation or source locks.
- Exact-prefix/count traversal: query versus probe competing signals, replay safepoint precedes both, worker interrupt attribution, disposal after each failure class.
- Separate tiny exact-key probe, long unfiltered scan, long filtered scan returning zero, batched fill, and adjacency-routed controls. Report actual checkpoint/get counts and polling interval, not only returned-row throughput. Native checkpoint changes do not optimize the independent regular-engine CancellationSignal automatically.

## 2. Highest-frequency plausible non-cancellation candidate: explicit dictionary read/exposure context

Frequency below is code-path potential, not measured rank. Value materialization scales with returned bindings and misses rather than the masked 1024-candidate cadence, so it deserves profiling ahead of more tiny loop helpers.

### ValueStore borrowed ReadContext

`ValueStore.java:1240-1255` already consolidates several locals in one never-removed, per-thread mutable `ReadContext`: borrowed transaction/revision, read-lock ownership, retained decode, writer capture, overlay materializer state and LWJGL MemoryStack. The object is **thread-confined**; its fields are temporarily replaced/restored during nested borrowed reads. Never store this holder on a shared query source, lazy value, retained reader or reusable plan, and never transfer it to a worker.

Concrete redundant lookup sites:
- `withValueReadTransaction` (`5465-5524`) resolves `readContext.get()` at `5470`, `5471`, `5497`; `withBorrowedReadContext` wrapper (`5529-5533`) resolves it again. A single local **holder reference**, passed down synchronous helpers, can avoid these gets while still rereading its mutable fields where existing code does. Do not snapshot borrowedTxn across callback/lock transitions without revalidating.
- `resolveWithRetainedReader` (`5338-5358`) already gets the holder once at `5344` and passes it into the overload: useful existing precedent.
- `readTransaction` (`11791-11810`) gets the holder once at `11795`; its fallback calls `withValueReadTransaction`, which looks it up again. An explicit synchronous overload could carry that same holder. The writer-owner early branch remains separate.
- Other current gets at `4980`, `5041`, `7267`, `7336`, `7386`, `7758`, `11900`, `12339`, `15541`, `16130`, `16173` have differing call paths. Do not globally replace them without tracing the relevant branch and ownership.

The low-risk starting experiment is deduplicating same-call-chain **holder** gets, without removing any read barrier, lease, validation, writer admission or callback restore. Measure overlay hit, retained-reader miss, ordinary dictionary decode and nested RDF-star cases separately. Inlining/CSE may already remove some cost, so require profile/assembly or dynamic counts plus latency.

### ValueResolutionScope / explicit exposure

`ValueResolutionScope.java:59-62` reads ACTIVE and checks matching ValueStore; `current` additionally rejects closed scopes, whereas `activeExposure` intentionally returns a **closed still-installed** scope to delimit internal access. Those are not interchangeable.

`ValueStore.getLazyValue(id, positionMask)` (`6871-6895`) resolves exposure once at `6876`, then passes it down. `getLazyValue(id, revision)` (`6976-6978`) performs another ambient lookup; an explicit exposure overload already exists at `6990` for a validated paired snapshot. `scopedRevision` at `5359-5364` also resolves ambient exposure. There is potential to propagate an already validated exposure along native materialization calls, but **null is not permission to pretend a closed installed scope is absent**. Preserve store identity, generation, revision/role masks and exposure/export boundary rules.

Required tests: nested same-store and different-store scopes; closed-but-installed exposure; callback restoration; detached/escaped values after result close; retained dictionary reader and map growth; concurrent close and worker propagation; writer callback borrowing; language/datatype/RDF-star recursive decoding; memo hit/miss with old and refreshed revision. Reuse existing lifecycle/replay suites and add a route assertion showing the changed decode path actually executed.

## Recommendation

First profile/count the non-cancellation holder/exposure chains on the real value-heavy regular/interpreted/Janino routes. Pursue small explicit synchronous holder propagation if it is hot. Design source-checkpoint ownership separately as an opt-in path, because constructor/reset capture is already contradicted by tests. Neither recommendation is a speedup claim, and neither replaces the full all-ThreadLocal inventory.

## 3. Concrete per-row non-cancellation boundary candidate: SOURCE_PULL

Parent suggested this candidate; source inspection confirms it. In current `RepositoryValueIteration.java` the method is named **prepareNext** (`130-178`). For a new ready row it invokes `sourceHasNext()` (`140`), checks `closed`, handles exhaustion/close, then invokes `sourceNext()` inside `extractor.snapshot(...)` (`150`). Each helper (`185-207`) independently performs SOURCE_PULL.get, set(this), and finally set(previous). Thus a successful new row currently has **two gets and four sets** for these two boundaries (excluding nested source work). Already buffered `rowReady` returns before these operations; exhaustion performs only the hasNext boundary.

Candidate: one lexical source-pull scope spanning only source.hasNext, the existing closed guard, and source.next for an available row. Restore before `extractor.snapshot`, owner extraction, resolution waves, sourceEnded handling or close. This can remove one get and two sets per successfully fetched row without removing cancellation polls or changing value-wave batching. Do not introduce a wrapper/result allocation per row merely to return both availability and row. Keep scope restoration in finally on all exceptional and early-return branches.

Semantic constraints:
- `isPullingSource` (`181-183`) requires both !closed and SOURCE_PULL identity. Only source callbacks should observe this owner. `extractor.snapshot` currently runs **after sourceNext's finally**, so broad wrapping of the whole prepareNext would change ownership and is unsafe.
- The intermediate closed check must remain before source.next. When hasNext is false, set sourceEnded and close after restoration, not under SOURCE_PULL.
- Nested repository iterations must restore the outer owner after their own callbacks. HasNext may run arbitrary functions or block; preserve exception and async-close handling and do not prefetch another row.
- Preserve source.next -> snapshot ordering when close races during next; do not silently add/reorder exposure work because local restructuring makes that tempting.

Tests: nested outer/inner source hasNext and next ownership; hasNext false and throwing paths; next throws; async close while hasNext blocks, ensuring next is not called; async close during next; source close callbacks see restored ambient owner; snapshot callback sees restored outer/null owner; lazy RDF-star/composite values resolved before escape; buffered repeated hasNext does not fetch again; early stop/close cleans readers. Parent's existing repository partial-ready/grouped-wave and lifecycle suites are starting coverage, not proof that a new merged boundary was hit. Capture dynamic SOURCE_PULL get/set counts separately from latency forks. Do not implement before profile prioritization.
