# Wide30 component isolation with a linear benchmark consumer

## Verdict

Serializer alone has a modest positive completion signal in this workload: 2.81% lower pooled EOF latency, all four paired blocks positive, descriptive unadjusted interval +0.59% to +4.94%. This does not establish a universal gain or merge readiness. The primary incremental serializer-on-depth comparison is inconclusive (1.93% lower, interval -3.63% to +7.14%). The combined stack is 0.39% slower than base pooled, also inconclusive; no synergy or first-batch gain is established. Depth addition to serializer is adverse in all four pairs, but its interval still crosses zero. Measured-index drift differs by arm and remains visible below; no favorable suffix was selected.

## Scope and interpretation

This is a separate corrected-consumer population. The original wide-isolation evidence was published at c9006c5eb655df2e7348035bd82d4104310888d1 and remains unchanged. Do not pool these samples with the old client or infer a controlled parser speedup from different runs. No production application code changed in this phase. Actual Workbench browser code already used a linear fragment splitter; the repeated scan was in the Node benchmark endpoint only.

Real on-disk LMDB, synthetic RDF fixture, actual RDF4J Server binary output, Workbench Java decoding/NDJSON serialization and a Node streaming consumer. Not browser rendering, concurrent QPS, cold disk, remote network or server-only throughput.

## Immutable arms

- base: `5ac89b5c137030fea1aa6aa7160984423d7f4cd5`
- depth: `0750336e71ab0ca0d216ce9c36483a6e9063337d`
- serializer: `e2dc1ea1fa1f86fe07e29330df6f82cd4d1be8b8`
- stack: `d94617b37d2142e3226bd71d29916c7588675b1c`

Same pinned binary writer/compact identity mapping/memo/IR stack and 100,000-row 30-slot query across arms. Only the read-depth and three-class typed serializer overlays differ. No ordered adapter, resource-holder, initializer restoration, cancellation or test-seam overlays. All 3,137 runtime files reverified against the frozen manifest.

## Qualification and fixed protocol

997 differential parser cases passed: byte partitions, multilingual and emoji UTF8, empty/multiple/CRLF lines, malformed JSON/error records, invalid/truncated UTF8 and terminal/EOF rules. Parsed values and error classes match the old consumer; error-message identity is not claimed. Source-level newline-search regions are bounded linearly in the long partial-line test. Lazy line emission preserves the first parsed rows-batch boundary. Four full-term HTTP qualifications passed independent every-term/unique-bag checks and dual checksums, expected route/origins and no spill/migration.

One fixed four-block Latin square, four fresh JVMs per arm, 60 warmups and 20 measured queries each: 1,280 clean-protocol requests, 320 measured. All observations retained; no adaptive extension or tail selection. Profiles use separate JVMs and are never pooled into clean timings. First-batch latency is after JSON.parse of the first complete rows record, normally 32 rows, not first engine tuple or browser paint. EOF includes Node decoding/counting/backpressure.

## Clean means

| Arm | EOF ms | First batch ms | Derived rows/s | Fork EOF range ms |
|---|---:|---:|---:|---:|
| base | 1404.173 | 37.680 | 71216 | 1358.011–1436.203 |
| depth | 1437.514 | 37.892 | 69565 | 1354.756–1479.501 |
| serializer | 1364.780 | 37.318 | 73272 | 1347.487–1383.111 |
| stack | 1409.712 | 38.996 | 70936 | 1375.169–1458.990 |

Derived rows/s is reciprocal sequential full-response latency, not concurrent serving capacity. Positive reductions below mean lower latency.

## Component contrasts

### eofMs

| Contrast | Reduction % | Four paired reductions % | Descriptive 95% interval % |
|---|---:|---|---|
| stack-versus-depth | 1.934 | -1.51, +2.42, +6.28, +0.24 | -3.63 to +7.14 |
| serializer-versus-base | 2.805 | +3.00, +3.70, +3.66, +0.77 | +0.59 to +4.94 |
| depth-versus-base | -2.374 | +4.17, -1.19, -5.03, -7.69 | -10.94 to +5.60 |
| stack-versus-base | -0.394 | +2.73, +1.26, +1.57, -7.44 | -7.96 to +6.65 |
| stack-versus-serializer | -3.292 | -0.28, -2.53, -2.17, -8.27 | -8.84 to +2.02 |
| serializer-versus-depth | 5.060 | -1.22, +4.83, +8.27, +7.86 | -2.11 to +11.63 |

### firstRowMs

| Contrast | Reduction % | Four paired reductions % | Descriptive 95% interval % |
|---|---:|---|---|
| stack-versus-depth | -2.913 | -7.44, +2.94, -3.77, -3.57 | -10.08 to +3.82 |
| serializer-versus-base | 0.961 | +1.70, +8.68, -8.74, +2.70 | -10.74 to +11.99 |
| depth-versus-base | -0.562 | -3.38, -4.19, +9.02, -4.30 | -11.82 to +9.58 |
| stack-versus-base | -3.492 | -11.07, -1.13, +5.59, -8.03 | -16.12 to +7.83 |
| stack-versus-serializer | -4.496 | -12.99, -10.74, +13.17, -11.02 | -28.01 to +14.20 |
| serializer-versus-depth | 1.514 | +4.91, +12.35, -19.51, +6.71 | -21.83 to +20.88 |

Intervals use four independent paired log fork means and t with three degrees of freedom, unadjusted for multiple comparisons. Within-fork requests are not independent replications. A crossing-zero interval does not establish equivalence.
- headersMs interaction ratio 1.0380, interval 0.9197–1.1716.
- firstRowMs interaction ratio 1.0423, interval 0.9471–1.1469.
- batchMs interaction ratio 1.0092, interval 0.9624–1.0584.
- endMs interaction ratio 1.0092, interval 0.9623–1.0584.
- eofMs interaction ratio 1.0091, interval 0.9621–1.0585.

## All forks

| Fork | EOF ms | First batch ms |
|---|---:|---:|
| matched-01-base | 1413.781 | 37.442 |
| matched-02-depth | 1354.756 | 38.707 |
| matched-03-stack | 1375.169 | 41.588 |
| matched-04-serializer | 1371.328 | 36.806 |
| matched-05-depth | 1453.292 | 39.418 |
| matched-06-serializer | 1383.111 | 34.549 |
| matched-07-base | 1436.203 | 37.832 |
| matched-08-stack | 1418.092 | 38.258 |
| matched-09-serializer | 1357.193 | 42.862 |
| matched-10-stack | 1386.596 | 37.217 |
| matched-11-depth | 1479.501 | 35.865 |
| matched-12-base | 1408.696 | 39.419 |
| matched-13-stack | 1458.990 | 38.921 |
| matched-14-base | 1358.011 | 36.028 |
| matched-15-serializer | 1347.487 | 35.056 |
| matched-16-depth | 1462.506 | 37.578 |

## Convergence

| Arm | First measured third ms | Middle third ms | Last third ms |
|---|---:|---:|---:|
| base | 1434.167 | 1397.044 | 1380.288 |
| depth | 1462.822 | 1432.298 | 1416.677 |
| serializer | 1341.528 | 1347.925 | 1402.479 |
| stack | 1410.461 | 1418.267 | 1401.630 |

Fixed longer warmup does not automatically prove convergence. Every measured index remains included. Whole-JVM GC logs include startup/warmup and cannot be assigned to particular query times; heap snapshots are not allocation or retained-heap measurements. Narrow results remain separate evidence. Prior application safety tests are scoped, not a whole-repository pass or automatic merge readiness.

## Framing and observed route counters

- rowRecords: base [581]; depth [581]; serializer [581]; stack [581]
- firstRecordRows: base [32]; depth [32]; serializer [32]; stack [32]
- firstRecordBytes: base [41615]; depth [41615]; serializer [41615]; stack [41615]
- contentEncoding: base ['gzip']; depth ['gzip']; serializer ['gzip']; stack ['gzip']
- bytes: base [151462499]; depth [151462499]; serializer [151462499]; stack [151462499]
- LmdbNativeJaninoCodegen:COMPILATIONS: base [0]; depth [0]; serializer [0]; stack [0]
- LmdbNativeJaninoCodegen:CACHE_HITS: base [1]; depth [1]; serializer [1]; stack [1]
- NativeDistinctTracker:PRIMITIVE_ROWS: base [100000]; depth [100000]; serializer [100000]; stack [100000]
- NativeExpressionDomain:SPILL_FILES_CREATED: base [0]; depth [0]; serializer [0]; stack [0]
- NativeBudgetedWideDistinct:MIGRATED: base [0]; depth [0]; serializer [0]; stack [0]

These counters establish observed routing/cache/grouping, not byte-identical generated plans or JIT machine code. Bytes are decoded stream bytes, not compressed wire bytes.

## Evidence

summary.json retains every request index, fork, contrast, counter and framing observation. Exact protocol/source/runtime manifests and raw logs remain available; evidence/index.json plus pool chunks reconstruct textual run artifacts with verified SHA-256. Large JFR/Node raw files remain local with hashes; compact profile summaries are separate.
