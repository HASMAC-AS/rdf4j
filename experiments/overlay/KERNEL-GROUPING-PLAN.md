# Certify compiled root grouping for bounded DISTINCT

This living plan follows `.agent/PLANS.md`. Grouping means all rows with the same projected root identifier occur contiguously, allowing the residual DISTINCT set to be cleared when the root changes.

## Purpose

Avoid global wide-key spill for repeated nested OPTIONAL expansions while retaining compiled execution. Existing interpreted ordered DISTINCT saves memory but allocates heavily. This experiment remains opt-in through `rdf4j.lmdb.nativeDistinct.kernelGrouping`.

## Progress

- [x] Inspect actual fixture generated root.
- [x] Capture smallest query spill failure.
- [x] Implement conservative producer certificate checks.
- [x] Verify adversarial fallback, lifetime, replay and full-scale fixed-adjacency measurements.
- [x] Measure bounded compiled query benefit.
- [in_progress] Independent matched current-main baseline (separate worker coordinated by parent).

## Discoveries

The actual100k fixture generates SipDomainProbe as the outermost loop over dom0; v0 is the root key. It processes each domain key's complete continuation before advancing. No production changes have been made at this point.

## Decision Log

Support only final serial SipDomainProbe roots initially. Check the actual bound domain is strictly increasing, contains no sentinels, and maps identically to canonical RDF-term keys. Reject any later write to the key, opaque producers, aliases and parallel cursors. Attach the proof to the selected cursor, not a logically reordered plan. Keep ordinary global hash fallback unchanged.

## Context

Sources are under `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation`. LmdbNativeKernelExecution binds a lowered kernel then creates KernelRowCursor. LmdbNativeRowStep.acceptKernel currently returns a HASH NativeUnorderedInput. Its iterator creates NativeOrderedDistinctTracker after strategy selection. That tracker already supports PARTITIONED_HASH and ledger-safe clear.

## Work

First run NativeKernelGroupedDistinctTest with a2MiB query cap. Its100k-row30column nested OPTIONAL query must have exact checksum1220594000736094224 and compiled IR but currently spills, failing the no-spill assertion. Only after recording this failure, add a pure structural grouping analyzer and bound-domain validator, carry its slot in the serial cursor, then create a producer-specific partition plan in the consumer. Unit cases reject nonunique/unbound/aliased domains, overwritten root, nonprojected key, opaque/parallel paths. Run focused and nearby suites. Compare100k under2MiB then8m under640MiB only if proof and parity pass.

## Commands and acceptance

All JVMs acquire `../native-benchmark.lock`. Use existing `experiments/overlay/mvn.sh` (local JDK25 and Maven repository), offline module test compile and nonquiet Surefire selection. Preserve existing frozen snapshots. Require exact count/checksum, compiled route, zero ledger after close, spill cleanup and green adversarial tests. Do not claim private workload replication.

## Recovery

No dependencies or public API additions. All experiment source changes remain uncommitted for parent coordination. Do not reset unrelated report edits. Preserve red/green logs under experiments/overlay.

## Outcomes

Pending red test and implementation.

Red evidence: NativeKernelGroupedDistinctTest1 test,1 failure,0 errors. Correct100k rows/checksum and irKernel compiledBinds=1; expected0spill files, actual14. Full report retained in grouping-red-report.txt.

## Recovery checkpoint, 2026-11-17

Recovered source includes external residual-callback exclusion and eight structural proof tests. Fresh combined Surefire run found the 100k query still produces correct output but declines the certificate (0 trackers, 14 spill files). Earlier 1.55 s result predates the final rebuild and is not current validation. Investigating isolated fixture versus test-state effects before 8M measurement. The replay lifecycle test remains running. This is a non-merge-ready experimental checkpoint.

## Fixed-adjacency extension

The 8M witness declines because its real root is EnumerateAdjKeys. NativeProbe.adjacency explicitly returns an immutable revision-valid complete view retained until probe close. NativeAdjacency enumerates distinct keys. Compiled key-only and key/run enumeration completes the continuation for each key before advancing. Added a separate bound-view certificate: reject wildcard roots, validate all ordinal keys as strictly unsigned-ascending canonical identity keys without allocating a domain or retaining a new cursor, preserve projection/tail/serial/callback restrictions. Replay recomputes against the new bound view.

Red evidence: NativeKernelGroupingProofTest.fixedAdjacencyRootWithCanonicalUniqueKeysIsCertified, 1 test/1 failure, expected 4 actual -1, grouping-adjacency-red.log. Green initial selection: 12 tests/0 failures/0 errors, grouping-adjacency-green.log. Additional adversarial tests cover repeated/descending/aliased/sentinel keys, missing/unsupported view, per-snapshot recomputation, borrowed-view ownership, and cancellation before key reads.

Current validation step includes the full-scale witness-bearing ABBA. No performance claim until every enabled fork consumes the certificate.

Final outcome:57 focused tests passed after final formatting and full module test compilation. Map-growth replay rechecks producer and safely declines ScanQuad, never inheriting the old certificate. Four-fork8M comparison complete; see KERNEL-GROUPING-RESULTS.md. Current-main comparison is not yet available.
