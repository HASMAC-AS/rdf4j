# Publish an immutable experiment checkpoint

The coordinator selects and commits the exact files locally before publication. This helper never selects working-tree changes, stages files, creates a local Git object, modifies the index, or moves a local ref. Publication must wait for the coordinator's explicit commit and parent IDs.

The expected existing remote branch is `HASMAC-AS/rdf4j:GH-0000-lmdb-lazy-result-coordinates`. Its previous durable checkpoint is `97284c5229fc826b156275b15f105aac4dd801d6`, whose tree is `4db5b366c00c68f5e89c7301612b5a821fc0e5d0`. That branch must be read again immediately before a new publication. A changed remote head requires inspection; it is never overwritten or forced.

## Prepare from Git objects

After the coordinator provides an immutable commit, run from the repository root:

```bash
python3 result-coordinate-evidence/direct-memo-followup/recovery/prepare_github_checkpoint.py --commit LOCAL_COMMIT --parent EXPECTED_REMOTE_HEAD --output /workspace/scratch/37853bcaea36/github-transfer/CHECKPOINT_NAME
```

The output directory must be new and outside the repository. It contains regenerated transfer payloads, a blob identity manifest, and `transfer-plan.json`. These payloads remain temporary transport material; their contents are already represented by the selected Git commit. Small publication receipts belong under this phase's `recovery` directory so a later checkpoint can preserve them without copying another full transport payload.

Preparation reads the parent and target commit trees independently. It recreates both Git tree hashes from path, mode, type, and blob IDs before building any upload plan. It also checks that the selected target has exactly the expected parent and that local HEAD and the index remain unchanged while preparing.

Only one copy of each new text blob is uploaded, using its first changed path. Additional identical paths are installed by SHA in the final tree request. Blobs already present in the parent tree need no upload. Binary files, including the three lossless gzip recovery archives, receive separate base64 blob uploads. Every intermediate and final tree has an independently calculated expected Git SHA.

## Apply the prepared plan with the GitHub connector

Use the working GitHub connector; command-line authentication is not required. Keep mutations sequential and write a receipt immediately after each completed call. Never print the complete source or base64 payload into the conversation.

1. Read the remote Git ref and parent Git commit through `github_fetch`. Require the branch SHA to equal `expectedRemoteParent` and the parent commit's tree to equal `baseTreeSha`.
2. For every `binaryUploads` entry, load the recorded base64 file, verify its SHA-256, and call `github_create_blob` with `encoding: "base64"`. Require the returned SHA to equal `expectedBlobSha` before continuing.
3. Apply each `textTreeBatches` entry in order with `github_create_tree`, using its recorded `baseTreeSha` and JSON entries. Require the returned SHA to equal `expectedTreeSha`. The preceding step's returned tree must be the next step's base.
4. Apply `finalTree` with its SHA-only entries. This installs binary paths, repeated content paths, mode changes, and any recorded deletions. Require the returned tree SHA to equal the immutable target tree exactly.
5. Create one commit with that exact tree, the expected remote parent, and the prepared message. GitHub may assign a different author/time and therefore a different commit SHA from the local checkpoint. Verify the remote commit through raw Git Data: exact tree, sole parent, and message.
6. Read the branch again, then call `github_update_ref` with the new remote commit SHA, `expected_sha` equal to the inspected parent, and `force: false`. A lease mismatch stops the operation without replacing the existing branch.
7. Read the Git ref once more and require it to point to the verified remote commit. Record the remote URL, local equivalent commit, tree, parent, branch, all step receipts, failures, and final local HEAD/index observations.

The coordinator may then fetch or adopt the equivalent remote commit. This publisher does not mutate the local branch, index, or working tree.

## Large recovery artifacts

Three original recovery JSON files remain untouched and are excluded by exact `.gitignore` paths. Their complete bytes are committed as gzip files under `publication-qualified-followup/recovery/preserved-json-v1`, with raw and compressed lengths and SHA-256 digests. The adjacent restore helper verifies the archives and can recreate only missing originals. All code, protocols, ordinary reports, failed attempts, and smaller raw evidence remain unchanged ordinary files in the checkpoint.

## Verification performed before publication

The independent tree-hash implementation reproduced both known immutable roots: parent `15fe7d5011d27d31d76436238363ee3f34b3ba50` has 18,692 leaf paths and tree `5b102d0e4a9ca6b11862b2277f88eaeab877930f`; restored checkpoint `97284c5229fc826b156275b15f105aac4dd801d6` has 19,017 leaf paths and tree `4db5b366c00c68f5e89c7301612b5a821fc0e5d0`. This preparation check used no Java, network mutation, or local Git write.
