# LMDB Leapfrog Triejoin (WCOJ) ExecPlan

This ExecPlan is a living document. Maintain it according to PLANS.md and update every section as work proceeds.

If PLANS.md file is checked into the repo, reference the path to that file here from the repository root and note that this document must be maintained in accordance with PLANS.md.

Reference: /PLANS.md (repository root). All guidance and formatting rules in that file apply.

## Purpose / Big Picture

Introduce a worst-case optimal join operator based on Leapfrog Triejoin (LFTJ) for RDF4J's LMDB store. After completing this plan, a user can execute conjunctive/BGP-style queries over LMDB-backed quads using LFTJ: the engine will navigate existing SPOC/PSOC/OSPC/CSPO indexes as tries, intersect variable bindings per global order, and emit bindings without building new persistent structures. Successful implementation means the LMDB store can answer cyclic or join-heavy queries with predictable latency using only existing B+tree indexes.

## Progress

Use a list with checkboxes to summarize granular steps. Every stopping point must be documented here, even if it requires splitting a partially completed task into two ("done" vs. "remaining"). This section must always reflect the actual current state of the work.

- [x] (2025-11-22 08:30Z) Draft ExecPlan capturing goals, scope, and initial milestones.
- [x] (2025-11-22 09:05Z) Stubbed QuadKey/Prefix/TrieIterator scaffolding and added encoding/prefix unit tests.
- [x] (2025-11-22 09:24Z) Implemented LMDBTrieIterator around LMDB cursors with open/next/seek semantics and unit coverage.
- [x] (2025-11-22 14:02Z) Added pattern/index mapping utilities and prefix construction for variable orders with unit tests.
- [x] (2025-11-22 16:29Z) Implemented LFTJ execution driver and leapfrog intersection wired to iterators with LMDB-backed integration tests for chains and triangles.
- [ ] Integrate WCOJ operator into planner/runtime with heuristics and end-to-end validation.
- [ ] Add benchmarks and document outcomes; finalize retrospective.

## Surprises & Discoveries

Document unexpected behaviors, bugs, optimizations, or insights discovered during implementation. Provide concise evidence.

- LMDB `MDB_SET_RANGE` seeks can land on keys that satisfy earlier slots but not later constrained slots (e.g., predicate) when those earlier slots are unbound. The LMDBTrieIterator now advances with `MDB_NEXT` until it finds the first key matching the prefix to avoid premature exhaustion of iterators in LFTJ.

## Decision Log

Record every decision made while working on the plan.

- Decision: Use dedicated `docs/execplans` location for this plan to keep it discoverable outside code packages.
  Rationale: Keeps living design artifacts organized without interfering with source directories.
  Date/Author: 2025-11-22 / Assistant

## Outcomes & Retrospective

Summarize outcomes, gaps, and lessons learned at major milestones or at completion. Compare the result against the original purpose.

- Pending initial implementation.

## Context and Orientation

RDF4J's LMDB store lives under `core/sail/lmdb`. Existing code manages triple/quad indexes with LMDB B+trees using varint-encoded IDs. There is currently no LFTJ operator. We will introduce new LFTJ-focused abstractions in a dedicated package (e.g., `org.eclipse.rdf4j.sail.lmdb.lftj`) without altering existing persistent layouts. Key supporting utilities like `Varint`, `LmdbUtil`, and `TripleStore` encode IDs and navigate LMDB cursors; these will stay intact while new code wraps LMDB indexes as trie iterators.

The primary artifacts to add are:
- Data classes to describe quad keys (`QuadKey`), index orders (`QuadKeyOrder`), and binding prefixes (`Prefix`).
- A `TrieIterator` interface and an `LMDBTrieIterator` implementation that uses LMDB cursor operations (`MDB_SET_RANGE`, `MDB_NEXT`) to walk index tries.
- LFTJ execution logic that orders variables, constructs iterators per pattern, performs leapfrog intersection, and emits bindings.
- Planner/runtime integration to select WCOJ for suitable patterns and maintain LMDB transaction semantics.

## Plan of Work

Work proceeds in incremental milestones that each add observable behavior and associated tests.

1. Define quad-centric data structures and encoding helpers. Add unit tests that validate encode/decode reversibility across index orders and prefix matching behavior. This establishes the foundational representation used by iterators.
2. Implement `LMDBTrieIterator` that wraps LMDB cursors, supporting `open`, `key`, `next`, `seek`, and `atEnd` with prefix-awareness and value deduplication. Add tests using synthetic LMDB datasets to verify prefix handling and iteration semantics.
3. Build pattern-to-index mapping utilities and prefix construction functions that respect a chosen global variable order. Validate with synthetic patterns and bindings to ensure prefixes materialize constants and prior bindings correctly.
4. Implement the LFTJ driver with leapfrog intersection across iterators. Introduce variable-order heuristics and compare results with existing join strategies on small datasets to ensure correctness.
5. Integrate a WCOJ operator into the planner/runtime with heuristics to decide when to use it. Ensure LMDB transaction handling remains snapshot-consistent and visible in explain plans.
6. Add benchmarks for cyclic and acyclic query patterns to measure latency, LMDB cursor operations, and storage impact. Document results and update retrospective sections.

## Concrete Steps

State the exact commands to run and where to run them (working directory). Update as work proceeds.

- From repository root, run module tests during each milestone: `mvn -pl core/sail/lmdb -DskipITs test`
- For LMDB iterator prototyping, create temporary LMDB environments under `target/test-lmdb` and clean them per test.
- Use `rg` for code search and `mvn -pl core/sail/lmdb -DskipTests compile` for quick compile checks when needed.

## Validation and Acceptance

The change is accepted when:
- New unit tests for key encoding/prefix handling fail before implementations exist and pass afterward.
- LMDBTrieIterator tests demonstrate correct prefix-respecting iteration (including `seek` and duplicate skipping).
- LFTJ execution tests match baseline join outputs on sample data.
- Planner integration shows WCOJ in explain plans for eligible queries and produces correct bindings under LMDB.

## Idempotence and Recovery

All steps are additive and safe to rerun. LMDB test environments should be created under `target/test-lmdb` and removed between runs to avoid residue. If a migration or refactor fails mid-way, revert using git and re-run the same commands; no destructive operations touch production data structures.

## Artifacts and Notes

As work progresses, include test output snippets, sample bindings, and any LMDB cursor operation counts observed during debugging. Keep evidence concise and focused on demonstrating correctness and performance expectations.

## Interfaces and Dependencies

Prescribed interfaces and types to create:
- `org.eclipse.rdf4j.sail.lmdb.lftj.Slot` enum with entries `S`, `P`, `O`, `C`.
- `org.eclipse.rdf4j.sail.lmdb.lftj.QuadKey` holding four long term IDs with getters and constructors.
- `org.eclipse.rdf4j.sail.lmdb.lftj.QuadKeyOrder` describing position order (array of `Slot`) and helpers for common orders (SPOC, PSOC, OSPC, CSPO, etc.).
- `org.eclipse.rdf4j.sail.lmdb.lftj.Prefix` capturing bound/unbound flags and values for S/P/O/C with builder-style helpers.
- `org.eclipse.rdf4j.sail.lmdb.lftj.QuadKeyEncoding` (or equivalent) with `encode`/`decode`, `matchesPrefix`, `componentForRole`, and `minimalKeyForPrefix` utilities using monotone big-endian encoding of long term IDs.
- `org.eclipse.rdf4j.sail.lmdb.lftj.TrieIterator` interface with methods `open(Prefix)`, `boolean atEnd()`, `long key()`, `void next()`, and `void seek(long)`, designed for variable-at-a-time navigation.
- `LMDBTrieIterator` implementing `TrieIterator` with LMDB cursor operations and prefix-aware navigation.
