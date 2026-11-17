# JFR binary writer profile summary

Input: `/workspace/scratch/966a0d01c43b/rdf4j/binary-writer-evidence/jfr-final/baseline-query-heartbeat-pid133-2026-11-17T11_58_07.586007344Z-12342234900223714368.events.json`

Selection is a benchmark-thread match **or** a writer/benchmark method anywhere in the recorded stack.

| Measure | All events | Selected events | Selected share |
| --- | ---: | ---: | ---: |
| Execution samples | 1,024 | 1,024 | 100.00% |
| Allocation samples | 3,608 | 3,602 | 99.83% |
| Allocation weight, bytes | 13,160,290,344 | 13,137,165,360 | 99.82% |

Selected CPU samples with stacks: 1,024; marked truncated: 0. Selected allocation samples with stacks: 3,602; marked truncated: 0.

## CPU self attribution

| Method (JVM descriptor retained) | Samples | Selected share |
| --- | ---: | ---: |
| `org.eclipse.rdf4j.sail.lmdb.LmdbFanOutStats.estimateDistinct([J)D` | 58 | 5.66% |
| `org.eclipse.rdf4j.sail.lmdb.evaluation.LmdbNativePackedFtree$Planner$OrderSearch.childLanes(IID)D` | 58 | 5.66% |
| `java.util.IdentityHashMap.put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;` | 37 | 3.61% |
| `java.lang.invoke.VarHandleLongs$FieldInstanceReadWrite.weakCompareAndSetRelease(Ljava/lang/invoke/VarHandle;Ljava/lang/Object;JJ)Z` | 28 | 2.73% |
| `org.eclipse.rdf4j.http.client.QueryResponseHeartbeat.writePayload(I)V` | 28 | 2.73% |
| `org.eclipse.rdf4j.http.client.QueryResponseHeartbeat.writePayload([BII)V` | 26 | 2.54% |
| `org.eclipse.rdf4j.sail.lmdb.LmdbStoreConnection.executeObservedRead(Lorg/eclipse/rdf4j/sail/helpers/AbstractSailConnection$ReadOperation;Lorg/eclipse/rdf4j/sail/lmdb/LmdbStoreConnection$ReplayableIteration;)Ljava/lang/Object;` | 21 | 2.05% |
| `java.lang.ref.Reference.<init>(Ljava/lang/Object;Ljava/lang/ref/ReferenceQueue;)V` | 20 | 1.95% |
| `java.io.ByteArrayOutputStream.write(I)V` | 19 | 1.86% |
| `org.eclipse.rdf4j.http.client.QueryResponseHeartbeat.stopProbingLocked()V` | 19 | 1.86% |
| `org.eclipse.rdf4j.sail.lmdb.ValueStore.getLazyValue(Lorg/eclipse/rdf4j/sail/lmdb/ValueStore$ReadSnapshot;JI)Lorg/eclipse/rdf4j/sail/lmdb/model/LmdbValue;` | 19 | 1.86% |
| `java.io.ByteArrayOutputStream.write([BII)V` | 17 | 1.66% |
| `org.eclipse.rdf4j.http.client.QueryResponseHeartbeat$HeartbeatOutputStream.write(I)V` | 15 | 1.46% |
| `org.eclipse.rdf4j.sail.lmdb.LmdbFanOutStats.meanFanOut(JZ)Ljava/util/OptionalDouble;` | 15 | 1.46% |
| `java.lang.ThreadLocal$ThreadLocalMap.cleanSomeSlots(II)Z` | 12 | 1.17% |
| `org.eclipse.rdf4j.sail.lmdb.LmdbSailStore$LmdbSailDataset.withNativeSourceRead(Ljava/util/function/Supplier;)Ljava/lang/Object;` | 12 | 1.17% |
| `org.eclipse.rdf4j.sail.lmdb.evaluation.LmdbNativePackedFtree$Planner$OrderSearch.recurse(IJ[I[I[I[DDI)V` | 12 | 1.17% |
| `org.eclipse.rdf4j.sail.lmdb.model.LmdbLiteral.init()V` | 11 | 1.07% |
| `java.lang.invoke.VarHandleReferences$Array.getAcquire(Ljava/lang/invoke/VarHandle;Ljava/lang/Object;I)Ljava/lang/Object;` | 10 | 0.98% |
| `java.util.HashMap.getNode(Ljava/lang/Object;)Ljava/util/HashMap$Node;` | 10 | 0.98% |
| `java.util.concurrent.atomic.AtomicLong.incrementAndGet()J` | 10 | 0.98% |
| `org.eclipse.rdf4j.sail.lmdb.RepositoryValueIteration.drainIfIdle()V` | 10 | 0.98% |
| `java.lang.Math.scalb(DI)D` | 9 | 0.88% |
| `java.lang.ThreadLocal.get()Ljava/lang/Object;` | 8 | 0.78% |
| `java.lang.invoke.DirectMethodHandle.allocateInstance(Ljava/lang/Object;)Ljava/lang/Object;` | 8 | 0.78% |

## CPU inclusive attribution

| Method (JVM descriptor retained) | Samples | Selected share |
| --- | ---: | ---: |
| `java.lang.Thread.run()V` | 1,024 | 100.00% |
| `java.lang.Thread.runWith(Ljava/lang/Object;Ljava/lang/Runnable;)V` | 1,024 | 100.00% |
| `java.lang.invoke.DirectMethodHandle$Holder.invokeSpecial(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;` | 1,024 | 100.00% |
| `java.lang.invoke.LambdaForm$MH.0x000000006f05d400.invoke(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;` | 1,024 | 100.00% |
| `java.lang.invoke.LambdaForm$MH.0x000000006f05d800.invokeExact_MT(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;` | 1,024 | 100.00% |
| `java.lang.reflect.Method.invoke(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;` | 1,024 | 100.00% |
| `java.util.concurrent.Executors$RunnableAdapter.call()Ljava/lang/Object;` | 1,024 | 100.00% |
| `java.util.concurrent.FutureTask.run()V` | 1,024 | 100.00% |
| `java.util.concurrent.ThreadPoolExecutor$Worker.run()V` | 1,024 | 100.00% |
| `java.util.concurrent.ThreadPoolExecutor.runWorker(Ljava/util/concurrent/ThreadPoolExecutor$Worker;)V` | 1,024 | 100.00% |
| `jdk.internal.reflect.DirectMethodHandleAccessor.invoke(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;` | 1,024 | 100.00% |
| `jdk.internal.reflect.DirectMethodHandleAccessor.invokeImpl(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;` | 1,024 | 100.00% |
| `org.eclipse.rdf4j.query.resultio.binary.benchmark.jmh_generated.BinaryWriterBenchmark_serialize_jmhTest.serialize_AverageTime(Lorg/openjdk/jmh/runner/InfraControl;Lorg/openjdk/jmh/infra/ThreadParams;)Lorg/openjdk/jmh/results/BenchmarkTaskResult;` | 1,024 | 100.00% |
| `org.openjdk.jmh.runner.BenchmarkHandler$BenchmarkTask.call()Ljava/lang/Object;` | 1,024 | 100.00% |
| `org.openjdk.jmh.runner.BenchmarkHandler$BenchmarkTask.call()Lorg/openjdk/jmh/results/BenchmarkTaskResult;` | 1,024 | 100.00% |
| `org.eclipse.rdf4j.query.resultio.binary.benchmark.BinaryWriterBenchmark.serialize(Lorg/openjdk/jmh/infra/Blackhole;)I` | 1,023 | 99.90% |
| `org.eclipse.rdf4j.query.resultio.binary.benchmark.BinaryWriterBenchmark.writeDocument()V` | 1,023 | 99.90% |
| `org.eclipse.rdf4j.query.resultio.binary.benchmark.jmh_generated.BinaryWriterBenchmark_serialize_jmhTest.serialize_avgt_jmhStub(Lorg/openjdk/jmh/runner/InfraControl;Lorg/openjdk/jmh/results/RawResults;Lorg/openjdk/jmh/infra/BenchmarkParams;Lorg/openjdk/jmh/infra/IterationParams;Lorg/openjdk/jmh/infra/ThreadParams;Lorg/openjdk/jmh/infra/Blackhole;Lorg/openjdk/jmh/infra/Control;ILorg/eclipse/rdf4j/query/resultio/binary/benchmark/jmh_generated/BinaryWriterBenchmark_jmhType;)V` | 1,023 | 99.90% |
| `org.eclipse.rdf4j.common.iteration.IterationWrapper.hasNext()Z` | 747 | 72.95% |
| `org.eclipse.rdf4j.sail.helpers.CleanerIteration.hasNext()Z` | 747 | 72.95% |
| `org.eclipse.rdf4j.sail.helpers.SailBaseIteration.hasNext()Z` | 747 | 72.95% |
| `org.eclipse.rdf4j.sail.lmdb.RepositoryValueIteration.hasNext()Z` | 744 | 72.66% |
| `org.eclipse.rdf4j.sail.lmdb.RepositoryValueIteration.prepareNext()Z` | 732 | 71.48% |
| `org.eclipse.rdf4j.sail.lmdb.LmdbStoreConnection$ReplayableIteration.trackedCall(Lorg/eclipse/rdf4j/sail/helpers/AbstractSailConnection$ReadOperation;)Ljava/lang/Object;` | 518 | 50.59% |
| `org.eclipse.rdf4j.sail.lmdb.LmdbStoreConnection.executeTrackedRead(Lorg/eclipse/rdf4j/sail/helpers/AbstractSailConnection$ReadOperation;Lorg/eclipse/rdf4j/sail/lmdb/LmdbStoreConnection$ReplayableIteration;)Ljava/lang/Object;` | 510 | 49.80% |

## Sampled allocation by class

| Class | Weight, MiB | Selected share |
| --- | ---: | ---: |
| `java.util.HashMap$Node` | 3,259.915 | 26.02% |
| `java.lang.Object[]` | 1,767.733 | 14.11% |
| `int[]` | 706.094 | 5.64% |
| `org.eclipse.rdf4j.query.impl.SimpleBinding` | 513.368 | 4.10% |
| `java.lang.String` | 507.910 | 4.05% |
| `org.eclipse.rdf4j.sail.lmdb.ValueStoreRevision$Lazy` | 396.034 | 3.16% |
| `org.eclipse.rdf4j.sail.lmdb.RepositoryValueIteration$$Lambda.0x000000006f374670` | 339.601 | 2.71% |
| `java.util.LinkedHashMap` | 338.585 | 2.70% |
| `java.util.LinkedHashMap$Entry` | 330.614 | 2.64% |
| `double[]` | 328.940 | 2.63% |
| `java.nio.HeapByteBuffer` | 320.250 | 2.56% |
| `org.eclipse.rdf4j.sail.lmdb.evaluation.LmdbNativePackedFtree$EdgePlan` | 285.172 | 2.28% |
| `java.lang.ThreadLocal$ThreadLocalMap$Entry` | 279.667 | 2.23% |
| `java.util.LinkedHashMap$LinkedEntrySet` | 278.674 | 2.22% |
| `java.nio.StringCharBuffer` | 262.155 | 2.09% |
| `org.eclipse.rdf4j.http.client.QueryExecutionContext$$Lambda.0x000000006f280468` | 222.707 | 1.78% |
| `byte[]` | 209.755 | 1.67% |
| `java.lang.Runnable[]` | 204.263 | 1.63% |
| `java.util.OptionalDouble` | 204.263 | 1.63% |
| `org.eclipse.rdf4j.sail.lmdb.model.LmdbLiteral` | 203.262 | 1.62% |
| `java.util.ArrayDeque` | 176.764 | 1.41% |
| `org.eclipse.rdf4j.sail.lmdb.LmdbSailStore$ReadResourceContext` | 165.811 | 1.32% |
| `org.eclipse.rdf4j.sail.lmdb.csf.ImmutablePagedQuadCsfIndex$RowCursor` | 125.296 | 1.00% |
| `org.eclipse.rdf4j.sail.lmdb.LmdbSailStore$LmdbSailDataset$$Lambda.0x000000006f3163a0` | 119.363 | 0.95% |
| `java.util.HashMap$Node[]` | 118.863 | 0.95% |

## Sampled allocation by top frame

| Method (JVM descriptor retained) | Weight, MiB | Selected share |
| --- | ---: | ---: |
| `java.util.HashMap.newNode(ILjava/lang/Object;Ljava/lang/Object;Ljava/util/HashMap$Node;)Ljava/util/HashMap$Node;` | 3,259.915 | 26.02% |
| `java.lang.invoke.DirectMethodHandle.allocateInstance(Ljava/lang/Object;)Ljava/lang/Object;` | 991.782 | 7.92% |
| `java.util.IdentityHashMap.init(I)V` | 855.211 | 6.83% |
| `java.util.ArrayDeque.<init>()V` | 543.074 | 4.33% |
| `org.eclipse.rdf4j.query.MutableBindingSet.addBinding(Ljava/lang/String;Lorg/eclipse/rdf4j/model/Value;)V` | 459.948 | 3.67% |
| `org.eclipse.rdf4j.sail.lmdb.ValueStore.positionedRevision(Lorg/eclipse/rdf4j/sail/lmdb/ValueStoreRevision;I)Lorg/eclipse/rdf4j/sail/lmdb/ValueStoreRevision;` | 396.034 | 3.16% |
| `java.lang.StringLatin1.newString([BII)Ljava/lang/String;` | 369.570 | 2.95% |
| `java.util.ArrayList.grow(I)[Ljava/lang/Object;` | 365.953 | 2.92% |
| `org.eclipse.rdf4j.sail.lmdb.RepositoryValueIteration.resolveWaves(Ljava/util/List;)V` | 351.071 | 2.80% |
| `java.util.LinkedHashMap.newNode(ILjava/lang/Object;Ljava/lang/Object;Ljava/util/HashMap$Node;)Ljava/util/HashMap$Node;` | 330.614 | 2.64% |
| `java.nio.ByteBuffer.allocate(I)Ljava/nio/ByteBuffer;` | 320.250 | 2.56% |
| `java.math.MutableBigInteger.<init>()V` | 299.601 | 2.39% |
| `org.eclipse.rdf4j.sail.lmdb.evaluation.LmdbNativePackedFtree$Planner.orient(Lorg/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativePackedFtree$EdgePlan;II)Lorg/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativePackedFtree$EdgePlan;` | 285.172 | 2.28% |
| `java.lang.ThreadLocal$ThreadLocalMap.set(Ljava/lang/ThreadLocal;Ljava/lang/Object;)V` | 279.667 | 2.23% |
| `java.util.LinkedHashMap.sequencedEntrySet()Ljava/util/SequencedSet;` | 278.674 | 2.22% |
| `java.nio.CharBuffer.wrap(Ljava/lang/CharSequence;II)Ljava/nio/CharBuffer;` | 262.155 | 2.09% |
| `org.eclipse.rdf4j.sail.lmdb.evaluation.LmdbNativePackedFtree$Planner$OrderSearch.recurse(IJ[I[I[I[DDI)V` | 221.208 | 1.77% |
| `org.eclipse.rdf4j.sail.lmdb.LmdbStoreConnection.executeObservedRead(Lorg/eclipse/rdf4j/sail/helpers/AbstractSailConnection$ReadOperation;Lorg/eclipse/rdf4j/sail/lmdb/LmdbStoreConnection$ReplayableIteration;)Ljava/lang/Object;` | 204.263 | 1.63% |
| `java.util.OptionalDouble.of(D)Ljava/util/OptionalDouble;` | 204.263 | 1.63% |
| `org.eclipse.rdf4j.sail.lmdb.ValueStore.createLazyValue(JLorg/eclipse/rdf4j/sail/lmdb/ValueStoreRevision;ZLorg/eclipse/rdf4j/sail/lmdb/ValueResolutionScope;Lorg/eclipse/rdf4j/sail/lmdb/ValueStore$ValueCacheEpoch;ZILorg/eclipse/rdf4j/sail/lmdb/ValueStore$CanonicalValueEntry;)Lorg/eclipse/rdf4j/sail/lmdb/model/LmdbValue;` | 203.262 | 1.62% |
| `org.eclipse.rdf4j.sail.lmdb.RepositoryValueIteration.captureReadyOwners(Lorg/eclipse/rdf4j/model/Value;Ljava/util/Map;)V` | 176.764 | 1.41% |
| `org.eclipse.rdf4j.sail.lmdb.evaluation.LmdbNativeCostVector.<init>([D[D[DZ)V` | 167.802 | 1.34% |
| `org.eclipse.rdf4j.sail.lmdb.LmdbSailStore.enterReadResourceContext(Ljava/util/function/Supplier;)Lorg/eclipse/rdf4j/sail/base/SailClosable;` | 165.811 | 1.32% |
| `java.util.Arrays.copyOfRange([III)[I` | 160.809 | 1.28% |
| `org.eclipse.rdf4j.sail.lmdb.LmdbAdjacencyRunCodec$RunCursor.<init>()V` | 125.296 | 1.00% |

## Sampled allocation inclusive attribution

| Method (JVM descriptor retained) | Weight, MiB | Selected share |
| --- | ---: | ---: |
| `java.lang.Thread.run()V` | 12,528.577 | 100.00% |
| `java.lang.Thread.runWith(Ljava/lang/Object;Ljava/lang/Runnable;)V` | 12,528.577 | 100.00% |
| `java.lang.invoke.DirectMethodHandle$Holder.invokeSpecial(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;` | 12,528.577 | 100.00% |
| `java.lang.invoke.LambdaForm$MH.0x000000006f05d400.invoke(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;` | 12,528.577 | 100.00% |
| `java.lang.invoke.LambdaForm$MH.0x000000006f05d800.invokeExact_MT(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;` | 12,528.577 | 100.00% |
| `java.lang.reflect.Method.invoke(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;` | 12,528.577 | 100.00% |
| `java.util.concurrent.Executors$RunnableAdapter.call()Ljava/lang/Object;` | 12,528.577 | 100.00% |
| `java.util.concurrent.FutureTask.run()V` | 12,528.577 | 100.00% |
| `java.util.concurrent.ThreadPoolExecutor$Worker.run()V` | 12,528.577 | 100.00% |
| `java.util.concurrent.ThreadPoolExecutor.runWorker(Ljava/util/concurrent/ThreadPoolExecutor$Worker;)V` | 12,528.577 | 100.00% |
| `jdk.internal.reflect.DirectMethodHandleAccessor.invoke(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;` | 12,528.577 | 100.00% |
| `jdk.internal.reflect.DirectMethodHandleAccessor.invokeImpl(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;` | 12,528.577 | 100.00% |
| `org.eclipse.rdf4j.query.resultio.binary.benchmark.BinaryWriterBenchmark.serialize(Lorg/openjdk/jmh/infra/Blackhole;)I` | 12,528.577 | 100.00% |
| `org.eclipse.rdf4j.query.resultio.binary.benchmark.BinaryWriterBenchmark.writeDocument()V` | 12,528.577 | 100.00% |
| `org.eclipse.rdf4j.query.resultio.binary.benchmark.jmh_generated.BinaryWriterBenchmark_serialize_jmhTest.serialize_AverageTime(Lorg/openjdk/jmh/runner/InfraControl;Lorg/openjdk/jmh/infra/ThreadParams;)Lorg/openjdk/jmh/results/BenchmarkTaskResult;` | 12,528.577 | 100.00% |
| `org.eclipse.rdf4j.query.resultio.binary.benchmark.jmh_generated.BinaryWriterBenchmark_serialize_jmhTest.serialize_avgt_jmhStub(Lorg/openjdk/jmh/runner/InfraControl;Lorg/openjdk/jmh/results/RawResults;Lorg/openjdk/jmh/infra/BenchmarkParams;Lorg/openjdk/jmh/infra/IterationParams;Lorg/openjdk/jmh/infra/ThreadParams;Lorg/openjdk/jmh/infra/Blackhole;Lorg/openjdk/jmh/infra/Control;ILorg/eclipse/rdf4j/query/resultio/binary/benchmark/jmh_generated/BinaryWriterBenchmark_jmhType;)V` | 12,528.577 | 100.00% |
| `org.openjdk.jmh.runner.BenchmarkHandler$BenchmarkTask.call()Ljava/lang/Object;` | 12,528.577 | 100.00% |
| `org.openjdk.jmh.runner.BenchmarkHandler$BenchmarkTask.call()Lorg/openjdk/jmh/results/BenchmarkTaskResult;` | 12,528.577 | 100.00% |
| `org.eclipse.rdf4j.common.iteration.IterationWrapper.hasNext()Z` | 8,078.464 | 64.48% |
| `org.eclipse.rdf4j.sail.helpers.CleanerIteration.hasNext()Z` | 8,078.464 | 64.48% |
| `org.eclipse.rdf4j.sail.helpers.SailBaseIteration.hasNext()Z` | 8,078.464 | 64.48% |
| `org.eclipse.rdf4j.sail.lmdb.RepositoryValueIteration.hasNext()Z` | 8,078.464 | 64.48% |
| `org.eclipse.rdf4j.sail.lmdb.RepositoryValueIteration.prepareNext()Z` | 8,078.464 | 64.48% |
| `java.util.HashMap.put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;` | 3,610.507 | 28.82% |
| `java.util.HashMap.putVal(ILjava/lang/Object;Ljava/lang/Object;ZZ)Ljava/lang/Object;` | 3,610.507 | 28.82% |

## Interpretation

- CPU percentages are fractions of selected execution samples, not wall-clock time.
- Allocation weight estimates bytes represented by samples; sample counts are not object counts.
- Inclusive methods are deduplicated per stack; their percentages overlap and need not sum to 100%.
- Self means the top recorded Java frame, not an exact native instruction or allocation count.
- Missing-stack selected events remain in denominators; method tables omit those events.
- The default selection covers benchmark worker threads and writer stacks, not arbitrary asynchronous query workers.
- All matching events in the input are included, including any recorded setup or warmup.
- Print JFR with --stack-depth 128; the default five-frame display can hide callers even when truncated=false.
