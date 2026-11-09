# GQL/LMDB baseline profiling and algorithm review

The baseline has two different costs: scalar-property access performs an unnecessarily broad replay join, while merged topology pays for repeated algebra analysis and per-edge deduplication. The profiles below identify work to remove; **their instrumented, unwarmed timings are not speedup evidence**. Use the separately recorded, identical-harness A/B measurements for that purpose.

## Provenance and method

| Item | Recorded value |
| --- | --- |
| Production baseline | `dd1be8b6200afeb755c4278a5a542ac59cbff6ff` |
| Baseline executable JAR SHA-256 | `2e34efd0d2a09be35e960dbfe4d41245c79ff304f2e00d4a8323299465ad5c87` |
| Extended benchmark source SHA-256 | `6cc0174909a053f22eb8615b4c999f06af07edd1264a3a5658b979c7af59bf46` |
| Runtime | OpenJDK 25.0.5+8-LTS; JMH 1.37; one thread and one fork; `-Xms512m -Xmx2g` |
| Profiled method | `GqlAdjacencyQueryBenchmark.countPaths` |
| Parameters | GQL, MERGED, PREFER, native evaluation enabled, fanout 32, root count 64 |
| Workloads | TOPOLOGY and PROPERTY_FILTER, separately recorded |
| Profiling configuration | Supported helper with `--enable-jfr`: profile settings, no warmup, ten 10-second measurement iterations |

The baseline JAR combines the pinned production baseline with the extended harness. Loading, adjacency construction and query preparation occur outside the timed method. Result caching is disabled. Each invocation consumes and closes the count result and verifies its independently calculated value. Evaluation still includes ordinary optimizer and physical-planning work.

The public bundle includes [baseline artifact identity](baseline-artifact.json), [environment details](environment.json) and the following original profiling exports. These are sampled text/JSON summaries; the full binary JFR recordings are not included here.

| Workload | Recording command | Worker-stack samples | Whole-recording hot methods | Allocation sites |
| --- | --- | --- | --- | --- |
| TOPOLOGY | [Command](profiles/baseline-profile-topology-command.json) | [Summary](profiles/baseline-profile-topology-sample-summary.json) | [Hot methods](profiles/baseline-profile-topology-hot-methods.txt) | [Allocation pressure](profiles/baseline-profile-topology-allocation-by-site.txt) |
| PROPERTY_FILTER | [Command](profiles/baseline-profile-property_filter-command.json) | [Summary](profiles/baseline-profile-property_filter-sample-summary.json) | [Hot methods](profiles/baseline-profile-property_filter-hot-methods.txt) | [Allocation pressure](profiles/baseline-profile-property_filter-allocation-by-site.txt) |

The [baseline diagnostic index](plans/baseline-diagnostic-index.json) records the 20 untimed helper invocations, exact result counts and engagement counters. Selected plan exports below retain their fixture, query, optimized/executed JSON and result lines; unrelated process logs are omitted.

Worker-stack summaries contain 8,383 of 8,446 execution samples for TOPOLOGY and 9,569 of 9,713 for PROPERTY_FILTER. Their percentages mean that a method appears in a recorded worker stack; **inclusive percentages overlap and must not be added**. Worker selection is by thread, not a steady-state time window. The JFR hot-method and allocation views cover the recording, which includes startup/setup. Allocation pressure is sampled allocation attribution, not retained heap, exact allocated bytes or bytes per operation.

## Scalar properties: eliminate the replay cross product

The property worker-stack summary records `ValueStore.getLazyValue` in 44.41% of samples, `LexicalLeftJoinCursor.sharedKeyMatches` in 31.89%, `ValueStoreRevision$Lazy.resolveValue` in 22.27%, and `LmdbIRI.equals` in 22.23%. The whole-recording hot-method view independently places `getLazyValue` first at 43.68% and `resolveValue` second at 21.83%. These are separate views with different denominators, not additive categories.

The corresponding [MERGED property plan](plans/baseline-plan-gql-merged-property_filter.log) contains a `LexicalReplayLeftJoin` beneath the result filter and scalar extension. Its left input is the two-hop path; its right input is a `GenericEvalPlan(node=Group)` over property owners.

In the baseline, `GqlCompiler.flush()` groups all literal values of the selected property by owner, then left-joins the scalar results to the matched paths. `LexicalLeftJoinCursor.materializeRight()` evaluates that relation before the left side supplies owner bindings. Its `next()` restarts the materialized-right cursor for every left row.

This fixture has 1,024 left paths and 64 × 32 = 2,048 property-owner groups. Therefore the replay visits **1,024 × 2,048 = 2,097,152 compatibility candidates** to produce 512 retained paths. This is a derivation from the fixture and cursor algorithm, **not a measured JFR event count**. More unrelated roots increase the right relation while leaving the query answer unchanged. The allocation view also attributes 22.19% of sampled pressure to `NativeSpillValueCodec` construction, 3.83% to `AggState.boundedConcatenations`, and 2.98% to `NativeResultCell.term`, consistent with materialization and value handling.

The appropriate algorithmic target is a keyed compatibility lookup at the lexical boundary, or a safely correlated owner lookup, rather than repeated full replay. A keyed implementation must handle unbound shared fields, exact RDF-term compatibility, duplicate right rows, null extension, bounded memory/spilling and the original evaluation frame. Scalar-property lookup must preserve missing-value behavior, distinct-value cardinality checks and error propagation. Removing these semantics would invalidate the comparison.

Primary implementation locations: `GqlCompiler.flush()`, `LexicalLeftJoinCursor` in `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativeLeftJoinPlans.java`, and `GenericEvalPlan`. The baseline [NAMED property diagnostic](plans/baseline-plan-gql-named-property_filter.log) shows the same replay boundary; this cost is not confined to merged graphs.

## Merged topology: deduplication and repeated analysis

The topology worker-stack summary records `AbstractQueryModelVisitor.meet` in 14.62% of samples, `RowDistinctPlan$1.next` in 6.51%, `QueryAlgebraBindingAnalysis.transfer` in 4.75%, and `QuerySafetySnapshot`'s visitor in 4.07%. The whole-recording hot-method view is dispersed: its leading entries are `AbstractQueryModelVisitor.meetNode` at 3.91%, `PatternCursor.next` at 3.81%, and `HashMap.putVal` at 3.39%. This supports reducing analysis repeated during evaluation planning as well as traversal overhead; parsing is outside this benchmark method.

The allocation view attributes 23.18% of pressure to `PrimitiveTupleTable` construction, 9.73% to `IdentityHashMap.init`, 7.76% to `HashMap.newNode`, and 3.44% to algebra-node cloning. These figures identify allocation sites, not independent speedup budgets.

The [baseline merged GQL plan](plans/baseline-plan-gql-merged-topology.log) is `Join(RowDistinctPlan, RowDistinctPlan)`. `GqlCompiler.edgeRelation()` requires DISTINCT per edge to collapse the same SPO across contexts. `compileDistinctStatement()` retains that operation natively, but `SlotPlan.canFlatten()` excludes `RowDistinctPlan`, preventing the path from becoming one `MultiJoin`. Each opened distinct cursor also creates a tracker. The NAMED topology plan is already a `MultiJoin` of filtered statement patterns.

The resulting target is to prove uniqueness from the active storage view when possible, or represent a deduplicated statement leaf without losing safe native planning opportunities. The proof must respect dataset/context selection and snapshot lifetime. Unconditionally dropping merged-edge DISTINCT, or deduplicating the complete path instead, changes graph and bag semantics.

The implemented proof is deliberately conservative: a pinned source with canonical IDs and an unchanged transaction version asks whether its transaction-local catalog has at most one live context, including the default context. A composite delegates only when it has one active statement source; unknown or advancing sources supply no proof. Selecting one graph in a larger multi-context source does not by itself establish this capability. The compiler removes only the physical statement-DISTINCT barrier and leaves the algebra intact, so later evaluations must obtain their own proof. See `NativeLmdbQuerySource.hasUniqueStatementTriples()`, `LmdbSailStore`'s implementations and `TripleStore.hasAtMostOneContext()`.

## Residual label and composition work

All 20 baseline diagnostic cases returned the expected counts. LABEL still produces three `GenericEvalPlan(node=Distinct)` subplans in both graph modes. Its `Distinct(Projection(Filter(StatementPattern)))` shape misses the direct statement-DISTINCT recognizer. A fixed named context with one fixed label offers a narrow uniqueness simplification; disjunction, wildcard labels and merged contexts need separate proofs. These label subplans preserve outer bindings and support correlated memoization, so their global cardinality estimates do not establish full-label scans.

NEXT_UNION duplicates the incoming stage in the GQL algebra: its physical plan has four native subqueries, compared with two in the equivalent SPARQL fixture. Sharing an expensive incoming working table is a remaining opportunity, subject to scope and multiplicity guarantees. This fixture's incoming stage is only one bound hop.

The diagnostic engagement counters confirm access routes but do not measure work or latency. For MERGED TOPOLOGY, GQL records 36 adjacency lookups/0 kernel views versus SPARQL 18/4; for MERGED LABEL, 107/0 versus 66/8. Yet NAMED LABEL records 107/0 for GQL versus 141/0 for SPARQL. More lookups or kernel views alone do not rank performance. The `Executed` JSON contains execution-path annotations but no numeric actual row/work counts; use untimed `Explanation.Level.Telemetry` diagnostics if those counts are required.

## Algorithm comparisons from the supplied primary sources

The supplied archives were inspected as implementation references; their programs were not executed and no source was copied into this report. Archive hashes and unverified embedded commit comments are recorded in [reference provenance](../../conformance/references/sources.json); upstream links identify the projects, not independently verified archive versions.

- **[DuckDB-GQL](https://github.com/rahul-iyer/duckdb-gql):** `src/gql_optimizer.cpp::PushFilter` respects null-extended OPTIONAL boundaries, while its access search seeds label postings on demand rather than expanding entire labels when a bound connected frontier is cheaper. `src/gql_csr.cpp::CsrExpandFunction` fills vectors across multiple input seeds and can filter neighbor labels during expansion. `GqlNeighborLabelIsSelective` considers whether the extra membership check is worthwhile. These support preserving selective bound inputs and avoiding unnecessary operator/materialization boundaries.
- **[froGQL](https://github.com/pleiad/frogql):** `src/runtime/engine.rs::get_candidate_nodes` tries pinned inputs and indexed property equality before label candidates; `smallest_label_set` chooses the smallest required-label posting. Full descriptor checks remain after narrowing. `src/optimizer/pushdown.rs` pushes supported top-level conjuncts into node candidate filtering. The relevant idea is bounded candidate generation with residual semantic checks, not treating an index hit as the entire predicate.

These source comparisons justify algorithm choices; they provide neither cross-engine speedup measurements nor an ISO conformance oracle.

## Fixture and interpretation limits

This is a static, regular two-hop count workload over one named context. All roots have private middles but share leaves: increasing roots preserves output while increasing leaf incoming degree, so a scaling regression can reflect reverse traversal as well as a global scan. Fanout is uniform within each layer; the benchmark does not explore other degree distributions. LABEL has a 100% match rate; PROPERTY_FILTER has one integer per middle and approximately 50% selectivity. The queries are equivalent on this fixture, not on arbitrary multivalued RDF property data. There are no concurrent writes, cross-context duplicates or cold-store measurements.

Restrict performance claims to the configurations with recorded paired results, and keep prepared evaluation and prepare-plus-evaluate results distinct. Matched SPARQL timings, different selectivities and additional execution-route controls are separate comparisons unless their results are explicitly included. The uninstrumented comparison must use the same extended harness on both revisions; none of the JFR profile timings above establish a speedup.
