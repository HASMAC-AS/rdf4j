# Round-two benchmark methodology review

This is a read-only source review of [BinaryWriterBenchmark](harness/src/org/eclipse/rdf4j/query/resultio/binary/benchmark/BinaryWriterBenchmark.java), [SmallDocumentBenchmark](harness/src/org/eclipse/rdf4j/query/resultio/binary/benchmark/SmallDocumentBenchmark.java), and [the runner](harness/bench.py). No builds, smoke executions, or timings were performed for this review. The fixture construction, measured operations, and normalization support comparisons between implementations within the same parameter combination.

## Implementation identity and measured work

The published baseline is commit 741b9389538e80214075692439a8cd25a8a9ae84. A direct source comparison verified that [PublishedBinaryQueryResultWriter.java](harness/PublishedBinaryQueryResultWriter.java), after undoing its class-name rename, exactly matches that commit's production writer. Both benchmarks select this class for "published", the production BinaryQueryResultWriter for "current", and a supplied simple or fully qualified class name otherwise. The runner compiles the actual production source and places the harness classes before dependency jars on the runtime classpath.

Synthetic binding sets and values are constructed during trial setup. Each measured invocation resets a real ByteArrayOutputStream, constructs one fresh writer, emits a complete document, and consumes the backing array, four sampled bytes, and output size. Setup writes a full document first, growing the sink and initializing lazy fixture fields before measurement. The output is stored; there is no discarding sink. Constructor lookup occurs once per trial; reflective instantiation occurs once per document and remains part of its measured cost.

The heartbeat parameter constructs the real QueryResponseHeartbeat wrapper for every document, calls start(writer), verifies that binary output declines whitespace scheduling, and calls complete(). Serializer writes pass through its synchronized payload stream. The measurement includes wrapper construction and completion, but no heartbeat thread, network transport, servlet, backpressure, or disconnect.

| Benchmark | One invocation | JMH time and normalized allocation |
| --- | --- | --- |
| BinaryWriterBenchmark | One complete 4,096-row document | ns/row and B/row, through OperationsPerInvocation(4096) |
| SmallDocumentBenchmark | One complete document containing 0, 1, or 8 rows | ns/document and B/document; no operations-per-invocation normalization |

Do not divide the large benchmark results by 4,096 again. Multiplying them by 4,096 reconstructs document cost. Small-document zero-row results still represent a complete eight-column header and document terminator, so dividing those results by the number of rows is inappropriate.

## Fixture coverage and limits

Mixed rows contain IRIs, plain literals, integers, language literals, blank nodes, decimals, and a custom datatype. Stable namespace layout uses hyphens inside subject and kind local names, yielding four namespaces for mixed fixtures. Churn changes the separator to a colon, creating 4,115 namespaces in the large mixed document; this is a separate namespace-churn workload.

The equality fixtures use initialized LmdbLiteral objects with an explicitly canonical synthetic revision. Identity repeats reuse value objects in runs of 32 rows; equal-value repeats create distinct objects with equal native IDs; unique values have distinct IDs. They isolate serialization/equality overhead and do not exercise LMDB access or decoding.

The query workload does evaluate a real native-enabled LMDB query inside each invocation, including iteration, term materialization, serialization, and result closure. Setup loads 512 subjects with eight values each and prepares the query. Each invocation requires 4,096 results. This measures a warmed, small, local-store query and writer together; a query timing cannot establish the writer's isolated speedup.

Unicode coverage includes Latin-1 characters, CJK, supplementary emoji, mixed scripts, and a mostly ASCII large label ending in one CJK character. Unicode mixed rows also use a directed language literal. The long/medium label occupies column 1; seven short mixed values remain in every row.

| Workload | UTF-16 code units in column-1 label | UTF-8 payload bytes |
| --- | ---: | ---: |
| long | 8,192 | 8,192 |
| long_unicode | 8,870 | 14,326 |
| long_latin1 | 8,192 | 16,380 |
| long_cjk | 8,192 | 24,568 |
| long_emoji | 8,192 | 16,380 |
| long_sparse | 8,192 | 8,194 |
| medium | 1,024 | 1,024 |
| medium_unicode | 1,109 | 1,789 |

These sizes include the four-character row identifier and were checked directly from the fixture expressions. They are not equal-byte workloads. Compare published and candidate implementations within each workload; differences between workloads also reflect payload size and other columns. Malformed text is covered by correctness tests, not timing fixtures.

SmallDocumentBenchmark uses eight short ASCII-valued columns with mixed RDF types and stable namespaces. It directly exposes per-writer scratch allocation and cache initialization costs, but does not characterize tiny Unicode results, zero-column documents, writer reuse, or all-core-datatype populations. Header writing, reflection, and heartbeat lifecycle costs remain included and can be a large fraction of tiny-document time. Shared static template initialization occurs before timing, so these are warm-JVM costs, not first-use class-loading measurements. Normalized allocation measures bytes allocated, not retained heap.

## Smoke checks and interpretation

The large harness's smoke mode parses the complete output, verifies headers and row count, and compares every synthetic row and term. Query validation allows order changes but verifies each of the 4,096 subject/value combinations exactly once, checks identifier bounds, and compares all terms. It establishes semantic roundtrip, not exact byte identity between implementations.

The small harness performs those header/count/row comparisons and also requires identical output bytes across implementations and sinks for each selected row count. Its first combination supplies the reference. Independent wire-format unit tests remain necessary to catch shared writer/parser mistakes.

The large smoke main still defaults to "baseline,current", where "baseline" selects the original upstream writer. Round-two smoke commands must explicitly select "published,current" or the intended candidate classes. JMH parameters and the small smoke main default to "published,current".

Preserve the actual commands, source identity metadata for each compilation, raw results, and per-fork observations. The compiler rewrites its identity file, so later candidate compilations must not obscure which source produced earlier results. Short screens select candidates; confidence intervals, fork variability, reversed-order checks when warranted, and shared-host interference determine how strongly final timings can be interpreted.
