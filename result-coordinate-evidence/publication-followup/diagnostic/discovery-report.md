# Publication targets selected by the prospective rule

The two original-page diagnostics completed eight full queries with normal AUTO selection: three warmups and one observed query in each fresh JVM. All eight passed their result, route, ownership-endpoint and internal counter checks. The common runtime kept coordinate transport and ordinary reuse active. A 512 MiB per-query allowance admitted every page, so target selection was not distorted by the earlier shared-tail admission cap.

The rule was fixed before execution. A canonical target could be selected only if it admitted pages and completed publications, had no successful factor hit, and had no factor weak-reference or witness miss in all four queries. Alias mappings were checked before aggregating requested-target counters into canonical banks. Both fixtures have identity alias mappings. Targets with no pages were excluded.

The exact selected masks are `measurement,reading` for fanout and `l0,l1` for shared-tail. The following counts are the same in every query of each fixture.

| Fixture and target | Publication calls | Completed publications | Actual cell writes | Admitted pages | Declared page bytes | Ordinary hits | Factor hits |
|---|---:|---:|---:|---:|---:|---:|---:|
| Fanout `measurement` | 1,152,000 | 1,152,000 | 100 | 2 | 32,768 | 1,151,900 | 0 |
| Fanout `reading` | 1,152,000 | 1,152,000 | 1,000 | 100 | 1,638,400 | 1,151,000 | 0 |
| Shared-tail `l0` | 1,048,576 | 1,048,576 | 8,192 | 128 | 2,097,152 | 1,040,384 | 0 |
| Shared-tail `l1` | 1,048,576 | 1,048,576 | 8,192 | 8,192 | 134,217,728 | 1,040,384 | 0 |

The fanout mask identifies 2,304,000 completed publication calls for 1,100 cell writes; none of those pages produces a successful factor lookup. Shared-tail identifies 2,097,152 completed publication calls for 16,384 cell writes with the same absence of factor hits. These counts make repeated publication work a concrete candidate for ablation. They do not show its CPU cost: neither the instrumentation nor the count ratios estimate time saved.

Fanout totals are 13,824,000 binding reads, 11,053,899 ordinary hits, 2,768,954 factor hits and 1,147 resolver calls. Shared-tail totals are 6,291,456 binding reads, 4,177,855 ordinary hits, 2,080,768 factor hits and 32,833 resolver calls. In every query, the helper's find calls equal binding reads; ordinary hits plus factor hits plus resolver calls equal binding reads; remember calls equal resolver calls. No ordinary or factor weak-reference or witness miss was recorded. No page admission was refused.

The fanout query ledger peaks at 2,264,968 bytes, including 2,031,616 declared page bytes. Shared-tail peaks at 272,948,260 bytes, including 272,629,760 declared page bytes. Every admitted page was observed at explicit release, with all granted page bytes released. Ordinary, directory and memo claims are observed only as close-time snapshots in this diagnostic. The separate harness proves zero ledger endpoints after every query. Declared charges are ledger accounting, not measurements of physical heap residency.

The selected shared-tail targets own half the declared page charge. The fanout targets own 1,671,168 of 2,031,616 declared page bytes. The remaining projected targets in each fixture include the actual factor hits: shared-tail `r0` and `r1` each supply 1,040,384; fanout's nine remaining replay targets supply the complete 2,768,954. Fanout `patient` and shared-tail `s,a` supply no factor hits and admit no pages, so they are explicitly outside the selected masks.

`suites/original512/candidate-masks.json` recomputes the rule from all four raw owner JSONL files per fixture. The per-fixture `analysis.json` files record 91 fanout checks and 99 shared-tail checks, plus per-target partition and page-release equations. The initial source was bound to preserved SHA `cb0749719313b85c8cd134b6c03085f712915188f0e53333cbd46e19e2f0f96e`, and the 128-entry base classpath matched the common preserved runtime. Compilation verified that discarded plan/page/memo class descriptors were compatible with the classes retained from the base runtime. Only the materializer outer class and diagnostic helper were overlaid.

This evidence describes these full-consumption executions. It does not prove that a general planner can safely classify all future reuse as consecutive, and does not establish behavior under early close, retained rows, different bindings or other strategies. The next source-matched diagnostic uses the same masked-capable code for both all-target and omitted-target arms, then repeats shared-tail at the normal 256 MiB allowance to distinguish avoided publication work from admission relief.
