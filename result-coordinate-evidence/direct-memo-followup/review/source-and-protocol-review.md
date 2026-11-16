# Direct memo source and protocol review

## Outcome and scope

No static blocker was found for the proposed fixed-fixture experiment. The table implements the specified adaptive direct map, the integration preserves the reviewed source and exposure boundaries, and the protocol measures complete strategy cost while distinguishing ledger charges from retained heap. Executed correctness qualification and the later audit of raw benchmark records remain separate requirements.

This review concerns serial query execution, with memo access confined by the owning materializer's monitor. It does not establish concurrent or reentrant use of the memo. Revision matching assumes the ordinary immutable revision-number and owner metadata supplied by the built-in `ValueStoreRevision` implementations. The contracts also use immutable test revision metadata. Arbitrary custom revision objects whose getters change in place are outside this qualification: retaining an `InternalIdentity` does not independently snapshot mutable fields inside its referenced revision.

The reviewing agent, `/root/recover_checkout`, authored the table and performed the generator and protocol review independently of their authors. `/root/miss_accounting` separately inspected the table implementation and reported no static blocker within this scope. No Java, compiler, formatter, or benchmark was launched for this review. Source transformations were replayed in memory; no production files were changed.

## Exact source identity

The accompanying `review-receipt.json` records the files inspected, their SHA-256 hashes, the generated source identities, and hashes for the five preserved source components.

| Item | SHA-256 |
|---|---|
| Table implementation | `05e6013c60d23415d75f5612dfa16e179ceb35784caf651d4bef1befb0215654` |
| Originally reviewed generator, preserved as `contracts/history/generator-v1.py` | `923ff7240fef5dade423aab68f02bce34f50373df6df3242f66e228c8c25d31d` |
| Current generator, reviewed after that checkpoint | `962ae6eae32a2419c0cd852dc8d366bd20e52785bb337e48fb91d3b20ab954cd` |
| Generated `NativeResultMaterializer.java` | `a1e569055db8d9a5af42fa2d375811e5de280112fe0b0d7b46c551f1d662bfa6` |
| Generated `NativeProjectedBindingSet.java` | `089f854e63e3accf0871c072c6fefff47e7f5de298b2f87ccc9551adf41306e8` |

The current generator differs from its archived predecessor only by wrapping a long `current.remember(...)` call and changing the default output directory from `direct-memo-v1` to `direct-memo-v2`. The wrapped call has identical arguments and behavior. Replaying the current transformations in memory produced the exact source copies in both `contracts/generated/direct-memo-v2` and `runtimes/identity-witness-v2/generated/src`.

These source checks do not themselves establish compiled-artifact identity. The compiler/freezer separately verifies the emitted and selected class files. The preserved source checks cover all five components whose existing classes continue to come from the common runtime:

1. The complete import and `NativeResultMaterializerPlan` prefix.
2. `NativeResultMaterializer.FactorMemo`.
3. `NativeResultMaterializer.FactorPage`.
4. `NativeProjectedBindingSet.ProjectionLayout`.
5. The anonymous binding iterator in `NativeProjectedBindingSet.iterator()`.

All five compared equal byte for byte. The table adds no nested class, and the integration adds no anonymous class. This is compatible with the prescribed six-class experimental prefix; actual class inventory remains the freezer's responsibility.

## Table implementation

The table contains exactly five arrays: IDs, complete position masks, semantic versions, strong `Value` references, and captured `InternalIdentity` references. Its full-key hash is byte-identical to the sealed stub's hash function. Values are admitted only when they supply a nonnegative semantic version, a matching non-unknown ID, and a non-null revision. Unsupported or stale versions are rejected before additional identity access. No lexical getter or initialization operation is added to the memo.

A same-key hit validates the semantic version, ID, revision number, and exact revision-owner identity. It does not require the identity record or revision object to remain the same object. Consequently, equivalent identity replacement during cold-to-initialized transition remains eligible, while an ID or revision change without a semantic-version bump is rejected. The fifth array is a deliberate strengthening of the original four-array proposal and can itself retain a previous revision capability until eviction, clear, disabling, or close.

The first actual bucket lookup allocates 4,096 entries. Initial reservation refusal records one refusal and zero bucket probes because no bucket was examined. `remember` neither allocates a table nor increments probes. Collisions are occupied buckets with a different complete key, counted at lookup. A declined unsupported admission preserves an unrelated valid bucket; an existing same-key entry is cleared.

Probe, hit, and collision totals remain cumulative. Only the first 8,192-probe boundary can disable for low hit rate. Growth begins at 16,384 and occurs at later 8,192-probe boundaries when both cumulative thresholds hold, with at most one doubling per boundary and a maximum capacity of 65,536. A lookup that already captured a valid value can return that value even if growth at the same boundary is refused and disables future reuse.

Each allocation reserves `80 + 36 × capacity` bytes before allocating its arrays. A replacement reserves its full charge while the old reservation remains live. Reservation refusal drops the old table and disables the memo; allocation exceptions release the new claim and the old table before propagating. `clearEntries()` drops value and identity references while preserving capacity, reservation, and adaptive history. `disable()` releases all arrays without closing the statistics owner. Final `close()` captures occupancy and pre-close disabled state, performs cleanup, and exports statistics once. Closing the ledger first is compatible with the reservation's idempotent close operation.

The independent table review checked the argument order against the statistics helper, the first and later adaptation boundaries, the current-hit behavior during failed growth, and the old/new reservation lifecycle. The source contains no extra weak-reference operations or strong value/source references outside the intended table entries and their identity witnesses.

## Integration and ownership

Direct mode bypasses the ordinary last-value cache and factor cache. It does not place the direct map behind a successful ordinary lookup. Native coordinate work at the materializer is bypassed in direct mode, and the timed direct arm explicitly disables coordinate transport.

The materializer certifies source identity, a non-null result-view token, and exposure scope before lookup. An uncertified result view permanently disables the experimental table. A changed certified scope clears entries without restarting adaptive history. Source mismatches decline reuse. Existing final view checks and exception rollback abandon provisional admissions; aliases of provisional hits are cleared before repair.

A direct miss is published immediately after source resolution, before a later projected column looks up its key. This preserves the requested replacement order. The deferred publication pass remains only for the ordinary implementation. Wide projections acquire separately charged hit-mask scratch, and materializer closure releases that scratch. The memo closes before shared cleanup so final occupancy and scope-clear totals retain their intended meaning. Detachment removes the row's materializer reference and coordinate sequence in addition to its source reference.

Individual and collection access lack an explicit exposure token. For an internal native wrapper with a non-null store owner, the integration deliberately re-enters ordinary `lazyValue` to preserve the source's ownership handoff. It can reuse an externally owned representative or an owner-null test/synthetic representative. The added standalone ownership contract first establishes the normal handoff—two source calls and a distinct externally owned value—then tests reuse on the third access. The inert red stub fails at the feature-marked reuse assertion rather than hiding a failure of the ownership prerequisite.

**A standalone memo hit is a validated table candidate, not necessarily an avoided resolver call.** A candidate can still require the source handoff described above or a scope repair. Raw resolver counts must remain visible; hit counts must not be converted algebraically into saved CPU or allocation.

## Benchmark design and consumer review

The estimand is total query cost under natural garbage collection for the complete reuse strategies. Direct hashing, stronger retention, adaptive decisions, coordinate transport differences, resolver work, and observation overhead change together. The comparison cannot identify an isolated cost for any one mechanism.

The design uses 36 fresh JVMs: six rounds, two fixtures, three arms, with every permutation of arm order used once. Fixture order alternates by round. Each JVM contributes the median of all five measured queries after eight retained warmups. Ratios pair the same fixture and round; fixtures and historical studies are not pooled. The six paired ratios, their median and full range, and absolute observations are reported. The fixed engineering screen is explicitly not a significance test or a production-promotion rule.

Each direct JVM must export exactly thirteen owners aligned with semantic-compilation ordinals 1 through 13. Owner resolver intervals must equal the original per-query counters and have no inter-owner gap. Controls create no direct owner. The consumer checks root compilation reconciliation, normal AUTO selection, compiled measured execution, result and checksum validity, shared-tail exact-bag validation, adjacency observations, ledger endpoints, and zero unexpected mutation or admission refusal. Per-query route/tier and complete adjacency observations match both comparator pairs, including warmups. Resolver equality is deliberately not required.

The separate six-JVM lifecycle preflight uses the same full-size fixtures and settings and validates 78 queries. Its raw timing fields are retained but not summarized, selected, or pooled into the main comparison. This establishes lifecycle and output-format compatibility without treating preflight timing as comparison evidence.

Two concrete consumer gaps identified during review have been corrected in the current protocol and both raw consumers:

- For `r > 0` successful resizes, at least `8,192 × (r + 1)` probes are necessary. Aggregate statistics must reject growth before the earliest possible boundary.
- `rehashDiscards` must be zero. With a fixed hash and exact power-of-two doubling, an entry moves to its old slot or that slot plus the old capacity. Distinct occupied old slots therefore cannot collide during rehash. The counter records a defensive impossible condition, not a legitimate adaptive outcome for this algorithm.

Scope-clear totals are reported without a zero gate. Low-hit disabling is a valid result. Aggregate totals provide necessary consistency checks, not a reconstruction of every adaptive decision. The observer does not separately count dense factor-page refusal; the protocol states that limitation instead of claiming complete admission telemetry.

## Ledger and retained-memory limits

The array charge assumes a 16-byte header and eight bytes per reference. It is a declared accounting model, not a measurement of exact object layout. The initial charge is 147,536 bytes, the final table charge is 2,359,376 bytes, and the largest old-plus-new growth reservation is 3,539,104 bytes.

These charges exclude the graph retained through values and identity witnesses: lexical data, nested values, revision and owner objects, and payload initialized after admission. Initialization can preserve the semantic version while increasing retained payload. A smaller allocation total does not establish a smaller live heap, and a smaller query-ledger peak does not establish bounded retained memory. Passing the timing screen cannot justify production promotion under a strict query-memory bound.

## Retained-memory follow-up

Run a separate diagnostic cohort without changing the current memo policy. Freeze short traces and checkpoints before observing results: after cold admission, after partial and full initialization, after scope clear, after early close, and after final close. Repeat with no retained rows and with a fixed retained-row set. Control distinct full keys, payload length, repetition phase, and retained-row count independently. Use built-in cold wrappers for mechanism checks and real-store confirmation for revision and ownership graphs.

Report three different quantities:

| Quantity | Interpretation |
|---|---|
| Declared table-array charge | What the prototype reserves in the query ledger |
| Identity-deduplicated reachable graph | Objects reachable through memo values and captured identities |
| Memo-exclusive retained graph | Objects whose continued reachability depends on those memo edges |

The last quantity requires considering other roots, including caller-retained rows, source state, and shared store caches. Conceptually remove the memo's value and identity edges while preserving the other roots, then identify objects that become unreachable. Summing sizes independently for every entry would double-count shared strings, datatypes, wrappers, and revision owners. Examine value-array and identity-array edges both jointly and separately; their contributions need not add because their reachable graphs can overlap.

Keep observer references weak, and avoid retaining a value in a probe local across a snapshot. Explicit collection and heap inspection belong only in this diagnostic cohort, never in the natural-GC timing sequence. Record the observation method so post-collection retention is not mistaken for ordinary-GC behavior.

Cleanup succeeds when no graph remains reachable exclusively through memo entries after clear or close, while caller-retained rows remain valid. Flat array accounting alongside growing initialized payload is an expected accounting-limit observation, not by itself a leak. A surviving memo-exclusive graph after clear or close would be a concrete retention defect.

A production bound needs one of three additional contracts: immutable bounded detached values; a conservative pre-decode size quote with reservation before retained growth; or initialization and mutation hooks that reserve or revoke before growth. Size checks only at admission or lookup cannot bound caller-driven growth between lookups. This follow-up leaves the current first-window cutoff and five-array experimental policy unchanged.

## Subsequent audit

This document is a source and protocol review snapshot. It accepts no timing estimate and certifies no benchmark outcome. The later raw-record audit must use the frozen protocol hash, verify every planned JVM and query, independently recompute the paired estimates, and preserve failed or interrupted attempts rather than replacing them.
