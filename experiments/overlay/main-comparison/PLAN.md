# Main comparison (investigation only)
No production changes. No issue supplied; GH-0000.
- [done] Pin current upstream main revision
- [in_progress] Build independent current main classes
- [todo] Compile identical cross revision harness
- [todo] Verify narrow no spill workload
- [todo] Measure matched wide narrow forks
- [todo] Preserve evidence and report limitations

Main be0c4c724f582e22d1a2936b87bb24969cc9154a (2026-11-17 fetch).
Experimental branch uses bounded wide table and memo/partial readiness, whereas main NativeDistinctTracker uses an unbounded HashMap for widths >4 and primitive table <=4. Same query budget does not guarantee equal charged memory enforcement. We must disclose this.
