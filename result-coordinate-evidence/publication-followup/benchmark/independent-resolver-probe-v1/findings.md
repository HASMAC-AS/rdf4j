# Independently confirmed resolver-probe findings

The fixed all/omit/omit/all diagnostic completed exactly four fresh JVMs and 52 queries. Independent raw reconciliation passed every query, all eight earlier same-arm reference queries, all four serial process intervals, the complete 130-entry runtime, and 113 pinned raw/source/reference inputs. The driver summary was checked only after the independent calculation and agrees exactly.

Native execution comprised 4 interpreted and 48 compiled binds, with zero aggregate-tier binds. Every result oracle, normal AUTO/native route, adjacency revision, root-host audit, page grant/release identity, and zero query-ledger endpoint passed.

## Changed observations

| Run | Stage | Iteration | Resolver calls | Delta from same-arm reference |
| --- | --- | ---: | ---: | ---: |
| 01-omit | warmup | -7 | 32,834 | +1 |
| 02-omit | warmup | -7 | 32,834 | +1 |

The remaining 50 queries have 32,833 resolver calls and reference-identical materializer counters. In each changed query, only `r1` differs:

- `r1.coordinateAttempt`: +1.
- `r1.coordinateSuccess`: +1.
- `r1.factorHit`: -1.
- `r1.factorWeakMiss`: +1.
- `r1.publicationCall`: +1.
- `r1.publicationComplete`: +1.
- `r1.publicationWrite`: +1.
- `r1.rememberCall`: +1.

Each changed query has the exclusive final-factor miss decomposition `69 initial + 24,700 absent-page + 8,064 absent/mismatched-cell + 1 cleared-weak + 0 witness = 32,834`. This reconciles globally with resolvers and per target with remember calls. All other target counters and final page counts/occupancy match the same-arm reference. All ledger peaks also match that reference.

Across all 52 queries, ordinary weak branches total 0, ordinary witness branches 0, final factor weak branches 2, final factor witness branches 0, and page-admission refusals 0.

## What this establishes

The two new instrumented queries each reached the existing final-factor cleared-weak branch once, then resolved and republished the value instead of recording one factor hit. Ordinary and factor weak events remain separate; their counts are never added as extra resolutions. No witness or refusal event explains these two probe differences.

All four JVM logs declare G1 and contain reference-processing messages, with no explicit System.gc() text. The logs are not aligned to individual queries or referents. The unchanged diagnostic helper itself changes allocation and close-time work, so these observations do not establish natural miss rates or any arm-performance ranking. They also do not prove the cause of the earlier uninstrumented timing event.

The original primary timing protocol remains failed and incomplete at nine JVMs/117 queries. No performance ratios, replacements, extensions, relaxed gates, or production promotion follow from this probe.

Full raw-file identities, all 52 query decompositions, and all per-target counter changes are preserved in `independent-resolver-probe.json`; the later claim comparison is `summary-reconciliation.json`.
