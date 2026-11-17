# Complete native Value memo source package

This source package was assembled from branch `GH-0000-lmdb-lazy-result-coordinates` at checkout commit
`4e9af30d660dcc59b2e7148bb7cff00580441f29`, plus the explicitly hashed retention experiment sources in this package.
Its four production files are byte-identical to the final formatted `retention-followup/runtime-v2/src` files.

## Scope and prerequisites

This is a complete source overlay for this experiment, not a standalone RDF4J distribution. Use a full
RDF4J checkout at the commit above, JDK 25 (the study uses Temurin 25.0.5+8), Maven, the repository's
workspace `.m2_repo` dependencies, and a platform supported by the LMDB native library. Python 3.11+
is required for the standard-library runners. No dependency JARs, compiled classes, LMDB databases,
runtime classpaths, or comparative timing measurements are included. The separately supplied binary
prefix, if any, is not part of this source archive and is insufficient without its matched base classpath.

Extract into an empty directory for review. The archive root is `complete-source-v1/`. `MANIFEST.json` gives
the byte length and SHA-256 of every payload except itself; the external verification receipt also
hashes the manifest and the archive. Identical source contents at production and experiment paths
are intentional: the original runner paths are preserved. No files are hidden behind archive links.

## Production review and patch

The full four production files appear under `core/sail/lmdb/src/main/java/`:
`NativeResultMaterializer.java`, `NativeProjectedBindingSet.java`, `NativeDirectValueMemo.java`,
and `NativeDirectValueMemoStatistics.java`. The patch modifies the first two and adds the last two.
It contains exactly those four files. From an unmodified matching checkout, first run:

```bash
git apply --check /path/to/complete-native-value-memo.patch
```

Applying the patch is a separate user action. The archive build only performed the check and an
independent in-memory reconstruction; it did not modify production files or execute Java.

The direct memo remains experimental and disabled by default. Its enable property is
`rdf4j.lmdb.nativeValueMemo.enabled`; retention is selected with
`rdf4j.lmdb.nativeValueMemo.retention=strong|weak`. The strong reservation covers arrays. The weak
reservation also covers a declared conservative metadata estimate. Neither reservation is a
measurement of total process heap. The experiment preserves source/exposure ownership fences.

## Test identity

The original eight-case retention contract remains at its exact evidence-relative path:
`result-coordinate-evidence/retention-followup/contracts/src/org/eclipse/rdf4j/sail/lmdb/evaluation/NativeDirectValueMemoRetentionContractTest.java`. It matches the supplied `contract-seal-v1.json`.
`review/core/sail/lmdb/src/test/java/org/eclipse/rdf4j/sail/lmdb/evaluation/NativeDirectValueMemoRetentionContractTest.java` is the formatted production-review copy. It differs only in whitespace;
Java tokens, strings and comments compare identically. Use the original for replay of sealed evidence.
The historical 20-case memo table test and 12-case integration test are also included at their
original `direct-memo-followup/contracts/` paths. Other repository tests require the full checkout.

## Rebuilding and replaying the study

For study replay, retain the checkout's unmodified production sources while building the ordinary
baseline. Copy only the supplied experimental `result-coordinate-evidence/` sources into that
checkout initially; applying the production patch before baseline recovery changes the comparator.
Perform the repository-required root build, then use the supplied recovery assembler to create a
fresh matched baseline and preserved strong-direct overlay. The package includes all six common
harness sources, the root optional-fanout launcher, the five preserved strong-direct sources, and
their source-provenance inputs. The final weak-capable overlay uses the five new implementation/
benchmark source files; the production statistics file and its benchmark copy are identical.

The canonical runners are `recovery/prepare_runtime.py`, `contracts/run_contracts.py`,
`contracts/compile_runtime.py`, `contracts/qualify_runtime.py`, `diagnostics/run_memory.py`, and
the three scripts under `benchmark/`, relative to `retention-followup/`. The source-generation
helper, isolated formatter helper, evidence-preservation helper, and delivery builder are included.
Read `benchmark/README.md` and `contracts/TEST-DESIGN.md` for their intended sequence and invariants.

These are the original study runners, not a rewritten portable build system: some toolchain paths,
receipt paths and output-version defaults refer to the study workspace. On another machine, configure
a fresh replay copy for the actual checkout and toolchain, record those changes, and use fresh output
directories. Do not silently alter archived sources, seals or protocols, reuse old paths as evidence
of a new execution, or relax qualification gates. The archived pre-implementation red receipt is
provenance for the original eight tests with five expected failures; it is not verification of a new
checkout. Recreate appropriate red/green qualification evidence before claiming reproduction.
The supplied frozen comparison protocol records the prospective schedule and machine-specific
identities, not measured results; generate a new protocol after qualifying newly built runtime inputs.

The comparison uses normal AUTO execution, matched ownership and source caches, coordinate transport
off, fresh JVMs in balanced arm order, eight warmups and five measurements. Explicit collection is
confined to the separate memory diagnostic. Never run builds, tests, compression or diagnostics
alongside a timed JVM. Do not pool diagnostics, prior-host timings, or qualification-pilot timings
with the primary cohort. Compilation and archive verification do not establish a latency improvement.
