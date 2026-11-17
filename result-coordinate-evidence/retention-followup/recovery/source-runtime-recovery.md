# Fresh source and runtime recovery

Prepared 2026-11-17 from the current `AGENTS.md`, root/LMDB POMs, and the preserved compact-page/direct-memo harness builders. **These are recommendations for the root coordinator; none of the Java commands below has been run by this agent.** The saved November 16 Maven dependency archive is the recovery source. Small-cache fallback work has stopped.

## Preserve recovered identities before building

Restore and verify the two saved dependency ZIPs into the new checkout's `.m2_repo`, retaining the ZIPs and their verification records. Before `install` replaces local RDF4J SNAPSHOT jars, compare the restored jars with the 124 file entries in `result-coordinate-evidence/direct-memo-followup/runtimes/identity-witness-v2/classpath-manifest.json`. A fresh copy of the matching jar bytes can be preserved under this new phase. The historical manifest remains unchanged.

The six-class direct overlay is recoverable exactly from `direct-memo-followup/artifacts/identity-witness-v2-prefix.tar.gz`; its original tree SHA is `1db2e14afc44a344b418030c729354982a984e6cfdcbe3910f37bb7e2190e5e2`. The old 15-class common harness and original production/test class trees are missing locally. Their complete six harness sources survive and match the copied provenance in `direct-memo-followup/comparison-v2/runtime-identity/baseInputs-sourceProvenanceFile-prepared-harness-provenance.json`. Fresh source builds need new runtime identities and paths; the original runtime/protocol files must not be edited to accept them.

## 1. Shortest compliant baseline build

`AGENTS.md` requires the initial root quick clean install, Java 25+, offline Maven where possible, a workspace-local `.m2_repo`, complete build logs, and no `-am` or `-q` when tests run. Root `quick` sets `skipTests=true`, not `maven.test.skip=true`; LMDB explicitly sets `maven.test.skip=false`. The root install therefore compiles fresh test support as well as production classes. No separate test-compile pass is needed after it succeeds.

Run these commands only after dependency recovery and the root's exclusive Java-slot grant. `source-v1` must be a new directory; use a new version for another attempt.

```bash
cd /workspace/scratch/73b19a6d6f5c/rdf4j
export JAVA_HOME=/workspace/scratch/d0a0e51b9386/toolchain/jdk-25.0.5+8
export PATH="$JAVA_HOME/bin:$PATH"
retention_mvn=/workspace/scratch/d0a0e51b9386/toolchain/maven-mvnd-1.0.6-linux-amd64/mvn/bin/mvn
retention_recovery="$PWD/result-coordinate-evidence/retention-followup/recovery/source-v1"
mkdir "$retention_recovery"
set -o pipefail

"$retention_mvn" -B -ntp -o -Dmaven.repo.local=.m2_repo \
  -Dmaven.compiler.showWarnings=false -T 1C -Pquick \
  -DskipFrontend=true clean install 2>&1 \
  | tee maven-build.log "$retention_recovery/root-clean-install.log" \
  | awk '/\[WARNING\]/ { next } /\[ERROR\]/ { print; next } /Reactor Summary/ { summary=1 } summary { print }'
```

Require a terminal exit code zero before proceeding. Use a process timeout of at least 60 seconds, and retain any failure. For a missing dependency, the repository permits one exact-command retry without `-o`, followed by a return to offline operation. For another failure, its stated fallback is the exact command without `-T 1C`. The old direct-memo report records an unrelated Spring/Kotlin reactor dependency block; a new attempt must report its actual outcome rather than inherit that result or claim a successful reactor build from a module-only build.

After the initial gate, a later rebuild limited to LMDB and its dependencies may use the same flags with `-pl core/sail/lmdb -am -Pquick install`; `-am` is valid only while tests are skipped. Source edits require the relevant TDD and formatting gates; this recovery itself changes no production source.

## 2. Focused baseline tests from freshly compiled source

Use the existing eight-class selection, one fork, a 4 GiB test heap, and explicit memo disabled. `surefire:test` is the shortest path because the successful clean install already compiled current production and tests. Preserve fresh XML/text reports in a new phase directory immediately, along with before/after source hashes and the actual Surefire execution classpath.

```bash
retention_selection='NativeResultMaterializerTest,LmdbNativeProjectionValueReuseTest,LmdbNativeIrFactorCoordinateTransportTest,LmdbNativeIrOptionalFanoutCoordinateTest,LmdbNativePackedFtreeTest#delayedPackedRowsFallBackAfterAdvanceAndIterationClose,KernelResultCoordinatesTest,KernelResultCoordinateRepresentationsTest,KernelResultRowSelectionsTest'

"$retention_mvn" -B -ntp -o -Dmaven.repo.local=.m2_repo \
  -Pquick -DskipTests=false -pl core/sail/lmdb \
  "-Dtest=$retention_selection" -DforkCount=1 \
  -Drdf4j.lmdb.test.maxHeap=4G \
  -Drdf4j.lmdb.nativeValueMemo.enabled=false \
  -Dsurefire.runOrder=alphabetical surefire:test \
  > "$retention_recovery/baseline-focused.log" 2>&1
```

The preserved direct-memo baseline had 106 cases in this selection. Compare the actual fresh test names/count with the selected current sources and archived baseline, and require zero failures/errors/skips; do not silently accept a smaller selection. These tests include existing compiled/interpreted IR coverage. Source hashes, not a historical count alone, determine which validation belongs to the fresh runtime.

## 3. Build a new common harness from preserved source

Create a new dependency classpath from the fresh source build:

```bash
"$retention_mvn" -B -ntp -o -Dmaven.repo.local=.m2_repo \
  -Pquick -pl core/sail/lmdb dependency:build-classpath \
  "-Dmdep.outputFile=$retention_recovery/dependencies.classpath.txt" \
  > "$retention_recovery/dependency-classpath.log" 2>&1

retention_base_cp="$PWD/core/sail/lmdb/target/test-classes:$PWD/core/sail/lmdb/target/classes:$(cat "$retention_recovery/dependencies.classpath.txt")"
retention_harness_src="$PWD/result-coordinate-evidence/compact-page-followup/harness-src"
mkdir "$retention_recovery/common-classes"
mkdir "$retention_recovery/root-launcher-classes"

"$JAVA_HOME/bin/javac" --release 25 -proc:none -implicit:none \
  -sourcepath '' -encoding UTF-8 -cp "$retention_base_cp" \
  -d "$retention_recovery/common-classes" \
  "$retention_harness_src/org/eclipse/rdf4j/sail/lmdb/NamespaceAdjacencyObservation.java" \
  "$retention_harness_src/org/eclipse/rdf4j/sail/lmdb/benchmark/OptionalFanoutExperiment.java" \
  "$retention_harness_src/org/eclipse/rdf4j/sail/lmdb/benchmark/SharedSelectionExperiment.java" \
  "$retention_harness_src/org/eclipse/rdf4j/sail/lmdb/benchmark/SharedSelectionExperimentFixture.java" \
  "$retention_harness_src/org/eclipse/rdf4j/sail/lmdb/benchmark/SharedSelectionFixtureCheck.java" \
  "$retention_harness_src/org/eclipse/rdf4j/sail/lmdb/evaluation/RootQualifiedSharedSelectionLauncher.java" \
  > "$retention_recovery/common-compile.log" 2>&1

"$JAVA_HOME/bin/javac" --release 25 -proc:none -implicit:none \
  -sourcepath '' -encoding UTF-8 \
  -cp "$retention_recovery/common-classes:$retention_base_cp" \
  -d "$retention_recovery/root-launcher-classes" \
  "$PWD/result-coordinate-evidence/root-qualified-launcher/src/org/eclipse/rdf4j/sail/lmdb/evaluation/RootQualifiedOptionalFanoutLauncher.java" \
  > "$retention_recovery/root-launcher-compile.log" 2>&1
```

Require the expected 15 common classes and the single fanout launcher class, with no implicit implementation classes emitted. Record exact argv, return code, source hashes, complete output class maps and tool hashes. Then copy the fresh LMDB test/production outputs and dependency jars into new immutable phase directories before subsequent builds can alter them. The logical classpath order is common harness, LMDB test classes, LMDB production classes, dependency jars, fanout root launcher. Derive the actual entry count and new digest; do not hard-code a claim that this is the original common128.

The preserved `compact-page-followup/compile-harness.py` cannot perform this recovery unchanged: it copies old compiled classes and validates missing historical descriptors. Keep it untouched. A new recovery assembler should consume the source-based outputs above, preserve all dependencies and expose a new descriptor to the retention phase.

## 4. Direct memo qualification on the new baseline

The previous contract/compiler runners also hard-pin old descriptor paths, source maps and exact tree hashes. Port their validation logic into a newly named retention-phase adapter; do not weaken or rewrite the old files. In particular, pointing `run_contracts.py` at a new output directory alone does not replace its pinned base runtime.

The fresh experimental compile has five explicit Java inputs, all preserved under `direct-memo-followup/runtimes/identity-witness-v2/generated/src/org/eclipse/rdf4j/sail/lmdb/evaluation/`: `NativeResultMaterializer`, `NativeProjectedBindingSet`, `NativeDirectValueMemo`, `NativeDirectValueMemoStatistics`, and `DirectMemoLauncher`. The prior command shape is:

```text
javac --release 25 -g -proc:none -implicit:none -sourcepath '' \
  -cp NEW_IMMUTABLE_COMMON_CLASSPATH -d NEW_EMITTED_CLASSES FIVE_EXPLICIT_JAVA_SOURCES
```

Preserve all emitted classes for audit. Select exactly the original six overlay class names; leave the original plan, factor nested classes, projection layout and anonymous iterator supplied by the newly frozen baseline. Check unchanged source regions and emitted plan bytes against that baseline. Diagnose compilation-flag differences explicitly instead of substituting old expected hashes. The overlay's placement must be identical across ordinary, dense and direct treatments.

Use isolated copied production/test trees and a generated POM as the previous `copied_pom` helper does. Recompile selected tests against the new runtime. The minimum carried-forward qualification is:

| Selection | Sources and expected scope |
| --- | --- |
| Direct table contracts | `contracts/test-src/.../NativeDirectValueMemoContractTest.java`: all 20 sealed methods, including three identity-witness contracts |
| Direct integration contracts | `contracts/integration-src/.../NativeDirectValueMemoIntegrationTest.java`: all 12 sealed methods, including standalone ownership handoff |
| Existing focused suite | The eight-class selection above with direct memo disabled |
| Enabled lifetime suite | Eight existing `LmdbNativeIrFactorCoordinateTransportTest` methods named in `compile_freeze_direct_runtime.py`, with memo true and transport off; both IR tiers |

For the isolated POM, the Surefire command shape is `mvn -B -ntp -o -Dmaven.repo.local=.m2_repo -Pquick -DskipTests=false -f NEW_ISOLATED_POM -Dtest=EXACT_SELECTION -DforkCount=1 -Drdf4j.lmdb.test.maxHeap=4G -Dsurefire.runOrder=alphabetical surefire:test`, plus the explicit treatment properties. Verify properties and the selected implementation/test classpath from actual XML. Do not run the enabled cases against an unmodified production tree and infer feature coverage merely from a true property.

The archived red evidence still documents the existing algorithm's development; new retention behavior needs its own smallest failing contracts before implementation. Fresh green evidence and lifecycle qualification must belong to the new source/runtime identities. The prior six-fixture lifecycle sequence is a useful control template, but the root must specify the new retention experiment's coverage and acceptance before collecting observations.

## State

Only this recovery note was written. No old study files were changed, no Java or Maven process was launched, and no build/test outcome is claimed. Root owns dependency restoration, execution slots and the new retention protocol.
