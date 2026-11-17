# Segmented DISTINCT cancellation and lifecycle hardening

Description: add bounded cancellation/deadline polling to segmented bucket growth and collision chains; pass the owning query token through the bounded DISTINCT owner. No storage format, equivalence, or admission-limit change.

Evidence: `segmented-cancellation-natural-red.log` contains 2 failures against the original table. Tests use only natural `add` calls: cancellation is raised by the existing hash test seam after the entry check. The rehash case chooses an empty initial bucket, proving the new rehash poll, rather than a lookup poll, observes cancellation. An earlier reflective diagnostic was replaced and is not the final test design.

`segmented-cancellation-final-green.log`: 29 tests passed with wide DISTINCT, bulk ID I/O and miss-slot reuse enabled. Six new tests cover collision and rehash cancellation, real primitive-to-membership migration cancellation and I/O failure, canonical aliases through migration, and simultaneous spilled clear/reopen under a shared ledger. All claims/files release after failure and close. Spill failure injection is a package-private typed factory seam; no reflective state mutation is used.

Remaining gap: a deterministic shared-budget race where another owner steals the released workspace during `clear` and forces re-admission refusal is not separately injected. Existing tests cover initial refused admission and concurrent successful clear/reopen.

The complete 5045-test evaluation sweep is pre-this-patch evidence. Postpatch affected scope is 29 passing tests; a broader rerun is coordinated by the parent. Plan: implementation and focused validation complete; broader regression pending.

## Deterministic clear/re-admission race closure

A typed package-private `clear(Runnable afterWorkspaceReleased)` seam now permits an exact interleaving after the old spill workspace is closed, before replacement admission. Ordinary `clear()` passes null.

The new test uses a second executor and separate query ledger to reserve the released 4096 bytes. Replacement admission then genuinely fails under the shared 12000-byte cap. Before hardening, the failed owner retained its 1728-byte structural claim until explicitly closed and could be reopened by another clear. `wide-readmission-red.log` records two assertion failures (the reservation race and an injected transition callback exception).

Clear transition failures now close the owner immediately, preserve the primary exception, and permanently reject later add/clear calls. The competing tracker retains its exact prior membership and remains usable; all claims return to zero after close. This is failure-lifecycle hardening, not evidence of an incorrect result before the patch.

`wide-readmission-green.log`: **31 tests passed**, including both new cases. The earlier successful concurrent-clear fixture also needed deterministic initial setup: transient arenas can legitimately refuse a competing initial admission. Owners now spill before concurrent clear begins, and a barrier re-admits both workspaces before either may grow an arena. The initial fixture-race failure remains in `wide-readmission-green-initial-fixture-race.log`.

The prior deterministic re-admission gap is closed. Source and focused validation complete; full postpatch sweep pending separately.
