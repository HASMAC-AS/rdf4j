# Next resource/replay bookkeeping candidate

Read-only analysis during exclusive timing forks. No runtime probes, compilation, source changes or performance assertions. Current resource experiment changes the **static external initializer key only**. Do not fold this proposal into that result.

## Recommendation

If the current static-key experiment justifies pursuing remove/reinsert churn, the next isolated per-pull candidate is `ReadResourceContext.close`: retain its existing identity/nesting/idempotence checks and replace the previous-null remove with `activeReadResourceContext.set(previous)`. Keep all frames and exact boundaries. This is narrower than caching replay state/thread across escaping Activation.close.

This candidate trades one surviving empty ThreadLocal entry per touched store/thread for avoiding outer-pull removal/reinsertion. That cost is materially different from the static initializer's one key. Qualify memory behavior on persistent workers before recommending it. If that tradeoff is unacceptable, prefer an independently isolated **adjacent replay getter consolidation** on entry/capture, rather than inventing a close cache. None has a demonstrated speedup yet.

## What the profiles actually support

The saved `record-cancellation/profiles-01/summary.json` identifies resource enter/close and initializer activation under observed pulls. Broad interpreted A/B0 resource-enter nearest-caller samples are14/9 (731/672 main samples), broad Janino A/B0 15/12 (542/551); resource-close3/4 and3/2 respectively. Replay getter appears in some arms, e.g broad Janino B0 seven samples. These counts are neither call frequency nor recoverable wall time; inlining and stale-map cleanup distort attribution. Current resource timings are unfinished here and are not used to claim either candidate wins.

## Resource exact behavior and static frequency

`ReplayableIteration.hasNext/next -> trackedCall -> executeTrackedRead -> executeObservedRead` adds a resource frame per observed synchronous pull. A successful uncached hasNext+next may therefore add two frames, but wrappers/caching/nesting change that.

`LmdbSailStore.enterReadResourceContext` currently performs one ThreadLocal get, allocates one ReadResourceContext frame holding previous+supplier, and sets the frame. First proper close performs one get for identity validation and either remove for null previous or set for nonnull previous. Proposed change keeps **two gets and two map mutations per complete scope**; it replaces outer removal/reinsertion only. It adds no Java source-level frame allocation and removes none. Actual escaping/allocation effects require measurement.

The supplier is evaluated on demand by `readViews()`, which unions lease identities through the entire parent chain. Keep suppliers live/dynamic, even if lists are empty/equal at activation. `GuardedEstimatorStatementSource` distinguishes context presence from resource list contents for borrowing/admission/deferral. No empty-frame elision.

Worker `enterCapturedReadViews` retains and validates captured views before adding its resource frame; a nested exposure frame can add dynamically acquired views. Parent union and cleanup order remain semantically necessary. A restoration bookkeeping change must not alter generation validation, map-growth coordination, retention or exception suppression.

## Retained-slot accounting

Let T be persistent threads and S_t the number of still-live stores whose resource ThreadLocal each thread t has touched.
- Static external initializer: at most one live-key entry per touched thread, at most T entries globally, regardless of store count.
- Per-store resource set(null): up to sum(S_t) live-key entries across threads, worst-case S*T for S stores touched by all T workers. One store on64 workers can retain64 empty entries;100 live stores on64 workers can retain6,400 entries for this key family, not64.
- A closed-but-still-referenced store keeps its ThreadLocal key live, so its touched-thread null entries remain until thread death or explicit removal. Close must not attempt to clear another thread's ThreadLocal by acting on the closer's state.
- Collected stores make keys weakly stale; null values do not strongly retain the store/consumer/lease. Stale entries/table capacity can nevertheless persist until subsequent ThreadLocal cleanup, so actual occupied/stale capacity is not bounded solely by currently live S_t. Repeated ephemeral stores on quiet long-lived workers need a dedicated test.
- Do not assign bytes per entry without a measured JVM layout. ThreadLocalMap entry objects, backing-table sizing and cleanup load are separate costs.

Even baseline read getters can create null entries transiently because plain ThreadLocal.get initializes absence to null. The proposal specifically prevents successful outer close from removing its entry; it does not introduce the first possible null entry everywhere.

## Is there a persistent holder to reuse?

No compatible permanent mutable **per-store/current-thread** holder was found among the adjacent fields. `publicationContext`, `activeReadView`, `activeWriterOwner` and callback admission hold transaction/publication/read-view/owner identities, not persistent scratch state. Their null/non-null meanings, binding and cross-thread publication contracts cannot be repurposed to hide resource state.

`LmdbStoreConnection.ReadThreadRegistration` is now a persistent per-connection/thread holder, but a backing store spans multiple connections and also worker/estimator paths not owned by that connection. Piggybacking the store resource head there would lose the store-wide parent union or require searching connections. The successful depth fold is not a general holder-reuse license.

A new per-store holder ThreadLocal would still retain sum(S_t) entries **plus a holder allocation per touched pair**. It could replace repeated map set/remove with a head field, but is a bigger experiment with a permanent object per pair. Its value must not capture the store, or the weak ThreadLocal-key benefit can be defeated by value→store→key retention. A static thread-local map keyed by stores is even more dangerous unless weak-key/value ownership and cleanup are designed explicitly. Neither is the next narrow candidate.

## Replay close: two APIs, different existing association

1. `activateReplaySafepoint` already captures `ReplayState state = replay()` at entry. Its close lambda retires the shared active/checkpoint/capture references, then conditionally restores `state.safepoint`. **It performs zero Replay ThreadLocal lookups on close today.** There is no close lookup to eliminate. The captured holder association, including behavior on foreign-thread close, is existing behavior; do not silently replace it with closing-thread state.

2. `ReplayContext.install` returns an escaping activation. Its close first closes worker scopes, then `restore -> restoreReplayContext` obtains **one current closing-thread replay() lookup** and restores fields. External initializer and installed-context restoration likewise target the closer's thread. This is not the same association as (1). Caching the install thread's ReplayState unconditionally would write the wrong holder on foreign-thread close and change baseline semantics. Misuse may already be unsafe, but that does not justify changing association silently.

A cache with creator Thread+ReplayState and `Thread.currentThread()==creator ? cached : replay()` preserves the holder selected for this one store, but adds two captured references, a thread comparison and fallback branch to every close. Existing lambda already allocates an activation; adding captures may enlarge it without introducing a new object, but JVM layout/escape behavior must be measured. A new explicit frame or wrapper would add another source-level allocation unless it replaces the lambda. Capturing Thread can keep it reachable as long as an activation is retained. No claim of free caching or same-thread-only public close.

Worker close callbacks run before restoration and may nest/query/throw. With a cached state on the same thread, mutable state contents must be read/restored at the original points, not snapshotted early. Preserve reverse worker close order, primary/suppressed exception rules and finally restoration. Deferral depth must restore the exact old integer, not booleanize it.

## Cheaper replay entry opportunity

`ReplayContext.activate`, `withContext` and getAsInt read `replay().safepoint` then `replay().deferralDepth` consecutively. Capturing one local ReplayState at that synchronous entry avoids one get without expanding the escaping close frame, without changing which thread close targets, and without holding scopes across calls. `captureReplayContext` similarly can use one local for its adjacent state reads; its intermediate active-chain copy invokes no user callback. This is one saved lookup per context entry/capture, not per row universally.

Do not reuse a pre-callback **value** snapshot across `WorkerScope.activate`: install deliberately rereads the actual resulting safepoint/deferral/initializer after worker activations. A local stable holder can be reread after callbacks only if the ThreadLocal itself is never replaced/removed; current REPLAY is private persistent withInitial, but audit every setter first. This is separate from the immediate adjacent-get simplification and should not be bundled without evidence.

## Required tests

For resource null restoration:
- Baseline semantic tests first: null after proper close, nested suppliers dynamically change, parent union/dedup, empty-frame presence, idempotence, wrong-order close retains active frame and recoverability.
- Wrong-thread close on an isolated worker must keep identity rejection and leave creator frame intact; no foreign-holder mutation. Test normal creator cleanup afterward only where baseline contract permits it.
- Captured worker views and exposure scope teardown: setup failure, nested callback failure, generation rejection, cancellation, late dispatch, map-growth/replay, close-order and suppressed-exception invariants.
- Weak owner/store/lease references after every frame/activation is released; keep worker alive so death cannot hide retention. Separately quantify entries for many live and discarded stores on persistent threads. No timing-loop GC probes.
- Static/bytecode proof distinguishes remove→set(null), not just externally equal values. Then immutable arms with all unrelated families fixed; regular/interpreted/Janino latency, throughput and allocation.

For replay adjacent-get consolidation:
- Nested deferral depths, no-op/current-context paths, captured worker installation and failure restoration, callbacks altering state, interleaved contexts and reused workers.
- Assert closure association unchanged with source/bytecode capture proof; do not run unsafe misuse through shared test VM.
- No new captured Thread/ReplayState fields or frames for the minimal entry-only version; count relevant bytecode gets in the specific method and use route diagnostics to establish actual frequency.

## Explicitly rejected shortcuts

No blanket hasNext/next lifetime fusion; no removal of observed NO_REPLAY_CHECKPOINT frames (they carry worker admission); no pooling/rearming replay frames whose active atomics may be shared with captured contexts; no dropping resource frames based on equal suppliers/lists; no reusing per-connection holder as per-store state; no assuming every Activation.close executes on its activating thread; no indiscriminate set(null) for unrelated ThreadLocals.

## Additional candidate: one explicit external-initializer activation object

Source review: `activateExternalValueInitializer` currently creates an AtomicBoolean(true), installs next in the ThreadLocal, and returns a capturing lambda holding active, next and previous. A **static nested dedicated Activation implementation** could instead contain final next/previous Consumer references and one volatile boolean active=true. A static-final VarHandle targeting that boolean performs strong compareAndSet(this,true,false), followed by the same current closing-thread EXTERNAL_VALUE_INITIALIZER.get()==next identity guard and setExternalValueInitializer(previous).

This is a plausible narrower allocation experiment than retaining one resource slot per store/thread, **if allocation evidence shows these objects actually escape**. It does not need a stored creator thread, captured ReplayState, ThreadLocal changes or lifetime fusion. The external-initializer activation remains one token per invocation even when previous==next.

### Source-level cost, not a heap-saving claim

Baseline source has two per-activation objects: explicit AtomicBoolean plus stateful capturing-lambda activation. Proposed source has one dedicated activation object. Its layout has two Consumer reference fields plus the volatile boolean; it removes the separate atomic object and the activation's reference to it. Object headers, boolean padding, alignment and compressed-reference settings determine actual bytes, so do not assert a byte saving without JVM allocation/layout evidence. The VarHandle and lookup machinery are static one-time class state, not per-scope allocations.

HotSpot may inline and scalar-replace either/both baseline objects when the scope does not escape, making realized allocation savings zero. The explicit class can itself inhibit or improve escape/inlining decisions; source object count is not enough. Returned activations stored in scope fields, worker-scope lists and error-cleanup paths may escape, but prove that with allocation profiles rather than assuming. Do not cite ThreadLocal CPU samples as evidence of AtomicBoolean heap cost. The new close still executes one CAS and, on the winning call, the same get/set behavior; it does not remove synchronization or lookups.

### Exact semantic requirements

- Use **strong** compareAndSet with volatile ordering, matching AtomicBoolean.compareAndSet, not weakCompareAndSet/plain reads or non-atomic boolean flip.
- Preserve evaluation order: CAS consumes the token before the ThreadLocal identity check. A wrong-order/foreign-thread close with a different current Consumer still consumes the token and does not restore. Moving identity check before CAS changes the existing behavior.
- Read the **closing thread's** current initializer and restore there, exactly as the lambda currently does. Do not capture a thread-local holder or creator thread to redirect restoration.
- Exactly one competing closer wins. The losing closer must not read/write the ThreadLocal as a side effect. A winner whose identity check fails must not rearm active.
- Keep reference identity `==`, not equals, and retain separate tokens for nested same-Consumer scopes. Keep the existing setter semantics, including the currently tested null restoration; do not bundle a separate policy change.
- Make the class static nested to avoid an accidental enclosing-instance capture. Keep final next/previous fields and don't add ref-clearing after close in this experiment: retained activation references currently retain those Consumers, and mutable clearing introduces extra races/behavior changes. Reachability after the token itself is released still must match current tests.
- Keep initializer null validation and prior ThreadLocal read order. Construct the explicit token before installing next, so allocation/class-initialization failure does not publish an unclosable frame. Baseline lambda allocation occurs after set, so this improves a narrow OOME/setup-failure window rather than being byte-for-byte failure equivalence; document that difference and don't add complicated rollback solely to recreate the old failure. VarHandle initialization should fail before publishing the frame.

### Required qualification

1. Reuse existing same-Consumer, idempotent close, wrong-order identity guard, nested restoration, worker activation failure and live-worker weak-owner tests unchanged, first against baseline.
2. Dedicated foreign-thread test on disposable workers: distinct current Consumer makes close consume token without touching creator state; creator's later close does not resurrect it. A same-identity foreign worker case must restore the token's previous Consumer on the closer just as baseline does. Avoid presenting these misuse cases as recommended API usage.
3. Deterministic competing-close test using barriers and two disposable workers with explicit local initializers; assert one restoration at most and no creator-thread mutation. Avoid sleep-based race expectations.
4. Structural bytecode/source proof: no AtomicBoolean allocation in the activation factory, static strong VarHandle CAS in close, no captured creator Thread/ReplayState. Structural proof does not replace behavior or allocation measurements.
5. Paired allocation and throughput/latency profiles with unchanged result-consuming workload and scope boundaries. Record allocations per external initializer activation in diagnostic runs, separate from total query allocations and escape-eliminated cases. Compare against the selected immutable current winner, not an unqualified combination.

**Priority judgment:** if allocation profiles attribute meaningful surviving allocations to this exact AtomicBoolean+lambda pair, I would test the dedicated token before per-store null-entry retention. It avoids the store-count-scaled empty-slot tradeoff while preserving the same scope contract. Without that evidence, adjacent replay get consolidation is simpler, and neither allocation redesign nor per-store retention has a demonstrated benefit.
