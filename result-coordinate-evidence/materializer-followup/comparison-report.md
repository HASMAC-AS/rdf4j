# Materializer optimization comparison

## Decision

**Neither candidate qualified for promotion. Retain the previously validated production implementation, keep the new mutation characterization test, and preserve both prototypes as experiments.** Candidate 1 did not show a consistent latency gain over original DENSE. The cumulative Candidate 1+2 comparison was effectively centered near parity with original DENSE, with paired results on both sides of it. Every enabled arm remained slower than the preserved original OFF reference on this default workload.

The decision follows the rule fixed before execution: all four matched-round latency ratios must favor a candidate, with result/route/call/ledger evidence preserved. A range crossing 1 is inconclusive, not evidence of equivalence or a win. No arm was rerun, no measured tail was dropped, and no extra rounds were added.

## What was changed and what was isolated

Candidate 1 combines three materializer changes: it reads coordinates directly from the memo instead of copying five longs into scratch on a memo hit; it moves page-key validation to coordinate replacement so unchanged memo hits reuse the validated page; and it uses weak-reference identity as the publication witness while retaining value validity checks. Its result therefore cannot isolate the cost of the five-long copy alone. Three production class files changed.

Candidate 2 is incremental on Candidate 1. It makes KernelRowCursor.mayReplay a conservative eligibility check while callers continue to validate the live coordinate, removing one repeated live-cell probe. Exactly KernelRowCursor.class changes relative to Candidate 1; the Candidate 1 materializer source and bytes are identical. A Candidate 2-only runtime was not tested.

All arms used the unchanged original harness, fixture, diagnostics, dependencies and post-main root launcher. Candidates replaced only production classpath entry 1 of 127. Thus original DENSE/original OFF is the source-identical transport-mode ablation; candidate/OFF comparisons use the preserved original-runtime ordinary-reuse reference. Cumulative Candidate 1+2 was correctness-tested with OFF; neither candidate OFF configuration was timed.

## Fixed protocol and validity

The protocol used four matched rounds in a balanced Williams square, with four separate fresh JVMs per round. Each JVM ran eight warmups and all five measured queries. Java 25.0.5+8, 1 GiB/G1, normal AUTO and the original M=100, R=10, width=12, two-status, full-consumption fixture were fixed. No profiler ran during these timings.

- **16/16 JVMs; 208/208 raw query records valid:** 128 warmups and 80 measurements, independently recounted from the JSONL files.
- All queries returned 1,152,000 rows with checksum `3123987218479850048`. The measured queries consumed 92,160,000 rows in total.
- Every process reported zero hosted generic outer roots and exactly 13 semantic native compilations, one per query. The inert hard-decline counter is not used as proof.
- All 80 measured queries used the same normal ROW→irKernel selection and one compiled bind, with no interpreted bind. Warmups contained 32 interpreted and 96 compiled binds.
- All query ledgers started and ended at zero. Every enabled query admitted its coordinate owner without initial/growth refusal; OFF admitted none.
- Source/runtime/protocol/driver/helper identities matched before and after the complete protocol.

The table reports the median of four candidate/comparator ratios, followed by their complete range. Each ratio divides the candidate JVM’s median of five measured queries by the comparator JVM’s median in the same round. These are ratios across separate JVMs, not within one shared fork.

| Comparison | Total-time ratio: median [min, max] | Allocation ratio: median [min, max] | Primary interpretation |
|---|---:|---:|---|
| Original DENSE / Original OFF | 1.319224 [1.212237, 1.442790] | 0.837386 [0.809217, 0.837461] | Slower in all four rounds |
| Candidate 1 DENSE / Original DENSE | 1.024433 [0.970179, 1.179192] | 1.000037 [0.999994, 1.034875] | Inconclusive; crosses parity |
| Candidate 1+2 DENSE / Candidate 1 DENSE | 0.942030 [0.910700, 1.008146] | 1.000009 [0.999917, 1.000107] | Inconclusive; crosses parity |
| Candidate 1 DENSE / Original OFF | 1.381660 [1.336855, 1.429459] | 0.837448 [0.837372, 0.837463] | Slower in all four rounds |
| Candidate 1+2 DENSE / Original OFF | 1.330905 [1.241791, 1.350472] | 0.837425 [0.837387, 0.837528] | Slower in all four rounds |
| Candidate 1+2 DENSE / Original DENSE | 0.997763 [0.908802, 1.084001] | 1.000046 [0.999911, 1.034986] | Inconclusive; crosses parity |

Candidate 1/original DENSE had a median ratio of 1.024433. Candidate 1+2/Candidate 1 had a favorable median of 0.942030, but one primary pair was 1.008146. The cumulative Candidate 1+2/original DENSE median was 0.997763 with a 0.908802–1.084001 range. None satisfies the prospective rule.

## Every JVM median

Each cell below is a median of all five measured queries. Full native-unit values and all phase metrics are in the linked CSVs; decimal GB means 1,000,000,000 bytes.

| Arm | Round 1 time (s) | Round 2 time (s) | Round 3 time (s) | Round 4 time (s) |
|---|---:|---:|---:|---:|
| Original OFF | 4.187196 | 4.554813 | 4.678183 | 4.826679 |
| Original DENSE | 6.041243 | 5.521512 | 6.392294 | 6.139746 |
| Candidate 1 DENSE | 5.861085 | 6.510920 | 6.378966 | 6.452569 |
| Candidate 1+2 DENSE | 5.654691 | 5.985326 | 5.809327 | 6.505129 |

| Arm | Round 1 allocation (GB) | Round 2 allocation (GB) | Round 3 allocation (GB) | Round 4 allocation (GB) |
|---|---:|---:|---:|---:|
| Original OFF | 1.630706 | 1.630638 | 1.630674 | 1.630618 |
| Original DENSE | 1.365483 | 1.319540 | 1.365627 | 1.365505 |
| Candidate 1 DENSE | 1.365507 | 1.365559 | 1.365618 | 1.365583 |
| Candidate 1+2 DENSE | 1.365556 | 1.365705 | 1.365505 | 1.365558 |

Original DENSE round 2 retained the lower 1.319540464 GB/query median. Its measured routes, resolver calls, ledger peak and checksums matched the other enabled runs. The cause of this allocation variation was not isolated; it remains in every primary estimate. Candidate allocation was otherwise essentially unchanged relative to ordinary DENSE.

## Preparation, first row, consumption and ledger

The following figures are descriptive medians of the four JVM medians, not the estimator used for the paired comparisons. Evaluation ends when query.evaluate returns; first-row latency is measured from there. First-row latency overlaps the consumption interval and must not be added to consume or total time. All per-JVM and per-query values, including close/connection time and caller-thread allocation, are preserved in the machine-readable outputs.

| Arm | Prepare (ms) | Evaluate (ms) | First row after evaluate (ms) | Consume (s) | Ledger peak (bytes) | Resolver calls/query |
|---|---:|---:|---:|---:|---:|---:|
| Original OFF | 2.810 | 22.092 | 9.575 | 4.590533 | 2,432 | 2,770,101 |
| Original DENSE | 2.881 | 19.755 | 9.104 | 6.063650 | 2,556,992 | 1,147 |
| Candidate 1 DENSE | 2.436 | 20.816 | 9.195 | 6.390762 | 2,556,992 | 1,147 |
| Candidate 1+2 DENSE | 2.441 | 17.948 | 7.829 | 5.873239 | 2,556,992 | 1,147 |

The time difference remains predominantly in consumption. The enabled variants preserve the resolver-call reduction and lower allocation, while retaining the larger coordinate/factor-cache ledger peak. Cleanup invariants remained identical. Preparation and first-row differences are secondary observations and do not turn the inconclusive total-latency result into a win.

## Drift and limits

Within a JVM, the last measured query divided by the first ranged from 0.873255 to 1.180047 across this experiment. The original OFF JVM medians also ranged from 4.187196 to 4.826679 seconds across rounds. This variation is why the report preserves every per-query progression, balances run positions and uses matched rounds; it does not justify removing samples. Fixed warmup plus compiled IR observations do not prove complete stability of every JVM optimization or host condition.

The independent audit includes an exploratory last-three sensitivity. It gives a favorable Candidate 1+2/Candidate 1 result in all four rounds, but it excludes two prespecified primary observations from each JVM and cannot replace the all-five result. The primary range crosses parity, and the cumulative comparison with original DENSE remains inconclusive. No timing extension followed this observation.

This is a bounded default-workload experiment with four paired JVMs, not a broad statistical-confidence claim or a repetition/projection/retention sweep of the candidates. Earlier protocols remain separate and unchanged. The JFR profiles also identified coordinate-namespace reference-count/hash work; no namespace optimization was implemented or measured here, so it remains a separate, untested hypothesis.

## Final selected implementation

The selected original implementation passed the final matching gate: 94 tests, zero failures/errors/skips. All 2,780 production files match the preserved original runtime exactly. The only active LMDB source change relative to the previous delivery is the new NativeResultMaterializerTest characterization. See [production selection](production-selection.json) and [final selection verification](validation/selected-original/final-selection-verification.json). Both experimental runtime snapshots and their patches remain preserved.

## Artifacts and provenance

- [Frozen protocol](optimization-comparison-protocol.json) and [protocol SHA-256](optimization-comparison-protocol.sha256). Its SHA-256 is `5fc5bca41f6b27b0d04ea6ce33d995023154eb119d173b96536600016a5873bb`.
- [Complete independent summary](comparison-summary.json): all 16 JVM medians, all paired effects, all 80 measured routes and all 208 query-validity records.
- [Fork medians CSV](comparison-fork-medians.csv), [all 80 measured-query metrics CSV](comparison-query-metrics.csv), [all matched-round ratios CSV](comparison-paired-ratios.csv), and [metric summary CSV](comparison-summary.csv).
- [Driver raw records](optimization-comparison/runs.json), [driver summary](optimization-comparison/summary.json), [post-run preservation](optimization-comparison/post-run-preservation.json), and [independent audit](optimization-comparison/independent-analysis.md). Each process directory contains its original samples.jsonl, root audit, console and exact command.
- [Candidate 1 corrected descriptor](runtimes/candidate1-metadata-corrected/runtime-descriptor.json), [corrected provenance](runtimes/candidate1-metadata-corrected/source-provenance.json), and [production patch](runtimes/candidate1/production-delta.patch).
- [Candidate 1+2 descriptor](runtimes/candidate1-and-2/runtime-descriptor.json), [provenance](runtimes/candidate1-and-2/source-provenance.json), [cumulative production patch](runtimes/candidate1-and-2/production-delta.patch), and [incremental class/source proof](runtimes/candidate1-and-2/candidate1-to-candidate2-delta.json).
- [JFR diagnosis](profile-report.md), [scrubbed OFF diagnostic copy](delivery-profiles/frozen-off/profile.jfr), and [scrubbed DENSE diagnostic copy](delivery-profiles/frozen-dense/profile.jfr). Instrumented profile timings are not pooled with this comparison.

Before timing, a source-baseline metadata reference was corrected to the exact measured benchmark-source-hashes.json digest 34767935…f7a8ac. Candidate 1’s source map and production bytes were unchanged. The [additive correction record](runtimes/candidate1-metadata-corrected/correction-record.json) links old/new metadata and [preserved executed helpers](helper-history/README.md); the active protocol explicitly uses the corrected descriptor. No previous artifacts were rewritten.

The downloadable JFRs are scrubbed diagnostic copies; original raw recordings remain private and unmodified, with original hash provenance preserved. Only initial environment/security/system-property events and process-list events were removed. The [disclosure receipt](delivery-profiles/disclosure-verification.json) confirms unchanged retained event counts/payloads, and the [regeneration proof](delivery-profiles/analysis-regeneration-verification.json) verifies the extracted performance-event data and analysis. These diagnostic-copy changes did not alter any timed input, result or runtime.
