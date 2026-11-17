# Sequential compact-name hint: no demonstrated HTTP win

## Decision

Keep `rdf4j.sail.compactSequentialLookup=false` in the fastest supported combination. The guarded hint reduced hash-lookup sampling as intended, but this matched HTTP batch did not show lower latency or higher throughput. Retain it as a default-off experiment rather than infer a benefit from its profile alone.

## Matched unprofiled ABBA

Same frozen sequential API, query, HTTP-client and lifetime LMDB outputs in both arms; validated identity-preserving transport and pinned #6440 writer enabled. Only the sequential hint flag differs. All four fresh-server runs passed compiled IR, memo, grouping, no spill, count and zero-retained-ledger witnesses. No competing builds/tests during timing.

| Metric | Hint OFF mean (range) | Hint ON mean (range) |
|---|---:|---:|
| Full8M count completion | 66.778s (65.654–67.902) | 68.289s (67.326–69.253) |
| First received rows | 0.918s (0.855–0.981) | 0.947s (0.934–0.960) |
| 1M window received | 21.398s (20.877–21.919) | 22.701s (22.672–22.729) |

ON mean completion was2.26% longer and window time6.09% longer. Completion ranges overlap; two forks per arm cannot establish a general regression. They do not justify promoting the hint as a speed improvement. Full completion means8M consumed/counted, not8M transferred. These are not browser paint timings or upstream-main comparisons.

## Correctness

42 focused tests passed separately OFF/ON, plus23 assertions-disabled tests separately OFF/ON. Includes10 new lookup tests with arbitrary order, repeated/foreign equal names, collisions/missing names, null/unbound values, materialization, mutation and reentrancy. Exact HTTP smokes OFF/ON each validated100 emitted tuples and consumed/count-checked8M; matching checksum495a217169c1db06ff284e3b4d8a765a6cc75b575cc8179fa1e448b09628c301. See SEQUENTIAL-QUALIFICATION.md for commands and scope. No whole-repository test claim.

## Mechanism profile

Separate diagnostic ON run completed in69.029s with all witnesses, excluded from the timing means. Of5,386 Java execution samples, only10(0.19%) had compact `slot` as a leaf under `getValue`, while136(2.53%) were append/build slot calls. The preceding transport-only profile had209(3.43%) lookup and138(2.26%) build samples. Thus the hint addresses the intended lookup path, but reduced sampling is not itself an end-to-end speedup. These profiles were separate runs, and changing shares are not absolute time savings.

No compact materialization frames were sampled. Remaining largest categories: lifetime21.93%, projection/memo20.94%, writer15.78%, kernel14.65%. NativeValueMemo.value was10.45% leaf. GC pauses totalled1.077s in the69.688s recording interval. Java sample shares are not wall-time percentages or removable fractions.

Next experiment: entry-local primitive memo metadata retaining weak-reference semantics, with measured object-layout guard and unchanged conservative ledger charge on the supported VM. Sequential hint stays OFF for that comparison.

## Evidence

`../workbench-e2e/sequential-a-off`, `sequential-b-on`, `sequential-c-on`, `sequential-d-off` JSON/manifests; exact smoke receipts; `sequential-profile-on-summary.txt`. Raw JFR remains local. Safety source checkpoint7f0ad109b4.
