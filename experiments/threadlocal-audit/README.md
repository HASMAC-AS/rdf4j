# Query ThreadLocal audit and first propagation experiment

Start with [INVENTORY.md](INVENTORY.md) for actual regular, native and Janino paths, static access frequencies, purposes and ownership. It inventories 86 repository declarations and separately verifies indirect LWJGL stack access. It is not an exhaustive dynamic third-party trace and does not establish a 5–10% overhead.

The first candidate only changes type-matrix co-scan/flush-run cancellation polling. It passes captured query/probe references through the loop while keeping the existing 1024-tick check cadence. Workers keep ambient scopes for indirect callbacks; missing dispatch references retain boundary fallback semantics. This does not remove unrelated read-view, lifetime, blank-node, expression cache or planning context.

## Validation

- Original source is `ba74f4782e95e386653a6b1c7f6b0969e09b8f12`, not upstream main.
- The original serial co-scan test could pass without hitting co-scan. Tightened route-proving tests passed against original TypeMatrix bytecode: 4 tests, zero failures.
- Initial candidate: 11 focused tests and 180 selected LMDB safety tests passed. The final worker-null-fallback refinement passed **181 tests, zero failures/errors/skips** in `safety-suite-fallback.log`; see `NATIVE-CHECKPOINT-PLAN.md` for exact commands and baseline evidence.
- Root quick bootstrap reached LMDB, not a complete repository build. Test counts are selected module coverage, never a full repository pass.

## Performance scope

**No performance benefit was demonstrated.** The steady-state co-scan candidate mean was 9.40 ms versus 8.61 ms baseline, with inconsistent fork effects. Do not interpret it as a performance-neutral or faster change. See [RESULTS.md](RESULTS.md) for all retained samples and scope. `ThreadLocalQueryBenchmark` compares regular, interpreted and Janino repository-query controls on a narrow, heap-only/no-spill fixture. `ThreadLocalCoScanBenchmark` directly exercises the changed aggregate operator and verifies it against generic query results. These are different measurements; a compiled-IR control does not prove the co-scan optimization ran. Neither is Workbench HTTP/browser latency.

`BENCHMARK-PLAN.md` records the intended matched protocol. `run-matched.sh` requires explicitly frozen A/B classpaths and a shared benchmark lock. The recovered executor has fresh JDK25/Maven and no historical binary baseline. Never compare its new times directly to yesterday's results. `baseline-type-matrix` is the valid original-class overlay; the incomplete `baseline-classes` directory is explicitly invalid.

## Next direction

Separate JFR profiles point to per-pull context binding/restoration and read/value scopes rather than masked co-scan polling. These sample shares are not recoverable wall-time percentages. [NEXT-CANDIDATES.md](NEXT-CANDIDATES.md) lists safe boundaries and tests. [RECORD-CANCELLATION-DESIGN.md](RECORD-CANCELLATION-DESIGN.md) explains a direct cooperative record-iterator flag with explicit parent/probe references, forwarding and per-logical-open ownership. That design is not yet implemented.

## Follow-up: per-pull scope and writer guard

The four-arm follow-up is complete: baseline, SOURCE_PULL-only, no-writer-guard-only and both, across regular/interpreted/Janino routes. Guard-only and combined each passed the same 330 selected safety tests. All 48 timing forks and 12 separate profiles passed route/result/resource checks. Guard-only Janino is promising (4.65% lower pooled mean), but its small-sample interval includes no improvement; no established gain or synergy. The combined arm was not best on regular or Janino. See [the complete four-arm report](source-pull/FOUR-ARM-RESULTS.md). No main, Workbench or whole-repository validation claim is made.

## Completed owned-record cancellation experiment

The default-off candidate is checkpointed at `7ac1650`. Both ON/OFF selected safety suites passed 213 tests. All 60 matched timing forks and 15 separate profiles are complete: the qualified owned fast path was slower, not a performance recommendation. Warm-state trace checks prove it ran. See [full evidence](record-cancellation/RESULTS.md), [implementation safety](record-cancellation/IMPLEMENTATION.md), and [next scope design](OBSERVED-READ-DESIGN.md). No main/Workbench/concurrent-QPS or whole-repository claim.
