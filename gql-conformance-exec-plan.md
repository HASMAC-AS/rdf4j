# Implement complete ISO GQL semantics over LMDB adjacency

This is a living ExecPlan maintained under the repository's `PLANS.md`. It supersedes the limited delivery target in `gql-exec-plan.md`; that earlier plan remains the historical record of the initial RDF query profile. This plan records unfinished work honestly and is not a conformance declaration.

## Purpose / Big Picture

The requested outcome is an implementation of ISO/IEC 39075:2024 with ISO/IEC 39075:2024/Cor 1:2026, including the complete language and its optional feature surface. A user must be able to create and maintain property graphs, prepare and execute typed GQL programs, manage session and catalog state, and query those graphs through LMDB adjacency without silently substituting RDF or SPARQL semantics. A passing parser test or support for a keyword does not establish a feature's conformance.

The current branch, `GH-0000-gql-lmdb-adjacency`, starts this phase at commit `9e51d79726312dddc9649eb9a1040a7b7e2e9e44`. It implements a read-query profile over RDF. The earlier 207 passing tests establish that profile's tested behavior, not ISO conformance. The full 2024 normative text is a required input that has not yet been obtained. Public ISO electronic inserts and the full free 2026 corrigendum have been obtained; they establish some exact corrections but do not replace the base semantics.

## Progress

- [x] Audit the existing branch and interfaces.
- [x] Verify the standard and current corrigendum.
- [x] Retrieve official feature and choice inventories.
- [x] Identify representation and transaction architecture gaps.
- [x] Reproduce directly specified semantic discrepancies.
- [x] Correct and validate those observed discrepancies.
- [in_progress] Obtain and review full normative requirements (full 2024 base text required).
- [ ] Implement typed graph storage and transactions.
- [ ] Implement typed values and session execution.
- [ ] Complete query, mutation, and catalog semantics.
- [ ] Verify every requirement and publish evidence.

## Surprises & Discoveries

ISO published the first technical corrigendum in July 2026. The electronic inserts retrieved from the ISO maintenance directory still have March 2024 modification dates. Their feature list contains 228 entries; the corrigendum supplies six additional identifiers. An inventory must apply the corrigendum rather than treating the original feature list as current and complete. Mandatory requirements without feature identifiers require a separate clause-level inventory.

The current RDF projection loses information: it has no independent representation for an empty catalog graph, an isolated node without statements, or two separately identified edges with the same endpoints and label. Edge identity currently equals an RDF triple. No parser change can recover these distinctions.

The public grammar gives double-quoted sequences two roles: string literals in value positions and delimited identifiers in name positions. The existing lexer merges their interpretation with backtick identifiers, causing a quoted string to resolve as a variable. The corrigendum also provides exact corrections for LET sibling visibility, aggregate sort keys, and grouping aliases. Those corrections can be implemented now without guessing at missing base rules.

Raw delimited parameter tests exposed a separate integration error: projecting a parameter directly from a singleton input did not leave a value expression for API binding assignment. An explicit expression slot fixes repeated bindings and clearing without materializing every ordinary variable unnecessarily.

The LMDB route matrix then exposed incorrect multiplicity for a labeled traversal with a bound starting node. Cursor and generic adjacency evaluation returned each edge four times, while native adjacency returned it once. Internal node/label column-pruning projections had accidentally retained RDF4J's default subquery boundary, preventing constant constraints from reaching all scan arms. Focused MemoryStore RED cases also returned rows for an unlabeled starting node. Marking these internal projections as part of the enclosing scope corrects the GQL compiler contract without changing the outer RETURN projection.

## Decision Log

The target is the published 2024 edition plus its 2026 correction, not the future committee draft. All optional feature identifiers remain in scope; an unimplemented optional feature must not be removed from the inventory merely to make a completeness claim pass.

The existing RDF projection remains a compatibility view. Complete graph semantics require explicit graph-element records, stable edge identity, typed values, and session state. Existing paged adjacency, native cursors, retained snapshots, and cancellation mechanisms are reusable after their contracts are made edge-identity aware.

The normative base text must settle defaults, type rules, error conditions, feature dependencies, and implementation-defined choices. Other implementations, permissive grammars, and research interpreters can supply comparison cases but are not the final oracle. Pending access, implement only requirements directly established by the official material already obtained. No conformance percentage or successful certification is claimed.

## Outcomes & Retrospective

The first correction batch is complete and verified on the formatted source. All 166 selected tests passed with zero failures, errors, or skips: 108 GQL parser/lexer/MemoryStore tests, 48 GQL LMDB repository/integration cases, six native DISTINCT tests, and four existing native cancellation tests. The LMDB tests cover cursor, generic adjacency, and native adjacency execution. The final parser artifact was rebuilt and installed before downstream LMDB tests. Repository formatting, copyright/SPDX verification, and `git diff --check` passed.

The corrections cover contextual quoting, regular binding variables, raw delimited parameter names, direct parameter projection, LET incoming scope, grouping aliases, aggregate sort scope, and the internal-projection multiplicity error. RED evidence was preserved before each production correction. `core/queryparser/gql/conformance/VALIDATION.md` records reproduction and evidence limits. No generic evaluator production change was made; the separate real-SPARQL-subquery issue remains documented and unresolved.

The complete implementation remains unfinished. The next requirement is the full 2024 normative base text; targeted searches of available user files did not locate it. The official inserts and complete 2026 corrigendum already obtained are insufficient to finish the clause-level semantic and conformance oracle. The architecture and inventory establish the remaining scope without treating planned storage, types, sessions, mutations, catalogs, or path operators as implemented. Full repository CI and a new benchmark run were not performed.

## Context and Orientation

`core/queryparser/gql` contains the lexer, expression parser, and direct RDF algebra compiler. `GQLParser.parseUpdate` currently rejects all mutations and catalog operations. `core/queryalgebra/evaluation` supplies RDF-valued execution. `core/sail/lmdb` owns the LMDB authoritative data, derived adjacency, and native execution. `core/repository/api` exposes RDF query and update operations; its `BindingSet` values cannot losslessly expose all native GQL values. `testsuites` and `compliance` contain the repository's reusable test infrastructure.

The reference material is retained outside the repository under `../gql-conformance-research`. Original standards documents and electronic inserts must not be bundled into the implementation patch. Publish only original analysis, source provenance, factual identifiers, and original tests.

## Plan of Work

First add tests demonstrating the directly specified lexical and semantic discrepancies. Run those tests against the unchanged production implementation and retain the Surefire reports. Then distinguish ambiguous double-quoted tokens until syntactic context determines their role; enforce regular identifiers for binding variables; evaluate LET right-hand sides in the incoming scope; and compile grouping and aggregate ordering from the same input relation. Preserve positive tests for delimited labels, properties, output names, and parameter names.

Next obtain the complete normative base text and extract each requirement into an original traceability record. Record its clause, mandatory or optional status, dependencies, implementation choices, expected diagnostics, positive and negative tests, and execution routes. Mark a requirement verified only after its semantics, not just its syntax, are exercised.

Implement the graph storage vertical slice before expanding syntax broadly. In the same authoritative LMDB transaction, persist catalog graph existence, isolated nodes, independent parallel edges, edge orientation, labels, and typed properties. Adjacency must retain stable edge IDs. Rollback, retained snapshots, crash recovery, reopen, backup/restore, map growth, and dictionary lifetime must cover these records. An independently committed sidecar environment is not an atomic substitute.

If GQL records use existing dictionary IDs, those references must be roots for dictionary garbage collection, rebuild, and retirement. Test values and identifiers referenced only by GQL, including deletion followed by ID reuse. Catalog-only and GQL-only commits must invalidate affected prepared plans and result caches even when no RDF triple changes; each retained reader must observe one coherent catalog, type, property, and topology revision.

Add a typed GQL value model and session/program interface. Resolve names, scopes, graph ownership, and types into a GQL semantic representation before lowering compatible operations to existing RDF4J algebra. Add dedicated physical operators where RDF values or SPARQL error semantics do not preserve GQL behavior. Extend remote result transport and user tools to preserve these types and session outcomes.

Semantic lowering must distinguish internal column pruning from genuine language scope boundaries. The corrected GQL internal projection is not a general workaround for subqueries. A separate existing generic-evaluator conflict between fixed and incoming bindings still affects a real SPARQL subquery; `core/queryparser/gql/conformance/KNOWN_EVALUATION_ISSUE.md` contains its complete reproducer. Resolve the applicable backend contract before relying on real subqueries for full GQL semantics.

Define session ownership explicitly: an owned repository connection closes with its session, while a borrowed connection remains under its caller's lifecycle contract. Commands must participate in the correct active transaction and retain the existing handling of uncertain commit outcomes. Publish neither an automatic rollback nor success when the underlying commit outcome is unresolved.

Complete query composition, runtime path operators, graph mutations, catalog and schema operations, procedures, diagnostics, and declared implementation choices from the requirement inventory. Runtime path algorithms must enumerate or select paths according to the specified semantics; removing the current 32-hop expansion bound is not an implementation of unbounded paths.

## Concrete Steps

Use Java 25 and the workspace Maven repository. From the repository root, install prerequisites with tests skipped before targeted tests:

    mvn -B -ntp -o -Dmaven.repo.local=.m2_repo -pl core/sail/lmdb -am -Pquick install
    mvn -B -ntp -o -Dmaven.repo.local=.m2_repo -pl core/queryparser/gql test

Never use `-am` or `-q` for enabled tests. Use the existing repository formatter before committing. Preserve all previous untracked evidence. Record new logs under `validation-gql-conformance/` and keep environment-specific Maven settings out of source control and delivery archives.

## Validation and Acceptance

The first correction batch must demonstrate literal values and identifier contexts separately, illegal same-LET sibling references, legal references from a subsequent LET, aggregate ORDER BY on the group's input rows, rejection of aggregate ordering in an ungrouped nonaggregating result, and grouping by output aliases including computed expressions.

The storage milestone must create an empty graph, two isolated nodes, and two separately identified parallel edges with different properties; query both edges through adjacency; mutate one while preserving both identities; delete only one; and retain the empty graph and isolated nodes after reopen. Repeat with rollback and concurrent retained readers. Ordinary SPARQL scans and exports must not expose internal GQL metadata.

Specify RDF interoperability as part of the storage contract. Native GQL graph contents are not implicitly exposed as RDF statements; any RDF projection must be explicit. Test context enumeration, exports, and SPARQL CLEAR against that contract, including CLEAR ALL, while ensuring that no accidental hidden-metadata convention decides which GQL records survive.

Before performance comparisons, require exact result bags and identities to match the reference cursor implementation, generic adjacency execution, and native adjacency execution. Measure graph/value setup, materialization, runtime planning, traversal, and result serialization separately and together. Vary fanout, parallel-edge multiplicity, labels, property widths, path lengths, graph size, and concurrent mutation. Include type-validation and catalog/metadata write costs, and verify actual adjacency engagement. The prior profile's short benchmark did not establish an adjacency speedup and is not evidence for this new data model.

Route comparisons must also be checked against independently specified expected results from reviewed normative requirements. Agreement between cursor and native execution does not rule out a shared compiler error. Include adversarial scope, conflicting-binding, and duplicate-sensitive cases with their expected result bags written explicitly.

Final acceptance requires every normative requirement and optional feature in scope to have a reviewed implementation and reproducible evidence. Verify compile-time rejection, runtime diagnostics, null and type behavior, multiplicity, transaction outcomes, persistence, remote results, and adjacency execution. Run relevant existing RDF4J regressions and complete CI. Test counts, grammar coverage, or another engine's answers alone do not meet this acceptance condition.

## Idempotence and Recovery

Continue on the existing feature branch. Do not change the base branch or force-push. Preserve prior validation artifacts. Use temporary repositories for destructive test cases. Persistent format changes must have an explicit compatibility and recovery strategy before they become the default.

## Artifacts and Notes

Authoritative source locations are `https://www.iso.org/standard/76120.html`, `https://www.iso.org/standard/93701.html`, and `https://standards.iso.org/iso-iec/39075/ed-1/en/`. The free corrigendum was obtained through the Estonian Centre for Standardisation and Accreditation. Provenance records contain retrieval dates and SHA-256 hashes. Targeted searches of available user files did not locate the full 2024 standard.

## Interfaces and Dependencies

The proposed additions are a `GqlGraphStore` storage contract, a typed `GqlValue` hierarchy, a `GqlSession` program execution interface, a typed semantic algebra, and a reusable clause-indexed conformance suite. Their exact public signatures must follow reviewed normative requirements. Additive internal prototypes may proceed before publishing public APIs. Reuse JDK and existing repository dependencies where sufficient; new dependency choices require their own necessity and licensing review.

Revision 2026-11-09: expanded the task from an RDF read-query profile to complete standard semantics; recorded the corrigendum, missing normative input, representation gaps, and a test-first correction batch.

Revision 2026-11-09 after validation: completed the first correction batch with 166 selected passing tests; added direct-parameter and internal-projection regressions, preserved the separate SPARQL subquery finding, and moved the active step to obtaining and reviewing the complete normative base.
