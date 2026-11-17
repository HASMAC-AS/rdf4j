# Compact stable snapshot prototype

Default-off flag: `rdf4j.sail.compactResultSnapshots=true`.

This API-only experiment changes repository result snapshot representation, not ownership, value preparation, transaction or replay semantics. The parent must run LMDB integration and matched measurements before interpreting performance.

## Representation and compatibility

For a declared row width at most256, the private final CompactBindingSetSnapshot subclass owns String[] names, Value[] references and an int[] open-addressed name index. It keeps the same Value identities and source.forEach access path. Values are not inspected or initialized during copying or root extraction. No row schema cache or source reference is retained.

The initial common path allocates the compact object, the superclass's empty LinkedHashMap and three arrays. Source forEach iterator, consumer lambda and temporary source Binding objects may additionally allocate depending on JIT elimination; no claim assumes they vanish. For30 columns the index has64 int slots. Strings/Values are referenced, not copied or decoded. This removes retained per-column LinkedHashMap entries and SimpleBinding objects from the getValue/hasBinding/size and specialized root-extraction path.

Calls exposing Binding objects, iterators, names views or mutations materialize a private plain MapBindingSet delegate and release all compact arrays. This preserves MapBindingSet cast compatibility, mutable views/iterator removal, insertion order and stable retained Binding objects. Structural callback mutation fails before the next root; replacement of an existing binding affects subsequent roots. Foreign source.forEach overrides and foreign Binding getter counts are preserved. Serialization returns the exact plain-map delegate via writeReplace, avoiding private-class requirements and preserving arbitrary foreign Binding identities and stale map keys without additional getter calls.

Widths above256 use ordinary MapBindingSet directly. Inaccurately reported source sizes, duplicate names and null input pairs follow original behavior, including enabled/disabled Java assertions, with a map fallback as needed.

## Important performance limitation

Iterator/getBinding/getBindingNames consumers pay compact setup followed by full map materialization. They may regress compared with the old map-only path. This is not a universal row-consumer throughput claim. Header-name getValue consumers, including the current no-writer harness and binary writer, are the intended initial measurement. Parent will compare iteration-based consumption separately. Default remains off.

## Verification

Test-first:10 initial tests,1 expected compact-witness failure before production changes. Review then reproduced4 failures (extra foreign-name reads, serialized private class, source.forEach bypass, names-view spliterator semantics) before fixes. A separate serialization test reproduced1 failure for foreign Binding getter/rekeying and drove the private delegate design. Evidence files retain these reports.

Final focused suite:23 tests pass with flag on and off. Assertion-disabled fork:18 tests pass, with an assertion-mode guard verifying the JVM actually disables assertions. Complete Sail API unit suite:137 tests pass with flag on and independently137 with flag off. Zero failures/errors/skips. Scoped Spotless apply succeeds on all three touched Java files. This is not repository-wide or LMDB integration validation.

Coverage includes exact Value identity/no Value interactions, reused mutable Binding source, source reuse, hash collisions, empty/missing keys, equality/hash/order, mutable/retained views and Bindings, all bulk view removals, callback mutation, serialization graph identity/keys,256/257-width boundary, under/overreported source sizes, duplicate/null source pairs under both assertion modes.

Frozen candidate API classes: `experiments/overlay/compact-snapshots/candidate-api-classes` (local, not versioned). Parent owns matched benchmarks against `experiments/overlay/lifetime/original-api-classes`, with identical frozen LMDB classes.

No performance result yet. No remote publication attempted; owner confirmation is pending for publication gate. No issue number supplied, so local checkpoint uses GH-0000.
