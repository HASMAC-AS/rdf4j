# Test namespace reference lookup costs


This is a living experiment plan maintained under `.agent/PLANS.md`. It covers the additive harness and protocol. The root agent owns production changes and their correctness gates.

## Purpose and outcome


The preceding result-coordinate experiments showed lower allocation but worse total latency than ordinary value reuse. The next hypothesis is that repeated namespace-directory lookups in reference retention and release are substantial avoidable work. A namespace identifies the lifetime of source coordinates; each retained coordinate references that lifetime. We will first count the actual repeated managed namespaces in frame and source-batch ranges, then test the root agent's candidate against the original DENSE implementation and an ordinary-reuse OFF control. The user will receive new measured evidence, including whether the candidate improves DENSE and whether it closes the gap to OFF.

## Progress


- [x] Read preserved runtime and namespace paths.
- [x] Select three arms and balanced design.
- [x] Prepare common harness and diagnostic copies.
- [x] Validate original diagnostics and record namespace-directory absence.
- [x] Stop the namespace-memo hypothesis: no managed lookups occur in this fixture.
- [x] Prepare and independently review the separate frame-directory-guard protocol; root validated its candidate with the matching 103-test gate.
- [x] Freeze verified candidate and shared harness identities; strict protocol/runtime validation passed.
- [x] Run the separate frame guard timing study after the shared-tail occupancy diagnostic and exclusive slot grant: all 18 forks / 234 queries passed.
- [x] Audit every raw JSONL result and independently recalculate all medians and paired ratios; publish `frame-directory-guard/RESULTS.md`.
- [in_progress] Hand off the complete evidence and report for the root’s final packaging.

## Decisions


On 2026-11-15 the parent approved three arms, `original-off`, `original-dense`, and `candidate-dense`, across all six permutations. Each arm appears twice in each position and equally often before every other arm. Every fresh JVM runs eight warmups and five measurements. Eighteen JVMs produce 144 warmups and 90 primary measurements. No additional arms, timing rounds, favorable tails, or replacement forks are added based on results.

The new protocol explicitly enables both native evaluation and direct adjacency. The old frozen harness enabled native evaluation but left direct adjacency disabled. Consequently a small common harness overlay enables direct adjacency and waits for real readiness before populating/measuring. This overlay is shared identically by all timing arms and changes no fixture, query, consumption loop or result validation. This is a new protocol; its timings are not pooled with the previous disabled-adjacency measurements. The existing post-main root-audit launcher is reused byte-for-byte.

An independent package-local test helper reads existing direct-adjacency metrics before and after each query, outside its time/allocation window. Successful AUTO selection, compiled/interpreted bind counts, direct-adjacency lookup/planner counters and the post-main outer-root audit will show the actual route. Enabling a flag alone is not treated as evidence of an adjacency execution route.

The diagnostic binary is separate from timing binaries. Its owner-local counters distinguish frame retention, frame release, source-batch retention, source-batch release, direct retain and direct release. Counts include visited/positioned/managed cells, transitions between processed managed namespaces, managed runs, singleton ranges, reference calls and actual directory probes. Counts are published once per closed owner and sampled outside query timings. Diagnostic timings have no performance interpretation.

## Discoveries


`KernelResultCoordinates.retainRange` and `releaseRange` currently perform one `retain` or `release` per positioned cell. Each helper finds the namespace again. Frame calls occur in save/release paths; source-batch calls occur in `retainBatch` and `releaseBatch`. `namespaceIndex` ignores namespace IDs zero and one. Namespace removal can move collision-chain slots, so caching slots across arbitrary namespace mutation is unsafe; the root agent's candidate must validate lifetime and invalidation behavior.

The public store API includes `awaitDirectAdjacencyReady(timeout, unit)`. The test helper `LmdbTestUtil.awaitBackgroundMaintenance` waits for actual deferred maintenance completion. The package-local adjacency metrics expose lookup hits, planner-stat hits, exact misses and fallback reasons. No reflection or private-state mutation is needed to observe them.

## Work and commands


Work is additive under `result-coordinate-evidence/namespace-followup/`. `prepare-sources.py` copies exact baseline sources, checks their hashes against the frozen source manifest, and creates common and diagnostic source trees. It invokes no compiler or JVM. The common source differs only in explicit direct-adjacency setup/readiness and outside-window telemetry. The diagnostic source additionally collects namespace counts.

All compiler, test and Java execution commands wait for explicit release of a slot by the root agent. Existing classpath bytes remain immutable. Shared harness classes are placed before the original 127-entry classpath; the candidate replaces only the original production entry. Before timing the driver verifies the full ordered manifests, source identities, permitted class changes, protocol hash and absence of unexpected JVM environment overrides. It repeats preservation checks after the final JVM.

The fixed Java runtime is `/workspace/scratch/d0a0e51b9386/toolchain/jdk-25.0.5+8/bin/java`. Every timing fork uses `-Xms1024m -Xmx1024m -XX:+UseG1GC`, normal AUTO, enabled hot diagnostic counters, default M=100/R=10/width=12/two-status/full-consumption fixture, and one prescribed coordinate mode. The output directory must be new. The driver retains exact argv, console, JSONL, store, root audit and runtime identities for every process.

## Validation and acceptance


No timing begins until matching original/candidate ownership and transport tests pass and the diagnostic run confirms the target actually executes. Every timing query must retain the independent full-bag checksum `3123987218479850048`, row count 1,152,000, nonnegative allocation counters and zero query-ledger use before and after closure. Native/adjacency readiness and actual route observations are retained. A generic outer root, missing coordinate participation in a DENSE target, or invalid result makes the intended target comparison unqualified and stops the protocol without replacing the fork.

The primary statistic is each JVM's median of all five measured queries, then candidate/comparator ratios within each matched round. Report median and the complete range of six ratios for candidate/original DENSE, candidate/OFF and original DENSE/OFF, with allocation, preparation, evaluation, consumption, first-row, ledger and route evidence. Call a gain consistent only when all six latency ratios favor it and route/ownership/result evidence is coherent. A range crossing one is inconclusive. A lower resolver-call count or reduced allocation alone is not a latency win.

## Recovery and limits


All output paths are new; no prior benchmark or evidence file is overwritten. On a failed process, invalid record or changed frozen artifact, preserve the record and stop. Diagnose the failure and document any new independent protocol rather than replacing failed forks in the current study. An unexpected route remains an outcome and must not be forced to match a desired strategy.

The query-ledger peak covers admitted query reservations, not all JVM heap, retained application values or the separate direct-adjacency memory budget. The experiment targets this fixture and a local namespace-reference optimization; it cannot establish all workload, strategy, memory or concurrency behavior.

## Outcomes and retrospective


Two separate original-DENSE diagnostic forks completed. They observed zero managed namespace lookups and therefore ruled out namespace-directory memoization for this fixture. The v2 fork directly observed a null directory at every frame range entry: compiled queries visited 17,237,000 cells per direction, all unmanaged, so a narrow frame-only guard has 34,474,000 existing visits per query to eliminate. All result, lifetime, native/AUTO/root and actual query-time adjacency guards passed. See `DIAGNOSTIC-FINDINGS.md` and the raw v1/v2 evidence.

The namespace-memo timing draft remains unexecuted. A new, separately named frame-directory-guard study is being prepared using the same fixed three-arm/six-permutation design. Its candidate has not yet been accepted on timing evidence. Diagnostic timings have no performance interpretation.

Revision 2026-11-15: initial plan created before compilation or new benchmark execution; parent approved common enabled-adjacency harness and six-permutation design.

Revision 2026-11-15: prepared source copies, explicit frame/batch diagnostics, guarded diagnostic runner and fixed six-permutation protocol draft. Independent review corrections were applied before compilation; first preparation retained. Parent requires observed locality before freezing performance execution.

Revision 2026-11-15: completed original v1 and additive v2 diagnostics; preserved both. Abandoned the memo timing study after zero observed lookups. Prepared a new frame-only directory-absence guard protocol, with the exact shared uninstrumented harness and immutable reviewed patch identity required before timing.

Revision 2026-11-15: root reported matching candidate gate of 8 classes / 103 tests with zero failures, errors or skips. Frozen frame guard protocol SHA-256 `bbad71c3c879b49022ff6cb22d229ee94353961d1f512defdec722fe2a368f54` binds independently reviewed production patch `0ceed5b1f28931ae334671960f8f875e94d61883ab4e4ed1da29a3f3db70ac0e`. Full identity validation passed; no primary timing JVM has launched.

Revision 2026-11-15: completed all six prescribed timing rounds, 18 JVMs, 144 warmups and 90 measurements without replacement or exclusion. All result/ledger/native/AUTO/adjacency/root/identity gates passed. Guard/original-DENSE latency ratio was 0.949192 [0.901233, 1.059632], an inconclusive 4/6 favorable pairs; guard/OFF was 1.255492 [1.109081, 1.401140], slower in every pair. Independent raw recalculation matched every estimate. Root restored the exact original production source; the candidate is not promoted. Complete source/patch/runtime and all evidence remain preserved. No further experiments are planned for this task.
