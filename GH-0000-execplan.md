# Implement LMDB bulk loader for index-at-a-time ingestion

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` must be kept up to date as work proceeds.

This document must be maintained in accordance with PLANS.md at the repository root (/workspace/rdf4j/PLANS.md).

## Purpose / Big Picture

LMDB users need a fast bulk-loading path that ingests a large RDF file without inserting into every index on every write. After this change, an operator can point the LMDB store at an RDF file (for example, N-Quads), and the loader will parse it once, translate values into internal IDs, and then load each triple index in sequence by bucketing the ID file and writing one index at a time. This makes index creation efficient and reduces random write overhead. Success is visible by running a new bulk-load test that parses a small N-Quads file, generates the ID file, bucket-sorts per index, and results in all configured indexes containing the same statements.

## Progress

- [x] (2025-12-26 16:25Z) Capture baseline layout and add ExecPlan.
- [x] (2025-12-26 16:34Z) Add bulk-load test and N-Quads fixture that initially fails.
- [x] (2025-12-26 16:44Z) Implement ID-file conversion, bucketing, and index-at-a-time loader.
- [x] (2025-12-26 16:44Z) Wire bulk loader into LmdbStore API and validate with LMDB tests.

## Surprises & Discoveries

- Observation: `org.eclipse.rdf4j.common.annotation.Nullable` is not available in the LMDB module classpath.
  Evidence: Compilation failure when using the annotation, resolved by removing the dependency.

## Decision Log

- Decision: Use a binary quad ID file (four longs per quad) to keep bucket processing fast and streaming-friendly.
  Rationale: A fixed-length binary format simplifies bucket writing and sorting while keeping I/O predictable.
  Date/Author: 2025-12-26 / Codex

- Decision: Bucket by the first component of the target index order, then sort within each bucket by full index order.
  Rationale: This keeps bucket assignment O(1) while still producing fully ordered input for LMDB append inserts.
  Date/Author: 2025-12-26 / Codex

## Outcomes & Retrospective

Bulk loading is now supported via a dedicated loader and a new LmdbStore entry point. The loader parses RDF files into an ID file, buckets data by index order, and loads indexes sequentially. The LMDB module tests that cover bulk loading now pass.

## Context and Orientation

The LMDB store lives under core/sail/lmdb. `LmdbSailStore` (core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbSailStore.java) handles writes via `LmdbSailSink`, which calls `TripleStore.storeTriple` to write quads into LMDB indexes. `TripleStore` (core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/TripleStore.java) maintains multiple `TripleIndex` instances, each with its own LMDB database. `ValueStore` (core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/ValueStore.java) maps RDF values to IDs and vice-versa. The new bulk loader must use `ValueStore` to assign IDs, then build indexes by writing sorted keys directly to each index database.

## Plan of Work

First, add a targeted test in core/sail/lmdb that fails because the bulk-load API does not exist. The test should create a temporary LMDB store with a small N-Quads resource, invoke the bulk loader, and then query the store through the standard API to ensure statements are available and all configured indexes are populated. The test should also validate that the ID file is created and cleaned up.

Next, introduce a new bulk-loading helper in the LMDB package, for example `LmdbBulkLoader` in core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbBulkLoader.java. This class should parse RDF input using Rio, call `ValueStore.storeValue` for each component, and write fixed-length binary records (four longs) to a temporary ID file. It should track the maximum ID observed for each component to support bucketing.

Then implement bucket processing: for each index order in `TripleStore`, read the ID file, assign each quad to a bucket based on the first component of the index order and the max ID, write bucket files, then for each bucket sort the quads by the full index order and append them to the LMDB database for that index. Use LMDB append insert flags when possible and fall back to standard inserts if ordering checks fail.

Finally, wire the loader into a public entry point. The simplest path is a new method on `LmdbStore` (core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbStore.java) that accepts a `Path`, an RDF format, and an optional base URI. It should ensure the store is not currently open for reads/writes, invoke `LmdbBulkLoader`, and reopen or refresh state as needed. Update tests to use this method.

## Concrete Steps

From /workspace/rdf4j, create the failing test and run it with the LMDB module only. Use the mvnf runner so it performs the required root install first.

    python3 .codex/skills/mvnf/scripts/mvnf.py LmdbBulkLoadTest#testBulkLoadNQuads

Implement the bulk loader and run the same test until it passes. Then run the LMDB module tests.

    python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/lmdb

## Validation and Acceptance

The new test `LmdbBulkLoadTest#testBulkLoadNQuads` should fail before the bulk loader exists and pass afterward. The LMDB module tests should be green. A manual smoke check can be done by creating an LMDB store, calling the new bulk-load API with a small N-Quads file, and verifying that the statements are readable via a `RepositoryConnection`.

## Idempotence and Recovery

The bulk loader should write temporary files in a dedicated working directory under the LMDB data directory and remove them after a successful load. If a load fails, it should leave the working directory in place for debugging. Rerunning the loader should remove stale temp files before proceeding to avoid mixing data from prior attempts.

## Artifacts and Notes

Example ID file record format (binary, four longs):

    [subjectId][predicateId][objectId][contextId]

Example bucket assignment for a SPOC index with 256 buckets:

    bucket = (subjectId * 256) / (maxSubjectId + 1)

## Interfaces and Dependencies

In core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbBulkLoader.java, define a public method:

    public void load(Path inputFile, RDFFormat format, @Nullable String baseUri) throws IOException, SailException

In core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbStore.java, add a public entry point:

    public void bulkLoad(Path inputFile, RDFFormat format, @Nullable String baseUri) throws SailException

The bulk loader should depend on Rio parsers (core/rio/api) and reuse existing ValueStore and TripleStore classes without introducing new external dependencies.

---

Change Log: Initial ExecPlan created with bulk load flow and test plan.
Change Log: Updated progress, discoveries, and outcomes after implementation and test validation.
