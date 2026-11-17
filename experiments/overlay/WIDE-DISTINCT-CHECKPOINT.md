# Wide DISTINCT checkpoint

Description: Default-off `rdf4j.lmdb.nativeDistinct.widePrimitive` permits widths above four in NativeDistinctTracker's primitive tuple table. Scalar and selectBatch paths share existing exact equality/hash/null/authority normalization. Flat-key and bucket capacity guards migrate all already-seen normalized keys to HashMap before array-limit overflow. Small package-private array-limit constructor permits deterministic fallback testing without enormous allocations. Primitive table arithmetic and growth use checked long bounds.

Evidence:
- wide-pre-green.log:7 tests,0fail/errors,2flag-specific skips.
- wide-pre-hit.log: expected wide path null and missing test-limit constructor failures before implementation.
- wide-final-tests.log:16 tests,0fail/errors/skips,flagtrue. Includes7focused,2metrics,3DISTINCT/order/limit,4parallel DISTINCT tests.
- wide-final-default.log: default-off regression result.
- Whole modified source javac succeeded after review fixed two accidentally-added calls in ordered tracker; no ordered-strategy behavior changed.

Plan: implementation complete; next in_progress is parent-coordinated benchmark with reconstructed memo baseline. No benchmarks run by this worker.

Scope: production file LmdbNativePrimitiveTupleTable.java; new WideNativeDistinctTrackerTest.java. No new ledger accounting; outer budget/spill mechanisms unchanged. Array-limit fallback is not a guarantee that available memory suffices, matching pre-existing HashMap path limitations. Defaults remain unchanged. No commit/publication by worker.
