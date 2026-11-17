# Candidate equivalence review

**Verdict: no source-level correctness blocker found; approved for isolated compilation, validation, and measurement.** This is a read-only review against published commit `741b9389538e80214075692439a8cd25a8a9ae84`, not a claim that the candidates' tests or benchmarks have passed. No production edits, builds, tests, or timing runs were performed by this review.

| Area reviewed | Finding |
| --- | --- |
| Successful wire records | Each staged record preserves the published marker, big-endian integer/string lengths, and payload order. Generic typed literals place their QName at the label's exclusive end; language literals place their second string there. QName/namespace headers occupy the five bytes before the framed string. |
| Datatype namespaces and state | Namespace definitions still precede typed-literal records. Namespace IDs enter the table only after successful declaration output. The canonical identity guard, static-template cloning, per-writer namespace patching, and `startQueryResult()` cache resets are unchanged. Alternate datatype splits and mutations between rows retain the generic path. |
| PrefixRecord | The copied ASCII prefix remains at its original offsets. Encoding resumes at the first non-ASCII code unit; the capacity bound covers only remaining units/bytes. Supplementary characters consume four bytes for two units and remain within the bound. |
| AdaptiveRecord | Both growth sites use `Arrays.copyOf`, preserving earlier staged strings, their length prefixes, and the current ASCII prefix. Increasing `limit` by the array-size delta preserves the caller's reserved suffix. Returned offsets remain absolute indexes in the new array. |
| Integer and array bounds | Before minimum-size arithmetic, length must fit the maximum available payload. Before multiplying remaining units by three, the division-based maximum-capacity check must pass. Required allocation sizes are therefore at most 4,100; doubling is at most 8,200 before capping. Capacity after the ASCII scan is nonnegative. Growth cannot shrink the array. |
| Malformed UTF-16 | All candidates reject isolated or mismatched surrogates through the same strict loop or existing `CharsetEncoder` fallback. A capacity failure may defer validation to fallback but does not accept invalid input. Subsequent calls rewrite scratch contents; the convenience fallback encoder resets its own encoding state. |
| IOException propagation | Bulk writes propagate failures to the existing public callback wrapper. No new catch suppresses an exception, no automatic retry can resend an uncertain prefix, and no buffered drain remains to run after failure. Header/error/end and explicit-flush structure remain unchanged. |
| Row visibility and reuse | Each successful record is sent before returning; existing marker runs drain before a value or at row end. Previous-value updates, EMPTY_ROW behavior, and writer reuse remain unchanged. Adaptive storage may remain enlarged across documents, but is bounded and contains no pending output. |

## Observable differences to retain in the handoff

The candidates preserve successful bytes for values stable during their callback. They deliberately change output-call boundaries. A malformed label/local name, an exceptional custom getter, or a downstream failure can therefore expose a shorter prefix of an **incomplete** record than the published writer. Generic datatype local names and language tags are obtained earlier than before. Code that mutates a value from inside an output-stream callback cannot rely on the former getter/write interleaving. These are not hidden cross-callback buffering effects, but claiming identical failure-observation timing would be inaccurate.

The existing tests cover the surrounding state and strict encoding contracts. Promotion should additionally use the new header-reservation and growth cases already identified in [full-record-analysis.md](full-record-analysis.md) and [prefix-adaptive-review.md](prefix-adaptive-review.md), especially growth in the second staged string. Those are the concrete remaining verification targets; this review did not discover a new implementation defect.

## Reviewed source identities

| Candidate | SHA-256 |
| --- | --- |
| FullRecord | `53974c7280d700985d906ce1678e785266f5db020512f68f61e6a5dd64f146a8` |
| PrefixRecord | `0244821ec2a2cbabf3e7c38cadd8c40e71c99c55f59f2d7753ba4cb232217d3b` |
| AdaptiveRecord | `03b530b180d1d4589ad3c4cfdeda3e30f3efe60f7b2604cc144ca4d93e0e23aa` |
