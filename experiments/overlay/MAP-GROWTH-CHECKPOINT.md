# Real map-growth compiled replay proof

The diagnostic `LmdbMapGrowthReplayLifecycleTest#initiallyCompiledMemoIsRecreatedAcrossUnobservedMapGrowth` uses a genuine memo hit in eager ORDER BY/top-K preparation, before any result row is exposed, then triggers real map growth through the existing writer path.

## Captured query-context bug and fix

Before the fix, the first attempt selected `ROW:irKernel`, but replacement selected `ROW:batch`; compiled binds stayed at 1. This was not a compiled-binding cache or intentional replay fallback. `SailTupleQuery.evaluate` restores `SlowQueryContextHolder` before lazy consumption. `LmdbStoreConnection` retained the replay factory but not that context, so the fresh evaluation strategy read no forcing (or an unrelated caller's forcing) on reopen.

The factory now captures the immutable query context once, installs it only while opening an attempt, and restores the previous caller context in `finally`. No dispatch eligibility or replay safety rules changed. A null captured context also prevents an unrelated caller's settings from being inherited.

Strict red evidence: `memo-growth-forcing-red.log` and `memo-growth-forcing-context-red.log`: 1 test, 1 failure, routes `irKernel → batch`, compiled binds 1. The latter deliberately installs a different caller context (`batch`) during result consumption.

Strict green evidence: `memo-growth-forcing-green.log`: 1 test, no failures/errors, two distinct memo instances with genuine hits, routes `irKernel → irKernel`, compiled binds 2. It also checks caller-context identity after consumption. Production classes rebuilt under the shared lock at 2026-11-17T07:58:12Z.

Verified lifecycle: zero rows observed before growth; exactly one accepted replay; two distinct memo instances with hits; exactly 200 expected rows without duplicates/loss; compiled execution on both attempts.

The successful-replay assertion is now unconditional (the old optional strict gate is removed). Reproduce with:

```
bash experiments/overlay/run-memo-growth-proof.sh
```

The eager-projection setup is deliberate and must not be called default-lazy-projection replay coverage. The hook is diagnostic-only and disabled by default. The exceptional-reopen test uses the existing dispatch observer's safely cleaned-up failure path to check caller-context restoration on failure.
