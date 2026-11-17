# Binary query writer: second optimization round

This round extends the already optimized writer published at **741b9389538e80214075692439a8cd25a8a9ae84**. It addresses three remaining costs: separate stream calls for fields of short records, temporary arrays proportional to large strings, and the eager scratch allocation paid by every new writer. The comparison baseline is that published implementation. The original upstream main revision, 5434e51194e6c2e1aa745e5d65a4c998918d6073, remains historical context in the [first-round report](../binary-writer-evidence/REPORT.md).

The new implementation stages complete short records, grows scratch storage with demand, and encodes ordinary large strings in bounded chunks after strict validation. Shared core-datatype templates, per-document namespace records, previous-column snapshots, and NULL/REPEAT batching remain from the published baseline. LMDB literal equality is unchanged.

## Final matched measurements

All figures compare **published 741b938 → this production source**, using JMH 1.37 on Temurin 25.0.5+8. Time reductions below are point estimates; a dagger marks overlapping 99.9% confidence intervals reported by JMH. The complete [result summary](final-results.md) retains confidence errors and every fork mean, with [machine-readable results](final-results.json) and individual raw JMH files.

### Large documents through the heartbeat wrapper

Each operation writes 4,096 rows; time and allocation below are already normalized **per row**. Fixtures contain eight columns except the 32-column case. Long strings have different encoded sizes, so compare implementations within a row of this table.

| Workload | Before, µs/row | After, µs/row | Time reduction | Allocation, B/row before → after |
| --- | ---: | ---: | ---: | ---: |
| [Mixed ASCII, eight columns](final-ascii.json) | 2.150 | 1.210 | 43.7% | 361.39 → 360.40 |
| [Mixed Unicode, eight columns](final-unicode.json) | 2.245 | 1.224 | 45.5% | 409.39 → 408.40 |
| [Identity repeats](final-repeat_identity.json) † | 0.308 | 0.286 | 6.9% | 1.32 → 0.33 |
| [Equal LMDB literals](final-repeat_equal_lmdb.json) | 0.388 | 0.333 | 14.1% | 1.33 → 0.33 |
| [Distinct LMDB values](final-unique_lmdb.json) | 2.204 | 1.185 | 46.2% | 1.34 → 0.34 |
| [Mixed rows, 32 columns](final-wide.json) | 10.095 | 5.817 | 42.4% | 1441.59 → 1440.50 |
| [1 KiB ASCII label](final-medium.json) | 5.313 | 2.829 | 46.8% | 361.42 → 360.67 |
| [Medium mixed Unicode label](final-medium_unicode.json) † | 4.846 | 3.792 | 21.8% | 409.41 → 409.51 |
| [8 KiB ASCII label](final-long.json) | 31.135 | 22.958 | 26.3% | 9505.59 → 361.54 |
| [Long mixed Unicode label](final-long_unicode.json) | 58.540 | 41.692 | 28.8% | 29889.74 → 409.66 |
| [Long Latin-1 label](final-long_latin1.json) | 46.188 | 33.841 | 26.7% | 27601.67 → 361.62 |
| [Long CJK label](final-long_cjk.json) | 70.107 | 32.602 | 53.5% | 63721.83 → 361.61 |
| [Long emoji label](final-long_emoji.json) | 59.435 | 33.324 | 43.9% | 27601.76 → 361.61 |
| [Long sparse-Unicode label](final-long_sparse.json) † | 32.628 | 31.475 | 3.5% | 9505.59 → 361.60 |
| [Native LMDB query plus writing](final-query.json) † | 8.912 | 7.713 | 13.5% | 7049.14 → 7451.20 |

The strongest timing results are the short/mixed rows, wide rows, and most long-string distributions. Identity repeats and medium Unicode remain timing-inconclusive. Sparse Unicode is strongly bimodal in both implementations: published fork means 42.80/22.46 µs and current 19.73/43.22 µs. Its allocation reduction is clear; a timing improvement is not established.

**The full-query result is also inconclusive for time and has an allocation tradeoff in this primary run:** 8.912±1.404 µs becomes 7.713±1.493 µs, while Java heap allocation rises 7,049→7,451 B/row, approximately 5.7%. A separate, prespecified longer reversed-order check is retained below; it does not replace this result.

The [longer reversed-order query check](query-allocation-recheck.json) used ten one-second warmups and five two-second measurements, with two forks per implementation. It measured published **8.412 ± 0.837 µs/row** and current **8.215 ± 1.475 µs/row**, again overlapping. Allocation changed direction: **7,546 ± 152 B/row → 7,132 ± 314 B/row**. Thus the primary allocation increase did not reproduce in direction. Both runs are retained; neither a stable whole-query time improvement nor a stable allocation change is established. Their different protocols are not pooled into a favorable single number. The [allocation investigation](query-allocation-analysis.md) separates this GC-profiler observation from JFR sampling artifacts.

### Small complete documents, memory sink

| Rows/document | Before, µs/document | After, µs/document | Time reduction | Allocation, B/document before → after |
| --- | ---: | ---: | ---: | ---: |
| 0 rows | 1.396 | 0.693 | 50.4% | 5000.01 → 936.00 |
| 1 row | 3.253 | 1.825 | 43.9% | 5792.02 → 1728.01 |
| 8 rows | 12.708 | 8.172 | 35.7% | 8232.09 → 4192.06 |

These measurements include a fresh writer and complete document framing. The empty-result allocation difference is 4,064 bytes/document in this JVM. It is not a claim about cold class initialization or every application’s object layout.

### Direct-memory control for large documents

| Workload | Before, µs/row | After, µs/row | Allocation, B/row before → after |
| --- | ---: | ---: | ---: |
| Mixed ASCII, eight columns † | 1.744 | 0.970 | 361.36 → 360.36 |
| Long mixed Unicode label | 57.450 | 36.905 | 29889.72 → 409.60 |

The long-Unicode control improves without the heartbeat wrapper. The ASCII control has a lower mean but overlapping confidence intervals because the published forks vary. Full uncertainty and raw samples remain in [final-results.md](final-results.md) and [final-memory.json](final-memory.json).

## UTF-8 remains part of the binary format

Binary framing does not remove the need for a defined representation of text. This format carries string byte counts followed by ordinary UTF-8, and the parser expects that representation. Java's UTF-16 code-unit count is not generally the encoded byte count. NUL occupies one UTF-8 byte; supplementary characters occupy four bytes and consume two Java code units. Changing to raw Java characters would require a protocol change and a matching parser. The implementation preserves the existing version, markers, big-endian fields, and UTF-8 bytes. See the [format constants](../core/queryresultio/binary/src/main/java/org/eclipse/rdf4j/query/resultio/binary/BinaryQueryResultConstants.java) and [parser](../core/queryresultio/binary/src/main/java/org/eclipse/rdf4j/query/resultio/binary/BinaryQueryResultParser.java).

The opportunity is to perform that conversion with less surrounding work. ASCII can be copied directly because its character values equal its UTF-8 bytes. Non-ASCII text still requires encoding, and malformed surrogate sequences must still be rejected.

## Concrete implementation changes

**Complete short records use one bulk write where staging permits.** QName and namespace records combine their marker, namespace ID, string length, and payload. Blank nodes combine their marker and identifier. Typed literals combine their label with the datatype QName; language literals combine their label and language/direction tag. Namespace declarations still precede any literal that references them. The cached QName bytes of a canonical core datatype can be appended directly to a staged literal.

This reduces entry into the actual heartbeat payload wrapper for individual fields. It introduces no persistent output stream buffer: a completed callback has sent its output to the supplied stream. The implementation can fall back to existing field framing when an entire record does not fit its bounded staging path.

**Scratch storage starts small and grows only when needed.** The published writer allocated 4,100 bytes immediately. The new writer starts with 132 bytes and grows up to the same 4,100-byte ceiling. Growth preserves previously staged fields, the ASCII prefix already copied, and space reserved for a following datatype or language tag. A small document therefore need not acquire the maximum scratch array merely to write its column names.

The short encoder continues from its ASCII prefix when it encounters non-ASCII text. Its capacity calculation applies the conservative UTF-8 bound to the remaining suffix. This avoids rescanning and rewriting the known ASCII prefix while keeping capacity arithmetic explicit.

**Ordinary large strings use counted, bounded encoding.** A first pass validates the complete immutable String and computes its exact UTF-8 byte length. Only after that pass succeeds does the writer publish the string length and encode the payload through 4,096-byte chunks. A counted ASCII string takes a direct copy path. The general chunk loop handles complete supplementary pairs within its output budget and relies on the preceding validation.

This removes the output-sized intermediate byte array from the ordinary large-string path. It also removes the eager CharsetEncoder from each writer. The costs are a counting pass and more output calls than one full-size payload write; those costs must be assessed across character distributions and sinks. The [implementation notes](implementation-notes.md) explain the helpers and maintenance invariants.

## Why counted chunks instead of validated getBytes

Surrogate validation followed by String.getBytes(UTF_8) is a plausible alternative. It reaches the JDK's specialized String encoding paths and avoids generic CharBuffer/ByteBuffer machinery. Validation is essential: calling getBytes directly permits replacement of malformed input, changing the writer's contract.

The alternative still allocates an output-sized array. The inspected JDK 25 paths can also overallocate and trim, depending on whether the String contains ASCII, Latin-1, supplementary pairs, or sparse wider characters. A result from one long ASCII fixture cannot establish its behavior for all of those cases. The [large-string analysis](large-string-analysis.md) documents the JDK paths, public API constraints, and comparisons with Protocol Buffers and Avro; those projects provide design references rather than transferable RDF4J performance results.

The selected chunked design makes ordinary large-string temporary storage independent of payload length. Its counting pass also supplies the byte length required by the wire format and validates the entire input before string framing is emitted. Exploratory alternatives informed the design, but only the final matched measurements above establish its observed timing and allocation tradeoffs.

## Core datatypes and generic values

The published shared templates for the XSD, RDF, and GEO core vocabularies remain in use. Templates are created once; a writer clones a used datatype record and inserts its document-local namespace ID. Those records are reset for each independent document. Language and directed-language literals retain their dedicated record representation.

The cache requires both a recognized core enum and the canonical datatype IRI object. This preserves an unusual namespace/local-name split supplied by another IRI implementation. Custom datatypes continue through the generic path, which reads their current namespace and local name. That path now benefits from record staging too. There is no new unbounded cache for arbitrary literals or datatypes, and no shortcut that substitutes raw native IDs for LMDB equality.

## Correctness and integration evidence

The required full root clean install completed successfully before the final verification. The separate binary and focused server selections also completed successfully:

| Validation | Observed result | Evidence |
| --- | --- | --- |
| Full root build with quick profile | BUILD SUCCESS; test execution checked separately | [Root install log](final-root-install.log) |
| Binary module | 267 tests, no failures or errors; includes 204 encoding cases | [Binary verify log](final-binary-tests.log) |
| Focused server selection | 2 tests, no failures or errors | [Server verify log](final-server-tests.log) |
| Large-document smoke | 120 configurations passed complete semantic roundtrip | [Large smoke log](final-production-smoke.log) |
| Small-document smoke | 12 configurations passed roundtrip and cross-configuration byte checks | [Small smoke log](final-production-small-smoke.log) |

The encoding suite independently constructs expected wire bytes. It covers ASCII/NUL, Latin-1, BMP and supplementary text, string and complete-record boundaries, malformed surrogates, cached and generic datatypes, namespace order, repeats, nulls, empty rows, nested triples, language direction, and output/flush failures. Three added growth cases specifically verify that a staged label survives buffer expansion while adding an ASCII datatype suffix, a Unicode datatype suffix, or a language tag.

Malformed input retains MalformedInputException with input length one and the existing callback exception wrapping. Tests also check valid output after a rejected error string, completed-row visibility, and final flush without closing the supplied stream. The server selection covers terminal timeout and circuit-breaker errors after rows have already been sent.

The large smoke matrix spans both implementations, fifteen workloads, two sinks, and stable/churning namespaces. It validates every synthetic row and every expected real-query combination. Small smoke covers zero, one, and eight rows with both sinks; its three reference documents anchor nine exact byte comparisons. Neither smoke replaces the independent wire-format assertions.

## Measurement design and limits

Final large measurements run sequentially, one workload per JMH invocation, pairing published and current before moving to the next workload. Requested implementation order alternates between workloads. Each combination uses two forks, five one-second warmup iterations, five one-second measurement iterations, and the GC profiler. The runner checks compiled source hashes before and after comparisons. [run_final.py](harness/run_final.py) and [REPRODUCING.md](REPRODUCING.md) define the complete procedure.

Synthetic inputs are preconstructed. Every invocation creates a fresh writer and emits actual bytes into a reusable, pre-grown ByteArrayOutputStream; the benchmark consumes its backing array, sampled bytes, and size. Constructor lookup occurs outside timing, while reflective construction remains once per document. Heartbeat cases use the real synchronized wrapper without a heartbeat thread or network.

The 4,096-row harness reports **time and allocated bytes per row**. The zero/one/eight-row harness reports **per document**, exposing fixed construction and scratch costs. The final small mode uses memory output; separate memory controls help assess wrapper overhead. The real-query fixture measures query evaluation and serialization together. Synthetic LMDB literals isolate equality behavior and do not measure database access.

The [methodology review](methodology-review.md) records the different UTF-16 and UTF-8 sizes of the Unicode fixtures. These are not equal-byte workloads. Warmed local-store results and two forks on a shared host cannot establish a universal production speedup. Retain uncertainty, fork variation, and inconclusive comparisons; do not infer end-to-end server gains from serializer-only cases.

## Profile attribution

Six separate restricted JFR recordings cover ASCII, long mixed Unicode, and the real query for both implementations. Each actual event inventory was inspected: only the six configured CPU/allocation/GC/load event types and JFR structural metadata/checkpoints are present. The [grouped CPU counts](profile-groups.json) count a group at most once per selected stack, including overloaded heartbeat writes without double-counting.

| Selected execution-sample attribution | Published share | Current share | Published/current sample denominators |
| --- | ---: | ---: | ---: |
| ASCII: heartbeat stream write methods | 58.20% | 30.20% | 945 / 937 |
| Long Unicode: CharsetEncoder.encode call path | 90.67% | 0% | 997 / 981 |
| Real query: writer handleSolutionImpl | 14.29% | 6.97% | 868 / 846 |
| Real query: RepositoryValueIteration.prepareNext | 77.07% | 84.40% | 868 / 846 |

The current long-Unicode profile spends 35.17% of selected execution samples in the strict UTF-8 length/validation pass. Encoding remains necessary; the general encoder machinery has been replaced by the counted chunk path. The query iteration frame includes underlying query work and must not be described as pure decoding. These are inclusive fractions of selected Java execution samples from different runs, not wall-clock fractions or additive buckets. JFR-instrumented timings are excluded from the performance tables.

**Allocation-profile correction:** recording starts after warmup, and the first allocation sample on a thread can inherit the thread's earlier cumulative allocation weight. This was verified against the exact JDK source and observed as single samples carrying 26–73% of some raw distributions. The [allocation investigation](query-allocation-analysis.md) documents the pinned sources, omissions, and separate `*.steady-summary` sensitivity reports. Raw profiles and unadjusted summaries remain intact; only the sensitivity reports omit the first allocation sample per thread, and every CPU report remains exactly unchanged.

The correction is approximate because the omitted event also includes some valid post-start allocation. Use JMH's GC profiler for absolute B/row. The earlier round's per-class allocation percentages from this same recording method have the same caveat; its measured timings, JMH allocation figures, and CPU sample results are unaffected. In the corrected long-Unicode comparison, the old large encoding-buffer allocation path remains visible, while the current method's apparent dominant allocation site disappears because it came entirely from the first sample. This supports the code-level explanation but is not proof of zero allocation.

## Compatibility limits and reviewed source

Successful output preserves the wire representation, but an incomplete failed record need not reach the target in the identical sequence or byte prefix. Staging can postpone its first write, and chunking changes where a target observes a payload failure. Some datatype or language fields are obtained earlier. No retry or recovery guarantee is added; previously emitted fields may remain after failure.

For Strings longer than Integer.MAX_VALUE / 3 UTF-16 units, a local strict CharsetEncoder retains the allocating fallback rather than risking overflow in the custom count. Those extreme inputs are outside these tests and measurements. The 4,100-byte ceiling describes retained encoding scratch, not total writer memory or all possible temporary allocation: namespace state, column state, datatype records, growth copies, and that extreme fallback have separate costs.

The reviewed production file has SHA-256 **4a05a2794ff59c05b52cf0c7f70cacedc1a5602d080f1c5a55bae8599c455799**. The [source identity](final-source-identity.json), [composition review](combined-review.md), and [candidate equivalence review](candidate-equivalence-review.md) provide the audit trail. The change adds no public API, dependency, parser revision, LMDB equality change, or HTTP-wrapper modification.

## Published source and downloads

The source is committed and pushed as [c03171474130c67ca2eed2b78e3cc82cd4fba345](https://github.com/HASMAC-AS/rdf4j/commit/c03171474130c67ca2eed2b78e3cc82cd4fba345), extending branch [GH-0000-binary-query-writer-performance](https://github.com/HASMAC-AS/rdf4j/tree/GH-0000-binary-query-writer-performance). It updates the already open [PR #6440](https://github.com/eclipse-rdf4j/rdf4j/pull/6440).

- [Complete changed Java source](source-package/binary-writer-complete-source.zip): the full writer and both complete regression-test files, plus a manifest.
- [Incremental diff-only patch](source-package/binary-writer-incremental.patch): apply to the previously published 741b938 commit.
- [Cumulative diff-only patch](source-package/binary-writer-cumulative.patch): both optimization rounds relative to original main at 5434e511.
- [Complete round-two evidence bundle](rdf4j-binary-writer-round2-evidence.zip): reports, candidate sources, harness, commands, measurements, validation logs, inspected JFR recordings and both raw/sensitivity summaries.

The [package verification record](source-package-verification.json) records all source and artifact hashes. Both patches were applied in separate temporary indexes and reproduced the published source tree `32efd9174b509fd46a0c0c63168eaff03c69dd43`. ZIP CRCs and every archived source byte were verified against the published commit. The [publication record](source-publication.json) links the local and remote tree identity.
