# Independent static review of publication diagnostics

## Scope and verdict

This review inspected the diagnostic source generator, the complete Java helper, the diagnostic compile/run driver, the protocol note, and the selected original materializer's lookup, publication, admission, retirement, page/memo, and close methods. It also inspected the experimental omission generator and the outward binding capture loop. No Java process was run, no production source or diagnostic implementation was changed, and no measurement is claimed by this review.

The added counters and close-time page snapshots preserve the original weak-reference and value-witness operations. I found no added weak dereference, retained `Value`, source, revision owner, or coordinate owner, and no new page admission or early release. Canonical versus requested target counting is placed consistently with the documented definition. The source review supports using this code as isolated diagnostics after addressing two evidence/qualification gaps below. It does not make instrumented timing comparable to uninstrumented timing.

Reviewed versions:

| File | SHA-256 |
|---|---|
| `diagnostic/build_overlay_source.py` | `90422afebc3c4efc632cb5e4c6c37506c704a1f2a809bea976285495ab6c9c70` |
| `diagnostic/run_diagnostic.py` | `cfac032327f4ee2660db7001e6cbe24b73e69b3f65631e68423696813ed92b64` |
| `diagnostic/helper-src/org/eclipse/rdf4j/sail/lmdb/evaluation/PublicationDiagnostics.java` | `813f8310d078dea490081e3fb1d990a0aacde24dc3be1e9689137418134e4d1a` |
| `diagnostic/protocol.md` | `da1eb6a5f53696a24f86b61b65eeed81a13b96a40654b0dc30b730d3a60a8a13` |

Paths in this table are relative to `result-coordinate-evidence/publication-followup/`.

## Required qualification before masked diagnostics

`compile_overlay` intentionally keeps only the generated outer materializer class and the helper. It discards the newly compiled `NativeResultMaterializerPlan.class`, `NativeResultMaterializer$FactorMemo.class`, and `NativeResultMaterializer$FactorPage.class`, loading those three from the selected base runtime. That is valid only when the selected base provides the same plan and nested-class interfaces as the source used to compile the diagnostic outer class.

The current generator proves the plan and nested Java regions are unchanged relative to its own input. The compile driver does not connect that input to the selected base runtime. In particular, a masked source contains `plan.omittedFactorTargets`, whereas the default original common128 runtime does not. Compiling that source against the default succeeds because the source file declares the new plan field; discarding its generated plan then produces a runtime `NoSuchFieldError` when the original plan is loaded.

The masked diagnostic must therefore use the exact experimental runtime as its base. Add a fail-fast source/provenance check: the diagnostic generation input hash must match the experimental base descriptor's pinned `overlaySourceFile` hash, and the base classpath must be the expected experimental prefix over the exact preserved common128 runtime. For original diagnostics, require the known selected original source hash `cb0749719313b85c8cd134b6c03085f712915188f0e53333cbd46e19e2f0f96e` and original common128 composition. Also retain the generator's plan/page/memo region checks. These checks reject the concrete wrong-base case before launching Java.

The nested page/memo classes themselves have unchanged declarations and behavior. Their nest host remains `NativeResultMaterializer`, and the replacement host still lists the same nested members. There is no newly added diagnostic method inside those retained nested classes. The added top-level `PublicationDiagnostics` helper does not alter their private-access relationship. This is a source/interface assessment; no independent classfile ABI execution was performed in this review.

## Required reconciliation before using the counters to select masks

`owner_analysis` currently proves per-target partitions and that page grants equal observed explicit page releases. Those checks are useful, but they are internal to the added instrumentation. They do not independently connect total recorded traffic to the harness's `bindingReads` and `lazyValueCalls` counters.

For these fully bound fixtures, first confirm the recorded canonical mapping and the absence of duplicate projection aliases. Then check, for every query:

    sum(find) == bindingReads
    sum(ordinaryHit) + sum(factorHit) + lazyValueCalls == bindingReads
    sum(rememberCall) == lazyValueCalls

The earlier shared-tail data satisfied this accounting pattern. These are fixture-specific checks, not general materializer invariants: the outward capture loop can bypass a materializer lookup when a previous column already materialized the same value; unbound columns and scope rejection can also bypass recorded lookup paths. The two selected fixtures should be explicitly qualified before enforcing the equations.

Validate that target indices form exactly the expected unique range, names match the harness projection, every canonical index points to a canonical root, and page targets refer to canonical roots. Then aggregate requested-target lookup/publication counters by their canonical mapping when comparing them with page charges or constructing a mask. The raw output already contains the necessary names, canonical indices, source slots, and cache banks.

Without these checks, an instrumentation placement mistake or attribution mismatch could satisfy all local partitions yet lead to an incorrect interpretation of which cache bank earns its page cost. This is an evidence gap rather than an identified wrong counter placement in the reviewed implementation.

## Weak references, lifetimes, and close behavior

The instrumentation is limited to primitive counter increments, immutable projection metadata copies, and primitive page-key snapshots. `PublicationDiagnostics.snapshot` tests only whether each stored cell object is non-null; it never calls `get`, `dereference`, a `Value` method, or a witness method. A stored weak-reference object with a cleared referent therefore still counts as occupied. This is correctly documented and must not be described as live-value occupancy.

The helper copies only projection names and primitive metadata arrays. It does not store its constructor's plan, the page's `Object[]`, any cell object, a value, a source, or the coordinate owner. Its page table stores nine primitive words per page. The generator's textual `.get()` count check is corroborated by inspection of every added call and the complete helper; the textual count alone would not have been sufficient proof.

The original `find` and `findFactor` calls still perform the same dereferences and witness checks in the same order. The ordinary-hit counter is added before the existing call to `rememberFactor`, without replacing or bypassing that call. The optional experimental guard already exists in the omission source; the diagnostic only counts whether it returns. Factor lookup remains active for all targets.

Close captures primitive metadata before the existing cleanup. The capture call is inside a `try` whose `finally` contains the complete original close body. If snapshot allocation or capture fails, the original cleanup still runs. JSON publication occurs after cleanup; an output error can fail the diagnostic process, but does not skip the original cleanup. A later close sees `closed` and does not publish a duplicate record. No failure is swallowed.

## Page accounting and its limits

Admission is counted after the page has been inserted and the production page count incremented. All successful normal paths return that page. Every normal `return null` in `admitFactorPage` increments the refusal counter, including a directory refusal. An exception causes the diagnostic process to fail and preserves the production claim-close catch; it is not misreported as a successful or refused normal admission.

Explicit page-release counters cover both namespace retirement and full `clearFactorPages`. A namespace retirement removes its page from the directory before later full cleanup can see it, so the two sites do not double-count a normal release. Full cleanup visits each remaining non-null page once. The retained nested `FactorPage.close` still clears both reference arrays and closes its reservation. The helper records the reservation's byte count before closing it.

The final page table describes pages still present when close starts. It does not enumerate pages retired or cleared earlier. Lifetime admission and release counters can reconcile even when the final table is smaller. The final-table page count should not be equated with total lifetime admissions unless the observed run also establishes no prior page retirement or clearing.

Ordinary-cache, directory, and memo charges are only sampled at close. They are not instrumented grant/release totals, and directory growth can release earlier claims that will not appear in that snapshot. Accordingly, the reviewed implementation supports an explicit grant-release equality claim for pages, not for every accounting class. Unchanged original close methods and zero harness ledger endpoints independently support overall cleanup. If the report needs observed equality for all reservation classes, additional counters would be required.

## Runner preservation and interpretation

Generation creates a fresh output directory, preserves the input preimage, writes an exact diagnostic patch, and checks the plan/page/memo source regions. Compilation preserves full javac output while selecting only the intended outer/helper classes. Runtime and source manifests are hashed before use. Each suite freezes its configuration and helper/driver/protocol hashes, runs serial fresh JVMs, retains all raw records, and rejects process/result/route/counter failures without replacement runs.

Normal AUTO, native IR, query-time adjacency use, result checksums, the shared-tail exact bag oracle, and zero ledger endpoints are checked. The observed query must be compiled; earlier queries may use the ordinary AUTO tier progression. Diagnostic times and allocations include the helper's storage and serialization and must remain excluded from performance estimates.

The diagnostic source/runtime linkage and independent traffic equations above are the two changes recommended before relying on the masked comparison. No change to production ownership or weak-reference semantics is recommended by this review.

## Follow-up review after the qualification fixes

The diagnostic driver was subsequently revised to SHA-256 `35a26061c0a71fa294c2222a168e579ea92b0fa76ebd401e0363280d6e0b586d`. The generator, Java helper, and protocol note retain the hashes listed above. This follow-up inspected the changed code only and ran no Java process.

The masked path now requires the diagnostic preimage hash to match the base descriptor's pinned experimental overlay source. The original path requires the selected original source hash and a 128-entry composition whose final 127 entries match the preserved original runtime. More importantly, after compilation, the driver reads the first actual definition of every discarded class in classpath order and compares its class name, version, access flags, superclass, interfaces, field names/descriptors/flags, and method names/descriptors/flags against the freshly emitted class. The selected class bytes and both source identities are recorded. The parser covers the constant-pool forms used by these Java 25 classes and skips debug attributes, which correctly avoids treating changed source line numbers as an ABI difference.

These checks reject the concrete missing `omittedFactorTargets` field composition identified in the original review. For the intended original/common and masked/common compositions, no remaining ABI blocker was found. The field/method comparison does not claim arbitrary semantic equivalence of class bodies; the preimage/source checks and unchanged source regions provide that separate support.

For every query, the revised validator now requires target names to match the projection, target and canonical indices to form an identity mapping, and binding reads to equal rows multiplied by projection width. It then checks all three independent traffic equations requested above. This narrows the accepted diagnostic fixtures rather than silently applying these equations to aliased or partially bound executions. Local lookup/publication partitions and explicit page grant/release equalities remain intact.

The analysis now records a `releaseCoverage` statement explicitly distinguishing observed page grant/release counters from ordinary, directory, and memo close-time snapshots, with the independent harness proving zero ledger endpoints. This resolves the reporting ambiguity.

Both recommended qualification changes are addressed for the intended frozen runtime compositions. The remaining interpretation limits in this review still apply: diagnostics perturb execution, page occupancy counts stored weak-reference cells rather than live referents, and zero observed factor hits is a fixture observation rather than a general no-replay proof. A separate independent recomputation will check the raw discovery evidence before target masks are frozen.

## Follow-up driver version for descriptor spelling compatibility

The six control/omission diagnostics use the additive `run_diagnostic_v2.py`, SHA-256 `d037db6bd17adc8f428fbd3136fc07b318dba4745c31e49370e5951c95dae4c5`. The preserved `driver-v2.patch` adds a descriptor verifier accepting both `...FileSha256` and shortened `...Sha256` key spellings and replaces the call into the older compact runner's descriptor reader. The actual selected runtime pins its source, patch, validation, classpath, and manifest inputs. The patch does not change Java instrumentation, source/ABI matching, counter partitions, fixture validation, timing, or suite ordering. The preceding semantic and accounting assessment therefore continues to apply. The original driver remains unchanged.
