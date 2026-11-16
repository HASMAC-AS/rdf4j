# Independent audit of the fixed resolver probe

All four scheduled ABBA JVMs and all 52 queries passed independent raw reconciliation. The primary timing study remains failed/incomplete; no timing or allocation ratios were calculated.

The checker reads raw commands, process results, samples, owner/page records, root audits, and GC logs. It uses the earlier independent raw-owner checker, not the diagnostic driver or its derived analyses. The same-arm references are independently recomputed from all eight earlier raw queries.

## Every observed query

| Run | Stage | Iteration | Resolvers | Delta from same-arm reference | Ordinary weak | Factor weak | Ordinary witness | Factor witness | Page refusals |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| 00-all | warmup | -8 | 32833 | 0 | 0 | 0 | 0 | 0 | 0 |
| 00-all | warmup | -7 | 32833 | 0 | 0 | 0 | 0 | 0 | 0 |
| 00-all | warmup | -6 | 32833 | 0 | 0 | 0 | 0 | 0 | 0 |
| 00-all | warmup | -5 | 32833 | 0 | 0 | 0 | 0 | 0 | 0 |
| 00-all | warmup | -4 | 32833 | 0 | 0 | 0 | 0 | 0 | 0 |
| 00-all | warmup | -3 | 32833 | 0 | 0 | 0 | 0 | 0 | 0 |
| 00-all | warmup | -2 | 32833 | 0 | 0 | 0 | 0 | 0 | 0 |
| 00-all | warmup | -1 | 32833 | 0 | 0 | 0 | 0 | 0 | 0 |
| 00-all | measurement | 0 | 32833 | 0 | 0 | 0 | 0 | 0 | 0 |
| 00-all | measurement | 1 | 32833 | 0 | 0 | 0 | 0 | 0 | 0 |
| 00-all | measurement | 2 | 32833 | 0 | 0 | 0 | 0 | 0 | 0 |
| 00-all | measurement | 3 | 32833 | 0 | 0 | 0 | 0 | 0 | 0 |
| 00-all | measurement | 4 | 32833 | 0 | 0 | 0 | 0 | 0 | 0 |
| 01-omit | warmup | -8 | 32833 | 0 | 0 | 0 | 0 | 0 | 0 |
| 01-omit | warmup | -7 | 32834 | 1 | 0 | 1 | 0 | 0 | 0 |
| 01-omit | warmup | -6 | 32833 | 0 | 0 | 0 | 0 | 0 | 0 |
| 01-omit | warmup | -5 | 32833 | 0 | 0 | 0 | 0 | 0 | 0 |
| 01-omit | warmup | -4 | 32833 | 0 | 0 | 0 | 0 | 0 | 0 |
| 01-omit | warmup | -3 | 32833 | 0 | 0 | 0 | 0 | 0 | 0 |
| 01-omit | warmup | -2 | 32833 | 0 | 0 | 0 | 0 | 0 | 0 |
| 01-omit | warmup | -1 | 32833 | 0 | 0 | 0 | 0 | 0 | 0 |
| 01-omit | measurement | 0 | 32833 | 0 | 0 | 0 | 0 | 0 | 0 |
| 01-omit | measurement | 1 | 32833 | 0 | 0 | 0 | 0 | 0 | 0 |
| 01-omit | measurement | 2 | 32833 | 0 | 0 | 0 | 0 | 0 | 0 |
| 01-omit | measurement | 3 | 32833 | 0 | 0 | 0 | 0 | 0 | 0 |
| 01-omit | measurement | 4 | 32833 | 0 | 0 | 0 | 0 | 0 | 0 |
| 02-omit | warmup | -8 | 32833 | 0 | 0 | 0 | 0 | 0 | 0 |
| 02-omit | warmup | -7 | 32834 | 1 | 0 | 1 | 0 | 0 | 0 |
| 02-omit | warmup | -6 | 32833 | 0 | 0 | 0 | 0 | 0 | 0 |
| 02-omit | warmup | -5 | 32833 | 0 | 0 | 0 | 0 | 0 | 0 |
| 02-omit | warmup | -4 | 32833 | 0 | 0 | 0 | 0 | 0 | 0 |
| 02-omit | warmup | -3 | 32833 | 0 | 0 | 0 | 0 | 0 | 0 |
| 02-omit | warmup | -2 | 32833 | 0 | 0 | 0 | 0 | 0 | 0 |
| 02-omit | warmup | -1 | 32833 | 0 | 0 | 0 | 0 | 0 | 0 |
| 02-omit | measurement | 0 | 32833 | 0 | 0 | 0 | 0 | 0 | 0 |
| 02-omit | measurement | 1 | 32833 | 0 | 0 | 0 | 0 | 0 | 0 |
| 02-omit | measurement | 2 | 32833 | 0 | 0 | 0 | 0 | 0 | 0 |
| 02-omit | measurement | 3 | 32833 | 0 | 0 | 0 | 0 | 0 | 0 |
| 02-omit | measurement | 4 | 32833 | 0 | 0 | 0 | 0 | 0 | 0 |
| 03-all | warmup | -8 | 32833 | 0 | 0 | 0 | 0 | 0 | 0 |
| 03-all | warmup | -7 | 32833 | 0 | 0 | 0 | 0 | 0 | 0 |
| 03-all | warmup | -6 | 32833 | 0 | 0 | 0 | 0 | 0 | 0 |
| 03-all | warmup | -5 | 32833 | 0 | 0 | 0 | 0 | 0 | 0 |
| 03-all | warmup | -4 | 32833 | 0 | 0 | 0 | 0 | 0 | 0 |
| 03-all | warmup | -3 | 32833 | 0 | 0 | 0 | 0 | 0 | 0 |
| 03-all | warmup | -2 | 32833 | 0 | 0 | 0 | 0 | 0 | 0 |
| 03-all | warmup | -1 | 32833 | 0 | 0 | 0 | 0 | 0 | 0 |
| 03-all | measurement | 0 | 32833 | 0 | 0 | 0 | 0 | 0 | 0 |
| 03-all | measurement | 1 | 32833 | 0 | 0 | 0 | 0 | 0 | 0 |
| 03-all | measurement | 2 | 32833 | 0 | 0 | 0 | 0 | 0 | 0 |
| 03-all | measurement | 3 | 32833 | 0 | 0 | 0 | 0 | 0 | 0 |
| 03-all | measurement | 4 | 32833 | 0 | 0 | 0 | 0 | 0 | 0 |

## Interpretation and preservation

Each query has a complete exclusive final-factor decomposition and per-target changes in the JSON receipt. The five terminal miss outcomes sum to resolver calls globally and to remember calls per target. Ordinary weak branches are reported separately: a factor hit can recover one, and ordinary/factor weak branches can occur in the same lookup. Their counts must not be added as additional resolver calls.

All query results, AUTO/native routes, compiled observed-query tiers, current adjacency revisions, root-host audits, page grant/release identities, and zero query-ledger endpoints passed. The four process intervals are serial. Every frozen runtime entry and source/reference input was rehashed and preserved.

GC/reference logs are JVM-wide and do not associate a GC event with a query or referent. Instrumentation changes allocation and close-time page reporting differently between the arms. This receipt therefore makes no latency, allocation, GC-cost, natural miss-rate, or arm-performance inference. New branch observations do not retrospectively identify the earlier timing event. The fixed four-JVM scope is complete without replacement or extension.
