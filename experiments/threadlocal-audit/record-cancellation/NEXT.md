# Next experiment: consolidate owned cancellation checks

Read-only design against the frozen candidate. No implementation, compilation, or timing changes.

## Observed motivation, not attribution
Parent reports interpreted actual-path times A 447.58 ms, candidate OFF 466.15 ms, candidate ON 480.59 ms. These are unfavorable observations. They do not establish that extra cancellation checks caused the difference. Await matched profile evidence before choosing or claiming an explanation.

## Actual static accounting
For scanner-owned, directly physical retained probes (not adjacency/ordered merge), successful `LmdbNativeKernelScanner.open` adds:
1. One full `NativeRecordCancellation.check` at admission, before source/probe open.
2. On subsequent opens of the same bound physical cursor, one full owner check in `LmdbRecordIterator.reset`, before native locking/reset work. First creation has no such check.
3. One full owner check in `ReusableCursor.reset` after binding and before returning open.
4. One full owner check at each `ReusableCursor.fill`, including calls on a detached cursor.
5. Existing physical checkpoints now use the explicit owner: initial entry when tick is zero; every 1,024 physical candidates, including filtered candidates; exhaustion. Reset returns tick to zero.

Thus with F live wrapper fill calls and L actual physical checkpoints, a newly created physical probe performs **2 + F + L** full owner checks; a reused already-bound probe performs **3 + F + L**. This excludes other kernel/row checks and source/lifecycle validation. These are static call counts, not profile-derived counts or a claim every query follows this route.

For a typical first fill returning fewer than 1,024 physical candidates without exhaustion, L=1: new probe 4 checks, retained reopen 5. For a short fully exhausted scan with one fill and entry+exhaustion, L=2: new probe 5, reopen 6. Exact-key fills may require an extra fill to discover exhaustion. Do not substitute returned-row count for physical candidate count or assume one exhaustion check per query; closed calls return early, filtered scans vary, and ordered priming already consumes leaves before binding.

Each full owner check polls native query/parent state first, then local/lifecycle state, then the speculative probe deadline when present. The deadline may consult the clock. Leaf ownership removes ambient lookup at these leaf checkpoints, **not** the added admission/reset/fill checks.

## Smallest promising isolated experiment
Keep admission, post-binding, and physical reset checks unchanged initially. Target only the wrapper fill check, and only a narrowly certified unbuffered physical route:
- Record a per-current-delegate capability at bind time. It must prove that actual physical iteration, not merely some child, performs bounded explicit-owner polling. A successful `bindCancellation` alone is NOT sufficient proof: OrderedRecordIterator can serve an already primed buffered head, and arbitrary future wrappers may accept binding without matching physical cadence.
- Initially certify LmdbRecordIterator plus audited transparent measurement/source-lock forwarders, not ordered/concatenating/adjacency leaves. Preserve the complete existing wrapper check for all other paths.
- For the certified route, let existing physical entry/candidate/exhaustion checks own full polling. A cheap local-stop guard at wrapper fill can retain immediate scanner-local cancellation. If that flag is set, call the full owner check rather than throwing QIE directly, preserving already-published parent provenance.
- Retain full check on detached cursor fill if its currently tested terminal behavior is to be preserved; normal detach is not cancellation.

This removes at most F full checks on the eligible route, replacing them with a local flag load/branch. It does not eliminate reset or admission checks. No owner/lease allocation per key is needed.

### Important semantic qualification
Omitting full wrapper polling changes when a newly published **parent/lifecycle/probe** cancellation is observed between short noninitial fills. Physical polling still bounds progress by 1,024 candidates (and exhaustion), but the current candidate happens to poll at every wrapper fill. This is a bounded-cancellation contract choice, not exact observational equivalence. If immediate per-fill detection for those signals is required, this proposal is not admissible as written. Keep full boundary checks or introduce a separately proved leaf API that consumes an already-performed check without silently suppressing a check across callbacks/locks. Even such an API changes race observation points and needs an explicit cadence contract.

A local-only fast guard must not silently omit lifecycle checks if lifecycle cancellation is promised before every fill. Strengthening the fast guard with lifecycle state retains that property but does not avoid its read; on a positive guard it must still poll parent first.

## Checks not safe to simply delete
- Admission: local cancellation before the first open must prevent source.newProbe/open and potentially blocking native work. Opening latch must be set before the guard and retained through finally.
- Post-bind: observes a request arriving during unknown opener/ordered priming before cursor exposure. Removing it breaks the latch-controlled admission regression. Replacing it with a local-only check delays a query/probe event during open and must not be called equivalent.
- Physical reset: direct retained callers outside this scanner rely on terminal cancel-before-reset. Scanner admission is not a proof that no event arrived while probe.open performed callbacks/selection. Do not weaken generic/direct reset or clear its flag. A dedicated scanner-only reset protocol is a larger experiment, not this change.
- Parent priority: when parent cancellation and local/lifecycle/probe signals coexist, report parent provenance before local QIE and probe decline. Do not invent root cancellation or mutate row/root to make a cheap guard convenient.
- Unsupported leaves: keep the permanent unsupported-binding latch and false acceptance; a prior successful leaf cannot certify a later declined leaf. Current-delegate capability must reset on every open/detach without resetting terminal cancellation.
- Request path stays volatile publication only, with no traversal of mutable heads/cursor arrays and no native locking/close. Bind/reset/dispose remain evaluation-thread serialized.

## Required tests and measurement isolation
1. Route hit proof for the certified transparent physical chain; explicit negative proof for ordered primed/buffered, mixed accepting/declining, adjacency and detached paths.
2. Cancel between admission and bind, after bind before first fill, and between later short fills with nonzero tick. Include simultaneous parent/local/lifecycle/probe signals and parent-child ownership.
3. Existing filtered-candidate <=1,024 bound and request while native lock held; no dependence on returned-row count.
4. Direct reset-after-cancel, owner replacement refusal, reuse, final disposal, replay/map-growth and snapshot error ordering remain covered. Changing pre-lock wrapper polling can change cancellation versus snapshot-failure precedence: assess explicitly, not only successful queries.
5. Existing nested ambient/conflicting scope tests remain unchanged. No generic cursor constructor capture.
6. Separate candidate OFF-control overhead from owned ON consolidation; freeze new runtime only after behavioral validation. Untimed diagnostics count wrapper/full/leaf checks if added, disabled in timing. Compare the same qualifying route, not an adjacency path where the optimization does not run.

No implementation is recommended until the profile and intended bounded-cancellation contract justify this specific change. If strict every-fill exception/precedence behavior is mandatory, keep this path unchanged and prioritize another overhead instead.
