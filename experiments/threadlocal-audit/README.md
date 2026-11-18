# Query ThreadLocal audit and first propagation experiment

Start with [INVENTORY.md](INVENTORY.md) for actual regular, native and Janino paths, static access frequencies, purposes and ownership. It inventories 86 repository declarations and separately verifies indirect LWJGL stack access. It is not an exhaustive dynamic third-party trace and does not establish a 5–10% overhead.

The first candidate only changes type-matrix co-scan/flush-run cancellation polling. It passes captured query/probe references through the loop while keeping the existing 1024-tick check cadence. Workers keep ambient scopes for indirect callbacks; missing dispatch references retain boundary fallback semantics. This does not remove unrelated read-view, lifetime, blank-node, expression cache or planning context.

## Validation

- Original source is `ba74f4782e95e386653a6b1c7f6b0969e09b8f12`, not upstream main.
- The original serial co-scan test could pass without hitting co-scan. Tightened route-proving tests passed against original TypeMatrix bytecode: 4 tests, zero failures.
- Initial candidate: 11 focused tests and 180 selected LMDB safety tests passed. The final worker-null-fallback refinement passed **181 tests, zero failures/errors/skips** in `safety-suite-fallback.log`; see `NATIVE-CHECKPOINT-PLAN.md` for exact commands and baseline evidence.
- Root quick bootstrap reached LMDB, not a complete repository build. Test counts are selected module coverage, never a full repository pass.

## Performance scope

No measured gain is claimed yet. `ThreadLocalQueryBenchmark` compares regular, interpreted and Janino repository-query controls on a narrow, heap-only/no-spill fixture. `ThreadLocalCoScanBenchmark` directly exercises the changed aggregate operator and verifies it against generic query results. These are different measurements; a compiled-IR control does not prove the co-scan optimization ran. Neither is Workbench HTTP/browser latency.

`BENCHMARK-PLAN.md` records the intended matched protocol. `run-matched.sh` requires explicitly frozen A/B classpaths and a shared benchmark lock. The recovered executor has fresh JDK25/Maven and no historical binary baseline. Never compare its new times directly to yesterday's results. `baseline-type-matrix` is the valid original-class overlay; the incomplete `baseline-classes` directory is explicitly invalid.
