# Final new-experiment packaging

Packaging starts only after the coordinating agent declares the final report, restoration verification, decision,
and independent review complete. The frame-directory guard is not promoted. The actual core source delta from the
latest materializer delivery must contain only `KernelResultCoordinatesTest.java`; the nine new characterizations
remain, while prototype production source/runtime/patches remain explicitly experimental evidence.

All outputs are additive under `/workspace/scratch/73b19a6d6f5c/lmdb-next-experiments-deliverables/`.

1. Run `prepare-selection.py` with the exact final report and restoration-record paths. This carries forward the
   previous task-source selection and includes every new-phase Java/Python source, report and experimental patch.
   It closes the new-phase evidence inventory after all writers stop. Store files, classes and copied dependency
   trees are excluded from this text evidence product; historical evidence archives remain separate and preserved.
2. Run the unchanged `result-coordinate-evidence/package-coordinate-deliverables.py` with the closed selection and
   output `source-and-evidence/`. It packages complete current tracked source, new LMDB source, selected task sources
   and initialized external submodule source. It creates the checkpoint-based full binary patch, constructs a private
   Git tree, proves patch/tree equality, reads back both archives, and checks that real indexes and inputs stayed unchanged.
3. Run `build-turn-delta.py` with the preserved `result-coordinate-materializer-deliverables/` as prior delivery,
   `source-and-evidence/` as new delivery, and `turn-delta/` as output. The helper pins the prior tree and receipts.
   It verifies full and narrow core deltas by forward/reverse application in private indexes. The permitted core
   delta is the characterization test file only. The complete checkpoint patch remains unchanged.
4. Run `bundle-next-runtime.py` to output `runtime/`. It retains the unique 127-entry original base, common harness,
   experimental candidate production tree, both namespace diagnostics, shared-selection harness and page diagnostic.
   Eight runtime compositions should reference 134 unique classpath artifacts. The page diagnostic's raw javac
   outputs are preserved separately from its thin runtime overlay. Every new-phase source, raw result, protocol,
   independent analysis, decision and restoration/review record is included. The JDK and regenerated stores are not.
5. Extract the new runtime into a fresh private directory. Run `verify-and-relocate-runtime.py --validate-only`
   against the externally recorded manifest hash, then generate an additive relocation plan with the compatible
   Java path. Verify all mapped artifact hashes and preserved command order without launching Java. Original raw
   descriptors remain byte-identical; derivative classpaths and launch commands have explicit path-change records.

The runtime bundle reuses the previously verified archive writer/readback implementation. Every member and unique
artifact is hashed after archive creation, and an independent extracted-tree verifier repeats file/artifact binding
checks. Relocated shell plans preserve all non-path JVM/application arguments; primary timings and diagnostics remain
separate. They do not claim that a future rerun passed its original correctness or route checks.

Complete project sources and Git patches stay in the workspace. Reports, evidence, runtime archives and verification
receipts are the artifacts selected for durable delivery. No old deliverable is replaced, no source is committed,
no real Git index is staged, and no Java/build/test operation is part of this packaging workflow.
