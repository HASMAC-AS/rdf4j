# Independent primary comparison reconciliation

**The complete cohort is valid, and the independent results reconcile with the frozen driver without numerical or execution-route divergence. Both prespecified engineering screens fail.** No Java or frozen-code change was needed for this audit.

The pinned independent analyzer read raw data before the driver summary was opened. It validated all 36 fresh JVMs, 468 queries (288 warmups and 180 measurements), and 312 treatment-query pairs. The subsequent independent receipt audit matched all 324 per-round metric ratios, including 216 primary ratio values; all 324 per-JVM metric medians; every aggregate median and range; and all 18 screen decisions.

## Primary outcomes

Ratios pair the same fixture and round using the median of all five measurements per JVM. The table gives the median and full range of six paired ratios. Lower favors direct reuse.

| Fixture | Comparator | Latency ratio [range] | Allocation ratio [range] |
|---|---|---:|---:|
| fanout | ordinary | 0.938869 [0.904949, 1.014191] | 0.838266 [0.838224, 0.847811] |
| fanout | dense | 0.720358 [0.648404, 0.778009] | 1.001190 [1.001131, 1.001209] |
| shared-tail | ordinary | 0.931292 [0.759988, 0.999835] | 0.854791 [0.854773, 0.854795] |
| shared-tail | dense | 0.790969 [0.746203, 0.913119] | 0.973545 [0.973511, 0.973575] |

Direct versus ordinary fails because two fanout latency ratios exceed one: **1.006659** and **1.014191**. Both fixture medians pass the 0.95 latency threshold, and all allocation thresholds pass. Direct versus dense fails because fanout median allocation is **1.001190**, above the required 1.0. All twelve direct/dense latency ratios favor direct. These thresholds remain unchanged; a near miss remains a failure.

## Absolute observations

These are medians of six JVM medians. Their quotients are not the paired estimator above. Allocation uses decimal GB. Ledger values are exact reported byte peaks, not retained-heap measurements.

| Fixture | Arm | Total seconds | Allocation GB | Preparation ms | Ledger peak bytes | Source calls/query |
|---|---|---:|---:|---:|---:|---:|
| fanout | ordinary | 2.399233 | 1.630341 | 1.764441 | 2,432 | 2,770,101 |
| fanout | dense | 3.172454 | 1.365037 | 1.891369 | 2,264,968 | 1,147 |
| fanout | direct | 2.273520 | 1.366661 | 1.826934 | 147,536 | 33,386 |
| shared-tail | ordinary | 1.633235 | 1.320700 | 1.081096 | 1,472 | 2,113,601 |
| shared-tail | dense | 1.881426 | 1.159598 | 1.015421 | 272,948,260 | 32,833 |
| shared-tail | direct | 1.482211 | 1.128922 | 1.065981 | 147,536 | 172,090 |

Direct performs more resolver calls than dense on both fixtures while finishing faster. This measures the combined strategy change; it does not isolate transport bookkeeping, hashing, strong retention, or resolver cost causally.

## Routes, ownership, and map behavior

All 180 measured queries used normal AUTO row/join dispatch to `irKernel`, with one compiled native bind each and no interpreted or aggregate bind. Warmups contained 252 compiled binds and 36 interpreted binds. The 468 root audits reconcile one native compilation per query and zero generic outer roots. All matched query pairs preserve complete route and adjacency observations. Adjacency lookup hits were 55 per fanout query and 10 per shared-tail query.

Every fanout query returned 1,152,000 rows with checksum `3123987218479850048`; every shared-tail query returned 1,048,576 rows with checksum `8327963502356915200`. All 234 shared-tail queries passed the exact bag oracle. Every query finished with ledger endpoints zero.

The 156 direct owners align exactly with source-call intervals and semantic-compile ordinals. Control arms created no direct owner. The following pattern held for every direct query, including warmups:

| Per-owner field | Fanout | Shared-tail |
|---|---:|---:|
| Probes | 13,824,000 | 6,291,456 |
| Validated candidate hits | 13,790,614 | 6,119,366 |
| Occupied different-key probes | 32,405 | 167,994 |
| Source calls | 33,386 | 172,090 |
| Entries immediately before close | 981 | 4,096 |

Every table stayed at 4,096 entries of capacity, with an array charge of 147,536 bytes. There were no resizes, low-hit disables, scope clears, witness invalidations, unsupported admissions, rehash discards, or admission refusals. Final reported array charge was zero. These workloads therefore do not test the runtime benefit of growth or low-hit disabling. Candidate hits are not generally resolver calls avoided; exact miss/source conservation was observed specifically in these two fixtures.

**The ledger does not charge retained Values, lexical payloads, identity/owner graphs, or future lazy initialization.** The smaller direct/dense ledger peak is not evidence of a complete retained-memory bound.

## First-query preparation

The first query runs after fixture construction and excludes JVM launch and fixture setup. It is not part of the warm-latency screen. Values below are medians across the six first queries.

| Fixture | Arm | First-query seconds | First-query preparation ms |
|---|---|---:|---:|
| fanout | ordinary | 4.801314 | 65.387065 |
| fanout | dense | 7.404039 | 69.058892 |
| fanout | direct | 4.061346 | 58.564283 |
| shared-tail | ordinary | 3.298350 | 58.191179 |
| shared-tail | dense | 3.758951 | 67.669901 |
| shared-tail | direct | 3.013617 | 60.920032 |

The corresponding paired first-query direct/ordinary latency medians are 0.891382 for fanout and 0.909894 for shared-tail; direct/dense medians are 0.554387 and 0.821584. First-query preparation is variable: for example, the shared-tail direct/ordinary preparation ratios range from 0.837827 to 3.138335. The complete paired ranges remain in the independent report; no cold sample was removed.

## Supervision and evidence identity

All 36 claim/completion links and all 252 raw files named by completion receipts were independently checked. No incomplete, extra, retried, or imported slot was observed. Coordinator records report 36 separately observed terminal exits of zero; this child verified the recorded chronology and identity, not the original tool responses themselves. The 35 administrative inter-JVM gaps ranged from 2.654 to 20.183 seconds, with median 4.246 seconds. They were retained without exclusions or tuning.

The audit rehashed the independent analyzer's 184 input files, 25 watched identity/source files, and six experimental prefix classes. The original supervisor's complete classpath preservation receipt reports unchanged bytes; this audit did not repeat that historical full-runtime hash walk.

The only expected schema difference is route-histogram representation: the independent analyzer keeps `irOpened` and `irRows` and uses JSON signature keys, while the driver uses a narrower signature/count list. Common route signatures and counts match after explicit normalization, and the full raw samples match exactly. This is not a route divergence.

The complete results are in `independent-v2/independent-results.json` and `independent-v2/independent-report.md`. The compact adjacent JSON pins their hashes, the driver outputs, protocol, supervision receipts, and reconciliation counts. Historical, interrupted, diagnostic, and preflight samples are not pooled. No population confidence interval or production-promotion conclusion is claimed.
