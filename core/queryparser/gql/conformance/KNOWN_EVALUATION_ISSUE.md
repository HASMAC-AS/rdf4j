# Uncorrected generic-evaluator subquery join issue

## Status and scope

**Uncorrected.** Investigation of the GQL correction tests exposed a separate SPARQL result-multiplicity issue in the generic evaluator. This report preserves the reproducer; the GQL correction does not modify the generic evaluator or claim to repair this SPARQL case.

The relevant SPARQL parser, `SameTermFilterOptimizer`, `JoinQueryEvaluationStep`, and `StatementPatternQueryEvaluationStep` sources are unchanged from this phase's starting commit, `9e51d79726312dddc9649eb9a1040a7b7e2e9e44`. The issue is therefore pre-existing relative to this correction batch. Its earlier introduction point has not been established.

## Complete fixture

Load the following nine statements into the default graph of an otherwise empty LMDB repository and commit:

```turtle
@prefix ex: <urn:gql:> .

ex:alice a ex:Person ;
    ex:knows ex:bob, ex:carol ;
    ex:name "Alice" .
ex:bob a ex:Person ; ex:knows ex:dora .
ex:carol a ex:Person ; ex:knows ex:dora .
ex:dora a ex:Person .
```

There are four people and four `ex:knows` edges. The name property is retained to reproduce the original fixture but is not referenced by the SPARQL query.

## Complete query and evaluation

```sparql
SELECT ?friend
WHERE {
  {
    SELECT DISTINCT ?n
    WHERE {
      ?n a <urn:gql:Person>
    }
  }
  ?n <urn:gql:knows> ?friend
  FILTER (?n = ?start)
}
ORDER BY ?friend
```

Prepare the query as `QueryLanguage.SPARQL`, call `setIncludeInferred(false)`, and bind `start` to the IRI `urn:gql:alice` through `setBinding`. Consume the complete `TupleQueryResult` from `evaluate()` and close it. The probe used normal repository preparation and evaluation, without modifying the parsed algebra or evaluator.

The expected ordered result contains exactly these two bindings:

| Row | friend |
| --- | --- |
| 1 | `urn:gql:bob` |
| 2 | `urn:gql:carol` |

The subquery produces one row for each person. The outer join shares `n`; only compatible mappings join. Filtering `n` to Alice leaves her two outgoing edges. SPARQL's join definition is available in the [W3C SPARQL 1.1 Query specification](https://www.w3.org/TR/sparql11-query/#defn_algJoin).

## Observed execution routes

All routes used `LmdbStoreConfig("spoc,posc")`. Adjacency routes selected `DirectAdjacencyMode.PREFER`, disabled automatic construction during initialization, and built adjacency after committing the fixture, using the same test helper as `GqlLmdbIntegrationTest`.

| Route | Native evaluation | Direct adjacency | Observed result |
| --- | --- | --- | --- |
| Cursor | Disabled | Disabled | Bob four times, then Carol four times: **8 rows** |
| Generic adjacency | Disabled | Enabled and built | Bob four times, then Carol four times: **8 rows** |
| Native adjacency | Enabled | Enabled and built | Bob once, then Carol once: **2 rows** |

Removing `DISTINCT` from the subquery gives the same results on this fixture: eight rows on both generic routes and two on the native route.

## Evidence and mechanism

The original LMDB GQL test produced a retained Surefire failure with the same fourfold multiplication. A separate Java probe then executed the complete SPARQL query above using the installed dependencies from that Surefire run. It also obtained an optimized explanation through `query.explain(Explanation.Level.Optimized)`.

The optimized generic plan has these relevant properties:

- The left operand of a `JoinIterator` is a subquery projection of `n` over all four Person statements.
- The right operand is a `StatementPattern` whose named variable `n` has been specialized to the fixed value `urn:gql:alice`.
- The outer equality filter has been removed.
- The generic statement step uses the specialized value for lookup and restores it to the output binding, accepting incoming left rows with conflicting values of `n`.

Consequently, each of the four left rows produces Alice's two outgoing edges. The observed native route retains the required compatibility behavior and produces two rows.

This diagnosis points to the interaction between scope-aware specialization and generic compatible-binding enforcement. It is not evidence that subqueries should become correlated or that ordinary query constants should be constrained by unrelated same-named bindings. A future fix needs focused generic-evaluator tests and SPARQL scope regressions before changing these shared components.

## Relationship to the GQL correction

The GQL compiler used `Projection` for internal column pruning while leaving its default `subquery=true`. Those internal projections do not introduce a new language scope. Marking them as ordinary projections is the appropriate GQL lowering correction; it allows specialization to see the same generated variable consistently and prevents the duplicate rows in the GQL matrix.

That correction does not remove real SPARQL subquery boundaries and does not repair the independent SPARQL reproducer documented here. Keep both facts distinct when reporting test results or conformance progress.
