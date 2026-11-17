# Final production comparison

Baseline: published commit `741b9389538e80214075692439a8cd25a8a9ae84`.

Two fresh JVM forks per implementation, five one-second warmup and five one-second measurement iterations; one thread. The large-document table uses the actual heartbeat payload wrapper and stable namespaces. JFR and exploratory timings are excluded.

## Large documents: 4,096 rows

| Workload | Before, µs/row | After, µs/row | Time reduction | Before, B/row | After, B/row |
| --- | ---: | ---: | ---: | ---: | ---: |
| [ascii](final-ascii.json) | 2.150 | 1.210 | 43.7% | 361.394 | 360.395 |
| [unicode](final-unicode.json) | 2.245 | 1.224 | 45.5% | 409.395 | 408.395 |
| [repeat_identity](final-repeat_identity.json) | 0.308 | 0.286 | 6.9% | 1.325 | 0.332 |
| [repeat_equal_lmdb](final-repeat_equal_lmdb.json) | 0.388 | 0.333 | 14.1% | 1.325 | 0.333 |
| [unique_lmdb](final-unique_lmdb.json) | 2.204 | 1.185 | 46.2% | 1.337 | 0.338 |
| [wide](final-wide.json) | 10.095 | 5.817 | 42.4% | 1441.592 | 1440.504 |
| [medium](final-medium.json) | 5.313 | 2.829 | 46.8% | 361.416 | 360.666 |
| [medium_unicode](final-medium_unicode.json) | 4.846 | 3.792 | 21.8% | 409.413 | 409.513 |
| [long](final-long.json) | 31.135 | 22.958 | 26.3% | 9505.587 | 361.544 |
| [long_unicode](final-long_unicode.json) | 58.540 | 41.692 | 28.8% | 29889.738 | 409.661 |
| [long_latin1](final-long_latin1.json) | 46.188 | 33.841 | 26.7% | 27601.671 | 361.618 |
| [long_cjk](final-long_cjk.json) | 70.107 | 32.602 | 53.5% | 63721.828 | 361.609 |
| [long_emoji](final-long_emoji.json) | 59.435 | 33.324 | 43.9% | 27601.765 | 361.611 |
| [long_sparse](final-long_sparse.json) | 32.628 | 31.475 | 3.5% | 9505.590 | 361.597 |
| [query](final-query.json) | 8.912 | 7.713 | 13.5% | 7049.140 | 7451.197 |

### Timing uncertainty and fork means

All values below are µs/row. The ± values are JMH’s 99.9% confidence errors. Intervals are descriptive; overlapping intervals and host variability preclude treating every point estimate as a demonstrated gain.

| Workload | Before mean ± error | After mean ± error | Before fork means | After fork means | Intervals overlap |
| --- | ---: | ---: | --- | --- | --- |
| ascii | 2.150 ± 0.057 | 1.210 ± 0.053 | 2.141, 2.159 | 1.205, 1.214 | no |
| unicode | 2.245 ± 0.250 | 1.224 ± 0.052 | 2.280, 2.209 | 1.243, 1.205 | no |
| repeat_identity | 0.308 ± 0.019 | 0.286 ± 0.025 | 0.297, 0.319 | 0.291, 0.282 | yes |
| repeat_equal_lmdb | 0.388 ± 0.018 | 0.333 ± 0.015 | 0.393, 0.383 | 0.333, 0.334 | no |
| unique_lmdb | 2.204 ± 0.206 | 1.185 ± 0.267 | 2.175, 2.233 | 1.320, 1.051 | no |
| wide | 10.095 ± 0.603 | 5.817 ± 0.522 | 9.915, 10.275 | 6.110, 5.525 | no |
| medium | 5.313 ± 0.762 | 2.829 ± 0.431 | 5.537, 5.089 | 2.867, 2.791 | no |
| medium_unicode | 4.846 ± 1.064 | 3.792 ± 0.678 | 4.670, 5.021 | 3.763, 3.820 | yes |
| long | 31.135 ± 3.614 | 22.958 ± 3.183 | 31.017, 31.253 | 23.366, 22.551 | no |
| long_unicode | 58.540 ± 3.141 | 41.692 ± 3.928 | 59.342, 57.738 | 41.440, 41.943 | no |
| long_latin1 | 46.188 ± 2.617 | 33.841 ± 2.340 | 47.433, 44.944 | 33.856, 33.827 | no |
| long_cjk | 70.107 ± 4.000 | 32.602 ± 4.854 | 70.586, 69.628 | 30.033, 35.171 | no |
| long_emoji | 59.435 ± 5.308 | 33.324 ± 10.448 | 56.646, 62.225 | 28.941, 37.706 | no |
| long_sparse | 32.628 ± 16.455 | 31.475 ± 19.269 | 42.797, 22.460 | 19.726, 43.224 | yes |
| query | 8.912 ± 1.404 | 7.713 ± 1.493 | 8.272, 9.552 | 7.169, 8.257 | yes |

## Small documents: memory sink

| Workload | Before, µs/document | After, µs/document | Time reduction | Before, B/document | After, B/document |
| --- | ---: | ---: | ---: | ---: | ---: |
| [0 rows](final-small.json) | 1.396 | 0.693 | 50.4% | 5000.010 | 936.005 |
| [1 rows](final-small.json) | 3.253 | 1.825 | 43.9% | 5792.023 | 1728.013 |
| [8 rows](final-small.json) | 12.708 | 8.172 | 35.7% | 8232.089 | 4192.057 |

### Timing uncertainty and fork means

All values below are µs/document. The ± values are JMH’s 99.9% confidence errors. Intervals are descriptive; overlapping intervals and host variability preclude treating every point estimate as a demonstrated gain.

| Workload | Before mean ± error | After mean ± error | Before fork means | After fork means | Intervals overlap |
| --- | ---: | ---: | --- | --- | --- |
| 0 rows | 1.396 ± 0.148 | 0.693 ± 0.033 | 1.480, 1.312 | 0.692, 0.693 | no |
| 1 rows | 3.253 ± 0.167 | 1.825 ± 0.217 | 3.201, 3.304 | 1.729, 1.921 | no |
| 8 rows | 12.708 ± 1.235 | 8.172 ± 0.268 | 12.645, 12.771 | 8.252, 8.091 | no |

## Large-document memory-sink control

| Workload | Before, µs/row | After, µs/row | Time reduction | Before, B/row | After, B/row |
| --- | ---: | ---: | ---: | ---: | ---: |
| [ascii](final-memory.json) | 1.744 | 0.970 | 44.4% | 361.360 | 360.362 |
| [long_unicode](final-memory.json) | 57.450 | 36.905 | 35.8% | 29889.723 | 409.601 |

### Timing uncertainty and fork means

All values below are µs/row. The ± values are JMH’s 99.9% confidence errors. Intervals are descriptive; overlapping intervals and host variability preclude treating every point estimate as a demonstrated gain.

| Workload | Before mean ± error | After mean ± error | Before fork means | After fork means | Intervals overlap |
| --- | ---: | ---: | --- | --- | --- |
| ascii | 1.744 ± 0.737 | 0.970 ± 0.114 | 1.990, 1.498 | 0.997, 0.944 | yes |
| long_unicode | 57.450 ± 7.551 | 36.905 ± 7.011 | 59.543, 55.358 | 40.140, 33.670 | no |

## Interpretation

The query workload includes native query evaluation and serialization. Synthetic fixtures preconstruct their input values. Long workloads encode different numbers of bytes, so comparisons are within each workload. Allocation includes other model and document costs; it is not a count of encoder bytes alone.

Complete raw samples, source hashes and exact commands are preserved beside this report. See `methodology-review.md` and `REPRODUCING.md` for scope and reproduction.
