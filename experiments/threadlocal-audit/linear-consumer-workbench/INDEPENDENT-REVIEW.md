# Independent linear-consumer review

Approved frozen follow-on sixteen-fork four-arm comparison, kept separate from previous consumer populations. Read-only review; no JVM/build/benchmark or production edits. Read splitter, client diff, tests, PLAN, runner diff and saved qualification evidence.

## Parser invariant and semantics

At push entry fragments holds exactly the prior unterminated line. Each loop scans only the new decoded text at start, which advances beyond the previous LF. A found LF joins prior fragments with this chunk's prefix once, clears fragments, and yields immediately. On no LF it saves only the new remainder. Empty strings do nothing; a chunk ending in LF leaves no pending remainder; leading/consecutive LF yields empty lines, still skipped by the existing consumer. Multiple records in one chunk are handled lazily, so JSON parsing/first rows-batch timestamp happens before the next generator iteration scans later records. No accumulated prefix is rescanned.

Incremental fatal TextDecoder is unchanged and owns byte-to-UTF16 state, including split emoji/CJK and incomplete sequences. Fragments are decoded strings, not byte offsets. finish appends decoder flush as tail without parsing it, exactly preserving prior trim-empty EOF check and missing-terminal failure. Unterminated JSON is still rejected. Error record propagation and already-processed records remain ordered; iterator abandonment after error does not matter because client aborts rather than reuses splitter. Space/CRLF behavior matches prior parser. Memory remains proportional to incomplete line plus fragment count; this is not a new bounded maximum-line parser guarantee.

## Evidence reviewed

parser-tests.json reports997 differential cases and30001 searched UTF16 regions for10000 three-unit fragments plus LF. Cases cover every two-way byte split of supplied payloads, one-byte/random chunks, Unicode/emoji, malformed JSON, blank/CRLF/whitespace, invalid/truncated UTF8, error records and duplicate/missing terminal. Differential comparison checks parsed objects and error CLASS, not exact exception messages. Added explicit success, missing-newline and invalidUTF8 expectations prevent a shared silent-failure false positive; explicit generator next assertions complement source-level lazy-yield review. Search instrumentation is test-only and absent from client.

Reviewed unchanged splitter SHA256 `c4948712974527be56fed767ed2c6af1aa3406257984318cbe3299a3dcb1d502` and client SHA256 `ec0d6ff896be2d2e7bd801c6878b164152badc47dd1f843f63a7b8b341111b05`. Test source was strengthened during review, so use refreshed frozen manifest for its final hash. run.py matches prior wide-isolation runner byte-for-byte; only harness parser/client identity changes. No application code or runtime-arm substitution.

Independently read/checked all four saved qualification entries:100000 rows, expected dual hashes -4997214842688641232 /5775044763147336544, first record32rows, zero spill creations/migrations. Existing full-term/Cartesian bag checks, origins and cleanup remain runner-owned. Rows record counts are581/581/581/582 across base/depth/serializer/stack, so do not claim identical segmentation; correct bag equivalence and recorded framing witnesses support comparisons without requiring identical row order.

## Interpretation

This repairs benchmark endpoint scanning, not application frontend code, which already uses new-chunk fragments. Corrected results form a new population. Do not pool earlier values, infer a controlled parser speedup across separate runs, or explain any component change solely from removal of one sampled hotspot. Clean timing remains separate from JFR/Node diagnostics; Node main-isolate profiles omit other worker/native CPU and include setup/metrics unless explicitly filtered. First-row and EOF boundaries remain unchanged. No merge-ready conclusion follows.
