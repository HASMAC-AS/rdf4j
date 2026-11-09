# Validation of the GQL query implementation

Validated on 2026-11-09 using Eclipse Adoptium JDK 25.0.5+8 and Maven. The base is HASMAC-AS/rdf4j main at `ec7e48248eb20c4f7681d8ea5cbed3100718527a`. This report covers the documented query profile in [README.md](README.md); it is not an ISO conformance certificate.

## Final formatted snapshot

All **207 selected tests passed**, with zero failures, errors, or skips:

| Selection | Tests |
| --- | ---: |
| GQL parser, lexer, expressions, semantic Repository API cases, and resource limits | 90 |
| GQL scalar-cardinality and boolean helpers, including fatal-error deferral | 16 |
| Existing evaluation/function/constant-optimization regressions | 52 |
| GQL LMDB repository and three-route integration matrix | 39 |
| Native DISTINCT statement planning and execution | 6 |
| Existing native row-plan cancellation polling | 4 |
| **Total** | **207** |

The initial base build passed all 125 reactor modules with tests skipped. After implementation, LMDB and all its upstream modules, including the new GQL module and changed evaluation code, were rebuilt and installed successfully. The final tests used those installed artifacts. This is a targeted validation run; the entire repository test suite and GitHub CI were not executed as part of this local result.

Repository Spotless formatting used Eclipse 4.40 and the repository's XML settings, restricted to 28 changed Java/POM files across seven modules. After fetching missing formatter dependencies once, the same formatter passed offline. Copyright/SPDX verification and `git diff --check` passed. No dependency versions were substituted to obtain these results.

## Test-first evidence and review findings

Before production changes, `GqlAvailabilityTest` failed with:

```text
Tests run: 1, Failures: 0, Errors: 1, Skipped: 0
UnsupportedQueryLanguageException: No factory available for query language GQL
```

The same parser-registry assertion now passes in the GQL module. Further failing cases drove the fixes for native per-statement DISTINCT, constant-folding fatal-error preservation, property cardinality and boolean strictness, NULLIF null behavior, grouping/ordering scope, grouped edge identities, node wildcard labels, comment-safe result names, and parser/algebra construction limits. Full red and green Surefire reports were retained in the delivery evidence.

The LMDB tests run with ordinary cursor access, generic adjacency access, and native adjacency enabled. They verify actual adjacency counter increases, outgoing/incoming paths, diamond multiplicities, context-union edge deduplication, full edge identities, literal/label/triple-term exclusions, prepared parameter changes, optional null preservation, write/read/rollback behavior, retained snapshots across another connection's commit, committed deltas, reopen, result closure, and interruption.

## Reproduction

From the repository root, with JDK 25 and dependencies available:

```sh
mvn -B -ntp -o -Dmaven.repo.local=.m2_repo \
  -pl core/sail/lmdb -am -Pquick install

mvn -B -ntp -o -Dmaven.repo.local=.m2_repo \
  -pl core/queryparser/gql test

mvn -B -ntp -o -Dmaven.repo.local=.m2_repo \
  -pl core/queryalgebra/evaluation \
  -Dtest='Gql*Test,DefaultEvaluationStrategyValueExprErrorTest,ConstantOptimizerTest,UndeclaredFunctionPolicyTest,FunctionDeterminismTest,ExtensionFunctionRegistrationContractTest' test

mvn -B -ntp -o -Dmaven.repo.local=.m2_repo \
  -pl core/sail/lmdb \
  -Dtest='GqlLmdb*Test,LmdbNativeDistinctStatementTest,LmdbNativeRowPlanPollPointTest' test
```

If dependencies are missing, retry the relevant command online before returning to offline. The `-am` command above uses the quick profile with tests skipped; the test commands deliberately omit `-am`.

## Performance evidence

The [benchmark report](benchmarks/2026-11-09/README.md) and [raw JMH results](benchmarks/2026-11-09/results.json) cover eight GQL/SPARQL, adjacency, and graph-selection combinations at fanout 32. Every case returned the required 1,024 walks. GQL used adjacency for merged and named graphs; the named-graph case also used generated kernels. The merged GQL case used adjacency without generated-kernel views in this workload.

GQL's measured means were higher than the corresponding SPARQL means. The short run's uncertainty intervals were broad and overlapping, so it does not establish an adjacency speedup or a general language-performance ratio. Additional RDF node-kind checks and merged-edge deduplication are part of GQL's projection semantics. Loading, parsing, and adjacency construction were excluded from timing; query-result caching was disabled.
