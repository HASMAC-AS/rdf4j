# Measure continued memo admission with bounded capacity

This living plan follows `.agent/PLANS.md` and `AGENTS.md`.

## Purpose / Big Picture

Håvard asked to test whether continuing memo admission after an initially unrepetitive stream pays off when combined with lifetime and compact result optimizations. The memo reuses weakly held Values, avoiding some source calls without retaining the entire Value graph. This experiment must distinguish recovered reuse, allocation savings and actual elapsed-time improvement. Neither publication nor default-policy changes are authorized by this experiment.

## Progress

- [x] 2026-11-17: inspected immutable b8d2015ec1b722b71f9a8af9e50cd98558f7882a and current local 22b9bbd0; documented non-equivalence.
- [x] 2026-11-17: prepared eight dedicated in-repository policy contracts.
- [x] 2026-11-17: machine cleared; smallest red test captured (1 test, 1 expected policy assertion failure).
- [x] 2026-11-17: implemented policies, 27 focused tests passed, 12 exact-model phase traces passed, 24 synthetic timing JVMs completed.
- [ ] in_progress: extended lifecycle/replay qualification and actual compiled-query comparison coordination.
- [ ] Freeze classpaths and run matched combined compiled-query comparisons under the shared benchmark lock.
- [ ] Review results, retain local checkpoint and report limitations. No push.

## Context and Orientation

The repository is `<repo>`. Production ownership is limited to `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/NativeValueMemo.java`; tests belong in the same package under `src/test/java` as `NativeMemoPolicyTest.java`. Experiment artifacts belong in `experiments/overlay/memo-policy`.

Current NativeValueMemo is a reconstructed weak-only direct-map memo. It disables permanently if fewer than one quarter of the first 8,192 probes hit. Thereafter it resets per-window counters and can double from 4,096 up to 65,536 slots when a window has at least half hits and three-quarters occupancy. Its reservation includes both old and new arrays during resizing. It validates full position masks and semantic versions. This proposal changes no source lifetime, exposure, memory-refusal or identity behavior.

Published b8d2015e supplies a different NativeDirectValueMemo integrated through NativeResultMaterializer, absent here. Its cumulative-history growth and ownership representation differ. Its 32,768-row ordered benchmark is not our compiled eight-million-row benchmark. Do not transplant that patch or reuse its timings as local evidence.

## Plan of Work

First run `NativeMemoPolicyTest#continuedAdmissionSurvivesInitialColdWindow` against unchanged production. Persist its Surefire report and command log before implementing. Only after observing the intended assertion failure add two final constructor-captured selections: `rdf4j.lmdb.nativeValueMemo.admission=initial|always` (default initial) and `rdf4j.lmdb.nativeValueMemo.capacity=adaptive|fixed4096` (default adaptive). Reject unknown values. Gate only the existing low-hit cutoff and existing growth branch. Do not read properties per Value, change hashing, or add strong references.

Prepare diagnostic snapshots using existing totals plus current capacity/reservation and resize count. A snapshot request outside the hot loop must not mutate adaptation. If trace callbacks are necessary, gate them with a static-final disabled property; ordinary benchmark timings must have tracing disabled. Prefer no callback and untimed explicit checkpoints in the test harness. Collect weak-reference miss and lifetime behavior separately from strongly retained deterministic trace fixtures.

The four arms form a factorial comparison: initial/adaptive, initial/fixed4096, always/adaptive, always/fixed4096. Keep the best combined execution flags, output consumer and class bytes identical across these arms, changing only policy properties. Retain a frozen pre-change baseline to detect default-mode regressions. Keep snapshot and writer selections explicit rather than silently pooling different pipelines.

## Milestones and Concrete Steps

Milestone one proves the option does not already work. Builds/tests are prohibited until parent clearance because the shared machine is running the snapshot matrix and Workbench is queued. Once cleared, acquire `experiments/overlay/native-benchmark.lock` before compiling or running any JVM. Coordinate parent baseline class snapshots before modifying target directories. Use the existing local Maven/JDK setup after inspecting parent test logs. A nominal smallest-scope command from repo root is:

    mvn -o -pl core/sail/lmdb -Dtest=NativeMemoPolicyTest#continuedAdmissionSurvivesInitialColdWindow test

Do not use `-am` or `-q`. Preserve the actual executed command and report under this experiment, and append initial evidence to top-level `initial-evidence.txt` without overwriting prior evidence. Confirm the failure is the policy assertion, not compilation/setup. Then implement only the minimal two branch gates and rerun the same method, all policy tests and existing NativeValueMemoTest. Preserve final reports separately.

Milestone two verifies phase behavior without using mock overhead as a performance result. Retain returned Values in the trace consumer to eliminate GC randomness. Use an early-hot control; 32,768 unique IDs followed by 98,304 opportunities cycling 64 chosen distinct-slot keys; hot then unique then hot; and a deterministic growth-control sequence of one hot 8,192-probe window, one unique window and another hot window. Snapshot every 8,192 probes and phase boundary. Independently compute the expected map behavior; verify exact returned lexical/mask values and counts, not just checksums. Explicitly clear a weak representative in a separate safety test, and verify semantic mutation, different masks, memory refusal, close idempotence and zero reservation after close in each arm. Existing replay/callback/retained-row tests must run with nondefault policies as well.

Milestone three measures the real combined workload after qualification. The current reference runner is `experiments/overlay/lifetime/run.sh`; copy and adapt into this experiment without changing it. It runs ExperimentalProfileProbe with memo, partialReady, grouped compiled DISTINCT, widePrimitive, buffered spill and membership reuse enabled, 5 GiB max heap and 640 MiB query budget. Preserve the eight-million-row fixture and output checksum. Use isolated immutable class directories, equal warmup counts and balanced arm order, at least two complete balanced blocks initially. Log route, spill, memo activity, result correctness, timing and allocation. Run profiles separately from timing. If real data never triggers policy differences, report that rather than manufacturing a throughput claim. Parent owns Workbench end-to-end work; this task owns direct combined compiled-query comparison only.

## Validation and Acceptance

The smallest new test must fail before implementation and pass after. Default exact cutoff and adaptive growth must remain unchanged. Always must survive initial misses; fixed must never resize; construction must capture options once; invalid options must fail clearly. All policies must preserve exact masks, semantic invalidation, weak collection semantics and cleanup, including refusal during growth. Route/output gates must match across timing arms. Distinguish sample shares, allocation reduction and measured query latency. No significance or general speedup claims from noisy small cohorts.

## Surprises & Discoveries

The published memo differs structurally and in growth policy from our local implementation. The published always-admission late-repeat result recovered hits but increased primary latency 13.53%, while lowering allocation 2.47%. This motivates controlled combinations, not promotion. Current source has no NativeResultMaterializer and already uses weak references.

## Decision Log

2026-11-17: preserve defaults and weak semantics; expose two independent constructor selections to separate admission from growth. Rationale: Håvard requested the factorial experiment and combined interactions, not transplanting an unrelated implementation.

2026-11-17: prepare tests/docs only until clearance. Rationale: repository requires observed red evidence before production edits, and concurrent tests would contaminate active benchmarks.

## Idempotence and Recovery

Restore system properties in every test. Never delete untracked files or modify another worker's sources. Never overwrite frozen class snapshots or benchmark outputs; assign unique run names. Failed timing gates invalidate the run, not permission to relax correctness. Preserve initial failures and report setup blockers accurately.

## Artifacts and Notes

Red test ran against unchanged production: 1 test, 1 assertion failure, expected null but got low-repetition. Minimal production policy gates now implemented; green validation pending. Current evidence is source inspection only. The pending report is `core/sail/lmdb/target/surefire-reports/org.eclipse.rdf4j.sail.lmdb.evaluation.NativeMemoPolicyTest.txt`.

## Interfaces and Dependencies

Use existing JUnit5, Mockito and KernelRuntime.MemoryAccount; no new dependencies or public API. Policy selection is experimental internal configuration. Current NativeValueMemo package-private interface and all callers remain compatible.

## Outcomes & Retrospective

Preparation is complete; implementation, correctness, performance and local checkpoint are pending machine clearance. No result or speedup is claimed.

Revision 2026-11-17: initial self-contained plan and test-first queue, documenting shared-machine restrictions and incompatible published predecessor.

Revision 2026-11-17: 27 green tests and 24 synthetic phase JVMs completed; full candidate module frozen in candidate-lmdb-classes. Returned machine to Workbench.

Revision 2026-11-17: prepared run-lifecycle.sh and CompiledLateHotProbe.java; neither executed. Workbench owns the measurement window. After release, run lifecycle validation and qualify actual repository late-hot stream with route/counter/domain assertions before any timing comparison. Qualification uses existing testHitObserver only when testProbe is enabled; timing must disable it. Keep initial/adaptive defaults and frozen candidate bytes unchanged.
