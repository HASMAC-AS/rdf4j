# LMDB bulk load by index with ID bucketing

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` must be kept up to date as work proceeds.

PLANS.md lives at `/workspace/rdf4j/PLANS.md`; this document must be maintained in accordance with that file.

## Purpose / Big Picture

Users need a faster way to load large RDF files into the LMDB store. The new bulk loader will parse an input RDF file (e.g., N-Quads), convert values to internal LMDB IDs once, and then load one LMDB index at a time using bucketed, sorted batches. After this change, a developer can call a new LMDB bulk load API with a file and RDF format and see all data in the store, with all configured indexes loaded, without relying on the per-statement insert path.

## Progress

- [x] (2025-12-26 16:50Z) Capture current LMDB load path context and decide entrypoint API.
- [x] (2025-12-26 16:50Z) Implement bulk load pipeline (parse to ID file, bucket, per-index load).
- [x] (2025-12-26 16:55Z) Add tests proving bulk load inserts and queries work.
- [x] (2025-12-26 16:51Z) Run targeted LMDB tests and update plan sections.
- [ ] Finalize documentation notes and clean up temporary artifacts.

## Surprises & Discoveries

- Observation: None yet.
- Observation: Offline verify failed to resolve surefire-junit-platform; reran with network access to fetch dependencies.
  Evidence: mvnf verify log showed missing org.apache.maven.surefire:surefire-junit-platform:3.5.4 in offline mode; rerun succeeded.

## Decision Log

- Decision: Use an LMDB-specific bulk load API exposed on `org.eclipse.rdf4j.sail.lmdb.LmdbStore` instead of a generic Sail API.
  Rationale: The feature is LMDB-specific (index ordering, LMDB append semantics), and the user asked for an LMDB bulk loading feature.
  Date/Author: 2025-12-26 / Codex

## Outcomes & Retrospective

- Bulk load pipeline and tests are implemented. Targeted LMDB tests now pass; remaining work is final review and cleanup.

## Context and Orientation

The LMDB store lives under `core/sail/lmdb`. `LmdbStore` is the public Sail implementation. `LmdbSailStore` owns the `TripleStore` (index storage) and `ValueStore` (value-to-ID mapping). `TripleStore` stores quads as varint-encoded keys across multiple LMDB databases (indexes like `spoc` and `posc`). `ValueStore` assigns IDs to RDF values. For bulk loading, we will parse an RDF file into IDs, write the IDs to a temporary quad file, then load each index in order, ensuring keys are inserted in sorted order so LMDB can use append operations.

Key terms defined for this plan:

- Index order: the four-letter sequence (e.g., `spoc`) that defines lexicographic key order for a triple index.
- Bucketing: splitting the temporary quad file into multiple smaller files based on the leading component of the index order to keep per-bucket sorting manageable.
- Quad ID file: a binary file storing four `long` values per statement (subject, predicate, object, context IDs).

## Plan of Work

We will add a bulk load entrypoint on `org.eclipse.rdf4j.sail.lmdb.LmdbStore` that accepts a `Path` plus `RDFFormat`. The method will validate the store is writable and empty, then delegate to a new helper class in the LMDB module that coordinates parsing and loading. The helper will parse the RDF file using Rio into statements and store each value in the `ValueStore`, while writing `(s,p,o,c)` IDs to a temporary quad file. It will track the maximum ID for each component. After the ID file is written, it will iterate over the configured triple indexes in order. For each index, it will bucket the quad file based on the index’s first component (using the max value to derive bucket boundaries), sort each bucket by the full index order, and stream the sorted quads into a new `TripleStore` bulk insert method that appends keys in sorted order. The first index load will also update LMDB’s context counts (since the normal insert path does so per statement). Finally, the loader will commit the value store and triple store transactions and return.

This plan adds:

- A new `LmdbBulkLoader` (package-private) in `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb` to coordinate the pipeline.
- A `TripleStore.bulkLoadIndex(...)` method to append sorted quads into a specific LMDB index (explicit-only for now), with optional context count updates.
- A `LmdbSailStore.bulkLoad(...)` method invoked by `LmdbStore.bulkLoad(...)` to run bulk loading under the LMDB store’s internal lock.
- New tests under `core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb` to validate bulk load behavior.

## Concrete Steps

1. Inspect LMDB store entrypoints and confirm the best place to add the bulk load API. Update this plan if the entrypoint changes.
2. Add `LmdbStore.bulkLoad(Path, RDFFormat)` that validates writable state and delegates to `LmdbSailStore.bulkLoad`.
3. Implement `LmdbSailStore.bulkLoad` to acquire the store access lock, ensure no active transaction, and call `LmdbBulkLoader` with `ValueStore` and `TripleStore`.
4. Implement `LmdbBulkLoader` to:
   - Create a temp directory under the data dir (e.g., `bulk-load-tmp`).
   - Parse the input RDF file with Rio, storing values in `ValueStore` and writing quad IDs to a binary file.
   - Track `maxSubjectId`, `maxPredicateId`, `maxObjectId`, and `maxContextId`.
   - For each index field sequence: bucket the quad file by first component, sort each bucket by full index order, and call `TripleStore.bulkLoadIndex` to append sorted keys.
5. Add `TripleStore.bulkLoadIndex` that opens a write transaction, appends sorted keys with `MDB_APPEND`, skips duplicates, resizes the LMDB map when needed, and updates context counts on the first index.
6. Add a `LmdbBulkLoadTest` that writes a small N-Quads file to disk, calls `LmdbStore.bulkLoad`, then asserts statement count and query results across multiple patterns and contexts.
7. Run targeted LMDB tests with `mvnf` and update `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` with outcomes and any changes.

Example command usage to be validated once implemented (from repo root):

    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbBulkLoadTest

Expected output will show the new test passing.

## Validation and Acceptance

The change is accepted when a developer can load a small N-Quads file via `LmdbStore.bulkLoad(Path, RDFFormat.NQUADS)`, open a connection, and see all statements and contexts. The new unit test must fail before the implementation (due to the missing bulk load API) and pass after. Additionally, running the targeted LMDB test class should complete successfully and demonstrate that queries by subject, predicate, and context return the expected statements.

## Idempotence and Recovery

The bulk loader will refuse to run if the store is non-empty, so re-running it is safe after clearing the data directory. Temporary files are written under the data directory and are deleted by the loader at the end of a successful run. If a run fails midway, the loader will leave the temp directory in place for inspection; a subsequent run should first delete the temp directory or use a fresh data directory.

## Artifacts and Notes

A successful run should create a temporary quad ID file and per-index bucket files under the LMDB data directory. These are implementation details and should not be committed to the repository.

## Interfaces and Dependencies

The following new or extended APIs will exist after implementation:

- `org.eclipse.rdf4j.sail.lmdb.LmdbStore#bulkLoad(java.nio.file.Path, org.eclipse.rdf4j.rio.RDFFormat)`
- `org.eclipse.rdf4j.sail.lmdb.LmdbSailStore#bulkLoad(java.nio.file.Path, org.eclipse.rdf4j.rio.RDFFormat)` (package-private)
- `org.eclipse.rdf4j.sail.lmdb.TripleStore#bulkLoadIndex(String fieldSeq, java.util.Iterator<long[]> quads, boolean explicit, boolean updateContexts)` (package-private)
- `org.eclipse.rdf4j.sail.lmdb.LmdbBulkLoader` (package-private helper)

These methods will only use existing project dependencies (Rio for parsing, LMDB for storage).


Plan change note: initial ExecPlan created with skeleton and bulk load approach on 2025-12-26.
Plan change note: updated progress and discoveries after running LMDB tests and handling offline dependency on 2025-12-26.
