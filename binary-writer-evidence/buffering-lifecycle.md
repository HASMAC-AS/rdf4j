# Binary writer buffering and HTTP lifecycle

Source inspection against upstream main
`5434e51194e6c2e1aa745e5d65a4c998918d6073`, 2026-11-17. This note records
source-derived risks and recommendations, not measured performance.

## The binary format still uses the response wrapper

`AbstractQueryRequestHandler.prepareResponse()` creates a
`QueryResponseHeartbeat`, gives its output stream to the actual selected writer,
and calls `heartbeat.start(writer)`. `QueryResponseHeartbeat.supports()` rejects
binary tuple results, so no whitespace probing thread starts for that format.
The writer nevertheless retains the wrapper.

Both scalar and bulk wrapper writes enter `outputLock`, check that output is
writable, stop probing when applicable, mark payload started, and forward to the
target. Failures signal client disconnection after the lock is released. Thus
reducing the number of output calls can save wrapper work even for binary
responses without a running heartbeat. Bypassing this wrapper would also bypass
its disconnection and terminal-state handling.

## What a persistent writer-side buffer changes

A `BufferedOutputStream` above the wrapper postpones the first payload write,
the wrapper's `hasPayload()` transition, physical write failures, and associated
disconnect signalling until the buffer drains. Server response commitment also
depends on actual downstream writes and flushes. A query that previously
committed before failing can remain uncommitted after this change and take the
normal HTTP error path instead of the binary terminal-error path. These are
lifecycle changes, even if successful response bytes remain identical.

The server owns and closes the response stream or heartbeat, not the writer's
new buffer. Closing the downstream stream does not drain a buffer above it.
Successful binary serialization already calls `endQueryResult()`, which flushes
the writer stream. Committed evaluation failure is also explicitly handled:
`TupleQueryResultView.writeCommittedBinaryError()` calls `writer.error(...)`,
then `writer.getOutputStream().flush()`, then `heartbeat.complete()`. Those
paths can remain correct if `getOutputStream()` exposes the actual buffered
writer stream and the entire chain is flushed before terminal completion.

However, `error()` itself currently writes immediately without flushing; code
that calls it and subsequently closes the original underlying stream would
lose pending bytes after a persistent buffer is added. `startDocument()` and
`handleSolution()` similarly expose bytes immediately today. On explicit
cancellation or serialization failure, cleanup stops/aborts/closes the heartbeat
and does not ask the writer to flush. Abandoning an incomplete binary response
is generally correct, but changing the amount and timing of delivered data is
observable.

The binary format's lack of whitespace heartbeats means inserting whitespace in
the middle of its buffered data is not a concern in this existing integration.
The relevant concerns are output visibility, failure detection, commitment,
and which layer owns flushing and closure.

## Recommended first experiment

Use row-local batching of adjacent NULL/REPEAT markers, draining before a value
and at the end of each successful `handleSolution()`. It reduces output calls
without retaining successful-row bytes across callbacks. It needs no change to
`getOutputStream()`, `error()`, successful completion, or server cleanup. It
does move marker delivery within a row; if value access/equality throws before
the run drains, fewer bytes of that incomplete row have reached the stream.
That should be documented as a smaller failure-timing difference rather than
claimed to be completely unobservable.

The scratch `harness/BatchedBinaryQueryResultWriter.java` implements only this
experiment. It preserves the original previous-BindingSet comparison and
EMPTY_ROW behavior, keeps a reusable byte array with one slot per result
column, uses a scalar write for singleton runs, and a bulk write for longer
runs. No production source was changed by this subtask and it was not compiled
or benchmarked by this subtask.

If a full byte buffer wins materially, a design that drains to the downstream
stream at public writer callback boundaries can preserve immediate callback
visibility while still combining individual field writes. It should drain
without imposing an underlying network `flush()` on every row. Keep explicit
`getOutputStream().flush()` and final completion semantics intact.

## Existing targeted integration coverage

The following tests in `TupleQueryResultViewTest` already exercise binary
terminal failure output:

- `testRender_BinaryTupleTimeoutAfterRowsPreservesTypedTerminalError`
- `testRender_BinaryCircuitBreakerAfterRowsPreservesItsReason`

Any persistent buffering implementation also needs coverage for explicit writer
flush, error followed by flush, successful completion, and downstream write
failure. If preserving callback visibility is part of the chosen contract,
assert bytes at each callback boundary rather than only at end of result.

## Source references

- [QueryResponseHeartbeat](https://github.com/eclipse-rdf4j/rdf4j/blob/5434e51194e6c2e1aa745e5d65a4c998918d6073/core/http/client/src/main/java/org/eclipse/rdf4j/http/client/QueryResponseHeartbeat.java)
- [AbstractQueryRequestHandler](https://github.com/eclipse-rdf4j/rdf4j/blob/5434e51194e6c2e1aa745e5d65a4c998918d6073/tools/server-spring/src/main/java/org/eclipse/rdf4j/http/server/repository/handler/AbstractQueryRequestHandler.java)
- [TupleQueryResultView](https://github.com/eclipse-rdf4j/rdf4j/blob/5434e51194e6c2e1aa745e5d65a4c998918d6073/tools/server-spring/src/main/java/org/eclipse/rdf4j/http/server/repository/TupleQueryResultView.java)
- [QueryResultView](https://github.com/eclipse-rdf4j/rdf4j/blob/5434e51194e6c2e1aa745e5d65a4c998918d6073/tools/server-spring/src/main/java/org/eclipse/rdf4j/http/server/repository/QueryResultView.java)
- [TupleQueryResultViewTest](https://github.com/eclipse-rdf4j/rdf4j/blob/5434e51194e6c2e1aa745e5d65a4c998918d6073/tools/server-spring/src/test/java/org/eclipse/rdf4j/http/server/repository/TupleQueryResultViewTest.java)
