# Validation of the first ISO GQL correction batch

This report covers changes after `9e51d79726312dddc9649eb9a1040a7b7e2e9e44`, the initial RDF-backed query profile. The normative target is ISO/IEC 39075:2024 plus ISO/IEC 39075:2024/Cor 1:2026. **The implementation is not fully conformant.** These results establish only the behavior exercised by the selected tests. The complete base standard, clause-level mandatory requirement inventory, feature-dependency review, and full semantic verification remain outstanding.

## Changes covered

- Preserve the syntactic distinction between double-quoted string values and delimited identifiers. Reject delimited binding-variable declarations while retaining delimited labels, property names, parameters, and result-column names.
- Parse raw delimited parameter names without interpreting backslash escapes. Materialize projected parameters as expressions so prepared-query rebinding and clearing are observed correctly.
- Evaluate every right-hand side in one LET statement against the incoming scope; only subsequent LET statements see its newly declared variables.
- Resolve explicit GROUP BY binding references through non-aggregate RETURN aliases, including computed aliases. Reject grouping by an aggregate result or an arbitrary unaliased expression.
- Evaluate aggregate ORDER BY operands over input rows and restrict aggregate ordering to grouped or aggregating results. Keep aggregate operands in the input scope when a RETURN alias shadows an input name.
- Keep internal label and node-population projections in their enclosing GQL scope. An accidental subquery boundary prevented a starting-node constraint from reaching the label scan and multiplied results on generic evaluation routes.

The last two parameter and projection corrections are repository integration defects exposed while testing the normative corrections. They do not establish new ISO conformance claims.

## Test-first evidence

Every production correction was preceded by an observed failing automated test. Preserved RED reports cover double-quoted expression interpretation, LET sibling visibility, grouping aliases, aggregate ordering and alias shadowing, raw parameter lexing, direct parameter projection, and duplicate-sensitive label traversal.

The LMDB multiplicity regression returned Bob and Carol four times each with ordinary cursor and generic adjacency execution; native adjacency returned each once. A separate MemoryStore reproducer isolated the incorrect internal subquery scope from quoting, LET, and property materialization. Tests also exercise a starting node that has outgoing edges but lacks the required label.

## Final formatted results

Validated on 2026-11-09 with Eclipse Adoptium JDK 25.0.5+8 and Maven. All **166 selected tests passed**, with zero failures, errors, or skips:

| Selection | Tests |
| --- | ---: |
| GQL parser, lexer, expressions, MemoryStore semantics, registry, and resource limits | 108 |
| GQL LMDB repository and three-route integration matrix | 48 |
| Native DISTINCT statement planning and execution | 6 |
| Existing native row-plan cancellation polling | 4 |
| **Total** | **166** |

The final GQL artifact was rebuilt and installed before the LMDB tests. The earlier prerequisite install rebuilt LMDB and its upstream modules successfully with tests skipped. The repository formatter passed for all eight changed Java files across two modules. Copyright/SPDX verification and `git diff --check` passed. No dependency versions were substituted.

Logs and selected plain-text Surefire reports are retained in the delivery evidence. Environment-specific Maven settings and original standards publications are excluded. The unchanged evaluation-helper module's 68 tests from the preceding delivery were not rerun and are not included in this count.

## Reproduction

Use JDK 25 and the workspace Maven repository. Install prerequisites with tests skipped before running the selected modules:

```sh
mvn -B -ntp -o -Dmaven.repo.local=.m2_repo \
  -pl core/sail/lmdb -am -Pquick install

mvn -B -ntp -o -Dmaven.repo.local=.m2_repo \
  -pl core/queryparser/gql test

mvn -B -ntp -o -Dmaven.repo.local=.m2_repo \
  -pl core/sail/lmdb \
  -Dtest='GqlLmdb*Test,LmdbNativeDistinctStatementTest,LmdbNativeRowPlanPollPointTest' test
```

The quick-profile install skips tests; enabled test commands deliberately omit `-am`. Missing dependencies must be obtained before returning to offline execution. The repository formatter, copyright/SPDX check, and `git diff --check` are also required before publication.

## Limits of the evidence

The selected tests cover MemoryStore and LMDB cursor, generic adjacency, and native adjacency configurations. The LMDB matrix includes actual adjacency counter assertions, exact result bags, parameter changes, transactional visibility, rollback, retained snapshots, reopen, result closure, and interruption. Passing them does not cover the full language, rich GQL values, independent property-graph storage, mutation and catalog commands, sessions, or all diagnostics.

The entire repository test suite and GitHub CI have not been run for this correction batch. The original [207-test report](../VALIDATION.md) and [short benchmark](../benchmarks/2026-11-09/README.md) describe the preceding implementation. That benchmark has not been rerun after these corrections and supplies no new performance claim. Its original measurements did not establish an adjacency speedup.
