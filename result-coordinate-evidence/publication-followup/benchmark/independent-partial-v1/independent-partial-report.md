# Independent audit of the stopped timing study

**The frozen performance study is incomplete and has no accepted performance comparison.** Its first nine scheduled JVMs completed, then the exact DENSE resolver-matching prerequisite failed. This audit preserves the failure and does not compute latency or allocation ratios.

The independent audit validated **9 JVMs**, **117 queries** (72 warmups and 45 measurements). Of **39** completed DENSE query pairs, **38** match every recorded counter, route, adjacency, and result observation.

## Exact mismatches

| Fixture | Zero-based round | Stage | Iteration | All-enabled resolver calls | Omitted resolver calls | Counter difference |
|---|---:|---|---:|---:|---:|---|
| shared-tail | 1 | warmup | -7 | 32,833 | 32,834 | lazyValueCalls: 32833 → 32834 |

The JSON receipt lists every completed pair and recursively compares the complete counter dictionaries. Route/tier, adjacency, and result differences are recorded separately. Ledger peaks differ between publication arms as expected, so they are reported as arm observations rather than counter-equality failures.

## All observed resolver and ledger values

| Fixture | Arm | Queries | Resolver-call frequencies | Ledger peaks, bytes |
|---|---|---:|---|---|
| fanout | off | 13 | 2,770,101: 13 queries | 2,432 |
| fanout | dense-all | 13 | 1,147: 13 queries | 2,264,968 |
| fanout | dense-omit | 13 | 1,147: 13 queries | 592,008 |
| shared-tail | off | 26 | 2,113,601: 26 queries | 1,472 |
| shared-tail | dense-all | 26 | 32,833: 26 queries | 272,948,260 |
| shared-tail | dense-omit | 26 | 32,833: 25 queries, 32,834: 1 queries | 136,502,308 |

## Result, execution, and preservation checks

Every query retains the prescribed count/checksum, full-consumption flags, and zero ledger endpoints; shared-tail also records a valid exact-bag oracle. Every measured query uses compiled native IR through normal AUTO. All query-time adjacency checks and outer-root audits pass. All nine Java processes exited successfully, and their recorded intervals are serial and nonoverlapping. The observed directories are exactly the first nine entries of the frozen schedule.

The runner's post-preservation receipt reports unchanged identities. This audit additionally rehashed all 129 runtime classpath entries and all 99 watched source/protocol/evidence files, reproducing their preserved identities. Raw command, process, sample, root-audit, console, validation, and preservation files are pinned by hash. The prepared complete-study analyzer remains unchanged.

## What the failure establishes

The observed exception is a resolver-work equality failure under the prospectively frozen gate. It is not a result failure, process failure, route change, or nonzero-ledger failure. The timed harness has no per-target weak-reference or witness-miss telemetry, so these records alone cannot identify why the extra lazy-value call occurred.

The failed warmup remains part of the protocol. The successful measured queries do not override a gate that applied to every query, and the nine-fork prefix is not substituted for the planned thirty-six-fork comparison. A separate diagnostic investigation may examine the counter variation while keeping this original study failed and incomplete.

Frozen protocol SHA-256: `2a94e81f10d63ec177cb998d3270fb9cb48adee569ca1808370f6fc81465107b`.

Partial audit script SHA-256: `5075daea158d87f15f999e207e5d3b08da36fe45eaeea8bd8b1cb9fa0bb45b70`.
