# Compare compact factor pages on two fixtures


This is the benchmark component of the compact-page follow-up, maintained under `.agent/PLANS.md`. The root agent owns production/test changes and their correctness gate. This plan changes only additive harness, protocol and runtime-freezing files in `result-coordinate-evidence/compact-page-followup/`.

## Purpose / Big Picture


The previous shared-tail diagnostic found that 98.46% of admitted factor pages held one reference while each charged a 64-slot payload. That workload reached its 256 MiB query limit despite only 3.08% slot occupancy. A compact initial page could reduce reserved memory and allocation, but extra lookup or promotion work may cost CPU. This experiment will compare the original ordinary-reuse OFF control, current DENSE pages, and compact-page DENSE on both the existing fanout and shared-tail fixtures. It will report memory and latency separately before any promotion decision.

## Progress


- [x] Reconcile frozen sources and both fixtures.
- [x] Prepare matched harness and prospective protocol.
- [x] Review protocol and freeze candidate identities.
- [x] Run serial comparisons after explicit authorization.
- [x] Recalculate results and document final decision.
- [x] Hand off the verified numerical report for the root's final delivery.

## Context and Orientation


The preserved original runtime is the 127-entry classpath in `result-coordinate-evidence/root-qualified-launcher/classpath.txt`. Its repaired ownership implementation and original frame handling are the baseline for every arm. Its source identity is `15fe7d5011+worktree-sha256-34767935d60f32ba61adfb44495c607e17ca760d5d2f77a17ddb2862c4f7a8ac`. The previous frame-guard candidate was restored and is not part of this comparison.

The frozen fanout harness lives under `result-coordinate-evidence/namespace-followup/compiled-v1/common-harness/classes`. It already observes actual direct-adjacency counters outside query measurement windows. Its fixture uses M=100, R=10, 12 projected columns and two statuses, producing 1,152,000 rows with checksum `3123987218479850048`.

The shared-tail harness lives under `result-coordinate-evidence/namespace-followup/shared-selection`. Its default fixture has 64 roots, 128 left and 128 right entries, and two columns per OPTIONAL tail, producing 1,048,576 rows with checksum `8327963502356915200`. Its independent oracle verifies that every value is an IRI, all columns belong to the expected tuple and parent, every complete row key is legal and unique, and the full count/checksum match. Its oracle storage is allocated before timing; result consumption and checking stay inside timing.

A factor page is the materializer's bounded cache for primitive source coordinates and weak value references. Compact pages may change only that representation in `NativeResultMaterializer.java`, preserving coordinate keys, semantic witnesses, weak-reference behavior, ownership and refusal fallback. The root controls the implementation and validation; this plan does not assume that it is correct or faster.

## Plan of Work


First prepare complete source copies. Preserve the existing fanout harness, adjacency helper, shared-tail fixture/oracle, fixture checker and root launchers. Modify only the shared-tail main to wait for actual startup maintenance and read existing adjacency snapshots before and after the query, outside its timing/allocation window.

When the root grants a compiler slot, create one new shared harness classpath prefix. Copy the six frozen fanout/helper classes and nine frozen shared-tail classes. Compile only the changed shared-tail main and replace its four harness classes in the new prefix. Check that fixture/oracle/launcher and fanout class bytes are unchanged. All three arms and both fixtures use this identical prefix before the preserved 127-entry runtime.

The root supplies passing matching correctness records and the compact-page candidate. Freeze only the candidate production entry, with exact source, class, dependency and reviewed patch hashes. Verify that original frames and every production file outside the declared materializer file are unchanged. The co-declared `NativeResultMaterializerPlan.class` is allowed only as an explicitly mapped class from that same source file.

Before timing, agree the promotion criterion with the root, freeze the protocol and complete its identity validation. No timing JVM or CPU-heavy task runs without an explicit exclusive slot. All prior artifacts remain untouched.

## Proposed Protocol


The proposed design is three arms across all six permutations on each fixture, eight warmups and all five measured queries per fresh JVM. This gives 36 JVMs, 288 warmups and 180 measured queries. Each round runs a complete three-arm fixture block, alternating which fixture starts the round. Each fixture has its own matched ratios; results are never pooled across different fixtures or with prior studies.

Use Java 25.0.5+8, a 1 GiB fixed heap, G1 and the same existing hot counters in every arm. Native evaluation and direct adjacency PREFER remain enabled. Global query-memory authority is 1 GiB and per-query allowance is 256 MiB. The original per-fixture store configuration remains unchanged. Normal AUTO chooses strategies and IR tiers without overrides.

For each fork, summarize all five measured queries by their median. Compare compact DENSE/current DENSE, compact DENSE/OFF and current DENSE/OFF within each round, then report the median and full range of six ratios. Report all-thread and caller allocation, preparation, evaluation, first-row, consumption, close and ledger peaks alongside total latency. Preserve cold first-query and all warmup observations separately.

The root approved the engineering acceptance screen before new data: shared-tail candidate peak ledger must be at most 25% of current DENSE in every pair, and all-thread allocation must be lower in every shared-tail pair. The median paired total-latency ratio must be at most one on each fixture, with no individual pair above 1.10 on either fixture. All gates must pass for promotion. This is an engineering screen for a memory optimization, not a statistical noninferiority result. A separate consistent-latency-win claim still requires all six ratios below one on that fixture. Report every pair and every failed gate; no tolerance or extension may be chosen after data.

## Validation and Acceptance


Every query must produce the exact expected result count/checksum, and shared-tail must additionally pass its complete independent bag oracle. Query-ledger use must be zero before and after closure. DENSE must admit its coordinate owner; OFF must not. Native semantic compilation counts must reconcile per query and with the post-main root audit, which must contain zero hosted-generic roots.

Native/direct-adjacency flags are insufficient by themselves. Require ACTIVE adjacency and stable applied/current revision before and after each query, positive successful lookup hits, successful normal AUTO selection and native IR participation. Measured queries must use compiled IR and match the first qualified measured route signature for that fixture. Lookup hits establish query-time adjacency use, potentially including preparation/probes, not that every row used adjacency.

The root's correctness gate must validate compact insertion/replacement, witness mismatch, weak-reference clearing, promotion failure and retry, admission refusal, release/closure and both IR tiers. Benchmark protocol gates do not replace those tests.

## Concrete Steps


Work from `/workspace/scratch/37853bcaea36/rdf4j`. Source preparation launches no Java:

    python3 result-coordinate-evidence/compact-page-followup/prepare-harness.py

After explicit compiler-slot authorization, compile the additive harness:

    python3 result-coordinate-evidence/compact-page-followup/compile-harness.py --slot-released --output result-coordinate-evidence/compact-page-followup/compiled-v1

The command preserves the original 127 entries and all copied fixture/oracle/launcher/fanout classes. The new runtime descriptor names the common 128-entry baseline. The candidate was frozen after the matching 108-test correctness gate, then bound to the independently reviewed production patch:

    python3 result-coordinate-evidence/compact-page-followup/run-comparison.py --protocol result-coordinate-evidence/compact-page-followup/protocol-draft.json --freeze-candidate result-coordinate-evidence/compact-page-followup/runtimes/compact-page/runtime-descriptor.json --frozen-protocol result-coordinate-evidence/compact-page-followup/protocol.json --reviewed-production-patch-sha256 665dca2f8fc662b510fa8d7f285f417a57546e8aec71418f169340c1b8153aa1

Validation without a JVM passed:

    python3 result-coordinate-evidence/compact-page-followup/run-comparison.py --protocol result-coordinate-evidence/compact-page-followup/protocol.json --validate-only

Only after the root grants the exclusive timing slot:

    python3 result-coordinate-evidence/compact-page-followup/run-comparison.py --protocol result-coordinate-evidence/compact-page-followup/protocol.json --output result-coordinate-evidence/compact-page-followup/comparison-v1 --slot-released

## Idempotence and Recovery


All output directories and protocol snapshots must be new. Existing files are never overwritten. Any failed process, invalid result, changed route or changed identity preserves its evidence and stops the affected protocol; no replacement forks, favorable-tail selection, forced routes or extra timing rounds are permitted. A repaired or revised study needs a separately frozen protocol.

## Surprises & Discoveries


The previous shared-tail harness awaited direct-adjacency readiness but did not record physical lookup-hit deltas or await startup maintenance before population. The new main adds the same proven observation/readiness path used by fanout, keeping its fixture and exact oracle unchanged.

The previous shared-tail ROWS pilot did not establish multi-column sharing: groups/columns remained one. This follow-up therefore uses DENSE for the candidate and targets observed sparse page occupancy, not a presumed row-selection benefit. The namespace/frame optimization was not promoted and is excluded from both production arms.

## Decision Log


On 2026-11-16 the root assigned only benchmark, protocol and freeze tooling to this agent, with no core production/test edits and no JVM before authorization. The root approved the two-fixture, 36-JVM balanced protocol and the explicit memory/latency engineering acceptance screen before data. Independent review approved the shared-tail harness additions: snapshots remain outside allocation/timing windows and no route override was added.

## Outcomes & Retrospective


The approved common harness is compiled from frozen inputs. The new prefix contains 15 classes, of which only four shared-tail main/nested harness classes changed; the other 11 copied classes and all 127 original base entries are unchanged. The prefix SHA-256 is `05c88f61336fce5028bb617e5ff7f7759e4a55ae4c4dba3a3b487c5512f0d825`. Driver/protocol/freezer received independent read-only review with no blocking issue. The matching correctness gate passed 108 tests with zero failures, errors or skips; the root froze a candidate with exactly three materializer classes changed. After the explicit exclusive timing grant, all 36 benchmark JVMs and 468 queries completed with no guard failure. Raw recalculation and independent review agree exactly with every driver estimate. The candidate fails the fixed latency screen despite the shared-tail memory gains. `BENCHMARK-RESULTS.md` records the complete numerical decision, and its 29 artifact links have been verified. No extra performance run was added. The root has received and incorporated the complete numerical handoff. All benchmark work is complete, and all benchmark-owned files are closed for delivery selection.

Revision 2026-11-16: created this self-contained benchmark plan and additive harness tooling before compilation or candidate timing. Root-approved acceptance requires all six shared-tail ledger ratios <=0.25 and allocation ratios <1, plus each fixture median latency ratio <=1 and every individual ratio <=1.10. The candidate charges a 512-byte inline shell and a separate 15,872-byte dense-promotion reservation; original frames remain fixed.

Revision 2026-11-16: root authorized the additive javac build while its correctness gate ran. Compilation and complete preservation checks passed. Pinned the resulting common runtime descriptor in the unfrozen protocol; preserved the earlier draft in pre-freeze-history. Review confirmed the acceptance arithmetic and per-fixture route guards. Timing shows coordinate admissions and ledger closure, not an explicit owner-closure counter.

Revision 2026-11-16: froze the final protocol after source-matched correctness and independent patch review. Protocol SHA-256 `8ef50b935b83cd22c525616438949f730042ed09c179eff332e39c5601f9b374` binds production patch SHA-256 `665dca2f8fc662b510fa8d7f285f417a57546e8aec71418f169340c1b8153aa1` and candidate production tree SHA-256 `efbb57205da275bd4032ee0f127eb2028010e6707dde8b4538807f94c5542595`. Validation-only passed. The delivery agent's separate diagnostic owns the JVM slot; this timing study has not started.

Revision 2026-11-16: after the root's exclusive timing grant, the exact frozen protocol completed all 36 JVMs and 468 queries, including all 180 measured queries, with no errors, exclusions or replacement forks. Every fork passed result and route qualification; full post-run runtime preservation passed. The timing slot was released immediately. Direct raw JSONL recalculation reproduced every driver median and ratio, and independently confirmed nonoverlapping JVM process intervals. The prospective screen failed: fanout round 3 latency ratio 1.1156027995 exceeded 1.10, and shared-tail median paired latency ratio 1.0125792493 exceeded 1.0. Both shared-tail memory gates passed. No extra forks were run or proposed to revise these gates.

Revision 2026-11-16: completed the 3,803-word numerical report with all paired rounds, absolute arm summaries, allocation variants, warm execution phases, cold-query observations and counter ranges. Verified all 29 linked artifacts and saved `benchmark-report-verification.json`. Report SHA-256: `516bf9eed9c40afcaaaeec531c615973ec4f77281001b14b3bb75907e66d037c`. Handoff to the root is the sole active step; no further benchmark work is planned.

Revision 2026-11-16: the root confirmed receipt and incorporation of the complete numerical handoff. Marked the final handoff step complete. This plan-only closure changed no frozen protocol, runtime, raw result or numerical report; all benchmark-owned files are now closed for final delivery selection.
