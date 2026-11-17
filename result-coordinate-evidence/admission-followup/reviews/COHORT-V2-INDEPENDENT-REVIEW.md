# Independent review of the complete admission comparison

The independent auditor reconstructed the complete comparison-v2 cohort from sealed raw records: **54 fresh JVMs, 702 queries, 432 warmups and 270 measured queries**. All source/runtime bindings, result oracles, route matching, owner checks and cleanup gates passed. Its independently calculated values agree with the primary analysis within a relative/absolute tolerance of `1e-12`. No query or process was excluded.

The audit is [cohort-v2-independent-audit.json](cohort-v2-independent-audit.json). The selected effect sizes, all-query ownership totals, exact five-value allocation sequences for every JVM and interpretation notes are preserved in [cohort-v2-numerical-review.json](cohort-v2-numerical-review.json). The independently audited primary report is [comparison-v2/audit.json](../comparison-v2/audit.json), SHA-256 `26d98249a53427021a89176a4f2b8f06feaf4266ddc778f3c321f543f2877949`.

## Paired effects and their limits

Each value below is the median of **six within-round candidate/comparator JVM ratios**. The primary estimator uses each JVM's median of its five measurements; the sensitivity estimator uses its arithmetic mean of the same five measurements. Ratios below one favor the candidate. Five queries within one JVM are nested measurements, not five independent replications. These paired ratios need not equal ratios of separately reported arm medians.

| Fixture | Candidate / comparator | Primary latency ratio | Mean sensitivity latency ratio | Primary allocation ratio | Faster JVM pairs |
|---|---|---:|---:|---:|---:|
| early-cold-hot | always / initial | 1.0542 | 1.0305 | 0.9662 | 2/6 |
| early-cold-hot | initial / ordinary | 0.9172 | 0.9050 | 0.9708 | 4/6 |
| early-cold-hot | always / ordinary | 0.9414 | 0.9173 | 0.9563 | 4/6 |
| late-cold-hot | always / initial | 1.1353 | 1.0541 | 0.9753 | 2/6 |
| late-cold-hot | initial / ordinary | 1.0772 | 0.9344 | 0.9889 | 3/6 |
| late-cold-hot | always / ordinary | 1.2220 | 1.0528 | 0.9743 | 1/6 |
| hot-unique | always / initial | 1.0004 | 1.0614 | 1.0000 | 3/6 |
| hot-unique | initial / ordinary | 0.9356 | 0.9141 | 1.0022 | 5/6 |
| hot-unique | always / ordinary | 0.9188 | 0.9348 | 1.0022 | 4/6 |

No latency comparison wins in all six process pairs. The central late-repetition comparison, always versus initial, recovers reuse but has a primary latency ratio of **1.1353**, with a range of **0.9057–1.1737** and four of six pairs slower. The mean sensitivity ratio is **1.0541**. The allocation ratio is **0.97530**. This does not demonstrate an end-to-end latency win.

Late always versus ordinary has a larger primary latency ratio of **1.2220**, but only **1.0528** under the mean sensitivity estimator. Late initial versus ordinary reverses direction: **1.0772** primary and **0.9344** sensitivity. The latter must be described as estimator-sensitive, rather than as an established speedup or slowdown.

The early-repetition allocation reduction for always versus ordinary occurs in all six pairs (ratio **0.95633**, range **0.92534–0.98825**). Its latency direction still varies across processes. In the hot-to-unique case the memo is unproductive after its initial hot prefix, but measured latency summaries are often lower than ordinary. It would therefore be incorrect to report that case as a measured CPU regression.

## Exact mechanism and lifecycle observations

Every fixture/arm combination had 78 queries, including warmups. Within each enabled combination, all 78 queries shared one exact memo event signature. Source calls matched projected materializations throughout.

| Fixture / memo mode | Probes per query | Hits | Source calls | Resizes | Peak charged memo reservation |
|---|---:|---:|---:|---:|---:|
| Early cold→hot, initial | 131,072 | 126,944 | 4,128 | 0 | 606,448 bytes |
| Early cold→hot, always | 131,072 | 126,944 | 4,128 | 0 | 606,448 bytes |
| Late cold→hot, initial | 8,192 | 0 | 131,072 | 0 | 606,448 bytes |
| Late cold→hot, always | 131,072 | 98,240 | 32,832 | 4 | 14,549,472 bytes |
| Hot→unique, initial | 131,072 | 8,128 | 122,944 | 2 | 3,637,728 bytes |
| Hot→unique, always | 131,072 | 8,128 | 122,944 | 2 | 3,637,728 bytes |

Ordinary uses 131,072 source calls in every query and opens no memo owner. Across all 702 queries there are **468 enabled owners, all closed**, **78 low-hit cutoffs**, all in late initial, and **624 resizes**. Collected misses, invalidations, unsupported values, reservation refusals, rehash discards and post-close array charges are all zero. All recorded ledger endpoints return to zero.

The late always arm reduces source calls by **74.95%**. Its hot tail needs only 64 source calls; the remaining 98,240 tail bindings hit. Nevertheless, cumulative cold-prefix collision history combines with later hits to grow the table to **65,536 slots**, despite a 64-value hot set. This growth and its reservation peak are directly observed. Its contribution to elapsed time is not isolated by this experiment.

Hot→unique admits successfully, then receives no hits in its 122,880-binding unique tail. It still grows twice to 16,384 slots and remains enabled with a final cumulative hit rate of **6.20%**. This establishes the one-time cutoff's inability to retire an initially useful cache after reuse disappears; it does not establish a measured latency penalty here.

The quoted reservation includes the implementation's conservative metadata estimate and old/new table overlap. It is not a complete retained-heap measurement. The harness records the process memory authority during serial query windows, not a separate query-ledger peak. Ordinary's 1,152-byte authority peak should not be mistaken for total query memory.

## Why the latency difference cannot be assigned to resizing

For late always versus ordinary, the paired first-row ratio is **1.2391**, while the hot-tail consumption ratio is **1.02355** with a **0.725–1.305** range. First-row arm summaries are approximately 400.9 ms ordinary and 494.9 ms always. The first-row and prefix intervals include upstream ordered-result production; they are not isolated decoder or memo-loop costs.

The harness ends `firstRowAfterEvaluateNs` immediately after the first `result.next()`, before `fixture.consume(row)`. Qualification established projected values are demanded row by row. Current-query table growth in late always cannot occur until the later hot phase restores the cumulative admission conditions. It therefore cannot directly account for the earlier first-row interval as resize CPU. Prior-query heap/JIT history and other process-level effects remain possible, and are not identified by these counters.

There are useful behavior-equivalent controls. Early initial and always have identical hit, miss, collision and capacity traces, but their primary latency ratio spans **0.765–1.188** and their paired allocation ratio is **0.9662**. Hot→unique initial and always also have identical event traces: their primary latency ratio is **1.00036**, versus **1.06138** for the mean sensitivity. These observations caution against assigning modest cross-process differences to the tiny admission branch.

## Allocation clustering and GC

Allocation is usually much more stable within a JVM than across JVMs. For example, late initial process medians cluster near **320.060 MB** or **326.352 MB**; typical five-query spreads within a process are roughly **0.015–0.024%**, with one 0.559% spread. Early ordinary medians span **290.655–317.064 MB**, while most within-process spreads are about 0.02%. Hot→unique ordinary has stable process levels near **455.725, 462.016 and 473.753 MB**.

All-thread allocation exceeds caller-thread allocation by at most approximately 59.5 KB per measured query in these records. The million-byte process differences therefore predominantly occur on the query thread. JIT or escape-analysis decisions are plausible explanations for structured allocation levels, but no compilation evidence was collected to prove that cause. Every process and measurement remains included; there is no justification for deleting a cluster or treating extra within-process measurements as extra independent evidence.

Natural G1 summaries cover the **whole JVM**, including fixture population, warmups and measured queries. Median pause counts are approximately 8 for early, 9 for late and 12 for hot→unique; every JVM completes one concurrent cycle. Concurrent duration overlaps other work and cannot be added to pause duration. These summaries cannot attribute a particular measured-query latency difference to GC. No explicit collection was used.

The ordered wrapper may retain consumed binding sets and Values until close. Zero collected misses in this cohort does not establish useful-key weak rematerialization. The separate natural-GC diagnostic addresses that question; source calls and cold-to-initialized observations are not physical decoder counts.

## Execution scope and next experiment

All 702 queries used the qualified normal AUTO ordered-factorized route with no IR execution. The first eight query root scans use the preferred adjacency source; measured queries 9–13 are its normal alternative-source learning phase. They are matched across all processes but are not a converged post-learning steady state. The preserved tests provide the separate ownership/tier qualification.

The strongest next single ablation is **always admission with a fixed 4,096-slot table versus the current always-admit growth policy**, holding weak ownership, hash, source caches, inputs, consumer and JVM settings equal. The observed late hot set already reuses successfully in the initial 4,096 slots before growth. Qualifying equal exposure/hit outcomes in the new comparison would isolate cumulative-history growth from recovering late reuse. This is a proposed causal test, not a claim that removing growth will necessarily improve latency.
