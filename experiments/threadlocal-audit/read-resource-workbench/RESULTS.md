# Read-resource holder: actual Workbench results

## Verdict

The new read-resource holder did not demonstrate an incremental HTTP latency or throughput benefit on depth. Pooled completion times were effectively identical; the four paired blocks were variable. Neither the resource-only arm nor the factorial interaction establishes a win. Do not promote this candidate on performance grounds from this experiment.

The workload is real on-disk LMDB with synthetic RDF data, actual RDF4J Server binary output, actual Workbench Java decoding and NDJSON serialization, and a Node streaming client. It excludes browser rendering, concurrent QPS, remote-network effects and the historical wide eight-million-row fixture.

## Immutable arms

- **base**: `5ac89b5c137030fea1aa6aa7160984423d7f4cd5`.
- **depth**: `0750336e71ab0ca0d216ce9c36483a6e9063337d`.
- **resource**: `90fe4e3258f55cea3641e78a087fcfa3d528afc3`.
- **both**: `edaff8133c80043526d14285eca1cce05a3f6021`.

All arms use the same reconstructed frozen server/dependency stack, pinned PR #6440 writer, compact identity mapping, memo/IR flags and full six-slot 100,000-row response. The holder layer contains only 105 LmdbSailStore family classes. Its pre-edit 104-class family was byte-identical to incumbent. Cancellation changes, initializer null-slot restoration and the grouped test-only observer are excluded. The archive branch is not a runtime identity.

## Protocol and qualification

One prespecified four-block Latin square, four independent fresh JVMs per arm. Each fork completed 60 warmups and 30 measured sequential queries: 1,440 requests total, 480 measured. Every request passed compiled IR, memo, binary MIME, row count, no-spill, ledger cleanup and class-origin checks. Four separate untimed qualification queries validated every term, uniqueness and dual checksums. All samples were retained; no trimming or additional clean round. Startup and client setup are outside POST-to-EOF latency.

Correctness: eight focused tests and 110 integration tests passed separately on resource-only and combined, with three expected gated skips in each integration selection. The three corrected gated fixtures then passed separately on both arms using a test-only observer that is absent from timed classpaths. This is scoped validation, not a whole-repository pass.

## Clean means

| Arm | EOF ms | First row ms | Derived rows/s | Fork EOF range ms |
|---|---:|---:|---:|---:|
| base | 474.635 | 37.297 | 210688 | 449.963–520.248 |
| depth | 480.126 | 35.505 | 208279 | 468.097–494.541 |
| resource | 489.107 | 36.868 | 204454 | 453.974–524.486 |
| both | 480.122 | 35.506 | 208280 | 464.441–522.156 |

Rows/s is the reciprocal of sequential full-response latency, not concurrent serving capacity.

| Contrast | EOF reduction % | Paired reductions % | Descriptive 95% interval % |
|---|---:|---|---|
| both-versus-depth | 0.001 | +5.76, +2.47, +0.78, -9.20 | -10.45 to +9.65 |
| resource-versus-base | -3.049 | -16.56, -0.56, +12.74, -10.27 | -26.03 to +15.74 |
| depth-versus-base | -1.157 | -9.91, -2.00, +10.02, -4.39 | -15.96 to +11.51 |
| both-versus-base | -1.156 | -3.57, +0.51, +10.73, -13.99 | -18.87 to +13.86 |
| both-versus-resource | 1.837 | +11.14, +1.06, -2.31, -3.38 | -9.63 to +12.04 |
| resource-versus-depth | -1.871 | -6.06, +1.42, +3.02, -5.63 | -9.50 to +5.49 |

Factorial geometric interaction ratio: **0.969**, descriptive 95% interval **0.776–1.211**. Values below one suggest supermultiplicative latency reduction, but this wide interval crosses one. Intervals use paired log fork means and t with three degrees of freedom; they are unadjusted for multiple contrasts and do not establish equivalence.

## All independent fork means

| Fork | Arm | EOF ms | First row ms |
|---|---|---:|---:|
| matched-01-base | base | 449.963 | 33.912 |
| matched-02-depth | depth | 494.541 | 35.029 |
| matched-03-both | both | 466.040 | 34.782 |
| matched-04-resource | resource | 524.486 | 37.009 |
| matched-05-depth | depth | 479.688 | 34.293 |
| matched-06-resource | resource | 472.877 | 35.326 |
| matched-07-base | base | 470.262 | 39.134 |
| matched-08-both | both | 467.852 | 35.952 |
| matched-09-resource | resource | 453.974 | 36.726 |
| matched-10-both | both | 464.441 | 34.343 |
| matched-11-depth | depth | 468.097 | 36.387 |
| matched-12-base | base | 520.248 | 41.989 |
| matched-13-both | both | 522.156 | 36.945 |
| matched-14-base | base | 458.066 | 34.153 |
| matched-15-resource | resource | 505.092 | 38.410 |
| matched-16-depth | depth | 478.179 | 36.312 |

## Convergence and limitations

| Arm | First measured third ms | Middle third ms | Last third ms |
|---|---:|---:|---:|
| base | 456.276 | 489.855 | 477.773 |
| depth | 474.987 | 472.361 | 493.031 |
| resource | 488.077 | 493.218 | 486.027 |
| both | 493.041 | 478.339 | 468.986 |

Longer fixed warmup did not prove convergence: the combined arm still decreases across measured thirds. We retain the full prespecified interval rather than select its faster tail. Four forks per arm constrain precision; request samples within a fork are not independent replications. Earlier embedded depth gains do not establish a gain through this HTTP pipeline.

Whole-JVM GC logs include startup/warmup, not query-attributed pauses. Heap-used before/after snapshots are not allocation or retained heap. RSS was not collected; no memory-capacity conclusion follows. The browser remains unmeasured following the prior policy-blocked launch; no retry or bypass occurred.

## Evidence

`summary.json` retains every fork and index, GC summaries, first application-query values and all contrasts. `runs/matched-*` retain raw request metrics, commands and logs. `protocol.json`, source IDs, class manifests and the frozen common manifests establish provenance. Setup correction is recorded in `SETUP-NOTES.md`; it occurred before any comparison. Separate JFR results will be added in `HTTP-PROFILES.md`, never pooled into the clean timing population.

### Published raw-evidence packaging

`evidence/index.json` and its ordered `poolChunks` losslessly reconstruct all 3,684 textual run files. Concatenate indexed UTF-8 pool lines for ordinary files. For `compactJsonFrom` log entries, compact the referenced reconstructed JSON by removing whitespace only outside strings, then append a newline. Verify every recorded SHA-256. `package-evidence.py` verified exact byte round-trips for all 32,022,432 original bytes; originals remain local. Raw JFR and multi-gigabyte full-stack exports are excluded; compact full-stack analysis summaries are published separately.
