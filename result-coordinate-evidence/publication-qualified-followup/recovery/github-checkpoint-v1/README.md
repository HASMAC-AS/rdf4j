# Persisted restored checkpoint on GitHub

The exact restored checkpoint is now reachable on `HASMAC-AS/rdf4j`, branch `GH-0000-lmdb-lazy-result-coordinates`.

- Remote commit: `97284c5229fc826b156275b15f105aac4dd801d6`.
- Exact source tree: `4db5b366c00c68f5e89c7301612b5a821fc0e5d0`.
- Sole parent: `15fe7d5011d27d31d76436238363ee3f34b3ba50`.
- Equivalent local commit retained unchanged: `3ba65db01d0e4e68af18c1f5151f30a76dfb5423`.
- Commit URL: https://github.com/HASMAC-AS/rdf4j/commit/97284c5229fc826b156275b15f105aac4dd801d6.

All 493 changed UTF-8 blobs were read from the local commit, preserving their original Git modes and bytes. The transfer used 19 sequential GitHub tree operations. Each returned intermediate tree matched a separately computed Git tree identity, and the final tree matched the selected snapshot exactly. The commit was then read through GitHub's raw Git Data endpoint to verify its tree, parent, and complete message. The normalized commit endpoint also confirmed the commit exists; its diff was not used as complete-file verification.

The branch did not exist at the initial check or the fresh check immediately before creation. It was created at the verified commit. A subsequent exact Git ref read confirmed the branch points to that commit. No force update or unrelated branch overwrite was performed.

The connector assigns commit author and time metadata, so the remote commit SHA differs from the equivalent local commit while its tree, parent, and message are preserved. Local HEAD and the real Git index remained byte-for-byte unchanged during this persistence task. No observer source was edited and no Java process was launched by this task.

`receipt.json` is the completion record. `local-checkpoint.json` lists every changed path, mode, blob ID, byte count, and SHA-256. `tree-elements.json` is the exact content payload; `transfer-plan.json` and `expected-intermediate-trees.json` reconstruct all batch requests. Each `batch-NN.json` records the remote response and the independently expected tree. Commit and branch mutation/read receipts are preserved separately. The earlier direct Git push failure is retained by path and hash in the completion record and copied alongside these receipts.

