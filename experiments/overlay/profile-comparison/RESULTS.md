# Matched profiling: remaining materialization overhead

## Scope

Real LMDB execution of the synthetic 2.136-million-triple / 8-million-row, 30-column OPTIONAL fixture. Not the user's private query, not HTTP/Workbench. Upstream main `be0c4c724f582e22d1a2936b87bb24969cc9154a`; combined frozen production `b8104ecf51d09cae7a2e4a827c0b19844beb9623`. Same source harness, heap and configured budgets as main-comparison. Main does not charge its wide DISTINCT object table to the ledger, so nominal budgets are not equivalent enforcement.

JFR records only the query interval after maintenance readiness; analysis filters CPU/allocation events to the main query thread. CPU sampling is 10 ms; allocation events have statistical weights. Categories choose the nearest matching meaningful stack frame; they are a documented diagnostic grouping, not exact wall-time percentages. Allocation origin is first RDF4J frame, so shared helper costs can be grouped rather than caller-specific. Raw recordings stay local because they can include machine/JVM metadata; sanitized aggregate stack reports are preserved.

## No-writer profiles

| | Main | Combined |
|---|---:|---:|
| Query time, one profiled cold execution | 69.004 s | 39.436 s |
| Query-thread allocated bytes | 53.075 GB | 32.180 GB |
| Main-thread CPU samples | 6,204 | 3,675 |
| Rows | 8,000,000 | 8,000,000 |
| Lexical checksum | -3441391656945048864 | -3441391656945048864 |
| Spills | 0 | 0 |

Both compiled IR. Combined grouping fired and memo had 218.75 million hits in 220.8 million probes. These one-off profiled timings are **not** a replacement for the independent repeated unprofiled comparison. Profiling perturbation and cold-run variation matter. Some short analysis/tool compilation overlapped these diagnostic runs; no competing benchmark ran.

### CPU stack categories (sample share)

| Category | Main | Combined |
|---|---:|---:|
| Value projection / memo / lazy value creation | 25.52% | 27.46% |
| Lifetime, readiness and read context | 20.55% | 27.05% |
| Query kernel / row production | 12.04% | 18.64% |
| Stable row snapshot | 7.27% | 11.89% |
| DISTINCT | 26.77% | 3.62% |
| Value decode | 1.00% | 1.82% |
| Checksum consumer / harness | 6.85% | 9.52% |

An increased percentage does not imply increased absolute cost; the combined query has substantially fewer total samples. Main's `ValueStore.getLazyValue` is 16.09% of leaf samples. Combined's `NativeValueMemo.value` is 14.91%; `findMaterializedDuplicate` remains 3.35%. Packed adjacency searches and thread-local/read-context operations also remain visible.

### Allocation evidence

- Main `ValueStore.positionedRevision`: approximately **14.73 GB** of sampled allocation weight; combined approximately **0.159 GB**. This supports keeping the memo rather than discarding it because its lookup remains hot.
- Combined `MapBindingSet.addBinding` plus `MutableBindingSet.addBinding`: approximately **16.66 GB**, **51.6%** of sampled allocation weight. Main approximately **15.92 GB**. Thus row-copy allocation is largely unchanged and becomes the leading remaining allocation source.
- Combined `NativeProjectedBindingSet` construction: approximately **3.23 GB**, 10%.
- Read-context/replay/external-initializer scope machinery remains several GB in aggregate.

Source explains the row-copy signal: `SailResultValueExtractor.BINDING_SETS.snapshot` allocates a `MapBindingSet` and adds each named binding to stabilize results before preparation. This preserves a necessary guarantee for reused mutable source rows; deleting it is not safe. `NativeProjectedBindingSet.findMaterializedDuplicate` scans the complete projected row for each unresolved value.

## Recommendations

1. **Keep the memo and readiness improvements.** They remove large repeated revision/lazy-value work; the remaining memo cost is not evidence that memo is a net loss.
2. **Prototype a compact immutable/stable snapshot for known native rows**, sharing immutable projection-name layout and copying only the value/ID arrays as needed. Avoid rebuilding a hash map, entries and binding objects for every row. Preserve ownership, retained-row independence, replay, unbound values and mutable-source compatibility.
3. **Reduce duplicate scans and memo access cost**, for example precomputed duplicate-slot structure when applicable or a cheaper per-row lookup, keeping exact ID/position/snapshot semantics. Measure lookup overhead separately from saved resolutions.
4. **Reduce per-row read-context/lifetime scope overhead** only after preserving replay and mutation-safety tests. It remains a real cost, not merely decoding.
5. **Do not prioritize overlay lexical decoding on this fixture**: it accounts for little of the measured query CPU after reuse. Query production/search is next after the above, not evidence everything remaining is raw copying.

## Writer caveat and ongoing ablation

Initial writer profiles exposed a benchmark artifact: a system-property lookup was executed for each output call in the byte-counting sink. Those initial writer timings are excluded from performance claims. The sink now caches that switch once, and clean matched writer profiles are running. Both initial outputs contained exactly 3,637,322,543 bytes, with matching tuple checksums. The writer path adds the actual binary writer to the same lexical checksum consumer, so it isolates incremental encoding work but does not simulate network backpressure or eliminate the harness's own string accesses.
