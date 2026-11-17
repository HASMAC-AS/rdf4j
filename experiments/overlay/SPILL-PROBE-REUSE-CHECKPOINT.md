# Membership miss-slot reuse experiment

## Description and plan

Default-off `rdf4j.lmdb.expressionDomain.reuseMembershipMissSlot` reuses the empty index slot already found by an exact membership miss. Only the synchronized membership lookup→insert path supplies the hint; expression evaluator callbacks never supply it. Index growth invalidates the hint by changing capacity. Full-key equality, payload format, ledger admission, cancellation and cleanup remain unchanged.

Completed: deterministic typed-seam tests, red/green, frozen candidate snapshot.
Completed: three matched-workload 8-million-row candidate measurements and route verification.
Next (not started): repeated interleaved baseline/candidate comparison to resolve latency variance.

No append-offset cache, pre-sizing, or additional per-row allocations are included. The test seed constructor is package-private; ordinary production constructors still draw exactly one ThreadLocalRandom seed per domain. Existing file-factory seam supplies real counting files; no reflection or private-state manipulation.

## Test-first evidence

`spill-probe-red.log`: 31 tests, one expected failure: empty slot expected 1 index read, actual 2. The same selection passed 31/31 with reuse enabled (`spill-probe-on.log`) and disabled (`spill-probe-off.log`), with bulk record I/O enabled in both.

Hit proofs: empty-slot insertion 2→1 index reads; deterministic three-slot collision chain 6→3; exact duplicate remains three index reads. Growth test places 48 keys in one cluster, then chooses a key whose correct bucket changes from 48 to 112 after doubling. Every exact key remains a duplicate afterward, and representative iteration matches all 49 inserted keys. Partial index-write failure closes files and releases the ledger without incrementing membership size. Existing width 0/1/30/64, truncated payload, partial payload write, cancellation, expression outcomes and Kind.VALUE tests remain green.

Prior frozen bulk snapshots are untouched. Candidate `probe-segmented-classes` copies `bulk-segmented-classes` and replaces only `NativeExpressionDomain*.class`. Benchmark script uses the original fixture, 5 GiB heap, default 640 MiB ledger, no warmups, compiled IR, memo + partial-ready + segmented wide DISTINCT + bulk record I/O.

## First measurement, with explicitly recorded witness correction

First candidate query: 103,252.646 ms vs frozen segmented+bulk baseline 118,629.758 ms (15.377 s, 13.0% lower in this single comparison). Exact 8,000,000 rows/checksum -3441391656945048864; compiled IR; 8,000,000 successful bulk records; 7,999,934 reused membership slots. Ledger peak 671,067,808 B, cap 671,088,640 B; 20 files created/deleted, zero ledger after close. No payload format or memory-budget change.

The first wrapper mistakenly expected reuse only for rows after primitive migration. Migrated rows also enter Membership.add. The timed query and ordinary correctness/cleanup checks passed, then the wrapper's wrong exact-count assertion failed. Original evidence is retained in `probe-eight-million-segmented-witness-correction.log`. Corrected the expected reuse count to 7,999,934 and reran the identical frozen candidate; do not describe the first process as wholly green. This number excludes pre-spill in-memory membership entries, the first spill-trigger insertion, and capacity-changing insertions. Timings remain single cold runs rather than a controlled ABBA series.

Second identical candidate run: **158,900.140 ms**, exact reuse count 7,999,934 and all query/cleanup checks passed. This large variance means the first 13% observed improvement is **not established as a stable performance gain**. Repeated matched baseline/candidate runs are needed; index-probe reduction is deterministic, total latency is not. Both measurements must be retained, not cherry-picked. The final reusable wrapper checks `0 < reusedSlots <= bulkRecords`, while exact count reduction and growth handling are proven by focused tests.


Final bounded-wrapper run: **129,194.591 ms**, exact 8m checksum, 8m bulk writes, 7,999,934 reused slots, 20 files cleaned and ledger zero. Peak 671,067,808 B unchanged. `probe-eight-million-segmented.log` is fully green; `probe-eight-million-segmented-repeat.log` preserves the second run. Three candidate timings are 103.253 / 158.900 / 129.195 s versus one earlier bulk-only baseline 118.630 s. **No reliable end-to-end improvement claim is warranted yet.**

Independent probe-on / bulk-off configuration also passes 31/31 (`spill-probe-no-bulk.log`). `run-probe-focused.sh true false` reproduces it; `true true` and `false true` reproduce the other two green configurations. `git diff --check` passed. No publication or commit performed by this worker.

## Follow-up stability check completed

See `SPILL-PROBE-ABBA.md` and `probe-abba-summary.csv`: identical frozen candidate classes, off/on/on/off, 128.643 / 124.014 / 116.193 / 118.804 s. Mean observed reduction 2.93%, with lower system CPU but higher user CPU, similar RSS, sub-1.1-second GC pause totals, and declining physical reads across order. This supports a modest opt-in candidate, not the initially suggested 13% general speedup. All four query and route witnesses passed. No production code changed during the series.
