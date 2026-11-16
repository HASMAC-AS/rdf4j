# Measure publication omission under ordinary weak-reference collection

This living ExecPlan follows `.agent/PLANS.md`. Maintain Progress, Surprises & Discoveries, Decision Log, and Outcomes & Retrospective as work proceeds. This is an experimental overlay and measurement task; preserve the selected production source and all earlier evidence. The user explicitly authorized committing and pushing checkpoints to HASMAC during this work. Root owns those mutations.

## Purpose / Big Picture


The user needs to know whether omitting factor-cache publication for selected bindings improves total query latency, beyond the demonstrated reduction in memory-ledger charges. A factor cache stores weak references indexed by provenance coordinates; ordinary reuse remembers recent values without those coordinates. The previous timing comparison stopped because its exact resolver-count gate treated one legitimate weak-reference fallback as disqualifying. A separate observer subsequently reproduced such a fallback, but cannot identify the cause of the earlier event. This phase measures total query cost under normal garbage collection, including any treatment-induced changes in weak availability. It does not subtract estimated resolver cost or pool the earlier incomplete run.

Success means a new, fully specified, serial 36-JVM comparison with identical ownership code and input settings across ordinary reuse (OFF), coordinate reuse with all publications (DENSE all), and coordinate reuse with selected publications omitted (DENSE omit). Both data fixtures use normal AUTO selection, original dense pages, and fresh contemporaneous controls. Results, routes, compiled measured execution, adjacency identity, and ledger cleanup must qualify before latency conclusions are drawn.

## Progress


- [done] Recover all 19,331 selected source entries and exact frozen runtimes.
- [done] Attempt mandatory root builds; compile LMDB and verify all 2,778 production classes exactly.
- [done] Record deliberate red accounting contracts: 15 tests, 14 intended assertion failures, zero errors or skips.
- [done] Pass the fresh focused106 and publication6 baseline; implement and independently review rare accounting.
- [todo] Qualify rare accounting and the publication runtime after the newly requested direct-memo experiment.
- [todo] Freeze overlay and new comparison protocol.
- [todo] Run serial balanced comparison and audit.
- [todo] Deliver source, patches, results, and limitations.

## Surprises & Discoveries


The original checkout was cleared between turns. The conversation workspace also began empty. The exact checkpoint commit is recoverable through the configured Git remote and surviving object stores; an earlier complete source archive and the latest frozen runtime, evidence, verification, and source manifest were recovered from saved artifacts. The new checkout is restored at the original path so frozen classpath descriptors can remain byte-for-byte unchanged. Later source is accepted only when its size and SHA-256 match the last delivered source manifest. The recreated Git index has a new restoration baseline and must not be claimed to be the previous index bytes.

The existing materializer counts ordinary and factor weak misses together. An ordinary weak miss can recover through a factor hit, or both tiers can miss for a single resolution. Adding those two counters would double count some fallbacks. For this single-threaded native harness, classification will happen only in the existing ordinary-null branch, under the materializer monitor, using the result and a per-instance terminal-factor-weak counter around its one factor lookup. No new reference dereference or retained Value is needed.

## Decision Log


2026-11-16: Restore the exact selected snapshot before building. New main or an approximate ownership migration would confound the comparison. Keep restoration attempts and original failed timing evidence intact.

2026-11-16: Use rare-path accounting only in the timing overlay. Count ordinary weak outcomes, factor terminal weak outcomes, witness rejection, abandon/repair, refusals, and lifecycle completeness. Avoid unconditional hit, remember, initial/page/cell, or per-target counters in the measured paths. The heavy prior observer remains separate diagnostic evidence.

2026-11-16: The primary estimand is the total effect of publication omission under normal GC. Exact resolver equality is not a qualification requirement. The weak partition must reconcile exactly, joint weak outcomes cannot exceed factor weak outcomes, and the terminal weak union cannot exceed raw resolutions for these fully bound, unaliased fixtures. A resolver-minus-weak residual is descriptive only. No post-hoc time correction, sample removal, replacement, or extension is allowed.

## Context and Orientation


Repository root is `/workspace/scratch/37853bcaea36/rdf4j`, branch `GH-0000-lmdb-lazy-result-coordinates`, checkpoint `15fe7d5011d27d31d76436238363ee3f34b3ba50`. The selected prior source manifest identifies tree `4db5b366c00c68f5e89c7301612b5a821fc0e5d0`. Source restoration inputs and receipts are under `/workspace/scratch/73b19a6d6f5c/recovery` and `checkpoint-recovery-v1`. Existing experimental sources and drivers live under `result-coordinate-evidence/publication-followup`; shared frozen harness/runtime inputs live under `result-coordinate-evidence/compact-page-followup` and earlier evidence directories.

The production `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/NativeResultMaterializer.java` remains unchanged. Its publication overlay from the preceding phase adds a plan-final omitted-target mask and returns early in `rememberFactor` for those targets. The new overlay will start from those exact experimental bytes and add primitive accounting. `NativeProjectedBindingSet.captureForOutput` remains byte-identical; its existing calls to materializer `abandon` can expose scope repair or failure rollback. The benchmark fixture has no callback reentrancy into its materializer; do not generalize per-instance before/after classification to arbitrary reentrant implementations.

## Milestones and Plan of Work


First recover all selected source and frozen runtime files with hash receipts, then run the mandatory offline root quick clean install using JDK25.0.5 and a workspace-local Maven repository. Preserve the initial log and failed attempts. Compare fresh production class bytes to the frozen composition; retain original frozen resources for experiments. Qualify the existing focused materializer/publication tests before instrumentation.

Next add in-repository experimental tests for five distinct paths: an ordinary weak miss recovered by a factor hit, joint ordinary and factor weak failure, an ordinary weak miss with a nonweak factor failure, factor weak failure alone, and witness/scope repair kept distinct. Capture the failing counter-contract reports before implementing accounting. Verify lifecycle export and duplicate close. The experimental adapter must preallocate primitive records before the unchanged launcher runs and write accounting JSON only after the harness completes. Export counts at idempotent materializer close, without holding Values, sources, rows, or page snapshots. Existing harness timing/allocation windows and class bytes should remain unchanged.

Then freeze a new protocol, executable runner, runtime descriptor, source hashes, fixtures, and watched inputs before observing timing. Use 36 JVMs: three arms, two fixtures, all six permutations of arm order; eight warmups plus five measurements per JVM. Use the original JVM settings (1GiB G1 heap and global memory, 512MiB query memory), original dense page representation, normal AUTO, PREFER native/adjacency, and the original fixture sizes. Fanout omits measurement and reading; shared-tail omits l0 and l1. Preserve OFF as the ordinary-reuse reference. Run one Java process at a time, including tests and builds.

Finally independently parse raw output and verify identities, accounting, result checksums/oracles, routes, tiers, adjacency, and ledger endpoints. Analyze process-level medians and paired ratios under the fixed schedule; individual query samples are not independent JVM replicates. Report preparation, execution latency, allocation, ledger peaks, raw resolutions, weak outcomes, and actual routes. Treat the earlier nine-JVM prefix as failed/incomplete evidence only. Package complete changed source, an incremental overlay diff, a phase diff, a full checkpoint diff, raw evidence, protocol, and runnable frozen inputs with verified manifests.

## Concrete Steps


All implementation and run commands are recorded in phase receipts as they execute. The root prerequisite is the repository's command, run with the recovered JDK and Maven binaries on PATH and `.m2_repo` populated from verified cached dependencies:

    mvn -B -ntp -Dmaven.compiler.showWarnings=false -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install

Run focused tests without `-am` or `-q`. Preserve full logs and Surefire summaries below this phase directory, and append first evidence to root `initial-evidence.txt`. Do not run a broad formatter over archived XML reports; format new Java explicitly and use a read-only module Spotless check. The experimental source copies and frozen classpath compose the test overlay; root selected production remains unchanged.

## Validation and Acceptance


Require complete restored source hash reconciliation, passing baseline and post-overlay focused tests, deliberate pre-overlay counter-contract failures, byte-verified common runtime inputs, all scheduled JVMs completed exactly once, and an independent raw-data audit. Every query must meet the fixture oracle/checksum and zero final query/global ledger usage. Measured iterations must use the compiled tier. Unexpected witness rejections, abandon/repair, or refusals at 512MiB invalidate timing qualification. Reject missing or repeated materializer export and malformed weak partitions. Weak misses alone are valid outcomes and are never used to select favorable samples.

## Idempotence and Recovery


All new attempts use new versioned directories. Do not overwrite earlier protocols, runtime descriptors, logs, failed runs, or user changes. Restoring files is allowed only from exact archived bytes or matching Git blobs. Preserve unrelated surviving checkout HEAD, index, and refs. Use a separate Git index for historical snapshot/patch generation. Before each authorized checkpoint commit, stage only the explicitly selected source, protocols, receipts, and results; exclude generated runtime trees and dependencies. If a timing gate fails, preserve the full attempted prefix and analyze it as incomplete rather than silently rerunning samples.

## Interfaces and Dependencies


Use the existing Java25 toolchain and Maven dependencies only. Primitive counters must not change result ownership or retain weak referents. Aggregate or export only once per materializer close; allocate reporting storage before the unchanged harness timing starts. The wrapper and instrumentation are experimental artifacts, not public APIs or production defaults. The independent audit must derive its conclusions from raw sample and accounting files rather than trusting the runner's summary.

## Outcomes & Retrospective


Work in progress. No new benchmark JVM has run in this phase. Exact selected source recovery is complete. Both root build attempts reached the LMDB module successfully; the full reactor is blocked on uncached Kotlin 2.3.21 and Maven Central DNS resolution in the Spring modules. All 2,778 freshly built LMDB production classes match the frozen benchmark classes byte for byte. The deliberately failing accounting tests established the new observer contracts before implementation. The timing protocol and new accounting runtime are not yet frozen.

Revision2026-11-16: Created this plan after discovering the cleared workspace, preserving the user's requested experiment while explicitly separating restoration, accounting qualification, and a fresh timing study.

Revision 2026-11-16: All production, harness, and 129 frozen classpath entries matched before the mandatory build. Three historical test fixtures initially remained unresolved; subsequent reconstruction recovered every file exactly, including the final constant-folded Join fixture. The complete selected source manifest now verifies without omissions.

Revision 2026-11-16: Preserved the offline root dependency failure and online DNS failure, then accepted the 15-case accounting red receipt. The user authorized checkpoint commits and pushes to HASMAC; the restored selected tree will be checkpointed before the new runtime is qualified.

Revision 2026-11-16: The user asked to try a direct-mapped iteration-wide ID/full-mask memo. Continued work now follows `../direct-memo-followup/EXECPLAN.md`. This publication phase is preserved before green/lifecycle/timing execution; no new publication benchmark JVM was run. The current source and previous experiments are checkpointed on HASMAC as 97284c5229fc826b156275b15f105aac4dd801d6.
