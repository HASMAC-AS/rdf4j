# Add GQL queries over RDF4J's LMDB adjacency indexes

This ExecPlan is maintained according to `PLANS.md`. Its Progress, Surprises & Discoveries, Decision Log, and Outcomes & Retrospective sections are updated as implementation proceeds.

## Purpose / Big Picture

An application should be able to call `connection.prepareTupleQuery(QueryLanguage.GQL, query)` and evaluate graph patterns over the same RDF data, transaction, query bindings, and LMDB adjacency indexes used by SPARQL. No second graph database or graph copy is introduced. The implementation supplies an explicitly documented GQL query profile; the complete ISO language also includes graph catalog, schema, update, session, and type facilities outside this change. Unsupported syntax must fail during preparation, rather than silently run with SPARQL semantics.

The repository base is HASMAC-AS/rdf4j `main`, commit `ec7e48248eb20c4f7681d8ea5cbed3100718527a`. Work takes place on `GH-0000-gql-lmdb-adjacency`. No issue number was provided.

## Progress

- [x] Inspect fork, parser architecture, and adjacency APIs.
- [x] Research ISO grammar and existing GQL implementations.
- [x] Establish build and failing language acceptance test.
- [x] Implement tokenizer, compiler, registration, and distribution integration.
- [x] Validate language semantics and LMDB execution paths.
- [x] Format, review, benchmark, and package complete changes.

## Surprises & Discoveries

The requested fork main currently predates newer upstream work. Its direct adjacency structures are in memory, derived from LMDB triples and maintained with commit deltas. Reusing them does not introduce persistent adjacency storage or change the LMDB disk format.

SPARQL and GQL differ on missing bindings: a null variable introduced by OPTIONAL MATCH cannot subsequently become a new unconstrained graph variable. RDF properties can also contain multiple values, whereas a scalar property lookup must not multiply graph matches. Both require explicit compiler behavior and tests.

The existing LMDB triple source already chooses adjacency access for ordinary statement patterns. Lowering graph patterns directly into the existing algebra therefore reaches adjacency and native planning without new storage APIs.

## Decision Log

Use the official published ISO/IEC 39075 BNF and the OpenGQL grammar as syntax references. Consult SurrealDB's direct lowering design and DuckGQL's use of derived adjacency for implementation comparisons. Do not copy third-party source code or introduce parser-generator dependencies.

Compile GQL directly to RDF4J TupleExpr, rather than constructing a SPARQL query string. Preserve constant predicates and endpoint bindings so the existing optimizer can select adjacency scans and native kernels. Construct edge triple identities only when needed by expressions or matching modes.

Use full RDF IRIs as GQL delimited identifiers, for example `MATCH (a)-[:\`urn:knows\`]->(b) RETURN a, b`. Bare label and property names use the supplied base URI as an RDF namespace. Literal-valued statements form node properties; rdf:type statements form labels; other resource-to-resource statements form directed edges. Nodes are IRIs and blank nodes occurring as subjects or non-type resource objects. Triple terms are not nodes in this profile. An edge's RDF triple is its identity within the selected RDF graph projection. A merged dataset coalesces identical statements, as RDF does.

Support finite graph-pattern queries, pipeline filtering and projection, scalar expressions, parameters, aggregation, ordering, pagination and union. Explicit finite path quantifiers expand to joins/unions preserving path multiplicities; unbounded paths and path values require dedicated operators and are rejected. Explicit match modes are supported; the profile documents its chosen repeatable-elements default without asserting unverified ISO conformance.

Compile scalar node properties through a grouped property relation with count and sample, followed by a deterministic cardinality assertion. A missing property becomes an absent result binding (null). Multiple distinct RDF property values raise QueryEvaluationException. Property grouping avoids arbitrary first-value selection or duplicate result expansion. All reads remain algebra statement patterns, so prepared-query datasets and transaction snapshots apply.

## Outcomes & Retrospective

Baseline evidence: GqlAvailabilityTest ran 1 test with 1 error before production changes: UnsupportedQueryLanguageException: No factory available for query language GQL. Full report and command are preserved in validation-gql/initial-evidence.txt. The final formatted implementation passes 207 tests: 90 parser/language tests, 16 new evaluation-helper tests, 52 existing evaluation regressions, 39 LMDB GQL cases, and 10 native DISTINCT/cancellation tests. The Java 25 rebuild and repository formatter both pass. A controlled eight-case benchmark verified 1,024 paths in every case and actual GQL adjacency engagement; its broad intervals do not demonstrate an adjacency speedup. Complete source changes and a diff-only patch accompany the handoff. The supported query profile and explicit ISO/RDF mapping gaps are documented in core/queryparser/gql/README.md.

## Context and Orientation

`core/query/src/main/java/org/eclipse/rdf4j/query/QueryLanguage.java` exposes language identifiers. `core/queryparser/api` defines QueryParser, QueryParserRegistry and ParsedTupleQuery. Service providers under META-INF/services make parser implementations discoverable. The new `core/queryparser/gql` module contains the tokenizer and compiler. `core/queryalgebra/model` supplies joins, optional joins, filters, grouping, projection and expression nodes. `core/queryalgebra/evaluation` evaluates those nodes and supplies scalar function discovery. `core/sail/lmdb` runs the same algebra through its LMDB statement source and native engine. `core/client`, `bom`, and runtime aggregation POMs make the parser available to applications and server distributions.

A statement pattern is a subject/predicate/object lookup. A tuple expression is a tree of such lookups and relational operations. A left join preserves a row when its optional pattern does not match. An adjacency index supplies a node's neighbouring IDs without scanning unrelated triples. The existing LMDB implementation chooses safe fallback readers where the current snapshot or transaction requires them.

## Plan of Work

First build the base with Java 25 and create a failing in-repository availability test using `new QueryLanguage("GQL")`. Record UnsupportedQueryLanguageException before production changes. Add the new Maven module and language registration after that failure is observed. Implement a bounded tokenizer with line/column diagnostics and recursive-descent expressions; construct algebra directly with fresh internal bindings that cannot collide with user variables or parameters.

Next implement graph patterns and the RDF projection, retaining clause ownership for OPTIONAL predicates. Track nullable variables and avoid SPARQL rebinding by using fresh pattern variables with explicit equality to existing nullable variables. Add scalar property cardinality validation. Integrate the parser into client and BOM metadata and add the LMDB test dependency.

Finally run semantic tests against MemoryStore and execution tests against LMDB with cursor, generic adjacency and native adjacency configurations. Test parameters, multiplicities, missing properties, ambiguous properties, dataset context isolation, transactions, snapshot visibility and store reopen. Verify adjacency counters around actual GQL execution. Format changed Java files using the repository formatter, review the diff, and provide the complete source changes plus a git patch.

## Concrete Steps

Run commands from the repository root using JDK 25. Maven's local repository is `.m2_repo`. Set JAVA_HOME to a JDK 25 installation and put Maven on PATH. The validation environment used an installed JDK 25 and Maven without altering the project dependency versions.

    mvn -B -ntp -Dmaven.compiler.showWarnings=false -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install
    mvn -B -ntp -o -Dmaven.repo.local=.m2_repo -pl core/queryparser/sparql -Dtest=GqlAvailabilityTest test

If the initial offline build lacks dependencies, repeat once online, then return to offline. Do not use -am or -q when running tests. Use a quick install with tests skipped to publish changed upstream artifacts before tests in downstream modules. Keep build logs in maven-build.log and focused validation logs under validation-gql.

## Validation and Acceptance

Given Alice knows Bob and Bob knows Carol, GQL `MATCH (a)-[:\`urn:knows\`]->(b)-[:\`urn:knows\`]->(c) RETURN a, c` returns Alice/Carol. An externally supplied binding for a restricts the same prepared query on each evaluation. An optional missing neighbour leaves its result unbound; a later mandatory traversal from that neighbour yields no rows. A diamond graph returns the common endpoint twice without DISTINCT. Scalar property reads preserve missing values and fail for multiple values. LMDB integration tests must prove adjacency lookups or kernel views increased during GQL evaluation, and verify write/rollback/snapshot behavior.

## Idempotence and Recovery

Use the isolated branch and preserve unrelated working directories. Tests use temporary repositories. No data migration, database format change, existing branch reset or force push is needed. Re-run focused tests after compiler fixes. Do not change upstream dependency versions merely to obtain a green local run; report any environment limitation with exact evidence.

## Artifacts and Notes

Source references: https://standards.iso.org/iso-iec/39075/ed-1/en/ISO_IEC_39075(en).bnf.txt ; https://github.com/opengql/grammar ; https://github.com/surrealdb/surrealdb/blob/main/doc/gql/LOWERING.md ; https://duckgql.com/docs/guides/csr-and-algorithms.html . These inform the design; the documented RDF projection is an RDF4J design choice.

## Interfaces and Dependencies

Expose QueryLanguage.GQL and a service-loaded GQLParserFactory implementing QueryParserFactory. GQLParser implements QueryParser and returns ParsedTupleQuery with the original source and a QueryRoot algebra tree. Parsing updates currently raises a clear MalformedQueryException. The new parser uses existing RDF4J modules and JDK classes only. A deterministic `urn:rdf4j:gql:single-value` evaluation function accepts the grouped value count and sampled literal, returning the sole value or raising QueryEvaluationException for ambiguity.

Revision: initial executable plan records the fork base, semantic mapping decisions, existing adjacency routing, and test-first/build work.

Revision: implemented the parser/compiler and added semantic, LMDB, native-planner and resource-limit coverage. Review discovered and fixed null/property/grouping issues, wildcard label semantics, and recursive-algebra exhaustion. Captured red and green reports are retained under validation-gql. Native resource checks now use IsResource, which excludes RDF 1.2 TripleTerm while enabling ID type filtering.

Revision: final formatted-source rebuild and all 207 selected tests pass with no failures, errors, or skips. The benchmark and raw measurements are committed under core/queryparser/gql/benchmarks/2026-11-09. Publication uses a new GH-0000 feature branch based on the verified HASMAC main commit; no issue number was supplied.
