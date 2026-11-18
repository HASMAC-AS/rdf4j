# Resource / replay / external-initializer follow-up

Read-only design, 2026-11-18, following the separate holder-depth experiment. No production edits, builds or measurements. Source sites refer to current rdf4j-threadlocal; line numbers may move. Do not combine this proposal into holder-depth before measuring its isolated result.

## Recommendation

The next smallest experiment is **null restoration without ThreadLocal removal for the static external-value initializer only** (`QueryExecutionContext.setExternalValueInitializer`). Keep both activation scopes, AtomicBoolean close token, identity guard, worker propagation, and exact activation boundaries. Use `set(null)` instead of `remove()` when restoring null. This changes map-entry bookkeeping, not initializer behavior. A plain ThreadLocal with no initializer exposes the same null to all existing readers. It does retain one empty map slot on a thread that used the hook, not a prior Consumer or resource owner.

A separate follow-up can do the same for `LmdbSailStore.activeReadResourceContext`, but that key is **per store**, so long-lived worker threads visiting many short-lived stores make its empty-entry/stale-key memory tradeoff larger. Do not bundle the two and ascribe benefit to either. No predicted speedup.

## Current profile grounding

`record-cancellation/profiles-01/summary.json` has the following nearest ThreadLocal caller samples (not invocation counts or wall-time shares):

| profile | main samples | resource enter | initializer activate | initializer null/previous setter | resource close |
|---|---:|---:|---:|---:|---:|
| broad interpreted A | 731 | 14 | 8 | 5 | 3 |
| broad interpreted B0 | 672 | 9 | 10 | 1 | 4 |
| broad Janino A | 542 | 15 | 10 | 2 | 3 |
| broad Janino B0 | 551 | 12 | 6 | 2 | 2 |
| probes interpreted B0 | 1077 | 7 | 13 | 0 | 5 |
| probes Janino B0 | 896 | 8 | 12 | 0 | 3 |

Counts locate repeat bookkeeping but do not show which removal caused cleanup elsewhere. ThreadLocal cleanup can charge a caller for stale entries created by another key. No exclusive recoverable percentage follows from these samples.

## Exact paths and static frequencies

`LmdbStoreConnection.ReplayableIteration.hasNext/next -> trackedCall -> executeTrackedRead -> executeObservedRead` installs the three scopes around each observed synchronous delegate pull. A normal successful uncached hasNext plus next can therefore install each twice per row; caching, nested fast paths, extra availability calls and iterator adapters alter that. Never describe it as universally once per row.

Observed pull in `LmdbStoreConnection` around 2877–2882:
1. Value foreground activation.
2. `activateExternalValueInitializer(LmdbStoreConnection::protectValueTree)`.
3. `enterReadResourceContext(() -> owner.readViews)`.
4. `activateReplaySafepoint(NO_REPLAY_CHECKPOINT, () -> observedWorkerScope(owner))`.
5. Execute, lifecycle check and snapshot validation; restore depth and close worker admission, initializer, resource context, value scope in existing order.

Unobserved/tracked exposure setup also installs initializer/resource/replay frames in `beginReadExposureScope` around 1802–1812. It uses a real replay checkpoint (`scope`) rather than the observed no-op. Keep that distinction.

Per successful ordinary observed outer pull with no nested callbacks:
- External initializer activation: **one get + one set**, one AtomicBoolean and activation lambda at source level. First proper close: **one get + one remove** if previous null; otherwise one get + one set. Atomic CAS is not a ThreadLocal access. Thus proposed outer null restoration keeps two gets and two map mutations, replacing removal/reinsert churn, not eliminating lookups or claiming allocations are escaped.
- Resource context: enter **one get + one set**, new frame; close **one get + one remove/set**. Views are **not enumerated at activation**. `currentReadResourceViews()` or estimator access adds a get and evaluates suppliers on demand.
- Replay safepoint: activation **one REPLAY.get**, new frame with AtomicReference checkpoint and AtomicBoolean active plus close lambda; close uses the captured ReplayState directly, not another ThreadLocal get. REPLAY already has a persistent mutable holder, so null-restoration advice does not apply to it.
- Dispatch capture adds `captureReplayContext`'s two replay gets, initializer get, active-chain walk and worker-scope captures. These are conditional on dispatch, not unconditional per row.

## Why resource frames cannot just be skipped

`LmdbSailStore.ReadResourceContext` (4873–4924) stores a **supplier**, not a fixed read-view snapshot. `readViews()` walks previous frames and unions lease identities each time. Empty or equal lists at admission do not prove equal future ownership.

`GuardedEstimatorStatementSource.getStatements` (around 7293) checks resource-frame **presence separately from list contents**. With no active read view, exactly one resource view may be borrowed; multiple/empty owned contexts can cause estimator deferral instead of waiting for a guard. Dropping an empty frame changes deadlock-avoidance/admission decisions.

Worker `enterCapturedReadViews` acquires admission/generation under map-growth coordination, retains and validates captured views, optionally binds the preferred view, then adds a resource frame. Worker activation subsequently creates an exposure scope which adds another frame. These are not obviously duplicate: the outer captured views and inner dynamically accumulated scope views have different lifetime/cleanup owners. Preserve unions and close order, including setup-failure unwind and suppressed exceptions.

Resource close checks nesting on the **closing thread** before changing closed state. Null restoration must keep that check, idempotence, and ordering; never capture a foreign-thread holder to bypass it.

## Replay scope is not redundant after observation

The observed checkpoint runnable is no-op, but its frame carries `workerScopeCapture`. `captureReplayContext` walks active frames and records worker capabilities, including observed owner's generation and resource ownership. Removing or reusing a frame across pulls can allow late dispatch after scope retirement, miss closure, or bind a worker to the wrong generation.

`ReplaySafepoint` copies share AtomicBoolean/AtomicReference state and scopeOwner. Close clears checkpoint and worker-capture references to retire capabilities. Do not replace these atomics with plain state, pool/rearm retired frames, or share one frame across public pulls. Captured contexts can outlive the synchronous frame and must observe it retired.

A separate tiny read-only optimization candidate exists in `captureReplayContext`: capture `ReplayState state = replay()` once for the two adjacent safepoint/deferral reads. No user callback runs between the two current reads (`copyActiveReplaySafepoints` only checks/copies internal atomic frames), so same-thread state association is stable. This saves **one get per dispatch capture**, not one per row; likely lower priority. Do not broaden that to the install/worker callback sequence: callbacks can alter context, and existing post-callback reads deliberately record actual installed state.

## External initializer invariants

`activateExternalValueInitializer` restores only once and only if the closing thread's current Consumer is identity-equal to installed `next`. Preserve AtomicBoolean, identity comparison, and current-thread lookup. Nested scopes installing the same Consumer are still separate tokens; skipping an identity-equal installation is not proven equivalent under out-of-order close.

ReplayContext captures the Consumer and restores the previous initializer on worker exit or activation failure. Null must remain null to `getExternalValueInitializer`, value/binding initialization, context capture and `isCurrentContext`. Changing the shared setter affects worker install/restore too, so tests must cover more than the ordinary activation lambda.

The static-key set(null) proposal retains no Consumer through its value. Prove that with a weak-reference check after all activation/context references are released; tests must avoid mistaking a test-local variable for leaked ownership. A no-previous context remains eligible for the null/no-context fast paths.

## Rejected scope fusion

- No scope held across public hasNext/next: observation, cancellation, nested callbacks, worker dispatch and map growth can happen between returns.
- No unconditional reuse of initializer/replay/resource frame merely because same owner or Consumer is installed.
- No cached supplier result, empty-resource elision, or map-growth admission bypass.
- No worker foreground-substitution or shared mutable holder between threads.
- No claim that resource/safepoint source allocations survive JIT; allocation profiles needed.

## Test and measurement gate

External initializer experiment:
1. Existing `ExternalValueInitializerContextTest` and `QueryReplayWorkerScopeTest`; add previous-null restoration, same-Consumer nesting, idempotent and out-of-order close, thrown callback, worker install failure/cleanup and conflicting pre-existing worker initializer.
2. Preserve baseline wrong-thread activation-close behavior without silently corrupting creator state; do not redesign ownership in this bookkeeping patch.
3. Assert public getter/capture semantics empty after close; weak references to retired Consumer/owner clear after tokens/contexts released. Repeat on pooled workers.
4. Untimed instrumentation or reflection-only diagnostic can distinguish map removal from retained null; no production per-row counters in timings.

Resource experiment, independently:
5. `LmdbEstimatorPublicationAdmissionTest` observed failure and callback resource restoration; test dynamic supplier changes, union/dedup across nested contexts, empty context presence, wrong-order and wrong-thread close, idempotence, successful subsequent lookup.
6. Captured-worker resources: failure during retain/validation/exposure setup, close order, generation rejection, map growth/replay and late worker admission. Existing replay/exposure/worker and registration-failure suites from OBSERVED-READ-DESIGN remain required.
7. Many short-lived stores on persistent worker threads: quantify weak-key/empty-slot behavior and verify no view/store owner retained through values. Do not assume set(null) has zero memory cost.
8. Freeze separate arms against the holder-depth winner (or same unchanged baseline); measure regular/interpreted/Janino actual routes, first-row/full latency and allocation. Keep record-cancellation flag/bytecode constant.

## Holder-depth independent review checklist

Await writer's settled patch. Verify every old depth get/set/remove was replaced with current-thread association; internal depth stays distinct from registration.depth. In synchronous calls a local holder is safe; the escaping CapturedReadContext activation-close lambda must fetch the closing thread's holder, not mutate the activating thread's captured holder. Wrong-thread close must not corrupt creator depth even when endReadExposureScope throws. Cancellation's shared linked registration list must never write internalScopeDepth. Read-operation setup must increment internal depth only at the original point after scopes/admission are established, and restore in original finally/error order.

### Independent holder review result
Source review after writer fold found no blocker. Five direct guards still look up executing-thread registration; three setup stacks capture a local holder only for synchronous increment/restoration. The escaping activation close looks up the closing thread again. All eight former depth read sites and three increment/restoration pairs preserve their original control-flow boundaries. Registration admission depth, linked-list lifecycle and cancellation-generation handling are unchanged; no readThreadRegistration.set/remove or shared-list internalScopeDepth writes were introduced.

Read-only javap evidence is in `review-captured-context.javap.txt`. The activation invokedynamic captures CapturedReadContext, ReadExposureScope, integer oldDepth and SailClosable, **not ReadThreadRegistration**. Synthetic `lambda$activate$2` resolves `this$0.readThreadRegistration.get()` before each internalScopeDepth store, including normal, caught-error and catch-all finally paths. This verifies current-thread restoration in compiled code; it is not a claim that wrong-thread close misuse is safe or fixed. No such misuse test was run.
