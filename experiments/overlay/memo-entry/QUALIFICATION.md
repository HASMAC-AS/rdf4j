# Weak memo entry-local metadata: qualification

Default-off experiment: `rdf4j.lmdb.nativeValueMemo.entryMetadata=true` requests entry-local primitive metadata. The actual path remains gated to the measured Java VM layout. It does not change weak referent ownership, hash calculation, collision replacement, semantic-version validation, admission policy, query boundaries, or ledger reservations.

## Layout

Measured OpenJDK 64-Bit Server VM 25.0.5+8-LTS, Eclipse Adoptium, amd64, compressed oops/class pointers, 8-byte alignment, non-compact headers. Entry objects are 48 bytes. A fully occupied 4,096-entry table plus reference array is 213,008 bytes, below the existing charge of 294,976 bytes. Growth still reserves the full new charge before releasing the old charge. Unsupported configurations retain the old parallel-array implementation. This is not a universal proof of the old accounting formula across arbitrary JVM layouts.

## Evidence and limitations

The original interrupted reactor invocation used `-am` for tests; it is not accepted qualification. Compliant pre/post-change NativeValueMemoTest (16) and NativeMemoPolicyTest (11) evidence is preserved. Existing tests directly call value/clear, exercise versions, collisions, growth, weak-reference clearing and ledger refusal; `hit-proof.txt` records the direct call sites.

Recovery root `-Pquick clean install` passed. Final module-only matrix ran **37 tests, zero failures/errors/skips**, separately in all four entry-metadata OFF/ON × diagnostic-hit-probe OFF/ON states. This includes ten entry-specific tests: actual path witness, primitive-only static entry fields, unsupported-layout fallback, collisions, weak-object reuse, cleared-entry growth, overlap reservation, refusal release, unknown versions and callback semantics. This is focused coverage, not a module or whole-repository pass.

Three focused map-growth replay tests passed, zero failures/errors/skips, with metadata enabled: originally compiled memo replacement, caller-context restoration on reopening failure, and conservative grouping recheck. The replay harness requires the existing native-engine/memo/synchronous-codegen flags and the diagnostic hit probe. Diagnostic instrumentation is OFF during timing.

Two configuration mistakes during recovery are retained, not hidden. The initial ordinary memo run failed one new test's requirement that diagnostic probing be enabled; the test now checks zero callbacks and retained identity when disabled, and the original callback/counter/clear contract when enabled. The initial replay invocation omitted the engine/memo/compiler flags and failed actual-path witnesses. Corrected runs above passed without changing production code, relaxing replay assertions or skipping tests.

## Timing artifact discipline

The metadata timing directory starts as a copy of the previously fastest frozen `lifetime/combined-classes`; only NativeValueMemo and its nested classes are recompiled into that copy. Both arms use this same directory and differ only by the metadata property. Identity-preserving transport and PR #6440 writer are enabled; sequential lookup is disabled. CPU-intensive builds and benchmarks use the shared JVM lock; the harness also holds its benchmark lock. The unprofiled ABBA runs have separate exact-output smoke runs. Manifests record class fingerprints, properties and actual path witnesses.

The HTTP fixture consumes/counts eight million 30-column tuples but emits only the requested one-million-row window. Completion is count completion, not transfer/render of all eight million rows. No browser paint or upstream-main comparison is implied.

## Reproduction

Use a fresh output name/directory rather than replacing any frozen timing artifact. With the existing Java 25 toolchain and dependency cache:

```bash
bash experiments/overlay/lifetime/mvn.sh -o -T 1C -Pquick clean install > maven-build.log 2>&1
bash experiments/overlay/memo-entry/final-tests.sh
bash experiments/overlay/memo-entry/replay-tests.sh
```

`resume-benchmark.sh` documents the exact frozen-class compilation and smoke/ABBA commands. It intentionally refuses an existing candidate class directory. `compare-previous-best.sh` compares the metadata-enabled candidate against the prior fastest frozen memo implementation, not merely against the newer candidate with its option OFF. Keep these comparisons distinct: newer source also contains the earlier memo-policy experiment's control fields.
