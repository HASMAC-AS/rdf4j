# Independent review of completed follow-up results

This is an additive results review by `audit_ownership`. It follows the prospective source/protocol review in `independent-review.md`. No production file was changed and no build, test, or JVM was launched for this review.

The reviewer independently read the raw JSONL from all 18 directories in `frame-directory-guard/comparison-v1/`, recomputed every fresh-JVM median from all five measured queries, and recomputed all six paired ratios for all nine prespecified metrics. Every resulting ratio and median exactly matches both `comparison-v1/summary.json` and `independent-analysis/independent-results.json`. The calculation did not import either experiment analysis implementation.

## Primary decision

The frame guard does **not** meet the frozen consistency criterion. Its paired latency ratio against original DENSE has median **0.94919**, but the six ratios span **0.90123–1.05963** and favor the guard in only **four of six rounds**. Two rounds regress, including one by approximately 5.96%. The result is inconclusive under the prespecified all-six-below-one rule. It does not establish that the optimization is useless or equivalent, but it does not justify promoting it as a demonstrated performance improvement.

Both DENSE arms remain slower than ordinary reuse with coordinate transport OFF in every matched round. Their allocation advantage is clear and is separate from the unresolved benefit of the guard.

| Comparison | Median paired latency ratio | Full six-round range | Rounds favoring numerator | Median paired allocation ratio |
|---|---:|---:|---:|---:|
| Guard DENSE / original DENSE | 0.94919 | 0.90123–1.05963 | 4/6 | 0.999972 |
| Guard DENSE / original OFF | 1.25549 | 1.10908–1.40114 | 0/6 | 0.837274 |
| Original DENSE / original OFF | 1.33057 | 1.04667–1.46400 | 0/6 | 0.837286 |

Ratios below one favor the numerator. Original OFF retains ordinary value reuse; it disables coordinate transport, not all reuse. The measured median penalty relative to OFF is approximately 25.55% for the guard and 33.06% for original DENSE, while both reduce allocation by approximately 16.27%.

### Raw per-round latency medians

Each cell below is the median of all five measured queries in its own fresh JVM. Round numbers are the zero-based identifiers in the raw evidence.

| Round | Original OFF, seconds | Original DENSE, seconds | Guard DENSE, seconds | Guard / original DENSE |
|---|---:|---:|---:|---:|
| 0 | 3.604733 | 4.966843 | 4.507606 | 0.907540 |
| 1 | 3.381145 | 4.338916 | 4.345982 | 1.001629 |
| 2 | 4.065345 | 4.255057 | 4.508796 | 1.059632 |
| 3 | 3.114828 | 4.356574 | 3.926290 | 0.901233 |
| 4 | 3.328272 | 3.922285 | 3.692140 | 0.941324 |
| 5 | 3.239984 | 4.743349 | 4.539673 | 0.957061 |

The prescribed statistic is the median of matched round ratios. It is not the ratio of the two arms' overall medians. The latter can rank this noisy sample differently: the median of fork medians is 4.426794 seconds for the guard and 4.347745 seconds for original DENSE. Neither alternate aggregation nor removal of the two unfavorable rounds should replace the frozen analysis.

## Allocation, preparation, ledger, and traffic

The guard introduces no reproducible allocation saving over original DENSE. Its median paired allocation ratio is 0.999972. Five rounds are approximately equal; round 3 has a substantially lower guard allocation, 1,318,956,200 versus 1,365,008,976 bytes. That isolated difference is preserved. Its cause was not established and it should not become an allocation-gain claim.

The median of fresh-JVM allocation medians is approximately 1.630381 GB/query for OFF, 1.365103 GB/query for original DENSE, and 1.365065 GB/query for guard DENSE. These are total allocated bytes, not live heap or ledger peaks.

Preparation ratios are variable and cross one: guard/original DENSE has median 1.12304 and range 0.45239–1.46566. Original DENSE/OFF has median 1.03561 and range 0.60317–1.77399. Typical preparation medians are about 2–3 milliseconds; full-query medians are several seconds and are dominated by consumption. These data do not establish a stable preparation-cost improvement from the guard.

The following values are constant across every measured query in each arm:

| Metric | Original OFF | Original DENSE | Guard DENSE |
|---|---:|---:|---:|
| Lazy-value resolver calls | 2,770,101 | 1,147 | 1,147 |
| Process-ledger peak, bytes | 2,432 | 2,264,968 | 2,264,968 |
| Coordinate owners opened | 0 | 1 | 1 |
| Initial/growth coordinate admission refusals | 0 | 0 | 0 |
| Successful query-time adjacency lookup hits | 55 | 55 | 55 |

Every query starts and finishes with zero ledger usage. The guard changes neither resolver-call count nor the ledger peak. The resolver counter records calls into lazy-value resolution, including paths that can return already available values; it is not a count of physical LMDB reads.

## Validity and limits of the timing comparison

The raw record audit found 18 fresh JVMs, 234 queries, and 90 measured queries. Every fork contains the exact prescribed iteration sequence, eight warmups and five measurements. All queries return 1,152,000 rows and checksum `3123987218479850048`, with valid complete results and zero ledger endpoints. Measured queries use one compiled IR bind, no interpreted/aggregate bind, observed nonforced AUTO selection, and identical measured route/tier histograms. Every outer-root audit records thirteen semantic native compilations and zero hosted generic roots. The preservation record reports unchanged runtime and identity files.

The interpretation remains scoped to this full-consumption optional-fanout fixture, prescribed JVM/settings, and enabled native/direct-adjacency configuration. Positive adjacency lookup hits establish successful query-time use, not that every output row used adjacency or that each hit occurred only during execution. The reported range is the range of six paired observations, not a confidence interval. No power calculation establishes equivalence. Earlier measurements with another adjacency configuration or source checkpoint must not be pooled with this experiment.

The correct promotion decision under the frozen gate is to retain the candidate and evidence as an experiment, rather than present it as an accepted performance improvement. This single target also does not by itself establish a universal coordinate-transport default.

## Independent page-occupancy reconciliation

The reviewer separately read every raw page record and summary from `shared-selection/page-occupancy/page-records/owner-{1,2,3,4}.jsonl`, together with `run/samples.jsonl`. These belong to the different shared-selection fixture, which returns 1,048,576 rows and six bindings per row.

All four owners have the same structural findings:

- 16,364 unique admitted pages and exactly 16,364 releases.
- 16,112 pages with one stored weak-reference slot and 252 pages with all 64 slots stored.
- All page keys use namespace 1. No page retires or clears before final close, so the final table covers every admitted page.
- Total page reservation grants, bytes observed before release, and final page-table charges all equal 268,107,776 bytes.
- One page-payload reservation fails; directory and factor-memo reservations do not fail.
- The first payload refusal occurs at ledger usage 268,426,276 bytes, leaving 9,180 bytes of the 268,435,456-byte query allowance. A 16,384-byte page cannot fit.
- All four query ledgers finish at zero. Granted-minus-observed-explicit-release differences are zero for pages, directories, and the factor memo in these executions.

The measured owner has this exact key/traffic distribution:

| Target | Admitted pages | Stored slots per page | Distinct scoped parent identities | Page charge, bytes | Factor hits |
|---|---:|---:|---:|---:|---:|
| `s` | 0 | — | 0 | 0 | 0 |
| `a` | 0 | — | 0 | 0 | 0 |
| `l0` | 126 | 64 | 63 | 2,064,384 | 0 |
| `l1` | 8,048 | 1 | 8,048 | 131,858,432 | 0 |
| `r0` | 126 | 64 | 63 | 2,064,384 | 1,024,128 |
| `r1` | 8,064 | 1 | 8,064 | 132,120,576 | 1,024,128 |

The table contains 32,240 stored slots out of capacity for 1,047,296, or **3.0784% stored occupancy**. Singleton pages constitute 98.4600% of admitted pages. Left-side pages charge 133,922,816 bytes while producing zero factor hits in this traversal. This does not mean the left values receive no reuse: their consecutive reuse is serviced by the ordinary cache.

### Exact ordinary/factor/resolver traffic reconciliation

In every one of the four diagnostic queries:

`4,177,855 ordinary hits + 2,048,256 factor hits + 65,345 resolver calls = 6,291,456 binding reads`

This is exactly `1,048,576 rows × 6 bindings`. The 4,243,200 publication calls also equal ordinary hits plus resolver calls; successful factor hits do not add an extra publication in this path.

An independent count from the fixture's traversal matches the observed totals. There is one common `s` value, 64 outer `a` values, and 8,192 distinct values in each left column. Their consecutive hits total `(1,048,576 − 1) + (1,048,576 − 64) + 2 × (1,048,576 − 8,192) = 4,177,855`. The two right columns each obtain `63 × 127 × 128 = 1,024,128` factor hits. The remaining resolver traffic is `1 + 64 + 2 × 8,192 + 2 × (63 × 128 + 128 × 128) = 65,345`.

The stored-key count, traffic count, and reservation accounting therefore tell a consistent story: singleton source runs consume almost all admitted pages, ordinary reuse already covers the left-side repetition, and the global page-admission refusal leaves part of the subsequent right-side work on resolver fallback. The measured first refusal is for target `l1`, group 10, parent token 8049, page index 0. Parent-token numeric representations differ between early warmups and compiled execution; identities are scoped to their owner/group and should not be equated across executions.

## Limits of the occupancy finding

These are observations from the instrumented execution. Stored slots do not mean currently valid Values or lifetime ever-touched cells. Live-referent counts vary between the four queries, and calling `WeakReference.get()` plus diagnostic allocations and output can affect GC. No timing or allocation comparison is inferred from the page diagnostic.

Page charges are conservative ledger reservations, not measured resident heap bytes. A different page layout or admission policy might improve this geometry, but no such change has been tested here. The zero ledger endpoints and matching release counts support cleanup correctness for this run; they do not turn the diagnostic into a general proof for every ownership path.

Finally, this memory-pressure finding must not be used to explain the primary optional-fanout latency penalty. The primary DENSE arms peak at only 2,264,968 ledger bytes. The nearly 256 MiB page pressure occurs in the separate shared-selection fixture. Both observations are useful, but they describe different execution patterns.
