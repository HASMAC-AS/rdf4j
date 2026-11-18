# SOURCE_PULL and writer guard: four-arm results

**No established throughput/latency win or combination benefit yet.** Writer-guard-only has the strongest promising signal in Janino, but the small four-block uncertainty interval still includes no improvement. SOURCE_PULL alone did not improve the pooled means. The combination was not faster than writer-guard-only on regular or Janino. No samples were dropped and no further timing round was selected after seeing these outcomes.

## Arms and qualification

- A baseline: published `cba8ad48965a39c9bf7f179d6c75187b06d20e4a`, represented by the previous immutable measured c02-semantics runtime. This is not a claim that classes were rebuilt from the publication-only commit.
- B SOURCE_PULL only: `16abd01ee0b4bef9df3040944412b7e7ab2f288d`; one scoped hasNext/next sequence, snapshot and ordinary cleanup remain outside.
- C writer guard only: `2cb655f0747bbc256d228cad69bf0245bbcef1e8`; if shared writeTxn is zero, return false before readContext lookup. For an active writer retain the original lookup, second writeTxn read and ownership predicate.
- D both: `6c1c805b966cdfe9c70ffd889b61355887d87167`; independent class-prefix layers combined over A.
- Worker qualification: SOURCE_PULL default regression suite 202 passed and 10 compatible extra-flag cases passed. Five older grouped-wave tests failed identically on the before/after versions; assertions were not weakened. Guard-only and combined exact-runtime safety suites each passed 330 tests. Baseline white-box no-get test deliberately failed before the new guard, while the other five focused cases passed. These are scoped suites, not full repository validation.
- Each runtime/classpath and SHA is recorded in `four-arm-01/environment.txt`, `class-manifest.deduplicated.json`, `dependency-sha256.json`, `sources.sha256`, plus code-source witnesses in each fork. Local documentation-only HEAD is not the measured production identity.

## Fixed measurement protocol

48 fresh JVM forks: regular, native interpreted and Janino; 16 forks per route, four independent forks per arm. Four blocks in order A B D C / B C A D / C D B A / D A C B. Every fork uses 20 warmups followed by 10 measurements. No post-hoc shortening, outlier removal or GC filtering. JDK25.0.5, Xms2g/Xmx3g, canonical lock; no concurrent build/profile work. Exact commands and low-volume whole-JVM GC logs retained.

100 groups × fanout 10: 26,700 triples and 100,000 DISTINCT six-slot results. Same inherited measured-combination flags in all arms, including partialReady ON. Grouped value waves are OFF/default, so the five legacy extra-flag failures do not describe this active benchmark configuration. Do not call these flags universally best. Generic collections use heap-only delegate (sync threshold zero), native cap 640 MiB. All row counts, dual RDF-term-aware checksums, factories/routes/bind counters, no-spill checks and returned reservations passed.

Full latency includes connection open, prepare, evaluate, fully materialized consumption and result/connection close. First-row latency includes eager evaluation and projected lexical/datatype/language consumption. This is repository-level, not HTTP/Workbench/browser, upstream main, concurrent QPS or request-tail latency. Independent fork means, not the 40 in-JVM observations per arm, are the primary units.

## Arm means

| Route | Arm | Full ms | Fork-mean range ms | First row ms | Query-thread allocation MB |
|---|---|---:|---:|---:|---:|
| regular | A | 1510.512 | 1468.838–1590.615 | 7.834 | 2317.15 |
| regular | B | 1513.572 | 1502.812–1529.663 | 8.271 | 2315.03 |
| regular | C | 1474.816 | 1438.402–1511.731 | 7.495 | 2350.60 |
| regular | D | 1488.082 | 1443.470–1511.042 | 7.713 | 2349.42 |
| interpreted | A | 230.973 | 219.600–250.298 | 11.920 | 188.40 |
| interpreted | B | 232.879 | 229.383–237.157 | 11.935 | 187.87 |
| interpreted | C | 234.059 | 226.751–254.003 | 11.820 | 189.06 |
| interpreted | D | 230.709 | 221.886–238.172 | 11.728 | 187.86 |
| janino | A | 177.039 | 171.854–183.192 | 12.820 | 174.70 |
| janino | B | 181.272 | 158.660–190.664 | 13.240 | 176.24 |
| janino | C | 168.804 | 166.914–171.734 | 11.466 | 177.30 |
| janino | D | 172.846 | 169.319–177.314 | 11.622 | 175.43 |

Allocation is query-thread allocated bytes, not total worker allocation or retained heap.

## Factorial contrasts

Positive latency reduction favors the first arm. Throughput uses the reciprocal ratio, not the negated latency change.

| Route | Contrast | Pooled latency reduction | Throughput increase | Four paired block latency reductions |
|---|---|---:|---:|---|
| regular | B/A | -0.20% | -0.20% | -2.91%, +3.83%, -2.73%, +0.64% |
| regular | C/A | +2.36% | +2.42% | +1.41%, +9.57%, -2.10%, +0.05% |
| regular | D/A | +1.48% | +1.51% | +1.73%, +5.00%, -2.47%, +1.39% |
| regular | D/B | +1.68% | +1.71% | +4.51%, +1.22%, +0.25%, +0.76% |
| regular | D/C | -0.90% | -0.89% | +0.32%, -5.05%, -0.36%, +1.35% |
| interpreted | B/A | -0.82% | -0.82% | +7.58%, -7.99%, +1.24%, -5.37% |
| interpreted | C/A | -1.34% | -1.32% | -1.48%, -3.92%, +2.15%, -2.26% |
| interpreted | D/A | +0.11% | +0.11% | +6.48%, -1.04%, +1.53%, -7.41% |
| interpreted | D/B | +0.93% | +0.94% | -1.19%, +6.44%, +0.30%, -1.94% |
| interpreted | D/C | +1.43% | +1.45% | +7.84%, +2.77%, -0.63%, -5.04% |
| janino | B/A | -2.39% | -2.34% | -8.03%, +10.53%, -8.16%, -4.08% |
| janino | C/A | +4.65% | +4.88% | +2.30%, +5.88%, +1.82%, +8.38% |
| janino | D/A | +2.37% | +2.43% | +2.99%, +4.52%, -1.38%, +3.21% |
| janino | D/B | +4.65% | +4.87% | +10.20%, -6.72%, +6.27%, +7.00% |
| janino | D/C | -2.39% | -2.34% | +0.70%, -1.44%, -3.26%, -5.64% |

## Uncertainty and combination effects

The following descriptive intervals use paired log-latency contrasts across four balanced blocks, t critical value 3.182 (3 degrees of freedom). They assume independent approximately normal block effects, are not adjusted for the multiple explored contrasts, and should not be mistaken for a strong confirmatory study. Negative reduction means slower. The detailed block spread is retained above rather than hidden behind one percentage.

| Route | Contrast | Paired geometric latency reduction | Descriptive 95% interval |
|---|---|---:|---:|
| regular | B/A | -0.25% | -5.50% to +4.74% |
| regular | C/A | +2.33% | -6.31% to +10.27% |
| regular | D/A | +1.45% | -3.53% to +6.19% |
| regular | D/C | -0.91% | -5.46% to +3.45% |
| interpreted | B/A | -0.95% | -12.83% to +9.67% |
| interpreted | C/A | -1.35% | -5.54% to +2.67% |
| interpreted | D/A | +0.01% | -9.58% to +8.77% |
| interpreted | D/C | +1.35% | -7.72% to +9.66% |
| janino | B/A | -2.13% | -17.86% to +11.50% |
| janino | C/A | +4.63% | -0.46% to +9.47% |
| janino | D/A | +2.36% | -1.77% to +6.32% |
| janino | D/C | -2.38% | -6.76% to +1.82% |

Interaction uses log(D)−log(B)−log(C)+log(A). Ratio D×A/(B×C) below 1 would mean lower combined latency than the multiplicative independent prediction, not simply that D is faster than A.

| Route | Geometric interaction ratio | Descriptive 95% interval | Four block ratios |
|---|---:|---:|---|
| regular | 1.0065 | 0.9214–1.0995 | 0.9686, 1.0924, 0.9770, 0.9929 |
| interpreted | 0.9772 | 0.8945–1.0675 | 0.9971, 0.9003, 1.0189, 0.9969 |
| janino | 1.0025 | 0.8664–1.1599 | 0.9192, 1.1338, 0.9547, 1.0150 |

All interaction intervals include 1. There is no established synergy. The general observation that optimizations can combine remains valid; this experiment does not demonstrate that these two do so beneficially.

Guard-only Janino has four positive paired effects (2.30%, 5.88%, 1.82%, 8.38%) and pooled 4.65% lower latency. It is worth distinguishing this consistent directional hint from SOURCE_PULL and the combination, but its descriptive interval still spans zero. D is 2.39% slower than C in Janino pooled means. This is not a basis to promote D as the best throughput configuration.

## Convergence and GC evidence

Measured-index mean latencies below retain the fixed 10-sample window. No warmup was changed after outcomes. Per-fork min/max and every sample remain in summary/raw logs. GC summaries cover the whole JVM, including fixture/readiness and warmups; they cannot be assigned to a particular measured query without an exact timestamp join and do not justify deleting observations.

### regular
- A measured-index means ms: 1478.68, 1590.33, 1649.31, 1504.12, 1468.83, 1474.80, 1476.97, 1488.31, 1516.92, 1456.85.
- B measured-index means ms: 1549.78, 1581.02, 1542.69, 1510.62, 1499.54, 1588.50, 1483.39, 1467.27, 1441.01, 1471.89.
- C measured-index means ms: 1540.22, 1472.75, 1443.62, 1455.39, 1427.71, 1471.56, 1448.78, 1544.24, 1484.98, 1458.92.
- D measured-index means ms: 1534.16, 1573.92, 1577.90, 1439.42, 1484.84, 1462.24, 1468.48, 1456.71, 1430.41, 1452.74.
- regular-01-A.log: mean 1468.838ms, measured range 1370.170–1536.692ms; whole-JVM 65 GC pauses, total 1841.2ms, max 83.4ms.
- regular-02-B.log: mean 1511.576ms, measured range 1381.034–1775.069ms; whole-JVM 63 GC pauses, total 1844.9ms, max 90.2ms.
- regular-03-D.log: mean 1443.470ms, measured range 1353.328–1668.785ms; whole-JVM 66 GC pauses, total 1759.4ms, max 57.4ms.
- regular-04-C.log: mean 1448.130ms, measured range 1360.328–1639.040ms; whole-JVM 66 GC pauses, total 1953.5ms, max 71.6ms.
- regular-05-B.log: mean 1529.663ms, measured range 1436.749–1698.341ms; whole-JVM 66 GC pauses, total 1908.6ms, max 85.3ms.
- regular-06-C.log: mean 1438.402ms, measured range 1349.655–1663.999ms; whole-JVM 66 GC pauses, total 1703.1ms, max 68.9ms.
- regular-07-A.log: mean 1590.615ms, measured range 1467.450–1883.460ms; whole-JVM 66 GC pauses, total 1859.5ms, max 80.6ms.
- regular-08-D.log: mean 1511.042ms, measured range 1368.690–1657.412ms; whole-JVM 66 GC pauses, total 1868.2ms, max 81.1ms.
- regular-09-C.log: mean 1501.002ms, measured range 1444.843–1545.623ms; whole-JVM 66 GC pauses, total 2012.2ms, max 95.7ms.
- regular-10-D.log: mean 1506.426ms, measured range 1423.483–1692.204ms; whole-JVM 66 GC pauses, total 1799.1ms, max 101.1ms.
- regular-11-B.log: mean 1510.236ms, measured range 1395.714–1616.173ms; whole-JVM 66 GC pauses, total 1763.8ms, max 86.9ms.
- regular-12-A.log: mean 1470.155ms, measured range 1404.396–1597.321ms; whole-JVM 63 GC pauses, total 1875.4ms, max 80.3ms.
- regular-13-D.log: mean 1491.392ms, measured range 1380.268–1581.348ms; whole-JVM 66 GC pauses, total 1800.7ms, max 68.9ms.
- regular-14-A.log: mean 1512.438ms, measured range 1428.067–1603.255ms; whole-JVM 66 GC pauses, total 1732.7ms, max 63.4ms.
- regular-15-C.log: mean 1511.731ms, measured range 1396.827–1589.126ms; whole-JVM 66 GC pauses, total 1735.4ms, max 60.5ms.
- regular-16-B.log: mean 1502.812ms, measured range 1427.558–1587.917ms; whole-JVM 66 GC pauses, total 1818.7ms, max 60.9ms.

### interpreted
- A measured-index means ms: 225.01, 228.09, 226.39, 221.62, 232.99, 269.59, 229.47, 221.07, 229.29, 226.20.
- B measured-index means ms: 229.72, 233.90, 236.37, 230.50, 241.48, 236.11, 246.56, 227.29, 224.59, 222.27.
- C measured-index means ms: 229.34, 231.62, 233.92, 228.49, 238.98, 254.49, 235.65, 224.33, 235.78, 227.98.
- D measured-index means ms: 216.05, 229.57, 224.70, 228.66, 240.39, 238.80, 242.19, 220.85, 237.86, 228.02.
- interpreted-01-A.log: mean 250.298ms, measured range 219.750–363.688ms; whole-JVM 9 GC pauses, total 77.1ms, max 15.7ms.
- interpreted-02-B.log: mean 231.338ms, measured range 224.837–240.925ms; whole-JVM 8 GC pauses, total 60.3ms, max 14.4ms.
- interpreted-03-D.log: mean 234.084ms, measured range 210.565–294.363ms; whole-JVM 8 GC pauses, total 77.7ms, max 23.0ms.
- interpreted-04-C.log: mean 254.003ms, measured range 237.039–301.281ms; whole-JVM 8 GC pauses, total 102.7ms, max 45.4ms.
- interpreted-05-B.log: mean 237.157ms, measured range 219.701–269.217ms; whole-JVM 8 GC pauses, total 90.0ms, max 36.2ms.
- interpreted-06-C.log: mean 228.214ms, measured range 221.062–241.191ms; whole-JVM 8 GC pauses, total 65.6ms, max 25.0ms.
- interpreted-07-A.log: mean 219.600ms, measured range 209.210–231.377ms; whole-JVM 8 GC pauses, total 69.7ms, max 27.2ms.
- interpreted-08-D.log: mean 221.886ms, measured range 207.980–237.287ms; whole-JVM 8 GC pauses, total 59.9ms, max 18.7ms.
- interpreted-09-C.log: mean 227.269ms, measured range 216.088–237.185ms; whole-JVM 8 GC pauses, total 64.8ms, max 20.2ms.
- interpreted-10-D.log: mean 228.693ms, measured range 218.353–241.001ms; whole-JVM 8 GC pauses, total 86.6ms, max 30.0ms.
- interpreted-11-B.log: mean 229.383ms, measured range 219.073–239.409ms; whole-JVM 8 GC pauses, total 83.2ms, max 24.9ms.
- interpreted-12-A.log: mean 232.257ms, measured range 217.285–250.510ms; whole-JVM 8 GC pauses, total 71.8ms, max 21.3ms.
- interpreted-13-D.log: mean 238.172ms, measured range 220.514–274.008ms; whole-JVM 8 GC pauses, total 115.5ms, max 46.3ms.
- interpreted-14-A.log: mean 221.738ms, measured range 210.870–238.789ms; whole-JVM 8 GC pauses, total 66.5ms, max 17.7ms.
- interpreted-15-C.log: mean 226.751ms, measured range 218.126–247.077ms; whole-JVM 8 GC pauses, total 63.0ms, max 24.3ms.
- interpreted-16-B.log: mean 233.637ms, measured range 220.856–292.615ms; whole-JVM 8 GC pauses, total 115.8ms, max 71.5ms.

### janino
- A measured-index means ms: 179.32, 171.14, 171.96, 172.90, 185.68, 179.72, 184.34, 181.23, 174.90, 169.21.
- B measured-index means ms: 174.03, 183.33, 170.10, 184.20, 195.63, 184.43, 190.73, 194.49, 173.41, 162.38.
- C measured-index means ms: 162.70, 161.46, 160.13, 163.34, 173.43, 170.80, 184.99, 171.68, 170.95, 168.57.
- D measured-index means ms: 172.39, 162.11, 160.97, 174.60, 177.54, 176.50, 182.85, 190.57, 169.29, 161.65.
- janino-01-A.log: mean 175.778ms, measured range 160.507–198.113ms; whole-JVM 8 GC pauses, total 70.3ms, max 21.2ms.
- janino-02-B.log: mean 189.890ms, measured range 163.159–237.315ms; whole-JVM 8 GC pauses, total 68.1ms, max 21.8ms.
- janino-03-D.log: mean 170.524ms, measured range 153.900–199.076ms; whole-JVM 8 GC pauses, total 55.4ms, max 12.8ms.
- janino-04-C.log: mean 171.734ms, measured range 158.131–190.318ms; whole-JVM 8 GC pauses, total 73.3ms, max 22.2ms.
- janino-05-B.log: mean 158.660ms, measured range 151.635–171.433ms; whole-JVM 8 GC pauses, total 65.1ms, max 16.9ms.
- janino-06-C.log: mean 166.914ms, measured range 158.970–177.574ms; whole-JVM 8 GC pauses, total 54.3ms, max 16.7ms.
- janino-07-A.log: mean 177.333ms, measured range 165.518–203.578ms; whole-JVM 8 GC pauses, total 61.4ms, max 20.5ms.
- janino-08-D.log: mean 169.319ms, measured range 158.529–176.872ms; whole-JVM 8 GC pauses, total 57.6ms, max 13.8ms.
- janino-09-C.log: mean 168.725ms, measured range 160.722–180.745ms; whole-JVM 8 GC pauses, total 54.8ms, max 12.2ms.
- janino-10-D.log: mean 174.227ms, measured range 154.867–222.884ms; whole-JVM 8 GC pauses, total 71.8ms, max 20.6ms.
- janino-11-B.log: mean 185.875ms, measured range 161.584–244.295ms; whole-JVM 8 GC pauses, total 90.0ms, max 38.0ms.
- janino-12-A.log: mean 171.854ms, measured range 161.460–183.437ms; whole-JVM 8 GC pauses, total 110.5ms, max 60.3ms.
- janino-13-D.log: mean 177.314ms, measured range 159.786–209.872ms; whole-JVM 8 GC pauses, total 67.1ms, max 19.9ms.
- janino-14-A.log: mean 183.192ms, measured range 152.085–194.339ms; whole-JVM 8 GC pauses, total 82.1ms, max 24.6ms.
- janino-15-C.log: mean 167.842ms, measured range 154.615–199.724ms; whole-JVM 8 GC pauses, total 84.1ms, max 32.2ms.
- janino-16-B.log: mean 190.664ms, measured range 169.007–238.018ms; whole-JVM 8 GC pauses, total 70.7ms, max 21.9ms.

## Separate profiles

All 12 diagnostic profiles completed with the expected dual checksums. Fresh JVMs, 20 warmups excluded; regular 5 queries and native routes 30 queries recorded using JFR profile settings. No JFR/PrintCompilation was enabled in clean timing. These longer diagnostic workloads and their latencies are not pooled into the clean experiment.

| Profile | Main-thread samples | Any ThreadLocal frame | Inclusive sample share | Visible RepositoryValueIteration TL caller samples |
|---|---:|---:|---:|---:|
| interpreted-A | 709 | 99 | 14.0% | 10 |
| interpreted-B | 679 | 97 | 14.3% | 7 |
| interpreted-C | 680 | 68 | 10.0% | 9 |
| interpreted-D | 673 | 104 | 15.5% | 3 |
| janino-A | 499 | 97 | 19.4% | 10 |
| janino-B | 478 | 118 | 24.7% | 8 |
| janino-C | 603 | 134 | 22.2% | 8 |
| janino-D | 500 | 117 | 23.4% | 5 |
| regular-A | 783 | 25 | 3.2% | 2 |
| regular-B | 738 | 48 | 6.5% | 2 |
| regular-C | 805 | 29 | 3.6% | 3 |
| regular-D | 722 | 38 | 5.3% | 1 |

These are inclusive sampled stacks, not wall-time percentages, exact invocation counts, or attainable speedups. Stack truncation/inlining and different denominators matter. Each arm has one profile per route, so changed sample fractions alone do not establish a causal improvement.

SOURCE_PULL helper names disappear after consolidation, but work moves into prepareNext. The table aggregates all visible RepositoryValueIteration ThreadLocal callers, not just old sourceHasNext/sourceNext names. The static successful-row count changes from 2 gets/4 sets to 1 get/2 sets; that is not a measured machine-instruction count or a 50% total-scope saving. Guard tests establish zero readContext.get only when writeTxn is zero; other ValueStore lookups and active-writer cases remain.

**Remaining priority:** read/resource/replay boundary context churn still appears broadly. In combined Janino D, visible nearest ThreadLocal callers include executeObservedRead (25 samples), activateExternalValueInitializer (14), enterReadResourceContext (10), executeTrackedRead (10), fused SIP attach (8), CancellationSignal.current (5), beginReadThread (5), and ReadResourceContext.close (5), among 500 main-thread samples. Counts are not exclusive wall-time budgets. RepositoryValueIteration-related callers contribute only 5 visible samples in that profile.

This points to examining the surrounding observed/tracked read and external-value/resource-context scopes with ownership and nested-callback semantics intact. It does not authorize removing lifecycle checks or justify automatically launching another benchmark round. Generated IR/Janino alone cannot remove scope work in the outer repository wrappers.

**Decision:** writer-guard-only Janino is promising but not established. There is no demonstrated synergy or combined superiority. Do not promote D as the best throughput configuration on this evidence. Preserve the tests and artifacts; no additional unrequested measurement round was run.



## Evidence

- `four-arm-01/summary.json`: arm/fork data, block contrasts, interaction, first-row, allocations and GC summaries.
- `four-arm-01/*.log`, `*.gc.log`, `*.command`: all 48 clean forks plus reference, no filtered runs.
- `FOUR-ARM-PLAN.md`: prespecified protocol; `run-four-arm.sh`, `summarize-four-arm.py` reproduce execution/analysis.
- `published-identities.txt` and immutable runtime/class manifests retain exact A/B/C/D provenance.
- Profile `.jfr` and sample JSON remain local evidence; compact summaries and logs can be checkpointed without publishing binary recordings.

The published class manifest is losslessly semantically deduplicated: each arm retains its ordered list of entry references, and every distinct entry retains its full class hashes. Expanding those references exactly reproduces the original JSON data; its original file SHA256 is included. The original 8.76MB formatting-heavy file remains local.
