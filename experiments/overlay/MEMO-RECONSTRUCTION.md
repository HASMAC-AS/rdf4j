# Reconstructed memo and combination experiments

This is a new reconstruction, not recovered historical code. The previous memo worktree/archives could not be found in the available workspace or saved files. Historical benchmark numbers must not be compared as though they used this exact implementation.

## Memo baseline

Opt in with `rdf4j.lmdb.nativeValueMemo.enabled=true`. All combination comparisons keep this enabled. Default remains off in the WIP source.

- Per-AggContext weak direct map keyed by complete ID, complete position mask, and semantic mutation version.
- Lazily allocates4096slots; maximum65536.
- First8192probes disable below25%hits.
- Reconstructed growth policy: at8192-probe windows, double only with at least50%hits and75%occupiedslots. The exact historical growth threshold was unavailable.
- Charges arrays and weak-reference node allowance to memory account, never claims ownership/accounting of strongly retained decoded payloads.
- No eager decoding, unstable-ID sources bypass. Composite sources must prove every component stable.
- Clears before input/snapshot teardown; escaped projection rows release memo references upon detach.
- Corrects full-position-mask collision in AggContext's existing cache.

## Separate improvements

`rdf4j.lmdb.repository.partialReady=true`: filters already-prepared roots from mixed-row traversal using the existing readiness predicate. Initialized Lazy roots and unresolved descendants keep their owner protection. No read-ahead.

`rdf4j.lmdb.nativeDistinct.widePrimitive=true`: enables primitive tuple DISTINCT above4columns, with checked array arithmetic and migration preserving seen keys when primitive capacity would exceed the array limit.

Earlier typed-overlay/grouped-wave flags remain off in this matrix. Default global cache is256k entries.

## Validation and hit proof

Memo focused34tests passed, including15new memo tests. Replay/lifetime selection32tests passed. These are not yet proof of compiled-map-growth replay with memo hits on both sides. Partial-ready14tests passed flagon/off, with ready-root traversal3→1. Wide DISTINCT16tests passed flagon, including30columns,batch,nulls,clear and forced capacity fallback.

First actual query correctly aborted its strict zero-hit check: CompositeNativeLmdbQuerySource had not forwarded the new stable-ID capability. That wrapper integration is being corrected and tested before timing the combinations. Do not interpret the aborted run as memo performance evidence.

## First actual-query matrix

Composite forwarding fixed. Combined67tests pass; actual100k-row runs have2,760,000memo probes and2,731,750hits (~99%). CompiledIR and output checksum verified every run. Wide flag actually switches DISTINCT100kobject probes to100kprimitive probes.

Eight forks A/B/C/D/D/C/B/A,100krows,defaultcache256k,two warmups+six measured,finalfour means:
- Memo baseline:587.328/491.347ms,overall539.337ms.
- Memo+partial-ready:462.773/504.513ms,overall483.643ms.
- Memo+wide primitive:557.412/458.687ms,overall508.050ms.
- Memo+both:572.794/504.393ms,overall538.593ms.

This variance is too large for a firm speedup claim. Partial-ready reduces allocation; wide primitive increases allocated bytes due array growth, even though it may reduce retained table overhead. Combination is not additive in this first fixture. Longer/larger matched runs are required. Historical memo numbers remain separate.
