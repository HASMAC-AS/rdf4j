# Memo-policy experiment status

Primary decision metrics are query latency and throughput. Allocation and memo hit counts are diagnostics, not success criteria.

`NativeMemoPolicyTest` was written first. The smallest continued-admission contract failed as expected (1 test, 1 failure), then the initial 8 policy contracts and 16 existing memo tests passed after the minimal policy gates. The expanded suite passed 27 tests (11 policy, 16 existing), including 12 arm×phase exact-model traces. Immutable snapshot diagnostics and the standalone phase probe compiled successfully. Production defaults remain `initial` admission and `adaptive` capacity; alternative options are `always` and `fixed4096` respectively.

`MemoPhaseProbe.java` is a standalone synthetic shell-source mechanism probe, **not a SPARQL benchmark and not a physical-decode benchmark**. Its source constructs initialized LmdbBNode shells using precreated lexical labels. The consumer retains returned Values, computes the complete lexical checksum and measures prefix/tail/full-loop durations. It prints external calls separately from active memo probes (the latter stop after disable), source shell creations, capacity and tracked metadata peak. Source calls are not physical decodes. It performs 10 warmup trials then 5 measured trials per fresh JVM. Use fresh processes in balanced policy order, with the same heap/JDK and immutable classes. Two forward/reverse rounds completed: 24 JVMs, 360 total trials, 120 measured trials. All outputs and tracked ledger endpoints validated; timings show substantial between-JVM variation and do not establish a speedup.

The actual compiled 2×2 factorial is scheduled by the parent after Workbench and the snapshot hash comparison, using the winning API snapshot configuration. Do not infer its result from the synthetic probe.

Executed compile command after machine clearance, from repository root:

    mkdir -p experiments/overlay/memo-policy/harness
    experiments/overlay/tools/jdk-25.0.5+8/bin/javac -cp "core/sail/lmdb/target/classes:$(cat experiments/overlay/lifetime/classpath.txt)" -d experiments/overlay/memo-policy/harness experiments/overlay/memo-policy/MemoPhaseProbe.java

Pending example run (must hold shared benchmark lock, use frozen candidate classes and reserve machine slot):

    java -Xms1g -Xmx1g -Drdf4j.lmdb.nativeValueMemo.admission=always -Drdf4j.lmdb.nativeValueMemo.capacity=fixed4096 -cp "experiments/overlay/memo-policy/harness:experiments/overlay/memo-policy/candidate-lmdb-classes:$(cat experiments/overlay/lifetime/classpath.txt)" org.eclipse.rdf4j.sail.lmdb.evaluation.MemoPhaseProbe late-hot

## Prepared follow-up, not executed

`run-lifecycle.sh` prepares all three nondefault policy arms against callback, cancellation, retained-result, replay and full map-growth suites. It enables the existing gated memo-hit probe, verifies target memo bytecode matches the frozen candidate and preserves per-arm evidence. This script has not run yet.

`CompiledLateHotProbe.java` is source-only preparation for a real repository-query qualification, not a measured result. It seeds/reopens a marked store with 32,768 unique cold Values followed by 131,072 occurrences of 64 hot Values. It forces compiled IR and rejects any observed stream that fails the cold-prefix/hot-tail domain, uniqueness and multiplicity assertions. It also requires exactly one compiled root, cutoff counters for initial admission, and continued hits plus one observed memo identity for always admission during gated qualification. No UNION or ORDER BY is used. An optional hidden guard join uses a separately stamped fixture if the simple projection proves ineligible. The harness has not been compiled or run; route, emitted order, single-owner behavior and recovered reuse are hypotheses pending qualification. No new production hooks are added.
