# Next all-ThreadLocal candidate: synchronous ValueStore holder propagation

Read-only triage. Established comparison remains depth0750336 versus baseline5ac89b5; archival06d865f2 is not a new performance winner. No builds, probes, source edits or speed claims. Unproven null-restoration and slower direct-record cancellation are not premises for this proposal.

## Recommendation and confidence

Recommend a bounded **same-thread ReadContext holder reuse** experiment across `ValueStore.readTransaction -> withValueReadTransaction -> withBorrowedReadContext`, preserving every admission/revision/lock operation and rereading mutable fields at their original points. Start with the three direct gets inside withValueReadTransaction becoming one local holder; then use the already existing explicit-holder borrowed-context overload. This does not introduce a new long-lived context, new ThreadLocal slot, or per-row lease.

This is the strongest *bounded semantic* candidate found here, not the strongest measured bottleneck. Current broad/probe JFR summaries have only isolated ValueStore.isWriteTransactionOwner nearest-TL samples (one in broad regular B0 and one in probes interpreted B0), versus recurring fused attach samples. Consequently this should first qualify a value-decode-heavy actual route, not be sold as a universal next query win. If ordinary query routes remain memo/overlay hits and bypass transaction setup, reject it as low impact rather than tuning a microbenchmark alone.

## Existing optimized baseline

ValueStore already owns a persistent per-store/thread ReadContext (`1243–1255`), replacing several separate ThreadLocals. Fields include borrowedTxn, borrowedRevision, hasReadLock, retainedDictionaryDecode, writerCapture, overlay materializer/busy and MemoryStack. It is never removed. `withBorrowedReadContext(ReadContext,...)` already exists and saves/restores transaction/revision/read-lock/stack in finally. MemoryStack is already reused from the holder, avoiding repeated stackGet in nested borrowed contexts.

`readTransaction` already has a fast borrowed path: one context get after its writer check, direct transaction.exec with reused stack. Do not claim a gain there merely by rewriting method signatures. Overlay materializer is already reusable/reentrancy-guarded; batch deduplication, memo, source-pull consolidation and depth folding are separate established code, not fresh opportunities to count again.

## Exact candidate path and static counts

ValueStore current sites:
- `withValueReadTransaction` ~5465: ensure requested association readable, get borrowedTxn, get writerCapture separately, writer admission/ownership checks, then get borrowedTxn again after the writer path. The second read of borrowedTxn is semantically intentional, even if repeated holder lookup is not.
- `withScopedValueReadContext` ~5521 validates native transaction/revision association then calls borrowed-context helper.
- Default `withBorrowedReadContext` ~5529 gets the holder and delegates to the existing explicit-holder overload ~5535.
- `readTransaction` ~11801 calls writer-owner predicate, then captures holder, uses borrowed transaction/stack if present, otherwise calls withValueReadTransaction.
- `isWriteTransactionOwner` ~16139 returns early when volatile writeTxn is0; only with a writer does it obtain borrowedTxn, reread writeTxn, and test owner/current thread. Preserve both writer reads and nonzero protection against ownership races.

For the ordinary **no writer, no borrowed transaction** entry into readTransaction, excluding nested admission/revision/callback work:
1 get in readTransaction +3 direct gets in withValueReadTransaction +1 in default borrowed helper = **5 relevant gets** before callback execution. A local-only consolidation reduces that to3. Passing the known holder to existing borrowed overload reduces to2. A subsequent proven entry-to-helper overload can reduce to1. These are static route counts, not all-query measurements; extra context reads elsewhere remain.

For direct withValueReadTransaction writerCapture early-return, its first2 gets can become1. For ordinary borrowed fast readTransaction with no writer, the baseline already uses1, so zero reduction. With an active writer additional isWriteTransactionOwner gets occur; quantify the actual branch before counting them. Avoid a broad helper conversion that eagerly initializes context on a path where writeTxn==0 currently returns without a lookup.

## Scope of an initial patch

1. Capture ReadContext once only after current ensureAssociationReadable returns; keep validation order.
2. Read context.borrowedTxn / writerCapture at the exact original control-flow points. **Cache holder identity, not field values across native admission, locks, callbacks, waits or retries.**
3. Use existing explicit-holder overload for synchronous borrowed setup. If extending the scoped helper to accept it, verify callback thread confinement first. TxnManager.doWithPriority directly acquires its permit and calls doWithScoped, which opens scopes/read barrier and invokes transaction.exec on the same call stack. It does not dispatch a worker.
4. Leave public/default ambient entry helpers for callers lacking a validated holder. Do not store ReadContext in a query plan, value shell, iterator, worker task, transaction lease or replay object.
5. Do not initially change isWriteTransactionOwner or the overlay decode chain. Those are separate optional refinements once first route is measured.

No new wrapper allocation is required. Passing a holder through an existing callback lambda can add one captured reference and affect escape/layout; measure rather than assuming it is free. The safest initial local-only patch changes no closure captures.

## Semantic hazards to preserve

- Same-store nested callbacks mutate the shared holder and must restore it; a local holder remains valid, but cached borrowedTxn/revision flags may become stale during callbacks. Different stores have different holders.
- Writer association gate and writerAdmission ordering cannot be removed. Owner identity and writeTxn can change from another thread; preserve current rereads and checks.
- Read barrier ownership, revision compatibility, protected-ID horizon, snapshot validation, map-growth retry and retained dictionary reader fallbacks remain intact.
- Closed-but-installed ValueResolutionScope is intentionally different from absent scope. This proposal does not propagate or elide exposure lookups.
- Recursive namespace/datatype/RDF-star decoding and overlay reentrancy use mutable context flags. No extension across asynchronous worker dispatch or escaping close.
- `ReadContext` never removed/replaced is a necessary identity-stability proof; retain a source audit of all get/set/remove sites. If future code introduces replacement, explicit synchronous helper contract must be revisited.

## Tests, hit proof and workload

Baseline semantic tests before edits; new test cases should exercise borrowed-context nesting/failure restore, same and different ValueStores, writerCapture and writer-owner fallback, no borrowed fresh reader, current revision mismatch, worker thread reuse, nested datatype/namespace/triple decoding, and map-growth/close while native admission is pending.

Relevant existing files include ValueStoreWriterReadLifetimeTest, ValueStoreWriterGuardTest, ValueStoreDetachedSnapshotReadTest, ValueStoreNativeValueCodecTest, ValueStoreOverlayMaterializationTest, ValueStoreRevisionTest, LmdbBorrowedResultWorkerTest and replay/registration lifecycle suites. Select concrete tests by source before executing; file existence is not proof every branch is covered.

Untimed-only route proof must show fresh-reader/helper entries versus borrowed fast path, memo/overlay hit/miss, and actual context gets or bytecode deltas. A getter-count test alone cannot establish end-to-end importance. Use a diagnostic-only layer, not atomic per-value counters in timing.

Benchmark narrow materialization-heavy queries with unique IRIs, custom-datatype literals, languages and nested triples, force lexical consumption and compare checksums. Include repeated values as a negative-control memo-hit case. Compare regular/interpreted/Janino, stable dictionaries and retained snapshot variants; distinguish deliberately disabled-overlay diagnostic from the user's best normal flags. Report first-row/full latency and allocations. Benchmark old/new bytecode with identical memory budget/no-spill state; don't conflate removing spill or changing cache population with holder reuse.

## Fused runtime attach investigation

Current profile nearest-TL samples recur in LmdbFusedSipFactorizedRuntime.attach: broad interpreted A/B0 6/7, Janino A/B0 5/7; probes interpreted A/B0 5/8. These are samples, not invocation counts or saved time.

`LmdbNativeRowStep` attaches fusedSession around row.advance (~3106), plus initialization/open (~3202) and other run paths (~828,2805,3297). One actual uncached advance attachment currently does get+set and close remove/set, plus a Scope source allocation. Cached next availability can return before attach. Session holds query budget, telemetry, immutable SIP domains and worker-confined mutable batches, so it is not a no-op mere logging scope.

Do not simply skip attach(null): null is converted to NOOP specifically to shadow any ambient parent. Do not leave attachment across public pulls or share mutable worker sessions. `inherit` deliberately creates a child with shared immutable/root accounting but confined mutable batches. Scope.close may execute on another thread and currently restores the closer's ThreadLocal; a captured-holder shortcut changes association.

Generated `LmdbFusedKernelRuntime` APIs resolve current() at several per-operation/domain/batch/telemetry sites. Explicitly threading the already-owned session into generated code is potentially more impactful than suppressing attach, but needs a complete method/callgraph audit: callbacks must still see the right dynamic session, parallel workers must use their child, and owner-close/account release must remain exact. Removing a set only when previous==session saves at most that set; eliding restore is not equivalent if callbacks change ambient state or close moves threads. This is a larger independent experiment, not folded into the ValueStore patch.

Existing LmdbFusedRuntimeTelemetryTest, generated borrowed-factor contracts and worker budget/cancellation tests are starting points, not enough proof for all ambient-session consumers. Gather route frequencies before choosing generated API surgery.

## JIT and decision gate

Inlining/CSE may already coalesce adjacent readContext.get calls, particularly the first two. Conversely a large ValueStore method and opaque callbacks may prevent it. Static5→1 is not a predicted CPU percentage. Inspect matched profiles/compiled behavior after correctness qualification; if no meaningful real-query benefit, retain the simpler implementation or archive the experiment. The user's throughput/latency objective, not number of removed ThreadLocal expressions, decides adoption.
