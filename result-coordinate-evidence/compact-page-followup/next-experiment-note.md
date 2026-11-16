# A possible next factor-publication ablation

The completed compact-page diagnostic supports a future experiment that omits optional factor publication for the shared-tail fixture's left targets while preserving ordinary last-value reuse. It does not yet support a generic planner rule. Whether this is the next priority depends on the completed current performance study; no experiment described here extends the frozen 36-fork protocol.

## What the measured counters establish

All four compact diagnostic queries have the following final page distribution. These counts were independently reconciled from raw page and query JSONL in `independent-diagnostic-reconciliation.json`; the original raw files are under `diagnostic/runs/compact/`.

| Target | Dense pages | Inline pages | Declared page bytes | Successful factor hits |
|---|---:|---:|---:|---:|
| `l0` | 128 | 0 | 2,097,152 | 0 |
| `l1` | 0 | 8,192 | 4,194,304 | 0 |
| `r0` | 128 | 0 | 2,097,152 | 1,040,384 |
| `r1` | 0 | 8,192 | 4,194,304 | 1,040,384 |
| **Total** | **256** | **16,384** | **12,582,912** | **2,080,768** |

The left pages own 6,291,456 declared bytes, exactly half the total page charge. They supply no successful factor hit in this full-consumption execution. That is an opportunity to test whether their admission, coordinate lookup, witness comparison, publication and cleanup work can be avoided. It is not a measured CPU saving.

Ordinary last-value reuse still records 4,177,855 hits across the complete projection. The diagnostic did not split that counter by target. The deterministic fixture geometry is consistent with left members being consumed consecutively while the right side replays, and compact admission is uncapped here: there are no base-page or promotion refusals. Those facts strengthen the hypothesis but do not prove a general consecutive-only classification.

The compact query ledger peaks at 12,901,412 bytes. No promotion is refused in this normal-budget fixture. Consequently the completed persistent-refusal probe answers a different question from the residual CPU costs in this query. That probe records exactly 1,024 refused promotions followed by one recovery, with no reclamation and a zero endpoint ledger. It does not measure time spent on the reservation attempts or manager monitor.

## What a counterfactual experiment can establish

A future fixture-specific ablation could omit factor publication for the two demonstrated left targets while keeping ordinary reuse, repaired ownership, frame behavior and normal AUTO selection identical. It should verify unchanged results, resolver calls and execution routes, then measure latency, allocation, preparation cost and ledger peaks with matched input/JVM settings and alternating order. Treating this as a counterfactual tests the cost of the observed extra cache work without assuming that the same rule improves other queries.

Apply equivalent diagnostics to fanout before extrapolating. Shared-tail's target distribution cannot explain the other fixture's timing by itself. A large difference in a publication ablation would still combine admission, metadata lookup, reference writes and cleanup; it would not isolate a single instruction or prove that a general classifier is free.

## Proof needed to claim no useful reuse is lost

Suppressing optional factor publication can remain result-correct because a cache miss follows normal resolution. The implementation must preserve the usual value witnesses, source lifetimes, accounting and cleanup, but proving that all reuse is consecutive is not a prerequisite for returning correct results. A heuristic exclusion can be semantically valid while discarding helpful cache entries; adopting it more broadly would require performance and strategy validation across representative workloads. The stronger structural proof below is needed to claim that an exclusion loses no useful nonconsecutive reuse.

Zero successful factor hits is weaker than proof that every reuse is consecutive. It can also reflect expired weak references, rejected semantic witnesses, missing admissions, or a later replay absent from the observed consumption pattern. Retained rows can change referent survival. Rebinding, aliases, unions, optional-join order, wrappers and other execution strategies can change whether a previously seen coordinate returns.

To establish that no useful nonconsecutive reuse is lost, the structural argument should show that a canonical target/cache bank's complete coordinate key cannot reappear after its ordinary memo ceases to cover the value. The key includes target, namespace, group, parent, tuple column and long member position. The reasoning must include unbound gaps, accepted/rejected rows, restored frames, namespace or activation changes, and aliases. Monotone positions alone do not suffice if a parent key or frame can recur.

The existing `acceptedReplayHitAdmitsItsOriginalFactorCoordinate` test is a concrete counterexample to claiming that skipping publication on ordinary cache hits never loses useful reuse: the same value can be accepted at another coordinate and later benefit from that coordinate's factor entry. Ordinary resolution can still supply the correct value without the entry. The proposed counterfactual excludes selected targets in one known fixture; it does not recommend removing that reuse generally.

Useful additional evidence would count ordinary hits, factor lookups/hits, weak and witness misses, resolver calls, and coordinate-key revisits after ordinary-cache displacement per canonical cache bank. Current page admissions are attributed to canonical targets, whereas successful hits are attributed to the requested target. Alias aggregation must therefore be explicit before drawing per-target conclusions.

## When pressure latency would be the better next question

If the completed study or another representative workload shows frequent promotion refusal, investigate constrained-memory latency separately. Vary the duration of competing claims, per-query headroom and process-manager contention; record failed attempts and resolver fallback alongside time. The current retry probe establishes safe retry and recovery, and confirms that a rejected optional reservation is attempted again. It does not establish that this is a material runtime problem.

Neither direction is an automatic follow-up commitment. The current frozen study's results determine which unresolved cost deserves the next experiment.
