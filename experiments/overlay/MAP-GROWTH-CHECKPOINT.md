# Real map-growth lifecycle proof and compiled replay gap

The diagnostic `LmdbMapGrowthReplayLifecycleTest#initiallyCompiledMemoIsRecreatedAcrossUnobservedMapGrowth` passes with the test probe enabled. It uses a genuine memo hit in eager ORDER BY/top-K preparation, before any result row is exposed, then triggers real map growth through the existing writer path.

Verified: zero rows observed before growth; exactly one accepted replay; two distinct memo instances with hits; exactly 200 expected rows without duplicates/loss; initial compiled execution.

Observed routes: `ROW:irKernel`, then `ROW:batch`; compiled binds: 1. Thus this is **not** proof of compiled kernels on both attempts. The optional strict property `rdf4j.lmdb.nativeValueMemo.requireCompiledReplay=true` reproduces that failing gate. No production replay/forcing behavior was changed to conceal it.

`run-memo-growth-proof.sh` reproduces the passing lifecycle portion. Its eager-projection setup is deliberate and must not be called default-lazy-projection replay coverage. The hook is diagnostic-only and disabled by default.
