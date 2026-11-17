# Direct memo retention: controlled payload and source-cache experiments

The weak treatment released the memo's value, revision, and decoded-payload graph while the iteration-owned memo remained open. The strong treatment retained that graph until close. A consumer-retained value remained valid in both modes. These are reachability results under explicit collection, not latency observations or a complete query-memory bound.

## Controlled fixture and observations

Four fresh JVMs ran the same 64 collision-free keys with 2,097,152-character distinct ASCII payloads. The source created cold native `LmdbIRI` objects and held only weak observers. No actual ValueStore or shared source cache participated. All 64 values were initially retained by the fixture, looked up twice, and then decoded. Each run had exactly 128 probes, 64 hits, 64 source creations and 64 initializations, without collisions or adaptation. The checksum was `-7785034976668276900` in all four runs.

Each of six checkpoints requested three collections and required an independent collection control to clear. The fixture used separate helper frames and `Reference.reachabilityFence` to preserve intentional consumer roots and remove temporary ones. All 24 checkpoints passed. Explicit collection was confined to these diagnostic JVMs.

The following observations are from `open_after_consumer_release`: the memo is still open and only the stated consumer root remains.

| Treatment | Consumer-held values | Live values | Logical live payload | Whole-process heap after GC | Memo ledger reservation |
| --- | ---: | ---: | ---: | ---: | ---: |
| Strong | 0 | 64 | 134,217,728 bytes (128 MiB) | 209,516,768 bytes | 147,536 bytes |
| Weak | 0 | 0 | 0 | 8,185,088 bytes | 606,448 bytes |
| Weak | 1 | 1 | 2,097,152 bytes (2 MiB) | 11,330,904 bytes | 606,448 bytes |
| Strong | 1 | 64 | 134,217,728 bytes (128 MiB) | 209,516,960 bytes | 147,536 bytes |

After memo close, neither treatment retained unowned values, and the ledger reservation was zero. With one consumer retained, that single initialized value remained readable after close. After its final release, all value, payload, revision and revision-canary observers cleared in every run.

The logical payload column counts known distinct ASCII characters and is a storage lower bound excluding object headers. The heap column includes observer arrays, table metadata, JVM infrastructure, collector allocation effects, and other roots. It is deliberately labelled whole-process heap; its difference is not a dominator-tree measurement of memo-exclusive retained bytes. The weak-observer counts establish which known fixture objects remained reachable.

## What the ledger measures

The matched strong control retains the earlier array estimate, `80 + 36 * capacity`. That is 147,536 bytes at 4,096 slots. Its declared reservation excludes wrappers, decoded text and revision ownership graphs. The experiment directly demonstrates that this charge can coexist with at least 128 MiB of live payload held by an open memo.

Weak mode reserves the same arrays plus `112 * capacity + 160` bytes for table-owned weak-reference metadata. The conservative allowance includes a weak value entry and owner witness per slot, an independent last-owner witness, and replacement overlap. The first reservation is therefore 606,448 bytes. Growth reserves new storage before releasing the old reservation. The selected runtime pins HotSpot object alignment at eight bytes and compressed class pointers; this is not a portable deep object-size formula.

Weak entries never strongly retain the Value, InternalIdentity, revision or ValueStore owner. Their larger declared metadata reservation is not evidence of a larger decoded-payload footprint. Conversely, weak retention does not bound values legitimately owned by consumer rows, source caches, or other parts of the query.

## Actual ValueStore cache control

The separate `NativeMemoSourceCacheRetentionDiagnosticTest` seeded and closed a real ValueStore, then reopened it with unchanged default cache settings. This removed input-model references from the observation frame. The test checked decoded IRI payloads of 1,024, 1,048,576 and 1,048,577 characters.

The first two decoded representatives and payloads remained strongly owned by the source cache, including the exact 1,048,576-character boundary. The larger representative was absent from the shared cache, and both its wrapper and payload became collectible while the store remained open. The one-test Surefire run passed without errors or skips.

This explains why an ordinary real-query heap comparison can obscure memo retention: weakening one owner cannot release a Value which another source cache still owns. The latency comparison keeps those source-cache settings identical across all arms; it does not disable them to magnify the weak treatment's effect.

## Evidence and reproduction

`memory-v1/receipt.json` records the four successful processes. Each run subdirectory contains exact commands, terminal status, GC log, raw JSON output and parsed observations. `run_memory.py` compiles the diagnostic and reproduces the fixed four-run sequence in a new output directory:

    python3 result-coordinate-evidence/retention-followup/diagnostics/run_memory.py --runtime result-coordinate-evidence/retention-followup/runtime-v2 --output result-coordinate-evidence/retention-followup/diagnostics/memory-reproduction-v1 --slot-granted

The real-source test evidence is under `contracts/validation/source-cache-diagnostic-v1`, including the exact source, command, classpath and Surefire report. The eight new retention/admission contracts passed; the broader final-runtime qualification passed 138 default/strong cases, 47 weak cases, and eight enabled strong lifetime cases. The weak group includes both compiled and interpreted native IR lifetime checks.

These fixtures establish the ownership distinction and preserve retained-value usability. They do not establish end-to-end latency, a universal heap bound, behavior for every source-cache configuration, or a solution to the unchanged first-8,192-probe cutoff.
