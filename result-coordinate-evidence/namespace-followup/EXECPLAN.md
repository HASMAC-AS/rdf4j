# Test coordinate frame bookkeeping and shared selections

This living plan follows `.agent/PLANS.md`. It continues the user's request to choose and conduct new experiments after the coordinate and materializer comparisons. All earlier source snapshots, runtimes, raw results and archives remain immutable. No commit or push is part of this request.

## Purpose / Big Picture

Determine which coordinate-transport costs are actually exercised under normal AUTO execution with direct adjacency enabled. A namespace is an execution-local identifier for the lifetime of a source of factor coordinates; its directory entry tracks live references and deferred retirement. The initial repeated-lookup hypothesis has been rejected for the measured fixture: no managed namespaces or directory lookups occurred. The revised candidate would skip saved-frame reference scans only while the owner has never allocated a namespace directory. Valid registers can then contain only permanent namespace 1 or unbound cells. Once a directory exists, every reference operation keeps its original implementation.

The observable result will be fresh diagnostic counts and a matched latency/allocation comparison with complete result checksums, query ledger endpoints and actual AUTO routes. A negative or inconclusive candidate remains an experiment rather than being promoted.

## Progress

- [x] Recover previous source and experiment identities.
- [x] Characterize range ownership and measure locality.
- [x] Qualify frame shortcut and shared fixture.
- [x] Verify candidate with matching ownership tests.
- [x] Freeze runtimes and execute all 18 prescribed comparison JVMs, retaining every sample.
- [x] Independently audit raw results, reject promotion, restore the production preimage and verify the rebuilt classes.
- [x] Prepare complete changes, reports and reproducible evidence for verified artifact delivery.

## Context and Orientation

Repository: `/workspace/scratch/37853bcaea36/rdf4j`, branch `GH-0000-lmdb-lazy-result-coordinates`, checkpoint `15fe7d5011`. The current source contains validated ownership repairs beyond that commit. The exact previous measured `KernelResultCoordinates.java` is preserved under this phase's `baseline/preimages/` and has been restored to the working source. The three-guard candidate was not promoted and remains only under `runtimes/frame-directory-guard/`. Nine new characterization cases remain in the test source; no production change belongs to this phase's final worktree delta.

The implementation is `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/codegen/KernelResultCoordinates.java`; characterization tests belong in the corresponding `KernelResultCoordinatesTest.java`. Prior scripts and frozen artifacts are under `result-coordinate-evidence/materializer-followup/` and `root-qualified-launcher/`. New phase scripts and evidence belong only under `result-coordinate-evidence/namespace-followup/`.

## Surprises & Discoveries

The old benchmark harness explicitly enabled the native engine but did not enable direct adjacency. This new protocol will use an identical additive harness overlay for all arms with direct adjacency explicitly enabled. This is a new experimental series and will not be pooled with the older measurements. Actual route observations and readiness are verified, not inferred from configuration flags.

Prior sampled leaf counts put namespace lookup/index work at a modest fraction of total CPU, across all callers. Range-local savings can only address a subset. The objective is to measure a concrete isolated cost, not assume this alone closes the prior runtime gap.

The fresh instrumented diagnostic passed all result, ledger, root-hosting, actual AUTO route, IR tier and adjacency checks. Each compiled query performed 1,567,000 frame-retain ranges and the same number of frame-release ranges. Each direction visited 17,237,000 cells, with 17,236,968 positioned reference calls. All scopes had zero managed namespace operations, directory lookups and probes. The first interpreted warmup also had zero managed lookups despite 109,641,455 visited cells in each range direction. Every query had 55 successful adjacency lookups, no misses or fallbacks, ACTIVE adjacency and matching current/applied revision 1. This instrumentation is diagnostic only; its durations are not performance evidence.

The original production implementation passed the new characterization gate: eight classes, 98 tests, zero failures/errors/skips. The coordinate ownership class contributed 14 passing cases, including repeated tokens, sentinels, unknown tokens, frame replacement, collision deletion and directory growth.

The expanded matching frame-guard gate passed before and after the candidate: eight classes, 103 tests, zero failures/errors/skips in each run. The coordinate ownership class has 19 passing cases in both. The freeze helper verified current sources against the successful post-change gate and froze the candidate under `runtimes/frame-directory-guard/`. Only two class files changed, both from the permitted production source; all 126 other original runtime entries are exact. Candidate production tree SHA-256 is `63ca7bd4fcd6f19efc32f1f1194f5f1f990aaf0488cbea538ab79ce4ad222016`. The reviewed production delta SHA-256 is `0ceed5b1f28931ae334671960f8f875e94d61883ab4e4ed1da29a3f3db70ac0e`.

The separate v2 diagnostic directly counted the proposed guard condition: all frame ranges entered while `namespaceIds == null`, including all 17,237,000 cells per compiled range direction. Thus the proposed guard would avoid 34,474,000 frame cell visits per compiled query on this fixture. V1 and v2 artifacts remain separate and unchanged.

The shared-tail pilot completed 12 full queries across OFF/DENSE/ROWS with exact bag/checksum agreement and zero ledger endpoints. All measured queries used compiled normal IR and no hosted generic root. ROWS encoded all 1,048,576 rows but reported 16,384 groups for 16,384 tracked column-window positions, so groups/columns equals one. The prospective shared-selection timing comparison is not qualified and will not run. This pilot also observed coordinate ledger peaks near the 256 MiB limit, versus 1,472 bytes for ordinary reuse; page occupancy is a new diagnostic question, not yet an established explanation.

The completed page diagnostic confirmed sparse storage. In its measured compiled query, 16,364 pages were admitted and released, charging 268,107,776 bytes. Exactly 16,112 pages had one occupied weak-reference slot and 252 pages had 64; total slot occupancy was 3.0784%. A single page-payload admission failed with only 9,180 bytes remaining below the 256 MiB limit. No directory or memo admission failed. All page/directory/memo grants and explicitly observed releases reconciled exactly; no page retired or cleared before final close, so the final key table covers every admitted page. All namespaces were 1. The left-side columns held 8,174 pages (127.71875 MiB) and recorded zero factor-cache hits in this traversal; right-side columns recorded 2,048,256 hits. All four exact-bag queries passed and ledgers returned to zero. Instrumented durations and allocation totals are not performance estimates. Shared-tail adjacency observations establish configuration/readiness, whereas the original fanout harness also records successful query-time lookup hits.

The completed primary experiment ran exactly 18 JVMs and 234 queries, including all 90 prescribed measured samples. Every result, checksum, route, ledger endpoint and identity check passed. The guard/original-DENSE median paired latency ratio was 0.949192, ranging from 0.901233 to 1.059632, with four of six favorable rounds. This fails the frozen all-six promotion rule. Guard/OFF was 1.255492 and original-DENSE/OFF was 1.330568 by median paired ratio; both coordinate variants lost all six matched OFF comparisons while retaining approximately 16.27% lower allocation. One guard fork had a stable lower-allocation regime of unknown cause; all its measurements remain included. Separate arm medians and paired median ratios differ, so the reports include the full six-round table rather than conflating the two statistics.

After non-promotion, only the candidate production file was restored. All 2,055 module source files exactly match the passing unchanged-production 103-case gate, including the retained tests. The restoration install succeeded, and all 2,778 production class files match the frozen original byte-for-byte. The only rebuilt resource difference is the quick profile's `formatter.skip=true` property versus `false` in the frozen original. This metadata difference is explicitly recorded in `validation/restored-original/verification.json`; no measured runtime was altered.

## Decision Log

2026-11-15: Choose range-local lookup memo before a broader namespace handle redesign or shared-selection architecture because the directory cannot relocate within these loops, making the change independently testable. Slot positions can change between calls after growth/deletion, so no slot escapes a call.

2026-11-15: Use separate instrumented diagnostic binaries to count locality and probes; do not use their durations as performance evidence. The primary comparison uses uninstrumented original OFF, original DENSE and candidate DENSE, all with the same new native-plus-direct-adjacency harness.

2026-11-15: Prespecify six matched rounds using all six permutations of three arms, eight warmups and all five measured queries per fresh JVM. Keep all samples and report median and complete range of per-round ratios. Promotion requires correct results/cleanup and all six candidate/original-DENSE ratios favoring the candidate. No additional rounds or tail replacement based on observed timings.

2026-11-15: Independently prepare a true shared-selection fixture with two-column optional tails. The prior ROWS experiment had one group per column and never exercised this intended advantage. This separate experiment will compare original OFF/DENSE/ROWS only, with normal AUTO and native/direct adjacency enabled. First require observed groups/columns below one and correct results/cleanup in a pilot; do not force IR to manufacture participation. Its final timing protocol, if warranted, will be frozen separately before timing. No results will be pooled with the namespace candidate or old direct-adjacency-disabled runs.

2026-11-15: Reject the range-local memo before implementation or timing because the new diagnostic observed zero lookup opportunity. Its drafted timing protocol remains unexecuted. Investigate a narrower frame-only guard using `namespaceIds != null`, without adding any metadata. Source-batch validation must remain unconditional because public batch arrays can contain unknown positive tokens. Obtain independent invariant review and add transition/invalid-token characterization on unchanged production before this neutral performance candidate.

2026-11-15: Follow up the shared-tail pilot's near-budget memory footprint with one separately instrumented original-DENSE diagnostic, three warmups and one measured-stage query. The materializer uses 64-cell pages charged at 16,384 bytes. Chained one-result probes may therefore allocate mostly empty pages, but that is a hypothesis until observed. Count admissions, refusals, released charges and occupancy without changing reservations or cache behavior. Publish exact page keys and distinguish stored weak-reference slots from currently live referents. Do not use instrumented durations for performance claims, and do not run the unqualified shared-selection timing protocol.

2026-11-15: Do not promote the frame guard because only four of six paired latency ratios improved. Restore the exact production preimage while retaining nine tests and the complete prototype. Do not add more rounds or discard the lower-allocation fork. Independent analyses reproduce every reported ratio directly from the raw JSONL.

2026-11-15: Select compact initial pages with charged promotion as the next implementation experiment, without implementing it during this completed study. It directly addresses observed sparse reservations while preserving coordinate keys, weak values, witnesses and ownership. Keep admission-policy changes separate. Do not use the shared-tail cap pressure to explain the primary fanout timing result, whose DENSE peak is only 2,264,968 bytes.

## Plan of Work

First add public-API characterization cases on the unchanged production implementation: repeated managed tokens mixed with unbound/sentinel cells, missing tokens after valid repetitions, and collision deletion plus directory growth between range calls. Capture matching pre-change passing evidence for the narrow optimization. Run the required offline root clean install using `../mvn`, whose wrapper supplies JDK 25 and `-Dmaven.repo.local=.m2_repo`; then run the focused eight-class transport/materializer selection without `-am` or `-q`.

The completed separate diagnostic source overlay counted managed cells, call-local token transitions, range lengths and directory probes, split source batches from saved frames. Its immutable evidence is under `diagnostic-original-v1/`; runtime identities are under `compiled-v1/`. The absence of managed lookups rules out a memo on this fixture.

For the revised candidate, first characterize permanent-coordinate frames before and after the first managed namespace allocation, frame overwrite and partial restore, and unknown source-batch tokens without an allocated directory. Guard only the retain/release range calls in `save` and `releaseFrame` while the directory is null. Register writes validate managed tokens before they can be copied into frames, and a directory is never discarded while an enabled owner remains live. Do not skip public source-batch scans or change the range loops themselves. Repeat the identical expanded test selection after formatting and official dependency/module installation. Freeze candidate production bytes separately and require every other production class/resource and every common harness/dependency to match its declared control.

The shared-selection milestone uses a new optional-tail fixture with 64 anchors and two independent 128-row tails, each tail projecting two correlated columns. Its independent Cartesian oracle checks exact row membership and multiplicity with a bitset, in addition to lexical checksums. Compile and run the oracle-only validation, then the frozen OFF/DENSE/ROWS diagnostic pilot. Proceed to a separate balanced timing comparison only if normal AUTO actually exercises shared selections, with groups/columns below one, exact full results and complete cleanup.

Because the pilot did not qualify for sharing but exposed a large memory charge, the next shared-tail milestone is an additive materializer diagnostic under `shared-selection/page-occupancy/`. Copy the exact original materializer source into an isolated overlay. Keep the existing fixture and benchmark harness bytes unchanged. Track page admissions, live-page peaks, refusals and cumulative reserved/released charges with primitive counters. Scan occupancy on existing page-removal paths and emit final page keys during owner close; report incomplete key-table coverage if earlier pages were removed. The runner must reconcile accounting, preserve all raw records, validate the same exact result oracle and routes, and forbid interpretation of instrumented timings.

After diagnostic and correctness gates, stop builds/tests/hashing/packaging during serial timed JVMs. Execute the fixed protocol; validate every query and actual route, record latency/allocation/preparation/ledger data, and independently recalculate ratios. Report negative and inconclusive outcomes. Retain a production optimization only if the fixed criterion is met; otherwise restore only this phase's exact production preimage and retain the characterization tests and prototype snapshot.

## Concrete Steps

Initial root build from repository root:

    ../mvn -o -Dmaven.compiler.showWarnings=false -T 1C -Pquick -DskipFrontend=true clean install

The phase validation runner records the full commands, pre/post source hashes, format output, `maven-build.log`, exact eight-class Surefire selection, and fresh reports under `validation/<stage>/`. The runner must supply workspace-local Maven repository through the wrapper and preserve successful/failed reports without replacement. Initial report evidence is also preserved in `initial-evidence.txt` without overwriting prior content.

Benchmark preparation and execution commands will be pinned in the new protocol and driver before any timed query. The original source-map identity is `34767935d60f32ba61adfb44495c607e17ca760d5d2f77a17ddb2862c4f7a8ac`; the existing root-audited 127-entry runtime is the production control. The new common harness overlay has a separate digest.

## Validation and Acceptance

Require matching pre/post targeted passing tests, including compiled and interpreted IR, retained rows, cancellation, mutation witnesses, collision/growth and ledger cleanup. Frame-guard diagnostics must prove that avoided scans contained no managed references. Timed runs use Java 25.0.5+8, fixed 1 GiB G1 heap, identical graph/query within each experiment, normal AUTO, explicit native/direct adjacency, and fresh serial JVMs. Each original fanout query must return 1,152,000 rows and checksum 3123987218479850048. Each full shared-tail query must return 1,048,576 exact rows and checksum 8327963502356915200. Root hosting, IR tier and adjacency observations must be recorded and compared. Missing diagnostics are not zero.

## Idempotence and Recovery

Use fresh stage and runtime directories with refuse-if-existing behavior. Do not overwrite old experiments or reset the dirty worktree. Preserve exact preimages and source hashes for the two files edited by this phase. On a failed benchmark validity check, preserve the failed process and stop the protocol rather than replacing its fork. Any redesigned protocol is separately named and justified before new timing begins.

## Artifacts and Notes

New deliverables will include complete modified source files, a precise delta against the preserved input, a checkpoint-based full patch if required for review, raw diagnostics/timings, test evidence, protocol and runtime identities, an independent analysis and reproduction commands. Repository-backed source changes stay with the project; user-facing reports/evidence are saved as durable artifacts.

## Outcomes & Retrospective

This phase completed 24 experiment JVMs and 258 full queries plus a separate oracle-validation JVM. The directory-memo hypothesis had no measured opportunity. A correct frame guard removed proven unnecessary scans but failed its fixed performance gate, so production was restored. The shared-selection fixture did not expose common selections under AUTO and therefore did not qualify for a timing comparison. Its separate page diagnostic identified 98.46% singleton pages, a payload admission refusal and exact release accounting. That observation supports a future compact-page experiment; it is not a demonstrated speedup. The reports, sources, raw evidence and frozen runtime identities are complete and ready for archive packaging, with final archive verification recorded by the delivery manifest. The earlier work's ownership repairs and historical evidence remain preserved.

## Interfaces and Dependencies

No new dependency or public API is needed for the candidate. Diagnostic counters and the shared harness overlay are isolated experimental binaries. The ownership directory remains execution-confined. Existing `namespaceIndex`, `releaseNamespace`, retirement queues, memory reservations and source-generation checks retain their semantics.

Revision note, 2026-11-15: updated the purpose, milestones, acceptance and decision log after the fresh diagnostic showed zero lookup locality. The replacement frame shortcut and the independent shared-selection fixture are qualification work, not assumed performance wins.

Revision note, 2026-11-15: v2 established the exact frame-guard hit condition; the shared-tail pilot failed its sharing gate, so its balanced comparison is intentionally unexecuted. Five additional ownership characterizations are ready for a matching pre/post gate before the production guard is edited.

Revision note, 2026-11-15: added a bounded page-occupancy diagnostic in response to the pilot's observed near-limit ledger peak. This investigates the original cache layout without changing its policy and remains separate from the uninstrumented frame-guard timing comparison.

Revision note, 2026-11-15: matching 103-case pre/post gates passed, and the narrow candidate was frozen with exact dependency and source-matched validation identities. The next milestone executes the page diagnostic and the fixed uninstrumented comparison serially.

Revision note, 2026-11-15: the page diagnostic completed and confirmed sparsity with exact cleanup accounting. The frame-guard comparison is now running under frozen protocol SHA-256 `bbad71c3c879b49022ff6cb22d229ee94353961d1f512defdec722fe2a368f54`; all other JVMs, builds, profiling, packaging and optional heavy analysis are paused until its 18 prescribed forks finish.

Revision note, 2026-11-15: the fixed comparison, independent reviews, source restoration and restoration build are complete. Updated the final decision, source state and outcomes, and prepared `new-experiments-report.md` for delivery. The production candidate is unpromoted; no further experiments are authorized by this completed protocol.
