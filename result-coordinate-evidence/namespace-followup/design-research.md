# Comparable execution and ownership designs

Reviewed on 2026-11-15 using primary documentation and implementation sources. These comparisons guide hypotheses; they are not measurements of RDF4J or evidence that a particular change will improve its performance.

## Selection vectors across columns

[DuckDB's execution format](https://duckdb.org/docs/2.0/internals/vector) represents a dictionary vector as a child vector plus an index selection. Its unified format permits consumers to handle physical encodings through a common view. In [DataChunk::Slice](https://github.com/duckdb/duckdb/blob/main/src/common/types/data_chunk.cpp), the same selection is passed across the chunk's columns, together with one merge cache. This is a useful comparison for the intended ROWS representation: shared selections are valuable only if execution actually exposes a common row origin across columns.

The new RDF4J shared-tail pilot did not expose that origin under normal AUTO. It encoded all output rows, but measured one selection group per tracked column and no factorized engagement. Therefore a performance claim about shared selections would be unsupported by that pilot. The negative qualification is preserved rather than forcing a different executor.

## Allocation ownership and reference bookkeeping

[Apache Arrow Java's memory model](https://arrow.apache.org/java/current/memory.html) associates shared buffers with explicit retain/release bookkeeping and allocation ledgers. A ledger can cover multiple views that share a lifetime. Its [ReferenceManager interface](https://arrow.apache.org/java/current/reference/org.apache.arrow.memory.core/org/apache/arrow/memory/ReferenceManager.html) distinguishes allocation accounting from reference operations and exposes a no-op reference manager as well as managed implementations.

The relevant inference for RDF4J is to prove whether a coordinate needs a managed lifetime before entering per-cell reference bookkeeping. This phase's proposed frame guard uses an existing owner invariant: while enabled and with no namespace directory ever allocated, private registers and frames can only hold permanent or unbound coordinates. This is a narrower local optimization than changing reference multiplicity, sharing ownership across producers, or relaxing public source-batch validation.

No code from these projects was copied and no dependency was added. Source layouts, memory models and execution routes differ, so only RDF4J's independently verified matched experiments determine promotion.

## Sparse-to-dense storage as a separate follow-up

The [Roaring format specification](https://github.com/RoaringBitmap/RoaringFormatSpec) provides a concrete example of selecting a container representation according to cardinality: small non-run containers use sorted arrays, whereas dense ones use a bitmap. Its thresholds and payload are not transferable to this value cache. The relevant design principle is to start with storage proportionate to occupancy and promote only when it becomes worthwhile.

The new RDF4J diagnostic measured 16,112 singleton pages among 16,364 admitted 64-cell pages. A future inline-singleton or small packed page followed by a charged promotion is therefore a more directly supported storage experiment than changing selection transport again. Such a candidate must retain the full coordinate key, weak values, mutation witnesses and lifetime rules. It must account for both old and replacement storage during promotion and preserve the existing page if optional promotion is refused. This phase does not implement or claim a speed benefit from that follow-up.
