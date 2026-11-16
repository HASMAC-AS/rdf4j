# Compact-page design comparisons

The candidate is motivated by this repository's recorded occupancy distribution, not an imported threshold or an external performance claim. The preceding measurement found predominantly singleton or full 64-position pages. The implementation therefore tests two representations: one inline tuple and the original dense arrays, with no intermediate capacities.

Primary external references reviewed for this phase:

- [Roaring format specification](https://github.com/RoaringBitmap/RoaringFormatSpec): non-run containers choose an array or bitset according to cardinality; array payload grows with occupied entries. Its bitmap payload and thresholds do not transfer to weak-value pages.
- [CRoaring implementation](https://github.com/RoaringBitmap/CRoaring/blob/master/src/roaring_array.c): the portable reader chooses representation from cardinality and derives the corresponding payload size before proceeding. This supports explicit representation-dependent accounting as an algorithmic comparison, not a reason to copy its numeric thresholds.
- [Roaring Go implementation](https://github.com/RoaringBitmap/roaring/blob/master/roaring.go): the available FromDense source selects compact arrays for sparse nonempty blocks and a bitmap for dense blocks. Copy-on-write behavior and container identity differ from RDF4J's cached FactorPage references.

Direct retrieval of the Java ArrayContainer and Go arraycontainer source pages was unavailable through the web source tool; no claim depends on reading those unavailable bodies. No code was copied and no dependency was added.

The independent in-repository review is in layout-audit.md. Its comparisons cover LossyConcurrentCache's cheap unchanged-reference path, LmdbNativeFactorizedRows' reserve/construct/publish discipline, direct-adjacency demanded allocation, and the sparse/dense accounting in LmdbNodeDomainSynopsis. The relevant differences are fixed 64-position keys, explicit no-reclamation admission, stable FactorPage identity, and preservation of the whole weak-reference/mutation-witness tuple.

The 512-byte compact reservation is a conservative model that includes shell and reference/accounting metadata. The dense representation retains the original 16,384-byte total budget through an additional 15,872-byte claim. Neither charge is asserted to equal actual object size. Allocation and latency are measured separately in this phase's fixed comparisons.
