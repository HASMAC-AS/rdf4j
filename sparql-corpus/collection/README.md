# Complete pinned test-source collection

This is the collection layer for the Jena/QLever corpus in `HASMAC-AS/rdf4j`.
It retains **all regular tracked files** at the revisions in `sources.lock.json`,
not just files recognized by the executable RDF4J importer. This includes test
source, fixtures, expected-result files, test runners, build configuration,
licenses, and suites embedded in upstream ZIP archives.

The source pins were checked against the heads of `HASMAC-AS/jena` and
`HASMAC-AS/qlever` on 2026-10-13. This collection does not claim coverage of every
triplestore, later upstream revisions, or every runtime expansion of a factory or
parameterized test.

## Open the collection

Open `index.html` here for the collection overview, or `../corpus/catalog.html`
for the separately maintained RDF4J/JUnit Jupiter cases. All JSONL files contain
one independently parseable JSON record per line.

| File | Contents |
| --- | --- |
| `files.jsonl` | Every verified Git file and expanded archive member, with byte hashes and modes. |
| `test-files.jsonl` | Files with test-related paths, including support files and fixtures. The path heuristic never excludes files from the full collection. |
| `manifest-tests.jsonl` | Every recognized manifest entry, including query, update, syntax, protocol, graph-store, serialization, RDF and SHACL tests. Original descriptors, query/update text, local assets, immutable provenance and existing RDF4J case links are retained. |
| `native-tests.jsonl` | Java/C++ declaration records with original source bodies, checksums, line ranges, prerequisites and links to existing adaptations. Includes JUnit 3 naming candidates, parameterized tests and test factories. |
| `yaml-tests.jsonl` | QLever's original YAML queries, assertion/check definitions, suite settings and existing fixture/adaptation references. |
| `unregistered-query-files.jsonl` | Query/update files not referenced by an indexed manifest or existing case; these may be tests, examples or benchmarks. |
| `summary.json` | Exact source-record counts and explicit completeness/execution boundaries. |
| `index-errors.json` | Any indexing errors. The two known failures are malformed vocabulary-definition files; their original bytes are retained. |

Manifest descriptors are a navigational projection. The immutable original
manifest/source file remains authoritative, particularly for shared blank-node
identity in descriptor graphs and engine-specific runner semantics. Queries and
assertions are never regenerated from RDF4J's actual output. Literal lexical
forms are not normalized during indexing.

## Collection is not execution

The existing corpus contains **6,022 RDF4J case definitions**, of which 5,743 are
marked ready and 279 blocked. Those are pre-existing catalogue statuses, not new
passing-test counts. The collection adds comprehensive source-level indexing; it
does not turn every engine-specific internal assertion into an equivalent
`RepositoryConnection` test. Source declarations are not runtime test counts:
parameterization, test factories, inheritance and runner settings may expand or
suppress them. JUnit 3 naming candidates can include helpers requiring review.

The original execution reports under `../reports` are historical results from
2026-10-05. `collection-python-tests.log` is the new collector/importer regression
run. No new RDF4J query execution is asserted by this collection update.

## Reproduce

From the `sparql-corpus` directory in the full distribution:

```sh
python3 -m pip install -r requirements.txt
python3 -m unittest discover -s tools/tests -v
python3 tools/collect_all_tests.py
```

The full ZIP already contains the vendor trees, archive members and original
corpus. A source-only Git checkout can reconstruct the original collection with
`python3 tools/rebuild.py --download` using the existing JDK 25/Maven/Jena capture
requirements. Then run the collector above. The GitHub collection workflow uses
the previously verified source artifact when available, with source rebuilding
as an explicit fallback.

Run existing executable cases with JDK 25 and Maven:

```sh
mvn test
mvn test -Dsuite.filter=qlever-yaml
```

A failed assertion must be investigated; it is never relabeled as a collection
failure or silently rewritten to make the suite pass.

## Verification

The collector checks every regular Git file against its pinned Git blob hash,
then compares every expanded archive member to the actual member bytes in its
pinned archive. Missing files, content mismatches, uncollected Gitlinks/symlinks,
and non-vocabulary manifest indexing errors fail the collection. All files remain
included even when their test declarations or runtime prerequisites cannot be
fully interpreted.

The packaging step checks ZIP CRCs and every archived file's SHA-256, restores
upstream executable modes in the ZIP, and applies the included Git patch to its
stated baseline before comparing the changed file bytes. No compiled targets,
caches or font assets are distributed.
