# Fully featured closed-loop optimizer (online feedback + micro-models + offline learning + adaptive execution)

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` must be kept up to date as work proceeds. This plan is maintained in accordance with PLANS.md at the repository root.

## Purpose / Big Picture

The goal is to make RDF4J’s query planner self-correcting and safe: it should learn from real execution feedback, adjust cardinality and cost estimates, reduce tail latency, and remain debuggable and reversible at any time. After this change, operators will be able to run queries with feedback collection enabled, inspect an EXPLAIN output that shows predicted vs actual rows and costs, and observe the planner choosing more stable, accurate plans while respecting strict safety guardrails (low overhead, fail-closed, bounded memory, deterministic keys).

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

- Decision: Use an incremental, layered estimator stack (baseline stats + online correction + micro-models + optional offline models) with strict feature flags for activation modes.
  Rationale: This aligns with the user’s “fail closed” and “low overhead” requirements while enabling safe adoption and rollback.
  Date/Author: 2025-02-14 / Codex

## Outcomes & Retrospective

No outcomes yet. This section will be updated as milestones are completed and validated.

## Context and Orientation

RDF4J’s query planning and evaluation stack lives primarily under `core/queryalgebra/evaluation` for optimizers and evaluation strategies, and `core/queryalgebra/model` for query algebra nodes. The optimizer pipeline is defined in `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/StandardQueryOptimizerPipeline.java` and invoked by `DefaultEvaluationStrategy` in `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/impl/DefaultEvaluationStrategy.java`. Evaluation statistics and join ordering are influenced by `QueryJoinOptimizer` in `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/optimizer/QueryJoinOptimizer.java`. Any new planner-facing estimate API should integrate into these points, while executor telemetry will need hooks inside evaluation strategy operators (for example, where tuple iteration is materialized and closed).

This plan introduces a feedback loop, which is a mechanism that collects runtime observations (actual rows and costs), aggregates them into persistent stores, and uses them to adjust future estimates. The “feature registry” is a centralized description of what features are used for feedback keys and how they are encoded, so both planner and executor compute identical fingerprints. A “micro-model” is a small in-memory summary (for example, a sketch or histogram) that approximates distributions at low cost. “Offline models” are heavier models trained outside the runtime process and loaded on demand; they must be optional and gated.

## Plan of Work

The work proceeds in phases that map to the user’s requested workstreams and acceptance criteria. Each phase introduces minimal, observable behavior and can be run in observe-only mode to preserve safety. The plan assumes new configuration flags will live alongside existing evaluation strategy configuration (likely under `core/queryalgebra/evaluation` or higher-level configuration modules such as `core/query` or `core/repository` if needed). The plan maintains deterministic key generation by ensuring that the same algebra patterns produce the same fingerprints in both the planner and executor via a shared canonicalizer and feature registry.

Phase 0 establishes telemetry and explainability. We will annotate executable plan nodes with a stable node id, plan shape hash, predicted rows, predicted cost, and the feedback key components needed to reproduce lookups later. This requires adding a small metadata container and ensuring it travels with `TupleExpr` nodes or with the evaluation strategy’s internal representations. We will extend EXPLAIN output (likely in the query rendering area under `core/queryrender` or in existing explain logic) to show predicted vs actual per node when telemetry is enabled. We will add a minimal `TelemetrySink` interface that can be disabled or set to a no-op for low overhead.

Phase 1 implements deterministic fingerprinting and the feature registry. We will build a canonicalizer for tuple expressions and filters that normalizes variable names, stable ordering for commutative sets, and coarse buckets for literals and filter functions. The canonicalizer will live in a new package, for example `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/feedback`, and the feature registry will define a versioned schema for feature encoding. We will include a small test corpus that validates “same query, different variable names” collisions and stable fingerprints across runs.

Phase 2 adds the feedback data plane: in-memory hot cache plus an on-disk key-value store with bounded size and safe startup. The plan assumes an embedded storage backend; if no current dependency exists, we will start with a simple on-disk format using existing IO utilities in `core/common/io` and a write-ahead log-like append file, then evaluate whether a dependency is acceptable. The store will maintain schema/version headers and a fixed maximum size with eviction based on observation count and recency. A `TelemetryFlusher` will aggregate events and update the store in batches, and will be safe to disable.

Phase 3 introduces online correction estimators. Each feedback key stores observation count, EWMA log error, variance, optional quantile sketch, and last-seen timestamps. The estimator stack will apply shrinkage against parent keys (global → graph → predicate → pattern → key) and will clamp corrections to avoid plan flapping. We will add a new `Estimate` type that includes mean, variance, confidence, and source breakdown. These estimates will be consumed by the optimizer pipeline and cost model entry points.

Phase 4 adds micro-models for predicate-level statistics. The plan introduces lightweight sketches (such as HLL for distinct counts and a small quantile sketch for numeric ranges) and a per-predicate activation policy to enforce bounded memory. The activation policy will be driven by observed frequency and cost, and will support eviction. This phase adds APIs such as `estimateEq`, `estimateRange`, and `estimateJoinNDV` in the estimator stack.

Phase 5 adds cost model calibration and planner integration. We will extend cost calculation in the evaluation strategy to use the new `Estimate` information, including uncertainty-aware cost (expected cost plus tail risk). We will implement plan stability controls (stickiness and improvement thresholds) to avoid flapping. We will also add a per-query memoization cache to avoid repeated store lookups within a planning cycle.

Phase 6 adds offline learning and model registry. We will define a model artifact format, a registry for versioned models with feature schema hashes, and a gating policy that only applies offline models when feature completeness and confidence are high. The data export pipeline will aggregate and anonymize feedback into a training dataset, respecting TTL and caps. Offline inference will be strictly bounded and fail-closed.

Phase 7 adds adaptive execution. We will add runtime checkpoints at strategic operators (joins, optionals, filters) to compare actual vs predicted rows and trigger bounded adaptations such as join algorithm switches or limited re-plans. The adaptation policy will enforce caps (maximum adaptations per query, minimum benefit threshold) and preserve semantic correctness.

## Concrete Steps

All commands below should be run from the repository root. Use the project’s preferred tooling to compile and test, keeping offline builds as the default. When steps mention new classes or packages, create them exactly in the noted paths.

1. Add telemetry metadata for plan nodes and a `TelemetrySink` interface.
   Update `DefaultEvaluationStrategy` to emit a `FeedbackEvent` with predicted vs actual metrics on close of each operator. Add a feature flag for OFF/OBSERVE/SHADOW/APPLY_SAFE/APPLY_FULL.

2. Add canonicalizer and feature registry.
   Create `core/queryalgebra/evaluation/src/main/java/org/eclipse/rdf4j/query/algebra/evaluation/feedback/Canonicalizer.java` and `FeatureRegistry.java`. Ensure canonicalization handles variable renaming, commutative ordering, and filter family bucketing. Add tests under `core/queryalgebra/evaluation/src/test/java/.../feedback/` verifying fingerprint stability.

3. Implement feedback store and aggregator.
   Create `FeedbackStore` and `TelemetryFlusher` with a hot in-memory cache and a bounded on-disk store under `core/queryalgebra/evaluation/.../feedback/store`. Add schema version headers and a safe fallback on version mismatch or corruption.

4. Implement online correction estimators and `Estimate` type.
   Add `Estimate` and `EstimatorStack` under `core/queryalgebra/evaluation/.../feedback/estimate`. Update `QueryJoinOptimizer` and any evaluation statistics classes to consume `EstimatorStack` when enabled.

5. Add micro-models and activation policy.
   Add `SketchStore` and per-predicate sketch types under `feedback/sketch`. Implement activation thresholds and eviction based on memory budget.

6. Integrate cost model calibration.
   Introduce a `CostModel` interface under `core/queryalgebra/evaluation/.../feedback/cost` and wire it into the evaluation strategy. Ensure explain output shows cost breakdown and uncertainty.

7. Add offline model registry and inference gating.
   Create a model registry under `core/queryalgebra/evaluation/.../feedback/model` with versioning metadata and a loader. Add configuration to enable canary models and instant rollback.

8. Add adaptive execution checkpoints.
   Insert checkpoints in evaluation operators (joins, filters, optionals) to compare actual vs predicted and trigger bounded adaptations. Ensure all adaptations respect max limits and are recorded in query profile output.

9. Expand tests, benchmarks, and documentation.
   Add property-based tests for canonicalization and key determinism, micro-benchmarks for store lookup and estimator inference, and documentation updates under `site/` for new configuration flags and EXPLAIN output fields.

## Validation and Acceptance

Validation proceeds per phase. For Phase 0, enable OBSERVE mode and run a representative query; EXPLAIN should show predicted vs actual rows/time for each operator. For Phase 1, run the canonicalization tests and confirm variable-renaming collisions and stable ordering. For Phase 2, run store tests that verify bounded size, eviction behavior, and recovery after restart. For Phase 3, run estimator tests that confirm corrections converge without exceeding clamps. For Phase 4, run micro-model tests to confirm sketches update and estimates are bounded. For Phase 5, run targeted planner tests that show plan stability controls prevent flapping under similar cardinality. For Phase 6, run model registry tests that load a model artifact and exercise canary gating. For Phase 7, run adaptive execution tests on a synthetic workload where large divergence triggers a bounded replan and logs an audit trail.

Acceptance requires that APPLY_SAFE reduces cardinality error and tail latency on benchmark workloads without plan flapping beyond configured thresholds, EXPLAIN output shows per-node evidence and estimator sources, and toggling the feature flag to OFF restores baseline planning behavior immediately.

## Idempotence and Recovery

All steps are additive and safe to re-run. Feature flags default to OFF, so any partially implemented phase should not alter production behavior. If a step fails or introduces instability, disable the feature flag, remove or roll back the new classes, and keep the store in quarantine; the system must fail closed. The feedback store must be deletable or purged with a single config flip to revert to baseline behavior.

## Artifacts and Notes

Expected artifacts include:

  - EXPLAIN output showing predicted vs actual metrics per operator, with source breakdown and correction factors.
  - Feedback store metadata files showing schema version and feature registry hash.
  - Benchmark logs from `scripts/run-single-benchmark.sh` (if used) showing overhead with feature modes OFF/OBSERVE/APPLY_SAFE.

## Interfaces and Dependencies

New interfaces should remain within `core/queryalgebra/evaluation` to minimize cross-module coupling. The minimum set of interfaces and types to introduce are:

- `TelemetrySink` in `core/queryalgebra/evaluation/.../feedback/TelemetrySink.java` with `emit(FeedbackEvent event)`.
- `FeedbackEvent` in `core/queryalgebra/evaluation/.../feedback/FeedbackEvent.java` containing key, predicted, actual, and minimal context.
- `FeedbackStore<K, V>` in `core/queryalgebra/evaluation/.../feedback/store/FeedbackStore.java` with `Optional<V> get(K key)` and `void updateBatch(Map<K, V> updates)`.
- `Estimate` and `EstimatorStack` in `core/queryalgebra/evaluation/.../feedback/estimate` with `Estimate estimate(NodeContext ctx)` returning mean, variance, confidence, and sources.
- `CostModel` in `core/queryalgebra/evaluation/.../feedback/cost/CostModel.java` with `CostBreakdown cost(NodeContext ctx, Estimate estimate)`.
- `ModelRegistry` in `core/queryalgebra/evaluation/.../feedback/model/ModelRegistry.java` with `Optional<OfflineModel> resolve(ModelId id)` and metadata for schema versions.

Any new dependency must be explicitly approved before adding; until then, favor internal implementations or existing utilities within `core/common` and `core/queryalgebra`.

Plan change note: Initial ExecPlan created from user-provided architecture and workstream requirements; no repository discoveries or revisions yet.
