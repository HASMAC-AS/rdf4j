# Specialized compact row serialization: correctness

The candidate replaces generic Jackson List/Object[] serializer dispatch with typed token emission over the **same already-constructed packed row**. No getter, materialization boundary, packed array, binding adapter, batch threshold, buffer cap, synchronization, flush, or engine/TLS code is removed. Fresh generators and Jackson validation/escaping remain. The sampled `_verifyValueWrite` leaf is not bypassed; any performance benefit must come from surrounding dispatch and be measured separately.

## Frozen identity

Production source SHA-256: `c0d92c6536072216931aafe8cfb57822d5e5080b9f6c5a6936ad91c430ba77f0`.

All three pre-change WorkbenchPageResultWriter family classes were byte-identical to the actual HTTP application's immutable Workbench JAR, independently extracted by the benchmark worker. Candidate overlay contains exactly the same three class names; hashes in `candidate-manifest.json`. No other target/classes belong in the benchmark overlay.

## Evidence

- Pre-change focused suite: 17 existing writer tests passed; five new behavioral tests passed and one expected structural generic-dispatch test failed.
- A later depth-limit regression passed on the frozen original writer and initially caught a candidate diagnostic-chain difference. Partial bytes already matched; exception-only path restoration corrected the difference.
- Final production source passed the complete Workbench JVM module run: **1,561 tests, zero failures/errors/skips**, 3m17s.
- After that run, one additional test-only structural assertion verified typed calls inside helpers, not just the calling method. Same production source passed the expanded focused selection: **25/25**.
- Explicit base+serializer and depth+serializer immutable HTTP runtime overlays each passed the expanded **25/25** selection. These focused results overlap module cases; do not sum them as unique coverage.

No frontend build/browser execution or full repository pass is claimed. Independent source/bytecode review approved the production source without a blocker.

## Preserved edge contracts

Tests compare exact compact row bytes with generic Jackson encoding, including null columns, UTF-8/escaping/surrogates, nested triple terms, typed/language/directional literals, blank-node identity/provenance, 32-row first batch then 1024-row batches, explicit flush, byte-cap splits and oversized streaming. A throwing later term still fails before any row bytes, even after an earlier 300 KB term was prepared. Existing tests cover disconnect exceptions and progress unable to interleave ordinary/oversized row records. Module tests cover heartbeat, cancellations, result reporting and query routes.

Deep nesting failure compares actual partial bytes, exception class and full reference-chain diagnostic against generic serialization. The implementation uses the same `ClassUtil.closeOnFailAndThrowAsJacksonE` helper as ObjectMapper: it disables auto-completing failed JSON and preserves close-failure suppression. Catch scope remains Exception, not Error; successful close stays outside that catch. This public implementation-facing Jackson helper is an explicit maintenance dependency.

Initial baseline test-oracle mistakes involving Java varargs flattening are preserved in earlier logs; they were corrected before production changes and are not attributed to the baseline. The successful later evidence is the acceptance basis.

## Performance handoff

Benchmark candidate family: `compact-json/candidate-layer`. Preserve original mapper/Jackson versions and all existing HTTP runtime identities. Evaluate serializer alone and combined with qualified depth against matched baselines; do not add the resource holder, unfavorable cancellation experiment, or memo test seam. No speedup conclusion follows from correctness tests or sample counts.
