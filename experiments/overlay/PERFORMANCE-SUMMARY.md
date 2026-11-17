# Query materialization and bounded DISTINCT: measured findings

This is a synthetic reproduction, not the unavailable downstream dataset/query. It uses 2,136,000 triples, 8,000,000 DISTINCT result rows, 30 projected columns and nested OPTIONALs. Every result value is consumed. Adjacency, value overlay and compiled IR are enabled; the ordinary value cache is the default 256k entries. The memo is a documented reconstruction, not the missing historical implementation.

## What moves the needle

1. Keep the memo. With it held constant, wide primitive DISTINCT was about 20.6% faster than the object-key baseline in the original 8-million-row comparison.
2. Account for the key table. Unbudgeted speed is not a safe memory policy. The flat table needs a large temporary resize allocation and spills early under tighter limits.
3. Segment the keys. This preserves roughly 40-second performance while lowering accounted peak from 3.117 GB to 2.042 GB, fitting an explicit 2.5 GiB query cap.
4. Batch spill records. Writing 32 separate eight-byte pieces for each 30-column record is expensive. One bounded record write reduces the default-budget segmented run from 179.817 to 118.630 seconds without increasing its memory limit.
5. Do not add more overlay caches blindly. Typed overlay decoding did not deliver a convincing end-to-end win. Partial-ready processing reduces allocation but did not add a clear 8-million-row runtime win on top of wide DISTINCT.

## 8-million-row results

All use 5 GiB JVM heap and the same verified persisted fixture. The first three rows are means of two cold, fresh-JVM runs in mirrored order. Later rows are single cold runs, no discarded query warmup: useful causal evidence with route checks, not a statistical speedup claim.

| Implementation | Query cap | Time | Accounted peak | Spill files |
|---|---:|---:|---:|---:|
| Memo, object DISTINCT baseline | Wide keys unaccounted | 56.528 s | Not comparable | 0 |
| Memo + unbudgeted flat wide DISTINCT | Wide keys unaccounted | 44.866 s | Not comparable | 0 |
| Above + partial-ready | Wide keys unaccounted | 44.884 s | Not comparable | 0 |
| Bounded flat + partial-ready | 3.5 GiB | 40.392 s | 3,116,898,144 B | 0 |
| Bounded flat + partial-ready | 640 MiB | 175.682 s | 393,746,272 B | 20 |
| Segmented + partial-ready | 2.5 GiB | 40.383 s | 2,042,135,040 B | 0 |
| Segmented + partial-ready | 640 MiB | 179.817 s | 671,067,808 B | 20 |
| Flat + bulk record I/O | 640 MiB | 126.486 s | 393,746,272 B | 20 |
| Segmented + bulk record I/O | 640 MiB | 118.630 s | 671,067,808 B | 20 |

The default 640 MiB query cap is a consequence of this 5 GiB heap: the existing global limit is half the heap, and the default per-query limit is one quarter of that global limit. No budget-policy default was changed. The 2.5 GiB segmented experiment explicitly allows one query to use the unchanged global limit; concurrent admission still shares that global authority.

Every run returns checksum `-3441391656945048864`, the exact row count, compiled `irKernel`, and memo hits. Budgeted runs release all ledger bytes and delete every created spill file. Bulk runs additionally assert exactly 8,000,000 successful bulk record writes. These ledger peaks are accounted operator memory, not whole-process RSS or a universal bound on all query allocations.

Flat storage spills after 1,048,576 primitive rows at the default cap. Segmented storage keeps 2,642,432 rows before spilling. But migration writes the retained prefix too: all eight million keys ultimately reach disk in either case. Delaying migration alone therefore does not remove spill I/O.

## Supporting measurements and profiling

- Matched 1-million-row ABBA, two forks/version, two warmups plus three measured queries/fork: old unbudgeted wide mean 4.486 s; bounded flat mean 4.207 s. Fork drift prevents claiming a definitive 6.2% speedup. New accounting records a 393,746,272-byte peak, no spill, zero retained budget.
- The same 1-million-row fixture with a 64 MiB query cap takes 25.915 s in one cold run, with a 61,512,000-byte peak and 17 spill files, all deleted. Do not directly compare this cold number with the warm ABBA means.
- The default-budget 8-million-row spill profile took 209.071 s under profiling, versus 175.682 s unprofiled. Of 4,247 main-thread native-method samples, 1,941 were file writes and 1,727 reads; approximately 97% were RandomAccessFile operations. This is a share of native samples, not 97% of query wall time. Java and native sampling periods differ.
- The source and focused call-count tests establish 32 payload writes becoming one for a 30-column ID record, with identical bytes. Other index probes and index rebuilds remain separate optimization candidates.
- Cumulative allocated bytes are not retained memory. Original partial-ready reduced the 8-million-row cumulative thread allocation from about 39.94 GB to 36.44 GB without an additive timing win. Segmentation further avoids payload recopying; see raw CSV values.

## Correctness and limits

- Fixed a real replay bug: lazy map-growth replay lost the original query context and changed `irKernel` to `batch`. The strict test now proves two compiled attempts, fresh memo hits, exact output and caller-context restoration, including exceptional reopen. Full lifecycle: 29 passed; nearby replay: 20 passed.
- Fixed optional memo integration for unsupported sources: they retain their established aggregate cache. Eligible memos still fall back without caching after memory refusal.
- Bounded-flat validation: 5,030 scoped evaluation tests completed with zero failures/errors and five skips; early shards were rerun after the final cleanup change. A broader store/value/replay selection completed 1,771 checks with zero failures and two diagnostic skips. These are not full-module/compliance claims, and overlap with focused suites.
- Segmented storage: 31 focused checks passed. Bulk I/O passes the same 28 format, call-count, failure and cleanup checks with the flag off and on, using naturally budget-triggered spill and a typed file factory. New stages require their own broader regression.
- The bounded optimization covers opted-in wide SELECT DISTINCT with a real ledger. Legacy aggregate, narrow and no-ledger paths are not newly memory-bounded.
- The existing spill representation supports at most 64 columns. Wider bounded opt-in requests refuse before publishing a key, rather than falling back to uncharged storage.
- All optimization flags remain default-off. No ready-to-merge or full-regression claim.

Raw data: `measurements.csv` preserves the historical memo matrix; `budget-measurements.csv` preserves later warmup and measured runs. `REPRODUCE.txt` explains snapshots, scripts and fixture verification. Relevant checkpoint reports distinguish each code version and validation scope.

## Probe-reuse follow-up

The same segmented+bulk candidate in an off/on/on/off series took 128.643 / 124.014 / 116.193 / 118.804 seconds. Means were 123.724 seconds off versus 120.103 on, a modest 2.93% reduction. System CPU decreased while user CPU increased; physical reads declined across the sequence. Earlier identical-candidate runs ranged from 103.253 to 158.900 seconds, so neither this small effect nor previous single-run bulk percentages are established general speedups. The deterministic benefit is avoiding a second index probe on eligible membership misses. All four runs preserve exact output, ledger cap and cleanup. See `SPILL-PROBE-ABBA.md` and `probe-abba-summary.csv`.

Probe reuse remains default-off. Focused tests cover collisions, growth invalidation, failure cleanup and bulk on/off combinations. The earlier 5,030-test broad pass does not cover this later stage.

## Existing ordered DISTINCT route

On a 100,000-row fixture with a 4 MiB query cap and memo retained, the existing proven ordered route used bounded partition state and avoided spill. It nevertheless took 4.703 seconds versus compiled IR's 1.731 seconds, allocating 31.39 GB versus 395 MB cumulatively. Separate allocation profiling implicated repeated packed factor-tree setup and buffers. The 20,000-row ordered case also passed under a 1 MiB cap without spill or leaked budget.

This is not a recommendation to replace compiled IR with the existing ordered route. Ordinary IR exports no ordering certificate, and its DISTINCT sinking rejects OPTIONAL. Safely exposing grouping from the final compiled producer could be valuable, but remains unimplemented and unproven. See `ORDERED-DISTINCT-EXPERIMENT.md`; no 8-million-row ordered run was attempted.

## Combined regression checkpoint

At `42c43d5`, segmented storage, bulk I/O and probe reuse together passed all 426 selected evaluation test classes: 5,040 executed tests, zero failures/errors, and five intentional benchmark skips. Source hashes were unchanged throughout the eleven shards. This is evaluation-package coverage, not the full module. Subsequent cancellation/review changes require their own validation. See `SPILL-FINAL-REGRESSION.md` and `spill-final-regression-summary.csv`.

Ordered-route buffer diagnostics support the allocation explanation: reducing only the existing root-buffer setting to 64 gave 3.708 seconds / 25.21 GB allocated; reducing both effective buffers to 64 gave 2.850 seconds / 11.78 GB. Both preserve exact 100k-row output, but remain worse than compiled IR. These are single warmed samples, not ABBA or a default-setting recommendation.

Review hardening adds cancellation/deadline polling to segmented growth and collision loops. Twenty-nine focused tests pass, including natural-path red-to-green cancellation tests, migration failure/cleanup, canonical aliases and concurrent spilled clear/reopen. The combined broad sweep above predates this patch; its rerun is pending.

A deterministic competing-query test now steals the 4,096-byte spill workspace between clear and re-admission. Admission refuses correctly; the hardening change immediately releases the failed owner's remaining 1,728-byte structural claim and prevents reuse after exact state was discarded. The competing tracker remains usable, and final claims return to zero. The focused suite now passes 31 tests; full post-fix regression and matched cancellation-overhead measurements are pending.
