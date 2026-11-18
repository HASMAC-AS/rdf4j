# Owned record cancellation implementation

Living ExecPlan. Generic record cursors retain dynamic ambient semantics. Native RowState scanner has a default-off owned mode, one terminal handle for scanner lifetime, no per-pattern lease allocation. Direct cancellation publishes a volatile flag only; evaluation thread unwinds and closes resources. An explicit bind cannot replace another owner or erase a pre-existing generic local request. Reset never clears cancellation. Native parent token priority precedes lifecycle/local terminal request and speculative probe deadline.

## Progress
- [x] Trace existing scanner/probe lifetimes and wrapper forwarding.
- [x] Qualify baseline48 and capture missing-API/classifier/review RED tests.
- [x] Implement owned binder, leaf polling, scanner admission and reachable forwarding.
- [x] Validate post-review owned/off suites: each 213 tests, zero failures/errors/skips.
- [x] Freeze candidate classes and transfer CPU to benchmark owner.

## Ownership/measurement
Parent authorized implementation; source tree has unrelated formatter changes that must be preserved. All builds/tests use canonical experiments/overlay/native-benchmark.lock. Inventory owns benchmark scripts/PLAN, this file is implementation-only. No publish until parent approval. Opt-in property permits A/B in same candidate bytecode. Untimed-only static-final trace property may count leaf binding/polls/reset to prove actual route; clean forks disable it.

## Safety
No source/plan-global cancellation owner. Only RowState scanner admission opts in. Existing generic constructors retain ambient checking. Normal reusable cursor close detaches without cancel. Unsupported iterator bind returns false, retaining fallback; no false claim of ownership. Wrapper forwarding must preserve this boolean. Legacy/writer/adjacency paths may remain fallback, explicitly documented. Local exception translation must not cancel a shared parent or poison sibling/fallback attempts.

## Evidence
Logs below retain baseline, expected RED failures, fixture correction, and subsequent verification.

## Implemented boundary/semantics
- Feature `rdf4j.lmdb.recordCancellation.owned` defaults false. Only RowState-based native scanner constructors create a terminal owner; source-only scanner constructors retain compatibility.
- RecordIterator has an internal Cancellation/check capability and default declining bind/cooperative methods. Physical LMDB records accept one owner identity, keep it across reset, and reject changed owner or a pre-existing local request. Generic local cancellation is monotonic and reset never clears it.
- NativeRecordCancellation checks existing native parent first (preserving origin), then its own volatile request or captured lifecycle signal as ordinary QueryInterruptedException, then probe deadline. It never mutates a potentially shared row/root token. Parent fixed kernel pre-execution QIE classification to avoid silent fallback.
- Source read-lock, measurement, value-onClose and ordered record wrappers forward binding/request; statement iterator forwards cooperative request. Legacy/writer/alias and unsupported adjacency/composite paths remain default-declining compatibility paths, not falsely advertised as fully owned.
- Scanner request does not traverse mutable cursor arrays. A permanent volatile unsupported-binding latch makes request return false after any declined leaf bind, though it still marks terminal local state for future admission. This avoids claiming bounded in-fill cancellation on unsupported long-running leaves; parent-token ambient cancellation still works there.
- Normal reusable kernel cursor close only detaches. Scanner terminal close rejects later owned opens. One owner per scanner, no per-key owner allocation.
- Untimed diagnostics `rdf4j.lmdb.recordCancellation.trace=true`: physical LmdbRecordIterator AtomicLong OWNED_BINDS/OWNED_POLLS/OWNED_RESETS, available via test reflection. Disabled in clean timings; static-final gate.

## Test-first evidence so far
- Baseline existing selected record/scanner/worker/context suite: 48 passed, `baseline.log`.
- New explicit-owner API tests compile RED against baseline (`red.log`). Parent classifier test RED: five tests, three pass/two expected QIE failures (`classifier-red.log`).
- Initial focused run 62 tests: one newly written assertion incorrectly demanded immediate cancellation on the second next rather than existing 1024-candidate cadence. Corrected test drains a batch to the next checkpoint; **production cadence unchanged**. First string replacement missed the formatted assertion; `green-owned-safety.log` preserves the same test failure. Verified corrected source before `green-owned-safety-2.log` run.
- Real native filtered-key tests exercise both generic and explicitly bound leaf request from another thread while the matcher holds its native read lock, conflicting ambient token, identity-safe reset, cancellation not erased by binding and no restart after cancellation. Scanner tests exercise same owner across correlated reopen, detach-only close and declined-bind acceptance behavior.

## Review fixes and limitations
- Added `opening` latch before the first owner check. A request during unknown source/probe open or ordered priming marks local terminal state but returns false, allowing escalation. Post-bind check observes a request before returning the admitted cursor. No claim to interrupt arbitrary blocked opener synchronously.
- Terminal `freeResources` drops strong owner reference; retained close does not. Binding a closed/disposed/freed physical cursor is rejected. A late request can only flag the old operation, never close native resources on requester thread.
- Review test RED (with explicit ByteBuddy agent):19 tests,17passed,2expectedfailures before fixes. Initial direct test launcher lacked Mockito agent and is retained as an infrastructure-failure log, not correctness evidence.
- Owned+trace pre-final suite:208/0/0 in `green-owned-safety-2.log`. Earlier estimate210 was incorrect; same selected14 classes yielded208. Final added four cases (opening race, before-first-open/freshowner, ordered mixed forwarding, disposal) should yield212. No suite-count change is described as identical.

## Exact candidate files (avoid blanket staging)
Production:
- `RecordIterator.java`: internal cancellation binding and default declining cooperative contract.
- `LmdbRecordIterator.java`: direct request flag, explicit owner binding/checks at existing checkpoints, reset invariants, final owner release, optional physical diagnostics.
- `LmdbStatementIterator.java`: cooperative forwarding to record leaf.
- `LmdbStatementAccessArbiter.java`: measured wrapper binding/request forwarding; volatile delegate publication for request snapshot.
- `LmdbSailStore.java`: source-lock wrapper and semantic physical wrapper forwarding (existing unrelated formatter dirt also exists).
- `ValueStore.java`: triple-term onClose wrapper forwarding (existing unrelated formatter dirt also exists).
- `evaluation/OrderedRecordIterator.java`: bind only live heads plus pending advance (retired children may already be disposed); request visits immutable original child list. Decline binding if any active child declines.
- `evaluation/LmdbNativeKernelScanner.java`: opt-in owned handle, terminal/opening/unsupported admission guards, binding and per-fill checks; detach-only cursor close preserved.
- `evaluation/NativeRecordCancellation.java` (new): explicit native/lifecycle/probe composition, local volatile request without parent mutation.
- `evaluation/LmdbNativeKernelExecution.java` (parent-owned): direct/wrapped QIE is terminal even before execution started; not reclassified as clean optimizer control.
Tests:
- `LmdbRecordIteratorFilteredKeyCancellationTest.java`: real LMDB direct/owned/reset/disposal/concurrent request coverage.
- `evaluation/NativeRecordCancellationTest.java` (new): helper/scanner/forwarding/ownership/admission tests.
- `evaluation/LmdbNativeKernelInterruptionClassificationTest.java` (new,parent-owned): five classifier cases.

All paths above relative to core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb or corresponding src/test path. Do not include other prior formatter-only files by accident. No publication by this child.

## Final ordered-merge review
Constructor priming can exhaust and dispose an empty physical leaf before binding. The original forwarding loop incorrectly bound that retired leaf. `ordered-red.log` records 10 tests, nine pass and the expected retired-child failure. Binding now visits only active heads and pending advance; these sets are disjoint. Request forwarding still uses the immutable original child list, avoiding cross-thread mutable-heap traversal. Binding, reset, disposal and consumption are explicitly evaluation-thread serialized; only cancellation requests are concurrent. No native lock was added.

## Final verification and handoff
- `green-owned-final-2.log`: 213 tests, zero failures/errors/skips, owned and trace enabled.
- `green-off-final.log`: same 213 tests, zero failures/errors/skips, owned and trace disabled in the same compiled bytecode (surefire:test).
- Frozen full LMDB classes: `candidate-classes/`; per-file hashes `candidate-classes.sha256`; exact production source hashes `candidate-sources.sha256`; runtime `cp-b.txt` replaces only baseline LMDB class directory from `cp-a.txt`. No measurements or speedup claim yet.
- No end-to-end forced kernel pre-execution binder failure test: classifier direct/wrapped tests and scanner admission tests cover distinct boundaries, not a claim of that integrated route.
