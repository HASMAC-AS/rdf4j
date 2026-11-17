# Isolate admission before choosing a new policy

This is a prospective experiment design against checkpoint `47e6eba9bd1b238ea84c1b9ad4cd8cdf28caebaf`. It is not a measurement report. Existing implementation, tests, protocols, and archives remain frozen. The parent ExecPlan is [../EXECPLAN.md](../EXECPLAN.md); its owner controls builds, Java processes, runtime qualification, and timing.

## Decision

Compare ordinary lazy Values, the existing weak memo with its initial cutoff, and the same weak memo with only that cutoff removed. Name the experimental property `rdf4j.lmdb.nativeValueMemo.admission=initial|always`, defaulting to `initial` and captured once by each memo constructor. `always` means that a low first-window hit rate does not disable admission. It must still respect source/exposure fences, semantic and identity validation, weak retention, the existing growth conditions, initial and growth reservation refusal, and close. It does not promise a hit, a minimum capacity, or admission after a refused reservation.

This narrow ablation can answer whether the first cutoff loses enough later reuse to matter and what it costs on unrepetitive traffic. It cannot choose periodic sampling, an eviction policy, or a universal cache size. Do not add another cache algorithm to this experiment.

The previous [retention report](../../retention-followup/RESULTS.md) supplies the motivation: all measured enabled queries stayed at 4,096 slots with no cutoff or growth. Measured weak clearing encounters did not increase source-call totals; one warmup had one additional same-key miss. The preceding study therefore did not establish the cost of frequent useful-key collection or delayed reuse.

## The relevant boundary is earlier than probe 8,192

The source of truth is `NativeDirectValueMemo.adapt` in [the preserved implementation](../../retention-followup/implementation-src/org/eclipse/rdf4j/sail/lmdb/evaluation/NativeDirectValueMemo.java). It requires at least 2,048 hits among the first 8,192 memo probes. Let F be the one-based position of the first actual hit. Even if every remaining probe hits, there can be at most `8192 - F + 1` hits at the boundary. Survival therefore requires `F <= 6145`.

The clean exact-edge trace is C distinct cold keys followed by repetitions of the last cold key. The last key is certainly resident immediately before the repeat, regardless of collisions earlier in the prefix. With live consumer roots, C=6,144 yields exactly 2,048 hits and survives; C=6,145 yields 2,047 hits and disables. The lookup at the disabling boundary can still return its already captured valid Value. Subsequent requests bypass the memo.

A first-repeat sweep only at probes 8,191, 8,192, and 8,193 would disable all three traces. It demonstrates the known limitation but misses the actual admission edge. A first logical repeat also need not be a cache hit: a conflicting key, collection, a fence, or invalidation may have removed the earlier representative.

For a fresh W-key hot set first introduced after P unique cold keys, the best possible first-window hit count is `8192 - P - W`; the theoretical survival edge is `P + W = 6144`. If the hot keys already appeared in the prefix, this expression changes, and the first repeated cycle may contain eviction misses. Record which construction is used. Projected columns that repeat a key within one row can be served before the memo; rows times projection width is not automatically the memo-probe count.

## Growth is a second, independent confounder

The first boundary never grows the table. At 16,384 and every subsequent 8,192 probes, growth requires both cumulative hits of at least 25% and cumulative collisions of at least 12.5%. There is no later low-hit disabling decision. Doubling stops at 65,536 slots.

Consequently, a large cold cyclic working set can be disabled before its first repeat. Removing the cutoff does not guarantee that it ever grows: a collision-heavy 4,096-slot table with too few hits can remain at 4,096 indefinitely. An admitted-state bootstrap provides historical hits that can permit growth before the larger working set has reused anything. It changes the policy history and must be reported separately from a cold start.

Cumulative collisions also persist after a phase change. A formerly cold prefix can cause later growth even when the new hot set is only one key. As a source-derived arithmetic model, keys 1 through 8,192 with the full OBJECT mask occupy 3,546 initial buckets and incur 4,646 collisions if referents stay live. Repeating only key 8,192 afterward under `always` makes the unchanged rules grow at probes 16,384, 24,576, and 32,768, reaching 32,768 slots. Those larger tables cannot improve this one-key tail. This prediction comes from the preserved integer hash and thresholds, not a Java observation. With prefixes of 6,144 or 6,145 keys, the same model predicts 2,956 or 2,957 collisions and one growth at 16,384.

Report capacity changes beside phase boundaries. The admission ablation includes downstream consequences of the unchanged growth rule; it is not a fixed-capacity comparison. The existing growth contract uses a pair that collides even at 65,536, so it proves growth mechanics but does not demonstrate that growth improves the hit rate.

## Test-first contracts

The new [NativeDirectValueMemoAdmissionContractTest.java](test-src/org/eclipse/rdf4j/sail/lmdb/evaluation/NativeDirectValueMemoAdmissionContractTest.java) contains 12 tests. It deliberately keeps returned Values in test consumer roots and fences those roots. No explicit collection is requested; every returned native ID and lexical value is checked. Its fresh source has no value cache. These contracts isolate policy behavior and make no retention or latency claim.

| Contract | Required observation |
| --- | --- |
| Always admission after 8,192 cold misses | The table remains enabled and 64 repetitions of the last cold key are same-object hits. |
| Default admission after the same prefix | All 64 repetitions use fresh ordinary Values; memo probes stop at 8,192 and reservation is zero. |
| Exact initial acceptance edge | 6,144 cold misses followed by 2,048 hits stays enabled. |
| Exact initial rejection edge | 6,145 cold misses followed by 2,047 hits disables; the boundary hit remains valid. |
| Always admission with 16,384 unique keys | Collision pressure alone does not grow the table without cumulative hits. |
| Initially hot, then unique | No later cutoff, including after cumulative hits fall below 25%; capacity changes are allowed. |
| Shared positive growth boundary | Both modes stay at 4,096 through probe 16,383 and grow to 8,192 at 16,384 when both conditions hold. |
| Initial reservation refusal | Releasing independent ledger pressure does not revive the disabled memo. |
| Growth reservation refusal | The old table is released; removing pressure does not restart admission. |
| Constructor policy snapshot | Changing the process property does not change an existing owner's policy. |
| Fresh iteration | Entries, counters, and the previous owner's disabled state do not cross replay. |
| Close | Reservation is released exactly once and later lookup cannot reopen the closed table. |

The smallest requested red selector is `NativeDirectValueMemoAdmissionContractTest#alwaysAdmissionReusesLateValuesAfterTheFirst8192ColdMisses`. Against the unchanged implementation, three tests are expected to fail: this one, the all-unique always test, and the constructor-policy test. The other nine characterize preserved behavior. This is a prediction until the root agent captures Surefire evidence. Do not change the implementation before that evidence exists. Preserve the original test bytes after the red seal; formatting can use a separate review copy.

## Compact trace matrix

Keep the following layers separate. Do not take their Cartesian product.

| Layer and trace | Purpose and controls |
| --- | --- |
| Cold C keys, then the last key; C=6,144, 6,145, 8,192 | Exact edge and late opportunity. First stop at 12,288 total requests to avoid the first growth boundary; longer query tails may deliberately exercise downstream growth. |
| Entirely unique traffic | Cost of keeping an unusable memo active. Initial should stop at 8,192; always continues without growing when hits remain zero. |
| Hot 8,192-request prefix, then unique traffic | Cost after useful repetition stops. Initial and always should make the same admission decisions because the only disabling check already passed. |
| Cyclic W-key working set, W=64, 4,096, 8,192, 16,384, 65,536 | Capacity and collision behavior under the actual hash. Run both from cold and after an 8,192-request repeated bootstrap key disjoint from the cycle. Bound each trace to 131,072 total requests. |
| A collision pair that splits at 8,192 slots versus a pair that still collides at 65,536 | Distinguish useful growth from permanent conflict. Hold the hot/collision request schedule and all ownership mechanics fixed. This is a small diagnostic, not another production algorithm. |
| One same-key revisit after D distinct intervening keys, with zero versus one conflicting intervening key | Demonstrate that direct-map reuse depends on bucket conflicts rather than age alone. Start with D=4,095/4,097 inside one adaptation interval; only extend to the maximum-capacity case if the cyclic sweep leaves a specific question. |

The first working-set diagnostic should implement only the ten cyclic cases: five W values times cold/bootstrap. Use the fixed real memo, ordinary fallback as a control, and explicit live consumer roots. Count requests, hits, source calls, collisions, metadata reservation, and capacity at every 8,192-request boundary. This is an untimed mechanism diagnostic. At W=65,536, the bootstrapped case has 122,880 cyclic requests, less than two full cycles; report that fact and tail-only hit counts rather than implying a steady state.

Within a cyclic tail, the request gap is W and the number of distinct intervening keys is W-1. The fixed hash can leave some keys permanently conflicted even when W is no larger than the slot count. Capacity is an observed state, not an input inferred from W. A bootstrap that enables larger capacity is evidence of history dependence, not evidence that the larger table is attainable from the corresponding cold trace.

For full queries, use ordinary, weak/initial, and weak/always with matching input, source caches, ownership code, JVM, heap, ledger budgets, and full result consumption. Keep normal AUTO selection. Qualify the ordered native route and actual projected-probe order before fixing the cohort. A native ORDER BY sink can retain materialized result rows; such a fixture can establish admission behavior while failing to isolate weak collection. Do not use it as proof of GC-driven rematerialization.

## GC and source-cache follow-up

Do not mix GC diagnosis into the deterministic policy sweep. Its intentional consumer roots must make collected-entry encounters zero. A separate bounded untimed diagnostic can use one collision-free live key, a fresh uncached source, and either an intentional retained consumer or only weak observers. Observe collection before a same-key revisit and then check the exact additional source/decode count. Use an independent collection control and `Reference.reachabilityFence` for deliberate roots; do not use sleeps, strong witness maps, or explicit GC inside a latency measurement.

For the actual ValueStore, use the already established source-cache boundary as a separate axis: decoded IRI representatives at 1,048,576 characters can remain source-cache-owned, while 1,048,577-character representatives were excluded in the previous diagnostic. Keep cache settings fixed. This is an IRI/source observation, not a universal statement about all RDF value graphs. First establish which layer owns the value, then add natural allocation between reuses in a separate qualified query shape that does not itself retain every consumed row.

## Oracles and interpretation

The deterministic launcher should check every returned ID and lexical value against the requested sequence and emit a stable checksum for cross-arm comparison. Its fresh source creates a cold native wrapper on each miss and counts actual initializations separately from source calls. Ordinary should call that source for every request; memo arms must satisfy `sourceCalls = totalRequests - hits` even after the memo disables. New owners begin with no entries or history, and every close ends with query/global ledger usage zero.

Keep total requests separate from memo probes: probes stop on disable and ordinary has no memo probes. Record the first repeated request, first actual hit, tail-only hit rate, disabled state, cutoff count, capacity transitions, collisions, invalidations, unsupported values, encountered collected entries, reservation refusals, weak entry allocations, metadata peak, and cleanup endpoints. Preserve existing primitive close records. Source calls do not in general equal decodes; they do only in the deliberately uncached, fully consumed diagnostic source.

The current `collectedMisses` counter is checked before key equality. It can count a cleared entry for a different requested key. It therefore cannot by itself identify useful-key loss. Similarly, occupied weak slots at close are metadata occupancy, not a count of live Values. A single-key collision-free diagnostic can remove that ambiguity without adding a new cache algorithm or assuming a whole-heap measurement belongs to the memo.

A successful always arm would justify investigating reconsideration. A faster initial arm on unique traffic would quantify why permanent admission is not a production recommendation. A warm/cold working-set difference would identify policy-history effects. None of these conclusions follows from allocation reduction or hit rate alone; full-query latency, allocation, preparation, route, result, and cleanup evidence still need the matched prospective protocol.
