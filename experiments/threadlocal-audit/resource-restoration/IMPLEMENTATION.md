# Static external-initializer null restoration

Description: retain the existing static ThreadLocal slot with a null value instead of removing it when restoring an absent external initializer. No scope fusion, frame/token reuse, ownership change, callback movement, or cancellation changes. Candidate independently layered on holder-depth AD, not slower cancellation.

## Living steps
1. completed: add semantic/retention and structural hit-proof tests; capture baseline and expected structural red when CPU lock is available.
2. completed: change only QueryExecutionContext.setExternalValueInitializer null restoration; run matching tests and relevant replay/lifecycle suites.
3. in_progress: freeze exact QueryExecutionContext class family; validate isolated immutable AD and hand off for prespecified factorial measurement. No publication.

## Exact contract and memory cost
EXTERNAL_VALUE_INITIALIZER is one static plain ThreadLocal without initialValue override. Its .get() already returns null on empty state. All readers use the value, not map-entry presence. Keeping null retains one ThreadLocalMap Entry/slot per live thread that used this static key, but not a Consumer or resource owner through that value. Removal can shrink effective occupancy/clean stale neighboring entries; set(null) changes this cleanup behavior. We must not claim identical memory footprint, only unchanged owner retention and observable scope semantics.

Preserve activation AtomicBoolean, identity guard, current-thread lookup, previous initializer restoration, nested identical-consumer tokens, replay worker capture/install/restore, setup-failure unwind and null capture fast path. No general replacement of remove in per-store keys or replay contexts.

## Ownership
Native-token worker initially owns build/test CPU slot for unrelated baseline fixture work. This worker may prepare tests/plans but does not build until explicitly handed the slot. Inventory owns benchmark PLAN/scripts; no edits to its files. Prior dirty files and frozen artifacts remain untouched.

## Prepared test matrix (before production edits)
- Existing ExternalValueInitializerContextTest: nested exception restoration and RDF value/bindings identity; captured worker callback failure restores a custom previous consumer and empty context.
- New ExternalValueInitializerRestorationTest: bytecode structural red requires setter's ThreadLocal calls to be exactly [set]; same-consumer tokens/idempotent close; baseline wrong-order token behavior on a disposable worker; worker-scope activation failure restoration; weak Consumer collection after released ordinary/replay activations while worker remains alive.
- Wrong-order test deliberately preserves baseline behavior: closing outer under distinct inner consumes its token without unwinding; inner later restores outer Consumer. It does not pretend this misuse is valid cleanup or repair it.

## Baseline runtime
Inventory located baseline class origin in unchanged rdf4j-http-client-6.2.0-SNAPSHOT.jar (SHA256 3f2895934d180bb136a78a04e668de76479460c76ce1f37160db231038ce8195). Extracted all 11 QueryExecutionContext family files to baseline-layer. All match existing target/classes byte-for-byte; see baseline-class-comparison.json. Builds must never run install/overwrite that dependency. Candidate family overlay only.

CPU lock is still native worker's; no compile/tests or production changes performed yet.

## Baseline evidence (before production edit)
Command: mvn.sh -o -pl core/http/client -Dtest=ExternalValueInitializerContextTest,ExternalValueInitializerRestorationTest test
Surefire: 7 tests, 1 expected structural failure, 0 errors. Six semantic/ownership tests pass, including collection while worker stays alive. Structural red: expected [set], actual [remove, set] in setter bytecode. Logs red.log and red-reports; initial-evidence appended. All 11 recompiled baseline context classes still exactly match immutable dependency (baseline-recompiled-comparison.json).

Minimal patch now replaces only setter conditional with unconditional set(initializer). Null restores remain null, non-null restores unchanged. AtomicBoolean/identity checks and every caller/close order remain unchanged.

## Current-source HTTP green and freeze
Command: mvn.sh -o -pl core/http/client -Dtest="$(cat experiments/threadlocal-audit/resource-restoration/http-selection.txt)" test
```
Tests run: 17, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```
Includes both initializer classes plus QueryReplayWorkerScopeTest and QueryRequestContextTest. Independent source/test reviewer found no blocker. Weak-owner GC check passed baseline and candidate while worker remained alive; this checks owner retention, not equal footprint or scheduling-independent collection guarantees. Frozen11-class QueryExecutionContext family in layer/, final source/test in final-source/, SHA256 inventories and semantic.patch retained. Dependency jar hash unchanged. CPU handed back to native worker for their remaining fixture validation before isolated AD+resource LMDB tests.

## First isolated AD+resource run
JUnit completed: 110 found, 3 gated skips, 107 started/successful, zero failures (141485 ms). Candidate QueryExecutionContext, holder-depth LmdbStoreConnection and immutable D ValueStore/LmdbRecordIterator origins verified in log.

Harness mistake: while the JVM was consuming a shell here-document, this worker edited the launcher to add two further origin witnesses. After successful tests, the shell resumed at the changed file offset and failed with a trailing syntax error (exit2). No production/class artifact changed. Preserved layered-AD-safety.log as-is. Current launcher passes bash -n; it is now frozen for clean-exit rerun. Do not describe this command as BUILD SUCCESS. CPU handed to native worker to finish the revised test-only memo observer snapshot, then rerun the frozen launcher.

## Final clean isolated run and gated replay qualification
Frozen launcher SHA256 `9db81ea20d90a1167702ed5918a26d0c4832896a7238669772b2ba1a598bb659`, verified unchanged before/after. No active-script edits.
```
110 tests found; 3 default-gated skipped
107 tests started; 107 successful; 0 failed
launcher_exit=0
```
Evidence: layered-AD-safety-clean.log (141912 ms). Code sources explicitly show resource QueryExecutionContext layer, holder-depth LmdbStoreConnection layer and original D NativeValueMemo/ValueStore/LmdbRecordIterator, with no cancellation or test-seam layer mixed into this main run. Earlier post-test launcher error remains recorded separately, not counted as a clean command.

Final corrected test-only memo callback fixture, with single-snapshot observer reads and deterministic bytecode validation, separately passed gated3 on D, AD and AD+resource. Evidence from independent native worker: grouped-fix/final-snapshot-{a,ad,adr}.log, plus final source/class manifests. The three assertions cover initial compiled memo recreation across growth, restoration of caller context after reopen failure, and grouped certificate recheck with zero rows/availability exposed before accepted growth and no lost/duplicated results. This uses a test-only NativeValueMemo family overlay and repaired fixture; **never include grouped-fix/classes in timed runtimes**.

Final source SHA256 `5982bb027c0a052c4d3af62a4884725da99603b6a65c79f6a589f3a1bf3e6544`; all11 candidate class hashes reverified. Source diff check clean. CPU/build released to inventory; no running JVM. Step3 remains in_progress only for parent/inventory acceptance and matched measurement. No publication, speedup claim, or full-module/repository pass claimed.
