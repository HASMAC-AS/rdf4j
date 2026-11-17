# Compiled producer grouping experiment

## Matched 8M result

Same experimental source (a655b62f functional implementation; independent test 68712e67 does not change runtime), four fresh serial JVMs, OFF/ON/ON/OFF, one measured execution each, no warmups. This compares the grouping flag on the experimental branch, **not current upstream main**. Both modes retain the reconstructed memo, partial-ready results, segmented/budgeted wide DISTINCT, bulk spill I/O, and reusable miss probes.

| Fork | Grouping | Query time | Accounted peak | Spill files |
|---|---|---:|---:|---:|
| 1 | OFF | 124.230 s | 671,067,808 B | 20 |
| 2 | ON | 37.283 s | 7,336,288 B | 0 |
| 3 | ON | 37.685 s | 7,336,288 B | 0 |
| 4 | OFF | 108.338 s | 671,067,808 B | 20 |

Mean: **116.284 s OFF vs 37.484 s ON**, about **67.8% faster / 3.10× speedup**. The large OFF-fork variation means these are an experimental comparison, not a precise universal speedup. ON reduces the shared ledger's accounted peak by about 98.9%; this is **not total JVM heap or RSS**. Thread allocation remains roughly 32.1 GB/query, so output materialization still dominates allocation.

Every fork produced exactly 8,000,000 distinct 30-column rows, checksum `-3441391656945048864`, selected compiled `irKernel`, and exercised memo hits. Every ON fork consumed exactly one producer certificate; OFF consumed none. All spills were deleted and ledger use returned to zero. Dataset: 2,136,000 triples; synthetic nested OPTIONAL fixture, not the user's private workload.

## Budget provenance

The test pins `-Xmx5g`, global query budget 2.5 GiB, and per-query budget 640 MiB. Those values reproduce this heap's defaults: `LmdbQueryMemoryManager.ProcessHolder` uses global `max(1 GiB, maxHeap/2)` and per-query `global/4`. **640 MiB is not a universal production default**; limits scale with heap and both have public system-property overrides.

## Proof and scope

Original SipDomainProbe-only proof failed the 8M hit witness because the actual producer was EnumerateAdjKeys. The fixed-adjacency extension validates the actual bound immutable revision-valid view. It requires canonical identity keys, strictly increasing unsigned IDs, a projected immutable root, supported continuation nodes, and serial compiled execution. Wildcard views, aliases, missing/unsupported enumeration, opaque callbacks, root overwrites, sorting/limit terminals, and foreign/parallel/scan producers decline to the existing global strategy.

The bind-time validation is O(number of root keys), constant auxiliary memory. It borrows no new persistent cursor and does not close the view owned by the existing query probe. This cost may be inappropriate for very large root domains or small LIMIT workloads (terminal LIMIT already declines); more workload coverage is needed before enabling by default.

## Validation discoveries

The 100k integration fixture initially raced asynchronous adjacency availability and sometimes selected unsupported ScanQuad. Results were still correct, but its certificate-hit assertion failed. The fixture now awaits the actual adjacency-ready signal before timing and explicitly targets adjacency lowering; separate tests assert ScanQuad cannot inherit a certificate. No production eligibility check was relaxed to fix this test.

Independent real-view test validates keyAt, cursor.advance, and fillKeys (widths 1/3/8/32) against the same 17-key base and 15-key deletion/insertion snapshot; the pinned old snapshot remains unchanged.

A direct 5,000-row map-growth test proves initial certified adjacency followed by a newly bound compiled replay producer. The replay can choose ScanQuad when its old adjacency view is unavailable. It rechecks and declines grouping rather than reusing the old certificate; both attempts remain compiled IR with separate hit-bearing memos and exact results. This is safe replay fallback, **not proof that grouping stays enabled on both attempts**.

Final formatting/build/test results are recorded below when complete. No whole-module, whole-repository, current-main performance, or merge-ready claim is made.

## Final verification

- Targeted Spotless formatting completed on six touched Java files.
- Offline LMDB module `test-compile` succeeded (634 production and 1,169 test source files).
- Final nonquiet Surefire selection: **57 tests, 0 failures, 0 errors, 0 skips**. Includes 16 proof cases; 100k grouped integration with spill/budget assertions; 17 parallel cases; 18 ordered-DISTINCT cases; real immutable adjacency-domain test; replay context admission; and three actual map-growth/memo replay tests (grouping fallback, compiled memo replacement, reopening failure context restoration).
- The grouped replay case observed initial trackers=1, final trackers=1, checks=1→2, two compiled IR binds, two distinct hit-bearing memo contexts, exact 5,000 rows, restored caller context, matching spill create/delete counts, and returned query-ledger usage.
- The earlier broader lifecycle selection independently completed 29 tests with no failures/errors and 2 intentional skips. Its former synthetic integration failure was fixed by adjacency-readiness/producer-specific fixture setup, not by weakening result or certificate assertions.

Commands: `run-grouping-final-tests.sh`, `run-grouping-replay.sh`, and the original `run-kernel-grouping-adjacency-abba.sh`. Measured four-fork data is in `kernel-grouping-abba-summary.csv`. Tests use explicit synchronous compilation flags. Some proof tests intentionally exercise fallback; a green selection does not imply every query receives a grouping certificate.
