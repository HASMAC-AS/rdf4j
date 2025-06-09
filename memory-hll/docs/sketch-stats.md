# HyperLogLog-based Statistics

This module demonstrates augmenting the RDF4J `MemoryStore` with
probabilistic sketches to improve query planning. Each predicate gets two
HyperLogLog counters tracking the number of distinct subjects and objects.
These sketches provide near-real-time estimates of join selectivity at a
fraction of the memory cost of exact statistics.

To enable the sketches:

```java
SketchRegistry registry = new SketchRegistry();
MemoryStore store = new SketchAwareMemoryStore(registry);
SailRepository repo = new SailRepository(store);
repo.setEvaluationStatistics(new SketchEvaluationStatistics(registry));
```

Removing statements is not tracked, so rebuild statistics after large
deletions.
