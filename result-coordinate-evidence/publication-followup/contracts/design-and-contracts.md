# Target publication ablation: implementation and semantic contracts

This is an experimental control, not a proposed query-planner policy. The selected production materializer remains unchanged. The generated source adds a per-plan omission array and one early return inside `NativeResultMaterializer.rememberFactor`. All timed arms use the same generated class files. The process property chooses which canonical target banks omit publication; it does not change result-coordinate transport, ordinary value reuse, factor lookup, ownership, or cleanup code.

## Exact experiment boundary

`build_publication_overlay.py` accepts only the selected materializer source with SHA-256 `cb0749719313b85c8cd134b6c03085f712915188f0e53333cbd46e19e2f0f96e`. It creates a fresh output directory containing the complete generated Java file, a unified patch, and a source receipt. Its structural assertion verifies that removing the single early-return guard from the materializer body reproduces the selected body exactly. The immutable plan receives the configuration parser and membership array.

The property is `rdf4j.lmdb.resultCoordinates.experiment.omitFactorTargets`. An absent property or exactly empty value selects no omissions. Otherwise it is a comma-separated list of canonical projection target names. Surrounding token whitespace is trimmed. Unknown names, repeated names, empty tokens, and names which denote an alias instead of its canonical target fail during plan construction. The configuration is sampled once per plan. Later property changes do not change an existing plan.

Every plan owns a `boolean[]` with the projection width, even when no targets are omitted. The publication entry tests `plan.omittedFactorTargets[plan.aliases[target]]`. This is deliberately independent of a 64-bit mask, and an alias beyond target 63 still tests the same canonical bank. Names are resolved during setup; the hot path performs no string parsing or hashing.

The additional array and plan reference are experimental metadata outside the existing query-memory ledger. They are common to all arms and are included in whatever preparation/allocation windows construct the plan. The study must not claim that these extra metadata bytes are charged by the ledger. Ordinary-cache, page, directory, and coordinate-memo reservations retain their existing accounting code.

The guard runs before `captureCoordinate`. It therefore omits publication-side coordinate validation/loading, page lookup/admission, witness/reference comparison and writes, and any future cleanup of the pages not created. It does not omit `findFactor`, including its unsuccessful lookups for excluded targets when other target pages exist. It does not omit row `position`, changed-slot collection, namespace-retirement delivery, frame transport, or source snapshots. A measured difference is the combined consequence of omitted publication and the changed cache population; it is not an instruction-level cost attribution.

Every instantiated materializer plan validates the property. A fixture with an intermediate projection lacking a selected name will fail setup rather than quietly omit a different bank. Run masked diagnostics before the timing protocol is frozen to establish that the actual fixture plans have the expected schema.

## Canonical targets and ordinary reuse

The plan aliases targets only when they have the same definite source slot and the same value-position mask. Factor pages are keyed by that canonical target; ordinary memo entries and coordinate memo entries are still indexed by the requested target. Filtering only the requested target would permit another alias to republish into the allegedly omitted bank. The guard therefore resolves `plan.aliases[target]` on every publication entry.

Ordinary last-value reuse still validates source/view/exposure identity, weak value survival, semantic version, revision and owner witnesses. An ordinary hit still calls `rememberFactor` for an enabled target. This matters when identical values are accepted at different factor coordinates. `acceptedReplayHitAdmitsItsOriginalFactorCoordinate` in the selected materializer suite already demonstrates that removing publication from every ordinary hit loses useful replay reuse.

Result correctness does not require every optional factor entry to exist. The ordinary resolution path remains available. An excluded bank may nevertheless lose useful reuse on other workloads. Zero successful factor hits in one execution does not establish a universal consecutive-only property: rejected rows, unbound gaps, restored frames, namespaces, alternate plans, retained weak referents and aliases can change future reuse. No production promotion is justified by the fixture-specific property.

## Why the main comparison uses 512 MiB per query

The preserved original shared-tail diagnostic at 256 MiB admitted 16,364 pages and refused another page reservation. Its target admissions were 126 for `l0`, 8,048 for `l1`, 126 for `r0`, and 8,064 for `r1`. It recorded 1,024,128 factor hits for each right target and none for either left target. These counts come directly from the final JSON summary in `compact-page-followup/diagnostic/runs/original/page-records/owner-4.jsonl`.

Omitting left publication at that budget can release room for missing right pages. A speedup would then combine avoided left work and additional right reuse. The main experiment therefore uses the selected original dense-page representation with a matched 512 MiB query limit in every arm. The global manager and fixed JVM heap remain 1 GiB. The diagnostic must confirm no optional page admission refusal and the comparison must verify unchanged resolver calls between publication-all and omission arms. A separate 256 MiB diagnostic quantifies admission redistribution without treating its instrumented time as a performance estimate.

## Six isolated semantic tests

`test-src/org/eclipse/rdf4j/sail/lmdb/evaluation/PublicationAblationContractTest.java` is a complete standalone JUnit test source. It uses existing package-visible interfaces only, so it compiles against both the selected original classes and the overlay. It does not reflect into or manipulate private cache structures. It saves and restores the property around every test. Run this suite without concurrent tests that mutate the same process property.

| Test | Observable contract |
|---|---|
| `rejectsUnknownDuplicateEmptyAndAliasTargetNames` | Invalid lists fail during plan construction; absent target names cannot silently change the experiment. |
| `canonicalExclusionCoversAliasesAndPreservesOtherBanks` | An omitted left bank and its alias resolve A–B–A three times; an enabled right bank reuses its A factor entry. Changing the property after plan creation has no effect. Retained rows remain usable after materializer and source closure. |
| `ordinaryHitsPublishEveryAcceptedCoordinateForEnabledTargets` | Enabled right-side A at coordinates 0 and 1 followed by B at 2, then replay of accepted A at 1, uses the original A without another resolver call. A rejected coordinate-0 producer row is not required for that reuse. |
| `omissionPreservesOrdinaryReuseAndFallsBackAfterUnboundAndViewChanges` | Consecutive A values still use ordinary reuse. An unbound gap causes ordinary fallback for the omitted bank. A changed source-view token yields a replacement value while an earlier detached row keeps its captured value. |
| `omittedTargetReservesNoFactorPagesAndSurvivesLedgerFirstClosure` | An all-omitted one-column sequence retains only the measured ordinary reservation. Ledger-first closure and repeated owner close leave zero query/global bytes, and the detached row stays readable. |
| `canonicalExclusionCoversAliasesBeyondOneLongMaskWord` | Target 64 aliases target 0; excluding target 0 suppresses publication through both positions and introduces no extra factor reservation. |

The enabled-publication test is expected to pass on the original implementation; the other five are expected to fail because the original does not interpret the omission property. These predictions are not test results. Root orchestration preserves actual pre-change Surefire failures and then runs the identical source against the generated overlay. The existing selected 106-case focused suite should also pass against the overlay when the property is absent. No existing test is weakened for this experiment.

To run the isolated test without replacing the module's selected classes, compile it into a fresh test output directory containing a copy of the installed module test classes. Merge the selected production class tree with the generated overlay class files in a fresh production output directory. A copied module POM with an absolute parent path can point `build.outputDirectory`, `build.testOutputDirectory`, and `build.directory` at those directories. Invoke only offline `surefire:test` with the module's original test dependencies and configuration, assertions enabled, no `-am`, and no `-q`. Root owns those commands and evidence preservation.

## Comparable implementation examined

SQLite's [function auxiliary-data contract](https://sqlite.org/c3ref/get_auxdata.html) treats cached computation as optional: a lookup can return no cached object, and the engine can discard it when arguments change, the statement resets, or allocation fails. Destruction remains an explicit once-only responsibility. This is a useful comparison for separating cache admission from result semantics. It is not evidence that this experiment's excluded targets cannot replay.

The project's [internal `AuxData` structure](https://github.com/sqlite/sqlite/blob/master/src/vdbeInt.h) carries the function instruction number, argument index, auxiliary pointer, destructor, and linked-list successor. The accompanying comment ties its lifetime to one virtual machine and states that its entries are destroyed when that machine halts, if not earlier. The structural lesson is to keep the reuse key and owner lifetime explicit even when admission is optional. No SQLite code or dependency is copied here. Attempts to read the current `vdbeaux.c` and `vdbeapi.c` bodies through the source site were unavailable; no claim in this note depends on those unread bodies.

## Validation status before root execution

The generator's Python source compiled successfully as Python syntax. All six Java tests are below the repository's 120-character line limit and carry the required 2026 BSD header and Codex signature. The repository copyright script completed with exit code 0 and `All files have valid copyright headers and SPDX lines.` That script scans Maven source roots; the isolated test header was also inspected separately. No Java compiler, JVM, Maven build, or test process was launched by this contracts worker.
