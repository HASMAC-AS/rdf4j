# Independent resource-holder review

Disposition: approve for continued correctness qualification and matched measurement; no blocking source/bytecode issue identified. Not a performance recommendation or full-repository pass.

Reviewed candidate source SHA256: f08b5729fc034e7221bdb8618d330a819a4bd32b67e40bf3b457907418d04639.

- Independently compared all104 baseline-compiled store-family files with record-cancellation/baseline-classes incumbent: all byte-identical. Semantic patch adds static holder and changes only four head lookup sites / corresponding map mutations. No archived cancellation code introduced by the isolated source.
- Bytecode evidence independent-holder.javap.txt: static ReadResourceState has only current, constructor invokes Object constructor and captures no outer owner. Nonstatic context retains its existing outer store, as expected.
- close returns immediately when already closed; otherwise gets closing-thread state, verifies identity, throws before closed mutation on mismatch, then marks closed and restores previous field. No captured activating holder or foreign-thread store is introduced.
- Enter preserves old-head lookup, null supplier validation and complete frame construction before install. Dynamic supplier evaluation, parent union and lease identity dedup remain unchanged. Estimator still distinguishes empty-but-present frame from no frame.
- Eight focused tests cover structural map mutation removal; nested dynamic/empty/duplicate union; wrong-order rejection/recovery; foreign-thread rejection/owner cleanup/closed-token no-op; null and throwing suppliers; empty-frame estimator deferral; separate stores; and weak store/supplier reachability with live worker and strongly held empty holders.

Limits: tests demonstrate behavioral store/thread isolation rather than an exact measured holder-count/retained-byte census. Per-store/thread holders and entries remain expected S*T-scaled overhead. Weak collection while keeping holder strong supports absence of owner backreference, not zero stale map capacity. No concurrent same-token-close guarantee added. Broader integration is owned by implementing worker and was still running at review time. This review ran read-only disassembly/hash checks only, no JVM behavioral tests or builds.
