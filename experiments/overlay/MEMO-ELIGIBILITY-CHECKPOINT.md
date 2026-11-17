# Aggregate memo eligibility compatibility fix

Regression: LmdbNativeIdAuthorityTest.aggregateValueCachePreservesResolutionAcrossGrowth expected64source resolutions but observed128 when memo flag enabled. ExactSpellingSource does not certify stable value IDs. AggContext nevertheless installed NativeValueMemo; its disabled unstable-ID path bypassed the established aggregate cache on every call.

Fix: AggContext only creates the optional memo when enabled AND source!=null AND hasStableValueIds(). Ineligible sources retain the old aggregate cache,including full position-mask keys. Eligible sources retain weak memo/mutation behavior. Once an eligible memo disables for memory/repetition,it still bypasses caching; this change does NOT introduce a strong aggregate-cache fallback after budget refusal. Null-source primitive-only contexts remain valid.

Evidence:experiments/overlay/memo-eligibility-red.log captured existing targeted failure (1test,1failure,expected64actual128) before production edit. After published checkpoint8df93a67,parent authorized fix. experiments/overlay/memo-eligibility-green.log:36tests pass,0failures/errors (full authority16,memo16,new eligibility4). New tests explicitly verify unstable cache/fullmask hits,stable memo retained,memoryrefusal one reservation/no strong fallback,and null-source primitive context. Existing ExactSpellingSource fixture unchanged.

Changed only LmdbNativeAggregateState.java constructor and new NativeAggregateMemoEligibilityTest.java. No commit/publication by this worker. Parent owns broader verification and checkpoint.

Final same36 selection rerun with all actual flags memo.enabled=true,repository.partialReady=true,nativeDistinct.widePrimitive=true passed36/0/0; log memo-eligibility-green-all-flags.log. Earlier green command's wideKeys property was misspelled and therefore is not evidence for wide path enabled; corrected rerun supersedes that caveat.
