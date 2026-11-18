# Delimiter rescan: application versus measurement client

Read-only source/artifact review. No application changes, builds, JVMs, or test execution.

## Conclusion

The benchmark Node client's repeated accumulated-prefix search is **not present in the actual Workbench client examined here**. An application delimiter patch is not justified by that Node profile. Repairing/qualifying the measurement client is a separate harness change and changes the measured client workload; preserve old measurements and do not silently combine them with new-client timings.

## Actual browser path

`tools/workbench/src/main/frontend/classic/queryStream.ts`:
- Query POST requests compact NDJSON (1253–1256), then routes its response to `consumeNdjsonResponse` (1263–1272).
- `consumeNdjsonResponse` constructs `NdjsonLineSplitter` (1144), streams UTF-8 decoding (1217–1224), and feeds each decoded chunk to it (1184–1193).
- The splitter (1080–1105) searches only the arriving `chunk` (1087), resumes within that chunk at `start` (1093), retains incomplete fragments in `parts`, and joins those fragments once when a complete line arrives (1089–1091). Already searched fragments are not searched again on the next chunk.
- The standalone `NdjsonResultParser` uses the same splitter (1031 onward).
- Generic Workbench page loading also delegates to `consumeNdjsonResponse`: `workbenchApp.ts:555–559`.
- Delivery awaits each record callback before subsequent record delivery/network reads (1184–1193,1217–1224). This is real browser backpressure, not just delimiter parsing.

The frozen reconstructed application agrees: `depth-workbench/runtime/BOOT-INF/classes/rdf4j/workbench-webapp/scripts/queryStream.js:928–947` contains this same fragment splitter; its async consumer constructs it at979, and query response routing calls the consumer at1100. The bundled `scripts/ts/queryStream.ts` and current source are byte-identical, SHA256 `2d33bf051cd0e4977fd0eb14e37c2047b94b7ff0d4e7d7481e2a7bb61fbce18a`.

Existing regression source explicitly addresses this previously identified bug: `tools/workbench/src/test/frontend/classic/review-fixes-query-stream.test.js:589–629` instruments delimiter scans for oversized records arriving in 16KiB chunks. Both the async reader and standalone parser assert scanned characters below three times input size. Tests were inspected, **not rerun** in this read-only review.

## Benchmark path

`experiments/threadlocal-audit/wide-isolation/client.mjs:28–32` appends newly decoded text to `carry`, then calls `carry.indexOf('\n')` with no start offset. If a record spans many chunks, each unsuccessful search starts at the beginning of the accumulated record again. Earlier `range-workbench/client.mjs:30` and `depth-workbench/client.mjs:29` have the same pattern.

For a record of length L split into k chunks, this permits roughly O(L*k) searched characters instead of a fragment splitter's O(L) delimiter search. String concatenation/flattening costs are additional and engine-dependent. This is algorithmic source evidence, not a measured percentage attribution. JSON parsing, UTF-8 decoding, application callbacks and transport backpressure still cost time in the real client.

## Measurement implications

The supplied profile's Node delimiter hotspot identifies harness overhead, not equivalent browser overhead. It can influence EOF/throughput and producer backpressure, and potentially obscure server-side deltas; the magnitude must be established with qualified matched measurements. First-record timing is also measured after client parse, though the first 32-row record is smaller than later cap-sized records. No browser-paint claim or direct projection from Node ticks is warranted.

A replacement harness splitter should be differential-tested for arbitrary chunk boundaries, multi-line chunks, multibyte UTF-8, empty/CRLF lines, final incomplete text, malformed/error records, and preserved timing/oracle semantics. Keep strict fatal UTF-8 and existing tail validation unless deliberately changing the benchmark contract. This review does not authorize changing application parser behavior.
