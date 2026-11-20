# Compact indexes for lowercase literal filters

LMDB maintains a compact summary for each newly stored, non-inline literal. The query evaluator probes this summary before decoding the literal for supported positive filters:

```sparql
FILTER(CONTAINS(LCASE(?label), LCASE("Needle")))
FILTER(STRSTARTS(LCASE(?label), LCASE("Prefix")))
FILTER(LCASE(?label) = LCASE("Exact label"))
```

Equality also supports the operands in the opposite order. The needle can be a constant string literal with or without `LCASE`. The same shapes inside positive conjunctions can use the guard. A function must resolve to RDF4J's actual built-in implementation; registering a custom implementation under the same URI does not authorize this optimization.

The index is a necessary-condition filter. Every surviving candidate is evaluated by the original expression, including language-tag, datatype, base-direction, and error rules. Original value IDs and graph rows remain intact, so case variants, duplicate solution mappings, and named graph multiplicities are preserved.

## What is stored

The `literal_text` named database lives inside the existing value-dictionary LMDB environment. Its entry key is the dictionary's original literal ID. Each summary has a fixed 56-byte value:

| Field | Bytes | Purpose |
| --- | ---: | --- |
| Format marker, version, flags | 4 | Reject incompatible layouts conservatively |
| Lowercase UTF-8 length | 4 | Necessary length check |
| First lowercase UTF-8 bytes | 8 | Prefix pruning and equality check |
| Full lowercase label hash | 8 | Equality pruning |
| Byte-trigram Bloom signature | 32 | Substring and long-prefix pruning |

The label is decoded from the bytes actually persisted in the dictionary, lowercased as a complete Java string, and then encoded as UTF-8. This preserves context-sensitive casing and avoids treating an unencoded input label as authoritative. The signature covers all overlapping three-byte grams; it stores no positions, term frequencies, or original text. A saturated signature conservatively admits the literal. Hash or signature collisions only cause extra verification.

The 56-byte figure excludes the ID key and LMDB page/node overhead. For example, 100 million stored literal summaries have 5.6 GB of summary payload before that overhead. Inline strings do not get entries. The data structure is intentionally bounded per literal rather than proportional to the number of characters or grams.

Inspect the actual allocation through `store.getLmdbStats().valueDatabases().get("literal_text")`. The allocated page bytes are `pageSize * (branchPages + leafPages + overflowPages)`. Its entry count includes the small policy, transaction-marker, and any in-progress rebuild records in addition to literal summaries.

## Existing repositories

Opening a repository does not automatically backfill this index or add a dictionary scan for it. New writes maintain summaries immediately; older literals without summaries continue through ordinary evaluation. To populate existing literals, close all repository instances and call the public offline maintenance operation using the repository's usual LMDB configuration:

```java
long written = LmdbStore.rebuildLiteralTextIndexOffline(dataDirectory, config);
```

The operation holds the repository directory lock. It scans and commits bounded batches, rather than loading the entire dictionary into the Java heap. Its return value is the number of summaries written during that invocation. An interrupted build leaves queries correct because a missing entry is always unknown. Running the operation again resumes durable progress. The method requires the store to be closed; it is not an online query-time indexing operation.

Use the same `LmdbStoreConfig` that normally opens the repository. Maintenance uses the existing map-size and durability settings. Plan disk space for the new database, including LMDB copy-on-write overhead. The initial build must read and normalize each indexed label once; the savings apply to subsequent queries.

## Casing compatibility

This fork's ordinary and native `LCASE` evaluators use Java's default locale. The index preserves that behavior. Its metadata records the creation locale, summary version, Java runtime version, and runtime vendor. New writes use the persisted locale even if the process default locale later changes.

A query uses negative summary results only when the persisted policy, its prepared needle, and the current default locale agree. A Java runtime mismatch also disables pruning. Ordinary expression evaluation remains available in either case. After deliberately changing the repository's casing locale or Java runtime, run the offline rebuild under the intended runtime and locale. Rebuild invalidates incompatible summaries before replacing their policy, so partial work cannot authorize stale negative results.

No Unicode normalization, accent stripping, or Unicode case folding is added. Those operations have different semantics from SPARQL `LCASE`. Malformed surrogate needles bypass the index because Java's UTF-16 substring operations cannot safely be modeled by UTF-8 gram matching for those inputs.

## Transactions and maintenance

Dictionary payloads and summaries are written in the same LMDB transaction. Scalar insertion, batched insertion, prepared imports, and raw bulk append all participate. Mutation capacity estimates include the summary changes before dictionary writes begin. Rollback removes both; read-own-writes and query snapshots use the same admitted dictionary view.

Reverse-mapping retirement does not remove a summary while a retained ID can still resolve its original label. Physical payload reclamation removes the summary with the payload. Clear/reopen reinitializes the index handles and policy. Missing or incompatible metadata never proves a nonmatch.

A transaction marker records the last dictionary transaction maintained by this implementation. Startup verifies that marker before trusting existing summaries. If an older writer or external maintenance operation commits dictionary changes without updating it, the summaries become unknown until rebuilt. A later compatible commit cannot silently make those stale summaries authoritative again. Opening a repository with an older binary is also subject to that binary's named-database compatibility checks; do not assume every older release can read the added database.

## Query behavior and limits

This implementation filters IDs produced by the existing graph plan. It avoids materializing and lowercasing rejected labels, while preserving the graph engine's existing selectivity and row semantics. It does not replace a broad graph scan with a complete text-first posting-list enumeration. On a query touching every object, it still probes metadata for those objects.

`CONTAINS` needs at least three UTF-8 bytes for gram pruning. Shorter needles use ordinary evaluation. Empty prefixes also bypass the index. Very short or common search strings, signatures saturated by long diverse labels, and prefixes shared by many labels may admit many candidates. Long prefix queries combine the eight-byte prefix with the trigram signature, but the exact prefix still requires verification. An index cannot make a query with a very large true result set selective.

Computed haystacks such as `LCASE(STR(?value))`, variable needles, and unsupported expression shapes retain ordinary evaluation. The guard is applied at acceptance boundaries, not substituted into arbitrary truth expressions beneath `NOT`, `IF`, or `COALESCE`. Inline, synthetic, and uncovered IDs pass through conservatively.

The built-in LMDB native and standard strategy factories support the guard. An explicitly supplied third-party or stock `DefaultEvaluationStrategyFactory` keeps its own evaluation behavior; it is also useful as an independent correctness oracle.

For query comparisons or operational diagnosis, disable pruning before preparing the query:

```text
-Drdf4j.lmdb.literalTextIndex.enabled=false
```

This switch disables query use, not index maintenance. Re-enabling it therefore does not create a coverage gap.

## Validation and measurement

`LiteralTextSummaryTest` checks the byte layout, hash vectors, Unicode casing, malformed needles, long-prefix pruning, saturated signatures, and randomized no-false-negative properties. `ValueStoreLiteralTextIndexTest` exercises durable storage and lifecycle behavior. `LmdbLiteralSearchIndexOracleTest` compares exact result multisets with independent evaluation and checks selective-query decoding counts, transactions, snapshots, and locale changes.

The targeted regression fixture contains 4,096 stored labels and one matching long needle. The unchanged implementation returned the correct count but performed 4,112 stored-value decodes, including planning work. The acceptance bound for the indexed implementation is fewer than 409 decodes. This is an observable pruning requirement, not a timing assertion.

The accompanying JMH benchmark compares supported operations with query pruning enabled and disabled against the same data shape. Treat its results as measurements of that workload and cache state; dictionary size, label length, gram diversity, graph selectivity, and storage cache residency determine production performance. Use representative data and measure both query throughput and ingest/rebuild costs before choosing deployment capacity.

From the repository root, build the benchmark profile using Java 25 and the repository-local Maven cache, then run:

```sh
mvn -o -Dmaven.repo.local=.m2_repo -pl core/sail/lmdb -Pbenchmarks,quick package
scripts/run-single-benchmark.sh \
  --module core/sail/lmdb \
  --class org.eclipse.rdf4j.sail.lmdb.benchmark.LiteralTextFilterBenchmark \
  --method executeCount --no-build \
  --warmup-iterations 3 --measurement-iterations 5 --forks 1 \
  --param rowCount=100000 --param labelLength=96 --param matchEvery=1000
```

The six parameter combinations cover all three operations with pruning on and off. Both modes maintain the same summaries; this isolates query cost and does not measure index creation overhead. Setup verifies exact result counts and prints stored-value decoding counts outside measured iterations. All labels share their first eight bytes, so the prefix workload also exercises gram pruning beyond the stored leading bytes.

### Measured reference workload

The implementation was measured on 2026-11-09 using JDK 25.0.5+8, one query thread, native evaluation and Janino enabled, and parallel query execution disabled. Each operation used 100,000 rows, 96-character ASCII labels, and 100 matching rows. The warm-cache JMH run used one fork, three one-second warmup iterations, and five one-second measurement iterations. Throughput includes opening a connection, preparing the query, and executing its COUNT result.

| Operation | Pruning disabled (queries/s) | Pruning enabled (queries/s) | Throughput ratio | Stored-value reads, disabled → enabled |
| --- | ---: | ---: | ---: | ---: |
| `CONTAINS` | 10.097 ± 0.765 | 24.171 ± 6.513 | 2.39× | 100,002 → 102 |
| Equality | 10.513 ± 1.647 | 25.008 ± 1.359 | 2.38× | 99,903 → 3 |
| `STRSTARTS` | 9.813 ± 0.928 | 24.461 ± 2.824 | 2.49× | 100,002 → 102 |

The ± values are JMH's reported 99.9% confidence intervals over the five measurements; this short, single-fork run is a reference measurement, not a production capacity estimate. Stored-value reads are codec counter increments from a separate setup query, including planning work, rather than a count of distinct literals. Equality reuses one matching literal across 100 subjects, whereas the substring and prefix fixtures have distinct matching labels.

The substring and prefix fixtures allocated 7,487,488 bytes (about 7.14 MiB) in `literal_text` pages for 100,000 summaries plus two metadata entries: approximately 74.9 allocated bytes per summary. This includes that database's branch, leaf, and overflow pages, but excludes other environment metadata, free pages, and temporary copy-on-write growth. Equality allocated 7,479,296 bytes for 99,901 distinct labels plus two metadata entries.

The roughly 99.9% reduction in lexical reads yields a smaller wall-clock improvement because graph iteration, summary lookups, query preparation, and residual verification still consume time. This benchmark does not establish ingest overhead, cold-storage behavior, or selectivity on a different label distribution.
