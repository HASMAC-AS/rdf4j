# Next-target comparison from completed HTTP profiles

Read-only analysis; no new JVM run, code change, candidate selection or timing. The holder is not adopted. All counts refer to one separate profile per arm; sample shares are not wall-time percentages.

## Comparable sample populations

| Arm | Java samples | TLS-visible, count (%) | NDJSON nearest, count (%) | Binding nearest, count (%) | Lifetime/read/replay nearest, count (%) |
|---|---:|---:|---:|---:|
| base | 1007 | 119 (11.8%) | 131 (13.0%) | 118 (11.7%) | 257 (25.5%) |
| depth | 1019 | 115 (11.3%) | 151 (14.8%) | 121 (11.9%) | 259 (25.4%) |
| resource | 959 | 112 (11.7%) | 183 (19.1%) | 142 (14.8%) | 237 (24.7%) |
| both | 870 | 82 (9.4%) | 116 (13.3%) | 136 (15.6%) | 198 (22.8%) |

The three nearest-work categories are mutually exclusive by the existing classifier. TLS visibility is a separate overlapping property and MUST NOT be added to those categories. Inclusive frames nest inside each other: e.g. executeTrackedRead includes executeObservedRead and downstream work. No inclusive frame totals are added together here.

## 1. Strongest larger non-TLS alternative: compact Workbench row serialization

`UTF8JsonGenerator._verifyValueWrite` leaf counts are **106 / 100 / 130 / 75** (base/depth/resource/both), versus **119 / 115 / 112 / 82** samples with any TLS frame. This one Jackson leaf has consistently substantial visibility, while the whole exclusive NDJSON category has **131 / 151 / 183 / 116** samples. This is not proof that JSON dominates total latency, but it is a stronger localized non-TLS candidate than another small map-operation tweak.

Actual top-stack witness: `_verifyValueWrite → writeString → StringSerializer → ObjectArraySerializer → IndexedListSerializer → ObjectMapper.writeValue → WorkbenchPageResultWriter.CompactRows.add → handleSolution`. In base, one exact top stack has 94 samples. This is actual compact-row Workbench output, not the XML writer or a synthetic serializer.

[WorkbenchPageResultWriter source](https://github.com/HASMAC-AS/rdf4j/blob/7f656b80/tools/workbench/src/main/java/org/eclipse/rdf4j/workbench/util/WorkbenchPageResultWriter.java#L253): handleSolution builds an ArrayList per row and Object[] packed terms, then CompactRows.add calls ObjectMapper.writeValue per row (around lines 500–538). That incurs generic serializer dispatch and generator/context setup around a fixed known wire grammar. Read-only proposal: compare a typed direct JsonGenerator row encoder against this generic object graph, initially retaining the existing capped RowOutput, record batching and oversized-row streaming. Generator lifetime reuse is a separate harder change; do not combine it blindly. A direct generator still needs value-state validation, so the hottest leaf will not automatically vanish.

Correctness gates: exact packed term grammar for IRI, blank origin references, datatype/language/directional literals, nested triple terms, nulls, escaping and malformed surrogate handling; first record batching, MAX_ROW_RECORD_BYTES, oversized-row streaming, cancellation/client disconnect, heartbeat invalidation and terminal completion. Keep server binary writer and protocol unchanged. Judge fresh matched HTTP throughput/full/first-row latency, not only encoding microbenchmarks.

## 2. Strongest structural TLS alternative: reduce repeated observed-read scope admission

Nearest TLS callers (exclusive assignment among TLS-visible samples):

| Arm | executeTrackedRead | executeObservedRead | fused attach | external initializer |
|---|---:|---:|---:|---:|
| base | 23 | 16 | 5 | 21 |
| depth | 11 | 8 | 17 | 22 |
| resource | 21 | 22 | 12 | 11 |
| both | 4 | 5 | 13 | 14 |

[Depth LmdbStoreConnection source](https://github.com/HASMAC-AS/rdf4j/blob/0750336e71ab0ca0d216ce9c36483a6e9063337d/core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/LmdbStoreConnection.java#L2607): tracked reads route to executeObservedRead, which binds foreground value scope, external initializer, read resources and replay-worker admission, then validates snapshots and unwinds. Actual sampled stacks show ReplayableIteration.hasNext and next each passing through this wrapper during RepositoryValueIteration.prepareNext. A structural successful-pull scope could eliminate repeated setup, not merely relocate one TLS entry.

This is higher risk than serializer specialization. Repository/source-pull scoping already had a previous isolated optimization without a convincing combined HTTP win. Any additional consolidation must prove exact owner identity, callback/reentrant scope restoration, pre/post cancellation, snapshot validation, replay barriers, failure cleanup and result exposure ordering. Do not simply keep scopes installed across externally visible iterator calls or remove lifecycle checks. Static paths show an opportunity to investigate, not that two independently guarded operations are semantically mergeable.

## 3. Smaller TLS candidate: identity-aware fused session attachment

[Fused runtime attach and Scope](https://github.com/HASMAC-AS/rdf4j/blob/5ac89b5c137030fea1aa6aa7160984423d7f4cd5/core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/LmdbFusedSipFactorizedRuntime.java#L65) reads CURRENT, allocates a non-owning Scope, unconditionally sets the session, then restores/removes on close. Nearest attach TLS samples are 5/17/12/13. Investigate whether same-session nested attaches can skip map mutations while preserving scope ownership and closure contracts. Actual same-session frequency is NOT measured by these samples, so require untimed path proof before implementing a hot-path optimization. This is a more bounded correctness surface than broad read admission, but a smaller likely target and another map-write optimization may again be invisible end to end.

## 4. Secondary binding/name lookup candidate

CompactBindingSetSnapshot.slot leaf counts are 38/20/21/16; exclusive binding work is 118/121/142/136 samples. The base top stack contains 24 samples through slot → index → getValue → BinaryQueryResultWriter.handleSolutionImpl. [Snapshot source](https://github.com/HASMAC-AS/rdf4j/blob/5ac89b5c137030fea1aa6aa7160984423d7f4cd5/core/sail/api/src/main/java/org/eclipse/rdf4j/sail/features/CompactBindingSetSnapshot.java#L94) hashes names for lookup. A negotiated fixed-schema indexed access path could avoid repeated name lookup without assuming arbitrary BindingSet order. However the earlier sequential hint did not demonstrate a win; do not rebrand that same approach. Needs schema identity proof, order/absent-binding tests and a genuinely distinct implementation.

## Allocation evidence does not identify a winner

Top sampled allocation weights vary wildly: base has Instant (~5.08 GB), EOFException (~3.28 GB), HashMap nodes (~2.62 GB); depth has Instant (~3.46 GB), byte arrays (~3.18 GB), JsonWriteContext (~2.41 GB); resource has EOFException (~3.20 GB), Instant (~2.85 GB), IOContext (~2.41 GB); both has Instant (~6.32 GB), EOFException (~3.62 GB), UTF8JsonGenerator (~2.60 GB). These are JFR statistical weights over one recording, not exact allocated bytes or retained heap. The exported summaries aggregate by class, not causal allocation stack, and recording includes metrics/setup/class loading. They support investigating serialization setup but cannot prove per-row generator allocation costs or rank GC savings. Do not sum these weights into an alleged throughput explanation.

## Recommendation before selection

Prefer a correctness-first design review of typed compact-row JSON emission as the strongest localized alternative on the actual HTTP path, alongside read-only analysis of repeated observed-read admission. Keep identity-aware fused attach as the smaller TLS fallback. Do not adopt the holder or old initializer tweak from sample reductions; the completed clean factorial showed no benefit. One profile per arm, measured tail drift, missing Node CPU and absent browser rendering limit every ranking. This recommendation chooses where to investigate, not a promise of speedup.
