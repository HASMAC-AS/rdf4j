# Strong direct-mapped memo: source and lifetime review

## Safe experimental placement

Use the existing iteration-owned `NativeResultMaterializer` as the first experiment boundary. It already owns the outward-row capture memo, receives the query ledger, validates exact source and exposure identity, repairs provisional cache hits if `resultValueScope()` changes during capture, and closes with the result iteration. A direct-mapped table there can share entries across columns using `(id, fullMask)` without widening the ownership lifetime.

The key is meaningful only within that surrounding domain. `NativeLmdbQuerySource.resultValueScope()` explicitly certifies ID-to-term stability and requires checks before and after lookup. `valueLookupScope()` certifies the opposite mapping and cannot replace it. `idSpace()` alone does not identify the current visible mapping. Synthetic facades delegate their result-view token while retaining separate plan catalogs or runtime term spaces that can reuse the same numeric IDs. Keep exact source identity and exposure identity as well as the result-view token.

`NativeProjectedBindingSet.captureForOutput()` already distinguishes provisional memo hits from fresh source results, repairs all provisional aliases after a changed view, and abandons partial publication on failure. Preserve that protocol. `detachAfterMaterialization()` removes the source from the outward row only after every bound value is captured; the value wrappers keep their established ownership capabilities.

For a later source-wide cache, `NativeExecutionContext.nativeState` provides the appropriate owned-state facility, with a key scoped to an exact source/catalog, as used by `NativeValueResolver.Scope`. Contexts can share scalar or term owners while having separate binding/source frames. Do not move a source-specific memo into a compiled plan or the query-wide scalar owner merely to increase sharing. Its cleanup must precede the release of the source capability it depends on, and leases must still allow accepted outer consumers to finish.

`AggContext` is an aggregate-local precedent, not a drop-in result cache. It uses open addressing and cache-bank keys, has aggregate-specific consumers and cleanup, and reserves decoded value estimates where it has a memory account. It does not provide the outward row's exact full-mask, source-view, exposure, and mutation-witness protocol.

## Identity witness

`LmdbValue.getSemanticVersion()` alone is insufficient. Built-in `setInternalID(id, revision)` methods publish a new immutable `InternalIdentity` without incrementing the semantic version. A correct entry therefore validates its expected ID, revision number, revision owner, and semantic version. Compare the revision number and owner rather than requiring the same `InternalIdentity` object: ordinary cold-to-warm or equivalent identity replacement can preserve the term while changing that object.

The source's `knownStatementId(nativeValue)` is a stronger metadata-only alternative. For built-in sources, it validates the live snapshot, stable-ID horizon, protected epoch, dictionary owner/completed revision, and committed association. It performs no lexical load. It does add per-hit source/snapshot validation and can propagate genuine source-lifetime failures. The current standalone memo API has no source argument, so the strengthened experiment stores an additional entry witness instead and leaves the established source fences in the materializer.

Unsupported/custom values should decline admission unless they provide a trustworthy mutation contract. Do not infer runtime/catalog membership from the sign or numeric interval of an ID. Existing `NativeExecutionContext.contains` checks published membership, and the outward-row path already materializes evaluation-owned terms before their owner can close.

## Existing size information and its limits

| Existing mechanism | What it knows | Why it does not bound this strong lazy memo |
|---|---|---|
| `LmdbValue.retainedLexicalLength()` | Loaded lexical length without forcing a cold payload | Negative while cold; literals report their label only; later initialization can increase retained data without changing semantic version |
| `AggContext.ownedValueBytes()` | Conservative scalar estimates including strings and numeric allowance | Calls lexical/datatype getters, can initialize values, and declines triple terms |
| `NativeRuntimeValueTable.retainedValueBytes()` | Conservative standard RDF value-graph charge | Reads lexical payloads; explicitly not arbitrary-subclass heap/RSS measurement; its owned values differ from public mutable lazy wrappers |
| `ValueStore.estimatedRetainedBytes()` | Admission estimate for ordinary statement/value buffers | Reads terms and lexical content; not a metadata-only cold-value upper bound |
| Shared `ValueStore` lexical cutoff | Evicts oversized values from the store's own bounded cache after initialization | Cannot remove a reference held in a separate strong memo; label length is not the whole retained graph |

A bounded number of entries gives a cardinality bound. Reserving arrays gives a table bound. Neither limits the size of the strongly retained values, their loaded children, literal numeric/calendar caches, or revision capabilities. A value may initialize or be mutated by an already returned row while the memo still owns it. A size recheck only on the next hit misses growth in entries that are never revisited.

Do not call the existing lexical estimators merely to obtain a budget number for an accepted lazy output. That changes when decoding, failures, and allocations happen, including for callers that stop early or never read a binding.

An honest initial experiment keeps the strong-reference intervention explicit, charges the complete arrays/metadata it can calculate, and reports the unbounded retained-payload limitation. Use controlled fixture payloads and the existing lifecycle/cleanup checks. Do not promote a successful fixture result as a production memory guarantee.

A production memory guarantee needs an additional ownership mechanism: immutable detached values with bounded complete graphs; trustworthy pre-decode size quotes plus reservation before all retained growth; or model initialization/mutation hooks that admit, update, or revoke strong retention before growth. Conservative duplicate charging can simplify shared-value accounting, but the graph must first be bounded. Weak entries retain the existing alternative of leaving payload lifetime with consumers.

## Growth and cutoff

Reserve a replacement table while the old arrays are still charged, then publish it and release the old arrays. Moving references does not duplicate the underlying payload, but both array allocations coexist during growth. Refusal must preserve the returned value and follow the declared disable/release policy; it must not turn an optional optimization into a result error. Clear every strong reference on scope change, disable, and close.

The proposed single 8192-probe cutoff is intentionally tested as written. A unique prefix followed by a small repeated working set remains uncached forever. It therefore measures a fixed startup heuristic, not whether later query phases can benefit.

Useful later comparisons keep the first experiment unchanged and separately test:

1. Fixed capacities such as 4096, 16384, and 65536, with no adaptive cutoff, to separate collision effects from admission policy.
2. Windowed decisions with a bounded cooldown and periodic re-admission, so later repetition can recover. The existing `NativeValueResolver.Bucket` admits one miss in 33 under saturation while allowing existing hits, explicitly to discover later hot values.
3. A small primitive ghost-key table or second-touch admission when strong values are disabled, so repeated keys can be detected without retaining their values.

Include both unique-then-repeated and repeated-then-unique traces, differing full masks, retained rows, early close, large cold payloads, and actual ledger pressure. These experiments answer different questions from the matched full-consumption fixture comparison.

## Source locations

All paths are relative to the repository root:

- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/NativeResultMaterializer.java`
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/NativeProjectedBindingSet.java`
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/NativeLmdbQuerySource.java`
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/NativeExecutionContext.java`
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativeAggregateState.java`
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/NativeRuntimeValueTable.java`
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/NativeValueResolver.java`
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbSyntheticValueSource.java`
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativeRowStep.java`
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/model/LmdbValue.java`
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/model/LmdbIRI.java`
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/model/LmdbLiteral.java`
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/ValueStore.java`
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/ValueStoreRevision.java`
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/ValueResolutionScope.java`
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbStoreConnection.java`
