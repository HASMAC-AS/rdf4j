# Improved snapshot hash: exploratory matched comparison

Candidate production `3397b8c6`: one integer multiply before xor-shift; same default-off compact snapshot semantics. Focused pre/post tests pass (24 on/off; 19 assertions-disabled). Frozen hash-api-classes vs original-api-classes, both with frozen lifetime-combined LMDB classes. Same full eight-million-row fixture/options as RESULTS.md. ABBA fresh JVMs; one measured query, no query warmup. Same harness for all arms now reports first-ready-row, completion and rows/sec.

| Arm | Completion s | First ready row ms | Thread allocation GB |
|---|---:|---:|---:|
| baseline A1 | 41.001 | 907.006 | 31.235 |
| improved compact B1 | 37.981 | 858.207 | 24.571 |
| improved compact B2 | 38.553 | 937.065 | 19.107 |
| baseline A2 | 38.739 | 817.580 | 31.237 |

Mean completion 39.870 → 38.267 s: 4.02% lower latency, 4.19% greater throughput at identical work. This is encouraging but only two forks per arm: no statistical significance or general-query claim. First-row mean 862.293 →897.636 ms, **no first-result improvement**. End-to-end HTTP/Workbench/browser latency is not measured by this harness.

All four runs: exactly eight million rows, identical checksum -3441391656945048864, compiled irKernel, primitive/grouped DISTINCT and memo witness, no spill and zero ledger retained. Exact-domain validation belongs to prior unchanged snapshot implementation; hash change additionally has generated lookup cases at all widths0–257. Frozen first prototype remains available separately.

The hash change removes the clear clustering problem (actual-name probes mean5.03/max16 →1.27/max3), and the updated prototype no longer shows the initial large latency regression in these runs. It remains experimental/default-off. Allocation varies sharply between candidate forks (24.57 vs19.11GB), while baseline stays31.24GB, so further repetitions/profiles are needed to understand compiler sensitivity. Source temporary SimpleBindings were prominent in the first candidate profile; differing escape/inlining behavior is a hypothesis, not established causation.

Do not combine percentages from different runs. Earlier four-way matrix remains evidence for the original compact hash, not the improved candidate. This comparison directly measures the new candidate on top of the lifetime improvements, honoring interactions rather than treating allocation reduction as an end goal.
