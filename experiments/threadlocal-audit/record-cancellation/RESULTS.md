# Direct record cancellation: three-arm performance evidence

**The owned fast path did not improve throughput/latency on the qualified repeated-probe workload.** Both targeted engine routes were slower with the flag enabled than with identical candidate bytes and the flag disabled. The interpreted total change versus the previous runtime was also consistently adverse. Keep this a correctness-tested experiment, not a performance recommendation or a default-enable justification. Generic support must be assessed separately; default-off does not make the whole patch cost-free.

## Exact comparison and safety scope

- A: previous combined D runtime, published source baseline `5ac89b5c137030fea1aa6aa7160984423d7f4cd5`. Baseline was chosen before measurements, not replaced by guard-only C after its earlier favorable pooled Janino result.
- B0: candidate `7ac1650a871b86f422dc4ec1aaf28d0a6742759f`, owned feature OFF. This includes generic cooperative-cancellation changes.
- B1: identical candidate bytes, owned feature ON. B0→B1 isolates the feature-switch comparison; A→B0 covers the other candidate changes; A→B1 is total change.
- Candidate runtime uses exactly ten changed production prefixes (215 outer/inner classes) layered over immutable A. The full build differed in 94 classes; the only change outside those prefixes was NativeCancellationCheckpoint. Its `javap -p -c -s` output was identical, indicating debug-only differences, and that rebuilt class was excluded anyway. Source/class hashes and comparison evidence are retained. No stale local HEAD is used as production identity.
- Worker safety selections passed 213 tests with owned ON/trace ON and 213 with owned OFF/trace OFF, same candidate build. This is scoped coverage, not a full repository pass. The qualification does not include an integrated forced pre-execution binder test; do not present helper/admission tests as proof of every such end-to-end interleaving.

## Physical-path qualification, separate from timing

Tiny 27-cell diagnostics and final selected-layer requalification checked A/B0/B1 outputs and routes. Old A lacks the diagnostic fields and is reported as unavailable, not measured zero. B0 owned counters were zero. Broad adjacency-enabled queries had no owned physical-record hits, so they are no-hit controls, not evidence that the owned mechanism was exercised. The flag can still introduce setup/other overhead in a no-hit route.

Direct-adjacency-disabled repeated-probe interpreted and Janino cells had actual physical owned hits. At the selected 100-group size, each untimed B1 run recorded **899,301 binds, 2,697,873 polls and 899,271 resets**, with 100,000 rows and matching full-term dual checksums. These are trace-enabled diagnostic counts, not timing results or an allocation count. Clean forks explicitly disable tracing.

Plain full-scan native/Janino forced routes declined because there was no fusion opportunity; those cells were excluded before timing. They were not relabeled as successful compiled scans. The regular plain scan succeeded but had no owned hits. Five cells were fixed before measurement: three broad routes plus two physically qualified no-adjacency probe routes.

## Clean timing protocol

60 fresh JVM forks: five cells × three arms × four forks. Per cell fixed order A B0 B1 / B0 B1 A / B1 A B0 / B1 B0 A; each fork uses 20 warmups and 10 measurements. Three cyclic blocks plus one reversed block are near-balanced, not exactly position-balanced (four blocks cannot divide equally among three positions). All samples retained; no post-hoc shortening, trimming or additional timing round.

Same 100-group, fanout-10, six-slot OPTIONAL fixture: 26,700 triples and 100,000 DISTINCT results. All arms match checksums 1899306305404719472 and 3694550068168048289, factories/routes/bind counters, zero native spill/migration and returned reservations. Generic collection threshold zero pins heap-only collections. Native cap 640 MiB, inherited measured-combination flags including partialReady ON/grouped waves OFF. JDK25.0.5, Xms2g/Xmx3g. GC-only logs in clean forks; JFR is separate and trace counters are OFF.

First row includes eager evaluation and complete projected-value consumption. Full latency includes connection open, prepare, evaluate, full consumption, result/connection close. Query-thread allocations are not retained heap/total worker allocations. This is experimental combined D versus a cancellation patch, **not main, HTTP, Workbench, browser rendering, concurrent QPS or request-tail latency**. Cancellation acknowledgement/cleanup bounds are separate correctness tests, not inferred from throughput.

## Arm means

| Cell | Arm | Full ms | Fork-mean range ms | First row ms | Query-thread allocation MB |
|---|---|---:|---:|---:|---:|
| broad-regular | A | 1531.662 | 1465.648–1581.141 | 7.895 | 2334.13 |
| broad-regular | B0 | 1530.651 | 1417.704–1612.839 | 8.371 | 2348.33 |
| broad-regular | B1 | 1513.387 | 1484.096–1560.482 | 8.568 | 2309.24 |
| broad-interpreted | A | 233.156 | 218.074–244.967 | 12.233 | 190.22 |
| broad-interpreted | B0 | 226.947 | 216.436–246.664 | 12.052 | 188.43 |
| broad-interpreted | B1 | 232.912 | 224.088–239.156 | 12.166 | 187.84 |
| broad-janino | A | 169.990 | 164.478–176.840 | 12.698 | 180.53 |
| broad-janino | B0 | 179.726 | 170.756–193.213 | 12.922 | 180.07 |
| broad-janino | B1 | 172.795 | 168.620–180.434 | 11.169 | 177.64 |
| probes-interpreted | A | 447.580 | 430.770–454.341 | 15.307 | 195.36 |
| probes-interpreted | B0 | 466.148 | 457.766–474.064 | 18.763 | 194.17 |
| probes-interpreted | B1 | 480.588 | 461.231–500.002 | 16.585 | 195.40 |
| probes-janino | A | 401.332 | 386.542–431.215 | 16.861 | 191.14 |
| probes-janino | B0 | 399.962 | 385.884–417.590 | 16.871 | 191.27 |
| probes-janino | B1 | 418.260 | 408.304–433.386 | 16.063 | 193.73 |

## Contrasts and paired blocks

Positive reduction favors the first arm; throughput is the reciprocal ratio, not simply negated latency change. Four independent fork means per arm are the analysis units, not 40 independent in-JVM measurements.

| Cell | Contrast | Pooled latency reduction | Throughput change | Four paired block reductions |
|---|---|---:|---:|---|
| broad-regular | B0/A | +0.07% | +0.07% | -0.06%, -7.20%, -3.02%, +10.01% |
| broad-regular | B1/B0 | +1.13% | +1.14% | +6.20%, +5.52%, -3.35%, -4.76% |
| broad-regular | B1/A | +1.19% | +1.21% | +6.14%, -1.28%, -6.47%, +5.72% |
| broad-interpreted | B0/A | +2.66% | +2.74% | -0.52%, +10.71%, -0.69%, +0.75% |
| broad-interpreted | B1/B0 | -2.63% | -2.56% | -8.50%, -3.54%, +6.52%, -6.06% |
| broad-interpreted | B1/A | +0.10% | +0.10% | -9.06%, +7.55%, +5.87%, -5.27% |
| broad-janino | B0/A | -5.73% | -5.42% | -1.45%, -11.66%, -9.26%, -0.57% |
| broad-janino | B1/B0 | +3.86% | +4.01% | -5.67%, +8.18%, +11.28%, +0.33% |
| broad-janino | B1/A | -1.65% | -1.62% | -7.20%, -2.52%, +3.07%, -0.23% |
| probes-interpreted | B0/A | -4.15% | -3.98% | -2.65%, -4.57%, -0.75%, -8.86% |
| probes-interpreted | B1/B0 | -3.10% | -3.00% | -7.80%, +2.71%, -6.64%, -0.86% |
| probes-interpreted | B1/A | -7.37% | -6.87% | -10.66%, -1.74%, -7.44%, -9.80% |
| probes-janino | B0/A | +0.34% | +0.34% | +8.58%, +2.19%, -2.32%, -8.03% |
| probes-janino | B1/B0 | -4.57% | -4.37% | -7.05%, -5.81%, -1.78%, -3.78% |
| probes-janino | B1/A | -4.22% | -4.05% | +2.13%, -3.49%, -4.14%, -12.12% |

## Uncertainty and interpretation

These descriptive intervals use paired log-latency contrasts across four blocks and t critical value 3.182 (3 degrees of freedom). They assume independent approximately normal block effects and are not adjusted for multiple comparisons. They describe this fixture, not a general cross-workload guarantee. Negative reduction means slower.

| Cell | Contrast | Paired geometric latency reduction | Descriptive 95% interval |
|---|---|---:|---:|
| broad-regular | B0/A | +0.14% | -12.50% to +11.36% |
| broad-regular | B1/B0 | +1.03% | -8.57% to +9.78% |
| broad-regular | B1/A | +1.16% | -8.84% to +10.25% |
| broad-interpreted | B0/A | +2.68% | -6.68% to +11.22% |
| broad-interpreted | B1/B0 | -2.73% | -14.06% to +7.48% |
| broad-interpreted | B1/A | +0.03% | -13.84% to +12.20% |
| broad-janino | B0/A | -5.62% | -14.82% to +2.84% |
| broad-janino | B1/B0 | +3.76% | -9.16% to +15.15% |
| broad-janino | B1/A | -1.65% | -8.72% to +4.96% |
| probes-interpreted | B0/A | -4.16% | -9.80% to +1.18% |
| probes-interpreted | B1/B0 | -3.06% | -11.27% to +4.55% |
| probes-interpreted | B1/A | -7.35% | -14.03% to -1.06% |
| probes-janino | B0/A | +0.29% | -11.58% to +10.90% |
| probes-janino | B1/B0 | -4.59% | -8.35% to -0.96% |
| probes-janino | B1/A | -4.28% | -13.97% to +4.58% |

- Broad regular/interpreted pooled differences are small/inconsistent. Broad Janino B0 was slower than A in all four blocks and 5.73% slower pooled, but its interval is broad. This is adverse evidence to retain, not sufficient to causally assign one extra generic-support instruction or assert universal regression.
- Actual-hit interpreted probes: A447.58ms, B0 466.15ms, B1 480.59ms; B1/A is 7.37% slower pooled, all four paired signs adverse. The descriptive total-change interval is also adverse, though limited to four blocks and this workload.
- Actual-hit Janino probes: A401.33ms, B0 399.96ms, B1 418.26ms. B1/B0 is 4.57% slower pooled and every paired feature-on comparison is adverse. Its descriptive interval excludes zero under the stated assumptions. The owned path was exercised; a missed-route explanation does not fit this diagnostic evidence.
- Removing ambient lookup is not automatically an overall win. Do not infer which added check/bind/reset operation caused the slowdown solely from these pooled timings.

## Convergence and whole-JVM GC evidence

Measured-index trends and complete fork ranges follow. GC counts/totals include startup, fixture/readiness and warmup, not just the measurement window. No GC-triggered sample deletion was done; these logs cannot causally explain a particular sample without an exact timestamp join.

### broad-regular
- A measured-index means ms: 1542.45, 1579.79, 1485.58, 1519.00, 1665.75, 1506.41, 1523.27, 1476.18, 1505.75, 1512.44.
- B0 measured-index means ms: 1576.72, 1571.57, 1567.72, 1523.92, 1512.87, 1521.14, 1547.51, 1471.40, 1480.61, 1533.05.
- B1 measured-index means ms: 1578.25, 1635.50, 1662.45, 1478.50, 1488.84, 1476.29, 1456.00, 1448.79, 1436.35, 1472.89.
- broad-regular-01-A.log: 1581.141ms, within-fork 1499.397–1664.324ms; whole-JVM GC 66 pauses, total 2143.4ms, max 101.1ms.
- broad-regular-02-B0.log: 1582.162ms, within-fork 1404.055–1834.607ms; whole-JVM GC 66 pauses, total 1932.7ms, max 58.1ms.
- broad-regular-03-B1.log: 1484.096ms, within-fork 1376.099–1572.519ms; whole-JVM GC 66 pauses, total 1745.8ms, max 65.6ms.
- broad-regular-04-B0.log: 1612.839ms, within-fork 1485.853–1842.060ms; whole-JVM GC 66 pauses, total 2109.3ms, max 160.9ms.
- broad-regular-05-B1.log: 1523.741ms, within-fork 1428.332–1680.627ms; whole-JVM GC 63 pauses, total 1840.2ms, max 77.3ms.
- broad-regular-06-A.log: 1504.527ms, within-fork 1410.924–1597.713ms; whole-JVM GC 66 pauses, total 1971.0ms, max 90.4ms.
- broad-regular-07-B1.log: 1560.482ms, within-fork 1424.262–1871.349ms; whole-JVM GC 65 pauses, total 1975.7ms, max 74.5ms.
- broad-regular-08-A.log: 1465.648ms, within-fork 1412.185–1494.684ms; whole-JVM GC 66 pauses, total 1962.2ms, max 126.2ms.
- broad-regular-09-B0.log: 1509.898ms, within-fork 1435.827–1639.103ms; whole-JVM GC 66 pauses, total 1790.5ms, max 112.8ms.
- broad-regular-10-B1.log: 1485.226ms, within-fork 1407.006–1589.103ms; whole-JVM GC 66 pauses, total 1780.5ms, max 71.8ms.
- broad-regular-11-B0.log: 1417.704ms, within-fork 1364.510–1492.668ms; whole-JVM GC 65 pauses, total 1761.9ms, max 74.6ms.
- broad-regular-12-A.log: 1575.331ms, within-fork 1411.435–1920.248ms; whole-JVM GC 65 pauses, total 1904.6ms, max 77.9ms.

### broad-interpreted
- A measured-index means ms: 227.39, 226.09, 243.10, 230.07, 240.33, 241.95, 231.68, 224.02, 244.24, 222.70.
- B0 measured-index means ms: 219.41, 230.94, 219.48, 231.05, 234.58, 236.89, 228.67, 222.50, 227.60, 218.35.
- B1 measured-index means ms: 225.48, 231.92, 226.14, 236.44, 239.06, 236.25, 240.90, 231.80, 234.14, 226.98.
- broad-interpreted-01-A.log: 218.074ms, within-fork 206.475–236.329ms; whole-JVM GC 8 pauses, total 51.2ms, max 12.8ms.
- broad-interpreted-02-B0.log: 219.204ms, within-fork 206.181–234.154ms; whole-JVM GC 8 pauses, total 73.8ms, max 19.1ms.
- broad-interpreted-03-B1.log: 237.827ms, within-fork 222.504–258.353ms; whole-JVM GC 8 pauses, total 61.0ms, max 14.1ms.
- broad-interpreted-04-B0.log: 216.436ms, within-fork 209.971–223.800ms; whole-JVM GC 8 pauses, total 60.7ms, max 19.1ms.
- broad-interpreted-05-B1.log: 224.088ms, within-fork 216.473–235.569ms; whole-JVM GC 8 pauses, total 65.0ms, max 15.9ms.
- broad-interpreted-06-A.log: 242.391ms, within-fork 231.491–272.591ms; whole-JVM GC 8 pauses, total 67.0ms, max 19.6ms.
- broad-interpreted-07-B1.log: 230.578ms, within-fork 219.695–243.014ms; whole-JVM GC 8 pauses, total 52.4ms, max 13.9ms.
- broad-interpreted-08-A.log: 244.967ms, within-fork 222.248–311.207ms; whole-JVM GC 8 pauses, total 57.2ms, max 15.0ms.
- broad-interpreted-09-B0.log: 246.664ms, within-fork 220.570–262.007ms; whole-JVM GC 8 pauses, total 76.3ms, max 25.1ms.
- broad-interpreted-10-B1.log: 239.156ms, within-fork 228.503–260.818ms; whole-JVM GC 8 pauses, total 77.0ms, max 29.4ms.
- broad-interpreted-11-B0.log: 225.485ms, within-fork 211.278–245.995ms; whole-JVM GC 8 pauses, total 58.7ms, max 16.2ms.
- broad-interpreted-12-A.log: 227.193ms, within-fork 218.605–243.462ms; whole-JVM GC 8 pauses, total 53.9ms, max 18.6ms.

### broad-janino
- A measured-index means ms: 172.35, 163.09, 161.85, 164.41, 173.15, 179.89, 175.98, 181.38, 168.42, 159.37.
- B0 measured-index means ms: 180.97, 161.67, 172.55, 168.81, 190.49, 181.11, 178.10, 223.08, 174.67, 165.81.
- B1 measured-index means ms: 185.55, 165.35, 166.96, 168.49, 175.25, 175.68, 175.86, 183.47, 168.88, 162.46.
- broad-janino-01-A.log: 168.321ms, within-fork 157.599–197.526ms; whole-JVM GC 8 pauses, total 62.8ms, max 13.3ms.
- broad-janino-02-B0.log: 170.756ms, within-fork 158.486–180.817ms; whole-JVM GC 8 pauses, total 64.8ms, max 15.4ms.
- broad-janino-03-B1.log: 180.434ms, within-fork 162.610–227.104ms; whole-JVM GC 8 pauses, total 57.6ms, max 13.7ms.
- broad-janino-04-B0.log: 183.650ms, within-fork 164.344–246.328ms; whole-JVM GC 8 pauses, total 77.9ms, max 21.1ms.
- broad-janino-05-B1.log: 168.620ms, within-fork 157.854–179.976ms; whole-JVM GC 8 pauses, total 60.1ms, max 15.9ms.
- broad-janino-06-A.log: 164.478ms, within-fork 159.096–170.168ms; whole-JVM GC 8 pauses, total 65.5ms, max 16.3ms.
- broad-janino-07-B1.log: 171.414ms, within-fork 161.345–185.034ms; whole-JVM GC 8 pauses, total 53.5ms, max 13.4ms.
- broad-janino-08-A.log: 176.840ms, within-fork 160.367–223.113ms; whole-JVM GC 8 pauses, total 59.1ms, max 15.5ms.
- broad-janino-09-B0.log: 193.213ms, within-fork 162.794–249.285ms; whole-JVM GC 8 pauses, total 49.7ms, max 10.2ms.
- broad-janino-10-B1.log: 170.712ms, within-fork 160.781–178.871ms; whole-JVM GC 8 pauses, total 67.1ms, max 17.0ms.
- broad-janino-11-B0.log: 171.285ms, within-fork 154.616–225.256ms; whole-JVM GC 8 pauses, total 65.3ms, max 17.5ms.
- broad-janino-12-A.log: 170.322ms, within-fork 157.607–179.728ms; whole-JVM GC 8 pauses, total 55.9ms, max 13.4ms.

### probes-interpreted
- A measured-index means ms: 448.97, 437.41, 431.71, 436.86, 453.00, 458.66, 449.93, 456.47, 450.46, 452.34.
- B0 measured-index means ms: 471.00, 458.45, 464.38, 466.48, 514.64, 446.63, 449.61, 472.25, 469.25, 448.78.
- B1 measured-index means ms: 496.08, 469.99, 475.61, 483.60, 518.25, 472.67, 466.28, 480.98, 474.59, 467.83.
- probes-interpreted-01-A.log: 451.848ms, within-fork 442.306–463.481ms; whole-JVM GC 9 pauses, total 62.6ms, max 10.8ms.
- probes-interpreted-02-B0.log: 463.814ms, within-fork 447.023–489.050ms; whole-JVM GC 9 pauses, total 71.1ms, max 14.6ms.
- probes-interpreted-03-B1.log: 500.002ms, within-fork 466.083–635.807ms; whole-JVM GC 9 pauses, total 56.2ms, max 11.2ms.
- probes-interpreted-04-B0.log: 474.064ms, within-fork 440.631–501.092ms; whole-JVM GC 9 pauses, total 63.1ms, max 13.1ms.
- probes-interpreted-05-B1.log: 461.231ms, within-fork 448.295–484.743ms; whole-JVM GC 9 pauses, total 66.9ms, max 18.0ms.
- probes-interpreted-06-A.log: 453.361ms, within-fork 427.757–483.890ms; whole-JVM GC 9 pauses, total 86.7ms, max 21.1ms.
- probes-interpreted-07-B1.log: 488.153ms, within-fork 467.140–525.102ms; whole-JVM GC 10 pauses, total 108.5ms, max 23.6ms.
- probes-interpreted-08-A.log: 454.341ms, within-fork 431.049–481.334ms; whole-JVM GC 9 pauses, total 67.2ms, max 16.4ms.
- probes-interpreted-09-B0.log: 457.766ms, within-fork 440.000–502.421ms; whole-JVM GC 9 pauses, total 55.9ms, max 11.4ms.
- probes-interpreted-10-B1.log: 472.967ms, within-fork 452.508–518.982ms; whole-JVM GC 9 pauses, total 61.7ms, max 12.4ms.
- probes-interpreted-11-B0.log: 468.947ms, within-fork 430.505–650.893ms; whole-JVM GC 9 pauses, total 68.8ms, max 13.4ms.
- probes-interpreted-12-A.log: 430.770ms, within-fork 417.852–447.832ms; whole-JVM GC 9 pauses, total 78.4ms, max 21.3ms.

### probes-janino
- A measured-index means ms: 497.47, 377.42, 381.36, 380.26, 381.81, 389.67, 397.12, 415.70, 399.62, 392.87.
- B0 measured-index means ms: 384.94, 396.02, 386.61, 419.54, 396.73, 422.46, 393.10, 401.85, 407.78, 390.58.
- B1 measured-index means ms: 431.75, 406.62, 408.14, 407.57, 421.51, 428.35, 422.30, 435.13, 413.11, 408.12.
- probes-janino-01-A.log: 431.215ms, within-fork 364.111–813.335ms; whole-JVM GC 9 pauses, total 89.8ms, max 26.4ms.
- probes-janino-02-B0.log: 394.211ms, within-fork 377.673–430.621ms; whole-JVM GC 9 pauses, total 87.1ms, max 22.2ms.
- probes-janino-03-B1.log: 422.021ms, within-fork 400.602–466.660ms; whole-JVM GC 9 pauses, total 70.5ms, max 14.1ms.
- probes-janino-04-B0.log: 385.884ms, within-fork 373.122–402.976ms; whole-JVM GC 9 pauses, total 72.6ms, max 11.4ms.
- probes-janino-05-B1.log: 408.304ms, within-fork 395.649–435.434ms; whole-JVM GC 9 pauses, total 79.5ms, max 21.7ms.
- probes-janino-06-A.log: 394.526ms, within-fork 376.278–428.524ms; whole-JVM GC 9 pauses, total 69.5ms, max 15.4ms.
- probes-janino-07-B1.log: 409.328ms, within-fork 400.586–422.444ms; whole-JVM GC 9 pauses, total 70.8ms, max 13.8ms.
- probes-janino-08-A.log: 393.045ms, within-fork 370.748–411.940ms; whole-JVM GC 8 pauses, total 55.5ms, max 14.0ms.
- probes-janino-09-B0.log: 402.162ms, within-fork 367.438–472.155ms; whole-JVM GC 9 pauses, total 84.7ms, max 21.4ms.
- probes-janino-10-B1.log: 433.386ms, within-fork 401.359–499.078ms; whole-JVM GC 10 pauses, total 116.7ms, max 20.5ms.
- probes-janino-11-B0.log: 417.590ms, within-fork 386.543–458.880ms; whole-JVM GC 9 pauses, total 79.8ms, max 19.1ms.
- probes-janino-12-A.log: 386.542ms, within-fork 365.868–413.076ms; whole-JVM GC 9 pauses, total 89.9ms, max 22.7ms.

## Separate profiles

15 separate JVM recordings, one per cell/arm, after 20 warmups. Regular records five queries, native/Janino 30. JFR and native-method sampling perturb execution; recorded durations are not clean timing comparisons. Categories are inclusive and overlap, stack depth/inlining can hide calls, and samples are neither wall-time percentages nor exact ThreadLocal/get/poll counts. No allocation-event profile was enabled; allocations above come from the clean harness query thread.

| Cell/arm | Main-thread samples | TLS visible | Cancellation/probe visible | Record-owner/reset visible | Read/lifetime/replay visible |
|---|---:|---:|---:|---:|---:|
| broad-interpreted-A | 731 | 125 | 35 | 0 | 78 |
| broad-interpreted-B0 | 672 | 98 | 33 | 0 | 81 |
| broad-interpreted-B1 | 737 | 100 | 39 | 0 | 90 |
| broad-janino-A | 542 | 88 | 53 | 0 | 65 |
| broad-janino-B0 | 551 | 127 | 25 | 0 | 72 |
| broad-janino-B1 | 506 | 95 | 48 | 0 | 66 |
| broad-regular-A | 730 | 41 | 9 | 0 | 75 |
| broad-regular-B0 | 777 | 47 | 2 | 0 | 78 |
| broad-regular-B1 | 716 | 24 | 7 | 0 | 60 |
| probes-interpreted-A | 1065 | 207 | 136 | 13 | 145 |
| probes-interpreted-B0 | 1077 | 197 | 169 | 40 | 137 |
| probes-interpreted-B1 | 1160 | 154 | 230 | 66 | 133 |
| probes-janino-A | 885 | 211 | 123 | 19 | 141 |
| probes-janino-B0 | 896 | 210 | 104 | 42 | 160 |
| probes-janino-B1 | 922 | 126 | 153 | 35 | 159 |

The record-owner category also includes existing LmdbRecordIterator.reset in A. Do not read its entire count as newly introduced ownership work. The comparison aggregates stacks rather than treating disappearance of a helper name as elimination of all work.

### Actual-path cancellation lookup versus explicit polling

| Profile | TLS caller checkCurrent | Inclusive NativeRecordCancellation.check | Inclusive token isCancellationRequested |
|---|---:|---:|---:|
| probes-interpreted-A | 48 | 0 | 46 |
| probes-interpreted-B0 | 57 | 0 | 62 |
| probes-interpreted-B1 | 0 | 53 | 164 |
| probes-janino-A | 71 | 0 | 16 |
| probes-janino-B0 | 53 | 0 | 8 |
| probes-janino-B1 | 0 | 22 | 118 |

The feature removes the sampled ambient cancellation lookup on the qualified leaf route while retaining explicit cancellation work. This is consistent with its design, not evidence of a throughput win. More visible explicit token/owner checking is a plausible cost to investigate; these single profile forks cannot isolate the causal contribution of open, bind, reset or fill checks, nor convert samples to saved nanoseconds.

### Remaining visible ThreadLocal callers

- broad-interpreted-B1: org.eclipse.rdf4j.http.client.QueryExecutionContext.activateExternalValueInitializer (16); org.eclipse.rdf4j.sail.lmdb.LmdbStoreConnection.executeTrackedRead (15); org.eclipse.rdf4j.sail.lmdb.LmdbStoreConnection.executeObservedRead (15); (truncated caller) (13); org.eclipse.rdf4j.sail.lmdb.LmdbSailStore.enterReadResourceContext (13); org.eclipse.rdf4j.sail.lmdb.LmdbStoreConnection.beginReadThread (6); org.eclipse.rdf4j.sail.lmdb.ValueResolutionScope.activateForeground (4); org.eclipse.rdf4j.sail.lmdb.ValueResolutionScope.activeExposure (3).
- broad-janino-B1: org.eclipse.rdf4j.sail.lmdb.LmdbStoreConnection.executeObservedRead (23); (truncated caller) (19); org.eclipse.rdf4j.sail.lmdb.RepositoryValueIteration.prepareNext (10); org.eclipse.rdf4j.sail.lmdb.LmdbSailStore.enterReadResourceContext (7); org.eclipse.rdf4j.sail.lmdb.LmdbStoreConnection.endReadThread (6); org.eclipse.rdf4j.common.iteration.CancellationSignal.current (6); org.eclipse.rdf4j.http.client.QueryExecutionContext.setExternalValueInitializer (5); org.eclipse.rdf4j.sail.lmdb.ValueResolutionScope.activeExposure (4).
- broad-regular-B1: org.eclipse.rdf4j.http.client.QueryExecutionContext.checkpointReplaySafepoint (8); org.eclipse.rdf4j.http.client.QueryExecutionContext.activateExternalValueInitializer (4); org.eclipse.rdf4j.sail.lmdb.LmdbSailStore.enterReadResourceContext (4); org.eclipse.rdf4j.sail.lmdb.LmdbStoreConnection.executeObservedRead (3); org.eclipse.rdf4j.common.iteration.CancellationSignal.current (2); org.eclipse.rdf4j.sail.lmdb.LmdbStoreConnection.executeTrackedRead (1); (truncated caller) (1); org.eclipse.rdf4j.http.client.QueryExecutionContext.lambda$activateExternalValueInitializer$0 (1).
- probes-interpreted-B1: org.eclipse.rdf4j.http.client.QueryExecutionContext.checkpointReplaySafepoint (40); org.eclipse.rdf4j.sail.lmdb.LmdbStoreConnection.executeObservedRead (31); org.eclipse.rdf4j.sail.lmdb.LmdbSailStore.enterReadResourceContext (18); org.eclipse.rdf4j.http.client.QueryExecutionContext.activateExternalValueInitializer (16); org.eclipse.rdf4j.sail.lmdb.LmdbStoreConnection.executeTrackedRead (11); (truncated caller) (9); org.eclipse.rdf4j.sail.lmdb.LmdbStoreConnection.beginReadThread (7); org.eclipse.rdf4j.sail.lmdb.evaluation.LmdbFusedSipFactorizedRuntime.attach (4).
- probes-janino-B1: org.eclipse.rdf4j.http.client.QueryExecutionContext.checkpointReplaySafepoint (40); org.eclipse.rdf4j.sail.lmdb.LmdbSailStore.enterReadResourceContext (12); org.eclipse.rdf4j.sail.lmdb.LmdbStoreConnection.executeObservedRead (11); org.eclipse.rdf4j.sail.lmdb.LmdbStoreConnection.executeTrackedRead (9); (truncated caller) (9); org.eclipse.rdf4j.sail.lmdb.LmdbStoreConnection.beginReadThread (6); org.eclipse.rdf4j.http.client.QueryExecutionContext.activateExternalValueInitializer (5); org.eclipse.rdf4j.sail.lmdb.RepositoryValueIteration.prepareNext (5).

Read context/resource scope and replay remain concrete next audit targets: executeObservedRead/executeTrackedRead, enterReadResourceContext, begin/endReadThread, external initializer activation/replay, and checkpointReplaySafepoint. Preserve nested callbacks, worker handoff, lifecycle and replay semantics; no deletion is justified by these samples alone. Binding copies, native pipeline work and value decoding also remain visible in the full summary, so optimizing TLS alone is not guaranteed to dominate end-to-end results.

## Warm-state physical hit validation

After all clean timings and profiles, two additional untimed B1 trace-enabled runs used 20 warmups then one measured-form output query. Their elapsed fields are diagnostic only and are excluded from all timing comparisons. This checks that warm execution still exercises the physical owner rather than silently moving entirely to adjacency.

- interpreted: binds 899301, polls 2697873, resets 899271; 100,000 rows, matching dual checksums, no spill.
- janino: binds 899301, polls 2697873, resets 899271; 100,000 rows, matching dual checksums, no spill.

## Next experiment hypothesis, not implementation

NEXT.md independently counts full owner checks on the direct physical route: 2 + F + L for a new probe and 3 + F + L for a retained reopen, where F is wrapper fill calls and L physical checkpoints. These are static route-specific counts, not measured call frequencies. Admission, post-bind, retained reset, every wrapper fill and physical entry/candidate/exhaustion checks can repeat similar polling. For a short first fill, this can mean four or five full checks, but filtered/exhausted/ordered cases differ.

The smallest proposed follow-up would consolidate full wrapper-fill polling only for a separately certified unbuffered physical delegate, retaining a cheap local-stop guard and physical bounded polling. Successful bind acceptance is NOT sufficient: an ordered wrapper can return a buffered primed head without advancing its physical leaf. Ordered/concatenating/adjacency/unknown/detached routes must retain their contract unless separately proved. Parent priority, lifecycle/probe deadlines, cancel during admission, terminal reset and snapshot-error ordering must remain explicit.

This is not exact semantic equivalence: moving parent/lifecycle/probe observation from every fill to bounded physical cadence changes an observation point. If immediate every-fill detection/precedence is required, do not apply this proposal. Keep all admission/post-bind/reset safeguards; no new performance measurement or production change has been made. In light of the adverse measured result, prioritize read/resource/replay scope consolidation or a precisely justified check-cadence experiment rather than enabling the current fast path.

## Evidence and reproducibility

- `matched-01/summary.json`, 60 raw fork logs with GC logs and exact commands, references/cells/flags/environment retain every observation.
- `matched-01/runtime-manifest-deduplicated.json` losslessly reconstructs the raw 3.82MB runtime manifest byte-for-byte; raw retained locally. `deduplicate-manifest.py` verifies reconstruction and original SHA.
- `candidate-class-delta.json`, `candidate-layer.sha256`, `candidate-sources.sha256`, `checkpoint-semantic.diff` and both javap files document the selected immutable runtime.
- `diagnostics-01`, `diagnostics-layered-01`, and `target100-*.log` retain route/hit qualification and unsupported scan evidence separately from timing.
- Large JFR, sample JSON, frozen classes and fixtures remain local; compact reports/logs/manifests are suitable checkpoint artifacts.
