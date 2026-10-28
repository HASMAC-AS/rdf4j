# Share native factorization contracts across planning and execution

This living ExecPlan follows `.agent/PLANS.md`. It is maintained with one active progress item. The authorized outcome is an integrated implementation on a new branch, tested, committed and pushed to HASMAC-AS/rdf4j; no pull request or merge is authorized.

## Purpose / Big Picture

The LMDB native query engine already represents independent child relations as borrowed factors. A factor is a deferred relation, not a scalar row weight. This change makes the scalar bindings required by each operator explicit and uses those same requirements to price composed plans. A safe high-fanout UNION, ordinary OPTIONAL or pure BIND around a basic graph pattern can then compete automatically for projected SELECT and aggregate execution, without forcing the packed strategy. Unbound variables, tuple correlation, duplicate mappings, effectful expressions and ownership boundaries remain exact.

## Progress

- [x] (2026-10-28 20:19Z) Inspect existing contracts and reviewer guidance.
- [x] (2026-10-28 20:22Z) Establish failing default-route regression evidence.
- [x] (2026-10-28 20:42Z) Implement shared demand and structural costing.
- [x] (2026-10-28 20:42Z) Integrate default row and aggregate admission.
- [x] (2026-10-28 21:38Z) Verify semantic contracts and complete native query manifests.
- [x] (2026-10-28 21:47Z) Verify corrected performance and finish independent reviews/style checks.
- [ ] in_progress: Publish the exact reviewed tree to the new fork branch and verify remote SHA/tree (external completion evidence accompanies delivery).

## Surprises & Discoveries

The existing FactorEnvironment already owns authoritative group membership, scalar dependencies and generation validation. Static planning must not invent a fixed group layout for UNION or OPTIONAL rows. Existing FactorRows.Expanded already implements partial flattening; it should consume a named demand contract, not be replaced.

The initial root clean install fails because maven-clean-plugin 3.5.0 is absent from the copied workspace cache. The online retry cannot resolve Maven Central. An escalation was not executed because sandbox setup failed with a read-only sessions-path error. A new isolated checkout has no stale build outputs; a module-and-dependencies install without clean is being tried using existing cache. These are environment limits, not test failures.

## Decision Log

- Decision: Use branch GH-0000-native-factorization-contracts from upstream main bf20ebcb1295d742c127dfa10d1a5bd4760f7fa8.
  Rationale: No issue was provided and AGENTS.md requires a GH prefix. The upstream revision was verified by ls-remote.
  Date/Author: 2026-10-28, Codex.
- Decision: Preserve the existing representation, cursor kernels, exact multiplicity and optional transport APIs.
  Rationale: The missing behavior is integrated demand and costing, not another executor.
  Date/Author: 2026-10-28, Codex.
- Decision: Require finite, strictly advantageous cost for newly automatic composed SELECT and IR aggregate bridge admission. Keep existing composed aggregate and forced capabilities.
  Rationale: UNKNOWN work preserves preference order and would otherwise silently promote newly offered strategies. Shared output cost must be charged symmetrically or cancelled before comparison.
  Date/Author: 2026-10-28, Codex.

## Outcomes & Retrospective

Implemented: shared live factor demands, conservative structural/boundness/ownership contracts, demand-sensitive composed work, automatic SELECT proposals and shared marginal aggregate lowering. Existing factor representation, borrowed lifetime, exact weight algebra, kernel execution and global arbitration uncertainty policy remain authoritative. Serial and parallel aggregate recipes are explicitly distinct. The final semantic selection passes 982 tests (10 existing skips), and all 248 native SPARQL query manifest cases pass. Corrected-candidate paired measurements demonstrate gains on the selected high-fanout fixtures while restoring the deep-OPTIONAL routes/allocation; publication remains in progress. These are bounded measured results, not a universal speed claim from structural work estimates.

## Context and Orientation

All production changes are in `core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/`. The `factor` package contains borrowed unary and tuple relations. `FactorEnvironment.expansionClosure` expands every column of a correlated tuple group, while independent siblings remain attached. `evaluation/LmdbNativeFactorRows` bridges grouped relations to scalar slots, preserving exact bag weights. `evaluation/LmdbNativeFactorAlgebra` composes JOIN, OPTIONAL, UNION, FILTER and extension. `evaluation/LmdbNativePackedFtree` plans basic graph regions and already estimates physical members separately from logical rows. `evaluation/LmdbNativeRowStep` and `LmdbNativeGroupStep` offer competing strategies; `LmdbNativeKernelLowering` already carried shared aggregate projections for direct MultiJoin inputs; this change also admits safe, finite-cost composed serial producers without changing partitionable parallel recipes.

A demand comprises scalar reads plus slots a consumer may write and therefore must compatibility-check. Guaranteed-bound slots differ from possibly produced slots: an OPTIONAL or UNION can leave produced slots unbound. Effectful expressions require one invocation per logical mapping. A borrowed descriptor is valid only while its owning producer remains on the current prefix; copying it does not extend lifetime.

## Plan of Work

First add and run an in-repository regression showing an unforced composed high-fanout projection does not currently take the costed packed route. Preserve its failing Surefire evidence before touching production.

Then introduce a named demand contract shared by FactorRows and FactorAlgebra and by a structural factor planner. The contract identifies reads, compatibility writes and logical-replay or semantic boundary reasons. Runtime closure always uses the live FactorEnvironment; it does not eagerly count hidden products. The structural planner distinguishes effect, order and lexical/typed ownership boundaries and guaranteed boundness. It reuses PackedFtree leaf topology and physical/projected cardinality estimates rather than assigning a constant factor discount.

For UNION, add independent arm work and projected cardinalities. For pure FILTER and BIND, charge evaluations at the demanded scalar prefix. For JOIN and ordinary OPTIONAL, charge RHS reopens at the left prefix required by RHS reads and writes; only assured left bindings enter hypothetical right-side estimates. Keep scalar estimates where hypothetical binding values prevent a reliable packed plan. Include projection/conversion costs and unavoidable SELECT logical emission. Price aggregate marginals separately using reducer capabilities instead of multiplying independent arguments together.

Consume this plan from composed packed row proposals and aggregate estimates, and use safe finite dominance for new automatic composed SELECT and shared IR projection admission. Existing unsupported, unknown-cost, ordered, lexical, typed and effectful cases retain current safe routes. Do not change nested factor IR validation restrictions.

## Concrete Steps

Use JDK 25 and Maven 3.9.16 by sourcing `../toolchains/activate.sh` from the repository root. Every Maven command uses `-Dmaven.repo.local=.m2_repo`. Tests must not use `-am` or `-q`. Build dependencies with `mvn -B -ntp -o -Dmaven.repo.local=.m2_repo -Pquick -pl core/sail/lmdb -am install`, then test with `mvn -B -ntp -o -Dmaven.repo.local=.m2_repo -pl core/sail/lmdb -Dtest=... verify`. Keep full logs and Surefire summaries, recording initial evidence in `initial-evidence.txt`. Run formatting and copyright checks before final verification. Adapt only the build bootstrap if uncached plugins prevent the prescribed root install; document the actual commands and limitations.

## Validation and Acceptance

The default-route regression must fail before implementation and pass after. It must compare exact duplicate-preserving results with the generic SPARQL evaluator. Unit tests must show one requested tuple column expands its complete correlated group, leaves an unrelated sibling unread, retains lazy weight overflow behavior and rejects use after owner advance/close. Cost tests must show independent marginal demands do not multiply each other, cross-group demand adds expansion cost, RHS writes enter demand, and nullable outputs do not become assured boundness.

Run existing native packed algebra, factor transport, factor kernel, marginal, lowering and work-model tests. Add unforced and forced oracle comparisons covering OPTIONAL no-match and empty factors, UNION bags, VALUES UNDEF, false/error filters, failed and volatile BIND, RDF-term DISTINCT identity, correlated entry bindings, ORDER/LIMIT/OFFSET and early close. Run relevant complete native SPARQL compliance suites. Include tiny/scalar-only and unknown-cost loss controls. Performance smoke tests must prove the actual selected path and report measured timings without claiming a universal speedup.

## Idempotence and Recovery

Work only in this isolated checkout. Keep unrelated and untracked artifacts. A decline must occur before a source advances; never retry after observable output or effectful evaluation. Preserve existing leaf fallback and ownership cleanup. Only intended source, tests and this plan are staged. Publication uses a new fork branch and verifies the exact remote commit, without merging.

## Artifacts and Notes

Initial checkout: `/workspace/scratch/bfa8fbb86df5/rdf4j-native-contracts`. Independent reviewers are examining semantics and performance without modifying sources or running overlapping builds.

## Interfaces and Dependencies

No new dependency is required. Keep `SlotPlan.open`, optional `openFactors`, FactorEnvironment, FactorProductCursor and existing reducer capabilities. Introduce internal demand and structural planning types rather than changing public query APIs. The demand type must be consumed at runtime partial-flatten sites and structural cost sites. The planner must return legality/capability and estimated work separately; possible factors are not evidence of actual retained factors.

Revision note: initial plan created after source audit and independent review guidance; implementation is blocked on red test/build bootstrap, not design approval.

Revision note (20:22Z): Fresh module/dependency offline install passed all 64 modules. New default composed SELECT regression failed at expected path assertion after exact 8192-row bag equality passed. Surefire: Tests run: 1, Failures: 1, Errors: 0, Skipped: 0. Initial evidence retained in initial-evidence.txt. Actual path was irKernelInterpreted with flattenedRows=8192; source optimizer factored common UNION pattern into Join(Pattern, Union(Pattern, Pattern)), so the shared cost planner must cover ordinary Pattern leaves as well as MultiJoin.

Revision note (20:32Z): The first green attempt now offers packedFtree automatically at work 518 versus scalar/IR 17480, proving admission integration, but the existing arbiter deliberately retains a 256x cold uncertainty band. Preserve that global safety policy. Strengthen the default-route fixture with a third independent 16-member factor (131072 exact mappings), keeping its exact-bag and packed-route assertions, so the expected route clears the existing band instead of weakening arbitration. The initial two-factor red evidence remains intact. Runtime reviewer caught and corrected an over-strong draft assertion: factor dependencies can be scalar UNKNOWN; scalar availability is not guaranteed RDF boundness.

Revision note (20:43Z): The three-axis shape optimizes to a deeper scalar RHS chain and correctly retains conservative hypothetical-bound pricing; it does not clear the cold band. Preserve the original 64x64 two-axis case as an offered-but-outranked loss control. A separate 1024x1024 two-axis UNION + visible BIND fixture matches the generic 2,097,152-row bag in streaming form and actually selects packedFtree (8006 producer/conversion work versus 4,211,720 rivals) without policy changes. Root strategy assertions now use the root execution-path metric and physical-plan prelude, not recursive child matches. A red test requires observed live-prefix demand/preservation counters; these are distinct from estimates and generated-kernel telemetry. Counting tuple/sibling/early-close ownership and nullable retained-selection tests pass.

Revision note (21:20Z): Broad relevant native regression selection completed: **980 tests, zero failures/errors, 10 existing skips, BUILD SUCCESS**, log `factor-contracts-native-final.log`. This includes all packed/factor/marginal contracts, native expressions/OPTIONAL/entry/order/distinct/strategy tests, lowering/work tests and the SPARQL11 regression class. The first broad pass caught two forced-parallel declines: the new serial PlanRows bridge obscured a partitionable IR root. The fix gives parallel admission/opening its own pre-existing recipe; the exact failing GroupedOptionalStability tests are now green. Serial IR work uses the same known marginal quote as its actual bridge, while parallel retains its own ordinary producer quote. Two older deep DISTINCT tests had correct bags but required the old IR tag; acceptance now also permits a costed packed root with actual preserved compatibility groups, never arbitrary native/nested fallback.

An incidental full Failsafe run (the earlier focused verify omitted `-DskipITs`) passed autogrow isolation/single-commit and frozen spill replay, then the unrelated ThemeQueryBenchmarkSmokeIT JVM exited 137 with an 8GiB configured heap. It is not reported as a green full-module integration suite. Final scoped native verification explicitly skips unrelated ITs; complete native query manifests and bounded performance smoke remain separate obligations.

Official Maven artifacts `opencsv:5.12.0`, `commons-collections4:4.5.0` and `junit-bom:5.13.2` were recovered through the supported cloud browser, checked byte-for-byte against Maven Central SHA-1 values, and copied to the private cache. No generated/stub dependency POMs are used. The ordinary full compliance reactor initially stops before tests on missing unrelated dependencies. An isolated harness compiles unchanged native compliance classes, uses every unchanged manifest/resource, and depends on the same built native/query-result/dataset modules; it is not the whole HTTP-server compliance reactor.

Both independent reviewers have signed off the current semantic/architecture diff subject to remaining compliance, formatting and measured performance. Java-only formatting with the repository's exact Eclipse 4.40 configuration and import order is being recovered independently: no XML source was changed, no repository POM was edited, and the unverified/blocked WTP artifact is not used.

Revision note (21:31Z): Paired baseline/candidate measurements (two forks/version, independent exact-bag oracles) found a real default deep-OPTIONAL DISTINCT regression despite correct results: about 18ms to 32ms and 6MB to 60MB allocation. Both such producers expanded every final group (zero final preserved groups). Do not ship that regression. New red tests (three failures, no errors, `factor-contracts-final-demand-red.log`) require fully demanded composed outputs to retain the established row authority/quote, while hidden projections remain admitted. The conservative general contract now requires a possible independent unary group outside final demand and fewer projected than logical mappings. `rowsWork` and automatic admission share this predicate, including the legacy composed-MultiJoin proposal route. This is bounded admission while full composed materialization/allocation costing remains incomplete, not a claim that fully demanded factor plans are universally inferior. Forced execution, pure direct BGP authority and independent aggregate marginals are unchanged. Restored the original deep-OPTIONAL IR path assertions; fresh validation and corrected-candidate timing are pending.

Java-only Spotless application succeeded using all 18 official checksum-verified Eclipse 4.40 dependencies and the exact repository conventions/import order (`factor-contracts-java-format.log`). The full XML/WTP check remains unavailable; it was not bypassed or represented as passed. No XML source/POM is part of the patch.

Revision note (21:41Z): Final guarded validation completed. `factor-contracts-native-final2.log`: **982 tests, zero failures/errors, 10 existing skips, BUILD SUCCESS**. This includes the hidden-scalar-alias loss control (every possible factor column is demanded), fully demanded DISTINCT and bag projections, the original deep-OPTIONAL IR assertions, and all earlier runtime ownership/replay/cost tests. `factor-contracts-compliance-final.log`: **248 tests, zero failures/errors/skips, BUILD SUCCESS** (177 SPARQL 1.1 and 71 SPARQL 1.2), using unchanged official classes and complete unchanged resources in the isolated harness. Copied support sources were hash-verified; dataset/text result modules and the same final native JAR were built normally. This is complete native query-manifest coverage, not a full repository/HTTP-server reactor run.

Final scoped native command (from the checkout root after sourcing the toolchain):

    mvn -B -ntp -o -Dmaven.repo.local=.m2_repo -DskipITs -pl core/sail/lmdb '-Dtest=*Factor*Test,*Packed*Test,*Marginal*Test,LmdbNativeWorkTest,LmdbNativeKernelLoweringTest,LmdbNative*Optional*Test,LmdbNative*Expression*Test,LmdbNative*Entry*Test,LmdbNative*Order*Test,LmdbNative*Distinct*Test,LmdbNativeStrategy*Test,LmdbSPARQL11ComplianceRegressionTest' -Dformatter.skip=true -Dimpsort.skip=true -Djapicmp.skip=true -Denforcer.skip=true verify

The two independent design/semantic reviews found no remaining blocker in the final guard and test diff. Corrected-candidate paired measurements are the remaining empirical gate. The original regression report is retained, rather than overwritten by a success-only report.

Known boundaries: possible-factor metadata is not a promise that storage exports a borrowed source; exact scalar fallback remains tested. Static retention currently describes independent unary terminal capabilities, not arbitrary tuple layouts. Actual tuple closure and ownership remain live-environment contracts. Newly automatic composed SELECT is conservatively declined when final projection expands every possible group; fully materialized allocation/reopen costing is not complete. Ordered, lexical, opaque/effectful and physical-cell boundaries retain established fallback. Parallel IR does not partition the new composed PlanRows bridge. Public projections still export weighted scalar rows; factors left unexpanded during projection supply exact multiplicity rather than escaping the owning prefix.

Revision note (21:47Z): Corrected candidate passed the complete paired ABBA matrix: **64 primary series plus four longer-warmup control series**, with independent exact-bag oracles throughout. High-fanout composed SELECT medians changed from 2674.6/2591.6ms to 2036.8/2007.4ms; composed aggregate changed from 397.3/389.1ms to 19.04/17.59ms, with allocation about 622MB to 4.63MB. Both deep-OPTIONAL shapes returned to their original IR route and allocations (about 6.2MB and 33.1MB rather than the rejected initial candidate's 60.5MB/105MB). Short-run ten-layer latency was noisy/slower; a bounded 100-warmup/20-measurement paired follow-up showed baseline 36.17/25.26ms versus guarded 29.64/24.36ms. This establishes no sustained regression in that control, not a robust speedup claim. High OPTIONAL, scalar and ordered controls retain their established routes; corrected high OPTIONAL medians were 1326.9/1332.8ms versus 1334.4/1306.9ms. The report retains both initial adverse evidence and corrected results.

Final Java-only exact Eclipse/import configuration check succeeded (`factor-contracts-java-format-final.log`), the repository copyright/SPDX script passed, and `git diff --check` is clean. No production changes occurred after the final guarded test/class freeze. The native JAR was repackaged by the final verify: ZIP comparison with the installed JAR used by the manifest harness showed identical entries and identical payloads except META-INF/MANIFEST.MF; every production class is byte-identical. Benchmark final source/class hashes were independently frozen. Independent semantic and architecture reviews approve the final guarded diff. Only the intended thirteen Java files and this ExecPlan will be published; all caches, harnesses, logs, timing data and temporary formatter POM remain outside the commit.

Performance limit retained in the final report: the small 32-member composed aggregate rose from 7.23/6.06ms to 7.62/7.74ms in the paired forks (roughly 0.4–1.7ms absolute overhead). The shared estimates do not yet fully price all planning/bridge overhead. This is explicitly not an all-case latency improvement; the substantive gains are the demonstrated high-fanout projected SELECT and marginal aggregate cases. `GUARDED-REPORT.md` and its proof bundle preserve all raw controls, initial regression data, final source/class hashes and independent oracles.
