# Recovery investigation

Original first baseline fork interrupted after 42 warmups, zero measured requests; preserved in linear-narrow-workbench. Separate series restarts all fixed arms on recovered executor, without pooling historic data. No production changes or rebuilds: frozen runtime is the measurement subject; rebuilding dirty source would invalidate it.

Plan
- [done] recover artifacts and inspect interruption
- [done] verify frozen runtime and qualify
- [done] run complete fixed matched series
- [done] profile separately and summarize evidence

Copied qualification-summary.json, runtime-reverified.json and INDEPENDENT-REVIEW.md document the original pre-interruption setup. Fresh recovery checks are recovery-integrity.json, recovery-qualification.json and runs/requal-*. Both sets are preserved, not silently substituted.
