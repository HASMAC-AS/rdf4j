# Independent control and omission reconciliation

All six new diagnostic JVMs, comprising twenty-four queries, pass an independent raw reconciliation. Eight original-discovery queries also pass as the unchanged-control reference. The implementation in `check-publication-controls.py` uses Python's standard library and the separately written raw checker in this directory; it imports no diagnostic analyzer or benchmark runner.

The detailed receipt is `independent-publication-controls.json`, SHA-256 `d4e1d67b40d4a1e8c3641f6c1afdfc8e1821eda02411fee0ef3c85acbeaa46df`. It pins eighty input files, including raw owner records, harness samples, root audits, commands, process results, frozen suite protocols, runtime descriptors, and runtime companion identities. The corresponding `selection-suggestion.json` is supplied for the root agent to review before freezing the timing protocol.

## The all-enabled overlay preserves the original reference

At 512 MiB per query, the experimental all-enabled overlay exactly matches the original diagnostic on every query's operation totals, individual canonical-bank counters, page charges, and complete factor-page keys. Resolver counts, routes, tiers, and adjacency counters also match. Both fixtures have no projection aliases in these executions. This establishes that the additional configuration/guard code leaves the observed cache behavior unchanged when no target is omitted; it is not a timing comparison of guard overhead.

## The omission removes precisely the selected publication work

| Quantity, per query | Fanout | Shared-tail |
|---|---:|---:|
| Omitted targets | `measurement,reading` | `l0,l1` |
| Skipped publication calls | 2,304,000 | 2,097,152 |
| Pages removed | 102 | 8,320 |
| Page bytes removed | 1,671,168 | 136,314,880 |
| Pages before → after | 124 → 22 | 16,640 → 8,320 |
| Ledger peak before → after | 2,264,968 → 592,008 B | 272,948,260 → 136,502,308 B |
| Resolver calls in both arms | 1,147 | 32,833 |
| Ordinary hits in both arms | 11,053,899 | 4,177,855 |
| Factor hits in both arms | 2,768,954 | 2,080,768 |

Every value repeats across all four queries in the corresponding comparison. Each selected target skips every publication call and makes no page admission attempt, page-cell write, or completed publication. The remaining targets retain exactly the same complete page keys, not merely the same total page count. Their relevant publication/admission/hit counts also match. Both arms have zero factor-page admission refusals at 512 MiB.

The differences in query ledger peaks exceed the removed page charges slightly because the smaller page directories also require less declared memory. These remain diagnostic reservation observations. The diagnostic's extra counters and JSON output preclude using its duration or allocation fields as performance estimates.

## The normal-budget comparison confirms admission redistribution

The shared-tail comparison at 256 MiB uses exactly the same diagnostic runtime and instrumentation as the 512 MiB controls. All four query pairs show the same result:

| Shared-tail quantity | Publish all, 256 MiB | Omit `l0,l1`, 256 MiB |
|---|---:|---:|
| Page admission refusals | 1 | 0 |
| Resolver calls | 65,345 | 32,833 |
| Total pages | 16,364 | 8,320 |
| `r0` pages | 126 | 128 |
| `r1` pages | 8,064 | 8,192 |
| `r0` factor hits | 1,024,128 | 1,040,384 |
| `r1` factor hits | 1,024,128 | 1,040,384 |
| Peak ledger | 268,426,276 B | 136,502,308 B |

Removing left-side publication admits 130 additional right-side pages and yields 32,512 additional successful factor hits, with exactly 32,512 fewer resolver calls. Ordinary-hit counts remain unchanged. The omitted 256 MiB execution exactly matches the omitted 512 MiB execution's per-bank traffic and complete page keys, recovering the uncapped resolver/hit counts.

This is why the CPU ablation uses 512 MiB for all timed arms. At the normal 256 MiB allowance, publication removal also improves cache admission and reduces resolver work; those are combined mechanisms. The 512 MiB controls establish equal resolver work and zero admission refusal, allowing the following timing experiment to isolate the cost of the diagnosed publication work more cleanly.

## Cleanup and execution checks

All local outcome partitions and independent binding/hit/resolver equations reconcile. Every page grant and byte has an observed explicit release. Every query returns the prescribed count and checksum; shared-tail also passes its exact bag oracle. All query ledger endpoints are zero. Normal AUTO, native IR, matching per-query tiers, active query-time adjacency, and absence of hosted generic roots are independently confirmed. Commands and successful process results match the frozen runtime/protocol identities.

These findings qualify the fixture-specific timing experiment. They do not establish a generic planner rule, benefit under retained rows or early close, or a latency saving. The selected source remains unchanged. No Java process was launched during this independent analysis.
