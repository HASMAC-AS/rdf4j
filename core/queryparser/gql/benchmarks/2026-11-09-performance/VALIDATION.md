# Validation of the GQL/LMDB performance changes

The final source, including the scalar-property child dispatch through `genericOnlyView()`, passed **698 selected test invocations across 38 classes, with zero failures, errors or skips**. The [machine-readable summary](validation/summary.json) and [per-class GREEN reports](validation/green/) contain the final results. This is targeted regression evidence for the changed query paths; it is not a full-repository CI run or a declaration of complete ISO GQL conformance.

## Final selection

| Module | Test invocations | Failures / errors / skips | Principal coverage |
| --- | ---: | --- | --- |
| Query algebra model | 26 | 0 / 0 / 0 | Scalar-property algebra contracts, binding analysis, subquery scope and analysis-cache behavior |
| Query algebra evaluation | 390 | 0 / 0 / 0 | Scalar validation, optimizer safety and reuse, statement binding compatibility, join scope, null contexts, input preservation and lookup behavior |
| GQL parser/evaluation | 180 | 0 / 0 / 0 | Syntax, numeric and scalar expressions, property access, graph selection, pipelines, bag semantics and resource limits |
| LMDB | 102 | 0 / 0 / 0 | Three execution routes, snapshot-bound DISTINCT proofs, context changes, cancellation, partial-result closure, rollback and adjacency engagement |
| **Total** | **698** | **0 / 0 / 0** | **38 classes** |

The LMDB total comprises 83 `GqlLmdbIntegrationTest` invocations, eight snapshot/DISTINCT-proof cases, six existing native statement-DISTINCT cases, four row-plan cancellation/poll-point cases and one repository-level GQL evaluation case. Parameterized route invocations are counted separately; the total is not a count of distinct query strings or ISO features.

The published [summary](validation/summary.json) matches the final run's machine-readable summary exactly, and [all 38 class reports](validation/green/) are bundled. The reactor records successful runs for the four modules above on JDK 25.0.5; earlier build-only or test-compilation attempts are not included in the 698 passes.

## Reproduce the selected tests

From the repository root, use JDK 25 and the workspace Maven cache. Install prerequisite modules before running the selected test modules:

```sh
mvn -B -ntp -o -Dmaven.repo.local=.m2_repo -Pquick install
mvn -B -ntp -o -fae -Dmaven.repo.local=.m2_repo \
  -pl core/queryalgebra/model,core/queryalgebra/evaluation,core/queryparser/gql,core/sail/lmdb \
  '-Dtest=Gql*Test,QueryAlgebraBindingAnalysis*Test,QueryJoinOptimizerReorderSafetyTest,QueryJoinOptimizerTest,FilterOptimizerTest,AlgebraEvaluationSafetyTest,QuerySafetyAnalysisReuseTest,QueryEvaluationUtilityTest,PreparedSafetyAnalysisTest,QuerySafetyNameFactsTest,StatementPattern*Test,ScopedJoinCompatibilityTest,ArrayBindingBasedQueryEvaluationContextTest,OptimizerInputPreservationTest,SameTermFilterOptimizerTest,LmdbNativeDistinctStatement*Test,LmdbNativeRowPlanPollPointTest' \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

The offline commands require a populated `.m2_repo`; obtain any missing dependencies through the normal repository build process first. The recorded final validation invoked `surefire:test` with this module and test selection after installation and test compilation had completed. A fresh checkout should use the `test` lifecycle shown above so current source and test classes are compiled before Surefire runs. The prerequisite installation is not part of the 698 reported test invocations.

## Preserved RED evidence

Behavior and work-bound regressions were recorded against the production baseline `dd1be8b6200afeb755c4278a5a542ac59cbff6ff` before the corresponding production changes. A preliminary test-compilation failure was corrected before obtaining these executable failures and is not treated as behavioral RED evidence.

| Focused selection | Baseline result | What the failing assertions establish | Final result |
| --- | --- | --- | --- |
| `GqlScalarPropertyEvaluationTest` | 20 run; 7 failures; 0 errors/skips | Absent owners open a property lookup; a nullable bound owner loses its existing property; scanning continues beyond the second distinct value; interruption is not observed promptly | 21 run; all pass, including the additional selective-owner regression |
| `LmdbNativeDistinctStatementSnapshotTest` | 8 run; 6 failures; 0 errors/skips | A source with a valid uniqueness proof retains an unnecessary DISTINCT barrier, preventing expected native plan admission and join flattening | 8 run; all pass |
| Baseline LMDB integration controls accompanying native RED | 74 run; all pass | Existing result, route and lifecycle behavior already holds while the optimization-admission assertions fail | Expanded final integration selection: 83 run; all pass |

The scalar failures are preserved in the [property RED report](validation/property-red/org.eclipse.rdf4j.query.parser.gql.GqlScalarPropertyEvaluationTest.txt). In particular, three absent-owner forms expected zero property lookups but observed one; the second-distinct-literal case expected to stop after three statements but observed 259. The duplicate-literal and nonliteral interruption cases expected cancellation to propagate, and the nullable bound-owner case expected `Alice` but received an absent value. These are deterministic value/work/lifecycle assertions, not elapsed-time thresholds.

The [native RED report](validation/native-red/org.eclipse.rdf4j.sail.lmdb.evaluation.LmdbNativeDistinctStatementSnapshotTest.txt) records plan-admission failures, including `RowDistinctPlan` where a proven-unique statement can remain a native pattern and an ordinary join where the diamond traversal can flatten. The [74 passing integration controls](validation/native-red/org.eclipse.rdf4j.sail.lmdb.GqlLmdbIntegrationTest.txt) remain alongside it. The admission failures should not be described as incorrect baseline query answers.

## Semantics and resource behavior protected by the final tests

The scalar-property tests assert that an absent optional owner, explicit null owner or missing parameter cannot become a wildcard property scan. A bound owner reads only its own property, with the same result when unrelated owners are added. Lookup stops on the second distinct literal, closes the cursor on success/error/cancellation, and preserves the interrupt. Repeated references preserve incoming row multiplicity.

Property cardinality uses exact RDF-term equality: identical literals repeated across merged contexts remain one scalar, while different lexical RDF terms are not collapsed merely because their numeric values compare equal. Nonliteral objects are excluded. Multi-value errors remain observable through CASE, COALESCE and otherwise unused expressions. Dataset selection, explicit null context, named-graph parameters and missing graph parameters are checked. A retained prepared value step is evaluated again after the source changes, guarding against an improperly persistent property-value cache.

The native DISTINCT tests retain the algebraic DISTINCT and vary whether the current source can prove its physical implementation unnecessary. They cover zero/one context, multiple contexts, distinct walks through a diamond, an old read view across a new-context commit, synthetic/composite source wrappers and multiple active statement planes. The optimization must decline when it lacks a valid proof; it must not deduplicate whole paths or reuse a committed-view proof over mutable writes.

The LMDB implementation of the capability requires canonical IDs and a pinned, unchanged source view whose live context catalog contains at most one entry. A composite may delegate that proof only with one active statement source. This is a sufficient source-wide proof, not an assumption that a query selecting one graph makes every source unique; the tests preserve the fallback for multiple contexts and active planes.

`GqlLmdbIntegrationTest` exercises cursor execution, generic adjacency execution and native adjacency execution. Its property cases change prepared graph bindings, preserve nullable owners, retain transaction snapshots, observe their own writes, reject multiple values and recover after rollback. Existing traversal/pipeline cases continue to check merged-context edge deduplication, exact path bags, NEXT/UNION scope, retained snapshots, partial closure, interruption and actual adjacency use. Source: [GqlLmdbIntegrationTest.java](../../../../sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/GqlLmdbIntegrationTest.java).

## Matched GREEN evidence for the final dispatch adjustment

The final LMDB override prepares the scalar property's child through the existing `LmdbNativeEvaluationStrategy.genericOnlyView()`. This prevents each bound owner lookup, including a graph-selection filter's child, from opening a complete native tuple evaluation. The view retains the source, dataset and evaluation context; the change selects a physical execution path without changing the scalar contract.

The final untimed property diagnostics for [NAMED](plans/final-plan-gql-named-property_filter.log) and [MERGED](plans/final-plan-gql-merged-property_filter.log) retain the optimized/executed plans and verify the independently expected count of 512 paths at fanout 32/root count 64. These are plan and result evidence, not latency measurements.

This localized change used the repository's **Routine B** procedure for behavior-preserving optimization with existing direct coverage. The complete `GqlLmdbIntegrationTest` selection passed **83/83 both before and after**, with zero failures, errors or skips. Its native route reaches the edited dispatch, and the same property fixtures also run on both generic routes. The bundled [coverage trace](validation/native-scalar-hit-proof.txt) identifies the compiler construction, dynamic evaluator dispatch and integration call sites. Preserved matching reports are:

- Before: [83 passing integration invocations](validation/native-scalar-pre-green.txt).
- After: [83 passing integration invocations](validation/green/org.eclipse.rdf4j.sail.lmdb.GqlLmdbIntegrationTest.txt).

The complete 698-test selection was also rerun after the adjustment, as recorded by the final summary and class reports above. Repeated runs provide matched controls; they are not additional unique tests and do not establish a speedup. No new failing semantic regression is claimed for this physical-path adjustment.

## Corrected snapshot fixture and evidence limits

An initial integration-test fixture selected `SNAPSHOT_READ` while expecting the same transaction-wide dataset snapshot across separate query evaluations. RDF4J distinguishes query-consistent `SNAPSHOT_READ` from transaction-consistent `SNAPSHOT`. The fixture was corrected to `SNAPSHOT`, matching the behavior it intended to test. This was a correction to the test's isolation contract, **not evidence of a product snapshot bug**. The final snapshot assertions remain in the passing route matrix; no test was skipped to obtain the final result.

The tests establish correctness and bounded work for their explicit fixtures and selected execution routes. They do not replace a full standard conformance suite, broad concurrent-workload validation or performance measurements. Profiling interpretation is documented in [PROFILE.md](PROFILE.md); warmed matched benchmarks and their uncertainty are reported separately. Test pass counts, wall-clock test durations and JFR samples are not interchangeable with query-speedup evidence.
