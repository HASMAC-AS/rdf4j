# Prespecified SOURCE_PULL × writer-owner four-arm experiment

Supersedes the three-arm-route A/B execution plan for the next experiment. This protocol was fixed before measurement. All four combinations passed the relevant behavioral qualification before timing; all 48 timing forks and 12 separate profiles are now complete. The protocol and raw samples were not reduced after seeing outcomes.

## Arms and immutable runtime construction

- **A baseline:** `baseline-classes/`, previously measured c02 production semantics, with unchanged dependencies.
- **B SOURCE_PULL only:** a complete `RepositoryValueIteration` outer/inner-class prefix layer before A.
- **C writer guard only:** a complete `ValueStore` outer/inner-class prefix layer before A.
- **D combined:** both independently verified prefix layers before A.

Do not take all classes from a changing target directory. Freeze each prefix only after its tests pass, and compare prefix inventories with A to detect added/deleted inner classes (if deletions exist, use a full filtered runtime copy so old baseline classes cannot leak through). The writer change must be the tested zero-writeTxn early return before the ORIGINAL borrowed-reader lookup and SECOND writeTxn read/nonzero predicate; no reordering that weakens race handling. Do not include other speculative optimizations.

A's code provenance remains the previous immutable measured runtime, not local HEAD. Record exact published source SHA for each independent patch, source file SHA, compiled outer/inner SHA and complete per-arm ordered classpath. For D record both patch identities; do not invent a combined commit. Print class origins for both changed classes. Snapshot harness classes and flags once; use identical dependency JAR bytes, JVM, heap, fixture and consumer in all arms.

## Fixed factorial protocol

Routes: regular, native interpreted (`irKernelInterpreted`), Janino (`irKernel`). All three are affected routes, not placebo controls. Same 100-group/fanout-10 fixture, 26,700 triples, 100,000 distinct six-slot results. Same dual full-RDF-term checksums, route/factory/bind assertions, zero spill and returned reservations. Generic collection sync threshold zero equally; native 640 MiB cap; all inherited measured-combination flags pinned.

Each route uses **16 fresh JVM forks**, four independent forks per arm, each **20 warmups and 10 measurements**. Fixed balanced order in four blocks:

1. A B D C
2. B C A D
3. C D B A
4. D A C B

Run the complete 48-fork series without builds/profilers/copyright scans sharing CPU. Do not reduce samples after seeing outcomes. Raw data retains all forks. Check measured-index convergence and within/between-fork spread. If a technical failure occurs, retain its log, diagnose, and explicitly record any restarted protocol. Full-query time includes connection open/prepare/evaluate/materialized consumption/close. First row includes eager evaluation and full projected-value consumption.

## Analysis prespecified before results

Compute each fork mean first. For each route report arm mean of four fork means, independent-fork range, within-fork range, first-row means, allocation bytes/query, checksum/route/spill status and measured-index means. Do not treat all 40 measurements as independent forks.

Report B/A, C/A and D/A latency reductions and reciprocal throughput changes, plus D/B and D/C to establish whether combining helps beyond either change. Match arms within each four-fork block for paired comparisons. Report all four block effects and their spread, not only the pooled percentage. For interaction, use log latency contrast **log(D) − log(B) − log(C) + log(A)** per block; negative means the combined latency is below the multiplicative independent prediction. Equivalently D×A/(B×C) < 1 suggests positive synergy in this latency convention. Do not claim synergy if uncertainty/noise covers zero or the pattern is not consistent across blocks. Lack of an individual win does not rule out a combined win; this protocol measures it rather than assuming additive benefits.

No comparison is against upstream main, no HTTP/Workbench result, and no concurrent QPS or request-tail-latency claim. Sampled ThreadLocal shares do not predict the attainable gain.

## Profiles

Only after clean timing, separate warmed JFR forks for all arms/routes or an explicitly labeled targeted subset agreed with the owner. Record SOURCE_PULL binding/restoration, ValueStore write-owner lookup, other read/replay context scopes and value/IR costs. Use adequate recorded duration (native at least 30 queries; regular at least 5) after 20 warmups. Class/source identities remain the same. Profiles never replace clean latency evidence.

## GC observability in clean timing

Every clean JVM uses identical low-volume `-Xlog:gc:file=<fork>.gc.log:time,uptime,level,tags`. No JFR or PrintCompilation in clean measurements. Retain all samples regardless of GC, and use logs only to explain observable events rather than filter inconvenient runs. Exact per-fork command and common flags are recorded.

## Active grouped-wave configuration

`repository.partialReady=true` is active. Grouped value waves are unset/default false, as in the preceding measured combination; do not change mid-series. The five legacy tests that fail equally under the extra grouped-wave qualification are not running under this benchmark configuration. The inherited flags are a measured combination baseline, not a universally best setting.

## Profile attribution across refactoring

SOURCE_PULL's old `sourceHasNext`/`sourceNext` helper names disappear after consolidation, but its binding remains in `prepareNext`. Aggregate all RepositoryValueIteration-related visible ThreadLocal callers, including restore and inlined/truncated stacks; disappearance of a helper is not proof of zero work. The source-level 2 get/4 set → 1 get/2 set successful-row count is static reasoning, not measured machine instructions. The focused writer test verifies no readContext.get when writeTxn is zero; other ValueStore lookups remain and JFR relative denominators cannot measure wall-time saved.
