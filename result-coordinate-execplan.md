# Validate LMDB result ownership and measure coordinate transport

This living ExecPlan follows `.agent/PLANS.md`. It starts from commit
`15fe7d5011d27d31d76436238363ee3f34b3ba50` on
`GH-0000-lmdb-lazy-result-coordinates` in `eclipse-rdf4j/rdf4j`.

## Purpose / Big Picture

Make native query results and scalar expressions remain valid for their documented
lifetimes, including after child cursors close, and release all invocation memory
when the owner closes. Then determine whether carrying source coordinates through
native evaluation improves elapsed query time compared with ordinary value reuse.
The ownership implementation, query input, JVM, and automatic strategy selection
must be identical in that decisive comparison. Allocation alone is not success.

## Progress

- [x] Recover exact checkpoint and inspect instructions.
- [x] Search for preserved historical experiment artifacts.
- [x] Reproduce ownership failures on untouched checkpoint.
- [x] Fix known ownership regressions and verify both IR tiers (broad-suite memory limitation recorded).
- [x] Implement and validate controlled transport variants.
- [x] Complete root-classified confirmation after matched ablation and sweeps.
- [x] Assemble complete source/evidence delivery inputs and verified runtime downloads.
- [x] Prepare strict patch-replay and archive-readback publication; its final receipt is `package-summary.json` beside the downloads.
- [x] Profile frozen materializer consumption costs.
- [x] Identify separately testable redundant cache work.
- [x] Optimize proven work and verify parity.
- [x] Measure candidates and retain supported implementation.
- [x] Package and verify self-contained comparison runtimes.
- [ ] in_progress: Verify complete source and patch publication.

## Surprises & Discoveries

The branch exists on the upstream repository, with the exact requested commit;
the HASMAC branch lookup returned not found. Existing local checkouts did not
contain the commit. No matching preserved baseline or Stage-2 binary, raw log, or
JVM command was found in local files or Library. The historical allocation and
runtime figures supplied by the user are therefore context, not fresh evidence.
The checkpoint's parent is `e7c6ca7537ebde5a3a5198f4e367286707c68edd`.

The follow-up's frozen JFR recordings locate extra consumption work in both the
materializer and the coordinate carrier. File-write events match every complete
JSONL record by order and byte count, allowing conservative consumption-only
sample windows without changing the benchmark harness. The two five-measurement
profiles are diagnostic; their times are not new unprofiled performance estimates.
An unchanged coordinate memo still copies five words, validates its cached page
key, and republishes an identical weak-reference witness tuple. The cursor also
repeats live-cell validation inside the conservative `mayReplay` eligibility hint.

## Decision Log

The user's subsequent "continue" authorizes a new optimization phase after the
completed first delivery. Preserve that delivery, all seven original protocols,
and their immutable runtimes as the baseline. Profile the frozen OFF/DENSE
consumption paths first, then isolate factor-cache population/validation from
coordinate copying. Only optimize work supported by code inspection and measured
evidence. Keep all authority, mutation, lifetime, and memory-admission checks.
The previous stop-after-protocol decision remains true for the completed phase;
it does not prohibit this newly requested work. Keep one active step in this plan.

Use an isolated checkout and local dependency repository. Preserve the checkpoint
unchanged until its targeted tests have run. Reconstruct historical variants only
when their exact source can be established; never label a reconstruction as a
recovered Stage-2 artifact. Continue with the same-ownership ablation even if the
historical artifacts cannot be recovered, because this directly answers the first
performance question.

Use Routine D for the overall migration, recording pre-change failures for each
concrete bug and keeping existing assertions intact. Do not add dependencies or
publish changes while conducting these experiments. Test and benchmark changes
will remain reviewable as a patch and complete source archive.

## Outcomes & Retrospective

The follow-up comparison completed sixteen fresh JVMs and all 208 queries. Neither
local performance candidate met the prospectively fixed requirement that all four
paired latency ratios improve. Candidate 1/original DENSE had a median ratio of
1.024433 with range 0.970179–1.179192; cumulative Candidate 1+2/original DENSE was
0.997763 with range 0.908802–1.084001. Original DENSE/original OFF remained slower
in all four pairs, median 1.319224, while allocating approximately 16.3% less.
The new prototypes therefore remain preserved experiments, and both production
files were restored to the prior validated delivery. The final matched 94-case
gate passed; all 2,780 production artifacts are byte-identical to the original
frozen runtime. Only the new factor-page mutation characterization remains as an
active LMDB source change relative to the previous delivery. The self-contained
runtime bundle passed archive readback, fresh extraction, relocation and generated
driver validation. Final source/evidence publication and exact patch replay are
the remaining self-verifying transaction. This source snapshot deliberately
precedes that transaction; its external `package-summary.json` and
`delta-verification.json` receipts record completion without rewriting the inputs
they authenticate.

The same-source ablation, compact comparison, workload screen, short-query
confirmations, and separately reconstructed parent comparison are complete.
The default fully consumed query favors ordinary reuse for latency and coordinates
for allocation. Final broad verification exposed additional missing-session fixtures
and a startup-maintenance readiness race. Their narrow repairs pass focused checks;
the final repeated gate completed all 484 selected classes with 6,469 passing cases,
twelve existing skips, and no failures or errors. The production bytecode remains
identical to the measured frozen runtime. A final, separately prespecified control
recorded outer-root hosting because the original observer captured native
operator/tier selections only. All sixteen new forks passed the zero-host audit;
their median paired DENSE/OFF latency ratio is 1.335632 and allocation ratio is
0.837405. Every enabled arm is slower than OFF in every matched round. Complete
source/evidence delivery inputs and the exact measured runtime archives are ready.
The final publication operation verifies its own patch and archive contents, then
emits `package-summary.json` beside the downloads without rewriting this snapshot.
Verification limitations and exact source identities are retained below.

## Context and Orientation

The native engine lives in `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation`.
`NativeExecutionContext` owns a query invocation, its cleanup leases, and memory
accounting. A lease keeps resources alive until its owner releases them. Scalar
plans and VALUES recipes must be reusable descriptions, while bound evaluation
state belongs to an invocation. Returned results must not accidentally retain a
closed child's resolver or an uncharged prepared-value catalog.

`NativeResultMaterializer` reuses value references. `KernelResultCoordinates` and
the row state carry provenance: the source, generation, position, and value
identity that allow repeated source positions to reuse resolution. Native IR is
the engine's intermediate instruction representation; it can be interpreted or
compiled into a generated Java kernel. Both tiers must preserve the same ownership
and coordinate invalidation rules.

`core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/OptionalFanoutBenchmark.java`
and `OptionalFanoutFixture.java` contain the checkpoint's benchmark. The default
M=100, R=10 fixture has 3,278 triples, 1,152,000 result rows, and 12 projected
columns. It fully consumes values and validates row count and an independently
calculated order-insensitive checksum. The current method includes preparation
and execution; separate counters are required for this task.

## Plan of Work

First build the untouched source using Java 25 and run the existing scalar,
entry-binding, VALUES, group/wrapper, cyclic-constant, materializer, coordinate,
fanout, and lifetime tests. Persist XML/text reports before another run overwrites
them. Divide fixes by files to avoid concurrent edits. Check both success and
exception/early-close cleanup, including ledger release and retained rows.

After ownership tests pass, introduce experimental transport choices without
changing optimizer selection: ordinary reuse, current five-word coordinates,
changed-register bookkeeping, and primitive selection transport. These choices
must share the same ownership code. Use focused parity tests with checksums and
both IR tiers before timing. Record whether each implementation is a full execution
variant or an isolated transport prototype; do not combine their performance claims.

Run fresh JVM forks with identical heaps, collectors, processor limits, inputs,
warmup, and measured repetitions. Alternate variant order. Record preparation,
execution/consumption, total latency, allocated bytes, memory-ledger peaks, routes,
row counts, and checksums. Sweep fanout/repetition, projected width, unique values,
full consumption, early close, retained rows, and constrained memory. Keep raw
observations and distinguish errors/incomplete runs from measured samples.

## Concrete Steps

Working directory: `/workspace/scratch/37853bcaea36/rdf4j`.
The local Maven launcher is `../mvn`, backed by the installed Java 25.0.5 and Maven
toolchain. All Maven commands use `-Dmaven.repo.local=.m2_repo`.

    ../mvn -T 1C -o -Pquick -DskipFrontend=true clean install
    ../mvn -o -pl core/sail/lmdb -Dtest=<focused classes> -DskipITs verify

Use no `-am` or `-q` when tests run. Keep exact commands and logs under
`result-coordinate-evidence/`. Test reports are copied into named subdirectories.
Run the repository formatter and copyright checker before final packaging.

## Validation and Acceptance

All initially reproduced ownership failures must pass with unchanged assertions,
and focused lifecycle/coordinate suites must pass in interpreted and generated IR.
Additional regression tests must cover a root cause that existing coverage misses,
especially retained-row independence, cancelled/failed cleanup, and shared recipe
accounting. Ordinary reuse and coordinate variants must produce equal results and
report matching execution routes under AUTO before their timings are compared.

Conclusions must state the exact measured workload and uncertainty. A faster
microbenchmark is not proof of an end-to-end win. If routes diverge, report that
fact and do not call the result a controlled transport ablation.

## Idempotence and Recovery

All work is isolated from other checkouts. Never remove existing untracked
artifacts. Preserve raw logs before reruns. Dependency downloads may be retried
once online when the offline cache is incomplete, then return offline. A failing
test is investigated at its narrowest scope; do not weaken assertions, hide errors,
or extend timeouts to mask ownership problems.

## Artifacts and Notes

Deliver the complete changed source files, benchmark scripts and raw results,
plus a git diff against the checkpoint. Verify archive contents and patch
applicability before linking them. Keep historical numbers explicitly separate
from fresh measurements and explain unrecovered artifacts.

## Interfaces and Dependencies

Use only existing Java, Maven, JUnit, JMH, and native-engine APIs. Ownership remains
explicit; no thread-local ambient owner or validation-disable switch is allowed.
Any experimental transport option affects representation only, not ownership,
automatic strategy selection, correctness checks, or memory limits.

Revision 1: established recovery evidence, sequencing, and the experiment contract.

Revision 2: preserved the successful untouched root build and 282-test reproduction
(10 failures, 55 errors) under `result-coordinate-evidence/initial`. The expanded
first repair pass builds successfully and runs 297 tests with 7 failures and 10
errors. Scalar registration after child closure, cyclic constant short circuiting,
entry serialization, and VALUES ownership now pass. Remaining recipe accounting,
wrapper reuse, group producer ownership, and replay invocation fixes are in progress.
The ordinary-reuse switch and two compact output-window representations are being
implemented independently; no timing starts before correctness verification.

Revision 3: added dense/off/changed-record/primitive-selection switches with identical
ownership and AUTO planning. Seven independent representation cases pass via isolated
JDK25 compilation (formal Surefire verification pending). An exploratory broad run
was stopped after 153 class reports when a null-source fixture in
NativeAliasJoinMemoMultiplicityTest leaked a tiny queryMemoryOverride into later
tests; contaminated errors are not independent regression evidence. Fixed explicit
standalone kernel fixtures and guaranteed override cleanup before the clean rerun.
Shared recipe accounting now admits temporary capture scratch and charges active
queries by recipe identity and frozen catalog identity, with preparation leases.
The immutable idle prepared algebra remains outside the query-ledger lifetime;
footprints are conservative scaled estimates, not measured retained heap sizes.

Revision 4: ownership-02 built successfully and ran 1,131 tests with 4 failures and 22
errors. All seven compact-representation cases and the original scalar, entry
serialization, VALUES, group, cyclic-constant, and materializer cases passed.
The remaining concrete failures led to literal entry-witness validation, explicit
invocation fixture migrations, and a compiled-wrapper context correction. A new
fixed-layout caller regression reproduced private-name loss and passes with the
ordinary compiled-binding context wrapper. Unsupported compound EXISTS inputs now
have a generic semantic fallback using the current immutable outer mapping.

The ownership-03 dependency/module install passed. Its first broad run completed 18
classes and exposed projection carrier loss in ORDER BY; projection and
multi-projection forwarding fixes have isolated before/after evidence. The next
class performs a 1,014-query telemetry census and was stopped deliberately, without
a hang or failure claim. A separate 381-class native evaluation/codegen run uses
the same frozen bytecode to reach relevant ownership and strategy fixtures. Only
classes completed in each run's own log count; older XML in target is not evidence
for a new run. Final timing remains blocked on the ownership gate.

Revision 5: the evaluation/codegen run completed 4,539 tests with 112 failures and 433
errors. A ReplayWorkerContext fixture failed before its restoration guard and
left parallelAggregation forced for later classes. That guard now covers setup,
and its wrapper forwards value-origin and scope metadata. Late forced-route
failures require a clean rerun rather than independent production fixes.
Production repairs additionally detach both native fragment emission paths,
preserve invocation carriers through projection, restore metadata-only generic
root explanation, and bound compiled recipe descriptions without eagerly rendering
large plan strings. Existing and new reproductions passed in isolated overlays.
The ownership-04 run freezes all source edits, performs a fresh root install, then
runs the complete native test selection except the separately documented expensive
telemetry census. No performance claim uses the earlier functional harness smoke.

Revision 6: the root quick install passed in 119 seconds. Quick-profile formatting
is skipped, so an explicit copyright/formatter gate is still required before final
delivery. The broad ownership-04 test run uses immutable compiled outputs and saved
source hashes. It additionally exposed an ownerless wildcard cancellation helper
and an accumulation observer that expected a source-backed fragment view. Both full
classes pass after narrow fixture repairs (57 and 18 cases); the accumulation test
still checks shared authority/budget and now verifies detached projection input.

The compact SELECTION arm is specifically per-cell indirection into changed
five-word records. To address primitive source-row selections directly, a separate
ROWS arm groups output columns with identical group/parent/position/namespace,
stores four-word source-row records, and shares one integer selection per group
and emitted row. Exact grouping and tuple-column checks can withdraw optional
transport; ordinary reuse then remains available. This prototype changes only the
bounded output window, leaving upstream registers, frames, and sidecars unchanged.
Its initial 15 cases plus 17 existing representation cases pass in isolation. Final
mode gates and timing will use the same newly frozen binary, with group/column
counts, buffered-row participation, and withdrawal diagnostics. The harness now
compares route/tier frequency histograms, so differing mixtures cannot masquerade
as matched execution merely because the sets of observed routes agree.

Revision 7: explicit copyright and formatting gates passed. The final fresh batches
ran 2,706 cases: 2,705 passed and one direct-execution fixture lacked an invocation.
That fixture was repaired without changing assertions; a fresh formatted module
install and official 10-case follow-up passed (five domain-failure and five benchmark
fixture cases). The four additional transport gates passed: OFF 84, CHANGES 90,
SELECTION 90, ROWS 90, including interpreted and compiled IR.

The broad ownership-04 run remains incomplete after 5,318 cases because its reused
2 GiB JVM exhausted heap. Its last class passes all five cases in a fresh 2 GiB JVM;
this does not establish the cause or rule out accumulated retention. All 37 reported
failures/errors were repaired, and the fresh batches cover changed tests and the
67 classes that broad run did not complete. The expensive telemetry census remains
unverified. These limitations are retained alongside the successful focused gates.

Source and benchmark drivers are now fingerprinted, and the actual successful
Surefire classpath is copied into a frozen experiment directory. Controlled timings
start only after builds and test JVMs stop. The first ablation uses four paired JVM
forks, alternating OFF/DENSE order, three warmups and five measurements, with AUTO
selection, a matched 1 GiB G1 heap, checksums, actual routes and ledger cleanup.

Revision 8: the first ablation completed eight fresh JVM forks and all 64 queries
were valid, including all 40 measurements. Every measured execution selected
compiled AUTO irKernel/normal; there were no route differences, transport refusals,
or nonzero post-close ledgers. DENSE/OFF median paired latency is 1.30291 (range
1.25470–1.40907), while allocation is 0.837446. The last-three-measurement sensitivity
gives latency 1.29989. OFF/DENSE descriptive median-of-fork-medians are 4.45974/5.85896
seconds and 1.630707/1.365660 decimal GB allocated per query. The longer-warmup
four-arm comparison uses five warmups and five measurements with four balanced
forks, and keeps the frozen binaries unchanged.

The workload screen will use two forks per non-default case, three warmups and
three measurements, with OFF/DENSE then DENSE/OFF order within each case. It is
exploratory; any crossover or route/tier instability needed for a conclusion will
receive a separate uniform confirmation. After all timing ends, a proposed
484-class broad gate uses the module's normal 4 GiB test heap, one reused process,
GC logging and heap-dump-on-OOM. This can strengthen final coverage without
claiming that the previous 2 GiB OOM cause has been diagnosed.

Revision 9: all 56 sweep forks completed, with all 336 queries valid and zero
post-close ledgers. Full-consumption cases use matched compiled AUTO routes;
width 1 opens no output-coordinate carrier. The 1 MiB ledger case admits dense
transport, peaks at 1,047,616 bytes, and completes correctly. The three short
cases with mixed initial tiers received a separate 32-warmup/7-measurement,
four-pair confirmation: all 936 queries are valid and all measured tiers match,
but latency remains variable across forks. A final prespecified extension uses
512 warmups and 101 measurements for those same cases; no further extensions
will follow if variation remains. Original protocols are preserved separately.

The fresh-parent comparison completed eight forks / 80 valid queries. Current
OFF/parent median paired latency is 0.930083, with individual ratios from
0.907870 to 1.018019; allocation is 0.435725. All measured routes are compiled
and matched. This is a cross-source reconstruction, not historical replication
or an isolated attribution to weak reuse. Raw ledger peaks are 0 versus 2,432
bytes and have different accounting scopes.

After the final short-query extension, stop timing and run the saved 484-class
normal-heap broad plan. Then finalize the report, whose four pending markers
cover sweep, parent, final verification, and recommendation. The prepared
packager will verify complete source entries, a temporary-index patch replay,
and evidence archive hashes. Both archives extract under rdf4j/. The original
Hugo theme submodule is empty at pin 85d93d0e4a64c04809fc6cc17af10d102c988ca6.
Try the current official GitLab repository after timing; if exact source access
fails, record it explicitly as a pinned external dependency and describe the
source archive as complete superproject source, not fully vendored dependencies.

Revision 10: all timing is complete: 136 fresh JVMs and 16,288 valid query
records across six separately analyzed protocols. The final short extension
still has cross-pair latency ranges crossing parity; its prespecified stop rule
is honored. Every timed coordinate representation increases default full-query
latency; compact output windows do not close the gap. All post-close ledgers
are zero. The exact theme submodule was fetched from the current official
repository and verified at its original pin; strict source packaging is possible.

The normal 4 GiB broad gate completed all 484 selected classes and 6,481 cases,
with no assertion failures, two missing-invocation fixture errors, and twelve
pre-existing disabled/opt-in cases. Both errors are in the cancellation cases
of LmdbNodePredicateRouteTest and are being repaired without production changes.
The benchmark source manifest remains immutable; final test-only differences
will receive a separate delivery provenance record.

Revision 11: the first normal 4 GiB broad run completed 6,481 cases with two
missing-session fixture errors. Their exact two-hunk repair passed the full
12-case class and a formatter-enabled official install. The next broad run
completed the same 484 classes / 6,481 cases with one assertion failure and no
errors: a VALUES query produced correct rows but its root was hosted generically.
Isolated traces proved that startup term-index CLEANUP temporarily nulls the
lookup generation during a derived-index writer. The strict native authority
guard correctly declines then; a native interior VALUES child explains the
otherwise confusing compilation counter. This was a fixture readiness race,
not an ownership-proof failure. The fixture now waits for existing post-ready
and index-completion barriers, preserving every result and route assertion.
Fifty repeated full classes passed (1,150 cases), and the official 23-case run
passed after copyright, formatting, and module installation.

The final source identity is 8c59454811c9927d79739c1780fa88238c4d613bc0c111cf8d4cde8feeb49059.
Only those two fixture sources differ from the benchmark's 2,057-file set; all
634 production sources/resources, both benchmark drivers, and all 2,780 rebuilt
production artifact files are byte-identical to the measured snapshot. A final
same-selection 4 GiB run is in progress under normal-heap-verified; its manifests
are copied into the run directory. Historical identities and unsuccessful gates
remain preserved. The two measured runtime snapshots have now been archived,
read back, and verified; fresh extraction and classpath rebasing passed for all
252 ordered artifact entries without rewriting original metadata.

Revision 12: the final selected gate passes all 484 classes / 6,481 cases with
zero failures/errors and twelve pre-existing skips. All source hashes remain
unchanged. The earlier 2 GiB OOM and excluded/disabled strategy coverage remain
explicit limitations.

The proven startup fallback exposed a concrete observation gap in the original
benchmark schema: native selected-open histograms and COMPILED counts do not
distinguish a native root from a generic host with one native child. Preserve
all original observations and qualify their route interpretation. One separately
prespecified four-arm confirmation will use the immutable original timed harness
with an external launcher that reads monotonic hosted/hard-decline counters only
after main returns. Four balanced rounds, five warmups/five measurements and
unchanged 1 GiB G1/AUTO settings retain the previous compact protocol. Zero
hosted totals qualify the new forks only; they will not be backfilled into old
samples. This is a response to missing route evidence, not another short-query
warmup extension. No production or original benchmark-driver source changes.

Revision 13: the root-qualified confirmation completed all sixteen fresh JVMs and
160 queries without exclusions or replacements. Every absolute HOSTED_GENERIC
counter was zero, with ten semantic compilations matching one per query. All
eighty measurements used matching compiled AUTO native-operator signatures and
passed full checksums and zero-ledger endpoint checks. The unchanged runtime and
protocol passed before/after verification. The hard-decline sentinel has no
production mutation, so its zero is consistency-only; neither it nor zero hosts
certifies every generic interior operator or classifies earlier experiments.

Primary paired ratios versus OFF are DENSE 1.335632, CHANGES 1.408327, and ROWS
1.434418. All candidate/OFF pairs are slower; the DENSE range is 1.173093–1.528292.
The allocation ratios are approximately 0.8374, with one DENSE JVM at 0.809214.
Its lower allocation level and changing measured durations are preserved without
causal attribution. Direct CHANGES/DENSE and ROWS/DENSE medians are 1.055339 and
1.063471, but individual paired ranges cross parity. The prespecified last-three
sensitivity retains OFF's advantage without settling the compact ranking. ROWS
participates in every output window but has G/W=1. No additional timing run is
planned or needed for this conclusion.

Across seven separate protocols, the final inventory contains 152 fresh JVMs and
16,448 validated queries (13,448 warmups and 3,000 measurements). The original six
protocols' 25 numerical comparisons are unchanged, and their CSV/count files are
preserved byte-for-byte. Three new rows make 28 comparisons without pooling
protocols or double-counting shared baselines. The final pre-delivery check
reverified all 2,057 core source/driver files, original branch/HEAD, and clean
tracked whitespace. Source identity remains 8c594548...; production remains
identical to the frozen benchmark. Reports now distinguish this clean selected
gate (6,469 passes, twelve skips) from repository-wide coverage and retain the
unresolved reused-2-GiB OOM and excluded strategy census.

Revision 14: all experiment and runtime-packaging writers are stopped. The base
runtime archive is verified at SHA-256
645e53e52b1e515c0a95bf3c8224a06285c5baa3aba92d9cc30cafa99323a422.
The final root-launcher add-on is verified at
fa08c60d65f1e174a3eb762692a7bfe8fdae35b6a464599d86f839c8edad2597.
Fresh extraction and the portable helper validate all 127 entries in exact order
and leave all 37 original metadata files byte-identical. A helper-only top-level
archive-directory validation bug found during extraction was corrected; the
successful v2 artifact is the delivery, and the first attempt remains preserved.

Strict source selection includes 19,058 entries, including the exact initialized
theme dependency, with zero source omissions. Source and evidence are assembled
under the common rdf4j archive prefix. This is the final in-repository delivery
checkpoint: the packager must replay the patch to an equal private Git tree,
read back both archives against per-entry hashes, verify unchanged inputs and
real indexes, and publish without overwrite. Its generated package-summary.json
and SHA256SUMS are the authoritative completion receipt for those publication
checks. No more tests, benchmarks, production changes, commits, or pushes are
part of this task.

Revision 15 (2026-11-15): the user requested continuation after the completed
delivery. Start a separate materializer optimization phase under
result-coordinate-evidence/materializer-followup/. The preserved 8c594548 source
snapshot and 34767935 measured runtime remain immutable controls. Recovery owns
initial serial profile JVMs; other agents inspect materializer invariants and
transport-ablation boundaries without concurrent builds or tests. Profiled times
are diagnostic and must not be pooled with unprofiled performance results.

The first milestone is a sampled CPU/consumption profile for frozen OFF and DENSE
with the existing full-result checks and post-main root audit. Use the installed
JDK's recording tools or an already available profiler; do not add dependencies.
The next milestone will use minimal diagnostic controls or a narrow optimization
to distinguish work that is redundant from work required to admit a new accepted
factor member. A correctness/performance claim must preserve source/generation
authority, mutation witnesses, weak references, namespace retirement, per-query
admission, and close/cancellation behavior. Before changing semantics, record a
small failing regression; for a strictly behavior-neutral local optimization,
capture pre/post passing evidence from the same directly exercising tests.

Revision 16 (2026-11-15): the two frozen profiles completed with all twenty query
checksums, compiled roots, and query-end ledgers valid. Proceed with two separately
frozen local performance candidates: first remove memo-to-scratch copies and
redundant cached-page/witness comparisons while maintaining page-key association;
second simplify only the kernel cursor's conservative replay-eligibility hint.
Both live coordinate-validity checks, final view validation, weak mutation
witnesses, locks, retirement, and memory admission remain unchanged. Existing
tests directly cover the edited paths. A new characterization additionally rejects
and republishes a mutated factor-page value; it must pass on the original code and
each candidate. Run the matching eight-class selection with
`python3 result-coordinate-evidence/materializer-followup/run-focused-validation.py
<new-stage>`; the runner checks copyright, formats, installs dependencies and the
module, then saves exact commands, source hashes, and fresh test reports.

The prospective performance comparison uses original OFF, original DENSE,
Candidate 1 DENSE, and Candidate 1+2 DENSE in four balanced rotations. Fix eight
warmups and five measurements per JVM before collecting timings: the diagnostic
recordings observed late transport compilation in the sixth query, so eight
warmups avoid deliberately placing that known transition in the measured block.
All five measured queries remain in the primary calculation, with no favorable
tail selection. The recovery agent will preserve a protocol before running any
candidate. Original runtimes and all first-phase outputs remain unchanged.

After the targeted gates, freeze the candidate runtime and compare it to the
immutable earlier runtime with matched inputs, fixed 1 GiB G1 heap, normal AUTO,
fresh serial JVMs, balanced ordering, checksum/ledger/root/tier validation, and
recorded source/classpath digests. Preserve inconclusive or negative candidates.
Deliver complete updated sources and a full checkpoint-based patch alongside a
small delta from the previous delivery; publish to a new directory so the first
delivery stays reviewable. No commit or push is authorized by this continuation.

Revision 17 (2026-11-15): the original code, Candidate 1, and Candidate 1+2 each
passed the identical eight-class, 94-case selection with identical test sources.
The original rebuild's 2,780 production artifacts matched the frozen measured
runtime exactly. Additional Candidate 1+2 mode gates passed OFF 84 cases and
CHANGES, SELECTION, and ROWS 90 cases each; these selections overlap and their
counts are not unique coverage. All gates had zero failures, errors, or skips.

Candidate 1's frozen source-map identity is 43359e40bd15cb80769bbea1672483202e1bd8d67c89df8889f618cd2ab799c1
and its production tree is fd3660f7a7ee9e42b7e382dda7e7ea624f189d989242da332e591f319b4068dc.
Candidate 1+2's corresponding identities are
87ef1e586e18172afe76564a3bef6b7b1f6149ca681a32c11d584895aa36607b
and 7965ccd5506514774b8943d2c4ec940b79a24cad2568c29e98e9bffd1d38cc41.
Only three materializer class files differ in Candidate 1; Candidate 1+2 retains
those exact files and adds only the KernelRowCursor class difference. Each runtime
retains the other 126 original classpath artifacts, including the original test
harness and root audit. The candidates allocate no additional retained arrays or
reservations, and all ownership/lifetime checks remain in place.

Two pre-timing provenance checks caught and resolved metadata issues. The first
freezer used an older source manifest while naming the measured source identity;
the exact measured manifest is final-validation/benchmark-source-hashes.json,
whose file digest is 34767935. Both manifests have identical production sources.
The original Candidate 1 metadata and executed helper versions remain preserved;
the active corrected descriptor is in runtimes/candidate1-metadata-corrected.
The source and compiled bytes did not change. Separately, mode installs changed
the generated maven-config.properties formatter.skip flag. Regenerating resources
with the original false setting restored exact original bytes before the accepted
Candidate 1+2 freeze. No unexpected executable differences were admitted.

The comparison is now authorized to run serially. Use the recorded Williams
orders, which put each arm in each position once and each directed adjacent pair
once. Eight warmups and five measured queries in each of sixteen JVMs give 208
queries. A consistent gain requires all four matched ratios to favor a candidate,
with result, route, resolver-call, and ledger evidence preserved. Always report
the median and complete range; a range crossing one is inconclusive. Do not drop
tails, repeat arms, or add rounds in response to timings. Prefer the least complex
variant with a supported gain. No builds, tests, compression, large hashing, or
profile cleanup may overlap timed JVMs.

The downloadable profiles will omit initial environment-variable and system-
property events because the original JFR configuration recorded those fields.
After timing, create separately named sanitized recordings with the installed
JDK tools, preserve the originals, and verify retained performance-event equality
and absence of discovered sensitive values. Package only sanitized recordings,
with original/sanitized digests and explicit transformation instructions. Derived
large event JSONL expansions may be omitted from downloads if their exact
extractor, input recording, and regeneration instructions are included; keep all
on-disk evidence and record the omission inventory.

Revision 18 (2026-11-15): the complete fixed comparison and independent raw-data
audit passed. Every one of 208 query records has the expected 1,152,000 rows and
checksum 3123987218479850048. All eighty measured queries selected normal compiled
ROW/irKernel execution; the warmups contained thirty-two interpreted and ninety-six
compiled binds. Every process had zero hosted generic outer roots and thirteen
semantic native compiles. Query ledger endpoints were zero throughout. Enabled
arms retained 1,147 resolver calls and 2,556,992-byte peaks; OFF retained 2,770,101
calls and a 2,432-byte peak. Before/after runtime and protocol identities matched.

The comparison did not establish a latency improvement for either candidate.
The favorable incremental Candidate 1+2/Candidate 1 median ratio, 0.942030,
still had a four-pair range crossing one (0.910700–1.008146). A post-hoc last-three
sensitivity was documented by the independent auditor but was not used to replace
the prespecified five-query medians. No sample or fork was replaced and no timing
extension occurred. Source files were restored only from their preserved
pre-follow-up images, keeping all prior ownership repairs. The source decision and
successful final gate are recorded in materializer-followup/production-selection.json
and validation/selected-original/final-selection-verification.json. The only active
module source difference from the previous delivered source is
NativeResultMaterializerTest.factorPageMutationRejectsOldWitnessAndRepublishesReplacement.

Both separately sanitized JFR recordings passed count and complete retained-field
digest checks. Fresh extraction reproduced byte-identical selected performance
event streams. Recomputed analysis matched all substantive fields; equal-weight
top-N list order/cutoff choices remain explicitly qualified. The original raw
recordings are excluded from downloads. One exact-key boolean feature flag caused
an initial sensitive-name false positive; its event is still removed, and only a
boolean-like value under that exact key is excluded from credential-value probes.
All real-sensitive-value and retained-event guards remain enabled. The disclosure
and regeneration receipts are under materializer-followup/delivery-profiles/.

Prepare three fresh publication directories: result-coordinate-materializer-deliverables
for complete source/evidence and the full checkpoint patch;
result-coordinate-materializer-deltas for the complete prior-delivery delta and
the one-test code delta; and ../coordinate-materializer-runtime-delivery for a
self-contained comparison runtime bundle. Runtime relocation must be verified by
fresh extraction and a Python-only validate-only pass. The source packager must
verify archive readback and exact checkpoint-patch tree equality; the delta helper
must verify forward and reverse application in private indexes. Their emitted
receipts are the authoritative final publication evidence. Preserve every earlier
archive and all original runtime/source snapshots. No commit or push is authorized.

Revision 19 (2026-11-15): the self-contained runtime archive was published at
../coordinate-materializer-runtime-delivery/rdf4j-materializer-comparison-runtime.tar.gz.
Its 113,017,279 bytes have SHA-256
ef3200acad0160048285da23d36d08caf0df3e48c6bba9a81ef7ce58bc3908fe.
Archive readback verified 13,067 file hashes across 13,272 members, all 129
distinct artifacts and all three ordered 127-entry classpaths. Fresh extraction
validated all 13,066 manifest-described files; the relocation helper and generated
comparison driver's validate-only mode passed without executing Java or a new
benchmark. The generated profile analyzer matches the derivative used in the
completed regeneration proof. Inputs and the published archive remained unchanged.
The small runtime receipts are under materializer-followup/runtime-delivery/.

Freeze source and evidence writers now and execute the prepared closed-selection,
complete-source/evidence packaging, and exact prior-delivery delta workflow. The
source packager's publication receipt must confirm archive readback, full checkpoint
patch replay, stable inputs and unchanged real indexes. The delta receipt must
confirm both forward and reverse application and restrict the active code delta
to the new mutation characterization test. These receipts complete the final
active step; keep them beside the published files, avoiding a self-referential
rewrite of this authenticated source snapshot. Do not run further application
benchmarks or modify the selected production files during publication.
