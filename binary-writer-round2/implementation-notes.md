# Implementation notes: second binary-writer optimization

This change extends published commit `741b9389538e80214075692439a8cd25a8a9ae84`. The implementation is in [BinaryQueryResultWriter.java](../core/queryresultio/binary/src/main/java/org/eclipse/rdf4j/query/resultio/binary/BinaryQueryResultWriter.java). After renaming the selected Combined candidate to the production class, the formatted production file differs only in two Javadoc line wraps. Its reviewed SHA-256 is `4a05a2794ff59c05b52cf0c7f70cacedc1a5602d080f1c5a55bae8599c455799`.

## What changed and why

**Complete short records share one write.** `writeNamedRecord`, the two `writeTypedLiteral` overloads, language literals, and blank nodes assemble their marker, fields, and UTF-8 payload in the existing scratch storage. Canonical typed literals append the already-cached datatype QName. The previous static datatype templates, document-local namespace IDs, and canonical IRI identity guard remain in place. This removes repeated stream entry for individual fields without adding a persistent output buffer.

**Scratch allocation follows demand.** Each writer starts with 132 bytes rather than 4,100 and grows geometrically or directly to the required size, capped at 4,100. `encodeString(text, offset, limit)` reserves headers/suffixes, preserves earlier staged strings when growing, and returns an exclusive end or `-1`. Growth increases the passed limit by the capacity delta, keeping reserved suffix space intact. The existing ASCII prefix is reused when encoding continues with non-ASCII text; only the remaining suffix enters the conservative three-bytes-per-code-unit capacity calculation.

**Large strings use bounded chunks.** The ordinary large-string path first validates UTF-16 and computes its exact UTF-8 byte length. It then publishes that length and encodes into 4,096-byte payload chunks. This removes the output array proportional to a large literal's size. The tradeoffs are a counting/validation pass, another pass for encoding, and more stream calls than one large payload write. A `CharsetEncoder` is no longer created for every writer.

The production scope remains one writer class plus its focused encoding tests. There are no new dependencies, public APIs, format versions, parser changes, LMDB changes, or HTTP-wrapper changes.

## Preserved contract

Successful output retains the same binary markers, big-endian lengths and IDs, ordinary UTF-8 encoding, and namespace-before-datatype ordering. The static datatype cache remains bounded by the core vocabulary; namespace-bearing records remain writer-local and reset for each document. Generic datatypes preserve their own namespace/local-name split and are read afresh between rows.

Malformed surrogate input is still rejected. The large-string validation pass completes before that string's length or payload is sent. NULL/REPEAT batching, EMPTY_ROW semantics, previous-column snapshots, nested triple handling, completed-row visibility, explicit stream flushing, final flush-without-close, and exception propagation retain their contracts. The [encoding tests](../core/queryresultio/binary/src/test/java/org/eclipse/rdf4j/query/resultio/binary/BinaryQueryResultWriterEncodingTest.java) exercise independent expected bytes, new record boundaries and growth cases, malformed text, output failures, and flush behavior; [state tests](../core/queryresultio/binary/src/test/java/org/eclipse/rdf4j/query/resultio/binary/BinaryQueryResultWriterStateTest.java) cover writer and binding-set reuse.

## Explicit limits

The writer does not promise identical delivery of an **incomplete failed record**. Staging can postpone a record's first bytes, while chunking changes where a target sees a payload failure. Datatype local names and language tags are also obtained earlier in some paths. No automatic retry is added, and no successful callback leaves pending output in a new stream buffer. Existing enclosing markers or earlier fields may already have been sent when validation of a later string fails.

For a Java String longer than `Integer.MAX_VALUE / 3` UTF-16 units, the implementation retains a local strict JDK encoder and its allocating buffer. This avoids overflow in the custom byte-count calculation while retaining the prior extreme-input behavior. Consequently, bounded scratch storage does not mean bounded temporary allocation for every theoretically possible String. An adaptive staging attempt may also grow before declining an input; malformed text is not guaranteed to allocate nothing.

## Maintenance assessment

No blocker or serious simplification warrants reopening the validated implementation. The new helpers keep framing choices local to their record type. The two UTF-8 loops deliberately have different contracts: the short-record loop validates while staging; the chunk loop relies on the prior complete validation pass. A common per-character abstraction would change those paths and deserves its own evidence rather than a late cleanup.

The key maintenance invariants are explicit in the code: preserve suffix reservations during growth; copy earlier staged bytes while a record is still being assembled; discard scratch contents only after abandoning that staging attempt; validate before chunk emission; and retain the four-byte-per-iteration chunk budget. The arithmetic and composition reviews are in [candidate-equivalence-review.md](candidate-equivalence-review.md) and [combined-review.md](combined-review.md). This note reports no performance estimates or benchmark conclusions.
