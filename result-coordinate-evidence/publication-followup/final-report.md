# LMDB factor-publication investigation

## Decision and principal finding

The next useful question was whether the coordinate cache spends substantial work publishing entries whose useful repetition is already captured by the ordinary last-value memo. The new source-matched diagnostics answer that question for two fixed, fully consumed workloads: they identify millions of avoidable publication operations and substantial declared cache charge, without losing an observed reuse hit at sufficient memory. Under the smaller query allowance, omitting those entries also admits additional useful entries and nearly halves shared-tail resolver calls.

The CPU tradeoff remains unresolved. The prospectively frozen timing study stopped after nine of its planned 36 JVMs because one shared-tail warmup made one extra resolver call. Its exact-counter qualification therefore failed. The original protocol and every sample remain intact; there are no replacement forks, accepted latency/allocation ratios, or evaluated performance screen. A separate fixed diagnostic investigates resolver variation and does not repair that failed study.

Keep the selected production implementation unchanged. The target-name mask is an experimental intervention, not a production admission policy. The complete experimental source, tests, generators, patches, raw records, and frozen runtime compositions are preserved separately from the selected production source.

## Why publication was the next experiment

The preceding compact-page experiment substantially reduced factor-page charge but did not pass its latency gates. That result makes another representation rewrite less decisive than isolating work that need not happen. The preceding shared-tail diagnostic showed that left-side targets occupied approximately half the factor-page charge yet supplied no successful factor hits.

There are two distinct caches. The ordinary memo remembers the last accepted value for a target. The factor cache remembers a weak value reference under a producer-coordinate key, which can recover a value after the ordinary memo has moved on. A frequently repeated value may be an excellent ordinary-memo candidate while adding no observed benefit to the factor cache. Repetition alone does not establish the incremental value of the second cache.

The experiment therefore omits publication for diagnosed targets while retaining factor lookup, ordinary reuse, ownership, weak witnesses, coordinate transport, and the original dense page representation. The primary counterfactual is DENSE with all targets versus the same DENSE runtime with selected publication omitted. A third arm disables coordinate transport and retains ordinary reuse as a contemporaneous reference.

The implementation is deliberately narrow. A copied `NativeResultMaterializer` source parses the experimental property once into a per-plan boolean array. An early guard in `rememberFactor` uses the canonical target, `plan.aliases[target]`, to return for excluded targets. The rest of that materializer's source body is unchanged. An empty property allows all targets; invalid, duplicate, empty-list-element, or noncanonical alias names fail at setup. The complete copied source has SHA-256 `25a321a3f9cb590492dc9d4efe0a2022ae4f731b6d90db38c5137a4ee1c21fb1`.

All arms share those experimental classes. That holds implementation constant but does not imply equal executed guard overhead: OFF and DENSE call the method at different frequencies. The common boolean array/reference are experimental metadata outside the existing query ledger; allocation measurements include them. No product accounting-completeness claim follows from this experiment.

The intervention can remove coordinate capture, page lookup/admission, cell checks/writes, and later directory/cleanup work. It can also change the cost of surviving lookups through a different cache layout. Even a successful timing comparison would measure this combined intervention, not one instruction's isolated cost.

## Correctness qualification and runtime identity

The required root offline quick clean install passed. The first isolated Surefire attempt encountered a harness setup error: its copied POM's direct invocation had not created the fork temporary directory. That run found 106 cases with 27 setup errors. All original reports are retained. Creating the missing directory in the runner, without changing the tested assertions or production source, allowed the same 106-case selection to pass with zero failures, errors, or skips.

Six new materializer contracts were run against the original implementation before the experimental source was generated or compiled. Five failed assertions and one passed, with no errors or skips. The passing case protects useful publication on an ordinary memo hit. After compiling the experimental copy, all six passed, together with the same 106 existing cases. Thus the overlay qualified against 112 cases; repeated runs are not additional unique tests.

The six new contracts cover configuration rejection, canonical aliases including an alias beyond index 63, per-plan configuration stability, ordinary-hit publication on enabled targets, resolver fallback after an unbound gap or source-view change, retained detached rows, omitted page charges, ledger-first closure, and repeated close. They are direct materializer contracts with controlled sources/coordinates. They do not constitute a new full SPARQL strategy matrix. Existing coverage includes both native IR tiers, transport, fanout, frame lifetime, delayed packed rows, coordinate representations, and row selections. Read-only module formatting also passed.

The fresh build reproduced all 2,778 production class bytes from the preserved selected runtime. Only the generated `maven-config.properties` resource differed; the experiment consistently uses the exact older frozen resource. The publication runtime exposes only two replacement classes, the materializer and its plan, ahead of the frozen common 128-entry classpath. Original nested page and memo class bytes remain in use. Its 129-entry runtime descriptor has SHA-256 `c46fe65a3515872535b55d0c979bf5f84e4d90b59168c86dd3bc14febaa244b2`.

Qualification evidence is in `validation/`, `format-validation/`, `initial/production-runtime-reconciliation.json`, and `runtimes/publication-overlay-v1/`. The source and index preservation receipt is `selected-source-verification.json`.

## Workloads, oracles, and memory control

| Workload | Fixed input | Rows per full query | Full checksum |
|---|---|---:|---:|
| Fanout | 100 measurements, 10 readings, 12 projected targets, two statuses, no unique-payload expansion | 1,152,000 | 3123987218479850048 |
| Shared-tail | 64 roots, 128 left members, 128 right members, two columns per tail, six projected targets | 1,048,576 | 8327963502356915200 |

The fanout harness checks an independent expected count and lexical checksum. Shared-tail additionally checks an independent exact Cartesian bag, including types, values, parent/tuple correlation, unique legal row keys, and multiplicity. Both fixtures use normal AUTO with actual native route and IR-tier observations. No successful route or tier is forced.

The fanout fixture's preserved metadata explicitly says historical Stage-2 artifacts are unavailable and that this is a reconstruction. Available earlier frozen runtimes, frame experiments, and compact-page artifacts were preserved. This study is a matched comparison within the current reconstruction; it is not a rerun of a preserved historical Stage-2 binary or a comparable update to the original 2.21–2.25-second measurements. In this report, original means the selected dense-page implementation at phase entry.

The original shared-tail factor cache reached its admission cap at 256 MiB per query. A publication omission at that allowance can both save work and make room for useful entries. To distinguish those mechanisms, the primary diagnostic and planned timing comparison use 512 MiB per query, while retaining the same 1 GiB heap and 1 GiB global ledger. Separate 256 MiB diagnostic runs measure admission relief. Their instrumented timing and allocation are not performance estimates.

## Confirmed publication and accounting results

Initial discovery and source-matched follow-up diagnostics comprise eight JVMs and 32 queries. Every query passed its result, route, adjacency, counter-partition, and cleanup checks. Recorded tier counters show eight interpreted and 24 compiled native IR binds under AUTO. These executions use three warmups and one observed query per JVM.

The prospective fanout rule selected canonical targets that admitted pages and completed publications but supplied no factor hit or factor weak/witness miss in each of four original-source diagnostic queries. It selected `measurement,reading`. The previously motivated shared-tail pair, `l0,l1`, passed the same checks. Zero-page targets were excluded. Both fixtures have identity aliases and fully bound projections; the independent analyzer verifies those conditions before applying per-target equations.

The following sufficient-memory counts are identical in all four queries of each arm:

| Quantity per query, 512 MiB allowance | Fanout all | Fanout omit | Shared-tail all | Shared-tail omit |
|---|---:|---:|---:|---:|
| Ordinary memo hits | 11,053,899 | 11,053,899 | 4,177,855 | 4,177,855 |
| Factor-cache hits | 2,768,954 | 2,768,954 | 2,080,768 | 2,080,768 |
| Resolver calls | 1,147 | 1,147 | 32,833 | 32,833 |
| Publication method entries | 11,055,046 | 11,055,046 | 4,210,688 | 4,210,688 |
| Early returns under the mask | 0 | 2,304,000 | 0 | 2,097,152 |
| Coordinate-capture attempts | 13,825,135 | 11,521,135 | 6,324,283 | 4,227,131 |
| Admitted factor pages | 124 | 22 | 16,640 | 8,320 |
| Declared factor-page bytes at close | 2,031,616 | 360,448 | 272,629,760 | 136,314,880 |
| Query-ledger peak, bytes | 2,264,968 | 592,008 | 272,948,260 | 136,502,308 |
| Factor-page admission refusals | 0 | 0 | 0 | 0 |

The ledger peak falls by 73.86% for fanout and 49.99% for shared-tail. Callers still enter the publication method; the guard avoids downstream work. The selected fanout targets previously completed 2,304,000 publications but wrote only 1,100 cells. The shared-tail pair completed 2,097,152 publications and wrote 16,384 cells. The omission removes those targets' pages and writes while preserving every observed nonselected hit count.

Each query reconciles total materializer finds with binding reads, ordinary hits plus factor hits plus resolver calls with binding reads, and remember calls with resolver calls. The empty-mask overlay reproduces the original discovery counters and ledger peaks exactly. That establishes the intended counterfactual in those observations; it does not establish deterministic weak-cache counts under every collection schedule.

At the normal 256 MiB allowance, the shared-tail effect is different:

| Quantity per query | All targets | Omit `l0,l1` |
|---|---:|---:|
| Ordinary hits | 4,177,855 | 4,177,855 |
| Factor hits | 2,048,256 | 2,080,768 |
| Resolver calls | 65,345 | 32,833 |
| Factor-page admission refusals | 1 | 0 |
| Admitted pages | 16,364 | 8,320 |
| Query-ledger peak, bytes | 268,426,276 | 136,502,308 |

Removing 8,174 admitted left pages permits 130 additional useful right pages. Right target `r0` grows from 126 to 128 pages and `r1` from 8,064 to 8,192. The resulting 32,512 extra factor hits replace exactly 32,512 resolver calls, a 49.75% reduction. The additional pages contain 256 new cells, consistent with 127 subsequent reuses per cell; individual cell hit counts were not recorded. Raising the all-target allowance instead admits 276 total pages, comprising 146 left and 130 right pages. These are distinct comparisons.

Every admitted page's declared grant was reconciled with explicit release observations. Ordinary, directory, and memo fields are close-time claim snapshots, not complete grant/release traces. Zero query-ledger endpoints are independently checked by the harness. Ledger charge is not physical heap residency, and non-null weak-reference cells do not prove live referents. The instrumentation adds no weak dereference and retains no values, sources, or coordinate owners, but its counters, page snapshots, and JSON still perturb allocation and collection.

Raw evidence and independent checks are in `diagnostic/suites/`, `diagnostic/publication-counterfactual-analysis.json`, and `benchmark/independent-publication-controls.json`. The root-reviewed prospective mask selection is `selection.json`.

## Why the timing comparison stopped

The frozen protocol, SHA-256 `2a94e81f10d63ec177cb998d3270fb9cb48adee569ca1808370f6fc81465107b`, planned three arms, two fixtures, all six arm-order permutations, and alternating first fixtures. Each fresh JVM had eight warmups and five measurements: 36 JVMs, 468 total queries, and 180 measurements. Java was Eclipse Adoptium 25.0.5+8 with a fixed 1 GiB G1 heap. Work ran serially without our builds, profiling, compression, or full runtime hashing overlapping the timed JVMs.

The protocol required exact DENSE-arm resolver, route/tier, and adjacency equality at every same-round query iteration, including warmups. It also required complete result oracles, current active adjacency, compiled measured queries, and zero ledger endpoints. Its prospective performance screen required at least a 5% median omission/all latency improvement on both fixtures, every paired latency ratio below one, and bounded allocation ratios. These were engineering gates, not significance tests.

Execution stopped after nine successful JVM processes, 117 total queries, and 45 measured samples. In round 1 (zero-based), shared-tail warmup iteration -7, the all-target arm recorded 32,833 resolver calls and the omission arm 32,834. The independent audit compared all 39 completed DENSE query pairs. That one counter was the sole mismatch; every other recorded counter, result flag, route/tier observation, adjacency observation, and ledger endpoint matched. All 15 completed measured DENSE pairs had exact resolver equality.

The one extra resolver is not an incorrect result or, by itself, an ownership failure. Nevertheless, the study failed its declared mechanism-control gate. The remaining 27 planned JVMs were not launched. No samples were discarded or replaced. No accepted latency, allocation, preparation-cost ratio, ordinary-reuse ranking, or performance-screen decision is reported from this incomplete prefix. All raw measurement fields remain available, including the stopped pair.

`prepareNs` measures the `connection.prepareTupleQuery` API window; evaluation and first-row phases may contain additional planning or compilation. It is not a complete isolated optimizer-cost measurement. Allocation records bytes allocated during the harness window, not retained memory. Those distinctions apply to the preserved raw fields even though this study has no accepted performance comparison.

The independent partial audit rehashed all 129 runtime entries and 99 watched files unchanged. Evidence is `comparison-v1/`, its unchanged failed `summary.json`, and `benchmark/independent-partial-v1/`.

## Separate resolver-variation investigation

The additional probe has its own prospective protocol and is not a replacement timing run. It reuses the frozen masked diagnostic runtime in four fresh shared-tail JVMs, in fixed order all, omit, omit, all. Each executes eight warmups and five observed queries, for 52 observations. Heap, allowances, input, AUTO selection, retention behavior, and source remain fixed. The only added JVM facility is normal GC/reference logging; no collection is forced.

The probe distinguishes ordinary weak/witness outcomes from the exclusive final factor-cache miss outcomes. An ordinary weak miss can be recovered by a factor hit, and a single binding lookup can encounter both ordinary and factor weak misses. Adding those two counts would double count some events and misstate the number of extra resolutions. The final factor miss partition is reconciled directly with remember and resolver counts, globally and per target.

The probe completed exactly four JVMs and 52 queries, with no execution or correctness failure and no extension:

| Fresh JVM in fixed order | Queries | Resolver calls per query | Ordinary weak misses | Final factor weak misses |
|---|---:|---|---:|---:|
| 00, all | 13 | 32,833 in all 13 | 0 | 0 |
| 01, omit | 13 | 32,834 at warmup -7; 32,833 in the other 12 | 0 | 1 |
| 02, omit | 13 | 32,834 at warmup -7; 32,833 in the other 12 | 0 | 1 |
| 03, all | 13 | 32,833 in all 13 | 0 | 0 |

Both additional resolutions are localized to target `r1`. In each affected query, `r1` factor hits decrease from the same-arm reference's 1,040,384 to 1,040,383, remember calls increase from 8,192 to 8,193, and final factor weak misses increase from zero to one. Publication calls, completed publications, actual writes, and coordinate attempts/successes each increase by one. Every other target counter is unchanged. The ordinary lookup partition is unchanged, and witness misses and page/coordinate admission refusals remain zero.

The exclusive final factor-miss decomposition in each affected query is 69 initial rejections, 24,700 absent-page outcomes, 8,064 absent/mismatched-cell outcomes, one weak-reference miss, and zero witness misses: exactly 32,834 resolver calls. Total ordinary hits plus factor hits plus resolver calls still equals 6,291,456 binding reads. The result and exact-bag oracles, route checks, page grant/release reconciliation, and zero query-ledger endpoints all pass. All 20 later observed queries have the baseline 32,833 resolver count.

These new instrumented observations demonstrate normal fallback after an unavailable factor-cache weak referent. They show that the extra resolver can be fully explained without a changed query result, a witness failure, a new admission refusal, or a broken lookup/remember equation. Their matching arm and warmup pattern makes weak-reference clearance a stronger explanation for the earlier timing event, but the earlier record lacks the branch counters needed to prove that attribution.

GC logs are JVM-wide and are not aligned to individual queries or referents. The diagnostic's page snapshots and JSON alter allocation/collection, with more pages to observe in the all-target arm. Consequently these four forks establish observed mechanisms, not production miss rates, GC-cost differences, or performance comparisons. The original nine-JVM timing study remains failed and incomplete. The fixed probe protocol is `diagnostic/resolver-variation/probe-v1/protocol.json`, SHA-256 `0ba7dd6c7bce031e8c7495e6be958c13444a670ada1b966dde2fdfcb1f54ac5d`; its raw files, summary, and separate independent reconciliation remain available.

The independent checker reread all 52 raw query/owner pairs and all eight prior reference queries, rather than accepting the probe summary as evidence. It reproduced both exact target deltas and every unchanged query, then checked agreement with the driver's summary. Across the probe it observed four interpreted and 48 compiled native IR binds, zero ordinary weak/witness events, zero factor witness events, and zero page refusals. All 130 runtime entries and 113 pinned source/raw/reference inputs were unchanged. The independent audit is `benchmark/independent-resolver-probe-v1/`.

The phase therefore retains 21 experiment JVMs and 201 query records: nine stopped timing JVMs/117 queries, eight initial diagnostic JVMs/32 queries, and four resolver-diagnostic JVMs/52 queries. These groups retain their separate purposes and protocols; their durations are not pooled.

## Engineering implications and next decision

The established opportunity is selective factor-cache admission. For these fixtures, some targets' repetition is already handled by the ordinary memo; their factor entries consume charge and repeated publication work without an observed hit. At a constrained allowance, this can displace entries with demonstrated value. Saved resolver calls and smaller pages are therefore incomplete optimization objectives: publication cost and the distribution of admitted entries also matter.

This agrees with a useful comparison from PostgreSQL's primary implementation: its memoization cost model includes entry/tuple insertion and eviction costs as well as hit probability and underlying work avoided, and its executor can bypass optional caching when storage cannot be admitted. The structures and semantics differ from these weak factor pages; no external implementation or dependency was copied. Source links and the narrowly drawn inference are in `design-research.md`.

Do not generalize the fixture names into product policy, and do not stop publishing on every ordinary hit. The existing `acceptedReplayHitAdmitsItsOriginalFactorCoordinate` contract demonstrates that an ordinary hit at a new accepted coordinate can create a useful later factor hit. A sound rule must consider the complete canonical coordinate identity and its possible recurrence after ordinary-cache displacement, including parents, frames, namespaces, aliases, and accepted/unbound/rejected row behavior.

A static source review also rejects an easy but unproven explanation: the PlanRows bridge already honors a producer's negative `mayReplay` decision before capturing coordinates, in both tiers. Some native IR producers create coordinates directly under a separate conservative expansion analysis. The diagnostics do not map the selected target banks to those producer sites. Producer-site attribution is therefore a useful next structural investigation; simply forwarding an assumed missing bit is not justified. Changing `mayReplay` could also suppress factor lookups and transport, which is a different intervention from this publication-only mask. Details are in `replay-hint-source-note.md`.

A future timing study must declare how weak-reference variation is qualified before collecting fresh data. The probe shows why exact raw resolver equality is an overly strict general control for this weak cache: the same fixed input and valid route can legitimately make an additional resolution after a weak referent is unavailable. Result correctness, route/tier behavior, ownership cleanup, and admission conditions remain exact requirements. Legitimate weak-entry availability can be part of the runtime effect of an allocation-changing optimization; it should not be erased by stronger retention or forced collection solely to stabilize a counter. Conversely, unexplained resolver changes cannot silently be treated as harmless. Lightweight mutually exclusive miss accounting would help distinguish those cases, provided its own cost and accounting are controlled. The failed protocol is not relaxed retrospectively.

The current evidence does not establish behavior across uniqueness, repetition, projection width, early close, retained rows, all strategy families, or concurrent query pressure. It establishes a concrete publication/admission opportunity and a measurement issue that must be resolved before claiming a CPU win. Selected production stays at the existing original dense pages and ownership implementation; all prior uncommitted work is preserved, and no commit or push is made.

## Reproduction and complete deliverables

The source archive contains the complete selected project, previous carried-forward task source, complete experimental Java, tests, Python drivers, source-generation patches, and this report. The full Git patch is against checkpoint `15fe7d5011d27d31d76436238363ee3f34b3ba50`. A separate phase patch is against the previous delivered source tree. The selected core-only phase delta is empty; the experimental selected-to-overlay patch is explicitly separate.

The raw evidence archive preserves initial failures, red/green reports, all completed query records, protocols, independent analyses, commands, logs, and checksums. The runtime archive preserves exact classpath artifacts, source/compile identities, and recorded commands. The JDK and regenerated stores are external. Extracted relocation plans change paths only and are not a claim that every legacy analysis script is portable. Archive contents and patch application are independently verified before delivery; see the delivery manifest and readback receipts.
