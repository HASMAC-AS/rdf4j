# Complete growth experiment source overlay

Prepared from branch GH-0000-lmdb-lazy-result-coordinates, immutable module patch base b8d2015ec1b722b71f9a8af9e50cd98558f7882a, and
checkout b8d2015ec1b722b71f9a8af9e50cd98558f7882a at packaging time. The frozen timing protocol SHA-256 is
60d231209b9d3a52e88d3ccc262422a90d342d7daacfe13383ec1a93ad209fe9; the final runtime qualification SHA-256 is de7cdee686e3bfe8875829e1ea69d04e316f1bf6638a4bba2508da7abc9b17b9.
The package is assembled before any process in the selected timing cohort starts.

## Contents and external dependencies

The package contains the four complete feature files, all canonical growth Java and
Python sources, the selected protocol and qualification provenance, focused contracts,
the primary and independent auditors, preservation helpers, two exact patches, and
the predecessor source dependencies needed to rebuild the experimental prefixes.
Historical sources and provenance are included from the verified admission source
archive; its timing observations do not become growth measurements.

This is a source overlay for a full RDF4J checkout at the patch base above. The
checkout supplies unrelated production sources, module POMs, existing ownership/IR
tests and build settings. Required external inputs are JDK 25 (the study uses
Temurin 25.0.5+8-LTS), Maven with workspace .m2_repo dependencies, Python 3.11+,
Git, and the platform's LMDB native dependencies. Dependency JARs, compiled
classes, databases, toolchain binaries and primary timing observations are
separate inputs and are not embedded here. Hash manifests identify those inputs;
they do not supply their bytes.

Extract into an empty review directory. The archive root is growth-complete-source-v1/.
Every regular payload except MANIFEST.json has a byte length, SHA-256 and
origin in that manifest. The external verification receipt hashes the archive
and manifest. The archive contains no links, device members, absolute paths or
parent traversal members. Source roots are explicit, so generated runtimes,
launch snapshots and extraction copies cannot recursively become new source inputs.

## Complete feature and review patches

The four complete production-review files appear under
core/sail/lmdb/src/main/java/org/eclipse/rdf4j/sail/lmdb/evaluation/:
NativeResultMaterializer.java, NativeProjectedBindingSet.java,
NativeDirectValueMemo.java and NativeDirectValueMemoStatistics.java.
Each is byte-identical to its canonical growth-followup/implementation-src source.

complete-native-value-memo.patch contains exactly those four changed or added
files relative to b8d2015ec1b722b71f9a8af9e50cd98558f7882a. From an unmodified matching checkout:

    git apply --check /path/to/complete-native-value-memo.patch

The packager verifies strict hunk offsets, context, additions and deletions using
an independent reader. It also performs git apply --check and actual application
in a disposable directory, then compares all reconstructed files with their complete
canonical counterparts. The repository's module sources are never changed by packaging.

growth-only-versus-admission.patch is the one-file review diff from the preceding
admission memo to the current growth memo. Its base is the canonical admission
NativeDirectValueMemo.java included at its original evidence-relative path.
Apply that small patch only to this memo base. The full feature patch is the
appropriate diff for the matching production checkout.

The feature stays off by default through rdf4j.lmdb.nativeValueMemo.enabled.
Retention remains strong or weak; admission remains initial or always, default
initial. The new rdf4j.lmdb.nativeValueMemo.growth selection is adaptive or fixed,
default adaptive. Fixed keeps the initially admitted 4,096-slot table and bypasses
only subsequent resizing. It does not replace reservation refusal, source identity
validation, semantic version checks, admission or close behavior.

Both enabled timing arms use weak retention and always admission. The strong
diagnostic variant still does not charge its retained payload graph. Weak-mode
accounting covers estimated arrays and table-owned reference metadata, including
resize overlap; it is not a complete live-heap or native-memory bound.

## Reproduction sequence

Start with a matching full checkout and read AGENTS.md and .agent/PLANS.md. Preserve
the original module sources while rebuilding the ordinary baseline. The experiments
compile their implementation as classpath prefixes before frozen dependency bytes.
Applying the production-review patch before building that baseline changes the comparator.

1. Restore or configure the pinned toolchain and dependencies and perform the required
   root build. The original growth workspace compiled and installed LMDB but its
   full reactor stopped on an unrelated Spring/Kotlin dependency failure. The package
   makes no claim that the whole reactor was green. Read growth recovery receipts.
2. Rebuild the preserved baseline and common helpers using the supplied predecessor
   recovery sources and their inventory. Rebuild the retention and admission prefixes
   from their complete sources and compile helpers. Historical seals are provenance;
   a changed checkout or toolchain requires new identity records.
3. Read the sealed twelve-case growth contract and retained pre-implementation failures.
   Use growth-followup/contracts/compile_runtime.py with a fresh output directory,
   and run_contracts.py / qualify_runtime.py with explicit runtime, source, selection
   and new attempt paths. Both native IR lifetime tiers and fixed/adaptive ownership
   cases must pass. The final original qualification records 265 passing
   executions with matching memo bytes; it does not prove a newly rebuilt replay.
4. Compile the complete growth fixture and harness through growth-followup/compile_support.py,
   providing the qualified memo classpath and explicit source files. The independent
   fixture oracle defines early-cold-hot, late-cold-hot and warm-cycle-8192. The last
   fixture begins with a small hot bootstrap and then cycles over a larger working set.
5. Run all nine fixture/arm combinations through benchmark/run_pilot.py with 8
   warmups and 5 exact queries. The supplied original final qualification
   records 117 exact query executions. Recreate that evidence for a new
   runtime; do not copy a qualifiedForTiming flag or weaken a result/ownership gate.
6. Use benchmark/qualify_runtime.py with the nine complete pilots, all final passing
   contract groups, final harness classpath and preserved baseline classes. It writes
   new fixture definitions and runtime qualification. Freeze a new protocol with
   benchmark/run_comparison.py freeze and explicit source inputs before any timing.
   Read benchmark/PROTOCOL.md for the exact schedule, estimators and gates.
7. Run one pending timing slot at a time using next --cohort NEW_COHORT --slot-released.
   After each completed JVM, check_route_profile.py must accept its full completed
   prefix. The selected protocol schedules 54 fresh JVMs, each with 8
   warmups and 5 measurements. Preserve failed/partial attempts; never
   replace samples or splice them into another cohort.
8. When every slot has completed, run the full route-profile check, the primary
   audit action and reviews/audit_growth_cohort.py. Keep diagnostics, exact pilots,
   historical timing and source-package verification outside the measured dataset.

These are the actual study scripts, not a portable dependency installer. Several
paths and output-version defaults refer to the original workspace. Some builders
require historical sealed receipts or qualified compiled prefixes. Configure a new
replay copy for a relocated checkout, preserve original evidence, document path-only
changes, rebuild the full classpath and produce new qualification records. Do not
make an unrelated runtime appear identical by editing old seals.

## Measurement and interpretation scope

The comparison keeps weak ownership, always admission, source caches, the consumer,
memory budgets and normal AUTO strategy selection identical. Coordinate transport
is off. The heap is fixed at 1 GiB with G1; process/query accounting limits remain
1 GiB / 512 MiB. The ordered query guarantees access-phase order and uses an
unprojected row key. Those ordered wrappers may retain consumed Values until close.
This workload does not establish weak collection behavior for otherwise unowned
streaming results or oversized uncached values.

The complete per-query AUTO profile is qualified across all 8+5
queries. The measured phase includes normal alternative-source learning, not a
claim of fully converged source selection. Equal late-case reuse with fixed capacity
is a hypothesis, not an admission gate. The larger working set is an explicit
control for useful reuse lost by suppressing growth.

Whole-query elapsed time remains primary. The harness also records bounded caller
CPU and allocation snapshots at query start, prefix completion, consumption
completion and final query completion. The tail can include automatic exhaustion
cleanup; explicit closes occur afterwards. Caller CPU excludes GC/JIT/other threads.
The older all-thread allocation endpoint and new aligned caller endpoint differ,
so subtracting them is not a pure estimate of worker allocation.

Per-JVM medians reduce nested query measurements before same-round ratios are
formed. The prescribed mean sensitivity uses the same observations. Keep all
processes, allocation clusters and direction reversals visible. A smaller metadata
reservation or fewer source lazy calls does not by itself establish lower latency,
less physical decoding or lower total heap use.

Do not run packaging, preservation, broad audit, compilation or diagnostic work
alongside a timed JVM. Final results are published separately after the complete
finite cohort and independent numerical review.
