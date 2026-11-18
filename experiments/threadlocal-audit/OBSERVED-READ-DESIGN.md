# Observed-read bookkeeping: next isolated scope experiment

Status: **design only**, 2026-11-18. No production edits, builds or timing runs made for this investigation. Reviewed the four-arm evidence published at `5ac89b5`; current checkout HEAD is `76e5bfc2`. Cancellation work proceeds independently. Line references below describe the inspected source and may move.

## Recommendation

First investigate folding `LmdbStoreConnection.internalReadScopeDepth` into the already persistent, per-thread `ReadThreadRegistration`, as a **separate `internalScopeDepth` field**. Preserve the exact points where internal scope depth becomes active and is restored. Do not substitute registration `depth`: registration begins before scope setup is complete, so its meaning is different.

This is a smaller semantic change than removing observed-read scopes. It eliminates a separate ThreadLocal entry and its per-pull set/remove/reinitialization cycle. A mechanical accessor replacement still performs a ThreadLocal get: `readThreadRegistration.get().internalScopeDepth`. It does **not** eliminate all gets. Synchronous tracked/observed scopes can potentially reuse a locally captured registration after a separate ownership review. Do not capture that holder into an escaping worker-close callback without preserving thread association.

A simpler alternative is restoring `internalReadScopeDepth.set(oldDepth)` even when oldDepth is zero. That removes remove/reinsert churn without folding holders, but retains a separate live ThreadLocalMap entry and still performs set/get operations. Integer values in the normal shallow depth range are cached; do not claim routine boxing allocation savings without measurement. The holder fold is the stronger mechanism, while sentinel restoration is the smaller patch.

Neither design is concurrency-proven or performance-qualified yet.

## Evidence, not a speedup promise

`source-pull/FOUR-ARM-RESULTS.md` reports no established throughput/latency win or combination benefit from the prior SOURCE_PULL and writer-guard experiment. Writer-guard-only's Janino signal is promising, but its four-block interval includes zero. Do not add a new cancellation change to this comparison and attribute the combined difference to scope bookkeeping.

The separate JFR files in `source-pull/four-arm-profiles-01/summary.json` locate repeated ThreadLocal work around observed reads:

| Profile | Main-thread samples | TL caller: observed read | tracked read | external initializer activation | enter resource context |
|---|---:|---:|---:|---:|---:|
| Janino A | 499 | 20 | 11 | 7 | 7 |
| Janino D | 500 | 25 | 10 | 14 | 10 |
| Interpreted A | 709 | 12 | 12 | 10 | 5 |
| Interpreted D | 673 | 25 | 14 | 12 | 10 |

Janino A TL-related leaves include getEntry (32), set (27), expungeStaleEntry (18), cleanSomeSlots (17); Janino D includes getEntry (42), cleanSomeSlots (33), expungeStaleEntry (16), set (16). These samples suggest investigating entry churn. They are **not call counts, exclusive recoverable time, or predicted improvement percentages**.

## Actual paths and frequency

`ReplayableIteration.hasNext` (~3710) and `next` (~3737) each enter `trackedCall` (~3776), which invokes `executeTrackedRead`. An ordinary uncached successful row can therefore produce two tracked calls; wrappers/repeated hasNext calls and internal sources can change this. Once availability/result observation is published, most subsequent pulls select `executeObservedRead`. Establish exact invocation counts with a diagnostic-only instrumented run, not per-row atomic counters in timed runs.

Observed outer pull, with no nested reads:

1. `executeTrackedRead` reads internal depth once (~2612), then checks observed owner and absence of active exposure.
2. `executeObservedRead` begins read registration under `readAttemptLock` (`beginReadThread` gets the registration once).
3. It reads internal depth again (~2872), installs value/external-initializer/resource/replay-worker scopes, then sets internal depth (~2883).
4. It executes the delegate, checks lifecycle cancellation and validates native snapshots.
5. It restores depth (~2895), closes the installed scopes in the existing order and ends registration (`endReadThread` gets the registration once).

Thus the common non-nested observed path has **two depth gets, one depth set, one depth remove**, plus **two existing registration gets**, before callback/worker-specific additions. Folding by accessor substitution preserves the four gets in total, removes the depth set/remove map operations, and replaces depth mutations with holder-field writes. Reusing one local registration through the synchronous path could reduce more gets, but is a separate refinement requiring careful setup/error-path review.

Other registration gets: `observedWorkerScope` (~2972) captures cancellation generation when a worker scope is requested; `throwIfReadScopeRequestedReplay` (~2994) executes on nested reads; `ReadExposureScope` construction (~3270) captures cancellation generation; `CapturedReadContext.consumeInterruptionOwner` (~3474) checks the caller's delivered interruption. Do not count worker-only or initial-exposure gets as unconditional on every observed pull.

## Complete internal-depth call-site / thread-association audit

Every existing depth get is a direct `ThreadLocal.get()` on the **executing thread**, not on a stored thread argument:

| Site in LmdbStoreConnection | Meaning and association |
|---|---|
| executeReadOperation ~795 | Immediate synchronous nested-read decision for its current caller. |
| read-result creation retry ~1389 | Current creation stack: if generation could not be admitted, nested work must unwind rather than retry independently. |
| read-result creation retry ~1535 | Same current creation stack after cleanup: nested retry must propagate map conflict to its owning read. |
| prepareForReadAttempt ~1970 | Current caller must not wait for replay while its own internal read scope blocks replay. |
| executeTrackedRead ~2612 | Current pull stack nested fast-path decision. |
| executeTrackedRead ~2656 | Current stack captures/restores depth around operation.execute, including exceptions. No returned closure owns this depth restoration. |
| executeObservedRead ~2872 | Current stack captures/restores depth around synchronous delegate work and cleanup. |
| CapturedReadContext.activate ~3498 | The activating worker's current thread, not the thread that originally captured the context. |

The first seven can use current-thread holder access; a local holder in the synchronous scope does not migrate merely because delegate work dispatches another worker. Workers still require their own activation/holder.

**Escaping close exception:** `CapturedReadContext.activate` returns an Activation lambda. Its depth restoration (~3541) currently executes a ThreadLocal remove/set on the **closing thread**. `endReadExposureScope` first checks nesting on that thread and can fail; the depth finally nevertheless runs. Capturing the activation thread's holder and writing it in this lambda would mutate foreign-thread state on wrong-thread close, unlike the baseline. Retain a current-thread accessor for this close in an initial fold, and test its existing failure behavior. Alternatively, add an explicit thread-association guard only as a separately justified contract change. The baseline wrong-thread behavior is not proof that misuse is safe; do not silently worsen it.

`beginReadThread` registers the current holder under lock and increments registration.depth **before** resource/value/exposure setup. `endReadThread` decrements that count and clears thread/previous/next references at zero. Cancellation can inspect/interrupt these registered holders from another thread under lock. A new internalScopeDepth must stay exclusively current-thread bookkeeping; cancellation/cleanup must never update it through the linked registration list. Leave registration.depth, cancellationGeneration, interruption ownership and link lifecycle unchanged.

## Secondary candidate: narrowly scoped null restoration

Two concrete outer-pull contexts currently remove their entry when restoring an empty parent:

- `LmdbSailStore.ReadResourceContext.close` (~4887–4900), `activeReadResourceContext`.
- `QueryExecutionContext.setExternalValueInitializer` (~537), `EXTERNAL_VALUE_INITIALIZER`.

For these plain ThreadLocals only, a restored null is currently indistinguishable to callers from an absent entry. Unlike withInitial callbacks, no initializer side effect needs reentry. Replacing null remove with set(null) may avoid map removal/reinsertion while retaining all frame/ownership checks. It retains a live ThreadLocalMap slot (including the static initializer key's slot), though the value no longer retains the supplier, consumer or resource owner. Per-store/per-connection keys and long-lived pooled threads warrant memory tests, including stale weak-key cleanup. This is not a blanket rule for other ThreadLocals.

Keep idempotent-close checks, wrong-order detection, external initializer identity checks/AtomicBoolean and previous restoration unchanged. Dynamic resource suppliers and parent union semantics remain necessary. Existing `ValueResolutionScope.activateForeground` already uses set(previous), including null; it does not need this change. `QueryExecutionContext.REPLAY` already uses a persistent mutable ReplayState holder.

## Unsafe simplifications rejected

- Keeping one observed scope open across public hasNext/next calls. Availability observation is serialized against growth before return; cancellation, close and replay may occur between calls. Callers can also interleave queries and callbacks.
- Equating registration depth with internal scope depth. Admission exists before setup completes, so doing this can bypass necessary nested setup/admission.
- Caching a connection holder in a plan, iterator, compiled source or worker activation close without a thread-affinity proof.
- Removing the observed no-op replay checkpoint. Its frame also carries worker admission/capture, despite no replay being allowed after observation.
- Collapsing resource scopes because current view lists look equal. Suppliers and parent scopes preserve ownership and can be dynamic.
- Skipping repeated external initializer activation solely because two consumers are identity-equal. Close-token ownership and restoration remain observable.
- Replacing worker value-scope activation with foreground activation. Worker reader generation and newly created reader release semantics differ.
- Treating a previously closed value scope as absent. Foreground restoration deliberately preserves it.

## IR / Janino boundary

The recommended bookkeeping change sits outside the native producer: public repository result pulls pass through these wrappers in regular, interpreted-native and Janino execution. IR fusion cannot simply erase their external callback, worker and resource ownership contracts. Generated code can amortize internal calls or use explicit query-owned context inside a proven activation, but public pull entry/exit and external value materialization boundaries still require correct scopes. Optimize the outer bookkeeping independently first; compare all three routes with identical data/result consumption and verify actual generated route selection.

## Required tests and experiment gate

1. Red/green diagnostic test proves no separate depth ThreadLocal get/set/remove in the folded implementation, while exact visible depth follows 0 -> 1 -> nested -> 1 -> 0. Do not make a timing assertion.
2. Setup callback before full internal activation: registration.depth is positive but internalScopeDepth remains zero. Retain admission rather than falsely taking the internal fast path.
3. Success, delegate failure, setup failure, cleanup failure and suppressed-exception ordering restore prior depth. Reuse the same thread immediately for another read.
4. Interleaved connections and nested queries on one thread; reused workers; worker activation and exception cleanup; attempted wrong-thread activation close must not corrupt the creator's holder. Inspect baseline behavior first rather than inventing a new expected contract.
5. Growth/replay before observation, availability observation, nested callbacks, close/cancel and late worker admission preserve outcomes and resource release.
6. For null restoration separately: empty getter/capture remains empty after close; nested initializer ownership and worker propagation restore prior custom initializer; resource supplier unions and wrong-order/idempotent close remain exact. Confirm no old owner survives through the null entry. Test repeated short-lived connections/stores on long-lived worker threads for retained heap/entry growth.
7. Existing regression targets: LmdbReplayExposureContractTest, LmdbReplayCheckpointContractTest (including unpublishedGrowthDoesNotBlockActiveComparison), LmdbReplayContextAdmissionTest, LmdbNativeStreamingReadContextTest, LmdbReadRegistrationFailureTest, LmdbMapGrowthReplayLifecycleTest, LmdbNativeReplayWorkerContextTest, LmdbShutdownCancellationTest, RepositorySourcePullScopeTest, RepositoryValueIterationTest and ExternalValueInitializerContextTest.
8. Freeze exact source/classes and run matched regular/interpreted/Janino correctness, first-row/full latency, allocation and profiling comparisons. Keep cancellation patch identical on both arms or use the same immutable prior baseline on both. Record no-spill/checksum/lifecycle evidence. No claim of full repository safety from these scoped suites.

Prioritize the separate-depth-field fold, with current-thread restoration for escaping worker close, then decide whether the smaller null-restoration experiment is worth its memory tradeoff. No performance benefit has yet been demonstrated for either.
