# Independent direct-memo timing analysis

This analysis recomputed the frozen study from raw samples, direct-memo owner records, commands, process results, and root audits. It imported no experiment code and did not read the driver results or summary.

All **36 fresh JVMs**, **468 queries** (288 warmups and 180 measurements), and **312 treatment query pairs** passed independent validation.

The primary outcome is total query cost under natural GC. Strong retention, direct-map bookkeeping, adaptation, coordinate transport differences, and resolver changes are included. No event-based correction or sample exclusion is applied.

## Measured comparisons

Each JVM contributes the median of all five measurements. Ratios pair the same fixture and round; below one favors the numerator. All six pairs contribute to the median and full range. Fixtures and historical studies are not pooled.

| Fixture | Ratio | Latency median [range] | Allocation median [range] | Preparation median [range] | Ledger peak median [range] |
|---|---|---:|---:|---:|---:|
| fanout | direct / ordinary | 0.938869 [0.904949, 1.014191] | 0.838266 [0.838224, 0.847811] | 1.045048 [0.798857, 1.103682] | 60.664474 [60.664474, 60.664474] |
| fanout | direct / dense | 0.720358 [0.648404, 0.778009] | 1.001190 [1.001131, 1.001209] | 0.941029 [0.811265, 1.083972] | 0.065138 [0.065138, 0.065138] |
| fanout | dense / ordinary | 1.322319 [1.198384, 1.444655] | 0.837269 [0.837216, 0.846853] | 1.048549 [0.934738, 1.233328] | 931.319079 [931.319079, 931.319079] |
| shared-tail | direct / ordinary | 0.931292 [0.759988, 0.999835] | 0.854791 [0.854773, 0.854795] | 1.061396 [0.754045, 1.386250] | 100.228261 [100.228261, 100.228261] |
| shared-tail | direct / dense | 0.790969 [0.746203, 0.913119] | 0.973545 [0.973511, 0.973575] | 1.053530 [0.857430, 1.429660] | 0.000541 [0.000541, 0.000541] |
| shared-tail | dense / ordinary | 1.185796 [1.014839, 1.252892] | 0.878018 [0.877996, 0.878032] | 0.932176 [0.826027, 1.184561] | 185426.807065 [185426.807065, 185426.807065] |

## Every primary latency ratio

| Fixture | Comparison | Round1 | Round2 | Round3 | Round4 | Round5 | Round6 |
|---|---|---:|---:|---:|---:|---:|---:|
| fanout | direct / ordinary | 0.904949015 | 0.914643982 | 0.936720477 | 1.006659312 | 0.941018349 | 1.014190932 |
| fanout | direct / dense | 0.755141408 | 0.685575576 | 0.648404467 | 0.768141926 | 0.674818505 | 0.778008987 |
| shared-tail | direct / ordinary | 0.948967131 | 0.901512186 | 0.974783961 | 0.913616885 | 0.759988453 | 0.999835011 |
| shared-tail | direct / dense | 0.913118776 | 0.785076208 | 0.796860876 | 0.746202692 | 0.748876040 | 0.798021999 |

## Absolute observations

These are medians of six JVM medians. Ratios of table entries are not the paired estimator above. Allocation uses decimal GB; ledger peaks use MiB. The direct ledger omits retained Value payload, so its peak is not a live-heap bound.

| Fixture | Arm | Seconds | All-thread allocation GB | Preparation ms | Ledger peak MiB | Raw resolver calls/query |
|---|---|---:|---:|---:|---:|---|
| fanout | ordinary | 2.399233 | 1.630341 | 1.764441 | 0.002319 | 2,770,101 |
| fanout | dense | 3.172454 | 1.365037 | 1.891369 | 2.160042 | 1,147 |
| fanout | direct | 2.273520 | 1.366661 | 1.826934 | 0.140701 | 33,386 |
| shared-tail | ordinary | 1.633235 | 1.320700 | 1.081096 | 0.001404 | 2,113,601 |
| shared-tail | dense | 1.881426 | 1.159598 | 1.015421 | 260.303745 | 32,833 |
| shared-tail | direct | 1.482211 | 1.128922 | 1.065981 | 0.140701 | 172,090 |

## Direct-map behavior

Counts cover all retained queries in each stage. Ordinary and dense create no direct-memo owner. Low-hit disabling is a valid adaptive outcome. Hits count validated table candidates; standalone access may still resolve a candidate to preserve source ownership handoff, while outward capture passes its actual exposure token. Hits are not generally resolver calls avoided. The full per-owner 22-field records and authoritative raw resolver counts are retained in JSON.

| Fixture | Stage | Owners | Probes | Hits | Collisions | Low-hit disables | Successful resizes | Peak capacities observed | Peak array charges observed |
|---|---|---:|---:|---:|---:|---:|---:|---|---|
| fanout | warmup | 48 | 663552000 | 661949472 | 1555440 | 0 | 0 | 4096 | 147536 |
| fanout | measurement | 30 | 414720000 | 413718420 | 972150 | 0 | 0 | 4096 | 147536 |
| shared-tail | warmup | 48 | 301989888 | 293729568 | 8063712 | 0 | 0 | 4096 | 147536 |
| shared-tail | measurement | 30 | 188743680 | 183580980 | 5039820 | 0 | 0 | 4096 | 147536 |

## Prespecified engineering screens

The screen is evaluated separately against each comparator. It requires a median latency ratio at most 0.95 on both fixtures, all six latency ratios below one in each fixture, median allocation at most one, and no allocation pair above 1.02. These are engineering thresholds, not a significance test. Incomplete retained-payload accounting prevents production promotion even if both screens pass.

### Direct versus ordinary

**Did not pass.**

| Criterion | Result | Observed |
|---|---|---|
| 312 treatment query pairs preserve route and adjacency observations | pass | 312 / 312 matched |
| fanout median latency ratio <=0.95 | pass | 0.938869413 |
| fanout every latency ratio <1 | fail | 0.904949015, 0.914643982, 0.936720477, 1.006659312, 0.941018349, 1.014190932 |
| fanout median allocation ratio <=1 | pass | 0.838266161 |
| fanout every allocation ratio <=1.02 | pass | 0.838223655, 0.838284504, 0.838263535, 0.838247873, 0.847810816, 0.838268787 |
| shared-tail median latency ratio <=0.95 | pass | 0.931292008 |
| shared-tail every latency ratio <1 | pass | 0.948967131, 0.901512186, 0.974783961, 0.913616885, 0.759988453, 0.999835011 |
| shared-tail median allocation ratio <=1 | pass | 0.854791041 |
| shared-tail every allocation ratio <=1.02 | pass | 0.854790999, 0.854791084, 0.854791087, 0.854789320, 0.854773390, 0.854794546 |

### Direct versus dense

**Did not pass.**

| Criterion | Result | Observed |
|---|---|---|
| 312 treatment query pairs preserve route and adjacency observations | pass | 312 / 312 matched |
| fanout median latency ratio <=0.95 | pass | 0.720358492 |
| fanout every latency ratio <1 | pass | 0.755141408, 0.685575576, 0.648404467, 0.768141926, 0.674818505, 0.778008987 |
| fanout median allocation ratio <=1 | fail | 1.001190467 |
| fanout every allocation ratio <=1.02 | pass | 1.001203538, 1.001209180, 1.001190131, 1.001179444, 1.001131062, 1.001190802 |
| shared-tail median latency ratio <=0.95 | pass | 0.790968542 |
| shared-tail every latency ratio <1 | pass | 0.913118776, 0.785076208, 0.796860876, 0.746202692, 0.748876040, 0.798021999 |
| shared-tail median allocation ratio <=1 | pass | 0.973545242 |
| shared-tail every allocation ratio <=1.02 | pass | 0.973538285, 0.973546381, 0.973546318, 0.973544166, 0.973510596, 0.973574540 |

## First-query observations

The first query follows fixture construction, excludes JVM launch and fixture setup, and is not used by the warm-latency screen. All eight warmups remain preserved.

| Fixture | Ratio | First-query total median [range] | First-query preparation median [range] |
|---|---|---:|---:|
| fanout | direct / ordinary | 0.891382 [0.770159, 0.943085] | 0.879910 [0.758802, 1.316549] |
| fanout | direct / dense | 0.554387 [0.484268, 0.589410] | 0.880990 [0.712803, 0.962257] |
| fanout | dense / ordinary | 1.583234 [1.524065, 1.736327] | 1.062274 [0.832498, 1.590498] |
| shared-tail | direct / ordinary | 0.909894 [0.817219, 1.072180] | 1.031161 [0.837827, 3.138335] |
| shared-tail | direct / dense | 0.821584 [0.641718, 0.898963] | 0.907631 [0.559930, 2.284263] |
| shared-tail | dense / ordinary | 1.139900 [0.909068, 1.568743] | 1.189660 [0.938627, 2.062441] |

## Qualification and limits

All raw result, checksum, exact shared-tail bag, active-adjacency, normal AUTO, native compiled-tier, root-host, and ledger endpoint gates passed. Direct owners align through semantic-compilation ordinals and cumulative resolver intervals. Both comparator pairs preserve per-query routes and complete adjacency observations, including warmups. Raw resolver differences remain visible in the JSON receipt.

Every direct owner passes the necessary aggregate probe/collision/capacity invariants, earliest possible growth-boundary check, exact charged array-peak calculation, and zero final array-charge gate. Rehash collision discards are zero, as required by fixed-hash power-of-two doubling. Invalidations, unsupported values, and direct table admission refusals are zero on these fixtures. Those checks supplement the sealed correctness contracts; aggregate final counts alone cannot replay every adaptive boundary. Original coordinate-owner admission refusals are also zero. No new observer claims to measure dense factor-page refusal.

The matched runtime retains the original fifteen-class harness and adds the same six-class prefix for all arms. The timed variant has a fifth identity-witness array beyond the four-array proposal. This avoids accepting a value whose internal revision changed without a semantic-version bump. The collector stores primitives at lifecycle boundaries and serializes only after the original launcher returns. These costs are part of the measured implementation.

The direct strategy retains strong values. Array charges exclude their object graphs and later lexical initialization. Therefore allocation and ledger reductions do not establish a retained-heap bound. The comparison does not isolate strong retention from hashing or removal of coordinate transport.

The fixed full-consumption fixtures do not establish performance for delayed repetition, early close, retained rows, long payloads, other projections, high uniqueness, or constrained memory. Six paired JVMs are a bounded engineering observation; no population confidence interval is claimed. No historical, diagnostic, preflight, or incomplete timing is pooled.

Frozen protocol SHA-256: `8140db1dafa30ff1121e56284837b52b7a395464f656a7f3eecf9eea8f0fcc84`.

Independent analysis SHA-256: `9f54c6ae0556d52070e2d1840229222d2588396f249ac50fc699106d860ed570`.
