# Performance interpretation and next experiment

## Interpretation

The completed cohort supports continuing the direct memo as an off-by-default experiment. It does not pass either prespecified engineering screen. The independent consumer validates all 36 fresh JVMs, 468 queries, 180 measurements, and 312 treatment query pairs. Ratios below are medians of six paired JVM ratios; each JVM contributes the median of its five measurements. These are bounded observations on two fixtures, without a claimed population confidence interval. [Independent results](../independent-v2/independent-results.json), [independent report](../independent-v2/independent-report.md).

| Fixture | Comparison | Latency ratio | Allocation ratio |
|---|---|---:|---:|
| Fanout | direct / ordinary | 0.938869 | 0.838266 |
| Shared tail | direct / ordinary | 0.931292 | 0.854791 |
| Fanout | direct / dense | 0.720358 | 1.001190 |
| Shared tail | direct / dense | 0.790969 | 0.973545 |

The direct memo lowers median latency by about 6.1% and 6.9% against ordinary reuse, with about 16.2% and 14.5% less allocation. Its ordinary comparison fails the consistency requirement because two fanout latency pairs are 1.006659 and 1.014191. Its dense comparison lowers latency in every pair on both fixtures, but fails the allocation requirement because fanout allocates about 0.119% more. The small allocation excess remains a failed gate; it does not erase the much larger descriptive latency benefit. The fixed screens are engineering thresholds, not significance tests. [Results](../independent-v2/independent-results.json).

The supported causal wording is: **the dense strategy increases total query latency on both matched fixtures despite fewer resolver calls**. Dense/ordinary latency ratios are 1.322319 and 1.185796. The experiment measures the complete strategy under natural GC; it cannot assign the gap solely to coordinate copying, hashing, bookkeeping, or retention. Direct/dense also changes strong retention and table organization. All route and adjacency comparisons passed, and raw resolver differences remain outcomes rather than timing corrections. [Report](../independent-v2/independent-report.md), [protocol](../benchmark/PROTOCOL.md).

## Exact limits of the evidence

- Every timed direct owner stayed at 4,096 slots, charged 147,536 array bytes, with no growth or low-hit disabling. Timing therefore qualifies neither the adaptation branches nor their behavior under memory pressure. Correctness contracts cover those branches separately.
- The 0.140701 MiB direct ledger peak excludes retained value graphs and subsequent lexical initialization. It is not a live-heap bound. Allocation reductions likewise do not establish lower live memory; the dense shared-tail ledger peak of 260.303745 MiB does not supply a comparable whole-heap measurement.
- Both workloads are repetitive and fully consumed, with a 1 GiB Java heap and 512 MiB query ledger limit. They do not establish delayed repetition, retained rows, early close, large payloads, high uniqueness, or constrained-memory performance. The interrupted first cohort, preflight, and historical measurements are excluded.
- The timed implementation includes the fifth identity-witness array. The earlier four-array diagnostic failed identity-invalidation contracts; dropping that witness is not a valid performance shortcut.

These boundaries follow the [independent report](../independent-v2/independent-report.md), [frozen protocol](../benchmark/PROTOCOL.md), and [source/lifetime review](../contracts/MEMORY-LIFETIME-REVIEW.md).

## Highest-value next experiment

**Test whether direct lookup keeps its latency benefit when memo-owned payload retention is removed or truly bounded, then test whether that policy can discover repetition arriving after the startup window.** This addresses the production blocker and an unexercised heuristic before expanding the current fixed-fixture timing study.

First perform a matched retention ablation: preserve ordinary reuse and the present strong direct memo as controls, and add a direct table with weak value references while holding the key, identity checks, source/exposure fences, replacement order, capacity, and adaptation policy fixed. Charge the weak-reference objects as well as arrays. Review witness references and owner capabilities for unintended strong retention. This tests the retention policy's total effect, including weak-reference allocation and GC behavior; it does not by itself prove a bound on all query memory. A genuinely bounded strong alternative needs trustworthy graph-size reservations before growth or initialization/mutation hooks that revoke retention before growth. Rechecking size only on a later hit leaves never-revisited entries unaccounted. [Lifetime review](../contracts/MEMORY-LIFETIME-REVIEW.md).

Use a small, prespecified boundary study with these independent axes:

| Boundary | Essential cases | Decision it enables |
|---|---|---|
| Payload ownership | Cold admission; later lexical initialization; initialize an already returned value and never revisit its key | Can a constant array charge conceal increasing memo-owned retention? |
| Consumer lifetime | Discard rows, retain selected rows, close before reading bindings, close after partial consumption | Separate consumer-owned retention from memo retention and verify release without breaking accepted rows. |
| Repetition order | Immediate repetition; 8,192 unique probes followed by a hot suffix; repeated then unique | Quantify the permanent cutoff's lost opportunity and any benefit from periodic reconsideration. |
| Adaptive boundary | Exactly 2,047 versus 2,048 hits at probe 8,192; controlled collision pressure after 16,384 | Exercise the actual decision thresholds, in probes rather than rows. |
| Memory pressure | Increasing decoded working-set bytes at fixed key count; array-admission and growth refusal | Find where extra retention changes GC, latency, or budget behavior while results remain correct. |

Keep heap attribution separate from timing: diagnostic heap/root analysis should distinguish references held by the memo, returned rows, and store, before and after close. Timed runs retain natural GC and report total latency, allocation, preparation, raw resolver calls, ledger peaks, GC behavior, and result checksums under matched settings and alternating order. Do not subtract diagnostic retention or resolver counts from time.

Hold the startup policy fixed for the retention comparison. Only afterward compare permanent disabling with a bounded periodic retry or primitive-key admission policy, preserving the chosen retention mechanism. Otherwise retention and adaptation change together again. A successful outcome needs both a defensible retention contract and measured recovery of late repetition without a material all-unique penalty; a small array ledger alone cannot qualify it.

Reviewed JSON SHA-256: `ba79dee70320b9e11d1aba149311702ab898e2c95acf8d32c7b75b8badfeacd5`.
