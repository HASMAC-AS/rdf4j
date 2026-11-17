# Isolated compact snapshot hash spread

Default-off property unchanged: `rdf4j.sail.compactResultSnapshots`.
Production delta: `name.hashCode() * 0x9e3779b9`, then existing unsigned xor-shift16 and table mask. Multiplication wraps as Java int. Same probe, comparison, insertion, and fallback semantics. No new object allocation.

Prior frozen candidate remains `candidate-api-classes`; new API freeze: `hash-api-classes`. No timings collected here. Prior compact candidate failed parent latency acceptance despite lower allocation; this is an isolated follow-up, not a recommendation.

Actual fixture names are s,label,seed, then n0,v0_0..v0_7,n1,v1_0..v1_7,n2,v2_0..v2_7. Deterministic64-slot insertion simulation: old mean5.0333/max16 probes; new mean1.2667/max3. Identical String hashes still collide; existing collision tests exercise equality/probing.

Verification: pre-change24 focused tests pass; post-change24 focused tests pass each with flag on and off;19 compact tests pass with assertions disabled and assertion-mode guard. All zero failures/errors/skips. Generated-name characterization spans widths0–257; existing tests cover reused sources, callback changes, views, null/duplicates, serialization and fallback. Scoped Spotless apply and quick API install pass. Full module suite not rerun for this isolated follow-up; prior full-suite evidence belongs to original prototype.

Commands: `experiments/overlay/lifetime/mvn.sh -o -pl core/sail/api -Pquick install`; followed by `-Drdf4j.sail.compactResultSnapshots=true` (or false) `-Dtest=CompactBindingSetSnapshotTest,SailResultValueExposureTest surefire:test`; assertion-off uses `-DenableAssertions=false -Dtest=CompactBindingSetSnapshotTest`. Raw logs are hash-pre-tests.log, hash-post-on.log, hash-post-off.log, hash-post-da.log and hash-format.log in this directory.
