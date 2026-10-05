# SPARQL correctness corpus: recovery edition

This is a complete, independently buildable source project using the pinned HASMAC-AS forks. It is not an exhaustive port of every native assertion, nor a recovery of the inaccessible v2 working directory. ZIP checkpoints include all implementation source, materialized fixtures, catalogues, vendor source and reports, even when tests fail.

## Run

Requires JDK 25, Maven and Python 3.11+. First-time dependencies need network access.

```sh
python3 -m pip install -r requirements.txt
python3 tools/build_corpus.py --download
mvn test
python3 tools/report.py
python3 tools/package.py local
```

A fully imported ZIP already includes vendor source trees. `python3 tools/build_corpus.py` regenerates without another download. `tools/import_corpus.py` provides the base manifest/YAML importer; `tools/build_corpus.py` is the complete entry point including additional reviewed native adapters.

```sh
# Only query cases whose names/IDs match the regular expression.
mvn test -Dsuite.filter='^qlever-'
# Force a fresh repository for every query rather than reusing immutable fixtures.
mvn test -Dsuite.fixtureCacheSize=0
# Supply a public no-argument implementation of RepositoryFactory.
mvn test -Drepository.factory=my.tests.Factory
# Fail, rather than report aborted, when a case has an unported prerequisite.
mvn test -Dsuite.strict=true
# Run only harness calibration tests.
mvn test -Dtest=ResultOracleTest,NativeAssertionOracleTest
```

All repository loading, transactions, and query preparation/evaluation use RepositoryConnection. Each query gets a fresh connection. The default read-only fixture cache holds at most two repository/dataset combinations; setting fixtureCacheSize=0 creates a fresh repository per query. No SPARQL updates run on cached repositories. DEFAULT and named graphs are isolated explicitly, and FROM resolves only to materialized source documents. SERVICE cases without controlled endpoint fixtures remain blocked and never contact their original external endpoints.

## Documentation and provenance

`corpus/catalog.html` is a searchable offline catalogue. `corpus/cases/<id>.md` documents each record's full query, default/named graph prerequisites, original expected-result asset or assertion, base URI, requirements, pinned source link and adaptation notes. JSON preserves the same information and hashes. Archived upstream collections are retained and identify their original ZIP and member name. They are distinct provenance records, not claimed to be distinct semantic scenarios.

`corpus/native-inventory.json` lists active Java/C++ test declarations with complete source excerpts. Comments/string-embedded declarations are excluded. `corpus/native-audit.json` identifies remaining candidates and partial adapters. Inventory-only declarations, unexpanded parameterized tests and unrelated native tests are NOT counted as executable ports. `corpus/discovery.json` reports manifest parsing diagnostics and unresolved includes. Scanning all files is not proof of registration in an upstream runner.

## Assertion fidelity

Manifest SELECT cases compare duplicate-preserving bags (or source ordering) with globally consistent blank-node mapping. The fast path hashes blank-node-free rows. Blank-node results use an RDF encoding of the full relation, including row occurrence and order group, followed by graph isomorphism. No query evaluation is used to produce expected results. REDUCED cardinalities are bounded by the upstream expected bag; blank-node/REDUCED combinations still require a specialized oracle. Literal lexical forms and datatypes are strict by default, which can be stricter than upstream numeric-value comparators. ORDER BY ties on projected variables allow within-group permutations; complex hidden ordering uses the exact source sequence. These comparator-policy differences must be investigated before calling a mismatch an RDF4J defect.

Reviewed Jena native TestExpressions helpers retain their actual comparison categories: booleans and strings compare values; numeric overloads retain integer/int32/int64, scale-sensitive BigDecimal, and numeric double-conversion behavior. Jena's isDouble helper category includes decimal and integer values. Expression-error assertions produce one unbound solution cell; they do not become empty result sets or arbitrary expected exceptions. Partial-expression parser-only tests remain inventory records.

QLever native EXISTS adapters explicitly encode compatibility predicates over renamed RHS variables, avoiding ambiguous outer-variable substitution into VALUES headers. OPTIONAL adapters preserve join columns, output-column layout, duplicates and UNDEF. Only constant-table helper invocations without unsupported mutations or loops are ported. Native execution strategy, sorting implementation, internal IDs, cache behavior and lazy-chunk boundaries are not asserted. IDs map injectively to IRIs, not guessed numeric values.

QLever YAML checks retain wildcard null cells, a 0.1 float tolerance and the upstream 5,000-row return cap. Scientists data is parsed as Turtle, as in QLever's e2e runner, despite its .nt extension. Relative data/query/expected IRIs share one documented synthetic base. Original weak YAML assertions are not relabelled as exact-result equality.

## Results, ZIP and patch

Read `reports/verification.json`, `reports/corpus-results.json`, `reports/junit-results.json`, and copied Surefire XML for actual outcomes. Corpus results are separate from harness calibration. Errors and failures stay errors/failures; capability annotations do not turn them into passes. Invalid Unicode diagnostics are JSON-escaped so every case remains reportable.

The workflow checkpoints source before installation, after import and after execution. `tools/package.py` uses only the standard library, streams the complete source ZIP, verifies CRCs and includes per-file SHA-256 hashes. It excludes caches, symlinks and unrelated font files, with an explicit exclusions list. `complete-source.patch` is against bootstrap commit a5ebbdb03ac304f69e054c90c4d9fb1cc8b100a3, not against v2. The ZIP contains generated corpus/vendor data in addition to all tracked implementation files. Upstream licenses and notices remain in their vendor trees.

The isolated recovery branch is an artifact-building branch, not a change intended for RDF4J main.
