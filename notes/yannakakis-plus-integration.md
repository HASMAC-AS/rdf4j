# Yannakakis+ integration sketch for RDF4J

This note distills the Yannakakis+ algorithm into RDF4J-specific steps so we can prototype an optimizer that rewrites acyclic SPARQL conjunctive queries into a standard relational DAG executed by the existing evaluation pipeline.

## Scope and detection
- Target SELECT queries whose WHERE clause is a basic graph pattern (BGP) with optional FILTERs and an optional GROUP BY/HAVING.
- Skip OPTIONAL/UNION/MINUS/VALUES/NOT EXISTS/property paths and any cyclic BGP; fall back to the default optimizers in those cases.
- In a `QueryOptimizer`, locate maximal BGP subtrees made only of `Join`, `StatementPattern`, and FILTER nodes. Extract their variables to form the hypergraph `H = (V,E)` where each triple pattern is a hyperedge.

## Acyclicity and join tree
- Apply a GYO-style reduction (Tarjan–Yannakakis) to decide acyclicity and recover a join tree whose nodes are triple patterns and whose edges are labeled by shared variables.
- If the BGP is acyclic, optionally split into connected components and handle each independently.

## Yannakakis+ plan construction
- Classify the query per Yannakakis+ (free-connex vs non-free-connex; relation-dominated heuristic to possibly skip semi-joins).
- Build an internal DAG (e.g., `YOp` records) over scans, filters, semi-joins, joins, aggregation-joins, and aggregations.
- Push FILTERs to leaves, schedule reduced semi-join passes along the join tree, and place aggregation-joins early when possible to shrink intermediate results.

## Lowering to RDF4J algebra
- Two lowering strategies:
  - **Reuse existing operators:** encode semi-joins as `Join` + `Projection` + `Distinct` and agg-joins as `Join` + `Group`. Works with the stock `DefaultEvaluationStrategy` and requires no new nodes.
  - **Add explicit operators:** define `SemiJoin`/`AggJoin` `TupleExpr` nodes and extend `DefaultEvaluationStrategy` (or `StrictEvaluationStrategy`) to evaluate them with hash-based semi-join logic and partial aggregation. Use RDF4J’s visitor registration hooks so optimizers and renderers recognize the new nodes.
- Translate the Yannakakis+ DAG back into a `TupleExpr` subtree and splice it into the original algebra in the optimizer.
- Protect the lowered subtree from `QueryJoinOptimizer` reordering by wrapping it in a sentinel (e.g., a no-op `Slice`) or by teaching `QueryJoinOptimizer` to skip descendants marked with a custom annotation.

## Prototype bring-up checklist
- Optimizer wiring: add `YannakakisPlusOptimizer` to the optimizer pipeline before `QueryJoinOptimizer` and behind `FilterOptimizer`/`BindingAssignerOptimizer`.
- Gatekeeping: feature flag + optional SPARQL hint to disable per query; skip cyclic BGPs and any query containing `LeftJoin`, `Union`, `Minus`, `Exists`, `Values`, or property paths.
- Logging: emit DEBUG logs when a BGP is detected as acyclic, when the Yannakakis+ plan is built, and when the subtree is lowered.
- Testing: start with unit tests on the join-tree builder and acyclicity detector, then integration tests that optimize/evaluate sample acyclic vs. cyclic queries to verify rewrites and runtime results.

## Execution hooks and safeguards
- Register the optimizer ahead of `QueryJoinOptimizer` (and ensure it does not reorder protected Yannakakis+ subtrees).
- Optionally provide a custom `EvaluationStrategy` for efficient semi-join/agg-join execution while still relying on the storage `TripleSource`.
- Add feature flags/hints to disable Yannakakis+ per query and logging hooks to dump pre/post plans for debugging.
