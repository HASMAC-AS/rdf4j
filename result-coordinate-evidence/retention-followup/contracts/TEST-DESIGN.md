# Direct memo retention experiment: prospective contracts

This design follows checkpoint `4e9af30d660dcc59b2e7148bb7cff00580441f29`. It proposes a controlled comparison; it does not report new latency or retained-heap results. The implementation and prior evidence under `direct-memo-followup` remain frozen.

## Question and source findings

The next useful question is whether weak retention preserves enough reuse to reduce the strong memo's unaccounted ownership of initialized payloads. Keep the direct-map key, hash, source/exposure fences, admission thresholds, capacity limits, and ownership handoff unchanged. Use `rdf4j.lmdb.nativeValueMemo.retention=weak` for the experimental treatment and retain the current strong behavior as its control. The existing enabled property remains off by default.

Source inspection establishes two separate retention paths in the current implementation:

* `NativeDirectValueMemo.values` retains each `Value` strongly.
* `NativeDirectValueMemo.identities` retains an `InternalIdentity`, its revision, and that revision's ownership graph. `ValueStoreRevision.Lazy` contains strong references to the store, association, retained epoch, resolution scope, and ID epoch. `Snapshot` contains a store, read snapshot, and tracked-value state. Weakening only the first path would leave the second intact.

The source also has its own completed-value and canonical caches. `ValueStore.cacheValue` rejects shared caching of lexical payloads longer than 1,048,576 characters. Consequently, real-query heap measurements cannot attribute every surviving value to the memo. Keep source-cache configuration identical across arms and first establish reachability with a fixture that creates fresh native wrappers and never retains them.

The existing contract fixture's `responses` map is a strong root and must not be reused unchanged for collection assertions. The memo belongs to the outward `NativeResultMaterializer`, which is the current iteration owner; it does not move into `AggContext` for this experiment.

## Minimum implementation contract

A weak slot should weakly reference the `Value` itself. A suitable slot object extends `WeakReference<Value>` and contains only a primitive captured revision number and a nullable weak store reference. The existing ID and semantic version arrays supply the other witness fields. The weak path must not retain an `InternalIdentity` or a `ValueStoreRevision`.

Do not weakly reference an entry bundle containing the value and witness: a consumer can retain the value while the otherwise unowned bundle disappears, causing unnecessary misses. The table may strongly retain the reference object and its primitive/weak metadata.

A lookup obtains the value once into a strong local, then checks semantic version, ID, revision number, and owner identity before returning it. A null owner at admission is distinct from a non-null owner whose weak reference later clears. For a captured non-null owner, require the current owner to be non-null and identical to the weak referent. Otherwise a value reassigned to a null-owner revision could become a false hit after collection.

An equivalent identity replacement remains a hit: lazy initialization may replace the revision object while preserving dictionary ID, revision number, and store identity. Collection of the old identity/revision must not invalidate the live value.

A cleared value reference is a collected miss. It is not a semantic invalidation, and a dead conflicting slot need not count as a live-key collision. Opportunistic cleanup at lookup, resize, clear, and close is sufficient; a reference queue is unnecessary for this bounded direct map. Rehash live reference objects without introducing a new strong value root. Preserve cumulative policy history.

Charge weak-reference and witness metadata as well as arrays. The prospective reservation adds a conservative `112 * capacity + 160` bytes for weak metadata to the existing `36 * capacity + 80` array estimate: `148 * capacity + 240` bytes in total, or 606,448 bytes at 4,096 slots. Reserve this amount before admission. This is a declared metadata estimate, not a measured complete heap bound. Weak-reference allocation and bookkeeping are part of this treatment's cost. Preserve the original strong representation where practical; changing its witness representation would require a separate bridge comparison.

## Test first

The new `src/.../NativeDirectValueMemoRetentionContractTest.java` uses the existing constructor and methods. The first intended red method requests weak retention and requires a dropped value, captured identity, revision, and store to become collectible while the memo remains open. It uses helper frames returning only weak observers, an unadmitted control graph, bounded GC requests, and no sleeps. This is a reachability diagnostic outside timing.

The focused selection now has eight test methods. In addition to the first weak-reachability test, it covers:

1. The strong control retains the same graph until close, then releases it and its ledger reservation.
2. A consumer-retained value remains a same-object hit after GC; `Reference.reachabilityFence` keeps the declared consumer root live.
3. After initialization and equivalent identity replacement, the obsolete identity/revision can collect while the consumer's live value remains a hit.
4. An owner changed from non-null to null remains a miss after the old owner collects.
5. ID, revision-number, owner, and semantic-version mutations still reject a representative.
6. Initial weak admission charges the complete declared array-plus-metadata estimate of 606,448 bytes.
7. A 500,000-byte ledger admits the strong control's arrays but refuses the requested weak table; the weak mode falls back to fresh ordinary values with zero remaining reservation.

Before timing, additionally run the existing exposure-handoff, source-view/exposure-change, detached-row, full-mask, collision, reservation-refusal, clear/close, and adaptation contracts against appropriate weak-mode selections. Policy tests must hold their trace values strongly so accidental GC does not change the intended trace. Existing strong-retention assertions are controls, not weak-mode requirements.

Do not edit implementation before preserving the failing Surefire result. This document and new test source alone do not establish a passing result.

## Bounded memory diagnostic

First run a memo-only fixture with real native wrappers and no source cache. Use a fixed small working set, distinct payload contents per ID, and two payload sizes: one small and one above the source shared-cache threshold. Size the number of large values to fit the fixed heap comfortably. Avoid reading lexical getters at a cold checkpoint.

Record these states in a declared order: cold admission with consumer holders; initialization with the same holders; release all holders while the memo stays open; retain a fixed subset of rows while releasing the rest; close the iteration while retained rows remain; finally release those rows. Use independent fixtures where a previous state cannot be reversed. Scope clearing is a separate checkpoint because it should release entries without restarting adaptive history.

At each diagnostic checkpoint record weak-observer liveness, initialized-value count, payload characters, table/metadata reservation, ledger peak/end, source calls, hits, collected misses, capacity, and policy transitions. Use `Reference.refersTo(null)` to observe collection without retrieving a strong referent. Bound GC requests prospectively, require a collection control, and report failure/inconclusive collection rather than repeating until the desired result appears.

Payload characters, allocation bytes, ledger reservations, and live heap are different measurements. Post-GC process heap is a whole-process measurement. Attribute memo-exclusive retained bytes only with a heap graph/dominator analysis that distinguishes table roots, source caches, retained rows, and shared owners; do not sum their overlapping reachable graphs. An initialized lazy value can grow without changing its semantic version or the array reservation.

Then repeat a small real-Sail diagnostic with identical source-cache settings, full consumption, early close, and retained detached rows. Keep all source/exposure ownership handoffs intact and verify retained rows remain readable after close. Treat source-owned retention as a measured confound, not as memo retention.

## Timing and acceptance

Only after contracts pass, compare ordinary reuse, current strong direct memo, and weak direct memo with coordinate transport disabled in all direct comparisons. Use normal AUTO selection, matched input/JVM/heap/ledger settings, fresh forks, and balanced alternating order. If the old runtime cannot be recovered exactly, establish new controls on the recovered runtime and do not pool old-host latency numbers.

Start with the two existing fixed workloads. Record preparation and consumption latency, allocation, ledger peaks/end, actual routes, result checksums, source calls, capacity/policy transitions, and weak misses. No explicit GC or reachability polling belongs in timed intervals. A latency gain with lower allocation does not establish lower retained heap. A weak memo that stays effective only because the source cache or retained rows own its values should be described accordingly.

The delayed-repetition/admission-cutoff question remains a separate experiment. Do not change the 8,192-probe cutoff while comparing retention policies.

## Primary API references

* Java 25 [`WeakReference`](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/lang/ref/WeakReference.html): weak reachability and collection semantics.
* Java 25 [`Reference`](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/lang/ref/Reference.html): `get`, `refersTo`, and `reachabilityFence` semantics.

