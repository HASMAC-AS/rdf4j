# Independent compact-token writer review

## Scope
Review only: replace generic serializer dispatch over the existing fully constructed List/Object[] packed row. Do not eliminate containers, alter RDF getter order, cache generators, change binding adapters, or combine ThreadLocal candidates. Current profile samples are not a removable wall-time fraction or a predicted speedup.

## Jackson 3.2.4 API verification
Read installed jackson-databind bytecode with javap (no behavioral JVM test/build): ObjectMapper.createGenerator(OutputStream) creates its SerializationContextExt, passes it to the configured stream factory, and invokes _initializeGenerator. ObjectMapper.writeValue(OutputStream,Object) uses the same context/factory (explicit UTF8) and _initializeGenerator before serialization. Prefer the same mapper API, not a fresh unrelated JsonFactory. Fresh generator per row retains root-context isolation and buffer recycling; close it before inspecting RowOutput.streaming/bytes or appending record suffix. RowOutput inherits no-op OutputStream.close/flush, so closing the generator does not close the servlet response.

**Concrete failure-cleanup issue:** _configAndWriteValue catches serialization Exception and calls ClassUtil.closeOnFailAndThrowAsJacksonE. This disables StreamWriteFeature.AUTO_CLOSE_CONTENT before closing and suppresses close failure onto the primary exception. Plain try-with-resources does not disable content completion. Preserve this detail: a failed row must not gain closing-array bytes only in the token arm. Same typed writeString uses Jackson escaping/UTF8 handling; do not manually escape or normalize invalid surrogate strings.

## Protocol and lifecycle review checklist
- Outer row array, null unbound entries, numeric tags 0/1/2/3, and packed-array arity remain identical.
- Blank-node reference is true at index3 with explicit null index2; ordinary blank nodes remain length2.
- Language uses explicit null datatype slot; directional language appends ltr/rtl. Plain typed literal uses datatype lexical string. Recursive triples retain exact nested array shape.
- All packed values are computed BEFORE row.reset/output, preserving later-getter failure ordering, including a huge first term followed by a failing second term.
- Fresh generator is closed/flushed before cap arithmetic; unchanged RowOutput performs capped batching, oversized transition, metadata/preamble ordering and client disconnect IO unwrapping.
- Synchronization remains unchanged so progress cannot interleave with a row; no raw JSON writes except existing protocol framing.
- Guard against generic fallback dispatch in compact row path with bytecode hit proof; keep noncompact metadata serializer untouched.

## Tests reviewed / extra qualification
Existing new focused tests cover exact packed bytes (Unicode/control escaping/nested triples), 32/1024 batching, explicit flush, cap/oversized order, later getter error, and structural no generic dispatch. Existing suite covers negotiated blank-node authority, disconnect IOException chain and progress interleaving.
Additional desired cases: malformed high/low surrogate after oversized valid prefix with partial-byte comparison to generic OutputStream serialization (not writeValueAsString, whose generator is different); nesting constraint failure; primary serialization failure plus close failure suppression; empty row / zero variables; LTR as well as RTL; exact cap boundary with multibyte text. At least one serialization failure test should distinguish AUTO_CLOSE_CONTENT behavior before accepting cleanup equivalence. No claim that all these have run.

## Status
Design acceptable subject to failure-cleanup parity and candidate review. No production changes or builds performed by reviewer. Writer owns test-first implementation and test results. Final source review follows when patch is ready.

## Final candidate source review
Reviewed writer source SHA-256 `c0d92c6536072216931aafe8cfb57822d5e5080b9f6c5a6936ad91c430ba77f0` after the failure-path refinement. No correctness blocker found for the selected private packed schema. Diff is imports, two typed-token helpers, and the compact add call substitution. Existing packed List/Object[] allocation, recursive RDF getters, RowOutput implementation, synchronization, noncompact output and generator-per-row lifetime are unchanged.

`writePackedRow` creates the mapper-initialized generator, uses successful close outside the serialization catch, and delegates exceptional cleanup to the same public ClassUtil helper used by ObjectMapper. This is an implementation-facing Jackson dependency and should be rechecked on Jackson upgrades. `writePackedValue` preserves all emitted arities, numeric tags, null slots, strings, booleans and recursive triples. Null entries remain supported. Array boundaries are outside local element catches, matching generic serializers. Exception-only prependPath(field,index) agrees with ObjectArraySerializer's bytecode (its wrapAndThrow uses the current element, not the enclosing array). Root indices use the containing List, matching the observed differential test. The private schema precludes arbitrary user serializer callbacks and malformed element classes; this is not a replacement general serializer.

Writer reports focused 24/24 green. Reviewed new depth test: it compares exact exception class/message and emitted oversized partial bytes to generic OutputStream serialization, preserving the chain and no auto-completion behavior. No tests run independently by reviewer; full module run remains writer-owned. This signoff is source correctness review for further HTTP measurement, not a performance or release claim.

Optional test hardening communicated: structural dispatch test currently inspects only CompactRows.add. Inspect helper bodies too and assert typed token calls, so merely moving generic serialization behind a helper cannot falsely satisfy the no-generic-dispatch guard. Current reviewed helper source does contain typed calls and no generic dispatch.
