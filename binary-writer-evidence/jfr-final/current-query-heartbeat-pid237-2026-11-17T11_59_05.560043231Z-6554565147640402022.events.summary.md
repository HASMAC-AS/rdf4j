# JFR binary writer profile summary

Input: `/workspace/scratch/966a0d01c43b/rdf4j/binary-writer-evidence/jfr-final/current-query-heartbeat-pid237-2026-11-17T11_59_05.560043231Z-6554565147640402022.events.json`

Selection is a benchmark-thread match **or** a writer/benchmark method anywhere in the recorded stack.

| Measure | All events | Selected events | Selected share |
| --- | ---: | ---: | ---: |
| Execution samples | 997 | 997 | 100.00% |
| Allocation samples | 3,544 | 3,534 | 99.72% |
| Allocation weight, bytes | 9,616,095,088 | 9,592,052,392 | 99.75% |

Selected CPU samples with stacks: 997; marked truncated: 0. Selected allocation samples with stacks: 3,534; marked truncated: 0.

## CPU self attribution

| Method (JVM descriptor retained) | Samples | Selected share |
| --- | ---: | ---: |
| `org.eclipse.rdf4j.sail.lmdb.LmdbFanOutStats.estimateDistinct([J)D` | 84 | 8.43% |
| `java.util.IdentityHashMap.put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;` | 35 | 3.51% |
| `java.lang.invoke.VarHandleLongs$FieldInstanceReadWrite.weakCompareAndSetRelease(Ljava/lang/invoke/VarHandle;Ljava/lang/Object;JJ)Z` | 26 | 2.61% |
| `java.lang.ThreadLocal$ThreadLocalMap.cleanSomeSlots(II)Z` | 25 | 2.51% |
| `org.eclipse.rdf4j.sail.lmdb.LmdbStoreConnection.executeObservedRead(Lorg/eclipse/rdf4j/sail/helpers/AbstractSailConnection$ReadOperation;Lorg/eclipse/rdf4j/sail/lmdb/LmdbStoreConnection$ReplayableIteration;)Ljava/lang/Object;` | 20 | 2.01% |
| `org.eclipse.rdf4j.sail.lmdb.RepositoryValueIteration.resolveWaves(Ljava/util/List;)V` | 20 | 2.01% |
| `org.eclipse.rdf4j.http.client.QueryResponseHeartbeat.writePayload([BII)V` | 19 | 1.91% |
| `java.lang.ThreadLocal.get()Ljava/lang/Object;` | 18 | 1.81% |
| `org.eclipse.rdf4j.sail.lmdb.evaluation.LmdbNativePackedFtree$Planner$OrderSearch.childLanes(IID)D` | 18 | 1.81% |
| `org.eclipse.rdf4j.sail.lmdb.ValueStore.getLazyValue(Lorg/eclipse/rdf4j/sail/lmdb/ValueStore$ReadSnapshot;JI)Lorg/eclipse/rdf4j/sail/lmdb/model/LmdbValue;` | 16 | 1.60% |
| `java.lang.ref.Reference.<init>(Ljava/lang/Object;Ljava/lang/ref/ReferenceQueue;)V` | 15 | 1.50% |
| `org.eclipse.rdf4j.sail.lmdb.LmdbSailStore$LmdbSailDataset.withNativeSourceRead(Ljava/util/function/Supplier;)Ljava/lang/Object;` | 14 | 1.40% |
| `org.eclipse.rdf4j.sail.lmdb.LmdbStoreConnection$ReplayableIteration.trackedCall(Lorg/eclipse/rdf4j/sail/helpers/AbstractSailConnection$ReadOperation;)Ljava/lang/Object;` | 14 | 1.40% |
| `java.lang.Math.scalb(DI)D` | 12 | 1.20% |
| `java.lang.String.equals(Ljava/lang/Object;)Z` | 12 | 1.20% |
| `java.lang.invoke.VarHandleInts$FieldInstanceReadWrite.compareAndSet(Ljava/lang/invoke/VarHandle;Ljava/lang/Object;II)Z` | 11 | 1.10% |
| `org.eclipse.rdf4j.sail.lmdb.RepositoryValueIteration.captureReadyOwners(Lorg/eclipse/rdf4j/model/Value;Ljava/util/Map;)V` | 11 | 1.10% |
| `org.eclipse.rdf4j.sail.lmdb.LmdbFanOutStats.meanFanOut(JZ)Ljava/util/OptionalDouble;` | 10 | 1.00% |
| `java.io.DataOutputStream.write([BII)V` | 9 | 0.90% |
| `java.lang.ThreadLocal$ThreadLocalMap.set(Ljava/lang/ThreadLocal;Ljava/lang/Object;)V` | 9 | 0.90% |
| `java.util.ArrayList.add(Ljava/lang/Object;[Ljava/lang/Object;I)V` | 9 | 0.90% |
| `java.util.HashMap.getNode(Ljava/lang/Object;)Ljava/util/HashMap$Node;` | 9 | 0.90% |
| `org.eclipse.rdf4j.query.resultio.binary.BinaryQueryResultWriter.writeString(Ljava/lang/String;)V` | 9 | 0.90% |
| `java.io.ByteArrayOutputStream.write([BII)V` | 8 | 0.80% |
| `java.util.concurrent.atomic.AtomicLong.incrementAndGet()J` | 8 | 0.80% |

## CPU inclusive attribution

| Method (JVM descriptor retained) | Samples | Selected share |
| --- | ---: | ---: |
| `java.lang.Thread.run()V` | 997 | 100.00% |
| `java.lang.Thread.runWith(Ljava/lang/Object;Ljava/lang/Runnable;)V` | 997 | 100.00% |
| `java.util.concurrent.FutureTask.run()V` | 997 | 100.00% |
| `java.util.concurrent.ThreadPoolExecutor$Worker.run()V` | 997 | 100.00% |
| `java.util.concurrent.ThreadPoolExecutor.runWorker(Ljava/util/concurrent/ThreadPoolExecutor$Worker;)V` | 997 | 100.00% |
| `java.lang.invoke.DirectMethodHandle$Holder.invokeSpecial(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;` | 996 | 99.90% |
| `java.lang.invoke.LambdaForm$MH.0x000000002d05d400.invoke(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;` | 996 | 99.90% |
| `java.lang.invoke.LambdaForm$MH.0x000000002d05d800.invokeExact_MT(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;` | 996 | 99.90% |
| `java.lang.reflect.Method.invoke(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;` | 996 | 99.90% |
| `java.util.concurrent.Executors$RunnableAdapter.call()Ljava/lang/Object;` | 996 | 99.90% |
| `jdk.internal.reflect.DirectMethodHandleAccessor.invoke(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;` | 996 | 99.90% |
| `jdk.internal.reflect.DirectMethodHandleAccessor.invokeImpl(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;` | 996 | 99.90% |
| `org.eclipse.rdf4j.query.resultio.binary.benchmark.jmh_generated.BinaryWriterBenchmark_serialize_jmhTest.serialize_AverageTime(Lorg/openjdk/jmh/runner/InfraControl;Lorg/openjdk/jmh/infra/ThreadParams;)Lorg/openjdk/jmh/results/BenchmarkTaskResult;` | 996 | 99.90% |
| `org.openjdk.jmh.runner.BenchmarkHandler$BenchmarkTask.call()Ljava/lang/Object;` | 996 | 99.90% |
| `org.openjdk.jmh.runner.BenchmarkHandler$BenchmarkTask.call()Lorg/openjdk/jmh/results/BenchmarkTaskResult;` | 996 | 99.90% |
| `org.eclipse.rdf4j.query.resultio.binary.benchmark.BinaryWriterBenchmark.serialize(Lorg/openjdk/jmh/infra/Blackhole;)I` | 995 | 99.80% |
| `org.eclipse.rdf4j.query.resultio.binary.benchmark.jmh_generated.BinaryWriterBenchmark_serialize_jmhTest.serialize_avgt_jmhStub(Lorg/openjdk/jmh/runner/InfraControl;Lorg/openjdk/jmh/results/RawResults;Lorg/openjdk/jmh/infra/BenchmarkParams;Lorg/openjdk/jmh/infra/IterationParams;Lorg/openjdk/jmh/infra/ThreadParams;Lorg/openjdk/jmh/infra/Blackhole;Lorg/openjdk/jmh/infra/Control;ILorg/eclipse/rdf4j/query/resultio/binary/benchmark/jmh_generated/BinaryWriterBenchmark_jmhType;)V` | 995 | 99.80% |
| `org.eclipse.rdf4j.query.resultio.binary.benchmark.BinaryWriterBenchmark.writeDocument()V` | 994 | 99.70% |
| `org.eclipse.rdf4j.common.iteration.IterationWrapper.hasNext()Z` | 789 | 79.14% |
| `org.eclipse.rdf4j.sail.helpers.CleanerIteration.hasNext()Z` | 789 | 79.14% |
| `org.eclipse.rdf4j.sail.helpers.SailBaseIteration.hasNext()Z` | 789 | 79.14% |
| `org.eclipse.rdf4j.sail.lmdb.RepositoryValueIteration.hasNext()Z` | 785 | 78.74% |
| `org.eclipse.rdf4j.sail.lmdb.RepositoryValueIteration.prepareNext()Z` | 774 | 77.63% |
| `org.eclipse.rdf4j.sail.lmdb.LmdbStoreConnection$ReplayableIteration.trackedCall(Lorg/eclipse/rdf4j/sail/helpers/AbstractSailConnection$ReadOperation;)Ljava/lang/Object;` | 554 | 55.57% |
| `org.eclipse.rdf4j.sail.lmdb.LmdbStoreConnection.executeTrackedRead(Lorg/eclipse/rdf4j/sail/helpers/AbstractSailConnection$ReadOperation;Lorg/eclipse/rdf4j/sail/lmdb/LmdbStoreConnection$ReplayableIteration;)Ljava/lang/Object;` | 540 | 54.16% |

## Sampled allocation by class

| Class | Weight, MiB | Selected share |
| --- | ---: | ---: |
| `java.util.HashMap$Node[]` | 1,973.010 | 21.57% |
| `java.lang.Object[]` | 1,376.886 | 15.05% |
| `java.util.OptionalDouble` | 530.284 | 5.80% |
| `java.util.LinkedHashMap$Entry` | 421.753 | 4.61% |
| `org.eclipse.rdf4j.sail.lmdb.LmdbSailStore$LmdbSailDataset$$Lambda.0x000000002d315100` | 408.424 | 4.46% |
| `org.eclipse.rdf4j.sail.lmdb.ValueStoreRevision$Lazy` | 392.459 | 4.29% |
| `java.util.LinkedHashMap` | 389.290 | 4.26% |
| `int[]` | 357.513 | 3.91% |
| `java.util.ArrayList` | 287.062 | 3.14% |
| `org.eclipse.rdf4j.sail.lmdb.model.LmdbValue$InternalIdentity` | 284.127 | 3.11% |
| `byte[]` | 251.203 | 2.75% |
| `org.eclipse.rdf4j.http.client.QueryExecutionContext$$Lambda.0x000000002d280d40` | 248.092 | 2.71% |
| `org.eclipse.rdf4j.sail.lmdb.evaluation.LmdbNativePackedFtree$EdgePlan` | 246.547 | 2.70% |
| `java.util.ArrayDeque` | 202.770 | 2.22% |
| `double[]` | 172.833 | 1.89% |
| `java.util.concurrent.atomic.AtomicBoolean` | 172.800 | 1.89% |
| `org.eclipse.rdf4j.query.impl.SimpleBinding` | 161.304 | 1.76% |
| `java.lang.ThreadLocal$ThreadLocalMap$Entry` | 138.342 | 1.51% |
| `java.util.IdentityHashMap` | 132.832 | 1.45% |
| `org.eclipse.rdf4j.sail.lmdb.LmdbStoreConnection$$Lambda.0x000000002d368000` | 105.379 | 1.15% |
| `boolean[]` | 100.386 | 1.10% |
| `java.lang.StringBuilder` | 81.907 | 0.90% |
| `org.eclipse.rdf4j.sail.lmdb.model.LmdbLiteral` | 67.860 | 0.74% |
| `org.eclipse.rdf4j.sail.lmdb.LmdbSailStore$ReadResourceContext` | 59.431 | 0.65% |
| `java.lang.String` | 55.937 | 0.61% |

## Sampled allocation by top frame

| Method (JVM descriptor retained) | Weight, MiB | Selected share |
| --- | ---: | ---: |
| `java.util.HashMap.resize()[Ljava/util/HashMap$Node;` | 1,973.010 | 21.57% |
| `java.lang.invoke.DirectMethodHandle.allocateInstance(Ljava/lang/Object;)Ljava/lang/Object;` | 1,033.533 | 11.30% |
| `java.util.ArrayDeque.<init>()V` | 684.150 | 7.48% |
| `org.eclipse.rdf4j.sail.lmdb.RepositoryValueIteration.resolveWaves(Ljava/util/List;)V` | 560.080 | 6.12% |
| `java.util.OptionalDouble.of(D)Ljava/util/OptionalDouble;` | 530.284 | 5.80% |
| `java.util.LinkedHashMap.newNode(ILjava/lang/Object;Ljava/lang/Object;Ljava/util/HashMap$Node;)Ljava/util/HashMap$Node;` | 421.753 | 4.61% |
| `org.eclipse.rdf4j.sail.lmdb.ValueStore.positionedRevision(Lorg/eclipse/rdf4j/sail/lmdb/ValueStoreRevision;I)Lorg/eclipse/rdf4j/sail/lmdb/ValueStoreRevision;` | 392.459 | 4.29% |
| `java.util.IdentityHashMap.init(I)V` | 328.077 | 3.59% |
| `org.eclipse.rdf4j.sail.lmdb.model.LmdbLiteral.setInternalID(JLorg/eclipse/rdf4j/sail/lmdb/ValueStoreRevision;)V` | 284.127 | 3.11% |
| `java.util.ArrayList.grow(I)[Ljava/lang/Object;` | 248.704 | 2.72% |
| `org.eclipse.rdf4j.sail.lmdb.RepositoryValueIteration.prepareNext()Z` | 247.107 | 2.70% |
| `org.eclipse.rdf4j.sail.lmdb.evaluation.LmdbNativePackedFtree$Planner.orient(Lorg/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativePackedFtree$EdgePlan;II)Lorg/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativePackedFtree$EdgePlan;` | 246.547 | 2.70% |
| `java.lang.Long.toString(J)Ljava/lang/String;` | 232.736 | 2.54% |
| `org.eclipse.rdf4j.sail.lmdb.RepositoryValueIteration.captureReadyOwners(Lorg/eclipse/rdf4j/model/Value;Ljava/util/Map;)V` | 202.770 | 2.22% |
| `org.eclipse.rdf4j.http.client.QueryExecutionContext$ReplaySafepoint.<init>(Ljava/lang/Runnable;Lorg/eclipse/rdf4j/http/client/QueryExecutionContext$ReplaySafepoint;Ljava/util/function/Supplier;)V` | 175.797 | 1.92% |
| `java.lang.ThreadLocal$ThreadLocalMap.set(Ljava/lang/ThreadLocal;Ljava/lang/Object;)V` | 138.342 | 1.51% |
| `org.eclipse.rdf4j.sail.lmdb.evaluation.LmdbNativePackedFtree$Planner$OrderSearch.recurse(IJ[I[I[I[DDI)V` | 137.262 | 1.50% |
| `java.math.BigInteger.toMagArray(J)[I` | 126.356 | 1.38% |
| `org.eclipse.rdf4j.query.MutableBindingSet.addBinding(Ljava/lang/String;Lorg/eclipse/rdf4j/model/Value;)V` | 124.358 | 1.36% |
| `java.util.IdentityHashMap.resize(I)Z` | 115.454 | 1.26% |
| `java.math.MutableBigInteger.<init>()V` | 105.881 | 1.16% |
| `org.eclipse.rdf4j.sail.lmdb.evaluation.LmdbNativeSelector.rank(Ljava/util/List;Lorg/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativeStaticPrior;DIZLorg/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativeAdvisoryLearner$Scope;)Lorg/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativeSelector$Ranking;` | 104.881 | 1.15% |
| `java.math.BigInteger.toString(I)Ljava/lang/String;` | 81.907 | 0.90% |
| `org.eclipse.rdf4j.sail.lmdb.RepositoryValueIteration.isPrepared(Lorg/eclipse/rdf4j/model/Value;)Z` | 68.422 | 0.75% |
| `org.eclipse.rdf4j.sail.lmdb.ValueStore.createLazyValue(JLorg/eclipse/rdf4j/sail/lmdb/ValueStoreRevision;ZLorg/eclipse/rdf4j/sail/lmdb/ValueResolutionScope;Lorg/eclipse/rdf4j/sail/lmdb/ValueStore$ValueCacheEpoch;ZILorg/eclipse/rdf4j/sail/lmdb/ValueStore$CanonicalValueEntry;)Lorg/eclipse/rdf4j/sail/lmdb/model/LmdbValue;` | 67.860 | 0.74% |

## Sampled allocation inclusive attribution

| Method (JVM descriptor retained) | Weight, MiB | Selected share |
| --- | ---: | ---: |
| `java.lang.Thread.run()V` | 9,147.694 | 100.00% |
| `java.lang.Thread.runWith(Ljava/lang/Object;Ljava/lang/Runnable;)V` | 9,147.694 | 100.00% |
| `java.lang.invoke.DirectMethodHandle$Holder.invokeSpecial(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;` | 9,147.694 | 100.00% |
| `java.lang.invoke.LambdaForm$MH.0x000000002d05d400.invoke(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;` | 9,147.694 | 100.00% |
| `java.lang.invoke.LambdaForm$MH.0x000000002d05d800.invokeExact_MT(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;` | 9,147.694 | 100.00% |
| `java.lang.reflect.Method.invoke(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;` | 9,147.694 | 100.00% |
| `java.util.concurrent.Executors$RunnableAdapter.call()Ljava/lang/Object;` | 9,147.694 | 100.00% |
| `java.util.concurrent.FutureTask.run()V` | 9,147.694 | 100.00% |
| `java.util.concurrent.ThreadPoolExecutor$Worker.run()V` | 9,147.694 | 100.00% |
| `java.util.concurrent.ThreadPoolExecutor.runWorker(Ljava/util/concurrent/ThreadPoolExecutor$Worker;)V` | 9,147.694 | 100.00% |
| `jdk.internal.reflect.DirectMethodHandleAccessor.invoke(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;` | 9,147.694 | 100.00% |
| `jdk.internal.reflect.DirectMethodHandleAccessor.invokeImpl(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;` | 9,147.694 | 100.00% |
| `org.eclipse.rdf4j.query.resultio.binary.benchmark.BinaryWriterBenchmark.serialize(Lorg/openjdk/jmh/infra/Blackhole;)I` | 9,147.694 | 100.00% |
| `org.eclipse.rdf4j.query.resultio.binary.benchmark.BinaryWriterBenchmark.writeDocument()V` | 9,147.694 | 100.00% |
| `org.eclipse.rdf4j.query.resultio.binary.benchmark.jmh_generated.BinaryWriterBenchmark_serialize_jmhTest.serialize_AverageTime(Lorg/openjdk/jmh/runner/InfraControl;Lorg/openjdk/jmh/infra/ThreadParams;)Lorg/openjdk/jmh/results/BenchmarkTaskResult;` | 9,147.694 | 100.00% |
| `org.eclipse.rdf4j.query.resultio.binary.benchmark.jmh_generated.BinaryWriterBenchmark_serialize_jmhTest.serialize_avgt_jmhStub(Lorg/openjdk/jmh/runner/InfraControl;Lorg/openjdk/jmh/results/RawResults;Lorg/openjdk/jmh/infra/BenchmarkParams;Lorg/openjdk/jmh/infra/IterationParams;Lorg/openjdk/jmh/infra/ThreadParams;Lorg/openjdk/jmh/infra/Blackhole;Lorg/openjdk/jmh/infra/Control;ILorg/eclipse/rdf4j/query/resultio/binary/benchmark/jmh_generated/BinaryWriterBenchmark_jmhType;)V` | 9,147.694 | 100.00% |
| `org.openjdk.jmh.runner.BenchmarkHandler$BenchmarkTask.call()Ljava/lang/Object;` | 9,147.694 | 100.00% |
| `org.openjdk.jmh.runner.BenchmarkHandler$BenchmarkTask.call()Lorg/openjdk/jmh/results/BenchmarkTaskResult;` | 9,147.694 | 100.00% |
| `org.eclipse.rdf4j.common.iteration.IterationWrapper.hasNext()Z` | 7,030.263 | 76.85% |
| `org.eclipse.rdf4j.sail.helpers.CleanerIteration.hasNext()Z` | 7,030.263 | 76.85% |
| `org.eclipse.rdf4j.sail.helpers.SailBaseIteration.hasNext()Z` | 7,030.263 | 76.85% |
| `org.eclipse.rdf4j.sail.lmdb.RepositoryValueIteration.hasNext()Z` | 7,030.263 | 76.85% |
| `org.eclipse.rdf4j.sail.lmdb.RepositoryValueIteration.prepareNext()Z` | 7,030.263 | 76.85% |
| `org.eclipse.rdf4j.sail.lmdb.LmdbStoreConnection$ReplayableIteration.trackedCall(Lorg/eclipse/rdf4j/sail/helpers/AbstractSailConnection$ReadOperation;)Ljava/lang/Object;` | 3,169.519 | 34.65% |
| `org.eclipse.rdf4j.sail.lmdb.LmdbStoreConnection.executeTrackedRead(Lorg/eclipse/rdf4j/sail/helpers/AbstractSailConnection$ReadOperation;Lorg/eclipse/rdf4j/sail/lmdb/LmdbStoreConnection$ReplayableIteration;)Ljava/lang/Object;` | 3,169.519 | 34.65% |

## Interpretation

- CPU percentages are fractions of selected execution samples, not wall-clock time.
- Allocation weight estimates bytes represented by samples; sample counts are not object counts.
- Inclusive methods are deduplicated per stack; their percentages overlap and need not sum to 100%.
- Self means the top recorded Java frame, not an exact native instruction or allocation count.
- Missing-stack selected events remain in denominators; method tables omit those events.
- The default selection covers benchmark worker threads and writer stacks, not arbitrary asynchronous query workers.
- All matching events in the input are included, including any recorded setup or warmup.
- Print JFR with --stack-depth 128; the default five-frame display can hide callers even when truncated=false.
