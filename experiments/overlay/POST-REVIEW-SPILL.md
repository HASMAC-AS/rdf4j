# Default-budget spill validation after safety fixes

Production under test is final safety checkpoint c8f2848, compared with the frozen pre-review implementation. No production changes were made during these runs. All five experimental flags enabled; 8,000,000 results, 30 projected columns, 2,136,000 triples; compiled IR; 5 GiB JVM heap, 640 MiB per-query ledger. Four fresh JVMs, pre/post/post/pre, no query warmup. Same persisted fixture and frozen harness. This is synthetic, not the downstream private query.

| Fork | Seconds |
|---|---:|
| Pre A1 | 113.393 |
| Post B2 | 114.949 |
| Post B3 | 110.392 |
| Pre A4 | 111.896 |

Means: **112.644 s pre / 112.671 s post**: +0.023%, effectively identical. This supports performance neutrality of the cancellation/lifecycle safety fixes on the spill workload, not a speedup claim.

Every query returned 8,000,000 rows, checksum `-3441391656945048864`, actual compiled IR route and memo hits. All JVMs exited 0. Each migrated at 2,642,432 primitive rows, created and deleted 20 spill files, returned budget usage to zero, and peaked at **671,067,808 B** below the **671,088,640 B** cap. Memo hit rate was approximately 99.07%.

## Final-code JFR

A separate 4 ms sampling recording with final classes took 121.652 s, with the same exact output and resource checks. Do not pool this instrumented run with ABBA.

Main-thread native-method samples (2,343): RandomAccessFile readBytes0 **1,573 (67.1%)**, writeBytes0 **371 (15.8%)**, seek0 **205 (8.7%)**, getFilePointer 77, length0 42. Stack traces are often truncated at native boundaries, so these percentages are sample counts, not wall-clock attribution or a fully resolved caller breakdown.

Main-thread Java execution samples (5,788): NativeValueMemo.value **590 (10.2%)**, packed-vector binary search 274, Arrays.binarySearch0 240, ValueStore.getLazyValue **199 (3.4%)**. The memo is active and useful; this does not resemble an 80% value-materialization CPU profile after the earlier changes.

The earlier pre-bulk recording was write-heavy; current native samples are read-heavy. Do not infer a precise before/after CPU percentage from separate profiles. Actual JDK25 bytecode confirms RandomAccessFile.readLong/writeLong already use an internal eight-byte buffer and bulk calls, so merely replacing these scalar helpers with another buffer is not a useful optimization.

## Next experiment

Highest potential: a **producer-certified partitioned DISTINCT** on the serial compiled kernel, preserving a mandatory projected immutable root key through nested OPTIONALs. This could eliminate global spill state rather than make it cheaper. A certificate must come from final lowered IR and actual selected producer, not observed row order or an independently planned logical route. Reject parallel/interleaved outputs, unbound partition keys, unsafe semantic aliases, opaque producers and any downstream overwrite. Existing aggregateProperties grouping proof is a useful template, not sufficient permission by itself.

If that proof cannot be established, optimize exact disk index probing/rehashing and operation count. More overlay caches are not the next evidence-led priority.

Raw timing/resource figures: `review-spill-abba-summary.csv`. Profile counts: `post-review-spill-profile-summary.txt`. Reproduction: `run-review-spill-abba.sh` and `run-post-review-spill-profile.sh`; use the recorded frozen classes. Local logs/JFR remain preserved.
