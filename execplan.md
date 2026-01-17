# Reduce Lucene reindex memory by batched, sorted indexing

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` must be kept up to date as work proceeds. This document must be maintained in accordance with PLANS.md at `PLANS.md`.

## Purpose / Big Picture

LuceneSail reindexing currently depends on a SPARQL query that orders all statements by subject, which can force large datasets to be sorted in memory. The goal is to preserve the requirement that Lucene indexing consumes statements in subject-sorted batches, while allowing the reindex loop to iterate over raw statements and sort them in smaller, bounded chunks. After this change, users can reindex large stores without loading the full dataset into memory, and the new tests will demonstrate that the sorting requirement is enforced by the reindex path.

## Progress

- [x] (2026-01-17 08:55Z) Inspect current Lucene reindex flow and tests.
- [x] (2026-01-17 08:55Z) Add failing tests covering sorted batching requirements.
- [x] (2026-01-17 08:55Z) Implement batched sorted indexing via getStatements.
- [x] (2026-01-17 08:55Z) Update tests, formatting, and targeted verification.
- [ ] (2026-01-17 08:55Z) Summarize outcomes and finalize artifacts.

## Surprises & Discoveries

- Observation: The first offline test run required fetching the Surefire JUnit platform artifacts.
  Evidence: Maven reported missing surefire-junit-platform in offline mode until the dependency was fetched online.

## Decision Log

- Decision: Use a batch size constant in LuceneSail reindexing and sort each batch before indexing.
  Rationale: Matches issue guidance and keeps memory bounded while preserving sorted input to lucene indexing.
  Date/Author: 2026-01-17 / Codex

- Decision: Feed sorted batches into `SearchIndex.addRemoveStatements` instead of per-subject `addDocuments`.
  Rationale: Allows batched indexing without requiring a globally ordered query result and keeps ordering guarantees in one place.
  Date/Author: 2026-01-17 / Codex

## Outcomes & Retrospective

The reindex path now batches statements from `getStatements`, sorts each batch, and indexes using the existing bulk API. A dedicated test exercises unsorted statement iteration and validates sorted batching behavior. The reindex process no longer requires a global ordered query, reducing memory pressure for large stores. The remaining work is to finalize summary artifacts.

## Context and Orientation

Lucene reindexing logic lives in `core/sail/lucene-api/src/main/java/org/eclipse/rdf4j/sail/lucene/LuceneSail.java` in the `reindex()` method. It previously used a SPARQL query with `ORDER BY ?s` and looped over results, assuming subject ordering to batch statements per subject before calling `luceneIndex.addDocuments`. Tests for Lucene reindexing live under `core/sail/lucene/src/test/java/org/eclipse/rdf4j/sail/lucene/`, including the new `ReindexBatchingTest`.

The new reindex loop uses `SailRepositoryConnection.getStatements` to iterate over statements without a global sort, collecting a bounded list of statements, sorting that list, and passing the batch to the existing bulk indexing API.

## Plan of Work

First, read existing Lucene reindex tests and any helper test utilities in the lucene module to decide where to add coverage. Add a new test (or tests) that assert reindexing behaves correctly when statements are delivered out of order and that the indexing logic expects sorted input. The test should fail against the current implementation if it does not enforce sorted batches.

Next, change `LuceneSail.reindex()` to iterate using `getStatements` rather than the ordered SPARQL query. Collect a fixed number of statements into a list, sort the list by subject (and optionally predicate/object/context to ensure stable ordering), and feed that list to the existing lucene indexing batching logic. Repeat until the iterator is exhausted. Ensure each batch is indexed correctly and any remaining statements are handled at the end.

Finally, re-run the targeted lucene tests, update the ExecPlan progress and outcomes, and capture any unexpected behavior in the Surprises section.

## Concrete Steps

From the repository root:

    mvn -T 1C -o -Dmaven.repo.local=.m2_repo -Pquick clean install | tail -200

Add or update lucene tests in `core/sail/lucene/src/test/java/org/eclipse/rdf4j/sail/lucene/` and run the smallest failing test with:

    python3 .codex/skills/mvnf/scripts/mvnf.py <TestClass#method>

Implement the reindex changes in `core/sail/lucene-api/src/main/java/org/eclipse/rdf4j/sail/lucene/LuceneSail.java` and re-run the same focused test, then broaden to the lucene module:

    python3 .codex/skills/mvnf/scripts/mvnf.py core/sail/lucene

Run formatter before final verification:

    mvn -o -Dmaven.repo.local=.m2_repo -q -T 2C process-resources

## Validation and Acceptance

Acceptance requires: the new test fails before the reindex change and passes after; lucene module tests pass; and the reindex logic now iterates over `getStatements` in bounded batches, sorts each batch, and indexes using the existing lucene index calls. The new tests should demonstrate that unsorted inputs are properly handled by the reindexing path.

## Idempotence and Recovery

Edits are confined to lucene reindexing and test files. Changes are safe to reapply; if a test or build fails, revert the local changes and re-run the focused test. The reindex logic uses existing repository connections and does not require external services.

## Artifacts and Notes

Relevant files:

    core/sail/lucene-api/src/main/java/org/eclipse/rdf4j/sail/lucene/LuceneSail.java
    core/sail/lucene/src/test/java/org/eclipse/rdf4j/sail/lucene/ReindexBatchingTest.java

## Interfaces and Dependencies

Use the existing `SailRepository`, `SailRepositoryConnection`, `Statement` collection types, and `SearchIndex.addRemoveStatements`. Avoid new dependencies. Sorting should rely on existing RDF4J model comparisons or a comparator built from statement components.

Note: This plan will be updated as decisions are made and tests are added or modified.

Plan updated 2026-01-17: marked completed steps, recorded the bulk-indexing decision, and updated references to the new test file and batch-oriented reindex implementation.
