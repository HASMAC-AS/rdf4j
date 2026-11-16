# Compile and qualify the scalar accounting runtime

Run these commands from `/workspace/scratch/37853bcaea36/rdf4j`. The coordinator owns the exclusive Java slot; each command that can launch Java requires `--slot-released`. Execute the stages serially. Every output directory is new and exclusive; preserve failed attempts and choose a new version suffix when a corrected attempt is necessary.

The builder does not change production sources, the module's target trees, or prior runtime artifacts. Compilation and timing-runtime freeze are separate. A compiled prefix is not qualified for performance measurement until the source, tests, and complete lifecycle evidence pass the freeze operation.

## Baseline existing coverage

The original two-class publication overlay is applied to copied frozen production classes. Test support classes are copied from the qualified fresh module test tree produced by the coordinator's prerequisite build. All eight selected focused source files are then archived and explicitly compiled into that isolated copy, so the tested selection comes from the current selected sources. Publication tests likewise compile their pinned source into the isolated copy. The actual Surefire execution classpath, fresh input class map, selected source copies, compilation commands, and executed class bytes are recorded. The focused selection has 106 tests in eight suites; the existing publication contract selection has six tests in one suite.

The preserved `baseline-focused-v1` attempt ran only 93 tests because the older frozen test tree lacked thirteen selected materializer contracts. Its passing Maven status did not satisfy the declared selection and was rejected. The corrected commands below retain the 106-test requirement and use fresh source compilation; the earlier attempt is not overwritten.

```bash
python3 result-coordinate-evidence/publication-qualified-followup/contracts/compile_freeze_qualified_runtime.py run-existing \
  --baseline --kind focused \
  --output result-coordinate-evidence/publication-qualified-followup/validation/baseline-focused-v2 \
  --slot-released

python3 result-coordinate-evidence/publication-qualified-followup/contracts/compile_freeze_qualified_runtime.py run-existing \
  --baseline --kind publication \
  --output result-coordinate-evidence/publication-qualified-followup/validation/baseline-publication-v1 \
  --slot-released
```

The fifteen new accounting contracts have their own red/green runner, described in `README.md`. Their red stage must be accepted before generating the observer. Generation produces the materializer, helper, source patch, and source receipt; it launches no Java.

## Compile the generated sources

```bash
python3 result-coordinate-evidence/publication-qualified-followup/contracts/compile_freeze_qualified_runtime.py compile \
  --generated result-coordinate-evidence/publication-qualified-followup/contracts/generated/rare-accounting-v1 \
  --output result-coordinate-evidence/publication-qualified-followup/runtimes/qualified-accounting-v1 \
  --slot-released
```

The compiler uses the exact prior 129-entry publication runtime. Its flags match the earlier compilation: Java release 25, debug information enabled, annotation processing and implicit source compilation disabled, and an empty source path. It compiles the materializer, primitive helper, and adapter launcher together.

The `classes` directory contains exactly four classes: the materializer outer class, `PublicationQualifiedAccounting`, its `Snapshot`, and `PublicationQualifiedLauncher`. That prefix precedes the unchanged prior publication runtime, giving 130 classpath entries. The prior plan and original nested page/memo class files remain selected from the base runtime.

The unchanged plan's emitted bytes must match the prior publication plan exactly. The nested page/memo source region must remain identical, and the selected runtime's nested bytes remain unchanged. Newly inserted preceding observer lines shift nested debug line positions; the unused emitted nested files are recorded separately and never copied into a selected prefix. No claim of byte equality is made for those unused files.

`validation-overlay` contains the four new classes plus the prior publication plan. Existing isolated test runners need this five-class composition because their copied production directory otherwise contains the pre-publication plan. The accounting contract runner already overlays the prior publication classes before applying the new four-class prefix.

The compile receipt pins the builder, generator, identity helper, lifecycle driver, Java compiler, source copies, and emitted class maps. Keep those files unchanged after compilation. No timing descriptor exists at this stage.

## Green contracts and existing coverage

```bash
python3 result-coordinate-evidence/publication-qualified-followup/contracts/run_contracts.py green-v1 \
  --mode green \
  --overlay result-coordinate-evidence/publication-qualified-followup/runtimes/qualified-accounting-v1/classes \
  --red-validation result-coordinate-evidence/publication-qualified-followup/contracts/validation/red-v1 \
  --slot-granted

python3 result-coordinate-evidence/publication-qualified-followup/contracts/compile_freeze_qualified_runtime.py run-existing \
  --runtime result-coordinate-evidence/publication-qualified-followup/runtimes/qualified-accounting-v1 \
  --kind focused \
  --output result-coordinate-evidence/publication-qualified-followup/validation/qualified-focused-v1 \
  --slot-released

python3 result-coordinate-evidence/publication-qualified-followup/contracts/compile_freeze_qualified_runtime.py run-existing \
  --runtime result-coordinate-evidence/publication-qualified-followup/runtimes/qualified-accounting-v1 \
  --kind publication \
  --output result-coordinate-evidence/publication-qualified-followup/validation/qualified-publication-v1 \
  --slot-released
```

Acceptance requires fifteen accounting, 106 focused, and six publication tests, all passing without skips or errors. The builder independently parses raw Surefire XML, checks exact contract identities, compares baseline and green selections, verifies copied class bytes against the selected composition, and checks unchanged module source maps. Actual workspace test dependency bytes must match across baseline, red, and green. These test dependencies are recorded honestly; the performance runtime separately retains the frozen publication dependencies.

## Six complete lifecycle preflights

The preflight uses both full-size fixtures and all three arms, in the fixed order fanout OFF/all/omit followed by shared-tail OFF/all/omit. Each fresh JVM runs eight warmups and five observed iterations. This gives six JVMs and 78 queries, with exactly thirteen owner records per JVM. Full inputs preserve the normal AUTO routes that the primary experiment requires.

```bash
python3 result-coordinate-evidence/publication-qualified-followup/contracts/compile_freeze_qualified_runtime.py launch-lifecycle \
  --runtime result-coordinate-evidence/publication-qualified-followup/runtimes/qualified-accounting-v1 \
  --output result-coordinate-evidence/publication-qualified-followup/lifecycle/full-fixtures-v1 \
  --slot-released
```

Before the first JVM, the command writes the complete lifecycle protocol and its hash commitment. Input sizes, graph/query identities, masks, JVM settings, normal AUTO, result oracles, ledger gates, rare accounting, and compiled measured tiers are identical to the proposed primary experiment. The original harness still writes its raw timing fields. This stage produces no latency or allocation estimates, screens no performance outcome, and contributes no observations to the primary comparison.

The launch stops on the first process or qualification failure and preserves the attempted prefix. On success it audits all six raw commands, process intervals, samples, root audits, and owner records and writes a separate validation receipt. Re-run the read-only audit without Java when needed:

```bash
python3 result-coordinate-evidence/publication-qualified-followup/contracts/compile_freeze_qualified_runtime.py qualify-lifecycle \
  --runtime result-coordinate-evidence/publication-qualified-followup/runtimes/qualified-accounting-v1 \
  --output result-coordinate-evidence/publication-qualified-followup/lifecycle/full-fixtures-v1
```

## Freeze the qualified runtime

```bash
python3 result-coordinate-evidence/publication-qualified-followup/contracts/compile_freeze_qualified_runtime.py freeze \
  --runtime result-coordinate-evidence/publication-qualified-followup/runtimes/qualified-accounting-v1 \
  --baseline-focused-validation result-coordinate-evidence/publication-qualified-followup/validation/baseline-focused-v2 \
  --baseline-publication-validation result-coordinate-evidence/publication-qualified-followup/validation/baseline-publication-v1 \
  --accounting-validation result-coordinate-evidence/publication-qualified-followup/contracts/validation/green-v1 \
  --focused-validation result-coordinate-evidence/publication-qualified-followup/validation/qualified-focused-v1 \
  --publication-validation result-coordinate-evidence/publication-qualified-followup/validation/qualified-publication-v1 \
  --lifecycle-validation result-coordinate-evidence/publication-qualified-followup/lifecycle/full-fixtures-v1
```

Freeze launches no Java. It verifies 112 baseline tests, 127 post-overlay tests, and all 78 lifecycle queries; recomputes runtime and tested class identities; and reconciles the lifecycle receipt from raw output. It then creates `validation.json` and `runtime-descriptor.json`. The prospective benchmark runner's actual runtime validator checks the newly frozen descriptor before the operation reports success.

Only after that operation succeeds may `benchmark/run-comparison.py --freeze` create the primary performance protocol. The primary study remains its separately specified 36 fresh JVMs and 468 queries; it excludes all lifecycle and prior-phase observations.
