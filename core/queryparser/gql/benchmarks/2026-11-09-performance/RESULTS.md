# Complete paired benchmark results

Latency is the JMH mean ± its reported 99.9% confidence-interval half-width. Ratios divide the two means; no confidence interval for a ratio is asserted. An overlap is treated as inconclusive. Allocation uses decimal MB (1,000,000 bytes).

| Case | Before ms/op | After ms/op | Before / after | Latency interpretation |
| --- | ---: | ---: | ---: | --- |
| property-named-1 | 8.384 ± 0.786 | 7.153 ± 1.560 | 1.17× | Intervals overlap |
| property-named-64 | 157.475 ± 15.116 | 8.579 ± 1.385 | 18.36× | After faster; intervals separate |
| property-named-256 | 833.614 ± 293.000 | 6.789 ± 0.869 | 122.78× | After faster; intervals separate |
| property-merged-64 | 158.509 ± 17.078 | 6.024 ± 1.072 | 26.31× | After faster; intervals separate |
| topology-merged-64 | 1.846 ± 0.225 | 1.799 ± 0.241 | 1.03× | Intervals overlap |
| topology-named-64 | 1.741 ± 0.134 | 2.443 ± 1.840 | 0.71× | Intervals overlap |
| topology-merged-fanout128 | 8.942 ± 1.624 | 8.805 ± 3.948 | 1.02× | Intervals overlap |
| label-named-64 | 18.326 ± 11.693 | 12.254 ± 1.446 | 1.50× | Intervals overlap |
| next-union-named-64 | 8.953 ± 2.271 | 9.065 ± 4.684 | 0.99× | Intervals overlap |
| prepare-property-named-64 | 162.199 ± 26.347 | 6.741 ± 1.321 | 24.06× | After faster; intervals separate |

## Allocation and fixture sizes

| Case | Statements | Expected count | Before MB/op | After MB/op | Allocation change |
| --- | ---: | ---: | ---: | ---: | ---: |
| property-named-1 | 1,088 | 512 | 3.364 | 3.725 | +10.7% |
| property-named-64 | 69,632 | 512 | 16.216 | 3.822 | -76.4% |
| property-named-256 | 278,528 | 512 | 66.070 | 3.798 | -94.3% |
| property-merged-64 | 69,632 | 512 | 13.226 | 3.724 | -71.8% |
| topology-merged-64 | 67,584 | 1,024 | 1.487 | 1.213 | -18.4% |
| topology-named-64 | 67,584 | 1,024 | 1.215 | 1.214 | -0.1% |
| topology-merged-fanout128 | 16,512 | 16,384 | 2.915 | 2.745 | -5.9% |
| label-named-64 | 69,728 | 1,024 | 8.230 | 8.260 | +0.4% |
| next-union-named-64 | 67,584 | 2,048 | 6.488 | 6.529 | +0.6% |
| prepare-property-named-64 | 69,632 | 512 | 12.759 | 3.787 | -70.3% |

## Adjacency engagement per evaluation

These counters cover warmup and measurement in both forks, exclude initial setup diagnostics, and are divided by the corresponding evaluation count. They establish route engagement, not latency by themselves. Raw per-fork counts are retained in summary.json and the full logs.

| Case | Lookup hits before → after | Kernel views before → after | Planner stats before → after |
| --- | ---: | ---: | ---: |
| property-named-1 | 9.0583 → 1060 | 3.05775 → 0 | 1 → 1 |
| property-named-64 | 12.7936 → 1060 | 6.78198 → 0 | 1 → 1 |
| property-named-256 | 17.5333 → 1060 | 11.4889 → 0 | 1 → 1 |
| property-merged-64 | 39.2361 → 1027 | 2.225 → 0 | 1 → 1 |
| topology-merged-64 | 36.0002 → 14.0129 | 0 → 2.00617 | 1 → 1 |
| topology-named-64 | 14.029 → 14.0404 | 2.0143 → 2.0199 | 1 → 1 |
| topology-merged-fanout128 | 132 → 24.0939 | 0 → 10.3624 | 1 → 1 |
| label-named-64 | 107.001 → 107 | 0 → 0 | 1 → 1 |
| next-union-named-64 | 72 → 72.0004 | 0 → 0 | 0 → 0 |
| prepare-property-named-64 | 9.98663 → 1060 | 3.97594 → 0 | 1 → 1 |
