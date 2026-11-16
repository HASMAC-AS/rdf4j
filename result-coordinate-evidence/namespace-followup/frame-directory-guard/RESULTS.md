# Frame directory guard: completed matched experiment

**The guard did not establish a consistent latency improvement, and coordinate transport remained slower than ordinary reuse in every matched round.** The guard/original-DENSE latency ratio was **0.949192**, with the full six-pair range **0.901233–1.059632**: four pairs favored the guard and two did not. Guard DENSE remained **25.55% slower** than OFF by the prespecified median paired ratio, with every pair slower. The candidate was therefore not promoted; the root restored the exact original production source. Its complete source, patch, passing validation and frozen runtime remain preserved for review.

All **18 fresh JVMs, 234 queries and 90 measured samples** completed successfully. There were no replacement forks, exclusions, added rounds or changed warmup counts. Independent recalculation from raw JSONL reproduced every reported median and paired ratio exactly.

## Why this test followed the namespace diagnostic

The first diagnostic found no managed namespace references or namespace-directory lookups in the default native/direct-adjacency fixture. Memoizing the last directory lookup therefore had no work to remove. That proposed timing study was stopped before execution.

The loops surrounding those absent lookups still visited substantial data. A separate v2 diagnostic observed `namespaceIds == null` on entry to every frame retain/release range. Each compiled query visited **17,237,000 coordinate cells in each direction**, or **34,474,000 visits across retain and release**, through 1,567,000 ranges per direction. The first interpreted query visited 109,641,455 cells per direction. Both diagnostics had zero managed tokens and zero source-batch ranges, and the v2 run reproduced all 113 original v1 counters per query exactly.

The tested candidate added three `namespaceIds != null` guards in frame save/release. It retained frame copying, restoration and active-frame state. It left public source-batch retention/release unchanged, preserving rejection of unknown managed tokens. The null-directory proof is deliberately narrow: an enabled owner cannot have managed register/frame references before directory allocation, and once a directory has been allocated the original frame scans remain active. The candidate added no per-row allocation or new tracking metadata.

The root’s matching pre/post gate covered eight classes and 103 tests, with zero failures, errors or skips. Independent review bound the exact production patch before timing. These facts establish a tested, isolated candidate; the work-count reduction alone does not establish a speedup.

## Fixed protocol and observed execution

The three arms were original OFF, original DENSE and candidate DENSE. OFF disables coordinate transport while preserving the same ordinary weak-reference reuse and ownership implementation as original DENSE. The candidate replaces only the LMDB production class entry, with changes confined to `KernelResultCoordinates`; the other 126 base entries are identical. Each arm uses the exact same uninstrumented common harness prefix, for 128 total classpath entries. The timing binary contains none of the namespace diagnostic instrumentation.

All six arm permutations ran serially. Each arm appeared twice in each position. Every fresh JVM received eight warmups and all five measured queries. The fixture remained M=100, R=10, projection width=12, two statuses, no unique-payload expansion and full consumption: 1,152,000 output rows per query.

The runtime was Java 25.0.5+8, `-Xms1024m -Xmx1024m -XX:+UseG1GC`, with existing hot counters enabled identically for all arms. Native evaluation and direct adjacency were enabled; adjacency mode was PREFER, global query-memory authority 1 GiB and per-query limit 256 MiB. Strategy and IR tier selection remained normal AUTO. No profiling or competing build/test JVM ran during the comparison.

Every query returned checksum `3123987218479850048`, had zero query-ledger use before and after closure, and recorded one semantic native compilation. DENSE admitted exactly one coordinate owner without initial or growth refusal. All 90 measured queries selected the same normal AUTO compiled `irKernel` route and recorded one compiled bind and zero interpreted/aggregate binds. Each fresh JVM’s first warmup selected interpreted IR normally. Every post-main root audit reported 13 native compilations, zero hosted-generic roots and normal return.

Every query observed **55 successful direct-adjacency lookup hits**, one planner-stats hit, zero exact misses and no fallbacks. Adjacency remained ACTIVE with applied/current revision one before and after every query. These counts prove successful query-time direct-adjacency use; planning/probe work can contribute, so they do not prove that every output row used adjacency.

The measured route/tier histogram was required to match the first qualified fork. A mismatch, failed result or changed identity would have preserved the raw fork and stopped the study. None occurred. Final checks confirmed unchanged protocol, driver, helpers, source evidence and both complete runtime manifests.

## Primary latency result: all six matched rounds

Each cell below is the median of all five measured queries in that fresh JVM. Ratios divide JVM medians **within the same round**. Lower ratios favor the numerator.

| Round | OFF seconds | Original DENSE seconds | Guard DENSE seconds | Guard / original DENSE | Guard / OFF | Original DENSE / OFF |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| 1 | 3.604733 | 4.966843 | 4.507606 | 0.907540 | 1.250469 | 1.377867 |
| 2 | 3.381145 | 4.338916 | 4.345982 | 1.001629 | 1.285358 | 1.283268 |
| 3 | 4.065345 | 4.255057 | 4.508796 | 1.059632 | 1.109081 | 1.046666 |
| 4 | 3.114828 | 4.356574 | 3.926290 | 0.901233 | 1.260516 | 1.398656 |
| 5 | 3.328272 | 3.922285 | 3.692140 | 0.941324 | 1.109326 | 1.178475 |
| 6 | 3.239984 | 4.743349 | 4.539673 | 0.957061 | 1.401140 | 1.464004 |

| Paired comparison | Median latency ratio | Full six-pair range | Pairs favoring numerator | Conclusion |
| --- | ---: | ---: | ---: | --- |
| Guard DENSE / Original DENSE | 0.949192 | 0.901233–1.059632 | 4/6 | Inconclusive: paired range crosses one |
| Guard DENSE / Original OFF | 1.255492 | 1.109081–1.401140 | 0/6 | Consistently slower in all six pairs |
| Original DENSE / Original OFF | 1.330568 | 1.046666–1.464004 | 0/6 | Consistently slower in all six pairs |

The guard’s median paired result is about 5.08% below original DENSE, but the range crosses one and two pairs were slower, so the prespecified consistent-gain criterion failed. Original DENSE was 33.06% slower than OFF by the paired median; the guard was 25.55% slower. Every original-DENSE/OFF and guard/OFF latency ratio exceeded one. Skipping the verified unnecessary scans did not close the overall runtime gap.

### Paired and unpaired summaries answer different questions

The separate medians across the six JVM medians are **3.354708 s OFF**, **4.347745 s original DENSE** and **4.426794 s guard DENSE**. The guard’s unpaired descriptive median is higher than original DENSE even though the median of within-round ratios is below one. This is not an arithmetic inconsistency: a ratio of separate medians is not the median of paired ratios, and the middle-ranked JVMs differ between arms. The full round table exposes the variation. The prospective analysis uses paired ratios throughout; the crossing range remains the relevant qualification. No statistic was selected afterward to obtain a favorable conclusion. These observed ranges are not confidence intervals.

## Allocation, preparation, phase timing and ledger accounting

Allocation values below include all JVM Java threads, including asynchronous preparation. Absolute values are descriptive medians across six fork medians. GB means 1,000,000,000 bytes. The ledger peak is an admitted-reservation measure and is reported separately.

| Arm | Allocation GB/query | Warm prepare ms | Warm evaluate ms | First-row-after-evaluate ms | Consume seconds | Close ms | Query-ledger peak bytes | Resolver calls/query |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| Original OFF | 1.630381 | 2.434 | 19.666 | 5.746 | 3.328073 | 0.122 | 2,432 | 2,770,101 |
| Original DENSE | 1.365103 | 2.371 | 23.435 | 7.325 | 4.323574 | 0.133 | 2,264,968 | 1,147 |
| Guard DENSE | 1.365065 | 2.557 | 21.263 | 7.235 | 4.383438 | 0.155 | 2,264,968 | 1,147 |

The primary paired ratios for the secondary metrics retain the same six-round structure:

| Metric | Guard / original DENSE: median [range] | Guard / OFF: median [range] | Original DENSE / OFF: median [range] |
| --- | ---: | ---: | ---: |
| All-thread allocation | 0.999972 [0.966262, 1.000019] | 0.837274 [0.808980, 0.837315] | 0.837286 [0.837226, 0.837313] |
| Caller-thread allocation | 0.999966 [0.966251, 1.000029] | 0.837262 [0.808972, 0.837301] | 0.837277 [0.837228, 0.837307] |
| Preparation | 1.123038 [0.452387, 1.465658] | 1.076617 [0.558325, 1.536444] | 1.035612 [0.603174, 1.773990] |
| Evaluation | 0.910302 [0.672511, 1.213289] | 1.039272 [0.696825, 1.380656] | 1.171276 [0.725717, 1.759851] |
| First row after evaluation | 0.941373 [0.808468, 1.377577] | 1.259384 [0.610962, 1.532285] | 1.180271 [0.721663, 1.432109] |
| Consumption | 0.951120 [0.896399, 1.057711] | 1.254096 [1.105703, 1.402178] | 1.332641 [1.045373, 1.460791] |
| Close | 1.026391 [0.604524, 1.598395] | 1.354338 [0.746178, 1.440359] | 1.008206 [0.802641, 2.183426] |
| Ledger peak | 1.000000 [1.000000, 1.000000] | 931.319079 [931.319079, 931.319079] | 931.319079 [931.319079, 931.319079] |

Both DENSE arms retain approximately **16.27% lower allocation** than ordinary reuse, and both reduce resolver calls from **2,770,101 to 1,147 per query**. Those benefits coexist with slower total execution in every matched OFF comparison. The guard/original-DENSE median allocation ratio is 0.999972, so this experiment does not show a repeatable new allocation benefit from the guard itself.

The guard’s round-four fork had a lower allocation regime, preserved without exclusion: its five measured all-thread allocations were **1,318,956,200; 1,318,956,632; 1,318,928,376; 1,318,942,664; and 1,318,996,088 bytes**. The other five guard fork medians were about 1.365 GB. Results, route counters, resolver calls, inputs and recorded identities still matched. The difference is roughly 40 bytes per output row, but its cause is not established. It is neither silently discarded nor attributed to the guard or to JVM optimization without evidence.

Preparation and first-row phase ratios vary substantially and do not establish consistent gains. Consumption dominates total latency. Component medians need not sum to the total median, because each median can come from a different query.

Every measured and warmup OFF query peaked at **2,432 ledger bytes**; every DENSE query peaked at **2,264,968 bytes**, identical with and without the guard. The relative peak ratio is large because the OFF baseline reservation is small. These are query-authority reservations, not whole-process heap, retained application values, idle prepared recipes or the separate direct-adjacency storage budget. All ledger endpoints returned to zero.

### First-query preparation is preserved separately

All first warmups used normally selected interpreted IR. The following are descriptive medians across the six first queries, and were not included in primary warmed estimates:

| Arm | First-query prepare ms | First-query evaluate ms | First-query total seconds |
| --- | ---: | ---: | ---: |
| Original OFF | 73.766 | 418.422 | 5.854131 |
| Original DENSE | 81.642 | 454.721 | 9.505945 |
| Guard DENSE | 75.793 | 383.677 | 8.864920 |

First-query preparation was variable: OFF ranged 65.164–107.720 ms, original DENSE 71.275–206.602 ms and guard DENSE 62.049–339.461 ms. All eight warmups remain in the raw data. No favorable warmup tail or alternative tier was substituted into the five-query primary measurement window.

## Decision and limits

The directory-memo hypothesis was ruled out for this fixture by zero lookup opportunity. The frame-guard hypothesis had directly observed work to remove and a correct, narrowly reviewed implementation, but its paired latency result was inconclusive and remained slower than ordinary reuse in every round. **No production optimization is accepted from this experiment.** The root restored the original production file; the candidate is retained only as complete experimental evidence.

This study covers the default full-consumption fixture with native evaluation and direct adjacency enabled. It does not establish performance across retained rows, early close, other projection widths, managed-namespace workloads or constrained memory. Both original and candidate DENSE retain the same ownership behavior outside the narrow never-allocated-directory frame path. Existing broader ownership/strategy verification is documented separately.

These new timings are not pooled with earlier runs that left direct adjacency disabled, nor with the unavailable historical baseline/Stage-2 recordings. The historical 2.79→1.42 GB/query and 2.21–2.25→2.68 s figures remain a separate, older claim; the current protocol has its own exact source/runtime and configuration identities.

## Reproduction and complete evidence

- `protocol.json`: immutable prospective design, SHA-256 `bbad71c3c879b49022ff6cb22d229ee94353961d1f512defdec722fe2a368f54`.
- `run-comparison.py` and `README.md`: complete driver and exact freeze/validation/execution commands. The driver was not changed during the study.
- `comparison-v1/`: all 18 raw JSONL files, exact commands, process exits, consoles, stores, route validations, root audits, consumed runtime identities, collected runs, calculated summary and final preservation result.
- `audit-results.py` and `audit-v1/raw-recalculation.json`: independent raw-data reconciliation, exact six-pair recalculation, per-query and per-fork evidence, including all allocation observations.
- `../diagnostic-original-v1/`, `../diagnostic-original-v2/`, `../compiled-diagnostic-v2/` and `../diagnostic-crosscheck.json`: diagnostic source/compiler/runtime identity, actual null-directory counts and repeated-counter checks.
- `../runtimes/frame-directory-guard/modified-sources/`: complete candidate source. `../runtimes/frame-directory-guard/production-delta.patch`: exact reviewed patch, SHA-256 `0ceed5b1f28931ae334671960f8f875e94d61883ab4e4ed1da29a3f3db70ac0e`.
- Candidate production tree SHA-256: `63ca7bd4fcd6f19efc32f1f1194f5f1f990aaf0488cbea538ab79ce4ad222016`. The original production tree and every other runtime entry are pinned in the frozen manifests.

The root’s final comprehensive report links this report alongside ownership validation and the separately conducted shared-selection/page-occupancy investigation. No further experiment was conducted after these results.
