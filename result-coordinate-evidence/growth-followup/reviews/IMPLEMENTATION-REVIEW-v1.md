# Independent review of the growth-only implementation change

The new four-file implementation overlay was compared byte-for-byte with the published admission overlay. Only `NativeDirectValueMemo.java` differs. Its reviewed SHA-256 is `8c7c482beb7118c020a14d919b2b406c3632c2abcd5f851b7fa5bceded347928`.

The materializer, projected binding set and statistics sources are byte-identical to their admission counterparts. Thus the source diff leaves the ownership integration, row handoff, weak-entry representation, identity validation, hashing, replacement, array accounting and statistics formulas unchanged.

The behavioral additions are one final constructor-captured boolean, validation of `rdf4j.lmdb.nativeValueMemo.growth=adaptive|fixed`, and one guard on the existing resize branch. Missing selection maps to adaptive. Invalid selection throws before the statistics owner is opened, and before any array reservation can occur. Changing the process property after construction does not change the existing memo's policy.

The first-window admission branch remains outside the new guard. Fixed growth therefore skips resizing while retaining the initial admission cutoff when selected, rather than skipping all adaptation. Initial allocation still occurs in the existing first-probe path. Fixed mode does not attempt a larger reservation, while adaptive mode retains its existing disable-on-refusal behavior. Closure and fallback code are unchanged.

The extra growth guard is evaluated at existing 8,192-probe adaptation boundaries. This review does not infer its compiled cost, nor assume that a final field removes all branch overhead. Both enabled benchmark arms must use the same new class bytes and differ only in the selected property.

No source-level correctness blocker was found. I also read the coordinator's closed receipts: the smallest unchanged-runtime test recorded one test/one failure, the full unchanged-runtime class recorded 12 tests/seven failures, and the changed-runtime class recorded 12 tests/zero failures. All three had zero errors and skips. These are existing execution receipts, not Java executions by this reviewer. The canonical formatted test source hashes to `2893aa7602b98981aa73ad276637f527763fd5472cc2d29f7319dd0adec13f13` and still contains all 12 methods.

Passing this focused class is not a replacement for the broader default, weak-retention and both IR-lifetime qualifications. Actual query exposure, source-call counts, AUTO chronology and measurement inputs also require their full exact pilots before freezing the comparison.
