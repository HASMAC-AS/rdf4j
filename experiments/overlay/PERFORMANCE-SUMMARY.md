# Measured outcome: memo baseline, wide DISTINCT and readiness

All comparisons below keep the reconstructed weak per-iteration memo enabled. This is not the unavailable historical memo implementation. All experimental flags remain opt-in on this WIP branch.

## Exact-scale synthetic workload

2,136,000 triples;8,000,000 distinct output rows;30 projected variables with nested OPTIONAL;adjacency, overlay and compiled IR;default 256k-entry value cache;5 GiB heap. Each fresh JVM performs one query after opening the same verified persisted fixture. Dataset load is excluded, no discarded query warmups. Mirrored order A/B/C/C/B/A controls ordering drift.

| Configuration | Fork timings | Mean | Versus memo baseline |
|---|---|---|---|
| Memo |57.978s,55.078s|56.528s|baseline|
| Memo + wide primitive DISTINCT |45.741s,43.990s|44.866s|20.6% faster|
| Memo + wide DISTINCT + partial-ready |43.993s,45.776s|44.884s|20.6% faster|

The combination is not measurably faster than wide DISTINCT alone. It allocates36.44 GB on the query thread versus39.94 GB for wide alone (~8.8% less); these are cumulative allocations, **not retained-memory or heap sizes**. Baseline allocates38.66 GB. Wide is faster despite allocating more than baseline because of flat-array growth; don't sell it as an allocation reduction.

All six runs returned exactly 8m rows and checksum -3441391656945048864. Every run used compiled IR. Each recorded220.8m memo probes and~218.753m memo hits (~99.1%). Wide variants switched 8m DISTINCT probes from object to primitive storage. No run was killed.

## Supporting million-row mirrored matrix

267,000 triples, 1m rows, 4 GiB heap, two warmups and four measured runs per fork; final three averaged; two forks per variant:
- Memo:6.177s.
- Memo+partial-ready:5.542s (~10.3% faster).
- Memo+wide DISTINCT:4.484s (~27.4% faster).
- Memo+both:4.548s (~26.4% faster).

Wide DISTINCT is the strongest demonstrated lever. Partial-ready reduces redundant traversal/allocation; its speed benefit is workload-dependent and not additive at 8m rows.

## What not to enable based on these results

Typed overlay decoding reduced a cold-microbenchmark's allocations but initially slowed it; a profile-guided vector bulk-copy improvement recovered only part of the loss. After correcting scalar-route integration and proving it executed, query improvement was negligible/noisy under cache pressure and absent under default cache. Keep typed overlay off. Grouped waves and deduplicated overlay APIs are correctness-tested prototypes, not performance recommendations.

## Validation and limits

75 combined focused/replay/lifetime tests passed with memo+partial-ready+wide enabled. Separate overlay and vector tests are recorded in RESULTS.md. Full module regression was not completed.

These are synthetic fixtures, not the downstream private query. There is no claim that its two-minute runtime becomes 44.9 seconds.

A diagnostic real-map-growth test found the first attempt uses irKernel but replacement selects batch. It establishes fresh memo instances/hits and correct output across actual growth before exposure, **not compiled execution on both attempts**. The forcing/replay route switch remains an explicit open issue.
