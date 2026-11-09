# Supplied GQL implementation references

These five user-supplied archives are engineering references for continuing the complete GQL implementation. Their absence of a full normative base does not block engineering work. They do not establish complete ISO conformance: each implements or tests a fragment, and several expectations disagree with the published grammar or already reviewed 2026 corrections.

`sources.json` pins each original archive's SHA-256, embedded revision comment, upstream URL, reviewed license, and relevant limitations. Embedded revision comments have not been independently verified against upstream. No downloaded project code was executed, no external implementation was added as a dependency, and no external source or ISO publication is bundled here. The audit reports and RDF4J tests are original work.

| Reference | Useful contribution | Limitation affecting adoption |
| --- | --- | --- |
| gql-compat | 581 cases, 28 fixtures, production/clause/feature references | Not a complete semantic oracle; some assertions are too weak and some syntax conflicts with its included grammar |
| gql-grammar | Explicit lexer and parser rules for comparison | Targets the March 2023 draft, with rule defects and lexical differences from the published grammar |
| froGQL | Independent graph-element identity, scope regressions, guarded shortest-path algorithms | Research prototype with documented omissions and implementation-specific transaction/self-loop choices |
| DuckGQL | AST/binding architecture, graph traversal data structures, compatibility tests | Its own family matrix has partial/planned support; derived openCypher scenarios are not ISO requirements |
| MGQL | Precisely specified read-query fragment, multiplicity and compatible-binding examples | Formal results apply to its limited calculus; they do not prove a complete GQL implementation |

## Review decisions

The supplied corpus's 581 cases are not reported as 581 passing RDF4J conformance tests. No blanket adapter or skip-excluding percentage has been used. Original regression tests strengthen later CASE operands, duplicate-sensitive result bags, exact numeric datatypes, graph identities, scope isolation, and rejection conditions.

The existing corrections for LET sibling visibility, grouping aliases, and aggregate sort scope remain in force. Conflicting examples are recorded in the individual audits. The draft grammar does not replace the hand-written compiler wholesale. Existing RDF4J transaction behavior is not changed to imitate a prototype's rollback strategy.

The first implementation batch from these references covers numeric spellings, scalar CASE extensions, read-query NEXT/YIELD composition, LET VALUE and FILTER WHERE spelling, and the shared statement evaluator's handling of optimizer-specialized variables. These are tested behaviors within the RDF projection. They do not supply native property-graph storage, independent parallel edges, full typed values, graph mutations, catalog/schema commands, sessions, procedures, or runtime unbounded/shortest paths.

## Reports and traceability

- [Grammar and compatibility audit](compat-grammar-audit.md) records corpus counts, weak assertions, grammar conflicts, and numeric/CASE reference leads.
- [froGQL audit](frogql-audit.md) records graph representation, scope and multiplicity fixtures, shortest-path guards, and transaction differences.
- [DuckGQL and MGQL audit](duckdb-mgql-audit.md) records licensing boundaries, typed binding architecture, path algorithms, and the limits of the formal calculus.
- [Source metadata](sources.json) records archive provenance and adoption decisions.
- `../REFERENCE_VALIDATION.md` records implementation-specific test evidence and reproduction commands for this batch.

New production behavior must still begin with a failing in-repository test and retained report. A test linked to a production or another engine is evidence for that reviewed behavior, not a substitute for the outstanding clause-level conformance review.
