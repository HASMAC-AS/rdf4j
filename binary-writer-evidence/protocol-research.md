# Binary query result writer: protocol and implementation research

Date: 2026-11-17. Inspected RDF4J upstream `main` revision
`5434e51194e6c2e1aa745e5d65a4c998918d6073` in the isolated task checkout.
This is source analysis; performance hypotheses below require the separately
recorded measurements. No production source was changed for this research.

## The charset question

The binary **query result** format requires ordinary UTF-8 strings with a
big-endian 32-bit **byte** length. Binary framing removes textual parsing of
record types and integer lengths; it does not remove the need to represent
Unicode strings as bytes. `BinaryQueryResultConstants` specifies UTF-8, and
`BinaryQueryResultParser.readStringV2()` reads the length, reads those bytes,
and decodes them with a strict UTF-8 decoder. The current writer uses format
version 4 and this string representation.

This format must not be confused with the separate Binary RDF statement
format. Its encoding rules do not determine the query result format.

The useful optimization is to avoid **repeated encoding, generic encoder
machinery, and temporary allocations**, while emitting the same UTF-8 bytes.
Writing raw Java `char` values, ISO-8859-1 bytes, modified UTF-8 from
`DataOutputStream.writeUTF()`, or store-local numeric identifiers would not be
wire compatible. NUL is one zero byte in ordinary UTF-8, and supplementary
characters use four bytes from a valid surrogate pair.

Primary references:

- [RDF4J binary query result constants](https://github.com/eclipse-rdf4j/rdf4j/blob/5434e51194e6c2e1aa745e5d65a4c998918d6073/core/queryresultio/binary/src/main/java/org/eclipse/rdf4j/query/resultio/binary/BinaryQueryResultConstants.java)
- [RDF4J binary query result parser](https://github.com/eclipse-rdf4j/rdf4j/blob/5434e51194e6c2e1aa745e5d65a4c998918d6073/core/queryresultio/binary/src/main/java/org/eclipse/rdf4j/query/resultio/binary/BinaryQueryResultParser.java)
- [Official constants API documentation](https://rdf4j.org/javadoc/latest/org/eclipse/rdf4j/query/resultio/binary/BinaryQueryResultConstants.html)

The constants documentation's historical header description includes a flags
byte. The parser consumes that byte only for version 2; the current version 4
writer does not write it. Use the pinned implementation for exact byte fixtures.

## Why the existing encoder can be expensive

`writeString()` invokes `charsetEncoder.encode(CharBuffer.wrap(s))`. The
encoder object is reused, but the convenience `encode` call allocates its
output `ByteBuffer` and backing byte array each time. It estimates capacity
from `averageBytesPerChar()` and grows/copies the output if required. The input
wrapper also adds work. For this String-backed character buffer, the OpenJDK
UTF-8 encoder selects `encodeBufferLoop`, with buffer operations for each
character, rather than its array-backed fast path.

OpenJDK 25's `String.getBytes(UTF_8)` selects a specialized implementation. For
an ASCII compact string, it checks for high bytes and clones the backing byte
array. It can therefore beat the general encoder despite still allocating an
array. It is **not** an equivalent drop-in replacement: `String.getBytes`
replaces malformed input, whereas the existing encoder reports it. An isolated
surrogate currently produces a `MalformedInputException`, wrapped by normal
writer entry points in `TupleQueryResultHandlerException`.

Primary references inspected:

- [OpenJDK 25 String](https://github.com/openjdk/jdk25u/blob/master/src/java.base/share/classes/java/lang/String.java), fetched blob `52af7dfd50376ee1b467f348309164b86c5b1c45`; `encode`, `encodeUTF8`, `encodeUTF8_UTF16`.
- [OpenJDK 25 UTF-8 encoder](https://github.com/openjdk/jdk25u/blob/master/src/java.base/share/classes/sun/nio/cs/UTF_8.java), fetched blob `89a199db99bc974c37d9a6fe9695afcfc39f04ef`; `encodeLoop`, `encodeBufferLoop`.
- [CharsetEncoder Java 25 contract](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/nio/charset/CharsetEncoder.html), especially default REPORT behavior and the allocating convenience method.
- [String Java 25 contract](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/lang/String.html), `getBytes(Charset)` replacement behavior.

The same implementation paths were checked in the installed JDK 25.0.5 source
archive. No JDK-private APIs are needed or recommended.

## Comparable implementations

### Apache Avro: bounded ASCII fast path and byte-oriented string values

Current `BinaryEncoder` keeps a reusable 63-byte string buffer. For strings
that fit, it attempts a direct ASCII character-to-byte copy. On success it
writes the length and the reused buffer. Non-ASCII or larger strings use
`String.getBytes(UTF_8)`. Its `writeString(Utf8)` overload directly writes the
value's existing UTF-8 bytes and byte length. `Utf8` stores bytes and a length,
and materializes a Java String lazily when requested.

For RDF4J, the transferable ideas are the small reusable ASCII path and avoiding
decode/re-encode when a safe byte-oriented representation already exists. Its
replacement-on-error fallback should not be copied into RDF4J because that
would change RDF4J's existing error behavior.

- [Avro BinaryEncoder](https://github.com/apache/avro/blob/main/lang/java/avro/src/main/java/org/apache/avro/io/BinaryEncoder.java), fetched blob `60d54718e2577de1c0133bdcc1acac03f1d5d559`.
- [Avro Utf8](https://github.com/apache/avro/blob/main/lang/java/avro/src/main/java/org/apache/avro/util/Utf8.java).

### Protocol Buffers: encoding directly into a destination buffer

`CodedOutputStream.ArrayEncoder.writeStringNoTag()` can reserve a length prefix,
encode directly into its byte buffer, and fill the prefix afterward. It also
has an intermediate-array path. Its variable-length prefix sometimes needs a
shift; RDF4J's fixed four-byte length makes reservation/backfilling simpler.
The technique is relevant only when RDF4J owns the destination buffer; an
arbitrary `OutputStream` does not support backpatching already-written bytes.

- [Protocol Buffers CodedOutputStream](https://github.com/protocolbuffers/protobuf/blob/main/java/core/src/main/java/com/google/protobuf/CodedOutputStream.java), fetched blob `35c22c580484d9a60e7b11568fbf81271b76cd7c`, `ArrayEncoder.writeStringNoTag()`.

## Candidate comparison

| Candidate | Main benefit | Cost and constraints |
| --- | --- | --- |
| Small reusable ASCII buffer; existing strict encoder fallback | Minimal code; removes wrapper/output allocation and generic encoding for common local names, language tags, numbers, and short ASCII labels | Non-ASCII strings still allocate; bound the attempted ASCII length so long Unicode labels do not get a redundant long scan |
| ASCII path plus reusable strict encoder output buffer | Removes output allocations for small Unicode strings as well | Still has encoder state and input wrapper work; reset, overflow, final encode, and flush must remain correct; use a bounded retained buffer with an explicit oversized-string path |
| Strict custom UTF-8 encoder into reusable storage | Can remove encoder/wrapper overhead for all common strings and combine output framing | More code and Unicode boundary risk; memory must be bounded; preserve errors and handle length overflow; justify by measured gains over the simpler path |
| `String.getBytes(UTF_8)` plus surrogate validation | Uses JDK optimized implementation | Still allocates and scans separately for validity; benchmark, and do not silently accept malformed UTF-16 |
| Cache already-encoded datatype/QNAME records | Avoids encoding and namespace work altogether for frequent constants | Namespace IDs are per document, so cached framed bytes are writer-local; arbitrary-value caches need explicit memory and mutation/lifetime policies |

My first candidate is the bounded ASCII path plus strict fallback, followed by
reuse of the strict fallback output buffer if Unicode workloads still allocate
substantially. A full custom UTF-8 loop is reasonable if the measured advantage
is material after removing repeated datatype encoding. It should not be chosen
solely because a generic encoder appears high in a profile.

For very large strings, a fixed-size scratch buffer requires either a byte-length
pass followed by streaming encoding, or another way to know the UTF-8 length
before writing. Growing one retained array to the largest observed literal can
make an otherwise transient large result a persistent memory cost. A custom
small-string path and standard large-string fallback is a useful intermediate
design, even if the large-string path still allocates.

## Other source observations for the experiment

`writeLiteral()` asks whether a value is a language literal twice. For typed
literals it gets/checks the datatype namespace, then calls `writeQName()`, which
gets/looks up the same namespace again and encodes the datatype local name
again. Common datatype QNAME records are an unusually attractive cache target:
their vocabulary is small and their bytes recur for nearly every typed literal.

Namespace definitions must be emitted **before the literal record marker**.
The parser expects a QNAME/URI datatype record immediately after a typed
literal's label, so moving namespace emission into that position would corrupt
the stream. Preserve this when caching or combining the two lookup paths.

The current row comparison uses `previousBindings.getValue(bindingName)` for
every column. A previous-values array avoids repeated lookup; a
`ListBindingSet` lookup currently searches its list of names linearly. An
explicit `value == previousValue` check avoids the virtual equality call for
identical instances. Equality still matters for distinct equal values and for
wire-efficient repeat markers.

`writeRepeat()` itself only writes a one-byte marker. Its inclusive profile cost
can be downstream stream calls. Buffering and batching should therefore be
measured separately from equality and UTF-8 changes. Preserve explicit flush
behavior through `getOutputStream()` and final `endQueryResult()`.

An EMPTY_ROW record does not update the parser's previous tuple or the current
writer's previous bindings. Preserve that basis for the next REPEAT record.

## Focused compatibility cases

Before accepting a UTF-8 or output-buffer change, check byte equality with the
baseline and parser round trips for ASCII, NUL, two- and three-byte characters,
supplementary characters, empty strings, byte/character length differences, and
strings crossing every chosen scratch-buffer boundary. Check rejection of a
trailing high surrogate, isolated low surrogate, high surrogate followed by an
ordinary character, and repeated high surrogates. Include long labels, nested
triple terms, directed language literals, new datatype namespaces, empty rows,
null bindings, and repeated values. Output failure and explicit flush tests
matter if output buffering is changed.

No external implementation code was copied into production by this research.

## Follow-up: forwarding existing LMDB UTF-8 bytes

There is a real opportunity to remove the UTF-8-to-String-to-UTF-8 round trip,
but it requires carrying a different representation across the result
materialization boundary. A generic writer-only change cannot eliminate a
decode that has already happened before the writer receives its row.

| LMDB representation | Bytes available for forwarding |
| --- | --- |
| Dictionary literal, including `T_CORE_LITERAL` references | The record contains a datatype ID, language/direction header, UTF-8 language bytes, and UTF-8 label bytes. The label can be copied; wire datatype and directed-language framing still need translation. `T_CORE_LITERAL` embeds the core datatype tag in the ID but remains a dictionary reference. |
| Dictionary IRI | The record contains a namespace ID plus UTF-8 local name. The namespace is another record containing UTF-8 bytes. Store namespace IDs must be translated to writer-local namespace IDs. |
| Namespace or blank node | UTF-8 text follows the record marker. Blank-node transport wrappers may alter the externally exposed identifier, so bypassing them is not valid. |
| Inline short string | Up to six UTF-8 bytes and their length are packed into the value ID; a future specialized path could extract those bytes without creating a String. |
| Inline numeric, boolean, date/time | The ID contains a typed compact value, not a stored UTF-8 lexical string. The persisted format's matching unpack/lexical rules must still be applied. |
| Triple term | The triple-term index supplies component IDs, which need recursive value serialization. There is no complete ready-to-send UTF-8 triple value. |

Exact source anchors:

- [ValueStoreRecordCodec.namespaceData/iriData/bnodeData](https://github.com/eclipse-rdf4j/rdf4j/blob/5434e51194e6c2e1aa745e5d65a4c998918d6073/core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/ValueStoreRecordCodec.java#L81) and [literalData](https://github.com/eclipse-rdf4j/rdf4j/blob/5434e51194e6c2e1aa745e5d65a4c998918d6073/core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/ValueStoreRecordCodec.java#L127).
- [Core literal references](https://github.com/eclipse-rdf4j/rdf4j/blob/5434e51194e6c2e1aa745e5d65a4c998918d6073/core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/ValueIds.java#L39), [inline Strings](https://github.com/eclipse-rdf4j/rdf4j/blob/5434e51194e6c2e1aa745e5d65a4c998918d6073/core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/inlined/Strings.java#L24), and [triple-term resolution](https://github.com/eclipse-rdf4j/rdf4j/blob/5434e51194e6c2e1aa745e5d65a4c998918d6073/core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/ValueStore.java#L9845).

Existing byte access is promising. `ValueStore.getRecordView()` returns an owned
record array, while `visitRecord()` and `withData()` provide callback-scoped
access. The optional compressed value overlay is memory-only, preserves exact
dictionary records, and can selectively decode requested lexical regions.
Its visitor record must not escape its callback or outlive the source lease;
the owned view instead copies/owns its bytes and does not pin native pages.
The current typed view/visitor rejects unsupported headers, including extended
language-length headers, and excludes inline/triple encodings; any serializer
extension needs fallback handling.

- [Owned/scoped record access](https://github.com/eclipse-rdf4j/rdf4j/blob/5434e51194e6c2e1aa745e5d65a4c998918d6073/core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/ValueStore.java#L5915), [native/overlay withData](https://github.com/eclipse-rdf4j/rdf4j/blob/5434e51194e6c2e1aa745e5d65a4c998918d6073/core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/ValueStore.java#L6597), and [visitor lifetime contract](https://github.com/eclipse-rdf4j/rdf4j/blob/5434e51194e6c2e1aa745e5d65a4c998918d6073/core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/valueoverlay/ValueStoreRecordVisitor.java#L21).

The native result path initially retains IDs in `NativeProjectedBindingSet`.
`LmdbStoreConnection.openEvaluation()` captures lazy values before detaching the
native source. The actual repository exposure path is `RepositoryValueIteration`:
it snapshots the row and resolves its unprepared values under their owning
resolution scopes before returning it. `ValueStore.data2literal()` and
`data2uri()` then construct Java Strings. The older
`LmdbValueMaterializingIteration` class is present, but inspection found no
production construction site for it in this revision; it should not be used to
describe the active repository boundary.

- [NativeProjectedBindingSet capture](https://github.com/eclipse-rdf4j/rdf4j/blob/5434e51194e6c2e1aa745e5d65a4c998918d6073/core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/NativeProjectedBindingSet.java#L229), [openEvaluation](https://github.com/eclipse-rdf4j/rdf4j/blob/5434e51194e6c2e1aa745e5d65a4c998918d6073/core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbStoreConnection.java#L1062), [repository row preparation](https://github.com/eclipse-rdf4j/rdf4j/blob/5434e51194e6c2e1aa745e5d65a4c998918d6073/core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/RepositoryValueIteration.java#L145), [resolution waves](https://github.com/eclipse-rdf4j/rdf4j/blob/5434e51194e6c2e1aa745e5d65a4c998918d6073/core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/RepositoryValueIteration.java#L248), and [String construction](https://github.com/eclipse-rdf4j/rdf4j/blob/5434e51194e6c2e1aa745e5d65a4c998918d6073/core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/ValueStore.java#L15737).

A safe extension would preserve an **owned UTF-8 lexical payload** when resolving
a dictionary-backed result, expose it through a store-independent optional
capability, and let the binary writer consume it synchronously. Strings could be
decoded lazily when a caller needs them. Alternatively, a separate native result
sink could serialize while the correct dictionary scope is held. Both designs
must preserve the captured revision/epoch, mutation invalidation, directed
language handling, store-format semantics, and fallback for computed values.
Looking up an old value solely by its numeric ID in the current dictionary is
not a substitute for its retained capability. Snapshot resolution already
rejects use after its snapshot closes ([ValueStoreRevision.Snapshot.resolveValue](https://github.com/eclipse-rdf4j/rdf4j/blob/5434e51194e6c2e1aa745e5d65a4c998918d6073/core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/ValueStoreRevision.java#L371)).

That extension changes representation, ownership, and module boundaries; the
generic binary writer currently depends on model/query APIs rather than LMDB.
It deserves a separate benchmark and lifetime/correctness change. Re-reading
dictionary bytes from the generic writer after normal repository preparation
would retain the original decoding cost and add another lookup.
