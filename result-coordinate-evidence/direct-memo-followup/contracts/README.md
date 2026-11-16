# Direct-mapped strong value memo: contracts

This is an isolated experiment seam. No production source is changed by these files. The stub establishes the new API while deliberately providing no caching, table allocation, memory reservation, or adaptation. Its `slot` helper implements the prescribed indexing function so the tests can construct deterministic conflicting keys; the tests do not test that hash algorithm against a duplicate implementation.

## API and policy

The package-local `NativeDirectValueMemo` has a `QueryLedger` constructor and these operations:

```java
Value find(long id, int fullMask);
void remember(long id, int fullMask, Value value);
void clearEntries();
void close();
boolean disabled();
int capacity();
long probes();
long hits();
long collisions();
long retainedBytes();
static int slot(long id, int fullMask, int capacity);
```

The constants are `INITIAL_CAPACITY = 4096`, `MAX_CAPACITY = 65536`, and `WARMUP_PROBES = 8192`. Entries use the entire position mask, including distinctions between masks with the same first-position cache bank. The memo admits supported native wrappers with known semantic versions; a hit also requires unchanged ID, revision number, and revision owner. The strengthened experiment therefore needs an identity witness beyond a version-only four-array implementation.

At the first 8192-probe boundary, cumulative hits below one quarter of probes disable and release the memo. Beginning at 16384 probes, cumulative hits of at least one quarter and occupied conflicting probes of at least one eighth permit growth, up to 65536 slots. An occupied slot with a different `(id, fullMask)` key counts as a collision at `find`; publication and rehash are not probe collisions. Disabled probes return null without extending the observed history. A failed initial or growth reservation disables and releases the table. A growth refusal is allowed to return an already captured hit from the current call, while all subsequent calls fall back.

The first `find` admits the optional table; `remember` publishes only into an already admitted active table. `clearEntries()` empties references while retaining an active table's capacity, reservation, and cumulative per-iteration probe/hit/collision history. `close()` releases and nulls retained state, is idempotent, and tolerates a ledger that was closed first.

These are standalone table contracts. Exact source identity, non-null stable `resultValueScope`, exposure identity, and before/after capture validation belong to the enclosing `NativeResultMaterializer`. This table API does not itself authorize sharing across sources, invocations, or exposure scopes.

## Sealed selection

The class is `org.eclipse.rdf4j.sail.lmdb.evaluation.NativeDirectValueMemoContractTest`. It contains exactly twenty tests:

| Test | Required behavior |
|---|---|
| `repeatedIdAndFullMaskReuseAcrossCallers` | One source resolution across repeated callers and rows |
| `fullMasksStayDistinctEvenWhenTheyShareACacheBank` | Exact mask keys preserve separate representatives |
| `unknownSemanticVersionsAreNeverAdmitted` | Unsupported mutation witnesses cause ordinary resolution |
| `semanticMutationRejectsTheOldRepresentative` | A changed value is rejected and its replacement can be reused |
| `changedInternalIdRejectsTheEntryWithoutAVersionChange` | A new internal ID cannot hide behind an unchanged version |
| `changedRevisionRejectsTheEntryWithoutAVersionChange` | A changed revision number invalidates the entry |
| `changedRevisionOwnerRejectsTheEntryEvenWithTheSameRevisionNumber` | Equal revision numbers do not merge dictionary owners |
| `coldValueRemainsAHitAfterInitializationAndEquivalentIdentityReplacement` | Cold-to-warm transition and equivalent identity objects preserve a valid hit |
| `aConflictingKeyReplacesOnlyItsDirectMappedEntry` | Conflicts replace an entry and count only at lookup |
| `lowHitWarmupDisablesAtTheFirst8192ProbeBoundary` | Low reuse disables exactly at the first boundary and releases charges |
| `delayedRepetitionAfterTheCutoffRemainsUncached` | This prototype's permanent cutoff misses later reuse |
| `cumulativeReuseAndCollisionPressureGrowOnlyFrom16384Probes` | Growth starts at the prescribed boundary under both forms of evidence |
| `repeatedGrowthStopsAt65536Entries` | Repeated qualifying pressure cannot exceed the slot cap |
| `initialReservationRefusalDisablesWithoutChangingResults` | Refused table admission preserves values and source fallback |
| `growthReservationRefusalReleasesTheOldTableAndFallsBack` | Real ledger pressure releases the optional table |
| `clearingKeepsCapacityAndHistoryButDropsEntries` | Scope clearing retains arrays and per-iteration history but discards references |
| `closeIsIdempotentAndSafeAfterTheLedgerClosesFirst` | Repeated and ledger-first close preserve retained values and release accounting |
| `missAndAdmissionDoNotInitializeColdPayloads` | Cache inspection and admission do not decode lexical payloads |
| `decodedPayloadGrowthDemonstratesTheArrayOnlyAccountingLimit` | A strongly retained value grows after admission without changing the table charge |
| `strongOwnershipEndsWhenTheMemoCloses` | A live entry retains its value, and closure releases that retention |

The expected feature-red result is **20 tests, 18 failures, 0 errors, 0 skips**. The two passing tests are `unknownSemanticVersionsAreNeverAdmitted` and `missAndAdmissionDoNotInitializeColdPayloads`: the absent feature already satisfies those negative contracts. Every intended failure contains `direct-memo:`. The source fixtures, identity mutations, decoding observations, and ledger cleanup have their own unmarked assertions; a fixture or collection failure is not accepted as feature-red evidence.

The strong-ownership test uses bounded explicit GC solely to check reachability in this unit contract. Collection after close is mandatory and cannot become a skipped test. No GC behavior in these tests authorizes forced GC in a timed experiment.

`contract-seal.json` pins the complete test and stub sources, exact method set, expected red failures, and marker. It also lists the three identity-witness methods for a separate diagnostic counterexample against a version-only four-array implementation. That diagnostic must not qualify a timing runtime.

## Accounting qualification

`retainedBytes()` in this prototype is the table/metadata reservation. It is **not a bound on the object graph held by strong entries**. The large-payload test admits a cold built-in literal, initializes a two-million-character label without changing its semantic version, and observes an unchanged table charge. The emitted diagnostic reports both charges and the decoded label length; it does not label either number as a measured heap footprint.

The fifth witness array can also retain an older `InternalIdentity` and its revision capability after the value publishes an equivalent identity. That lifetime lasts until replacement, clearing, disabling, or close. A production bound must cover retained decoded payload and dependent metadata, or avoid owning them strongly. Existing lexical estimators decode values and cannot be silently added to this lazy path.

## Verification state

Contract revision 1 produced twenty tests, eighteen expected failures, no errors, and no skips. Its runner rejected a JUnit method name containing the `TestReporter` parameter suffix, so that attempt was not accepted as the current implementation gate. Revision 1 sources, seal, and README are preserved under `history/unit-contract-v1`.

Revision 2 corrects two contract assumptions: clearing preserves cumulative per-iteration history, and the strong-ownership fixture calls `find` before publishing so admission follows the first-lookup lifecycle. No Java was run while making these revisions. The coordinator will run the complete newly sealed feature-red selection before implementation, then run those same assertions against the isolated compiled prefix. No passing Java result for the current contracts is claimed here.
