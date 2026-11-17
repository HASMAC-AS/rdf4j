# Bounded wide DISTINCT checkpoint

The opt-in `rdf4j.lmdb.nativeDistinct.widePrimitive` route now admits wide primitive tuple storage against the existing shared query ledger. It retains the memo/partial-ready work unchanged. Historical unbudgeted benchmark timings remain separate and must not be presented as equally memory-constrained results.

## Scope and semantics

- Bounded wide (>4 columns) SELECT tuple DISTINCT: RowDistinctPlan, NativeRowsIteration global/partitioned residual, top-K DISTINCT, and sorted projection, with a real query ledger.
- Default-off behavior remains unchanged. Narrow keys and legacy/no-ledger constructors retain their historical routes. No-ledger constructors decline wide primitive optimization, rather than silently enabling new uncharged primitive storage. This is not a universal query-memory bound.
- AggState aggregate membership was deliberately left unchanged after review found legacy group owners without terminal resource hooks. Enabling spill there needs a separate ownership audit.
- Canonical ID tuples migrate exactly to existing disk-backed NativeExpressionDomain.Membership when primitive growth cannot be admitted or reaches the flat-array ceiling. Noncanonical authority uses existing bounded term interning / NativeBoundedDistinct.
- NULL_CONTEXT and UNKNOWN normalize consistently; batch selection, clear and semantic aliases are covered.
- Existing Membership supports at most 64 columns. A wider bounded opt-in request refuses on first add before publishing a key. It never falls back to uncharged storage. Fixed-workspace refusal likewise fails closed.

## Allocation and ownership

Lazy first-add admission covers fixed scratch/metadata and Membership spill workspace before retaining keys. Array growth reserves old-plus-replacement peak, then releases old reservations. Query and process-wide limits are shared through the existing ledger. Migration retains old keys until exact insertion into spill membership completes.

Cursor close and scoped try-with-resources release reservations and spill resources. Close errors preserve the primary exception using closeResource / suppression. Tracker initialization failures release partial claims. ADMITTED/MIGRATED route counters supplement existing spill file and manager peak/current-byte counters.

The preflight currently uses size+1 before discovering a duplicate. At a growth boundary this can conservatively migrate on a duplicate that needs no new capacity. This is correct but may spill earlier than necessary; probing only at impending-growth boundaries is a future optimization, not part of these measured classes.

## Verification

- Initial red log `wide-budget-red.log`: missing ledger-aware constructor. This is API-red evidence, not proof of runtime allocation behavior; the prior source audit independently identified uncharged arrays and HashMap migration.
- Initial three budget tests passed before production wiring. One stale counter test class failed until its updated source was recompiled; no production behavior failure was inferred from that stale binary.
- Wired focused validation: 23 tests, zero failures/errors/skips (`wide-budget-wired-tests.log`).
- Post-ownership/close-review production compile and focused validation: 23 tests, zero failures/errors/skips (`wide-budget-final-tests.log`), completed 2026-11-17 08:21:48 UTC.
- These cover exact 30-column low-budget spill and clear, impossible-budget refusal, two overlapping trackers on one query ledger, noncanonical aliases, batch/nulls, overflow seam, counter publish-once, DISTINCT/order/limit and parallel DISTINCT aggregate regression.
- Additional boundary suite: 8 tests, zero failures/errors/skips (`wide-budget-boundary-tests.log`), completed 2026-11-17 08:24:13 UTC. This reruns the 5 budget tests and adds independent query ledgers sharing the process cap, >64 refusal, and no-ledger decline. Total distinct focused tests across these runs: 26. Budget tests explicitly require the opt-in flag so normal default-off runs do not fail on disabled behavior.

Parent owns matched query benchmarks and broader sweeps; consult their logs for actual end-to-end timing, route admission, spill counts, peak bytes and final bytes. Do not infer large-query spill throughput from the focused unit tests.
