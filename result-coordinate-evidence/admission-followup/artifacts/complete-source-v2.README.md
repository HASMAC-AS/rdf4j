# Complete admission experiment source overlay

Prepared from branch `GH-0000-lmdb-lazy-result-coordinates`, immutable module patch base `47e6eba9bd1b238ea84c1b9ad4cd8cdf28caebaf`
(checkout at packaging start `4a5f982b2ced59966af63084abf2a353ce994650`),
and the explicit canonical experiment sources recorded in `MANIFEST.json`.
The prospective timing protocol SHA-256 is `239c97fd23424922cde7b91ad507bf3dad4cdc0629f411fef63078fa6266149c`.
No comparison-v2 timing process had started when this archive was assembled.

**The original comparison-v1 is rejected.** Its first JVM completed thirteen valid queries, but
its frozen 98,309 adjacency-hit floor failed when the existing normal AUTO selector began
learning its LMDB root-scan alternative after eight preferred-source completions. No v1
performance observation is accepted or pooled. The historical v1 archive, protocol, raw records
and qualification remain unchanged. This v2 package adds the source-derived 98,308 floor,
nine full-sequence exact pilots, and a frozen per-query route-profile checker for a fresh cohort.

## Contents and dependency boundary

This package contains complete Java/Python source for the current four-file native Value memo
feature, ordered phase-query fixture and harness, sealed admission contracts, deterministic trace
and natural-GC diagnostics, compile/test/qualification/running scripts, independent oracles and
auditors, and the required source helpers from the verified preceding retention package. It also
includes the source dependency inventory, test/qualification provenance, prospective protocol,
two patches, and this reproduction guide. Original evidence-relative paths are preserved.

This is a source overlay for a full RDF4J checkout, not an installed or self-contained RDF4J,
JDK, Maven, or LMDB runtime. Required external inputs are the checkout above; JDK 25 (this study
uses Temurin 25.0.5+8); Maven and the repository's workspace `.m2_repo` dependencies; Python 3.11+
for the standard-library scripts; Git for patch verification; and the platform's LMDB native
library dependencies. Dependency JARs, compiled classes, toolchain binaries, databases, and the
primary timing observations are not included. Hash manifests identify those external runtime
inputs; they do not supply their bytes. The checkout supplies unrelated production code and its
existing IR/lifetime tests, module POMs, build settings and dependency resolution.

Extract into an empty review directory. The archive root is `admission-complete-source-v2/`. Every regular payload
except `MANIFEST.json` has a byte length, SHA-256 and source origin in that manifest. The external
verification receipt hashes the manifest and archive. There are no symlinks, device members,
absolute archive paths or path traversal members. The explicit source set cannot recurse into
the generated archive or readback directory.

## Complete code and exact patches

The four full production-review files are under `core/sail/lmdb/src/main/java/`:
`NativeResultMaterializer.java`, `NativeProjectedBindingSet.java`, `NativeDirectValueMemo.java`,
and `NativeDirectValueMemoStatistics.java`. Each is byte-identical to its current canonical
`admission-followup/implementation-src` counterpart. The statistics source has the corrected
repository copyright URL; historical frozen copies remain untouched as historical sources.

`complete-native-value-memo.patch` contains exactly these four changed/added files relative to
the checkout HEAD above. Verify it from an unmodified matching checkout with:

```bash
git apply --check /path/to/complete-native-value-memo.patch
```

The packaging procedure checked and applied the patch only in a disposable directory outside
the repository, then compared all four reconstructed files byte-for-byte. An independent strict
unified-hunk reader checked exact offsets, context, additions and deletions as well.

`admission-only-versus-retention.patch` is the optional one-file review diff from the preserved
retention memo to the current canonical memo. Its only algorithm change adds `initial|always`
admission selection and bypasses the one-time low-hit cutoff in `always` mode. It also retains
the current header/comment corrections. Apply it only to the retained memo base identified by
the manifest; it is not a substitute for the complete feature patch on the checkout HEAD.

The feature stays off by default through `rdf4j.lmdb.nativeValueMemo.enabled`. Retention is
`rdf4j.lmdb.nativeValueMemo.retention=strong|weak`; admission is
`rdf4j.lmdb.nativeValueMemo.admission=initial|always`, default `initial`. `always` is a diagnostic
ablation, not a selected production policy. Weak metadata accounting covers arrays/reference
metadata and resize overlap; it is not an estimate of total retained heap.

## Reproduction sequence

Start with a matching full checkout and read `AGENTS.md` and `PLANS.md`. Preserve its original
module sources while building the ordinary baseline: applying the production-review patch
before baseline recovery changes that comparator. The experimental sources belong under
`result-coordinate-evidence/` and are compiled as isolated classpath overlays.

1. Restore/configure the JDK and Maven, then perform the repository-required root build.
   The original study's root build had an unrelated Spring/Kotlin dependency failure; the source
   package does not claim a successful full reactor build. Read the recovery notes and receipts.
2. Rebuild the matched preserved baseline and common harness helpers using the supplied
   `retention-followup/recovery/prepare_runtime.py` and source provenance. Rebuild the retention
   overlay with its supplied source and compile helper. Those prior helper imports are included.
3. Inspect the sealed twelve-case admission contract and original red receipts, then use
   `admission-followup/contracts/compile_runtime.py` and `run_contracts.py`/`qualify_runtime.py`
   with fresh runtime/attempt paths. The source-generation helper is historical TDD provenance;
   do not rerun it over an existing canonical implementation directory. The current four-file
   canonical source is the delivery target. Both existing IR lifetime tiers require qualification.
4. Compile the phase harness and each diagnostic with `admission-followup/compile_support.py`,
   an explicit qualified classpath, explicit source files and a fresh output directory. The
   ordered harness uses `PhaseMemoQueryFixture.java` plus `PhaseMemoQueryHarness.java`.
   Diagnostics and their statistics support must use the matching qualified memo overlay.
5. Run exact qualification through `benchmark/run_pilot.py`. The three fixture definitions
   contain all dimensions explicitly. The v2 final receipt records nine clean pilot JVMs,
   117 exact queries (eight warmups plus five exact queries per JVM) and 225 earlier ownership/test executions with identical memo class bytes;
   it is evidence for this workspace, not proof of a newly built replay.
6. For a new runtime, create a new qualification receipt and protocol using the seven required
   bindings described by `benchmark/run_comparison.py freeze --help`. Read `benchmark/PROTOCOL-v2.md`
   and `FIXTURE.md`. Freeze before timing; execute exactly one pending process per `next` after
   the coordinator grants the exclusive Java slot. Preserve every failed/partial attempt.
   Freeze `reviews/check_qualified_route_profile_v2.py` explicitly as a source input. After each
   sealed JVM, run it with `--cohort NEW_COHORT --output FRESH_JSON --allow-incomplete`; any mismatch
   stops the new cohort. It reads only route/adjacency outcomes and never computes timing metrics.
7. Once all 54 new JVMs finish, rerun the route-profile checker without `--allow-incomplete`,
   then run the primary analyzer and
   `reviews/audit_admission_cohort.py --cohort ... --primary ... --output NEW_JSON`.
   Do not mix the archived exact pilots, historical timing or diagnostic observations into it.

These are the actual study scripts, not a newly portable build system. Toolchain paths,
qualification file paths and output-version defaults refer to the original workspace. Some
scripts deliberately require historical seals or preexisting qualified prefixes. Configure a
fresh replay copy for a new checkout/toolchain, document path-only adjustments, rebuild all
required classpath dependencies, and generate new receipts. Never edit preserved seals or
weaken gates to make an unrelated runtime appear identical. The receipt builder under `reviews/`
specifically rechecks original full-sequence `*-v3` pilot paths and root/base equality; a new replay needs its
own explicitly identified evidence rather than copying its `qualifiedForTiming` flag.

## Interpretation and timing isolation

The principal comparison uses normal AUTO ordered-factorized execution, coordinate transport off,
identical source caches/ownership, one fixed 1 GiB G1 heap, 512 MiB query and 1 GiB global accounting
limits, eight warmups and five measurements per fresh JVM. Six balanced process orders yield
six paired process observations per fixture/arm; queries within a JVM are nested measurements.
The median-based estimator and arithmetic-mean sensitivity are both prescribed before timing.
The last five queries are matched normal AUTO alternative-source learning observations, not a
converged post-learning steady state. The required per-query profile is 98,309 adjacency hits
for queries 1–8 and 98,308 for queries 9–13; the source-derived floor is
`(width - 1) * fullRows + width = 98,308`, supported by three bound tail probes per row and four
compiler exact-predicate counts. Normal top-level `orderedFactorizedSort` remains selected.

The hidden ordered key does not add projected memo probes. Ordered binding sets can retain consumed
Values until close, so this cohort tests admission/timing and does not establish weak collection
of useful keys. The separate natural-GC driver uses a real reopened ValueStore, unchanged source
caches, explicit consumer ownership and allocation pressure, with no explicit collection call.
Its source-call/cold-to-initialized observations are not physical decoder counters.

Never run compression, build, test, diagnostic, audit or other experimental work alongside a
timed JVM. Neither compilation, allocation savings nor archive verification proves a latency
improvement. Read the final separately published results after the complete finite cohort exists.
