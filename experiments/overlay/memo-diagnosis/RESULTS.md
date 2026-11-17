# Diagnose memo misses before adding cache complexity

## Miss counts on the previous best

A separate instrumented copy of the prior fastest frozen memo counted miss causes. Before instrumentation, normalized javap instruction output from the recovered source matched the frozen class exactly. Production and frozen classes were not modified for these diagnostics.

| Counter | Counters only | Exact first-seen keys enabled |
|---|---:|---:|
| Probes | 220,800,000 | 220,800,000 |
| Hits | 218,753,457 | 218,753,453 |
| Misses | 2,046,543 | 2,046,547 |
| Empty slot | 65,770 | 65,760 |
| Cleared reference, same key | 6 | 10 |
| Cleared reference, different key | 750,802 | 754,606 |
| Live reference, different key | 1,229,965 | 1,226,171 |
| Semantic-version mismatch | 0 | 0 |
| Non-native/uncacheable | 0 | 0 |
| First-seen ID/mask pairs | not counted | 1,992,000 |
| Repeated misses | not counted | 54,547 |

97.33% of misses are the first request for an ID/mask pair within this memo, under its current lookup contract. Repeated misses are only 0.0247% of all probes. A different-key collision is not automatically avoidable: a first-ever key necessarily needs its first lookup, regardless of which bucket it occupies.

This argues against stronger value retention or elaborate miss-table/associativity work on this fixture. It does not prove an architectural lower bound on value creation, rule out hit-path improvements, or convert counts into wall-time savings.

The exact-key set retains primitive-key records, not RDF values, but it adds allocation/retention and can perturb GC. The counters-only run provides a check: total misses differ by four; the breakdown of dead/live different-key entries changes as expected with GC timing. Neither instrumented run is a performance measurement. Both validate the same 100 emitted tuples, count 8M results, use compiled IR/grouping/memo without spill, and return the ledger to zero.

## JIT result and next experiment

Separate diagnostics used the **unchanged** frozen previous-best and metadata candidates, with all compilation events and HotSpot inline logging. Both use the weak-reference intrinsic and guarded direct source calls. Both refuse memo inlining into the collector because the hot method is too large. There is no demonstrated unresolved megamorphic dispatch, absent weak-reference intrinsic or recurring memo deoptimization storm to fix.

See JIT_FINDINGS.md and JIT_FOLLOWUP.md for compiler IDs, sizes, exact decisions and limitations. Sample attribution is not evidence that the miss call costs 9% of wall time.

The warranted next experiment is a behavior-neutral hot/cold split to shorten memo.value, preserving callbacks, counters, sample boundaries, weak references, versions, masks and budget-refusal early return. Verify actual inlining and full-combination throughput/latency directly against the previous best. A smaller method is not a performance result by itself.
