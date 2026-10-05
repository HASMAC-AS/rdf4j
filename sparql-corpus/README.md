# SPARQL correctness corpus — recoverable source edition

A complete source package for the implemented importer and RDF4J/JUnit Jupiter harness. **This is not a claim that every upstream native test has been ported.** The project is reconstructed from the pinned HASMAC-AS forks because the expanded interactive workspace could not be recovered. It does not claim to contain the inaccessible prior v2/v3 working tree.

## Run

Requires JDK 25, Maven, and Python 3.11+ for regeneration. The imported source ZIP already includes all discovered corpus definitions, fixture files, upstream sources, per-case documentation and actual execution reports. To run that corpus, Python and repository downloads are unnecessary:

```sh
mvn test
mvn test -Dsuite.filter=ExistsJoin
mvn test -Dsuite.filter=OptionalJoin
mvn test -Dsuite.filter=extendedType
```

To rebuild the corpus from its packaged vendor trees:

```sh
python3 -m pip install -r requirements.txt
python3 -m unittest discover -s tools/tests -v
python3 tools/build_corpus.py
python3 tools/optional_ports.py
mvn test
python3 tools/report.py
python3 tools/package.py --stage final
```

Run the reporting and packaging commands even if Maven exits nonzero. Add `--download` to `build_corpus.py` only to retrieve the immutable fork revisions again. Download verifies every regular tracked file against its Git blob SHA. `tools/import.py` is the underlying base importer; use `build_corpus.py` to include the reviewed native expansions and assertion-policy refinements, then `optional_ports.py` to append the reviewed OPTIONAL cohort. The latter is idempotent and checks its pinned source blob, lexical scope, UNDEF decoding, input widths, column mapping and source-asserted result multiplicities before emitting cases.

## Select repositories and execution bounds

`-Drepository.factory=my.pkg.Factory` selects an implementation of `RepositoryFactory` with a public no-argument constructor. Supply its additional Sail dependencies through the POM. Each case gets a fresh repository, shut down in `finally`. All fixture loading, transactions, parsing and query evaluation use `RepositoryConnection`.

`-Dsuite.strict=true` turns unavailable prerequisites into failures rather than Jupiter aborts. Ordinary RDF4J mismatches always fail. `-Dsuite.timeoutSeconds=15` controls query timeouts. `-Dsuite.maxRows=500000` bounds collected result size; hitting it fails, rather than accepting truncation. Fixtures larger than `-Dsuite.maxFixtureBytes=20000000` are aborted by default; raise this limit explicitly to run applicable large-fixture cases. This resource bound is visible in the execution report and is not a capability or coverage success.

## Scope and provenance

`sources.json` pins HASMAC-AS/jena main and HASMAC-AS/qlever master. `corpus/cases.json` contains individual query and query-syntax definitions. `corpus/catalog.html` is a searchable local catalogue; `corpus/cases/` contains individual Markdown descriptions with query, prerequisites, full expected result or referenced graph asset, source links, source hashes, and adaptation notes. `corpus/native-inventory.json` records native Java/C++ test declarations separately. **Inventory-only records are not ported tests.** Repeated inherited/parameterized invocation expansion remains incomplete. `manifest-inventory.json`, `other-manifest-records.json`, `unregistered-query-files.json`, and `import-errors.json` expose additional scope and discovery gaps. Source instances in duplicated upstream test collections remain distinct records; the catalogue count is not a count of unique semantic behaviors.

Every vendor source file and extracted test-archive member is retained, with licenses and notices. Non-test UI font binaries are omitted from the downloadable package; their paths are listed in `SNAPSHOT.json`. Existing test ZIPs are preserved. The corpus uses a stable virtual base (`https://corpus.invalid/`) for local assets, and never dereferences that hostname. Unprovided external FROM/SERVICE prerequisites are reported, not fetched from live endpoints.

The OPTIONAL extension documents both decoded input relations, the original join-column mapping, complete upstream expected rows, the original helper assertion, and query-level adaptation notes. It does not claim to verify QLever's internal ordering, lazy chunk layout, allocator behavior or selection of a physical join algorithm. `corpus/optional-port-audit.json` records the exact source cohort and source hash.

## Result comparison

Expected results come from upstream fixtures and assertions, never from RDF4J execution. SELECT comparison preserves duplicates, unbound cells, RDF-term kinds and globally consistent blank-node identity. Blank-node-free bags use hash counts. Blank-node results use graph isomorphism of an encoding of the complete relation; triple terms are structurally encoded. Explicit upstream row indexes select sequence comparison. This can be stricter than valid alternative ORDER BY tie arrangements, so those divergences require review.

REDUCED `mf:LaxCardinality` cases accept each expected distinct solution between one and its source multiplicity. Blank-node variants use bounded backtracking with a global bijection; exhausting the oracle search budget is reported as inconclusive/error, never as a pass. Graph results use graph isomorphism.

RDF term lexical equality is the default, which is stricter than value-based comparison in some Jena modes. The QLever YAML adapter supports original weaker checks: wildcard cells, numeric tolerance 0.1, and at most 5000 returned rows. The presence of adapter code or an inventoried YAML query does not establish executable coverage; use `coverage.json` and the per-case execution journal. Unported warning/ICU ordering assertions remain blocked. CSV and SRT oracles are blocked pending their format-specific comparisons. Engine extensions, extended datatype behavior, native helper category assertions, entailment and implementation-defined DESCRIBE policies are not silently equated with standard SPARQL semantics.

## Always-created complete ZIP and patch

`tools/package.py` has no third-party dependencies. CI creates and uploads a ZIP before installation and an expanded ZIP after import/testing even when those steps fail. ZIP CRCs and the SHA-256 of every archived file are checked. Source, corpus, fixtures, vendor trees and reports are included; build classes and caches are excluded. The CI artifact bundles `sparql-corpus-complete-source.zip`, `complete-source.patch`, `SHA256SUMS`, and `snapshot.json`. A separate smaller `sparql-corpus-reports-and-patch` artifact contains the reports and patch without the vendor source trees.

The git patch is against the isolated bootstrap commit `a5ebbdb03ac304f69e054c90c4d9fb1cc8b100a3`, not against the old v2 ZIP. Packaging applies it to that baseline in a temporary checkout and checks all tracked source bytes. Generated corpus data is in the complete ZIP and regenerated by the importer, not duplicated in the git patch.

`reports/verification.json` reconciles discovered cases with individually recorded executions and records importer/build status. `reports/per-case-results.jsonl` has stable IDs, names and pinned provenance; `reports/failures.json` isolates failures; Surefire XML and Maven logs retain full details. Packaging success, import success and Java compilation success are distinct from passing query correctness tests.
