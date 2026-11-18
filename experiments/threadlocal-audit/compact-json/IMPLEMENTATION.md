# Compact Workbench row serialization

1. completed: capture immutable actual-HTTP baseline writer identity and test-first exact-byte, error-order and dispatch evidence.
2. completed: schema-specialized typed Jackson tokens over the SAME fully packed row, fresh generator each row, with independent review.
3. completed: matching behavioral suites and class provenance, frozen overlay handoff for actual HTTP comparison.

## Bounded design

`CompactRows.add` currently calls ObjectMapper.writeValue(row, packedValues), creating a new UTF8 generator and dispatching List/Object[]/Integer/String serializers. Replace generic dispatch only. Create generator through the same ObjectMapper; preserve per-row lifetime and auto-close. Emit start/end array, integer tag, nullable strings/boolean, recursively packed triple terms through normal Jackson token methods. Keep `_verifyValueWrite`, escaping, nesting constraints and validation. No raw JSON or serializer validation bypass. Preserve all RDF access and packedTerm completion before RowOutput.reset/serialization; leave row batching, bounded buffering, oversized streaming, flush, synchronization, preamble/progress and failure unwrapping unchanged.

## Evidence contrast

Actual depth HTTP profile: UTF8JsonGenerator._verifyValueWrite 63 leaves, 60 with generic ObjectArray/List serializer chain. JsonWriteContext/UTF8JsonGenerator/IOContext show sampled allocation weight, not exact allocation counts or removable cost. The new four-arm profile also identifies this actual path; no top-leaf-removal claim. Remaining read-resource/TLS candidates are separately measured and not combined speculatively. Typed tokens target generic dispatch and serializers, not required JSON validation or generator allocation.

## Independent next candidate, deferred

QueryEvaluator.addResult projects each BindingSet into a reused ArrayList then a new Object[]; TupleResultBuilder.result builds a QueryBindingSet and SimpleBinding wrappers; WorkbenchPageResultWriter looks up each declared name again before packing. Binary parsing already creates ListBindingSet with linear name lookup. The server's CompactBindingSetSnapshot.slot also appears in the profile under BinaryQueryResultWriter, but it is a different side of the HTTP boundary. These copying/lookup paths merit a separate guarded optimization, not this patch. Direct row/token emission could alter getter order, lazy materialization, partial-error behavior, or adapter conversion semantics.

## Test-first progression and review

All three pristine writer family classes are byte-identical to the frozen actual HTTP application JAR (baseline-family-comparison.json). Initial oracle incorrectly flattened Object[] through List.of varargs; preserved logs show this test-only error, corrected with singletonList before production changes. Final pre-change new suite: 6 tests, 5 behavioral passes and one expected structural generic-dispatch failure; existing writer suite 17/17 passed. Added deep constraint regression later proved candidate partial bytes already matched but exception reference-chain text differed. The same test passes against the frozen baseline. Exception-only prependPath restores the generic list/array diagnostic chain; candidate focused selection then passed 24/24.

The independent reviewer verified mapper initialization and a crucial failure-close detail: ObjectMapper disables AUTO_CLOSE_CONTENT through ClassUtil.closeOnFailAndThrowAsJacksonE. Candidate deliberately uses that same public implementation-facing helper instead of try-with-resources, which could append closing brackets after a failed oversized row. Successful close remains outside catch(Exception); Error handling is not broadened. This Jackson implementation dependency is a maintenance cost. Final source review approved SHA c0d92c6536072216931aafe8cfb57822d5e5080b9f6c5a6936ad91c430ba77f0 with no blocker.

Candidate has three family classes, no new production dependencies, no engine/TLS modifications. Full Workbench JVM module suite passed 1,561 tests with zero failures/errors/skips; expanded focused suite and immutable base/depth overlay checks each passed 25/25. Frontend build/tests are not part of this JVM-only window.
