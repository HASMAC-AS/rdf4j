# Round two: complete short records in one stream call

Baseline: published writer commit `741b9389538e80214075692439a8cd25a8a9ae84`. Candidate: [FullRecordBinaryQueryResultWriter.java](harness/FullRecordBinaryQueryResultWriter.java), copied from that immutable commit with its class/constructor renamed. Production, the first investigation's evidence, and the shared benchmark source are unchanged by this experiment. This note contains source-derived call counts, not timing or measured speedup.

## Recommended next experiment

Assemble each complete short record in the writer's existing 4,100-byte scratch array, then call `out.write(bytes, offset, length)` once. Factor the existing strict UTF-8 loop into `encodeString(text, offset, limit)`, which stages a length-prefixed string and returns its exclusive end. It returns `-1` when the bounded fast path cannot fit; it emits no output. Callers reserve room for record headers and datatype suffixes before staging.

For canonical typed literals, namespace resolution and the existing immutable datatype-template cache stay intact. Once the document-local datatype record is available, stage the literal marker and label, append the cached QName record with `System.arraycopy`, and emit the assembled record. The QName suffix is small, but its additional copy is a real tradeoff to measure against eliminating two stream calls.

QName IRIs and namespace declarations have the same marker/ID/string structure, so one helper handles both. Generic typed literals stage their existing namespace/local-name split; language literals stage label plus language/direction tag; blank nodes stage their identifier. These are simple applications of the same offset helper and give an ordinary eight-column mixed row one write per value. Nested triple terms remain recursive rather than adding an aggregate tree buffer.

The candidate adds 65 net lines to the published class, retains exactly the same scratch-array capacity, and has no pending-output cursor or additional retained byte buffer. Header handling, row comparison, marker batching, `error()`, explicit stream exposure, and final flushing retain their existing structure.

## Remaining call counts

The table counts writes into the supplied stream, excluding namespace declarations already completed, result headers, and flush calls. All strings must qualify for the bounded short-record path. In the inspected JDK 25.0.5 source, `DataOutputStream.writeInt()` already makes **one** downstream bulk write; counting it as four would exaggerate the opportunity. `writeByte()` makes one scalar call, and the overridden array write forwards one bulk call. See [OpenJDK DataOutputStream](https://github.com/openjdk/jdk25u/blob/master/src/java.base/share/classes/java/io/DataOutputStream.java).

| Record | Published writer | Full-record candidate |
| --- | ---: | ---: |
| Typed literal with cached canonical datatype | 3: literal marker, framed label, cached QName | 1 |
| Typed literal with generic datatype | 5: literal marker, framed label, QName marker, namespace ID, framed local name | 1 |
| QName IRI | 3: marker, namespace ID, framed local name | 1 |
| Language/directed-language literal | 3: marker, framed label, framed language tag | 1 |
| Blank node | 2: marker, framed identifier | 1 |
| Newly declared namespace | 3: marker, ID, framed namespace | 1 |
| A run of adjacent NULL/REPEAT markers | 1 | 1 |

For the existing mixed eight-column ASCII or Unicode fixture after namespaces are established, this is **25 writes per row becoming 8**: two IRIs, three canonical typed literals, one language literal, one blank node, and one custom-datatype literal. That is a 68% reduction in write-call count, not a speedup estimate. The first row additionally declares four namespaces, giving 37 versus 12 writes for that row. An all-repeat row already uses one batched marker write and cannot improve through this mechanism.

For a canonical literal too large for complete-record staging, the candidate retains the published marker/string/cached-QName sequence. Generic typed literals can still benefit from a separately packed short QName when their label does not fit. Oversized individual strings keep the existing strict `CharsetEncoder` fallback and its allocation behavior.

The reason to test write count is the actual [QueryResponseHeartbeat](../core/http/client/src/main/java/org/eclipse/rdf4j/http/client/QueryResponseHeartbeat.java): both scalar and bulk payload methods enter `outputLock`, check terminal state, handle probing state, mark payload started, and forward to the target. Binary format declines scheduled whitespace heartbeats but still uses this wrapper. The experiment preserves that wrapper and its failure handling.

## Why start here rather than with a callback buffer

| Approach | Additional opportunity | Additional machinery and costs |
| --- | --- | --- |
| Complete short records, this candidate | Reduces common record writes to one without retaining completed records | Copies the cached QName suffix; adds bounded capacity checks; a failed staging attempt may repeat a bounded encoding pass |
| Callback-scoped buffer with drain after every public writer callback | Can combine a whole short row, including namespaces and markers, into one downstream write | Usually needs another byte array and copies every record; must handle buffer overflow, nested callbacks, externally exposed stream writes/flushes, and exceptions during draining |
| Persistent stream buffer spanning callbacks | Can combine rows as well | Changes successful-callback visibility, first-payload/HTTP commitment timing, disconnect detection, and ownership of pending bytes; broader scope than this experiment |

A callback buffer is a possible later measurement, but its drain must not call the underlying network `flush()` for every row. It must drain after successful header, row, and error callbacks, and before final completion, while defining what happens to incomplete bytes after a serializer or target-stream failure. A partial downstream failure cannot safely be treated as an ordinary retry: an unknown prefix may already have been delivered. Returning the original downstream stream also cannot substitute for draining a buffer above it.

Complete-record staging avoids this pending-output lifecycle: each successful record has already reached the supplied stream when its write method returns. The design still changes the amount and timing of an **incomplete** record delivered before an exception, so it should not be described as preserving every failure-observation detail. The broader HTTP reasoning is recorded in [the existing lifecycle analysis](../binary-writer-evidence/buffering-lifecycle.md).

## Compatibility and focused validation

The canonical datatype identity guard remains `datatype == coreDatatype.getIri()`, and namespace declarations still precede typed-literal records. Generic datatypes are read for each row, preserving alternate namespace splits and mutations between rows. All lengths remain big-endian UTF-8 byte lengths. The strict loop preserves NUL, two-/three-byte characters, valid surrogate pairs, and malformed-surrogate rejection; the standard fallback remains available. The shared datatype templates are copied into scratch and are never mutated.

Record headers reduce the payload capacity relative to the existing standalone string path. With this 4,100-byte array, the exact ASCII whole-record boundaries include:

| Record | Largest staged payload |
| --- | ---: |
| QName or namespace text | 4,091 bytes |
| Blank-node identifier | 4,095 bytes |
| Canonical `xsd:string` literal label | 4,080 bytes, including the 15-byte cached QName suffix |
| Generic typed literal | Label bytes + datatype local-name bytes at most 4,086 |
| Language literal | Label bytes + language-tag bytes at most 4,091 |

The non-ASCII fast path uses the conservative three-bytes-per-UTF-16-code-unit bound within each reserved region. Thus some encodings that would physically fit still take the fallback. A failed staging attempt can touch scratch bytes but cannot emit them; the fallback rewrites the strings before sending them. The candidate never grows retained storage to the largest seen literal.

The published [encoding tests](../core/queryresultio/binary/src/test/java/org/eclipse/rdf4j/query/resultio/binary/BinaryQueryResultWriterEncodingTest.java) already cover exact wire bytes, independent expected-byte construction, directed language, nested triples, namespace-before-datatype order, all core datatype families, unusual splits, mutable custom datatype IRIs, interleaved writers, document reuse, completed-row visibility, strict surrogate failures, and flush-without-close. [State tests](../core/queryresultio/binary/src/test/java/org/eclipse/rdf4j/query/resultio/binary/BinaryQueryResultWriterStateTest.java) cover reused binding sets and independent documents.

Before promoting this candidate, add targeted byte-equality/round-trip checks at the newly reserved-header boundaries above, including the second string overflowing a generic or language record and surrogate errors in each string position. A counting destination can verify the table independently of timing. Test a downstream failure during the new bulk record call and explicit flush/error output; larger calls necessarily change the point where a target observes the failure. The generic datatype local name and language tag are now obtained before staging completes, so exceptions from unusual user-supplied getters can leave a shorter incomplete record than before.

No compilation, test, profiling, or benchmark run was performed for this source experiment. Candidate SHA-256 at handoff: `53974c7280d700985d906ce1678e785266f5db020512f68f61e6a5dd64f146a8`.
