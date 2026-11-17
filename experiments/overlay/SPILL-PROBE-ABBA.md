# Matched membership probe-reuse stability check

## Method

Four fresh JVMs in **off/on/on/off** order, all using the **same frozen candidate production classes**, same frozen benchmark classes, same existing 2,136,000-triple fixture and compiled-IR query producing 8,000,000 distinct rows. Bulk ID record I/O remains enabled throughout. Only `rdf4j.lmdb.expressionDomain.reuseMembershipMissSlot` changes.

Common settings: `-Xms2g -Xmx5g`, default 640 MiB query ledger, memo enabled, partial-ready enabled, segmented wide DISTINCT enabled, zero warmups and one measured query per JVM. Production hash seeding remains random; no deterministic seed injected into the workload. All four ran serially under `../native-benchmark.lock`.

`run-probe-abba.sh` reproduces the series using frozen `probe-segmented-classes` and `probe-abba-test-classes`. The former is the previous segmented+bulk snapshot with only NativeExpressionDomain class files replaced by the probe candidate; the latter is a copy of compiled test classes after compiling the updated wrapper. Exact file hashes are recorded in `probe-abba-classes.sha256`; relevant source hashes in `probe-abba-source.sha256`. Source hashes were reverified after the series.

The wrapper always verifies exactly 8m bulk records. It also verifies zero reused slots with the flag off and positive reused slots no greater than bulk records with it on. Unit tests prove exact eligibility, collision chain reduction and growth invalidation.

`/usr/bin/time` is unavailable in this workspace. `time-probe-jvm.py` launches exactly one JVM subprocess, records monotonic elapsed time and `getrusage(RUSAGE_CHILDREN)` user/system CPU, peak RSS, faults and physical filesystem block counters. These are **whole-process** measurements, not query-only CPU attribution. GC logging is enabled identically in all four JVMs. `summarize-probe-abba.py` emits `probe-abba-summary.csv` from retained per-process JSON, logs and GC logs.

## All results

| Run | Reuse | Query seconds | Process elapsed s | User CPU s | System CPU s | Peak RSS KiB | GC pauses total ms | Physical input blocks |
|---|---|---:|---:|---:|---:|---:|---:|---:|
| 1 | off | 128.643 | 136.713 | 95.425 | 65.363 | 2,722,544 | 850.642 | 374,896 |
| 2 | on | 124.014 | 132.055 | 97.782 | 61.533 | 2,725,844 | 1,060.015 | 96,128 |
| 3 | on | 116.193 | 123.482 | 88.107 | 59.147 | 2,739,316 | 837.708 | 68,928 |
| 4 | off | 118.804 | 126.056 | 85.747 | 63.465 | 2,743,700 | 755.667 | 7,296 |

GC maximum pauses respectively: 61.177, 112.983, 66.998, 72.158 ms. There were 34, 33, 34, 34 pauses respectively. Physical output blocks: 29,768 / 31,448 / 29,448 / 31,464. Linux rusage block counters reflect physical I/O, not logical RandomAccessFile calls or bytes requested; they cover process setup too.

Every run returned exactly 8m rows with checksum **-3441391656945048864**, used compiled IR, wrote exactly 8m bulk payload records, created/deleted 20 spill files and returned the ledger to zero. Peak query-ledger usage remained **671,067,808 B**, below **671,088,640 B**. Reused slots were exactly **0 / 7,999,934 / 7,999,934 / 0**. Primitive/spill path rows remained 2,642,432 / 5,357,568.

## Interpretation and recommendation

Mean query time: off **123.724 s**, on **120.103 s**, a **3.620 s (2.93%)** reduction in this four-run series. Both adjacent comparisons favor reuse, but this is a small effect and two samples per mode cannot establish a general speedup, especially given earlier candidate timings of 103.253 / 158.900 / 129.195 s.

Mean system CPU fell from **64.414 s to 60.340 s** (4.074 s), while user CPU rose from **90.586 s to 92.944 s** (2.358 s). This is consistent with the proven reduction in index probing, but is not stack-level causal attribution. Peak RSS is essentially unchanged. GC pause totals under 1.1 s do not explain the earlier tens-of-seconds variation. Physical reads decline substantially across run order, consistent with a warming effect, but these counters alone cannot identify which files or fully explain latency variance. Randomized hash placement and environmental contention were not isolated.

**Keep the optimization as a correctness-tested opt-in candidate with a modest observed benefit, not a claimed large win or automatic default.** Bulk record I/O remains the stronger demonstrated intervention. Next improvements should target separately measured remaining I/O work; do not combine them into this comparison. Retain all timings, including the earlier failed witness assertion and corrected reruns, rather than selecting the fastest result.
