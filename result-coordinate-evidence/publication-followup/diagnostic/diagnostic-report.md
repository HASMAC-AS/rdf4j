# Diagnostic evidence for publication omission

The source-matched diagnostic establishes the intended primary comparison: omitting publication for the selected targets removes millions of materializer publication operations while preserving every observed ordinary hit, factor hit and resolver call at a 512 MiB per-query allowance. The timing study without the added publication diagnostics can therefore compare this optional cache work without conflating it with additional successful reuse caused by escaping the previous admission cap.

This conclusion is limited to the two fixtures and their full-consumption execution. It is not a measured latency saving and does not justify a general planner policy. All instrumented timing and allocation fields are excluded from these conclusions.

## Code and execution controls

Six follow-up JVMs used one masked-capable diagnostic source and one runtime composition. The all-target arm passed an empty mask; the omitted-target arm passed `measurement,reading` for fanout or `l0,l1` for shared-tail. The guard acts at the start of `rememberFactor`, before coordinate capture. Factor lookup, ordinary reuse, semantic witnesses, ownership, coordinate transport and the original dense page representation remain active. The projection plan and retained page/memo classes came from the verified experimental runtime. Source binding and classfile field/method compatibility checks ran before Java execution.

Each fresh JVM executed three warmups and one observed query with normal AUTO selection. Two JVMs covered all-target operation at 512 MiB, two covered the respective masks at 512 MiB, and two separately covered shared-tail all/omitted at the normal 256 MiB allowance. The global ledger and fixed G1 heap were 1 GiB throughout. Every query passed the original result oracle, actual-route checks, active-adjacency checks, complete counter partitions and zero ledger endpoints. Shared-tail also passed its independent exact bag oracle. The observed query used compiled native IR. Both fixtures had identity aliases and fully bound projections; every query reconciled find calls with binding reads, ordinary plus factor hits plus resolver calls with binding reads, and remember calls with resolver calls.

All 24 follow-up queries passed. Together with the original-source discovery stage, the complete diagnostic work covers eight JVMs and 32 queries. Every raw record and command is preserved. There were no replacement diagnostic runs. Actual tier counters across those 32 queries record eight interpreted native-IR binds and 24 compiled binds. Tier changes follow normal AUTO behavior; no tier was forced.

## Primary comparison at 512 MiB

These counts and ledger peaks are identical in all four queries of each arm.

| Quantity | Fanout, all | Fanout, omitted | Shared-tail, all | Shared-tail, omitted |
|---|---:|---:|---:|---:|
| Ordinary hits | 11,053,899 | 11,053,899 | 4,177,855 | 4,177,855 |
| Factor hits | 2,768,954 | 2,768,954 | 2,080,768 | 2,080,768 |
| Resolver calls | 1,147 | 1,147 | 32,833 | 32,833 |
| Publication calls entering the method | 11,055,046 | 11,055,046 | 4,210,688 | 4,210,688 |
| Calls skipped by the target guard | 0 | 2,304,000 | 0 | 2,097,152 |
| Coordinate capture attempts | 13,825,135 | 11,521,135 | 6,324,283 | 4,227,131 |
| Admitted pages | 124 | 22 | 16,640 | 8,320 |
| Declared page bytes at close | 2,031,616 | 360,448 | 272,629,760 | 136,314,880 |
| Query ledger peak, bytes | 2,264,968 | 592,008 | 272,948,260 | 136,502,308 |
| Page admission refusals | 0 | 0 | 0 | 0 |

The guard does not prevent callers from entering `rememberFactor`; its skipped-call counter records the early return. That return avoids exactly one coordinate-capture attempt per skipped call in these fixtures. It also removes the selected targets' page admissions, cell comparisons/writes and associated directory and cleanup work. This ablation combines those costs and their downstream effects; it does not isolate one instruction or one allocator.

The selected canonical targets have the following all-target counts, identical in all four discovery queries and reproduced by the empty-mask control. Each has zero factor hits and zero factor weak/witness misses. Both fixtures have identity aliases; these rows therefore require no alias summation.

| Selected target | Publication calls | Completed publications | Actual cell writes | Admitted pages | Declared page bytes | Ordinary hits |
|---|---:|---:|---:|---:|---:|---:|
| Fanout `measurement` | 1,152,000 | 1,152,000 | 100 | 2 | 32,768 | 1,151,900 |
| Fanout `reading` | 1,152,000 | 1,152,000 | 1,000 | 100 | 1,638,400 | 1,151,000 |
| Shared-tail `l0` | 1,048,576 | 1,048,576 | 8,192 | 128 | 2,097,152 | 1,040,384 |
| Shared-tail `l1` | 1,048,576 | 1,048,576 | 8,192 | 8,192 | 134,217,728 | 1,040,384 |

The fanout mask was selected under the prospective rule: positive page admissions and completed publications, zero factor hits, and zero factor weak/witness misses in every one of the four original-source diagnostic queries, after canonical alias aggregation. The preselected shared-tail mask passed those same checks. Fanout `patient` and shared-tail `s,a` have no admitted pages and are excluded even though they have no factor hits. Thus the ablation specifically removes published-but-unused factor entries; unsuccessful publication attempts by zero-page targets remain in scope of ordinary execution.

The selected fanout targets previously completed 2,304,000 publications but wrote only 1,100 cells. Shared-tail's selected targets completed 2,097,152 publications and wrote 16,384 cells. After omission, those targets have no admitted pages, cell writes or factor hits. Every nonselected target retains the same hit counts, and no nonselected target records a skipped publication. No ordinary/factor weak-reference or witness miss occurs in either arm.

The empty-mask experimental overlay reproduces the original-source discovery counters exactly, including coordinate attempts and ledger peaks, for every query. This is a counter-behavior check; the instrumented executions cannot establish the latency cost of adding the mask mechanism.

## Separate admission effect at the normal allowance

At 256 MiB, the original all-target shared-tail cache reaches its admission limit. Omitting the left targets now changes successful reuse as well as publication work:

| Quantity | All targets, 256 MiB | Omit `l0,l1`, 256 MiB |
|---|---:|---:|
| Ordinary hits | 4,177,855 | 4,177,855 |
| Factor hits | 2,048,256 | 2,080,768 |
| Resolver calls | 65,345 | 32,833 |
| Page admission refusals | 1 | 0 |
| Admitted pages | 16,364 | 8,320 |
| Declared page bytes at close | 268,107,776 | 136,314,880 |
| Query ledger peak, bytes | 268,426,276 | 136,502,308 |

Each right target gains 16,256 successful factor hits, for 32,512 additional hits and exactly 32,512 fewer resolver calls per query. Its final page counts rise from 126 to 128 for `r0`, and from 8,064 to 8,192 for `r1`. The omitted targets no longer consume page charge. Every compared reuse/publication/page-accounting counter and the ledger peak in the omitted 256 MiB arm match the omitted 512 MiB arm, including coordinate attempts, hits, resolver calls, page admissions and ledger peak. The allowances and metadata differ; raw instrumented duration and allocation fields are not compared. This establishes an admission-relief effect at the normal allowance; it must remain separate from the primary 512 MiB latency comparison. This page accounting distinguishes two different comparisons. Raising the all-target allowance from 256 to 512 MiB admits 276 additional pages: 146 left-target pages and 130 right-target pages. The left additions are two `l0` pages and 144 `l1` pages; the right additions are two `r0` pages and 128 `r1` pages. Omitting left publication at 256 MiB removes 8,174 existing left pages and permits those 130 additional useful right pages, ending with 8,320 right pages. The extra right pages contain 256 new cells. This is consistent with the fixture's 128 left members: 256 cells times 127 subsequent reuses equals the 32,512 additional factor hits. Individual cell hit counts are not recorded.

## Accounting, evidence and limits

The helper records page grant amounts after successful page construction and observes page claims immediately before each existing explicit close. Every admitted page is released and all granted page bytes are accounted for in every diagnostic query. Ordinary, directory and memo claims are close-time snapshots, not full grant/release traces. The original harness separately reports zero process query-ledger endpoints after closure. These values are declared accounting charges; they do not measure physical heap residency.

The helper holds no values, sources, coordinate owners or weak references and adds no weak dereferences. Page occupancy means non-null weak-reference cells, not live referents. Counter arrays, close-time primitive page snapshots and JSON output still perturb execution, so no duration or allocation estimate from these runs ranks implementations.

The runtime's descriptor used a newer hash-key spelling than the older common runtime. Before follow-up execution, an additive `run_diagnostic_v2.py` accepted both existing recorded spellings. `driver-v2.patch` and its receipt preserve that verification-only change; the original driver and discovery protocol remain untouched. The Java instrumentation, counter definitions and query checks did not change.

`publication-counterfactual-analysis.json` independently rereads raw owner and query JSONL, checks result and accounting equations, and records every paired comparison without timing fields. The raw suites are `overlay-all512`, `overlay-fanout-omit512`, `overlay-shared-omit512`, `overlay-shared-all256` and `overlay-shared-omit256`. `followup-schedule.json` records the prospective six-JVM schedule. The generator, both complete source copies, patches, runtime manifests, commands and all raw records are retained under this diagnostic directory.

The evidence supports timing this exact fixture-specific counterfactual. It does not establish a general classification of consecutive-only targets, behavior under early close or retained rows, or an improvement for other query strategies. A future production rule still needs structural reasoning or representative validation across those cases.
