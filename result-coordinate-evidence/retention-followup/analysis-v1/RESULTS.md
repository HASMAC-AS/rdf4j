# Fresh strong versus weak memo comparison

All 36 scheduled JVMs and all 468 queries completed. The 180 measured queries passed result, route, adjacency and cleanup gates. Each ratio pairs JVM medians within the same round and fixture.

| Fixture | Comparison | Latency ratio | Six-pair range | Allocation ratio |
| --- | --- | ---: | ---: | ---: |
| fanout | weak/strong | 0.988624 | 0.908447–1.088229 | 1.001065 |
| fanout | strong/ordinary | 0.935842 | 0.854843–1.041044 | 0.838262 |
| fanout | weak/ordinary | 0.958302 | 0.851157–0.977661 | 0.839131 |
| shared-tail | weak/strong | 0.981591 | 0.812478–1.055481 | 1.006100 |
| shared-tail | strong/ordinary | 0.934329 | 0.790926–1.188451 | 0.854790 |
| shared-tail | weak/ordinary | 0.938527 | 0.785763–0.977167 | 0.860004 |

A ratio below 1 favors the numerator. These are descriptive engineering results, not a significance test or a production-promotion gate.

| Fixture | Arm | Median query seconds | Allocation bytes | Preparation ms | Ledger peak bytes | Resolver calls |
| --- | --- | ---: | ---: | ---: | ---: | ---: |
| fanout | ordinary | 3.523619 | 1630367272 | 1.837997 | 2432 | 2770101 |
| fanout | strong | 3.232626 | 1366672540 | 2.138138 | 147536 | 33386 |
| fanout | weak | 3.282007 | 1368056908 | 2.440059 | 606448 | 33386 |
| shared-tail | ordinary | 2.465851 | 1320732948 | 1.259567 | 1472 | 2113601 |
| shared-tail | strong | 2.396675 | 1128937688 | 1.126407 | 147536 | 172090 |
| shared-tail | weak | 2.259718 | 1135815572 | 1.259591 | 606448 | 172090 |

These absolute figures are medians of six JVM medians. Ledger charges are accounting values: strong mode charges arrays; weak mode also reserves conservative weak-reference metadata. Neither number is total retained heap.

| Fixture | Arm | Whole-JVM GC pauses | Pause ms | Concurrent cycles | Concurrent-cycle ms |
| --- | --- | ---: | ---: | ---: | ---: |
| fanout | ordinary | 37.5 | 267.959 | 1 | 33.918 |
| fanout | strong | 32 | 254.358 | 1 | 50.570 |
| fanout | weak | 33.5 | 247.317 | 1.5 | 65.275 |
| shared-tail | ordinary | 31.5 | 319.118 | 1 | 61.168 |
| shared-tail | strong | 28 | 286.656 | 1 | 36.175 |
| shared-tail | weak | 29 | 297.912 | 1 | 55.535 |

GC figures cover the whole JVM, including fixture setup and warmups. Concurrent-cycle time overlaps other work and is not added to pause time. No forced collections or profiles were used.

The JSON contains all six paired ratios, per-JVM medians and weak-reference counters. Historical observations, diagnostics and warmups are excluded from the latency estimator.
