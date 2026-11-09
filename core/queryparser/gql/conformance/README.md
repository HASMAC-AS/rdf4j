# ISO GQL conformance work

**Status: not conformant. Full implementation and verification are incomplete.**

The target is ISO/IEC 39075:2024 together with ISO/IEC 39075:2024/Cor 1:2026. The requested scope includes the mandatory language and all optional feature families. The current RDF query profile must not be relabeled as complete ISO support. `target.json` records the outstanding prerequisites explicitly; it is a status record, not a certification mechanism.

## Normative input

The six original electronic inserts from ISO and the complete published 2026 corrigendum have been obtained and hashed. The full 2024 base publication has not been obtained. Its semantic rules, conformance clauses, feature implications, and implementation choices are required to complete the specification and test oracle. Public previews and the grammar are insufficient. The research input documents are kept outside the repository; this directory contains original tracking records and provenance, not copies of those documents.

ISO's [base publication](https://www.iso.org/standard/76120.html) identifies the [July 2026 corrigendum](https://www.iso.org/standard/93701.html) as separate from the base text. The original [electronic inserts](https://standards.iso.org/iso-iec/39075/ed-1/en/) have March 2024 modification dates. The full corrigendum is available through the [EVS product page](https://www.evs.ee/en/iso-iec-39075-2024-cor-1-2026). Do not substitute a later committee draft or an implementation's interpretation for the pinned normative target.

## Inventory and evidence rules

`optional-feature-inventory.json` starts with 228 identifiers in the original feature insert and adds six identifiers explicitly supplied by the corrigendum: GC00, GQ27, GS09, GS17, GS18, and GV51. All 234 entries are deliberately unverified. This is not a count of mandatory requirements or a conformance percentage. Mandatory rules without feature identifiers still need their own clause-level inventory.

`implementation-choice-inventory.json` records 117 original implementation-defined identifiers and 20 original implementation-dependent identifiers, plus the corrigendum's UA003. All 138 choices require review and evidence. An implementation-defined choice needs a documented implementation decision; an implementation-dependent behavior must still be checked against the permitted range of outcomes.

`status-code-inventory.json` records the original insert's 12 classes and 68 explicit subclasses. Diagnostic changes in the corrigendum must be applied before this can become a completed diagnostics contract. The original grammar's 814 productions are useful for syntax tracking, not a semantic oracle.

Each verified requirement must identify its normative clause and correction, feature dependencies, implementation choices, source code, positive and negative cases, runtime diagnostics, and the commit tested. Storage, transaction, protocol, and cancellation cases are additionally required where the behavior crosses those boundaries. Existing profile tests may be reused only after their expected behavior is checked against the normative rules.

## Architectural prerequisites

The current mapping cannot represent an isolated node without statements, an existing empty catalog graph, independent parallel edges with identical endpoints and labels, or stable edge identity across label changes. It also has no edge property storage. These are representation gaps, not missing parser productions. Full scope requires explicit graph-element records and typed values alongside an explicit RDF compatibility view.

GQL graph records, catalog/type descriptors, and mutation effects must share an authoritative transaction. The LMDB integration should use the existing TripleStore environment and writer machinery rather than independently committing a sidecar environment. Dictionary lifetime, adjacency publication, retained snapshots, map growth, recovery, incremental backup, and restore must account for the new records. Adjacency rows must preserve independent edge IDs.

The RDF `BindingSet` result API cannot losslessly represent the entire GQL value surface. A GQL value/session/program API and result codec must preserve node, edge, path, graph, list, and record semantics and diagnostic outcomes. Type and scope resolution must precede lowering; use existing RDF algebra only when its observable behavior matches the required GQL operation.

The full milestone sequence and acceptance gates are in the repository-root `gql-conformance-exec-plan.md`. Its first storage acceptance sequence creates an empty graph, isolated nodes, and two separately identified parallel edges, then exercises adjacency reads, individual mutations, rollback, concurrent snapshots, reopen, and RDF interoperability.

The [correction validation report](VALIDATION.md) records the implementation changes, test-first evidence, reproduction commands, and limits of the current verification. The [architecture analysis](ARCHITECTURE.md) traces the storage and API implications in more detail.

The audit also reproduced an [existing generic-evaluator subquery issue](KNOWN_EVALUATION_ISSUE.md). The first compiler correction fixed its own accidental scope boundary. The supplied-reference batch repairs the statement-evaluator contract and verifies a permanent real-subquery regression across all three LMDB routes. [Its validation report](REFERENCE_VALIDATION.md) records 397 selected passing tests and the remaining evidence limits.

## Known profile differences

The first correction batch intentionally changes three earlier profile behaviors: a double-quoted value expression is a string rather than a variable reference; quoted binding-variable declarations are rejected; and GROUP BY uses regular binding references rather than arbitrary expressions. To group a computed expression, give it a RETURN alias and group by that alias, for example `RETURN n.age + 1 AS nextAge, COUNT(*) AS total GROUP BY nextAge`. Delimited label, property, parameter, and result-column names remain supported.

The existing profile still delegates numeric, comparison, null/error, and ordering behavior to RDF4J in several places; only reviewed rules can establish whether those behaviors match GQL. It lacks the full type system, graph mutations, catalogs, schemas, sessions, procedure/program composition, unbounded and shortest paths, and rich result values. Its namespace mapping and contextual-keyword policy are implementation choices that have not been established as conforming extensions. No claim is made that correcting the first lexical and grouping defects resolves those areas.

No public official ISO GQL conformance suite was located during the source audit. This is a search result, not a claim that no private suite exists. Grammar samples, openCypher tests, other engines, and research semantics are useful supplementary tests. They do not replace clause-level review of the published standard.

## Supplied implementation references

The user supplied froGQL, gql-compat, gql-grammar, MGQL, and DuckGQL archives to continue implementation without the full base publication. Engineering now proceeds from reviewed fixtures, formal models, algorithms, and official grammar/corrigendum material. [The reference audit](references/README.md) records exact archive hashes, licenses, scope, disagreements, and adoption decisions. Missing normative text remains a limitation on final conformance verification, not a request for the user to obtain it before work can continue.

## Research implementations

The [MGQL research implementation](https://github.com/EngineeringSoftware/mgql) and [GPML conformance comparison](https://github.com/samyama-ai/gpml-conformance) provide candidate executable reference cases. They cover selected fragments and use their authors' semantic interpretations. Review each imported case's licensing, language version, supported fragment, and normative reference; do not treat either corpus as proof of full conformance. No third-party test corpus or new dependency has been added by this audit.
