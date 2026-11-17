# Measure phase-sensitive admission of the native Value memo

This ExecPlan is a living document maintained under `.agent/PLANS.md`. The sections Progress, Surprises & Discoveries, Decision Log, and Outcomes & Retrospective must be updated as the work proceeds.

## Purpose / Big Picture

The existing experimental cache reuses individual lazy RDF Values across columns and rows of one native result iteration. Its weak-retention variant avoids keeping Values and decoded payloads alive by itself. The previous study established ownership correctness and measured allocation savings on two highly repetitive queries, but neither query exercised the first-window admission cutoff or meaningful changes in repetition. This study measures whether that cutoff sacrifices useful later reuse, whether an initially useful cache remains costly after repetition stops, and where collisions and garbage collection limit the opportunity.

The user will receive reproducible query fixtures, complete source and a patch, correctness and cleanup evidence, and matched fresh-process measurements. The existing implementation is the control. A narrowly scoped, explicitly experimental `always` admission mode will disable only the one-time low-hit cutoff, leaving key comparison, ownership, collisions, table growth, source caches, and normal AUTO execution selection unchanged. This is a diagnostic ablation: it measures the opportunity and cost of reconsidering admission, and does not select a production policy in advance.

## Progress

- [x] (2026-11-17 10:45Z) Verify checkpoint and recover surviving artifacts.
- [x] (2026-11-17 10:52Z) Restore toolchain and perform root installs.
- [x] (2026-11-17 10:54Z) Capture failing contract before minimal treatment.
- [x] (2026-11-17 11:24Z) Qualify final fixtures and GC routes.
- [in_progress] Freeze protocol and run matched comparisons.
- [ ] Audit evidence and publish complete checkpoint.

## Surprises & Discoveries

The previous Git checkpoint, `47e6eba9bd1b238ea84c1b9ad4cd8cdf28caebaf`, and the frozen runtime classes survived workspace maintenance, but its Temurin JDK directory did not. A Maven 3.9.16 installation and a Zulu Java 25 installation survived. Recovery searched Library first, then restored the exact original Temurin and Maven distributions from official distribution sources. All 44 pinned JDK input records match the previous study. No old timing result will be pooled with this study.

The first admission check requires 2,048 hits among the first 8,192 probes. A probe is a lookup of a complete `(value ID, position mask)` key in the iteration memo after same-row reuse has been considered. For C unique keys followed by uninterrupted repetitions of the last of those keys, C=6,144 survives and C=6,145 fails, provided that last Value remains live. The prefix can collide; only the immediately repeated final entry matters. Therefore a sweep only around the first repetition at probe 8,192 would reject both sides and would miss the relevant boundary.

The current policy only disables on low hit rate at its first boundary. At subsequent boundaries it can grow based on cumulative hits and collisions, but never disables for a later low hit rate. A useful initial phase followed by unique values is a separate necessary control.

## Decision Log

Decision (2026-11-17, root): isolate the low-hit cutoff with an `initial|always` admission option before designing a periodic policy. The experiment will not add an eviction queue, a frequency sketch, or a new runtime dependency. Rationale: the user asks what is worth testing next, and the evidence must quantify the opportunity and cost before introducing a more elaborate policy.

Decision (2026-11-17, root): use an ordered native star query with an unprojected unique row key to make result phase order observable. The query projects only payload columns. Rationale: insertion order is not a SPARQL result-order guarantee, and a projected ordering key would create unrelated memo probes and possible hits. Qualification must confirm that native sorting retains IDs and that the final projection produces the intended probe stream; otherwise revise the fixture before timing.

Decision (2026-11-17, root): preserve all existing study paths, failed attempts, and protocols. New work is additive under `result-coordinate-evidence/admission-followup`. The root agent owns the Java process slot and will launch no concurrent build, test, profiler, or timing JVM. Rationale: prior evidence must remain reproducible and controls must not compete for resources.

## Outcomes & Retrospective

The cutoff-only ablation is implemented in an isolated experimental source/runtime prefix. There are 225 passing focused executions: 12 new admission contracts, 138 default checks, 47 weak/initial checks, and 28 weak/always ownership/integration checks. Both IR lifetime tiers are covered. Final memo runtime-v2 is byte-identical in all seven prefix classes to the qualified runtime-v1, and all 2,778 fresh root LMDB class files match the frozen baseline. The previous weak-retention report remains `result-coordinate-evidence/retention-followup/RESULTS.md`; its timing samples are historical context only.

Toolchain recovery restored the exact original Temurin and Maven distributions at their original paths. All 44 pinned JDK inputs match the prior protocol. The mandatory root offline build and its exact online retry both compiled and installed LMDB; both failed in unrelated Spring modules on missing Kotlin 2.3.21, with DNS failure in the online retry. Full build logs and terminal status are preserved under recovery/root-offline-v1 and recovery/root-online-v1. The ordered query path uses an ArrayList of results, which can retain consumed binding sets until close. It is suitable for deterministic admission measurement, but cannot establish weak collection of otherwise unowned result Values. Natural-GC work will therefore use a separately qualified source/consumer route.

The sealed admission test class ran against the unchanged retention-v2 prefix. Smallest red: `Tests run: 1, Failures: 1, Errors: 0, Skipped: 0`; full red: `Tests run: 12, Failures: 3, Errors: 0, Skipped: 0`. Only the three cases demanding `always` admission failed; the nine controls passed. Reports are in `contracts/validation/red-small-v1` and `red-all-v1`, and compact evidence was appended to root `initial-evidence.txt` before editing the implementation.

The untimed trace study has completed three JVMs and 30 cases, covering 3,932,160 requests. An independent Python model matched all 510 checkpoint states, every checksum group and all close ledgers. Cold cycling over 4,096 IDs is cut off after 1,515 first-window hits, whereas `always` produces 106,410 total hits and reaches 65,536 slots. For 8,192 cyclic IDs, removing cutoff alone leaves capacity at 4,096 and yields 12.93% hits; an admitted bootstrap permits much more growth. These are synthetic-ID, consumer-rooted mechanism results, not query performance measurements.

The short real-query trace qualified all 12,288 rows and 49,152 projected bindings across three queries. An independent direct-map replay exactly matched every per-row source/materialization change. AUTO selected `orderedFactorizedSort`, a native non-IR route, so the timing protocol binds that observed route without forcing a strategy. Both IR tiers remain independently covered by contracts. All nine final full-size exact-oracle pilot JVMs and their 27 queries passed independent checksum, exposure, ownership, route and ledger validation. No pilot timing observation contributes to the primary dataset.

All ten natural-GC diagnostic JVMs finished successfully without explicit collection. They used the real ValueStore and default source caches, with 16 rounds of allocation pressure and 136 requests per process. Small 1,024-character values remained source-cached: weak mode made eight source calls and had 128 subsequent identity hits. At 1,048,577 characters, weak mode without a retained consumer made 136 source calls and recorded 128 collected-slot misses; retaining one value gave 16 identity hits and 112 collected-slot misses. Strong mode made eight source calls in both sizes. Every query and manager ledger ended at zero. These source calls and cold initialization observations are not physical decode counts, and this diagnostic is not a latency comparison. The independent audit passed all 190 records, 170 rounds and 1,360 requests, regenerated both lexical checksums and verified 7,300 input files. Collection can occur during later consumption, so the evidence does not attribute every clearing event to the earlier pressure allocation phase.

The prospective comparison is frozen at `comparison-v1/protocol.json`, SHA-256 `8a2c2fd92799ae667965cf3bb1af86b12d47d4e6af6ce83f3ad13134318b6949`. It schedules 54 fresh JVMs, each with eight warmups and five measurements, across three fixtures and three arms with all six arm permutations and balanced fixture positions. The exact code and protocol will be checkpointed before the first timing JVM. All five measured queries and all six process pairs enter the prespecified median and mean-sensitivity estimates.

## Context and Orientation

Work from `/workspace/scratch/73b19a6d6f5c/rdf4j` on branch `GH-0000-lmdb-lazy-result-coordinates`. The native engine implementation lives under `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation`. Experiments use isolated compiled class directories placed before frozen module JARs on the Java classpath, so all comparison arms share the same dependency bytes. The existing direct memo implementation is preserved at `result-coordinate-evidence/retention-followup/implementation-src/org/eclipse/rdf4j/sail/lmdb/evaluation/NativeDirectValueMemo.java`; its companion materializer, projected binding set, statistics, launcher, contract runners, and runtime identity scripts are in the same study directory. Read those files before creating new copies, and never modify a frozen old file to accommodate a new runtime.

The current memo allocates 4,096 slots on the first probe. It hashes the complete ID and position mask to one slot, replaces conflicting keys, validates the Value's semantic version and source identity on hits, and never explicitly initializes a lazy Value on a miss. In weak mode it holds a WeakReference to the Value and a weak owner witness; consumer or source-owned Values may still stay alive. The query memory ledger reserves arrays and conservative table-owned reference metadata, and releases the reservation on disable or close. A rejected ledger reservation permanently disables the memo for that iteration. At 8,192 probes a cumulative hit rate below 25% permanently disables it. Later 8,192-probe boundaries can double capacity up to 65,536 when cumulative hits are at least 25% and collisions at least 12.5%.

The existing feature property `rdf4j.lmdb.nativeValueMemo.enabled` is false by default; retention is selected by `rdf4j.lmdb.nativeValueMemo.retention=strong|weak`. Add `rdf4j.lmdb.nativeValueMemo.admission=initial|always` only in the new experimental implementation copy after the smallest in-repository contract has actually failed under Surefire. Missing admission selection means `initial`, so previous behavior remains the default. `always` must bypass only the first low-hit cutoff. It must still disable for unstable source identity, refused initial or growth reservation, and closure.

## Plan of Work

Milestone 1 restores and identifies the toolchain, performs the mandatory root quick install, and adds a deterministic phase harness plus diagnostic trace tests. The harness will generate the same database for all arms, order rows by a unique fixed-width row IRI, and project only object-position Values. It will calculate expected output independently from the generated sequence, validate every observed row outside timed oracles where appropriate, and record checksums in every measured query. Early-close or retained-consumer variants, if included, need explicit endpoint and readability checks. Large-payload and natural-GC diagnostics are separate from the principal latency cohort and must not call explicit GC during latency measurements.

Milestone 2 creates admission contracts under `contracts/test-src`, observes and saves the smallest expected failure, and only then copies and edits the memo implementation. Use the repository's isolated Surefire pattern to keep source and compiled inputs explicit. The minimum contract set covers the exact initial threshold, late repetition, an initially hot then unique sequence, default equivalence, initial and growth reservation refusal, fresh contexts, and cleanup. Existing ownership contracts and both native intermediate-representation tiers must remain green. An intermediate representation is the native engine's executable query form; interpreted and compiled forms must obey the same Value lifetime rules.

Milestone 3 qualifies the actual query route and probe opportunities, then freezes a finite comparison protocol. Arms are ordinary lazy Values with memo disabled, the current weak memo with `initial` admission, and weak memo with `always` admission. Match input, JDK, heap, GC, query/global budgets, source caches, result consumer, coordinate transport setting, warmups, and measured iterations. Use normal AUTO native selection. Alternate or balance process order, one fresh JVM per invocation, and record exit status before launching the next process. Do not replace unfavorable samples. Frozen input and runtime identities must include source/class hashes, JDK identity, harness identity, and resolved classpath hashes.

Milestone 4 reports latency, thread allocation, preparation cost, ledger peaks and zero endpoints, memo hits/misses/collisions/growth/admission statistics, actual execution route, and result checksums. Analyze per-process observations first and paired ratios second; do not treat repeated queries within one JVM as independent process replicates. Natural-GC clearing counts must not be equated with extra useful-key misses without matching source calls and hit counts. Preserve failed and excluded qualification attempts with reasons. Deliver a complete source archive, a verified patch with only the implementation diff, and a HASMAC checkpoint.

## Concrete Steps

From the repository root, after restoring Java and Maven, record their paths and file hashes in `recovery/toolchain-v1.json`. Run the required sanity build with a process lifetime of at least 60 seconds and save all output to root `maven-build.log` and a distinct recovery log:

    mvn -B -ntp -Dmaven.compiler.showWarnings=false -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install

If dependencies are absent, retry exactly once without `-o` and then return offline. If the failure is unrelated to dependency resolution, retry without `-T 1C`. The previous checkpoint had an unrelated missing Spring Kotlin 2.3.21 dependency; do not claim a full reactor success unless observed. Do not run tests with `-am` or `-q`. The isolated Surefire runner may be adapted from `retention-followup/contracts/run_contracts.py`, using a new attempt directory and the selected current runtime paths. Append initial failing evidence to root `initial-evidence.txt` without erasing older entries.

The primary protocol was frozen with `benchmark/run_comparison.py freeze`, selecting `benchmark/fixture-definitions-v1.json`, `benchmark/runtime-v2/classpath.txt`, and `benchmark/qualification/final-runtime-v1.json`. Frozen source scopes are `implementation-src`, `benchmark/src`, `benchmark/PROTOCOL.md`, `compile_support.py`, and `contracts/test-src`, in addition to the runner's explicit analyzer/oracle/qualification inputs. Mutable reports and reviews are outside these scopes. The runner verifies complete runtime and JDK hashes before and after each Java process.

After the qualified checkpoint is published, execute exactly one slot at a time and observe its successful terminal result before the next invocation:

    python3 result-coordinate-evidence/admission-followup/benchmark/run_comparison.py next --cohort result-coordinate-evidence/admission-followup/comparison-v1 --slot-released

After all 54 slots seal successfully, use the runner's `audit` command and the independently implemented `reviews/audit_admission_cohort.py`. No timing result may be accepted from a failed or incomplete cohort, and no replacement slots are allowed.

## Validation and Acceptance

The exact-boundary contracts must demonstrate that `initial` accepts a 6,144-key cold prefix followed by the already-admitted last key and rejects a 6,145-key prefix, with full key and valid semantic version held constant. The `always` contract must first fail against the original control for genuinely late repetition, then pass after the minimal treatment. Both modes must release all reservations and reject unsafe sources. Verify both native IR tiers through existing explicit-lifetime tests.

Every accepted real-query observation must have its expected row count and checksum, one documented native iteration ownership route or an explicitly justified observed route, no unintended generic fallback, and zero outstanding query and memo ledger claims after close. All comparison processes must end successfully; violations are retained as qualification failures or invalidate the cohort rather than becoming timing samples. Results must distinguish diagnostic opportunity from a recommended production policy. No performance conclusion may be inferred from hit rate or allocation alone.

## Idempotence and Recovery

Every attempt uses a new directory and refuses to overwrite an earlier attempt. Preserve the Git branch and all untracked evidence. The `.m2_repo` cache and prior frozen runtimes are shared read-only inputs during timing; build installation occurs before freezing new inputs. If a process fails, keep its command, output, metadata, and exit status, explain the failure, and revise the protocol only in a new version. Never silently splice replacement observations into a frozen cohort.

The user has authorized commits and pushes to HASMAC-AS/rdf4j. Use the existing GitHub checkpoint helper if local Git push authentication remains unavailable. Verify the remote parent, tree, message, and final branch ref. Local and remote commit hashes can differ due to author metadata; exact tree identity is required.

## Artifacts and Notes

All new evidence belongs under `result-coordinate-evidence/admission-followup`, except append-only root evidence and the required root build log. Existing publication receipts under `retention-followup/recovery/github-checkpoint-v2` are preserved and may enter the next checkpoint. The final report will link complete source, a strict apply-checked patch, raw measurements, protocol, and independent audit. Archive and patch contents must be read back and verified before offering them for download.

## Interfaces and Dependencies

No new runtime dependency is planned. Use the existing Java 25 engine, Maven cache, JUnit/Surefire, Python standard library for orchestration and analysis, and the existing package-private memo/test interfaces. The phase harness and statistics launcher live in `org.eclipse.rdf4j.sail.lmdb.evaluation` so they can observe the same supported experimental counters as earlier studies. Additive scripts must accept explicit attempt/output directories, fail closed on mismatched hashes or missing evidence, and report concise summaries while preserving full logs.

Revision note (2026-11-17 10:45Z): created this plan after checkpoint verification and the admission-boundary review; runtime recovery and fixture qualification are the sole active step.

Revision note (2026-11-17): corrected the exact-edge explanation to allow cold-prefix collisions, recorded successful exact toolchain recovery, and separated ordered-result retention from natural-GC diagnostics before any timing.

Revision note (2026-11-17 11:24Z): closed the nine final exact pilots and ten natural-GC runs, recorded the prospective 54-JVM protocol and exact reproduction command, and advanced the sole active step to checkpointing and running the comparison.
