# Migrate NativeStore locking and hashing helpers to FileChannel

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` must be kept up to date as work proceeds.

See `PLANS.md` at the repository root for required structure and maintenance rules.

## Purpose / Big Picture

NativeStore currently relies on `RandomAccessFile` in its hashing data structure and directory locking helper. We want the store to exclusively use NIO `FileChannel`-based access so it benefits from the same interrupt-safe reopen logic provided elsewhere and avoids legacy I/O APIs. After these changes a developer can point to both the hash file rehash path and the directory lock manager using `FileChannel` primitives, with tests that guard against regressing back to `RandomAccessFile`.

## Progress

- [x] (2024-03-05 00:00Z) Draft plan and identify affected classes.
- [x] (2025-11-10 17:50Z) Add regression tests that currently fail because `RandomAccessFile` is still in use.
- [x] (2025-11-10 17:52Z) Update implementation to satisfy tests and ensure all resources use `FileChannel`.
- [x] (2025-11-10 17:53Z) Run targeted Maven test suites and formatting checks.
- [x] (2025-11-10 17:54Z) Document outcomes, clean up plan, and finalize.
- [x] (2025-11-10 18:05Z) Introduce FileChannel guard rails for DataFile/IDFile and port their channel helpers away from NioFile.
- [x] (2025-11-10 18:19Z) Run the full `core/sail/nativerdf` module tests to ensure the broader NativeStore suite remains stable.

## Surprises & Discoveries

- Maven module isolation: running a single module's tests (`core/sail/nativerdf`) without pre-installing sibling modules fails
  because the build expects snapshot artifacts for other RDF4J modules in the local repository. Evidence: dependency resolution
  errors when executing `mvn -pl core/sail/nativerdf -Dtest=HashFileFileChannelMigrationTest test`.

## Decision Log

- Decision (superseded): Attempt a root-level `mvn install -DskipTests` to populate local snapshot artifacts before targeted
  module tests. Rationale was to avoid the forbidden `-am` flag when running tests. This proved too time-consuming (multi-hour
  reactor), so the approach was abandoned partway through. Date/Author: 2025-11-10 / ChatGPT.
- Decision: Use `mvn -pl core/sail/nativerdf -am -DskipTests install` followed by `mvn -pl core/sail/api -am -DskipTests install`
  to pre-install only the relevant module subtrees. Rationale: `-am` is disallowed only for test runs per AGENTS.md, so using it
  for skipped-test installs is compliant and far faster than a full reactor build. Date/Author: 2025-11-10 / ChatGPT.

## Outcomes & Retrospective

- HashFile and DirectoryLockManager both rely exclusively on `FileChannel` primitives and close them deterministically, keeping
  the previous durability semantics intact.
- Guard-rail tests in `core/sail/nativerdf` and `core/sail/api` fail against the legacy code and pass after the migration,
  proving the refactor.
- DataFile and IDFile reopen their channels via `FileChannel`, maintain cached sizes, and expose regression tests that exercise
  recovery paths when channels are closed mid-operation.

## Context and Orientation

NativeStore lives under `core/sail/nativerdf`. Hash indexing lives in `core/sail/nativerdf/src/main/java/org/eclipse/rdf4j/sail/nativerdf/datastore/HashFile.java`. Directory locking is provided by `core/sail/api/src/main/java/org/eclipse/rdf4j/sail/helpers/DirectoryLockManager.java`, which NativeStore uses during initialization. Both classes still allocate `RandomAccessFile`. We already have an interrupt-safe wrapper `org.eclipse.rdf4j.common.io.NioFile` that wraps `FileChannel` and exposes reopen logic. The goal is to align these components with the rest of the NativeStore stack and prevent regressions with tests.

## Plan of Work

1. Create a new unit test in `core/sail/nativerdf` that introspects `HashFile`'s private `createEmptyFile` helper via reflection and asserts it returns a `FileChannel`. The legacy code returns `RandomAccessFile`, so this test will fail before the migration.
2. Add a complementary test in `core/sail/api` exercising `DirectoryLockManager.tryLock()` and verifying the anonymous `Lock` implementation no longer closes over a `RandomAccessFile` field but instead keeps a `FileChannel`. Reflection on the lock's declared fields provides the guardrail.
3. Modify `HashFile` to replace the `RandomAccessFile` usage with `FileChannel.open(...)` and adjust related logic (`createEmptyFile`, rehash temp file handling) to work with the new channel. Ensure all paths close channels and preserve truncation behavior.
4. Refactor `DirectoryLockManager` to open `FileChannel` instances using NIO options (read, write, create), adjust lock acquisition/release to work directly with channels, and update cleanup code accordingly.
5. Run targeted Maven tests for the affected modules, followed by any required formatting checks.
6. Extend the regression harness with DataFile/IDFile guard rails that verify FileChannel usage, reopen semantics, and recovery
   helpers.
7. Port DataFile/IDFile off `NioFile`, ensuring buffer flushes, cached sizes, and reopen loops use direct `FileChannel` calls.
8. Re-run module-level Maven tests (`mvn -pl core/sail/nativerdf test`) and update this plan's living sections with the latest
   evidence.

## Concrete Steps

1. `cd /workspace/rdf4j`
2. Implement and run `mvn -pl core/sail/nativerdf -Dtest=HashFileFileChannelMigrationTest test` to capture the pre-change failure.
3. Implement and run `mvn -pl core/sail/api -Dtest=DirectoryLockManagerFileChannelTest test` to capture its pre-change failure.
4. Apply production code changes described above.
5. Re-run the same Maven commands to confirm passing tests.
6. Run any additional formatting or verification commands required by the repository.

## Validation and Acceptance

- The new HashFile reflection test fails before the migration and passes afterwards, showing that rehash temp files now use `FileChannel`.
- The new DirectoryLockManager reflection test fails before the migration and passes afterwards, demonstrating lock management no longer depends on `RandomAccessFile`.
- DataFile/IDFile guard tests confirm FileChannel usage, reopen recovery, and header validation across the migration.
- Targeted Maven test runs complete successfully post-change.

## Idempotence and Recovery

- Tests and Maven commands are safe to re-run; they only operate on temporary directories under `java.nio.file.Files.createTempDirectory`.
- The code edits are reversible via Git if needed.

## Artifacts and Notes

- Capture Maven failure snippets for both new tests before applying fixes.
- Record passing output after the migration to evidence success.

## Interfaces and Dependencies

- Use `java.nio.channels.FileChannel` with `StandardOpenOption.CREATE`, `WRITE`, `READ`, and `TRUNCATE_EXISTING` where applicable.
- Continue to rely on `org.eclipse.rdf4j.common.io.NioFile` for safe channel operations in `HashFile`.
