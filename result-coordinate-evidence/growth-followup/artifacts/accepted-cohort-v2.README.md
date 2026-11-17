# Source package and accepted growth cohort

The complete source package for the accepted growth experiment is
[`complete-source-v1.tar.gz`](complete-source-v1.tar.gz), paired with the separately
frozen [`comparison-v2/protocol.json`](../comparison-v2/protocol.json). The source
archive, its manifest, and both patches remain byte-for-byte unchanged from the
qualified implementation. This addendum binds that package to the accepted
execution evidence without rebuilding the historical archive.

## Exact bindings

| Artifact | Bytes | SHA-256 |
|---|---:|---|
| `artifacts/complete-source-v1.tar.gz` | 2,466,704 | `ce87f598e1262540389aa5342bed470d09ca84d3ac80abf50d0a5f8987c2e04e` |
| `artifacts/complete-native-value-memo.patch` | 32,451 | `49c18a0b5e8b8f93964e68e73519cafdcbac0b752a90c9ba447735b4f8b7070d` |
| `artifacts/growth-only-versus-admission.patch` | 1,711 | `0c8c8ea00afc3a02112aec0b858cd94fe8b11a01a1700a2ba3566e29f1c45e36` |
| `comparison-v2/protocol.json` | 3,011,178 | `2aa846bb18d8c1eb028ca2fc9b90c782ac5446a7bcbcbf0cd94fc893d6539e0d` |
| `comparison-v2/audit.json` | 555,284 | `9ae4f85990d95aef7f74f0733c17883f3a19874ea1ef32fb2a7607c34bde49fa` |

The source package is a complete source overlay for the matching RDF4J checkout;
its dependency and rebuild requirements remain documented in
[`complete-source-v1.README.md`](complete-source-v1.README.md). The full feature
patch is based on `b8d2015ec1b722b71f9a8af9e50cd98558f7882a`. The smaller patch isolates
the growth change against the preceding admission experiment's canonical memo.

## Historical protocol and accepted execution

The protocol inside the unchanged source archive is the original, pre-execution
`comparison-v1` protocol, SHA-256
`60d231209b9d3a52e88d3ccc262422a90d342d7daacfe13383ec1a93ad209fe9`.
That cohort was permanently rejected after its execution-control interruption.
Its raw bytes and failure evidence remain preserved; its observations are excluded
from the accepted comparison.

The accepted primary execution is `comparison-v2`. The independent
[`protocol-restart-comparison-v2.json`](../reviews/protocol-restart-comparison-v2.json)
checked that the entire new protocol equals the original after normalizing exactly
218 authorized path/time fields: the cohort path, freeze timestamp, and 54 each of
slot-directory, GC-log, sample-output and store paths. Fixtures, run ordering,
arguments, estimators, source identity and runtime inputs are unchanged.

The shared final qualification receipt is
[`benchmark/qualification/final-runtime-v1.json`](../benchmark/qualification/final-runtime-v1.json),
SHA-256 `de7cdee686e3bfe8875829e1ea69d04e316f1bf6638a4bba2508da7abc9b17b9`.
It is backed by 265 passing ownership test executions and nine exact pilots
covering 117 queries. The accepted cohort subsequently completed all 54 JVMs and
702 queries: 432 warmups and 270 measurement queries. Its audit marks all result,
route and cleanup gates as passed. The final route-check receipt is
[`comparison-v2/profile-final-v1.json`](../comparison-v2/profile-final-v1.json),
SHA-256 `d1c9038bd764262b03143431ceb66d1c3ae161f220503cd72cfb19173fcfc08f`.
These execution facts do not replace the separate performance interpretation.

## Lossless recovery

The binary archive, accepted protocol and accepted audit have separate tracked
preservation manifests. Restore them from the repository root when their original
files are absent:

```sh
python3 result-coordinate-evidence/growth-followup/recovery/preserve_binary_artifacts.py restore --archive result-coordinate-evidence/growth-followup/recovery/preserved-source-archive-v1
python3 result-coordinate-evidence/growth-followup/recovery/preserve_evidence.py restore --archive result-coordinate-evidence/growth-followup/recovery/preserved-recovery-v2
python3 result-coordinate-evidence/growth-followup/recovery/preserve_evidence.py restore --archive result-coordinate-evidence/growth-followup/recovery/preserved-accepted-v2
```

| Preserved item | Manifest SHA-256 |
|---|---|
| Complete source archive | `b6771791718ea040794f735d3b1fc81b988c952dc3a9134605830f9fd975e05e` |
| Accepted v2 protocol | `0f34e9448a3c3b1795c1dca2e825c7cd78af082ae24a29c30028620ec5264078` |
| Accepted v2 audit | `a97c1b8c581d05ef1b9f91d353e616df74e23bfa555355273190ab88faf6a6ef` |

The audit uses one 63,441-byte gzip object. Fresh restoration, idempotent restoration
and refusal to replace different existing bytes passed before its exact ignore
path was appended. Original files remain present. All remaining accepted raw run
files fit the direct-transfer bound and are listed in
[`accepted-cohort-v2-inventory.json`](../recovery/accepted-cohort-v2-inventory.json).
The inventory verifies each of the 54 completion receipts against the saved raw
hashes and records 599 evidence files totaling 9,101,155 bytes, excluding derived
store directories. Root reports and subsequent independent reviews are separate
checkpoint artifacts and are outside that closed-cohort inventory.
