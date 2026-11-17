# Independent binary writer implementation review

## Result and reviewed source

No actionable correctness findings were identified in the production writer diff reviewed against upstream main `5434e51194e6c2e1aa745e5d65a4c998918d6073`.

Reviewed file: `core/queryresultio/binary/src/main/java/org/eclipse/rdf4j/query/resultio/binary/BinaryQueryResultWriter.java`.

Reviewed source SHA-256: `db7c2af9917591a274074854cd0e091222ef77006923901a959c3287450f7668`.

This was an independent source review of the production diff, relevant tests, and LMDB/protocol code. The reviewer made no production changes and ran no builds, tests, or timing measurements for this review. The conclusion is limited to the inspected source; it does not establish a new runtime or benchmark result.

## Correctness checks

- **Strict UTF-8:** Checked ASCII, two-byte and three-byte sequences, valid surrogate pairs, lone and reversed surrogates, and the short-path capacity bound. At most 1,365 UTF-16 code units enter the general short path, so its maximum payload is 4,095 bytes. A surrogate pair consumes four bytes for two code units and stays within that bound.
- **Length and payload framing:** `putInt` preserves the big-endian byte count. The bulk write emits the same length-plus-payload bytes. Malformed short strings throw before their length or payload is published.
- **Long-string fallback:** The original strict `CharsetEncoder` remains in use, preserving malformed-input rejection.
- **Repeat state:** The column array snapshots binding values, clears them on null cells, preserves previous values across empty rows, and retains semantic equality for distinct objects.
- **Marker batching:** Pending NULL/REPEAT markers flush before the next full value and at the row boundary, preserving wire order and completed-row visibility.
- **Datatype caching:** Only identity-equal canonical core datatype IRIs use cached records. Namespace IDs are patched into writer-local clones; caches and namespace mappings reset between documents. Custom or unusual IRI decompositions retain the generic path.
- **Language detection:** Reading `getCoreDatatype()` once is equivalent to the previous `Literals.isLanguageLiteral` checks for conforming literals.
- **LMDB interactions:** The patch does not replace semantic comparison with bare ID equality or weaken revision, canonicality, mutation, or ownership checks.

## Performance limits

The writer allocates roughly **4 KiB of scratch storage per instance**, plus its small datatype-reference array. The 4,096-row benchmarks amortize that cost; they do not characterize empty or very small documents. This is a limitation of the benchmark coverage, not a measured regression claim.

Non-ASCII strings longer than **1,365 UTF-16 code units** still use the allocating strict encoder. Strings up to 4,096 code units may first scan an ASCII prefix before falling back. Allocation-reduction claims should therefore describe common short strings rather than every string size and character distribution.

## Raw UTF-8 forwarding remains a separate direction

The earlier forwarding discussion matches the inspected main revision. Dictionary literals and IRI components contain UTF-8 bytes, and inline short strings contain up to six UTF-8 bytes. Inline numeric/date-time values and triple terms require different handling.

`RepositoryValueIteration` resolves ordinary LMDB payloads before repository exposure, so the generic writer receives values whose String decoding may already have occurred. `getRecordView()` owns copied bytes, while `visitRecord()` exposes callback-scoped access. Both current public entry points read the current dictionary; neither authorizes reading an old result's numeric ID without its captured revision capability. Current typed byte views reject extended language-length headers and unsupported inline/triple formats, requiring fallback.

Avoiding the decode/re-encode round trip therefore needs retained lexical bytes or a native serialization boundary with correct ownership, fallback, datatype/namespace translation, and language-direction handling. Re-reading dictionary bytes in the current generic writer would add another lookup after decoding.

## Source anchors

- `core/queryresultio/binary/src/main/java/org/eclipse/rdf4j/query/resultio/binary/BinaryQueryResultWriter.java`: `handleSolutionImpl`, `writeMarkers`, `writeLiteral`, `createCoreDatatypeRecordTemplates`, `writeBufferedString`, and `writeString`.
- `core/queryresultio/binary/src/main/java/org/eclipse/rdf4j/query/resultio/binary/BinaryQueryResultParser.java`: NULL/REPEAT/EMPTY_ROW processing and previous-tuple updates.
- `core/queryresultio/binary/src/main/java/org/eclipse/rdf4j/query/resultio/binary/BinaryQueryResultConstants.java`: UTF-8 and 32-bit byte-length framing.
- `core/queryresultio/binary/src/test/java/org/eclipse/rdf4j/query/resultio/binary/BinaryQueryResultWriterEncodingTest.java`: independent expected-byte fixtures, Unicode boundaries, malformed input, namespace order, custom datatype mutation, row visibility, nested triples, and flush behavior.
- `core/queryresultio/binary/src/test/java/org/eclipse/rdf4j/query/resultio/binary/BinaryQueryResultWriterStateTest.java`: reused mutable binding sets and writer reuse between independent documents.
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/model/LmdbLiteral.java` and `LmdbValue.java`: native equality and datatype mutation validation.
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/ValueStoreRecordCodec.java`: dictionary lexical encodings.
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/ValueStore.java`: owned/scoped record access and lexical String construction.
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/RepositoryValueIteration.java`: repository result preparation and resolution waves.
- `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/valueoverlay/ValueStoreRecordView.java` and `ValueStoreRecordVisitor.java`: supported headers and byte-ownership contracts.
