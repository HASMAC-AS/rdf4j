# Exact checkpoint publication

Published and verified 678292b6538f24d8982ec35bca8ba404a6797d5b on `GH-0000-lmdb-lazy-result-coordinates` in `HASMAC-AS/rdf4j`.

- Selected local commit: `bc872b63daacb63e2a581d3d6550a5f08a5e9dfc`.
- Exact tree: `369303da3e51d04c283469c0d10b393d36a83dea`.
- Sole parent and expected previous head: `97284c5229fc826b156275b15f105aac4dd801d6`.
- The update used the expected head and `force=false`.
- The remote commit message, raw commit tree, sole parent and final branch ref were checked.
- Local HEAD and index SHA-256 remained unchanged. No Java or local Git mutation was performed.

The remote commit has a different commit SHA because the connector creates commit metadata, but its full tree, message and parent match the selected checkpoint. All 4 uploaded binary blobs, 29 intermediate text trees and the final SHA-only tree matched independently computed local Git object identities.

`receipt.json` records the final outcome. The numbered blob/tree receipts retain each API response; the raw commit/ref reads and lease request retain publication verification. `artifact-manifest.json` pins every receipt in this directory except itself. The transfer plan points to reproducible payloads outside the repository, avoiding a second copy of source and binary payloads in Git.

Later qualification outputs and this publication receipt were intentionally not part of the immutable tree. They can be included in the next checkpoint after the timing run.
