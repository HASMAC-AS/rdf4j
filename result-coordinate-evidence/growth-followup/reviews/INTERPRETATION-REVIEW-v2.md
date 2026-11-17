# Interpretation review of the accepted growth comparison v2

## Judgment

The late small-working-set prediction was correct about useful reuse and memory accounting: fixed capacity preserves every observed hit and source-call count while eliminating four resizes, reducing the metadata reservation peak, and reducing allocation in the measured tail. It did **not** establish a latency improvement. The larger-working-set control also behaved as a useful counterexample: fixed capacity loses substantial reuse and increases tail CPU and allocation. Its near-unity whole-query latency ratio does not negate that cost. These results justify investigating a more selective growth rule; they do not justify choosing fixed capacity as a generally faster policy.

Before another end-to-end performance claim, the most valuable measurement work is a bounded diagnosis of the dominant first-row work and the observed differences between fresh JVMs under normal AUTO. This cohort matched the complete observable route history but did not identify the cause of its large first-row and allocation differences. A recent-window collision rule is a narrow subsequent mechanism experiment, with cumulative hit eligibility and admission behavior kept intact. Neither investigation requires reinterpreting, pruning, or extending this completed cohort.

## Evidence and review boundary

Only `comparison-v2` is observational evidence here: 54 fresh JVMs, 432 warmup queries, and 270 measured queries, with all 702 results and profiles accepted. Each fixture has six matched process pairs per comparison. The primary estimate is the median of the six ratios of five-query process medians. The separately declared mean sensitivity uses the same five queries and the same pairing. Warmups, exact pilots, rejected comparison-v1, and previous studies contribute no performance observations.

The final profile checker accepted all 702 corresponding query positions, and the independent auditor reproduced the primary calculations within its declared tolerance. This reviewer read the accepted raw sample records for allocation and timing decomposition, the primary audit, the independent numerical review, and the implementation sources. No Java was launched and no frozen execution source or observation was changed.

The source overlay changes the constructor-captured growth flag and guards the existing resize branch. Hashing, full keys, weak ownership, semantic and source identity validation, admission, output handoff, and accounting remain shared. All arms select always admission and weak retention, with coordinate transport disabled; ordinary disables the experimental direct memo. The enabled arms differ only in growth selection. See [the implementation review](IMPLEMENTATION-REVIEW-v1.md), [prospective protocol](../benchmark/PROTOCOL.md), and [independent numerical review](NUMERICAL-REVIEW-v2.md).

## What the memory and reuse prediction established

The following core counts are constant across all 78 queries in each fixture/arm, including warmups, as independently checked. Source calls are calls to the lazy source API, not counts of physical LMDB decodes.

| Fixture | Growth | Source calls | Memo hits | Resizes | Final capacity | Accounted peak, bytes |
| --- | --- | ---: | ---: | ---: | ---: | ---: |
| early-cold-hot | adaptive | 4,128 | 126,944 | 0 | 4,096 | 606,448 |
| early-cold-hot | fixed | 4,128 | 126,944 | 0 | 4,096 | 606,448 |
| late-cold-hot | adaptive | 32,832 | 98,240 | 4 | 65,536 | 14,549,472 |
| late-cold-hot | fixed | 32,832 | 98,240 | 0 | 4,096 | 606,448 |
| warm-cycle-8192 | adaptive | 32,566 | 98,506 | 4 | 65,536 | 14,549,472 |
| warm-cycle-8192 | fixed | 107,656 | 23,416 | 0 | 4,096 | 606,448 |

For the late 64-key tail, the smaller table preserves the exact 98,240 hits and 32,832 calls. The accounted peak falls by 13,943,024 bytes, approximately 95.8%. The adaptive peak includes the overlap between the old and new tables during the largest resize, including the conservative weak-reference metadata allowance. It is neither the final table size nor a measure of the retained Java heap. Both enabled arms have the same weak-entry allocation count in this case, so fixed is not obtaining its reduction by declining useful entries.

The source explains why cumulative growth can be unnecessary here. `NativeDirectValueMemo.find` counts key collisions over the whole iteration, and `adapt` checks those lifetime totals every 8,192 probes. After the initial admission boundary, it grows when cumulative hits reach 25% and cumulative collisions reach 12.5%, up to 65,536 slots. It does not ask whether the recent repeated tail is still suffering collisions. The late fixture reports 28,738 collisions in both modes; its small tail reaches the same useful reuse without growth. Cold-prefix collisions can continue to satisfy the growth condition after current demand has become easy to serve. This is a mechanism supported by the source and counts, not a measured estimate of time spent rehashing.

The larger-set control prevents generalizing that conclusion to every workload. Suppressing growth loses 75,090 hits per query and increases lazy source calls by a factor of 3.305779. Fixed makes 107,656 calls out of 131,072 projected opportunities, versus 32,566 for adaptive and 131,072 for ordinary. Its overall memo hit rate is about 17.9%, compared with about 75.2% for adaptive. Always admission deliberately allows that low-hit fixed result to remain visible. It is an outcome, not a reason to reject those observations.

All 468 memo owners opened and closed, and all 702 ledger endpoints returned to zero. Collection misses, invalidations, unsupported admissions, admission refusals, low-hit disables, and rehash discards were zero. This establishes clean behavior in this qualified cohort. It does not establish behavior under constrained-memory refusal or useful-key collection; neither occurred here.

## Latency, caller CPU, and allocation tell different parts of the result

All ratios below are fixed divided by adaptive; values below one favor fixed for that metric. They are paired estimators, not ratios of marginal arm medians.

| Fixture | Whole query, primary | Whole query, mean sensitivity | Primary pair range | Tail elapsed | Tail caller CPU | Tail caller allocation |
| --- | ---: | ---: | --- | ---: | ---: | ---: |
| early-cold-hot | 0.819682 | 0.795537 | 0.492549–1.054611 | 0.793423 | 0.819152 | 1.000000 |
| late-cold-hot | 1.012979 | 1.025954 | 0.663444–1.186715 | 1.049416 | 1.045735 | 0.866081 |
| warm-cycle-8192 | 0.998332 | 0.947475 | 0.464733–1.906999 | 1.358159 | 1.358229 | 1.139152 |

The late case supplies a particularly clear answer to the original hypothesis. Removing four resizes lowers tail allocation by 13.4%; the six differences between the process-median tail allocations are all approximately 3.44 MB. The source allocates five replacement arrays per resize and reuses the weak entry objects, which is consistent with the location and scale of that reduction. An allocation stack profile was not collected, so this is not an exact assignment of every saved byte to one source line.

Despite those savings, late tail caller CPU is 4.6% higher and tail elapsed time 4.9% higher in the primary paired estimates. Fixed has lower whole-query medians in only two of six pairs. The mean sensitivity also puts fixed above adaptive for total, tail CPU, and tail elapsed time. A 1.3% primary total increase, wide pair range, and six process pairs do not establish a general regression; they do rule out presenting this cohort as a demonstrated end-to-end improvement from suppressing growth.

For the larger set, fixed increases tail allocation by 13.9% and the primary tail CPU and elapsed estimates by about 35.8%. The mean sensitivities still point toward higher tail CPU and elapsed time, although their magnitudes are smaller: approximately 11.1% and 12.1%. The tail CPU result is consistent with the extra source calls and weak-entry allocations. It is a broader consumption interval than just memo work, so it does not quantify the cost of each lost hit.

Its primary whole-query ratio is nevertheless 0.998332, with three of six process pairs favoring each arm. This is not evidence that the capacity loss is free. Large first-row differences and the much larger work before the prefix can outweigh the tail when measured as total latency. Marginal absolute medians are about 867 ms for fixed and 819 ms for adaptive, while the matched primary ratio is near one. Both are valid descriptive summaries of different calculations; substituting the marginal ratio would violate the frozen estimator.

The ordinary reference remains important. Adaptive/ordinary whole-query ratios are 1.3162, 0.9313, and 0.8588 for early, late, and larger-set fixtures respectively; fixed/ordinary ratios are 0.9153, 0.9156, and 0.8694. These mixed whole-query outcomes coexist with substantial reductions in source calls. In the larger set, fixed's tail caller CPU is about 1.1150 times ordinary, while its tail allocation is about 0.9740 times ordinary. Avoiding some wrapper creation does not by itself make the memo's total checking and retention work faster.

Whole-query allocation savings are also much smaller than the source-call reductions. The primary all-thread allocation ratios versus ordinary are approximately 0.9601/0.9661 for early adaptive/fixed, 0.9921/0.9845 for late, and 0.9813/0.9952 for the larger set. Most allocation in this ordered query occurs before the consumed-prefix boundary. The earlier synthetic allocation result therefore cannot be transferred to this workload or presented as its measured saving.

## Why the early control and first-row source placement matter

Early adaptive and fixed have the same capacity, zero resizes, 1,544 collisions, 4,128 source calls, 126,944 hits, and metadata peak. Their primary whole-query ratio is still 0.819682, and their tail CPU ratio is 0.819152 while tail allocation is essentially equal. This is an equal-event control, **not** a pure A/A control: the growth flag and execution of its boundary guard differ. The observed difference may include flag-dependent behavior, compilation history, or other between-process effects. Their contributions were not measured. Because no resize occurred in either arm, avoided resizing cannot explain this result.

The ordering of operations supports an even more precise attribution limit:

1. `NativeOrderedRowsIteration.createIteration` calls `evaluateAll(...).iterator()` on first demand. `evaluateAll` performs the blocking ordered work and returns the result list.
2. `NativeRowsStep.projectSortedRows` creates projected rows while constructing that list. The lazy projected path does not attach the output materializer there.
3. The ordered iterator's `next` attaches the materializer when the caller requests a row. `LmdbStoreConnection.openEvaluation` then captures that row at the outward boundary. `NativeProjectedBindingSet` and `NativeResultMaterializer` perform the direct memo lookup and admission for the demanded bindings.
4. The harness records `firstRowAfterEvaluateNs` just after the first `result.next()`. That includes blocking first-demand work and the first row's handoff. Four projected values cannot reach even the first 8,192-probe admission boundary, much less a later growth boundary.

Therefore a difference in the current query's first-row interval is not time spent in that query's later resizes. Effects from previous queries in the same JVM remain possible, and the flags differ across JVMs; this source ordering does not prove those broader effects absent. Across the 54 JVMs, the ratio of each process's first-row median to its total median is approximately 80–92%, showing why whole-query interpretation is dominated by this interval. Those separately aggregated ratios are not an exact decomposition of a single query.

Preparation and evaluation are recorded separately and are generally much smaller than first demand in this cohort. A lower preparation or evaluation median should not be attributed to output growth either. Normal AUTO selected ordered-factorized sorting throughout, with the qualified source-learning transition from 98,309 adjacency lookups on queries 1–8 to 98,308 on queries 9–13. That is matched learning history, not proof of converged source selection or identical machine-code compilation history. The compiled/interpreted IR lifetime qualifications are correctness evidence, not the route that this cohort timed.

## Allocation clusters and estimator sensitivity

Accepted raw samples show mostly stable allocation within a fresh JVM and substantial offsets between JVMs, concentrated before the prefix. For example, warm fixed has to-prefix process medians of 301.22350, 309.29111, 281.15627, 295.22356, 295.22346, and 295.22321 MiB in round order, while its tail process medians stay within 37.70075–37.73164 MiB. Many process groups have only about 0.048 MiB of within-process prefix spread. Early ordinary, which has no direct memo, has an exact 6 MiB prefix-median difference between rounds 1 and 2. The independent numerical JSON preserves all 54 process vectors and ranges.

These clusters demonstrate that variation in whole-query allocation is not a clean measurement of the resize arrays. They do not identify its cause. It would be unsupported to assign the offsets to GC, JIT, a particular allocator, or a specific AUTO decision from these records alone. No cluster or unfavorable process was removed. Whole-JVM GC summaries include population and warmups and cannot assign a pause or cycle to a measured query; concurrent cycles also overlap other work.

There is no primary-versus-mean direction reversal for any of the nine whole-query comparisons. Component estimates are less stable: late fixed/adaptive first-row elapsed changes from 0.976882 to 1.014834, and to-prefix caller CPU from 0.999710 to 1.010021. Larger-set preparation changes from 1.004929 to 0.939671 and evaluation from 0.961693 to 1.089547. Early adaptive/ordinary tail elapsed changes from 0.9446 to 1.0296 and tail caller CPU from 0.9437 to 1.0337. These remain reported sensitivities; none authorizes replacing the primary estimator or selecting favorable components after inspection.

Caller CPU and allocation phase sums reconcile exactly per query. The tail includes final exhaustion `hasNext` and automatic cleanup, ending before explicit result and connection close. Caller CPU excludes GC, JIT, and other worker threads. The all-thread allocation interval ends later, after selection-capture cleanup, than the aligned caller interval; their subtraction is not pure worker allocation. Separately aggregated phase medians need not sum to the aggregate total.

## What to test next

### First resolve the measurement question needed for a speed claim

A finite diagnostic of first-demand work and normal AUTO/compilation history has higher priority than another assertion of end-to-end policy superiority. Keep the current cohort closed. Prospectively select a small diagnostic set including the early equal-event case, retain every result, and place query/phase markers alongside source-learning and compilation observations so any assignment is made to the correct query interval. A true same-arm repeat control can estimate repeatability without the growth-flag difference; the existing early comparison cannot do that alone.

The diagnostic should distinguish source-learning evolution, compiled-code evolution, and other unassigned process variation. Longer warmup is a possible new treatment, not an automatic repair: it requires a newly qualified complete AUTO history and a declared stopping rule. Do not force a convenient route or discard slower JVMs. Targeted profiling of the first-demand interval is useful if attribution is required, but profiler observations stay separate from unprofiled latency estimates. This is a recommendation for new work, not a claim that this review identified a compiler or GC problem.

### Then isolate collision recency as one mechanism change

The minimal growth ablation replaces only the collision-demand predicate at the existing 8,192-probe boundaries. Compare the current cumulative-collision condition with a condition based on collisions since the preceding boundary, using the existing 12.5% threshold over that complete window. Keep the cumulative hit threshold, first-boundary admission behavior, always/initial admission selection, capacity bounds, weak retention, full keys, identity checks, reserve-before-allocation behavior, and refusal fallback unchanged. Lifetime counters remain lifetime counters; an additional boundary checkpoint supplies the recent collision delta.

The checkpoint must advance on every boundary, even when the cumulative hit gate is false or growth is otherwise ineligible. Otherwise old cold-prefix collisions can be carried into the first eligible window and the proposed treatment fails to isolate recency. Any source scope clearing should preserve the existing admission/history semantics; changing that at the same time would introduce another treatment.

This is motivated by a specific hypothesis: the late small tail should stop paying for collisions that occurred during its unique prefix, while a genuinely capacity-limited repeated set should still be allowed to grow. Preserving exactly the current larger-set hit count is not guaranteed. A recent window may stop at an intermediate capacity or change the sequence of misses. Report that as the result, including unfavorable hit or latency changes.

Do **not** reset hit eligibility to the same recent window in this first ablation. The larger-set fixture starts with an 8,192-probe warm bootstrap, then an 8,192-key disjoint cycle. Its first new cycle has little or no reuse, and fixed capacity produces an overall hit rate below 25%. A recent-hit gate could refuse the very growth needed to improve that rate and remain stuck at the small table. Keeping cumulative hit eligibility preserves the bootstrap evidence while changing only whether recent collisions justify another resize. This is a source-grounded risk to test, not a measured outcome of an unimplemented policy.

Two freshly qualified memo arms are the minimum comparison for this predicate: cumulative collisions versus recent collisions, on all three fixtures with matched ownership bytes and balanced order. Ordinary and fixed controls remain useful if the question includes their end-to-end tradeoffs, but previous v2 observations must not substitute for fresh matched controls. Before timing, targeted contracts should cover the unchanged admission boundary, checkpoint advancement while ineligible, late cold-to-hot transition, larger-set growth opportunity, identity/cleanup, and reservation refusal. Continue to gate on correctness and implementation invariants; hits, capacity attained, CPU, allocation, and latency are outcomes. Reuse the accepted per-query phase scopes and normal AUTO qualification discipline.

The official [Caffeine Efficiency wiki](https://github.com/ben-manes/caffeine/wiki/Efficiency) describes a recency window and a frequency-based admission policy, including adaptation to changing access patterns. [TinyLFU, arXiv:1512.00727](https://arxiv.org/abs/1512.00727), explicitly bases admission on recent access history and studies hit ratios across traces. These primary sources motivate retaining information about recent demand. They do not validate an 8,192-probe collision window, this direct-mapped growth rule, or RDF4J query latency. No Caffeine dependency or TinyLFU implementation is proposed for this ablation.

## Limits that remain after this experiment

This is one ordered four-column query shape, full consumption, 64-character IRIs, ample ledger limits, one pinned JVM configuration, and a matched AUTO learning phase. The ordered result list can retain consumed binding sets and Values, and short payloads are eligible for source caching. No weak collection misses were observed. Streaming output, retained subsets after closure, early close, large decoded payloads, memory refusal, different projection widths, and converged AUTO behavior remain distinct experiments. The reservation is a conservative memo metadata charge, not complete Java/native memory. The feature remains experimental and disabled by default.

## Evidence identity

The hashes below bind this interpretation to the completed evidence. Numerical detail is owned by the independent auditor; this file changes no estimate or acceptance rule.

| Artifact, relative to the growth phase | SHA-256 |
| --- | --- |
| `comparison-v2/protocol.json` | `2aa846bb18d8c1eb028ca2fc9b90c782ac5446a7bcbcbf0cd94fc893d6539e0d` |
| `comparison-v2/audit.json` | `9ae4f85990d95aef7f74f0733c17883f3a19874ea1ef32fb2a7607c34bde49fa` |
| `comparison-v2/profile-final-v1.json` | `d1c9038bd764262b03143431ceb66d1c3ae161f220503cd72cfb19173fcfc08f` |
| `reviews/cohort-independent-audit-v2.json` | `f1431692eaed0bfee20a4a13227edc95ec4fa7c0b6c48a015d1d432a7c001a9d` |
| `reviews/NUMERICAL-REVIEW-v2.json` | `03f3c733d4483c8ad2a9ce8c27e276a1115cc955ebe1fbe514b1c82f220ae376` |
| `benchmark/qualification/final-runtime-v1.json` | `de7cdee686e3bfe8875829e1ea69d04e316f1bf6638a4bba2508da7abc9b17b9` |

The growth implementation reviewed is `implementation-src/org/eclipse/rdf4j/sail/lmdb/evaluation/NativeDirectValueMemo.java`, SHA-256 `8c7c482beb7118c020a14d919b2b406c3632c2abcd5f851b7fa5bceded347928`. The shared materializer and projected binding set in that directory are unchanged companions, with SHA-256 `a1e569055db8d9a5af42fa2d375811e5de280112fe0b0d7b46c551f1d662bfa6` and `089f854e63e3accf0871c072c6fefff47e7f5de298b2f87ccc9551adf41306e8` respectively. The compile receipt identifies only the memo and its nested weak-entry class as changed; qualification verifies the root LMDB classes against the preserved baseline. Source placement was read in `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativeRowStep.java` and `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbStoreConnection.java`, and timing boundaries in the phase harness.
