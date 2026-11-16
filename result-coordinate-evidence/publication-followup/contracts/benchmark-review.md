# Independent static review of the publication benchmark driver

Reviewed `benchmark/run-comparison.py` at SHA-256 `01a168541d872db4d31d923161adbe21105873a07c0c91e67c20e64375ba799f` and `benchmark/runtime_identity.py` at SHA-256 `dfc7ab42fade97a881bddef05c80a30464523fe883a781e6fc15f5a39e9c29af`. This was a source review, not another benchmark or test run. The experimental generator and six JUnit sources were left unchanged.

**Conclusion: no blocking defect found in the prospective design or its implementation.** The runner faithfully implements the stated publication counterfactual, subject to the coordinator obligations below.

## Design and ordering

The three fixed arms are OFF/all-publication, DENSE/all-publication, and DENSE/selected-omission. All use one experimental classpath. Only the transport and omission properties vary. The main per-query budget is 512 MiB in every arm, with a fixed 1 GiB G1 heap and 1 GiB global ledger. The original dense-page implementation and preserved common harness remain the control substrate.

All six permutations of the three arms occur once for each fixture. Consequently each arm occupies every within-fixture position twice, and each pair's relative ordering is balanced. The first fixture alternates by round. There are exactly 36 fresh JVMs, each with eight warmups and five measurements: 468 queries, 288 warmups and 180 measurements. There is no adaptive warmup selection, retry, replacement, exclusion, or extra-round path.

The primary estimate is a ratio of the medians of all five measurements in the same fixture and round. The summary preserves all six ratios and reports their median and full range. Fixtures are not pooled. The engineering screen is fixed in code and copied into the frozen protocol: each fixture needs median latency ratio at most 0.95, all six latency ratios below one, median allocation ratio at most one, and every allocation ratio at most 1.02. Passing is expressly not production promotion or a statistical significance claim.

## Runtime and timing boundaries

`validate_runtime` requires the experimental prefix followed by the exact preserved 128-entry common runtime. It checks the 15-file harness prefix, the original 127 entries, the original 2,780-file LMDB class tree, descriptor input hashes, and the classpath manifest. The timing prefix is restricted to materializer class names and cannot include the separate diagnostic helper. The same source identity and classpath are used by all three arms.

The driver does not edit Java timing windows or fixture data. The preserved harness classes measure query connection/preparation/evaluation/consumption/close using their existing clocks and allocation counters. Their identities are pinned. The selected budget and omission configuration are deliberate protocol changes; no hidden timing-window change was found. Cold-query interpretation remains after fixture construction and excludes JVM startup. Some descriptive metadata strings retain the older compact-study wording because the harness is preserved byte-for-byte; those strings do not imply pooled measurements.

Before launching the first timed JVM, the runner hashes and copies descriptor inputs. Between JVMs it checks only the protocol, driver, helper, and small materializer prefix. The full runtime and watched evidence are checked again after the serial sequence. No build, profile, compression, or full-runtime scan is launched inside a timed JVM interval.

## Per-query and cross-arm gates

Every run must contain one metadata record and exactly thirteen ordered query records, with iterations -8 through 4 and the prescribed warmup/measurement stages. Metadata checks the fixture dimensions, graph and query hashes, projection names, Java version, heap, ledger budgets, native evaluation and adjacency settings, complete JVM argument list, and exact experiment properties. Normalized metadata must agree across all arms of each fixture.

Each query must pass its expected row count and checksum, binding-read count, full-consumption result flags, nonnegative timing/allocation metrics, and zero before/after query-ledger endpoints. Shared-tail additionally requires the exact independent bag oracle. All queries must report native IR, normal AUTO selection, no forced mode, and positive adjacency hits against the unchanged active revision. Every measured query must report compiled native IR without interpreted or aggregate binds. The post-main audit reconciles thirteen native compiles and rejects a hosted generic root.

Coordinate-owner admission must be zero for OFF and one for DENSE, without coordinate-owner refusal. Factor-page admission is explicitly distinguished from that counter and delegated to the pinned diagnostics. The two DENSE arms must match resolver calls, route/tier signatures, and adjacency lookup hits for each same-round query iteration, including all eight warmups. This yields 156 matched query pairs. A measured route histogram also must equal the fixture's first-fork reference across all three arms.

Errors stop the protocol and preserve raw folders. An invalid fork may appear in the recorded run list because some target/pair checks happen after capture; the summary remains incomplete, does not produce accepted comparison estimates, and no replacement is launched. This is appropriate preservation rather than an exclusion.

## Process isolation and remaining coordinator obligations

The driver starts one Java child, waits for its termination, and only then launches the next. It records start/end timestamps and rejects overlapping intervals. Timeout or failure cleanup terminates the child's process group before proceeding. The supplied `--slot-released` option is an explicit coordinator acknowledgement, not an OS-wide lock: the runner does not enumerate or prevent another agent's build, JVM, profiler, or compression job. Root must establish the quiet interval and keep other workers out of it.

The selection receipt requires unrefused 512 MiB admissions and equal DENSE resolver counts, and all referenced diagnostic files are hash-pinned. The runner does not independently recompute those diagnostic assertions from page records, nor does it parse the pinned semantic-validation receipt to prove its suite passed. Root therefore must reconcile the diagnostic counters and inspect the fresh Surefire evidence before freezing. This is a bounded trust in verified upstream evidence, not a claim that the runner itself proves these conditions.

The Java executable path and reported Java version are fixed. The runner does not independently hash the JDK installation. The same preserved toolchain must remain unchanged for the study. No evidence of a toolchain change was found during this static review.

The final independent analysis should recompute estimates and gates directly from raw JSONL, check all process intervals, inspect all twelve DENSE pair audits, and confirm the post-run preservation receipt. These checks validate concrete experiment risks and should not add or replace timing samples.
