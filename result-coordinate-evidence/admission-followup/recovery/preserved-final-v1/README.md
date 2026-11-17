# Restore the independent cohort audit

This directory preserves the closed `reviews/cohort-v2-independent-audit.json` without changing its original 599,900 bytes. The gzip object is 59,871 bytes. The other 596 new cohort text files total 5,707,679 bytes and remain ordinary checkpoint files; none needs compression to fit the transfer limits.

From the repository root:

```bash
python3 result-coordinate-evidence/admission-followup/recovery/preserve_evidence.py restore --archive result-coordinate-evidence/admission-followup/recovery/preserved-final-v1
```

The helper verifies the manifest, compressed part, restored file and any existing destination before writing. It restores missing files only and refuses to overwrite different existing bytes. `roundtrip-verification.json` records fresh restoration, idempotence, refusal and verification of the unchanged original.

The complete cohort inventory is `recovery/final-closed-evidence-inventory-v1.json`. It covers all 54 claims, 54 query-output files, 54 completion seals and associated launch, process, input, validation, log and profile receipts. The query files contain 432 warmups and 270 measurements. The frozen v2 protocol remains preserved separately in `recovery/preserved-pretime-v2`; its bytes were verified unchanged.
