# Real-query allocation measurements and JFR attribution limits

## Keep the absolute measurement visible

The primary two-fork comparison in [final-query.json](final-query.json) reports **7,049.14 → 7,451.20 B/row: +402.06 B/row, or +5.70%**, for published commit `741b938` versus the current writer. This is the JMH GC profiler's whole-query measurement, including native-query planning/materialization and serialization. The JFR attribution problem below does **not** invalidate, adjust, or explain away this result.

| Primary result | Published | Current |
| --- | ---: | ---: |
| Allocation, B/row | 7,049.14 | 7,451.20 |
| Reported allocation interval, B/row | 6,947.87–7,150.41 | 7,426.74–7,475.65 |
| Allocation fork means, B/row | 7,112.25; 6,986.03 | 7,437.25; 7,465.14 |
| Time, µs/row | 8.912 | 7.713 |
| Reported time interval, µs/row | 7.508–10.316 | 6.220–9.206 |

Both current allocation fork means exceed both published means in this run. The time intervals overlap; the lower current time mean does not establish a precise whole-query speedup. This primary comparison used two forks, five 1-second warmups and five 1-second measurements per fork.

The separately preserved [query-allocation-recheck.json](query-allocation-recheck.json) reverses the requested implementation order and uses two forks, ten 1-second warmups and five 2-second measurements per fork. Its allocation direction **reverses**: 7,546.20 → 7,132.35 B/row, or −5.48%.

| Separate recheck result | Published | Current |
| --- | ---: | ---: |
| Allocation, B/row | 7,546.20 | 7,132.35 |
| Reported allocation interval, B/row | 7,394.14–7,698.25 | 6,818.45–7,446.25 |
| Allocation fork means, B/row | 7,641.59; 7,450.80 | 7,329.31; 6,935.39 |
| Time, µs/row | 8.412 | 8.215 |
| Reported time interval, µs/row | 7.575–9.249 | 6.740–9.689 |

Report both runs separately; pooling these different protocols would conceal the change in direction and substantial fork variation. Together they do not establish a robust whole-query allocation or time change. The recheck does not cancel the primary +5.70% observation.

## One first event dominates each raw allocation profile

The query `.events.json` exports were streamed using `summarize_jfr.iter_jfr_events`; all selected allocation events belong to the benchmark worker. The original reports and recordings remain unchanged.

| Raw query allocation profile | Published | Current |
| --- | ---: | ---: |
| Selected allocation samples | 3,051 | 3,039 |
| Selected allocation weight, bytes | 10,034,563,120 | 11,850,040,232 |
| First selected sample weight, bytes | 2,610,946,984 | 3,162,419,016 |
| First sample's share of selected weight | 26.0195% | 26.6870% |
| First sample's allocated class | `java.util.ArrayList` | `java.lang.Object[]` |
| First sample's top frame | `LmdbNativePackedFtree$Planner.lambda$plan$0` | `ImmutableCollections$SetN.<init>` |

Each headline 26–27% allocation site consists of **that single first sample**. These are not observations of multi-gigabyte objects or thousands of recurring allocations at those sites. The current first sample's stack passes through `Set.of`, `Set.copyOf`, and `QueryEvaluationUtility$QuerySafetySnapshot.captureNameInputs`; the recorded location identifies the triggering allocation, not the provenance of every byte represented by its weight.

## Exact JDK source explains the boundary artifact

The benchmark uses OpenJDK `jdk-25.0.5+8`, whose annotated tag resolves to commit `fda231468e56663992785ec199a6efb8eca3cc7a`. Its implementation establishes the following chain:

1. [`Thread` stores a cumulative heap-allocation counter](https://github.com/openjdk/jdk25u/blob/fda231468e56663992785ec199a6efb8eca3cc7a/src/hotspot/share/runtime/thread.hpp#L258-L260), initialized to zero [when the thread is constructed](https://github.com/openjdk/jdk25u/blob/fda231468e56663992785ec199a6efb8eca3cc7a/src/hotspot/share/runtime/thread.cpp#L91-L96).
2. [`JfrRecorderService::start()` calls `clear()`](https://github.com/openjdk/jdk25u/blob/fda231468e56663992785ec199a6efb8eca3cc7a/src/hotspot/share/jfr/recorder/service/jfrRecorderService.cpp#L454-L479). Its [allocation-sampling reset visits the threads](https://github.com/openjdk/jdk25u/blob/fda231468e56663992785ec199a6efb8eca3cc7a/src/hotspot/share/jfr/recorder/service/jfrRecorderService.cpp#L126-L148) and clears each JFR last-sample counter [to zero](https://github.com/openjdk/jdk25u/blob/fda231468e56663992785ec199a6efb8eca3cc7a/src/hotspot/share/jfr/support/jfrThreadLocal.hpp#L244-L254); it does not reset the cumulative thread allocation counter.
3. [`send_allocation_sample` subtracts that last-sample counter from the cumulative counter](https://github.com/openjdk/jdk25u/blob/fda231468e56663992785ec199a6efb8eca3cc7a/src/hotspot/share/jfr/support/jfrObjectAllocationSample.cpp#L32-L55), attaches the resulting weight to the current class/stack, and updates the last-sample counter only when the event commits.

The local [InProcessJfrProfiler](harness/src/org/eclipse/rdf4j/query/resultio/binary/benchmark/InProcessJfrProfiler.java) starts recording at the first measurement iteration, after warmup. Consequently, the first committed allocation sample can carry the existing worker's pre-recording cumulative allocation weight. The source and these first-event magnitudes support this boundary explanation; they do not identify which warmup allocation sites produced those bytes.

## Separate sensitivity summaries, with explicit limitations

[steady_profiles.py](harness/steady_profiles.py) reuses the existing streaming reader and summarizer. It omits the first allocation sample for every recorded thread ID **before** the usual thread/stack selection, records every omitted weight/timestamp/class/site and full recorded stack, and writes separate `*.steady-summary.json` / `*.steady-summary.md` files. All CPU events pass through unchanged. Run it after timing has completed:

```sh
python3 binary-writer-round2/harness/steady_profiles.py binary-writer-round2/profiles/*.events.json
```

This yields an **approximate corrected distribution / sensitivity analysis**, not exact accounting: omitting the first sample also discards an unknown amount of valid post-start allocation. Remaining events still represent sampled, throttled intervals, and their weights can vary substantially. Raw weighted totals from separate profiled forks are not normalized by rows or iterations; comparing their totals does not measure B/row. The same boundary caveat applies to allocation percentages from this profiler in the previous round. It changes neither its JMH results nor its CPU sample counts.

All six round-two sensitivity reports were generated after timing finished. Their complete CPU report objects match the raw reports exactly; retained plus omitted allocation counts and weights reconstruct the raw totals exactly. Each recording has one selected benchmark-worker sample omitted, plus first samples from other recorded threads that were outside the usual selection.

| Profile | Published selected weight omitted | Current selected weight omitted |
| --- | ---: | ---: |
| ASCII | 31.02% | 31.41% |
| Long Unicode | 36.43% | 73.33% |
| Query | 26.02% | 26.69% |

The long-Unicode current profile particularly illustrates the danger: its first sample assigns 285,721,736 weighted bytes to `writeLargeString`; no allocation samples with that top frame remain after omission. This does not prove zero allocation in the method. The published long-Unicode retained distribution remains 99.93% at `HeapByteBuffer.<init>`, consistent with the large encoding-buffer path. For the query, retained `handleSolutionImpl` inclusive allocation shares are 4.75% published and 5.35% current; these relative sampled shares do not quantify the whole-query B/row difference.

The raw query CPU writer-inclusive share fell from 14.29% to 6.97%, while `RepositoryValueIteration.prepareNext` rose from 77.07% to 84.40%. These are relative shares of different sample populations, and inclusive call stacks overlap. They support the observation that the writer occupies less of the recorded CPU profile; they do not establish an absolute whole-query improvement. Likewise, allocation under a writer stack can include lazy value access, so an inclusive writer percentage is not exclusive allocation by writer code.

**Conclusion:** the first-sample artifact prevents attributing the primary measured +5.70% whole-query allocation increase to the two apparent 26–27% sites. The longer recheck reverses allocation direction, so a stable whole-query allocation effect is unresolved. The evidence does not establish a causal contribution from writer code, query planning/materialization, runtime optimization, or another fork-dependent effect. Keep both protocols and their fork variation visible, and use the separate sensitivity reports only to assess the remaining sampled distribution.
