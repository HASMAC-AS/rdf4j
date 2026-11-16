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


## Selected source and verification

The final source uses the original production implementation. The selected-source gate passed **106 tests in eight reports, with zero failures, errors or skips**, after formatting and the module/dependency build. All 103 prior test identities remain, together with three new representation-independent cases. The strengthened existing no-reclamation test also remains. The two compact-only cases are preserved with the complete experimental test source, rather than left as failing expectations on the restored implementation.

All 2,055 module source files were compared with the phase baseline and the successful selected gate. The only selected module delta is `NativeResultMaterializerTest.java`. Every production source, including `KernelResultCoordinates.java`, is identical to the phase baseline. All 2,780 compiled production files exactly match the original frozen runtime, with tree SHA-256 `f822ad378729e2ebdd5c7e91d2420e9dbf1eb92aab382e76c35fba00989eab8c`. The restored materializer source SHA-256 is `cb0749719313b85c8cd134b6c03085f712915188f0e53333cbd46e19e2f0f96e`.

The complete compact production and test source, its precise two-file core patch and matching 108-test source map are under `experimental-candidate/`. Its uninstrumented runtime and both instrumented variants remain preserved separately. No earlier ownership repair, historical evidence, or previous delivery was reset or overwritten. Branch `GH-0000-lmdb-lazy-result-coordinates` and checkpoint HEAD `15fe7d5011d27d31d76436238363ee3f34b3ba50` are unchanged; nothing was committed or pushed.

The verifier and full receipt are `verify-selected-source.py` and `selected-source-verification.json`. The independent review records a separate reconciliation of source, test identities, results and restoration. The final delivery adds a complete selected-source archive, a full checkpoint patch, a precise phase delta, raw evidence, original/candidate runtimes and a verified path mapping.

## Method and correctness evidence

### What this experiment isolates

The question is whether a factor cache should allocate its original five 64-entry arrays for a page that currently contains one member. The preceding shared-tail diagnostic found that most admitted pages had only one stored weak-reference slot. That observation motivates an inline singleton representation, followed by one-way promotion when a second distinct occupied offset is inserted.

This is a factor-cache storage experiment. The name **DENSE** refers to the existing coordinate transport mode. The compact candidate still uses that transport; it does not introduce a new coordinate encoding. Original frame handling, weak-value reuse, detached outward results, mutation checks and the repaired ownership code are held constant.

The three arms are:

| Arm | Coordinate transport | Factor-page implementation |
|---|---|---|
| Current OFF | Disabled; ordinary reuse remains enabled | Preserved current implementation |
| Current DENSE | Existing dense coordinate transport | Original eager page arrays |
| Compact DENSE | Same dense coordinate transport | Inline singleton, then charged dense promotion |

Compact DENSE/current DENSE is the primary representation comparison. Current DENSE/current OFF is a coordinate-ablation comparison with identical ownership and production code. Compact DENSE/current OFF shows the combined tradeoff on the candidate. Earlier measurements from other source snapshots are kept as historical evidence and are not pooled with this study.

### Representation and ownership

A page keeps the same object identity throughout its life. Its key still contains the canonical target, namespace, group, parent, tuple column and long page index. This matters because existing coordinate memos retain the page object itself. The initial page stores its offset, value ID, semantic version, revision ID, revision-owner witness and weak value reference inline.

The compact shell reserves 512 bytes. A second distinct occupied position requests a separate 15,872-byte payload claim before allocating the original arrays. Successful promotion therefore retains the original 16,384-byte total dense allowance. All five arrays are allocated locally, the old complete witness tuple is copied to its original offset, and the arrays are published under the existing materializer lock. The incoming tuple is then inserted. An allocation exception closes the new claim and propagates; this rollback was source-reviewed, not fault-injected.

Promotion uses the existing non-reclaiming reservation path. A refusal leaves the singleton usable and does not poison admissions of other compact pages. The next eligible insertion retries, so a release by another owner can enable recovery. The implementation adds no memory-manager epoch, permanent refusal latch, demotion state or insertion-time weak-reference probe. It reuses an explicitly cleared inline slot without promotion and does not add strong references to result values.

The 512-byte and 16,384-byte charges are conservative accounting allowances. They are not measured object sizes. Removing mostly empty arrays can save allocated bytes, while admitting more pages can add objects and reduce resolver work. Those effects are measured separately from the ledger.

### Test-first verification

The mandatory offline root clean install passed before production edits. The new smallest failing case was then run against unchanged production. Under a 4,096-byte query/global allowance, it accessed A, B and A at positions on separate long-indexed pages. Values remained correct, but the original implementation made three resolver calls, `[42,43,42]`, instead of two. The fresh Surefire result was one test, one failure, zero errors and zero skips. This evidence was archived before the candidate source was changed.

After implementation, formatting, the module/dependency build and the focused correctness selection passed: **108 tests in eight reports, zero failures, errors or skips**. Independent XML reconciliation confirms that the selection retains every one of the prior 103 test identities and adds exactly five materializer cases. The test-source map before and after the gate matches the candidate's frozen module source map.

| Focused class or selection | Cases |
|---|---:|
| NativeResultMaterializerTest | 42 |
| LmdbNativeProjectionValueReuseTest | 12 |
| LmdbNativeIrFactorCoordinateTransportTest | 10 |
| LmdbNativeIrOptionalFanoutCoordinateTest | 2 |
| Delayed packed-row lifetime case | 1 |
| KernelResultCoordinatesTest | 19 |
| KernelResultCoordinateRepresentationsTest | 7 |
| KernelResultRowSelectionsTest | 15 |
| **Total** | **108** |

The selection exercises interpreted and compiled IR, alias/union/nested-optional bags, fanout, cancellation racing with close, retained rows, delayed fallback, weak-reference collection, mutation witnesses and coordinate representations. New cases cover small-budget replay, all 64 promoted positions at long indices, refusal followed by recovery, same-position replacement and reuse of an explicitly cleared inline cell, and retirement/ledger-first/double-close behavior for mixed page representations. The existing no-reclamation test was strengthened with a forced uncached replay and a setup based on actual ledger usage rather than the old first-page size.

This is a focused correctness gate, not a claim that every repository test was run. Independent invariant review is recorded in `independent-review.md`; raw XML, commands, build logs and source maps are preserved under `validation/`.

### Matched timing protocol

The fixed study has two full-consumption fixtures. Fanout uses M=100, R=10, width 12 and two statuses: 1,152,000 rows with checksum 3123987218479850048. Shared-tail uses 64 roots, 128 left members, 128 right members and two columns per tail: 1,048,576 rows with checksum 8327963502356915200. Its independent oracle verifies IRI types and lexical values, parent/tuple associations, legal unique row keys, count and checksum.

Each fixture receives all six permutations of the three arms. Every arm runs in a fresh JVM with eight warmups and all five measured queries. Complete three-arm blocks stay together; the first fixture alternates by round. The complete schedule is 36 JVMs, 288 warmups and 180 measured queries, totaling 468 queries. No samples may be excluded, replaced or selected from a favorable tail; no extra rounds or post-data tolerance changes are permitted.

All arms use Java 25.0.5+8, a fixed 1 GiB G1 heap, the same existing hot counters, a 1 GiB global query-memory authority and a 256 MiB per-query allowance. Native evaluation and direct adjacency PREFER are enabled; strategy and IR tier selection use normal AUTO. The original per-fixture store configuration is preserved. The fanout and shared-tail store budgets are not assumed identical, and no cross-fixture result is pooled.

Every query must have the expected complete result and zero ledger endpoints. Measured queries must use compiled native IR, with matching route histograms within each fixture. Post-main audits reconcile native compilation counts and require no hosted-generic outer root. Direct-adjacency observations require ACTIVE state, unchanged applied/current revisions and a positive lookup-hit delta across each query. These hits establish successful query-time adjacency participation, potentially including planning/probes; they do not establish that every output row came through adjacency.

The shared-tail harness adds startup-maintenance waiting and adjacency snapshots outside the timing/allocation window. All arms use the same new common prefix. Fanout, fixture, oracle and launcher class bytes are preserved. Candidate and control share the original dependency artifacts; only the LMDB production entry changes. Within that entry, changed bytes are confined to the materializer and its FactorPage/FactorMemo classes. The original frame source remains unchanged.

Timing runs are serial and isolated from other JVMs, builds, profiling, compression and optional hashing. Instrumented page diagnostics are separate and are excluded from every performance estimate.

### Statistical and measurement interpretation

For each fresh JVM, the estimate is the median of all five measured queries. A paired ratio divides candidate and comparator JVM medians from the same round and fixture. The primary summary is the median and complete observed range of all six paired ratios. These ranges are not confidence intervals, and ratios of unpaired descriptive medians do not replace the paired calculation.

The prospective engineering screen requires every shared-tail ledger ratio to be at most 0.25 and every shared-tail all-thread allocation ratio to be below one. On each fixture, the median paired total-latency ratio must be at most one, and no individual latency pair may exceed 1.10. Every condition is required. This is an engineering screen for a memory optimization, not a statistical noninferiority result or a population bound. A separate consistent-latency-win claim requires all six latency ratios below one on the relevant fixture.

Allocation is reported for both the caller and all observed JVM threads. Total timing covers connection acquisition, query preparation, evaluation, consumption/checking and close. `prepareNs` measures `prepareTupleQuery`; `evaluateNs` includes `evaluate`, which can contain planning and iteration construction. `firstRowAfterEvaluateNs` is part of consumption, so it must not be added again to total time. Oracle working storage is allocated before the measurement window; row consumption and checking remain inside it. The first cold query and every warmup are preserved separately from warmed estimates.

Ledger peaks measure reservations managed by the query-memory authority. They do not measure whole-JVM heap, RSS, retained decoded values or the separately configured adjacency budget. Full consumption, one host and one fixed JVM configuration do not establish results for early close, retained-row consumption, concurrent memory pressure, other cardinalities or other strategies. Those remain distinct experiments.

The complete prospective protocol is `protocol.json`, SHA-256 `8ef50b935b83cd22c525616438949f730042ed09c179eff332e39c5601f9b374`. The independently reviewed production patch is `665dca2f8fc662b510fa8d7f285f417a57546e8aec71418f169340c1b8153aa1`; the frozen candidate production tree is `efbb57205da275bd4032ee0f127eb2028010e6707dde8b4538807f94c5542595`.

## Compact pages achieve the predicted representation and charge

Both approved full-fixture diagnostics passed. The compact candidate's final measured page table contains exactly the predicted 16,384 inline pages and 256 dense pages. Those pages charge 12,582,912 bytes (12 MiB), compared with the original control's 268,107,776 page bytes. The separate constrained probe also passed: it observed 1,024 refused promotions while preserving the first cached member, followed by one successful promotion after the competing reservation was released.

These are structural and accounting results. The instrumentation adds counters, weak-reference observations, close-time tables and output. Its elapsed-time, allocation and GC fields must not rank the implementations. The separate balanced timing study determines the performance tradeoff.

### Full shared-tail fixture

Each variant ran in a fresh DENSE JVM with three warmups and one measurement query, using this phase's identical common harness, 64 roots, 128 left members, 128 right members and two tail columns. All eight queries returned 1,048,576 rows and checksum 8327963502356915200 and passed the independent exact bag oracle. The source/JVM/runtime identities matched their pinned protocol, every query started and ended with zero process-ledger use, and the measured selection signatures matched.

Actual execution qualified: normal AUTO, a native outer root, compiled IR for the measured query, ACTIVE adjacency with stable current/applied revisions, and positive query-time adjacency lookup hits. Those hits can include preparation/probes; they do not establish that every result row came through adjacency.

| Measured observation | Original DENSE | Compact DENSE |
|---|---:|---:|
| Final retained pages | 16,364 | 16,640 |
| Inline pages | 0 | 16,384 |
| Dense pages | 16,364 | 256 |
| Pages with one stored weak-reference slot | 16,112 | 16,384 |
| Pages with 64 stored weak-reference slots | 252 | 256 |
| Occupied stored-reference slots | 32,240 | 32,768 |
| Allocated cell capacity | 1,047,296 | 32,768 |
| Declared page charge, bytes | 268,107,776 | 12,582,912 |
| Query-ledger peak, bytes | 268,426,276 | 12,901,412 |
| Successful promotions | 0 | 256 |
| Refused promotions | 0 | 0 |
| Factor-cache hits | 2,048,256 | 2,080,768 |

The original control reproduces the previous sparse-page observation. The candidate allocates one inline cell for each singleton and 64 cells only for each dense page. Its stored-reference occupancy therefore equals its allocated cell capacity in this fixture. This does not imply all weak referents remained live: occupied slots mean nonnull weak-reference objects, and live-referent snapshots are transient observations.

The compact page charges consist of 16,640 shell claims at 512 bytes (8,519,680 bytes) and 256 successful payload claims at 15,872 bytes (4,063,232 bytes). Their sum is exactly 12,582,912 bytes. The original has one actual 16,384-byte claim per dense page; the diagnostic does not fabricate separate shell/payload claims for that control.

Every base, payload, directory and coordinate-memo byte granted in the accepted runs was observed immediately before its explicit release. Page counts, promotion counts, declared charges and exact occupied-offset masks reconcile. The final endpoints are zero. The diagnostic's attribution gate is stricter than a leak check: an earlier ledger close could legitimately zero a reservation before observation, but that would disqualify complete explicit-release attribution under this fixed protocol. It did not occur in these accepted runs.

The candidate also admits 276 more pages and obtains 32,512 more factor hits than the capped original. Increased admission is a separate mechanism from any reduction in lookup cost. This diagnostic establishes the representation and accounting benefit; it does not establish a CPU benefit or equate the reduced reservation to an equal reduction in allocated or retained heap.

Full results and per-target breakdowns are in `diagnostic/analysis.json`. Every raw query remains in `diagnostic/runs/{original,compact}/samples.jsonl`, and every final page key/mask remains in the corresponding `page-records/owner-*.jsonl`. The protocol SHA-256 is `a3a51e4371deda248d3861a46a7ec856a4c0ddb386a9f561b0b76c60e360389a`.

### Persistent refusal and recovery

The separate synthetic probe uses the exact compact diagnostic runtime plus its isolated driver. It makes no query-route or performance claim. A standalone 20,000-byte memory manager first retains A at offset 0. A competing claim leaves only 128 bytes available, so a 15,872-byte promotion cannot fit. Both values are deliberately retained by the synthetic source to remove GC variability.

The probe performs 1,024 B/A alternations. Each B at offset 1 resolves normally and attempts promotion; each A remains a factor hit. Cache charge and constrained ledger remain unchanged after each pair. Once the competing claim is released, B/A/B promotes the page and reuses both members.

All prospective counts matched: 2,052 captured rows, ID checksum 87,210, one A resolver call and 1,025 B resolver calls, 1,025 promotion attempts, 1,024 refusals, one success and 1,026 factor hits. No spill or shed callback ran. The final dense page contains exactly offsets 0 and 1 (mask 3). Materializer charge grows from 2,496 to 18,368 bytes after recovery, and explicit owner close returns the standalone ledger to zero. Its peak is 19,872 bytes from the competing claim.

This demonstrates that refusal preserves the first member, repeatedly retries rather than permanently poisoning the page, never reclaims the competing owner, and recovers when capacity is released. It also confirms that persistent pressure can cause one failed reservation attempt per eligible insertion. The CPU cost of those attempts is not measured here.

Raw probe and page records, compilation/runtime identities, predictions and checks are under `diagnostic/retry-probe/`; `diagnostic/retry-probe/analysis.json` contains the complete result. Fields named `processLedger*` in its raw JSON refer to its standalone testing manager. The probe protocol SHA-256 is `244553ceb44414bacb43267a35faa09ae6391fe4689eb6240609a84c3b150dba`.

### Scope and preservation

Both thin overlays select only the instrumented outer materializer class and its helper. Their respective frozen `FactorPage`, `FactorMemo` and projection-plan classes remain selected unchanged; complete source comparisons and raw javac outputs are preserved. All original production/evidence stays intact. Promotion hooks observe the existing outer call; an unexpected exception would make any transient internal reservation unobservable and fail the diagnostic rather than claim complete accounting. Allocation-failure rollback was source-reviewed, not fault-injected or claimed as experimentally exercised.

No diagnostic fork was replaced or extended. The compiler/JVM slot was released after the two full-fixture runs and the one retry probe. No additional experiment is part of this report.

## Primary design references

The external comparison is limited to representation selection and explicit accounting. The repository-specific constants and weak-reference semantics were designed from the recorded page occupancy and existing contracts. The [Roaring format specification](https://github.com/RoaringBitmap/RoaringFormatSpec), [CRoaring reader implementation](https://github.com/RoaringBitmap/CRoaring/blob/master/src/roaring_array.c), and [Go Roaring implementation](https://github.com/RoaringBitmap/roaring/blob/master/roaring.go) are primary sources for that comparison. `design-research.md` records which source bodies were available and which were not; no claim relies on unavailable bodies.

## Evidence and reproduction map

- `protocol.json`, `BENCHMARK-PLAN.md`, and `run-comparison.py` define the fixed design and validators.
- `comparison-v1/` contains every recorded command, process result, raw sample, root audit, validation, consumed runtime identity and the post-run preservation receipt.
- `independent-results/`, `raw-analysis-v1/`, `BENCHMARK-RESULTS.md`, and `independent-review.md` preserve independent calculations and all detailed cold/warm tables.
- `validation/sparse-budget-red/`, `validation/compact-correctness/`, and `validation/selected-original/` preserve the test-first failure and both passing source-matched gates.
- `diagnostic/` preserves both complete final page tables and the separate refusal/recovery probe, including source-generation patches and compiler records.
- `baseline/`, `runtimes/`, `compiled-v1/`, `candidate-runtime/`, and `experimental-candidate/` preserve the control, common harness, candidate and full source identities.
- `selection-decision.json`, `selected-source-verification.json`, and `delivery/` record the source decision and final archive/patch/runtime verification.

The runtime delivery includes dependency artifacts and class trees, but requires an external compatible Java 25.0.5+8 installation. Its verified relocation scripts preserve original raw commands and emit derivative classpaths and launch plans. They do not assert that a future rerun has passed the original validators, nor that every analysis helper is automatically relocatable. Source, runtime, instrumented diagnostic and selected-production identities remain distinct. Generated stores are excluded from the deliverable archives; exact fixture construction, query hashes, source and commands are included.
