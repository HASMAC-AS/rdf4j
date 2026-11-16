# Resolver variation: two directly observed factor weak-cache fallbacks

The fixed diagnostic completed **four fresh JVMs and all 52 queries**. Both omission JVMs produced one query with 32,834 resolver calls, at warmup iteration `-7`, and each event was fully accounted for by **one cleared weak referent in the `r1` factor cache**. The other 50 queries reported 32,833 resolver calls. Every result, route, counter-reconciliation, and cleanup check passed. An independent calculation from the raw files confirmed all 52 queries, the existing 130-entry runtime, the serial execution intervals, and agreement with the driver summary; its evidence is preserved under `../../benchmark/independent-resolver-probe-v1/`.

This directly establishes the fallback mechanism in these two newly instrumented queries. The stopped timing study recorded the same extra-call pattern without these branch counters, so the new evidence makes weak-cache fallback a concrete explanation to consider for that earlier event. **It does not prove the original event's cause or reopen the failed timing comparison.** No timing, allocation, GC-performance, or natural miss-rate estimate is accepted from this diagnostic.

## Fixed scope and source identity

The predeclared order was all, omit, omit, all. Every JVM ran eight warmups and five further observed queries, with all records retained. The unchanged harness labels the latter five as `measurement`; they are diagnostic observations here. Execution stopped at the declared four-JVM limit without replacement or extension.

Both arms used the same already compiled masked diagnostic runtime: 130 classpath entries, original factor pages, DENSE coordinate transport, ordinary reuse, normal AUTO selection, full consumption, the 64-root/128-left/128-right/tuple-width-two shared-tail fixture, a fixed 1 GiB G1 heap, a 1 GiB global query ledger, and a 512 MiB per-query ledger. The mask was empty for all and `l0,l1` for omit. The only added JVM facility was normal GC/reference logging, with no forced GC or retention/source changes.

The runtime descriptor is `../generated/publication-overlay-v1/runtime.json`. The generated materializer source SHA-256 is `95660f4dab2bd6d4ff947ee3f01ed3c61e6d00ace24722c27e8ff86aa782e317`. Its underlying publication-overlay descriptor SHA-256 is `c46fe65a3515872535b55d0c979bf5f84e4d90b59168c86dd3bc14febaa244b2`. The new protocol SHA-256 is `0ba7dd6c7bce031e8c7495e6be958c13444a670ada1b966dde2fdfcb1f54ac5d`; the driver SHA-256 is `ac1f446f5e9a37d52140bccc0a949d41ca9fbb98cee258965d27efd153b2d5e4`. Source files, exact commands, the common runtime, and prior same-arm counter references were frozen before execution. The full receipts and source patch accompany this report.

## Every declared JVM

| Run | Publication mask | Queries at 32,833 calls | Queries at 32,834 calls | Direct factor weak branches | Ordinary weak branches | Result/accounting checks |
|---|---|---:|---:|---:|---:|---|
| `00-all` | Empty | 13 | 0 | 0 | 0 | Passed, 13/13 |
| `01-omit` | `l0,l1` | 12 | 1, iteration `-7` | 1 | 0 | Passed, 13/13 |
| `02-omit` | `l0,l1` | 12 | 1, iteration `-7` | 1 | 0 | Passed, 13/13 |
| `03-all` | Empty | 13 | 0 | 0 | 0 | Passed, 13/13 |

These are counts of the retained observations, not estimates of a natural miss rate. Every JVM used the interpreted tier for its first query and the compiled tier for its other 12 queries: four interpreted and 48 compiled queries in total. Both extra-call observations used the compiled tier. The 20 further observed queries all had 32,833 calls; that fact does not create an accepted subset of the stopped timing study.

Every query produced 1,048,576 complete rows, 6,291,456 binding reads, and checksum `8327963502356915200`. The exact independent bag/membership oracle passed throughout. All queries used normal AUTO/native IR with active, current-revision adjacency; root audits found no hosted generic evaluation. Ordinary and factor witness misses were zero in all 52 queries. Factor-page and coordinate admission refusals were also zero.

## Exact accounting for both extra-call queries

The projection is `s,a,l0,l1,r0,r1`, with identity aliases verified in every owner record. The changed target was requested target 5, canonical target 5, named `r1`. Every counter on the other five targets matched the prior same-arm 512 MiB reference exactly. The two affected queries have the same target changes:

| `r1` counter | Same-arm reference | Each affected query | Difference |
|---|---:|---:|---:|
| Factor hits | 1,040,384 | 1,040,383 | −1 |
| Terminal factor weak misses | 0 | 1 | +1 |
| Remember calls | 8,192 | 8,193 | +1 |
| Publication method entries | 8,192 | 8,193 | +1 |
| Completed publications | 8,192 | 8,193 | +1 |
| Actual publication writes | 8,192 | 8,193 | +1 |
| Coordinate attempts | 1,056,767 | 1,056,768 | +1 |
| Successful coordinate captures | 1,056,767 | 1,056,768 | +1 |

The existing `findFactor` weak branch dereferenced a matching non-null weak-reference cell, obtained null, cleared that cell, and returned null. The resolved replacement was subsequently remembered and written into the existing page. No extra page was admitted for the repair, and no ordinary weak or witness branch was involved.

The final factor outcomes are mutually exclusive. Their exact decomposition is:

| Final factor miss outcome | Unaffected all query | Unaffected omit query | Each affected omit query |
|---|---:|---:|---:|
| Initial rejection | 69 | 69 | 69 |
| Absent page | 16,636 | 24,700 | 24,700 |
| Absent/mismatched cell | 16,128 | 8,064 | 8,064 |
| Cleared weak referent | 0 | 0 | 1 |
| Failed witness | 0 | 0 | 0 |
| **Sum: resolver calls** | **32,833** | **32,833** | **32,834** |

The higher page-miss count in unaffected omit queries is the expected lookup consequence of omitted left-side publication; it is present in the prior same-arm reference and is not an admission-refusal count. Initial rejection combines missing factor storage, coordinate rejection, and owner/activation rejection; this helper does not separate those subcauses.

Ordinary hits remained 4,177,855 in every query. Factor hits were 2,080,768 in an unaffected query and 2,080,767 in either affected query. Thus each affected query reconciles exactly as:

`4,177,855 ordinary hits + 2,080,767 factor hits + 32,834 resolvers = 6,291,456 binding reads`.

Find calls equalled binding reads, remember calls equalled resolver calls, and every target's remember count equalled its exclusive final-factor miss sum. In general, ordinary weak misses can overlap factor weak misses or be recovered by factor hits; the two earlier and later partitions must not be added as one-for-one resolver increments. Here the ordinary weak count was zero and the sole changed terminal outcome was the observed `r1` weak miss.

## Cleanup and memory accounting

Every query released its explicit page grants, and every final query-ledger endpoint was zero. The weak-cell repairs changed no page admission, explicit page-byte grant/release, or ledger peak relative to other queries in the same arm.

| Arm | Pages admitted/released per query | Explicit page bytes granted/released per query | Peak query-ledger charge |
|---|---:|---:|---:|
| All | 16,640 | 272,629,760 | 272,948,260 |
| Omit `l0,l1` | 8,320 | 136,314,880 | 136,502,308 |

These are ledger charges and explicit page events. Ordinary-table, directory, and memo claims are close-time snapshots, not independently counted allocations/releases. Page occupancy counts non-null weak-reference slots without dereferencing them; it does not measure live referents. These observations do not measure physical heap retention.

All four GC logs contain normal G1 and reference-processing messages. They are intentionally unaligned to individual query/miss boundaries and do not identify the cleared referent. No explicit `System.gc()` cause appeared in the logs. The helper's counters, primitive page snapshots, and JSON output alter allocation and GC pressure. In particular, the all arm closes twice as many pages as omit in these fixtures, so the instrumentation burden also differs. No comparison of GC counts, costs, or miss rates is made.

## Implication and evidence locations

The new evidence establishes that successful query execution under unchanged ownership and lookup code can include a cleared factor weak referent, one extra resolver call, and a correctly republished value. There is no unexplained resolver increment in either affected diagnostic query. This is useful evidence for a future prospective experiment's accounting rules; it is not a general proof that ownership is correct in every strategy or that either publication policy is faster.

The original timing protocol remains failed and immutable. Its event lacks the new branch evidence, so its precise cause remains unresolved. A future performance design would need to predeclare how expected weak-cache fallback affects its mechanism checks, while continuing to require correct results, routes, ownership accounting, and cleanup. This diagnostic selects no new implementation and accepts no performance result.

The complete query-by-query decompositions, source identities, and raw references are in `probe-v1/summary.json` and each run's `analysis.json`. The affected raw owner records are `probe-v1/runs/01-omit/page-records/owner-2.jsonl` and `probe-v1/runs/02-omit/page-records/owner-2.jsonl`; their corresponding samples are iteration `-7` in each run's `samples.jsonl`. All 52 owner records, four GC logs, commands, process results, root audits, and raw harness samples are preserved. `output-sha256.json` freezes the diagnostic output files, while `freeze-receipt.json` records the prospective source/protocol hashes. The earlier hypothesis and limitations are preserved separately in `investigation-note.md`.
