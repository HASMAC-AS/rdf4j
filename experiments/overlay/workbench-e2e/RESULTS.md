# Workbench HTTP/NDJSON throughput and latency

## Repeated matched writer result (ABBA)

Two fresh-process forks per arm, in old/new/new/old order, separated by a coordinated build-only window with no competing work during measurements. Query SHA and frozen API/LMDB fingerprints match across all four trials.

| Metric | Old writer mean (range) | PR #6440 mean (range) |
|---|---:|---:|
| First received rows | 0.994 s (0.990–0.998) | 1.049 s (1.031–1.066) |
| 1M-row window received | 21.341 s (21.140–21.542) | 23.014 s (22.499–23.529) |
| Full 8M query/count completion | 107.671 s (106.779–108.563) | 78.238 s (75.516–80.960) |

**27.3% less full completion time, 37.6% higher query throughput; no early-delivery win.** The received-window mean is 7.8% slower in these four runs. The first-row difference is ~54 ms; two forks are not enough to generalize it beyond this fixture. All four passed route, count, no-spill, and zero-retained-budget checks.

This tests #6440 combined with the prior query optimization stack, not against upstream main. Further lifetime and separately labeled improved-hash compact combinations are pending. See `ABBA-summary.json`, the four result JSON files and manifests. The earlier first-pair detail is retained below as provenance, not an additional independent dataset.

## Provisional first matched pair

Actual Server → binary HTTP → actual Workbench Java parser/page writer → streaming Node NDJSON client. Prior-best query implementation and identical flags/store/query in both runs. Only the binary writer changed to PR #6440 at `fca5653c2da8df975fac5760b457c436f3e12dc6`.

| Metric | Prior best + old writer | Prior best + PR #6440 |
|---|---:|---:|
| First received rows | 0.998 s | 1.031 s |
| Default 1M-row window received | 21.140 s | 22.499 s |
| Full 8M-row query consumed/counted | 106.779 s | 75.516 s |
| Full-query throughput | 74,921 rows/s | 105,938 rows/s |
| Received window rows | 1,000,000 | 1,000,000 |
| Compiled kernel binds | 1 | 1 |
| Interpreted binds / spill files | 0 / 0 | 0 / 0 |
| Ledger retained after close | 0 | 0 |

The first pair shows **29.3% less full-query elapsed time / 41.4% higher full-query throughput**, but **no early-delivery improvement**. It is one fresh-process trial per arm, not a conclusive repeated result. Reversed-order repeats are pending. Do not call these arms “main.” Both already include the previous query optimizations (memo, certified grouping, segmented/budgeted wide DISTINCT, partial readiness and spill I/O changes). Lifetime refinements and compact snapshots are separate variants, not silently enabled.

### Interpretation boundary

The query returns 8M rows, but Workbench's default unknown-total fetch emits a 1M-row window and continues consuming/counting all remaining rows. Therefore full completion is actual server-query/Workbench-count completion, **not delivery of all 8M rows to the client**. The roughly 1.542GB NDJSON response includes one million rows plus metadata/progress records. Small differences in total response bytes reflect progress records/timing and are not an output equality checksum.

The first pair suggests the faster writer can improve total query processing without improving initial/window delivery. The first-window phase additionally does Workbench term-to-NDJSON conversion and client JSON decoding; it is not justified to claim those are the bottleneck from timing alone.

### Correctness and version evidence

- `smoke-off-3.json`: 100 complete emitted tuples checked against the fixture including all lexical/language/null/domain values and uniqueness, plus exact 8M total consumed.
- Each timed run checks 30-column output, emitted count 1M, terminal result count 8M, completed status, forced compiled IR, memo hits, grouping admission, no spill and zero retained ledger.
- Actual server response MIME: `application/x-binary-rdf-results-table;charset=UTF-8`; client response MIME: Workbench query v2 NDJSON.
- Complete binary module variants and API/LMDB frozen classes are isolated from conflicting jars. `*-manifest.json` records actual command and content fingerprints; `boot-jar.sha256` pins all remaining dependencies and web resources. Runtime class origins are in each result.
- Measured logs: `http-a-off.json`, `http-b-on.json`; `http-pair-driver.log`. No production changes or upstream publication.

### Browser-paint limitation

A real browser harness was prepared, but both Chromium launches failed before page navigation because this executor forbids the required local Unix socket, including an approved escalated retry. No bypass is attempted. These results **do not measure DOM paint, IndexedDB storage, or browser rendering**. “First received rows” is not “first visible paint.”

### Current-head safety boundary

The production writer at current pinned head fca5653 is different from the earlier reviewed741b938 source (16.5KB→28.0KB). Earlier review/unit counts must not be represented as a review of this exact head. The benchmark is not a release/merge safety determination. Current-PR exact-value fixture verification is queued before the next combination trials.
