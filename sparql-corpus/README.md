# SPARQL correctness corpus — recoverable source edition

This archive contains the complete source of the implemented importer, RDF4J/JUnit Jupiter harness, generated case definitions, retained vendor sources, fixtures and execution reports. **It is not a claim that every upstream native test has been ported.** It is a reconstructed source edition, not a verified superset of the inaccessible earlier interactive v2/v3 workspace.

## Run the packaged corpus

Requires JDK 25 and Maven. The first Maven run needs dependency access. Python and repository downloads are unnecessary to run an already-imported archive:

```sh
mvn test
mvn test -Dsuite.filter=ExistsJoin
mvn test -Dsuite.filter=OptionalJoin
mvn test -Dsuite.filter=extendedType
```

The tests intentionally report incompatibilities and mismatches; a nonzero Maven exit is not automatically a broken build. Consult `reports/verification.json`, `reports/failures.json` and the Surefire XML to distinguish compilation failures, failed result assertions, exceptions and prerequisite skips.

## Regenerate from pinned sources

`tools/rebuild.py` is the single entry point for manifest extraction, all implemented native adapters and final oracle policies. Python 3.11+ is required only for regeneration:

```sh
python3 -m pip install -r requirements.txt
python3 -m unittest discover -s tools/tests -v
python3 tools/rebuild.py
mvn test
python3 tools/report.py
python3 tools/package.py --stage final
```

Add `--download` to `rebuild.py` only to retrieve the pinned forks again. Download validates each regular tracked file against its Git blob SHA. The lower-level `import.py`, `build_corpus.py` and `optional_ports.py` scripts are retained as implementation modules; invoking just one does not reproduce the final combined corpus.

## Repository implementations and resource bounds

All repository operations use `RepositoryConnection`: fixture loading, transactions, query preparation and evaluation. `-Drepository.factory=my.pkg.Factory` selects an implementation of `RepositoryFactory` with a public no-argument constructor; add its Sail dependencies to the POM. Each case gets a fresh repository, shut down in `finally`. The default is a non-inferencing MemoryStore. The parsed FROM/FROM NAMED dataset is preserved rather than confused with the API's optional dataset override.

`-Dsuite.strict=true` makes unavailable prerequisites fail instead of aborting. Actual result mismatches always fail. Other bounds are `-Dsuite.timeoutSeconds=15`, `-Dsuite.maxRows=500000`, `-Dsuite.maxFixtureBytes=20000000`, and `-Dsuite.maxOrderPairs=10000000`. Hitting a row bound never causes a truncated result to be accepted. Large fixtures and order-oracle work above the configured bound are explicit prerequisites, not passes. Raise the fixture bound explicitly to run the QLever scientists dataset cases.

## Catalogue and provenance

`sources.json` pins HASMAC-AS/jena main and HASMAC-AS/qlever master. `corpus/cases.json` holds each imported query or query-syntax test. `corpus/catalog.html` is a searchable offline catalogue; `corpus/cases/` contains one Markdown document per case, including query, fixture and graph prerequisites, base IRI, expected result or original graph-result asset, immutable source link and hash, original native assertion where available, and adaptation limitations.

Source instances duplicated across upstream test collections remain separate records. The catalogue count is not the number of unique semantic behaviors. `corpus/native-inventory.json` separately inventories Java/C++ native test declarations; **inventory-only records are not ported tests**. Arbitrary inherited and parameter-generated invocation expansion is incomplete. `manifest-inventory.json`, `other-manifest-records.json`, `unregistered-query-files.json`, `native-adapter-gaps.json`, `optional-port-audit.json` and `import-errors.json` expose remaining scope and conversion gaps. Transitive external conformance suites absent from the source snapshots are not silently represented as imported.

Vendor test sources, extracted test-archive members, original test ZIPs, licenses and notices are retained. Non-test UI font binaries are omitted; their paths are listed in `SNAPSHOT.json`. A stable virtual base `https://corpus.invalid/` maps packaged assets without dereferencing that hostname. Missing external FROM/SERVICE prerequisites are reported rather than fetched from live endpoints.

## Oracle semantics

Expected results come from original fixtures or assertions, never from executing RDF4J to manufacture an expected answer. Ground SELECT bags use hash counts and preserve duplicate rows, unbound variables, RDF-term kinds and lexical forms. Blank-node results use an RDF encoding of the entire relation and a globally consistent bijection, including nested triple terms. Graph outputs use graph isomorphism. Exact term equality is stricter than the numeric-value comparison used by some Jena runner modes; such differences require triage.

REDUCED `mf:LaxCardinality` tests accept each distinct expected solution between one and its source multiplicity. Blank-node variants use bounded backtracking with a global bijection; oracle search exhaustion is an inconclusive error, not a successful test or an established engine defect.

Outer ORDER BY over projected variables is now checked independently from multiset equality. The comparator implements the partial order of SPARQL 1.1 section 15.1, including term-kind ordering, numeric promotion, Unicode codepoint string/IRI comparison, booleans and dateTimes. It allows specification-undefined relative ordering and valid ties instead of imposing a particular upstream engine's arbitrary order. Subsequent keys are required only after identical RDF terms. All comparable row pairs are checked because an incomparable intermediate value can hide a reversal from adjacent-row checks. Complex expressions, non-projected keys and RDF 1.2 triple-term ordering need further oracles and remain explicitly unsupported. LIMIT/OFFSET tests with multiple valid tie-boundary slices can still differ from one fixed expected fixture; those differences are not automatically RDF4J bugs.

QLever YAML retains its original weaker checks: wildcard cells, numeric tolerance 0.1 and at most 5,000 returned rows. Unsupported warning/ICU ordering checks remain blocked. CSV/SRT comparisons, extensions, some native-helper category assertions, entailment and implementation-defined DESCRIBE policy need additional adapters. Native EXISTS/OPTIONAL/BOUND adaptations assert their documented observable result semantics, not QLever chunking, optimizer choices or storage internals.

## Complete ZIP, patch and verification

`tools/package.py` uses only the Python standard library. CI uploads a source ZIP before installation and an expanded ZIP after import/testing, including on failure. CRCs and SHA-256 of every archived file are checked. Source, corpus, fixtures, vendor trees and reports are included; build classes and caches are excluded.

The main GitHub artifact contains `sparql-corpus-complete-source.zip`, `complete-source.patch`, `SHA256SUMS` and `snapshot.json`. A smaller reports-and-patch artifact is provided separately. The git patch is against isolated bootstrap commit `a5ebbdb03ac304f69e054c90c4d9fb1cc8b100a3`, not the old v2 archive. Packaging applies the patch to that baseline and checks all tracked source bytes. Generated corpus/vendor data is included in the complete ZIP and regenerated by the importer, not repeated in the git patch.

`reports/verification.json` reconciles definitions with per-case execution IDs, separates query evaluation from syntax and harness calibration, and reports unexecuted/duplicate/unknown IDs. `per-case-results.jsonl` records actual outcomes; `failures.json` supplies initial triage categories and provenance. Categories are not root-cause findings. Nothing converts an unexpected failure into a pass, and packaging success is not test success.
