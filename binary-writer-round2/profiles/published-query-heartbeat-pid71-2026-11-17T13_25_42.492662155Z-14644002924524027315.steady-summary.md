# JFR allocation first-sample sensitivity analysis

Approximate corrected distribution: the first allocation sample per recorded thread ID is omitted. This also drops some valid post-start allocation weight. CPU events are unchanged; JMH remains the authority for measured B/row. Keep the raw summary alongside this report.

Omitted selected allocation weight: 2,610,946,984 bytes (26.02% of the raw selected weight).

## Every omitted allocation sample

| Thread ID | Timestamp | Weight, bytes | Selected | Allocated class | Top recorded frame |
| --- | --- | ---: | --- | --- | --- |
| javaThreadId=23 | 2026-11-17T14:25:42.594877133+01:00 | 2,610,946,984 | True | `java.util.ArrayList` | `org.eclipse.rdf4j.sail.lmdb.evaluation.LmdbNativePackedFtree$Planner.lambda$plan$0(Ljava/lang/Long;)Ljava/util/List;` |
| javaThreadId=18 | 2026-11-17T14:25:42.827048880+01:00 | 21,800 | False | `java.lang.String` | `<no stack>` |
| javaThreadId=3 | 2026-11-17T14:25:46.616413814+01:00 | 23,095,736 | False | `java.util.concurrent.locks.AbstractQueuedSynchronizer$ExclusiveNode` | `java.util.concurrent.locks.AbstractQueuedSynchronizer.tryInitializeHead()Ljava/util/concurrent/locks/AbstractQueuedSynchronizer$Node;` |
| javaThreadId=26 | 2026-11-17T14:25:49.721816574+01:00 | 881,776 | False | `java.lang.Object` | `org.eclipse.rdf4j.sail.lmdb.LmdbSailStore.processAsyncValueCleanup([JILorg/eclipse/rdf4j/sail/lmdb/ValueStore$CleanupReservation;)Z` |

## Retained-event summary

Input: `/workspace/scratch/966a0d01c43b/rdf4j/binary-writer-round2/profiles/published-query-heartbeat-pid71-2026-11-17T13_25_42.492662155Z-14644002924524027315.events.json`

Selection is a benchmark-thread match **or** a writer/benchmark method anywhere in the recorded stack.

| Measure | All events | Selected events | Selected share |
| --- | ---: | ---: | ---: |
| Execution samples | 868 | 868 | 100.00% |
| Allocation samples | 3,054 | 3,050 | 99.87% |
| Allocation weight, bytes | 7,423,621,160 | 7,423,616,136 | 100.00% |

Selected CPU samples with stacks: 868; marked truncated: 0. Selected allocation samples with stacks: 3,050; marked truncated: 0.

## CPU self attribution

| Method (JVM descriptor retained) | Samples | Selected share |
| --- | ---: | ---: |
| `org.eclipse.rdf4j.sail.lmdb.evaluation.LmdbNativePackedFtree$Planner$OrderSearch.childLanes(IID)D` | 83 | 9.56% |
| `org.eclipse.rdf4j.sail.lmdb.LmdbFanOutStats.estimateDistinct([J)D` | 35 | 4.03% |
| `java.util.IdentityHashMap.put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;` | 30 | 3.46% |
| `java.lang.invoke.VarHandleLongs$FieldInstanceReadWrite.weakCompareAndSetRelease(Ljava/lang/invoke/VarHandle;Ljava/lang/Object;JJ)Z` | 29 | 3.34% |
| `org.eclipse.rdf4j.http.client.QueryResponseHeartbeat.writePayload([BII)V` | 25 | 2.88% |
| `org.eclipse.rdf4j.sail.lmdb.LmdbStoreConnection.executeObservedRead(Lorg/eclipse/rdf4j/sail/helpers/AbstractSailConnection$ReadOperation;Lorg/eclipse/rdf4j/sail/lmdb/LmdbStoreConnection$ReplayableIteration;)Ljava/lang/Object;` | 24 | 2.77% |
| `org.eclipse.rdf4j.sail.lmdb.RepositoryValueIteration.resolveWaves(Ljava/util/List;)V` | 19 | 2.19% |
| `java.lang.String.equals(Ljava/lang/Object;)Z` | 15 | 1.73% |
| `java.lang.ThreadLocal$ThreadLocalMap.getEntry(Ljava/lang/ThreadLocal;)Ljava/lang/ThreadLocal$ThreadLocalMap$Entry;` | 13 | 1.50% |
| `org.eclipse.rdf4j.http.client.QueryResponseHeartbeat.stopProbingLocked()V` | 12 | 1.38% |
| `java.lang.ThreadLocal$ThreadLocalMap.cleanSomeSlots(II)Z` | 11 | 1.27% |
| `org.eclipse.rdf4j.sail.lmdb.RepositoryValueIteration.drainIfIdle()V` | 11 | 1.27% |
| `java.io.ByteArrayOutputStream.write([BII)V` | 10 | 1.15% |
| `java.lang.ThreadLocal.get()Ljava/lang/Object;` | 10 | 1.15% |
| `java.lang.ref.Reference.<init>(Ljava/lang/Object;Ljava/lang/ref/ReferenceQueue;)V` | 10 | 1.15% |
| `java.util.HashMap.getNode(Ljava/lang/Object;)Ljava/util/HashMap$Node;` | 10 | 1.15% |
| `org.eclipse.rdf4j.http.client.QueryExecutionContext.shouldCheckpoint()Z` | 10 | 1.15% |
| `org.eclipse.rdf4j.sail.lmdb.ValueStore.isWriteTransactionOwner()Z` | 10 | 1.15% |
| `org.eclipse.rdf4j.sail.lmdb.LmdbStoreConnection$ReplayableIteration.trackedCall(Lorg/eclipse/rdf4j/sail/helpers/AbstractSailConnection$ReadOperation;)Ljava/lang/Object;` | 8 | 0.92% |
| `org.eclipse.rdf4j.sail.lmdb.RepositoryValueIteration.captureReadyOwners(Lorg/eclipse/rdf4j/model/Value;Ljava/util/Map;)V` | 8 | 0.92% |
| `org.eclipse.rdf4j.sail.lmdb.ValueStore.getLazyValue(Lorg/eclipse/rdf4j/sail/lmdb/ValueStore$ReadSnapshot;JI)Lorg/eclipse/rdf4j/sail/lmdb/model/LmdbValue;` | 8 | 0.92% |
| `java.io.DataOutputStream.write([BII)V` | 7 | 0.81% |
| `java.lang.ThreadLocal.setInitialValue(Ljava/lang/Thread;)Ljava/lang/Object;` | 7 | 0.81% |
| `java.util.HashMap.putVal(ILjava/lang/Object;Ljava/lang/Object;ZZ)Ljava/lang/Object;` | 7 | 0.81% |
| `java.util.concurrent.atomic.AtomicLong.incrementAndGet()J` | 7 | 0.81% |

## CPU inclusive attribution

| Method (JVM descriptor retained) | Samples | Selected share |
| --- | ---: | ---: |
| `java.lang.Thread.run()V` | 868 | 100.00% |
| `java.lang.Thread.runWith(Ljava/lang/Object;Ljava/lang/Runnable;)V` | 868 | 100.00% |
| `java.lang.invoke.DirectMethodHandle$Holder.invokeSpecial(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;` | 868 | 100.00% |
| `java.lang.invoke.LambdaForm$MH.0x000000006f05d400.invoke(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;` | 868 | 100.00% |
| `java.lang.invoke.LambdaForm$MH.0x000000006f05d800.invokeExact_MT(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;` | 868 | 100.00% |
| `java.lang.reflect.Method.invoke(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;` | 868 | 100.00% |
| `java.util.concurrent.Executors$RunnableAdapter.call()Ljava/lang/Object;` | 868 | 100.00% |
| `java.util.concurrent.FutureTask.run()V` | 868 | 100.00% |
| `java.util.concurrent.ThreadPoolExecutor$Worker.run()V` | 868 | 100.00% |
| `java.util.concurrent.ThreadPoolExecutor.runWorker(Ljava/util/concurrent/ThreadPoolExecutor$Worker;)V` | 868 | 100.00% |
| `jdk.internal.reflect.DirectMethodHandleAccessor.invoke(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;` | 868 | 100.00% |
| `jdk.internal.reflect.DirectMethodHandleAccessor.invokeImpl(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;` | 868 | 100.00% |
| `org.eclipse.rdf4j.query.resultio.binary.benchmark.jmh_generated.BinaryWriterBenchmark_serialize_jmhTest.serialize_AverageTime(Lorg/openjdk/jmh/runner/InfraControl;Lorg/openjdk/jmh/infra/ThreadParams;)Lorg/openjdk/jmh/results/BenchmarkTaskResult;` | 868 | 100.00% |
| `org.openjdk.jmh.runner.BenchmarkHandler$BenchmarkTask.call()Ljava/lang/Object;` | 868 | 100.00% |
| `org.openjdk.jmh.runner.BenchmarkHandler$BenchmarkTask.call()Lorg/openjdk/jmh/results/BenchmarkTaskResult;` | 868 | 100.00% |
| `org.eclipse.rdf4j.query.resultio.binary.benchmark.BinaryWriterBenchmark.serialize(Lorg/openjdk/jmh/infra/Blackhole;)I` | 867 | 99.88% |
| `org.eclipse.rdf4j.query.resultio.binary.benchmark.BinaryWriterBenchmark.writeDocument()V` | 867 | 99.88% |
| `org.eclipse.rdf4j.query.resultio.binary.benchmark.jmh_generated.BinaryWriterBenchmark_serialize_jmhTest.serialize_avgt_jmhStub(Lorg/openjdk/jmh/runner/InfraControl;Lorg/openjdk/jmh/results/RawResults;Lorg/openjdk/jmh/infra/BenchmarkParams;Lorg/openjdk/jmh/infra/IterationParams;Lorg/openjdk/jmh/infra/ThreadParams;Lorg/openjdk/jmh/infra/Blackhole;Lorg/openjdk/jmh/infra/Control;ILorg/eclipse/rdf4j/query/resultio/binary/benchmark/jmh_generated/BinaryWriterBenchmark_jmhType;)V` | 867 | 99.88% |
| `org.eclipse.rdf4j.common.iteration.IterationWrapper.hasNext()Z` | 685 | 78.92% |
| `org.eclipse.rdf4j.sail.helpers.CleanerIteration.hasNext()Z` | 685 | 78.92% |
| `org.eclipse.rdf4j.sail.helpers.SailBaseIteration.hasNext()Z` | 684 | 78.80% |
| `org.eclipse.rdf4j.sail.lmdb.RepositoryValueIteration.hasNext()Z` | 682 | 78.57% |
| `org.eclipse.rdf4j.sail.lmdb.RepositoryValueIteration.prepareNext()Z` | 669 | 77.07% |
| `org.eclipse.rdf4j.sail.lmdb.LmdbStoreConnection$ReplayableIteration.trackedCall(Lorg/eclipse/rdf4j/sail/helpers/AbstractSailConnection$ReadOperation;)Ljava/lang/Object;` | 475 | 54.72% |
| `org.eclipse.rdf4j.sail.lmdb.LmdbStoreConnection.executeTrackedRead(Lorg/eclipse/rdf4j/sail/helpers/AbstractSailConnection$ReadOperation;Lorg/eclipse/rdf4j/sail/lmdb/LmdbStoreConnection$ReplayableIteration;)Ljava/lang/Object;` | 467 | 53.80% |

## Sampled allocation by class

| Class | Weight, MiB | Selected share |
| --- | ---: | ---: |
| `java.lang.Object[]` | 683.490 | 9.65% |
| `org.eclipse.rdf4j.sail.lmdb.ValueStoreRevision$Lazy` | 674.682 | 9.53% |
| `org.eclipse.rdf4j.sail.lmdb.evaluation.LmdbNativePackedFtree$EdgePlan` | 664.248 | 9.38% |
| `int[]` | 603.993 | 8.53% |
| `byte[]` | 493.851 | 6.98% |
| `java.util.LinkedHashMap$Entry` | 391.453 | 5.53% |
| `double[]` | 284.737 | 4.02% |
| `java.util.LinkedHashMap$LinkedEntrySet` | 248.173 | 3.51% |
| `org.eclipse.rdf4j.query.impl.SimpleBinding` | 247.701 | 3.50% |
| `java.util.ArrayList` | 220.276 | 3.11% |
| `java.util.LinkedHashMap` | 211.243 | 2.98% |
| `org.eclipse.rdf4j.sail.lmdb.RepositoryValueIteration$$Lambda.0x000000006f3633b0` | 199.268 | 2.81% |
| `org.eclipse.rdf4j.sail.lmdb.model.LmdbValue$InternalIdentity` | 193.781 | 2.74% |
| `java.util.HashMap$Node[]` | 191.281 | 2.70% |
| `org.eclipse.rdf4j.sail.lmdb.ValueResolutionScope$$Lambda.0x000000006f247318` | 190.281 | 2.69% |
| `org.eclipse.rdf4j.sail.lmdb.LmdbStoreConnection$$Lambda.0x000000006f34b6a0` | 186.285 | 2.63% |
| `java.lang.ThreadLocal$ThreadLocalMap$Entry` | 182.734 | 2.58% |
| `org.eclipse.rdf4j.sail.lmdb.RepositoryValueIteration$$Lambda.0x000000006f362bf0` | 179.791 | 2.54% |
| `long[]` | 179.646 | 2.54% |
| `org.eclipse.rdf4j.sail.lmdb.evaluation.LmdbNativePackedFtree$Planner$TreeChoice` | 108.256 | 1.53% |
| `org.eclipse.rdf4j.sail.lmdb.RepositoryValueIteration$$Lambda.0x000000006f366428` | 90.384 | 1.28% |
| `org.eclipse.rdf4j.sail.lmdb.ValueResolutionScope$RevisionKey` | 81.408 | 1.15% |
| `org.eclipse.rdf4j.http.client.QueryExecutionContext$$Lambda.0x000000006f248d40` | 57.936 | 0.82% |
| `org.eclipse.rdf4j.sail.lmdb.model.LmdbLiteral` | 57.933 | 0.82% |
| `org.eclipse.rdf4j.model.Value[]` | 46.436 | 0.66% |

## Sampled allocation by top frame

| Method (JVM descriptor retained) | Weight, MiB | Selected share |
| --- | ---: | ---: |
| `java.lang.invoke.DirectMethodHandle.allocateInstance(Ljava/lang/Object;)Ljava/lang/Object;` | 1,013.802 | 14.32% |
| `org.eclipse.rdf4j.sail.lmdb.evaluation.LmdbNativePackedFtree$Planner$OrderSearch.recurse(IJ[I[I[I[DDI)V` | 683.477 | 9.65% |
| `org.eclipse.rdf4j.sail.lmdb.ValueStore.positionedRevision(Lorg/eclipse/rdf4j/sail/lmdb/ValueStoreRevision;I)Lorg/eclipse/rdf4j/sail/lmdb/ValueStoreRevision;` | 674.682 | 9.53% |
| `org.eclipse.rdf4j.sail.lmdb.evaluation.LmdbNativePackedFtree$Planner.orient(Lorg/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativePackedFtree$EdgePlan;II)Lorg/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativePackedFtree$EdgePlan;` | 664.248 | 9.38% |
| `java.util.LinkedHashMap.newNode(ILjava/lang/Object;Ljava/lang/Object;Ljava/util/HashMap$Node;)Ljava/util/HashMap$Node;` | 391.453 | 5.53% |
| `java.util.ArrayDeque.<init>()V` | 390.858 | 5.52% |
| `java.util.Arrays.copyOfRange([BII)[B` | 330.039 | 4.66% |
| `java.util.LinkedHashMap.sequencedEntrySet()Ljava/util/SequencedSet;` | 248.173 | 3.51% |
| `org.eclipse.rdf4j.query.MutableBindingSet.addBinding(Ljava/lang/String;Lorg/eclipse/rdf4j/model/Value;)V` | 247.701 | 3.50% |
| `java.util.IdentityHashMap.init(I)V` | 206.231 | 2.91% |
| `org.eclipse.rdf4j.sail.lmdb.model.LmdbLiteral.setInternalID(JLorg/eclipse/rdf4j/sail/lmdb/ValueStoreRevision;)V` | 193.781 | 2.74% |
| `java.util.HashMap.resize()[Ljava/util/HashMap$Node;` | 191.281 | 2.70% |
| `java.lang.ThreadLocal$ThreadLocalMap.set(Ljava/lang/ThreadLocal;Ljava/lang/Object;)V` | 182.734 | 2.58% |
| `org.eclipse.rdf4j.sail.lmdb.RepositoryValueIteration.prepareNext()Z` | 174.299 | 2.46% |
| `org.eclipse.rdf4j.query.impl.MapBindingSet.<init>(I)V` | 157.304 | 2.22% |
| `java.math.BigInteger.smallToString(ILjava/lang/StringBuilder;I)V` | 151.258 | 2.14% |
| `java.lang.AbstractStringBuilder.<init>(Ljava/lang/String;)V` | 141.339 | 2.00% |
| `org.eclipse.rdf4j.sail.lmdb.evaluation.LmdbNativePackedFtree$Planner$OrderSearch.consider([I[I[DDI)V` | 137.716 | 1.95% |
| `org.eclipse.rdf4j.sail.lmdb.evaluation.LmdbNativeCostVector.<init>([D[D[DZ)V` | 112.366 | 1.59% |
| `org.eclipse.rdf4j.sail.lmdb.RepositoryValueIteration.resolveWaves(Ljava/util/List;)V` | 105.379 | 1.49% |
| `org.eclipse.rdf4j.sail.lmdb.ValueResolutionScope.revision(Lorg/eclipse/rdf4j/sail/lmdb/ValueStoreRevision$Lazy;)Lorg/eclipse/rdf4j/sail/lmdb/ValueStoreRevision$Lazy;` | 81.408 | 1.15% |
| `java.util.ArrayList.grow(I)[Ljava/lang/Object;` | 80.409 | 1.14% |
| `org.eclipse.rdf4j.sail.lmdb.evaluation.NativeProjectedBindingSet.<init>(Lorg/eclipse/rdf4j/sail/lmdb/evaluation/NativeLmdbQuerySource;Lorg/eclipse/rdf4j/sail/lmdb/evaluation/NativeProjectedBindingSet$ProjectionLayout;[J)V` | 67.242 | 0.95% |
| `org.eclipse.rdf4j.sail.lmdb.ValueStore.createLazyValue(JLorg/eclipse/rdf4j/sail/lmdb/ValueStoreRevision;ZLorg/eclipse/rdf4j/sail/lmdb/ValueResolutionScope;Lorg/eclipse/rdf4j/sail/lmdb/ValueStore$ValueCacheEpoch;ZILorg/eclipse/rdf4j/sail/lmdb/ValueStore$CanonicalValueEntry;)Lorg/eclipse/rdf4j/sail/lmdb/model/LmdbValue;` | 57.933 | 0.82% |
| `org.eclipse.rdf4j.sail.lmdb.LmdbStoreConnection.executeObservedRead(Lorg/eclipse/rdf4j/sail/helpers/AbstractSailConnection$ReadOperation;Lorg/eclipse/rdf4j/sail/lmdb/LmdbStoreConnection$ReplayableIteration;)Ljava/lang/Object;` | 44.450 | 0.63% |

## Sampled allocation inclusive attribution

| Method (JVM descriptor retained) | Weight, MiB | Selected share |
| --- | ---: | ---: |
| `java.lang.Thread.run()V` | 7,079.712 | 100.00% |
| `java.lang.Thread.runWith(Ljava/lang/Object;Ljava/lang/Runnable;)V` | 7,079.712 | 100.00% |
| `java.lang.invoke.DirectMethodHandle$Holder.invokeSpecial(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;` | 7,079.712 | 100.00% |
| `java.lang.invoke.LambdaForm$MH.0x000000006f05d400.invoke(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;` | 7,079.712 | 100.00% |
| `java.lang.invoke.LambdaForm$MH.0x000000006f05d800.invokeExact_MT(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;` | 7,079.712 | 100.00% |
| `java.lang.reflect.Method.invoke(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;` | 7,079.712 | 100.00% |
| `java.util.concurrent.Executors$RunnableAdapter.call()Ljava/lang/Object;` | 7,079.712 | 100.00% |
| `java.util.concurrent.FutureTask.run()V` | 7,079.712 | 100.00% |
| `java.util.concurrent.ThreadPoolExecutor$Worker.run()V` | 7,079.712 | 100.00% |
| `java.util.concurrent.ThreadPoolExecutor.runWorker(Ljava/util/concurrent/ThreadPoolExecutor$Worker;)V` | 7,079.712 | 100.00% |
| `jdk.internal.reflect.DirectMethodHandleAccessor.invoke(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;` | 7,079.712 | 100.00% |
| `jdk.internal.reflect.DirectMethodHandleAccessor.invokeImpl(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;` | 7,079.712 | 100.00% |
| `org.eclipse.rdf4j.query.resultio.binary.benchmark.BinaryWriterBenchmark.serialize(Lorg/openjdk/jmh/infra/Blackhole;)I` | 7,079.712 | 100.00% |
| `org.eclipse.rdf4j.query.resultio.binary.benchmark.BinaryWriterBenchmark.writeDocument()V` | 7,079.712 | 100.00% |
| `org.eclipse.rdf4j.query.resultio.binary.benchmark.jmh_generated.BinaryWriterBenchmark_serialize_jmhTest.serialize_AverageTime(Lorg/openjdk/jmh/runner/InfraControl;Lorg/openjdk/jmh/infra/ThreadParams;)Lorg/openjdk/jmh/results/BenchmarkTaskResult;` | 7,079.712 | 100.00% |
| `org.eclipse.rdf4j.query.resultio.binary.benchmark.jmh_generated.BinaryWriterBenchmark_serialize_jmhTest.serialize_avgt_jmhStub(Lorg/openjdk/jmh/runner/InfraControl;Lorg/openjdk/jmh/results/RawResults;Lorg/openjdk/jmh/infra/BenchmarkParams;Lorg/openjdk/jmh/infra/IterationParams;Lorg/openjdk/jmh/infra/ThreadParams;Lorg/openjdk/jmh/infra/Blackhole;Lorg/openjdk/jmh/infra/Control;ILorg/eclipse/rdf4j/query/resultio/binary/benchmark/jmh_generated/BinaryWriterBenchmark_jmhType;)V` | 7,079.712 | 100.00% |
| `org.openjdk.jmh.runner.BenchmarkHandler$BenchmarkTask.call()Ljava/lang/Object;` | 7,079.712 | 100.00% |
| `org.openjdk.jmh.runner.BenchmarkHandler$BenchmarkTask.call()Lorg/openjdk/jmh/results/BenchmarkTaskResult;` | 7,079.712 | 100.00% |
| `org.eclipse.rdf4j.common.iteration.IterationWrapper.hasNext()Z` | 6,699.234 | 94.63% |
| `org.eclipse.rdf4j.sail.helpers.CleanerIteration.hasNext()Z` | 6,699.234 | 94.63% |
| `org.eclipse.rdf4j.sail.helpers.SailBaseIteration.hasNext()Z` | 6,699.234 | 94.63% |
| `org.eclipse.rdf4j.sail.lmdb.RepositoryValueIteration.hasNext()Z` | 6,699.234 | 94.63% |
| `org.eclipse.rdf4j.sail.lmdb.RepositoryValueIteration.prepareNext()Z` | 6,699.234 | 94.63% |
| `org.eclipse.rdf4j.sail.lmdb.LmdbStoreConnection$ReplayableIteration.trackedCall(Lorg/eclipse/rdf4j/sail/helpers/AbstractSailConnection$ReadOperation;)Ljava/lang/Object;` | 3,318.101 | 46.87% |
| `org.eclipse.rdf4j.sail.lmdb.LmdbStoreConnection.executeTrackedRead(Lorg/eclipse/rdf4j/sail/helpers/AbstractSailConnection$ReadOperation;Lorg/eclipse/rdf4j/sail/lmdb/LmdbStoreConnection$ReplayableIteration;)Ljava/lang/Object;` | 3,318.101 | 46.87% |

## Interpretation

- CPU percentages are fractions of selected execution samples, not wall-clock time.
- Allocation weight estimates bytes represented by samples; sample counts are not object counts.
- Inclusive methods are deduplicated per stack; their percentages overlap and need not sum to 100%.
- Self means the top recorded Java frame, not an exact native instruction or allocation count.
- Missing-stack selected events remain in denominators; method tables omit those events.
- The default selection covers benchmark worker threads and writer stacks, not arbitrary asynchronous query workers.
- Print JFR with --stack-depth 128; the default five-frame display can hide callers even when truncated=false.
- This separate sensitivity report omits each thread's first allocation sample; raw recordings and summaries remain authoritative evidence.
- OpenJDK's first allocation sample can carry cumulative pre-recording weight; omission also discards some valid post-start allocations.
- Remaining allocation attribution is sampled and throttled, not exact; totals are not normalized by rows or iterations.
- CPU events and JMH results are unchanged; use JMH gc.alloc.rate.norm for measured absolute B/row.
- The same first-sample caveat applies to the previous round's allocation percentages from this profiler, without changing its CPU or JMH results.
