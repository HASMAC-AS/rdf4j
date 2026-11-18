# Ordered compact values: actual Workbench results

## Verdict

The ordered adapter did not establish an incremental gain on the qualified depth+serializer stack: full completion was 2.12% lower pooled, with mixed paired signs and a descriptive unadjusted 95% reduction interval of -6.70% to +10.19%. First-row latency was 4.88% lower pooled but also inconclusive. The existing qualified stack alone was 14.31% lower completion latency than base in this same matched series (all four pairs favorable, descriptive 95% reduction interval 4.35%–23.25%; derived throughput +16.70%). Its first-row latency was 1.82% slower pooled and inconclusive. Combined versus base was 16.13% lower with all four pairs favorable, but that total must not be credited to the ordered adapter. Ordered-only versus base was inconclusive; no factorial synergy was established. This interaction concerns the entire qualified stack, not independent depth or serializer effects. Do not promote the adapter as a proven incremental performance win.

The workload is real on-disk LMDB with synthetic RDF data, actual RDF4J Server binary output, actual Workbench Java decoding and NDJSON serialization, and a Node streaming client. It excludes browser rendering, concurrent QPS, remote-network effects and the historical wide eight-million-row fixture.

## Immutable arms

- **base**: `5ac89b5c137030fea1aa6aa7160984423d7f4cd5`.
- **stack**: `d94617b37d2142e3226bd71d29916c7588675b1c`.
- **ordered**: `db66096398582f3f0d83db2732101f0821bcf878`.
- **both**: `71631eeab59c8abd5f586756209df8f1f8cd3f9e`.

All arms use the same reconstructed frozen server/dependency stack, pinned PR #6440 writer, compact identity mapping, memo/IR flags and full six-slot 100,000-row response. Each ordered layer contains only nine classes in QueryEvaluator, TupleResultBuilder and WorkbenchPageResultWriter families. Both pristine nine-class baselines recompiled byte-identical to their actual frozen base/stack runtime. The ordered-only source does not include typed serialization; combined retains it. Read-resource holder, cancellation changes, initializer null-slot restoration and the grouped test-only observer are excluded. The archive branch is not a runtime identity.

## Protocol and qualification

One prespecified four-block Latin square, four independent fresh JVMs per arm. Each fork completed 60 warmups and 30 measured sequential queries: 1,440 requests total, 480 measured. Every request passed compiled IR, memo, binary MIME, row count, no-spill, ledger cleanup and class-origin checks. Four separate untimed qualification queries validated every term, uniqueness and dual checksums. All samples were retained; no trimming or additional clean round. Startup and client setup are outside POST-to-EOF latency.

Correctness: 1,574 Workbench module tests passed; isolated ordered-only 126 and combined 128 passed with hardened expected-count and container checks. Initial missing spring-test zero-test launcher runs were failures, retained and fixed before qualification. Exact-byte, subclass/mock fallback, lookup/prepacking order, no retention and streaming/error behavior were independently reviewed. This is not a whole-repository pass.

## Clean means

| Arm | EOF ms | First row ms | Derived rows/s | Fork EOF range ms |
|---|---:|---:|---:|---:|
| base | 546.815 | 37.606 | 182877 | 513.599–573.356 |
| stack | 468.560 | 38.291 | 213420 | 436.791–491.705 |
| ordered | 489.053 | 38.110 | 204477 | 451.251–531.318 |
| both | 458.613 | 36.422 | 218049 | 431.463–483.830 |

Rows/s is the reciprocal of sequential full-response latency, not concurrent serving capacity.

| Contrast | EOF reduction % | Paired reductions % | Descriptive 95% interval % |
|---|---:|---|---|
| both-versus-stack | 2.123 | -5.03, +7.53, +4.29, +1.22 | -6.70 to +10.19 |
| ordered-versus-base | 10.563 | +6.13, -0.32, +20.05, +15.54 | -5.07 to +24.11 |
| stack-versus-base | 14.311 | +18.62, +5.55, +14.24, +18.24 | +4.35 to +23.25 |
| both-versus-base | 16.130 | +14.52, +12.67, +17.92, +19.24 | +11.18 to +20.80 |
| both-versus-ordered | 6.224 | +8.94, +12.94, -2.66, +4.39 | -5.09 to +16.06 |
| ordered-versus-stack | -4.374 | -15.34, -6.21, +6.77, -3.31 | -19.85 to +9.37 |

Factorial geometric interaction ratio: **1.096**, descriptive 95% interval **0.907–1.325**. Values below one suggest supermultiplicative latency reduction, but this wide interval crosses one. Intervals use paired log fork means and t with three degrees of freedom; they are unadjusted for multiple contrasts and do not establish equivalence.

## All independent fork means

| Fork | Arm | EOF ms | First row ms |
|---|---|---:|---:|
| matched-01-base | base | 566.043 | 39.102 |
| matched-02-stack | stack | 460.654 | 37.953 |
| matched-03-both | both | 483.830 | 37.521 |
| matched-04-ordered | ordered | 531.318 | 35.639 |
| matched-05-stack | stack | 485.089 | 37.023 |
| matched-06-ordered | ordered | 515.231 | 44.400 |
| matched-07-base | base | 513.599 | 36.754 |
| matched-08-both | both | 448.538 | 34.882 |
| matched-09-ordered | ordered | 458.411 | 37.616 |
| matched-10-both | both | 470.623 | 39.393 |
| matched-11-stack | stack | 491.705 | 36.773 |
| matched-12-base | base | 573.356 | 38.588 |
| matched-13-both | both | 431.463 | 33.893 |
| matched-14-base | base | 534.262 | 35.980 |
| matched-15-ordered | ordered | 451.251 | 34.783 |
| matched-16-stack | stack | 436.791 | 41.416 |

## Convergence and limitations

| Arm | First measured third ms | Middle third ms | Last third ms |
|---|---:|---:|---:|
| base | 564.693 | 530.301 | 545.452 |
| stack | 445.009 | 465.050 | 495.620 |
| ordered | 501.007 | 478.178 | 487.973 |
| both | 458.325 | 443.689 | 473.826 |

Longer fixed warmup does not automatically prove convergence; measured thirds above retain evidence of drift or variation. We retain the full prespecified interval rather than select its faster tail. Four forks per arm constrain precision; request samples within a fork are not independent replications. Earlier embedded depth gains do not establish a gain through this HTTP pipeline.

Whole-JVM GC logs include startup/warmup, not query-attributed pauses. Heap-used before/after snapshots are not allocation or retained heap. RSS was not collected; no memory-capacity conclusion follows. The browser remains unmeasured following the prior policy-blocked launch; no retry or bypass occurred.

## Evidence

`summary.json` retains every fork and index, GC summaries, first application-query values and all contrasts. `runs/matched-*` retain raw request metrics, commands and logs. `protocol.json`, source IDs, class manifests and the frozen common manifests establish provenance. Separate JFR results are reported in `HTTP-PROFILES.md`, never pooled into the clean timing population.

## First-row contrasts

Positive reductions mean lower latency. These are separate from full completion; all intervals remain descriptive and unadjusted.

| Contrast | Reduction % | Paired reductions % | 95% interval % |
|---|---:|---|---|
| both-versus-stack | 4.88 | +1.14, +5.78, -7.13, +18.16 | -13.79 to +20.59 |
| ordered-versus-base | -1.34 | +8.86, -20.80, +2.52, +3.33 | -22.85 to +17.08 |
| stack-versus-base | -1.82 | +2.94, -0.73, +4.71, -15.11 | -16.56 to +11.16 |
| both-versus-base | 3.15 | +4.04, +5.09, -2.09, +5.80 | -2.55 to +8.75 |

## Published evidence packaging

`evidence/index.json` and ordered pool chunks reconstruct every textual run artifact losslessly. Concatenate pool lines by index, or for compactJsonFrom log entries compact the reconstructed JSON outside quoted strings and append a newline. All exact-byte SHA-256 round trips are verified by package-evidence.py. Originals remain local; raw JFR and huge full-stack exports are excluded, compact summaries included.
