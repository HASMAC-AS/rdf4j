# JFR allocation first-sample sensitivity analysis

Approximate corrected distribution: the first allocation sample per recorded thread ID is omitted. This also drops some valid post-start allocation weight. CPU events are unchanged; JMH remains the authority for measured B/row. Keep the raw summary alongside this report.

Omitted selected allocation weight: 3,162,419,016 bytes (26.69% of the raw selected weight).

## Every omitted allocation sample

| Thread ID | Timestamp | Weight, bytes | Selected | Allocated class | Top recorded frame |
| --- | --- | ---: | --- | --- | --- |
| javaThreadId=23 | 2026-11-17T14:26:00.919456178+01:00 | 3,162,419,016 | True | `java.lang.Object[]` | `java.util.ImmutableCollections$SetN.<init>([Ljava/lang/Object;)V` |
| javaThreadId=18 | 2026-11-17T14:26:01.153297037+01:00 | 17,272 | False | `java.lang.String` | `<no stack>` |
| javaThreadId=3 | 2026-11-17T14:26:04.944684210+01:00 | 23,101,328 | False | `java.util.concurrent.locks.AbstractQueuedSynchronizer$ExclusiveNode` | `java.util.concurrent.locks.AbstractQueuedSynchronizer.tryInitializeHead()Ljava/util/concurrent/locks/AbstractQueuedSynchronizer$Node;` |
| javaThreadId=38 | 2026-11-17T14:26:05.901435103+01:00 | 64,360 | False | `java.util.ArrayList$Itr` | `java.util.ArrayList.iterator()Ljava/util/Iterator;` |
| javaThreadId=26 | 2026-11-17T14:26:10.640641146+01:00 | 882,464 | False | `java.lang.Object` | `org.eclipse.rdf4j.sail.lmdb.LmdbSailStore.processAsyncValueCleanup([JILorg/eclipse/rdf4j/sail/lmdb/ValueStore$CleanupReservation;)Z` |

## Retained-event summary

Input: `/workspace/scratch/966a0d01c43b/rdf4j/binary-writer-round2/profiles/current-query-heartbeat-pid113-2026-11-17T13_26_00.839347787Z-12637277934331132499.events.json`

Selection is a benchmark-thread match **or** a writer/benchmark method anywhere in the recorded stack.

| Measure | All events | Selected events | Selected share |
| --- | ---: | ---: | ---: |
| Execution samples | 846 | 846 | 100.00% |
| Allocation samples | 3,042 | 3,038 | 99.87% |
| Allocation weight, bytes | 8,687,630,760 | 8,687,621,216 | 100.00% |

Selected CPU samples with stacks: 846; marked truncated: 0. Selected allocation samples with stacks: 3,038; marked truncated: 0.

## CPU self attribution

| Method (JVM descriptor retained) | Samples | Selected share |
| --- | ---: | ---: |
| `org.eclipse.rdf4j.sail.lmdb.evaluation.LmdbNativePackedFtree$Planner$OrderSearch.childLanes(IID)D` | 95 | 11.23% |
| `org.eclipse.rdf4j.sail.lmdb.LmdbFanOutStats.estimateDistinct([J)D` | 49 | 5.79% |
| `java.lang.invoke.VarHandleLongs$FieldInstanceReadWrite.weakCompareAndSetRelease(Ljava/lang/invoke/VarHandle;Ljava/lang/Object;JJ)Z` | 32 | 3.78% |
| `java.util.IdentityHashMap.put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;` | 31 | 3.66% |
| `org.eclipse.rdf4j.sail.lmdb.ValueStore.getLazyValue(Lorg/eclipse/rdf4j/sail/lmdb/ValueStore$ReadSnapshot;JI)Lorg/eclipse/rdf4j/sail/lmdb/model/LmdbValue;` | 24 | 2.84% |
| `org.eclipse.rdf4j.sail.lmdb.LmdbStoreConnection.executeObservedRead(Lorg/eclipse/rdf4j/sail/helpers/AbstractSailConnection$ReadOperation;Lorg/eclipse/rdf4j/sail/lmdb/LmdbStoreConnection$ReplayableIteration;)Ljava/lang/Object;` | 23 | 2.72% |
| `java.lang.ref.Reference.<init>(Ljava/lang/Object;Ljava/lang/ref/ReferenceQueue;)V` | 17 | 2.01% |
| `java.lang.ThreadLocal.get()Ljava/lang/Object;` | 15 | 1.77% |
| `org.eclipse.rdf4j.sail.lmdb.LmdbFanOutStats.meanFanOut(JZ)Ljava/util/OptionalDouble;` | 14 | 1.65% |
| `java.lang.ThreadLocal$ThreadLocalMap.cleanSomeSlots(II)Z` | 13 | 1.54% |
| `org.eclipse.rdf4j.sail.lmdb.RepositoryValueIteration.captureReadyOwners(Lorg/eclipse/rdf4j/model/Value;Ljava/util/Map;)V` | 12 | 1.42% |
| `java.lang.ThreadLocal$ThreadLocalMap.getEntry(Ljava/lang/ThreadLocal;)Ljava/lang/ThreadLocal$ThreadLocalMap$Entry;` | 11 | 1.30% |
| `org.eclipse.rdf4j.sail.lmdb.RepositoryValueIteration.resolveWaves(Ljava/util/List;)V` | 11 | 1.30% |
| `java.lang.ThreadLocal$ThreadLocalMap.set(Ljava/lang/ThreadLocal;Ljava/lang/Object;)V` | 10 | 1.18% |
| `java.util.HashMap.putVal(ILjava/lang/Object;Ljava/lang/Object;ZZ)Ljava/lang/Object;` | 10 | 1.18% |
| `org.eclipse.rdf4j.sail.lmdb.LmdbStoreConnection$ReplayableIteration.trackedCall(Lorg/eclipse/rdf4j/sail/helpers/AbstractSailConnection$ReadOperation;)Ljava/lang/Object;` | 10 | 1.18% |
| `java.util.concurrent.atomic.AtomicLong.incrementAndGet()J` | 9 | 1.06% |
| `org.eclipse.rdf4j.http.client.QueryResponseHeartbeat.writePayload([BII)V` | 9 | 1.06% |
| `org.eclipse.rdf4j.sail.lmdb.RepositoryValueIteration.drainIfIdle()V` | 9 | 1.06% |
| `java.io.DataOutputStream.write([BII)V` | 8 | 0.95% |
| `java.util.IdentityHashMap.hash(Ljava/lang/Object;I)I` | 8 | 0.95% |
| `org.eclipse.rdf4j.sail.lmdb.ValueStore$CanonicalValueEntry.isCurrent()Z` | 8 | 0.95% |
| `org.eclipse.rdf4j.query.resultio.binary.BinaryQueryResultWriter.handleSolutionImpl(Lorg/eclipse/rdf4j/query/BindingSet;)V` | 7 | 0.83% |
| `java.lang.String.equals(Ljava/lang/Object;)Z` | 6 | 0.71% |
| `java.lang.ThreadLocal.setInitialValue(Ljava/lang/Thread;)Ljava/lang/Object;` | 6 | 0.71% |

## CPU inclusive attribution

| Method (JVM descriptor retained) | Samples | Selected share |
| --- | ---: | ---: |
| `java.lang.Thread.run()V` | 846 | 100.00% |
| `java.lang.Thread.runWith(Ljava/lang/Object;Ljava/lang/Runnable;)V` | 846 | 100.00% |
| `java.lang.invoke.DirectMethodHandle$Holder.invokeSpecial(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;` | 846 | 100.00% |
| `java.lang.invoke.LambdaForm$MH.0x000000007305d400.invoke(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;` | 846 | 100.00% |
| `java.lang.invoke.LambdaForm$MH.0x000000007305d800.invokeExact_MT(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;` | 846 | 100.00% |
| `java.lang.reflect.Method.invoke(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;` | 846 | 100.00% |
| `java.util.concurrent.Executors$RunnableAdapter.call()Ljava/lang/Object;` | 846 | 100.00% |
| `java.util.concurrent.FutureTask.run()V` | 846 | 100.00% |
| `java.util.concurrent.ThreadPoolExecutor$Worker.run()V` | 846 | 100.00% |
| `java.util.concurrent.ThreadPoolExecutor.runWorker(Ljava/util/concurrent/ThreadPoolExecutor$Worker;)V` | 846 | 100.00% |
| `jdk.internal.reflect.DirectMethodHandleAccessor.invoke(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;` | 846 | 100.00% |
| `jdk.internal.reflect.DirectMethodHandleAccessor.invokeImpl(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;` | 846 | 100.00% |
| `org.eclipse.rdf4j.query.resultio.binary.benchmark.jmh_generated.BinaryWriterBenchmark_serialize_jmhTest.serialize_AverageTime(Lorg/openjdk/jmh/runner/InfraControl;Lorg/openjdk/jmh/infra/ThreadParams;)Lorg/openjdk/jmh/results/BenchmarkTaskResult;` | 846 | 100.00% |
| `org.openjdk.jmh.runner.BenchmarkHandler$BenchmarkTask.call()Ljava/lang/Object;` | 846 | 100.00% |
| `org.openjdk.jmh.runner.BenchmarkHandler$BenchmarkTask.call()Lorg/openjdk/jmh/results/BenchmarkTaskResult;` | 846 | 100.00% |
| `org.eclipse.rdf4j.query.resultio.binary.benchmark.BinaryWriterBenchmark.serialize(Lorg/openjdk/jmh/infra/Blackhole;)I` | 844 | 99.76% |
| `org.eclipse.rdf4j.query.resultio.binary.benchmark.BinaryWriterBenchmark.writeDocument()V` | 844 | 99.76% |
| `org.eclipse.rdf4j.query.resultio.binary.benchmark.jmh_generated.BinaryWriterBenchmark_serialize_jmhTest.serialize_avgt_jmhStub(Lorg/openjdk/jmh/runner/InfraControl;Lorg/openjdk/jmh/results/RawResults;Lorg/openjdk/jmh/infra/BenchmarkParams;Lorg/openjdk/jmh/infra/IterationParams;Lorg/openjdk/jmh/infra/ThreadParams;Lorg/openjdk/jmh/infra/Blackhole;Lorg/openjdk/jmh/infra/Control;ILorg/eclipse/rdf4j/query/resultio/binary/benchmark/jmh_generated/BinaryWriterBenchmark_jmhType;)V` | 844 | 99.76% |
| `org.eclipse.rdf4j.common.iteration.IterationWrapper.hasNext()Z` | 731 | 86.41% |
| `org.eclipse.rdf4j.sail.helpers.CleanerIteration.hasNext()Z` | 731 | 86.41% |
| `org.eclipse.rdf4j.sail.helpers.SailBaseIteration.hasNext()Z` | 731 | 86.41% |
| `org.eclipse.rdf4j.sail.lmdb.RepositoryValueIteration.hasNext()Z` | 729 | 86.17% |
| `org.eclipse.rdf4j.sail.lmdb.RepositoryValueIteration.prepareNext()Z` | 714 | 84.40% |
| `org.eclipse.rdf4j.sail.lmdb.LmdbStoreConnection$ReplayableIteration.trackedCall(Lorg/eclipse/rdf4j/sail/helpers/AbstractSailConnection$ReadOperation;)Ljava/lang/Object;` | 503 | 59.46% |
| `org.eclipse.rdf4j.sail.lmdb.LmdbStoreConnection.executeTrackedRead(Lorg/eclipse/rdf4j/sail/helpers/AbstractSailConnection$ReadOperation;Lorg/eclipse/rdf4j/sail/lmdb/LmdbStoreConnection$ReplayableIteration;)Ljava/lang/Object;` | 493 | 58.27% |

## Sampled allocation by class

| Class | Weight, MiB | Selected share |
| --- | ---: | ---: |
| `java.lang.Object[]` | 1,860.103 | 22.45% |
| `org.eclipse.rdf4j.sail.lmdb.ValueStoreRevision$Lazy` | 642.024 | 7.75% |
| `byte[]` | 418.501 | 5.05% |
| `java.lang.Runnable[]` | 388.000 | 4.68% |
| `boolean[]` | 385.055 | 4.65% |
| `double[]` | 375.619 | 4.53% |
| `org.eclipse.rdf4j.sail.lmdb.ValueResolutionScope$RevisionKey` | 360.828 | 4.36% |
| `java.util.LinkedHashMap$Entry` | 327.929 | 3.96% |
| `org.eclipse.rdf4j.http.client.QueryExecutionContext$$Lambda.0x0000000073248680` | 309.630 | 3.74% |
| `org.eclipse.rdf4j.sail.lmdb.RepositoryValueIteration$$Lambda.0x0000000073366568` | 300.114 | 3.62% |
| `java.lang.ThreadLocal$ThreadLocalMap$Entry` | 255.516 | 3.08% |
| `org.eclipse.rdf4j.model.base.AbstractLiteral$DecimalLiteral` | 234.721 | 2.83% |
| `java.math.BigDecimal` | 226.238 | 2.73% |
| `java.util.ArrayDeque` | 224.728 | 2.71% |
| `org.eclipse.rdf4j.sail.lmdb.evaluation.NativeProjectedBindingSet` | 192.274 | 2.32% |
| `java.util.LinkedHashMap` | 183.769 | 2.22% |
| `org.eclipse.rdf4j.sail.lmdb.LmdbStoreConnection$ReplayableIteration$$Lambda.0x00000000732d5c50` | 164.786 | 1.99% |
| `org.eclipse.rdf4j.sail.lmdb.LmdbStoreConnection$$Lambda.0x000000007335b4a0` | 153.324 | 1.85% |
| `org.eclipse.rdf4j.sail.lmdb.ValueResolutionScope$$Lambda.0x0000000073247528` | 147.830 | 1.78% |
| `org.eclipse.rdf4j.query.impl.SimpleBinding` | 142.335 | 1.72% |
| `org.eclipse.rdf4j.sail.lmdb.evaluation.LmdbNativePackedFtree$Planner$TreeChoice` | 105.836 | 1.28% |
| `java.lang.String` | 83.331 | 1.01% |
| `int[]` | 81.404 | 0.98% |
| `org.eclipse.rdf4j.sail.features.SailResultValueExtractor$1$$Lambda.0x000000007335ba98` | 76.890 | 0.93% |
| `org.eclipse.rdf4j.sail.lmdb.evaluation.LmdbNativePackedFtree$EdgePlan` | 69.402 | 0.84% |

## Sampled allocation by top frame

| Method (JVM descriptor retained) | Weight, MiB | Selected share |
| --- | ---: | ---: |
| `java.lang.invoke.DirectMethodHandle.allocateInstance(Ljava/lang/Object;)Ljava/lang/Object;` | 1,153.097 | 13.92% |
| `java.util.IdentityHashMap.init(I)V` | 787.877 | 9.51% |
| `java.util.ArrayDeque.<init>()V` | 731.638 | 8.83% |
| `org.eclipse.rdf4j.sail.lmdb.ValueStore.positionedRevision(Lorg/eclipse/rdf4j/sail/lmdb/ValueStoreRevision;I)Lorg/eclipse/rdf4j/sail/lmdb/ValueStoreRevision;` | 642.024 | 7.75% |
| `org.eclipse.rdf4j.sail.lmdb.LmdbStoreConnection.executeObservedRead(Lorg/eclipse/rdf4j/sail/helpers/AbstractSailConnection$ReadOperation;Lorg/eclipse/rdf4j/sail/lmdb/LmdbStoreConnection$ReplayableIteration;)Ljava/lang/Object;` | 388.000 | 4.68% |
| `org.eclipse.rdf4j.sail.lmdb.RepositoryValueIteration.isPrepared(Lorg/eclipse/rdf4j/model/Value;)Z` | 379.561 | 4.58% |
| `java.util.Arrays.copyOfRange([BII)[B` | 373.560 | 4.51% |
| `org.eclipse.rdf4j.sail.lmdb.ValueResolutionScope.revision(Lorg/eclipse/rdf4j/sail/lmdb/ValueStoreRevision$Lazy;)Lorg/eclipse/rdf4j/sail/lmdb/ValueStoreRevision$Lazy;` | 360.828 | 4.36% |
| `java.util.ArrayList.grow(I)[Ljava/lang/Object;` | 339.091 | 4.09% |
| `java.util.LinkedHashMap.newNode(ILjava/lang/Object;Ljava/lang/Object;Ljava/util/HashMap$Node;)Ljava/util/HashMap$Node;` | 327.929 | 3.96% |
| `java.lang.ThreadLocal$ThreadLocalMap.set(Ljava/lang/ThreadLocal;Ljava/lang/Object;)V` | 255.516 | 3.08% |
| `org.eclipse.rdf4j.model.base.AbstractValueFactory.createLiteral(Ljava/math/BigDecimal;)Lorg/eclipse/rdf4j/model/Literal;` | 234.721 | 2.83% |
| `org.eclipse.rdf4j.sail.lmdb.inlined.Decimals.unpackDecimal(JLorg/eclipse/rdf4j/model/ValueFactory;)Lorg/eclipse/rdf4j/model/Literal;` | 226.238 | 2.73% |
| `org.eclipse.rdf4j.sail.lmdb.RepositoryValueIteration.captureReadyOwners(Lorg/eclipse/rdf4j/model/Value;Ljava/util/Map;)V` | 224.728 | 2.71% |
| `org.eclipse.rdf4j.sail.lmdb.evaluation.LmdbNativePackedFtree$Planner$OrderSearch.recurse(IJ[I[I[I[DDI)V` | 220.245 | 2.66% |
| `org.eclipse.rdf4j.sail.lmdb.RepositoryValueIteration.resolveWaves(Ljava/util/List;)V` | 215.733 | 2.60% |
| `org.eclipse.rdf4j.sail.lmdb.evaluation.NativeRowsStep.project([JLorg/eclipse/rdf4j/sail/lmdb/evaluation/AggContext;Lorg/eclipse/rdf4j/sail/lmdb/evaluation/NativeProjectedBindingSet$ProjectionLayout;)Lorg/eclipse/rdf4j/query/BindingSet;` | 192.274 | 2.32% |
| `org.eclipse.rdf4j.sail.lmdb.evaluation.LmdbNativeCostVector.<init>([D[D[DZ)V` | 164.833 | 1.99% |
| `org.eclipse.rdf4j.query.MutableBindingSet.addBinding(Ljava/lang/String;Lorg/eclipse/rdf4j/model/Value;)V` | 142.335 | 1.72% |
| `org.eclipse.rdf4j.sail.lmdb.evaluation.LmdbNativePackedFtree$Planner$OrderSearch.consider([I[I[DDI)V` | 141.791 | 1.71% |
| `jdk.internal.misc.Unsafe.allocateInstance(Ljava/lang/Class;)Ljava/lang/Object;` | 78.388 | 0.95% |
| `java.lang.StringLatin1.newString([BII)Ljava/lang/String;` | 76.339 | 0.92% |
| `org.eclipse.rdf4j.sail.lmdb.evaluation.LmdbNativePackedFtree$Planner.orient(Lorg/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativePackedFtree$EdgePlan;II)Lorg/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativePackedFtree$EdgePlan;` | 69.402 | 0.84% |
| `org.eclipse.rdf4j.sail.lmdb.model.LmdbLiteral.setInternalID(JLorg/eclipse/rdf4j/sail/lmdb/ValueStoreRevision;)V` | 65.925 | 0.80% |
| `org.eclipse.rdf4j.sail.lmdb.LmdbSailStore.enterReadResourceContext(Ljava/util/function/Supplier;)Lorg/eclipse/rdf4j/sail/base/SailClosable;` | 65.423 | 0.79% |

## Sampled allocation inclusive attribution

| Method (JVM descriptor retained) | Weight, MiB | Selected share |
| --- | ---: | ---: |
| `java.lang.Thread.run()V` | 8,285.161 | 100.00% |
| `java.lang.Thread.runWith(Ljava/lang/Object;Ljava/lang/Runnable;)V` | 8,285.161 | 100.00% |
| `java.lang.invoke.DirectMethodHandle$Holder.invokeSpecial(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;` | 8,285.161 | 100.00% |
| `java.lang.invoke.LambdaForm$MH.0x000000007305d400.invoke(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;` | 8,285.161 | 100.00% |
| `java.lang.invoke.LambdaForm$MH.0x000000007305d800.invokeExact_MT(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;` | 8,285.161 | 100.00% |
| `java.lang.reflect.Method.invoke(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;` | 8,285.161 | 100.00% |
| `java.util.concurrent.Executors$RunnableAdapter.call()Ljava/lang/Object;` | 8,285.161 | 100.00% |
| `java.util.concurrent.FutureTask.run()V` | 8,285.161 | 100.00% |
| `java.util.concurrent.ThreadPoolExecutor$Worker.run()V` | 8,285.161 | 100.00% |
| `java.util.concurrent.ThreadPoolExecutor.runWorker(Ljava/util/concurrent/ThreadPoolExecutor$Worker;)V` | 8,285.161 | 100.00% |
| `jdk.internal.reflect.DirectMethodHandleAccessor.invoke(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;` | 8,285.161 | 100.00% |
| `jdk.internal.reflect.DirectMethodHandleAccessor.invokeImpl(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;` | 8,285.161 | 100.00% |
| `org.eclipse.rdf4j.query.resultio.binary.benchmark.BinaryWriterBenchmark.serialize(Lorg/openjdk/jmh/infra/Blackhole;)I` | 8,285.161 | 100.00% |
| `org.eclipse.rdf4j.query.resultio.binary.benchmark.BinaryWriterBenchmark.writeDocument()V` | 8,285.161 | 100.00% |
| `org.eclipse.rdf4j.query.resultio.binary.benchmark.jmh_generated.BinaryWriterBenchmark_serialize_jmhTest.serialize_AverageTime(Lorg/openjdk/jmh/runner/InfraControl;Lorg/openjdk/jmh/infra/ThreadParams;)Lorg/openjdk/jmh/results/BenchmarkTaskResult;` | 8,285.161 | 100.00% |
| `org.eclipse.rdf4j.query.resultio.binary.benchmark.jmh_generated.BinaryWriterBenchmark_serialize_jmhTest.serialize_avgt_jmhStub(Lorg/openjdk/jmh/runner/InfraControl;Lorg/openjdk/jmh/results/RawResults;Lorg/openjdk/jmh/infra/BenchmarkParams;Lorg/openjdk/jmh/infra/IterationParams;Lorg/openjdk/jmh/infra/ThreadParams;Lorg/openjdk/jmh/infra/Blackhole;Lorg/openjdk/jmh/infra/Control;ILorg/eclipse/rdf4j/query/resultio/binary/benchmark/jmh_generated/BinaryWriterBenchmark_jmhType;)V` | 8,285.161 | 100.00% |
| `org.openjdk.jmh.runner.BenchmarkHandler$BenchmarkTask.call()Ljava/lang/Object;` | 8,285.161 | 100.00% |
| `org.openjdk.jmh.runner.BenchmarkHandler$BenchmarkTask.call()Lorg/openjdk/jmh/results/BenchmarkTaskResult;` | 8,285.161 | 100.00% |
| `org.eclipse.rdf4j.common.iteration.IterationWrapper.hasNext()Z` | 7,793.311 | 94.06% |
| `org.eclipse.rdf4j.sail.helpers.CleanerIteration.hasNext()Z` | 7,793.311 | 94.06% |
| `org.eclipse.rdf4j.sail.helpers.SailBaseIteration.hasNext()Z` | 7,793.311 | 94.06% |
| `org.eclipse.rdf4j.sail.lmdb.RepositoryValueIteration.hasNext()Z` | 7,793.311 | 94.06% |
| `org.eclipse.rdf4j.sail.lmdb.RepositoryValueIteration.prepareNext()Z` | 7,793.311 | 94.06% |
| `org.eclipse.rdf4j.sail.lmdb.LmdbStoreConnection$ReplayableIteration.trackedCall(Lorg/eclipse/rdf4j/sail/helpers/AbstractSailConnection$ReadOperation;)Ljava/lang/Object;` | 3,306.856 | 39.91% |
| `org.eclipse.rdf4j.sail.lmdb.LmdbStoreConnection.executeTrackedRead(Lorg/eclipse/rdf4j/sail/helpers/AbstractSailConnection$ReadOperation;Lorg/eclipse/rdf4j/sail/lmdb/LmdbStoreConnection$ReplayableIteration;)Ljava/lang/Object;` | 3,306.856 | 39.91% |

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
