# Audit of the supplied gql-compat and gql-grammar archives

This is an original implementation-oriented inspection of the supplied snapshots. No downloaded project code was executed. Counts below were obtained by reading YAML/XML/source data; tests have not been run against RDF4J as part of this audit.

## Provenance and licensing

| Source | Archive commit comment | License in archive | Version relevance |
| --- | --- | --- | --- |
| `gql-compat-main` (`github.com/tamnd/gql-compat`) | `9de19917d1891e7b40fde274207fbff4cd16bbbf` | Apache-2.0 | Targets the 2024 base; no 2026 corrigendum overlay found |
| `gql-grammar-main` (`github.com/TuGraph-family/gql-grammar`) | `bfe41725cb3bab7ed15802814928105386210729` | Apache-2.0; grammar headers name AntGroup, 2023 | README explicitly says March 2023 draft |

Archive SHA-256 values:

- `gql-compat-main.zip`: `b9e5e49a005b195bfe79ff8296ac04d5275b2a62124875e835f5dd09b051acc9`
- `gql-grammar-main.zip`: `9da95690cf7696a7f70cdb5b769fb2a74f66d10f7bb776cab45917185c20279c`

There is no evident source-code licensing blocker to an attributed Apache-2.0 port. Preserve the license and applicable attribution for copied or adapted tests/code and identify modifications. The ISO files in `gql-compat-main/iso/artifacts/` are separately sourced ISO material: the repository's Apache license must not be represented as relicensing those artifacts. Avoid redistributing their full contents as new RDF4J-authored material. Neither archive contains the full normative base text. `iso/artifacts/artifacts.go` explicitly says the prose is not redistributed; `subclauses.txt` is a transcription of numbers and headings from a preview table of contents.

## What gql-compat actually contains

The snapshot has **581 unique cases across 20 suite YAML files** and **28 fixtures**. The README's leading 263-case statement and embedded 579-case example are stale for this snapshot.

| Kind | Cases |
| --- | ---: |
| Mandatory | 107 |
| Optional | 360 |
| Condition | 69 |
| Grammar | 18 |
| Performance | 27 |

Expectations comprise 434 exact-row cases, 3 empty-result cases, 68 accept-only cases, 65 error cases, and 11 rejection cases. Claims cite 224 distinct optional-feature IDs, 252 distinct subclauses, and 800 distinct grammar productions. These are citation counts, not independently verified semantic coverage. A case can cite a rule without distinguishing all of its behaviors.

`corpus/corpus.go` models stable IDs; standard query text; fixture names; parameters; setup statements; expected column names and row bags; GQLSTATUS codes and weaker message matching; diagnostic fields; feature dependencies; dynamically sized limit violations; unsupported/unreachable cases; mutating and session-ending behavior; and distinct vendor dialects. `corpus/load.go` validates references, applies defaults, and rejects generated cases from the reviewed corpus.

`grammar/generate.go` performs seeded, depth-budgeted grammar walks using a minimum-token cost fixed point. Its derivation-aware reducer is useful for parser robustness and reproducing minimized failures. The project deliberately excludes generated cases from conformance scores and requires manual promotion. Retain that separation.

`consensus/` compares engine results but treats unanimous failures as review candidates. `corpus/dispositions.yaml` explicitly records shared lineage among Neo4j, Ladybug, and zu, and includes corpus-bug, shared-gap, ambiguous, and harness-limit categories. Vendor agreement is supplementary evidence, not an independent normative oracle.

## Concrete defects and limitations to quarantine

1. **An invalid grouping example.** `corpus/suite/20-optional-query.yaml`, case `optional/gq15/group-by`, uses `GROUP BY p.city`. Its own official `gql.bnf.txt` defines a grouping element as a binding-variable reference. Preserve RDF4J's corrected rejection; use the already-projected alias `city` in an original positive case. This source case must not be imported as an unquestioned oracle.
2. **A draft grammar repetition bug.** `gql-grammar-main/GQLParser.g4`, `groupingElementList`, permits `groupingElement (COMMA groupingElement)?`, limiting a nonempty list to two keys. The 2024 BNF permits arbitrary repetition. A wholesale grammar replacement would introduce a clear regression.
3. **Weak CASE-list coverage.** `mandatory/value/case-when-operand-list` tests the first operand (`CASE 1 WHEN 1, 2 ...`), so a parser/evaluator that ignores subsequent operands can pass. Original regressions should match the second operand, reject all operands, and exercise nulls.
4. **Weak normalization coverage.** `mandatory/value/normalize` supplies already-normalized ASCII. An identity function passes. Add decomposed/composed Unicode, all four forms, supplementary code points, null input, and negative normalized predicates.
5. **Old feature inventory.** The vendor files contain 228 feature IDs and the original 117/20 choice lists. Preserve the existing RDF4J 2026 correction overlay and 234-feature target; do not replace it with this older denominator.
6. **A case pass is weaker than type conformance.** `rows/rows.go` normally treats integral floating values as equal to integer values; graph element comparison omits stable identity and compares labels/types/properties. Node/edge identity and exact numeric type tests need independent typed assertions. `runner/runner.go` supplies unordered/column options but does not enable strict numeric types for every type case.
7. **Skip accounting needs explicit presentation.** `KindTotals.Rate()` divides pass by pass+fail, excluding skips and harness errors. Such a conditional pass rate can rise when failures are skipped. Preserve absolute counts, capability coverage and total selected cases alongside any rate; do not repeat the source comment that skipping cannot improve it.
8. **YAML 1.1 conversion corrupts expectations.** Stock Python `yaml.safe_load` turns unquoted `yes`/`no` in CASE expected rows into booleans. The corpus is written for a YAML 1.2-capable Go reader. A Python conversion must restrict implicit booleans to true/false, and should deliberately handle other YAML-version differences instead of inheriting them accidentally.

## Fixture and adapter implications for RDF4J

`fixture/fixture.go` defines fixtures as independent nodes and edges. Edge keys, properties, undirectedness and parallel-edge property independence are explicit. Sixteen capabilities are derived from the data. `adapter/adapter.go` requires a driver plus a session with Load, Exec, Reset, PID, DataDir and Close; optional interfaces handle explanation, schema loading, fault injection, and storage metadata. Query rewriting is forbidden in its conformance mode; vendor spellings are a separate compatibility mode.

The current RDF projection can faithfully represent useful subsets such as labeled `unit` and `chain` fixtures. It cannot faithfully load every fixture. In particular, the shared `social` fixture includes edge properties even for cases that read only node properties. A strict adapter must not advertise those capabilities and silently discard those properties. Either test it as an explicitly limited RDF compatibility projection or write an original smaller fixture that actually fits the supported model. Parallel edges, stored undirected edges, null-valued properties, lists, and isolated unlabeled nodes require the new native graph/value model. The fixture capability list itself does not separately model every isolated-node concern, so adapter-side representability checks are still needed.

An external Go adapter could communicate with a small Java JSON-lines process, preserving exact standard queries and RDF4J result types. For immediate implementation work, original JUnit cases selected from the reviewed YAML are cheaper and avoid introducing Go 1.26.5, Bolt, SQLite and other unrelated harness dependencies into the Maven reactor. A version-pinned corpus adapter is still a good later independent integration target.

## Practical next implementation batches

Each batch should use a failing Java test first, an original expected-result assertion, exact source case IDs for traceability, and MemoryStore plus the LMDB route matrix where relevant.

| Batch | Concrete source cases | Required strengthening |
| --- | --- | --- |
| Numeric lexing | `mandatory/lexical/digit-group-separator`; `optional/gl01/hexadecimal-literal`, `gl02/octal-literal`, `gl03/binary-literal`; GL04–GL09 exact/approximate suffix cases | Invalid digit/separator positions, radix boundaries, overflow policy, exponent signs, exact RDF datatype assertions |
| Contextual string functions | `mandatory/value/trim-default`, `normalize`, `normalized-predicate`; corresponding optional string cases | Real Unicode normalization; TRIM LEADING/TRAILING/BOTH syntax; null/type behavior; no identity-function loophole |
| Extended CASE | `mandatory/value/case-when-operand-list`, `case-when-operand-is-a-comparison` | Hit later list entries, unmatched lists, NULL operands, relational/null predicate forms, evaluate a volatile operand only once |
| Query pipeline syntax | `mandatory/query/order-by-as-a-statement`; `optional/gq08/filter-where-clause`; `optional/gq15/empty-grouping-set` | Duplicate-sensitive bags; ORDER/OFFSET/LIMIT order and zero bounds; empty input grouping; existing alias scope preservation |
| Query composition | `optional/gq20/next-composition`, `next-yield`; GQ04–GQ07 EXCEPT/INTERSECT cases | Independent duplicate and null fixtures; multiset subtraction/intersection semantics; alias rebinding across explicit scope barriers |
| Full query forms | Mandatory SELECT/FROM/nested-query/HAVING cases; GQ18 scalar subqueries; GQ21 OPTIONAL blocks | Shared backend subquery compatibility bug must be fixed before broad lowering; correlated input, empty/multirow scalar results, null-preserving optional blocks |

The lowest-risk next changes are numeric lexing, extended CASE, explicit empty grouping, and pipeline ordering/filter forms, after checking existing code for actual gaps. Rich collections, property-graph mutations/catalogs, and complete typed values are larger milestones; do not patch over missing representation by erasing fixture distinctions.

## Declaration and documentation reuse

`impdef/probes.yaml` separates observations from assertions for implementation-defined/dependent behavior. Examples cover overflow, inexact division, collation, default match mode, null ordering, identifier support, duration comparisons, and literal bounds. `impdef/statement.go` produces an item-by-item declaration template with the observing statement and unknown entries left unfilled. This is useful for replacing broad unsupported claims with documented decisions. Add the 2026 UA003 item and keep every observation tied to the RDF4J commit and configuration.

The archives remove the practical need to stop all implementation while waiting for the standard: they provide many usable, reviewable cases and concrete algorithms. They do not remove the need to distinguish direct official-grammar evidence, corrected rules already obtained, author interpretation, vendor observations, and unresolved semantics. Keep that evidence classification in the existing conformance records rather than declaring the corpus itself authoritative.
