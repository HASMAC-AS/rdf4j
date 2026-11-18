# Read-depth holder fold implementation

Description: replace separate internalReadScopeDepth ThreadLocal with distinct internalScopeDepth field in ReadThreadRegistration. Preserve existing registration.depth admission semantics, exact activation/restoration boundaries, and executing-thread restoration in escaping worker close. No lifetime fusion, cancellation change or publication.

## Living steps
1. completed: baseline source snapshot and focused red/hit-proof tests.
2. completed: narrow holder fold; matching semantic checks and lifecycle regressions.
3. in_progress: immutable LmdbStoreConnection class-family layer; test independently over prior-best D runtime and current runtime; provide hashes. Benchmark prespec is in PLAN.md, owned by inventory worker.

## Baseline and evidence
Original production source saved in baseline-source; checkout HEAD/status recorded. Initial test compile failed because inherited protected ReadOperation type was inaccessible from test lambdas; corrected with a test-only subclass and Supplier adapter, no production change. Preserved compiler output in test-compilation-failure.log. This is not semantic red evidence. Focused four-test outcome is recorded below.

Every depth use stays current-thread owned; synchronous scope can retain a local holder only inside its call stack. Worker activation close will resolve the closing thread's holder, not a captured creator holder. Preserve dirty formatter/cancellation files. Tests inspect fields read-only and never mutate internals.

### Captured initial red
Command: `experiments/threadlocal-audit/mvn.sh -o -pl core/sail/lmdb -Dtest=LmdbInternalReadDepthTest test`
Report copied from `core/sail/lmdb/target/surefire-reports/org.eclipse.rdf4j.sail.lmdb.LmdbInternalReadDepthTest.txt` to `red-report.txt`:
```
Tests run: 4, Failures: 1, Errors: 0, Skipped: 0
LmdbInternalReadDepthTest.depthUsesExistingRegistrationHolder
Expected java.lang.NoSuchFieldException to be thrown, but nothing was thrown.
```
The other three tests passed: nested read/error restoration, interleaved connections and reused executor restoration. Added actual captured ReplayContext worker activation test before production changes; baseline result is recorded below.

Baseline compiled class proof: all 20 `LmdbStoreConnection*.class` files exactly equal prior-best D in record-cancellation/baseline-classes (baseline-class-comparison.json). This family overlay will not smuggle the cancellation patch into prior-best comparisons.

Wrong-thread close is existing misuse with unsafe cleanup accounting: failure of exposure stack checks does not prevent endReadOperationLocked from operating on closing-thread registration. Do not repair that contract here. The fold must leave this callback looking up closing-thread state, not writing captured activating-thread holder. Review/bytecode proof and isolated-process misuse evidence can establish that without corrupting a shared test VM.

Captured ReplayContext baseline: 1 test, zero failures/errors (captured-baseline-report.txt). Production edit follows this evidence. Wrong-thread close execution is explicitly untested; existing baseline misuse is unsafe. Initial setup/admission distinction is preserved at exact source boundaries but no new injected pre-setup callback test was added.

### Candidate review
Source hash: `18f20e0bd86aaf7c0fbbe30c36eb30f0f2d82f3784fedb6329423da96d54bcc3`.
Independent native-path reviewer found no blocker. Compiled CapturedReadContext SHA `b796f123c37fcf1fe344530a0d437c757c63ca0b9c926cee9445015d1758e28c`; lambda$activate$2 captures context/exposure/int oldDepth/SailClosable, NOT a registration. In normal/caught/catchall restoration it resolves `this$0.readThreadRegistration.get()` before writing internalScopeDepth. See review-captured-context.javap.txt and captured-worker.javap. This is source/bytecode proof, not execution of unsafe wrong-thread misuse.

### Scope and expected mechanism
Five direct depth predicates now resolve the executing thread's existing registration. Three activation stacks capture that holder for synchronous depth reads/writes; only escaping worker close resolves it afresh. Separate ThreadLocal depth entry is removed; no read admission, callback, observation, replay or value/resource lifetime boundary moved. This removes map set/remove churn, not every context lookup. It can initialize the existing holder earlier on paths that only check depth; no performance claim until matched measurements.

### Current-source selected suite
Command: `experiments/threadlocal-audit/mvn.sh -o -pl core/sail/lmdb -Dtest="$(cat experiments/threadlocal-audit/read-depth/safety-selection.txt)" test`, canonical build/benchmark lock held.
```
Tests run: 93, Failures: 0, Errors: 0, Skipped: 3
BUILD SUCCESS
```
90 executed tests passed across 11 selected classes; focused new class 5/5 passed. Evidence: green-safety.log and green-reports/. Three map-growth cases require `rdf4j.lmdb.nativeValueMemo.testProbe=true` and were skipped in this default run; separately gated follow-up planned. Scope is selected lifecycle/resource/cancellation tests, not the complete module/repository.

Candidate family saved under layer/; source snapshot under final-source/; class/source SHA256 inventories retained. Candidate-on-prior-D direct suite uses only this class family ahead of immutable baseline. Code-source witnesses confirm LmdbStoreConnection from candidate and ValueStore/LmdbRecordIterator from prior-D, not the current cancellation patch. Layered result pending.

### Candidate layered on prior-best D
Same selection passed: 93 found, 3 skipped, 90 successful, 0 failed; 141335 ms. See layered-D-safety.log and its exact code-source witnesses. This isolates the depth class family from cancellation changes. Parent/inventory can assemble combinations only from separately inventoried layers.

### Additional memo-replay qualification
Enabling only nativeValueMemo.testProbe was insufficient: candidate and unchanged D both failed the same three tests (two forced irKernel COMPILE_PENDING errors, one memo-hit barrier assertion). Logs retained as layered-D-memo-replay.log and baseline-D-memo-replay.log. This does not establish candidate regressions or passes. Rerunning both with validation prerequisites nativeValueMemo.enabled=true and janinoCodegen.synchronous=true, testProbe=true. No assertions changed.

### Final gated comparison and handoff
Source-defined flags verified in LmdbNativeJaninoCodegen.SYNCHRONOUS_PROPERTY and NativeValueMemo enable configuration. With testProbe=true, nativeValueMemo.enabled=true and janinoCodegen.synchronous=true on both exact runtimes:
- unchanged D: 3 found, 2 successful, 1 failed;
- depth-on-D: 3 found, 2 successful, 1 failed.
The compiled memo replay and reopening-failure caller-context tests both pass. The grouped test fails identically on both at assertMemoReplay line698: `never request replay after exposing a row ==> expected: <0> but was: <1>`. This is an unresolved existing fixture/path limitation, not a passed grouped replay qualification; no production workaround or weakened assertion. See *-memo-replay-validation.log. Initial incomplete-flag failures retained separately.

Source/classes are frozen and hash-verified, diff check clean. No publication or benchmark performed by this worker. CPU/build lock released after all owned JVMs completed. Next in_progress step is parent/inventory artifact acceptance and matched performance qualification; no measured gain claimed.

### Grouped fixture triage correction
The expected-zero/actual-one assertion at line 698 checks the number of consumed rows at the memo-hit barrier, **not a replay-request counter**. It fails before the test submits the growth writer. These logs therefore establish incomplete grouped replay qualification, not an observed replay-safety violation. See GROUPED-REPLAY-TRIAGE.md. Same-row duplicate-value reuse bypassing a memo lookup is a plausible explanation for a later first hit; runtime confirmation is still needed. The assertion and source were not weakened.
