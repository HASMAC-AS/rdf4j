# LMDB result coordinates: new experiments and the next useful test

## Decision

**The most useful next implementation experiment is a compact initial factor-cache page with fully accounted-for promotion.** Fresh measurements directly show a storage-granularity problem: 16,112 of 16,364 admitted pages contain one occupied slot, yet each page reserves 16,384 bytes for 64 positions. This is a stronger basis for a new candidate than another speculative change to coordinate transport.

The production candidate tested in this phase was smaller: skip saved-frame reference scans while the coordinate owner has never allocated a managed-namespace directory. It passed matching ownership tests and eliminates 34,474,000 coordinate-cell visits per compiled query on the measured fixture. It **did not establish a consistent latency improvement**. Four of six paired rounds favored it, two did not, and it failed the promotion rule fixed before timing. The original production source has been restored. The complete prototype, its runtime, nine new characterization tests, and all results remain available.

Ordinary weak-reference reuse with coordinate transport OFF was faster than both DENSE variants in every matched round. Coordinate transport still lowered allocation by approximately 16.27% and reduced lazy resolver calls from 2,770,101 to 1,147 per query. Those benefits did not translate into lower total latency in this full-consumption workload.

The near-memory-cap page result comes from a separate fixture. It does **not** explain the primary fanout timing gap, whose DENSE ledger peak was only 2,264,968 bytes. A compact-page candidate must be tested on both workloads to find out whether it helps the memory-heavy case without harming the small-cache case.

## What ran

This phase conducted 24 experiment JVMs and 258 complete query executions, plus a separate oracle-validation JVM. The primary timing study accounts for 18 JVMs and 234 queries. The remaining experiments were bounded diagnostics or qualification pilots; their timings are not implementation rankings.

| Experiment | JVMs / queries | Purpose and result |
| --- | ---: | --- |
| Namespace diagnostics v1 and v2 | 2 / 8 | Zero managed namespace operations, lookups or probes on the actual native/direct-adjacency route. All frame ranges entered with no allocated namespace directory. |
| Frame guard, coordinate transport OFF, original DENSE | 18 / 234 | Six complete order-balanced rounds; eight warmups and five measurements per fresh JVM. Guard fails the all-six improvement criterion. Both DENSE variants lose to OFF in all six rounds. |
| Shared-selection OFF/DENSE/ROWS pilot | 3 / 12 | Exact results and cleanup pass, but one selection group per tracked column means the proposed sharing benefit is not exercised. No full timing comparison was run. |
| Page-occupancy diagnostic | 1 / 4 | Confirms predominantly singleton pages, a page-payload admission refusal, exact cache traffic, and complete release accounting. |

The initial root build and formatting succeeded. The narrow neutral-change gate passed on the unchanged implementation and on the candidate: eight test classes, the exact same 103 test cases, zero failures, errors or skips. Both IR tiers and the materializer/transport/ownership paths are covered by this focused selection. This phase did not rerun the previous broad repository test gate and does not claim that every remaining ownership or strategy question is closed.

## 1. Test the opportunity before optimizing it

The first hypothesis was that repeated coordinate namespace tokens caused redundant hash-directory lookups during range retain/release. A namespace identifies a coordinate source's lifetime. Namespace 1 is permanent for this execution; managed namespaces require reference accounting and lookup. A small call-local memo seemed attractive because the namespace directory cannot move during one range operation, while its slots can move between calls after growth or deletion.

Two isolated instrumented builds measured that hypothesis. Each ran three warmups and one measured-stage query using the preserved original DENSE implementation. The second diagnostic added direct observations of the exact proposed null-directory guard condition. It reproduced all 113 existing v1 counters for each corresponding query.

For each compiled query:

| Observation | Retain | Release |
| --- | ---: | ---: |
| Saved-frame ranges | 1,567,000 | 1,567,000 |
| Coordinate cells visited in those ranges | 17,237,000 | 17,237,000 |
| Positioned reference calls within ranges | 17,236,968 | 17,236,968 |
| Direct calls outside ranges | 11 | 11 |
| Managed namespace operations | 0 | 0 |
| Namespace-directory lookups/probes | 0 | 0 |
| Source-batch range calls | 0 | 0 |

Every frame range entered while `namespaceIds == null`. The first naturally interpreted warmup also had zero managed lookups despite visiting 109,641,455 cells in each range direction. Every query returned 1,152,000 rows and checksum `3123987218479850048`, with zero ledger endpoints. Actual direct-adjacency counters recorded 55 successful lookup hits per query, one planner-stats hit, no exact misses and no fallbacks. Readiness was ACTIVE with applied/current revision 1.

**There was no lookup work for the proposed memo to remove in this fixture.** The memo was not implemented or timed. The diagnostic instead exposed semantically empty frame scans as a separate candidate. Counts from instrumented builds establish work and conditions; their duration and allocation observations are not performance evidence.

Evidence: [DIAGNOSTIC-FINDINGS.md](DIAGNOSTIC-FINDINGS.md), [diagnostic-crosscheck.json](diagnostic-crosscheck.json), and the complete `diagnostic-original-v1/`, `diagnostic-original-v2/`, `compiled-v1/`, and `compiled-diagnostic-v2/` directories.

## 2. A correct frame shortcut is not yet a demonstrated speedup

### Candidate and ownership proof

The candidate adds three `namespaceIds != null` guards around the existing saved-frame range retain/release calls in `KernelResultCoordinates.save` and `releaseFrame`. While an enabled owner has never allocated its namespace directory, validated private registers and saved frames can contain only permanent or unbound coordinates. A managed coordinate cannot enter those registers without its directory already existing. Once allocated, the directory is not discarded while the owner remains enabled, so the original reference loops remain active thereafter.

The change preserves frame copying, partial restoration, overwrite handling and active-frame state. It leaves public source-batch retain/release unconditional: a caller-provided array can contain an unknown positive token, and skipping its validation would be wrong. It adds no per-row allocation, new state or ownership policy.

Nine new characterization cases were added before the relevant candidate validation. They cover repeated managed tokens and sentinels, unknown tokens after valid repetitions, collision deletion and directory growth between calls, permanent-coordinate frame aliases/overwrite/partial restore/repeated release, invalid managed tokens before directory creation, and frames spanning the first managed-namespace allocation and subsequent retirement. The final pre/post gate used exactly the same 103 cases. Independent review checked the proof, patch, test identities and source/runtime separation.

The reviewed production patch has SHA-256 `0ceed5b1f28931ae334671960f8f875e94d61883ab4e4ed1da29a3f3db70ac0e`. Only two class files differ in the frozen LMDB production entry: the outer coordinate class and its nested row-selection class, whose source line metadata moves. Every other production byte and the other 126 base runtime entries match the declared control. The candidate tree SHA-256 is `63ca7bd4fcd6f19efc32f1f1194f5f1f990aaf0488cbea538ab79ce4ad222016`.

### Controlled timing design

The three arms were ordinary weak-reference reuse with coordinates OFF, original five-word DENSE coordinates, and the same DENSE implementation with the frame guard. The OFF/DENSE comparison holds ownership and ordinary reuse code identical. All three arms use the same uninstrumented native-plus-direct-adjacency benchmark harness. Diagnostic classes are absent from the timed classpaths.

All six permutations of the three arms ran serially, so each arm appeared twice in every position. Each fresh JVM received eight warmups and all five measured queries. No fork was replaced, sample excluded, warmup tail substituted, or extra round added. The protocol was frozen before timing under SHA-256 `bbad71c3c879b49022ff6cb22d229ee94353961d1f512defdec722fe2a368f54`. The candidate needed correct results/cleanup and all six paired latency ratios below one for promotion.

The fixture used M=100, R=10, projection width 12, two statuses, full consumption and no unique-payload expansion. Each query returned 1,152,000 rows. Graph SHA-256 was `cf637a7e80dbd9468d7e8b04116da072f726038282dfab15380c267c8cc82e9b`; query SHA-256 was `9cffa2dc37bbb1e2743442b228d5ab9a41115bf7c4a6667506231fbda3bfc301`.

Runtime settings were Java 25.0.5+8, `-Xms1024m -Xmx1024m -XX:+UseG1GC`, the same enabled hot counters, a 1 GiB global query-memory authority and a 256 MiB per-query cap. Native evaluation and direct adjacency were enabled with adjacency PREFER. Execution strategy and IR tier remained normal AUTO. No build, test or profiling JVM competed with the serial timed runs.

Every measured query used the same compiled `irKernel` route. Each fresh JVM's first warmup naturally used interpreted IR. All 234 queries had the expected checksum and row count, zero ledger endpoints, ACTIVE unchanged adjacency, 55 successful adjacency lookup hits, one planner-stats hit, no exact misses and no fallbacks. Every root audit found 13 native compilations and zero hosted-generic roots. Query-time lookup hits establish actual adjacency participation, but do not prove every output row was produced by an adjacency lookup. Final artifact/protocol/helper identity checks all passed.

### Every paired round

Each time is the median of all five measured queries within one JVM. Ratios are calculated within the same round. The primary estimate is the median of those six ratios, not a ratio of independently aggregated arm medians.

| Round | OFF seconds | Original DENSE seconds | Guard DENSE seconds | Guard / original DENSE | Guard / OFF | Original DENSE / OFF |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| 1 | 3.604733 | 4.966843 | 4.507606 | 0.907540 | 1.250469 | 1.377867 |
| 2 | 3.381145 | 4.338916 | 4.345982 | 1.001629 | 1.285358 | 1.283268 |
| 3 | 4.065345 | 4.255057 | 4.508796 | 1.059632 | 1.109081 | 1.046666 |
| 4 | 3.114828 | 4.356574 | 3.926290 | 0.901233 | 1.260516 | 1.398656 |
| 5 | 3.328272 | 3.922285 | 3.692140 | 0.941324 | 1.109326 | 1.178475 |
| 6 | 3.239984 | 4.743349 | 4.539673 | 0.957061 | 1.401140 | 1.464004 |

| Comparison | Median paired latency ratio | Full observed pair range | Favorable pairs |
| --- | ---: | ---: | ---: |
| Guard / original DENSE | 0.949192 | 0.901233–1.059632 | 4/6 |
| Guard / OFF | 1.255492 | 1.109081–1.401140 | 0/6 |
| Original DENSE / OFF | 1.330568 | 1.046666–1.464004 | 0/6 |

The guard's median paired result is 5.08% lower than original DENSE, but the range includes a 5.96% slowdown. It fails the fixed consistency criterion. Original DENSE is 33.06% slower than OFF by the paired median; guard DENSE is 25.55% slower. The observed ranges are not confidence intervals, and six matched rounds do not establish a universal effect.

The descriptive medians across each arm's six JVM medians are 3.354708 seconds OFF, 4.347745 seconds original DENSE and 4.426794 seconds guard DENSE. The guard's descriptive median is higher even though its median paired ratio is lower. Different middle-ranked JVMs contribute to the separate arm medians, so these statistics need not agree. The complete round table and fixed paired estimator prevent an aggregation choice from hiding that variation.

### Allocation, preparation and accounting

The following absolute numbers are descriptive medians across six JVM medians. Allocation includes all Java threads, including asynchronous preparation. GB is decimal; ledger reservations are stated in bytes.

| Arm | Allocation GB/query | Warm prepare ms | Warm evaluate ms | First row after evaluate ms | Consume seconds | Close ms | Ledger peak bytes | Lazy resolver calls |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| OFF | 1.630381 | 2.434 | 19.666 | 5.746 | 3.328073 | 0.122 | 2,432 | 2,770,101 |
| Original DENSE | 1.365103 | 2.371 | 23.435 | 7.325 | 4.323574 | 0.133 | 2,264,968 | 1,147 |
| Guard DENSE | 1.365065 | 2.557 | 21.263 | 7.235 | 4.383438 | 0.155 | 2,264,968 | 1,147 |

Both DENSE arms preserve approximately 16.27% lower allocation than OFF by paired ratios. Guard/original-DENSE allocation has median ratio 0.999972, so there is no repeatable new allocation saving from the guard. One guard fork, round four, has a lower regime across all five measured queries: approximately 1.319 GB rather than approximately 1.365 GB. The difference is roughly 40 bytes per output row. Inputs, results, routes and recorded identities match, but its cause is unknown. All five values remain in the analysis; the fork was neither discarded nor explained speculatively as a JIT improvement.

Preparation, evaluation and first-row ratios vary substantially. They do not establish a consistent guard benefit. Consumption dominates latency. First-query preparation is separately preserved: descriptive medians are 73.766 ms OFF, 81.642 ms original DENSE and 75.793 ms guard DENSE. These first interpreted queries are not pooled into the warmed estimate. All phase medians and complete paired ranges are in the dedicated comparison report.

The ledger tracks admitted query reservations. Its peak is not whole-process heap, retained application values, idle prepared recipes or the separate direct-adjacency budget. Every query closed back to zero. The candidate changes neither the peak nor resolver-call counts on this fixture.

Evidence: [frame-directory-guard/RESULTS.md](frame-directory-guard/RESULTS.md), [protocol.json](frame-directory-guard/protocol.json), `frame-directory-guard/comparison-v1/`, [independent-results.json](frame-directory-guard/independent-analysis/independent-results.json), [all-queries.csv](frame-directory-guard/independent-analysis/all-queries.csv), and [independent-result-review.md](independent-result-review.md). Both independent recalculations reproduce the raw-data ratios exactly.

## 3. Shared selections were not exercised by the pilot

The separate pilot used 64 anchors, a 128-row left tail and a 128-row right tail, with two correlated projected columns per tail. Six bindings are consumed: `s`, `a`, `l0`, `l1`, `r0`, `r1`. The graph contains 32,832 triples; the Cartesian output contains exactly 1,048,576 rows with checksum `8327963502356915200`.

An independent oracle verifies IRI/type membership, within-tail correlation, dense Cartesian keys, duplicates, cardinality and checksum. Its scratch bitset is allocated before measurement. A separate oracle-only run checks a small exact case and seven adversarial cases. The OFF, DENSE and ROWS pilot then ran three warmups and one measured-stage query per fresh JVM. All 12 full queries passed the exact oracle and ended with zero ledger use.

ROWS encoded all 1,048,576 rows into 4,096 windows but produced 16,384 groups for 16,384 tracked column-window positions. Thus groups per tracked column equals 1.0, and factorized engagement was zero. The intended common row selection across columns was absent. Source inspection and these counters suggest that normal AUTO's lowering exposes separate origins for the optional chains; that mechanism is an inference, because no lowered-plan dump was captured. **The sharing gate failed, so no balanced shared-selection timing study was conducted.** One sample per arm cannot support a latency ranking in any case.

The pilot did reveal a different issue. OFF peaked at 1,472 ledger bytes, DENSE at 268,426,276 bytes, and ROWS at 268,424,234 bytes. Lazy resolver calls fell from 2,113,601 to 65,345 in the coordinate variants. The near-cap reservation warranted a diagnostic of actual page occupancy.

This fixture records native outer-root/IR-route evidence and configured direct-adjacency PREFER with readiness, but its harness does not record individual query-time adjacency hits. It must not inherit the stronger hit-count claim from the primary fanout harness.

Evidence: `shared-selection/pilot/`, [prospective-protocol.json](shared-selection/prospective-protocol.json), [fixture-validation.json](shared-selection/fixture-validation.json), and the complete driver and four Java source files under `shared-selection/`.

## 4. Sparse factor pages consume the query allowance

One separately instrumented original-DENSE JVM ran the same shared-tail fixture for three warmups and one measured-stage query. The overlay counts page admissions, hits, refusals, occupied slots and released charges without changing coordinate keys, reservations or cache policy. It exposes only the two reviewed diagnostic classes; the original 127 runtime entries and the shared harness entry remain exact. Close-time scans record every admitted page key. There were no pre-final nonempty clears or namespace retirements, so the final table covers all admitted pages.

Each of the four queries admitted and released 16,364 pages. Of these, 16,112 had one nonnull weak-reference slot and 252 had all 64 slots occupied. There were 32,240 occupied positions out of 1,047,296 available positions: **3.0784% occupancy**. All pages belonged to namespace 1.

| Target | Pages | Occupied positions/page | Charged page bytes | Successful factor-cache hits |
| --- | ---: | ---: | ---: | ---: |
| `l0` | 126 | 64 | 2,064,384 | 0 |
| `l1` | 8,048 | 1 | 131,858,432 | 0 |
| `r0` | 126 | 64 | 2,064,384 | 1,024,128 |
| `r1` | 8,064 | 1 | 132,120,576 | 1,024,128 |
| Total | 16,364 | 32,240 positions overall | 268,107,776 | 2,048,256 |

The page payload alone charges 255.6875 MiB. Left-side targets charge 127.71875 MiB and supply zero factor-cache hits in this traversal. That is not an absence of value reuse: ordinary memo hits already serve consecutive repeats, and the right-side factor pages demonstrably serve nonconsecutive repeats. Cache traffic reconciles exactly:

    1,048,576 rows × 6 consumed bindings = 6,291,456 reads
    4,177,855 ordinary hits + 2,048,256 factor hits + 65,345 resolver calls = 6,291,456

One payload reservation failed at ledger usage 268,426,276 bytes, leaving 9,180 bytes below the 268,435,456-byte cap, insufficient for another 16,384-byte page. In the measured compiled query, the first refused key belongs to `l1`, namespace 1, group 10, parent 8049, tuple column 0, page index 0; interpreted warmups use different parent-token representations. The refusal latch subsequently suppresses 67,583 missing-page publication calls; those are calls, not unique refused pages. No directory or coordinate-memo reservation failed.

All accounting reconciles. Every page's observed release matches its grant, for 268,107,776 bytes. Directory grants and releases total 525,888 bytes, with a final directory claim of 262,272 bytes and a maximum old/new resize overlap of 393,472 bytes. The coordinate memo's 1,600 bytes are released. At final snapshot, pages plus final directory plus memo plus 1,472-byte base sum to 268,373,120 bytes. All ledger endpoints are zero, and every query passes the independent exact result oracle.

Occupied weak-reference slots are not necessarily live referents. Calling `WeakReference.get` during observation can temporarily strengthen a referent, and the instrumentation adds counters, scans and output. Consequently its timings, allocation totals and live-referent counts are not comparative performance evidence. The ledger is reservation accounting, not a measured heap size. The exact occupied-slot count and reservation reconciliation establish the granularity issue without making either mistake.

Evidence: [page report](shared-selection/page-occupancy/report.md), [analysis.json](shared-selection/page-occupancy/analysis.json), `shared-selection/page-occupancy/page-records/owner-{1..4}.jsonl`, and `shared-selection/page-occupancy/run/samples.jsonl`.

## 5. What to implement and test next

A compact initial page should preserve the entire existing coordinate key, weak-reference semantics, value-ID/mutation witnesses, owner lifetime, cleanup and refusal fallback. Begin with a singleton or small packed representation, then promote only when occupancy makes a dense representation worthwhile. Account for both old and replacement storage during promotion; if optional promotion is refused, retain a correct existing representation and use the existing resolution fallback for uncached positions. Do not change admission policy in the same candidate.

This separates two questions that the new evidence raises. First, can storage scale with actual occupied positions rather than precharging 64 cells? Second, should a page be admitted if the ordinary memo already serves all its repeats? The first has a direct representation experiment. The second needs a separate admission experiment because a blanket rule about left-side columns could fail for another traversal or retained result pattern.

The next comparison should hold repaired ownership and the original frame behavior constant. Compare ordinary reuse OFF, current DENSE and compact-page DENSE on the exact primary fanout and memory-heavy shared-tail fixtures. Preserve normal AUTO and actual route checks. Repeat correctness gates for both IR tiers, retained rows, early close, unknown tokens, mutation witnesses, promotion failure and full cleanup. Before a broad workload sweep, establish that the isolated page representation improves the observed near-cap case without regressing the low-page-count case. The broader repetition, width, unique-value and consumption-pattern sweep remains necessary for a general selection policy.

Useful primary-source comparisons informed this choice. [DuckDB's execution format](https://duckdb.org/docs/2.0/internals/vector) and [DataChunk::Slice implementation](https://github.com/duckdb/duckdb/blob/main/src/common/types/data_chunk.cpp) show why a selection can be shared across columns only when execution exposes a common row origin. [Arrow Java's memory model](https://arrow.apache.org/java/current/memory.html) and [ReferenceManager interface](https://arrow.apache.org/java/current/reference/org.apache.arrow.memory.core/org/apache/arrow/memory/ReferenceManager.html) provide a comparison for explicit accounting and shared lifetime. The [Roaring format specification](https://github.com/RoaringBitmap/RoaringFormatSpec) uses different representations according to cardinality, a useful analogy for compact-to-dense promotion. Its payload and thresholds are not transferable to this cache. No external code was copied and no dependency was added. These comparisons motivate hypotheses; only matched RDF4J measurements can establish benefit.

The compact-page candidate is a conclusion and a concrete next experiment, not an implementation or speedup claimed by this phase.

## Final source state and verification

The source remains on `GH-0000-lmdb-lazy-result-coordinates` at checkpoint `15fe7d5011d27d31d76436238363ee3f34b3ba50`, with the earlier uncommitted ownership repairs preserved. No commit or push was made. This phase retains nine new cases in `core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/evaluation/codegen/KernelResultCoordinatesTest.java` and the experiment/reproduction sources. Its production guard is preserved only as a prototype under `runtimes/frame-directory-guard/`.

The restored production source exactly matches its preserved preimage, SHA-256 `e079c212761f295e5c37566b6c71888d5b7240b1ac0891fdad521ec423aee4c4`. All 2,055 module source files exactly match the successful unchanged-production 103-case gate. The restoration install succeeded. All 2,778 rebuilt production class files are byte-identical to the frozen original. The sole rebuilt resource difference is `maven-config.properties`: `formatter.skip=true` under the restoration quick profile versus `false` in the frozen control. No timed artifact was replaced. The source and class comparisons are recorded explicitly instead of claiming that differing build metadata is byte-identical.

Evidence: [selection-decision.json](selection-decision.json), [restoration verification](validation/restored-original/verification.json), [pre-change gate](validation/original-frame-characterization/results.json), [candidate gate](validation/frame-directory-guard/results.json), [independent invariant review](independent-review.md), and [independent results review](independent-result-review.md).

The historical original baseline and Stage-2 artifacts were not recovered. This phase's control is the exact preserved repaired implementation, with a separately identified common harness enabling direct adjacency. It is not a reconstruction labeled as the historical baseline. Earlier timings with adjacency disabled and the older 2.79-to-1.42 GB allocation observations are not pooled into these new results. Full consumption dominates this phase; early-close, retained-row, projection-width, uniqueness and lower-heap conclusions must come from their own evidence. Prior unresolved low-heap suite failures and broader strategy questions remain outside this candidate's passing gate.

## Reproduction and deliverables

Repository root for the recorded runs was `/workspace/scratch/37853bcaea36/rdf4j`. All commands, JVM arguments, outputs, source maps, runtime manifests, protocol hashes, raw JSONL, page keys, checksums, exact test XML and analysis programs are preserved. Run drivers into fresh directories: they refuse existing output rather than replacing evidence.

The initial root build was:

    ../mvn -o -Dmaven.compiler.showWarnings=false -T 1C -Pquick -DskipFrontend=true clean install

The wrapper supplies JDK 25 and the workspace-local Maven repository. Focused test and format commands are recorded by `run-focused-validation.py` under `validation/<stage>/`; tests do not use `-am` or `-q`. The final restoration install records its full command and successful exit under `validation/restored-original/`.

The comparison protocol, exact execution command and runtime-freeze instructions are in [frame-directory-guard/README.md](frame-directory-guard/README.md). Recalculate its raw results independently with:

    python3 result-coordinate-evidence/namespace-followup/frame-directory-guard/independent-analysis.py \
      result-coordinate-evidence/namespace-followup/frame-directory-guard/comparison-v1 \
      --output /absolute/new-analysis-directory

Complete code delivery includes the repository source snapshot, the full patch from checkpoint `15fe7d5011`, a narrow new-phase delta, and the separate unpromoted guard source/patch. The evidence bundle contains this report and raw results. A separate self-contained frozen-runtime bundle contains the shared base artifacts once, each used overlay, complete manifests and relocation mappings. Artifact inventory and verification receipts accompany the archives; generated stores and cache directories are excluded. Download links and exact archive identities are supplied with the final response.
