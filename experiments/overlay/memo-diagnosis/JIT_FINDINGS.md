# Existing-profile JIT investigation

Read-only Routine C investigation; no production changes, benchmark runs, dependencies, or repository rebuild. Analysis helper and extracted records only. The frozen previous-best and entry-metadata artifacts remain untouched.

## Supported findings

Both recordings contain exactly two deoptimization events with NativeValueMemo on the stack, both in C2 code:

| Profile | Query-relative time | BCI | Reason/action |
|---|---:|---:|---|
| Previous best | 1.786s | 372 | unstable_if / reinterpret at SAMPLE branch |
| Previous best | 3.862s | 87 | null_check / make_not_entrant at cast after WeakReference.get |
| Entry metadata | 1.270s | 482 | unstable_if / reinterpret at SAMPLE branch |
| Entry metadata | 2.583s | 90 | null_check / make_not_entrant at cast after WeakReference.get |

The second event is consistent with a compiled null speculation being invalidated. It does not by itself distinguish an empty slot from a cleared reference, and is not a correctness failure.

The dominant sample attribution persists in every ten-second query bucket, well after these early events. Previous-best BCI 87 occurs from 0–10s through 70–80s; candidate BCI 306 (source.lazyValue miss call) likewise persists throughout. Apart from one previous-best interpreted memo leaf, these memo leaves are marked JIT compiled. Immediate collectMaterializationValues caller frames are marked Inlined for 567/570 previous-best and 532/533 candidate memo leaves. Thus there is no evidence here of a sustained interpreter fallback or recurring memo deoptimization storm explaining the whole difference.

Whole-recording compilation-failure events are seven versus eight: all except one per recording say retry without subsuming loads; one per recording says concurrent class loading. No event names the memo, and there are no CodeCacheFull events. These records do not establish permanent failed compilation or a code-cache bottleneck.

## Evidence gap

Profile settings record jdk.Compilation only above 100ms. There are 32 versus 40 such compilation events, none for NativeValueMemo. jdk.CompilerInlining is disabled and has zero events. Consequently these recordings do not show the memo's final compilation level history, compiled size, inline-tree decisions, whether WeakReference.get was intrinsified, or whether source.lazyValue dispatch/inlining differs. Bytecode/source attribution alone cannot answer those questions or convert the approximately 9% memo sample share into removable wall time.

## Minimal next diagnostic

Use the **unchanged frozen artifacts**, separately from miss-counter instrumentation, for diagnostic-only old-best and candidate runs. Exclude these runs from latency comparisons.

Enable jdk.Compilation with threshold=0ms, retaining jdk.Deoptimization. For exact inline decisions, either enable jdk.CompilerInlining in a separate JFC or use targeted PrintInlining and PrintCompilation options for NativeValueMemo.value and NativeProjectedBindingSet.collectMaterializationValues. The installed Java 25 CompileCommand help confirms these options are supported; no force-inline/dontinline/compiler-threshold change is required. If targeted logging misses a memo inlined into another root compilation, use LogCompilation XML and join compile IDs to the JFR/deoptimization records.

Suggested option patterns:

```
-XX:+UnlockDiagnosticVMOptions
-XX:CompileCommand=PrintInlining,org/eclipse/rdf4j/sail/lmdb/evaluation/NativeValueMemo.value
-XX:CompileCommand=PrintCompilation,org/eclipse/rdf4j/sail/lmdb/evaluation/NativeValueMemo.value
-XX:CompileCommand=PrintInlining,org/eclipse/rdf4j/sail/lmdb/evaluation/NativeProjectedBindingSet.collectMaterializationValues
```

Ask whether the weak-reference get is intrinsified, how the source type is profiled, whether lazyValue is inlined or remains virtual, and whether the changed bytecode size changes hot-call inlining decisions. Combine that evidence with untimed exact miss-cause counts. Neither the hit ratio nor the current line histogram alone justifies a hit-path or miss-path optimization.

## Local evidence

- jit-summary.json: compact compiler/deoptimization observations.
- transport-profile-on-memo-timeline.txt and memo-entry-profile-on-memo-timeline.txt: ten-second sample buckets and caller frame types.
- JitTimeline.java: JFR reader for the timeline.
- summarize-jit.py: reduction of jfr print --json output.
- Raw *-jit-events.json remain local; no need to publish their full stacks.

No Surefire test was run or claimed for this read-only analysis. Parent qualification receipts remain the correctness evidence for the already-tested artifacts.
