# Isolate native Value memo growth costs

This ExecPlan follows `.agent/PLANS.md`. Maintain Progress, Surprises & Discoveries, Decision Log, and Outcomes & Retrospective as work proceeds. This is a continuation of the published admission study at `b8d2015ec1b722b71f9a8af9e50cd98558f7882a` on `GH-0000-lmdb-lazy-result-coordinates`.

## Purpose / Big Picture

The experimental native iteration memo reuses lazy RDF Values across projected columns and rows. The preceding study found that a long unique prefix followed by 64 repeating keys recovers 98,240 hits when the first-window cutoff is bypassed, yet grows from 4,096 to 65,536 slots. Its peak tracked metadata reservation reaches 14,549,472 bytes even though the small repeating set already fits before the first growth. This experiment asks whether keeping the initial capacity preserves that reuse and improves query performance. A larger cyclic working set provides a counterexample where growth may be valuable, and an early small repeating set provides a control where neither policy should grow.

The deliverable is a minimal experimental growth selection, passing ownership and growth contracts, exact query qualification, a prospectively frozen comparison with ordinary access, complete source and patches, and a verified HASMAC checkpoint. This is not a production-default change. Timing conclusions must distinguish observed reservation and source-call effects from unproven attribution of elapsed time.

## Progress

- [x] (2026-11-17) Verify inputs and perform required build.
- [x] (2026-11-17) Capture failure before growth policy change.
- [x] (2026-11-17) Implement and qualify fixed growth behavior.
- [x] (2026-11-17) Qualify fixtures and freeze matched comparison.
- [x] (2026-11-17) Preserve interruption and freeze fresh cohort.
- [in_progress] Execute and audit complete measurement cohort.
- [ ] Publish source, evidence and verified findings.

## Surprises & Discoveries

The first growth cohort suffered an execution-control interruption at slot 23, after 22 JVMs had completed and sealed. The tool reported `Unknown process id 13272`; the runner's file lock was free, but its process and completion receipts were absent. The partial sample file contains four warmup queries and no measurements. Its last write stopped near the tool's initial yield. The actual process exit status and root cause are unknown. `comparison-v1/failure.json` permanently rejects the entire cohort, preserves all existing raw files, and contributes no performance estimate. The next cohort must retain the same Java, inputs, schedule and estimators, with no timing-based tuning. Keep one orchestration cell alive through each complete supervised invocation and confirm the process transport before another Java launch.

The independent rejection review verified all 181 raw hashes and all frozen inputs. The runner automatically saved per-process medians/means in the 22 terminal validation files; these were never inspected or used to select the new cohort. No accepted cohort or paired-comparison estimate was computed. This is the intended scope of `performanceEstimatesComputed=false` in the failure receipt, not a claim that the automatic per-process summaries do not exist. The saved console/GC files contain no inspected fatal/OOM/signal marker; no cause or exit code is inferred from that absence.

The preceding study's measured native route was normal AUTO `orderedFactorizedSort`, not either IR execution tier. With eight warmups and five measurements, the inner statement-access arbiter uses the preferred adjacency root for queries 1–8 and samples its LMDB alternative for queries 9–13. The full history must be qualified for every new fixture and arm; short pilots are insufficient. The observed latency also contains substantial blocking work before the first row, before that query can resize its memo. Per-query total, preparation, first-row and remaining-consumption intervals therefore remain separate reported outcomes.

The existing `PhaseMemoQueryFixture` and independent Python oracle already implement a warm-cycle pattern. A larger-set control can use 8,192 bootstrap opportunities cycling over 64 keys followed by 122,880 opportunities cycling over 8,192 keys, preserving the original 32,768 rows, four projected columns and 131,072 opportunities without new data-pattern code.

## Decision Log

Decision (2026-11-17, root): add `rdf4j.lmdb.nativeValueMemo.growth=adaptive|fixed`, default `adaptive`, only after an in-repository contract has failed against the unchanged admission runtime. `fixed` skips growth but retains initial allocation, initial-versus-always admission, source/semantic checks, ledger refusal, weak ownership and close behavior. Rationale: refusing a growth reservation would disable the memo and would not isolate growth.

Decision (2026-11-17, root): use weak retention and always admission in both enabled comparison arms. Ordinary access remains a fresh comparator. Keep ownership integration and dependency bytes identical. Rationale: the previous result identifies a capacity question, not a reason to mix retention changes into it.

Decision (2026-11-17, root): preserve all previous sources, protocols and observations; create new artifacts only under `result-coordinate-evidence/growth-followup`, apart from appending root `initial-evidence.txt`. The root agent owns the exclusive Java slot. Rationale: comparisons require immutable inputs and no concurrent benchmark, build, test or heavy background work.

Decision (2026-11-17, root): retain eight warmups and five measurements, with six balanced rounds across early-cold-hot, late-cold-hot and warm-cycle-8192. Add only four caller CPU/allocation snapshots: before connection work, after the prefix, after consumption and after close/retained reads. Whole-query elapsed time is primary; tail elapsed time, caller CPU and phase allocation are prespecified secondary measurements. Rationale: the previous gap largely preceded growth, so phase-specific observations are more useful than attributing total latency to copies. Caller measurements exclude GC and compiler worker work. Equal late-case reuse is a hypothesis to measure, not a gate for discarding a policy outcome.

## Outcomes & Retrospective

The growth treatment is implemented and has passed focused qualification; the latency cohort has not started. The published admission results are motivation only and will not be pooled with new samples. Recovery verified all 186 frozen input records, covering 7,370 files and 344,058,817 bytes, including all 44 pinned JDK records. Both retained classpaths exist without mutable Maven-cache entries. Both new root builds compiled and installed LMDB, but the reactor failed in unrelated Spring modules on missing Kotlin 2.3.21. The exact online retry recorded Maven Central DNS failure. Full logs and terminal receipts are under `recovery/root-offline-v1` and `recovery/root-online-v1`; this is not a successful full reactor build.

All 2,778 fresh LMDB class files and both resources match the frozen baseline exactly. The smallest growth contract failed before implementation: one test, one failure, no errors or skips, expected capacity 4,096 but observed 8,192. The full twelve-method class then produced exactly the seven missing-feature failures and five passing controls. Both actual Surefire reports and commands were appended to root `initial-evidence.txt`. The formatted contract was sealed before execution at SHA-256 `2893aa7602b98981aa73ad276637f527763fd5472cc2d29f7319dd0adec13f13` and remains unchanged.

Only after those failures were observed, the four implementation sources were copied into this phase. The memo now captures an adaptive/fixed boolean before opening its statistics owner and guards the existing later-boundary growth condition. The initial admission branch and all ownership companions remain unchanged. The seven-class `runtime-v1` changes only the memo and its nested-class bytecode relative to the preceding prefix; every companion remains byte-identical.

All six fresh qualification groups passed: twelve growth contracts, twelve admission contracts, 138 default checks, 47 weak/initial checks, 28 adaptive/always integration and lifetime checks, and 28 fixed/always integration and lifetime checks. Total: 265 executions, zero failures, errors or skips. Both native IR lifetime tiers are covered. Exact commands, copied Surefire XML and case lists are retained under `contracts/validation` and summarized in root `initial-evidence.txt`.

The final seven Java sources pass the exact required header check and copy-only Spotless apply/check. The global copyright scan retains four historical findings in old frozen formatting copies; no new source is implicated. The harness is compiled into `benchmark/runtime-v1` with eight classes. Four caller snapshots partition CPU/allocation exactly, and raw metadata explicitly documents final exhaustion/automatic cleanup, explicit close and the wider all-thread allocation interval.

All nine fresh full-sequence exact pilots passed: 117 queries, identical final runtime bytes, matching per-ordinal normal AUTO routes, exact lexical results, valid phase-counter partitions and zero ledger endpoints. The qualification builder independently rechecked all 265 focused test executions and raw pilot seals. Its final receipt is `benchmark/qualification/final-runtime-v1.json`, SHA-256 `de7cdee686e3bfe8875829e1ea69d04e316f1bf6638a4bba2508da7abc9b17b9`. No pilot timing or allocation estimate was used to select the protocol.

The pilot mechanism observations preserve the intended controls. Early repetition gives both policies 126,944 hits without growth. Late repetition gives both 98,240 hits and 32,832 source calls; fixed remains at 4,096 slots and 606,448 peak tracked memo bytes, while adaptive grows four times to 65,536 slots and 14,549,472 bytes. The larger working set gives adaptive 98,506 hits versus fixed 23,416; the policies are not interchangeable. These are exact-pilot observations, not accepted latency estimates. The frozen cohort and independent qualification audit precede publication and timing.

The empty `comparison-v1` cohort is frozen at protocol SHA-256 `60d231209b9d3a52e88d3ccc262422a90d342d7daacfe13383ec1a93ad209fe9`. Independent qualification-only audit passed, rehashing 188 manifest entries and 7,372 file checks over 344,166,308 bytes. The checker accepted a positive synthetic profile and rejected a deliberately changed ninth-query profile even after every raw seal was recomputed. The complete source archive contains 319 regular files, including 48 Java and 54 Python sources. Both full-feature and growth-only patches passed strict reconstruction and actual disposable Git application; the coordinator independently read every archive payload and matched all four current feature files and both patches. No accepted timing JVM has started. The immutable checkpoint must publish before the first scheduled invocation.

The initial checkpoint was published as `7538840c68014013d3aaf36cb485db63994d7f4d` before comparison-v1 launched. After its interruption, a 40-second non-Java transport probe completed with one execution cell alive through all child waits. The failed slot's files remained byte-identical beyond the original 300-second deadline. Fresh `comparison-v2` is frozen at SHA-256 `2aa846bb18d8c1eb028ca2fc9b90c782ac5446a7bcbcbf0cd94fc893d6539e0d`. Independent comparison found exactly the allowed 218 path/timestamp scalar changes; all code, input identities, options, schedule and estimators are unchanged. Independent qualification-only audit passed again with zero new timing claims. The source archive remains the same complete implementation/harness package; its original protocol is historical, and the accepted cohort, if completed, must be comparison-v2. Publish the recovery checkpoint before its first invocation.

## Context and Orientation

Work from `/workspace/scratch/73b19a6d6f5c/rdf4j`. The production-review files live under `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation`. The current experimental implementation is preserved under `result-coordinate-evidence/admission-followup/implementation-src` and compiled as a seven-class prefix placed before frozen module classes and JARs. This arrangement lets every timing arm share exactly the same executable bytes while selecting one behavior through properties. Copy the four complete implementation sources into the new phase only after the required red evidence.

`NativeDirectValueMemo.find` hashes the full ID and position mask to one slot. Weak entries do not hold a strong path to a Value or its store owner. `adapt` currently checks the first-window admission cutoff at 8,192 probes and, at later boundaries, doubles capacity if cumulative hits are at least 25% and cumulative collisions at least 12.5%. `allocate` reserves the estimated array/reference metadata footprint, including overlap with the old table; a refusal disables the memo. `close` releases the reservation and reports statistics. The growth treatment must change only the later doubling decision.

The query fixture, exact independent oracle, launcher, comparison runner, analyzer and route checker are under `admission-followup/benchmark`, `admission-followup/run_java.py`, and `admission-followup/reviews`. Reuse their source through explicit new copies or recorded read-only imports. Do not silently point a new qualification at an old `qualifiedForTiming` receipt. The new qualification must bind its actual sources, runtime, tests, fixture recipes and complete per-query routes.

## Plan of Work

First verify the retained toolchain and frozen classpaths, then run the required root quick clean install. Preserve all output under a fresh recovery attempt and append the root build log. Missing offline dependencies permit exactly the specified online retry. Do not claim a green reactor merely because LMDB succeeded.

Next create and seal growth contracts under `growth-followup/contracts/test-src`. Run the smallest test against the unchanged admission prefix and save its Surefire failure before editing implementation. Run the full new class to distinguish missing behavior from passing controls. Then copy the current four-file implementation and add a constructor-captured growth selection plus a conditional at the existing growth branch. Unknown selections must fail before a statistics owner opens. Compile only the memo's two emitted classes over the immutable admission prefix, verify unchanged companion bytes, and run focused admission, growth and ownership qualifications including both IR lifetime tiers.

Then prepare three real-query fixtures: existing early-cold-hot; existing late-cold-hot; and warm-cycle-8192 with an 8,192-probe 64-key bootstrap followed by a 122,880-probe 8,192-key tail. All have width four and full lexical consumption. Regenerate independent checksums and insertion-recipe identities. Qualify the complete eight-warmup/five-measurement query history with exact lexical checks for every fixture/arm. Record any failure in a fresh attempt and revise only before freezing timing inputs. The ordinary, adaptive and fixed arms use always admission, weak retention, unchanged source caches, coordinate transport off and normal AUTO selection. The initial proposed finite schedule is all six arm permutations across three fixtures, one fresh JVM per invocation. Final executable details and all expectations must be frozen and checkpointed before the first accepted timing JVM.

Finally execute each finite slot only after the previous process's terminal result and validation have been observed. All measurements enter the predeclared process-median paired estimate and the process-mean sensitivity. Report total latency and remaining consumption separately, with allocation, preparation, observed routes, source calls, memo events, tracked peaks and zero endpoints. Do not discard inconvenient observations or replace failed slots. Independently audit numerical aggregation and mechanism conclusions, package complete source plus exact patches, restore-check all preservation objects and publish the root-selected immutable Git tree.

## Concrete Steps

Use the retained Temurin JDK at `/workspace/scratch/d0a0e51b9386/toolchain/jdk-25.0.5+8` and Maven at `/workspace/scratch/d0a0e51b9386/toolchain/maven-mvnd-1.0.6-linux-amd64/mvn/bin/mvn`, after verifying they exist. The required root build arguments are:

    mvn -B -ntp -Dmaven.compiler.showWarnings=false -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install

Adapt `admission-followup/recovery/run_build.py` to the new recovery directory; it must preserve exit status and full logs without a process timeout shorter than 60 seconds. For tests, adapt `admission-followup/contracts/run_contracts.py` into the new phase. Its isolated Surefire copy preserves module sources and saves actual execution classpaths. Never use `-am` or `-q` for tests. Run each new test class through the copied runner with explicit `--classpath`, `--overlay`, `--source`, `--selection` and `--slot-granted` arguments. Append the exact command and report summary to root `initial-evidence.txt` before changing the memo.

The new timing runner will expose `freeze`, `next` and `audit` operations. `next` must start only one JVM, return its terminal status and reject a changed frozen input. The route/profile checker runs after every process and before latency analysis. Update this section with exact completed commands and artifact paths when those scripts are finalized.

## Validation and Acceptance

The smallest fixed-capacity contract must fail under the unchanged implementation and pass after the change. The adaptive default must reproduce existing growth, while fixed mode must continue reusing values at 4,096 slots without growth reservation attempts. A deliberately constrained ledger must demonstrate that fixed mode is not simulated by adaptive reservation failure. Admission cutoff, source identity, semantic versioning, fresh iteration state and close release must remain correct.

The late fixture's prospective prediction was 98,240 hits and 32,832 source calls under both enabled modes, with four resizes and 14,549,472 peak charged bytes under adaptive versus zero resizes and 606,448 under fixed. All fresh exact pilots reproduced these counts. The larger cyclic control loses hits at fixed capacity; qualify and report that difference rather than treating it as incorrect output. All arms must return identical rows and lexical checksums, correct exposure, documented native routes and zero ledger endpoints. Reject claims of a total heap bound or a physical decoder count from metadata reservations or lazy source calls.

## Idempotence and Recovery

Every build, test, pilot, runtime and cohort attempt uses a fresh directory and refuses overwrite. Preserve existing untracked publication receipts and raw evidence. Store large evidence losslessly only after complete hash verification and verify both fresh and idempotent restoration, including refusal to overwrite different bytes. Compile, format and package before timing; pause all agent-heavy work during a timed JVM. User authorization to commit and push to HASMAC persists; publish without forcing over an unexpected remote head.

## Artifacts and Notes

The preceding complete report is `result-coordinate-evidence/admission-followup/RESULTS.md`. Its source archive and exact patches are historical inputs, not new delivery artifacts. New source packages must contain complete current Java/Python files and all needed helper source, with a manifest and a strict patch-application check. The root build and TDD evidence will be appended to `initial-evidence.txt`.

## Interfaces and Dependencies

No new dependency is needed. Preserve package-private `NativeDirectValueMemo` interfaces and all ownership companions. The sole behavior option is `rdf4j.lmdb.nativeValueMemo.growth=adaptive|fixed`, with `adaptive` as its default. The parent `rdf4j.lmdb.nativeValueMemo.enabled` remains false by default. Java 25, existing Maven/Surefire dependencies, Python standard-library helpers and Git provide the complete experimental toolchain.

Revision note (2026-11-17): Initial plan for the fixed-capacity growth ablation, including a larger working-set counterexample and an unchanged-mechanism control.

Revision note (2026-11-17): Bound the schedule to 54 fresh JVMs and prespecified phase CPU/allocation measurements; recorded complete frozen-input survival and the observed offline dependency failure.

Revision note (2026-11-17): Recorded both terminal build outcomes and advanced to sealed pre-implementation contract execution.

Revision note (2026-11-17): Recorded actual smallest/full red results and exact baseline class equality; added the minimal growth guard after the red gate.

Revision note (2026-11-17): Recorded 265 green executions and the final harness/formatting qualification; advanced to fresh full-sequence exact pilots.

Revision note (2026-11-17): Recorded all nine exact pilots and the independent final runtime qualification, including the larger-set loss of reuse; retained the prospective schedule unchanged.

Revision note (2026-11-17): Sealed the empty 54-JVM cohort, recorded independent qualification and source-delivery readback, and advanced to supervised execution after checkpoint publication.

Revision note (2026-11-17): Preserved a tool/process-control interruption after 22 sealed JVMs and four warmup records in slot 23. Rejected the entire incomplete cohort without computing performance estimates; paused Java launches and moved to transport recovery before an identical fresh cohort.

Revision note (2026-11-17): Passed the continuous-cell transport probe and independent v2 protocol/qualification audits; clarified automatic per-process versus accepted cohort summaries and resumed the execution step pending recovery-checkpoint publication.
