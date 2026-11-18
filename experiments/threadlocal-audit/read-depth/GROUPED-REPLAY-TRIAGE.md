# Grouped replay qualification failure: read-only triage

2026-11-18. No production/test edits, builds or executions. Both immutable prior-best D and depth-layer AD fail the same gated case; this is an unresolved qualification, not a full pass or an established holder regression.

## Established from logs and source

`baseline-D-memo-replay-validation.log` and `layered-D-memo-replay-validation.log` each execute three gated memo-replay tests with nativeValueMemo.testProbe=true, nativeValueMemo.enabled=true, janinoCodegen.synchronous=true. Both ordinary compiled memo replay and replay-open-failure caller-context tests pass. `compiledGroupedDistinctIsRecheckedAcrossMapGrowth` fails at test line698 with expected0/actual1.

**The value 1 is not a replay counter.** It is test-local AtomicInteger `observed`, incremented immediately after the consumer's `result.next()` returns. The assertion runs after `NativeMemoReplayProbe.awaitHit()` succeeds. Only AFTER this assertion does the test change the scanSources property and submit its growth writer. The actual replayAccepted metric is checked later in `awaitReplayAccepted`, and again at the test's end. Those checks are not reached in this failing test body.

Consequently the assertion wording “never request replay after exposing a row” describes a fixture precondition. This failure establishes that the memo-hit pause occurred too late for the intended zero-output replay test. It does not establish that the system accepted or executed replay after exposing a row. The seeded store may perform unrelated growth; the specifically orchestrated concurrent writer is not yet submitted.

## Probe semantics

`NativeMemoReplayProbe` installs a global test-only NativeValueMemo.testHitObserver. On the first actual memo **hit**, it records compiled/grouped counters, counts down `entered`, and awaits release. It does not stop on first admission, first memo miss, first grouped certificate, or first source row. NativeValueMemo.value invokes the observer only in its hit branch after ID/mask/version checks.

Latch ordering makes this more than an arbitrary counter-read race: the first-hit callback publishes entered before waiting; it does not guarantee no earlier result was consumed. observed is atomic. With the consumer blocked in that hit callback, one previously completed row is a coherent possible state. Callback stack and query/thread/memo identity are not printed at this failing point, so whether this exact route occurred still needs evidence.

## Leading source-backed hypothesis: within-row reuse moved ahead of the memo

The grouped fixture seeds each subject with ?v and ?tag bound to the same repeated typed literal, and selects `DISTINCT ?s ?v ?tag` with no ORDER BY/LIMIT. The non-grouped fixture selects ?s/?v with ORDER BY/LIMIT200, which may materialize values while buffering before exposing anything.

Current `NativeProjectedBindingSet.collectMaterializationValues` first calls `findMaterializedDuplicate(i)`; only if none is found does it call `memo.value`. The direct `value(index)` accessor does the same. Duplicate lookup requires equal native ID, equal position mask, and a nonnull already materialized value. `LmdbStoreConnection.openEvaluation` calls this collector at the Sail boundary and then detaches the row.

If ?v and ?tag have equal IDs/masks on this plan, the first row's repeated literal is reused locally without a second memo lookup. With no earlier operator-level memo hit, the first row can therefore finish with memo misses only. The second row's repeated literal is the first genuine memo hit, after observed became1. This explains the exact symptom and why assigning equal literal values does not force an intra-row memo hit anymore.

This is a **hypothesis**, not a proved runtime trace. Confirm actual slot masks, collector path, callback stack and first-hit ordinal. Grouped certificate use itself is not proved by this failure: its assertion is later in `assertGroupingRechecked` and never reached. Global/static codegen state and class-order effects also remain possible, especially given earlier grouping-route instability; both runtimes share that state shape.

## Narrow next reproduction, after measurement lock release

1. Run only this grouped case in a fresh JVM on exact D, then exact AD, identical complete flags. Keep current zero-row assertion unchanged. Preserve code origins and test/runtime hashes.
2. Add untimed test-scope diagnostics at the first hit: thread, memo identity, stack, total probes/hits if exposed read-only, compiled/grouped tracker/check deltas; report observed at barrier. Record row ordinal and bound slot ID/mask equality at collector entry via a diagnostic seam, not extra value accesses that themselves trigger hits.
3. Distinguish first hit in Sail collector, grouped tracker/operator, or unrelated worker. Prove initial grouping certificate actually used before treating this as grouped replay coverage.
4. Run alone and after the two other gated methods. If outcome differs, isolate static plan cache/feature initialization and global observer lifecycle before changing query shape.
5. Once timing of first hit is proved, redesign fixture to cause a real memo hit while still inside unexposed execution, or add a dedicated pre-exposure grouped-admission barrier. Do not prewarm/manufacture memo entries if preserving the existing real-hit contract. If adding a blocking operator, prove the compiled grouped certificate still executes rather than testing a different plan.
6. Keep a separate negative test for growth after availability/row observation: it must not accept replay for the exposed result. That is distinct from repairing the positive pre-exposure replay fixture.

## Unsafe shortcuts rejected

- Do not change expected observed from0 to1 and continue with the writer: that abandons the intended unexposed replay precondition.
- Do not report actual1 as replayAccepted1, or call it proof of replay-after-exposure.
- Do not remove duplicate-value reuse or disable current optimizations just to make a formerly convenient memo-hit barrier early again without labeling the changed route.
- Do not add ORDER BY blindly: it may suppress or change compiled grouping and test another execution path.
- Do not infer safe grouped replay from the two non-grouped passes or from identical failure on D/AD. The grouped qualification remains incomplete until a valid pre-exposure scenario runs through the later counters, result equality, budget and spill cleanup assertions.

## Subsequent grouped-fixture qualification

The later [resource-restoration investigation](../resource-restoration/GROUPED-FIXTURE.md) confirmed the delayed memo-hit and availability-observation assumptions and repaired the Sail-level fixture without changing its query or weakening replay assertions. Its final gated selection passes all three cases on the incumbent, depth-only, and depth-plus-resource configurations with the separately identified test seam. This follow-up does not rewrite the historical outcomes or make the earlier selected suite a full repository pass.
