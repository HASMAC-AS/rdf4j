# LMDB result-coordinate transport: design and experiment limits

This report describes the implementation used for the same-ownership transport experiments. The JVM property `rdf4j.lmdb.resultCoordinateTransport` selects `off`, `dense`, `changes`, `rows`, or supplementary `selection`; it is read once when the carrier class initializes. DENSE remains the default. Use separate fresh JVMs for different arms.

The experiment retains the same ownership, detached results, weak value-reference reuse, source identity checks, and AUTO strategy-selection code. It changes optional coordinate transport and materializer coordinate use. It does not guarantee identical realized routes under every memory budget; route/tier observations must establish that separately.

## Representation and cost

A cell coordinate contains five full-width longs: source group, parent/run, source position, tuple column, and namespace. These identify a certified source cell, not a result-row number or an RDF-value hash. Copies are captured at terminal emission before upstream cursors move. No source-owned value array is retained by the carrier.

Let **B** be output-window capacity, **R ≤ B** emitted rows, **P** all output positions, **W ≤ P** tracked output positions including aliases, **G ≤ W** source-row groups, **C** recorded changed output cells, and **Cg** recorded changed group rows. The first row records every tracked cell/group. **L** is the allocated long-record buffer length, including growth slack; **Gmax** is the largest admitted group count so far in this owner. The table excludes common registers/frames, row-change and liveness arrays, namespaces, producer sidecars, headers, and reservations.

| Arm | Representation payload in bytes | Work and access behavior |
| --- | --- | --- |
| `off` | No `KernelResultCoordinates` carrier | Ordinary reuse remains. Coordinate materialization is bypassed, with residual scaffolding described below. |
| `dense` | `40 B W` | Copies five longs per tracked output on every emit. Direct constant-time cell lookup. |
| `changes` | `8 L + 12 B + 40 W`; recorded content is `40 C` | Scans outputs and records dirty cells. Stores each row's record start and change mask. Reconstructs changed coordinates into one reusable decoded row; backwards reads restart from the window's first full record. |
| `selection` | `8 L + 4 B W + 4 P`; recorded content is `40 C` | Same dirty-cell records, plus an integer index for every tracked emitted cell. Direct lookup trades index writes/storage for avoiding reconstruction. |
| `rows` | `8 L + 4 B Gmax + 13 P + 8 W`; recorded content is `32 Cg` | One integer selection per emitted row/group, four-long source-row records, and fixed output-column mappings. Direct lookup reconstructs five fields without a chain or search. |

These are primitive-array payload calculations from the code, not measured heap sizes. CHANGES and SELECTION start with record space for up to eight dense rows. ROWS admits its group-dependent selectors after seeing the first row and initially sizes records for up to eight grouped rows. Record buffers grow by approximately 1.5×, bounded by the worst-case window. Buffers retain admitted high-water capacity across windows.

All modes that emit coordinates still scan tracked outputs for bookkeeping. CHANGES also scans output positions while reconstructing each intervening row: sequential consumption amortizes that work, while repeated backwards requests can replay substantial prefixes. ROWS initially searches existing groups, costing at most quadratic work in tracked width once per window, then validates outputs and processes groups on each emit. Reduced copy volume therefore does not establish reduced CPU time.

ROWS has another algorithmic difference: it compares a group's current four fields with its last stored record. A change followed by restoration before emission can avoid a new record. CHANGES/SELECTION use accumulated dirty-register flags and may record that restored cell again. None of the compact arms globally interns all previously seen coordinates; returning to an older nonconsecutive record can append another record.

## ROWS invariants and withdrawal

The first emitted row establishes groups by exact equality of `(group, parent, position, namespace)`. Different tuple columns can share a source row. Each initially unknown/unbound output gets a separate group, even if its invalid fields resemble another output's. Each bound output records its full-width tuple-column mapping.

Every later emit verifies that all members still describe the same source row and that each bound output retains its fixed column. Unknown singleton outputs may learn their column when first bound. A completely unbound group may later restore the original mapping. A partly unbound group, differing row fields, or changed bound tuple column withdraws the optional transport for the remainder of that carrier's execution. It does not split groups adaptively or force a different query strategy. A new output window may establish a new partition if the owner remains enabled.

Withdrawal makes all buffered coordinates unavailable; ordinary result production/reuse continues. Earlier copied result values remain detached. This conservative behavior prevents a stale selection from being interpreted as a different source. It can also reduce coverage compared with a grouping proven statically from IR producer identities.

## Ownership and admission

Every carrier-owned allocation is preceded by the existing query-ledger admission. A small optional nested object holds ROWS-only state; the common carrier adds one reference, covered by an eight-byte increase in its conservative allowance. Helper/mapping arrays are admitted before construction. Selector and record replacement hold both old and new reservations during allocation/copying. Refusal withdraws optional coordinates; it does not silently allocate beyond the budget.

Registers, frames, and source sidecars retain namespace references. Emitted namespaces are additionally pinned once per bounded window until its last active row is released. This can retain a namespace and its charged factor pages beyond their last individual row until the remaining window is consumed. Retired allocated generation identifiers are never reused within an owner. Already admitted primitive buffers and their reservations remain until owner close even if optional coordinates are withdrawn; namespace draining after withdrawal may release materializer pages earlier. Early close releases all carrier, sidecar, namespace-directory, and replacement-buffer reservations.

**The compact experiment ends at the output-window boundary.** Five-word producer registers, saved frames, and source-side coordinate arrays remain unchanged. The materializer still validates source/activation identity and consumes a five-word coordinate to locate weak factor-cache entries. ROWS selects primitive provenance records; it does not hold a live vector of RDF values or transplant DuckDB/Velox's ownership model.

## What OFF still includes

OFF declines carrier creation and returns early from materializer coordinate publication/invalidation, preventing coordinate lookup/cache use while keeping ordinary last-value reuse. It does **not** remove coordinate-demand propagation, IR coordinate-shape analysis, generated coordinate fields/null guards, native coordinate interfaces, or the shared ownership migration. The ordinary materializer-cache allocation still includes its five-long coordinate scratch buffer.

Thus OFF versus DENSE is a runtime ablation in one binary. It is not equivalent to deleting the coordinate feature from source, nor to the historical pre-migration baseline. Native factorized cursors outside IR can also provide live coordinates: OFF disables their materializer coordinate use, while the compact output-window arms affect IR carriers specifically. A gain without IR carrier participation must not be attributed to compact IR copying.

## Diagnostic interpretation

The existing `opened`, `initialAdmissionRefused`, and `growthAdmissionRefused` counters describe carrier admission. They do not cover every decline: eligibility can reject unsupported widths or no tracked outputs before admission, and sidecar-word or namespace-directory admission can return unavailable without incrementing these counters. A zero refusal counter alone is not proof of complete coordinate participation. ROWS adds owner-local totals, published once when each owner closes:

| Accessor | Meaning |
| --- | --- |
| `rowSelectionOwners` | Closed, admitted ROWS carriers, including owners that emitted no rows. |
| `rowSelectionWindows` | Valid encoded windows. |
| `rowSelectionRows` | Encoded rows in those windows, including buffered rows an early close never consumed. |
| `rowSelectionColumns` | Tracked output positions summed once per valid window. |
| `rowSelectionGroups` | Source-row groups summed once per valid window. |
| `rowSelectionInvariantRefused` | Owners withdrawn for grouping/column divergence, counted once. |

A window later withdrawn for invariant or growth failure contributes none of its window/row/column/group participation counts; valid earlier windows remain counted. `groups / columns` measures sharing over valid windows, not returned-row coverage, cache hit rate, or total allocation saved. It can look favorable while other windows withdraw. Nonzero windows/rows establish ROWS participation; a configured mode or opened carrier alone does not. Missing accessors in an older binary mean unavailable information, not zero activity.

## Primary implementation precedents and rationale

[DuckDB's execution-format documentation](https://duckdb.org/docs/2.0/internals/vector) describes dictionary vectors as a child vector plus selection indices, and UnifiedVectorFormat as a common view over physical encodings. **Our design inference:** preserve a stable consumer interface while comparing primitive representations, rather than requiring each consumer to understand every storage arm.

[Velox's vector documentation](https://facebookincubator.github.io/velox/develop/vectors.html#dictionary-vector-scalar-types) explicitly describes multiple columns sharing one indices buffer after filtering, dictionary representations for join fanout, and separate null handling. **Our design inference:** a source-row selector shared across related columns is a distinct experiment from one selector per cell. Here, full-width provenance equality and conservative unbound handling provide the narrower correctness boundary.

[Velox's dictionary-encoding guide](https://facebookincubator.github.io/velox/develop/dictionary-encoding.html#decodedvector) explains why recursive virtual lookups through dictionary layers can be costly and why DecodedVector consolidates mappings; it also recommends reusing decoding storage. **Our design inference:** keep ROWS reads bounded/direct and compare them against the reusable sequential decoder in CHANGES. Compression savings alone do not predict latency.

The primary [Velox DictionaryVector.h implementation](https://github.com/facebookincubator/velox/blob/main/velox/vector/DictionaryVector.h) exposes a base vector, indices, optional nulls, and delayed loading for lazy bases. Its `loadedVector()` selects referenced non-null base rows before loading. This supports separating selection from materialization, but does not justify retaining source-owned values beyond their explicit owner in this RDF4J implementation. These references supply design precedents, not performance evidence for this patch.

## Evidence and remaining interpretation boundary

The new ROWS cases and existing carrier tests passed 32/32 in isolation; the existing factor-transport and optional-fanout integration suites passed 12/12 under ROWS across compiled/interpreted IR, retained rows, early close, cancellation, and exact bags. See [the preserved ROWS evidence](row-selection-regression/README.md). These checks establish correctness of the tested paths, not a runtime benefit. Official mode-gate and benchmark evidence are recorded separately.

Compare total latency, all-thread allocation, preparation/evaluation/consumption phases, ledger peaks, checksums, participation, and route/tier frequency distributions together. The ledger measures accounted query reservations, not RSS or all Java/application-retained heap. AUTO route changes remain real outcomes but confound a claim about representation overhead alone. Historical measurements and a fresh parent reconstruction must remain separate from this same-binary ablation.

## Source basis

The descriptions above follow `KernelResultCoordinates` (`open`, `emit`, `coordinateStorage`, `RowSelectionWindow`, and `close`), `NativeResultMaterializer` (`allocate`, `position`, and `invalidateCoordinates`), `LmdbNativeKernelExecution`, `LmdbNativeKernelEmitter`, and `LmdbNativeKernelInterpreter` under `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/`. The carrier is in its `codegen` subpackage. The experiment field semantics follow `OptionalFanoutExperiment` and `OptionalFanoutRouteAccess` under the matching test source tree.

Carrier SHA-256 inspected for this report: `e079c212761f295e5c37566b6c71888d5b7240b1ac0891fdad521ec423aee4c4`.
