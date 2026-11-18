# Grouped replay fixture repair

Living plan. No weakening zero-exposed-row assertion, fake memo warmup, or ORDER BY removing grouped route.

1. In progress: reproduce first genuine hit with diagnostic ordinal/stack and ID/mask/duplicate evidence on immutable D and depth layer.
2. Pending: select minimal test-only pre-exposure repair preserving actual compiled grouping/recheck evidence.
3. Pending: gated baseline/depth verification, exact tests and handoff.

Production resource/context files are owned by another worker and will not be edited here. Initial CPU/build lock is assigned to this worker; all runs use canonical native-benchmark.lock. Temporary diagnostic source/class overlay is explicitly labeled and never benchmarked or published as production.

## Diagnostic RED evidence
Temporary overlay compiled from unchanged NativeProjectedBindingSet plus logging only, and NativeMemoReplayProbe plus first-hit stack/snapshot logging. Same original failing test on both immutable D and depth AD, complete required flags, retains expected-zero assertion:
- first collector IDs [642,66180,66180], masks [1,4,4]; index2 reuses index1 without memo lookup;
- second collector IDs [770,66180,66180], same masks;
- first real memo hit snapshot probes4/hits1/occupied3, stack from collectMaterializationValues; consumed-row assertion fails actual1.
Logs grouped-diagnostic/{baseline-D,depth-AD}.log. This confirms first-hit barrier moved to second row due within-row reuse on both routes. No writer was submitted before the failure.

Potential repair submitted for parent approval: grouped-specific first actual memo access barrier, verifying compiled bind+group tracker consumption; keep existing real-hit barrier for non-grouped tests and later replacement-hit proof. Requires tiny gated test observer in NativeValueMemo, not a resource/context change. Dispatch selection barrier alone rejected because group tracker is constructed after selection returns.

## First repair attempt and true observation boundary
Parent approved test-only populated access seam AFTER a completed eligible lookup, with actual entry/budget and compiled/grouped assertions. First repository fixture run passed zero-consumed-row precondition but failed replayAccepted: writer completed without replay. Preserve grouped-fix/baseline-D.log. The raw source.hasNext had already published availability before collector populated the memo; zero consumer next calls alone was insufficient.

Minimal boundary repair: grouped test keeps identical SPARQL/IR shape but evaluates directly through Sail under captured forced-irKernel context, and makes its first pull next() without a preceding public hasNext(). SailBaseIteration.next delegates directly under beginPull; ReplayableIteration.next executes currentDelegate.next and value capture within executeTrackedRead before markExposed. Later availability checks run normally. This is specifically a Sail-level pre-exposure replay contract test, not a repository/Workbench end-to-end test. Non-grouped repository genuine-hit tests remain unchanged.

Initial D targeted green: grouped-fix/baseline-D-direct-sail.log, 1/1 pass. Real memo occupied1/reserved294976, initial compiled/grouped tracker consumed; replay CHECKS1→2, TRACKERS1→1 (new scan declines old proof), irKernel→irKernel and two memo contexts. Existing output5000, replayAccepted+1, budget/spill cleanup and zero-output assertions all retained and pass. Follow-up final source adds explicit live versioned-entry assertion and clearer first-pull boolean; final D/AD/full-gated validation pending.

CPU handed to resource-restoration writer after first targeted green. No test seam family permitted in timed resource primary layers.

## Final validation
Final source (including live versioned-entry assertion and firstDirectPull flag) compiled with Temurin25 javac --release25 against immutable D dependency/runtime classpath, into grouped-fix/classes. No diagnostic NativeProjectedBindingSet overlay used in final validation. Full three gated methods ran on both immutable D and depth AD in fresh JVMs under canonical lock, with:
- rdf4j.lmdb.nativeValueMemo.testProbe=true
- rdf4j.lmdb.nativeValueMemo.enabled=true
- rdf4j.lmdb.janinoCodegen.synchronous=true

`grouped-fix/final-a.log`:3 found/3 successful/0 failures/0 skipped.
`grouped-fix/final-ad.log`:3 found/3 successful/0 failures/0 skipped.
Launchers select initiallyCompiledMemoIsRecreatedAcrossUnobservedMapGrowth, replayRestoresCallerContextWhenReopeningFails, compiledGroupedDistinctIsRecheckedAcrossMapGrowth. Final validation classpath: grouped-fix/classes prepended to read-depth/cp_a.txt or cp_ad.txt. The only production family overlaid is NativeValueMemo with the gated seam; context/read-depth families remain those of the selected immutable arm. This is selected gated qualification, not full module/repository validation.

Exact source and final class manifests: grouped-fix/{source-sha256,class-sha256}.txt. CPU handed to resource writer for resource-layer safety. No publishing by this worker.

## Exact changes and limits
- NativeValueMemo.java: adds static testPopulatedAccessObserver and invoke it only behind existing static-final TEST_PROBE gate, after completed access with nonempty admitted memo. Ordinary benchmarking must continue to use unchanged frozen NativeValueMemo family, not this test layer.
- NativeMemoReplayProbe.java: optional grouped population barrier; asserts real compiled bind, consumed grouped certificate, nonempty charged memo and a live known-semantic-version Value. It records original memo identity, while genuine hit observer still records replacement hits. Original non-grouped hit-barrier behavior is preserved. Close clears both observers.
- LmdbMapGrowthReplayLifecycleTest.java: grouped case opts into populated barrier and direct Sail first-next consumption. No initial public availability observation, no synthetic memo seed and no ORDER BY/producer shape change. The ordinary repository tests remain repository-level. Failure-at-replay-open and caller-context restoration proofs remain unchanged.

This repairs a fixture assumption invalidated by within-row duplicate reuse and public availability publication; it does not change production replay eligibility or claim previously observed replay-after-exposure corruption. Earlier failed logs remain intact.

## Observer teardown refinement (pending final validation)
Review found double volatile reads could race probe.close clearing a hook between nonnull check and invocation. Both existing hit hook and new populated hook now snapshot the observer once inside TEST_PROBE before checking/invoking. A teardown racing an already captured callback may allow that callback to complete, but cannot produce the null dereference. Deterministic javap field-read count proof and final gated D/AD/resource reruns pending CPU handoff; old logs intentionally retain earlier seam evidence. No timed-layer change.

### Final observer-safe qualification
`grouped-fix/run-final-validation.sh` compiles exact final seam and tests, holds canonical lock, verifies bytecode, and executes three fresh gated test JVMs. Exit0. `final-snapshot-a.log`, `final-snapshot-ad.log`, `final-snapshot-adr.log`: each3 successful/0 failed/0 skipped on D, depth AD, and AD plus external-initializer resource layer respectively. `observer-check.log` verifies exactly one getstatic for each volatile test hook in value(long,int); manual disassembly confirms accept invokes saved local reference. No flaky scheduling-dependent race test needed. Prior manifests preserved as pre-snapshot-*; current source-sha256/class-sha256 reflect final code. CPU handed back for writer's clean main safety rerun. Test seam still excluded from timed layers.
