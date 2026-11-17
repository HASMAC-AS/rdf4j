# Frozen-artifact compilation follow-up

Both `memo-jit-baseline.json` and `memo-jit-entry.json` report `witnessesPassed: true`; both LogCompilation XML files have their completed `hotspot_log_done` element. These are diagnostic runs, not matched performance measurements. Miss-counter instrumentation was not on their classpaths.

## New evidence

| Final C2 body | Previous best | Metadata entry |
| --- | ---: | ---: |
| Memo compile ID | 12106 | 11894 |
| Memo bytecode bytes | 474 | 598 |
| Memo task_done nmsize | 4,144 | 4,544 |
| Memo nmethod total size | 4,408 | 4,808 |
| Memo inlined_bytes | 428 | 452 |
| Collector compile ID | 11413 | 11102 |
| Collector task_done nmsize | 2,560 | 2,560 |

Sizes are HotSpot log metrics, not estimates of wall-time cost. Final means last recorded standalone C2 compilation of the named method in this run.

1. In **both** memo bodies, `Reference.get` is hot-inlined and its `get0` is the `_Reference_get0` intrinsic. The earlier attribution to the checkcast after `get` was not evidence of a slow uninlined Java reference getter.
2. In **both**, the profiled source at the miss call is `SyntheticValueSource`. `lazyValue` becomes a guarded direct call after monomorphic type checking. Its inline is refused with `already compiled into a medium method`. The baseline call is BCI 230; candidate BCI 306. It is not an unresolved megamorphic interface dispatch in either final body.
3. In **both**, memo `value` is refused at collector BCI 116 with `hot method too big`, leaving a direct call. The final collector itself is hot-inlined into `LmdbStoreConnection$1.next` (baseline compile 11717, candidate 11635), and those root compilations also refuse memo inlining with the same reason. Candidate metadata did not newly cross this particular inline threshold: the previous best was already over it.
4. All four final standalone tasks succeed, with no compiler failure attached. No failed task with a memo or collector root was found. Other whole-log compilation failures include retry-without-subsuming-loads and stale tasks; these do not establish a memo compiler defect. Inline refusals above are optimization decisions, not compilation failure.

## Interpretation and next experiment

The records establish similar key inlining/dispatch decisions and a larger candidate memo body. They do **not** establish that larger code caused the prior neutral performance result or explain the exact change in sampled instruction attribution. Assembly/PC mapping would be needed to localize native instructions further. In particular, **“the miss call costs 9% wall time” remains unsupported** by the earlier sampled BCI shares.

Parent's separate miss-count experiment reports 2,046,547 misses / 220.8M probes, of which 1,992,000 are first-seen keys, 54,547 repeated misses, 10 same-key cleared weak references, and zero semantic-version misses. Those counts are a separate instrumented diagnostic, not counts inferred from HotSpot early type profiles. They argue against stronger weak-value retention as the next target for this fixture.

A defensible next candidate is extracting cold admission/sample/resize work from the previous-best memo to shorten the hit-path entry, preserving every correctness guard and weak-reference semantics. The reason is the confirmed hot-method-too-big refusal on a very frequent call. Do not assume a speedup: verify actual generated inlining and compare throughput/latency against unchanged previous best, including regression tests for sampling, migration, semantics and cleanup. Do not force-inline the existing large method as the primary production proposal.

## Reproduction and scope

`python3 experiments/overlay/memo-diagnosis/summarize-compilation.py` reads only completed logs and emits `compilation-summary.json`, including final call decisions and all observed memo/collector call sites. Raw XML and server logs remain under `experiments/overlay/workbench-e2e/`. No production code, frozen classes, builds or queries were run by this analysis worker.
