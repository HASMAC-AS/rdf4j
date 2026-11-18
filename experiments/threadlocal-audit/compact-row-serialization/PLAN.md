# Protocol-specialized compact-row serialization

## Evidence and scope decision
Parent approved this distinct non-ThreadLocal experiment after reviewing actual HTTP profiles. Jackson `_verifyValueWrite` was the top sampled leaf in all four holder arms (106/100/130/75 samples); generic list/array serialization connects it directly to Workbench compact rows. This does not make validation removable or establish a percentage speedup. ThreadLocal audit conclusions remain separate; two map-entry experiments did not demonstrate HTTP gains. See ../read-resource-workbench/NEXT-TARGET-COMPARISON.md.

## Bounded candidate
Keep existing packed row List/Object[] construction, including all RDF getter/prepacking failures before serializer output. Replace only generic ObjectMapper serializer dispatch with schema-specific Jackson token calls for the fixed packed-term protocol. Keep a fresh generator per row, Jackson escaping/validation, exact term encoding, record cap, oversized streaming, flushing, disconnect behavior and exception translation. Do not fold in generator reuse, array elimination, binding round-trip changes, resource holder, initializer restoration or cancellation experiments.

## Plan
- [in_progress] Establish byte-identical baseline and serialization contracts
- [todo] Implement minimal protocol-specialized token emission
- [todo] Independently review and validate output failures
- [todo] Compare qualified actual HTTP factorial configurations
- [todo] Publish complete evidence and qualified recommendation

## Qualification
Capture baseline output/differential contracts and a structural expected-red test before production edits. Exact bytes and parsed RDF terms for bound/unbound cells, IRIs, blank-node provenance, languages, directional literals, datatypes, nested triples, escaping/Unicode/surrogates, unsupported or throwing values, large rows and cap crossings. Preserve first-batch and subsequent-batch boundaries, interleaved metadata/progress/terminal records, output ownership, flush/disconnect exceptions and no unbounded buffering. Match original frozen Workbench class-family bytes before compiling the isolated candidate.

## Runtime and performance
Use immutable base5ac89b5 and depth0750336 engine identities, frozen reconstructed Server/Workbench runtime, pinned #6440 writer, compact identity mapping and full100k six-slot query. Overlay only the changed Workbench writer family. Four arms: base, depth, serializer, depth+serializer. The prior holder experiment is excluded from all arms. Four balanced Latin blocks, fixed60warm/30measured requests per fresh server, all observations retained; report convergence without adaptive trimming. Primary increment is serializer+depth versus depth; include serializer/base, all contrasts and interaction. No browser paint or concurrent-capacity claims.

## Ownership
writer_guard_design owns tests/implementation and first CPU window. native_token_path reviews protocol/Jackson/ownership semantics independently. inventory freezes provenance and later owns exact-output HTTP qualification and matched timings. Coordinator checkpoints authorized source and evidence. Canonical benchmark lock prevents builds/tests/profiles overlapping clean timings. Preserve all artifacts and historical negative results.
