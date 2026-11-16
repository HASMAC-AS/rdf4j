# Measure the cost of publishing unused factor entries

This is a living ExecPlan maintained under `.agent/PLANS.md`. The work is a controlled investigation with experimental source copies under this evidence directory. The selected production source remains unchanged. The user authorized continuing the LMDB experiments; no commit or push is requested.

## Purpose / Big Picture

The previous study reduced factor-page memory substantially but did not improve query time. Its shared-tail diagnostic showed that two projected targets consumed half of the factor-page charge while supplying no successful factor hits. This study will measure whether omitting publication for such targets removes meaningful CPU cost. A factor entry is a weak value reference indexed by producer coordinates; ordinary reuse is the separate last-value memo. A publication is the attempt to put an accepted value into the coordinate-indexed cache, including on an ordinary memo hit.

The result will be a reproducible counterfactual experiment: the same ownership implementation, coordinate transport, source fixtures and automatic native IR selection, with only a declared target publication mask changing between the two main arms. A third arm disables coordinate transport to provide a fresh ordinary-reuse reference. Target names are fixture-specific experimental inputs, not a new planner policy or a recommended production feature.

## Progress

- [done] Preserve source and establish initial validation.
- [done] Diagnose target traffic and validate masks.
- [done] Freeze balanced protocol and runtime identities.
- [done] Run serial comparison and preserve stop.
- [done] Diagnose resolver variation without timing claims.
- [done] Finalize complete sources and experiment findings for delivery.

## Surprises & Discoveries

The first isolated baseline Surefire run found 106 cases but reported 27 setup errors because its fresh copied POM did not execute the lifecycle step that creates `target/tmp/fork-1`. Both JUnit temporary-directory setup and Mockito agent temporary-file creation failed before their tests. All first reports are preserved under `validation/baseline/`; `harness-diagnosis.json` records the correction. The validation runner now creates that isolated directory explicitly before rerunning the same selection in a fresh stage. No experimental production copy had yet been compiled.

The fresh root build reproduced every production `.class` byte in the preserved original runtime. Its sole 2,780-file tree difference is the generated `maven-config.properties` resource, which records build configuration and paths. Timing continues to use the exact preserved runtime, including its original resource, rather than the fresh build output. Evidence is `initial/production-runtime-reconciliation.json`.

The original shared-tail implementation filled the 256 MiB query ledger: its diagnostic admitted 16,364 dense pages and refused a further page. Removing left-target pages can therefore admit extra right-target pages and reduce resolver calls. That is a memory-admission effect in addition to omitted publication work. The main comparison will give every arm 512 MiB per-query memory, with the same 1 GiB heap and global ledger, and verify zero admission refusals in diagnostics. Separate 256 MiB diagnostics will report the original-budget effect without treating their instrumented timing as a performance estimate.

## Decision Log

Decision: keep the original dense page representation and all selected production source bytes unchanged. Rationale: the compact page candidate failed its fixed latency gates and is not the selected implementation. This phase must not silently promote it. Date: 2026-11-16.

Decision: implement the omission only in a copied experimental `NativeResultMaterializer` source, as an early return in `rememberFactor` keyed by the canonical target. Rationale: aliases must share a publication decision; ordinary memo witnesses, factor lookup, transport, invalidation and close must remain intact. A cache miss still follows ordinary resolution. Date: 2026-11-16.

Decision: use three arms built from identical experimental classes: coordinate OFF with all publication permitted, DENSE with all publication permitted, and DENSE with diagnosed canonical targets omitted. Rationale: the primary comparison holds the mask-check implementation constant, while OFF supplies a matched reference. Any setup and branch overhead introduced by the experimental mask is part of all three arms; no claim will equate this overlay's absolute timing to the unmodified class. Date: 2026-11-16.

Decision: use six permutations of the three arms for each of the two fixtures, alternating which fixture runs first, with eight warmup and five measured queries per fresh JVM. Rationale: this preserves the previous study's within-round paired analysis and balances order without adaptive sampling. Gates and target masks will be frozen before the first timed JVM. Date: 2026-11-16.

## Outcomes & Retrospective

The required root clean install passed in 3:51. The corrected isolated baseline passed 106 cases in eight reports with no failures, errors or skips. The six new experimental contracts were compiled against the original API and run before generating or compiling the publication overlay: five failed assertions and one passed, with no errors or skips. This demonstrates that the tests distinguish the requested omission behavior from the unchanged original. Reports and commands are preserved in `validation/baseline-v2/` and `validation/contracts-before/`.

The experimental copy then passed the same 106 cases and all six new contracts. `runtimes/publication-overlay-v1/runtime-descriptor.json` is frozen at SHA-256 `c46fe65a3515872535b55d0c979bf5f84e4d90b59168c86dd3bc14febaa244b2`. Its two-class prefix replaces only `NativeResultMaterializer` and `NativeResultMaterializerPlan`; the other 2,776 original class files, including both nested page/memo classes, remain byte-identical. The complete copied experimental source has SHA-256 `25a321a3f9cb590492dc9d4efe0a2022ae4f731b6d90db38c5137a4ee1c21fb1`.

The original 512 MiB diagnostics selected exactly `measurement,reading` for fanout and `l0,l1` for shared-tail under the predeclared rule. Independent raw reconciliation covered all eight queries. The fanout candidates issue 2,304,000 publication calls and own 102 pages per query; the shared-tail candidates issue 2,097,152 calls and own 8,320 pages. Neither bank group supplies a factor hit or incurs weak/witness misses. Same-overlay masked 512 MiB diagnostics have preserved resolver counts (1,147 fanout; 32,833 shared-tail) and useful factor hits. The separate original-budget diagnostic findings are recorded below.

All eight diagnostic JVMs and 32 queries now pass the independent result, route, counter and cleanup checks. At 512 MiB the mask preserves resolver/hit counts while reducing the fanout ledger peak from 2,264,968 to 592,008 bytes and shared-tail from 272,948,260 to 136,502,308 bytes. At 256 MiB shared-tail omission admits 130 additional right-side pages, gains 32,512 factor hits and reduces resolver calls from 65,345 to 32,833. These diagnostic runs are not timing evidence. Full raw reconciliation is in `benchmark/independent-publication-controls.json` and the root-reviewed mask selection is in `selection.json`.

The timing protocol is frozen at `protocol.json`, SHA-256 `2a94e81f10d63ec177cb998d3270fb9cb48adee569ca1808370f6fc81465107b`. All 2,055 module source/resource files remain unchanged from phase entry; module Spotless check passed. The timing protocol stopped at its exact-counter qualification gate after nine successful JVM processes, 117 queries and 45 measured samples. Shared-tail round 1 (zero-based), second warmup iteration -7, has 32,834 resolver calls under omission versus 32,833 under all-target publication. All other recorded counters, routes, tiers, adjacency observations, results and ledger endpoints match. The 15 completed measured DENSE query pairs have exact resolver equality. The independent audit rehashed all 129 runtime entries and 99 watched files unchanged. No accepted latency/allocation ratios or mechanism-screen decision is produced from the incomplete run.

The source-matched diagnostic memory and operation-count findings remain valid. The extra resolver could be a weak-reference fallback, but the timing runtime does not record the cause, so that attribution is unproven. A separate prospective four-JVM shared-tail diagnostic will investigate counter variation with the existing per-target observer and normal GC/reference logging. It uses fixed order all, omit, omit, all; eight warmups plus five observed queries per JVM; identical 512 MiB query allowance and 1 GiB G1 heap; no forced collection, retention or heap changes; and no timing/allocation ranking. Its 52 queries are diagnostic evidence, not replacement forks for the stopped performance protocol. No further queries are added if no miss is observed.

The separate probe is now frozen at `diagnostic/resolver-variation/probe-v1/protocol.json`, SHA-256 `0ba7dd6c7bce031e8c7495e6be958c13444a670ada1b966dde2fdfcb1f54ac5d`. Root and independent static review approved its unchanged 130-entry diagnostic runtime, four exact commands, result/route/cleanup checks, and exclusive final-factor-miss partition. Ordinary weak events remain separate because they may overlap final factor misses or be recovered by a factor hit. GC logging has no per-query alignment; instrumentation perturbs collection, so neither production miss rates nor the original timing event's cause can be inferred from a new observation. Root granted the exclusive Java slot after checking the frozen protocol hash. No additional timing study is scheduled.

The fixed probe completed all four JVMs and 52 queries. Each omission JVM recorded one additional resolver at warmup -7, exactly reconciled to one `r1` terminal factor weak miss, one fewer factor hit, and one extra remember/publication/coordinate operation. Every other target counter and all other 50 queries match the same-arm prior references. There are no ordinary weak/witness events, factor witness events, or page refusals. All result, route, ledger and accounting checks pass, with four interpreted and 48 compiled binds. The independent checker reread all 52 raw pairs plus eight prior raw references and verified all 130 runtime entries and 113 pinned inputs unchanged. The new observed mechanism supports weak fallback as an explanation of the old event without proving its cause retrospectively. No duration, allocation, GC-cost or natural-miss-rate estimate is accepted from the probe.

Final source verification confirms all 2,055 module source/resource entries, selected materializer/test bytes, branch, HEAD and real index are unchanged. `final-report.md` and `decision.json` retain the original dense-page production selection and explain the failed timing study, confirmed admission/publication benefits, and bounded weak-fallback observation. This closes the source-side experiment plan. Subsequent delivery operations follow `delivery/packaging-plan.md`; archive readback, exact-tree patch checks, relocated-command verification and final artifact hashes are recorded beside the delivery archives after this source snapshot is frozen. They execute no Java and may not alter the frozen experiment findings.

This phase will not select a general production target filter from two fixed fixtures. Whether the publication work improves latency remains unresolved under the stopped fixed performance protocol.

## Context and Orientation

The repository is `/workspace/scratch/37853bcaea36/rdf4j`, on `GH-0000-lmdb-lazy-result-coordinates` at commit `15fe7d5011d27d31d76436238363ee3f34b3ba50`. It contains substantial pre-existing ownership repairs and test changes; do not reset or discard them. The selected materializer is `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/NativeResultMaterializer.java`, SHA-256 `cb0749719313b85c8cd134b6c03085f712915188f0e53333cbd46e19e2f0f96e`. The selected materializer tests have SHA-256 `68eceb14ad6010d3abc34fa2456cf38b82246ebccea763d66f2415c1995f9a6a`.

The previous common harness is described by `result-coordinate-evidence/compact-page-followup/compiled-v1/runtime-descriptor.json`. It includes 128 ordered classpath entries, both exact result oracles, direct adjacency snapshots outside measurement windows, and normal AUTO selection. Its 15-class harness prefix has tree SHA-256 `05c88f61336fce5028bb617e5ff7f7759e4a55ae4c4dba3a3b487c5512f0d825`. The original production entry contains 2,780 files and has tree SHA-256 `f822ad378729e2ebdd5c7e91d2420e9dbf1eb92aab382e76c35fba00989eab8c`.

Fanout projects twelve targets and emits 1,152,000 rows at measurement count 100, readings 10 and two statuses. Shared-tail projects six targets for 64 roots, 128 left members and 128 right members, emitting 1,048,576 rows. Their established full result checksums are respectively `3123987218479850048` and `8327963502356915200`; preserve the independent row/bag oracles as well as checksums. The expected shared-tail omission candidates are `l0,l1`; fanout requires new per-target diagnostic evidence before choosing its mask.

## Plan of Work

First preserve the branch/index identity, module source hashes, selected source copies and prior delivery receipt. Run the required offline root quick clean install and archive its complete output. Capture a fresh baseline focused gate with the existing eight-report selection (106 cases expected) before compiling experimental overlays.

Next generate diagnostic-only materializer copies that record ordinary and factor hits and misses, publications and skips, page admissions/refusals, canonical/requested target mapping, and ledger release. Run the two fixtures at 512 MiB per-query memory before selecting target masks. Preserve all diagnostic attempts and separate their times from performance claims. Generate the noninstrumented publication overlay independently and add contract tests for omission, canonical aliases, enabled ordinary-hit publication, fallback and cleanup. Production source is never edited for this spike. Run the baseline focused tests against publish-all, and semantic contract tests for masked behavior; retain their exact classpath and source identities.

Then freeze a protocol and runtime descriptor. Run all performance JVMs serially, with no build, profiling, hashing or compression overlapping them. Retain every sample. Compute the median of five measurements per JVM, pair variants within each fixture/round, and report the median and full range of six ratios. Do not pool fixtures or earlier studies. Record latency, preparation time, allocation, ledger peaks, resolver counts and actual execution routes alongside result checks.

Finally independently reconcile the raw records, report the engineering decision and its limits, verify selected source has not changed, and deliver all new source copies, patches, scripts, raw evidence and runtime provenance. Preserve earlier phase artifacts unchanged. Code is already backed by this Git repository; persist report/evidence deliverables through the available Library workflow.

## Concrete Steps

All repository commands run from `/workspace/scratch/37853bcaea36/rdf4j`. The `../mvn` wrapper selects Java 25.0.5+8 and appends the workspace-local Maven repository. Explicitly retain `-Dmaven.repo.local=.m2_repo` in new command records.

    ../mvn -o -Dmaven.repo.local=.m2_repo -Dmaven.compiler.showWarnings=false -T 1C -Pquick -DskipFrontend=true clean install

Use a Python runner with no execution timeout to send complete install output to this phase's `initial/maven-build.log`, and mirror the latest install log to repository-root `maven-build.log`. Tool polling may yield early but must not terminate the build. No `-am` or `-q` is permitted for test execution. Install with skipped tests may use `-am`.

    ../mvn -o -Dmaven.repo.local=.m2_repo -Pquick -DskipTests=false -pl core/sail/lmdb -Dtest=NativeResultMaterializerTest,LmdbNativeProjectionValueReuseTest,LmdbNativeIrFactorCoordinateTransportTest,LmdbNativeIrOptionalFanoutCoordinateTest,LmdbNativePackedFtreeTest#delayedPackedRowsFallBackAfterAdvanceAndIterationClose,KernelResultCoordinatesTest,KernelResultCoordinateRepresentationsTest,KernelResultRowSelectionsTest -DforkCount=1 -Drdf4j.lmdb.test.maxHeap=4G -Dsurefire.runOrder=alphabetical surefire:test

Archive fresh Surefire XML/text reports by their modification time. Append compact evidence to repository-root `initial-evidence.txt`; never overwrite earlier evidence. The existing tests directly invoke `find` and `remember` and exercise ordinary-hit publication and coordinate fallback. New standalone overlay tests and their launch commands will be recorded under `contracts/` before execution.

## Validation and Acceptance

The baseline focused selection must pass all 106 existing cases with zero failures, errors and skips. Publish-all overlay must preserve that behavior and exact result/route observations. Omitted targets must have zero publications after the early guard, while enabled targets retain ordinary-hit publication. At sufficient memory, resolver calls and result oracles must match between the DENSE arms; diagnostic ledger endpoints must be zero. The 256 MiB diagnostic may change resolver counts through newly admitted right-side entries and must identify that explicitly.

The prospective main performance signal is a median selective/all latency ratio at most 0.95 for each fixture, with each of the six paired ratios below 1.0. Allocation median must not increase and no paired allocation ratio may exceed 1.02. These are engineering gates for deciding whether to pursue a generic policy, not significance tests or production promotion criteria. The final immutable protocol will repeat these gates and record any adjustment made before timing. Preparation and ledger peaks are reported separately; no unplanned pressure or retained-row timing claims are permitted.

## Idempotence and Recovery

Every run uses a fresh output directory; refuse reuse. Never overwrite a completed protocol, runtime descriptor, raw log or report from a previous phase. If a run fails, retain its raw files, classify the failure, and decide whether the fixed protocol permits continuation without replacing evidence. No extra timing rounds may be added in response to observed results. Experimental masks must reject invalid names rather than silently omit nothing. Restoration, if ever needed, is restricted to explicitly recorded phase preimages and must preserve all user work.

## Artifacts and Notes

This phase lives only under `result-coordinate-evidence/publication-followup/` until delivery packaging. `contracts/`, `diagnostic/` and `benchmark/` are assigned to separate collaborators. Root owns this plan, initial source/build preservation, validation scheduling, final review and delivery. Only root schedules JVM work, to prevent overlapping experiments.

## Interfaces and Dependencies

No new dependencies or public configuration are added. The experimental system property is `rdf4j.lmdb.resultCoordinates.experiment.omitFactorTargets`, containing comma-separated canonical projected target names. Parse it outside the hot publication path, validate names against the projection, and store the omission choice by canonical target. The only hot-path alteration is the early guard in `NativeResultMaterializer.rememberFactor`. Mask lookup must use `plan.aliases[target]`. Keep all weak-reference dereference, lifetime, cleanup and coordinate-transport behavior otherwise unchanged.

Revision note (2026-11-16): created the phase plan before the initial build and overlay construction. Added the sufficient-memory main comparison because both independent source reviews identified the original budget cap as a confound.

Revision note (2026-11-16): preserved and diagnosed the first isolated validation setup failure; created the missing fork directory in the harness without changing the tested code or selection. Recorded the sole generated-resource difference after the root build.

Revision note (2026-11-16): completed baseline and expected red semantic evidence before experimental compilation. Activated diagnostic discovery; fanout targets must have admitted pages and completed publications but zero factor hits, weak misses and witness misses in each of four diagnostic queries, aggregated by canonical target. Zero-page targets are excluded from this hypothesis.

Revision note (2026-11-16): recorded the 112-case passing overlay and frozen two-class runtime. Qualified the diagnostic driver source/runtime ABI before masked execution and added independent global traffic equations. Recorded discovery masks and sufficient-memory counterfactual validation before timing.

Revision note (2026-11-16): completed all diagnostic qualification, independent raw reconciliation, read-only module formatting and source preservation checks. Froze the 36-JVM protocol without changing its predeclared gates, sample count or masks. Activated the serial timing interval; all collaborators are restricted to lightweight additive documentation or script preparation until it ends.

Revision note (2026-11-16): the timing driver enforced its predeclared stop after one warmup resolver mismatch. Preserved all nine forks and the unchanged protocol; did not replace samples or relax gates. Activated a distinct bounded diagnostic for the newly exposed counter-stability question. The fanout fixture's historical-artifact metadata was also clarified: this is a preserved reconstruction, not an available historical Stage-2 binary.
