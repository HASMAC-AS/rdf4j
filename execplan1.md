# Make LMDB LFTJ iterators decode only the active slot

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` must be kept up to date as work proceeds.

This plan must be maintained in accordance with `PLANS.md` at the repository root of this project.

## Purpose / Big Picture

Today, the LMDB-backed Leapfrog Triejoin (LFTJ) implementation decodes all four quad components (subject, predicate, object, context) from each LMDB key on every iterator move. Even after avoiding per-step `byte[]` allocation, `LMDBTrieIterator` still runs each key through `QuadKeyEncoding.decode*` which reads four varints and reconstructs a full `QuadKey` representation into `currentS/currentP/currentO/currentC`. For LFTJ, at recursion depth `x_i` we only need a single component per iterator – the slot (S, P, O, or C) that the current variable is joining on – plus enough information to enforce any bound constants and previously bound variables in the pattern prefix.

The goal of this plan is to make each `LMDBTrieIterator` behave more like a classic TrieArray iterator: at depth 1 you step through values of attribute 1 only; at depth 2 you index into the slice for the fixed prefix and step through attribute 2, and so on. Concretely, we will redesign key decoding so that, for each LMDB cursor position, we varint-decode only:

- The components that are actually constrained by the prefix for this pattern and depth, and
- The single component that serves as the iterator’s `key()` (the slot we are joining on),

and never decode “baggage” components that are neither constrained nor needed for the join key. Correctness (which quads are considered) and observable results of LFTJ must remain unchanged; only CPU work per step and micro-benchmark throughput should change.

A user will experience this as “the same query results as before, but LFTJ-based LMDB queries and benchmarks run faster,” demonstrable via `LMDBTrieIteratorBenchmark` and `LmdbCliqueBenchmark` before and after the change.

## Progress

Use this section to record concrete steps as they are completed. Timestamps are in UTC.

- [x] (2025-11-25 00:00Z) Draft initial ExecPlan describing partial decode approach for `LMDBTrieIterator`.
- [ ] Implement partial-decode logic in `LMDBTrieIterator` and related helpers in `QuadKeyEncoding` (if needed).
- [ ] Add/adjust unit tests for `LMDBTrieIterator` to cover prefix shapes and slot combinations under the new decode path.
- [ ] Re-run LFTJ correctness tests and LMDB-related tests in `core/sail/lmdb` to confirm unchanged behavior.
- [ ] Run `LMDBTrieIteratorBenchmark` and `LmdbCliqueBenchmark` before/after to measure the improvement and capture results.
- [ ] Update this ExecPlan with outcomes, surprises, and any design adjustments after implementation.

## Surprises & Discoveries

Document unexpected behaviors or insights as they arise; seed this with the key finding behind the plan.

- Observation: `LMDBTrieIterator.loadCurrentKey` decodes all four quad components from every key using `QuadKeyEncoding.decodeInto`, even when only one component is needed for the join and a small subset of components are constrained by the prefix.
  Evidence:

      core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/lftj/LMDBTrieIterator.java:loadCurrentKey

      private void loadCurrentKey() {
          ByteBuffer buffer = keyVal.mv_data();
          int len = (int) keyVal.mv_size();
          buffer.limit(len);
          buffer.position(0);
          QuadKeyEncoding.decodeInto(buffer, order, this);
      }

  This always routes through a `QuadKeyDecoder` that reads four varints and calls `sink.set(s, p, o, c)`, then rewinds the buffer position.

- Observation: `matchesPrefix()` only inspects fields that are actually present in the `Prefix` for this iterator; unconstrained components are currently still decoded but ignored.
  Evidence:

      core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/lftj/LMDBTrieIterator.java:matchesPrefix

      private boolean matchesPrefix() {
          if (prefix.hasSubject() && currentS != prefix.subject()) { return false; }
          if (prefix.hasPredicate() && currentP != prefix.predicate()) { return false; }
          if (prefix.hasObject() && currentO != prefix.object()) { return false; }
          if (prefix.hasContext() && currentC != prefix.context()) { return false; }
          return true;
      }

This makes it safe to switch to “decode only constrained slots + join slot” semantics as long as we keep those `current*` fields correct when needed and avoid reading components that are never compared.

## Decision Log

Record the main design decisions that shape the implementation.

- Decision: Optimize decoding by adding a prefix- and slot-aware partial decode path inside `LMDBTrieIterator` (driven by `QuadKeyOrder.positions()` and `Prefix`) rather than changing the LMDB key layout or introducing a completely new TrieArray storage format.
  Rationale: LMDB already stores quad keys as a varint-encoded sequence of four term IDs in one of 24 possible orders; LFTJ is wired against these indexes and their ordering. Replacing the storage layout would be a multi-module refactor with migration concerns. A targeted change that decodes only the necessary varints per iterator step preserves all existing indexing and LFTJ logic, touches a small surface area (`LMDBTrieIterator` and possibly `QuadKeyEncoding`), and directly addresses the wasted CPU work.
  Date/Author: 2025-11-25 / Codex agent

- Decision: Keep `QuadKeyEncoding.decodeInto` as the “full decode” utility but remove its use from the hot path in `LMDBTrieIterator`, replacing it with a local, order-driven partial decode that only reads as many varints as are actually required for this iterator.
  Rationale: Other callers (if any) can continue to rely on `decodeInto` semantics; `LMDBTrieIterator` can be the optimized special case since it knows the active slot and prefix. This keeps the encoding/decoding utility general while allowing the LMDB LFTJ iterator to be highly specialized.
  Date/Author: 2025-11-25 / Codex agent

## Outcomes & Retrospective

This section should be updated after implementation and measurement.

Expected outcomes at completion:

- `LFTJExecutionTest`, `LFTJDeterministicCorrectnessTest`, `LMDBTrieIteratorTest`, and other LMDB/LFTJ tests in `core/sail/lmdb` all pass unchanged, demonstrating that join semantics and result sets are identical to baseline.
- `LMDBTrieIteratorBenchmark.iteratePrefix` and `.seekWithinPrefix` complete successfully and show a measurable throughput improvement (for example, a noticeable reduction in time per operation or an increase in operations per millisecond) when comparing pre- and post-change JMH runs for typical entry counts (1,000 and 100,000).
- Higher-level benchmarks that exercise worst-case optimal joins on LMDB data (e.g., `LmdbCliqueBenchmark`) show a reduction in CPU time or an increase in throughput that correlates with the reduced per-step decoding work.

After implementation, summarize here:

- What changed in practice.
- Where the biggest wins were observed (which benchmarks, which patterns).
- Any regressions or trade-offs discovered (and how they were handled).

## Context and Orientation

This section explains the relevant parts of the repository and terminology for a novice.

The LMDB-backed LFTJ implementation lives under:

- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/lftj/`

Key classes:

- `QuadKey`: a simple value object that holds four `long` IDs (subject, predicate, object, context) representing a quad in the value store.
- `Slot`: an enum with values `S`, `P`, `O`, `C` indicating which quad component a particular position or iterator slot refers to.
- `QuadKeyOrder`: an enum describing one of the 24 permutations of `(S, P, O, C)`, e.g. `SPOC`, `POSC`, etc. This tells LMDB and the encoder which component appears first, second, third, and fourth in the key. It exposes `positions()` (the slots in order), `indexOf(Slot)`, and an internal `QuadKeyEncoding.QuadKeyDecoder`/`QuadKeyEncoder`.
- `QuadKeyEncoding`: utility methods to encode and decode quad keys. It uses `Varint` to varint-encode four `long` values in a fixed sequence into a `ByteBuffer` (for LMDB keys) or `byte[]`. It currently exposes:
  - `encode(QuadKey, QuadKeyOrder)`, `encodeInto(QuadKey, QuadKeyOrder, ByteBuffer)`, and `encodeFieldsInto(long s, long p, long o, long c, QuadKeyOrder, ByteBuffer)`.
  - `decode(byte[] bytes, QuadKeyOrder)` and `decode(ByteBuffer buffer, QuadKeyOrder)` which produce a full `QuadKey`.
  - `decodeInto(ByteBuffer buffer, QuadKeyOrder order, QuadKeySink sink)` which uses an order-specific decoder to read four varints, call `sink.set(s, p, o, c)`, and then reset the buffer position.
- `Prefix` and `PrefixBuilder`: a `Prefix` encapsulates which of the four slots are currently fixed (as constants or previously bound variables) for a given pattern at a particular depth. `PrefixBuilder.buildPrefix` computes this from a `QuadPattern`, the global variable order, the current variable index, and the current bindings map.
- `TrieIterator` / `CloseableTrieIterator`: an abstraction over trie-like iteration that LFTJ uses. `LMDBTrieIterator` is the LMDB-backed implementation and implements `QuadKeyEncoding.QuadKeySink`.
- `LMDBTrieIterator`: wraps an LMDB cursor (`mdb_cursor_open`/`mdb_cursor_get`) and presents it as a `CloseableTrieIterator`. It has fields for the DBI (index handle), `QuadKeyOrder`, `Slot role` (the slot it exposes via `key()`), a `Prefix`, and four `long` fields `currentS/currentP/currentO/currentC` that are set on each cursor move by decoding the LMDB key.
  - `open(Prefix prefix)` positions the cursor at the first key >= the varint-encoded minimal key for the prefix (`QuadKeyEncoding.minimalKeyForPrefix` + `encodeFieldsInto`) and then calls `loadCurrentKey()` followed by `matchesPrefix()` to ensure the starting key matches.
  - `next()` uses `mdb_cursor_get(..., MDB_NEXT)` in a loop, calls `loadCurrentKey()` each time, filters via `matchesPrefix()`, and returns upon the next matching join key.
  - `seek(long value)` uses a per-slot `LongConsumer` to reposition the cursor to the next key with at least the given slot value, again via `positionCursor(...)` and `loadCurrentKey()`.
  - `key()` returns `currentValue()`, which is `currentS` / `currentP` / `currentO` / `currentC` depending on the iterator’s `role`.
- `LFTJExecutor`: orchestrates the Leapfrog Triejoin over multiple `CloseableTrieIterator` instances, one per `(pattern, variable)` pair, using the `IteratorPool` to reuse `LMDBTrieIterator` instances. It asks a `TrieIteratorProvider` (by default creating `LMDBTrieIterator`) for iterators, sets prefixes, and then calls a `leapfrog` routine to intersect their value streams.

Relevant tests and benchmarks:

- `core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/lftj/LMDBTrieIteratorTest.java`: correctness tests for `LMDBTrieIterator`.
- `core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/lftj/LFTJExecutionTest.java`, `LFTJDeterministicCorrectnessTest.java`, and other LFTJ tests validate the join behavior on LMDB-backed data.
- `testsuites/benchmark/src/main/java/org/eclipse/rdf4j/benchmark/lmdb/lftj/LMDBTrieIteratorBenchmark.java`: JMH benchmarks focused on basic prefix iteration and seeking using `LMDBTrieIterator`.
- `core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/benchmark/LmdbCliqueBenchmark.java`: JMH benchmark that exercises worst-case optimal joins on LMDB data to compare LFTJ with the standard join strategy.

Important environment assumptions:

- All Maven commands should be run from the repository root (`/Users/havardottestad/Documents/Programming/hasmac-rdf4j` in this setup) with `-Dmaven.repo.local=.m2_repo`.
- Always perform a quick install before tests: `mvn -o -Dmaven.repo.local=.m2_repo -Pquick clean install | tail -200`.

## Plan of Work

This section describes the sequence of edits and design steps needed to implement the optimization.

1. **Establish a correctness and performance baseline.**

   Start by confirming that the current state of `core/sail/lmdb` is correct and by measuring the existing performance of `LMDBTrieIterator`:

   - From the repo root, run a quick install to prime the local Maven repo (offline if possible).
   - Run the module-level tests for `core/sail/lmdb` to ensure nothing is broken at the outset.
   - Run `LMDBTrieIteratorBenchmark` via the provided `scripts/run-single-benchmark.sh` helper (see “Concrete Steps”) and persist the results (iterations per millisecond or similar) as the baseline to compare against after the optimization.
   - Optionally run `LmdbCliqueBenchmark` or other LMDB LFTJ-related benchmarks to get a more end-to-end picture of performance.

   This step ensures that any future test failures or benchmark regressions can be attributed to the changes introduced by this plan.

2. **Analyze the current decode responsibilities per iterator slot and prefix.**

   For each `LMDBTrieIterator`, understand exactly which components are required at each step:

   - The iterator has a fixed `QuadKeyOrder order` and `Slot role`. The `order.positions()` method returns an array of four `Slot` values indicating which component appears at each varint position in the LMDB key.
   - The `Prefix prefix` describes which slots are fixed to specific `long` values for this pattern and depth (constants and previously bound variables). `prefix.hasSubject()`, `prefix.hasPredicate()`, etc. indicate which slots must match; `prefix.subject()`, `prefix.predicate()`, etc. give the values.
   - `key()` is defined to return the current value of the iterator’s `role` slot via `currentValue()`, which currently reads from the appropriate `current*` field.

   From this, derive that for each LMDB key the iterator only needs to decode:

   - For every slot `slot` where `prefix.has*` is true (subject, predicate, object, context), the varint representing that slot must be decoded to enforce `matchesPrefix()`.
   - For the iterator’s `role` slot, the varint representing that slot must be decoded so that `currentValue()` returns the correct join key.
   - No other components need to be decoded for correctness, because they are neither part of the prefix nor used as the iterator’s current key.

   Note that some slots may satisfy both conditions (for example, a slot that is a constant in the pattern and is also the `role`), but that doesn’t change what we decode, only that we write the same decoded value into `current*`.

3. **Design a per-iterator “decode plan” driven by order, prefix, and slot.**

   Define a small piece of per-iterator state that tells `LMDBTrieIterator` exactly which varint positions it must decode for each key. The plan should be computed when the iterator is opened (or when the prefix changes) and reused for every subsequent `loadCurrentKey()`:

   - When `LMDBTrieIterator` is constructed, store `order.positions()` into a local array (for example, `Slot[] decodeOrder = order.positions().toArray(new Slot[0]);`) to avoid repeated allocations.
   - Define four booleans indicating whether each slot is needed:
     - `needSubject = prefix.hasSubject() || role == Slot.S`
     - `needPredicate = prefix.hasPredicate() || role == Slot.P`
     - `needObject = prefix.hasObject() || role == Slot.O`
     - `needContext = prefix.hasContext() || role == Slot.C`
   - Map these booleans to the decode order: for each index `i` in `0..3`, let `slot = decodeOrder[i]`. For this position, decoding is needed if `slot` is S and `needSubject`, or `slot` is P and `needPredicate`, etc.
   - Compute `maxNeededIndex` as the highest index `i` such that the slot at `decodeOrder[i]` must be decoded. There will always be at least one such index because `role` is always required.
   - Build a small decode plan, e.g. an `int[] decodeUpToIndex = { maxNeededIndex }` plus a compact representation of which slots to write into `currentS/currentP/currentO/currentC` and which to decode and discard. For simplicity, the implementation can derive this directly in the decode loop without storing extra arrays, as long as it uses `maxNeededIndex` to limit how far it reads.

   This decode plan expresses, for this iterator and current prefix, “how many varints do we need to read from this LMDB key and which decoded values must be stored.”

4. **Implement partial decode in `LMDBTrieIterator` based on the decode plan.**

   Replace the call to `QuadKeyEncoding.decodeInto` in `LMDBTrieIterator.loadCurrentKey()` with a hand-rolled partial decode that uses the decode plan:

   - Keep the initial buffer setup:

       - Get the LMDB key buffer via `ByteBuffer buffer = keyVal.mv_data();`.
       - Set its limit to `len = (int) keyVal.mv_size();` and its position to `0`.

   - Use `decodeOrder` and `maxNeededIndex` to drive decoding:

       - Initialize the buffer position to zero (already done).
       - For `i` from `0` to `maxNeededIndex` inclusive:
         - Read a varint using `long value = Varint.readUnsigned(buffer);`.
         - Determine the slot at this position: `Slot slot = decodeOrder[i];`.
         - If this slot is subject and `needSubject` is true, assign `currentS = value`.
         - If this slot is predicate and `needPredicate` is true, assign `currentP = value`.
         - If this slot is object and `needObject` is true, assign `currentO = value`.
         - If this slot is context and `needContext` is true, assign `currentC = value`.
         - If the slot is not needed (no prefix constraint on it and it is not the iterator’s role), simply discard `value` and move on.
       - Do not read varints for indices greater than `maxNeededIndex`. This means that the buffer position after the loop may not be at the limit; that is acceptable for our purposes because we never use the buffer again before the next LMDB cursor call, and we no longer need to verify “trailing bytes” at this point.

   - Remove or bypass the previous use of `QuadKeyEncoding.decodeInto(buffer, order, this)` and the implicit requirement that the buffer be fully consumed for each key.

   - Ensure that `matchesPrefix()` and `currentValue()` remain unchanged: they still read from `currentS/currentP/currentO/currentC`, but those fields will now only be populated for slots that actually need them. All other `current*` fields can have stale values; `matchesPrefix()` guards each use with a corresponding `prefix.has*` check.

   - Ensure the decode plan is updated whenever the prefix changes. The simplest approach is to recompute `needSubject`/`needPredicate`/`needObject`/`needContext` and `maxNeededIndex` inside `open(Prefix prefix)` (after assigning `this.prefix = prefix`) so that subsequent calls to `loadCurrentKey` use an up-to-date plan.

   This step is where the core CPU savings come from: for patterns that constrain only one or two slots and where the join slot appears early in the order, `loadCurrentKey` will decode correspondingly fewer varints.

5. **Consider optional refinements to make prefix shapes friendlier to partial decode.**

   The partial decode scheme above still needs to decode every constrained slot, even if those constraints fall after the join slot in the index order. For example, with a `SPOC` order, a pattern that constrains `C` but joins on `S` will still have `maxNeededIndex = 3`, so the decoder must read S, P, O, C.

   To push performance further, consider, as a follow-on refinement (not strictly required for the initial implementation):

   - Examining `IndexSelector`’s scoring (`IndexSelector.compatibilityScore`) to favor index orders where constants and earlier variables appear as early as possible in the key, so that `maxNeededIndex` tends to be small.
   - Optionally adding a check or logging to detect patterns where no available index can satisfy “all constrained slots appear at or before the join slot” and deciding whether that is acceptable or whether to warn users or require more indexes.

   For this initial ExecPlan, keep this as an optional enhancement; the main optimization is achieved purely by decoding fewer varints for the common case where constraints and join slots are early in the key.

6. **Update and extend tests around `LMDBTrieIterator`.**

   Adjust existing tests and add new ones to ensure that the new partial decode behavior maintains correctness across a representative set of patterns and prefixes:

   - Ensure `LMDBTrieIteratorTest` still covers basic iteration and seeking for simple prefixes (e.g., predicate-only prefixes in `SPOC` order).
   - Add new test cases where:
     - Prefix constrains only the join slot (e.g., subject bound, iterating on `Slot.S`).
     - Prefix constrains a slot before the join slot (e.g., predicate fixed, joining on object).
     - Prefix constrains a slot after the join slot (to exercise the path where `maxNeededIndex` goes beyond the join slot).
     - Multiple slots are constrained simultaneously.
   - For each test case, build a small LMDB database in-memory or on disk using `QuadKeyEncoding.encode` and verify that the iterator yields exactly the expected sequence of join values and respects prefix constraints.
   - Ensure all existing LFTJ correctness tests that depend on LMDB indexes (`LFTJExecutionTest`, `LFTJDeterministicCorrectnessTest`, etc.) still pass, confirming there is no behavioral change at the join level.

   The focus of these tests is to confirm that the decode plan logic correctly populates `currentS/currentP/currentO/currentC` for all relevant combinations and that `matchesPrefix()` continues to function as before.

7. **Re-run benchmarks and document improvements.**

   With correctness validated, re-run `LMDBTrieIteratorBenchmark` and any relevant higher-level benchmarks:

   - Using the same parameters as the baseline (e.g., orders, entry counts, and prefixes), run `LMDBTrieIteratorBenchmark.iteratePrefix` and `.seekWithinPrefix` and record the results.
   - Optionally, run `LmdbCliqueBenchmark` and other LMDB-related benchmarks that exercise LFTJ for real SPARQL workloads.
   - Compare results to the baseline and summarize the improvements (e.g., “iteratePrefix throughput increased by 40% for 100,000 entries in SPOC order; seekWithinPrefix latency decreased by X%”).

   Capture these observations both in this ExecPlan (in `Outcomes & Retrospective`) and, if appropriate, in the project’s documentation or benchmark notes.

8. **Polish, format, and finalize.**

   Once tests and benchmarks are satisfactory:

   - Ensure the code is formatted according to repository standards via the formatter profile.
   - Re-run `mvn -o -Dmaven.repo.local=.m2_repo -Pquick clean install` to confirm a clean build.
   - Re-run any narrowly targeted module tests if needed.
   - Update this ExecPlan’s `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` sections to reflect the final state and lessons learned.

   At this point, an engineer (or another agent) should be able to follow this plan and the resulting code to understand the change and repeat the validation.

## Concrete Steps

This section gives concrete commands and where to run them. Adjust paths if your working directory differs.

1. **Baseline build and tests.**

   From the repository root:

       mvn -o -Dmaven.repo.local=.m2_repo -Pquick clean install | tail -200

   Then run the LMDB module tests:

       mvn -o -Dmaven.repo.local=.m2_repo -pl core/sail/lmdb verify | tail -500

2. **Baseline LMDBTrieIterator benchmarks.**

   Use the benchmark harness described in `AGENTS.md`:

   - For iteration:

         scripts/run-single-benchmark.sh \
           --module testsuites/benchmark \
           --class org.eclipse.rdf4j.benchmark.lmdb.lftj.LMDBTrieIteratorBenchmark \
           --method iteratePrefix

   - For seeking:

         scripts/run-single-benchmark.sh \
           --module testsuites/benchmark \
           --class org.eclipse.rdf4j.benchmark.lmdb.lftj.LMDBTrieIteratorBenchmark \
           --method seekWithinPrefix

   Capture the reported throughput numbers and, if JFR is enabled with `--enable-jfr`, note where the JFR file is written.

3. **Implement partial decode in LMDBTrieIterator.**

   Edit `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/lftj/LMDBTrieIterator.java`:

   - Introduce fields (or local variables computed in `open`) for:
     - The decode order: `Slot[] decodeOrder`.
     - Booleans `needSubject`, `needPredicate`, `needObject`, `needContext`.
     - An integer `maxNeededIndex`.
   - In `open(Prefix prefix)`, after `this.prefix = prefix;`, compute the `need*` booleans and `maxNeededIndex` based on `order.positions()` and `role`.
   - Replace the body of `loadCurrentKey()` with the new partial decode loop described in “Plan of Work” step 4, using `Varint.readUnsigned` and writing into `currentS/currentP/currentO/currentC` only when required.

4. **Update tests.**

   Re-run LMDB tests and adjust/add test cases as needed:

       mvn -o -Dmaven.repo.local=.m2_repo -pl core/sail/lmdb -Dtest=LMDBTrieIteratorTest verify | tail -500

   Once `LMDBTrieIteratorTest` is stable, run the full module tests again:

       mvn -o -Dmaven.repo.local=.m2_repo -pl core/sail/lmdb verify | tail -500

5. **Re-run benchmarks.**

   Repeat the benchmark commands from step 2 and compare output to the baseline. Optionally, run the clique benchmark:

       scripts/run-single-benchmark.sh \
         --module core/sail/lmdb \
         --class org.eclipse.rdf4j.sail.lmdb.benchmark.LmdbCliqueBenchmark \
         --method cliqueQuery

   (Use the actual benchmark method name as declared in `LmdbCliqueBenchmark`.)

6. **Formatting and final verification.**

   Before concluding:

       mvn -o -Dmaven.repo.local=.m2_repo -q -T 2C formatter:format impsort:sort xml-format:xml-format
       mvn -o -Dmaven.repo.local=.m2_repo -Pquick clean install | tail -200

   Confirm there are no new test failures.

## Validation and Acceptance

To accept the implementation as complete, verify the following:

- **Correctness:**
  - All tests in `core/sail/lmdb` pass after the changes, including but not limited to:
    - `LMDBTrieIteratorTest`
    - `LFTJExecutionTest`
    - `LFTJDeterministicCorrectnessTest`
    - Any LFTJ-related ITs or other tests that rely on LMDB indexes.
  - For specifically constructed test cases where prefixes constrain various combinations of S, P, O, C and the iterator’s `role` varies, `LMDBTrieIterator` yields the same sequences of `key()` values as before, and `atEnd()` transitions at the same logical points.

- **Performance:**
  - `LMDBTrieIteratorBenchmark.iteratePrefix` and `.seekWithinPrefix` show equal or better throughput for the default configurations (e.g., order `spoc`, entry counts `1000` and `100000`).
  - Any regression in a particular configuration should be investigated; if unavoidable, it should be documented here with an explanation.

- **Stability:**
  - No changes are needed to the `TrieIterator` or `CloseableTrieIterator` interfaces; callers do not need to know about the new decode behavior.
  - `QuadKeyEncoding` remains usable for any existing callers, with `decodeInto` still available as a full decode utility even though `LMDBTrieIterator` no longer calls it.

If all of the above hold, the optimization is considered accepted.

## Idempotence and Recovery

The steps described in this ExecPlan are additive and local to the LMDB LFTJ implementation:

- You can re-run the build and test commands as many times as needed; they are idempotent.
- The changes are confined to Java code under `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/lftj/`. If something goes wrong, you can revert those files to the previous commit (using standard Git commands) and re-run the tests to restore the baseline.
- Benchmarks are read-only against their temporary LMDB environments; they create and tear down their own temporary directories and databases in `LMDBTrieIteratorBenchmark`, so running them repeatedly is safe.

If you partially implement the decode plan and encounter test failures:

- Re-check the mapping from `QuadKeyOrder.positions()` to `Slot` and verify you are decoding and assigning components according to that mapping.
- Add temporary assertions or logging around `loadCurrentKey`, `matchesPrefix`, and `key()` to confirm that for each key:
  - Only needed slots are decoded.
  - `current*` values used in `matchesPrefix` and `currentValue` match expectations.

Once issues are resolved, remove temporary instrumentation and re-run the tests and benchmarks.

## Artifacts and Notes

Key code locations to consult while implementing this plan:

- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/lftj/LMDBTrieIterator.java`:
  - `open(Prefix prefix)`
  - `positionCursor(...)`
  - `loadCurrentKey()`
  - `matchesPrefix()`
  - `key()` / `currentValue()`
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/lftj/QuadKeyOrder.java`:
  - `positions()`
  - `indexOf(Slot)`
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/lftj/QuadKeyEncoding.java`:
  - `encodeFieldsInto(...)`
  - `decodeInto(...)` (reference for how varint decoding currently works).
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/lftj/Prefix.java` and `PrefixBuilder.java`:
  - For how prefixes are computed from patterns and bindings.

Keep brief notes here if you introduce helper methods or refactorings during implementation that are not explicitly listed above, so future readers have a map of where to look.

## Interfaces and Dependencies

At the end of this ExecPlan’s implementation, the relevant interfaces and dependencies should look conceptually as follows:

- `CloseableTrieIterator` remains unchanged:

    public interface CloseableTrieIterator extends TrieIterator, Closeable {
        @Override
        void close();
        Slot slot();
        default int slotDbi() { ... }
        default QuadKeyOrder slotOrder() { ... }
    }

- `LMDBTrieIterator` continues to implement `CloseableTrieIterator` and `QuadKeyEncoding.QuadKeySink`, but its internal key decoding logic uses a partial decode based on `QuadKeyOrder.positions()` and `Prefix`. Externally, callers still see:

    public class LMDBTrieIterator implements CloseableTrieIterator, QuadKeyEncoding.QuadKeySink {
        public LMDBTrieIterator(long txn, int dbi, QuadKeyOrder order, Slot role) throws IOException { ... }
        @Override public void open(Prefix prefix) { ... }
        @Override public boolean atEnd() { ... }
        @Override public long key() { ... }
        @Override public void next() { ... }
        @Override public void seek(long value) { ... }
        @Override public void close() { ... }
        @Override public Slot slot() { ... }
        @Override public int slotDbi() { ... }
        @Override public QuadKeyOrder slotOrder() { ... }
        @Override public void set(long s, long p, long o, long c) { ... } // used only if needed.
    }

- `QuadKeyEncoding` still exposes the existing encode/decode methods and may optionally gain helper methods if you decide to encapsulate some of the partial decode logic in a reusable way. The key requirement is that any new helpers do not change existing public signatures in backward-incompatible ways.

No new external dependencies (libraries) should be required; everything can be implemented using the existing `Varint` and LMDB bindings.

---

2025-11-25: Initial version of this ExecPlan created by Codex agent to guide optimization of LMDB LFTJ iterators toward decoding only necessary quad components per step.
