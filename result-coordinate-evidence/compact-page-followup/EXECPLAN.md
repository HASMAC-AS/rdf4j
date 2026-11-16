# Test compact factor pages with charged promotion

This living plan follows `.agent/PLANS.md` and repository `AGENTS.md`. It implements the user's request to continue after the namespace/frame experiments. Previous sources, runtime artifacts, raw measurements and deliveries remain immutable. No commit, push, new dependency or global strategy change is part of this task.

## Purpose / Big Picture

The result materializer currently admits five 64-entry arrays for each factor page, with a conservative 16,384-byte reservation. In the preceding shared-tail diagnostic, 16,112 of 16,364 admitted pages held just one occupied reference slot and exhausted nearly the entire 256 MiB query allowance. The next candidate stores a singleton entry directly in the page and allocates dense arrays only for a second distinct occupied position. It must preserve complete results, weak values, mutation witnesses, ownership, cached page identity and optional-cache fallback.

The observable goal is to retain nonconsecutive repeated values under a small query budget, preserve correct behavior across promotion/refusal/retirement, and measure whether the changed representation improves the sparse workload without an unacceptable latency cost on the existing fanout workload. Ledger savings are accounting observations; actual allocation and latency are measured separately.

## Progress

- [x] Preserve control and run initial build.
- [x] Capture sparse-budget failing test evidence.
- [x] Implement and review charged page promotion.
- [x] Verify both tiers and exact accounting.
- [x] Freeze and run balanced fixture comparisons.
- [x] Audit results and select final source.
- [in_progress] Package complete source and reproducible evidence.

## Context and Orientation

The repository is `/workspace/scratch/37853bcaea36/rdf4j` on `GH-0000-lmdb-lazy-result-coordinates`, checkpoint `15fe7d5011`. Earlier ownership repairs are present as uncommitted changes and must be preserved. This task changes `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/NativeResultMaterializer.java` and its focused test class. The previous frame guard was not promoted: `KernelResultCoordinates.java` must remain at the restored preimage throughout this comparison.

A factor page is keyed by projection target, coordinate namespace, group, parent, tuple column and a long member-position page index. Each current page has 64 positions, with value ID, semantic version, revision ID, revision-owner witness and a weak value reference per position. `FactorMemo.pages` and `recentFactorPage` retain the page object itself, so promotion must retain object identity. The materializer's existing lock protects accesses. Query memory uses `LmdbQueryMemoryManager.QueryLedger` reservations. Optional cache growth must use `tryReserveWithoutReclamation`; `Reservation.tryGrow` can reclaim another owner and is inappropriate here.

The preserved original 127-entry runtime is described by `result-coordinate-evidence/root-qualified-launcher/classpath.txt` and the identity helpers under `result-coordinate-evidence/materializer-followup/`. Its LMDB production tree SHA-256 is `f822ad378729e2ebdd5c7e91d2420e9dbf1eb92aab382e76c35fba00989eab8c`. The latest complete source delivery is recorded in `/workspace/scratch/73b19a6d6f5c/lmdb-next-experiments-deliverables/delivery-manifest.json`. New artifacts belong only under `result-coordinate-evidence/compact-page-followup/` or new delivery directories.

## Surprises & Discoveries

The existing 16 KiB page charge is a conservative accounting allowance, not measured heap or retained decoded Value payload. The three long arrays carry 1,536 payload bytes; two reference arrays carry another 512 or 1,024 bytes before headers. Weak-reference objects are copied from the ordinary memo, not newly created by each factor insertion. A reduction in ledger reservations therefore cannot be reported as an equal reduction in allocated or resident heap.

An existing payload-refusal test encodes the old dense first-page reservation. Its semantic no-reclamation contract must remain intact while its setup is adjusted to fill the remaining allowance independently of page representation. New small-budget reuse tests must fail on unchanged production before the candidate is edited.

The smallest test failed as predicted on unchanged production: `NativeResultMaterializerTest#sparseFactorPagesReuseWithinCompactPageBudget` reported `Tests run: 1, Failures: 1, Errors: 0, Skipped: 0`. Its actual resolver sequence was `[42,43,42]` rather than `[42,43]`, with correct values. Full evidence is under `validation/sparse-budget-red/` and appended to `initial-evidence.txt`. Production SHA-256 was still `cb0749719313b85c8cd134b6c03085f712915188f0e53333cbd46e19e2f0f96e`.

## Decision Log

2026-11-16: Use an inline singleton followed by one-way dense promotion. The measured occupancy distribution is nearly all one-entry or full pages; intermediate packed capacities add states and search work without a demonstrated need. Do not demote pages or probe weak referents merely to make insertion space.

2026-11-16: Keep a conservative 512-byte compact shell reservation and reserve a separate 15,872-byte payload before dense allocation. Keep both claims until page closure, for the original 16,384-byte total dense reservation. The shell remains physically present during promotion, and old weak-reference/witness objects are transferred without cloning. New arrays must be fully allocated and initialized locally before publication under the materializer lock.

2026-11-16: A refused promotion must not set the global missing-page admission latch. It must preserve the singleton and permit unrelated compact admissions. Retry on the next distinct-position insertion so that released global or query memory can enable later promotion. A failed non-reclaiming reservation allocates no Reservation, but takes the manager monitor; the diagnostic will count repeated attempts. Do not add a manager epoch API, permanent refusal latch or retry heuristic without measured need.

2026-11-16: Use a new common benchmark prefix for all three arms. Preserve fanout fixture/harness and shared-tail oracle/fixture bytes. Add query-time adjacency observations and a startup-maintenance wait to the shared-tail harness identically for all arms. Do not pool these fresh measurements with previous series.

## Plan of Work

First preserve exact production/test preimages, all module source hashes, branch/HEAD and the immutable control-runtime identity. Run the mandatory offline root quick clean install through `../mvn`, which supplies JDK 25 and the workspace-local Maven repository. Only after this unchanged-source build finishes should focused tests be edited.

Next add a small-budget A/B/A nonconsecutive replay case to `NativeResultMaterializerTest`. A and B occupy distinct pages, and the budget is sufficient for two compact pages but not one old dense page. The test must show correct values, exactly two resolver calls and zero final ledger use; unchanged production resolves A twice and B once, for three calls total. Preserve fresh Surefire evidence before production edits. Add promotion, complete witness preservation, same-cell replacement, no-reclamation/refusal, retry-after-capacity-recovery and mixed-page retirement/close characterization using public package-local behavior, without reflection or weakening old semantic assertions.

Implement the compact storage only in the existing materializer. Keep lookup keys, aliases, source activation, coordinate validity, ordinary reuse and witness checks unchanged. A same-position write or explicitly cleared singleton reuse must not promote. For a distinct occupied offset, obtain the non-reclaiming payload claim, allocate and populate all five arrays locally, then publish. On allocation failure release the new claim and propagate the error; on reservation refusal retain the old cache entry and let resolution continue normally. Close and retainedBytes must include both reservations. Independently review the implementation before timing.

Run focused materializer tests, then the existing transport/materializer/tier/retention selection expanded only for the new contracts. Capture all exact commands, source maps and fresh reports in uniquely named validation directories. Use the module/dependency quick install before direct test runs; never use `-am` or `-q` for tests. Preserve matching control evidence where the representation-independent tests permit it, and retain the specific red/green evidence for changed memory behavior.

Prepare separate accounting instrumentation outside production and exclude it from timing. Validate occupied representation counts, promotions/refusals, granted/released shell and payload bytes, results, routes and zero final ledger on the sparse workload. Any measured durations from this diagnostic are not performance estimates.

Freeze three timed arms: current OFF, current DENSE, compact DENSE. OFF preserves ordinary reuse and ownership while disabling coordinate transport. Only the candidate LMDB production entry differs between the DENSE arms. On each of the fanout and shared-tail fixtures, run all six arm-order permutations, eight warmups and five measured queries per fresh JVM. Alternate the first fixture by round, keep all samples, and stop on validity/identity/route failure instead of replacing a fork. No builds, profiling or other JVMs run during timed comparisons. Independently recompute results from raw JSONL before selecting source state.

## Concrete Steps

From the repository root, the initial build is:

    ../mvn -o -Dmaven.compiler.showWarnings=false -T 1C -Pquick -DskipFrontend=true clean install

The full Maven output is preserved in the new phase and `maven-build.log`. The benchmark protocol and exact commands will be frozen in this phase before any timed query. The validation runner records the specific test selector and fresh XML evidence. Initial test evidence is appended to the existing top-level `initial-evidence.txt`, preserving prior contents.

## Validation and Acceptance

Correctness requires exact test results, witness preservation through both representations, no unrelated-owner reclamation, and exact release accounting after normal close, early close and namespace retirement. The fanout fixture must return 1,152,000 rows and checksum 3123987218479850048; the shared-tail fixture must pass its exact Cartesian oracle with 1,048,576 rows and checksum 8327963502356915200. Strategy remains AUTO; actual roots, tier, route and adjacency participation must be observed.

The planned measurement design is 36 fresh JVMs, 468 full queries and 180 measured queries. Report per-fixture median and full range of paired JVM-median ratios for latency, all-thread allocation, preparation/phase timing and ledger peaks. The prospective production-promotion screen requires shared-tail ledger peak at most 25% of current DENSE and lower all-thread allocation in every pair, plus median paired total-latency ratio at most 1.0 on both fixtures and no individual latency ratio above 1.10 on either fixture. This is an engineering acceptance screen for a memory optimization, not a statistical noninferiority claim. A consistent latency-win claim separately requires all six latency ratios below one on that fixture. Report every failed gate and do not change tolerances or add runs after seeing the data. The broader consumption/width/repetition sweep remains future work unless needed to resolve a concrete correctness issue.

## Idempotence and Recovery

All stage directories refuse replacement. Do not overwrite old evidence or remove untracked artifacts. Preserve candidate source and runtime before any restoration. If the candidate is not selected, restore only this phase's production preimage and reconcile new tests with the selected implementation; do not disturb earlier ownership repairs. Never rerun or extend a timed protocol merely because its result is unfavorable.

## Artifacts and Notes

Deliver a complete source snapshot, full checkpoint patch, narrow phase delta, experimental source/patch if unpromoted, full report, raw measurements, accounting records, source/runtime identities and relocation instructions. Code backed by Git remains with the project; reports/evidence/runtimes are saved as durable user-facing artifacts. Verify archive contents and patch application before handing off.

## Interfaces and Dependencies

No public API or dependency changes are needed. The existing materializer remains package-private and the factor page remains private. The fixture and independent oracle are preserved. New instrumentation and counters belong only to isolated experimental binaries.

## Outcomes & Retrospective

The candidate passed its 108-case focused correctness gate and both independent accounting diagnostics. The full fixed timing study completed all 36 JVMs and 468 queries, with matching normal AUTO routes, exact result checks, zero ledger endpoints and preserved runtimes. Independent raw recomputation agreed with every ratio and gate.

The compact representation reduced shared-tail ledger peak by 95.1937% and allocated bytes by a median paired 3.1555%, but failed the fixed latency screen: shared-tail median latency ratio was 1.012579249 and the maximum fanout ratio was 1.115602800. The production candidate was not selected. Both DENSE arms remained slower than ordinary-reuse OFF in every paired round on both fixtures.

The complete compact source/tests/runtime remain preserved. Selected production source and all 2,780 compiled production files exactly match the original control. The selected 106-case gate passes and retains all prior 103 test identities plus three new neutral characterizations, along with the strengthened existing no-reclamation case. Only the test file differs from the phase's module-source baseline; prior ownership repairs and frame behavior are preserved.

The report, decision and source verification are complete. Packaging is the sole active item at the source/evidence freeze. Actual archive publication, patch application, extraction/relocation and final completion are recorded subsequently in the additive delivery manifest; this freeze snapshot does not claim those future actions have already succeeded.

Revision note, 2026-11-16: created the plan before production edits, recording the measured motivation, exact control identity, test-first gate, provisional compact layout and bounded comparison design.

Revision note, 2026-11-16: independent review confirmed stable page identity, two non-reclaiming claims and preservation of cleared weak-reference objects. Fixed the refusal-retry policy and memory-oriented promotion screen before implementation or timing. The 10% worst-observed-pair ceiling bounds the acceptance screen; it does not establish a population performance bound.

Revision note, 2026-11-16: the mandatory root build passed, and the active milestone is the smallest in-repository failing test. No production change precedes that evidence.

Revision note, 2026-11-16: the smallest failing test was observed and archived at 06:40 UTC. Its extra resolver call establishes the compact-admission behavior to change. Production implementation and the remaining ownership/accounting characterizations may now proceed.

Revision note, 2026-11-16: the stable-page inline representation and charged dense promotion are implemented. Independent review found no blocking production issue. Five new behavioral cases and the representation-independent no-reclamation update are ready; formatting and the full focused tier/lifetime selection run next. The candidate remains unselected pending all fixed performance gates.

Revision note, 2026-11-16 06:49 UTC: formatting, dependency/module build and the eight-report focused gate passed with108 tests,0 failures,0 errors and0 skips. The candidate runtime is frozen; only NativeResultMaterializer and its FactorPage/FactorMemo class bytes differ from the original production tree. The candidate production tree is efbb57205da275bd4032ee0f127eb2028010e6707dde8b4538807f94c5542595. Original frame source remains byte-identical. Independent accounting diagnostics now have the exclusive JVM slot before timing.

Revision note, 2026-11-16 06:53 UTC: both full-fixture diagnostic forks passed exact results, normal AUTO/actual native routes, ledger endpoints and complete shell/payload/directory/memo release attribution. The compact measured owner has16,384 inline+256 dense pages and12,582,912 page bytes, versus16,364 dense pages and268,107,776 page bytes. Peak query ledger is12,901,412 versus268,426,276 bytes; these are accounting observations. Independent raw-page review agrees. The separate constrained probe shows1,024 refused promotions followed by1 successful retry, with zero reclamation callbacks and final ledger. Frozen timing protocol SHA256 is8ef50b935b83cd22c525616438949f730042ed09c179eff332e39c5601f9b374; all36 uninstrumented forks are the next active milestone.

Revision note, 2026-11-16: all36 timing forks/468queries passed result/route/identity gates, with180 measured queries. Independent raw recomputation agrees with every ratio and gate. The engineering screen fails: fanout maximum compact/current latency ratio1.1156027995 exceeds1.10 and shared-tail median1.0125792493 exceeds1.00. Both shared-tail memory gates pass. Complete candidate source/tests/runtime are preserved under experimental-candidate/ and runtimes/. This phase production source is restored to its exact preimage; only the three representation-independent new characterizations and strengthened no-reclamation test will remain. A matching selected-source gate runs before packaging.

Revision note, 2026-11-16: selected-original formatting/module build and106-case gate passed with no failures/errors/skips. Verification confirms all2055 module source files match that gate, only the intended test source differs from the phase baseline, and all2780 production files equal original frozen tree f822ad…8c. The compact candidate remains preserved and unselected. The complete report and final selection receipts are ready for independent review and additive delivery packaging.

Freeze note, 2026-11-16: all code, numerical, diagnostic and independent-review writers are closed. Report SHA256 is c18d613dbf841c0dff1c207c27c1cdccfba0d3bd7e10c0de6503ff9d8680cf1b. The immutable archive selection captures packaging in progress; the later delivery-manifest.json is the authoritative completion receipt.
