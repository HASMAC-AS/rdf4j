# Independent compact-page review

## Scope and status

This review covers the compact initial factor-page representation, its new materializer tests, the prospective two-fixture comparison, and the separate diagnostic overlays. It does not approve a runtime or allocation claim before those experiments finish. The reviewer edited only `NativeResultMaterializerTest.java` and this additive report; production implementation, formatting, Java execution, runtime freezing, and benchmark execution belong to the coordinator or the other assigned agents. No JVM or build was launched by this reviewer.

The production review found no blocking correctness defect in the candidate described below. Candidate test gates were still running when this initial report was written. Their exact outcomes, source identities, and subsequent diagnostic observations will be appended after reviewing preserved raw records.

## Reviewed identities

Paths below are relative to the repository root unless stated otherwise. SHA-256 values were read from the actual files after formatting.

| Artifact | SHA-256 |
|---|---|
| `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/NativeResultMaterializer.java` | `2b8615ab1b078b7c32b78b5e3d801a0f3eac99e19eed5239ced30570cb5a6e39` |
| `result-coordinate-evidence/compact-page-followup/candidate-production-draft.patch` | `665dca2f8fc662b510fa8d7f285f417a57546e8aec71418f169340c1b8153aa1` |
| `core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/evaluation/NativeResultMaterializerTest.java` | `47306d60bfdc4f2d1658aee3cc634727b1634cbf0f9f74a1e9071e44b5ad1b0d` |
| `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/codegen/KernelResultCoordinates.java` | `e079c212761f295e5c37566b6c71888d5b7240b1ac0891fdad521ec423aee4c4` |
| `result-coordinate-evidence/compact-page-followup/run-comparison.py` | `97740361687738fb2675adfc3f9614e4c3742e593eca55db6bb7177ae6e7ea52` |
| `result-coordinate-evidence/compact-page-followup/protocol-draft.json` | `93fb264e629824a1458f195bdf444c0901cf3548a05fcbb522933faa028cd360` |
| `result-coordinate-evidence/compact-page-followup/compiled-v1/runtime-descriptor.json` | `c6c46c4dec351666e404dddf9d374aaee619f742b6d6ba4adbde32dcbb27e815` |

The phase baseline source map gives materializer hash `cb0749719313b85c8cd134b6c03085f712915188f0e53333cbd46e19e2f0f96e` and test hash `a5e15157c6792290a8462fa01b3187c4c9d7d318fcaa4425314749b2529709a9`. The current frame-coordinate source hash matches its phase baseline exactly. The previously rejected frame scan guard is not part of this candidate.

## Production invariants and failure behavior

The candidate preserves one `FactorPage` object for its entire lifetime. This matters because both `recentFactorPage` and `FactorMemo.pages` retain page references. Promotion changes the storage inside that object, so these references remain valid without walking or rewriting the directory or memo.

The complete key is unchanged: canonical target, namespace, group, parent, tuple column, and the long page index. The inline offset is only the within-page six-bit position. The new `findSingleFactor` checks the offset, ID, weak referent, semantic version, revision ID, and revision-owner witness in the same order and manner as the existing dense lookup. Successful lookup copies the complete witness tuple into the ordinary cache. No cache field newly owns a strong RDF value or revision owner.

The first cell lives in the page shell. Repeated publication at the same position replaces its witness tuple without promotion. An explicitly cleared cell may be reused at a different position. A nonnull stored weak-reference object remains occupied even if its referent has died. The code does not perform an extra weak-reference read to create insertion space, and it does not demote dense pages. The existing weak-miss test depends on this distinction: it clears A's referent, inserts B at a second offset, then requires A's later lookup to record the weak miss. Promotion copies the original weak-reference object rather than silently discarding it.

The accounting model is a 512-byte shell claim plus a separate 15,872-byte optional dense-payload claim. Total dense charge remains 16,384 bytes. `Reservation.tryGrow()` was deliberately avoided because that method calls `makeRoom()` and may reclaim another owner's memory. `FactorPage.promote()` uses `tryReserveWithoutReclamation()` before allocating any dense arrays. It builds all five arrays in locals, copies the original ID/version/revision/owner/value tuple to `singleCell`, and only then publishes the arrays and payload reservation. The shell remains charged throughout. Normal allocation/copy failures close only the new claim and leave the existing singleton available; the exception remains visible.

Promotion runs under the existing materializer lock. It introduces no source callback, value callback, or reclamation callback within allocation and publication. The enclosing scope and witness checks remain the existing ones. The dense path retains its original checks and assignments after one representation branch. Whether this branch and the now-mutable dense-array fields affect generated machine code is a benchmark question, not something this source review can establish.

A refused promotion preserves the singleton and returns to ordinary source resolution. It does not set the global missing-page admission latch, so another 512-byte page may still fit. The next distinct-position insertion retries; no sticky per-page refusal or new manager epoch was added. This can acquire the manager monitor repeatedly under persistent pressure, which the separate retry-count diagnostic will expose without pretending to measure its CPU cost.

`retainedBytes()` adds both claims. Page close clears inline weak references and dense weak-reference arrays, closes the payload if present, and closes the shell. Namespace retirement still invalidates memo references before removing pages and closes each removed page. Both reservations have idempotent close behavior, including when the query ledger already released them. The shape change does not move the existing scope/activation/abandon/final-close ownership boundaries.

Publication on ordinary last-value hits remains intact. It is not a free omission: `acceptedReplayHitAdmitsItsOriginalFactorCoordinate` demonstrates that an accepted repeated value can need publication at another source coordinate before a later replay.

## Test-first evidence and test coverage

The coordinator preserved the smallest new behavior test before implementing production. I independently read its raw report and JSON result under `validation/sparse-budget-red/`:

```text
Tests run: 1, Failures: 1, Errors: 0, Skipped: 0
sparseFactorPagesReuseWithinCompactPageBudget
actual:   [Lookup[id=42, mask=4], Lookup[id=43, mask=4], Lookup[id=42, mask=4]]
expected: [Lookup[id=42, mask=4], Lookup[id=43, mask=4]]
```

That method captures A, B, A at two different long-position page keys under a 4,096-byte query limit. It requires value identity, two source resolutions, and released endpoint ledgers. The original 16,384-byte page representation cannot admit the required pages. The method was left unchanged while the other tests were added.

The new and revised methods have the following separate purposes:

| Method | Coverage | Original-control expectation |
|---|---|---|
| `sparseFactorPagesReuseWithinCompactPageBudget` | Actual reuse benefit under a small budget, across distinct page keys | Intentionally red; observed before production edits |
| `refusedFactorPagePromotionPreservesCellsAndRetriesAfterMemoryRelease` | Exactly 512 bytes of free capacity; failed large promotion preserves A; another compact page can admit; no spill/shed; blocker release permits precisely 15,872 bytes of later growth; all members replay | Intentionally red for the old representation |
| `factorPagePromotionPreservesAllLongPositionCells` | Starts at offset 63 of a long page index, inserts offset 0 next, fills all 64 positions, clears the ordinary entry before every replay, and checks the unchanged 18,368-byte total including ordinary/directory/memo claims | Designed to pass both representations |
| `factorPageReusesClearedCellAfterSamePositionWitnessReplacement` | Rejects a mutated native witness, republishes at the same offset without growth, then uses an uncachable replacement to clear the cell and proves another offset can reuse the empty slot without growth | Designed to pass both representations |
| `factorPageRetirementAndLedgerCloseReleaseMixedPageRepresentations` | Two namespaces each contain a promoted page and a singleton page; retiring one preserves the other's values and exact relative charge; ledger-first and repeated owner close leave retained rows usable | Designed to pass both representations |
| `factorPagePayloadAdmissionDoesNotReclaimSharedQueryMemory` | Existing test now reserves all but 16 bytes of actual remaining budget, preserving the same page-refusal/no-reclamation condition under either representation; forced second-page replay proves that refused page was not retained | Revised representation-independent setup, designed to pass both |

The old payload-refusal test's fixed first-page charge of 17,248 bytes described the previous layout and was unsuitable after compact admission. Its result identities and no-reclamation checks were retained, a forced uncached replay was added, and accounting now compares actual cache claims with the competing reservation. This is a controlled setup change, not an assertion that refusal may disappear.

The three new representation-independent methods plus the revised old refusal method are the neutral control selection. The two small-memory benefit/refusal methods are separate new-behavior cases. Existing tests continue to cover aliases, roles, scope transitions, mutation witnesses, weak misses, activation changes, namespace overlap, retained buffered consumers, and publication on ordinary hits.

`git diff --check` passed for the edited test. The copyright checker completed with exit code zero and reported: `All files have valid copyright headers and SPDX lines.` Java test outcomes beyond the preserved smallest red case are pending in this initial record. Conditional GC tests remain conditional evidence; the deterministic mutation/empty-cell cases do not use GC or reflection.

## Prospective timing review

I reviewed `run-comparison.py`, `protocol-draft.json`, `prepare-harness.py`, `compile-harness.py`, and `freeze-candidate-runtime.py`. The approved design is 36 fresh serial JVMs: three arms across all six order permutations, separately for fanout and shared-tail. Fixture order alternates across rounds. Every fork retains all eight warmups and all five measured queries. Analysis uses the median of all five measured queries in each fork, then pairs ratios within the same fixture and round. The two fixtures are not pooled.

The engineering acceptance screen is encoded as agreed before data: every shared-tail paired ledger ratio must be at most 0.25 and every allocation ratio must be below 1; each fixture's median paired latency ratio must be at most 1 and every individual paired latency ratio at most 1.10. This is a practical acceptance screen, not statistical noninferiority. A consistent latency-win statement separately requires all six latency ratios below 1.

The runtime freezer checks a source-matched passing validation record, exact permitted production path, reviewed production-patch hash, and original source preimage. It replaces only the LMDB production classpath entry and preserves all other 126 original entries. The common prefix is identical in all three arms and is explicitly uninstrumented. It preserves the existing fanout driver, shared-tail fixture/oracle, and root launchers. Only shared-tail's main harness gains the existing adjacency observer and a startup maintenance wait. Both adjacency snapshots occur outside the query timing and allocation windows.

Every measured comparison is guarded by input/checksum identity, zero endpoint ledgers, native compile/root evidence, normal AUTO selection, compiled measured IR, direct-adjacency readiness and positive lookup-hit deltas. The measured route histogram must match the first qualified fork within each fixture. Protocol, helper, source/runtime, and common-prefix identities are checked before use and preserved afterward. Failed or unqualified forks stop the study, retain their raw outputs, and do not yield accepted comparison estimates or replacement forks.

Timing data can prove coordinate admission and released ledger endpoints. The common timing harness has no explicit coordinate-owner close counter; that stronger lifecycle observation belongs to diagnostics or tests. Positive adjacency lookup hits cover preparation plus execution and do not prove that every emitted row used adjacency.

## Diagnostic review and limits

The separate representation diagnostic uses original and compact DENSE runtimes, each with three warmups and one observed query. Its source generator preserves the plan, `FactorMemo`, and `FactorPage` source regions exactly. Only the outer materializer and a standalone diagnostic helper are selected into the runtime overlay; unchanged nested classes come from the chosen frozen production runtime. This avoids attributing shifted debug line tables to a behavioral nested-class change.

The hooks distinguish the original single 16,384-byte claim from compact's 512-byte shell and 15,872-byte payload. They count ordinary/factor hits, admissions and refusals, promotion attempts/successes/refusals, directory growth and overlap, memo claims, and explicit release observations. Successful compact promotion is counted around the unchanged outer call. An exceptional promotion may hide an internal transient grant/release; such a call invalidates the diagnostic rather than manufacturing complete accounting. Allocation failure rollback is reviewed in source, not fault-injected here.

A primitive final page table records complete keys, compact/dense representation, inline offset, stored-reference occupancy, a 64-bit occupied-offset mask, live weak referents, and both observed reservation sizes. The analyzer normalizes masks to 64 bits, so offset 63 and a full mask are handled correctly. Earlier removals are counted separately and cannot be silently included in final-table key distributions. The final snapshot is inside a try/finally that retains original owner cleanup, and JSON output follows cleanup.

The reviewer requested and verified four pre-pin refinements: page-admission attempt reconciliation; post-run overlay runtime verification; explicit compile/run timeout outcome records; and an explicit distinction between strict release-attribution qualification and a production leak claim. A reservation may already have been released by ledger close, so a positive grant-minus-observed-explicit-release difference alone is not a leak. This particular diagnostic requires complete explicit attribution for acceptance and must report any loss of attribution without substituting another run.

Instrumentation changes counters, fields, final-close allocation, output and weak-reference observation. It cannot rank latency, allocation or GC behavior. Live weak-referent reads can briefly strengthen references and are snapshots, not stable retained-heap measurements. Ledger charges are reservation accounting, not measured heap sizes.

The prospective geometry of 16,384 singleton pages plus 256 dense pages implies 12,582,912 declared page bytes under the new charge model. This remains a prediction until the raw records confirm or refute it. More admitted pages and fewer resolver calls would combine representation savings with improved admission; they must not be presented as evidence that the isolated page lookup became faster.

The separate candidate-only retry probe deliberately holds A and B strongly and uses a standalone 20,000-byte manager. Its arithmetic was independently checked: `1 + 2 * 1024 + 3 = 2052` rows, checksum `42 + 1024 * 85 + 128 = 87210`, resolver calls `1 + 1025 = 1026`, 1,026 factor hits, 1,025 promotion attempts, 1,024 refusals, and one successful promotion. The expected final occupied mask is 3 and charge 18,368 bytes. This probe validates repeated refusal and recovery without a query-route or timing claim. Its raw `processLedger*` field names refer to that standalone manager, as its protocol now states.

The prior shared-tail occupancy diagnostic motivates compact pages: it observed 16,112 singleton pages among 16,364 admitted pages and 3.08% occupied cell capacity. That fixture's near-cap pressure must not be used to explain the separate primary fanout result, whose prior dense ledger peak was only 2,264,968 bytes.

## Pending appendices

Exact source-matched gate results, frozen protocol/runtime identities, diagnostic results, and any final independent raw-timing review will be appended after those artifacts exist. No passing post-change result or performance outcome is assumed by this initial review.

## Appendix A: completed correctness gate and freeze

The preserved `validation/compact-correctness/surefire-reports/` XML files independently reconcile to **108 tests, zero failures, zero errors, and zero skips across eight reports**. The per-class counts are:

| Class | Tests |
|---|---:|
| `NativeResultMaterializerTest` | 42 |
| `LmdbNativeProjectionValueReuseTest` | 12 |
| `LmdbNativeIrFactorCoordinateTransportTest` | 10 |
| `LmdbNativeIrOptionalFanoutCoordinateTest` | 2 |
| `LmdbNativePackedFtreeTest` | 1 |
| `KernelResultCoordinatesTest` | 19 |
| `KernelResultCoordinateRepresentationsTest` | 7 |
| `KernelResultRowSelectionsTest` | 15 |
| **Total** | **108** |

A multiset comparison of `(testcase.classname, testcase.name)` with the earlier `namespace-followup/validation/original-frame-characterization` raw XML preserves all **103 prior test identities** and adds exactly the five methods listed above. No old test identity was removed. This does not claim that every test's source was byte-identical: the existing payload-refusal setup was deliberately revised as explained above. The independent computation is preserved in `independent-correctness-reconciliation.json`, SHA-256 `3a3dfd27a6337f5c285b4a081f011e80301307937023130cb5963c41d099f88d`.

The validation's `sources-before.json` and `sources-after.json` are identical. They also equal the LMDB module subset of the candidate's frozen full source manifest. The tested materializer, test, and frame hashes match the identities recorded at the beginning of this review. The frozen `runtimes/compact-page/production-delta.patch` has the same reviewed hash `665dca2f8fc662b510fa8d7f285f417a57546e8aec71418f169340c1b8153aa1`.

The gate includes distinct compiled and interpreted IR reuse, bag, retained-row, early-close, cancellation, and cancellation/close-race cases. Their source checks actual tier counters; these are targeted correctness routes. They should not be confused with the separate timing protocol's normal AUTO selection. This is the selected eight-class gate, not a rerun of the entire repository suite.

The frozen two-fixture protocol is `protocol.json`, SHA-256 `8ef50b935b83cd22c525616438949f730042ed09c179eff332e39c5601f9b374`. It binds the reviewed production patch and unchanged common driver. The comparison has not yet been evaluated by this appendix.

## Appendix B: independently reconciled diagnostic results

The two main diagnostic forks and the separate retry probe completed. I recomputed the following from raw per-page JSONL and raw query JSONL, without importing the diagnostic analyzer. The main receipt is `independent-diagnostic-reconciliation.json`. Every one of the four queries per variant had the same structural counts shown here:

| Observation | Original DENSE | Compact DENSE |
|---|---:|---:|
| Pages admitted, present at final close, and released | 16,364 | 16,640 |
| Inline pages | 0 | 16,384 |
| Dense pages | 16,364 | 256 |
| Stored weak-reference slots | 32,240 | 32,768 |
| Declared and observed page-claim bytes | 268,107,776 | 12,582,912 |
| Successful promotions | 0 | 256 |
| Promotion refusals | 0 | 0 |
| Base-page claim refusals | 1 | 0 |
| Peak query ledger bytes | 268,426,276 | 12,901,412 |
| Final snapshot ledger bytes | 268,373,120 | 12,848,256 |
| Ordinary cache hits | 4,177,855 | 4,177,855 |
| Factor cache hits | 2,048,256 | 2,080,768 |
| Resolver calls | 65,345 | 32,833 |

Each final page key is unique within its owner; every namespace is 1. No page was removed before final close, so these tables cover all admitted pages. The raw masks reconcile with stored-reference counts. Compact page base claims total 8,519,680 bytes and promoted payload claims total 4,063,232 bytes, summing to 12,582,912. Base, payload, directory, and memo granted bytes each equal the observed bytes immediately before their explicit releases. All query ledger endpoints are zero.

The compact target distribution is exactly 128 full dense pages for each of `l0` and `r0`, and 8,192 singleton inline pages for each of `l1` and `r1`. Each right-side target records 1,040,384 factor hits; each left-side target still records zero factor hits. The representation removes the old cap refusal and admits 276 additional page keys. It does not introduce a policy excluding the left-side entries.

The hit/resolver partition reconciles independently for every query:

```text
Original: 4,177,855 + 2,048,256 + 65,345 = 6,291,456 binding reads
Compact:  4,177,855 + 2,080,768 + 32,833 = 6,291,456 binding reads
```

The 32,512 extra factor hits equal the reduction in resolver calls. Directory, memo, and ordinary claims remain 262,272, 1,600, and 1,472 bytes respectively. Both ledger peaks exceed their close snapshots by 53,156 bytes. The page-charge reduction and total peak reduction are both 255,524,864 bytes. These are allocation-authority observations; instrumented wall time and heap allocation remain excluded from performance conclusions.

Independent raw route checks confirm all eight queries used normal AUTO/native IR, with positive direct-adjacency lookup deltas of 10 per query, stable ACTIVE applied/current revisions, and one semantic native compile per query. Both post-main root audits reconcile to four native compiles and zero hosted-generic roots. The observed measurement query in each fork used compiled IR. The exact bag oracle, 1,048,576 rows, checksum 8327963502356915200, and zero ledger endpoints all passed.

The small retry probe's raw record and page record independently satisfy the predicted 2,052 rows, checksum 87,210, 1,025 attempts, 1,024 refusals, and one promotion. Its 1,026 resolver calls plus 1,026 factor hits account for every row; there are zero ordinary hits and zero spill/shed callbacks. The surviving page is dense with mask 3 and two occupied slots, and its observed claims are exactly 512 plus 15,872 bytes. Initial and pressured cache charge stays 2,496 bytes; recovered charge is 18,368 bytes. The standalone manager peaks at 19,872 bytes and ends at zero. Every base/payload/directory/memo grant is observed at explicit release. This establishes retry behavior and cleanup, not retry CPU cost.

No accounting or observed-route issue in these diagnostics blocks the separately frozen timing study. That study still determines whether the memory optimization meets its latency screen.

## Prospective follow-up boundary

The left targets' zero factor hits support a useful future counterfactual experiment: omit their optional factor publication while preserving ordinary reuse, ownership and AUTO routing. Their compact pages still consume 6,291,456 declared page bytes, exactly half this fixture's page charge. The normal-budget compact diagnostic records no promotion refusal, so persistent retry latency does not explain its residual bookkeeping costs.

This observation does **not** establish that a generic exclusion loses no useful reuse. Suppressing optional publication can remain result-correct through normal resolution; a heuristic exclusion would still need broader performance and strategy validation. Zero successful hits can also result from weak misses, invalidated witnesses, missing admissions, or a replay that occurs outside the observed consumption pattern. The ordinary-hit counter here is aggregate. Retained rows, rebinding, aliases, unions, optional-join reordering and other strategies can change nonconsecutive reuse. To prove that no useful nonconsecutive reuse is lost, a structural classification must establish that a canonical target/cache bank's complete coordinate key cannot reappear after the ordinary memo stops covering it, including unbound gaps and restored frames. A monotone member position alone is insufficient when parent keys or generations can recur. The existing accepted-ordinary-hit publication test demonstrates useful reuse that blanket suppression can lose, rather than incorrect values from normal resolution.

A future diagnostic should distinguish per-canonical-target ordinary hits, factor lookups/hits, weak or witness misses, resolver calls and coordinate-key revisits after ordinary-cache displacement. Admissions currently use canonical target indices while hit counts use requested targets, so aliases require explicit aggregation. The shared-tail result also cannot establish the same opportunity in fanout without evidence from that fixture. A fixture-specific publication ablation can estimate the opportunity before designing a general rule.

Constrained-memory retry latency is a different prospective experiment involving pressure duration, failed reservation attempts, resolver fallback and possible contention on the process memory manager. The completed retry probe proves multiplicity and recovery only. No additional publication or pressure experiment is part of this turn's frozen 36-fork study.

## Appendix C: independent raw timing analysis and failed acceptance screen

After the coordinator released the exclusive timing slot, I ran the previously prepared `independent-raw-analysis.py` against `comparison-v1/`, writing the fresh `independent-results/` directory. The script exited zero on its first execution. Its source SHA-256 is `90e02b5021af57cfd147941f9926045d82401695cfa94372d69f62392b206106`. It uses the standard library and independently computes the arithmetic from raw JSONL; it does not import the comparison driver's arithmetic. It reads the driver's final summary only after reconstructing its own results.

The independent receipt is `independent-results/independent-results.json`. It confirms all **36 JVMs, 468 queries, 288 warmups, and 180 measured queries**. Each fork contributes all five measurements. All six comparisons, nine metrics, six within-round ratios per comparison, ratio medians/ranges, route equality flags, and acceptance gates agree with the driver, with no discrepancy.

Raw validation confirms the pinned protocol and production patch, source/descriptor/provenance hashes, actual command classpaths and JVM settings, fixture identity, result count/checksum and shared-tail exact-bag oracle, normal AUTO/native routes, zero ledger endpoints, and root audits. All measured queries use compiled IR. Measured direct-adjacency lookup deltas are 55 for fanout and 10 for shared-tail, with stable ACTIVE applied/current revisions. The runtime manifests preserve all original dependencies and the identical common prefix; only the permitted candidate production entry differs. This independent pass verifies descriptor, source, patch, and command bindings. Full binary-tree preservation additionally relies on the comparison driver's successful before/after artifact check, explicitly recorded in the receipt.

### All six primary latency ratios

Each entry below is compact DENSE divided by current DENSE, using that round's median of all five measurements in each fork. A value below one favors compact. Rounds are zero-indexed; no pair is excluded.

| Round | Fanout | Shared-tail |
|---|---:|---:|
| 0 | 1.058069513 | 1.000806630 |
| 1 | 0.939835950 | 0.930965949 |
| 2 | 1.115602800 | 1.054779955 |
| 3 | 1.028423540 | 0.999372265 |
| 4 | 0.967310312 | 1.034252750 |
| 5 | 0.872255874 | 1.024351868 |
| **Median paired ratio** | **0.997866926** | **1.012579249** |

The predeclared engineering screen **fails**. Fanout's round-2 ratio exceeds the permitted 1.10 ceiling, and shared-tail's median exceeds the required maximum of 1.00. The other four gates pass:

| Prespecified gate | Result |
|---|---|
| Shared-tail ledger ratio at most 0.25 in every pair | Pass; all six are 0.048063149 |
| Shared-tail allocation ratio below 1 in every pair | Pass; range 0.968418209–0.968474003 |
| Fanout median latency ratio at most 1 | Pass; 0.997866926 |
| Fanout every latency ratio at most 1.10 | **Fail; maximum 1.115602800** |
| Shared-tail median latency ratio at most 1 | **Fail; 1.012579249** |
| Shared-tail every latency ratio at most 1.10 | Pass; maximum 1.054779955 |

Neither fixture has all six latency ratios below one. The failure is a selection decision under the frozen engineering screen, not a statistical proof that the candidate is intrinsically slower. The favorable memory result does not change that screen after observing data.

### Memory, traffic and preparation reconciliation

| Observation per measured query | Fanout current DENSE | Fanout compact DENSE | Shared-tail current DENSE | Shared-tail compact DENSE |
|---|---:|---:|---:|---:|
| Ledger peak bytes | 2,264,968 | 2,233,224 | 268,426,276 | 12,901,412 |
| Resolver calls | 1,147 | 1,147 | 65,345 | 32,833 |
| Queries contributing to each arm | 30 | 30 | 30 | 30 |
| Total measured resolver calls | 34,410 | 34,410 | 1,960,350 | 984,990 |

Shared-tail's peak reduction is 255,524,864 bytes, or 95.193685%, in every measured query. Its median paired allocation ratio is 0.968444821, a 3.155518% reduction. The additional 32,512 factor hits in the separate diagnostic exactly match the 32,512 fewer resolver calls in every measured query here. This is improved admission plus representation savings; it does not establish faster individual lookup or promotion.

Fanout's peak falls by only 31,744 bytes, or 1.401521%, with no resolver change. Its median paired allocation ratio is 1.000009614, effectively unchanged at the precision relevant to this experiment. The shared-tail cap-pressure explanation cannot account for fanout's timing. All recorded strategy/tier counter differences between the two DENSE arms are zero; shared-tail's resolver-call reduction is the sole nonzero recorded traffic-counter difference.

Median paired preparation-time ratios are 1.078905127 for fanout and 1.171932763 for shared-tail. These small preparation intervals are reported separately; their ratio is not substituted for total latency, and the phase breakdown does not identify a causal instruction-level bottleneck.

The ordinary-reuse controls remain informative. Current DENSE/current OFF median total-latency ratios are 1.307340832 for fanout and 1.128405055 for shared-tail; compact DENSE/current OFF ratios are 1.304235210 and 1.136281670 respectively. Current OFF resolves 2,770,101 values per fanout query and 2,113,601 per shared-tail query, totaling 83,103,030 and 63,408,030 calls across each arm's 30 measurements. Both coordinate variants reduce resolver traffic and allocation on these fixtures, while neither closes the observed total-latency gap to ordinary reuse. OFF here retains ordinary last-value reuse and does not disable the repaired ownership implementation.

### Selection and limits

The compact candidate should remain experimental under the agreed criterion. Its complete source, tests, exact patch, passing 108-test gate, frozen runtime, diagnostic outputs, and raw timing data are evidence worth preserving. A restored production selection needs its own source-matched validation; that subsequent result is not assumed here.

These timings cover two fixed full-consumption fixtures and compiled steady-state IR after eight warmups. Early close, retained-row and cancellation behavior belong to the separate correctness gate; a broad repetition/width/unique-value/consumption/memory performance sweep has not been completed here. Ledger peaks measure the allocation authority, not heap or RSS. Adjacency hits cover preparation plus execution, and the uninstrumented timing harness does not expose an explicit owner-close counter. The engineering result supports another targeted CPU-cost experiment if work continues, but it does not justify an additional unplanned run or a generic publication-exclusion policy in this turn.

## Appendix D: selected-source restoration and independent final gate

Following the failed acceptance screen, the coordinator preserved the complete tested candidate production and test source, its patch, and its source map under `experimental-candidate/`, then restored the original materializer production file. I removed exactly two compact-only test methods from the selected test source: `sparseFactorPagesReuseWithinCompactPageBudget` and `refusedFactorPagePromotionPreservesCellsAndRetriesAfterMemoryRelease`. They require the new compact representation and remain present in the complete experimental test source. The exact before/after files, removal patch and receipt are in `selected-test-reconciliation/`.

The selected source retains all three new representation-independent characterizations and the strengthened pre-existing no-reclamation case. These four method bodies are byte-identical to their versions in the tested candidate. No assertion was relaxed to accommodate a failure in the selected implementation.

The coordinator's fresh `validation/selected-original/` gate passed copyright, formatting, the module/dependency build, and the eight-class test selection. Independent parsing of its raw XML confirms **106 tests, zero failures, zero errors, and zero skips**. The materializer class has 40 cases; all other class counts remain as in Appendix A. A testcase-identity multiset comparison proves that all 103 prior identities remain and that exactly the three neutral methods were added. Compared with the passing candidate's 108 identities, the selected set removes exactly the two compact-only methods named above.

The independent audit reads the actual current files in addition to recorded maps. All **2,055 module source/resource entries** equal both the selected gate's before and after maps. Relative to this phase's baseline, the only changed recorded source is `NativeResultMaterializerTest.java`. The selected materializer SHA-256 is `cb0749719313b85c8cd134b6c03085f712915188f0e53333cbd46e19e2f0f96e`, the exact original preimage. The frame source remains `e079c212761f295e5c37566b6c71888d5b7240b1ac0891fdad521ec423aee4c4`. The selected test SHA-256 is `68eceb14ad6010d3abc34fa2456cf38b82246ebccea763d66f2415c1995f9a6a`.

An independent file-by-file comparison also finds all **2,778 production `.class` files byte-identical to the original frozen runtime**. The non-class production resources have no delta in this build either. Thus the failed candidate is preserved experimentally and is absent from the selected production source and bytecode. This is a stronger direct check than merely observing that the source file was copied back.

Both conditional weak-reference tests explicitly report `referent-collected=true` in the raw candidate and selected XML, so their GC-dependent miss branches were observed in these executions. Their source remains conditional on collection in a future execution; that is why the deterministic mutation/explicit-clear cases remain useful complementary coverage.

The complete receipt is `independent-selected-source-reconciliation.json`. This independent audit ran no Java, build, test or new experiment. It confirms that the selected state has original production, original frame behavior, the three additional neutral characterizations and the stronger existing refusal characterization, while preserving all 108-test candidate evidence separately.

## Final report and packaging review

I checked `report-synthesis.md` and `report-methods.md` against the raw-derived receipt and preserved source/runtime evidence. The paired estimates and ranges, all-six DENSE/OFF slowdowns, descriptive median tables, phase timings, cold first-query values, and resolver/ledger counts reconcile. The lower allocation in the second matched fanout round occurs in all five measurements in each DENSE arm and remains included; its cause is not established. Cold first-query timing must be described as query initialization after fixture/store setup, excluding JVM launch and fixture construction.

The phase-specific packaging helpers were reviewed as source, without executing packaging operations. Their exact-path delta verification uses private Git indexes, the runtime catalog binds seven compositions and 39 launch commands, and retry-probe relocation handles its positional output/source schema correctly. All publication destinations are new. Large page JSONL records are included, and the thin diagnostic overlays remain separate from complete raw compiler outputs. The strengthened helper provenance binds preserved originals and the previous selection to the prior delivered source manifest. These source checks are prospective; final archive readback, extraction and relocation receipts must separately establish publication success. Relocated commands are launch plans, not a claim that a future run or analysis has qualified.

The final `experiment-report.md` was read back with SHA-256 `c18d613dbf841c0dff1c207c27c1cdccfba0d3bd7e10c0de6503ff9d8680cf1b`. Its cold-timing paragraph incorporates the requested correction: the first query includes query initialization and initially interpreted execution after fixture/store setup, while excluding JVM launch and fixture construction. The selected-source audit in Appendix D and this corrected report are ready for packaging. This review and its independent receipts are now closed to further edits unless the coordinator explicitly reopens them.
