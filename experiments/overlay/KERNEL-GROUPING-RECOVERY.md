# Compiled grouping recovery experiment, 2026-11-17

## Description and status

Recovered the opt-in serial compiled producer certificate, its callback exclusion, and existing tests. This is still experimental and not merge-ready. No issue number was supplied, so commits use GH-0000. The source checkpoint was preserved by the parent through the GitHub connector as e277e0f35a13ae8fc90a6e0d8feceb2fe43fae63 (tree identical to local c677579e).

## Evidence

Fresh initial combined Surefire selection: 74 tests, 1 failure, 0 errors, 2 skips. Ordered DISTINCT (18), pure proof (8), parallel (17), replay admission (1), and replay lifecycle (29 including 2 skips) completed. The sole failure was the integration certificate witness: expected 1, actual 0; its 100k rows and checksum were correct and all 14 spill files were deleted. Report snippet in `grouping-recovery-tests.log`; the existing integration assertion was not weakened.

An isolated uninstrumented integration run passed: 100k rows, checksum 1220594000736094224, compiled IR, 1 certificate, no spill, zero retained ledger reservation; query time 1704.715 ms. A subsequent combined diagnostic run passed 29 tests. This variation means the integration fixture's eligibility is not yet stable across execution contexts. The producer certificate is intentionally narrower than all legal optimizer plans.

Two additional adversarial pure-proof tests verify rejecting emitted ORDER/LIMIT/OFFSET, an unprojected root, and opaque residual callbacks nested inside OPTIONAL. The diagnostic proof selection passed all 10 proof tests. Temporary diagnostic classes were kept separate or restored; no diagnostic prints/throws are present in production source.

## Full-scale attempted ABBA

Command: `bash experiments/overlay/run-kernel-grouping-abba.sh`.
Each standalone JVM uses identical current production classes and fixture: 2,136,000 triples, 8,000,000 distinct 30-column output rows, memo + partial-ready + bounded wide primitive DISTINCT, bulk spill writes + reusable miss probes, 640 MiB per-query budget and 2.5 GiB global budget. Zero warmups, one measurement per fork. JVMs acquire the shared benchmark lock.

| Grouping flag | Query ms | Certificate count | Spill files created/deleted | Result |
|---|---:|---:|---:|---|
| false | 110168.889 | 0 | 20/20 | exact checksum and compiled route |
| true | 107732.152 | 0 | 20/20 | exact checksum and compiled route; grouping witness failed |

Both used 671067808 bytes peak accounted memory and returned ledger usage to zero. Both checksums were -3441391656945048864. Since the enabled fork did not exercise grouping, the probe correctly stopped the ABBA driver. These timings are NOT a grouping speedup and are NOT a completed paired benchmark.

## Root-cause diagnostic

A separate throw-on-decline diagnostic ran only until bind-time certification (not timed). Actual 8M producer: `EnumerateAdjKeys`, serial compiled=true, external callbacks=false, canonical authority=true, terminal Emit. Current certificate only supports SipDomainProbe, hence its refusal is correct. Do not remove callback, alias, or canonical-domain checks to make the measurement pass.

## Plan

- completed: recover/checkpoint code, rerun focused and lifecycle tests, add adversarial proof coverage, attempt full-scale benchmark, identify no-hit producer.
- in_progress: design and reproduce support for fixed-adjacency EnumerateAdjKeys as a separate producer proof.
- pending: validate its bound snapshot key enumeration, canonical identity/unique contiguous grouping, immutable projected root and serial cursor; reject wildcard/mutable views and opaque tails.
- pending: rerun exact-result, resource-lifecycle and adversarial tests, then complete a fresh 8M ABBA only with successful grouping witnesses.

No full module/repository test pass or merge-ready claim is made. The previous 100k speedup cannot be extrapolated to 8M.

## Follow-on resolution

The fixed-adjacency extension and final verification supersede the pending items above; see [KERNEL-GROUPING-RESULTS.md](KERNEL-GROUPING-RESULTS.md). The completed 8M ABBA has positive grouping witnesses in both ON forks. The test-only 100k no-hit was traced to asynchronous adjacency availability and unsupported ScanQuad selection; targeted readiness and producer-specific setup preserve all assertions. Final focused validation is 57/0/0/0. Broader main comparison remains separate.
