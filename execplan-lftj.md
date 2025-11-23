# ExecPlan: Make the LMDB LFTJ implementation competitive with reference implementations

> Goal: Turn the current LMDB‑backed Leapfrog Triejoin into a high‑performance engine comparable to canonical LFTJ implementations (Veldhuizen, triangle listing, Datalog materialisation), without breaking RDF4J semantics or LMDB safety.

---

## 0. Scope, constraints, and success criteria

### 0.1 Scope

We’re optimizing the code under `org.eclipse.rdf4j.sail.lmdb.lftj`:

* `LFTJExecutor`
* `LeapfrogIteratorCursor`
* `LMDBTrieIterator`
* Supporting classes (`IndexSelector`, `PrefixBuilder`, `LmdbWCOJ*`)

We **do not** change:

* RDF4J public APIs.
* LMDB transactional semantics (no long‑lived write transactions, etc.).

### 0.2 Constraints

* Must remain thread‑safe w.r.t. how RDF4J uses LMDB (e.g. read‑only snapshot per query).
* No semantic changes to query results.
* Garbage creation in the hot path must be minimized.
* JNI calls to LMDB must be minimized or at least have efficient amortised cost.

### 0.3 Success metrics

Define success in measurable terms:

* For a set of representative SPARQL queries (star joins, path queries, snowflakes, dense joins):

    * **≥ 3× speedup** vs current implementation on large datasets (e.g. ≥ 10M quads).
    * GC pressure noticeably reduced (e.g. “Allocated bytes per op” down by ≥ 50% in JMH microbenchmarks for the hot iterators).
    * No regressions in existing unit tests or RDF4J compliance tests.

---

## 1. Build a minimal but solid performance harness

Before changing code, establish a repeatable way to measure:

### 1.1 Microbench for `LMDBTrieIterator`

Create a JMH benchmark module (e.g. `rdf4j-sail-lmdb-lftj-bench`):

* Populate an LMDB store with a synthetic quad index (e.g. SPOC).
* Benchmark `LMDBTrieIterator` alone:

    * `next()` throughput under a fixed prefix.
    * `seek()` patterns typical for leapfrog (jumping forward to a moving max).
    * Measure:

        * Ops/sec,
        * Allocations/op,
        * GC time.

Classes to add:

* `org.eclipse.rdf4j.sail.lmdb.lftj.bench.LMDBTrieIteratorBench`

    * Uses a fixed LMDB environment and snapshot.
    * Runs benchmarks for different cardinalities and prefix shapes.

### 1.2 Microbench for set intersection

* Benchmark `LeapfrogIteratorCursor` vs your current `LFTJExecutor.leapfrog` (with in‑memory `TrieIterator` mocks):

    * Implement `MockTrieIterator` backed by sorted `long[]` arrays.
    * Measure the time to intersect `k` arrays with:

        * `LeapfrogIteratorCursor`
        * A reimplementation of your current `leapfrog` logic.

### 1.3 End‑to‑end benchmark: LFTJ vs baseline joins

* Add a small benchmark harness that runs RDF4J queries against:

    * LFTJ path (via `LmdbWCOJ` optimizer).
    * Baseline join plan (rebuild join tree and evaluate via default strategy).

Use:

* A couple of synthetic datasets (e.g. social network‑like, chain graph, star).
* A couple of real datasets if available (e.g. LUBM, BSBM).

This gives you a “before” snapshot and protects against regressions later.

---

## 2. Use `LeapfrogIteratorCursor` in `LFTJExecutor` (fix the algorithmic mismatch)

### 2.1 Replace `LFTJExecutor.leapfrog` with cursor‑based variant

Right now `LFTJExecutor` has its own `leapfrog` that sorts inside the loop. Preferred design:

* `LeapfrogIteratorCursor` *is* your LFJ implementation.
* `LFTJExecutor` just wraps it.

Implementation plan:

1. Introduce a new method in `LFTJExecutor`:

   ```java
   private void leapfrog(List<LMDBTrieIterator> iterators, LongConsumer consumer) {
       LeapfrogIteratorCursor cursor = new LeapfrogIteratorCursor(iterators);
       while (cursor.hasValue()) {
           consumer.accept(cursor.current());
           cursor.advance();
       }
       // Optionally: assert !cursor.sawStalledSeek() in debug builds.
   }
   ```

2. Remove or deprecate the existing `leapfrog` implementation to avoid confusion.

3. Ensure `LeapfrogIteratorCursor` is robust:

    * It already sorts iterators once in the constructor.
    * It detects “non‑advancing seek” cases; keep that behaviour.

### 2.2 Tests

* Add tests that compare:

    * `LFTJExecutor.leapfrog` (new) vs a simple in‑memory intersection function for random arrays.
    * Ensure that `LeapfrogIteratorCursor` plus `MockTrieIterator` matches expected results for:

        * Non‑overlapping ranges.
        * Small overlaps.
        * Fully identical arrays.

---

## 3. Make LMDBTrieIterator hot path allocation‑free

Objective: match the reference implementations that do *no* allocation in `next()`/`seek()`.

### 3.1 Eliminate per‑step `byte[]` allocation in `loadCurrentKey`

Change from:

```java
byte[] keyBytes = new byte[(int) keyVal.mv_size()];
buffer.get(keyBytes);
buffer.rewind();
currentKey = QuadKeyEncoding.decode(keyBytes, order);
```

to something like:

1. Add a reusable buffer:

   ```java
   private final byte[] keyBytes = new byte[MAX_ENCODED_KEY_LENGTH];
   ```

2. Use it in `loadCurrentKey`:

   ```java
   private void loadCurrentKey() {
       ByteBuffer buffer = keyVal.mv_data();
       int len = (int) keyVal.mv_size();
       buffer.get(keyBytes, 0, len);
       buffer.rewind();
       currentKey = QuadKeyEncoding.decode(keyBytes, 0, len, order);
   }
   ```

3. Extend `QuadKeyEncoding` with a `decode(byte[], int, int, QuadKeyOrder)` overload to avoid extra array copies.

OR, even better:

### 3.2 Avoid allocating QuadKey on every step

Right now `currentKey` is an immutable `QuadKey`:

```java
private QuadKey currentKey;
```

To make this leaner:

1. Replace `QuadKey currentKey` with primitive fields:

   ```java
   private long currentS;
   private long currentP;
   private long currentO;
   private long currentC;
   ```

2. Change `QuadKeyEncoding.decode(...)` to fill these fields directly:

    * E.g., a variant returning a small struct or writing into an interface:

      ```java
      public static void decodeInto(byte[] keyBytes, int off, int len,
                                    QuadKeyOrder order, QuadKeySink sink);
 
      public interface QuadKeySink {
          void set(long s, long p, long o, long c);
      }
      ```

    * `LMDBTrieIterator` can implement `QuadKeySink` and store into its fields.

3. Update `key()`:

   ```java
   @Override
   public long key() {
       if (end) throw new IllegalStateException("Iterator is at end");
       switch (role) {
           case S: return currentS;
           case P: return currentP;
           case O: return currentO;
           case C: return currentC;
           default: throw new IllegalStateException("Unexpected slot: " + role);
       }
   }
   ```

This removes `QuadKey` allocations entirely from the hot path.

### 3.3 Verify with microbench

* Use JMH to verify allocations/op before vs after.
* Expect `LMDBTrieIterator.next()` / `seek()` to go from “some allocations” to ~0.

---

## 4. Fix LMDB cursor lifecycle & reduce churn

### 4.1 Fix the cursor leak on early‑end `open(prefix)`

Currently:

```java
LMDBTrieIterator iterator = new LMDBTrieIterator(txn, dbi.intValue(), order, slot);
iterator.open(prefix);
if (iterator.atEnd()) {
    return;
}
iterators.add(iterator);
```

Leak: if `atEnd()` is true, the iterator is never closed.

Change to:

```java
LMDBTrieIterator iterator = new LMDBTrieIterator(txn, dbi.intValue(), order, slot);
iterator.open(prefix);
if (iterator.atEnd()) {
    iterator.close();
    return;
}
iterators.add(iterator);
```

Add a regression test:

* Build a tiny LMDB index with no matches for some prefix.
* Run a query that triggers this path.
* Ensure number of open cursors doesn’t grow over repeated evaluation (instrument with logging or LMDB stats).

### 4.2 Introduce iterator pooling / reuse

Goal: avoid `mdb_cursor_open/close` per prefix.

Design:

1. Introduce a factory/pool:

   ```java
   final class LMDBTrieIteratorPool {
       // keyed by (dbi, order, role)
       private final Map<Key, Deque<LMDBTrieIterator>> pool = new HashMap<>();

       LMDBTrieIterator acquire(long txn, int dbi, QuadKeyOrder order, Slot role) { ... }
       void release(LMDBTrieIterator it) { ... } // call close() internally or keep cursor open
   }
   ```

   But note: cursors are tied to a transaction; we must ensure that:

    * Pool is per query / per snapshot (per LMDB transaction).
    * We don’t reuse cursors across different transactions.

2. In `LFTJExecutor`, construct a pool per `evaluate` call:

   ```java
   LMDBTrieIteratorPool pool = new LMDBTrieIteratorPool();
   recurse(..., pool, ...);
   ```

3. In `recurse`, replace direct construction:

   ```java
   LMDBTrieIterator iterator = pool.acquire(txn, dbi, order, slot);
   iterator.open(prefix);
   if (iterator.atEnd()) {
       pool.release(iterator);
       return;
   }
   iterators.add(iterator);
   ```

4. In `finally`:

   ```java
   for (LMDBTrieIterator iterator : iterators) {
       pool.release(iterator);
   }
   ```

Option A: `release` actually closes the cursor but reuses Java object + buffers.

Option B: Keep `mdb_cursor_open` cursors alive until the query finishes, then close all in one sweep.

Implementation detail:

* Track per‑query resources so that `LFTJExecutor.evaluate` can close any remaining cursors when done.

### 4.3 Measure impact

* Use microbench + end‑to‑end benchmark to confirm:

    * Fewer calls to `mdb_cursor_open/close`.
    * Lower wall‑clock time for deep joins.

---

## 5. Precompute per‑variable participation & variable index

Reduce overhead in `recurse`, `PrefixBuilder`, and `IndexSelector`.

### 5.1 Precompute variable → patterns map

Right now `patternsWithVariable` scans the whole pattern list:

```java
for (QuadPattern pattern : patterns) {
    if (pattern.variables().contains(variable)) {
        participating.add(pattern);
    }
}
```

Change to:

1. When starting evaluation, build:

   ```java
   private static Map<String, List<QuadPattern>> groupByVariable(List<QuadPattern> patterns) {
       Map<String, List<QuadPattern>> byVar = new HashMap<>();
       for (QuadPattern pattern : patterns) {
           for (String var : pattern.variables()) {
               byVar.computeIfAbsent(var, v -> new ArrayList<>()).add(pattern);
           }
       }
       return byVar;
   }
   ```

2. Store `patternsByVar` in `LFTJExecutor.evaluate` and pass it into `recurse`.

3. Replace `patternsWithVariable` call with a simple map lookup:

   ```java
   List<QuadPattern> participating = patternsByVar.get(variable);
   if (participating == null || participating.isEmpty()) {
       throw new IllegalArgumentException("Variable not found in any pattern: " + variable);
   }
   ```

### 5.2 Precompute variable index map

`PrefixBuilder` currently uses `variableOrder.indexOf(variable)` for each term; that’s O(#vars).

At evaluation start:

```java
Map<String, Integer> variableIndex = new HashMap<>();
for (int i = 0; i < variableOrder.size(); i++) {
    variableIndex.put(variableOrder.get(i), i);
}
```

Then:

* Pass `variableIndex` into `PrefixBuilder.buildPrefix` (extend signature).
* Replace `indexOf` calls with `variableIndex.get(var)`.

### 5.3 IndexSelector tie‑breaker optimisation

In `IndexSelector.chooseBestOrder` you do:

```java
} else if (compatibility.equals(bestScore)
        && candidates.indexOf(candidate) < candidates.indexOf(bestOrder)) {
    bestOrder = candidate;
    bestScore = compatibility;
}
```

Instead:

* Iterate with explicit index:

  ```java
  for (int i = 0; i < candidates.size(); i++) {
      QuadKeyOrder candidate = candidates.get(i);
      ...
      if (compatibility.equals(bestScore) && i < bestIndex) {
          bestOrder = candidate;
          bestScore = compatibility;
          bestIndex = i;
      }
  }
  ```

This is tiny but cheap to change and cleans up an O(n²) pattern.

---

## 6. Optional but high‑leverage: data‑aware variable order

Once the mechanical fixes are in and validated, tackle query planning.

### 6.1 Gather basic statistics

For each quad index (per `QuadKeyOrder`), add simple per‑slot counters:

* Number of distinct values for S/P/O/C (approximate is fine: HyperLogLog or similar).
* Perhaps histograms for degree distribution if you’re feeling ambitious.

These can be:

* Precomputed offline.
* Or lazily computed and cached per dataset snapshot.

### 6.2 Use stats in `chooseVariableOrder`

Augment `VariableScore` with:

* Estimated selectivity (e.g. inverse of distinct count).
* Maybe average degree.

Heuristic:

* Prefer variables that:

    * Appear in many patterns (join connectivity),
    * Are highly selective (rare values),
    * Align well with leading positions of indexes.

Instead of just structural metrics, use a weighted sum:

```java
score = w1 * leadingMatches
      + w2 * (1 / avgPosition)
      + w3 * joinDegree
      + w4 * selectivityEstimate;
```

Even rough stats can dramatically reduce the number of prefixes explored for nasty joins.

### 6.3 Keep it pluggable

* Wrap `chooseVariableOrder` behind a strategy interface:

  ```java
  interface VariableOrderHeuristic {
      List<String> chooseVariableOrder(List<QuadPattern> patterns,
                                       Collection<QuadKeyOrder> availableOrders,
                                       Statistics stats);
  }
  ```

* Make `LFTJExecutor` accept a `VariableOrderHeuristic` (default: current structural one).

* This lets you experiment and compare heuristics via benchmarks.

---

## 7. Validation, regression tests, and documentation

### 7.1 Correctness tests

* Extend existing tests of `LmdbWCOJ` and `LFTJExecutor`:

    * Randomised query generator:

        * Generate random sets of `QuadPattern` over a small LMDB dataset.
        * Evaluate with:

            * LFTJ path.
            * Rebuilt join tree via `DefaultEvaluationStrategy`.
        * Compare binding sets for equality.

* Add targeted tests for:

    * Empty joins.
    * Single‑pattern “join” (degenerate case).
    * Highly redundant joins (same pattern repeated).

### 7.2 Performance regression suite

* Turn the micro‑ and macro‑benchmarks into a CI job that:

    * Runs JMH with a small number of iterations.
    * Fails if performance drops by more than X% relative to a baseline file.

Even a lightweight check (compile + “doesn’t crash”) is better than nothing; you can run full JMH locally.

### 7.3 Documentation

Add a design doc in `docs/` or as class‑level javadoc:

* Explain:

    * How `LFTJExecutor` maps to the LFTJ algorithm (variables as levels, leapfrog per variable).
    * The reasoning behind using `LeapfrogIteratorCursor`.
    * LMDBTrieIterator’s contract (`open`, `next`, `seek`, `atEnd`, prefix semantics).
    * Any quirks around LMDB transaction/cursor lifecycles.

This helps future you (and colleagues) understand why some parts are “weird but fast”.

---

## 8. Rollout strategy

1. Implement & test **in this order**:

    * Swap to `LeapfrogIteratorCursor` in `LFTJExecutor`.
    * Remove allocations in `LMDBTrieIterator`.
    * Fix cursor leak.
    * Introduce iterator pooling.
    * Precompute variable metadata.

2. After each step:

    * Run unit tests.
    * Run microbenchmarks and basic end‑to‑end tests.

3. Only once the low‑level parts are stable, experiment with:

    * Data‑aware variable ordering.
    * Alternative heuristics for index selection.

4. Keep a feature flag (e.g. a system property) that lets you temporarily fall back to the old behaviour for debugging or if users report regressions.

---

