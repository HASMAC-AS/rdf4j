# Narrow six-slot component isolation with a linear benchmark consumer

## Scope and interpretation

This is a separate corrected-consumer population. Original narrow-consumer and corrected wide-consumer evidence remain unchanged and separate. Do not pool these samples with the old client or infer a controlled parser speedup from different runs. No production application code changed in this phase. Actual Workbench browser code already used a linear fragment splitter; the repeated scan was in the Node benchmark endpoint only.

Real on-disk LMDB, synthetic RDF fixture, actual RDF4J Server binary output, Workbench Java decoding/NDJSON serialization and a Node streaming consumer. Not browser rendering, concurrent QPS, cold disk, remote network or server-only throughput.

## Immutable arms

- base: `5ac89b5c137030fea1aa6aa7160984423d7f4cd5`
- depth: `0750336e71ab0ca0d216ce9c36483a6e9063337d`
- serializer: `e2dc1ea1fa1f86fe07e29330df6f82cd4d1be8b8`
- stack: `d94617b37d2142e3226bd71d29916c7588675b1c`

Same pinned binary writer/compact identity mapping/memo/IR stack and 100,000-row six-slot query across arms. Only the read-depth and three-class typed serializer overlays differ. No ordered adapter, resource-holder, initializer restoration, cancellation or test-seam overlays. All 3,137 runtime files reverified against the frozen manifest.

## Qualification and fixed protocol

997 differential parser cases passed: byte partitions, multilingual and emoji UTF8, empty/multiple/CRLF lines, malformed JSON/error records, invalid/truncated UTF8 and terminal/EOF rules. Parsed values and error classes match the old consumer; error-message identity is not claimed. Source-level newline-search regions are bounded linearly in the long partial-line test. Lazy line emission preserves the first parsed rows-batch boundary. Four full-term HTTP qualifications passed independent every-term/unique-bag checks and dual checksums, expected route/origins and no spill/migration.

One fixed four-block Latin square, four fresh JVMs per arm, 60 warmups and 20 measured queries each: 1,280 clean-protocol requests, 320 measured. All observations retained; no adaptive extension or tail selection. Profiles use separate JVMs and are never pooled into clean timings. First-batch latency is after JSON.parse of the first complete rows record, normally 32 rows, not first engine tuple or browser paint. EOF includes Node decoding/counting/backpressure.

## Clean means

| Arm | EOF ms | First batch ms | Derived rows/s | Fork EOF range ms |
|---|---:|---:|---:|---:|
| base | 488.412 | 38.733 | 204745 | 470.260–511.948 |
| depth | 480.795 | 35.500 | 207989 | 441.755–533.591 |
| serializer | 454.563 | 37.034 | 219991 | 422.297–485.919 |
| stack | 436.530 | 37.554 | 229080 | 418.435–471.973 |

Derived rows/s is reciprocal sequential full-response latency, not concurrent serving capacity. Positive reductions below mean lower latency.

## Component contrasts

### eofMs

| Contrast | Reduction % | Four paired reductions % | Descriptive 95% interval % |
|---|---:|---|---|
| stack-versus-depth | 9.207 | +13.23, +3.77, +19.30, -1.37 | -7.00 to +22.76 |
| serializer-versus-base | 6.930 | +5.21, +17.51, +7.40, -3.33 | -7.81 to +19.77 |
| depth-versus-base | 1.560 | -0.64, +13.71, -8.39, +0.99 | -14.31 to +15.54 |
| stack-versus-base | 10.623 | +12.67, +16.96, +12.53, -0.36 | -1.67 to +21.52 |
| stack-versus-serializer | 3.967 | +7.87, -0.67, +5.54, +2.87 | -2.04 to +9.60 |
| serializer-versus-depth | 5.456 | +5.82, +4.40, +14.57, -4.36 | -7.85 to +16.92 |

### firstRowMs

| Contrast | Reduction % | Four paired reductions % | Descriptive 95% interval % |
|---|---:|---|---|
| stack-versus-depth | -5.784 | -5.25, -8.58, +3.50, -14.20 | -18.55 to +5.34 |
| serializer-versus-base | 4.387 | -6.25, +31.31, +2.12, -21.17 | -41.94 to +34.45 |
| depth-versus-base | 8.347 | +1.80, +36.78, -10.14, -6.59 | -39.20 to +38.67 |
| stack-versus-base | 3.046 | -3.36, +31.36, -6.28, -21.73 | -45.04 to +33.94 |
| stack-versus-serializer | -1.402 | +2.72, +0.07, -8.58, -0.47 | -9.40 to +5.88 |
| serializer-versus-depth | -4.321 | -8.20, -8.66, +11.13, -13.67 | -24.29 to +12.31 |

Intervals use four independent paired log fork means and t with three degrees of freedom, unadjusted for multiple comparisons. Within-fork requests are not independent replications. A crossing-zero interval does not establish equivalence.
- headersMs interaction ratio 1.1171, interval 0.7448–1.6757.
- firstRowMs interaction ratio 1.0983, interval 0.7452–1.6187.
- batchMs interaction ratio 0.9772, interval 0.8003–1.1933.
- endMs interaction ratio 0.9777, interval 0.8002–1.1946.
- eofMs interaction ratio 0.9775, interval 0.7980–1.1974.

## All forks

| Fork | EOF ms | First batch ms |
|---|---:|---:|
| matched-01-base | 479.148 | 37.184 |
| matched-02-depth | 482.226 | 36.515 |
| matched-03-stack | 418.435 | 38.432 |
| matched-04-serializer | 454.184 | 39.508 |
| matched-05-depth | 441.755 | 31.028 |
| matched-06-serializer | 422.297 | 33.715 |
| matched-07-base | 511.948 | 49.080 |
| matched-08-stack | 425.110 | 33.690 |
| matched-09-serializer | 455.851 | 34.853 |
| matched-10-stack | 430.600 | 37.844 |
| matched-11-depth | 533.591 | 39.216 |
| matched-12-base | 492.294 | 35.607 |
| matched-13-stack | 471.973 | 40.248 |
| matched-14-base | 470.260 | 33.062 |
| matched-15-serializer | 485.919 | 40.060 |
| matched-16-depth | 465.609 | 35.242 |

## Convergence

| Arm | First measured third ms | Middle third ms | Last third ms |
|---|---:|---:|---:|
| base | 476.292 | 491.856 | 497.580 |
| depth | 488.798 | 482.870 | 471.015 |
| serializer | 444.061 | 457.981 | 462.136 |
| stack | 430.835 | 410.398 | 464.623 |

Fixed longer warmup does not automatically prove convergence. Every measured index remains included. Whole-JVM GC logs include startup/warmup and cannot be assigned to particular query times; heap snapshots are not allocation or retained-heap measurements. Narrow results remain separate evidence. Prior application safety tests are scoped, not a whole-repository pass or automatic merge readiness.

## Framing and observed route counters

- rowRecords: base [106]; depth [106]; serializer [106]; stack [106]
- firstRecordRows: base [32]; depth [32]; serializer [32]; stack [32]
- firstRecordBytes: base [8603]; depth [8603]; serializer [8603]; stack [8603]
- contentEncoding: base ['gzip']; depth ['gzip']; serializer ['gzip']; stack ['gzip']
- bytes: base [27345361]; depth [27345361]; serializer [27345361]; stack [27345361]
- LmdbNativeJaninoCodegen:COMPILATIONS: base [0]; depth [0]; serializer [0]; stack [0]
- LmdbNativeJaninoCodegen:CACHE_HITS: base [1]; depth [1]; serializer [1]; stack [1]
- NativeDistinctTracker:PRIMITIVE_ROWS: base [100000]; depth [100000]; serializer [100000]; stack [100000]
- NativeExpressionDomain:SPILL_FILES_CREATED: base [0]; depth [0]; serializer [0]; stack [0]
- NativeBudgetedWideDistinct:MIGRATED: base [0]; depth [0]; serializer [0]; stack [0]

These counters establish observed routing/cache/grouping, not byte-identical generated plans or JIT machine code. Bytes are decoded stream bytes, not compressed wire bytes.

## Evidence

summary.json retains every request index, fork, contrast, counter and framing observation. Exact protocol/source/runtime manifests and raw logs remain available; evidence/index.json plus pool chunks reconstruct textual run artifacts with verified SHA-256. Large JFR/Node raw files remain local with hashes; compact profile summaries are separate.

## Recovery and conclusion

The original linear-narrow-workbench attempt was interrupted during its first baseline JVM after 42 warmups, before any measured query. Its artifacts remain intact. This new directory contains an entirely restarted balanced series, not a continuation or selective rerun. All 3,137 runtime hashes matched, 997 parser tests passed again, and four fresh full-term qualifications passed before timing. Frozen production code was not rebuilt or edited.

The combined arm has a 10.62% lower mean completion time than baseline, but its paired descriptive interval is −1.67% to +21.52%. Serializer-only and read-depth-only contrasts also cross zero. Three of four combined/base blocks favor the combination; the fourth is slightly adverse. This supports continued investigation, not a statistically established narrow win, synergy or merge-readiness. No first-batch contrast establishes improvement. No old-consumer or interrupted data were pooled.
