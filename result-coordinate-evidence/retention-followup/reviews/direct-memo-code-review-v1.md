# Independent weak direct-memo review

Reviewed 2026-11-17 by the benchmark/runtime recovery agent. This review used source reads, source hashes, and comparisons only; it launched no Java, compiler, tests, or benchmarks and changed no implementation. **No blocking correctness issue was found in the reviewed source.** Passing runtime contracts, tier coverage, and diagnostic qualification remain separate evidence owned by the root agent.

## Reviewed identity and provenance

The paths below are relative to `result-coordinate-evidence/retention-followup`. Java package suffixes are `org/eclipse/rdf4j/sail/lmdb/evaluation/`.

| Source | SHA-256 |
| --- | --- |
| `implementation-src/.../NativeResultMaterializer.java` | `a1e569055db8d9a5af42fa2d375811e5de280112fe0b0d7b46c551f1d662bfa6` |
| `implementation-src/.../NativeProjectedBindingSet.java` | `089f854e63e3accf0871c072c6fefff47e7f5de298b2f87ccc9551adf41306e8` |
| `implementation-src/.../NativeDirectValueMemo.java` | `e3ac88bf33accfe70424ee0ba2c3e6a0bc2b0e222720e8b36e49cd1a4b5c862e` |
| `benchmark/src/.../NativeDirectValueMemoStatistics.java` | `13550b7a78d7a0d7775ccc989b8a1fffa37c155cc08016c28ecf6bb61fb79e01` |
| `benchmark/src/.../DirectMemoLauncher.java` | `44dc0f81bdeaf2c4c8a9ea4f826d6ba9c0448bae6a2675cea6cc25b1baa84c78` |

The materializer and projected binding-set files are byte-identical to `direct-memo-followup/runtimes/identity-witness-v2/generated/src`. Source comparisons also confirmed that the original strong `matches`, `decline`, `clear`, `adapt`, and `slot` methods remain unchanged. The five-source generation receipt binds the sealed test source SHA-256 `ee750e0aa460b49813011d1956458ac3379f84faae304ea03374595cc6912110`. The preserved `contracts/validation/red-all-v1/receipt.json` records eight tests, the exact five expected failures, zero errors, and zero skips. Its `passed: true` means the expected-red qualification passed; it does not mean those tests were green.

## Lookup and ownership reasoning

`find` retrieves a weak referent once into a local `Value`. That local is then validated and returned, so collection cannot invalidate an already returned non-null candidate between a second dereference and its use. A cleared reference is removed and counted as `collectedMisses` before key comparison. Thus an encountered dead conflicting slot does not inflate the live collision or semantic-invalidation counters.

The weak entry stores only a primitive revision number and a nullable `WeakReference<ValueStore>`, in addition to the JDK weak referent. It does not store `InternalIdentity` or `ValueStoreRevision`. Weak admissions explicitly leave the identity array slot null. Equivalent replacement of an identity/revision object therefore remains compatible with a hit when semantic version, native ID, revision number, and store identity remain equal.

The null-owner branch is correct: an owner absent at admission is represented by a null witness; a non-null owner whose witness clears remains a non-null witness object. The latter requires a non-null current owner and `refersTo(currentOwner)`. It cannot become a false hit when the Value is reassigned to a null-owner revision. Owner-witness reuse also uses reference identity, not `equals`.

The existing materializer still certifies source, result view, and exposure before calling the memo and retains its monitor confinement. Scope clearing and permanent disable both drop the cached latest-owner witness. Clear drops every slot and identity reference without resetting adaptive counters. Close remains idempotent and releases the reservation before exporting scalar statistics. These entry-level observations do not assert that a real query has no independent source-cache, retained-row, or ledger-associated roots.

## Growth, fallback, and accounting

The full key/hash, initial and maximum capacities, 8,192-probe decision boundary, cumulative hit/collision rules, and permanent fallback behavior are unchanged. Rehash copies wrapper objects without calling `get`; it introduces no additional strong Value root. Fixed-hash doubling maps previously distinct occupied buckets to distinct new buckets, so the existing zero-rehash-discard invariant remains applicable.

The implementation deliberately carries cleared wrappers through rehash. This is bounded metadata, not a correctness fault: the next lookup of that bucket removes its cleared referent, and clear/disable/close remove all wrappers. Consequently, close occupancy is a count of occupied metadata slots, not a count of live Values. The prospective test-design phrase about rehashing live references should not be read as evidence that this implementation scans or discards dead references during growth.

Weak admission reserves `80 + 36N + 112N + 160` bytes before allocating its arrays, giving 606,448 bytes at 4,096 slots. The declared conservative reference allowance comprises one 64-byte entry and one 48-byte owner reference per slot, plus an independent latest-owner witness and a transient replacement pair. Growth first reserves the entire new allowance while retaining the old claim; its peak therefore accounts for the old/new overlap. Refusal disables the table and releases the old claim. Successful growth releases the old claim after publishing the new arrays. Allocation failure closes the new claim, disables the table, and propagates the failure.

This is a declared array/reference estimate under the pinned HotSpot layout assumptions. It is not a complete heap measurement or a bound on source-owned and consumer-owned payloads, process-global diagnostics, ledger implementation overhead, or all surrounding owner objects. `peakArrayBytes` remains the array-only estimate; `peakReservationBytes` includes the weak allowance. Both should be reported with their distinct meanings.

## Instrumentation and qualification implications

The recorder expands from 22 to 27 scalar words consistently across close, snapshot, and JSON output. It adds encountered collection misses, weak-entry allocations, owner-reference allocations, peak reservation, and the actual per-owner retention mode. Reporting still occurs after the delegated harness returns. The timing validator should require the expected `weakRetention` value for every direct owner, zero weak allocation counters in strong mode, balanced opened/closed owners, no observation overflow, and zero endpoint ledger/charge.

No algorithmic correction is requested by this review. Before timing, the planned tests should qualify weak-mode full-mask/collision, boundary-hit, growth/refusal, clear/close, source/exposure handoff, and retained-row behavior in addition to the eight new reachability contracts. Deterministic policy traces need externally retained Values so accidental collection does not change their prescribed hit stream. Native interpreted and compiled routes remain separate gates.

The new strong mode preserves strong ownership semantics and policy, but it now shares an `Object[]`, retention branch, extra fields, and counters with weak mode. It is therefore a fresh matched strong control, not a byte-identical reproduction of historical strong timing. Preserve the new runtime identity and do not pool historical measurements.
