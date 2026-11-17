# Combined and bounded-chunk review

**Verdict: no source-level blocker found.** This review inspected `CombinedBinaryQueryResultWriter` and independently checked the large-string loop from `BoundedChunkedBinaryQueryResultWriter`. It did not execute either candidate, build, test, profile, or measure performance.

## Composition

Source comparison confirms that Combined preserves AdaptiveRecord's `encodeString` exactly. Its `utf8Length` matches BoundedChunked exactly, and its large-string body differs only by allocating the full scratch array after validation when needed. There is no eager `CharsetEncoder` field.

Every call to `writeLargeString` comes through `writeString` after the containing whole-record staging attempt has been abandoned. Canonical literals keep the datatype suffix in a separate cached array. Generic and language literal fallbacks write their strings again; no caller subsequently consumes the abandoned label offsets or scratch contents. Replacing the array with a fresh 4,100-byte array is therefore safe here, whereas growth *inside* `encodeString` must continue copying existing staged bytes.

The large-string path validates the entire current string and calculates its exact UTF-8 byte length before allocating its chunk array and before writing that string's four-byte length. Existing enclosing markers or earlier strings may already have reached the stream. Adaptive staging can itself grow before declining an input, so this is not a guarantee that malformed input never causes any scratch allocation.

For input lengths greater than `Integer.MAX_VALUE / 3`, the candidate deliberately uses a fresh local strict JDK encoder and its allocating output buffer. This preserves the old extreme-input path and avoids arithmetic overflow in the new length calculation. The bounded-memory claim must therefore be limited to the custom chunked path, not every theoretically possible Java String.

## Independent capacity and progress proof

`utf8Length` begins with one byte per UTF-16 unit, then adds one for a two-byte character and two for a three-byte character or valid surrogate pair. The pair's two units were already counted, giving exactly four bytes. It rejects unmatched surrogates. Its caller bounds input length `n` by `MAX_VALUE / 3`, so the total is at most `3n <= MAX_VALUE - 1`.

The chunk capacity is 4,096 bytes in both candidates. For a Unicode chunk with `F` free bytes, the inner range permits at most `floor(F / 4)` iterations. Each iteration emits at most four bytes. A surrogate pair advances the input by an additional code unit and can cross the chosen range's end by one unit; that reduces, rather than increases, the iteration count. Full-string validation guarantees the matching low surrogate exists. Hence the chunk cannot exceed its byte capacity and the low surrogate is not processed twice.

The inner loop starts only with at least four free bytes, giving at least one iteration while input remains. Every iteration advances the input and emits bytes. Thus each outer iteration produces a nonempty chunk and terminates or advances; there is no zero-write/infinite-loop case. `end = i + min(length - i, free / 4)` is bounded by input length, so that addition cannot overflow. The ASCII path likewise uses a positive bounded `min(capacity, length - offset)` count.

After validation, `byteLength == length` proves the string is entirely ASCII: every valid non-ASCII code point contributes additional bytes. The ASCII shortcut therefore emits the same UTF-8 bytes. Both paths propagate output failures without retries or deferred drains. Subsequent strings overwrite scratch contents normally.

## Reviewed source identities

| Candidate | SHA-256 |
| --- | --- |
| BoundedChunked | `2e824b584e2917ee94e337dd804dad10d6b6f99ecf96172e2b22e129d072c17d` |
| Combined | `59da79bfd31d7f28df8ea503b1c462255487eef36527a827c0a327722dcef825` |
