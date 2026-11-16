# Direct memo validation and runtime assembly

This directory runs the sealed table and integration contracts against isolated copies of the preserved LMDB classes. It then compiles an experimental prefix containing six classes, runs the existing focused selection with the new feature disabled, and freezes a timing descriptor only after the raw correctness, enabled lifetime, and lifecycle evidence reconcile. Production sources, module targets, previous evidence, and the Git index are never written by these scripts.

All commands below run from `/workspace/scratch/37853bcaea36/rdf4j`. The coordinating agent must grant the exclusive Java slot after the required build before any command containing `--slot-granted` or `--slot-released` is executed. Source inspection, Python syntax checks, report replay, and the final freeze do not launch Java.

## Test first

The primary `contract-seal.json` pins the unit source, red-only stub, exact method names, intentional failure set, and three identity-witness methods. `integration-contract-seal.json` does the same for the real projected-result paths. Previous seals are kept under `history`. The runner archives each seal and complete source beside its raw reports. Every stage needs a new directory name.

The first unit run, `red-v1`, ran all 20 cases with 18 intentional failures and no errors or skips, but its wrapper rejected JUnit's `TestReporter` suffix in one XML case name. That attempt remains preserved and is not accepted implementation evidence. The runner now stores both the exact `rawName` and the normalized selection name. The revised unit contract also corrects its scope-clear history expectation and allocates through a real probe before checking strong ownership.

Run the current sealed contracts before implementation:

```bash
python3 result-coordinate-evidence/direct-memo-followup/contracts/run_contracts.py red-v2 --mode red --slot-granted
python3 result-coordinate-evidence/direct-memo-followup/contracts/run_contracts.py integration-red-v3 --mode red --contract-seal result-coordinate-evidence/direct-memo-followup/contracts/integration-contract-seal.json --slot-granted
```

The unit `red-v2` receipt accepts 20 cases with 18 intended feature failures, no errors, and no skips. Integration `integration-red-v1` ran 11 cases with 6 intended failures; its wrapper rejected the incorrectly expected seventh failure. Source inspection confirmed that full-mask alias separation already works in the preserved implementation. That unchanged case is now classified as preservation coverage, and integration `integration-red-v2` accepts 11 cases with 6 feature failures, 5 passing preservation cases, no errors, and no skips. Every rejected attempt and prior seal remains archived.

A further standalone ownership contract was added before the outer integration changed. It requires a normal source lookup when the cached wrapper still belongs to a ValueStore and is not externally owned, then permits reuse after the source returns an externally owned representative. Integration `integration-red-v3` accepted all 12 cases with 7 intended feature failures, 5 passing preservation cases, no errors, and no skips. The current integration seal is revision 4.

An accepted red stage has exactly the sealed feature failures, no unexpected errors, no skipped tests, and no semantic, source-call, or collection failure hidden behind the feature marker. Compilation failure cannot qualify as red. The stub is compiled only into the isolated red test tree; it never enters an experimental runtime.

## Compile the experimental runtime

The generator must first write `contracts/generated/direct-memo-v2/source-receipt.json`, `direct-memo-overlay.patch`, and three Java sources under `src/org/eclipse/rdf4j/sail/lmdb/evaluation`: `NativeResultMaterializer`, `NativeProjectedBindingSet`, and `NativeDirectValueMemo`. The receipt pins the generated sources, patch, generator, accepted unit red receipt, and accepted integration red receipt. Both red receipts are replayed from XML before compilation.

```bash
python3 result-coordinate-evidence/direct-memo-followup/contracts/compile_freeze_direct_runtime.py compile --generated result-coordinate-evidence/direct-memo-followup/contracts/generated/direct-memo-v2 --output result-coordinate-evidence/direct-memo-followup/runtimes/identity-witness-v2 --slot-granted
```

The compiler archives those sources with the final statistics helper and delegating launcher. Its `compiled-sources.json` contains the five absolute Java paths and their SHA-256 digests. The emitted classes, actual compiler command, prefix classes, common dependency entries, source boundaries, and tools are recorded independently.

The timing prefix contains exactly `NativeResultMaterializer`, `NativeProjectedBindingSet`, `NativeDirectValueMemo`, `NativeDirectValueMemoStatistics`, its `Snapshot`, and `DirectMemoLauncher`. It precedes the unchanged common128 runtime. The original materializer plan, factor memo/page, projection layout, and anonymous binding iterator remain selected from the original runtime. Their source bodies must remain unchanged; newly emitted copies are archived for inspection and excluded from the timing prefix. The emitted plan must also match the original bytes exactly.

The first compiled candidate directory, `runtimes/identity-witness-v1`, preserves a failed javac attempt: the wrapper declared `Exception` while its preserved delegates declare `Throwable`. The wrapper signature and rethrow type were corrected, with its original source archived. The working generated bundle is `contracts/generated/direct-memo-v2`, and the successful six-class compilation is `runtimes/identity-witness-v2`.

Compilation produces no timing descriptor. The compiled prefix is only a candidate awaiting validation.

## Green contracts and existing coverage

Use the same sealed sources and the actual accepted red directories:

```bash
python3 result-coordinate-evidence/direct-memo-followup/contracts/run_contracts.py green-v1 --mode green --overlay result-coordinate-evidence/direct-memo-followup/runtimes/identity-witness-v2/classes --red-validation result-coordinate-evidence/direct-memo-followup/contracts/validation/red-v2 --slot-granted
python3 result-coordinate-evidence/direct-memo-followup/contracts/run_contracts.py integration-green-v1 --mode green --contract-seal result-coordinate-evidence/direct-memo-followup/contracts/integration-contract-seal.json --overlay result-coordinate-evidence/direct-memo-followup/runtimes/identity-witness-v2/classes --red-validation result-coordinate-evidence/direct-memo-followup/contracts/validation/integration-red-v3 --slot-granted
python3 result-coordinate-evidence/direct-memo-followup/contracts/compile_freeze_direct_runtime.py run-existing --runtime result-coordinate-evidence/direct-memo-followup/runtimes/identity-witness-v2 --output result-coordinate-evidence/direct-memo-followup/validation/focused-green-v1 --slot-granted
python3 result-coordinate-evidence/direct-memo-followup/contracts/compile_freeze_direct_runtime.py run-existing --runtime result-coordinate-evidence/direct-memo-followup/runtimes/identity-witness-v2 --kind enabled-lifetime --output result-coordinate-evidence/direct-memo-followup/validation/enabled-lifetime-green-v1 --slot-granted
```

The focused selection explicitly recompiles its eight current test sources against the copied frozen production tree and executes exactly 106 cases with the direct-memo flag false. All accepted green selections require zero failures, errors, and skips. Freeze compares the actual selected class bytes with the timing composition and rejects a test tree that shadows an implementation class.

A separate enabled selection compiles the unchanged `LmdbNativeIrFactorCoordinateTransportTest` source and runs exactly eight existing methods: UNION/OPTIONAL bag semantics, retained first-row early close, retained first-row cancellation, and cancellation racing with close, each in compiled and interpreted IR. The two factor-reuse-count methods are excluded because this treatment uses transport OFF. The runner explicitly supplies direct memo true and transport off, verifies both values in the actual Surefire XML, and requires 8 passing cases with no errors or skips. Existing test setup does not overwrite either flag. This correctness selection may force its stated IR tier; the later timing study still uses normal AUTO.

Javac uses the preserved common classpath. Surefire uses the copied LMDB production tree plus freshly built module test support and the workspace Maven dependencies. The actual execution classpath is extracted from XML, hashed, and compared between red, green, and the preserved focused baseline. These are distinct dependency scopes; the scripts do not claim the original timing jars are all executed by Surefire.

## Identity counterexample and lifecycle

A separately generated four-array variant may be demonstrated by compiling it into its own diagnostic prefix, then running:

```bash
python3 result-coordinate-evidence/direct-memo-followup/contracts/run_contracts.py identity-counterexample-v1 --mode witness-counterexample --overlay PATH_TO_DIAGNOSTIC_PREFIX --red-validation result-coordinate-evidence/direct-memo-followup/contracts/validation/red-v2 --slot-granted
```

This mode executes only the three sealed identity-witness methods and requires exactly their three intended assertion failures. Its receipt is diagnostic and cannot qualify a timing runtime. It never changes the strengthened candidate.

The benchmark package owns the six full-fixture lifecycle runs and their independent replay. It verifies 78 queries, 26 direct owners, no direct owners in the controls, source/compile/resolver intervals, checksums, routes, and released ledgers. The timing fields produced by the unchanged harness are retained but neither summarized nor pooled into the primary comparison.

```bash
python3 result-coordinate-evidence/direct-memo-followup/benchmark/lifecycle-preflight.py --runtime result-coordinate-evidence/direct-memo-followup/runtimes/identity-witness-v2 --output result-coordinate-evidence/direct-memo-followup/lifecycle/full-fixtures-v1 --slot-released
```

## Freeze after validation

```bash
python3 result-coordinate-evidence/direct-memo-followup/contracts/compile_freeze_direct_runtime.py freeze --runtime result-coordinate-evidence/direct-memo-followup/runtimes/identity-witness-v2 --contract-validation result-coordinate-evidence/direct-memo-followup/contracts/validation/green-v1 --focused-validation result-coordinate-evidence/direct-memo-followup/validation/focused-green-v1 --integration-validation result-coordinate-evidence/direct-memo-followup/contracts/validation/integration-green-v1 --enabled-lifetime-validation result-coordinate-evidence/direct-memo-followup/validation/enabled-lifetime-green-v1 --lifecycle-validation result-coordinate-evidence/direct-memo-followup/lifecycle/full-fixtures-v1
```

Freeze replays the actual test XML and lifecycle files, verifies exact source/class/dependency identities, and connects the runtime to both accepted red receipts. It also reconciles the existing 106-case baseline at `publication-qualified-followup/validation/baseline-focused-v2`; that baseline used the prior publication prefix over the same common128 runtime. The final descriptor is checked by the actual comparison driver before the candidate is reported ready. No benchmark is launched by freeze.

The experiment remains disabled by default. Array reservations and the fixed slot cap do not bound the decoded payloads that strong `Value` references can retain. That limitation is recorded in the validation receipt and remains separate from the observed ledger peaks.
