# Compact stable repository result snapshots

This ExecPlan follows repository PLANS.md and is maintained as implementation progresses.

## Purpose / Big Picture

Repository rows must become stable shallow copies before the LMDB boundary prepares their Values. The current MapBindingSet snapshot allocates a linked-map entry and a SimpleBinding per bound column. Profiles of the 8-million-row, 30-column synthetic query attribute about half of sampled allocation to this representation. This experiment keeps the ready-value and ownership contract unchanged and tests a smaller representation. It is default-off and is not a release recommendation.

## Progress

- [x] (2026-11-17) Read boundary, BindingSet and MapBindingSet contracts.
- [x] Add adversarial compatibility tests and capture initial failure.
- [x] Implement default-off compact snapshot representation.
- [x] Run focused and module tests with flags both ways.
- [in_progress] Supply isolated candidate classes for parent's matched benchmark.
- [ ] Save reviewed local checkpoint and measured findings.

## Surprises & Discoveries

MapBindingSet exposes mutable keySet views, removable iterators, MutableBindingSet setters, and stable Binding object identities. Merely replacing it with an immutable BindingSet would change existing behavior. NativeProjectedBindingSet itself creates Binding objects while iterating; the snapshot must read each name/value immediately because an arbitrary source can reuse a mutable Binding instance.

## Decision Log

Use a package-private MapBindingSet subclass with owned String[] and Value[] arrays and a primitive hash index. This preserves cast compatibility and common getValue/hasBinding/size reads without per-column map objects. Materialize into a private plain MapBindingSet delegate when getBinding, iterator, keySet views or mutation are requested; these are cold fallback operations in the benchmark, not forbidden operations. Keep the original Value references and copy names in source iteration order. Do not retain the source or use a global layout cache. Use a default-off system property read once in a private holder: rdf4j.sail.compactResultSnapshots.

Extraction of this private class traverses the value references directly, without invoking Value methods. Structural modifications during callback traversal must fail before the next element, matching the old iterator. Nonstructural value replacement during callbacks must affect subsequent elements. Wrappers around exposed map iterators and keySet views track structural modifications, including view removals.

## Context and Orientation

core/sail/api/src/main/java/org/eclipse/rdf4j/sail/features/SailResultValueExtractor.java owns the BINDING_SETS snapshot and extraction methods. The LMDB RepositoryValueIteration first snapshots, extracts roots, recursively prepares values, and only then returns a row. This experiment changes only the snapshot representation in the Sail API module. Parent owns LMDB changes and benchmarking. Original Sail API classes are frozen at experiments/overlay/lifetime/original-api-classes, and current LMDB classes at experiments/overlay/lifetime/combined-classes. Candidate jars must never silently replace the baseline in performance classpaths.

## Plan of Work

First add CompactBindingSetSnapshotTest in the same package. With the experiment flag enabled it must initially fail its compact-representation witness while ordinary compatibility assertions describe existing MapBindingSet behavior. Then add CompactBindingSetSnapshot as a package-private subclass and route snapshots/extraction through it only when enabled. The arrays and hash table are private and never shared with source containers. Large/irregular inputs must be handled without truncation, unchecked capacity overflow, or missed bindings. Existing-map fallback keeps all operations supported. No external dependencies or public API additions are required.

The first milestone is functional compatibility: retained rows, reused source Bindings, order/equality/hash, missing and colliding names, lazy values with no method calls during snapshot/extraction, serialization, mutable views, iterator removal and callback mutation. The second milestone is API module verification and LMDB boundary/replay tests coordinated by parent. The final milestone is measured allocation and timing with the same query, flags, data and baseline, not estimated improvements from sample shares.

## Concrete Steps

All commands run from the repository root. The restored helper experiments/overlay/lifetime/mvn.sh uses JDK25.0.5+8, Maven3.9.11 and .m2_repo; it refreshes the per-command network proxy. Initial root quick install already succeeded before this assigned subtask. Install affected API module before dependent tests. Tests never use -am or -q.

    experiments/overlay/lifetime/mvn.sh -o -pl core/sail/api -Dformatter.skip=true -Drdf4j.sail.compactResultSnapshots=true -Dtest=CompactBindingSetSnapshotTest test
    experiments/overlay/lifetime/mvn.sh -o -pl core/sail/api -Pquick install
    experiments/overlay/lifetime/mvn.sh -o -pl core/sail/api -Drdf4j.sail.compactResultSnapshots=true -Dtest=CompactBindingSetSnapshotTest,SailResultValueExposureTest surefire:test

Save initial report snippets under experiments/overlay/compact-snapshots and do not overwrite parent's initial-evidence.txt. Before any build coordinate with parent; before benchmark freeze candidate API classes and report flags and source revisions.

## Validation and Acceptance

Every test must preserve exact Value identity and show no Value interactions during snapshot or root extraction. Result names/values must stay unchanged after source containers and Binding objects are reused. Mutable operations must behave like MapBindingSet, including live views, iteration order, fail-fast structural mutation and stable returned Binding identities. Hash/equality and serialization must match MapBindingSet. Flag-off must return ordinary MapBindingSet. No timing claim is allowed before parent completes matched measurement and verifies exact output, compiled route, memory budget and spill cleanup.

## Idempotence and Recovery

Use only this subtask's files. Never clean unrelated experiment artifacts. Restored baseline classes are immutable inputs; copy candidate classes to a separate directory. Publication is paused because approval evidence was not accepted; retain local commits only. If semantics fail, fix or decline the optimization rather than relaxing ownership/readiness checks. The original map path remains available by leaving the flag unset.

## Outcomes & Retrospective

Pending implementation and validation. No performance claim yet.

Initial evidence: CompactBindingSetSnapshotTest ran 10 tests, 1 expected failure, 0 errors/skips. The enabled witness expected a compact subclass but received exact MapBindingSet. Nine compatibility tests passed on the old implementation. Saved initial-evidence.txt and initial-test-report.xml before production edits.

Review found four concrete gaps, reproduced as 4 failures among 13 tests before fixes: foreign Binding getName called too often, private serialized class leaked, names-view spliterator lost ORDERED, and custom source.forEach was bypassed. Fix uses size-before/after mutation bookkeeping, writeReplace plain MapBindingSet, delegating names-view spliterator/forEach, and preserving source.forEach dispatch. Evidence: review-red-evidence.txt. Compact eligibility is capped at256 declared bindings; wider rows preserve the original map path.

Further review reproduced an extra-name-callback/rekeying serialization bug: foreign-serialization-red-evidence.txt,1 failure. Materialization now uses a private plain MapBindingSet delegate, and writeReplace returns that exact map without recopying bindings. This keeps original map keys and arbitrary foreign Binding identities/getter behavior, at the cost of one additional reference field in compact rows and one extra map wrapper only on fallback. All compact arrays clear after materialization.

Candidate verified: scoped formatting passes;23 focused tests pass with flag on and off;18 tests pass with JVM assertions explicitly disabled and a test confirms assertion mode; complete Sail API unit suite137 tests passes independently with flag on and off. No failures/errors/skips. Candidate API classes frozen in candidate-api-classes; LMDB integration/replay and timing remain parent-owned pending gates.

### Follow-up: integer hash spreading

Parent's balanced four-variant comparison found the original compact representation slower despite lower allocation: combined compact45.30s versus lifetime-only39.04s; compact-only44.90s versus priorbest39.72s. These are reported parent measurements; see ../compact-snapshot-benchmark/RESULTS.md. Candidate JFR identified slot lookup as833/4059 main-thread CPU samples (20.52%, not wall-time share). Therefore test only a stronger integer spread before broadening representation changes.

Added deterministic generated-name equivalence test for every width0–257 using sequential, random and hexadecimal names, fresh equal lookup strings, absent lookups, hash/equality, and exact Value identity. Pre-change focused suite24/24 passed. Production change is only multiplication by0x9e3779b9 before the existing xor-shift and mask. No fallback, lifetime, extraction, or public behavior changes. Post-change focused suites24/24 pass with gate on and off, and19/19 compact tests pass with assertions disabled. Scoped formatting and module quick install pass. Existing22b candidate freeze remains unchanged; new classes are hash-api-classes. No benchmark run here; timed window owned by Workbench agent and parent.

Deterministic simulation of actual30 query names in insertion order at64 slots reproduces old average5.0333/max16 successful probes; multiply+xor gives1.2667/max3. This is probe-count evidence, not a throughput claim. Parent must run matched query comparison before adopting.
