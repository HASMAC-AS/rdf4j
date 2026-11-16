# Sparse factor pages reach the query-memory cap

The diagnostic confirms that the shared-tail fixture's near-256 MiB ledger peak comes from sparsely occupied factor pages. The measured query admitted 16,364 pages. Of those, 16,112 held exactly one nonnull weak-reference slot; only 252 had all 64 slots occupied. Thus 98.46% of pages were singleton pages, and 3.08% of their combined slot capacity was occupied. These are stored-reference observations, not estimates from the fixture geometry.

Every admitted page appears in the final key table. There were no namespace retirements or nonempty page clears before final close. All page namespaces were the stable adjacency namespace `1`; the four target groups were `6`, `10`, `14`, and `18`.

| Target | Admitted pages | Occupied cells per page | Charged page bytes | Successful factor-cache hits |
|---|---:|---:|---:|---:|
| `l0` | 126 | 64 | 2,064,384 | 0 |
| `l1` | 8,048 | 1 | 131,858,432 | 0 |
| `r0` | 126 | 64 | 2,064,384 | 1,024,128 |
| `r1` | 8,064 | 1 | 132,120,576 | 1,024,128 |
| **Total** | **16,364** | **32,240 occupied cells** | **268,107,776** | **2,048,256** |

The left-side pages alone charged 127.71875 MiB and produced no successful factor-cache hits in this query. This does not mean their values received no reuse: the ordinary last-value memo recorded 4,177,855 hits across the complete projection. It means the extra factor-page layer did not supply a value for either left-side target in this execution. The right-side page cache demonstrably did supply repeated values.

One 16,384-byte payload reservation was refused at a ledger total of 268,426,276 bytes, leaving 9,180 bytes below the 268,435,456-byte limit. The first refused key was target `l1`, namespace `1`, group `10`, parent `8049`, tuple column `0`, page index `0`. The refusal latch subsequently suppressed 67,583 missing-page publication calls. These are calls, not unique refused page keys. No directory or coordinate-memo reservation failed.

Accounting reconciles exactly. All 16,364 admitted pages were closed, and their 268,107,776 granted bytes were observed immediately before release. The directory's 525,888 cumulative granted bytes were all released; its final claim was 262,272 bytes and its maximum overlapping old/new resize claims were 393,472 bytes. The coordinate memo's 1,600 bytes were also released. Every query ended at zero process-ledger bytes. These charges are reservation accounting, not a measured retained-heap size.

All four queries passed the independent exact bag, count, type, correlation, duplicate, and checksum checks. The measured query used normal AUTO compiled IR with a native outer root. Direct adjacency was configured as PREFER and readiness was awaited; this harness did not collect individual adjacency lookup-hit counts. The original 127 runtime entries and the shared fixture/harness entry remained unchanged; only the reviewed two-class diagnostic overlay was added.

The next candidate worth evaluating is a compact initial representation for a sparsely populated factor page, with optional charged promotion when additional positions arrive. That directly targets the measured 64-cell reservation for one occupied position. It should preserve the existing coordinate key, weak-reference and semantic-witness rules, and refusal fallback. A separate alternative is better admission for pages that never provide a factor hit, but this execution alone cannot justify a general rule excluding left-side columns. Neither change has been implemented or timed here.

The full observations and reconciliation checks are in `analysis.json`; raw page records are in `page-records/owner-4.jsonl`; all four query records are in `run/samples.jsonl`. The instrumentation changes counters, close-time allocation, output, and weak-reference observation. Its latency, allocation and live-referent counts must not be used to rank implementations. No further JVM was run after this fixed diagnostic.
