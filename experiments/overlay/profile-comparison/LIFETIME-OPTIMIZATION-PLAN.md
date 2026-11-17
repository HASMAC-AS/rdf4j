# Foreground activation specialization

Description: isolate redundant worker-reader bookkeeping from synchronous foreground activation, without changing lifetime, callbacks, reader retirement, or returned-value semantics. No issue number supplied (GH-0000).

Plan
- [done] restore build and capture baseline evidence
- [done] characterize nesting and closed-exposure restoration
- [done] specialize foreground activation without semantic changes
- [done] rerun matching tests and adversarial suites
- [done] compare matched timings allocations and profiles
- [in_progress] preserve local checkpoint; await publication authorization

This is a Routine B behavior-neutral micro-performance refactor: `activateForeground` currently invokes `activate(false)`; Thread/readers/generation snapshots are used only in the disabled worker-release branch. The replacement must still restore the exact prior ACTIVE entry with set, including closed scopes. Worker activation remains unchanged. Pre/post green from the same selection is required before claiming correctness. Existing direct hits include RepositoryValueIterationTest (lines 418,481,500) and RepositoryPartialReadyTest; additional characterization will exercise cross-store nesting, same-scope nesting, closed scopes, and retirement inside foreground activation.

Recovery: checkout restored from remote 4724600b10af6b044cc28154309352416d0660cf. Previous fixture/classes/dependency cache were absent. JDK25.0.5+8 and Maven3.9.11 downloaded from official sources. Initial offline build failed on absent dependencies; direct online Maven failed DNS; existing HTTPS proxy configured in ignored local settings. No production edits so far.

Measurement: freeze combined baseline target/classes before patch. Compare identical 30-column compiled query, all previous best flags on, no writer, 5GiB heap, 640MiB per-query/2.5GiB shared budget, deterministic fixture and output checksum, alternating baseline/candidate/candidate/baseline forks. A small activation microbenchmark may explain mechanism but is not a query speedup. Record CPU sample shares separately from wall-time improvements. Keep raw JFR local. Public results must specify fresh environment and no direct comparability to old wall times.

Pre-change evidence: 17 tests, zero failures/errors/skips (4 classes), 2026-11-17. Direct activation calls in new ValueResolutionScopeActivationTest provide Hit Proof. Saved pre-reports and initial-evidence.txt. Root quick clean install passed 125 modules; tests were not run in that build.

Foreground first ABBA complete: see lifetime/RESULTS.md; no convincing query speedup. Next Routine B isolated change is SOURCE_PULL.set(previous), including null. Pre-green hit proof: 25 LmdbRepositoryValueExposureTest tests (including exactRepositorySourceReleasesWithoutLegacyColdOwnership and nestedRawCallbackKeepsItsIndependentColdOwnership), 11 RepositoryValueIterationTest, all passed with original restoration code. Plain ThreadLocal identity consumers treat absent/null identically; no scope or owner remains retained by null. Independent review found no source-level blocker.

SOURCE_PULL candidate: 84 focused tests pass, followed by all 3 gated compiled memo/grouping map-growth and conflicting caller-context tests (no skips). Baseline foreground-only snapshot retained. Second ABBA and baseline/combined JFR collection underway. Publication of foreground checkpoint was denied twice by approval reviewer despite forwarded historical permission; no alternate publication route attempted. Local commits retained; parent handling authorization recognition.

Final direct ABBA: no convincing latency win; 2.76% fewer query-thread allocated bytes. Final broad rerun225 executed pass,3 skips separately covered by3 passing gated tests. Source tests84 pass. Scoped production/test formatting fixed a single line break. Global copyright check reports unrelated pre-existing headers; no changed lifetime files appear in its findings. Compact stable snapshots now assigned as next investigation in a separate scoped worker, with baseline API classes frozen before changes.
