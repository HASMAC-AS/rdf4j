# A smaller memo hot method did not improve Workbench latency

## Decision

Do not promote this hot/cold split. The previous fastest frozen combination remains selected. The candidate is preserved as source and patch, but its production edit was restored to the exact pre-experiment contents. No change to retention, ownership, budget or correctness guards was needed or justified.

## Why test it?

The preceding miss diagnosis found that 97.33% of misses were first-seen ID/mask keys and repeated misses were only 0.0247% of all probes. Stronger retention or a complex miss table therefore looked unpromising on this fixture. Compiler logs instead showed a real `hot method too big` refusal for memo.value in the result collector.

This behavior-neutral candidate extracts cache lookup, miss insertion and sample-window handling into private helpers. The hot entry shrinks from 598 to 181 bytecode bytes. It preserves exact masks/versions, weak references, callback ordering, counters, sampling/admission, and the early return when an initial ledger reservation fails. The candidate retains the experimental source's existing default-off metadata/policy support; the control is the actual older fastest frozen implementation, not a new flag-OFF control.

## Matched unprofiled ABBA

Same fixture and all other frozen modules; identity-preserving transport and pinned #6440 binary writer enabled, sequential lookup and entry metadata disabled. Fresh server per fork, prior best / split / split / prior best. No competing builds/tests during timing.

| Metric | Previous best mean (range) | Split mean (range) |
|---|---:|---:|
| Full 8M count completion | 66.712s (64.927–68.496) | 70.192s (66.509–73.874) |
| First received row | 0.890s (0.843–0.938) | 1.038s (1.013–1.063) |
| 1M window received | 21.552s (20.819–22.284) | 21.603s (21.500–21.706) |

Completion is 5.2% longer on average; window time is essentially unchanged (+0.24%). Two forks per arm and overlapping completion ranges are not proof of a universal regression, but there is no demonstrated gain. First-row means are cold-request observations, not a general p50/p95 latency characterization.

All runs count eight million rows while emitting a one-million-row window. They do not transfer/render all eight million rows. This is not a browser-paint, concurrent-throughput or upstream-main comparison.

## Safety and artifact discipline

Fresh pre-change: NativeValueMemoTest (16), NativeMemoPolicyTest (11), NativeMemoEntryMetadataTest (10), all passed. Post-change: the same 37-test selection passed in all four metadata/probe OFF/ON configurations (148 executions), plus three replay cases. Independent source review found no concrete semantic difference beyond helper stack frames and JIT shape. These are focused checks, not a full module/repository pass.

Exact HTTP smoke validates 100 tuples with checksum `495a217169c1db06ff284e3b4d8a765a6cc75b575cc8179fa1e448b09628c301`, and counts 8M. All measured and diagnostic requests pass actual compiled-IR, memo, grouping, no-spill, binary-content-type and ledger-release witnesses. Frozen candidate differs from the previous-best LMDB directory only in NativeValueMemo and its nested classes.

The first launcher was interrupted by a shell parsing issue after the successful smoke and before any timing trial. Its log is retained. The actual ABBA used the separate immutable `run-measurements.sh`; no result was overwritten. Replay qualification completed before that ABBA. The candidate's exact source and `candidate.patch` permit reproduction without keeping the unsuccessful production edit active.

## Reproduction caution

The working production memo is restored, so do not run postcheck.sh and assume it tests the archived candidate. Apply candidate.patch on an isolated checkout first; postcheck.sh verifies that the source equals the candidate. run-benchmarks.sh compiles the archived candidate into a new frozen directory and refuses overwrite. Build/test/install steps and all matching receipts are retained. Raw JFR, compiler XML, full environment logs and class directories remain local; published JSON paths are sanitized.

## Compiler result and restoration

The local inline goal succeeded: C2 inlined the split memo entry, lookup helper and Reference.get intrinsic into the collector. However, repository next then refused the enlarged collector (`already compiled into a big method`). The previous best inlined the collector but called the memo. Collector native code grew from 2,560 to 5,408 bytes. This moves the inlining boundary rather than eliminating it; it is supporting code-shape evidence, not proof of the timing difference's cause. See ../memo-diagnosis/HOT_SPLIT_JIT.md.

After restoring the exact pre-experiment production source, a targeted module install and the 37-test memo selection passed again. Frozen previous-best artifacts remain selected. Further memo changes should require a new workload-backed hypothesis rather than smaller helper methods or greater cache retention alone.
