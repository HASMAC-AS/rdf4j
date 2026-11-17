# LMDB equality and binary writer investigation

Inspected upstream main `5434e51194e6c2e1aa745e5d65a4c998918d6073`. This is source analysis, not a performance measurement. No production code was changed by this investigation.

## Existing fast paths

`core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/model/LmdbLiteral.java` already returns immediately for `this == o`. Otherwise it calls `LmdbValue.equalsNative`. That method rejects incompatible value kinds, checks that both values expose a supported semantic mutation version, captures each `(ID, revision)` together, and uses native equality only when the two non-null revisions compare equal. Equal known IDs imply equality. Unequal known IDs imply inequality only when the owning store format guarantees canonical language tags. All other cases fall back to RDF literal equality.

`core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/model/LmdbValue.java` returns cached `Boolean.TRUE`/`Boolean.FALSE` or `null`; its tri-state return does not allocate a new boxed Boolean per comparison. Built-in `getInternalIdentity()` methods return an already published immutable record, also without allocating a new record per call. Custom implementations using the default method can allocate.

For `LmdbLiteral`, both `getSemanticVersion()` and `getInternalIdentity()` call `checkDatatypeVersion()`. Thus an ordinary native comparison checks datatype child freshness twice per literal. For a mutable native datatype this reads the child's semantic version; for a known immutable datatype it is a much cheaper constant check. A profile attribution to `LmdbLiteral.equals` can therefore mean cheap but very frequent checks, semantic fallback, or initialization. These must be distinguished before changing the model contract.

## Recommended experiments, ranked

1. **Store previous values in a reusable column array.** `BinaryQueryResultWriter.handleSolutionImpl` currently looks up the current binding by name and then looks up the previous binding by name. `NativeProjectedBindingSet.getValue` scans its name array, and `ListBindingSet.getValue` uses `List.indexOf`, so repeatedly looking up both rows doubles this portion of the quadratic-in-column-count work. A `Value[]` in header order removes all previous-row lookups and avoids retaining an entire native result row. Keep the array unchanged on an empty row: the existing parser deliberately leaves `previousTuple` unchanged on `EMPTY_ROW_RECORD_MARKER`.

2. **Check reference identity and null at the writer call site.** After the existing current-value null case, compare `value == previous || previous != null && value.equals(previous)`. Equality already has an identity check inside each normal model implementation, so any additional gain is removal of polymorphic dispatch rather than removal of the complete native equality path. The null guard is useful because `LmdbLiteral.equals(null)` currently calls `init()` before `AbstractLiteral.equals` rejects null. Serialization would need the lexical value anyway, but the equals dispatch and checks are needless.

3. **Prefer writer encoding work to changing native equality.** A cache for common datatype QName records can eliminate repeated namespace lookup and string encoding without altering store or value ownership. Common `CoreDatatype` constants are a safer basis for such a cache than arbitrary mutable `IRI` objects. If arbitrary values are cached, use immutable lexical keys or a validated mutation/version contract and enforce a memory bound. A value object used as a mutable hash-map key is not a sufficient correctness basis.

4. **Only after profiling, specialize the common `LmdbLiteral` pair path.** A possible local experiment is an exact-built-in-class path which performs datatype freshness validation once, then inspects the built-in fields, retaining all current revision/canonical checks and the generic `equalsNative` fallback. It must preserve arbitrary subclasses, query-owned `PreparedLmdbValue` representations, unknown IDs, and lexical fallback. Adding a general API to return a validated identity would be a broader change. The expected benefit is much smaller than eliminating duplicate column lookup and string encoding, and has not been measured here.

5. **A future native serializer could consume validated repetition metadata.** It would avoid creating values only to compare them. Such metadata must carry the owning revision/generation and semantic guarantees. Passing bare LMDB IDs to a general query-result writer is unsafe and would introduce a storage-specific dependency. This is a larger architectural experiment, not an immediate writer patch.

## Constraints that rule out bare ID comparison

- A number is meaningful only within its store and revision. `ValueStoreRevision.Base.equals` compares both `getRevisionId()` and `getValueStore()`. Lazy and position wrappers can be distinct Java objects while representing the same revision, so replacing revision equality with reference equality is also wrong.
- Unknown IDs must fall back. A literal constructed by the caller may have the same RDF value as a stored literal without already knowing its dictionary ID.
- Legacy dictionaries may hold multiple IDs for semantically equal language literals, including case differences such as `EN` and `en`. `hasCanonicalIds()` checks `store.getFormat().usesCanonicalLanguageTags()` before using unequal IDs as proof of inequality.
- Literal label, language, direction, and datatype mutation invalidate its ID. Datatype child mutation also invalidates the parent through `ChildVersion`. A cached ID detached from those checks can create false repeats.
- RDF literal equality includes lexical label, datatype, case-insensitive language, and base direction. Different numeric spellings such as `42` and `042` remain different RDF terms even if their value-space interpretation is equal.
- Query-owned `PreparedLmdbValue` values have immutable semantic payloads but a changing dictionary proof, separate decode capability, and ownership/generation state. Cross-representation comparisons are deliberately supported by `equalsNative`.
- Keeping a previous value does not authorize rebinding it to the newest revision after rollback, clear, reclamation, or ID reuse. Lifetime protection belongs to the LMDB result/materialization boundary, not to the writer.

## Tests relevant to any equality change

`core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/model/LmdbLiteralEqualsTest.java` directly covers same-canonical-revision lazy comparisons without resolution; known/unknown ID sharing; repeated equality fast paths; noncanonical known aliases; differing stores and revisions; distinct lazy wrappers for equal revisions; language case; directed language literals; unequal datatype and lexical forms; hash preservation; and lazy resolution. In particular, retain `sameCanonicalRevisionKnownIdsCompareWithoutResolving`, `noncanonicalKnownAliasesUseRdfEqualityWithoutRewritingIds`, `sameRevisionNumberFromDifferentStoresNeverSharesIds`, `differentRevisionsOfSameStoreNeverShareIds`, and `equalRevisionsWithDistinctWrappersShareIds`.

`core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/LmdbCacheSelectionTest.java` includes mutation of a datatype nested inside literals/triple terms, rollback, reopen, and clear. `LmdbCanonicalValueCacheTest` exercises canonical identity, aborted IDs, ID reuse, and revision retention. `SnapshotPositiveLookupCacheTest` also covers literal mutations and cache authorization.

For the writer column-array change, directly test mixed bound/unbound columns, empty rows between repeated nonempty rows, equal distinct value objects, a reused mutable `BindingSet` with values replaced between callbacks, and wide results. The current previous-`BindingSet` approach will reread a reused mutable binding set after it has already been modified; an array captures the value references observed at the callback. That correction should have a regression test before production edits. Capturing references does not itself snapshot the internals of a mutable value object; avoiding that larger behavioral question keeps this change focused.

## Source anchors

- `core/queryresultio/binary/src/main/java/org/eclipse/rdf4j/query/resultio/binary/BinaryQueryResultWriter.java`: `handleSolutionImpl`, `writeRepeat`, `writeLiteral`, `writeQName`.
- `core/queryresultio/binary/src/main/java/org/eclipse/rdf4j/query/resultio/binary/BinaryQueryResultParser.java`: `EMPTY_ROW_RECORD_MARKER`, `REPEAT_RECORD_MARKER`, and `previousTuple` updates.
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/model/LmdbLiteral.java`: `equals`, `getSemanticVersion`, `getInternalIdentity`, `checkDatatypeVersion`, `payloadChanged`.
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/model/LmdbValue.java`: `equalsNative`, `InternalIdentity`, `ChildVersion`.
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/ValueStoreRevision.java`: `Base.equals`, `Lazy`, `Position`, `hasCanonicalIds`.
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/PreparedLmdbValue.java`: `Prepared`, `State.semanticEquals`, `Proof`, and decode ownership.
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/NativeProjectedBindingSet.java`: `getValue`, `indexOf`, `value`.
- `core/model-api/src/main/java/org/eclipse/rdf4j/model/base/AbstractLiteral.java`: `equals` and language-tag comparison.
