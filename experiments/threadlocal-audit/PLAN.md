# Audit and reduce query ThreadLocal overhead

This living ExecPlan follows `.agent/PLANS.md` and covers all query-reachable ThreadLocal state, not just cancellation.

## Purpose / Big Picture

Trace actual regular RDF4J, native interpreted, and Janino query paths. Separate one-time capture from per-pull/per-row access and bounded inner-loop polls. Replace ambient lookups with explicit references only when cancellation, nested callbacks, worker scopes, replay and ownership remain correct. Measure fresh matched baselines and candidates rather than claim ThreadLocal cost from sample shares.

## Progress

- [x] Recover published experiment at ba74f478 in a separate checkout.
- [x] Inventory repository-owned paths and provision JDK25/Maven.
- [in_progress] Qualify baseline cancellation and lifecycle tests.
- [ ] Implement explicit propagation with adversarial tests.
- [ ] Compare regular/native/compiled matched runs.
- [ ] Publish selective verified evidence and conclusions.

## Context and Orientation

`core/common/iterator/.../CancellationSignal.java` carries enclosing lifecycle cancellation. `core/sail/api/.../AbstractSailConnection.java` composes operation lineage; `SailBaseIteration` captures it. `core/sail/lmdb/.../LmdbStoreConnection.java` manages read exposure, workers and replay. `core/sail/lmdb/.../evaluation/NativeCancellationToken.java` separately supports evaluation and local pipeline cancellation. Do not substitute one for the other. The companion INVENTORY.md records 86 repository declarations, exact sites and static frequency classifications; third-party transitive accesses remain a separately identified limit.

## Surprises & Discoveries

The executor was replaced: yesterday's checkout, frozen classes and dependency cache are absent. Recovered published source is ba74f478; fresh machine has JDK21 and no Maven. Provisioned official Maven and Temurin25 locally. New measurements must rerun both arms on this machine. No inference from yesterday's times is valid.

`SailBaseIteration` already captures lineage and does not install it per pull. Native token polling itself uses fields, but source checkpoints and type-matrix polls access ambient tokens. Read-exposure constructors separately capture lifecycle cancellation.

## Decision Log

Use Routine D for the cross-cutting refactor, with focused pre/post and adversarial tests even though the routine permits implementation under the plan. Preserve all existing files; use a separate recovered checkout and serialize heavy JVM runs. Inventory is static initially, not proof of dynamic counts or a performance claim.

## Plan of Work

First recover toolchain and root quick install. Collect existing tests that hit each proposed change and run them before edits. Start with one explicit-token candidate, retaining ambient fallback at APIs that lack an execution context. Compare checks for nested independent queries, local pipeline stop versus parent cancellation, worker interruption, deadline probes, replay, map growth and closed iterators. Expand only after this first candidate is correct.

## Concrete Steps and Validation

From repository root use JDK25 and Maven3.9.11, workspace-local `.m2_repo`. Run required root `-Pquick clean install`, offline first and then online for missing dependencies. Never use `-am` or `-q` for tests. Persist logs in experiments/threadlocal-audit and initial-evidence.txt. Run focused classes with module-only Maven. Record exact commands and report counts. Compare same-process settings and frozen source; explicitly verify regular, interpreted native and compiled route before timing.

## Outcomes & Retrospective

The source inventory is complete. Cold root quick build compiled production through LMDB, then failed as expected because new checkpoint contract tests name the not-yet-implemented class. This red compile is preserved in maven-build.log. Baseline existing tests and implementation remain underway. No throughput improvement claimed.
