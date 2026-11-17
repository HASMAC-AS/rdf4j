# Prefix reuse and adaptive scratch candidates

These are isolated source experiments derived from `FullRecordBinaryQueryResultWriter`; production and FullRecord remain unchanged. No compilation, test, profiling, or timing run was performed by this subtask.

## PrefixRecord

[PrefixRecordBinaryQueryResultWriter.java](harness/PrefixRecordBinaryQueryResultWriter.java) changes only the class/constructor name and three lines in `encodeString`: the capacity check uses remaining UTF-16 units and remaining payload bytes, the output position starts after the existing ASCII prefix, and the UTF-8 loop continues at the first non-ASCII character.

If the prefix has `i` ASCII characters, its bytes are already staged. The remaining bound is `(length - i) <= (capacity - i) / 3`. This both avoids encoding the prefix twice and admits mixed strings with a long ASCII prefix that the old whole-string three-byte bound rejected. The bound remains conservative for the non-ASCII suffix; it does not promise that every physically fitting UTF-8 string uses the fast path. Malformed surrogate handling and fallback encoding are unchanged.

## AdaptiveRecord

[AdaptiveRecordBinaryQueryResultWriter.java](harness/AdaptiveRecordBinaryQueryResultWriter.java) derives from PrefixRecord. The scratch array begins at **132 bytes: four prefix bytes plus 128 payload bytes**, and grows geometrically, or directly to the required size, with a **4,100-byte ceiling**. The only additional state change is that the scratch-array reference becomes mutable. Growth occurs within `encodeString`, first to fit the minimum one-byte-per-code-unit requirement, then if necessary to fit the remaining suffix's conservative UTF-8 bound.

The caller's reserved suffix remains intact. Let `B` be the current array length, `R = B - limit` the reserved suffix, `o` the string offset, and `C = limit - o - 4` its payload capacity. Growing by `delta` changes both `limit` and `C` by `delta`, while `R` remains constant. This matters for canonical datatype suffixes and for the marker/ID/length space reserved after a generic literal label.

`Arrays.copyOf` preserves the entire previous array. It therefore retains earlier staged strings, their length prefixes, and the current ASCII prefix when the second string in a generic or language literal triggers growth. No wire bytes are emitted during an unsuccessful staging attempt or a growth operation.

## Source review of arithmetic and state

- Before minimum-capacity arithmetic, `length` is checked against `C + 4100 - B`. A very long Java String returns to the existing fallback before addition can overflow. The admitted required size `B + length - C` is at most 4,100.
- Before multiplying remaining code units by three, the code checks them against `(C - i + 4100 - B) / 3`. The multiplication is therefore bounded by the available payload at the maximum array size.
- Doubling the current array length is at most 8,200 before the result is capped at 4,100. Both growth branches strictly increase capacity when taken. There can be at most five growth copies from the initial 132-byte array.
- The initial minimum-capacity check guarantees `i <= length <= C` during the ASCII scan, so `C - i` is nonnegative. A valid surrogate pair consumes four bytes for two UTF-16 units, within the three-per-unit bound.
- Partial scratch contents after `-1` or a malformed-input exception are not pending output. Subsequent staging rewrites its own lengths and payload; fallback methods emit independently. Growth may remain after a failed attempt, but retained storage stays bounded.
- AdaptiveRecord trades lower initial allocation for growth branches and possible extra allocations/copies on larger records. These are candidates for measurement, not assumed improvements.

## Focused validation implications

In addition to FullRecord's byte-boundary checks, exercise:

1. Long ASCII prefixes followed by a two-byte character, a three-byte character, or a supplementary pair; include a malformed surrogate after the same prefixes. These distinguish prefix reuse and the revised remaining-capacity bound.
2. Growth while staging the **second** string: a generic label of 105 ASCII characters followed by a 14-character ASCII datatype local name; and a 100-character label with local name `abcdef` followed by six `中` characters. The latter reaches Unicode-driven growth after a local-name ASCII prefix has already been staged.
3. A language literal with a 120-character ASCII label and `en-US`, which requires preserving the label when the language tag grows the array.
4. Exact whole-record boundaries at the 4,100-byte maximum, then one byte beyond; progressive record sizes that cause multiple growths; and inputs too large for any staging attempt.
5. Valid output after malformed input, existing namespace/template reuse, output visibility after callbacks, and downstream bulk-write failures.

Candidate SHA-256 values at handoff:

| Candidate | SHA-256 |
| --- | --- |
| FullRecord, unchanged | `53974c7280d700985d906ce1678e785266f5db020512f68f61e6a5dd64f146a8` |
| PrefixRecord | `0244821ec2a2cbabf3e7c38cadd8c40e71c99c55f59f2d7753ba4cb232217d3b` |
| AdaptiveRecord | `03b530b180d1d4589ad3c4cfdeda3e30f3efe60f7b2604cc144ca4d93e0e23aa` |
