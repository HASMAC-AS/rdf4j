# Test an iteration-wide direct-mapped lazy Value memo

This living plan follows `.agent/PLANS.md`. It continues the result reuse investigation after the user's 2026-11-16 instruction to try a direct-mapped memo keyed by value ID and full position mask. The user authorized checkpoint commits and pushes to HASMAC. Production source and previous experimental evidence remain preserved while this phase uses explicit experimental overlays.

## Purpose / Big Picture

Test whether a single table shared across outward columns and rows can preserve the reuse benefit while avoiding factor-coordinate transport and publication overhead. The proposed table starts at 4,096 slots, replaces collisions directly, disables after the first 8,192 probes if fewer than 25% hit, and may double at subsequent 8,192-probe boundaries when cumulative hits are at least 25% and cumulative collisions at least 12.5%, up to 65,536 slots. The table does not explicitly initialize a cold value. Its strong references can retain wrappers and payloads initialized by later consumers.

The first comparison keeps the selected ownership implementation and original AUTO execution routes. Ordinary reuse with transport OFF, coordinate reuse with DENSE transport, and the direct memo with transport OFF use the same compiled experimental prefix. No previous-host absolute timings enter the estimates. The unfinished publication-omission timing study remains separate.

## Progress

- [done] Recover the exact selected source tree and frozen common runtime; preserve them on HASMAC.
- [done] Compile LMDB, verify 2,778 production classes against the frozen runtime, and pass the fresh 106-case focused plus six-case publication baseline.
- [done] Specify and run direct-memo correctness and adaptive-policy contracts before implementation.
- [done] Implement the experimental table and its existing-ownership integration; demonstrate the four-array witness counterexample.
- [done] Verify enabled and disabled behavior, both IR tiers, detachment, cleanup, and lifecycle accounting.
- [in_progress] Complete fresh comparison with finite supervision.
- [todo] Independently audit raw observations, report the retention and cutoff limitations, and checkpoint complete source and evidence.

## Surprises & Discoveries

The restored checkpoint is `97284c5229fc826b156275b15f105aac4dd801d6` on HASMAC, with the exact preserved selected tree `4db5b366c00c68f5e89c7301612b5a821fc0e5d0`. The full root reactor build is blocked in unrelated Spring modules by an uncached Kotlin 2.3.21 dependency and Maven Central DNS failure; the LMDB module compiled successfully and its production classes match the frozen benchmark bytes. The initial focused validation used an older support bundle and ran only 93 passing cases; explicit compilation of the selected current tests restored the prescribed 106 passing cases. All attempts are preserved in `publication-qualified-followup`.

The current outward-row owner is `NativeResultMaterializer`. It already owns source, result-view, and exposure validation, provisional-hit repair, and per-iteration close. `AggContext.value` serves internal evaluation, keys by cache bank rather than full mask, and does not intercept the benchmark's lazy outward values. The table will therefore use the existing result materializer's ownership boundary, preserving the user's per-iteration and cross-column semantics without changing the ownership migration.

`LmdbValue.getSemanticVersion()` alone is not a complete cache witness. `setInternalID` can replace a cold value's dictionary ID or revision without incrementing that version. The strengthened experimental table will retain an additional identity witness and validate the ID, revision number, and revision owner. A separately generated four-array variant will demonstrate the counterexample; it is not the timing candidate.

A cold wrapper can acquire a large decoded payload while its semantic version remains unchanged. The existing lexical estimators either initialize values or omit parts of the object graph. Array reservations and a fixed slot limit therefore remain incomplete bounds for this strong memo. This phase explicitly measures the experiment, keeps it disabled by default, and does not claim production memory safety from the array ledger.

The first primary controller disappeared after seven complete JVMs and during the eighth JVM's warmup. The incomplete eighth directory has four warmup samples and no process-result receipt. The available evidence does not establish the termination cause; it does establish that the cohort is incomplete. All first-attempt files remain preserved under `comparison-v1`, with no accepted comparison estimates. A fresh cohort will use one finite controller invocation per JVM so each terminal exit and durable receipt is checked before the next JVM starts.

## Decision Log

2026-11-16: Preserve publication accounting work, then prioritize the user's newly requested direct-memo experiment. No new publication timing JVM had started, and no prior timing sample will be repurposed.

2026-11-16: Keep the first-boundary cutoff and later cumulative growth policy exactly as described. Scope clears empty entries without resetting per-iteration adaptive history. A refused initial or growth reservation disables the table and releases all its arrays. A current valid hit may still be returned when a subsequent growth attempt fails; later probes use ordinary lazy resolution.

2026-11-16: Count a collision only for an occupied slot with a different full key at lookup. Unknown or invalid semantic/identity witnesses are misses, not hash collisions. Resizing records discarded entries separately. Full position masks remain part of the key even when they select the same existing value-cache bank.

2026-11-16: The normal capture path currently defers publication until the row is captured. Direct mode must publish a miss immediately after ordinary lazy resolution to preserve the proposed table's within-row collision behavior. Ordinary and factor reuse are both bypassed in direct mode. The existing source/view/exposure validation and repair remain authoritative.

2026-11-16: Preserve the interrupted primary cohort in full and begin `comparison-v2` from slot zero. Add a separate supervision controller that imports the frozen driver, pins the same runtime, protocol, schedule and Java commands, and launches exactly one JVM per invocation. It reconstructs completion from validated receipts, rejects any partial, failed, extra or missing predecessor, and never retries a fork. The original protocol, driver and independent analyzer remain unchanged. No first-attempt measurements are pooled, substituted or used to choose a new runtime or analysis rule. Record the gaps between JVMs without introducing a new cooling policy.

## Context and Orientation

Repository: `/workspace/scratch/37853bcaea36/rdf4j`. This phase is `result-coordinate-evidence/direct-memo-followup`. The immutable common runtime is described by `result-coordinate-evidence/compact-page-followup/compiled-v1/runtime-descriptor.json`; it contains the original fifteen common harness class files plus the preserved 127-entry runtime. The new prefix will contain exactly the changed materializer outer class, changed projected-binding-set outer class, direct memo, primitive statistics helper and its Snapshot, and a delegating launcher. Original plan, nested materializer classes, all harness classes, and remaining runtime entries stay byte-identical.

The publication phase's 106-case baseline explicitly compiled the eight current selected test classes. Its six publication cases also passed. Their commands, XML, class identities, and actual Surefire dependency manifests are preserved. New contracts will use isolated class copies and the same dependencies. Root owns the exclusive Java execution slot.

## Milestones and Plan of Work

First create contracts and a feature-red stub. Cover repeated ID/full-mask reuse, distinct masks, lazy initialization, unsupported versions, semantic mutation, dictionary identity mutation, collision replacement, first-window cutoff, delayed reuse, later growth/cap, initial and growth refusal, scope clear history, idempotent close, and ledger-first close. Assert actual source calls, object identities, and reservations. Keep failures and commands before implementation.

Then implement the direct table and an additive source generator. Preserve normal source resolution and all ownership code. Use the existing materializer's begin/end fences for capture; provide an explicit before/after validation path for individual value reads and collection, whose cached values otherwise lack a Sail exposure token. Detachment must discard materializer references. Demonstrate the exact four-array witness weakness separately and retain its failing evidence.

Qualify the actual prefix with direct contracts, selected existing coverage, and small/full end-to-end lifecycle checks as prescribed before timing. The primitive observer is allocated before the unchanged harness and serialized after it returns. It must never retain an extra Value or source, or scan retained payload graphs inside the timed window. It records table policy counters, capacity/array charges, and owner/query alignment. Array ledger numbers remain distinct from retained heap.

Freeze a fresh three-arm, two-fixture protocol with six balanced permutations, eight warmup and five measured queries per JVM. Use the preserved input hashes, 1 GiB G1 heap/global ledger, 512 MiB query ledger, normal AUTO, and PREFER native/adjacency settings. All 36 JVMs run serially, without builds, tests, profiling, compression, or optional heavy hashing. Natural weak misses and resulting resolver differences in ordinary/DENSE controls remain legitimate outcomes. No samples are replaced, excluded, extended, or pooled with earlier hosts.

Finally derive process-level medians and six paired ratios per fixture from raw data. Compare direct/ordinary and direct/DENSE separately and report DENSE/ordinary as a reference. The engineering screen is a median latency ratio at most 0.95, all six latency ratios below one, median allocation ratio at most one, and every allocation ratio at most 1.02. It is neither a statistical significance claim nor a production-promotion gate. Report all outcomes even if the screen fails, including preparation, actual routes, checksums, resolver calls, adaptive behavior, array ledger peaks, and the unresolved strong-retention bound.

## Validation and Acceptance

Require test-first evidence, no failures/errors/skips in accepted tests, exact common runtime identity, correct full result consumption/checksums and shared-tail exact-bag oracle, normal AUTO routes, compiled measured IR, one direct owner per direct query, no direct owners in control arms, complete owner/query resolver alignment, zero final ledgers, and a complete independent raw audit. Do not require identical resolver counts between arms. Admission refusal is a valid policy result in dedicated constrained-budget tests, but must be absent in the prescribed 512 MiB primary comparison.

## Idempotence and Recovery

Use new versioned output directories. Preserve every failed or rejected attempt. Never write production source through a broad formatter or overwrite original artifacts. Stage only explicit source, protocols, reports, receipts, and raw observations for commits; generated runtime directories and stores are packaged separately. Checkpoint to the authorized HASMAC branch without force or overwriting unrelated changes.

The finite supervision entry point is `result-coordinate-evidence/direct-memo-followup/supervision/run-step.py`. Initialize a fresh directory with `init`, then use one `next --slot-released` invocation per JVM and wait for its actual tool exit code before continuing. Each step must verify every previous terminal receipt and the exact frozen command. If any step disappears before its terminal receipt, preserve that cohort and diagnose the execution failure; do not replace its partial slot. `audit` performs no Java execution and can reconstruct derived summaries only from the complete validated receipt chain.

## Outcomes & Retrospective

Revised unit contracts passed their intentional-red check: 20 cases, 18 expected failures, zero errors or skips. The final 12 integration contracts passed their intentional-red check with seven expected failures and five preservation passes. The identity-witness-v2 runtime then passed 20 table contracts, 12 integration contracts, 106 existing disabled-feature cases, and 8 enabled-feature real-IR lifetime/bag cases (146 passing test executions; zero failures/errors/skips). The separate four-array counterexample then failed exactly the three identity-invalidation cases, with no errors or skips; it is diagnostic only. All six full-size lifecycle JVMs qualified (78 queries, 26 direct owners); an independent raw audit agreed. The runtime descriptor and 36-JVM primary protocol are frozen. The validated implementation and prior evidence are checkpointed on HASMAC as 678292b6538f24d8982ec35bca8ba404a6797d5b. The restored source and prior experiments are safely checkpointed; fresh baseline validation passed. The next outcome is concrete correctness evidence for the proposed cache key and adaptive policy.

Revision 2026-11-16: Created after the user's direct-mapped memo proposal, preserving the original performance objective and all earlier evidence.

2026-11-16 implementation note: The existing Plan and projected-row duplicate scan already compare full masks exactly; preserve them unchanged. Individual reads lack an explicit Sail exposure token. A valid table hit whose native dictionary value is not externally owned must therefore still pass through ordinary lazy lookup to establish the current exposure handoff. Certified outward capture keeps its explicit exposure fence. The additional integration contract was sealed and reproduced before the helper was generated, and now passes.

Revision 2026-11-16: After an unexplained controller disappearance, preserve the incomplete first primary cohort and change only external supervision for a completely fresh cohort. The candidate, controls, runtime, input, JVM settings, schedule and analysis criteria remain frozen.
