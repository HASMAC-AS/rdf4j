# DuckGQL and MGQL reference audit

Reviewed the user-supplied `duckdb-gql-main.zip` and `mgql-main.zip` on 2026-11-09. This was source inspection and original inventory analysis only: no downloaded build scripts, engines, or tests were executed. Paths below are relative to the extracted archive roots.

## Practical conclusion

These archives provide useful implementation and test material, but neither supplies a complete ISO semantic oracle. DuckGQL is the stronger engineering reference for typed lowering and adjacency execution. MGQL is the stronger small, explicit mathematical reference for graph identity, direction predicates, compatible-binding joins, bag operators, and a schema-aware type checker. Each requires semantic review before porting expected results.

The most immediately actionable discrepancy is also a warning: DuckGQL intentionally tests sibling-visible LET definitions, which conflicts with RDF4J's already applied 2026-corrigendum correction. Preserve the corrected incoming-scope behavior instead of importing this expectation.

## Provenance and license

| Archive | Observed project license | Embedded material requiring separate attribution |
| --- | --- | --- |
| DuckGQL | MIT-style grant in `LICENSE`, copyright Rahul Iyer and Stichting DuckDB Foundation | `third_party/opengql/LICENSE` is Apache-2.0; the clause breadth corpus is derived from the Apache-2.0 openCypher TCK and records its source separately |
| MGQL | MIT in `LICENSE`, copyright The MGQL Authors | No extra dependencies beyond pinned core Lean are declared in its README; theorem/code reuse still retains the MIT notice |

DuckGQL identifies itself as `rahul-iyer/duckdb-gql`; MGQL identifies itself as `EngineeringSoftware/mgql`. The uploaded ZIPs do not themselves establish an immutable upstream commit, so use their archive hashes as the local provenance pin rather than guessing a commit from `main`.

DuckGQL's `duckdb/` and `extension-ci-tools/` submodule directories are empty in this ZIP. Its original `test/features/clauses/` subtree and referenced `clauses/SOURCE.json` are also absent, although all 93 generated SQLLogic feature specifications under `sqllogic/clauses` are present. The archive therefore is not a standalone DuckGQL build checkout or a complete original TCK source export.

## Coverage: what is actually present

### DuckGQL

`README.md` explicitly calls the implementation experimental, incomplete, and nonconforming. Static inventory of `test/conformance/iso-gql-2024.tsv` found **36 project-defined clause-family rows: 24 partial, 12 planned, zero implemented**. These `GQL-14.7`-style identifiers are project clause-family labels, not the official optional-feature identifiers in RDF4J's inventory.

There are 70 `.test` files under `test/sql`. The breadth manifest `test/features/sqllogic/manifest.tsv` records **827 scenarios: 93 executable and 734 pending**. Its fixture inventory records 548 setup entries, 532 ready and 16 pending. These counts are inventory values, not a test run performed here.

`test/features/README.md` documents that the breadth corpus is an adapted openCypher TCK. It explicitly states that generated files are not executed as Gherkin and that pending ports are not passing tests. Its mechanical `WITH` to `LET` conversion is syntax-shaped only and has a `with_scope_translation` blocker. `execution.tsv` has 93 promoted rows, each naming a local executable test.

The script `scripts/gql_conformance/check_gql_conformance.py` validates manifest structure, a fixed grammar-rule count, and fixture generators. Its release flag rejects incomplete families. A passing ordinary invocation is therefore not semantic conformance evidence.

### MGQL

`README.md` describes a Lean 4.14.0 artifact with equivalent big-step/small-step engines, a sound type checker, and type-soundness proofs. The source contains explicit theorems and proof bodies in `MGQL/Metatheory.lean`, `MGQL/SmallStep.lean`, and `MGQL/TypeChecker.lean`; I did not execute Lean to recheck them. The README's assertion totals and award claims are not independently verified by this audit.

`HOWTO.md` precisely scopes the fragment: USE/MATCH/WHERE/RETURN, seven edge directions, label expressions, property constraints, quantified edges and patterns, pattern conjunction, three-valued predicates, selected aggregates, projections, and UNION/OTHERWISE/EXCEPT/INTERSECT composition. OPTIONAL MATCH, ORDER BY/LIMIT, shortest paths, writes, and catalog/session operations are outside the fragment. The type checker is explicitly **sound but not complete**: rejection does not establish that a query is untypable.

Formal soundness is proved relative to the artifact's own calculus and graph/schema assumptions. It is not a proof of complete ISO implementation equivalence.

## Concrete conflicts and boundaries

1. **LET sibling visibility.** `duckdb-gql-main/test/sql/gql_open_gql_extensions.test` expects `LET VALUE base_value = node.id, incremented = base_value + 1, VALUE final_value = incremented + 1` to produce consecutive values. RDF4J must retain the incoming-scope rule already established from the corrigendum. The optional `VALUE` spelling can be reviewed against the supplied grammar independently of this conflicting expectation.

2. **Unbounded path limits.** `mgql-main/MGQL/Semantics.lean`, `quantifierNormalize` and `evalPattern`, cap unbounded repetition at the graph's edge count explicitly for trail semantics. `evalQuery` always uses `evalPatternTrail`. Do not copy this bound into arbitrary WALK queries or infer the ISO default match mode from it.

3. **Expression domain is intentionally narrow.** MGQL's primitive values are integer, string, and boolean, with node/edge references, null, and lists added by `Value`. `evalRelOp` implements comparisons only on integers and returns unknown for string/boolean operands. `evalArith` uses Lean integer arithmetic and maps division by zero to null. These are not general ISO scalar/type/error rules to copy into RDF4J.

4. **Aggregation is a calculus encoding.** `evalQuery` maps projections over every matched row; top-level aggregates in `projectItem` read the whole matched binding table. The code therefore is not a general grouped RETURN engine and is not a suitable oracle for global aggregation cardinality, empty-input groups, GROUP BY, or ORDER BY scope. The current RDF4J corrected grouping cases should remain independently specified.

5. **Transaction boundaries differ.** DuckGQL's README restricts CREATE/DROP/COPY/ANALYZE GRAPH and CSR construction/algorithms to autocommit, while graph queries and mutations can participate in caller-controlled transactions. That is an implementation limitation, not a contract for RDF4J's same-LMDB-transaction architecture.

6. **Direction support differs.** DuckGQL's AST has RIGHT, LEFT, and ANY directions; MGQL models all seven direction predicates and stored directedness. Do not mistake traversal in both directions over a directed edge for a stored undirected edge.

## Reusable architecture

### Typed lowering from DuckGQL

`src/include/gql_ast.hpp` keeps source ranges and delimited-identifier metadata, then models a linear query as an ordered clause stream with indices into statement-owned pools. This preserves clause ownership without copying full payloads for physical alternatives. `src/include/gql_ir.hpp` adds bound expressions, dense binding indices, nullable types, graph/procedure binding sources, and logical output/required/nullable/correlated binding sets. `src/gql_binder.cpp` and `src/gql_ir.cpp` show the intermediate stage before relational lowering.

This is a useful shape for replacing RDF4J's direct token-to-RDF-algebra compiler: keep syntax, semantic binding/type analysis, and physical lowering distinct. Internal column pruning and true language scope boundaries must remain distinct. DuckGQL's type enum is still incomplete, so copying it as a finished ISO type model would repeat the current architectural gap.

### Independent graph identity from MGQL

`MGQL/Schema.lean` defines independent finite node and edge domains, edge source/destination functions, edge directedness, node/edge label sets, and separate property maps. `MGQL/Values.lean` identifies graph elements by graph site plus node/edge ID. Consequently, isolated nodes, parallel same-endpoint/same-label edges, and graph-scoped identity are representable directly.

The Java/LMDB model should preserve these distinctions in authoritative records and adjacency entries. An RDF statement identity is insufficient for the complete property-graph model. MGQL's `phiD` in `Semantics.lean` provides a compact truth-table reference for all seven direction choices, including self-loop destination deduplication.

## Adjacency algorithms worth adapting

### Iterative trail enumeration

DuckGQL `src/gql_csr.cpp` defines `CsrPathFrame`, `CsrPathExpandLocalState`, `MakeCsrPathFrame`, and `NextCsrPath`. The implementation uses an explicit DFS frame stack over base and delta adjacency, preserves actual edge IDs, tracks the active path, emits when the minimum length is reached, and unwinds without recursive Java/C++ call-stack growth. Short trails use linear membership checks; after 64 edges it creates a hash membership set and updates it through backtracking.

For RDF4J, reuse retained snapshot/native adjacency cursors and adapt membership thresholds through measurement. Keep full edge identity, exact bags, cancellation, cursor closure, and minimum/maximum semantics in the contract. This is substantially more suitable than bounded algebra expansion for runtime paths.

### Shortest paths

DuckGQL's `CsrVisitedSet` starts sparse and converts to a dense bitmap after 16,384 entries, avoiding full-graph initialization for short searches while improving large searches. `SearchCsrShortest` performs BFS and explicitly permits revisits below a minimum-length constraint. It also changes from one-target search to a complete distance search when repeated targets share a source.

`SearchCsrAllShortest` retains a predecessor DAG with edge IDs, stops after the target's shortest level, then reconstructs only target-reaching paths. `test/sql/gql_all_shortest.test` is especially useful: its hand-worked fixture includes parallel edges, a longer alternate route, a cycle, optional no-path results, direction reversal, and upper-bound exclusion. Parallel edges yield repeated node sequences because edge paths differ. This is a strong acceptance-test pattern for edge identity.

The current ALL SHORTEST implementation materializes all result paths before returning. For production RDF4J, retain the predecessor-DAG concept but enumerate output lazily with explicit stacks and cancellation, since shortest-path output can be exponentially large. The test and transformer restrict some minimum-bound forms; do not extrapolate full shortest-search semantics from this subset.

`src/gql_algorithms.cpp`, `ComputeShortestPathLength`, uses bidirectional BFS for a scalar endpoint-pair distance and chooses the next whole layer by adjacency edge volume rather than frontier vertex count. `WeightedSsspState` uses an indexed binary heap with decrease-key to bound traversal state by O(V) despite parallel edges and repeated improvements. These are useful performance references, but named algorithm procedures are engine extensions rather than ISO GQL query conformance evidence.

### Capability-based adjacency acquisition

`src/include/gql_csr.hpp` separates outgoing/incoming topology, edge IDs, labels, postings, and statistics into capability bits. `GqlCsrSnapshot` stores independent identity/topology arrays and revision metadata. Algorithms request only required capabilities. The general technique can avoid decoding or preparing identity/property data for endpoint-only computations in LMDB; retain RDF4J's existing transaction/snapshot contract rather than transplanting DuckDB cache-generation code.

## Recommended next implementation slices

1. **Reference-case ingestion with provenance.** Store archive hashes, exact source paths, fixture requirements, semantic status, and disagreements in the RDF4J conformance records. Port only reviewed cases into automated tests. Keep grammar acceptance, reference compatibility, and ISO-verified status separate.

2. **Independent graph-element storage acceptance.** Use a fixture with an empty graph, isolated nodes, two parallel edges with different properties, a self-loop, and a stored undirected edge. Require distinct identities and all direction-predicate results before adapting more syntax. Exercise commit, rollback, reopen, and retained readers through existing LMDB transactions.

3. **Typed AST/binding boundary.** Introduce a semantic representation before further direct RDF lowering, guided by DuckGQL's ordered clause stream and binding indices. This enables lists, records, graph elements, paths, and correct nullable/scoped values without encoding them accidentally as RDF literals.

4. **Runtime path evaluation.** Implement explicit-stack trail enumeration against adjacency and test the hand-worked parallel/cycle fixtures. Then add shortest-path operators using edge-preserving BFS/predecessor DAGs and lazy enumeration. Do not merely increase the current 32-hop compiler expansion limit.

5. **Bag composition.** MGQL `applySetOp` and `Test.lean` provide clear independently reviewable examples: EXCEPT ALL multiplicity is `max(leftCount-rightCount,0)`, INTERSECT ALL is `min(leftCount,rightCount)`, and OTHERWISE chooses the right relation only when the left is empty. Use counted hash tables and lazy branch selection rather than the artifact's list scans. Check column/name/type/null equality rules against the grammar and other references before exposing these as complete ISO operators.

6. **Small verified grammar additions.** `LET VALUE` can be added as an optional spelling while keeping incoming-scope LET behavior; a new positive test must not inherit DuckGQL's sibling-reference expectation. Byte literals/length functions and named-path functions require a deliberate value model and are not safe mere aliases to existing RDF string functions.

## No execution or production edits performed

This audit makes no claim that the uploaded engines build, that their reported tests currently pass, or that their performance transfers to RDF4J. No external source corpus, license file, generated parser, or production code was copied into RDF4J by this agent. This report is original analysis intended for root's implementation decisions.
