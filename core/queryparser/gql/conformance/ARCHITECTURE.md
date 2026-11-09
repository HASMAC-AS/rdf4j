# Architecture required for complete GQL semantics in RDF4J

## Status and target

This is an original engineering analysis of the GQL implementation at commit `9e51d79726312dddc9649eb9a1040a7b7e2e9e44` in HASMAC-AS/rdf4j. It is a design report, not an implementation or a declaration of ISO conformance. The investigation made no production or test changes.

The requested target is the mandatory requirements and the complete optional-feature scope of ISO/IEC 39075:2024, with its 2026 corrigendum incorporated. That target is broader than the standard's minimum-conformance concept. Optional features must not be described as universally mandatory. The companion normative audit identified the official corrigendum and a 234-identifier optional-feature inventory, but the complete normative base text was not available to this investigation. Exact semantic rules, permitted implementation limits, feature dependencies, and implementation-defined choices remain subject to clause-by-clause verification against that text.

## Architectural conclusion

The existing RDF projection is not an information-preserving representation of the full GQL graph model. More grammar and functions cannot recover information that has no stored representation. Complete semantics requires an explicit graph-element model, typed values, catalog/session execution, and atomic persistence. The smallest sound extension is a GQL layer integrated into the existing LMDB transaction and adjacency infrastructure, with an explicit RDF interoperability view.

This does not imply that ordinary RDF data must be copied into a second authoritative graph database. The existing RDF projection remains useful as a view over RDF. Native GQL property graphs need additional authoritative records for information that RDF statements do not currently retain.

## Representation counterexamples

| State that the full feature target must represent | Present projection | Failure |
| --- | --- | --- |
| An isolated node with no label or property | Nodes are discovered from RDF statements | The node is indistinguishable from absence |
| An empty graph that exists in a catalog | Contexts are discovered from statements | Graph existence is indistinguishable from absence |
| Two independently identified edges with the same source, target, and label | Each edge is the same RDF triple | Distinct edges collapse |
| Properties on an edge | Edge values are triple terms; property syntax is rejected | No persistent edge-property semantics |
| Stable edge identity while a label or property changes | Identity is the subject/predicate/object tuple | Relabeling changes identity |
| Stored undirected edges | Stored edges are directed RDF statements | Any-direction pattern matching cannot recover stored orientation |
| Rich GQL values and graph/path results | BindingSet contains RDF Value | No faithful public value/result representation |

These examples establish information loss; they do not classify every listed feature as mandatory. The final conformance matrix must do that separately.

Relevant inspected sources:

- `core/queryparser/gql/src/main/java/org/eclipse/rdf4j/query/parser/gql/GqlCompiler.java`: `nodeScan`, `edgeSyntax`, `edgeRelation`, `triple`, and `sameEdge`.
- `core/queryparser/gql/src/main/java/org/eclipse/rdf4j/query/parser/gql/GQLParser.java`: `parseUpdate` rejects updates and catalog operations.
- `core/model-api/src/main/java/org/eclipse/rdf4j/model/Value.java`: RDF type discrimination.
- `core/query/src/main/java/org/eclipse/rdf4j/query/BindingSet.java`: RDF-valued result bindings.

## Suggested module and API boundaries

The following names are proposed boundaries, not classes that already exist. The final packaging can follow RDF4J naming conventions without changing their responsibilities.

### GQL values and semantic model

Add a small public GQL model/API module, separate from the RDF model API. It should define typed scalar/composite values, graph-element identities, paths, graph references where the feature permits them, row descriptors, diagnostic outcomes, and type descriptors.

Suggested concepts are `GqlValue`, `GqlNode`, `GqlEdge`, `GqlPath`, `GqlRow`, and `GqlResult`. Identity must not depend on labels or properties. Element-handle lifetime, graph membership, cross-graph identity, deletion, and reuse semantics must be resolved from the normative requirements before committing the public identity contract. A store identifier plus a never-reused element identifier is a possible implementation technique, not a normative conclusion.

Do not implement arbitrary GQL values as custom RDF Value objects and assume compatibility. `Value.getType`, expression functions, optimizers, and query-result writers assume RDF types. Nor should lists, records, paths, or graph values be silently stringified into literals. Lossless codecs and equality, comparison, grouping, hashing, casting, and null rules belong in the GQL value model.

### Session and statement execution

Add a GQL repository/session API, potentially in `core/repository/gql`, with concepts such as `GqlSession`, `GqlPreparedStatement`, and `GqlProgram`. A session owns or explicitly borrows a repository connection, maintains session state, and exposes query rows together with statement completion and structured diagnostics.

Session state includes the working graph, schema/catalog resolution, parameters, and transaction characteristics as specified by the standard. It must not live in static parser state or a global map keyed by query text. Preparing a statement must not execute side effects.

Keep `prepareTupleQuery(QueryLanguage.GQL, ...)` as a compatibility route for results that can be represented faithfully as RDF bindings. Updates, catalog commands, session commands, and procedures must not be disguised as tuple queries with side effects in scalar functions. Exact behavior when rich results are requested through a legacy tuple API must be explicit and tested.

Existing boundaries to extend or adapt:

- `core/queryparser/api/.../QueryParser.java` returns `ParsedQuery` or `ParsedUpdate`.
- `core/repository/api/.../RepositoryConnection.java` exposes tuple queries, updates, and repository transactions.
- `core/repository/sail/.../SailTupleQuery.java` and `SailUpdate.java` implement the corresponding execution routes.
- `core/queryresultio/sparqljson/.../SPARQLResultsJSONWriter.java` serializes RDF result values.

### Parser and compiler phases

Retain a separate GQL syntax tree and semantic analysis phase before lowering to RDF query algebra. The pipeline should be parser to AST, then scope/type/catalog resolution, then GQL semantic algebra, then physical planning. Resolution must use a coherent catalog/session snapshot.

Lower fragments into existing RDF4J algebra only where semantics match. Dedicated operators are needed where GQL values, null/error behavior, graph mutation, path enumeration, or command outcomes are not represented faithfully. Keeping SPARQL numeric, ordering, and error rules merely because existing operators implement them does not establish GQL correctness.

Unbounded and shortest-path support needs runtime path operators and algorithms selected for the actual matching and selection semantics. Increasing the current 32-hop expansion limit is not an implementation of these features. Resource limits that the standard permits must be documented; the normative audit must determine which limits and exception behavior are allowed.

### Graph-store SPI

Add a transactional `GqlGraphStore` capability, potentially in `core/sail/gql`, for:

- Catalog, schema, graph, and graph-type objects.
- Explicit node existence and independent edge identity.
- Element labels, properties, direction, and graph membership.
- Typed property access and graph-type validation.
- Snapshot reads and atomic mutations.
- Outgoing/incoming/undirected adjacency with edge identity retained.

A Sail that does not provide this capability may still support the existing RDF interoperability view. It must report its supported capability set truthfully. Unsupported native graph management must not be simulated with process-local maps that disappear on reopen.

## RDF interoperability without metadata leakage

Treat the existing RDF projection as an explicit view: IRIs/blank nodes, rdf:type labels, resource-object edges, and literal properties. This is useful for querying RDF data but intentionally has the limits of its source representation. A view can be read-only initially where update translation is ambiguous. If writable interoperability is supported, define each translation and its effects on both APIs rather than allowing accidental behavior.

Native GQL catalog and element records must not be ordinary hidden RDF triples in a normal named graph. Such triples can leak through SPARQL, wildcard contexts, counts, inference, export, repository wrappers, and optimizers. They may also be removed accidentally by `CLEAR ALL`. Filtering a reserved graph name only in the GQL compiler would not prevent those failures.

Tests must establish what ordinary SPARQL sees before and after creating, mutating, and dropping native GQL graphs. Exported RDF views should be explicit projections, not accidental exposure of a physical encoding.

## LMDB persistence and recovery

### Authoritative transaction boundary

Use dedicated named LMDB databases in the existing TripleStore environment, enlisted in its existing writer transaction. Logical record groups include catalog/type descriptors, explicit element records, labels, typed properties, and adjacency data/deltas. Choose physical DBI count and key layouts from access patterns; one database per logical group is not a requirement.

Do not introduce an independently committed sidecar file, map, or LMDB environment for authoritative GQL metadata. Creating a catalog entry in one store and applying graph mutations through another commit creates split outcomes on failure.

The inspected code already has a useful model for metadata participation. `LmdbSailStore.commitPendingPublication` stages namespace state in the TripleStore transaction, commits dictionary additions, commits the authoritative TripleStore transaction, and then publishes derived state. Namespace-only commits also use that environment.

Relevant source files:

- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/TripleStore.java`.
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbSailStore.java`, especially `commitPendingPublication`, `stageNamespaceSnapshot`, `commitTripleTransaction`, and `commitDictionaryBeforeTriple`.
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbStoreConnection.java`.
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/WriterTransaction.java`.
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/TxnManager.java`.
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/ValueStore.java`.

Writer ownership, map growth, replay, asynchronous mutation queues, failed-commit reconciliation, and cancellation are existing correctness mechanisms. GQL writes must participate in them rather than making raw native writes around the connection's transaction owner.

### Dictionary liveness

TripleStore and ValueStore own separate LMDB environments. Existing dictionary additions are committed before authoritative triples. If GQL records refer to RDF dictionary IDs, those records must participate in dictionary liveness, garbage collection, rebuild, retirement horizons, and ID reuse. Otherwise an ID referenced only by GQL data can be reclaimed incorrectly.

A dedicated typed-value encoding in the authoritative environment is a possible initial correctness boundary. Reusing the existing dictionary is also possible, but only with complete ownership and reclamation rules. Do not claim that reuse is free because values already have long IDs.

### Snapshot consistency and publication

Catalog/type definitions, element properties, and topology must be read at a coherent snapshot. A query must not combine an old adjacency publication with newer properties or graph-type definitions. GQL-only writes must invalidate appropriate query plans/caches and publish or invalidate adjacency views even when no ordinary RDF statement changed.

Catalog-only revisions may need their own revision counter, analogous to existing namespace behavior, while a shared transaction identifier ties topology and metadata to one commit. Design this explicitly; blindly advancing or reusing RDF data revisions can interfere with cache and retirement assumptions.

Store-format changes need backward-compatible discovery/versioning, backup/restore inclusion, and tested recovery. Any migration must preserve existing RDF data and remain independently verifiable. No migration should be invented before the target graph representation and normative semantics are settled.

## Adjacency integration and performance gates

The current `LmdbAdjacencyProvider` scans subject/predicate/object/context patterns. `NativeLmdbQuerySource.NativeAdjacency` exposes neighbor and context runs for a fixed predicate/direction, not independent edge IDs. Full multigraph semantics needs an edge-ID-aware view.

An appropriate cursor exposes a key node, neighbor node, edge ID, graph identity, and orientation. Labels may be handled through the row key or a label index. Parallel edges must remain separate physical/logical entries; RDF set distinctness must not collapse them.

Reuse the existing primitive/paged/compressed vector machinery, batched reads, snapshot leasing, cancellation, and native code generation where applicable. Preserve separate RDF and GQL identity contracts. In particular, a native DISTINCT key suitable for an RDF statement is not automatically suitable for GQL edges.

Relevant source files:

- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbAdjacencyProvider.java`.
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbAdjacencyReadView.java`.
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbAdjacencyPublishedState.java`.
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/AdjacencyDelta.java`.
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/NativeLmdbQuerySource.java`.

`LmdbAdjacencyReadView` already retains a publication and snapshot revision, with iterator leases. Generalized GQL views should preserve its rule that a read never combines parts of different publications.

Performance acceptance should compare a trusted scalar/cursor reference path, generic adjacency, and native adjacency. Verify exact result bags and identities before timing. Record setup, catalog/type resolution, optimization, materialization, and steady-state execution separately and together where users pay those costs. Disable result caching in engine comparisons and report adjacency/kernel counters.

Vary node count, fanout, label selectivity, edge-property selectivity, parallel-edge multiplicity, graph count, orientation mix, loop density, path length/selection, output size, and concurrent write/snapshot behavior. Include insert/property-update/delete throughput and catalog/type validation costs. Benchmark ordinary SPARQL as a regression surface. A higher lookup counter is evidence of engagement, not evidence of a speedup.

## First complete vertical slice

Use normative-valid commands for the following behavior before broad grammar expansion:

1. Create a graph and retain its existence while empty.
2. Create two isolated nodes.
3. Create two distinct parallel edges with identical endpoints and labels but distinct properties.
4. Query both edges through adjacency, observing two identities and two results.
5. Change a property or label without changing edge identity.
6. Delete one edge while retaining the other.
7. Remove all incident edges, labels, and properties while retaining explicit node existence.
8. Commit, close, reopen, and verify all identities and graph/node existence.
9. Repeat with rollback and a reader retaining an earlier snapshot.
10. Verify ordinary SPARQL contents, counts, contexts, and exports remain correct.

Each behavior-changing production step must follow the repository's test-first rule. Passing this slice validates core representation and transaction choices; it does not establish full conformance.

## Failure and regression verification gates

- Crash before dictionary publication, between dictionary and authoritative commit, and after authoritative commit but before derived-state publication.
- Cancellation during native commit and reconciliation of an uncertain result.
- Complete rollback of a failed multi-operation statement or transaction according to the normative rules.
- Concurrent readers observing matching catalog, type, property, and topology versions.
- Adjacency delta publication, fallback, cache invalidation, and reopen after GQL-only writes.
- Preservation of parallel edges through distinct, aggregation, degree, path, and generated-kernel operators.
- Dictionary GC/rebuild with IDs referenced only by GQL data.
- Stable handles after delete/reinsert and potential allocator reuse.
- Undirected self-loop traversal and direction matching.
- Type/schema constraint checks and read-only transaction enforcement at the required stage.
- Backup/restore and recovery including all GQL records.
- No internal metadata in ordinary SPARQL results or RDF exports.
- Lossless rich-value serialization and diagnostics through local and HTTP APIs.
- GQL-specific equality, null, ordering, casting, aggregation, and error semantics across optimized and reference routes.

## Research references and their limits

These are primary sources used for architectural context. None replaces the normative ISO base text.

- ISO standard landing page: https://www.iso.org/standard/76120.html
- LDBC authors' mathematical model report: https://ldbcouncil.org/docs/papers/w16045-prepub-LDBC-Technical-Report-TR-2021-01--Property-graphs-and-paths-in-GQL-Mathematical-definitions.pdf . This is an earlier technical report describing the developing GQL graph/path model, not final normative requirements.
- Google Spanner's graph type API: https://docs.cloud.google.com/spanner/docs/reference/standard-sql/graph-data-types . This is an implementation example of graph-element and path types, not a conformance oracle.
- Neo4j's conformance declaration: https://neo4j.com/docs/cypher-manual/current/appendix/gql-conformance/ . This illustrates explicit mandatory/optional feature and datatype declarations, not the complete standard.

The conclusions about the present RDF4J architecture derive primarily from inspected repository contracts and the representation counterexamples above. Exact ISO requirements and exceptions must be added to the conformance matrix with clause-level provenance before an implementation-completeness claim can be made.
