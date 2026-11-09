# Implement complete ISO GQL semantics over LMDB adjacency

This is a living ExecPlan maintained under the repository's `PLANS.md`. It supersedes the limited delivery target in `gql-exec-plan.md`; that earlier plan remains the historical record of the initial RDF query profile. This plan records unfinished work honestly and is not a conformance declaration.

## Purpose / Big Picture

The requested outcome is an implementation of ISO/IEC 39075:2024 with ISO/IEC 39075:2024/Cor 1:2026, including the complete language and its optional feature surface. A user must be able to create and maintain property graphs, prepare and execute typed GQL programs, manage session and catalog state, and query those graphs through LMDB adjacency without silently substituting RDF or SPARQL semantics. A passing parser test or support for a keyword does not establish a feature's conformance.

The current branch, `GH-0000-gql-lmdb-adjacency`, starts this phase at commit `9e51d79726312dddc9649eb9a1040a7b7e2e9e44`. It implements a read-query profile over RDF. The earlier 207 passing tests establish that profile's tested behavior, not ISO conformance. The full 2024 normative text is a required input that has not yet been obtained. Public ISO electronic inserts and the full free 2026 corrigendum have been obtained; they establish some exact corrections but do not replace the base semantics.

## Progress

- [x] Audit the existing branch and interfaces.
- [x] Verify the standard and current corrigendum.
- [x] Retrieve official feature and choice inventories.
- [x] Identify representation and transaction architecture gaps.
- [x] Reproduce directly specified semantic discrepancies.
- [x] Correct and validate those observed discrepancies.
- [x] Audit supplied implementation and compatibility archives.
- [x] Implement reviewed query and expression families.
- [x] Repair and verify shared binding contracts.
- [x] Validate composed queries across LMDB routes.
- [x] Measure and reduce GQL execution overhead.
- [in_progress] Complete remaining language and storage semantics.
- [ ] Complete normative requirements and conformance review.
- [ ] Implement typed graph storage and transactions.
- [ ] Implement typed values and session execution.
- [ ] Complete query, mutation, and catalog semantics.
- [ ] Verify every requirement and publish evidence.

## Surprises & Discoveries

ISO published the first technical corrigendum in July 2026. The electronic inserts retrieved from the ISO maintenance directory still have March 2024 modification dates. Their feature list contains 228 entries; the corrigendum supplies six additional identifiers. An inventory must apply the corrigendum rather than treating the original feature list as current and complete. Mandatory requirements without feature identifiers require a separate clause-level inventory.

The current RDF projection loses information: it has no independent representation for an empty catalog graph, an isolated node without statements, or two separately identified edges with the same endpoints and label. Edge identity currently equals an RDF triple. No parser change can recover these distinctions.

The public grammar gives double-quoted sequences two roles: string literals in value positions and delimited identifiers in name positions. The existing lexer merges their interpretation with backtick identifiers, causing a quoted string to resolve as a variable. The corrigendum also provides exact corrections for LET sibling visibility, aggregate sort keys, and grouping aliases. Those corrections can be implemented now without guessing at missing base rules.

Raw delimited parameter tests exposed a separate integration error: projecting a parameter directly from a singleton input did not leave a value expression for API binding assignment. An explicit expression slot fixes repeated bindings and clearing without materializing every ordinary variable unnecessarily.

The LMDB route matrix then exposed incorrect multiplicity for a labeled traversal with a bound starting node. Cursor and generic adjacency evaluation returned each edge four times, while native adjacency returned it once. Internal node/label column-pruning projections had accidentally retained RDF4J's default subquery boundary, preventing constant constraints from reaching all scan arms. Focused MemoryStore RED cases also returned rows for an unlabeled starting node. Marking these internal projections as part of the enclosing scope corrects the GQL compiler contract without changing the outer RETURN projection.

## Decision Log

The target is the published 2024 edition plus its 2026 correction, not the future committee draft. All optional feature identifiers remain in scope; an unimplemented optional feature must not be removed from the inventory merely to make a completeness claim pass.

The existing RDF projection remains a compatibility view. Complete graph semantics require explicit graph-element records, stable edge identity, typed values, and session state. Existing paged adjacency, native cursors, retained snapshots, and cancellation mechanisms are reusable after their contracts are made edge-identity aware.

The normative base text must settle defaults, type rules, error conditions, feature dependencies, and implementation-defined choices. Other implementations, permissive grammars, and research interpreters can supply comparison cases but are not the final oracle. Reviewed official material takes precedence over implementation references when they disagree. The supplied-archive decision below permits engineering to continue while the complete normative review remains outstanding. No conformance percentage or successful certification is claimed.

On 2026-11-09 the user supplied froGQL, gql-compat, gql-grammar, MGQL, and DuckGQL archives as the available reference material and asked that they be used instead. Continue implementation using their reviewed fixtures, formal models, and algorithms together with the official grammar and already reviewed corrigendum. Absence of the base publication no longer pauses engineering work. Keep normative review incomplete, retain provenance, and record disagreements explicitly; an implementation's successful test is not automatically an ISO requirement.

## Outcomes & Retrospective

The first correction batch is complete and verified on the formatted source. All 166 selected tests passed with zero failures, errors, or skips: 108 GQL parser/lexer/MemoryStore tests, 48 GQL LMDB repository/integration cases, six native DISTINCT tests, and four existing native cancellation tests. The LMDB tests cover cursor, generic adjacency, and native adjacency execution. The final parser artifact was rebuilt and installed before downstream LMDB tests. Repository formatting, copyright/SPDX verification, and `git diff --check` passed.

The corrections cover contextual quoting, regular binding variables, raw delimited parameter names, direct parameter projection, LET incoming scope, grouping aliases, aggregate sort scope, and the internal-projection multiplicity error. RED evidence was preserved before each production correction. `core/queryparser/gql/conformance/VALIDATION.md` records reproduction and evidence limits. That first batch made no generic evaluator production change and left the separate real-SPARQL-subquery issue unresolved. The later supplied-reference batch below now corrects it.

At the end of that first correction batch, the complete implementation remained unfinished and work awaited the full 2024 normative base text; targeted searches of available user files did not locate it. The supplied-archive milestone below resumes engineering without requiring the user to obtain that publication. The official inserts and complete 2026 corrigendum already obtained are insufficient to finish the clause-level semantic and conformance oracle. The architecture and inventory establish the remaining scope without treating planned storage, types, sessions, mutations, catalogs, or path operators as implemented. Full repository CI and a new benchmark run were not performed.

The supplied-reference implementation checkpoint is complete. It adds NEXT/YIELD read-query composition, LET VALUE/FILTER WHERE spellings, extended simple CASE scalar operands, numeric radix/separator/suffix forms, and matching unsigned spellings in pagination and finite path bounds. It repairs specialized-binding compatibility and the explicit-null subject/predicate pair. All 397 selected tests pass: 159 GQL tests, 159 evaluator regression tests, and 79 LMDB tests. Actual adjacency use, composed named-graph identity, retained snapshots, read-own-writes, partial closure, rollback, and prepared-query reuse are exercised. Installed class files match the final compiled classes. Formatting, copyright/SPDX checks, and diff checks passed.

This checkpoint does not implement the native graph storage, complete typed values, catalog/schema operations, GQL mutations, session/program interface, runtime unbounded/shortest paths, or full conformance oracle. The reference archives remain partial implementations with recorded conflicts. The next architectural work is the authoritative LMDB graph-storage vertical slice described below; parser acceptance alone cannot represent independent parallel edges or graph ownership. `core/queryparser/gql/conformance/REFERENCE_VALIDATION.md` and the sanitized `references/validation-summary.json` preserve the current acceptance evidence. Full repository CI and performance benchmarking remain unperformed for this checkpoint.

## Context and Orientation

`core/queryparser/gql` contains the lexer, expression parser, and direct RDF algebra compiler. `GQLParser.parseUpdate` currently rejects all mutations and catalog operations. `core/queryalgebra/evaluation` supplies RDF-valued execution. `core/sail/lmdb` owns the LMDB authoritative data, derived adjacency, and native execution. `core/repository/api` exposes RDF query and update operations; its `BindingSet` values cannot losslessly expose all native GQL values. `testsuites` and `compliance` contain the repository's reusable test infrastructure.

The reference material is retained outside the repository under `../gql-conformance-research`. Original standards documents and electronic inserts must not be bundled into the implementation patch. Publish only original analysis, source provenance, factual identifiers, and original tests.

## Plan of Work

First add tests demonstrating the directly specified lexical and semantic discrepancies. Run those tests against the unchanged production implementation and retain the Surefire reports. Then distinguish ambiguous double-quoted tokens until syntactic context determines their role; enforce regular identifiers for binding variables; evaluate LET right-hand sides in the incoming scope; and compile grouping and aggregate ordering from the same input relation. Preserve positive tests for delimited labels, properties, output names, and parameter names.

Complete normative review remains an independent obligation: when the complete base text becomes available, extract each requirement into an original traceability record. Record its clause, mandatory or optional status, dependencies, implementation choices, expected diagnostics, positive and negative tests, and execution routes. Mark a requirement verified only after its semantics, not just its syntax, are exercised.

The supplied-archive milestone continues from commit `b68fe28508de9c44bce869c779b7666a27001cfd`. Inspect licenses and pin archive hashes and embedded revisions before adapting ideas. The compatibility corpus has 581 cases; the draft ANTLR grammar targets 2023, while its bundled official grammar describes the 2024 base. Quarantine conflicting LET visibility, aggregate ordering, grouping syntax, self-loop, and transaction expectations instead of overwriting already reviewed behavior.

Add independently specified tests for numeric literals, extended simple CASE operands, and NEXT/YIELD query pipelines. Observe and retain the smallest failures before editing `GqlLexer`, `GqlExpressionParser`, or `GqlCompiler`. Pipeline stages must preserve result bags, nulls, graph-element identity, alias scope, parameter namespaces, and grouping boundaries. Exercise the resulting algebra on MemoryStore and all three LMDB routes, with actual adjacency usage checked where traversal should use it.

Before relying on additional scope boundaries, reproduce and fix the documented generic-evaluator incompatibility between an optimizer-specialized named variable and an incoming conflicting binding. Test subject, predicate, object, and context positions, equal and absent bindings, ordinary constant terms, repeated variables, and every statement-step entry point. Keep the ordinary no-specialization path free of extra per-row comparisons by compiling compatibility checks only for the affected variables.

The retained baseline reports now reproduce all four conflicting statement positions (16 unit invocations, four failures) and the real SPARQL subquery on cursor/generic-adjacency routes (three route invocations, two failures). The native route returns the correct two rows. These reports authorize a narrowly compiled compatibility guard, with no unconditional comparison overhead for ordinary statement patterns.

New evidence belongs under `validation-gql-references/`. Use the existing Java 25 toolchain and a private workspace Maven cache. Root owns Maven execution while delegated implementations edit separate methods/files. Preserve downloaded archives as research inputs; publish original analysis, tests, and required attribution rather than bundling external build trees or ISO publications.

Implement the graph storage vertical slice before expanding syntax broadly. In the same authoritative LMDB transaction, persist catalog graph existence, isolated nodes, independent parallel edges, edge orientation, labels, and typed properties. Adjacency must retain stable edge IDs. Rollback, retained snapshots, crash recovery, reopen, backup/restore, map growth, and dictionary lifetime must cover these records. An independently committed sidecar environment is not an atomic substitute.

If GQL records use existing dictionary IDs, those references must be roots for dictionary garbage collection, rebuild, and retirement. Test values and identifiers referenced only by GQL, including deletion followed by ID reuse. Catalog-only and GQL-only commits must invalidate affected prepared plans and result caches even when no RDF triple changes; each retained reader must observe one coherent catalog, type, property, and topology revision.

Add a typed GQL value model and session/program interface. Resolve names, scopes, graph ownership, and types into a GQL semantic representation before lowering compatible operations to existing RDF4J algebra. Add dedicated physical operators where RDF values or SPARQL error semantics do not preserve GQL behavior. Extend remote result transport and user tools to preserve these types and session outcomes.

Semantic lowering must distinguish internal column pruning from genuine language scope boundaries. The corrected GQL internal projection is not a general workaround for subqueries. The supplied-reference batch corrects the generic-evaluator conflict between fixed and incoming bindings, retaining genuine subquery boundaries. `core/queryparser/gql/conformance/KNOWN_EVALUATION_ISSUE.md` contains the complete reproducer, before/after behavior, and permanent tests. Continue testing backend scope contracts before adding richer language composition.

Define session ownership explicitly: an owned repository connection closes with its session, while a borrowed connection remains under its caller's lifecycle contract. Commands must participate in the correct active transaction and retain the existing handling of uncertain commit outcomes. Publish neither an automatic rollback nor success when the underlying commit outcome is unresolved.

Complete query composition, runtime path operators, graph mutations, catalog and schema operations, procedures, diagnostics, and declared implementation choices from the requirement inventory. Runtime path algorithms must enumerate or select paths according to the specified semantics; removing the current 32-hop expansion bound is not an implementation of unbounded paths.

## Concrete Steps

Use Java 25 and the workspace Maven repository. From the repository root, install prerequisites with tests skipped before targeted tests:

    mvn -B -ntp -o -Dmaven.repo.local=.m2_repo -pl core/sail/lmdb -am -Pquick install
    mvn -B -ntp -o -Dmaven.repo.local=.m2_repo -pl core/queryparser/gql test

Never use `-am` or `-q` for enabled tests. Use the existing repository formatter before committing. Preserve all previous untracked evidence. Record new logs under `validation-gql-conformance/` and keep environment-specific Maven settings out of source control and delivery archives.

## Validation and Acceptance

The first correction batch must demonstrate literal values and identifier contexts separately, illegal same-LET sibling references, legal references from a subsequent LET, aggregate ORDER BY on the group's input rows, rejection of aggregate ordering in an ungrouped nonaggregating result, and grouping by output aliases including computed expressions.

The storage milestone must create an empty graph, two isolated nodes, and two separately identified parallel edges with different properties; query both edges through adjacency; mutate one while preserving both identities; delete only one; and retain the empty graph and isolated nodes after reopen. Repeat with rollback and concurrent retained readers. Ordinary SPARQL scans and exports must not expose internal GQL metadata.

Specify RDF interoperability as part of the storage contract. Native GQL graph contents are not implicitly exposed as RDF statements; any RDF projection must be explicit. Test context enumeration, exports, and SPARQL CLEAR against that contract, including CLEAR ALL, while ensuring that no accidental hidden-metadata convention decides which GQL records survive.

Before performance comparisons, require exact result bags and identities to match the reference cursor implementation, generic adjacency execution, and native adjacency execution. Measure graph/value setup, materialization, runtime planning, traversal, and result serialization separately and together. Vary fanout, parallel-edge multiplicity, labels, property widths, path lengths, graph size, and concurrent mutation. Include type-validation and catalog/metadata write costs, and verify actual adjacency engagement. The prior profile's short benchmark did not establish an adjacency speedup and is not evidence for this new data model.

Route comparisons must also be checked against independently specified expected results from reviewed normative requirements. Agreement between cursor and native execution does not rule out a shared compiler error. Include adversarial scope, conflicting-binding, and duplicate-sensitive cases with their expected result bags written explicitly.

Final acceptance requires every normative requirement and optional feature in scope to have a reviewed implementation and reproducible evidence. Verify compile-time rejection, runtime diagnostics, null and type behavior, multiplicity, transaction outcomes, persistence, remote results, and adjacency execution. Run relevant existing RDF4J regressions and complete CI. Test counts, grammar coverage, or another engine's answers alone do not meet this acceptance condition.

## Idempotence and Recovery

Continue on the existing feature branch. Do not change the base branch or force-push. Preserve prior validation artifacts. Use temporary repositories for destructive test cases. Persistent format changes must have an explicit compatibility and recovery strategy before they become the default.

## Artifacts and Notes

Authoritative source locations are `https://www.iso.org/standard/76120.html`, `https://www.iso.org/standard/93701.html`, and `https://standards.iso.org/iso-iec/39075/ed-1/en/`. The free corrigendum was obtained through the Estonian Centre for Standardisation and Accreditation. Provenance records contain retrieval dates and SHA-256 hashes. Targeted searches of available user files did not locate the full 2024 standard.

## Interfaces and Dependencies

The proposed additions are a `GqlGraphStore` storage contract, a typed `GqlValue` hierarchy, a `GqlSession` program execution interface, a typed semantic algebra, and a reusable clause-indexed conformance suite. Their exact public signatures must follow reviewed normative requirements. Additive internal prototypes may proceed before publishing public APIs. Reuse JDK and existing repository dependencies where sufficient; new dependency choices require their own necessity and licensing review.

Revision 2026-11-09: expanded the task from an RDF read-query profile to complete standard semantics; recorded the corrigendum, missing normative input, representation gaps, and a test-first correction batch.

Revision 2026-11-09 after validation: completed the first correction batch with 166 selected passing tests; added direct-parameter and internal-projection regressions, preserved the separate SPARQL subquery finding, and moved the active step to obtaining and reviewing the complete normative base.

Revision 2026-11-09 after supplied archives: resumed implementation from reviewed independent reference material at the user's direction. Added a source-provenance audit, explicit conflict handling, numeric/CASE/pipeline acceptance, and the generic binding-compatibility prerequisite. Full normative review remains incomplete without blocking supported engineering work.

Revision 2026-11-09 after reference validation: completed the reviewed language checkpoint with 397 passing tests, corrected both generic evaluator defects after RED evidence, and recorded remaining native-storage/value work without treating absent normative text as an engineering stop.

## Performance milestone requested after the reference checkpoint

The user requested a significant performance improvement after commit `dd1be8b6200afeb755c4278a5a542ac59cbff6ff`. This milestone preserves the full conformance goal while first removing measured execution overhead from the existing GQL profile. Significant means a reproducible reduction in end-to-end query latency and allocation on representative supported queries, with exact result bags, bindings, property errors, datasets, transactions, and cancellation preserved. It does not mean changing GQL graph semantics to match a cheaper SPARQL query.

The preserved executable baseline is `validation-gql-performance/baseline-jmh-benchmarks.jar` (SHA-256 `2e34efd0d2a09be35e960dbfe4d41245c79ff304f2e00d4a8323299465ad5c87`). All twenty untimed baseline diagnostic cases return their independently specified counts. Executed plans show a global property Group on the right of `LexicalReplayLeftJoin`, whose materialized right rows are scanned again for each path row. At 64 roots and fanout 32, the fixture has 2,048 property owners and 1,024 left paths: approximately 2.1 million compatibility candidates, derived from the fixture and cursor implementation rather than runtime telemetry. Merged topology uses two `RowDistinctPlan` barriers instead of one flattened native join. Named and merged label queries retain three generic label subplans; their estimates must not be reported as measured full-label scans.

The initial native regression selection records eight tests with six expected admission failures and no errors; its 74 integration controls pass. The scalar-property selection records twenty tests with seven failures and no errors. An absent owner opens a property scan; the second-distinct-value case reads 259 records rather than stopping at record three; explicit interrupts are not observed promptly. The nullable bound-owner case also returns a missing value instead of the existing property. Report copies are retained under `validation-gql-performance/*-red-reports`, and the original failures are appended to `initial-evidence.txt`. A preliminary test-compilation error was fixed before this behavioral evidence and is not counted as RED.

Implement a dedicated `GqlScalarProperty` value operator in an eager Extension. Its compiled child tuple retains the current dataset and graph selection, receives a fresh private owner binding only after the owner is known, skips nonliteral objects, and compares exact RDF terms with only one retained value. It closes immediately on a second distinct literal and polls interruption/replay cancellation throughout scans. Optimizer safety must retain observable cardinality errors and may not remove validation through COALESCE, CASE, later filters, or join reordering. Recheck selective incoming constraints in the optimized plans to avoid trading the global replay join for an unrelated-owner scan.

For edge DISTINCT, add a conservative source capability valid for its entire pinned read view. At most one live entry in LMDB's transaction-local context catalog is a sufficient proof for one canonical statement plane. Multiple active planes, mutable overlays, noncanonical IDs, stale snapshots, and unknown sources decline. Elide only the compiled SlotPlan barrier, leaving the original algebra DISTINCT intact. Each evaluation/replay creates a new source-bound plan; the generated-class cache rebinds the current runtime plan and does not cache this semantic proof. A future full-plan cache must include or revalidate the capability.

The first complete candidate passes 698 selected tests, including all 83 LMDB integration cases, but its initial warmed small-property comparison regresses from about 9.5 ms and 3.37 MB allocated per query to about 50 ms and 27.8 MB. Preserve those prototype measurements rather than presenting them as a completed improvement. The owner-directed algorithm has removed the global replay, but each of the 1,024 per-row lookups can still enter a full native tuple execution setup. Use the existing `LmdbNativeEvaluationStrategy.genericOnlyView()` to prepare this scalar's child through the indexed generic statement evaluator, recursively including graph-selection filters. This is a behavior-neutral physical-path change under Routine B: the retained pre-GREEN 83-case integration report directly exercises compiler property lowering and evaluator dispatch on all routes; rerun the same complete 698-test selection after the change. The view preserves the original TripleSource, dataset, scalar context and configuration. Do not introduce a property-value cache into a retained prepared value step; that would outlive an evaluation and violate the existing source-freshness test.

Begin from the published checkpoint and retain its source and executable benchmark artifact for paired before/after runs. Inspect `core/queryparser/gql/src/main/java/org/eclipse/rdf4j/query/parser/gql/GqlCompiler.java`, the shared evaluator, and LMDB native admission/planning. The initial candidate costs are global or repeatedly constructed scalar-property aggregation, node/label population relations, merged-context edge deduplication, repeated input evaluation under NEXT/UNION, and runtime planning of generated algebra. These are hypotheses until profiles and optimized plans establish their importance.

Build the unchanged checkpoint with JDK 25 and the required workspace Maven cache. Use `scripts/run-single-benchmark.sh` with a single benchmark method, pinned workload parameters, and the built executable JAR. For profiling use the supported JFR mode with one pinned case; it enforces ten ten-second iterations and one fork. For comparative timings use warmed multiple-fork JMH runs with identical JVM settings and fixtures, no concurrent Maven builds or other benchmark runs, result caching disabled, fixture setup outside timing, matching exact result checks in both variants, and measured adjacency engagement. Preserve raw iterations and report uncertainty. Include both evaluation and prepare-plus-evaluate measurements where changes affect compilation.

Expand the benchmark fixture to distinguish topology, labels, selective scalar properties, wide property projection, and NEXT/UNION composition. Vary unrelated graph size as well as result fanout, so small-result queries cannot hide full-graph work. Keep property multiplicity checks valid for arbitrary RDF, including duplicate terms across contexts, nonliteral objects, missing owners, and runtime parameter/dataset changes.

Before production edits, add the smallest in-repository failing work-bound or semantic regression and retain its Surefire report. A benchmark slow result alone does not satisfy the repository's test-first gate for an externally observable behavior change. For a behavior-neutral optimization using existing tests, first capture the same selection passing and document direct execution of the edited path. Root owns Maven and benchmarks; delegates inspect independent hot paths and edit only assigned files. Never run enabled tests with `-am` or `-q`.

After each fix, run its focused tests, the GQL module, and the affected evaluator/LMDB regressions. Rebuild downstream dependencies before validation. Compare the immutable baseline benchmark JAR with the final JAR using the same harness, seeds, and parameters. Stop broadening tests once concrete risks are resolved; spend remaining measurement time on stable before/after results and regression cases. Format before commit, verify source/artifact identity, publish to the existing feature branch with an expected-head lease, and supply complete changed files plus cumulative and incremental patches.

Evidence and private execution settings belong in `validation-gql-performance/`; publish an original benchmark report, raw JMH JSON, relevant profile summaries, exact commands, and limitations under `core/queryparser/gql/benchmarks/`. Preserve all earlier untracked evidence and do not publish private Maven settings, certificates, or machine-specific credentials. At this milestone's start no production optimization or new speedup has been established.

Revision 2026-11-09 after performance request: make measured execution performance the active step; retain the complete language/storage goal and add profiling, workload, correctness, and paired-measurement acceptance criteria before optimizing.

### Completed performance milestone

The formatted final implementation passes all 698 selected invocations across 38 classes, with zero failures, errors or skips: 26 model, 390 evaluator, 180 GQL and 102 LMDB cases. The latter include 83 integration invocations across cursor, generic adjacency and native adjacency routes. Preserved RED evidence covers seven scalar-property/work/cancellation failures and six snapshot-bound native plan-admission failures. The final claim-suppressed scalar-child dispatch has matched pre/post GREEN integration coverage and a direct path hit proof under Routine B. No test was skipped, and the initial SNAPSHOT_READ fixture correction is explicitly recorded as a test-contract correction.

All ten before/after JMH pairs completed with the same eight benchmark instruction listings, two independent forks per trial, ten two-second warmups, five two-second measurement iterations, one thread, identical heaps, result caching disabled, and an independently checked answer on every invocation. Named property filtering at 64 roots improves from 157.475 ± 15.116 ms to 8.579 ± 1.385 ms (18.36× by means); at 256 roots it improves from 833.614 ± 293.000 ms to 6.789 ± 0.869 ms (122.78×). Merged property filtering at 64 roots improves from 158.509 ± 17.078 ms to 6.024 ± 1.072 ms (26.31×). Preparation plus evaluation improves from 162.199 ± 26.347 ms to 6.741 ± 1.321 ms (24.06×). Errors are the reported 99.9% confidence-interval half-widths. These are workload-specific results on a shared host, not a blanket latency guarantee.

The one-root property result and all topology, label and NEXT/UNION latency controls have overlapping intervals; no latency improvement is established for them. The named topology after mean is higher with a wide interval and is retained in the report. Allocation falls 76.4% and 94.3% for the 64- and 256-root named property queries, while the one-root property case increases 10.7%. Merged topology allocation falls 18.4%, and the final diagnostic proves native kernel admission while preserving all 1,024 paths. The warmed engagement counters are reported separately from untimed diagnostics.

All 3,783 target production classes in the affected modules match their installed and measured artifacts. Formatting, copyright/SPDX verification, and diff checks pass. `core/queryparser/gql/benchmarks/2026-11-09-performance/` contains the original analysis, exact commands, complete raw warmed JMH results, selected plan exports, profile summaries, validation reports, source/artifact identities and reproduction scripts. The performance milestone is complete; the remaining language/storage and normative-conformance work remains active. Full repository CI and complete ISO conformance are not claimed.
