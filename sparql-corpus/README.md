# Jena and QLever SPARQL correctness corpus

A complete source distribution of the implemented RDF4J/JUnit Jupiter port, with pinned upstream sources, fixtures, per-case documentation, extraction audits and actual execution reports. **The exhaustive port of every native upstream test is not complete.** Inventory-only declarations, unavailable prerequisites and unhandled assertion forms are recorded separately, never counted as passing tests.

## Run the included corpus

Requires JDK 25 and Maven. Dependencies must be available locally or downloadable from Maven repositories. Python and Jena are not needed to execute the already generated RDF4J tests.

```sh
mvn test
mvn test -Dsuite.filter=jena-native-captured
mvn test -Dsuite.filter=qlever-native-lazy
mvn test -Dsuite.filter=TestSPARQLKeywordFunctions
mvn test -Dsuite.filter=OptionalJoin
```

A failing Maven result is expected when an imported assertion differs from the selected RDF4J engine. Expected answers are not rewritten to turn the suite green. Engine extensions, lexical comparison policies, unsupported prerequisites and harness issues must be separated from genuine RDF4J defects during triage.

Each imported case is a separately named Jupiter dynamic test. Every case receives a fresh repository, which is closed in `finally`. Loading, transactions, preparation and evaluated-query interaction use `RepositoryConnection`. Set `-Drepository.factory=your.package.Factory` to a public no-argument implementation of `RepositoryFactory`; add its Sail dependencies to the POM.

## Documentation and coverage

Open `corpus/catalog.html` for the searchable catalogue or read `corpus/cases/<id>.md`. Records document fixture files/default and named graphs, base IRI, complete query, original expected rows/results/assertion, immutable source provenance, and adaptation boundaries.

`sources.json` pins `HASMAC-AS/jena` at `df0d523eed42cd0b7c9ef11c74eeb0a63302a423` and `HASMAC-AS/qlever` at `a815b3b71a6c764d927332dcd5505fe6436ad214`. Fetching validates regular tracked source files against their Git blob hashes. Test archives and extracted members retain provenance. Non-test font binaries are omitted from distribution and listed in `SNAPSHOT.json`.

`coverage.json` reports the implemented catalogue. `native-inventory.json`, `unregistered-query-files.json`, `import-errors.json`, `native-adapter-gaps.json`, `optional-port-audit.json`, `lazy-port-audit.json` and `jena-capture-audit.jsonl` expose remaining gaps. Source cases repeated across historical upstream archives are distinct instances, not unique semantic behaviors. A captured helper call does not imply every internal assertion in its enclosing C++/Java method is covered.

## Result comparison

The core comparator preserves bags, duplicate mappings, unbound variables, RDF term identity and globally consistent blank-node identity. Blank-node-free results use hash counts; blank nodes and nested triple terms use whole-relation isomorphism. Graph results use graph isomorphism. REDUCED cases have a source-multiplicity interval comparator with explicit search limits, not strict equality or silent set conversion.

Simple projected outer ORDER BY keys are checked as partial orders independently from the result bag. Valid ties are accepted, and incomparable adjacent values cannot hide an inversion elsewhere. Unsupported order expressions and hidden sort keys remain explicitly blocked rather than falsely asserting complete order coverage. FROM/FROM NAMED datasets are recovered from the parsed query and are not replaced by an unrelated default dataset.

Jena native helper modes preserve exact term, value-plus-datatype, floating tolerance and type-predicate assertions. The source-derived 676-pair comparator matrix is a harness calibration, not imported query coverage. QLever YAML preserves its weaker original checks, including wildcard cells, the numeric tolerance and the 5,000-row return cap. Unsupported YAML checks are not silently discarded.

See `docs/NATIVE-CAPTURE.md` for compiler-backed argument capture, the known source-helper defect, lazy-operation adaptations and their limitations.

## Rebuild all implemented cohorts

```sh
python3 -m pip install -r requirements.txt
python3 -m unittest discover -s tools/tests -v
python3 tools/rebuild.py
mvn test
python3 tools/report.py
python3 tools/package.py --stage final
```

Add `--download` to `rebuild.py` only to fetch the pinned forks again. The pipeline combines manifests, reviewed OPTIONAL/EXISTS ports, lazy MINUS/OPTIONAL/EXISTS ports, compiler-backed Jena helper capture, and final ordering policy. Native capture uses a compiled matching Jena checkout for extraction only. It reuses `JENA_REFERENCE_ROOT` plus `JENA_REFERENCE_LIBS` when supplied; otherwise it builds a reference under `target/`. The source-index/event JSONL also supports `python3 tools/native_capture.py --import-only`.

## Execution bounds and reporting

`-Dsuite.strict=true` turns missing prerequisites into assertion failures rather than skips. `-Dsuite.timeoutSeconds=15`, `-Dsuite.maxRows=500000`, and `-Dsuite.maxFixtureBytes=20000000` set explicit resource limits. Result truncation is not accepted as exact equivalence. Raise the fixture limit to run larger datasets when appropriate. External SERVICE/FROM dependencies are never fetched from live endpoints without a supplied fixture.

`reports/per-case-results.jsonl` is the ID-based outcome ledger. JSON diagnostics escape non-ASCII UTF-16 code units, so invalid Unicode parser messages cannot make journal entries disappear. `reports/verification.json`, `VERIFICATION.md`, `failures.json`, Surefire XML and the Maven log preserve failed assertions, evaluation errors, skipped cases, missing outcomes and harness results separately. A successful archive upload is not a successful test run.

## Full source ZIP and patches

Packaging runs before and after tests and retains the source even on failure. It verifies ZIP CRCs and every archived file's SHA-256. Source, corpus, fixtures, vendor trees, provenance and reports are included; compiled classes, caches and font binaries are excluded.

The artifact bundle contains `sparql-corpus-complete-source.zip`, `complete-source.patch`, `incremental-source.patch`, `PATCH-VERIFICATION.json`, `SHA256SUMS` and `snapshot.json`. The full patch applies to bootstrap `a5ebbdb03ac304f69e054c90c4d9fb1cc8b100a3`; the incremental patch applies to the previously delivered source commit `4c8a3e9126e4b00a1f8db01ba1ee15f447458688`. Packaging applies both patches to their stated bases and checks tracked bytes and executable modes. The generated corpus/vendor files are in the complete ZIP and regenerated by the importer, not duplicated in tracked-source patches.
