# Qualify rare weak-fallback accounting

This experimental contract set follows the phase `../EXECPLAN.md` and the repository's `AGENTS.md`. It does not edit production source. The measured materializer will be generated only after the uninstrumented publication overlay has failed these counter contracts with their result and source-call assertions already satisfied.

## Current step

- [in_progress] Add and observe failing accounting contracts.
- [todo] Generate the minimal experimental counter overlay.
- [todo] Verify matching contracts and existing coverage.
- [todo] Freeze exact validated source and class identities.

The coordinator owns the mandatory root build and the exclusive Java slot. Creating these files does not authorize a Java process. Run the command below only after that slot is explicitly granted.

## Contract and observer boundary

`PublicationQualifiedAccounting` is an experiment-only helper in package `org.eclipse.rdf4j.sail.lmdb.evaluation`. Its public API is `initialize(int capacity)`, `snapshot(int index)`, `opened()`, `closed()`, `size()`, and `overflowCount()`. `initialize(0)` disables recording. Positive capacity preallocates a flat primitive buffer. `snapshot` creates its immutable result only when the wrapper serializes after the original benchmark launcher returns. The measured prefix will contain only the changed `NativeResultMaterializer` outer class, this helper and its `Snapshot`, and the experimental launcher. The prior publication plan, nested factor classes, and benchmark harness classes remain byte-identical.

Each Snapshot has sixteen `long` fields, in this order: `ownerOrdinal`, `compileOrdinalAtOpen`, `compileOrdinalAtClose`, `lazyValueCallsAtOpen`, `lazyValueCallsAtClose`, `ordinaryWeak`, `ordinaryWeakRecovered`, `ordinaryWeakTerminalOther`, `jointWeakTerminal`, `factorWeakTerminal`, `ordinaryWitnessFailures`, `factorWitnessFailures`, `abandonCalls`, `ordinaryAdmissionRefusals`, `factorMemoAdmissionRefusals`, and `factorPageAdmissionRefusals`.

The red-stage stub returns a zero snapshot and zero lifecycle counts. No materializer invokes it. The real source and return paths execute unchanged, then a positively expected counter fails with a `qualified-observer:` assertion description. Compilation failures, test errors, skipped tests, and failure to collect the required test referent are not accepted red evidence. The stub is never included in a timing runtime.

## Weak-reference scenarios

Four tests exercise real weak references without reflection, reference clearing, source internals being bypassed, or assumptions that skip the case. The ordinary-only case omits publication for the sole target. The factor-only case changes the ordinary ID before revisiting the collected factor member. The joint case revisits the ordinary and factor entries that share the collected reference. The recovered case retains an earlier row's value at one factor position, resolves a separate same-term wrapper at another position, allows that later ordinary wrapper to be collected, then returns to the retained earlier factor position. A counting source and exact returned-value identity establish whether each scenario resolved again or recovered a hit.

These unit tests may request bounded explicit GC to reach the weak-reference paths. They require collection with an assertion and never skip. The timed experiment must not request GC or retain extra results. The observer itself must not add `WeakReference.get`, `Reference.refersTo`, or a strong value/source/page reference.

Ordinary weak outcomes form the exact partition `ordinaryWeak = ordinaryWeakRecovered + ordinaryWeakTerminalOther + jointWeakTerminal`; `jointWeakTerminal <= factorWeakTerminal`; the terminal union is `factorWeakTerminal + ordinaryWeakTerminalOther`. The union is descriptive alongside raw resolver calls. It is not a time correction or a causal estimate of extra resolutions.

Witness tests mutate values through their public methods and verify replacement results. Scope tests change the source-view token at the existing validation boundary and verify that provisional hits are repaired. Admission tests use actual query-ledger limits to refuse ordinary storage, factor directory storage, page payload, and factor memo storage. Closure tests keep detached results readable after ledger-first and repeated closure.

The overflow contract opens and closes two real materializers against a one-record buffer. Both results and source-call lists must remain correct, both ledgers must close, and the observer must report two opened/closed owners, one retained record, and one overflow. Overflow disqualifies benchmark output; it must not silently lose the lifecycle evidence.

The test source increments the existing process lazy-value counter when its `lazyValue` method actually runs, just as the native store source does; its independent call list and returned-value assertions still establish the expected work. Each enabled-observer test checks the captured lifecycle boundaries against the real cumulative counter and the unchanged semantic-compile counter. A separate end-to-end preflight must check nonzero, consecutive semantic-compile ordinals because these direct materializer contracts do not compile SPARQL.

## Commands and evidence

From `/workspace/scratch/37853bcaea36/rdf4j`, after the coordinator's root-build receipt and exclusive Java-slot grant:

    python3 result-coordinate-evidence/publication-qualified-followup/contracts/run_contracts.py red-v1 --mode red --slot-granted

The runner composes copied frozen production/test classes and the exact prior publication overlay, compiles the stub and new tests into isolated folders, then invokes Surefire through a copied module POM. It records commands, source hashes, class hashes, XML reports, and results under `contracts/validation/red-v1`. A red receipt requires all declared tests, no errors or skips, and only deliberate observer assertion failures. It leaves module source and module build outputs untouched.

Compilation uses the verified frozen classpath. Surefire executes the isolated LMDB production/test copies with the workspace Maven dependency jars installed by the mandatory root build; this is not a claim that the complete frozen benchmark classpath executes inside Surefire. The runner records the actual `surefire.test.class.path` from its XML report, hashes every entry, and requires the green dependency entries to match the accepted red run exactly. Both stages require the sealed test source SHA, and red also requires the sealed stub SHA. The timing runtime will separately use and verify its frozen benchmark composition.

The sealed selection contains fifteen tests. The red stub must produce fourteen named observer assertion failures and one passing disabled-observer case, with zero errors and zero skipped tests. Required-GC or result/source-call assertion failures are not acceptable evidence for the missing observer.

After red evidence is recorded, the coordinator may authorize generator implementation. The same test source will then run against the new compiled prefix:

    python3 result-coordinate-evidence/publication-qualified-followup/contracts/run_contracts.py green-v1 --mode green --overlay <compiled-prefix-directory> --slot-granted

If the accepted red attempt uses a name other than `red-v1`, supply `--red-validation <accepted-red-directory>` to green. A changed workspace dependency requires a fresh red attempt before green; source assertion changes require a newly reviewed and sealed contract set.

An accepted green run requires every test to pass without skips. Earlier failed or interrupted attempts remain in their original directories; choose a new version suffix for a repeated attempt.

## Review decisions

Counter updates belong only to existing rare branches and owner lifecycle. The ordinary-weak classification uses a per-materializer factor-weak counter around the single existing factor lookup under the existing monitor. This is scoped to the experiment's non-reentrant native fixture; a general reentrant callback API would need an explicit local outcome. The helper records semantic-compile ordinal and cumulative lazy-value counts at owner open/close so the analyzer can align owners with unchanged harness samples.

No source generator or observer implementation has been written at this scaffold stage. No Java process has been launched by this work.
