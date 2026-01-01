# Fully featured closed-loop optimizer (online feedback + micro-models + offline learning + adaptive execution)

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` must be kept up to date as work proceeds. This plan is maintained in accordance with PLANS.md at the repository root.

## Purpose / Big Picture

The goal is to make RDF4J’s query planner self-correcting and safe. The system should learn from real execution feedback, adjust cardinality and cost estimates, reduce tail latency, and remain debuggable and reversible at any time. After this change, operators will be able to run queries with feedback collection enabled, inspect an EXPLAIN output that shows predicted versus actual rows and costs, and observe the planner choosing more stable, accurate plans while respecting strict safety guardrails such as low overhead, fail-closed behavior, bounded memory, and deterministic keys.

## Progress

- [ ] (2025-02-14 00:00Z) Establish baseline telemetry hooks and explain output.
- [ ] (2025-02-14 00:00Z) Implement deterministic fingerprinting and feature registry.
- [ ] (2025-02-14 00:00Z) Add bounded feedback store and aggregation pipeline.
- [ ] (2025-02-14 00:00Z) Implement online correction estimators with uncertainty.
- [ ] (2025-02-14 00:00Z) Integrate estimator stack into planner and cost model.
- [ ] (2025-02-14 00:00Z) Add micro-model sketches and activation policy.
- [ ] (2025-02-14 00:00Z) Add offline model registry and gated inference.
- [ ] (2025-02-14 00:00Z) Add adaptive execution triggers and safety caps.
- [ ] (2025-02-14 00:00Z) Expand tests, benchmarks, and documentation.

## Surprises & Discoveries

No discoveries yet. This section will capture unexpected optimizer behaviors, performance findings, and evidence from test or benchmark output as work proceeds.

## Decision Log

- Decision: Use an incremental estimator stack that combines baseline statistics with online corrections, micro-models, and optional offline models, and gate activation by explicit feature flags.
  Rationale: This matches the user’s safety requirements, keeps overhead bounded, and ensures a clear rollback path.
  Date/Author: 2025-02-14 / Codex

## Outcomes & Retrospective

No outcomes yet. This section will be updated as milestones are completed and validated.

## Context and Orientation

RDF4J’s query planning and evaluation stack lives primarily under `core/queryalgebra/evaluation` for optimizers and evaluation strategies, and `core/queryalgebra/model` for query algebra nodes. The optimizer pipeline is defined in `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/StandardQueryOptimizerPipeline.java` and invoked by `DefaultEvaluationStrategy` in `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/impl/DefaultEvaluationStrategy.java`. Evaluation statistics and join ordering are influenced by `QueryJoinOptimizer` in `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/QueryJoinOptimizer.java`. Any new planner-facing estimate API should integrate into these points, while executor telemetry will need hooks inside evaluation strategy operators where tuple iteration is materialized and closed.

A feedback loop is a mechanism that collects runtime observations, aggregates them into persistent stores, and uses them to adjust future estimates. The feature registry is a centralized description of what features are used for feedback keys and how they are encoded, so both planner and executor compute identical fingerprints. A micro-model is a small, bounded in-memory summary such as a sketch or histogram that approximates distributions at low cost. Offline models are heavier models trained outside the runtime process and loaded on demand; they must be optional and gated so inference can be disabled instantly.

## Plan of Work

The work proceeds in phases that map to the requested workstreams and acceptance criteria. Each phase introduces minimal, observable behavior and can be run in observe-only mode to preserve safety. Phase 0 establishes telemetry and explainability by annotating executable plan nodes with stable identifiers, a plan shape hash, predicted rows, predicted cost, and the feedback key components needed to reproduce lookups later. This requires a small metadata container and ensuring it is carried with the query plan or evaluation strategy structures that are executed. EXPLAIN output must be extended to show predicted versus actual per node when telemetry is enabled, and the telemetry sink must default to a no-op for low overhead.

Phase 1 implements deterministic fingerprinting and the feature registry. A canonicalizer must normalize tuple expressions and filters by stable variable renaming, commutative ordering where safe, and coarse feature buckets for literals and filters. The feature registry defines a versioned schema for encoding and ensures planner and executor compute identical representations. A test corpus should validate that semantically equivalent queries collide to the same fingerprint.

Phase 2 adds the feedback data plane. The plan assumes a hot in-memory cache backed by a bounded on-disk store, with safe startup, version checks, and corruption handling. A batch flusher aggregates events by key, updates the store, and remains safe to disable. Eviction should prioritize low-observation and stale entries.

Phase 3 introduces online correction estimators. Each feedback key stores observation count, EWMA log error, variance, optional quantile summaries, and last-seen timestamps. The estimator stack blends corrections using a hierarchical shrinkage strategy and clamps adjustments to avoid plan flapping. Estimates provide mean, variance, confidence, and source breakdowns for explainability.

Phase 4 adds micro-models for predicate-level statistics such as distinct counts and numeric range selectivity. Activation is controlled by observed frequency and cost thresholds, with memory budgets and eviction policies to keep resource use bounded. The estimator stack exposes query-time APIs such as equality selectivity, range selectivity, and join NDV estimates.

Phase 5 calibrates the cost model and integrates the estimator stack into planning. Costing uses mean and uncertainty to apply a risk-aware objective. Plan stability controls such as stickiness and improvement thresholds prevent flapping. A per-planning-cycle cache avoids repeated store lookups.

Phase 6 adds offline learning and a model registry. The system exports aggregated, anonymized training data, supports versioned model artifacts keyed by feature schema, and gates inference based on confidence and completeness. Offline inference must be fast and fail closed if budgets are exceeded.

Phase 7 adds adaptive execution. Runtime checkpoints compare actual versus predicted rows at joins, optionals, and filters, and trigger bounded adaptations such as join algorithm switches or limited re-plans. Adaptations are capped per query, require minimum benefit thresholds, and must not alter semantics. All adaptations are recorded in query profile output for auditability.

## Concrete Steps

All commands should be run from the repository root. Use the project’s preferred tooling to compile and test, keeping offline builds as the default. Create new classes and packages exactly in the paths described below. First, add telemetry metadata for plan nodes and a `TelemetrySink` interface, then update `DefaultEvaluationStrategy` to emit a `FeedbackEvent` with predicted versus actual metrics on operator close, guarded by a feature flag with modes OFF, OBSERVE, SHADOW, APPLY_SAFE, and APPLY_FULL. Next, add the canonicalizer and feature registry under a new feedback package and add tests that verify stable fingerprints across variable renaming and commutative ordering. Then implement the feedback store and batch flusher with bounded storage, schema headers, and safe fallback on corruption or version mismatch. After that, add online correction estimators and the `Estimate` type, then integrate the estimator stack into `QueryJoinOptimizer` and evaluation statistics. Continue by adding micro-models and activation policy with bounded memory budgets. Extend cost modeling with a new `CostModel` interface and wire it into the evaluation strategy, ensuring explain output includes cost breakdown and uncertainty. Finally, add the offline model registry and gated inference, then implement adaptive execution checkpoints and caps, followed by tests, benchmarks, and documentation updates.

## Validation and Acceptance

Validation proceeds per phase. For Phase 0, enable OBSERVE mode and run a representative query; EXPLAIN must show predicted versus actual rows and time for each operator. For Phase 1, run canonicalization tests and confirm variable-renaming collisions and stable ordering. For Phase 2, run store tests that verify bounded size, eviction behavior, and recovery after restart. For Phase 3, run estimator tests that confirm corrections converge without exceeding clamps. For Phase 4, run micro-model tests to confirm sketches update and estimates are bounded. For Phase 5, run targeted planner tests that show plan stability controls prevent flapping under similar cardinalities. For Phase 6, run model registry tests that load a model artifact and exercise canary gating. For Phase 7, run adaptive execution tests on a synthetic workload where large divergence triggers a bounded replan and logs an audit trail.

Acceptance requires that APPLY_SAFE reduces cardinality error and tail latency on benchmark workloads without plan flapping beyond configured thresholds, EXPLAIN output shows per-node evidence and estimator sources, and toggling the feature flag to OFF restores baseline planning behavior immediately.

## Idempotence and Recovery

All steps are additive and safe to re-run. Feature flags default to OFF so any partially implemented phase does not alter production behavior. If a step fails or introduces instability, disable the feature flag, remove or roll back the new classes, and keep the store in quarantine. The feedback store must be deletable or purgeable with a single configuration flip to revert to baseline behavior.

## Artifacts and Notes

Expected artifacts include EXPLAIN output showing predicted versus actual metrics per operator, feedback store metadata files showing schema version and feature registry hash, and benchmark logs from `scripts/run-single-benchmark.sh` that compare overhead across feature modes. These artifacts should be referenced in progress updates as they are produced.

## Interfaces and Dependencies

New interfaces should remain within `core/queryalgebra/evaluation` to minimize cross-module coupling. The minimum set of interfaces and types to introduce includes `TelemetrySink` with an `emit(FeedbackEvent event)` method, `FeedbackEvent` containing key, predicted, actual, and minimal context, `FeedbackStore<K, V>` with `Optional<V> get(K key)` and `void updateBatch(Map<K, V> updates)`, `Estimate` and `EstimatorStack` with `Estimate estimate(NodeContext ctx)` returning mean, variance, confidence, and sources, `CostModel` with `CostBreakdown cost(NodeContext ctx, Estimate estimate)`, and `ModelRegistry` with `Optional<OfflineModel> resolve(ModelId id)` and metadata for schema versions. Any new dependency must be explicitly approved before adding; until then, favor internal implementations or existing utilities within `core/common` and `core/queryalgebra`.

Plan change note: Rewrote the ExecPlan to comply with PLANS.md prose-first requirements and to reduce list usage outside the Progress section.
