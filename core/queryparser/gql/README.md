# GQL queries over RDF

This module adds `QueryLanguage.GQL` to RDF4J. It parses a documented GQL query profile directly into RDF4J query algebra. An LMDB repository evaluates that algebra using its existing optimizer, adjacency readers, native execution engine, transactions, and cancellation machinery. It does not maintain a second copy of the graph.

This is **query support, not complete ISO/IEC 39075 conformance**. GQL also defines graph updates, schemas, catalogs, sessions, extensive types, and other facilities. Unsupported constructs are rejected during query preparation. The supported syntax and RDF mapping below are the contract of this module.

Work toward the complete published standard, including the 2026 corrigendum, is tracked in [the conformance inventory](conformance/README.md) and the repository-root `gql-conformance-exec-plan.md`. The inventory explicitly records unverified requirements and the missing full normative base text. Engineering continues using the [reviewed supplied reference archives](conformance/references/README.md); their tests and implementations do not establish complete conformance.

## Using the parser

The `rdf4j-client` aggregate includes the parser. Applications using individual modules can add:

```xml
<dependency>
  <groupId>org.eclipse.rdf4j</groupId>
  <artifactId>rdf4j-queryparser-gql</artifactId>
  <version>6.2.0-SNAPSHOT</version>
</dependency>
```

No parser-generator or other new third-party dependency is required. The normal query parser service registry discovers `GQLParserFactory`.

Given the RDF statements:

```turtle
@prefix ex: <https://example.org/> .
ex:alice a ex:Person ; ex:name "Alice" ; ex:knows ex:bob .
ex:bob a ex:Person ; ex:name "Bob" ; ex:knows ex:carol .
ex:carol a ex:Person ; ex:name "Carol" .
```

the following code returns Carol:

```java
try (RepositoryConnection connection = repository.getConnection()) {
    TupleQuery query = connection.prepareTupleQuery(QueryLanguage.GQL, """
            MATCH (person:Person)-[:knows]->(friend)-[:knows]->(other)
            WHERE person = $start
            RETURN other, other.name AS name
            ORDER BY name
            """, "https://example.org/");
    query.setBinding("start", connection.getValueFactory().createIRI("https://example.org/alice"));
    query.setMaxExecutionTime(30);
    try (TupleQueryResult result = query.evaluate()) {
        while (result.hasNext()) {
            BindingSet row = result.next();
            System.out.println(row.getValue("name"));
        }
    }
}
```

`repository` can be a `SailRepository` containing an `LmdbStore`, or another compatible RDF4J repository. Import `RepositoryConnection`, `TupleQuery`, `QueryLanguage`, `TupleQueryResult`, and `BindingSet` from their standard RDF4J packages. Repository initialization, shutdown, and transactions are unchanged.

Parameters and graph variables have separate namespaces. `setBinding("start", value)` binds `$start`; it does not bind `(start)`. Parameters are RDF values, never text substituted into the query. A prepared query can be evaluated repeatedly with changed bindings. `setDataset`, `setIncludeInferred`, result closure, and query timeouts use the ordinary repository contracts.

The RDF4J HTTP protocol can select the parser with `queryLn=GQL` on installations containing this module. Omitting `queryLn` retains the existing SPARQL default. No new GQL media type is introduced.

## RDF graph projection

| GQL concept | RDF representation |
| --- | --- |
| Node | An IRI or blank node appearing as a subject, or as a non-`rdf:type` resource object |
| Node identity | The RDF IRI or blank node itself |
| Node label | An IRI object of an `rdf:type` statement |
| Directed edge | A statement with IRI/blank-node endpoints whose predicate is not `rdf:type` |
| Edge label | The statement predicate IRI |
| Edge identity/result | The full RDF triple term, including subject, predicate and object |
| Node property | A literal-valued predicate of the node |
| Missing property or optional variable | An absent binding in the result `BindingSet`, representing null |
| Graph | The normal RDF dataset projection, or a named RDF context selected with `USE GRAPH` |

Literal values and RDF triple terms are not nodes. A class IRI used only as an `rdf:type` object is a label, not an automatically created node. A node with only labels or literal properties still exists. A resource occurring only at the object end of an edge also exists. `MATCH (n) RETURN n` emits every node once regardless of how many labels, properties, or edges it has.

Literal properties do not become edges. `rdf:type` statements supply labels and are excluded from edge traversal. Distinct predicates between the same endpoints are distinct edges. RDF cannot represent two distinct copies of the same triple in one graph; this projection does not invent parallel edge IDs for such copies. Edge properties and reification/annotation mappings are not implemented.

A scalar property has zero or one **distinct RDF term** value. Zero values produces null. More than one value raises `QueryEvaluationException`, including when a `COALESCE` expression surrounds the property access. The implementation never picks an arbitrary first value or multiplies path rows by the number of property values. Literal lexical forms and datatypes remain meaningful RDF terms.

The default graph projection follows the repository's normal default dataset. Identical RDF triples across merged contexts are coalesced **before** path matching. Distinct paths remain distinct rows. A selected named graph consistently supplies its edges, labels, node population, and properties:

```gql
USE GRAPH `urn:people`
MATCH (a:`https://example.org/Person`)-[:`https://example.org/knows`]->(b)
RETURN a, b
```

A graph parameter can replace the graph identifier: `USE GRAPH $graph`. Bind it to an IRI. An unbound graph parameter never becomes a wildcard scan over all named contexts.

### Names and namespaces

Full IRIs are legal GQL delimited identifiers:

```gql
MATCH (a:`urn:Person`)-[:`urn:knows`]->(b)
RETURN a.`urn:name` AS name, b
```

Bare label/property identifiers are appended to the absolute base URI passed to `prepareTupleQuery`. In the Java example, `Person` maps to `https://example.org/Person` and `name` to `https://example.org/name`. This is an RDF namespace mapping, not local-name matching or inferred prefix resolution. Without a base URI, use absolute IRIs. Repository namespace declarations are not consulted.

Single and double quotes delimit string literals in value expressions. Double quotes also delimit identifiers in name positions, including labels, properties, parameters, and result aliases; backticks delimit identifiers. Binding variables, including node/edge variables and LET targets, use regular identifiers. A double-quoted value is always a string literal, even when a variable has the same text. Keywords are case insensitive and names are case sensitive. Delimited names are never interpreted as keywords. The profile permits contextual keywords as unquoted identifiers in otherwise unambiguous name positions; it does not enforce ISO's complete reserved/pre-reserved word list. `--`, `//`, and `/* ... */` comments are supported. `--` is a comment, so use `(a)-(b)` or `(a)-[]-(b)` for any-direction matching.

The `@` prefix disables backslash escapes in a quoted sequence. It also applies to delimited parameter names, for example `$@"path\\name"`; bind the exact resulting name through `setBinding`.

## Supported query forms

The parser supports `MATCH`, `OPTIONAL MATCH`, clause-local `WHERE`, pipeline `FILTER`/`FILTER WHERE`, `LET`/`LET VALUE`, `RETURN`, `RETURN ALL`, `RETURN DISTINCT`, `GROUP BY`, `ORDER BY`, `OFFSET`/`SKIP`, `LIMIT`, `UNION`/`UNION ALL`/`UNION DISTINCT`, and `NEXT` with optional `YIELD`. Each stage ends with `RETURN`; a final semicolon is optional. UNION branches must expose the same column names in the same order.

Patterns support anonymous/named nodes and edges, labels, node property maps, node-local predicates, directed outgoing/incoming/any-direction edges, comma-separated patterns, repeated variables, self-loops and finite anonymous edge quantifiers. Label alternatives (`|`), conjunctions (`&`), negation (`!`) and wildcard (`%`) are supported; parenthesized label expressions are not implemented. A node wildcard requires at least one IRI label; `!%` selects unlabelled nodes.

```gql
MATCH (a:Person {name: 'Alice'})-[:knows]->(b)
OPTIONAL MATCH (b)-[:worksAt]->(company)
WHERE company.name IS NOT NULL
RETURN a, b, company.name AS company
```

The optional predicate belongs to its optional match. A later `FILTER company.name IS NOT NULL` instead removes the null-extended rows. A nullable variable is never rebound as a fresh unconstrained variable by a subsequent match.

Right-hand sides within one LET statement see the incoming bindings, not sibling definitions. `LET a = 1, b = a` is rejected when `a` was not already available. Use separate statements, `LET a = 1 LET b = a`, to make the first definition available to the second.

### Query composition

`NEXT` passes the preceding result rows into a new read-query stage. Only returned columns are visible in that stage. `YIELD` can select columns and rename them into regular binding variables:

```gql
MATCH (person:Person)-[:knows]->(friend)
WHERE person = $start
RETURN friend AS candidate
NEXT YIELD candidate AS person
MATCH (person)-[:knows]->(other)
RETURN other, COUNT(*) AS routes
GROUP BY other
```

The boundary preserves duplicate rows, null values, and the RDF node/edge identities. A carried edge constrains the next match to that same triple identity; a carried null cannot become a fresh unconstrained edge. Stage variables remain separate from API parameters even when their names match. Grouping and local aliases do not leak across the boundary.

YIELD selection happens after the preceding DISTINCT, ordering, and pagination. Later predicates cannot change which rows an earlier LIMIT selected. UNION branches complete before a following NEXT, and every UNION branch in the new stage receives the same incoming relation and visible column names. The current lowering evaluates the incoming deterministic read algebra separately in each UNION branch; it does not promise once-only execution for future volatile expressions, procedures, or mutations.

A `USE GRAPH` selection is local to its query stage; a later stage can select its own graph. This composition does not add session graph commands or general subqueries. Typed LET initializers and parameterized pagination remain unsupported.

### Paths and matching modes

This RDF4J profile defaults to `REPEATABLE ELEMENTS` and `WALK`. State the mode explicitly for portable expectations; this document does not claim that all ISO default semantics have been established from the public grammar.

```gql
MATCH REPEATABLE ELEMENTS (a)-[:knows]->(b), (a)-[:knows]->(c)
RETURN a, b, c
```

`MATCH DIFFERENT EDGES` forbids reusing a full edge identity anywhere in that MATCH clause; a later MATCH begins a new scope. Fixed patterns additionally support `WALK`, `TRAIL`, `SIMPLE`, and `ACYCLIC` path prefixes. TRAIL forbids repeated edges, ACYCLIC forbids repeated nodes, and SIMPLE allows only the start/end node to coincide.

Finite anonymous edge quantifiers use GQL postfix syntax:

```gql
MATCH (a)-[:knows]->{0,3}(b)
WHERE a = $start
RETURN b
```

These compile into ordinary joins and unions. They preserve one result per path; two different paths ending at the same node produce two rows. `RETURN DISTINCT b` requests endpoint deduplication. Zero hops requires the same existing node at both ends. Self-loops are emitted once for any-direction matching, not once for each orientation.

Quantified edges require anonymous edge variables and repeatable WALK semantics in this implementation. The maximum bound is 32 hops. Unbounded paths, shortest-path selectors, path values, quantified edge-group values, and quantified TRAIL/SIMPLE/ACYCLIC/DIFFERENT EDGES are rejected. SPARQL's endpoint-reachability operator is deliberately not substituted for GQL path enumeration.

### Values and aggregation

Expressions include strings, integers, decimals, floating-point numbers, booleans, null, parameters, property access, arithmetic, concatenation, comparisons, `AND`/`OR`/`XOR`/`NOT`, `IN`, `BETWEEN`, `IS NULL`/`IS TRUE`/`IS FALSE`/`IS UNKNOWN`, and CASE expressions. GQL conditions require boolean values; RDF4J's SPARQL truth-value coercion of arbitrary numbers and strings is not used.

Numeric tokens support separators between digits, binary/octal/hexadecimal integers (`0b1010`, `0o12`, `0xA`), and an optional separator directly after a radix prefix (`0x_FF`). Radix prefixes are lowercase. Unsuffixed integers use arbitrary-precision RDF integers; decimal-point forms and `M`/`m` suffixes use exact RDF decimals. `F`/`f` selects an RDF float, `D`/`d` an RDF double, and an exponent without a suffix selects a double. Examples include `1_000`, `.5F`, `1.25e2M`, and `6.02e23D`. Non-finite literal overflow is rejected. Exact exponent expansion and source-token length are bounded before allocating a large decimal lexical form.

LIMIT, OFFSET/SKIP, and finite path bounds accept unsigned decimal/radix integer spellings and separators. They reject signs, decimal points, exponent forms, and numeric suffixes, retain signed-long range checks, and retain the existing 32-hop path limit.

Simple CASE supports multiple WHEN operands and comparison or null/truth predicate tails:

```gql
RETURN CASE 12
  WHEN 0, 1 THEN 'small'
  WHEN >= 10 THEN 'large'
  ELSE 'other'
END AS size
```

Operands are tested in order with three-valued conditions, and only the selected THEN/ELSE value is evaluated. The existing profile's IN and BETWEEN predicates also work as CASE tails; this is not a claim that those extensions are ISO productions. Graph/type predicate tails and the full type system remain unsupported.

RDF literals retain their RDF4J numeric and comparison semantics. For example, division of integer literals produces an RDF decimal; ordinary RDF expression errors become null/unbound where RDF4J permits that. This is an explicit part of the RDF value projection, not a claim to implement every ISO numeric type and error condition. `MOD(a, b)` and `%` compute the remainder with the dividend's sign, preserving integer/decimal types and using normal numeric promotion. Use `AS` to assign stable output names to computed expressions.

Supported functions include `COALESCE`, `NULLIF`, `LOWER`, `UPPER`, `CHAR_LENGTH`, `CHARACTER_LENGTH`, `ABS`, `CEIL`/`CEILING`, `FLOOR`, `ROUND`, `MOD`, `CONTAINS`, `STARTS_WITH`, `ENDS_WITH`, `CONCAT`, and `SUBSTRING`. Graph identity helpers include `ELEMENT_ID`, `SOURCE`, and `DESTINATION`; the RDF projection also supplies `IRI`, `STR`/`STRING`, `ID`, and singular edge `LABEL` helpers. `ELEMENT_ID` and `ID` preserve the input RDF value; they do not manufacture an opaque string ID. `SOURCE`, `DESTINATION`, and `LABEL` extract the subject, object, and predicate RDF terms from an edge's triple identity. Unknown functions fail during preparation. `IN` accepts inline value lists rather than arbitrary list-valued expressions.

`COUNT`, `SUM`, `AVG`, `MIN`, and `MAX` support DISTINCT arguments. COUNT(*) counts matches, including duplicate paths. Non-count aggregates over no values produce null. Grouping follows the RETURN list; explicit `GROUP BY` is accepted after RETURN:

```gql
MATCH (a)-[:knows]->(b)
RETURN a, COUNT(*) AS degree
GROUP BY a
ORDER BY degree DESC
LIMIT 10
```

Without explicit grouping, the non-aggregate RETURN expressions supply grouping keys. GROUP BY can refer to the aliases of non-aggregate RETURN expressions; an aggregate result cannot be a grouping key. Variable references outside aggregate calls in grouped RETURN/ORDER BY expressions must be covered by grouping; aggregate arguments such as `COUNT(n)` and `SUM(n.age)` read the input rows normally. `ORDER BY` can use RETURN aliases and aggregate expressions when RETURN contains an aggregate or explicit grouping is present. Aggregate arguments use input bindings even when a RETURN alias has the same name. Explicit `NULLS FIRST` and `NULLS LAST` are supported; this profile places nulls last ascending and first descending by default.

## LMDB execution and performance

Predicate labels compile to constants in ordinary StatementPattern nodes. Graph joins stay visible to the optimizer. LMDB can select outgoing/incoming adjacency and its existing native execution plans. Named edge identities are constructed only when needed, avoiding an RDF triple allocation for anonymous topology-only traversals.

Merged graph edge deduplication uses a narrow native DISTINCT path over statement patterns and their filters. It reuses `RowDistinctPlan` and its ID-based distinct tracker rather than requiring a generic value-level island. Normal fallback remains available when a plan or transaction cannot use adjacency safely.

Scalar properties use grouped count/sample relations before attaching one value to each input row. This preserves correctness for arbitrary RDF data, but grouping can cost more than an unchecked single-value lookup. Unlabelled node enumeration and complex expressions may also use generic algebra operators. Enabling adjacency is not a promise that every operator executes as a native kernel; inspect the explanation and counters for the workload in question.

This branch's LMDB direct adjacency structures are in-memory indexes derived from persistent LMDB data, with the existing commit-delta/snapshot handling. GQL does not add a new persistent adjacency disk format.

A controlled initial comparison, including raw results and adjacency engagement, is in [the benchmark report](benchmarks/2026-11-09/README.md). The measured GQL means were higher than the corresponding SPARQL means; broad overlapping intervals do not establish an adjacency speedup.

## Limits and unsupported features

This module does not implement graph/schema/catalog DDL, GQL graph mutations, session or transaction commands, procedure calls, general subqueries, all GQL value types/functions, graph-valued results, path-valued results, undirected stored edges, or full ISO conformance. A MATCH predicate that accesses a property requires that owner to occur in that MATCH's pattern. Use a separate pipeline FILTER for a previously bound owner's property.

The tokenizer limits source length to one million characters and token count to 100,000. Expressions limit nesting and construction complexity; finite paths cap their upper bound at 32. The compiler caps expanded statement patterns at 4,096, label terms at 256, uniqueness comparisons at 4,096, and final algebra depth/size at 256/16,384 nodes. It validates the algebra iteratively before exposing it to recursive RDF4J visitors. These limits report `MalformedQueryException` with a source location. They prevent an accidentally enormous generated query from exhausting parser resources.

## Validation

After a quick install of changed modules and dependencies with tests skipped, run tests without `-am`:

```sh
mvn -o -Dmaven.repo.local=.m2_repo -pl core/queryparser/gql test
mvn -o -Dmaven.repo.local=.m2_repo -pl core/queryalgebra/evaluation -Dtest='Gql*Test' test
mvn -o -Dmaven.repo.local=.m2_repo -pl core/sail/lmdb \
  -Dtest='GqlLmdb*Test,LmdbNativeDistinctStatementTest,LmdbNativeRowPlanPollPointTest' test
```

The LMDB matrix checks cursor, generic-adjacency and native-adjacency configurations; actual adjacency counter increases; path/edge multiplicities; parameter changes; read-your-writes, deletion and rollback; retained snapshots; committed deltas and reopen; dataset isolation; result closure and interruption. `GqlAdjacencyQueryBenchmark` supplies corresponding GQL/SPARQL two-hop count queries with fanout, graph selection and adjacency parameters. Benchmark results are workload-specific; preserve their complete JMH settings when comparing runs.

## Design references

- [ISO/IEC 39075:2024](https://www.iso.org/standard/76120.html) and its [published grammar](https://standards.iso.org/iso-iec/39075/ed-1/en/ISO_IEC_39075(en).bnf.txt).
- [OpenGQL grammar](https://github.com/opengql/grammar), an implementation grammar based on the published standard.
- [SurrealDB GQL lowering](https://github.com/surrealdb/surrealdb/blob/main/doc/gql/LOWERING.md), for binding scopes, clause ownership and a shared execution representation.
- [DuckGQL adjacency execution](https://duckgql.com/docs/guides/csr-and-algorithms.html), for deriving topology-oriented indexes from authoritative storage.

These references informed the design. The RDF projection and the explicitly stated limitations are RDF4J implementation choices.
