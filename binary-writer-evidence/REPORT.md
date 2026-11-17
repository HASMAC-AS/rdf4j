# RDF4J binary query result writer: profiling and optimization

Date: 17 November 2026. Baseline: upstream main at [5434e51194e6c2e1aa745e5d65a4c998918d6073](https://github.com/eclipse-rdf4j/rdf4j/commit/5434e51194e6c2e1aa745e5d65a4c998918d6073). Development branch: **GH-0000-binary-query-writer-performance** in **HASMAC-AS/rdf4j**.

Published implementation: [741b9389538e80214075692439a8cd25a8a9ae84](https://github.com/HASMAC-AS/rdf4j/commit/741b9389538e80214075692439a8cd25a8a9ae84).

## Result

The implemented writer pre-encodes **all 52 built-in core datatype QName templates once per class loader**, shares those immutable templates between writers, and patches document-specific namespace IDs into private copies. Combined with bounded string encoding, direct previous-column access, and fewer stream writes, the measured short-string and repeat-heavy serializer workloads improved **1.88–2.44×** through RDF4J's actual heartbeat output wrapper. The eight-column mixed ASCII case decreased from **3.697 to 1.971 µs per row**, with allocation decreasing from **2,256.7 to 361.4 bytes per row**. [Final heartbeat measurements](final-heartbeat.json)

The result has two important practical limits. An 8,192-character-literal workload showed essentially unchanged timing, because large strings retain the strict encoder fallback. The complete native LMDB query benchmark includes evaluation and materialization as well as writing; its first comparison was inconclusive, and one longer-warmup recheck showed a favorable mean with overlapping 99.9% confidence intervals. These measurements establish a substantial serializer improvement, while the query-wide speedup remains less certain.

The production change is confined to [BinaryQueryResultWriter.java](https://github.com/HASMAC-AS/rdf4j/blob/741b9389538e80214075692439a8cd25a8a9ae84/core/queryresultio/binary/src/main/java/org/eclipse/rdf4j/query/resultio/binary/BinaryQueryResultWriter.java), with two accompanying test classes. LMDB equality, storage, and query evaluation code are unchanged. Validation completed with **197 binary-module tests, two server result-view tests, and 64 output round-trip configurations passing**.

## Serializer measurements

Each benchmark invocation emits a complete **4,096-row document**. JMH's @OperationsPerInvocation(4096) divides both reported time and normalized allocation by 4,096: the numbers below are **per row**, including the amortized cost of constructing the writer and, where selected, the heartbeat wrapper. They are not per complete query or per value.

The primary matrix uses stable namespaces and the actual QueryResponseHeartbeat output stream backed by a reusable memory buffer. “±” is JMH's reported 99.9% confidence-interval half-width. Ratios divide baseline mean time by current mean time; they are descriptive ratios of these samples.

| Workload | Baseline, µs/row | Current, µs/row | Mean speedup | Baseline allocation, B/row | Current allocation, B/row |
| --- | ---: | ---: | ---: | ---: | ---: |
| Mixed ASCII, 8 columns | 3.697 ± 0.175 | 1.971 ± 0.106 | **1.88×** | 2,256.72 | 361.39 |
| Mixed Unicode, 8 columns | 4.084 ± 0.289 | 2.169 ± 0.239 | **1.88×** | 2,658.86 | 409.39 |
| Same LMDB literal objects repeated | 0.757 ± 0.071 | 0.328 ± 0.061 | **2.31×** | 72.56 | 1.325 |
| Distinct, equal LMDB literal objects | 0.808 ± 0.054 | 0.353 ± 0.026 | **2.29×** | 72.56–128.56¹ | 1.325 |
| Unique initialized LMDB literals | 4.286 ± 0.366 | 1.757 ± 0.133 | **2.44×** | 2,304.58 | 1.334 |
| Mixed ASCII, 32 columns | 17.527 ± 1.762 | 9.072 ± 1.137 | **1.93×** | 9,025.77 | 1,441.58 |
| One 8,192-character literal per row | 23.544 ± 1.895 | 23.737 ± 2.042 | **0.99×; no resolved timing change** | 11,248.85 | 9,505.54 |

¹ The equal-literal baseline produced approximately **128.56 B/row in one fork and 72.56 B/row in the other**. Its aggregate is 100.56 ± 44.62 B/row. Reporting the fork range exposes that allocation variability. The current approximately 1.3 B/row includes amortized per-document allocation; it does not mean that a newly constructed writer allocates only 1.3 bytes. [Raw measurements and per-fork samples](final-heartbeat.json)

Mixed ASCII and Unicode allocation decreased by about 84–85%; the wide case decreased by about 84%. The initialized-literal fixtures isolate writer work particularly well because their values already exist. The long-string case still saves allocation in datatype and other short-field handling, about 15.5% overall, but it provides no evidence of a timing improvement. Its confidence intervals overlap substantially.

Two controls use the same memory destination without the heartbeat wrapper:

| Workload | Baseline, µs/row | Current, µs/row | Mean ratio | Allocation, baseline → current |
| --- | ---: | ---: | ---: | ---: |
| Mixed ASCII | 2.264 ± 0.243 | 1.321 ± 0.189 | 1.71× | 2,256.67 → 361.36 B/row |
| Distinct, equal LMDB literals | 0.986 ± 0.521 | 0.319 ± 0.037 | 3.09× | 72.53–128.53 → 1.293 B/row |

The second control's baseline timing is noisy, so its 3.09× mean ratio is not the headline claim. The ASCII control supports a benefit beyond the heartbeat wrapper, while the primary matrix measures the cost of the real synchronized payload path. Neither sink includes a network socket, servlet container, or slow reader. [Memory-sink measurements](final-memory.json)

## Complete native LMDB query

The query fixture contains 512 subjects, six single-valued properties per subject, and eight values of a seventh property. Its native query produces 4,096 rows. Loading and query preparation happen during setup; the measured operation includes evaluation, result materialization, serialization, and result closure. Every invocation checks the result count.

| Run | Baseline, µs/row | Current, µs/row | Baseline 99.9% interval | Current 99.9% interval |
| --- | ---: | ---: | ---: | ---: |
| Original matrix: 5 warmup, 5 measurement iterations | 9.172 ± 0.917 | 10.036 ± 7.635 | 8.255–10.090 | 2.401–17.670 |
| One recheck: 10 warmup, 8 measurement iterations | 8.681 ± 0.826 | 7.881 ± 0.631 | 7.855–9.507 | 7.250–8.513 |

Both runs used two forks and one-second iterations. The original current-writer run contained large timing excursions and cannot establish a query speedup or regression. One follow-up increased warmup and measurement duration and reversed the implementation order to current first, then baseline. Its mean decreased **9.2%**, approximately 8.68 → 7.88 µs/row, but the 99.9% intervals still overlap. Both runs remain part of the evidence; the recheck supports a promising direction rather than a firm query-wide percentage. [Original query samples](final-heartbeat.json), [recheck samples](final-query-recheck.json), [recheck command](final-query-recheck-command.json)

Query allocation decreased in both comparisons: 7,834 → 7,091 B/row originally, and 8,206 → 7,133 B/row in the recheck. Per-fork levels vary. Evaluation and materialization continue to dominate the allocation budget relative to the initialized-literal serializer fixtures. The large serializer gains therefore should not be applied directly to total query time.

## Why these changes help

### Pre-encode the fixed datatype vocabulary

The writer previously revisited a datatype's namespace and local name and encoded that local name for each emitted typed literal. The vocabulary is finite: 46 XSD, five RDF, and one GEO datatype. The new static table contains the QName marker, local-name byte length, and UTF-8 local-name bytes for every entry. The two RDF language datatypes still use their required language-record path when writing literals.

A namespace ID belongs to a document, because its value depends on the order in which namespaces appear. On first use, the writer declares or resolves that namespace, clones the shared template, and fills its four namespace-ID bytes. Subsequent literals emit that writer-local record directly. Enum ordinals select internal array entries; no ordinal appears on the wire.

The optimization requires the actual datatype IRI to be the canonical instance returned by the core datatype. This preserves exact bytes for an equal IRI with an unusual namespace/local-name split. Custom datatypes and noncanonical IRI instances use the generic writer path. Shared templates are never mutated, and writer-local records reset with the namespace table between documents. [Implementation details](technical-analysis.md)

### UTF-8 remains part of the binary protocol

RDF4J's binary query result protocol defines strings as ordinary UTF-8 preceded by a four-byte big-endian **byte length**. Binary record markers do not remove the need to encode Unicode text. Writing Java UTF-16 characters, Latin-1 bytes, modified UTF-8, or LMDB numeric IDs would change the protocol. [Pinned format definition](https://github.com/eclipse-rdf4j/rdf4j/blob/5434e51194e6c2e1aa745e5d65a4c998918d6073/core/queryresultio/binary/src/main/java/org/eclipse/rdf4j/query/resultio/binary/BinaryQueryResultConstants.java)

Changing strings now use a reusable **4,100-byte array per writer**: four bytes for the length and 4,096 for the payload. ASCII strings up to 4,096 characters copy directly. Other strings up to 1,365 UTF-16 code units use a strict UTF-8 loop; three bytes per code unit is a safe upper bound, including surrogate pairs. The writer emits the prefix and encoded payload together, reducing stream calls.

Larger strings retain the existing strict CharsetEncoder. Malformed surrogates still throw instead of being silently replaced, including after a previous malformed-input failure. The bound applies to retained scratch storage; the large-string fallback still allocates encoding buffers. Arbitrary literal labels are not retained in a new cache. [Encoding and memory tradeoffs](technical-analysis.md)

### Avoid redundant equality and small writes

An indexed array stores the values observed in the previous row. This removes a previous-row lookup by binding name and permits an identity check before semantic equality. Distinct objects still invoke their ordinary equals implementation. In particular, the patch does not replace LMDB equality with unchecked internal-ID comparison.

Adjacent NULL and REPEAT markers are emitted in groups, before the next full value or before returning from the row callback. Short-string frames and cached QName records similarly combine bytes into fewer calls. That matters because the heartbeat payload wrapper acquires its output lock and checks state on each write, even when binary output does not schedule heartbeat whitespace. Completed-row bytes remain visible before endQueryResult; the patch adds no persistent output buffer. [Stream lifecycle analysis](buffering-lifecycle.md)

Two related state defects are fixed. Retaining a caller's mutable BindingSet could cause a changed next row to be mistaken for a repeat. Retaining the namespace table across documents could omit namespace declarations in the second document. Both were reproduced as failing tests before the production change; the previous-value snapshot and coordinated document reset correct them. [Pre-change failures](state-before.log)

## What the profiles show

Separate final JFR runs used the stable-namespace fixtures. Percentages below are **inclusive shares of selected CPU stacks**: parent and child methods overlap, so the shares are not additive. Profiling-run timings are excluded from the performance tables.

| Workload and method | Baseline share | Current share |
| --- | ---: | ---: |
| Mixed ASCII: writeString | 56.75% | 33.13% |
| Mixed ASCII: writeQName | 42.98% | 24.33% |
| Equal LMDB repeats: writeRepeat → writeMarkers | 45.42% | 18.54% |
| Equal LMDB repeats: literal equality | 4.31% | 11.79% |
| Native query: writer handleSolutionImpl | 20.12% | 10.63% |
| Native query: RepositoryValueIteration.prepareNext | 71.48% | 77.63% |

The ASCII recordings each contain 1,126 selected CPU samples. In the baseline, HeapByteBuffer accounts for **33.91%** of selected sampled allocation weight and StringCharBuffer **13.73%**. Neither class appears in the current ASCII sampled-allocation list, consistent with removing generic encoding buffers from the common path. Absence from samples is not a guarantee of zero allocations; large-string fallback remains. [Baseline ASCII profile](jfr-final/baseline-ascii-heartbeat-pid71-2026-11-17T11_57_28.358697780Z-5472984616024138580.events.summary.md), [current ASCII profile](jfr-final/current-ascii-heartbeat-pid175-2026-11-17T11_58_25.949050532Z-12983475304324440210.events.summary.md)

Repeat-marker batching reduces the relative stream-writing burden. Equality occupies a larger share after other work becomes cheaper; these relative percentages do not establish that equality became slower, and its implementation is unchanged. [Baseline repeat profile](jfr-final/baseline-repeat_equal_lmdb-heartbeat-pid102-2026-11-17T11_57_46.779780822Z-2005400205080308205.events.summary.md), [current repeat profile](jfr-final/current-repeat_equal_lmdb-heartbeat-pid206-2026-11-17T11_58_44.001663192Z-1943548925326117293.events.summary.md)

The query recordings contain 1,024 baseline and 997 current selected CPU samples. They show a smaller writer share and a larger share below prepareNext, which includes underlying query evaluation as well as result preparation. It must not be interpreted as pure value decoding. This supports investigating that execution path next, while the separate JMH query measurements determine the strength of the timing conclusion. [Baseline query profile](jfr-final/baseline-query-heartbeat-pid133-2026-11-17T11_58_07.586007344Z-12342234900223714368.events.summary.md), [current query profile](jfr-final/current-query-heartbeat-pid237-2026-11-17T11_59_05.560043231Z-6554565147640402022.events.summary.md)

## Method and limits

Measurements used **JMH 1.37, OpenJDK 25.0.5+8, one benchmark thread, two forks, a 512 MiB initial heap, a 1 GiB maximum heap, and ActiveProcessorCount=2**. The primary and memory matrices used five one-second warmup iterations and five one-second measurement iterations, with the GC profiler. JFR profiling is a separate activity. Exact commands and raw samples are retained alongside this report.

Synthetic rows and values are constructed during setup. A full preliminary write grows the reusable byte-array destination before timing, including the long-literal fixture. Measured invocations still write real bytes, and the benchmark consumes the resulting buffer, sampled bytes, and size. Writer construction occurs once per document; its scratch and reference arrays are amortized over 4,096 rows. Empty results and very small documents were not measured.

The synthetic LMDB workloads use initialized literals and a shared canonical revision with consistent lexical values and IDs. They exercise model equality without database access. The separate query workload is the database measurement. Its smoke validation checks every expected subject/value combination and every returned term.

The original screening fixture unintentionally generated **4,115 namespaces per mixed document** because its URNs split at the final colon. The corrected final fixture uses **four stable namespaces**. Earlier screening and profile artifacts remain explicitly labeled namespace-churn evidence; their timings are not substituted for the final stable-namespace matrix. Both layouts are covered by output validation. [Independent methodology review](methodology-review.md)

This is a shared execution host, not an isolated performance laboratory. Two forks provide useful evidence but leave sensitivity to JVM compilation, allocation optimization, and host activity. The visible equal-literal allocation split and query timing excursions illustrate those limits. Source inspection explains plausible mechanisms; controlled comparisons quantify the combined patch. The measurements do not assign a precise independent percentage to each individual optimization or establish HTTP throughput under backpressure.

## Validation and deliverables

| Validation | Observed result | Evidence |
| --- | --- | --- |
| Binary query-result module | **197 tests; zero failures, errors, or skips** | [Final binary test log](final-binary-tests.log) |
| Server TupleQueryResultViewTest | **2 tests; zero failures, errors, or skips** | [Final server test log](final-server-tests.log) |
| Output smoke matrix | **64 passing configurations**: two implementations × eight workloads × two sinks × two namespace layouts | [Smoke log](smoke.log) |
| Mutable-row and document-reuse regressions | Both failed before the change and pass in the final module run | [Pre-change evidence](state-before.log) |
| Independent implementation review | No actionable correctness findings in reviewed source | [Review](implementation-review.md) |
| Download artifacts | ZIP CRC/source-content checks and reverse patch check recorded as passing | [Artifact checks](artifacts/source-artifact-checks.json) |

The encoding suite compares complete streams with an independent DataOutputStream reference encoder. It covers Unicode boundaries and malformed input, exact QName splits, mutable custom datatype IRIs, all 50 non-language built-in datatype templates, language directions, nested triple terms, interleaved writers, changing namespace order, and output visibility before final flush. Parser round trips supplement the byte comparisons.

Download [rdf4j-binary-writer-complete-source.zip](artifacts/rdf4j-binary-writer-complete-source.zip) for the **complete contents of the changed production file and both test files**, plus its README. Download [rdf4j-binary-writer.patch](artifacts/rdf4j-binary-writer.patch) for their diff against the pinned baseline. The ZIP overlays those three complete source files onto the pinned RDF4J checkout. Full repository context is available on the published branch.

The complete [benchmark and profile evidence bundle](artifacts/rdf4j-binary-writer-evidence.zip) includes this report, the harness sources, raw final JFR recordings, measurement JSON and logs, and verification records.

Reproduction entry points are [the benchmark source](harness/src/org/eclipse/rdf4j/query/resultio/binary/benchmark/BinaryWriterBenchmark.java), [bench.py](harness/bench.py), and the recorded [heartbeat](final-heartbeat-command.json), [memory](final-memory-command.json), and [query-recheck](final-query-recheck-command.json) commands. Their captured classpaths use this checkout's absolute paths; regenerate the dependency classpath for a different checkout and use JDK 25 or later.

The next substantial opportunity is avoiding LMDB's decode-to-String/re-encode sequence where lexical UTF-8 bytes already exist. It requires preserving those bytes through materialization with correct revision ownership and fallback for computed, inline, or mutated values. Re-reading the dictionary in the current generic writer would add a lookup after decoding has already occurred. That representation change deserves its own benchmark and lifetime design; the present patch provides a measured writer improvement without introducing it. [Further technical analysis](technical-analysis.md)
