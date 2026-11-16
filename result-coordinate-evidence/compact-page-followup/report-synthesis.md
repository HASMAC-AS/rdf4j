# Compact factor pages: memory benefit confirmed; candidate not selected

The compact-page candidate passed correctness and accounting checks, substantially reduced sparse-page reservations, and reduced actual allocation on the shared-tail workload. It did **not** pass the prospective latency screen. The candidate is therefore preserved as an experiment, and this phase's production source is restored to its original preimage. Earlier ownership repairs and original frame behavior remain intact.

The completed study also answers the ordinary-reuse versus coordinate-transport question for these two full-consumption fixtures. With ownership code held identical, current DENSE transport was slower than ordinary reuse with coordinates OFF in every paired round on both fixtures. Compact pages did not close that runtime gap. The next useful normal-budget experiment is a targeted factor-publication ablation, motivated by measured targets that incur page costs without successful factor hits.

All **36 fresh timing JVMs and 468 queries** qualified, including all 180 measured queries. There were no replacement forks, excluded samples, extra rounds or post-data threshold changes. Full runtime and protocol preservation checks passed. A separate independent program recalculated every primary metric and acceptance gate directly from the raw JSONL and agreed with the driver. Evidence is in `comparison-v1/`, `independent-results/independent-results.json`, `raw-analysis-v1/` and `independent-review.md`.

## Why compact pages were the next test

The preceding diagnostic showed that 16,112 of 16,364 admitted pages held one stored reference, although every page eagerly allocated five 64-entry arrays and reserved 16 KiB. The query nearly exhausted its 256 MiB allowance. That was a concrete mismatch between observed occupancy and storage.

The candidate keeps a stable page object with one inline weak-reference/witness tuple. It charges 512 bytes initially and requests another 15,872 bytes before promotion to the original dense arrays. A refused promotion preserves the first member and retries on a later eligible insertion. Same-position replacement and reuse of an explicitly cleared singleton do not promote. This tests one representation change while keeping coordinate transport, ownership and result semantics constant.

This design was also compared with primary Roaring representation/accounting implementations and existing reserve/construct/publish patterns in this repository. The choice of one inline entry followed by dense promotion comes from the measured one-or-64 occupancy distribution; no external threshold, code or dependency was copied. Sources and limits of the comparison are recorded in `design-research.md` and `layout-audit.md`.

## The primary comparison

The following ratios compare compact DENSE with current DENSE. Each point is a ratio of two fresh-JVM medians of all five measured queries from the same round. The reported center is the median of six paired ratios; ranges contain every observed pair and are not confidence intervals.

| Metric | Fanout: paired median [full range] | Shared-tail: paired median [full range] |
|---|---:|---:|
| Total latency | 0.997867 [0.872256, 1.115603] | 1.012579 [0.930966, 1.054780] |
| All-thread allocated bytes | 1.0000096 [0.979485, 1.000071] | 0.9684448 [0.968418, 0.968474] |
| Caller allocated bytes | 1.0000042 [0.979499, 1.000072] | 0.9684444 [0.968417, 0.968473] |
| Query-ledger peak | 0.985985 in every pair | 0.0480631 in every pair |

Shared-tail therefore has **95.1937% less peak ledger usage** and **3.1555% less all-thread allocation** by the paired estimates. Its median latency ratio is **1.2579% higher**. Fanout's median paired latency change is approximately **−0.21%**, with a wide range that includes a **+11.56%** pair. Allocation on fanout is essentially unchanged in the paired median.

The six latency pairs make the lack of a consistent win explicit:

| Matched round | Fanout latency change | Shared-tail latency change |
|---|---:|---:|
| 1 | +5.81% | +0.08% |
| 2 | −6.02% | −6.90% |
| 3 | +11.56% | +5.48% |
| 4 | +2.84% | −0.06% |
| 5 | −3.27% | +3.43% |
| 6 | −12.77% | +2.44% |

The original and candidate DENSE fanout forks in round 2 both have lower allocation than their other forks. Their medians are approximately 1.3466 and 1.3190 GB, respectively, and the pattern appears in all five measurements in those forks. The records remain included. This study does not establish why that allocation level changed; it must not be explained away as a known JIT or GC effect without further evidence.

## The fixed decision

All six gates were declared before timing. The decision uses unrounded ratios.

| Prospective gate | Observed result | Decision |
|---|---|---|
| Shared-tail ledger ratio at most 0.25 in every pair | 0.0480631 in all six | Pass |
| Shared-tail allocation ratio below 1 in every pair | 0.968418–0.968474 | Pass |
| Fanout median latency ratio at most 1 | 0.997866926 | Pass |
| Every fanout latency ratio at most 1.10 | Round 3 is 1.115602800 | **Fail** |
| Shared-tail median latency ratio at most 1 | 1.012579249 | **Fail** |
| Every shared-tail latency ratio at most 1.10 | Maximum 1.054779955 | Pass |

This is a failed engineering acceptance screen, not proof of a population slowdown of precisely 1.26%. The observed latency variation does not justify a general claim that the compact layout is uniformly slower or faster. It does justify following the precommitted rule and declining promotion. Neither fixture meets the separate requirement that all six latency ratios favor the candidate.

`selection-decision.json` records the decision, the exact failed gates, protocol and independent-result hashes, preserved candidate location, and selected core path set. No tolerance was relaxed and no run was added after the result.

## Ordinary reuse remains the faster control here

OFF keeps ordinary weak-value reuse and the same repaired ownership code. It disables coordinate transport. The DENSE/OFF comparison therefore asks whether the additional coordinate reuse path pays for itself in these workloads.

| Comparison | Fanout latency ratio: median [range] | Shared-tail latency ratio: median [range] |
|---|---:|---:|
| Current DENSE / OFF | 1.307341 [1.271317, 1.460321] | 1.128405 [1.056954, 1.228892] |
| Compact DENSE / OFF | 1.304235 [1.268615, 1.446187] | 1.136282 [1.104335, 1.192871] |

Both DENSE arms are slower than OFF in every paired round on both fixtures. Their allocation is lower: current DENSE/OFF allocation ratios have paired medians of 0.837254 on fanout and 0.879904 on shared-tail; compact DENSE/OFF has 0.837278 and 0.852137. Thus the allocation/runtime tradeoff persists even after the sparse page representation is improved.

The resolver counts are constant across every measured query in each arm:

| Workload | Ordinary reuse, coordinates OFF | Original pages, DENSE | Compact pages, DENSE |
|---|---:|---:|---:|
| Fanout resolver calls | 2,770,101 | 1,147 | 1,147 |
| Shared-tail resolver calls | 2,113,601 | 65,345 | 32,833 |

Avoiding resolver calls is clearly useful for allocation, but the complete additional reuse path takes more elapsed time here. The present comparison does not partition that cost among coordinate movement, publication, witness checks, synchronization, generated-code behavior or other execution effects. Compacting pages alone is insufficient to reverse it.

These results do not alter AUTO strategy or the global transport default. They support ordinary reuse with transport OFF as the latency reference for further experiments, while retaining the coordinate candidate as a measured memory tradeoff. They are not extrapolated to other cardinalities, consumption modes or execution strategies.

## Absolute measurements and preparation cost

For scale, the following values are descriptive medians of the six JVM medians. Allocation uses decimal GB; ledger uses bytes. These columns are not the primary paired estimator. In particular, dividing the two descriptive fanout latency medians does not reproduce the median of the six within-round ratios.

| Workload and arm | Total seconds | All-thread allocation, GB/query | Peak ledger, bytes |
|---|---:|---:|---:|
| Fanout OFF | 2.963757 | 1.630360 | 2,432 |
| Fanout current DENSE | 3.889048 | 1.365037 | 2,264,968 |
| Fanout compact DENSE | 3.934880 | 1.365071 | 2,233,224 |
| Shared-tail OFF | 2.100872 | 1.320700 | 1,472 |
| Shared-tail current DENSE | 2.374996 | 1.162088 | 268,426,276 |
| Shared-tail compact DENSE | 2.409827 | 1.125419 | 12,901,412 |

The candidate saves approximately 36.7 MB of allocated bytes per shared-tail query in these descriptive medians. That is much smaller than the reduction of 255,524,864 ledger bytes. The difference is expected because ledger allowances are conservative reservations rather than heap measurements.

| Warm phase, descriptive median milliseconds | Fanout OFF | Fanout current DENSE | Fanout compact DENSE | Shared-tail OFF | Shared-tail current DENSE | Shared-tail compact DENSE |
|---|---:|---:|---:|---:|---:|---:|
| Prepare query | 2.198 | 2.066 | 2.229 | 1.316 | 1.152 | 1.399 |
| Evaluate | 19.299 | 18.127 | 22.353 | 6.947 | 6.628 | 6.887 |
| First row after evaluate | 6.221 | 5.678 | 7.793 | 4.297 | 4.423 | 4.952 |
| Consume and check | 2945.589 | 3864.604 | 3912.803 | 2093.170 | 2363.893 | 2401.635 |
| Close | 0.130 | 0.119 | 0.140 | 0.140 | 0.186 | 0.159 |

Most elapsed time is in consumption, which includes result checking. First-row time overlaps consumption and must not be added again. The much shorter preparation/close phases are still reported; their sometimes large percentage ratios should be read with their small absolute durations.

Cold first-query descriptive medians are 5.412/8.697/8.385 seconds for fanout OFF/current DENSE/compact DENSE, and 3.772/4.460/4.622 seconds for shared-tail. Cold query preparation is approximately 65–77 ms across these arm summaries; evaluation is approximately 328–438 ms. The cold first query includes query initialization and initially interpreted execution after fixture/store setup; its timer excludes JVM launch and fixture construction. It is not pooled into the warmed compiled measurements. Every warmup, phase value and range is preserved in `raw-analysis-v1/numeric-tables.md` and `BENCHMARK-RESULTS.md`.

## What accounting and pressure experiments added

The separate main diagnostic confirmed the predicted compact geometry: 16,384 inline pages and 256 dense pages, occupying exactly 32,768 stored-reference slots in 32,768 allocated cells. The original has 16,364 dense pages with 32,240 occupied slots in 1,047,296 allocated cells. Occupied weak-reference slots are not a live-value census.

The original's page charge is 268,107,776 bytes. The compact page charge is 12,582,912 bytes: 16,640 shell claims at 512 bytes plus 256 payload claims at 15,872 bytes. The candidate admits 276 more pages. Its ordinary hits remain 4,177,855, while factor hits increase from 2,048,256 to 2,080,768; resolver calls fall by exactly the same 32,512. Every base, payload, directory and memo grant is observed at explicit release, and query endpoints are zero.

The separate 20,000-byte constrained probe leaves only 128 bytes free and alternates two offsets 1,024 times. It observes 1,024 refused promotions, preserves the original member on every replay, then promotes successfully after the competing claim is released. Counts are exact: 1,025 promotion attempts, one success, 1,026 resolver calls and 1,026 factor hits across 2,052 rows. No reclamation callback runs and final ledger usage is zero. It establishes safe fallback and recovery, and exposes one failed attempt per eligible insertion under persistent pressure; it does not measure the CPU cost of that behavior.

The diagnostic runtimes contain instrumentation and are excluded from performance estimates. Their full evidence and the distinction between explicit-release attribution and leak detection are in the diagnostic appendix below.

## The next experiment worth conducting

The next normal-budget experiment should omit factor publication for the shared-tail targets that supplied no factor hits, while retaining ordinary reuse, ownership, frame transport and AUTO selection. This is a targeted counterfactual to quantify the cost of cache work that provided no observed benefit.

| Target | Dense pages | Inline pages | Declared page bytes | Successful factor hits |
|---|---:|---:|---:|---:|
| l0 | 128 | 0 | 2,097,152 | 0 |
| l1 | 0 | 8,192 | 4,194,304 | 0 |
| r0 | 128 | 0 | 2,097,152 | 1,040,384 |
| r1 | 0 | 8,192 | 4,194,304 | 1,040,384 |

The two left targets own exactly half the compact page charge and contribute no successful factor hit in the observed full-consumption fixture. No promotion is refused under its normal budget, so retry overhead cannot explain this fixture's measured normal-budget gap. A publication ablation is therefore a more direct next test of that gap than another page-capacity tweak or a retry heuristic.

Zero hits in one execution does not prove that all reuse is consecutive in general. Expired weak references, witness changes, rebinding, aliases, unbound gaps, restored frames and other query shapes can change reuse. Optional-cache exclusion can remain result-correct through normal resolution; a stronger structural proof is needed to claim that no useful nonconsecutive reuse is lost. A heuristic exclusion would require broader performance and strategy validation. Per-canonical-target counters should distinguish ordinary hits, factor lookups, weak/witness misses, resolver calls and revisits after ordinary-cache displacement. Fanout needs its own target-level evidence before extrapolation.

Persistent-pressure latency, early close, retained rows, cardinality/width/repetition sweeps and concurrent manager contention remain separate questions. The present turn ends with the completed fixed study and preserved evidence; it does not silently extend the protocol. The full rationale and proof boundary are in `next-experiment-note.md`.
