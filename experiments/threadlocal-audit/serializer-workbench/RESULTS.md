# Typed compact-row serialization: actual Workbench results

## Verdict

Typed serialization on depth reduced full-response latency by 8.11% (derived rows/s +8.82%), with all four primary paired comparisons favorable and a descriptive unadjusted 95% interval of 1.59%–14.44% lower latency. This is promising within this fixture, not an established universal win. Serializer alone versus base was inconclusive, combined versus base was inconclusive, and factorial synergy was not established. First-row latency did not improve: serializer alone was 14.48% slower than base across all four pairs, while combined versus depth was 2.77% slower with mixed/uncertain signs. Do not recommend adoption as an all-latency improvement without weighing that tradeoff.

The workload is real on-disk LMDB with synthetic RDF data, actual RDF4J Server binary output, actual Workbench Java decoding and NDJSON serialization, and a Node streaming client. It excludes browser rendering, concurrent QPS, remote-network effects and the historical wide eight-million-row fixture.

## Immutable arms

- **base**: `5ac89b5c137030fea1aa6aa7160984423d7f4cd5`.
- **depth**: `0750336e71ab0ca0d216ce9c36483a6e9063337d`.
- **serializer**: `e2dc1ea1fa1f86fe07e29330df6f82cd4d1be8b8`.
- **both**: `d94617b37d2142e3226bd71d29916c7588675b1c`.

All arms use the same reconstructed frozen server/dependency stack, pinned PR #6440 writer, compact identity mapping, memo/IR flags and full six-slot 100,000-row response. The serializer layer contains only three WorkbenchPageResultWriter family classes. Its pre-edit three-class family was byte-identical to the actual frozen common Workbench JAR. Read-resource holder, cancellation changes, initializer null-slot restoration and the grouped test-only observer are excluded. The archive branch is not a runtime identity.

## Protocol and qualification

One prespecified four-block Latin square, four independent fresh JVMs per arm. Each fork completed 60 warmups and 30 measured sequential queries: 1,440 requests total, 480 measured. Every request passed compiled IR, memo, binary MIME, row count, no-spill, ledger cleanup and class-origin checks. Four separate untimed qualification queries validated every term, uniqueness and dual checksums. All samples were retained; no trimming or additional clean round. Startup and client setup are outside POST-to-EOF latency.

Correctness: 1,561 Workbench module tests passed, final focused suite 25 passed, and isolated base+serializer/depth+serializer each 25 passed. These test scope statements are not a whole-repository pass. Exact-byte compatibility, failure/flush/streaming behavior and prepack ordering were independently reviewed.

## Clean means

| Arm | EOF ms | First row ms | Derived rows/s | Fork EOF range ms |
|---|---:|---:|---:|---:|
| base | 484.528 | 34.463 | 206386 | 471.594–502.648 |
| depth | 495.459 | 35.956 | 201833 | 469.787–528.120 |
| serializer | 475.763 | 39.455 | 210189 | 456.614–492.200 |
| both | 455.289 | 36.953 | 219641 | 414.030–494.864 |

Rows/s is the reciprocal of sequential full-response latency, not concurrent serving capacity.

| Contrast | EOF reduction % | Paired reductions % | Descriptive 95% interval % |
|---|---:|---|---|
| both-versus-depth | 8.108 | +9.20, +2.90, +12.70, +7.89 | +1.59 to +14.44 |
| serializer-versus-base | 1.809 | +3.26, +3.18, +1.59, -0.80 | -1.23 to +4.78 |
| depth-versus-base | -2.256 | -5.07, -8.07, +0.27, +3.79 | -10.99 to +5.96 |
| both-versus-base | 6.034 | +4.59, -4.93, +12.94, +11.39 | -7.32 to +18.11 |
| both-versus-serializer | 4.303 | +1.38, -8.38, +11.53, +12.09 | -11.88 to +18.50 |
| serializer-versus-depth | 3.975 | +7.93, +10.41, +1.32, -4.77 | -7.51 to +14.10 |

Factorial geometric interaction ratio: **0.935**, descriptive 95% interval **0.860–1.016**. Values below one suggest supermultiplicative latency reduction, but this wide interval crosses one. Intervals use paired log fork means and t with three degrees of freedom; they are unadjusted for multiple contrasts and do not establish equivalence.

## All independent fork means

| Fork | Arm | EOF ms | First row ms |
|---|---|---:|---:|
| matched-01-base | base | 502.648 | 35.788 |
| matched-02-depth | depth | 528.120 | 39.347 |
| matched-03-both | both | 479.553 | 41.689 |
| matched-04-serializer | serializer | 486.243 | 37.478 |
| matched-05-depth | depth | 509.658 | 36.234 |
| matched-06-serializer | serializer | 456.614 | 41.582 |
| matched-07-base | base | 471.594 | 33.931 |
| matched-08-both | both | 494.864 | 40.449 |
| matched-09-serializer | serializer | 467.993 | 36.694 |
| matched-10-both | both | 414.030 | 32.862 |
| matched-11-depth | depth | 474.273 | 33.001 |
| matched-12-base | base | 475.557 | 32.423 |
| matched-13-both | both | 432.710 | 32.813 |
| matched-14-base | base | 488.313 | 35.710 |
| matched-15-serializer | serializer | 492.200 | 42.064 |
| matched-16-depth | depth | 469.787 | 35.242 |

## Convergence and limitations

| Arm | First measured third ms | Middle third ms | Last third ms |
|---|---:|---:|---:|
| base | 489.061 | 487.531 | 476.991 |
| depth | 509.870 | 488.215 | 488.293 |
| serializer | 499.563 | 467.323 | 460.402 |
| both | 426.570 | 487.584 | 451.713 |

Longer fixed warmup does not automatically prove convergence; measured thirds above retain evidence of drift or variation. We retain the full prespecified interval rather than select its faster tail. Four forks per arm constrain precision; request samples within a fork are not independent replications. Earlier embedded depth gains do not establish a gain through this HTTP pipeline.

Whole-JVM GC logs include startup/warmup, not query-attributed pauses. Heap-used before/after snapshots are not allocation or retained heap. RSS was not collected; no memory-capacity conclusion follows. The browser remains unmeasured following the prior policy-blocked launch; no retry or bypass occurred.

## Evidence

`summary.json` retains every fork and index, GC summaries, first application-query values and all contrasts. `runs/matched-*` retain raw request metrics, commands and logs. `protocol.json`, source IDs, class manifests and the frozen common manifests establish provenance. Separate JFR results are reported in `HTTP-PROFILES.md`, never pooled into the clean timing population.

## First-row contrasts

Positive reductions mean lower latency. These are separate from full completion; all intervals remain descriptive and unadjusted.

| Contrast | Reduction % | Paired reductions % | 95% interval % |
|---|---:|---|---|
| both-versus-depth | -2.77 | -5.95, -11.63, +0.42, +6.89 | -15.93 to +9.67 |
| serializer-versus-base | -14.48 | -4.72, -22.55, -13.17, -17.79 | -27.26 to -2.78 |
| both-versus-base | -7.23 | -16.49, -19.21, -1.35, +8.11 | -29.58 to +12.24 |

## Published evidence packaging

`evidence/index.json` and ordered pool chunks reconstruct every textual run artifact losslessly. Concatenate pool lines by index, or for compactJsonFrom log entries compact the reconstructed JSON outside quoted strings and append a newline. All exact-byte SHA-256 round trips are verified by package-evidence.py. Originals remain local; raw JFR and huge full-stack exports are excluded, compact summaries included.
