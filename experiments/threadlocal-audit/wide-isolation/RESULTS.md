# Wide30 depth and serializer component isolation: actual Workbench results

## Verdict

No component demonstrated a wide30 completion or first-batch improvement. Depth alone was 1.59% lower completion latency pooled, serializer alone 4.33% slower, and the stack 1.55% slower than base; all intervals cross zero. Adding serializer to depth was 3.19% slower pooled with three of four pairs adverse, still inconclusive. The fourth block materially affects serializer/base and stack/base; it remains included. No synergy is established and no cause is assigned from pooled timing. Prior narrow benefits remain separate evidence; do not generalize either outcome across widths.

The workload is real on-disk LMDB with synthetic RDF data, actual RDF4J Server binary output, actual Workbench Java decoding and NDJSON serialization, and a Node streaming client. It excludes browser rendering, concurrent QPS, remote-network effects and the historical wide eight-million-row fixture.

## Immutable arms

- **base**: `5ac89b5c137030fea1aa6aa7160984423d7f4cd5`.
- **depth**: `0750336e71ab0ca0d216ce9c36483a6e9063337d`.
- **serializer**: `e2dc1ea1fa1f86fe07e29330df6f82cd4d1be8b8`.
- **stack**: `d94617b37d2142e3226bd71d29916c7588675b1c`.

All arms use the same reconstructed frozen server/dependency stack, pinned PR #6440 writer, compact identity mapping, memo/IR flags and full 30-slot 100,000-row response. The serializer layer contains only three WorkbenchPageResultWriter family classes. Its pre-edit three-class family was byte-identical to the actual frozen common Workbench JAR. Read-resource holder, cancellation changes, initializer null-slot restoration and the grouped test-only observer are excluded. The archive branch is not a runtime identity.

## Protocol and qualification

One prespecified four-block Latin square, four independent fresh JVMs per arm. Each fork completed 60 warmups and 20 measured sequential queries: 1,280 requests total, 320 measured. Every request passed compiled IR, memo, binary MIME, row count, no-spill, ledger cleanup and class-origin checks. Four final untimed qualification queries (with four earlier setup smokes preserved) validated every term, uniqueness and dual checksums. All samples were retained; no trimming or additional clean round. Startup and client setup are outside POST-to-EOF latency.

Correctness: 1,561 Workbench module tests passed, final focused suite 25 passed, and isolated base+serializer/depth+serializer each 25 passed. These test scope statements are not a whole-repository pass. Exact-byte compatibility, failure/flush/streaming behavior and prepack ordering were independently reviewed.

## Clean means

| Arm | EOF ms | First rows-batch ms | Derived rows/s | Fork EOF range ms |
|---|---:|---:|---:|---:|
| base | 2343.196 | 35.704 | 42677 | 2234.983–2443.867 |
| depth | 2305.897 | 35.500 | 43367 | 2213.305–2457.191 |
| serializer | 2444.546 | 40.263 | 40907 | 2255.733–2861.669 |
| stack | 2379.525 | 39.311 | 42025 | 2233.191–2573.210 |

Rows/s is the reciprocal of sequential full-response latency, not concurrent serving capacity.

| Contrast | EOF reduction % | Paired reductions % | Descriptive 95% interval % |
|---|---:|---|---|
| stack-versus-depth | -3.193 | -0.90, +0.32, -7.44, -4.72 | -8.94 to +2.36 |
| serializer-versus-base | -4.325 | +0.41, +1.00, +7.70, -28.04 | -30.55 to +17.32 |
| depth-versus-base | 1.592 | +6.08, +0.72, +8.65, -9.94 | -12.05 to +13.63 |
| stack-versus-base | -1.550 | +5.23, +1.04, +1.85, -15.13 | -16.40 to +11.56 |
| stack-versus-serializer | 2.660 | +4.84, +0.04, -6.33, +10.08 | -9.40 to +12.82 |
| serializer-versus-depth | -6.013 | -6.03, +0.28, -1.04, -16.46 | -18.12 to +5.57 |

Factorial geometric interaction ratio: **0.993**, descriptive 95% interval **0.788–1.251**. Values below one suggest supermultiplicative latency reduction, but this wide interval crosses one. Intervals use paired log fork means and t with three degrees of freedom; they are unadjusted for multiple contrasts and do not establish equivalence.

## All independent fork means

| Fork | Arm | EOF ms | First rows-batch ms |
|---|---|---:|---:|
| matched-01-base | base | 2356.483 | 38.094 |
| matched-02-depth | depth | 2213.305 | 34.159 |
| matched-03-stack | stack | 2233.191 | 34.830 |
| matched-04-serializer | serializer | 2346.753 | 37.347 |
| matched-05-depth | depth | 2320.593 | 36.160 |
| matched-06-serializer | serializer | 2314.031 | 36.609 |
| matched-07-base | base | 2337.452 | 35.167 |
| matched-08-stack | stack | 2313.094 | 35.121 |
| matched-09-serializer | serializer | 2255.733 | 34.191 |
| matched-10-stack | stack | 2398.606 | 43.596 |
| matched-11-depth | depth | 2232.497 | 32.606 |
| matched-12-base | base | 2443.867 | 35.753 |
| matched-13-stack | stack | 2573.210 | 43.700 |
| matched-14-base | base | 2234.983 | 33.802 |
| matched-15-serializer | serializer | 2861.669 | 52.905 |
| matched-16-depth | depth | 2457.191 | 39.073 |

## Convergence and limitations

| Arm | First measured third ms | Middle third ms | Last third ms |
|---|---:|---:|---:|
| base | 2317.916 | 2362.763 | 2351.705 |
| depth | 2294.798 | 2278.364 | 2340.594 |
| serializer | 2498.603 | 2522.893 | 2323.336 |
| stack | 2377.597 | 2306.210 | 2444.295 |

Longer fixed warmup does not automatically prove convergence; measured thirds above retain evidence of drift or variation. We retain the full prespecified interval rather than select its faster tail. Four forks per arm constrain precision; request samples within a fork are not independent replications. Prior narrow HTTP improvements remain separate evidence and do not establish a wide-shape improvement.

Whole-JVM GC logs include startup/warmup, not query-attributed pauses. Heap-used before/after snapshots are not allocation or retained heap. RSS was not collected; no memory-capacity conclusion follows. The browser remains unmeasured following the prior policy-blocked launch; no retry or bypass occurred. Node CPU is separately sampled in diagnostic runs only, over V8 main-isolate samples spanning client process lifetime including session setup/metrics, not clean POST-only or complete native-worker CPU.

## Evidence

`summary.json` retains every fork and index, GC summaries, first application-query values and all contrasts. `runs/matched-*` retain raw request metrics, commands and logs. `protocol.json`, source IDs, class manifests and the frozen common manifests establish provenance. Separate JFR results are reported in `HTTP-PROFILES.md`, never pooled into the clean timing population.

## First-row contrasts

Positive reductions mean lower latency. These are separate from full completion; all intervals remain descriptive and unadjusted.

| Contrast | Reduction % | Paired reductions % | 95% interval % |
|---|---:|---|---|
| stack-versus-depth | -10.74 | -1.96, +2.87, -33.70, -11.84 | -38.04 to +11.84 |
| serializer-versus-base | -12.77 | +1.96, -4.10, +4.37, -56.51 | -60.51 to +23.00 |
| depth-versus-base | 0.57 | +10.33, -2.82, +8.80, -15.59 | -19.85 to +17.74 |
| stack-versus-serializer | 2.36 | +6.74, +4.06, -27.51, +17.40 | -31.99 to +26.45 |
| stack-versus-base | -10.10 | +8.57, +0.13, -21.94, -29.28 | -42.07 to +15.55 |

## Published evidence packaging

`evidence/index.json` and ordered pool chunks reconstruct every textual run artifact losslessly. Concatenate pool lines by index, or for compactJsonFrom log entries compact the reconstructed JSON outside quoted strings and append a newline. All exact-byte SHA-256 round trips are verified by package-evidence.py. Originals remain local; raw JFR and huge full-stack exports are excluded, compact summaries included.

## Measured framing and cache observations

Every measured request in all four arms emitted 581 rows records, with 32 rows and 41,615 decoded UTF8 bytes in the first rows record, and Content-Encoding gzip. Bytes count decoded response stream data, not compressed wire bytes. No measured framing difference was observed. The one initial depth qualification with 582 rows records is retained but not pooled into clean timings.

Every measured query recorded zero Janino compilations, one cache hit and grouped tracker, 100,000 primitive DISTINCT rows and zero spills. No observed counter drift explains timing variation; this is not proof of byte-identical generated or JIT machine code. All per-query counters/framing remain in summary.json and raw evidence.

## Client-side attribution caution

Separate Node V8 profiles expose substantial delimiter-search work in this benchmark client: source-line ticks for repeated carry.indexOf(newline) are roughly twice JSON.parse ticks across all arms. See HTTP-PROFILES.md. This suggests shared width/chunk-sensitive endpoint overhead, not a proven cause of arm differences. The clean numbers remain valid for the exact current Node-consumed pipeline, but must not be presented as server-only throughput or real-browser behavior. No parser change or timing rerun occurred.
