# Compact factor-page benchmark results

## Decision and principal findings

**The compact-page candidate fails the engineering acceptance screen agreed before timing.** Its large reduction in shared-tail reserved memory is established, but the fixed latency gates do not permit promotion. Fanout round 3 had a compact/current DENSE latency ratio of **1.115602800**, above the maximum allowed 1.10. Shared-tail's median paired latency ratio was **1.012579249**, above the required 1.0. These are the two failed gates; neither the thresholds nor the sample set changed after results were observed.

The shared-tail memory improvement is substantial: peak query-ledger reservation fell from **268,426,276 to 12,901,412 bytes**, a **95.193685% reduction**, in every measured query. All-thread allocation fell by a median paired **3.155518%**, and was lower in all six matched rounds. Measured resolver calls fell from **65,345 to 32,833**. Those gains did not establish a total-latency improvement. Against ordinary reuse with coordinate transport OFF, **both current DENSE and compact DENSE were slower in all six pairs on both fixtures**.

The study completed exactly **36 fresh JVMs, 288 warmup queries and 180 measured queries: 468 queries total**. All queries passed their result checks and required execution evidence; all measured routes qualified, every endpoint query ledger was zero, and before/after runtime identity checks passed. No failed fork was replaced, no measured query was removed, and no extra fork was added. The raw records, commands and frozen candidate are preserved. [Raw analysis](raw-analysis-v1/analysis.json), [original summary](comparison-v1/summary.json), [independent review](independent-results/independent-results.json).

## What was compared

All arms use the same repaired ownership implementation and original frame handling. The prior frame-directory guard is absent. Current OFF disables coordinate transport while retaining the ordinary reuse implementation. Current DENSE enables the existing dense coordinate transport and page representation. Compact DENSE changes only `NativeResultMaterializer.java`: an initial page uses a 512-byte inline shell, with a separate non-reclaiming 15,872-byte payload reservation when it needs dense storage. The production bytecode delta is confined to three classes declared in that source file. This is a comparison of those particular frozen implementations, not the unavailable historical baseline or Stage-2 snapshots.

Every arm and both fixtures use the same uninstrumented common harness prefix. It contains 15 classes: only four shared-tail main/nested harness classes differ from the previous preserved harnesses, to await startup maintenance and record adjacency observations outside query timing/allocation windows. The other 11 copied classes, including the fixtures, exact oracle and launchers, retain their original bytes. Candidate production replaces one entry of the original 127-entry classpath; its other 126 entries are preserved. No page-occupancy or retry-probe instrumentation is in a timed runtime.

The source-matched correctness gate passed **108 tests, zero failures, errors or skips** before candidate freezing. The candidate also underwent independent source and diagnostic review; this report evaluates the timing data and does not substitute benchmark checks for those ownership tests. [Candidate provenance](runtimes/compact-page/source-provenance.json), [correctness evidence](validation/compact-correctness/results.json), [common harness provenance](prepared-harness-provenance.json), [exact production patch](runtimes/compact-page/production-delta.patch).

## Protocol, inputs and estimator

The fixed protocol uses all six arm permutations for each fixture, eight warmups and all five measured queries per new JVM. Its orders are OFF–current DENSE–compact DENSE; OFF–compact DENSE–current DENSE; current DENSE–OFF–compact DENSE; current DENSE–compact DENSE–OFF; compact DENSE–OFF–current DENSE; compact DENSE–current DENSE–OFF. Each round keeps a fixture's three arms together; fanout starts odd human-numbered rounds and shared-tail starts even rounds. Recorded JVM process intervals do not overlap. Other builds, tests, profiles and compression were paused during timing.

The JVM is Eclipse Adoptium Java **25.0.5+8-LTS**, with `-Xms1024m -Xmx1024m -XX:+UseG1GC`, existing hot counters enabled in all arms, and the transport property set to `off` or `dense`. Native evaluation and direct-adjacency `PREFER` are enabled. The global query-memory authority is **1 GiB**, and the per-query limit is **256 MiB**. Store population and readiness checks occur before the query samples. Normal AUTO selection remains in effect; no route or IR tier is forced.

| Fixture | Shape | Rows per query | Projected values read per query | Expected checksum |
| --- | --- | --- | --- | --- |
| Fanout | 100 measurements, 10 readings each, width 12, two statuses | 1,152,000 | 13,824,000 | `3123987218479850048` |
| Shared-tail | 64 roots, 128 left and 128 right entries, two columns per tail, width 6 | 1,048,576 | 6,291,456 | `8327963502356915200` |

Fanout checks the independent full-bag count and lexical checksum. Shared-tail additionally checks IRI types, tuple and parent correlation, legal unique complete-row keys, count and checksum with its independent exact bag oracle. The shared-tail oracle's 131,072-byte bitset and other setup scratch are allocated before timing; per-row result consumption and validation remain inside timing. The two fixtures retain their respective store settings and are never pooled.

For every metric, first take the median of **all five** measurements in each JVM. A paired ratio divides the candidate JVM median by the comparator JVM median from the **same fixture and round**. The primary reported effect is the median of all six ratios, with their full range. Absolute arm summaries separately take the median of that arm's six JVM medians. These operations need not produce the same ratio or even the same direction when near one; neither replaces the other after seeing data. Ranges below are observed ranges, not confidence intervals. [Frozen protocol](protocol.json), [protocol validation](protocol-validation.json), [raw recalculation method](analyze-results.py).

## All six paired latency observations

Times in the following tables are each JVM's median of its five measured queries, in seconds. Round numbers are 1–6; directory and JSON round indices are 0–5. Ratios below one favor the numerator. All pairs are retained. [Raw numerical tables](raw-analysis-v1/numeric-tables.md).

### Fanout

| Round | OFF s | Current DENSE s | Compact DENSE s | Compact/current | Compact/OFF | Current/OFF |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | 3.001039844 | 3.815272483 | 4.036823498 | 1.058069513 | 1.345141587 | 1.271316837 |
| 2 | 2.928721774 | 3.953265699 | 3.715421224 | 0.939835950 | 1.268615290 | 1.349826308 |
| 3 | 2.913734911 | 3.777154903 | 4.213804584 | 1.115602800 | 1.446186669 | 1.296327572 |
| 4 | 2.949056482 | 3.824831055 | 3.933546295 | 1.028423540 | 1.333832132 | 1.296967718 |
| 5 | 3.088102069 | 4.069235161 | 3.936213134 | 0.967310312 | 1.274638288 | 1.317713945 |
| 6 | 2.978456532 | 4.349503925 | 3.793880346 | 0.872255874 | 1.273773951 | 1.460321438 |

### Shared-tail

| Round | OFF s | Current DENSE s | Compact DENSE s | Compact/current | Compact/OFF | Current/OFF |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | 2.114748470 | 2.333507910 | 2.335390188 | 1.000806630 | 1.104334733 | 1.103444662 |
| 2 | 2.127417551 | 2.614366832 | 2.433886499 | 0.930965949 | 1.144056792 | 1.228892199 |
| 3 | 2.321496638 | 2.453715111 | 2.588129514 | 1.054779955 | 1.114853871 | 1.056953980 |
| 4 | 2.057968300 | 2.387265397 | 2.385766826 | 0.999372265 | 1.159282592 | 1.160010772 |
| 5 | 2.048549913 | 2.362726689 | 2.443656575 | 1.034252750 | 1.192871386 | 1.153365448 |
| 6 | 2.086995632 | 2.299198456 | 2.355188234 | 1.024351868 | 1.128506547 | 1.101678614 |

### Summary of paired effects

| Fixture | Ratio | Latency median [range] | Latency pairs below 1 / 6 | All-thread allocation median [range] |
| --- | --- | --- | --- | --- |
| fanout | Compact DENSE/Current DENSE | 0.997866926 [0.872255874–1.115602800] | 3 | 1.000009614 [0.979485277–1.000071477] |
| fanout | Compact DENSE/Current OFF | 1.304235210 [1.268615290–1.446186669] | 0 | 0.837278383 [0.809003326–0.837324576] |
| fanout | Current DENSE/Current OFF | 1.307340832 [1.271316837–1.460321438] | 0 | 0.837253882 [0.825947408–0.837326011] |
| shared-tail | Compact DENSE/Current DENSE | 1.012579249 [0.930965949–1.054779955] | 2 | 0.968444821 [0.968418209–0.968474003] |
| shared-tail | Compact DENSE/Current OFF | 1.136281670 [1.104334733–1.192871386] | 0 | 0.852137360 [0.852117365–0.852162960] |
| shared-tail | Current DENSE/Current OFF | 1.128405055 [1.056953980–1.228892199] | 0 | 0.879904343 [0.879902773–0.879921508] |

Compact versus current DENSE favors compact in three fanout rounds and two shared-tail rounds. Neither fixture supports the prospectively defined consistent-win claim, which required all six ratios below one. Compact versus OFF has median paired latency penalties of **30.423521% on fanout** and **13.628167% on shared-tail**. Current DENSE versus OFF has corresponding penalties of **30.734083%** and **12.840506%**, with every pair slower in all four comparisons.

### The fixed acceptance screen

| Prespecified gate | Observed compact/current DENSE result | Result |
| --- | --- | --- |
| Every shared-tail ledger ratio <=0.25 | 0.048063149 in all six pairs | Pass |
| Every shared-tail allocation ratio <1.0 | 0.968418209–0.968474003 | Pass |
| Fanout median paired latency <=1.0 | 0.997866926 | Pass |
| Every fanout latency ratio <=1.10 | Maximum 1.115602800, round 3 | **Fail** |
| Shared-tail median paired latency <=1.0 | 1.012579249 | **Fail** |
| Every shared-tail latency ratio <=1.10 | Maximum 1.054779955 | Pass |

All gates were required, so the candidate is rejected by this screen. The result is an engineering acceptance decision for a memory optimization, not a statistical proof of inferiority or noninferiority. In particular, the 1.26% shared-tail median increase does not by itself prove a small causal slowdown in the population; it fails the specific zero-increase gate that was chosen beforehand. No tolerance, extra round or favorable subset has been introduced. [Frozen acceptance criteria](protocol.json), [recomputed gates](raw-analysis-v1/analysis.json).

## Absolute warmed latency, allocation and reservation

Each entry below is the median of six JVM medians, followed by their minimum and maximum. GB means decimal bytes / 1,000,000,000; ledger bytes are exact reservation counts. These summaries give absolute scale and do not replace the paired estimator.

| Fixture | Arm | Total s [range] | All-thread GB [range] | Caller GB [range] | Peak ledger bytes |
| --- | --- | --- | --- | --- | --- |
| fanout | Current OFF | 2.963757 [2.913735–3.088102] | 1.630360 [1.630340–1.630419] | 1.630301 [1.630262–1.630340] | 2,432 |
| fanout | Current DENSE | 3.889048 [3.777155–4.349504] | 1.365037 [1.346604–1.365128] | 1.364976 [1.346523–1.365055] | 2,264,968 |
| fanout | Compact DENSE | 3.934880 [3.715421–4.213805] | 1.365071 [1.318978–1.365129] | 1.365003 [1.318919–1.365057] | 2,233,224 |
| shared-tail | Current OFF | 2.100872 [2.048550–2.321497] | 1.320700 [1.320696–1.320733] | 1.320648 [1.320643–1.320680] | 1,472 |
| shared-tail | Current DENSE | 2.374996 [2.299198–2.614367] | 1.162088 [1.162088–1.162121] | 1.162035 [1.162035–1.162068] | 268,426,276 |
| shared-tail | Compact DENSE | 2.409827 [2.335390–2.588130] | 1.125419 [1.125417–1.125452] | 1.125367 [1.125367–1.125399] | 12,901,412 |

For fanout, the ratio of the two absolute DENSE latency summaries is about 1.0118, whereas the prespecified median of paired ratios is 0.9979. This is expected from different aggregation operations: dividing two separately ranked medians loses the within-round pairing. Both quantities are reported, and the acceptance screen uses the paired one throughout.

Shared-tail saves **255,524,864 bytes of peak reservation**: current DENSE reaches **255.991245 MiB**, while compact reaches **12.303745 MiB**. The previous representation sits 9,180 bytes below the 256 MiB query limit. Compact's 4.806315% ledger ratio is comfortably below the 25% gate in all six pairs. Fanout's ledger improvement is much smaller, **31,744 bytes**, from 2,264,968 to 2,233,224 bytes, a ratio of 0.985984791.

Allocation and peak reservation measure different properties. The ledger reduction does not imply the same reduction in Java allocations or resident heap: the measured shared-tail all-thread allocation improvement is about **36.7 MB per query**, roughly **3.16%**, while total allocation remains about 1.125 GB. Across the 30 measurements in each arm, median all-thread allocation beyond the caller is only about 52–66 KB; all-thread measurement is still retained to include workers and asynchronous preparation. No heap-residency, RSS or total native-memory estimate is inferred from the query ledger. [Raw arm statistics](raw-analysis-v1/analysis.json).

### Allocation by matched round

These are all-thread bytes, each the median of all five measured queries. All six allocation ratios are shown independently of the latency results.

#### Fanout

| Round | OFF bytes | Current DENSE bytes | Compact DENSE bytes | Compact/current |
| --- | --- | --- | --- | --- |
| 1 | 1,630,419,408 | 1,365,037,184 | 1,365,050,264 | 1.000009582 |
| 2 | 1,630,374,528 | 1,346,603,616 | 1,318,978,416 | 0.979485277 |
| 3 | 1,630,394,312 | 1,365,036,280 | 1,365,053,904 | 1.000012911 |
| 4 | 1,630,340,280 | 1,365,075,512 | 1,365,088,680 | 1.000009646 |
| 5 | 1,630,342,616 | 1,365,128,280 | 1,365,104,584 | 0.999982642 |
| 6 | 1,630,346,224 | 1,365,031,392 | 1,365,128,960 | 1.000071477 |

#### Shared-tail

| Round | OFF bytes | Current DENSE bytes | Compact DENSE bytes | Compact/current |
| --- | --- | --- | --- | --- |
| 1 | 1,320,732,864 | 1,162,120,664 | 1,125,419,408 | 0.968418722 |
| 2 | 1,320,709,592 | 1,162,120,776 | 1,125,418,920 | 0.968418209 |
| 3 | 1,320,700,272 | 1,162,087,936 | 1,125,417,024 | 0.968443944 |
| 4 | 1,320,700,200 | 1,162,087,768 | 1,125,451,792 | 0.968474003 |
| 5 | 1,320,695,760 | 1,162,087,904 | 1,125,419,032 | 0.968445699 |
| 6 | 1,320,700,104 | 1,162,087,768 | 1,125,418,920 | 0.968445716 |

Fanout round 2 contains lower-allocation forks in **both** DENSE arms: the current DENSE median is 1,346,603,616 bytes and compact is 1,318,978,416 bytes, while the other DENSE medians are near 1,365 million bytes. This is not a single removed sample. All five current-DENSE measurements lie between 1,346,567,832 and 1,346,645,400 bytes; all five compact measurements lie between 1,318,971,688 and 1,319,009,880 bytes. Their results, routes, resolver counts and ledger peaks satisfy the same gates as the remaining forks. The cause is unexplained by these observations. Both forks remain in every summary and pair; no JIT, escape-analysis or GC cause is asserted without evidence.

## Warmed preparation and execution phases

The following values are **milliseconds**, median [range] of the six per-JVM medians. `prepare` measures `prepareTupleQuery`; `evaluate` measures the subsequent `query.evaluate()` call. Their separation matters because work can occur in either stage. The first-row metric begins when `evaluate()` returns and ends when the first row is obtained. It is **contained within consumption**, so it must not be added to consumption a second time. Consumption includes the full row traversal and result oracle; close includes result and connection closure. Connection setup and a negligible final empty retained-read loop are also included in total latency.

Phase medians are computed separately and therefore are not expected to sum to the median total. The raw timestamps do reconcile exactly for every query: connection + prepare + evaluate + consume + close + retained-read equals total. Adjacency snapshots lie outside the timing/allocation windows. [Harness timing source](harness-src/org/eclipse/rdf4j/sail/lmdb/benchmark/SharedSelectionExperiment.java), [all raw metrics](raw-analysis-v1/analysis.json).

### Fanout

| Phase ms [range] | Current OFF | Current DENSE | Compact DENSE |
| --- | --- | --- | --- |
| Prepare | 2.198 [1.949–3.289] | 2.066 [1.969–2.978] | 2.229 [2.059–2.972] |
| Evaluate | 19.299 [18.373–21.761] | 18.127 [16.547–25.851] | 22.353 [19.672–25.149] |
| First row after evaluate | 6.221 [5.536–7.099] | 5.678 [5.161–8.383] | 7.793 [6.176–8.829] |
| Consume | 2,945.589 [2,886.823–3,065.016] | 3,864.604 [3,754.262–4,316.812] | 3,912.803 [3,672.399–4,192.435] |
| Close | 0.130 [0.119–0.151] | 0.119 [0.097–0.253] | 0.140 [0.103–0.152] |
| Connection | 0.091 [0.089–0.127] | 0.093 [0.084–0.111] | 0.098 [0.089–0.157] |

### Shared-tail

| Phase ms [range] | Current OFF | Current DENSE | Compact DENSE |
| --- | --- | --- | --- |
| Prepare | 1.316 [1.214–1.409] | 1.152 [1.038–1.328] | 1.399 [1.137–1.570] |
| Evaluate | 6.947 [5.857–7.499] | 6.628 [5.635–7.728] | 6.887 [5.750–7.433] |
| First row after evaluate | 4.297 [4.023–4.615] | 4.423 [3.513–5.022] | 4.952 [4.013–5.548] |
| Consume | 2,093.170 [2,041.344–2,313.554] | 2,363.893 [2,290.398–2,607.656] | 2,401.635 [2,327.429–2,566.171] |
| Close | 0.140 [0.114–0.230] | 0.186 [0.131–0.252] | 0.159 [0.135–0.170] |
| Connection | 0.095 [0.092–0.103] | 0.096 [0.092–0.116] | 0.097 [0.096–0.103] |

The main latency scale is full consumption, measured in seconds; prepare/evaluate/first-row differences are in milliseconds on warmed queries. For compact/current DENSE, the median paired prepare ratio is **1.078905** on fanout and **1.171933** on shared-tail. The corresponding evaluate ratios are **1.155326** and **0.979600**, and consume ratios are **0.997832** and **1.012627**. These phase observations describe where the time was recorded; they do not independently attribute causal overhead to a particular implementation method.

## First query in each fresh JVM

These values summarize the six **first warmup queries** per fixture and arm. They are retained cold-start observations and are excluded from warmed comparisons by the fixed eight-warmup protocol. “First query” means after JVM startup, store population and readiness; it is not a cold-disk or empty-cache experiment. Every first query selected the native **interpreted IR** normally; subsequent warmups and all measured queries selected compiled IR. No tier was forced to make the comparison qualify.

### Fanout

| First-query median [range] | Current OFF | Current DENSE | Compact DENSE |
| --- | --- | --- | --- |
| Total s | 5.412 [4.867–5.931] | 8.697 [8.434–9.961] | 8.385 [8.036–8.700] |
| Prepare ms | 76.920 [66.758–91.703] | 74.630 [69.427–102.714] | 72.349 [68.083–82.687] |
| Evaluate ms | 412.621 [383.680–491.767] | 438.131 [382.555–635.728] | 412.308 [367.625–423.299] |
| First row after evaluate ms | 170.191 [160.891–328.448] | 209.279 [165.594–453.491] | 182.314 [154.256–212.501] |
| Consume s | 4.860 [4.407–5.470] | 8.101 [7.974–9.407] | 7.891 [7.551–8.262] |
| Close ms | 0.157 [0.132–0.335] | 0.142 [0.113–0.171] | 0.133 [0.118–0.409] |
| All-thread GB | 1.692 [1.692–1.693] | 1.427 [1.408–1.427] | 1.427 [1.381–1.427] |

### Shared-tail

| First-query median [range] | Current OFF | Current DENSE | Compact DENSE |
| --- | --- | --- | --- |
| Total s | 3.772 [3.572–4.191] | 4.460 [4.087–4.895] | 4.622 [4.211–5.407] |
| Prepare ms | 72.497 [66.576–74.792] | 65.467 [63.225–69.610] | 68.444 [60.507–86.532] |
| Evaluate ms | 348.949 [295.734–472.203] | 328.475 [286.274–472.093] | 339.757 [317.795–517.862] |
| First row after evaluate ms | 145.160 [134.085–266.793] | 150.485 [136.161–316.727] | 146.239 [131.472–291.683] |
| Consume s | 3.240 [3.210–3.775] | 4.069 [3.697–4.358] | 4.186 [3.682–5.003] |
| Close ms | 0.161 [0.121–0.501] | 0.153 [0.130–0.223] | 0.151 [0.135–0.203] |
| All-thread GB | 1.412 [1.412–1.414] | 1.253 [1.253–1.255] | 1.217 [1.216–1.219] |

All eight warmup positions, their per-fork raw metrics, and their per-position summaries remain in the analysis artifact. The six first-query observations are descriptive startup evidence; they are not substituted for the warmed paired estimator and were not used to change the fixed warmup count. [Warmup and cold-query records](raw-analysis-v1/analysis.json).

## Resolver, route, adjacency and lifetime evidence

The value-call counter is `lazyValueCalls`. In the following table each count is **median [minimum–maximum] over all 30 measured queries** in the fixture/arm, rather than a selected favorable query. The ranges are degenerate: every measured query has exactly the displayed count.

| Fixture | Arm | Resolver calls [range] | Native IR rows [range] | Coordinate admissions [range] |
| --- | --- | --- | --- | --- |
| fanout | Current OFF | 2,770,101 [2,770,101–2,770,101] | 1,152,000 [1,152,000–1,152,000] | 0 [0–0] |
| fanout | Current DENSE | 1,147 [1,147–1,147] | 1,152,000 [1,152,000–1,152,000] | 1 [1–1] |
| fanout | Compact DENSE | 1,147 [1,147–1,147] | 1,152,000 [1,152,000–1,152,000] | 1 [1–1] |
| shared-tail | Current OFF | 2,113,601 [2,113,601–2,113,601] | 1,048,576 [1,048,576–1,048,576] | 0 [0–0] |
| shared-tail | Current DENSE | 65,345 [65,345–65,345] | 1,048,576 [1,048,576–1,048,576] | 1 [1–1] |
| shared-tail | Compact DENSE | 32,833 [32,833–32,833] | 1,048,576 [1,048,576–1,048,576] | 1 [1–1] |

Compact does not change fanout resolver calls relative to current DENSE. Shared-tail compact performs 32,512 fewer calls than current DENSE, a **49.754381% reduction**. Both DENSE arms still materially reduce calls relative to OFF. Lower calls and allocation therefore remain observed benefits, but they are insufficient to establish a total-latency benefit on these fixtures.

Across **every measured query** on all arms, `irPlanned`, `irOpened`, `compiledBinds` and `semanticNativeCompiles` each have median/range **1 [1–1]**. `interpretedBinds`, both aggregate-bind counters, `chunkEngaged`, `leftJoinReplayMaterializations` and `factorizedEngagement` each have **0 [0–0]**. Initial and growth coordinate-admission refusals and all row-selection owner/window/row/column/group/refusal counters are **0 [0–0]**. These are DENSE transport runs, not qualified shared ROWS transport runs. Per-fork counter arrays and the complete flattened counter table are preserved in the raw analysis.

All **180 measured queries** record one successful `irKernel` selection at `ROW` / `row/join dispatch`, mode `normal`, with one native compiled bind and no interpreted bind. The same route/tier histogram holds across all 18 forks within each fixture. Of the 288 warmups, 36 first queries use `irKernelInterpreted` and the remaining 252 use `irKernel`. Every query has one native semantic compile; each post-main root audit reconciles **13 total native compilations**, zero hosted-generic roots and zero unhosted hard declines.

Adjacency is observed through actual query-time counters, not inferred from the configuration flag. Every fanout query records **55 successful lookup hits**; every shared-tail query records **10**. Both have one planner-statistics hit, zero exact misses and no fallback entries. Setup and before/after observations are ACTIVE, and applied/current revisions remain equal to **1** throughout. These counts may include preparation and probe work; they do not establish that every result row was read through adjacency.

Every query ledger starts and ends at zero, with the peak values reported above. DENSE opens one coordinate owner per query; OFF opens none. The timing harness does **not** expose an explicit owner-close counter, so these timing records prove coordinate admission and zero endpoint reservation, not a counted owner-close event. Exact release accounting and retry behavior belong to the separate diagnostics and correctness tests. Also, zero coordinate-admission refusal counters must not be read as zero materializer factor-page refusal: those are different counters and the original shared-tail page budget can fill without refusing the coordinate owner. [Complete counter statistics](raw-analysis-v1/analysis.json), [independent route/ownership audit](independent-results/independent-results.json).

## Scope and evidence preservation

This study measures full consumption of two fixed fixtures. It does not complete a repetition/projection/unique-value sweep, early-close or retained-row sweep, or a new range of query-memory budgets. It does not compare changed-register or ROWS representations, and it does not reconstruct the unavailable historical Stage-2 performance result. The original and compact runtimes, graph/query identities, fixed JVM settings and current results are kept distinct from all earlier timing phases.

The observed memory improvement supports compact-page representation as a useful engineering direction. Under this experiment's decision rule, however, the frozen candidate must not be promoted. A different future acceptance tradeoff would need a new prospective decision and experiment; it cannot retroactively alter this failed screen. No additional performance forks were run after this result. Production restoration and the final delivery state are recorded by the root's experiment report.

The complete numerical evidence is available in the following files:

- [Frozen protocol](protocol.json): exact arm/order/input/JVM/acceptance identities.
- [Raw study](comparison-v1/runs.json) and [summary](comparison-v1/summary.json): all 36 forks, all raw queries, root audits and driver estimates; individual directories contain commands, JSONL, console output and process status.
- [Post-run preservation](comparison-v1/post-run-preservation.json): both runtimes and bound identity files unchanged.
- [Raw recalculation](raw-analysis-v1/analysis.json) and [complete numeric tables](raw-analysis-v1/numeric-tables.md): every five-query median, six-pair ratio, cold/warmup observation and counter range, with raw input hashes.
- [Independent recalculation](independent-results/independent-results.json): separate audit of all gates and ratios.
- [Frozen candidate descriptor](runtimes/compact-page/runtime-descriptor.json), [complete modified source](runtimes/compact-page/modified-sources/core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/NativeResultMaterializer.java) and [exact diff](runtimes/compact-page/production-delta.patch): rejected candidate preserved for review.

The protocol SHA-256 is `8ef50b935b83cd22c525616438949f730042ed09c179eff332e39c5601f9b374`; the independently reviewed production patch is `665dca2f8fc662b510fa8d7f285f417a57546e8aec71418f169340c1b8153aa1`; the frozen candidate production tree is `efbb57205da275bd4032ee0f127eb2028010e6707dde8b4538807f94c5542595`; and the shared harness prefix is `05c88f61336fce5028bb617e5ff7f7759e4a55ae4c4dba3a3b487c5512f0d825`.
