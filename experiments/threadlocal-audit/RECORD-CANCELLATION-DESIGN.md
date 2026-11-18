# Direct cooperative cancellation for LMDB records

Read-only design for the user's explicit request, 2026-11-18. No source changes, compilation or performance claims. Candidate must be measured separately from the existing type-matrix experiment.

## Conclusion

**Yes: implement CooperativeCancellation on the LMDB record leaf and poll an explicitly owned flag.** This gives a nonblocking direct delivery path and can eliminate ambient signal lookup on a certified native-owned path. It is not enough simply to add `volatile boolean cancelled` to the physical cursor: forwarding currently stops above that cursor, reusable logical probes need separate ownership, and query cancellation, speculative probe timeout/capacity, lifecycle close, and worker interrupt are not the same signal.

Recommended first design is a **per-logical-open cancellation lease** containing a local direct flag plus explicit parent signals, rather than a listener per record or a single shared global boolean. Parent flags can be read by reference without ThreadLocal access. A general cancellation-listener registry is optional, more invasive, and not required to obtain the first benefit.

## Existing delivery path and missing links

- `common/iterator/.../CooperativeCancellation.java`: `requestCancellation()` must return true only if this or a downstream implementation accepted. `forward(Object)` tests the interface; `forwardAll` visits every delegate without short-circuiting. This is not automatic graph traversal or registration.
- `TimeLimitIteration.java:405-425` requests cancellation from its wrapped iteration, then schedules interrupt/close escalation. A leaf implementation must publish promptly, without waiting for cursor/transaction locks or closing native resources on the timer thread.
- `RepositoryValueIteration.java:403-418` sets its own volatile request flag, forwards to source, and interrupts an active foreign caller to unblock value-store admission. This behavior must remain; a record flag alone does not unblock all native/read admission waits.
- `NativeContextClosingIteration.java:106-113` requests its owned context root token, then forwards to its delegate. Borrowed-context wrappers deliberately do not cancel the enclosing owner. Root-token publication currently has **no listener API**.
- Common Converting/Filter/IterationWrapper/ExceptionConverting/DualUnion/Delayed wrappers forward, but only when every delegate supports the contract. `LookAheadIteration` defaults `forwardCancellation()` to false; subclasses must supply real forwarding or poll their own flag.
- **Concrete break:** `LmdbStatementIterator.java:31` extends AbstractCloseableIteration and implements IndexReportingIterator only. It owns `recordIt` (`45`), but does not implement CooperativeCancellation. A request arriving there cannot reach LmdbRecordIterator today.
- `RecordIterator` currently extends Closeable only. `LmdbRecordIterator` implements RecordIterator only. Measurement wrappers in `LmdbStatementAccessArbiter` (including MeasuredRecordIterator at `284`), ordered wrappers and anonymous TripleStore record wrappers must be audited/forwarded too. A `default false` method on RecordIterator would be a compatibility fallback, not evidence that these wrappers deliver requests.
- Native kernels and eager operators may be running before any public result wrapper exists. They need their already-admitted query/lifecycle signal or an active-child registry; post-return wrapper forwarding cannot cancel work that is still constructing the result.

## Lease and direct flag design

Illustrative internal shape, not a committed API:

```
RecordCancellationLease {
  localRequested: release/opaque boolean or volatile boolean
  lifecycle: explicitly admitted CancellationSignal view
  nativeQuery: explicit native token/checker preserving origin and child ownership
  probe: explicit speculative deadline/checker
  active: lifecycle marker (not a replacement for the cursor's existing closed/disposed state)
}
```

The cross-package API should expose an internal checkpoint/lease interface, not make mutable native-token internals public. It needs **query-before-probe** checking for source operations, unlike the current type-matrix helper. It also needs a defined local-request exception. Use the owning native child's attributed cancellation where applicable; regular record iteration should preserve its caller's timeout/cancellation translation rather than manufacture a foreign native origin. This choice must be proved by outer TimeLimit/repository tests before shipping.

`requestCancellation()` snapshots the logical lease, marks only that lease's local flag, and returns accepted. It must not acquire `txnRef` monitor/read-lock, invoke callbacks, perform LMDB calls or close resources. A cancellation request while the owning thread holds a long scan's lock must still publish immediately. A volatile flag is sufficient for visibility; release/opaque can follow CancellationSignal's established discipline if justified, but a plain unsynchronized boolean is not safe.

At the current entry/candidate/exhaustion checkpoints, check the explicit lease. Keep the same 1024-candidate cadence, including filtered keys and exact-key/empty results; do not move to output-row-only polling. Cancellation and normal cleanup unwind on the evaluation thread through existing closePreservingFailure. Request acceptance is not evidence that cleanup finished.

### Ownership and reuse

- Physical retained cursor and logical cancellation lease are different lifetimes. `LmdbRecordIterator.reset` clears the poll interval and pattern; it should install a **new** lease for a new logical probe rather than clear the old lease flag. Never turn a monotonic cancellation signal back to false.
- A delayed cancellation callback or owner registration for lease A must target A, not `iterator.currentLease`. Otherwise reset to B lets an old request cancel the new probe. Similarly, unregistering A must not unregister B.
- A raw reusable object referenced by an old caller cannot infer which logical generation that caller means. Either keep retained cursors private and return a per-open handle whose cancellation method targets its immutable lease, or define the cooperative handle on the operator-owned probe rather than expose the reusable physical cursor. The existing NativeProbe contract replaces/cleans its prior selection; async cancellation still needs a generation-safe target.
- Close/exhaustion deactivates/detaches the logical lease and its registrations; dispose releases native buffers/cursor once. Cancellation callbacks already in flight may still flag old A harmlessly; they cannot touch native state or B.
- Worker transfer passes explicit signal references after effective scopes are installed. Parent and child-local native cancellation remain distinct. Sibling-local stop must not cancel sibling/parent. Null inheritance is a no-op, so missing explicit signals resolve at an actual admission boundary, not a global assumption of no context.

## How parent cancellation reaches the flag

### Preferred first implementation: direct shared signal references

`CancellationSignal` already provides monotonic cancellation, immutable flattened combine views and direct polling. `NativeCancellationToken` already checks parent ownership and preserves cancellation origin. Capture these explicit references at the correct evaluation admission and pass them to the logical lease. Then root cancellation is visible by reading that same parent object; no registration/unregistration is necessary for parent delivery, and no ThreadLocal is required in the inner scan. Local `CooperativeCancellation.requestCancellation` marks the lease's own signal (or local native child) without cancelling the shared root.

Keep the lifecycle CancellationSignal independent of NativeCancellationToken: the former can encode the enclosing reusable-query/result lifetime; the latter encodes query and local pipeline attribution. Do not call cancel on a combined CancellationSignal view (it is read-only). Compose a local owning child with an explicit read-only parent view at admission.

The speculative deadline remains an explicit checker: time passes without a request callback, and capacity exhaustion must remain distinguishable from a timeout. A single boolean supplied only by requestCancellation cannot represent either automatic deadline expiry or its classification. Interrupts also require existing owner-aware checks; flattening them into a leaf flag loses attribution.

### If a registry is needed for a boolean-only leaf

Register a lease-specific sink at the **owning logical evaluation/operator admission**, not per visited record. Owner registration and owner cancellation publication need a linearizable handshake: under a small owner lock, either add active registration or observe already-cancelled and schedule immediate flag delivery. Publish cancellation before snapshotting registrations; deliver outside the owner lock. Sink does only atomic flag/cause publication. Removal returns/removes the exact registration object/generation; it does not rely on equality of cursor objects.

A registry on only one child token is insufficient: cancellation may originate in an ancestor or be discovered via an evaluation-thread interrupt during polling. Either owner hubs propagate ancestor publication to child hubs, or register each relevant monotonic ancestor, or retain the explicit parent check. This is a reason to prefer direct references initially. Preserve first-origin/native ownership rather than collapsing every incoming cause into an unattributed boolean.

Unregister on logical close/exhaustion, failed open, reset/replacement, replay attempt discard and final dispose. Do not let a token retain a closed cursor/source/transaction indefinitely. A callback may race removal; because its target is a resource-free old lease, late publication is harmless. Never execute synchronous native close under owner cancellation locks.

## Compatibility boundary: ambient callers still exist

Existing `LmdbRecordIteratorFilteredKeyCancellationTest.retainedResetStartsANewCancellationPollBudget` constructs and resets outside the signal scope, then installs cancellation only around next. Constructor/reset-only ambient capture would fail it. Do not use that counterexample to reject explicit delivery; instead distinguish:

1. **Explicit-owned path:** signal/lease is supplied by admitted native or regular owner; direct checks are authoritative, and conflicting caller ambient state must not steal ownership.
2. **Compatibility path:** no owner supplied; existing ambient query/probe behavior remains at checkpoints. The direct local flag is additional, not a reason to disable fallback. If later converting all callers, prove coverage before removing fallback.

Capturing once per pull is a separate candidate, not a free win: an ordinary one-row next currently often performs zero ambient checks until the 1024 boundary, while per-pull capture adds work. A long filtered scan benefits differently from short exact-key probes.

## Replay, exact-index and wrappers

`TripleStore.exactIndexCancellation` currently runs replay safepoint, native query check, probe check, then current-thread interruption. Preserve this ordering. A record lease does not replace replay contexts or read snapshots. Replayed/reopened attempts get the same terminal query/lifecycle owner but a new local attempt/deadline lease as appropriate; a losing attempt's stop must not poison successful replay. Map-growth renewals retain existing transaction-version/lease validation.

Forwarding changes must be tested at the actual root-to-leaf path. Add LmdbStatementIterator forwarding and record measurement/ordered/union forwarding as necessary; do not report acceptance at a wrapper that simply drops the request. Multi-child wrappers must forward to all live children. Direct-adjacency leaves need analogous cooperative ownership or explicit shared parent checks; implementing only the LMDB cursor does not cover an adjacency-routed query.

## Acceptance tests before performance claims

- Real long all-filtered NEXT/FILL scan, direct request from another thread while scan owns its read lock; bounded acknowledgement, no native close by requester, zero lost requests, correct provenance.
- Request before first pull, during candidate loop, during exhaustion, after close/dispose; exact-key success/absence; repeated cancellation idempotence. Timeout escalation and exception translation unchanged.
- End-to-end TimeLimit -> repository/converting/statement -> measurement -> record delivery with assertions at leaf. Multi-child wrapper delivers all. Eager evaluation before public result creation cancelled via explicit parent.
- Explicit owner under conflicting ambient scope; legacy construct-outside/consume-inside behavior remains. Parent/child/sibling signals and regular lifecycle signal combined correctly.
- Retained reset A -> B while cancellation of A races: A stopped, B unaffected unless shared parent cancelled. New B cancellation effective immediately; stale handle cannot target B. Registration count returns to baseline on every exit.
- Probe time expiry and capacity trip remain separate, query-before-probe priority, losing attempt/replay restoration, worker interruption not attributed to unrelated root, map growth/transaction renewal and shutdown cleanup.
- Compare tiny repeated probes, long scans, all-filtered scans, batched fill and adjacency controls. Count actual ambient gets removed and extra registrations/allocations separately. Preserve throughput, first-row/full latency and cancellation-acknowledgement/cleanup latency. No percentage assumed.

## Refinement: an owned native probe need not allocate a lease per pattern open

The earlier per-logical-open lease requirement is **not universal**. It applies when a cancellation target can outlive one logical scan and the same physical object later serves an independent owner. A stricter operator/attempt-owned API can instead use **one monotonic terminal cancellation state for the probe/scanner's entire lifetime**, shared across ordinary pattern resets, with no allocation per open. Cancellation makes that owner terminal; later open/reset must reject it. Normal iterator close/exhaustion is not cancellation.

### Concrete ownership evidence

1. `NativeLmdbQuerySource.java:364-367` explicitly calls NativeProbe **operator-owned** and says a returned RecordIterator is valid only until the next open or probe close. This already prohibits retaining the old record handle across subsequent opens. It does not, by itself, bind the probe to a cancellation owner or guarantee every source implementation rejects reopening after close: the default implementation at 369-390 has no permanent closed flag. Therefore use an explicit owned overload/contract, not an assumption imposed on every existing newProbe caller.
2. `LmdbNativeKernelScanner.java:40-86` owns final source/runtime invocation and arrays of probes, record iterators and reusable cursor wrappers. At `163-168` an unordered scan site lazily creates **one** source.newProbe and reopens it for correlated keys. There is one probe per scan site, not one per output tuple. `217-231` closes and clears all owned probes/ordered iterators. `ReusableCursor.close` (`258-260`) only detaches its delegate; it does not cancel or dispose the owning probe. This distinction permits early scan end without poisoning the next correlated pattern.
3. `LmdbNativeKernelExecution.java:1751-1753` allocates a fresh scanner from the current RowState during tryOpenRows; aggregate binding has analogous allocation at `753-755`. The returned row cursor's close (`2335-2381`) is terminal via its closed flag and owns kernel, scanner and binding probe cleanup. Bind-time fallback explicitly closes the old probe and retries with a fresh tryOpenRows/aggregate attempt (`1633-1640`, `657-666`), rather than handing the old probe to an independent query. This is a strong initial site for the opt-in owned scanner API.
4. Parallel row work creates a child `workerCancellation` at `LmdbNativeParallelKernelRows.java:360`, captures speculative deadline at `364`, installs worker scope around work (`398-403`) and creates a scanner from workerRow at `746`. Each worker should get its own terminal scanner/probe state referencing the correct worker child and shared root. Do not replace it with a single global flag, and do not cancel the root for local worker/attempt cleanup.
5. `LmdbNativeLeftJoinPlans.java:110-123` allocates a new LeftJoinCursor on runtime plan.open(row); that cursor lazily owns rightProbe at `725-749` and closes/nulls it at `948-950`. Repeated pattern opens reuse one rightProbe, while the compiled plan itself does not store that probe. This is another plausible bounded owner, but path/adaptive/lexical alternatives need separate checks before enabling the new API across all left-join branches.
6. Real retained implementation `LmdbSailStore.RetainedNativeProbe` (`16118-16137`) is freshly allocated by newProbe. Each open first checks its `closed` flag, closes prior selection, and creates/resets its private retained LmdbRecordIterator (`16149-16181`). An open failure closes the whole probe (`16192-16196`). Final close (`16425-16469`) marks closed, closes selection, disposes retained cursor, drops its reference and releases owned views/stamp. The other source's anonymous probe (`14486...14757`) likewise owns a private retained cursor. Neither exposes that retained cursor as a source-wide singleton.
7. `SyntheticValueSource.newProbe` (`LmdbSyntheticValueSource.java:625`) creates/delegates a fresh inner probe and wraps it; the wrapper does not magically bind cancellation. `CompositeNativeLmdbQuerySource` routes/delegates probe creation, including multiple component probes. Any explicit cancellation overload must propagate through these wrappers rather than capture on a reusable delegate source.

### Allocation-free-per-open fast-path contract

Proposed initial scope: native kernel scanner created for one terminal RowState attempt. Construct one owned cancellation handle at scanner admission from its actual local native token, lifecycle view and attempt deadline. Pass it explicitly when lazily creating each retained probe. Its local request flag never resets. Every physical cursor retained by that probe reads the same handle; `reset` changes only scan pattern/tick/resources and first checks that the owning handle is not cancelled. No listener or cancellation lease is allocated per correlated key. A shared parent reference delivers query cancellation directly.

The only public cancellation target for this mode is the **owning scanner/probe/attempt**, not an independently owned per-key cursor. A late request targets the same terminal owner, so it is correct to stop any later pattern under that owner. Reuse of an old cursor after next open was already outside NativeProbe's documented validity contract. Normal KernelQuadCursor.close remains detach-only and must not set terminal cancellation. If an algorithm needs to cancel one scan but continue the owner, it must use a distinct short-scan stop mechanism or the generic per-open model, not this terminal flag.

On replay, fallback, worker replacement or a genuinely different cancellation/deadline owner, allocate a new owner/probe once, close/discard the previous one and never reactivate a cancelled handle. The scanner class itself currently lacks a permanent closed flag and can theoretically allocate probes again after close; the opt-in owned implementation should explicitly reject terminal reuse, and callsite tests must verify it rather than claim the entire existing API already enforces it.

### What is not proved by these callsites

No blanket proof yet for every packed/factorized/adaptive path that stores a probe in runtime state, or for arbitrary external code using generic retained TripleStore APIs. A source object can be reusable across queries; it is not the cancellation owner. Attempt deadlines may change independently of root query identity. Generic constructor/reset-outside-scope tests must continue to use ambient compatibility behavior. The direct-flag optimization should therefore start with the verified native kernel scanner ownership boundary, not silently change all RecordIterator constructors.

Tests for this stricter path should additionally show: many correlated resets allocate one handle; cancellation makes all future opens fail before another native visit; normal early scan close still permits next key; no reuse after terminal close; fallback/replay gets a fresh owner and losing attempt stop cannot poison it; independent queries compiled from the same plan receive distinct scanner/probe state; parallel worker-local stop remains local. These are acceptance requirements, not claims already tested.
