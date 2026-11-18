# Capture native co-scan cancellation at its execution boundary

This living ExecPlan follows `.agent/PLANS.md`. The goal is to remove repeated native-token and probe-deadline ThreadLocal lookup from type-matrix co-scan loops without removing polls or changing callback scopes. This is the first narrow slice, not a claim that all query ThreadLocals are eliminated.

## Progress

- [x] Inspect native co-scan and worker dispatch.
- [x] Add checkpoint contract tests before implementation.
- [x] Qualify original pre-change cancellation tests (3 pass; later discovered serial fixture could hit adjacency before coScan).
- [x] Implement explicit checkpoint propagation and verify tightened route-specific tests.
- [in_progress] Parent: broader validation and matched route-specific measurement; no throughput claim yet.

## Context and orientation

`LmdbNativeTypeMatrix` evaluates an aggregate by scanning subject-ordered quad records. Its `coScan` and `flushRun` loops call `pollCancellation` after each tick; the helper masks ticks to 1024 operations, then calls `LmdbNativeProbeDeadline.poll` and `NativeCancellationToken.checkCurrent`. Both perform a ThreadLocal lookup. Parallel co-scan dispatch already captures `queryCancellation` and `probeParent`, installs these in worker scopes, then discards the explicit references when entering the scanning helpers. Serial co-scan can capture them at entry.

## Plan of work

Introduce a package-private immutable `NativeCancellationCheckpoint` holding the native cancellation token and speculative deadline. Capture once at a serial execution boundary, or construct directly from already captured worker references. Its masked poll preserves the existing order: probe first, native query token second. Its unconditional query check is used at coarse range boundaries. Pass this checkpoint through coScan and flushRun. Keep the worker ThreadLocal scopes because indirect source operations and nested callbacks still depend on them. Never store a checkpoint on a reusable query plan or shared source.

## Validation and acceptance

Before production edits run the existing `LmdbNativeTypeMatrixCancellationTest` selection and retain its report. New focused tests cover explicit-token precedence over conflicting ambient scope, later cancellation visibility, null contexts, probe-before-query ordering, masked tick semantics, and token ownership. Repeat the existing tests afterwards, including parallel co-scan and resource closure. Parent owns root installation and build locks; do not run overlapping Maven or benchmark processes. Use JDK25 and module-only tests, no `-am` or `-q` when tests are enabled. No throughput claim until an actual co-scan workload is route-verified and matched before/after on this replacement environment.

## Surprises and discoveries

The compiled kernel already receives a `KernelCancellation`; this candidate is an interpreted aggregate path, not a fix to every compiled kernel poll. The ThreadLocal check is masked, not once per row, even though the helper is invoked per row.

## Decision log

Preserve ambient scopes for callbacks rather than assuming explicit loop polling makes their scope unnecessary. Preserve poll cadence, exception attribution, probe capacity behavior and cleanup. This change cannot substitute the native token for the independent enclosing lifecycle cancellation signal.

## Outcomes and retrospective

Implemented narrow co-scan candidate, qualified original and tightened integration tests, and verified seven explicit-signal contracts. No performance evidence yet.

## Execution evidence

Commands use `experiments/threadlocal-audit/mvn.sh` (JDK 25, Maven 3.9.11, project-local Maven repository, fresh executor proxy settings).

1. Root `maven-build.log` captured RED compilation: planned `NativeCancellationCheckpoint` absent.
2. Temporarily rename new checkpoint test to `.java.pending`; run `-pl core/sail/lmdb -Dtest=LmdbNativeTypeMatrixCancellationTest test`: **3 tests, zero failures/errors/skips**, `native-checkpoint-baseline.log`. Restore new test.
3. Implement checkpoint and coScan/flushRun propagation; run `-pl core/sail/lmdb -Dtest=NativeCancellationCheckpointTest,LmdbNativeTypeMatrixCancellationTest test`: **10 tests, zero failures/errors/skips**, `native-checkpoint-green.log`.
4. Add deterministic probe-trip integration coverage: `native-checkpoint-probe-green.log` preserves **one failing assertion**, because serial source's adjacency prefix fixture cancelled query before coScan. This uncovered weak original route coverage (zero statement reads satisfied `< ROWS`). Decline prefix plans in a dedicated coScan source and require reads reach cancellation threshold. Keep predicate-precompute fixture intact. Re-run recorded in `native-checkpoint-route-green.log`.

Root Spotless reformatted 26 unrelated existing files during qualification; those are not part of this candidate. No semantic change to those files is intended. Parent owns their reconciliation.

5. Corrected route-specific run: **11 tests, zero failures/errors/skips** (7 checkpoint contracts + 4 integration tests, with the fourth exercising both serial and parallel probe trip). Log: `native-checkpoint-route-green.log`.
6. Reconstructed original production `LmdbNativeTypeMatrix.java` from `ba74f478` in a separate temporary source folder. Compiled with JDK25 `javac --release 25 -proc:none -implicit:none -cp "$(cat native-checkpoint-classpath.txt)" -d baseline-type-matrix .../LmdbNativeTypeMatrix.java`. This emits original outer and all nested classes; `baseline-type-matrix.sha256` records hashes. `baseline-type-matrix.javap` contains **zero** references to `NativeCancellationCheckpoint`. No production source restoration or target-class swapping occurred.
7. Ran the **tightened** four integration tests against the original bytecode layer, before current test/dependency classpath, via `CheckpointTestLauncher`: **4 passed, zero failures/skips**. Log: `native-checkpoint-tightened-baseline.log`. This repairs the original baseline's insufficient serial hit proof. Original and modified implementations preserve serial/parallel query cancellation and probe behavior.

The untracked `baseline-type-matrix/` directory is the valid baseline bytecode overlay. The earlier partial `baseline-classes/` copy was taken after Maven cleared its classes and is **not usable**. The exact local Surefire classpath is saved in `native-checkpoint-classpath.txt`; it is local evidence, not a portable dependency manifest. The launcher source is saved beside this plan. Parent resumed ownership of Maven/JVM execution after these checks.

## Null-inheritance review correction

Parent review identified that `inherit(null)` intentionally leaves an existing worker ambient scope unchanged. A shared dispatch checkpoint built from null signals would ignore that ambient signal, unlike the old loop's ThreadLocal check. The final candidate therefore constructs its worker checkpoint **after** replay/probe/query scopes are installed. `captureMissing(query, probe)` uses each explicit non-null dispatch reference and resolves only missing references from the effective ambient scope, once at that boundary. Direct-constructor null semantics remain unchanged (absent signal).

A new contract test exercises both-null, query-only and probe-only supplied signals, explicit precedence over cancelled/tripped ambient signals, and unchanged ambient scopes. Before implementation a direct JDK25 compile of the test failed with four missing `captureMissing` symbols (`native-checkpoint-null-red.log`). The earlier parent safety suite passed 180 tests. The same `run-safety.sh` is being rerun with the new contract test; `safety-suite-fallback.log` records its result. No benchmark or other Maven process overlaps this run.

Final null-fallback safety result: **181 tests, zero failures, errors or skips**, BUILD SUCCESS, `safety-suite-fallback.log`. The same ten selected test classes previously passed 180 tests before the extra null-fallback contract. Final candidate has eight helper contract tests and four co-scan/precompute integration tests; broader suite covers worker cancellation, row cancellation, filtered-key cancellation, shutdown, replay exposure, coordinator and lifecycle behavior. No performance claim follows from these test timings. Maven/JVM lock released to parent and benchmark owner after completion.
