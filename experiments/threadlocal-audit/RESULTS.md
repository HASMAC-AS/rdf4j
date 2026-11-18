# ThreadLocal checkpoint: fresh matched results

**No demonstrated performance benefit.** The candidate only changes interpreted TypeMatrix co-scan checkpoint lookup, not the three repository-query control routes. Those controls are near parity overall but noisy. The dedicated co-scan candidate has a **9.19% higher mean latency**, with inconsistent paired results. This is not evidence of improvement, performance neutrality, or a proven repeatable regression. Keep the slower fork included.

## Provenance and scope

- Baseline: original `ba74f4782e95e386653a6b1c7f6b0969e09b8f12` TypeMatrix plus identical frozen candidate runtime. Candidate published source: `c02f684d78bce58d1c8f2710cb9bb2aa8bed5dfa`; later header-only changes do not alter measured classes. Local HEAD alone is not the candidate identity. Source and complete LMDB class hashes: `matched-02/sources.sha256`, `classes.sha256`; exact JVM/classpaths: `environment.txt`.
- JDK25.0.5, Xms2g/Xmx3g, sequential fresh JVMs under canonical lock. Each arm has 4 independent forks, order A B B A B A A B; 20 warmups then 10 measurements per fork. No trimming. Independent-fork means are primary, not 80 supposedly independent queries.
- Repository controls: 100 groups×fanout10,26,700 triples,100,000 DISTINCT six-slot results. Generic collections pinned to heap delegate (`iterationCacheSyncThreshold=0`) equally in all arms, native 640MiB cap; common combination flags in `matched-02/flags.txt`. This is a controlled no-spill comparison, not default-policy regular behavior or upstream main.
- All runs match both RDF-term-aware checksums (`1899306305404719472`, `3694550068168048289`) and rows. Actual regular factory/native gate and route/counters prove regular, `irKernelInterpreted`, `irKernel`; native spill/migration zero and ledger reservations returned. Materialization includes lexical, datatype and language reads.
- Full repository latency includes connection open, query preparation, evaluate, complete result consumption, result/connection close. First row means first fully consumed row and includes eager evaluate. No HTTP/Workbench/browser claim.
- Dedicated coScan:real LMDB 267,000-triple fixture, direct TypeMatrix invocation and 6 exact aggregate rows compared against independent generic SPARQL; RUNS+1, ADJACENCY/PARALLEL unchanged prove serial coScan, with adjacency disabled and threads=1. Timing includes dataset open/operator creation/evaluate/result consume/close. This is **operator-level**, not repository or Workbench end-to-end.

## Means across independent fork means

| Route | A ms | B ms | B latency change | B throughput change | A first row ms | B first row ms |
|---|---:|---:|---:|---:|---:|---:|
| regular | 1492.696 | 1511.912 | +1.29% | -1.27% | 8.729 | 8.154 |
| interpreted | 231.120 | 230.523 | -0.26% | +0.26% | 11.529 | 11.388 |
| janino | 175.120 | 176.644 | +0.87% | -0.86% | 12.647 | 12.386 |
| coscan | 8.610 | 9.402 | +9.19% | -8.42% | N/A | N/A |

Positive latency change is slower. Throughput change is reciprocal A/B−1, not simply negated latency. No concurrent QPS or request p95 measured.

## Fork means and paired effects

Parentheses give min–max individual measured latency within each fork. Pair deltas use adjacent balanced A/B pairs; a positive reduction would favor B, but signs and magnitudes vary.

### regular
- regular-1-A.log: 1487.012ms (1416.575–1650.186).
- regular-2-B.log: 1452.743ms (1421.116–1498.528).
- regular-3-B.log: 1470.785ms (1388.068–1562.967).
- regular-4-A.log: 1523.500ms (1423.332–1601.913).
- regular-5-B.log: 1564.120ms (1453.284–1718.099).
- regular-6-A.log: 1499.587ms (1416.971–1692.702).
- regular-7-A.log: 1460.686ms (1376.957–1595.331).
- regular-8-B.log: 1560.001ms (1442.937–1718.104).
- Paired latency reduction: +2.30%, +3.46%, -4.30%, -6.80%.
- Measured-index means 0–9(ms): 1563.77, 1553.19, 1517.68, 1541.22, 1478.44, 1471.04, 1448.92, 1481.76, 1489.29, 1477.73.

### interpreted
- interpreted-1-A.log: 237.284ms (220.376–256.534).
- interpreted-2-B.log: 232.989ms (224.494–257.211).
- interpreted-3-B.log: 227.518ms (214.757–241.988).
- interpreted-4-A.log: 226.075ms (210.036–241.126).
- interpreted-5-B.log: 228.786ms (221.789–244.724).
- interpreted-6-A.log: 226.476ms (212.661–256.812).
- interpreted-7-A.log: 234.644ms (216.350–264.823).
- interpreted-8-B.log: 232.800ms (222.042–255.201).
- Paired latency reduction: +1.81%, -0.64%, -1.02%, +0.79%.
- Measured-index means 0–9(ms): 227.07, 232.02, 226.83, 229.31, 232.66, 237.12, 234.34, 228.16, 229.97, 230.73.

### janino
- janino-1-A.log: 181.094ms (162.793–244.650).
- janino-2-B.log: 169.433ms (162.015–180.858).
- janino-3-B.log: 199.143ms (158.662–261.355).
- janino-4-A.log: 161.871ms (158.238–171.027).
- janino-5-B.log: 161.568ms (153.562–171.369).
- janino-6-A.log: 184.827ms (158.822–220.175).
- janino-7-A.log: 172.686ms (162.239–188.239).
- janino-8-B.log: 176.432ms (165.250–189.811).
- Paired latency reduction: +6.44%, -23.03%, +12.58%, -2.17%.
- Measured-index means 0–9(ms): 192.29, 169.16, 171.15, 171.63, 174.73, 184.37, 181.27, 184.10, 165.52, 164.59.

### coscan
- coscan-1-A.log: 9.079ms (8.213–10.103).
- coscan-2-B.log: 8.175ms (7.797–9.603).
- coscan-3-B.log: 8.650ms (7.924–11.251).
- coscan-4-A.log: 8.095ms (7.475–11.784).
- coscan-5-B.log: 8.344ms (8.083–8.619).
- coscan-6-A.log: 8.629ms (8.298–9.519).
- coscan-7-A.log: 8.638ms (8.010–9.042).
- coscan-8-B.log: 12.438ms (9.696–21.730).
- Paired latency reduction: +9.96%, -6.86%, +3.31%, -44.00%.
- Measured-index means 0–9(ms): 9.06, 9.97, 10.58, 8.91, 8.42, 8.65, 8.43, 8.49, 8.57, 9.00.

## Convergence and slower coScan fork

Longer warmup substantially reduced the pilot native-control discrepancy, but does not guarantee perfect steady state. Regular index means trend somewhat downward; Janino still has substantial fork/within-fork variation. Interpreted is relatively flat. CoScan B8 contributes 12.438ms versus the other B fork means near8–9ms. Its individual values are 9.90,19.99,21.73,12.29,9.70,9.81,10.01,10.39,9.71,10.86ms: there are two large spikes and a generally higher floor. It is retained, not excluded as an inconvenient outlier.

These clean JVMs did not capture per-run GC/JIT/scheduler traces, so the cause cannot be assigned retrospectively. Separate profile forks are not evidence of what happened in that exact fork. No further broad rerun is claimed or used to select a favorable result.

## Invalid pilot

`matched-01` is diagnostic only: three warmups were insufficient, apparent improvements appeared on unchanged native routes, and a parent copyright scan overlapped early regular forks. All pilot observations remain available, but **none support speedup claims**. Entire regular series was replaced in matched-02; no samples were cherry-picked.

## Allocations (query-thread only)

- regular A: 2355.21MB/query mean, query thread only; not retained heap or total worker allocation.
- regular B: 2350.43MB/query mean, query thread only; not retained heap or total worker allocation.
- interpreted A: 187.80MB/query mean, query thread only; not retained heap or total worker allocation.
- interpreted B: 188.41MB/query mean, query thread only; not retained heap or total worker allocation.
- janino A: 174.02MB/query mean, query thread only; not retained heap or total worker allocation.
- janino B: 176.53MB/query mean, query thread only; not retained heap or total worker allocation.

## Separate profiles

Eight separate profile JVMs completed, A/B for all four routes. Recording starts after 20 warmups: regular 5 measured queries, interpreted/Janino 30, coScan 500. These longer profile workloads are not the clean latency series. JFR `profile` settings; only main-thread execution/native samples analyzed. See `profiles-01/PROTOCOL.txt` and `summary.json`. Every profile retained correct result/route assertions.

| Profile | Main-thread samples | Samples containing ThreadLocal frames | Inclusive share |
|---|---:|---:|---:|

| coscan-A | 280 | 2 | 0.7% |
| coscan-B | 265 | 2 | 0.8% |
| interpreted-A | 719 | 106 | 14.7% |
| interpreted-B | 681 | 90 | 13.2% |
| janino-A | 512 | 129 | 25.2% |
| janino-B | 531 | 110 | 20.7% |
| regular-A | 684 | 45 | 6.6% |
| regular-B | 709 | 51 | 7.2% |

These are **inclusive sample shares**, not wall-time percentages, exact lookup counts, or achievable speedups. Categories overlap, stacks may be truncated, and inlining hides accesses. There is one diagnostic profile per arm, not enough to claim a profile percentage changed causally.

### Next optimization target

The native/compiled routes point toward **per-pull scope binding/restoration**, not the masked TypeMatrix checkpoint:

- `LmdbStoreConnection.executeObservedRead` / `executeTrackedRead` and `LmdbSailStore.enterReadResourceContext`.
- `QueryExecutionContext.activateExternalValueInitializer` / replay scope.
- `RepositoryValueIteration.sourceHasNext`, `sourceNext`, `restoreSourcePull`.
- `ValueResolutionScope.activateForeground` and `LmdbFusedSipFactorizedRuntime.attach`.

These align with the inventory's per-pull ownership/read-view/replay scopes. Do not remove lifecycle checks wholesale: a next experiment should reuse an explicitly owned context across adjacent internal calls while keeping boundary fallback and nested restoration, then count avoided gets/sets/removes separately from latency measurement. Fused-session capture is another local candidate where ownership is already explicit. Preserve cancellation and worker/map-growth semantics.

Regular-profile ThreadLocal callers additionally include `ValueStore.isWriteTransactionOwner → getLazyValue` and `QueryExecutionContext.checkpointReplaySafepoint → ensureSnapshot`. Regular execution has substantial value-version validation, adjacency decoding, binding and collection work too. Native interpreter profiles still show `PullStage.next/open`; generated-code work cannot by itself remove the outer repository wrappers.

CoScan profiles are dominated by `Varint.getIntBigEndian` and native LMDB cursor operations. The small masked lookup change is not the obvious next throughput target on this fixture. The profile is consistent with the lack of measured benefit, but does not explain the anomalous B8 clean fork.

## Artifact guide

- `matched-02/summary.json`, `convergence.json`: independent means, paired differences, first-row means and measured-index diagnostic.
- `matched-02/*-*-*.log`: all 32 accepted clean fork logs; `reference.log`, environment/flags/source/class hashes retain verification and provenance.
- `matched-01/`: full invalid pilot retained with `PILOT-ONLY.txt`.
- `profiles-01/summary.json`: top leaves, inclusive frames, nearest visible ThreadLocal callers, category counts and stacks. Raw `.jfr` and `.samples.json` remain local evidence; do not need to publish large binary recordings to GitHub to checkpoint this report.
- Harnesses and runners: `ThreadLocalQueryBenchmark.java`, `ThreadLocalCoScanBenchmark.java`, `run-matched.sh`, `run-profiles.sh`, `summarize-matched.py`, `analyze-series.py`, `summarize-profiles.py`, `best-flags.txt`.
