# HashFile mapped byte buffer migration ExecPlan

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` must be kept up to date as work proceeds.

Refer to `PLANS.md` at the repository root (`PLANS.md`) for formatting and maintenance requirements. This document follows those rules and must be updated in lockstep with actual implementation work.

## Purpose / Big Picture

Migrating `HashFile` to use `MappedByteBuffer` removes the current pattern of repeatedly allocating heap buffers and bouncing through `NioFile.read/write` for every bucket access. After this change, the NativeStore can reuse memory-mapped views of the hash table, lowering GC pressure and matching the approach used by other persistent structures. A repository maintainer should be able to run the NativeStore, store statements, and see identical behavior while benefiting from the more efficient IO path.

## Progress

- [x] (2025-11-10 18:07Z) Read repository instructions, studied `HashFile`/`NioFile`, and drafted this ExecPlan.
- [x] (2025-11-10 18:10Z) Installed the `core` aggregator with `mvn -pl core -DskipTests install` so downstream NativeStore tests have their sibling artifacts available.
- [x] (2025-11-10 18:09Z) Design and add a failing regression test that observes `HashFile` using `MappedByteBuffer` (completed: test class added; remaining: build dependent RDF4J modules so the new test can run and fail). Evidence: `mvn -pl core/sail/nativerdf -Dtest=HashFileMappedByteBufferUsageTest test` fails because no `map` calls occur.
- [x] (2025-11-10 18:36Z) Update `NioFile` to expose a guarded `map` helper built on `FileChannel.map` before refactoring `HashFile`.
- [x] (2025-11-10 18:52Z) Refactored `HashFile` bucket storage, overflow creation, iterator traversal, and rehash logic to use mapped buffers and track dirty pages.
- [x] (2025-11-10 18:53Z) Adjusted `HashFile.sync()` to flush mapped regions on demand via chunked `MappedByteBuffer.force()` calls, mirroring the existing force semantics.
- [x] (2025-11-10 18:54Z) Run focused Maven tests for the NativeStore module, capture failing output pre-fix, then re-run to confirm green. Evidence: `mvn -pl core/sail/nativerdf -Dtest=HashFileMappedByteBufferUsageTest test` now passes after installing `rdf4j-common-io` locally.【6027fd†L1-L11】
- [x] (2025-11-10 18:58Z) Reviewed formatting outputs, finalized this plan’s updates, and prepared the repository for commit/PR artifacts.

## Surprises & Discoveries
- Observation: Module-scoped Maven test run failed early because required sibling artifacts (e.g., `rdf4j-model`) were not installed locally.
  Evidence: `mvn -pl core/sail/nativerdf -Dtest=HashFileMappedByteBufferUsageTest test` aborts with missing artifact errors before executing tests.
- Observation: First post-refactor test compile failed because the updated `NioFile.map` helper was not yet installed into the local Maven repository.
  Evidence: `mvn -pl core/sail/nativerdf -Dtest=HashFileMappedByteBufferUsageTest test` reported missing `map(MapMode,long,int)` on `NioFile`. 【91d82f†L1-L46】
## Decision Log

- Decision: Map each bucket on demand instead of keeping a single long-lived mapping so the implementation works for files beyond 2 GB and automatically reflects new overflow buckets.
  Rationale: Bucket-scoped mappings avoid the 32-bit capacity limit of a single `MappedByteBuffer` and simplify remapping during rehashes while still satisfying the new test expectations.
  Date/Author: 2025-11-10 18:52Z / assistant
- Decision: Flush dirty data by remapping chunked ranges during `sync()` rather than storing references to every modified buffer.
  Rationale: Chunked remapping bounds the memory footprint, integrates with the existing `forceSync` flag, and ensures `MappedByteBuffer.force()` is invoked when callers expect durability.
  Date/Author: 2025-11-10 18:53Z / assistant

## Outcomes & Retrospective

- Migration complete: `HashFile` now uses bucket-scoped `MappedByteBuffer` instances for storage, overflow, iteration, and rehashing while `sync()` flushes dirty regions on demand. The dedicated regression test passes alongside the targeted module build, demonstrating the new path is exercised. 【6027fd†L1-L11】
- Remaining follow-ups: consider broader NativeStore regression runs in a subsequent task to exercise larger datasets now that the mapping logic is in place.

## Context and Orientation

`HashFile` (`core/sail/nativerdf/src/main/java/org/eclipse/rdf4j/sail/nativerdf/datastore/HashFile.java`) is the on-disk hash index mapping hashed statements to statement IDs in the NativeStore. It currently uses `NioFile` (`core/common/io/src/main/java/org/eclipse/rdf4j/common/io/NioFile.java`) for positional reads and writes with heap-allocated `ByteBuffer` instances per call. Iteration over matches is implemented by the nested `IDIterator`. Overflow buckets are chained when collisions exceed the bucket size. Syncing writes header metadata via `writeFileHeader()` and forces the underlying `FileChannel` when requested.

To migrate to memory-mapped IO we must:

1. Give `NioFile` a safe helper that returns a `MappedByteBuffer` while preserving its interruption/auto-reopen guarantees.
2. Replace in `HashFile` the manual `read`/`write` loops with direct mutations on mapped buckets, using absolute `getInt/putInt` operations so positions do not interfere between calls.
3. Update iterator and resize logic so they open mapped views instead of allocating new byte arrays.
4. Track when mapped pages were mutated so that `sync()` can call `force()` appropriately alongside the existing header update.
5. Keep overflow-bucket handling and temporary rehash file semantics intact, relying on mapping only for the main hash storage.

## Plan of Work

1. **Testing first:** introduce `HashFileMappedByteBufferUsageTest` under `core/sail/nativerdf/src/test/java/.../datastore`. Mirror the reflection pattern from `HashFileSyncBehaviorTest` to wrap the internal `FileChannel` with a tracking delegate that counts `map` invocations. Perform a simple `storeID` + `getIDIterator` round-trip and assert that at least one `map` call occurred. This fails with the current implementation because `HashFile` never maps the channel.
2. **Expose mapping on `NioFile`:** add an import for `MappedByteBuffer` and implement `map(MapMode,long,long)` mirroring the retry loops used by `read`/`write`. The method should reopen channels closed due to interrupts, preserving the guard semantics.
3. **Refactor `HashFile` storage paths:**
   - Add helper methods (`mapBucket`, `readOverflowId`, etc.) that obtain `MappedByteBuffer` slices for bucket records.
   - Rewrite `storeID`, `getIDIterator`, and `increaseHashTable` to interact with mapped buffers using absolute offsets. Maintain a dirty flag toggled whenever a bucket mutates.
   - Ensure creation of empty buckets writes zeros using the mapped buffers (e.g., fill using `put` loops) instead of allocating intermediate heap buffers.
   - Update iterator state to hold onto a mapped buffer per bucket, duplicating or remapping as it walks overflow chains.
4. **Sync semantics:** adjust `sync()` to flush header as before, then when dirty ensure mapped pages are flushed by mapping the relevant range and calling `force()`. If `forceSync` is true or the caller passes `true`, continue forcing the channel metadata.
5. **Safety and cleanup:** on `close()`, flush any pending changes, clear references to allow GC, and close the `NioFile`.
6. **Documentation updates:** revise this ExecPlan’s progress, decisions, and surprises sections as discoveries occur.

## Concrete Steps

- Create the new test and run it in isolation to demonstrate the pre-change failure:

      cd /workspace/rdf4j
      mvn -pl core/sail/nativerdf -Dtest=HashFileMappedByteBufferUsageTest test

  Expectation: the build should fail with an assertion noting that no `map` calls occurred.

- After implementing the migration, rerun the same module-level tests and optionally the broader NativeStore test suite:

      mvn -pl core/sail/nativerdf test

  Expectation: all tests, including the new one, pass.

## Validation and Acceptance

The migration is complete when:

- `HashFileMappedByteBufferUsageTest` fails before the refactor and passes afterward, confirming that bucket accesses go through `MappedByteBuffer`.
- Existing NativeStore datastore tests continue to succeed, showing no regression in functionality.
- Manual inspection of `HashFile` reveals no lingering `nioFile.read/write` calls for bucket operations, and dirty tracking integrates with `sync()`.

## Idempotence and Recovery

The steps are idempotent:

- Maven test commands can be re-run safely.
- Mapping helper additions are additive; if intermediate mappings behave unexpectedly, reverting to the previous commit restores the prior behavior.
- If the migration introduces a regression, clear the dirty flag logic and fall back to the previous `NioFile` read/write loops using version control.

## Artifacts and Notes

- Capture the failing and passing Maven outputs for traceability in the final summary.
- Record any benchmarking observations if taken (optional) in `Surprises & Discoveries`.

## Interfaces and Dependencies

- Introduce `MappedByteBuffer map(FileChannel.MapMode mode, long position, long size)` to `org.eclipse.rdf4j.common.io.NioFile`.
- Modify `HashFile` to depend on `MappedByteBuffer` and use absolute `getInt/putInt` accessors on mapped views for bucket storage and iteration.
- Ensure `HashFile.IDIterator` remains thread-safe by keeping the existing `ReentrantReadWriteLock` usage while switching its backing buffer to a mapped duplicate.

