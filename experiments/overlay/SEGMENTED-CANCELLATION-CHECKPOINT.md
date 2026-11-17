# Segmented DISTINCT cancellation and lifecycle hardening

Description: add bounded cancellation/deadline polling to segmented bucket growth and collision chains; pass the owning query token through the bounded DISTINCT owner. No storage format, equivalence, or admission-limit change.

Evidence: `segmented-cancellation-natural-red.log` contains 2 failures against the original table. Tests use only natural `add` calls: cancellation is raised by the existing hash test seam after the entry check. The rehash case chooses an empty initial bucket, proving the new rehash poll, rather than a lookup poll, observes cancellation. An earlier reflective diagnostic was replaced and is not the final test design.

`segmented-cancellation-final-green.log`: 29 tests passed with wide DISTINCT, bulk ID I/O and miss-slot reuse enabled. Six new tests cover collision and rehash cancellation, real primitive-to-membership migration cancellation and I/O failure, canonical aliases through migration, and simultaneous spilled clear/reopen under a shared ledger. All claims/files release after failure and close. Spill failure injection is a package-private typed factory seam; no reflective state mutation is used.

Remaining gap: a deterministic shared-budget race where another owner steals the released workspace during `clear` and forces re-admission refusal is not separately injected. Existing tests cover initial refused admission and concurrent successful clear/reopen.

The complete 5045-test evaluation sweep is pre-this-patch evidence. Postpatch affected scope is 29 passing tests; a broader rerun is coordinated by the parent. Plan: implementation and focused validation complete; broader regression pending.
