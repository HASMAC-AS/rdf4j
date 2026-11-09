# froGQL reference audit for RDF4J GQL

## Scope and provenance

Read-only inspection of the user-supplied `frogql-main.zip`, SHA-256 `9b6b95f1361ae86128fa7976c8f8bc32817c6db7d76469db5d82d791dc2d2ae5`. No downloaded program was executed. Paths below are relative to `gql-reference-sources/frogql-main/`. No `AGENTS.md` or `PLANS.md` was present in the extracted project.

`LICENSE` is MIT, copyright 2026 Matías Toro and contributors. `Cargo.toml` identifies the package as version 0.6.0 and points to `https://github.com/pleiad/frogql`. Substantial copied source or tests must retain its copyright and permission notice. Independent RDF4J tests can record this archive and the originating test path as comparative provenance.

The README explicitly calls the project a research prototype that grew into a working database. Its typechecker derives from the authors' gradual-type-system work; large portions of the database implementation were AI-assisted. That history does not disqualify the code, but implementation claims must be checked against the actual guards and tests. The archive is useful executable-design evidence and a rich regression-case source. It is not evidence of complete ISO conformance.

## Most reusable mechanisms

### Independent graph-element identity

`src/model/graph.rs::MemoryGraphStore` stores independent node IDs and edge IDs. The topology columns `edge_src`, `edge_tgt`, and `edge_directed` are indexed by edge ID; labels and properties are separate node/edge columns. `outgoing`, `incoming`, and `undirected_adj` contain edge IDs, so parallel edges with identical endpoints remain distinct. Isolated nodes exist independently of edges or properties.

`src/model/graph_access.rs::GraphAccess` separates topology, labels, properties, orientation, and traversal access. This is a useful conceptual boundary for an LMDB graph-access interface. Its `Vec<Id>` return types and 32-bit dense ID choices should not be copied into the RDF4J hot path: reuse retained LMDB cursor lifetimes, compressed adjacency iteration, and the existing 64-bit dictionary/element identities instead.

`tests/it/iso_multiplicity_test.rs` is particularly valuable. Its small graph has two R edges a→b, one b→c, a c→c self-loop, one S edge a→b, and an isolated node. Hand-specified expectations include:

- An unbound R edge from a to b contributes two rows even when no edge variable is projected.
- Projecting or merely naming the edge does not change multiplicity.
- An unlabeled edge pattern from a to b contributes three rows across R/R/S.
- The two-hop R path from a through b to c contributes two rows.
- RETURN DISTINCT collapses endpoint duplicates explicitly.

These cases expose exactly what the current RDF-triple edge identity cannot represent. Add them when an explicit property-graph storage mode exists; do not silently reinterpret duplicate RDF statements as independent edges.

### Runtime path selection and shortest paths

`src/syntax/path_prefix.rs` normalizes ANY SHORTEST to one shortest path, ALL SHORTEST to one shortest length group, and counted path/group forms separately. `src/runtime/path_select.rs` partitions selection by both boundary nodes and preserves path multiplicity. It provides a straightforward materialized reference implementation, not a suitable performance target for arbitrarily large path sets.

`src/runtime/engine.rs::try_shortest_bfs` is the useful optimized design:

- It applies only to WALK with SHORTEST 1 PATHS/GROUPS over one repeated edge, lower bound at most one, and an anonymous edge.
- It requires at least one constrained endpoint and chooses the smaller available endpoint set to drive traversal; reverse traversal reconstructs source-to-target output orientation.
- It records distance once per reached node and predecessor `(node ID, edge ID)` pairs. All shortest groups retain every equal-distance predecessor; one shortest path needs only one.
- Zero-length matches and positive-length coincident endpoints are handled separately. Directed positive-length cycles fall back to the general algorithm; they are not approximated by ordinary visited-node BFS.
- Other forms fall back to length-ordered walk enumeration or mode-pruned finite enumeration.

`tests/it/shortest_bfs_test.rs` and `tests/it/path_prefix_test.rs` contain useful diamond, cycle, zero-length, bounded-length, endpoint-pinning, and generic/optimized differential cases. Their performance claims in documentation were not reproduced here. Preserve cancellation, result closure, bounded memory, and LMDB snapshot ownership when adapting the algorithm; do not import a materialize-every-path execution policy wholesale.

The restrictive modes in `src/runtime/path_select.rs` use distinct edge IDs for TRAIL and node IDs for ACYCLIC; SIMPLE permits a final return to the opening node. Their interpretation of SIMPLE and directed self-loops still needs corroboration against other supplied sources or the official corrections.

### Scope and nullable OPTIONAL boundaries

`tests/it/cross_clause_scope_test.rs` supplies an excellent independent three-node fixture: a and c have key `1`; b has key `2`. Both a later MATCH referencing the earlier variable and sibling comma operands with an equality descriptor must produce exactly `(a,a), (a,c), (b,b), (c,a), (c,c)`. An unevaluated correlation gives zero rows; an ignored correlation gives nine.

The same file checks three-clause chains, outer references inside OPTIONAL predicates, null extension, unknown-variable errors, and keeping a filter inside its UNION arm. `src/elaborate/hoist.rs` lifts predicates only to the smallest enclosing relation that binds their references and treats UNION, repetition/questioned patterns, and selective prefixes as scope barriers. An OPTIONAL predicate belongs to the optional branch and must not become a filter over the completed outer join.

`tests/it/optional_match_test.rs` checks unmatched input preservation, repeated optionals, compatible shared variables, and WHERE-before-outer-join behavior. These tests can be independently rewritten against MemoryStore and all LMDB routes immediately, using the existing RDF projection where representation is lossless.

### Typed values and semantic analysis

`src/model/value.rs`, `src/syntax/expr.rs`, `src/typing/simple_type.rs`, `src/typing/checker.rs`, and `src/runtime/assignment.rs` keep graph elements, paths, lists, records, null, syntax, and type information explicit. `tests/it/first_class_node_edge_values_test.rs`, `tests/it/group_variable_test.rs`, `tests/it/named_path_test.rs`, and `tests/it/three_valued_logic_test.rs` provide candidate tests for the richer API planned for RDF4J.

The prototype's type system and emptiness analysis are useful research references, but are not the complete ISO value/type system. A native GQL value API and codec are still needed; converting every list, record, edge, and path to an RDF string literal would lose the semantics those tests exercise.

## Known gaps and contradictions: do not import as normative behavior

1. **Rollback is deliberately too coarse for statement isolation.** `docs/data-modification.md` and `tests/it/dm_rollback_test.rs` state that a failing statement rolls back every unsaved change to the last `.save`, including earlier successful statements. The docs' early atomicity description must be read together with the explicit carve-out. RDF4J must retain its own transactional guarantees and must not port this session-reset behavior.
2. **Durability and concurrency are not an LMDB design.** Mutation state is an in-memory overlay; `.save` rewrites a temporary file and renames it. `docs/internals/iso-gql-gaps.md` explicitly deprioritizes real transactions and MVCC for the research prototype. Its layout does not demonstrate crash-safe LMDB catalog/property/topology publication.
3. **Self-loop multiplicity is a contested source claim.** `tests/it/anydir_iso_test.rs::full_one_hop_both_orientations` expects a directed c→c self-loop twice for an any-direction pattern, counting traversal senses separately. The file calls this a team decision and contrasts it with older fallback behavior. Keep it as a comparative discrepancy until independently resolved; do not replace a different RDF4J answer merely because the filename says ISO.
4. **Aggregate ordering is incomplete.** `tests/it/return_item_shape_test.rs::order_by_a_direct_aggregate_absent_from_return_is_an_error` rejects aggregate ORDER BY expressions absent from RETURN. This must not undo RDF4J's previously reviewed 2026-corrigendum correction that supports aggregate sort expressions in grouped results.
5. **Grouping alias precedence is not a final oracle.** `docs/internals/iso-gql-gaps.md` describes choosing a pre-existing binding variable over an identically named RETURN alias. The earlier RDF4J correction derived grouping behavior from the official corrigendum. Retain normative precedence and use the disagreement as a regression/review case.
6. **Extensions are mixed into useful tests.** The docs explicitly call NODES/EDGES path functions translation divergences, accept `GROUP BY n.city` as a deviation, add NEAREST/vector-search syntax, and describe custom graph-type and secondary-index commands. The query surface should classify these separately rather than label them ISO features.
7. **Known missing families remain.** The gap report lists NEXT/LET pipelines, OFFSET, multi-DML chains, real graph management, richer temporal support, aggregate functions, and transaction/session semantics as unfinished. Some source comments are stale relative to newer fixes, so source/test inspection is required for each capability claim.
8. **Some convenient collection semantics need review.** The gap report says COLLECT_LIST drops records whose fields are all null, and its list-comprehension description maps a null input to an empty list. These are implementation choices to compare against authoritative rules, not assumptions for the RDF4J implementation.
9. **Mixed correlated subquery handling is admitted incomplete.** The gap report documents a path that combines a shared pattern variable with an external parameter but caches only on the shared variable. Use tests to guard the full parameter dependency key rather than adopting that optimization.

## Suggested immediate work order

1. Independently rewrite the scope, OPTIONAL, path-prefix placement, and scalar/aggregate cases supported by the existing RDF projection; run them on MemoryStore, LMDB cursor, generic adjacency, and native adjacency. Preserve original source references and mark expectations as corroborated, source-specific, or disputed.
2. Keep immutable graph-element/transaction design separate from scalar query additions. Implement isolated nodes and independent parallel edges in the same authoritative LMDB transaction before claiming native property-graph semantics.
3. Introduce runtime path execution with a correctness reference and a tightly guarded BFS specialization. Keep edge IDs throughout predecessor structures, preserve exact result bags, and test every fast-path guard's fallback.
4. Use the MIT tests as comparative material with attribution, not as an automatic ISO certificate. In particular, pin the 2026 official corrections above the prototype's older or restricted behavior.
