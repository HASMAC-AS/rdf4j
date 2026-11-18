# Read-depth holder: matched throughput and latency

**Holder depth improved both broad native routes in this matched fixture:** interpreted full latency fell 7.63% and Janino 4.69%, with all four paired blocks favorable in each. Regular improvement was small and inconsistent. The physically qualified cancellation interaction does not show synergy or rescue the previously adverse owned path. This supports the isolated holder change as the better candidate for further validation, not a universal speedup or release-readiness claim.

## Scope and provenance

Primary question: does moving internal read depth into the existing per-thread registration holder help the incumbent measured combination? A is previous combined D, not the later regressing cancellation experiment and not upstream main. Earlier guard-only Janino was promising but uncertain; D is not claimed to be proven globally fastest. The separate six-arm interaction tests cancellation combinations without presuming they belong in the preferred configuration.

| Arm | Meaning | Published source |
|---|---|---|
| A | Incumbent combined D | `5ac89b5c137030fea1aa6aa7160984423d7f4cd5` |
| AD | A plus holder depth | `0750336e71ab0ca0d216ce9c36483a6e9063337d` |
| C0 | A plus record cancellation, owned OFF | `7ac1650a871b86f422dc4ec1aaf28d0a6742759f` |
| C0D | C0 plus holder depth | `bb0e3df7c63c5dd1661b672775d84d6c504ffdf1` |
| C1 | C0 identical bytes, owned ON | `7ac1650a871b86f422dc4ec1aaf28d0a6742759f` |
| C1D | C0D identical bytes, owned ON | `bb0e3df7c63c5dd1661b672775d84d6c504ffdf1` |

AD adds only the 20-class LmdbStoreConnection family over immutable A; C0D/C1D adds the same family over the previous 215-class cancellation layer. Pre-edit connection classes were all byte-identical to A. Eighteen of 20 depth classes changed, two unchanged, no class additions/deletions. Source/class hashes, loaded origins and selected runtime manifests are retained; stale local HEAD is not a source identity.

## Behavior qualification

Worker selections passed 90 executed tests with three gated skips on current source and 90 with three gated skips on isolated depth-over-D. An explicit enabled gated selection gives two passes and the same failure on unchanged D and depth AD. As GROUPED-REPLAY-TRIAGE.md explains, the failure is expected zero/observed one consumed row at a memo-hit barrier, BEFORE the growth writer starts, not a replay-request-count failure or demonstrated replay-safety bug. The shared grouped fixture remains incompletely qualified; a delayed first memo hit is a hypothesis, not a proven cause. No assertions were weakened. These are scoped selections, not full repository validation.

All 26 untimed qualification runs passed: six arms across three small broad routes, six small actual-probe arms, and C1/C1D actual-size warm-state checks. Both latter checks after 20 warmups recorded 899,301 binds, 2,697,873 polls and 899,271 resets, with identical 100k dual checksums/no spill. Instrumented elapsed fields are never timing results. The broad routes use adjacency and previously showed no physical owned-cancellation hits; only the separate no-adjacency Janino interaction is used to assess that combination.

## Prespecified clean protocol

48 fresh JVM forks: three primary routes A/AD × 4 forks each (24), plus Janino physical repeated-probe six-arm interaction × 4 forks each (24). Each fork has 20 warmups and 10 measurements. Primary order A AD / AD A / A AD / AD A. Interaction order A AD C0 C0D C1 C1D / C0 C0D C1 C1D A AD / C1D C1 AD A C0D C0 / C0D C0 A AD C1D C1. Each arm appears once per near-time block; four blocks cannot exactly balance six positions. All samples retained, no post-hoc shortening or trimming.

Same 100-group/fanout-10 fixture: 26,700 triples, 100,000 six-slot OPTIONAL DISTINCT results, dual checksums 1899306305404719472 and 3694550068168048289. Same native 640 MiB cap, heap-only generic collections, no spill/migration or retained ledger. Inherited measured-combination flags include partialReady ON, grouped waves OFF. JDK 25.0.5, -Xms2g/-Xmx3g. Tracing and JFR OFF; identical low-volume GC logging ON. First row includes eager evaluate and complete projected values; full latency includes connection open, prepare, evaluate, consume and close. Allocation is query-thread allocated bytes, not retained heap or all workers. This is not main/HTTP/Workbench/concurrent QPS/tail-latency evidence.

## Arm means

| Cell | Arm | Full ms | Fork range ms | First row ms | Allocation MB |
|---|---|---:|---:|---:|---:|
| broad-regular | A | 1562.381 | 1505.132–1686.591 | 10.541 | 2348.33 |
| broad-regular | AD | 1541.620 | 1494.782–1594.037 | 11.479 | 2336.14 |
| broad-interpreted | A | 235.812 | 229.777–241.252 | 12.427 | 186.56 |
| broad-interpreted | AD | 217.830 | 213.411–226.916 | 11.534 | 177.40 |
| broad-janino | A | 170.183 | 164.834–173.372 | 12.512 | 177.89 |
| broad-janino | AD | 162.201 | 152.729–170.039 | 11.988 | 172.12 |
| probes-janino | A | 400.971 | 385.522–426.864 | 17.495 | 191.29 |
| probes-janino | AD | 392.400 | 384.821–406.984 | 17.282 | 184.93 |
| probes-janino | C0 | 400.660 | 395.446–405.982 | 16.100 | 192.48 |
| probes-janino | C0D | 396.486 | 372.698–432.389 | 15.763 | 186.12 |
| probes-janino | C1 | 409.818 | 396.797–417.743 | 15.763 | 192.50 |
| probes-janino | C1D | 411.336 | 392.217–438.582 | 17.200 | 184.90 |

## Contrasts

Positive latency reduction favors the numerator. Throughput is reciprocal latency, not simply the negative percent latency change. Independent fork means, not individual measured iterations, are the statistical units.

| Cell | Contrast | Pooled latency reduction | Throughput change | Four paired blocks | Descriptive 95% interval |
|---|---|---:|---:|---|---|
| broad-regular | AD/A | +1.33% | +1.35% | +2.00%, +0.73%, +5.49%, -3.40% | -4.75% to +6.91% |
| broad-interpreted | AD/A | +7.63% | +8.25% | +3.80%, +8.58%, +7.12%, +10.91% | +2.80% to +12.24% |
| broad-janino | AD/A | +4.69% | +4.92% | +7.34%, +1.16%, +5.95%, +4.43% | +0.45% to +8.86% |
| probes-janino | AD/A | +2.14% | +2.18% | +9.10%, +0.18%, +1.40%, -2.72% | -6.43% to +9.92% |
| probes-janino | C0D/C0 | +1.04% | +1.05% | +5.36%, -6.50%, -1.08%, +6.47% | -8.80% to +10.28% |
| probes-janino | C1D/C1 | -0.37% | -0.37% | -10.53%, +1.05%, +1.43%, +6.11% | -11.94% to +10.12% |
| probes-janino | C0/A | +0.08% | +0.08% | +5.65%, -5.31%, -0.04%, -0.58% | -7.43% to +6.93% |
| probes-janino | C1/C0 | -2.29% | -2.23% | +1.47%, -2.48%, -3.35%, -4.83% | -6.69% to +1.96% |
| probes-janino | C0D/A | +1.12% | +1.13% | +10.71%, -12.16%, -1.11%, +5.93% | -15.60% to +15.58% |
| probes-janino | C1D/A | -2.58% | -2.52% | -2.75%, -6.78%, -1.91%, +1.00% | -7.80% to +2.40% |

Intervals use four paired log ratios, t(3) = 3.182, approximately normal independent block effects assumed. Exploratory, unadjusted for multiple comparisons; not a cross-workload guarantee.

## Combination interaction

On physical probes AD/A reduced pooled latency 2.14%, C0D/C0 1.04%, while C1D/C1 was 0.37% slower. Each depth contrast has mixed block signs and broad uncertainty. Combined owned ON plus depth was 2.58% slower than incumbent A. No demonstrated synergy; do not replace the incumbent with this combination based on these results.

Difference of paired log depth effects: log(CxD/Cx) minus log(AD/A). Negative means a larger depth effect on the cancellation configuration. This is not itself an absolute win versus A.

- probes-janino C0: blocks +0.04029, +0.06484, +0.02485, -0.09377; mean +0.00905, descriptive 95% interval [-0.10312,+0.12123].
- probes-janino C1: blocks +0.19550, -0.00870, -0.00026, -0.08993; mean +0.02415, descriptive 95% interval [-0.16867,+0.21697].

## Convergence and GC

All fork ranges and index trends retained. GC totals span startup/fixture/warmups/measurements; no timestamp join attributes a pause to a specific query. No GC filtering was used.

### broad-regular
- A measured-index means ms: 1573.85, 1502.07, 1604.69, 1610.56, 1594.69, 1593.26, 1522.50, 1554.41, 1568.67, 1499.12.
- AD measured-index means ms: 1634.79, 1584.61, 1556.26, 1544.21, 1496.29, 1463.79, 1531.44, 1493.89, 1541.71, 1569.21.
- broad-regular-01-A.log: mean 1525.235 ms, within-fork 1404.892–1601.443 ms; GC 66 pauses, total 1767.5 ms, max 75.2 ms.
- broad-regular-02-AD.log: mean 1494.782 ms, within-fork 1434.460–1590.125 ms; GC 66 pauses, total 1760.9 ms, max 79.1 ms.
- broad-regular-03-AD.log: mean 1521.315 ms, within-fork 1425.259–1705.857 ms; GC 65 pauses, total 1849.7 ms, max 72.3 ms.
- broad-regular-04-A.log: mean 1532.568 ms, within-fork 1406.578–1631.406 ms; GC 66 pauses, total 1977.7 ms, max 75.1 ms.
- broad-regular-05-A.log: mean 1686.591 ms, within-fork 1510.773–1881.089 ms; GC 66 pauses, total 2202.6 ms, max 70.2 ms.
- broad-regular-06-AD.log: mean 1594.037 ms, within-fork 1444.295–1743.309 ms; GC 66 pauses, total 2276.2 ms, max 85.1 ms.
- broad-regular-07-AD.log: mean 1556.346 ms, within-fork 1442.760–1682.246 ms; GC 65 pauses, total 2056.8 ms, max 77.2 ms.
- broad-regular-08-A.log: mean 1505.132 ms, within-fork 1389.048–1629.998 ms; GC 66 pauses, total 2005.0 ms, max 98.4 ms.

### broad-interpreted
- A measured-index means ms: 232.17, 231.93, 228.49, 230.34, 238.64, 249.81, 249.17, 231.35, 236.45, 229.79.
- AD measured-index means ms: 213.56, 210.48, 212.96, 210.60, 215.63, 216.84, 223.15, 239.04, 216.63, 219.42.
- broad-interpreted-01-A.log: mean 235.886 ms, within-fork 218.532–257.632 ms; GC 8 pauses, total 62.6 ms, max 15.0 ms.
- broad-interpreted-02-AD.log: mean 226.916 ms, within-fork 206.657–271.708 ms; GC 8 pauses, total 64.7 ms, max 14.5 ms.
- broad-interpreted-03-AD.log: mean 216.053 ms, within-fork 204.400–232.797 ms; GC 8 pauses, total 63.3 ms, max 15.9 ms.
- broad-interpreted-04-A.log: mean 236.334 ms, within-fork 220.016–272.109 ms; GC 8 pauses, total 60.9 ms, max 17.6 ms.
- broad-interpreted-05-A.log: mean 229.777 ms, within-fork 222.100–246.491 ms; GC 8 pauses, total 71.0 ms, max 20.3 ms.
- broad-interpreted-06-AD.log: mean 213.411 ms, within-fork 204.886–221.484 ms; GC 8 pauses, total 65.7 ms, max 25.3 ms.
- broad-interpreted-07-AD.log: mean 214.942 ms, within-fork 206.114–237.122 ms; GC 8 pauses, total 49.9 ms, max 17.0 ms.
- broad-interpreted-08-A.log: mean 241.252 ms, within-fork 225.718–258.009 ms; GC 8 pauses, total 64.1 ms, max 14.8 ms.

### broad-janino
- A measured-index means ms: 173.58, 170.47, 168.22, 169.70, 172.40, 175.34, 177.44, 164.66, 167.03, 163.00.
- AD measured-index means ms: 174.36, 161.15, 155.10, 159.86, 155.69, 161.65, 165.20, 167.83, 165.08, 156.08.
- broad-janino-01-A.log: mean 164.834 ms, within-fork 154.977–172.932 ms; GC 8 pauses, total 79.1 ms, max 21.5 ms.
- broad-janino-02-AD.log: mean 152.729 ms, within-fork 146.986–168.613 ms; GC 8 pauses, total 86.7 ms, max 26.8 ms.
- broad-janino-03-AD.log: mean 170.039 ms, within-fork 158.525–183.595 ms; GC 8 pauses, total 77.5 ms, max 20.4 ms.
- broad-janino-04-A.log: mean 172.035 ms, within-fork 158.350–182.973 ms; GC 8 pauses, total 83.2 ms, max 22.7 ms.
- broad-janino-05-A.log: mean 170.490 ms, within-fork 165.314–179.670 ms; GC 8 pauses, total 62.2 ms, max 17.9 ms.
- broad-janino-06-AD.log: mean 160.342 ms, within-fork 153.852–172.232 ms; GC 8 pauses, total 56.0 ms, max 14.6 ms.
- broad-janino-07-AD.log: mean 165.694 ms, within-fork 152.513–188.081 ms; GC 8 pauses, total 71.9 ms, max 21.2 ms.
- broad-janino-08-A.log: mean 173.372 ms, within-fork 166.332–186.873 ms; GC 8 pauses, total 68.0 ms, max 21.7 ms.

### probes-janino
- A measured-index means ms: 382.55, 375.78, 381.99, 505.17, 444.93, 374.77, 374.38, 388.39, 389.24, 392.52.
- AD measured-index means ms: 390.10, 400.65, 380.65, 385.19, 381.32, 472.30, 383.39, 377.33, 380.45, 372.62.
- C0 measured-index means ms: 387.43, 393.30, 394.49, 392.79, 402.56, 399.60, 416.65, 406.84, 404.61, 408.34.
- C0D measured-index means ms: 399.53, 395.01, 389.82, 383.43, 408.30, 468.39, 388.09, 373.54, 373.90, 384.86.
- C1 measured-index means ms: 412.47, 405.51, 402.62, 404.27, 416.27, 419.71, 402.56, 418.58, 413.10, 403.08.
- C1D measured-index means ms: 506.48, 410.65, 405.43, 414.05, 397.01, 398.74, 399.93, 389.22, 388.34, 403.51.
- probes-janino-01-A.log: mean 426.864 ms, within-fork 363.642–723.825 ms; GC 10 pauses, total 96.2 ms, max 29.7 ms.
- probes-janino-02-AD.log: mean 388.030 ms, within-fork 359.998–430.184 ms; GC 9 pauses, total 82.2 ms, max 16.8 ms.
- probes-janino-03-C0.log: mean 402.733 ms, within-fork 393.323–412.145 ms; GC 9 pauses, total 68.9 ms, max 11.8 ms.
- probes-janino-04-C0D.log: mean 381.146 ms, within-fork 365.625–400.000 ms; GC 9 pauses, total 83.6 ms, max 22.4 ms.
- probes-janino-05-C1.log: mean 396.797 ms, within-fork 385.590–412.903 ms; GC 9 pauses, total 79.6 ms, max 16.9 ms.
- probes-janino-06-C1D.log: mean 438.582 ms, within-fork 372.248–792.487 ms; GC 9 pauses, total 86.4 ms, max 19.7 ms.
- probes-janino-07-C0.log: mean 405.982 ms, within-fork 389.841–476.438 ms; GC 10 pauses, total 80.5 ms, max 14.0 ms.
- probes-janino-08-C0D.log: mean 432.389 ms, within-fork 377.920–685.082 ms; GC 9 pauses, total 85.2 ms, max 22.9 ms.
- probes-janino-09-C1.log: mean 416.033 ms, within-fork 395.684–431.153 ms; GC 9 pauses, total 78.9 ms, max 17.9 ms.
- probes-janino-10-C1D.log: mean 411.678 ms, within-fork 384.838–435.122 ms; GC 9 pauses, total 82.9 ms, max 19.5 ms.
- probes-janino-11-A.log: mean 385.522 ms, within-fork 366.251–465.216 ms; GC 9 pauses, total 69.5 ms, max 14.6 ms.
- probes-janino-12-AD.log: mean 384.821 ms, within-fork 371.939–411.168 ms; GC 9 pauses, total 89.1 ms, max 21.2 ms.
- probes-janino-13-C1D.log: mean 402.865 ms, within-fork 384.366–424.456 ms; GC 9 pauses, total 77.8 ms, max 15.9 ms.
- probes-janino-14-C1.log: mean 408.699 ms, within-fork 392.975–426.647 ms; GC 9 pauses, total 65.3 ms, max 11.0 ms.
- probes-janino-15-AD.log: mean 389.765 ms, within-fork 356.563–515.038 ms; GC 10 pauses, total 84.3 ms, max 14.8 ms.
- probes-janino-16-A.log: mean 395.307 ms, within-fork 368.907–522.178 ms; GC 9 pauses, total 114.2 ms, max 30.7 ms.
- probes-janino-17-C0D.log: mean 399.712 ms, within-fork 380.775–441.439 ms; GC 9 pauses, total 75.5 ms, max 15.4 ms.
- probes-janino-18-C0.log: mean 395.446 ms, within-fork 377.245–429.073 ms; GC 9 pauses, total 77.6 ms, max 13.7 ms.
- probes-janino-19-C0D.log: mean 372.698 ms, within-fork 353.403–404.019 ms; GC 9 pauses, total 75.3 ms, max 19.1 ms.
- probes-janino-20-C0.log: mean 398.482 ms, within-fork 374.183–426.498 ms; GC 9 pauses, total 107.2 ms, max 19.7 ms.
- probes-janino-21-A.log: mean 396.191 ms, within-fork 379.455–428.176 ms; GC 10 pauses, total 75.3 ms, max 15.1 ms.
- probes-janino-22-AD.log: mean 406.984 ms, within-fork 369.229–602.452 ms; GC 9 pauses, total 90.5 ms, max 21.2 ms.
- probes-janino-23-C1D.log: mean 392.217 ms, within-fork 383.432–402.214 ms; GC 9 pauses, total 182.0 ms, max 113.0 ms.
- probes-janino-24-C1.log: mean 417.743 ms, within-fork 392.088–453.279 ms; GC 9 pauses, total 95.8 ms, max 18.6 ms.

## Separate profiles

All 12 profile forks completed and matched 100k dual checksums: A/AD for the three broad routes and all six physical-probe arms. Each has 20 warmups excluded; regular records 5 queries and native 30. Profiles are separate from clean timings, trace=false. Inclusive sampled categories overlap; inlining and stack truncation hide frames. Counts are neither wall-time percentages nor exact calls. Single profile forks cannot assign the measured latency change to one map operation. Query-thread allocation figures above are clean-harness counters, not JFR allocation-event analysis.

| Profile | Main-thread samples | TLS visible | Read/lifetime/replay | Binding/copy | Value decode/memo |
|---|---:|---:|---:|---:|---:|
| broad-interpreted-A | 742 | 92 | 98 | 110 | 38 |
| broad-interpreted-AD | 707 | 71 | 71 | 112 | 40 |
| broad-janino-A | 503 | 92 | 74 | 116 | 50 |
| broad-janino-AD | 500 | 81 | 89 | 102 | 40 |
| broad-regular-A | 669 | 35 | 67 | 88 | 100 |
| broad-regular-AD | 711 | 32 | 60 | 125 | 110 |
| probes-janino-A | 849 | 195 | 149 | 106 | 38 |
| probes-janino-AD | 846 | 86 | 119 | 126 | 50 |
| probes-janino-C0 | 936 | 239 | 123 | 100 | 46 |
| probes-janino-C0D | 880 | 108 | 153 | 108 | 31 |
| probes-janino-C1 | 920 | 129 | 138 | 101 | 44 |
| probes-janino-C1D | 930 | 71 | 123 | 101 | 41 |

### Source change versus sampled map work

This is not merely a helper rename. The source removes the internalReadScopeDepth ThreadLocal instance, uses a distinct internalScopeDepth integer on the existing ReadThreadRegistration holder, and restores the captured holder field in finally rather than set/remove on a second map entry. Admission depth is still separate, and entry/exit placement is unchanged. Some reads now look up the registration ThreadLocal, so it is not removal of every TLS lookup or all connection scopes.

| Profile | TL get | TL set | TL remove | Map cleanSomeSlots | Map expungeStaleEntry |
|---|---:|---:|---:|---:|---:|
| broad-interpreted-A | 56 | 12 | 24 | 14 | 24 |
| broad-interpreted-AD | 52 | 16 | 3 | 22 | 2 |
| broad-janino-A | 36 | 39 | 17 | 53 | 13 |
| broad-janino-AD | 56 | 20 | 5 | 32 | 5 |
| broad-regular-A | 28 | 4 | 3 | 1 | 3 |
| broad-regular-AD | 32 | 0 | 0 | 1 | 0 |
| probes-janino-A | 162 | 30 | 3 | 34 | 3 |
| probes-janino-AD | 46 | 33 | 7 | 7 | 4 |
| probes-janino-C0 | 191 | 32 | 16 | 37 | 5 |
| probes-janino-C0D | 61 | 35 | 12 | 6 | 6 |
| probes-janino-C1 | 94 | 32 | 3 | 34 | 2 |
| probes-janino-C1D | 51 | 13 | 7 | 6 | 1 |

Broad interpreted remove samples fell 24 → 3 and broad Janino 17 → 5, while other TLS set/get/cleanup work remains. Physical-probe total TLS-visible samples fall with depth, but even unchanged cancellation/replay callers change their sampled distribution. Do not interpret that as removing those callers or exact per-call savings. JVM code generation, map layout and sampling variation can affect what is visible; the source-level removal is established, a precise causal timing decomposition is not.

### Remaining nearest visible ThreadLocal callers

- broad-interpreted-AD: (truncated caller) (11); org.eclipse.rdf4j.sail.lmdb.LmdbStoreConnection.executeObservedRead (8); org.eclipse.rdf4j.http.client.QueryExecutionContext.activateExternalValueInitializer (8); org.eclipse.rdf4j.sail.lmdb.LmdbStoreConnection.executeTrackedRead (7); org.eclipse.rdf4j.sail.lmdb.RepositoryValueIteration.prepareNext (6); org.eclipse.rdf4j.sail.lmdb.LmdbSailStore.enterReadResourceContext (4); org.eclipse.rdf4j.sail.lmdb.evaluation.LmdbFusedSipFactorizedRuntime.attach (4); org.eclipse.rdf4j.sail.lmdb.evaluation.LmdbFusedSipFactorizedRuntime$Scope.<init> (4).
- broad-janino-AD: (truncated caller) (16); org.eclipse.rdf4j.sail.lmdb.RepositoryValueIteration.prepareNext (10); org.eclipse.rdf4j.http.client.QueryExecutionContext.activateExternalValueInitializer (9); org.eclipse.rdf4j.sail.lmdb.LmdbStoreConnection.executeObservedRead (6); org.eclipse.rdf4j.sail.lmdb.LmdbStoreConnection.beginReadThread (5); org.eclipse.rdf4j.http.client.QueryExecutionContext.lambda$activateExternalValueInitializer$0 (5); org.eclipse.rdf4j.sail.lmdb.LmdbSailStore$ReadResourceContext.close (5); org.eclipse.rdf4j.http.client.QueryExecutionContext.setExternalValueInitializer (4).
- broad-regular-AD: org.eclipse.rdf4j.http.client.QueryExecutionContext.checkpointReplaySafepoint (12); org.eclipse.rdf4j.sail.lmdb.ValueStore.isWriteTransactionOwner (6); org.eclipse.rdf4j.sail.lmdb.LmdbStoreConnection.executeTrackedRead (3); org.eclipse.rdf4j.common.iteration.CancellationSignal.current (2); org.eclipse.rdf4j.sail.lmdb.LmdbSailStore.enterReadResourceContext (2); (truncated caller) (1); org.eclipse.rdf4j.http.client.QueryExecutionContext.activateExternalValueInitializer (1); org.eclipse.rdf4j.sail.lmdb.RepositoryValueIteration.prepareNext (1).
- probes-janino-AD: org.eclipse.rdf4j.http.client.QueryExecutionContext.activateExternalValueInitializer (12); org.eclipse.rdf4j.sail.lmdb.evaluation.NativeCancellationToken.checkCurrent (11); org.eclipse.rdf4j.sail.lmdb.ValueResolutionScope.activateForeground (11); org.eclipse.rdf4j.http.client.QueryExecutionContext.checkpointReplaySafepoint (10); (truncated caller) (7); org.eclipse.rdf4j.sail.lmdb.RepositoryValueIteration.prepareNext (6); org.eclipse.rdf4j.sail.lmdb.RepositoryValueIteration.restoreSourcePull (4); org.eclipse.rdf4j.sail.lmdb.evaluation.LmdbFusedSipFactorizedRuntime$Scope.<init> (4).
- probes-janino-C1D: org.eclipse.rdf4j.http.client.QueryExecutionContext.checkpointReplaySafepoint (12); org.eclipse.rdf4j.sail.lmdb.LmdbSailStore.enterReadResourceContext (11); org.eclipse.rdf4j.common.iteration.CancellationSignal.current (11); org.eclipse.rdf4j.sail.lmdb.evaluation.LmdbFusedSipFactorizedRuntime.attach (6); (truncated caller) (6); org.eclipse.rdf4j.sail.lmdb.LmdbSailStore$ReadResourceContext.close (4); org.eclipse.rdf4j.sail.lmdb.RepositoryValueIteration.prepareNext (4); org.eclipse.rdf4j.http.client.QueryExecutionContext.setExternalValueInitializer (3).

The next read-scope candidate is still resource/context restoration and replay ownership: enterReadResourceContext/close, external-value initializer activation/replay, begin/endReadThread, and replay safepoints remain visible. RepositoryValueIteration/source ownership, binding/copy and value work also remain. These profiles justify inspecting redundant nested setup while preserving callback/worker/replay/cancellation boundaries, not removing safeguards or claiming these scopes dominate every query. The separate resource candidate remains unimplemented in this experiment.

## Evidence

`PLAN.md`, exact source identities/class hashes/layer audit, qualification logs/summary, all matched raw logs/commands/GC and profile summaries retain the protocol and every observation. The 11.54 MB raw runtime manifest is retained locally; `runtime-manifest-deduplicated.json` losslessly reconstructs it byte-for-byte (1.45 MB). `deduplicate-manifest.py` verifies structure and original SHA. Profile recordings, full samples and large class/fixture trees remain local. `GROUPED-REPLAY-TRIAGE.md` records the shared gated-test limitation.

## Subsequent grouped-fixture qualification

The later [resource-restoration investigation](../resource-restoration/GROUPED-FIXTURE.md) confirmed the delayed memo-hit and availability-observation assumptions and repaired the Sail-level fixture without changing its query or weakening replay assertions. Its final gated selection passes all three cases on the incumbent, depth-only, and depth-plus-resource configurations with the separately identified test seam. This follow-up does not rewrite the historical outcomes or make the earlier selected suite a full repository pass.
