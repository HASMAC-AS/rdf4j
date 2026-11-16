# Compact factor-page layout: independent source audit

This is a read-only design audit of the preserved original materializer and the preceding page-occupancy evidence. No production source, runtime, test, benchmark, or earlier evidence was changed or run for this audit. The baseline identity is recorded in `baseline/identity.json`; the audited materializer is preserved in `baseline/preimages/`.

## What the original 16 KiB means

`NativeResultMaterializer.PAGE_BYTES` is a conservative reservation of 16,384 bytes. It is not an observed object size, retained-heap measurement, or charge for decoded `Value` payloads. Each original `FactorPage` immediately creates five 64-element arrays: three `long[]` arrays contain 1,536 payload bytes, and two reference arrays contain 512 payload bytes with four-byte references or 1,024 with eight-byte references. Array headers, alignment, the page, reservation and ledger-set metadata add overhead. The source comment also allows for per-cell weak-reference metadata.

`rememberFactor` copies the ordinary memo's weak value reference, weak revision-owner witness and three primitive witnesses into the page. It does not create another decoded Value or another weak-reference object. Some weak metadata can therefore be shared between caches. The accounting model is intentionally generous; ledger savings must be reported separately from measured allocation or heap savings.

The prior measured query admitted 16,364 pages: 16,112 with one nonnull stored reference and 252 with all 64 occupied. Those are stored weak-reference slots, not a claim that all referents remained live. Replacing those same singleton pages' 16 KiB charge with 512 bytes would save 255,729,664 charged bytes. Their five arrays contain 32,997,376 payload bytes with four-byte references, or 41,246,720 with eight-byte references. Neither arithmetic result is a measured JVM allocation reduction; the new inline fields, metadata and changed admission behavior also matter.

The fixture's uncapped geometry permits 16,384 singleton pages and 256 dense pages. At 512 and 16,384 charged bytes respectively, those pages would charge exactly 12 MiB, before directory, coordinate memo, ordinary memo and other query structures. This is a prospective model. The current baseline hits the allowance before admitting every page, so a compact candidate can change admitted-page count and cache-hit count as well as storage cost.

Evidence: `namespace-followup/shared-selection/page-occupancy/{report.md,analysis.json,page-records/owner-4.jsonl}`. The original query admitted 268,107,776 page bytes and released all of them; directory and memo charges reconciled separately. Left-side pages supplied no factor hits, but ordinary last-value reuse remained active. The earlier instrumented timings are not comparative performance evidence.

## Recommended first representation

Keep the existing `FactorPage` object and complete directory key stable. `FactorMemo.pages` and `recentFactorPage` retain that object, so replacing the page during promotion would require unnecessary pointer repair. The materializer already serializes these operations.

Store one inline offset, ID, semantic version, revision ID, weak value reference and weak revision-owner witness. Keep the five existing arrays null until a second distinct occupied offset needs storage. An empty inline marker must be independent of the ID, for example an offset of -1 or a null reference; valid IDs must not be repurposed as occupancy sentinels. Keep references weak exactly as before.

| State and incoming event | Required action |
|---|---|
| Empty inline page | Store all witnesses and the weak reference inline. |
| Occupied inline page, same offset and same tuple | Preserve the allocation-free repeated-publication path. |
| Occupied inline page, same offset and changed ID/reference/witness | Replace the complete tuple inline; do not promote. |
| Explicitly cleared inline slot, different offset | Reuse the inline slot; do not promote. |
| Occupied inline page, distinct offset | Attempt charged dense promotion. Preserve the original inline tuple if refused. |
| Dense page | Use the existing indexed arrays and complete witness checks. |
| Dead or invalid weak referent on lookup | Clear only the captured offset, preserving the original weak-miss accounting. |
| Namespace retirement or close | Release shell and any promotion claim exactly once; clear reference-bearing fields. |

Do not call `WeakReference.get()` during insertion to classify an occupied slot as free. The weak reference itself remains an occupied slot until the existing lookup invalidation clears it. In particular, `factorPageWeakMissUsesTheCapturedMember` expects an old, GC-cleared weak reference to survive insertion of another member and increment the weak-miss counter when that captured member is looked up. Promotion must carry that reference and all its witnesses, even if its referent is already dead. Do not add demotion, scanning or compaction to this first candidate.

## Charged, transactional promotion

A conservative 512-byte shell is plausible for the stable page, inline tuple, claim, ledger-set entry and weak metadata, including eight-byte references. It is a modeled budget rather than a measured object-layout assertion. Retaining the original 16,384-byte total dense charge leaves ample modeled space for the shell's extra fields and a second claim.

Reserve the remaining 15,872 bytes with `QueryLedger.tryReserveWithoutReclamation` before allocating any dense array. **Do not use `Reservation.tryGrow`: it calls `makeRoom`, which may reclaim another registered owner's memory.** A second independent non-reclaiming claim avoids a memory-manager API change.

Allocate all five arrays into local variables, populate the old inline member at its exact offset and the new member at its exact offset, and publish the completed representation only after all allocation and initialization succeeds. The old tuple must remain valid throughout construction. On allocation failure, close the new claim and propagate the failure. After publication, clear redundant inline reference fields. Keeping both claims until close charges the shell that still exists and avoids a release/reacquire gap.

`retainedBytes()` currently adds only `page.reservation.bytes()`. It must include the promotion claim. Namespace retirement, full clear and idempotent close must release both. If the ledger closes first, claims already report zero; diagnostics must distinguish declared structural charge from remaining charge observed immediately before page close.

## Refusal policy

A refused promotion must not set the materializer's global missing-page admission latch. Failure to admit roughly 15.5 KiB does not imply that a new 512-byte shell cannot fit. Preserve the old inline entry and ordinary resolver fallback.

For the first candidate, retry on a later distinct-position insertion. A permanently sticky per-page refusal cannot observe headroom released by an unrelated reservation. Query-ledger `usedBytes()` does not reveal global capacity released by another query, so a failure snapshot using only that count is incomplete. A failed non-reclaiming reservation creates no Reservation object, but still takes the manager monitor. Persistent-pressure attempt counts should be exposed by the separate diagnostic; if this cost is material, a retry/backoff policy can be designed from that evidence. This simple policy does not promise to bound the number of failed attempts over an arbitrarily long query.

## Existing implementation patterns that support this choice

- `LossyConcurrentCache` keeps cold cells compact and promotes only after a changed-reference update; a same-reference offer avoids publication. Its concurrency/padding machinery is unnecessary here, but its cheap common-state path is relevant.
- `LmdbNativeFactorizedRows.promoteRepeatedEnumResult` reserves before copying a durable payload and releases a failed publication's reservation. Its reserve/construct/publish discipline applies directly. Its separate second-encounter admission heuristic is not part of this candidate.
- `LmdbDirectNativeAdjacency` explicitly avoids eager partition-wide lookup construction for scalar probes and retains page-local promotion. That supports local, demanded allocation rather than a new global structure.
- `LmdbNodeDomainSynopsis.Layout` distinguishes modeled bytes from physical measurements and chooses dense versus sparse representations from cardinality/span costs.
- `NativeRuntimeValueTable` keeps payload locations stable while growing its index. The relevant lesson is stable identity; its concurrency and exceptional sparse map are not needed for a 64-offset factor page.

The measured distribution is almost entirely singleton or full pages. A bitmap plus rank/select, packed intermediate arrays, a tiny hash map, or a multi-step 1/4/16/64 capacity ladder would add states, lookups and growth copying without a measured intermediate-occupancy need. Inline-one followed by the original dense representation is the narrowest experiment with a clear memory hypothesis and recognizable dense hot path.

## Diagnostic implications

Record shell admissions/refusals, promotion attempts/successes/refusals, compact and dense populations, occupied stored-reference slots, and shell/payload grants/releases separately. Preserve directory-growth overlap and coordinate-memo accounting. Record per-target page keys and factor hits so greater admission can be distinguished from cheaper storage. Reconcile every admitted shell and successful payload claim through retirement/final close, alongside endpoint ledger zero. Weak-referent snapshots and diagnostic counters may change allocation/GC/timing, so this run must remain outside the timed comparison.

The ownership reviewer independently identified focused contracts for failed promotion preserving the old hit, unrelated compact admission, recovery after a competing reservation is released, same-offset witness replacement, complete tuple preservation, and mixed compact/dense retirement. The existing payload-refusal test hard-codes the old first-page retained charge of 17,248 bytes and needs a representation-independent pressure setup while keeping its no-reclamation assertion.
