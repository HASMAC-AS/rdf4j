# Vectorize LMDB record-to-statement pipeline

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` must be kept up to date as work proceeds.

This plan must be maintained in accordance with PLANS.md located at `./PLANS.md` in the repository root.

## Purpose / Big Picture

The LMDB sail currently decodes one record at a time: every cursor advance copies a single key from LMDB into Java primitives, translates each ID to a value, and finally materialises a statement. This change introduces a vectorised path so that batches of records are decoded and converted together. The goal is lower per-record overhead while preserving semantics; users should see faster iteration for large query answers without any behavioural change. Success is demonstrated by new tests exercising the batch API and confirming it returns the same data as the scalar code path.

## Progress

- [x] (2025-11-04 02:45Z) Draft and land regression tests that lock in batch decoding expectations for LMDB iterators and value conversion; verified the new tests execute under `mvn -pl core/sail/lmdb test`.
- [x] (2025-11-04 02:55Z) Implemented vectorised decoding and value materialisation according to this plan, keeping scalar fallbacks for compatibility.
- [x] (2025-11-04 02:57Z) Ran targeted module tests (`mvn -pl core/sail/lmdb test`) and updated documentation/plan sections.

## Surprises & Discoveries

- Observation: The newly introduced batch helpers compiled and tests passed immediately because default fallbacks already satisfied behavioural expectations, so performance-focused assertions were needed to justify the production changes.
  Evidence: `mvn -pl core/sail/lmdb test` succeeded before and after implementing the vectorised path (Surefire summary).

## Decision Log

- Decision: Decode LMDB keys in `TripleStore.TripleIndex.keyToQuad` using absolute byte reads and `Varint.firstToLength` to avoid intermediate `ByteBuffer` duplication and to write directly into batch buffers.
  Rationale: Reduces per-record overhead during batch filling and enables `LmdbRecordIterator.fillBatch` to populate contiguous arrays without extra copies.
  Date/Author: 2025-11-04 / AI agent.

- Decision: Fail fast in `ValueStore.bulkGetLazyValues` and `LmdbStatementIterator.fillStatementBatch` when caller-provided buffers are undersized to prevent silent data loss while batching.
  Rationale: Batch APIs are internal but shared across iterators; explicit range validation catches misuse early.
  Date/Author: 2025-11-04 / AI agent.

## Outcomes & Retrospective

- Completed vectorised batch decoding and statement materialisation. `LmdbRecordIterator.fillBatch` and `LmdbStatementIterator.fillStatementBatch` now drive the new tests while resolving values in bulk. All LMDB module tests pass, matching scalar semantics with improved batching hooks. Future work: add micro-benchmarks once infrastructure is available.


## Context and Orientation

The LMDB sail stores triples as varint-encoded keys. `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbRecordIterator.java` decodes those keys into `long[]` quads via `TripleStore.TripleIndex.keyToQuad`. `LmdbStatementIterator` then resolves IDs through `ValueStore.getLazyValue` and constructs statements one-by-one. Value lookup and statement creation live in `ValueStore`. Tests for LMDB storage are under `core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb`.

We will extend the iterator API with a batch method, add vectorised decoding utilities to `TripleStore.TripleIndex` and `Varint`, and add batch-friendly lookup helpers to `ValueStore`. `LmdbStatementIterator` will cache a batch of decoded statements and deliver them through `next()` to maintain the public contract.

## Plan of Work

1. **Tests first**
   - Add a new JUnit test in `core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/LmdbRecordIteratorTest.java` (new file) that loads a handful of triples into a temporary store, then exercises both the scalar `next()` and the new batch method to assert identical quads. The test should fail until batch support is implemented.
   - Extend `ValueStoreTest` with a scenario that uses the forthcoming `bulkGetLazyValues` helper to resolve multiple IDs in a single call, asserting the returned values match repeated `getLazyValue` calls. This fails until the helper exists.
   - Add a test for `LmdbStatementIterator` verifying that iterating with the new batch-backed implementation returns the same statement sequence as the legacy scalar path. This can reuse the triples from the record iterator test to avoid redundant fixtures.

2. **Implement batch decoding**
   - Introduce a default `int fillBatch(long[] quads, int quadOffset, int maxQuads)` method on `RecordIterator` with a scalar fallback calling `next()`. Override it in `LmdbRecordIterator` to fetch up to `maxQuads` entries, using a new `TripleIndex.decodeQuad(ByteBuffer key, long[] originalQuad, long[] target, int offset)` that writes directly into the batch buffer. Implement supporting utilities in `Varint` to decode four consecutive varints efficiently without repeated method dispatch.
   - Adjust `TripleIndex` so range-filtering and value matching still work with the batched decoder. Ensure `GroupMatcher` integration remains intact.

3. **Vectorise value and statement materialisation**
   - Add `ValueStore.bulkGetLazyValues(long[] ids, int offset, int length, Value[] target)` which acquires the revision lock once and resolves all IDs, consulting caches when possible.
   - In `LmdbStatementIterator`, maintain reusable arrays for IDs and resolved values. Populate them via `recordIt.fillBatch(...)` and `bulkGetLazyValues`, then emit statements from the buffered batch. Provide scalar fallback for contexts that cannot be batched.

4. **Wire up callers and clean up**
   - Update `TripleStore` and any other class that consumes `RecordIterator` to prefer the batch method where beneficial (e.g., when streaming to consumers that can accept multiple quads). Keep existing loops intact if batching offers no advantage.
   - Ensure the iterator closes resources correctly even when partially consumed batches remain.

5. **Validation**
   - Run `mvn -pl core/sail/lmdb test` and capture evidence. If additional modules need testing due to API changes, extend the coverage accordingly.
   - Update this plan’s progress, decisions, and outcomes, noting any surprises or deviations.

## Concrete Steps

1. From `core/sail/lmdb`, ensure the batch-focused unit tests exist (record iterator batch parity, value store bulk lookup, statement iterator parity).
2. Implement interface and utility changes for batch decoding, then update iterators to use them.
3. Add bulk value lookup and batched statement buffering, updating iterators accordingly.
4. Run `mvn -pl core/sail/lmdb test`.
5. Review the plan sections (`Progress`, `Decision Log`, `Outcomes`) and adjust to reflect reality.

## Validation and Acceptance

- The new batch-aware tests pass, proving that batched decoding and value materialisation produce identical results to the scalar path.
- Existing LMDB store tests continue to pass, indicating behavioural compatibility.
- Code is formatted and free of static analysis issues triggered by the new changes.

## Idempotence and Recovery

- Tests can be re-run repeatedly; they set up and tear down their own temporary stores.
- Batch buffers are cleared between runs, so re-running the iterator does not leak resources.
- If batch decoding fails mid-stream, the fallback scalar `next()` remains available through the default interface method, making it easy to revert to the previous behaviour if necessary.

## Artifacts and Notes

- Capture failing test output before implementation and passing output afterward to include in the final handoff.

## Interfaces and Dependencies

- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/RecordIterator`: add `default int fillBatch(long[] quads, int quadOffset, int maxQuads)`.
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbRecordIterator`: override `fillBatch`, implement batch decoding using `TripleIndex` helpers.
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/TripleStore.java`: add methods `decodeQuadBatch` or equivalent to support the iterator.
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/Varint.java`: extend with vectorised quad decoding helper.
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/ValueStore.java`: expose `bulkGetLazyValues` and batch statement creation helper if required.
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbStatementIterator.java`: consume batched records and convert them to statements using the new helpers.
- Add corresponding tests under `core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb` for iterator parity and value bulk retrieval.

---

2025-11-04: Updated progress, decisions, and outcomes after implementing vectorised iterators and running module tests (AI agent).
