# Large-string UTF-8 alternatives on JDK 25

Source analysis, 2026-11-17. Baseline: published RDF4J commit
`741b9389538e80214075692439a8cd25a8a9ae84`, writer SHA-256
`db7c2af9917591a274074854cd0e091222ef77006923901a959c3287450f7668`.
No production edits, builds, profiles, or timings were performed for this analysis.
Performance conclusions below are hypotheses to measure, not benchmark results.

## Recommendation

Measure **surrogate validation followed by `String.getBytes(UTF_8)`** first as
the smallest long-string change. Measure **strict length calculation followed
by bounded chunk encoding** separately: its strongest guaranteed advantage is
bounded temporary memory, while its speed depends on the character mix and
downstream stream cost. Keep **small initial scratch with bounded growth** and
**lazy encoder construction** as separate allocation experiments.

Do not select a universal winner from the existing ASCII-only long workload.
The JDK allocation behavior differs substantially for Latin-1, sparse Unicode,
dense three-byte characters, and surrogate pairs.

## Published behavior and the compatibility boundary

`core/queryresultio/binary/src/main/java/org/eclipse/rdf4j/query/resultio/binary/BinaryQueryResultWriter.java`
has an eager 4,100-byte scratch array and an eager UTF-8 `CharsetEncoder`.
ASCII strings of up to 4,096 UTF-16 code units use scratch. General Unicode
strings of up to 1,365 code units use the strict custom encoder. Larger strings
use `charsetEncoder.encode(CharBuffer.wrap(s))`.

For non-ASCII strings between 1,366 and 4,096 code units, the existing ASCII
attempt may scan and copy a long prefix before falling back and encoding the
whole string again. Inputs longer than 4,096 skip that attempt. For short
non-ASCII strings the custom path also restarts after an ASCII prefix attempt.
An isolated long-string experiment should keep these distinctions visible.

The required output remains ordinary UTF-8 preceded by a big-endian signed
Java `int` containing its nonnegative byte count. `BinaryQueryResultParser.readStringV2()`
reads that length and uses a strict UTF-8 decoder. In particular, NUL occupies
one byte and a valid supplementary code point occupies four bytes.

On malformed UTF-16 the published writer throws `MalformedInputException`
with input length 1, including a trailing high surrogate, isolated low
surrogate, high followed by an ordinary character, and high followed by high.
The caller may already have written record markers or namespace metadata, but
`writeString` has not written that string's length or payload when this error
occurs. A chunked replacement must validate the complete immutable `String`
before publishing its length or any payload if it preserves this behavior.
No whole-row or whole-document atomicity is implied.

## What JDK 25 actually does

Checked the installed Temurin `jdk-25.0.5+8/lib/src.zip`, particularly
`java.lang.String`, `java.nio.charset.CharsetEncoder`, `sun.nio.cs.UTF_8`,
`java.lang.StringCoding`, and `java.lang.StringUTF16`.

The current convenience encoder creates a `StringCharBuffer`, a heap
`ByteBuffer`, and its output array. UTF-8 advertises an average of 1.1 and a
maximum of 3 bytes per UTF-16 code unit. The convenience method initially
allocates approximately `1.1 * N` bytes, grows approximately twofold on
overflow, and copies existing output into each new array. A String-backed
character buffer has no accessible character array, so `UTF_8.Encoder.encodeLoop`
selects its buffer loop rather than its array loop. Actual object allocation
can be affected by JIT escape analysis; the output-array work is explicit.

`String.getBytes(StandardCharsets.UTF_8)` bypasses that generic machinery:

| String representation/content | JDK 25 work and output-array allocation |
| --- | --- |
| Compact Latin-1, all ASCII | `hasNegatives` scan, then clone of the `N` backing bytes. `countPositives`, used by the scan, is an intrinsic candidate. |
| Compact Latin-1, some non-ASCII | Scan until a high byte is found, allocate `2*N`, encode every input byte, then copy to an exact-size array unless the output fills `2*N`. |
| UTF-16 backing | Usually allocate `3*N`; encode an ASCII prefix followed by a general loop; copy to an exact-size array unless the output fills `3*N`. For an overflowing `3*N` estimate, JDK computes the exact size first. |

The ASCII prefix and general UTF-16 loops together traverse the input once;
they are not two complete passes. A caller's independent surrogate validator
adds another full character pass. On Latin-1, the JDK's high-byte scan adds
its own work even though the caller's validation has already established
validity. The public API does not expose the String coder so that caller
cannot safely skip validation merely because the JDK might use compact
Latin-1 internally.

Approximate total newly allocated **output-array bytes** for normal-sized
long inputs, excluding headers, wrappers, input Strings, and the writer scratch:

| Character mix (`N` is UTF-16 length, `B` is UTF-8 bytes) | Published allocating encoder | Validated `getBytes` |
| --- | ---: | ---: |
| ASCII, `B=N` | `1.1*N` | `N` |
| All non-ASCII Latin-1, `B=2*N` | `3.3*N` | `2*N` |
| Mixed Latin-1, e.g. `B=1.5*N` | `3.3*N` | `3.5*N` |
| Dense three-byte BMP, `B=3*N` | `7.7*N` | `3*N` |
| Only supplementary pairs, `B=2*N` | `3.3*N` | `5*N` |
| Mostly ASCII with one non-Latin-1 BMP character, `B` near `N` | `1.1*N` | Approximately `4*N` |

These are source-derived capacity sums, not measurements; rounding, array
limits, and growth-boundary cases change exact numbers. They explain why
validated `getBytes` can improve CPU while increasing allocation for some
Unicode distributions. It also performs trimming copies that the published
encoder does not perform, while avoiding the latter's generic buffer loop.

## Alternative A: strict validation, then `getBytes`

Scan UTF-16 code units for surrogates. A high surrogate must have a following
low surrogate; consume both. Any other surrogate throws
`new MalformedInputException(1)`. After successful validation, call
`s.getBytes(StandardCharsets.UTF_8)`, then write the returned length and bytes.
String immutability makes successful validation durable through encoding.

A direct validator needs only surrogate detection; it does not need UTF-8
byte-count classification. Do not call `CharsetEncoder.canEncode(s)` first:
in JDK 25 it calls the allocating convenience encoder internally, retaining
the expensive work this alternative is meant to remove. `String.codePoints()`
also does not by itself reject unpaired surrogates.

This option preserves malformed-input handling only with explicit validation.
`getBytes(Charset)` and `Charset.encode` replace malformed input by contract.
The replacement used by JDK 25's UTF-8 String encoder is `?`, so looking for a
replacement byte afterward cannot distinguish malformed input from valid
question marks.

It retains an output-sized allocation and can allocate substantially more
than the final payload. At extreme sizes, JDK 25's non-ASCII Latin-1 path
requests `2*N` before trimming; `StringUTF16.newBytesFor` rejects `N >=
(Integer.MAX_VALUE >> 1)`. Thus a huge mostly-ASCII Latin-1 String can fail
allocation even when its exact UTF-8 length would fit the wire's signed int.
The published generic encoder has different allocation limits. Do not claim
equivalent allocation-failure behavior or universal allocation reduction.

If this replaces the last `CharsetEncoder` use, remove that eager field too.
That changes per-document allocation in addition to long-string encoding.

## Alternative B: strict byte-length pass, then bounded encoding

The first pass validates surrogates and counts exact UTF-8 bytes. A simple
overflow-safe formulation starts with `long bytes = s.length()`, adds one for
each code unit in `0x80..0x7ff`, adds two for each non-surrogate code unit above
that range, and adds two per valid surrogate pair while skipping its second
code unit. The initial count already includes both units of a pair, yielding
four bytes in total. Check the final count against `Integer.MAX_VALUE` before
writing it.

The second pass emits into bounded reusable storage and writes filled chunks.
It may use the existing custom UTF-8 branches; validity has already been
established. Never split a surrogate pair into independent conversion calls.
Keeping at least four free bytes before encoding a code point makes chunk
boundaries simple; UTF-8 byte sequences may cross OutputStream calls legally,
but avoiding such splitting simplifies the implementation. Write the prefix
once, or place it before the first chunk in the existing four reserved bytes.

This performs two character passes, but removes output-sized intermediate
arrays and repeated array growth/copy. It introduces roughly `ceil(B/C)`
payload writes for scratch capacity `C`, so benchmark the real heartbeat
wrapper as well as memory output. A 4 KiB chunk is usually far less expensive
than per-byte writes, but it is still more calls than one array write.

A public-API implementation can instead count first, then reuse
`CharsetEncoder.encode(input, output, true)` over the entire remaining input,
draining on overflow and resetting/flushing in the documented sequence.
That bounds output memory, but retains the generic String-buffer encoding
loop and an input wrapper. It is a useful lower-code-risk comparator if the
custom chunk encoder's advantage is uncertain. Copying chunks through
`String.getChars` can unlock the JDK character-array path, at the cost of an
extra char buffer, copying, and surrogate-boundary handling; it should be a
secondary experiment rather than the first candidate.

Do not allocate `new byte[3 * N]` or `new byte[B + 4]` without checked bounds.
The latter can overflow even if `B` itself fits the protocol. Java's maximum
allocatable array can also be smaller than `Integer.MAX_VALUE`. Chunking needs
neither oversized array. A simple long accumulator is clearer than a delicate
wrapped-int check; if a branch-reduced integer formula is tried, its overflow
proof must cover every supported String length. Define the oversized-string
exception deliberately; the current allocating path can fail with
`OutOfMemoryError` before it reaches an explicit format-length check.

## Scratch sizing and public API limits

Making the existing 4 KiB scratch merely lazy helps writers that never encode
a string, but **does not remove its cost for most small documents**: their
column-name header allocates it immediately. A more relevant isolated variant
starts with a small payload capacity, such as 128 or 256 bytes, and grows
within a hard 4 KiB cap when needed. Two size tiers are simpler than arbitrary
geometric growth. Choose required capacity without overflow and preserve the
same short-string encoding coverage, so capacity changes do not accidentally
route previously allocation-free strings into the allocating fallback.

Growing a retained buffer to the largest observed literal is a different
memory policy and should be avoided here. A single large result would then
retain that memory for the rest of the writer's lifetime. Also measure lazy
construction of the existing `CharsetEncoder` independently: short-only
documents do not need it. A 4,096-row benchmark amortizes per-writer costs;
0-, 1-, 8-, and 64-row documents are needed to see this tradeoff.

JDK 25 has no public specialized String-to-UTF-8 byte-array API that both
avoids replacement and uses the internal String fast path. Package-private
`String.getBytesUTF8NoRepl` and `jdk.internal.access.JavaLangAccess` exist, but
are not supported exported APIs; no `--add-exports`, reflection into String
internals, or Unsafe access is justified for this library. The supported
strict API is `CharsetEncoder`. `OutputStreamWriter` with an explicit strict
encoder is public, but does not solve the length-prefix problem; its JDK 25
String write path also copies to a fresh `char[]`. The deprecated
`String.getBytes(int, int, byte[], int)` truncates chars and is not a general
UTF-8 encoder.

## Comparable implementations and isolated measurements

Current Protocol Buffers server code deliberately delegates to
`String.getBytes(UTF_8)`, reporting that it outperformed its custom loops on
their servers. Its mobile path differs. This is evidence to include the JDK
path in our experiment, not transferable performance evidence for RDF4J.
Its current malformed-input contract allows replacement, including in its
encoded-length fallback, so that code is not a strict drop-in. Its length
algorithm does illustrate an ASCII-prefix pass followed by byte-counting
branches and explicit overflow detection. Its optional `String.encodedLength`
path targets newer JDKs; that method is absent from the inspected JDK 25.

Apache Avro keeps a 63-byte ASCII scratch and uses `getBytes` for other
Strings. The 63-byte cutoff is tied to Avro's variable-length integer framing,
so it is not evidence for an optimal RDF4J cutoff. Its `Utf8` value overload
can reuse existing bytes, a representation change separate from these
String-based writer optimizations.

Recommended isolated variants, all against published `741b938`:

1. **Validated getBytes long fallback**; keep current short path and scratch.
2. **Strict counted custom chunks**; keep current short path and 4 KiB cap.
3. **Strict counted reusable CharsetEncoder chunks**, if a standard-API
   comparator would inform whether custom encoding is justified.
4. **Small initial bounded scratch**, otherwise unchanged encoding.
5. **Lazy CharsetEncoder**, otherwise unchanged encoding; then measure a
   combined candidate only after the individual results are understood.

For the long-path comparisons include ASCII, dense Latin-1, mixed Latin-1,
three-byte BMP, supplementary pairs, and sparse non-Latin-1 near the start
and end of a long ASCII string. Include 1,365/1,366, 4,095/4,096/4,097,
8 KiB, 64 KiB, and at least one much larger size where allocation/GC matters.
Use memory and heartbeat sinks. Ensure input lengths are described in UTF-16
code units or UTF-8 bytes explicitly; they are not interchangeable.

The existing `BinaryQueryResultWriterEncodingTest` already covers valid
Unicode boundaries, 1,365/1,366 three-byte characters, large inputs, malformed
surrogates, and recovery after errors. A chunked candidate additionally needs
malformed input after more than one would-be output chunk, pairs straddling
each chosen chunk boundary, and a check that no string prefix/payload escaped
before the malformed-input failure. Preserve byte-for-byte baseline output.

## Primary source references

- [Published RDF4J writer](https://github.com/eclipse-rdf4j/rdf4j/blob/741b9389538e80214075692439a8cd25a8a9ae84/core/queryresultio/binary/src/main/java/org/eclipse/rdf4j/query/resultio/binary/BinaryQueryResultWriter.java).
- [JDK 25 String contract](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/lang/String.html), `getBytes(Charset)`.
- [JDK 25 CharsetEncoder contract](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/nio/charset/CharsetEncoder.html), REPORT defaults and encoding sequence.
- [OpenJDK 25 String source](https://github.com/openjdk/jdk25u/blob/master/src/java.base/share/classes/java/lang/String.java), `encode`, `getBytesUTF8NoRepl`, `encodeUTF8`, `encodeUTF8_UTF16`. Also checked installed source SHA-256 `15cc88d0676a2193b72f56e6f1bb7efcebff9bf97ce5d1fdc01732636a23e38a`.
- [OpenJDK 25 CharsetEncoder source](https://github.com/openjdk/jdk25u/blob/master/src/java.base/share/classes/java/nio/charset/CharsetEncoder.java), convenience `encode` and `canEncode`; installed SHA-256 `1a64a504004f9593af27bf037173d0411b243606492f551eb077b0fcae6e25ba`.
- [OpenJDK 25 UTF-8 encoder](https://github.com/openjdk/jdk25u/blob/master/src/java.base/share/classes/sun/nio/cs/UTF_8.java), `Encoder`, `encodeBufferLoop`, `encodeLoop`; installed SHA-256 `df8352a74946f565b7c54e468413c3f001caa6ad70a004f3561e06c88d12849f`.
- [Protocol Buffers Utf8](https://github.com/protocolbuffers/protobuf/blob/main/java/core/src/main/java/com/google/protobuf/Utf8.java), `Processor.encodedLength`, `ServerProcessor`, and `ServerProcessorWithEncodedLength`, retrieved 2026-11-17.
- [Apache Avro BinaryEncoder](https://github.com/apache/avro/blob/main/lang/java/avro/src/main/java/org/apache/avro/io/BinaryEncoder.java), `writeString(String)` and `writeString(Utf8)`, retrieved 2026-11-17.
