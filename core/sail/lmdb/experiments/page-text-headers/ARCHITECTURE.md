# Adjacency text headers: architecture and space accounting

This appendix audits production commit [`4a5f5aaad30c081390717c0cf3c8e4c026539fe3`](https://github.com/HASMAC-AS/rdf4j/commit/4a5f5aaad30c081390717c0cf3c8e4c026539fe3). The experiment changes test code only.

## These are native-memory pages

The current direct-adjacency index is strictly in memory. It is rebuilt from an authoritative LMDB snapshot; its lifecycle contract explicitly prohibits creating files. CSF pages are **not persisted as capacity-sized images or translated into separate LMDB records**. “Persistent” ownership in this subsystem refers to immutable versions and structural sharing. See [`LmdbDirectAdjacencyStore`](https://github.com/HASMAC-AS/rdf4j/blob/4a5f5aaad30c081390717c0cf3c8e4c026539fe3/core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbDirectAdjacencyStore.java) and [`LmdbInMemoryAdjacencyIndex`](https://github.com/HASMAC-AS/rdf4j/blob/4a5f5aaad30c081390717c0cf3c8e4c026539fe3/core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbInMemoryAdjacencyIndex.java).

Consequently, page-capacity differences are **native-memory estimates, not measured LMDB disk growth**. The benchmark's dictionary-file total belongs to its real ValueStore, including the existing separately persisted `literal_text` database. It does not measure an incremental disk cost for experimental page headers. The existing literal summary is a 56-byte record before LMDB key, node, and page overhead; its persistence is implemented in [`ValueStore`](https://github.com/HASMAC-AS/rdf4j/blob/4a5f5aaad30c081390717c0cf3c8e4c026539fe3/core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/ValueStore.java).

## Existing layout and a zero-allocation small mask

[`CompactCsfPageFormat`](https://github.com/HASMAC-AS/rdf4j/blob/4a5f5aaad30c081390717c0cf3c8e4c026539fe3/core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/csf/CompactCsfPageFormat.java) defines:

| Field | Current value |
|---|---:|
| Page version | 4 |
| Format hash | `0x2dc970b6` |
| Fixed header | 160 bytes |
| Reserved word | 32 bits at byte offset 36 |
| Maximum rows | 1,024 |
| Maximum capacity | 65,536 bytes |

The reserved word could hold a 16-, 24-, or 32-bit character mask with **zero additional page allocation**. This is a future format option, not an implemented header. The current reader requires that word to be zero and validates the version/hash. Adoption must update encoder, validator, format identity, and conservative unknown-summary semantics together. Normalization identity and coverage must also be established. The prototype's four-byte framing envelope is not intrinsically required as four additional production bytes when existing header space supplies that information. See [`CompactCsfPageReader.validatePage`](https://github.com/HASMAC-AS/rdf4j/blob/4a5f5aaad30c081390717c0cf3c8e4c026539fe3/core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/csf/CompactCsfPageReader.java).

## What the measured space columns mean

The encoder rounds the mandatory encoded image to a capacity class in 256-byte increments. It then puts optional row-search and prefix accelerators into otherwise unused capacity. Thus `encoded_bytes` already includes selected accelerators, while `capacity_bytes` includes remaining slack. See [`CompactCsfPageEncoder.tryMeasure`](https://github.com/HASMAC-AS/rdf4j/blob/4a5f5aaad30c081390717c0cf3c8e4c026539fe3/core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/csf/CompactCsfPageEncoder.java) and the capacity policy in [`NativeSlabAllocator`](https://github.com/HASMAC-AS/rdf4j/blob/4a5f5aaad30c081390717c0cf3c8e4c026539fe3/core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/csf/NativeSlabAllocator.java).

Production materialization uses `CsfShard.Writer`: a native shard directory, contiguous page-capacity areas, and an eight-byte guard for wide vector tail reads. Copy-on-write shards can retain unchanged pages and allocate replacements. The adapter uses the same encoder/decoder and capacity policy, with contiguous page areas plus the same guard, but omits shard directories, routing metadata, and Java owners. Its `capacityBytes()` excludes the guard; `nativeAllocatedBytes()` includes it. Neither includes allocator bookkeeping. See [`CsfShard`](https://github.com/HASMAC-AS/rdf4j/blob/4a5f5aaad30c081390717c0cf3c8e4c026539fe3/core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/csf/CsfShard.java).

Let `U` be measured encoded bytes, `A` measured accelerator bytes, `C` existing capacity, and `H` added summary-envelope bytes. Holding page membership fixed:

- **Preserve accelerators:** extra capacity is `ceil256(U + H) - C`.
- **Replace accelerators:** the reported lower space bound is `max(0, ceil256(U - A + H) - C)`.
- **Reuse the reserved word:** a compatible small-mask format can have zero extra capacity.

Exceeding 64 KiB requires splitting; the census reports that case instead of presenting a partial total. Removing accelerators can change other query costs; the scan measurements retain them. `summary_bytes` counts envelope contents, not Java array headers/references. Production totals would sum pages across the relevant predicates and planes, not multiply unique dictionary literals by one header size.

## Page grouping and rejection seams

The real splitter flushes at 1,024 rows or 128 KiB rough scratch, then divides groups that cannot fit the encoded 64-KiB limit. Small experimental groups model small partitions/tails or alternative grouping policies. They are not the default page size. Four neighbors per row can produce 4,096 incidences in a 1,024-row page.

Verified extension points in [`ImmutablePagedQuadCsfIndex`](https://github.com/HASMAC-AS/rdf4j/blob/4a5f5aaad30c081390717c0cf3c8e4c026539fe3/core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/csf/ImmutablePagedQuadCsfIndex.java) are:

- `PageCursor.advance()` selects a header address; payload binding is deferred until `ensurePayloadBound()`.
- `KeyCursor.advance()` already rejects common-context-disjoint pages before `load()`.
- `findLocalReferenceByPartition()` checks context headers before `cursor.pageChanged()`.
- `RowCursor.nextContextCandidate()` identifies candidate physical spans within continued rows.

A text summary must describe the queried coordinate: outgoing object neighbors and incoming object roots are different columns. Rejecting one physical page cannot reject an entire logical row when continuation pages may still contain a match. The existing context paths preserve this distinction.

## Snapshot and maintenance correctness

Overlay generations contain complete replacement rows or tombstones. Resolution is pending-row proof, newest applicable generation, then base. A rejected base page must never hide matching overlay replacements. The existing context-filtered root scan merges filtered base keys with authoritative changed keys; it provides the relevant template. See [`LmdbAdjacencyDeltaGeneration`](https://github.com/HASMAC-AS/rdf4j/blob/4a5f5aaad30c081390717c0cf3c8e4c026539fe3/core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbAdjacencyDeltaGeneration.java) and [`LmdbDirectNativeAdjacency.rootScanCursor`](https://github.com/HASMAC-AS/rdf4j/blob/4a5f5aaad30c081390717c0cf3c8e4c026539fe3/core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbDirectNativeAdjacency.java).

Build summaries with new/replacement pages and retain them with shared pages under the same snapshot ownership. Unknown summaries and unsupported run/composite leaves must pass through. Derive metadata from complete persisted lexical forms under the validated lowercase policy; admitted values still require exact RDF/SPARQL evaluation. This experiment does not implement those production integration steps.
