# Independent numerical review of growth comparison v2

**Final pass.** The frozen independent auditor accepted all 54 JVMs and 702 queries and reproduced the primary calculations to the prescribed 1e-12 tolerance. All six matched process pairs and all five measurements per process were retained. Rejected comparison-v1 contributes no performance observations.

Sources: [independent audit](cohort-independent-audit-v2.json), SHA-256 `f1431692eaed0bfee20a4a13227edc95ec4fa7c0b6c48a015d1d432a7c001a9d`, and [numerical detail](NUMERICAL-REVIEW-v2.json), SHA-256 `03f3c733d4483c8ad2a9ce8c27e276a1115cc955ebe1fbe514b1c82f220ae376`. The latter includes every paired ratio below, all nine comparisons with ordinary reuse, full owner/call ranges, allocation observations and GC summaries.

## Fixed versus adaptive

Ratios below one favor fixed. Each estimate is the median of six same-round ratios; each process contributes its five-query median. The mean sensitivity uses all five queries per process and stays separate.

| Fixture | Total primary | Total mean sensitivity | Fixed lower, total pairs | Tail elapsed | Tail caller CPU | Tail caller allocation |
|---|---:|---:|---:|---:|---:|---:|
| early-cold-hot | 0.819682 | 0.795537 | 5/6 | 0.793423 | 0.819152 | 1.000000 |
| late-cold-hot | 1.012979 | 1.025954 | 2/6 | 1.049416 | 1.045735 | 0.866081 |
| warm-cycle-8192 | 0.998332 | 0.947475 | 3/6 | 1.358159 | 1.358229 | 1.139152 |

The six total-latency ratios, in round order, are:

- **early-cold-hot:** 1.054611, 0.806233, 0.492549, 0.538328, 0.902253, 0.833131.
- **late-cold-hot:** 0.663444, 1.099493, 0.740786, 1.021212, 1.004745, 1.186715.
- **warm-cycle-8192:** 1.906999, 0.464733, 0.809707, 0.970682, 1.508553, 1.025982.

There is no primary-versus-mean direction reversal in any of the nine total-latency comparisons. There is also no fixed/adaptive total-latency winner across all six pairs. In the larger working set, the near-unity total estimate coexists with a 35.8% tail CPU increase and pair ratios from 0.465 to 1.907.

## Reuse, capacity and ownership

The following per-query observations hold across all 78 queries in each fixture/arm, including warmups. Lazy source calls are not physical decode counts.

| Fixture | Arm | Source calls | Hits | Collisions | Resizes | Capacity | Ledger peak, bytes |
|---|---|---:|---:|---:|---:|---:|---:|
| early-cold-hot | adaptive | 4,128 | 126,944 | 1,544 | 0 | 4,096 | 606,448 |
| early-cold-hot | fixed | 4,128 | 126,944 | 1,544 | 0 | 4,096 | 606,448 |
| late-cold-hot | adaptive | 32,832 | 98,240 | 28,738 | 4 | 65,536 | 14,549,472 |
| late-cold-hot | fixed | 32,832 | 98,240 | 28,738 | 0 | 4,096 | 606,448 |
| warm-cycle-8192 | adaptive | 32,566 | 98,506 | 24,857 | 4 | 65,536 | 14,549,472 |
| warm-cycle-8192 | fixed | 107,656 | 23,416 | 104,128 | 0 | 4,096 | 606,448 |

Early cold-hot is the decisive interpretation control: neither mode grows, and their core event counts match. Its 18.0% primary latency difference cannot be credited to avoided rehash work. Policy checks and other process variation remain possible explanations; this cohort does not identify their contributions.

Late cold-hot preserves exactly 32,832 source calls and 98,240 hits while removing four resizes. Fixed cuts tail allocation by 13.4% and the accounted metadata peak from 14,549,472 to 606,448 bytes. Nevertheless, total latency is 1.3% higher and tail CPU is 4.6% higher in the paired primary estimates, with mixed pair directions. It establishes an accounting/allocation benefit, not a demonstrated speed improvement.

Warm-cycle-8192 increases source calls from 32,566 to 107,656 (3.305779 times) and tail allocation by 13.9%. Its tail CPU cost supports the expected capacity tradeoff even though noisy total latency obscures it. Ordinary reuse makes 131,072 source calls per query in every fixture.

All 468 memo owners opened and closed; all 702 ledger endpoints returned to zero. Collection misses, invalidations, admission refusals, low-hit disables, unsupported-value events and rehash discards were zero. Source calls equal projected materializations throughout. The reservation peak includes resize overlap and weak metadata; it is not a retained-heap bound or a separate query-ledger measurement.

## Before-prefix work and allocation clusters

| Fixture | Fixed/adaptive first-row elapsed | To-prefix caller CPU | To-prefix caller allocation | Total caller allocation |
|---|---:|---:|---:|---:|
| early-cold-hot | 0.787327 | 0.776166 | 1.022207 | 1.019989 |
| late-cold-hot | 0.976882 | 0.999710 | 1.006854 | 0.995603 |
| warm-cycle-8192 | 0.953561 | 0.963266 | 0.999998 | 1.014026 |

First-row work and the to-prefix interval include the blocking ordered path; to-prefix also includes connection, preparation and evaluation. The late first-row estimate changes direction under mean sensitivity (0.976882 to 1.014834). Warm-cycle preparation changes from 1.004929 to 0.939671, and evaluation from 0.961693 to 1.089547. These component reversals must remain visible; they do not alter the prespecified total estimator.

Allocation varies much more across JVMs than within most five-query process groups. For example, warm fixed to-prefix medians are 301.22350, 309.29111, 281.15627, 295.22356, 295.22346 and 295.22321 MiB, while its tail medians stay between 37.70075 and 37.73164 MiB. Most process groups have only about 0.048 MiB of within-process prefix spread. Early ordinary rounds 1 and 2 differ by exactly 6 MiB before the prefix. These are descriptive allocation clusters, with no proven causal attribution. All clusters remain in the estimator.

Paired ratios can differ from a ratio of marginal arm medians. For example, warm fixed and adaptive marginal total medians are about 867 and 819 ms, while the matched primary ratio is 0.998332. Replacing the paired calculation with those marginal medians would change the analysis.

## Counter reconciliation and scope

An additional direct read of the 702 accepted samples found maximum absolute CPU-phase residual **0 ns**, caller-allocation residual **0 bytes**, total wall-interval residual **0 ns**, and prefix-plus-tail consumption residual **0 ns**. Each all-thread allocation total covers its caller total. These identities hold per query; independently aggregated component medians need not sum to an aggregated total.

Caller CPU excludes GC, JIT and other worker threads. Tail includes the final exhaustion check and any automatic cleanup, ending before explicit result/connection close. All-thread allocation ends later, after capture cleanup, so subtracting the caller total does not isolate worker allocation.

GC logs span entire JVMs, including setup and eight warmups. Their pauses cannot be assigned to the five measured query windows from these summaries, and concurrent durations overlap. Ordered rows retain Values, and no collection misses were observed; the cohort does not establish useful-key rematerialization under GC. No data-integrity contradiction was found. No Java was launched and no frozen source, protocol or raw record was edited.
