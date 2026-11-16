# Compact pages achieve the predicted representation and charge

Both approved full-fixture diagnostics passed. The compact candidate's final measured page table contains exactly the predicted 16,384 inline pages and 256 dense pages. Those pages charge 12,582,912 bytes (12 MiB), compared with the original control's 268,107,776 page bytes. The separate constrained probe also passed: it observed 1,024 refused promotions while preserving the first cached member, followed by one successful promotion after the competing reservation was released.

These are structural and accounting results. The instrumentation adds counters, weak-reference observations, close-time tables and output. Its elapsed-time, allocation and GC fields must not rank the implementations. The separate balanced timing study determines the performance tradeoff.

## Full shared-tail fixture

Each variant ran in a fresh DENSE JVM with three warmups and one measurement query, using this phase's identical common harness, 64 roots, 128 left members, 128 right members and two tail columns. All eight queries returned 1,048,576 rows and checksum 8327963502356915200 and passed the independent exact bag oracle. The source/JVM/runtime identities matched their pinned protocol, every query started and ended with zero process-ledger use, and the measured selection signatures matched.

Actual execution qualified: normal AUTO, a native outer root, compiled IR for the measured query, ACTIVE adjacency with stable current/applied revisions, and positive query-time adjacency lookup hits. Those hits can include preparation/probes; they do not establish that every result row came through adjacency.

| Measured observation | Original DENSE | Compact DENSE |
|---|---:|---:|
| Final retained pages | 16,364 | 16,640 |
| Inline pages | 0 | 16,384 |
| Dense pages | 16,364 | 256 |
| Pages with one stored weak-reference slot | 16,112 | 16,384 |
| Pages with 64 stored weak-reference slots | 252 | 256 |
| Occupied stored-reference slots | 32,240 | 32,768 |
| Allocated cell capacity | 1,047,296 | 32,768 |
| Declared page charge, bytes | 268,107,776 | 12,582,912 |
| Query-ledger peak, bytes | 268,426,276 | 12,901,412 |
| Successful promotions | 0 | 256 |
| Refused promotions | 0 | 0 |
| Factor-cache hits | 2,048,256 | 2,080,768 |

The original control reproduces the previous sparse-page observation. The candidate allocates one inline cell for each singleton and 64 cells only for each dense page. Its stored-reference occupancy therefore equals its allocated cell capacity in this fixture. This does not imply all weak referents remained live: occupied slots mean nonnull weak-reference objects, and live-referent snapshots are transient observations.

The compact page charges consist of 16,640 shell claims at 512 bytes (8,519,680 bytes) and 256 successful payload claims at 15,872 bytes (4,063,232 bytes). Their sum is exactly 12,582,912 bytes. The original has one actual 16,384-byte claim per dense page; the diagnostic does not fabricate separate shell/payload claims for that control.

Every base, payload, directory and coordinate-memo byte granted in the accepted runs was observed immediately before its explicit release. Page counts, promotion counts, declared charges and exact occupied-offset masks reconcile. The final endpoints are zero. The diagnostic's attribution gate is stricter than a leak check: an earlier ledger close could legitimately zero a reservation before observation, but that would disqualify complete explicit-release attribution under this fixed protocol. It did not occur in these accepted runs.

The candidate also admits 276 more pages and obtains 32,512 more factor hits than the capped original. Increased admission is a separate mechanism from any reduction in lookup cost. This diagnostic establishes the representation and accounting benefit; it does not establish a CPU benefit or equate the reduced reservation to an equal reduction in allocated or retained heap.

Full results and per-target breakdowns are in `analysis.json`. Every raw query remains in `runs/{original,compact}/samples.jsonl`, and every final page key/mask remains in the corresponding `page-records/owner-*.jsonl`. The protocol SHA-256 is `a3a51e4371deda248d3861a46a7ec856a4c0ddb386a9f561b0b76c60e360389a`.

## Persistent refusal and recovery

The separate synthetic probe uses the exact compact diagnostic runtime plus its isolated driver. It makes no query-route or performance claim. A standalone 20,000-byte memory manager first retains A at offset 0. A competing claim leaves only 128 bytes available, so a 15,872-byte promotion cannot fit. Both values are deliberately retained by the synthetic source to remove GC variability.

The probe performs 1,024 B/A alternations. Each B at offset 1 resolves normally and attempts promotion; each A remains a factor hit. Cache charge and constrained ledger remain unchanged after each pair. Once the competing claim is released, B/A/B promotes the page and reuses both members.

All prospective counts matched: 2,052 captured rows, ID checksum 87,210, one A resolver call and 1,025 B resolver calls, 1,025 promotion attempts, 1,024 refusals, one success and 1,026 factor hits. No spill or shed callback ran. The final dense page contains exactly offsets 0 and 1 (mask 3). Materializer charge grows from 2,496 to 18,368 bytes after recovery, and explicit owner close returns the standalone ledger to zero. Its peak is 19,872 bytes from the competing claim.

This demonstrates that refusal preserves the first member, repeatedly retries rather than permanently poisoning the page, never reclaims the competing owner, and recovers when capacity is released. It also confirms that persistent pressure can cause one failed reservation attempt per eligible insertion. The CPU cost of those attempts is not measured here.

Raw probe and page records, compilation/runtime identities, predictions and checks are under `retry-probe/`; `retry-probe/analysis.json` contains the complete result. Fields named `processLedger*` in its raw JSON refer to its standalone testing manager. The probe protocol SHA-256 is `244553ceb44414bacb43267a35faa09ae6391fe4689eb6240609a84c3b150dba`.

## Scope and preservation

Both thin overlays select only the instrumented outer materializer class and its helper. Their respective frozen `FactorPage`, `FactorMemo` and projection-plan classes remain selected unchanged; complete source comparisons and raw javac outputs are preserved. All original production/evidence stays intact. Promotion hooks observe the existing outer call; an unexpected exception would make any transient internal reservation unobservable and fail the diagnostic rather than claim complete accounting. Allocation-failure rollback was source-reviewed, not fault-injected or claimed as experimentally exercised.

No diagnostic fork was replaced or extended. The compiler/JVM slot was released after the two full-fixture runs and the one retry probe. No additional experiment is part of this report.
