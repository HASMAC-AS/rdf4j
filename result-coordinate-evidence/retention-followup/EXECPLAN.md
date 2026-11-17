# Compare strong and weak retention in the direct Value memo

This living ExecPlan follows `.agent/PLANS.md` and Routine D in `AGENTS.md`. Update Progress, Surprises & Discoveries, Decision Log, and Outcomes & Retrospective as work proceeds. Dates below are UTC unless stated otherwise.

## Purpose / Big Picture

The experimental memo reuses lazy RDF values across projected rows in one native query iteration. Earlier measurements showed lower allocation and promising latency, but a strong memo can retain decoded text and revision ownership graphs that its array reservation does not account for. This continuation will establish whether weakening retention releases those graphs without defeating the measured reuse benefit. A reader will be able to run focused ownership contracts, reproduce a separate memory diagnostic, and compare fresh matched query timings with ordinary reuse, strong direct reuse, and weak direct reuse. All variants remain experimental and disabled by default.

## Progress

- [x] (2026-11-17) Restore pushed source and historical evidence.
- [x] (2026-11-17) Recover dependencies and build fresh baseline.
- [x] (2026-11-17) Capture failing weak retention contract evidence.
- [x] (2026-11-17) Implement and qualify minimal weak retention.
- [x] (2026-11-17) Measure ownership and decoded payload retention.
- [in_progress] Freeze and run matched latency comparison.
- [ ] Publish verified source, patch, and findings.

## Surprises & Discoveries

The previous checkout and common benchmark runtime are absent from the current workspace. The Git checkpoint at `4e9af30d660dcc59b2e7148bb7cff00580441f29` preserves source and historical evidence, including the exact six-class direct overlay archive, but not the original common128 classpath. The surviving JDK is Temurin 25.0.5+8. New measurements therefore require a newly frozen runtime and new controls; historical timings will not be pooled.

Both the `Value[]` and `InternalIdentity[]` arrays hold strong references in the existing memo. A weak value reference alone would leave the identity witness holding a revision and possibly its ValueStore, snapshot, and scope. The weak variant must capture primitive identity data and a weak owner witness. The ValueStore also has its own strong decoded-value caches, so actual query heap retention and memo-only reachability must be reported separately.

The saved Maven dependency archive has two parts. Their verified SHA-256 values are `3670b130792d8f164479d474e6950ff2b3f24f59dc9db19b38757f6fee3f4b57` and `76870510dbd7d7ccd629705d60547e6ee6ff549f61f886c90c76de6be16f74b0`. Its contents will seed the workspace-local `.m2_repo`; the fresh source install will replace any snapshot jars needed by this checkout.

## Decision Log

Decision: omit the transport override from the existing coordinate-regression selection. Rationale: the first post-change qualification runner accidentally applied the timing arm's `off` override to tests which require coordinate transport. All 38 observed failures were coordinate assertions, while the 32 direct-table/integration cases passed. Preserve `qualified-default-v1` and correct only the runner configuration in `qualified-default-v2`; do not alter implementation or assertions. Date/author: 2026-11-17, root.

Decision: keep the direct-map key, hash, table capacities, cumulative growth rules, permanent first-boundary cutoff, source checks, and ownership code fixed while changing retention. Rationale: changing admission simultaneously would confound the cost and lifetime comparison. Date/author: 2026-11-17, root.

Decision: preserve all previous protocols and artifacts byte-for-byte and place this work under `result-coordinate-evidence/retention-followup`. Rationale: recovered history is evidence, not a new runtime description. Date/author: 2026-11-17, root.

Decision: require a smallest in-repository failing test before altering the experimental implementation, and preserve Surefire reports for every attempt. Rationale: weak retention changes externally observable object lifetime and must preserve semantic and ownership checks. Date/author: 2026-11-17, root.

Decision: keep explicit-GC reachability diagnostics out of latency measurements. Rationale: a forced collection answers a lifetime question but changes the natural performance of a weak memo. Date/author: 2026-11-17, root.

## Outcomes & Retrospective

Dependency recovery completed with 1,476 JARs. Both required root builds compiled and installed LMDB successfully; the full reactor remains blocked by missing Kotlin 2.3.21 in unrelated Spring modules (the online retry failed DNS). The weak-capable implementation is now qualified by 193 final-runtime tests, a real-source-cache diagnostic, four controlled memory JVMs with 24 passing checkpoints, and two full weak fixture pilots with 26 queries. The memory experiment establishes that weak retention releases the memo-owned graph while respecting consumer roots. The comparative latency study is frozen but has not launched. The prior completed study is recorded in `result-coordinate-evidence/direct-memo-followup/RESULTS.md`; it is context only for this new experiment.

## Context and Orientation

The repository root is `/workspace/scratch/73b19a6d6f5c/rdf4j`. LMDB source is in `core/sail/lmdb`. The ordinary production materializer lives in `src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/NativeResultMaterializer.java` under that module. The experimental direct implementation is separately preserved in `result-coordinate-evidence/direct-memo-followup/contracts/implementation-src/org/eclipse/rdf4j/sail/lmdb/evaluation/NativeDirectValueMemo.java`, with integration sources under `contracts/generated/direct-memo-v2/src`. It is compiled as a classpath prefix: the JVM loads those selected classes ahead of the baseline classes without changing the baseline checkout.

A direct-mapped table hashes the full value ID and full position mask to one slot. A hit requires the same key, a current semantic version, and matching internal ID, revision number, and ValueStore identity. A miss creates a lazy Value through the existing source. The table does not explicitly decode that Value. Decoding later can add a large payload to the same object. The query ledger is explicit reservation accounting; it is not a measurement of JVM heap reachability.

The existing table starts at 4,096 slots and can double to 65,536. At 8,192 probes it permanently disables itself if fewer than one quarter were hits. At later boundaries it doubles when cumulative hit rate is at least one quarter and collisions are at least one eighth. These rules remain unchanged in this study. The strong mode stores Value and InternalIdentity objects; the planned weak mode will retain weak references and primitive identity witnesses. A weak reference permits collection when no strong root retains its referent. A retained row is a legitimate strong root and must keep its Value usable.

## Plan of Work

First recover dependencies and build the unmodified checkout with the required quick profile. Record any root reactor failure and use the smallest supported LMDB build only when an unrelated module blocks the full build. Compile the preserved common harness from its verified source into newly named directories. Never overwrite old frozen descriptors or silently label rebuilt binaries as the old runtime.

Next add a new `NativeDirectValueMemoWeakRetentionTest` in this study's contracts source directory. Set the new experiment property `rdf4j.lmdb.nativeValueMemo.retention=weak` while running the existing strong implementation. At least one test must demonstrate that a memo which remains open retains a dropped value or revision graph before the change. Capture the actual failing Surefire snippet. Live retained values, identity mutations, equivalent identity replacement, ownership handoff, table adaptation, and reservation release must be checked before measurement.

After red evidence, add the weak branch to a copied implementation under this study's implementation directory. Capture the semantic version in the existing primitive array, the revision ID as a primitive, and the owner as a weak reference with a distinct null-owner state. Read each weak Value once into a strong local before validation. Do not weak-reference a bundle whose lifetime could end while a user still retains the Value. Treat a cleared referent as a collected miss, not as a different-key collision or semantic invalidation. Account for table-owned metadata and report any remaining accounting limitation explicitly.

Use the existing integration behavior and both native IR tiers, interpreted and compiled. Then run a bounded isolated memory diagnostic with an uncached synthetic source, controlled decoded payload size, and separately retained rows. Include an actual-query diagnostic with unchanged store cache settings so source-owned retention cannot be mistaken for memo-exclusive retention. Forced collection is allowed only in these diagnostics.

Before timings, freeze exact sources, classpath hashes, JVM flags, fixture inputs, query checksums, measurement counts, arm order, and acceptance rules in a new protocol. Use normal AUTO strategy selection. Run ordinary, strong, and weak arms with identical ownership code and freshly rebuilt runtime; alternate arm order in fresh JVMs. Include query latency, thread allocation, preparation cost, ledger peaks, actual routes, source calls, memo counters, and result checksums. Each timing subprocess must be individually supervised to completion. Do not rerun a failed fork or replace an unfavorable sample. Interpret small cohorts as engineering evidence with observed spread, not as universal proof.

## Concrete Steps

Use the following environment for Java commands from the repository root:

    export JAVA_HOME=/workspace/scratch/d0a0e51b9386/toolchain/jdk-25.0.5+8
    export PATH="$JAVA_HOME/bin:/workspace/scratch/d0a0e51b9386/toolchain/maven-mvnd-1.0.6-linux-amd64/mvn/bin:$PATH"

After archive extraction into `.m2_repo`, run the required baseline command with its full output captured in `maven-build.log` and an immutable copy under this study's recovery directory:

    mvn -B -ntp -Dmaven.compiler.showWarnings=false -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install

Use a command lifetime of at least sixty seconds. If the offline build fails for a missing dependency, retry exactly once without `-o`, then return offline. For another failure, retry without `-T 1C` as required by the repository. Do not use `-am` or `-q` while running tests. All Maven calls use `-Dmaven.repo.local=.m2_repo`. Append initial verification evidence to the existing root `initial-evidence.txt` rather than replacing it.

The rebuilt baseline and original direct control are under `recovery/runtime-v1`; the final seven-class weak-capable prefix is under `runtime-v2`. `runtime-v2/qualification.json` binds the accepted post-change test groups, source-cache check, memory diagnostics and full-fixture pilots. The following commands run the accepted selections in fresh attempt names:

    python3 result-coordinate-evidence/retention-followup/contracts/qualify_runtime.py default --runtime result-coordinate-evidence/retention-followup/runtime-v2 --attempt reproduction-default-v1 --slot-granted
    python3 result-coordinate-evidence/retention-followup/contracts/qualify_runtime.py weak --runtime result-coordinate-evidence/retention-followup/runtime-v2 --attempt reproduction-weak-v1 --slot-granted
    python3 result-coordinate-evidence/retention-followup/contracts/qualify_runtime.py strong-lifetime --runtime result-coordinate-evidence/retention-followup/runtime-v2 --attempt reproduction-strong-lifetime-v1 --slot-granted

The successful controlled memory command was:

    python3 result-coordinate-evidence/retention-followup/diagnostics/run_memory.py --runtime result-coordinate-evidence/retention-followup/runtime-v2 --output result-coordinate-evidence/retention-followup/diagnostics/memory-v1 --slot-granted

Use a new output directory for reproduction. Both full weak fixtures passed through `benchmark/qualify_fixtures.py` into `qualification-v1`. Their timings are excluded. The primary 36-process schedule is frozen at `comparison-v1/protocol.json`, SHA-256 `e151245f589b655d7b2f8240998552486e84836d0895147ab7b8428d7fe111af`. Run exactly one pending process per invocation, observing the tool process terminate with exit zero before continuing:

    python3 -B result-coordinate-evidence/retention-followup/benchmark/run_comparison.py next --cohort result-coordinate-evidence/retention-followup/comparison-v1 --slot-released

After all 36 normal completions:

    python3 -B result-coordinate-evidence/retention-followup/benchmark/run_comparison.py audit --cohort result-coordinate-evidence/retention-followup/comparison-v1
    python3 -B result-coordinate-evidence/retention-followup/benchmark/analyze_comparison.py --cohort result-coordinate-evidence/retention-followup/comparison-v1 --output result-coordinate-evidence/retention-followup/analysis-v1

Before reading a large raw evidence file after a fresh Git checkout, restore missing archived bytes:

    python3 result-coordinate-evidence/retention-followup/recovery/preserve_evidence.py restore --archive result-coordinate-evidence/retention-followup/recovery/preserved-large-v1

This verifies compressed and raw hashes and refuses to overwrite different bytes. It does not reconstruct missing baseline binaries; use the recovery builder and a new runtime/protocol when exact original binary inputs are unavailable.

## Validation and Acceptance

The weak mode must release a dropped Value and the old identity graph while the memo itself remains live; it must still reuse the exact same object when a row or consumer retains it. Mutating ID, revision, owner, or semantic version must remain a miss. Equivalent identity replacement must remain a hit. Close and admission failure must leave ledger reservations at zero, and retained detached rows must remain valid after source closure. Existing key, full-mask, collision, growth, and fallback contracts must continue to pass.

Every latency observation must match its fixture's expected result count and checksum, use the prescribed AUTO route, and finish with the expected ledger cleanup. The same input, JVM, ownership code, store caches, and consumption pattern apply to all paired arms. Memory diagnostics will quantify observed retained payload and table-owned metadata; an array-only ledger total will never be described as a complete memory bound.

## Idempotence and Recovery

Use new attempt directories and refuse to overwrite failed test reports or partial timing cohorts. A timing failure invalidates its cohort; keep it and freeze a new attempt only after explaining the failure. Build and compile operations may be retried for a diagnosed prerequisite problem, with distinct logs. Git commits and pushes to the existing HASMAC branch are authorized by the user. Preserve untracked files and keep downloaded dependencies out of Git.

## Artifacts and Notes

Deliver a complete source archive containing every changed source and experiment script, plus a patch with only the changes. Verify archive members and patch reconstruction before linking them. Save raw observations, build/test excerpts, protocol and runtime identities, and an interpretation document in this Git-backed evidence directory, then commit and push checkpoints.

## Interfaces and Dependencies

Do not add external runtime dependencies. Use JDK `WeakReference`, `Reference.reachabilityFence`, and existing RDF4J APIs. `NativeDirectValueMemo` retains its constructor, `find`, `remember`, `clearEntries`, `disable`, and `close` integration points. Its owner remains `NativeResultMaterializer` so iteration lifetime and detachment behavior remain identical across arms. Add diagnostic counters only when they are needed to distinguish collection from mutation and collisions; serialize observations outside measured query work.

Revision note (2026-11-17): Created the recovery and retention plan before implementation, after verifying the pushed checkpoint and both dependency archive checksums.

Revision note (2026-11-17): Recorded successful fresh LMDB builds, the preserved root reactor dependency failure, and the move to focused baseline and test-first retention validation.

Revision note (2026-11-17): The fresh focused baseline passed 106 tests. Smallest retention contract compiled and failed as intended (1 test, 1 failure, 0 errors, 0 skips), while the independent collection control passed; preserved at contracts/validation/red-smallest-v1. Implementation may now begin. Weak metadata reservation is fixed at112 bytes per slot plus160 bytes for independent last-owner and replacement overlap; with the existing arrays initial reservation is606,448 bytes. JVM object alignment and compressed-pointer settings will be pinned in the new experiment.

Revision note (2026-11-17): Final formatted runtime is runtime-v2 (seven classes byte-identical to runtime-v1). Qualification passed 138 default/strong, 47 weak, and 8 strong-lifetime checks with 0 failures/errors/skips; the real source-cache diagnostic passed 1 test. The corrected default run is qualified-default-v2; preserve the misconfigured first attempt. New diagnostics are now running separately from timing.

Revision note (2026-11-17): All four controlled memory JVMs and 24 checkpoints passed. Strong mode retained 128 MiB of logical payload until close; weak mode retained zero or the one 2 MiB value held by its consumer. Raw bytes and interpretation are in diagnostics/MEMORY-RESULTS.md. Full-fixture weak pilots now qualify the real harness/stats interface before freezing the 36-JVM comparison; pilot latency will not enter estimates.

Revision note (2026-11-17): Both weak full-fixture pilots passed (26 queries, 26 owners), and runtime-v2/qualification.json combines the test, memory and pilot evidence. comparison-v1/protocol.json is now frozen for 36 JVMs; no primary timing process has launched. The next checkpoint will publish this qualified state before timing.

Revision note (2026-11-17): Added actual qualification outcomes, exact reproduction and finite timing commands, and lossless evidence recovery instructions before the pre-timing checkpoint.
