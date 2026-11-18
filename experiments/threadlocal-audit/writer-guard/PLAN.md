# Skip writer-ownership ThreadLocal lookup when no writer exists

## Purpose

Profiles identify `ValueStore.isWriteTransactionOwner` as a frequent caller of `ThreadLocal.get` in regular materialization. Add only an early `writeTxn == 0` false return, then retain the entire original positive path, including the second `writeTxn` read and nonzero guard. This is a separate candidate from SOURCE_PULL consolidation; compare alone and in combination. No percentage improvement is assumed.

## Progress

- [x] Read-only ownership/concurrency and caller analysis.
- [x] Prepare focused test outside production/test source tree; parent granted lock.
- [x] Copy test and observe baseline RED: 6 tests, exactly 1 no-get assertion failure; other 5 semantic cases pass.
- [x] Add minimal guard; combined Maven safety selection passed 330 tests.
- [x] Guard-only layered runtime passed the same 330 tests; report clean patch, hashes and class layer to parent.
- [in_progress] Parent owns four-arm matched throughput/latency measurement.

## Constraints and rationale

The fields `writeTxn` and `writeTxnOwner` are plain, not volatile. `writerPublication` is volatile. This optimization must not claim a publication guarantee or replace association/admission rechecks. A false return observed while no writer exists grants no native access. The thread-confined ReadContext initializer only creates a default holder; it is never replaced or removed by production code, and no caller depends on this predicate initializing it.

The positive path intentionally reads `writeTxn` AGAIN after `readContext.get`. Keeping `transaction != 0` prevents an empty borrowed context from becoming an owner when a concurrent consumer clears the writer. Do not reuse the preliminary shared read.

## Test approach

Use a test-only reflective wrapper around the existing final ThreadLocal field to count accesses to the private ownership predicate. The wrapper delegates to the original holder, preserving its type and values. Restore the original field in every exit path. No production test hook is added.

Use real writer transactions, not fabricated native handles. For the controlled clear interleaving, the first instrumented get invokes a legitimate rollback exactly once, including a borrowed-writer-context variant. Never fabricate or temporarily hide native handles; nested rollback lookups delegate normally. Exercise regular and borrowed-writer variants. Additional tests use a real borrowed writer context and nested restoration.

Existing semantic safety selections include ValueStoreTest's uncommitted-ID/rollback-cleanup/commit-reset/association tests, writer admission restart, close versus writer commit/growth, ValueStoreIdCacheTest and ValueStoreStandaloneMaintenanceTest, plus replay/shutdown suites. Parent owns benchmarking and source/Class snapshots.

## Execution

No source edits or Maven commands until parent grants ownership. Use existing JDK25/Maven wrapper, module-only tests, no `-am`/`-q`. Add copyright/SPDX and Codex signature before copying new Java source; run repository copyright check before benchmark window. Preserve unrelated formatter diffs and all raw logs. Record the semantic patch separately from ValueStore formatting changes.

## Outcomes

Implementation complete and correctness-qualified; no performance claim. Exactly three guard lines added before the unchanged positive path. The new focused baseline ran six cases: five passed and only the no-writer no-get assertion failed (expected0, actual1). Combined SOURCE_PULL+guard Maven run passed330 tests with zero failures/errors/skips. Guard-only layered runtime with original RepositoryValueIteration passed the same330 tests using the exact configured Mockito5.24.0 agent and4GiB heap.

The first direct launch omitted the Maven-configured Mockito agent, causing13 Mockito initialization failures (317 tests passed); preserved as guard-only-missing-agent.log. Adding the existing agent, not changing security/attach settings, resolved this. The intermediate8GiB run also passed330, then the final4GiB run matched module heap configuration.

The guard-layer contains82 ValueStore outer/inner classes with no missing/extra names. Only ValueStore.class differs from the frozen baseline; all81 inner classes are byte-identical. guard-only.patch is the three-line semantic diff against the original frozen source, excluding unrelated working-tree formatting. Copyright checker reports pre-existing invalid headers elsewhere, not the new test; no automatic header rewrites were used.

All JVM/Maven work finished and CPU/source lock released to the parent for measurement.

## Evidence files

- red.log: baseline Surefire6/1/0/0, intentional no-get failure.
- green-safety.log: combined Maven330/0/0/0.
- guard-only-safety.log: explicit ValueStore guard-layer and RepositoryValueIteration baseline origins,330 successful/0failed,4GiB heap.
- safety-selection.txt and run-layered-safety.sh: exact selections and reproducible layered launcher.
- guard-only.patch: semantic publication patch against source-pull/ValueStore.baseline.java.
- class-prefix-comparison.json and ../source-pull/guard-layer.sha256: complete class-layer inventory/proof.
- source-sha256.txt: frozen baseline, publishable semantic-only and working-source hashes.
