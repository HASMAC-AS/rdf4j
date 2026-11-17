# Memo admission/capacity: qualified mechanics, query timings pending

The two new constructor-captured options preserve defaults (`initial`, `adaptive`) and all existing weak ownership, key guards and ledger behavior. The actual eight-million-row compiled-query factorial is still pending; no query throughput improvement is claimed.

## Verification

The smallest new admission test failed before production changes: one test, one failure, expected continued admission but observed `low-repetition`. The initial green suite passed 24 tests. After adding phase and safety contracts, 27 focused tests passed, with zero failures/errors. Twelve policy×trace cases independently modelled every request and matched 96 phase checkpoints. Every arm also checks weak-reference clearing, semantic mutation, full-mask separation, initial/growth reservation refusal and release. Broader callbacks, cancellation, replay and retained-result suites under the nondefault policies remain pending.

## Synthetic phase probe

This is a synthetic initialized BNode shell-source probe, not SPARQL and not a dictionary-decoding benchmark. The consumer keeps returned Values alive, so weak-reference collection does not confound admission/collision behavior. Precreated lexical strings mean allocation and source cost are not representative of all real queries. Twenty-four fresh JVMs ran two reversed-order blocks, three traces, four policies, ten warmups and five measurements each: 360 total and 120 measured trials. All matched lexical checksums and released the tracked ledger. Raw logs and phase-summary.json are preserved.

Late cold-hot performs 32,768 unique requests followed by 98,304 requests across 64 repeating keys. Initial admission disables at 8,192 misses, with zero hits and 131,072 source shell creations. Always admission produces 98,240 hits and 32,832 source shell creations with either capacity policy. Adaptive ends at 8,192 slots and peaks at 884,864 charged bytes; fixed4096 ends at 4,096 and peaks at 294,976 bytes. Thus **the current policy differs from the published predecessor**, which grew to 65,536 on its own fixture. The fixed-capacity arm recovers the same reuse with one third the metadata peak here.

Late-hot total-loop median milliseconds for the two JVMs per arm:

- Initial/adaptive: 10.233, 3.163.
- Initial/fixed4096: 4.232, 9.208.
- Always/adaptive: 3.570, 3.386.
- Always/fixed4096: 2.882, 3.691.

These are five-trial within-JVM medians, not paired summary estimates. The large between-JVM variation, also visible in behavior-identical controls, prevents a convincing latency claim. Mechanism gains do not substitute for actual query latency/throughput.

All early-hot arms produce 126,912 hits and remain at 4,096 slots. All hot-unique arms produce 8,128 hits and remain at 4,096 slots. External calls are 131,072 in every trial; active memo probes stop after disabling. Source shell calls are not counts of physical dictionary decodes. Tracked metadata reservations are not total Java/native memory.

## Frozen candidate

Complete module output is in candidate-lmdb-classes. NativeValueMemo.class SHA256:

    9bb8a14f7383fa01ec9b6a33bb238ecd39514b5d1b32b0fd9cafdb754434bbc7

The parent owns query-run.sh, actual compiled factorial scheduling and selection of the same winning API snapshot configuration for every policy arm. No concurrent compilation or synthetic runs should overlap Workbench/query timing.
