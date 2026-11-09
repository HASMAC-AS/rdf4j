# Validation of the supplied-reference implementation batch

This batch continues from `b68fe28508de9c44bce869c779b7666a27001cfd` on `GH-0000-gql-lmdb-adjacency`. It uses the five user-supplied archives documented in [the reference audit](references/README.md), together with the already reviewed official grammar and 2026 corrigendum. Engineering continues without requiring the user to obtain the full base publication. **Complete ISO GQL implementation and verification remain unfinished.**

## Implemented and tested behavior

| Area | New behavior and distinguishing checks |
| --- | --- |
| Read-query composition | NEXT and optional YIELD select/rename prior result columns; UNION completes before NEXT; each later UNION branch receives the same input relation |
| Scope and identity | Fresh private column names separate stage variables from API parameters; prior grouping/aliases do not leak; exported node and RDF triple edge identities remain usable |
| Stage boundaries | YIELD follows DISTINCT and LIMIT; a later filter cannot change which rows a preceding LIMIT selected; nulls and duplicate rows survive |
| Basic spellings | Untyped LET VALUE and FILTER WHERE compose with other supported clauses |
| Simple CASE | Multiple WHEN operands, comparison tails, and existing scalar null/truth tails; later operands can match; selected results remain lazy; malformed tails and resource exhaustion are rejected |
| Numeric literals | Digit separators, binary/octal/hex integers, exact M suffix, float F suffix, double D suffix, and exponent forms retain exact RDF datatypes and valid lexical boundaries |
| Unsigned scalar positions | LIMIT, OFFSET/SKIP, and finite path bounds accept radix/separator integers; reject signs, fractions, exponents, suffixes, and overflow; existing path limits remain |
| Shared evaluator | Optimizer-specialized named variables reject conflicting incoming bindings before iteration, existence/count lookup, or cache reuse; ordinary constants remain exempt |
| Explicit nulls | The subject/predicate-only variable combination rejects either explicit null instead of requiring both to be null |

The code is original and adds no third-party dependency. Numeric scanning is linear in token length. Integer pagination/path bounds are parsed without normalized strings or BigInteger allocation. Exact decimal exponent expansion is bounded before allocating its plain lexical form. The statement evaluator compiles checks only for specialized named variables, leaving ordinary statement patterns without additional per-row compatibility comparisons. These are implementation properties, not measured speedup claims.

## Test-first evidence

Production edits followed observed automated failures in this repository. The original reports were retained before subsequent runs overwrote Maven's report directories; root `initial-evidence.txt` also retains their snippets.

| Pre-change selection | Tests | Failures | Errors | What the failure established |
| --- | ---: | ---: | ---: | --- |
| New numeric, CASE, and pipeline classes | 46 | 7 | 36 | Missing language behavior before lexer/parser/compiler edits |
| StatementPatternBindingCompatibilityTest, initial cases | 16 | 4 | 0 | Conflicting values accepted in subject, predicate, object, and context positions |
| Real SPARQL subquery, three LMDB routes | 3 | 2 | 0 | Cursor and generic adjacency returned eight rows instead of two; native adjacency already returned two |
| Pipeline class with added unsigned-bound cases | 20 | 0 | 2 | Expressions accepted radix/separators while integer-only clauses rejected them |
| Subject/predicate explicit-null pair | 2 | 2 | 0 | The pair-specific helper used AND where either null must reject the input |

An early test-compilation error was corrected before the language RED run; it was not treated as failing behavioral evidence. Additional cache, repeated-variable, numeric boundary, named-graph, and transaction tests strengthen verification without replacing or weakening the original failure cases.

Representative retained summaries:

```text
Language RED:            Tests run: 46, Failures: 7, Errors: 36, Skipped: 0
Binding RED:             Tests run: 16, Failures: 4, Errors: 0,  Skipped: 0
Subquery LMDB RED:       Tests run: 3,  Failures: 2, Errors: 0,  Skipped: 0
Unsigned-bound RED:      Tests run: 20, Failures: 0, Errors: 2,  Skipped: 0
Subject/predicate RED:   Tests run: 2,  Failures: 2, Errors: 0,  Skipped: 0
```

## Final verification

Validated on 2026-11-09 with Eclipse Adoptium JDK 25.0.5+8 and Maven 3.9.16. All **397 selected test executions passed**, with zero failures, errors, or skips:

| Final selection | Tests |
| --- | ---: |
| Complete GQL module: nine lexer/parser/MemoryStore/registry/resource-limit classes | 159 |
| Targeted shared-evaluator regressions: ten classes | 159 |
| GQL LMDB repository and three-route integration | 69 |
| Existing native DISTINCT statement regressions | 6 |
| Existing native row-plan cancellation polling | 4 |
| **Total** | **397** |

The final evaluator selection includes all 34 binding-compatibility/null cases plus existing constant-optimizer, scoped join, input-preservation, array-binding, statement lookup/cache/count/seek, and remote blank-node regressions. The final GQL verification was split across two reactor invocations (78 plus 81 executions); the count above includes each class once. An earlier standalone full GQL-module run also passed all 159 tests.

The LMDB matrix uses cursor, generic adjacency, and native adjacency configurations. Its NEXT traversal checks actual adjacency lookup counters. The composed tests verify exact duplicate bags, node/edge identity, named-graph and start-parameter rebinding, an absent graph binding, and edge-kind preservation through repeated NEXT/YIELD and UNION.

Two transaction cases retain a partially consumed composed query across a writer commit and exercise read-own-writes, partial close, rollback, a later commit, and prepared-query reuse. Retained readers see the old four-row bag while fresh readers see the new two-row bag. Existing tests additionally cover reopen, graph projection, property multiplicity, cancellation, and ordinary query transaction behavior.

The permanent SPARQL reproducer now returns Bob and Carol exactly once on every route, and rebinding its prepared parameter to Bob returns Dora once. [The regression report](KNOWN_EVALUATION_ISSUE.md) preserves the complete fixture, original mechanism, and correction.

## Artifact and source checks

The prerequisite LMDB reactor install succeeded with tests skipped, publishing the updated evaluator and GQL parser to the workspace Maven repository before downstream execution. Every generated class corresponding to the five changed production classes, including their nested classes (36 class files), was byte-compared against the installed jars and matched the final compiled output. This verifies that downstream tests used the updated implementation.

The repository formatter ran successfully for all three affected Java modules. The repository copyright/SPDX script and `git diff --check` passed. No dependency versions were substituted. [The sanitized validation summary](references/validation-summary.json) lists every executed test case and source-file checksums; it excludes system properties and private build settings.

## Reproduction

From the repository root, use JDK 25 and a workspace Maven repository. Resolve missing dependencies once if needed, then return to offline operation. These commands reproduce the equivalent final selection:

```sh
mvn -B -ntp -o -Dmaven.repo.local=.m2_repo \
  -pl core/sail/lmdb -am -Pquick install

mvn -B -ntp -o -Dmaven.repo.local=.m2_repo \
  -pl core/queryalgebra/evaluation \
  -Dtest=StatementPatternBindingCompatibilityTest,StatementPatternQueryEvaluationStepTest,StatementPatternQueryEvaluationStepDirectLookupCacheTest,StatementPatternHasStatementTest,StatementPatternRemoteBNodeTest,StatementPatternSeekForwardingTest,SameTermFilterOptimizerTest,ScopedJoinCompatibilityTest,OptimizerInputPreservationTest,ArrayBindingBasedQueryEvaluationContextTest \
  test

mvn -B -ntp -o -Dmaven.repo.local=.m2_repo \
  -pl core/queryparser/gql test

mvn -B -ntp -o -Dmaven.repo.local=.m2_repo \
  -pl core/sail/lmdb \
  -Dtest='GqlLmdb*Test,LmdbNativeDistinctStatementTest,LmdbNativeRowPlanPollPointTest' test
```

The prerequisite quick-profile install skips tests; enabled test commands omit `-am` and `-q`. Raw logs and original reports are retained under `validation-gql-references/` in the working checkout. They are not all included in the source patch; the committed sanitized summary and this report preserve the reviewable evidence without publishing environment-specific settings or original standards documents.

## Limits and remaining scope

The 581 cases in gql-compat were audited, not imported and declared passing. The supplied projects cover partial or draft language surfaces and contain documented conflicts. These results establish the tested behavior of the RDF-backed profile, not a conformance percentage or certification.

NEXT currently clones its deterministic incoming read algebra into each UNION branch. Future volatile functions, procedures, or mutations require an execution design that preserves once-only working-table evaluation where required. USE GRAPH is local to a linear query/branch; graph ownership is not encoded in the current RDF node/edge values. Typed LET, dynamic pagination, full CASE graph/type predicates, and full GQL numeric/error semantics remain incomplete.

Native property-graph storage still needs independent edge IDs, isolated nodes, graph existence, edge orientation/properties, and graph ownership inside the authoritative LMDB transaction. Complete typed values, catalogs, schemas, GQL mutations, session/program execution, richer result transport, runtime unbounded/shortest paths, and clause-level conformance review are also unfinished. Correcting query composition does not implement those facilities.

Full repository CI and a new performance benchmark were not run. The earlier short comparison did not establish an adjacency speedup; this batch makes no new latency or throughput claim. The next architectural work remains the storage/value vertical slice in the repository-root `gql-conformance-exec-plan.md`.
