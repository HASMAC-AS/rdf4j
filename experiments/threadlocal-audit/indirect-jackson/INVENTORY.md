# Pinned Jackson 3 indirect-library audit

Read-only during ordered-values HTTP timing. No JVM, javap, builds, tests, or runtime probes executed. Used existing saved ObjectMapper javap and lightweight Python ZIP/classfile constant-pool and instruction-reference reads. The script and resulting symbolic references are preserved here; these are static references, not runtime invocation counts. No source fetch was required.

## Identity and actual path

Pinned tools.jackson 3.2.4 artifacts:
- core SHA256 `494b9f5fccf008042d5a72c5bf00629b144c0ba7362a9864cbb02234c9772651`
- databind SHA256 `c4af37223d5024f1294a5450598b01b65f30c53d9e906f02c3955ba1fb14a67d`

WorkbenchPageResultWriter uses its own static `new ObjectMapper()`. Saved mapper bytecode constructs new JsonFactory. JsonFactory superclass construction reaches TokenStreamFactory, which assigns JsonRecyclerPools.defaultPool. **In this version defaultPool calls newConcurrentDequePool, not threadLocalPool.** The pool wraps a ConcurrentLinkedDeque. Acquisition is pollFirst, creating a BufferRecycler only if needed; release is offerLast. Pool acquisition links recycler back to its pool for eventual release. This is a per-factory pool, not the global shared pool accessor.

CompactRows.add -> writePackedRow -> ObjectMapper.createGenerator(RowOutput) -> TokenStreamFactory/TextualTSFactory.createGenerator -> _createContext -> _getBufferRecycler -> RecyclerPool.acquireAndLinkPooled -> ConcurrentDequePoolBase.acquirePooled. RowOutput does not implement BufferRecycler.Gettable, so no externally supplied recycler branch applies. Factory creates IOContext and UTF8JsonGenerator; generator obtains write-encoding bytes and character scratch through IOContext/BufferRecycler. Recycled arrays are not evidence of new arrays per row. BufferRecycler uses AtomicReferenceArray storage, not TLS.

**TLS classification:** zero direct ThreadLocal get/set in the inspected default recycler acquisition/release path. JsonRecyclerPools.ThreadLocalPool does exist as an explicitly selectable alternative: its acquirePooled gets a ThreadLocal soft reference and may set a replacement. Existence does not mean it is selected. Do not import Jackson 2 default assumptions or label generator creation a TLS hotspot. This conclusion is scoped to the pinned default writer path, not every plugin/custom initializer/JDK descendant.

## Close and context boundaries

ObjectMapper.createGenerator obtains SerializationContextExt and initializes configured generator features, escaping, root separator and pretty-printer. A new generator has a new root write context. Array descendants can reuse child contexts within that generator; a new row generator reinitializes root/error location state.

Successful close runs generator input/output finish (including buffered-byte flush), buffer release, and IOContext.close. IOContext releases linked recycler once. Failure cleanup uses ClassUtil.closeOnFailAndThrowAsJacksonE to disable AUTO_CLOSE_CONTENT, close, and preserve suppressed failures. RowOutput.close/flush are no-op inherited methods, so generator close does not close the response. Existing application separately appends oversized-row suffix and flushes response after generator completion. Resetting RowOutput before pending generator bytes are flushed would corrupt row ownership/caps.

## Profile evidence, not a selected experiment

Saved serializer-workbench profile-both has **6 samples** at ConcurrentLinkedDeque.pollFirst with the complete path above through WorkbenchPageResultWriter.writePackedRow. This directly supports actual default pool use, but is not a precise call count, wait time, contention proof or speedup estimate. Top-list absence in other arms is not absence of work.

The same profiles list sampled allocation weights for JsonWriteContext, UTF8JsonGenerator, IOContext and SerializationContextExt. Weights vary heavily between recordings and include sampled/scaled allocation estimates, not exact per-row allocations. They establish lifecycle/allocation work worth considering, not a demonstrated win from reuse. The selected serializer still validates every JSON token. Current ordered-values timing is separate and must finish undisturbed.

A future reuse investigation would target **allocation/pool churn, not ThreadLocal elimination**. It requires explicit row-boundary flush before RowOutput.reset, root-separator suppression for multiple root rows, preserved caps/metadata/oversized framing, same depth constraints/error paths, and terminal cleanup on errors/cancellation/abandoned results. Reuse retains recycler/buffers/context across a response rather than releasing each row; slow/disconnected responses change resource lifetime. Completed contexts may retain current row value references unless safely cleared. A reused generator's root entry index and diagnostic location accumulate unlike fresh per-row context. Error cleanup must not auto-complete malformed rows or publish extra bytes. Holding generator state on the writer also introduces reentrancy concerns absent with a method-local generator.

No reuse implementation or new experiment recommended ahead of those semantics and measured ranking. Changing to a ThreadLocal pool would introduce TLS and different retention semantics; it is not an obvious optimization and is not proposed.
