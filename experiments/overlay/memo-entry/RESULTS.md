# Entry-local memo metadata does not yet improve the previous best

## Decision

Keep the previous fastest frozen combination. Do **not** promote entry-local metadata from this evidence. It beat its newer same-source OFF arm, but a direct matched comparison with the previously fastest frozen memo showed essentially equal completion and slightly worse first-row/window latency. These are two fresh-server forks per arm on one synthetic workload, not broad performance guarantees.

## Comparison 1: same-source metadata OFF/ON

All artifacts identical; only `rdf4j.lmdb.nativeValueMemo.entryMetadata` differs. Order OFF, ON, ON, OFF.

| Metric | OFF mean (range) | ON mean (range) |
|---|---:|---:|
| Full 8M count completion | 75.745s (70.810–80.680) | 63.303s (62.748–63.859) |
| First received row | 0.893s (0.798–0.988) | 0.930s (0.917–0.942) |
| 1M window received | 22.360s (21.931–22.789) | 20.247s (20.025–20.469) |

That is 16.4% lower count-completion elapsed and 9.4% lower window elapsed, with 4.1% longer mean first-row latency. The OFF fork spread is large. This comparison alone does **not** establish an improvement over the previous fastest combination.

## Comparison 2: previous best versus metadata candidate

The previous frozen LMDB implementation predates the memo-policy control fields in the newer source. All other frozen modules, transport mapping and pinned #6440 writer are unchanged. Order previous best, metadata ON, metadata ON, previous best. Do not pool the ON forks from comparison 1 into this matched batch.

| Metric | Previous best mean (range) | Candidate mean (range) |
|---|---:|---:|
| Full 8M count completion | 68.277s (68.126–68.429) | 68.070s (66.076–70.064) |
| First received row | 1.029s (0.964–1.095) | 1.070s (1.027–1.113) |
| 1M window received | 21.826s (21.511–22.142) | 22.242s (21.625–22.859) |

Only 0.30% lower mean completion, with overlapping ranges; window latency is 1.9% longer and first-row latency 4.0% longer. No demonstrated gain over the previous best. A default-off flag is not proof that the compiled candidate is performance-equivalent to the older frozen class. This experiment does not identify the cause of all between-fork or between-source variation.

## Correctness and scope

All eight timed runs passed actual compiled IR, real memo hits, grouping, no-spill, binary content type, expected result count, and zero-retained-ledger witnesses. Exact HTTP smoke runs validated 100 emitted tuples each with matching checksum `495a217169c1db06ff284e3b4d8a765a6cc75b575cc8179fa1e448b09628c301`, and consumed/count-checked 8M. Timed runs emit a one-million-row window and count the full result; they do not transfer/render all eight million rows. No concurrent request throughput, browser paint, or upstream-main comparison was measured.

Final focused safety: 37 tests in each of metadata OFF/ON × diagnostic probe OFF/ON (148 executions), plus three map-growth replay tests. Root quick clean install passed. No whole-module/repository test claim. See QUALIFICATION.md, explicit preserved recovery failures, and successful test receipts.

The candidate remains default-off with the conservative measured-VM layout gate and unchanged ledger charge. Weak reference and version semantics remain intact; reducing sample share is not sufficient reason to promote it.

## Diagnostic profile (excluded from timing means)

One metadata-ON diagnostic run completed in 70.565s with all witnesses. Across 5,788 Java execution samples: lifetime 20.42%, projection/memo 18.87%, query kernel 14.51%, binary writer 13.42%, compact snapshots 11.20%, parser 7.98%. Writer encodeString was 5.60% leaf. No compact-map materialization frame was sampled. GC pauses totalled 1.151s in the 71.084s query sampling interval. These are sample shares, not wall-time percentages or removable fractions.

NativeValueMemo.value remained 9.21% leaf (533 samples). Of those, 527 mapped to bytecode 306, the source.lazyValue miss call. In the previous-best profile, 563 of 570 memo leaves instead mapped to bytecode 87, the cast immediately following WeakReference.get(). That older memo share was 9.34%. Source/bytecode attribution changed while overall sampled share and matched latency did not materially improve. This is **not** proof that misses now consume 9% wall time or that rearranging the table removed the reference-read cost. JIT/inlining/code layout and sampling attribution need examination.

## Next experiment

Instrument a separate untimed run to distinguish memo misses caused by empty slots, hash collisions, cleared weak references and semantic-version changes. Inspect compilation/inlining behavior before choosing another table-layout change. The fixture records approximately 218.75M hits in 220.8M global probes, but hit ratio alone does not identify the dominant cost.

One candidate is a per-projection-slot last-value cache to bypass a global hash/table lookup when factorized producers repeat the same ID. First measure that temporal locality; it does not remove WeakReference.get() and is not a promised gain. Any prototype must preserve exact ID/mask/version checks, weak ownership or explicitly budgeted bounded ownership, query/replay boundaries and output materialization guarantees. Compare the full combination directly with the prior best, not merely a new option's OFF state.
