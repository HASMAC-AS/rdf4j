# SPARQL correctness corpus: recovery edition

This is a complete, independently buildable implementation reconstructed from the pinned HASMAC-AS forks. It does not claim to recover the inaccessible v2 working directory or to be an exhaustive native-test port. All generated definitions, source evidence, fixtures and reports available at a checkpoint are packaged even if import or execution fails.

## Run

Requires JDK 25, Maven and Python 3.11+. First-time dependency and source retrieval needs network access.

```sh
python3 -m pip install -r requirements.txt
python3 tools/import_corpus.py --download
mvn test
python3 tools/report.py
python3 tools/package.py local
```

A fully imported archive already contains its vendor source trees, so `python3 tools/import_corpus.py` regenerates without another source download. `mvn test -Dsuite.filter=pattern` selects case identifiers/names. `-Dsuite.strict=true` turns blocked records into failures. `-Drepository.factory=my.tests.Factory` selects your public no-argument implementation of RepositoryFactory. Default implementation is MemoryStore. Repository operations, fixture loading and all query preparation/evaluation go through RepositoryConnection.

`corpus/cases.json` and the searchable `corpus/catalog.html` document executable and blocked query/syntax cases, complete query text, prerequisite fixtures, source assertions/results, source revision, and adaptations. Original expected-result files are preserved. `corpus/native-inventory.json` separately lists native Java/C++ test declarations with source excerpts; inventory-only entries are NOT claimed to be ported. `corpus/discovery.json` reports manifest parsing problems and includes, archives, non-query test kinds and native counts. Scanning every manifest is not proof that each is registered in an upstream runner. Protocol and update-only tests and unrelated RDF parser/unit tests are not misrepresented as query-result tests.

## Comparison contract

SELECT uses duplicate-preserving bags, or sequence order when requested, with globally consistent blank-node mapping. The common blank-node-free bag path uses hash counts. Blank-node cases use an RDF encoding of the result relation and graph isomorphism. REDUCED/LaxCardinality is checked against the upstream permitted cardinality range; unsupported blank-node/lax combinations fail explicitly. Literal lexical forms and datatypes are preserved. This may expose differences from upstream value-normalizing comparators; a mismatch is not automatically an RDF4J defect. Query expression errors produce an unbound result cell, not an empty solution sequence. ORDER BY ties are compared as unordered groups when the top-level order keys are projected variables; complex hidden order keys use exact upstream sequence, noted in reports. DESCRIBE or engine extensions may require implementation-specific behavior and remain explicitly classified.

The QLever YAML adapter keeps its original weaker checks (wildcard null cells, 0.1 floating-point tolerance, 5,000 returned-row cap). Unsupported text-index features, warning checks and uncontrolled SERVICE prerequisites are blocked, not silently discarded. The QLever scientists fixture is parsed as Turtle, matching e2e.sh, despite its .nt filename. Relative fixture/query IRIs use one documented synthetic base consistently.

## Source ZIP and patch

The workflow saves source before installation, after import, and after Java execution. `tools/package.py` uses only Python's standard library, verifies ZIP CRCs, and includes per-file SHA-256 hashes. All implementation files and the materialized corpus are present. Vendor .git directories, build caches, and unrelated font binaries are excluded and listed. `complete-source.patch` is against bootstrap commit a5ebbdb03ac304f69e054c90c4d9fb1cc8b100a3, NOT against v2. No original source revision or expected result is replaced with RDF4J output. Upstream license and notice files remain in the vendor trees.

Read `reports/verification.json` and `reports/junit-results.json` for actual outcomes. Successful packaging/import/compilation is not equivalent to passing tests. The recovery branch is isolated and must not be merged into RDF4J main as a normal source change.
