# JFR binary writer profile summary

Input: `/workspace/scratch/966a0d01c43b/rdf4j/binary-writer-round2/profiles/published-query-heartbeat-pid71-2026-11-17T13_25_42.492662155Z-14644002924524027315.events.json`

Selection is a benchmark-thread match **or** a writer/benchmark method anywhere in the recorded stack.

| Measure | All events | Selected events | Selected share |
| --- | ---: | ---: | ---: |
| Execution samples | 868 | 868 | 100.00% |
| Allocation samples | 3,058 | 3,051 | 99.77% |
| Allocation weight, bytes | 10,058,567,456 | 10,034,563,120 | 99.76% |

Selected CPU samples with stacks: 868; marked truncated: 0. Selected allocation samples with stacks: 3,051; marked truncated: 0.

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
| `java.util.ArrayList` | 2,710.269 | 28.32% |
| `java.lang.Object[]` | 683.490 | 7.14% |
| `org.eclipse.rdf4j.sail.lmdb.ValueStoreRevision$Lazy` | 674.682 | 7.05% |
| `org.eclipse.rdf4j.sail.lmdb.evaluation.LmdbNativePackedFtree$EdgePlan` | 664.248 | 6.94% |
| `int[]` | 603.993 | 6.31% |
| `byte[]` | 493.851 | 5.16% |
| `java.util.LinkedHashMap$Entry` | 391.453 | 4.09% |
| `double[]` | 284.737 | 2.98% |
| `java.util.LinkedHashMap$LinkedEntrySet` | 248.173 | 2.59% |
| `org.eclipse.rdf4j.query.impl.SimpleBinding` | 247.701 | 2.59% |
| `java.util.LinkedHashMap` | 211.243 | 2.21% |
| `org.eclipse.rdf4j.sail.lmdb.RepositoryValueIteration$$Lambda.0x000000006f3633b0` | 199.268 | 2.08% |
| `org.eclipse.rdf4j.sail.lmdb.model.LmdbValue$InternalIdentity` | 193.781 | 2.02% |
| `java.util.HashMap$Node[]` | 191.281 | 2.00% |
| `org.eclipse.rdf4j.sail.lmdb.ValueResolutionScope$$Lambda.0x000000006f247318` | 190.281 | 1.99% |
| `org.eclipse.rdf4j.sail.lmdb.LmdbStoreConnection$$Lambda.0x000000006f34b6a0` | 186.285 | 1.95% |
| `java.lang.ThreadLocal$ThreadLocalMap$Entry` | 182.734 | 1.91% |
| `org.eclipse.rdf4j.sail.lmdb.RepositoryValueIteration$$Lambda.0x000000006f362bf0` | 179.791 | 1.88% |
| `long[]` | 179.646 | 1.88% |
| `org.eclipse.rdf4j.sail.lmdb.evaluation.LmdbNativePackedFtree$Planner$TreeChoice` | 108.256 | 1.13% |
| `org.eclipse.rdf4j.sail.lmdb.RepositoryValueIteration$$Lambda.0x000000006f366428` | 90.384 | 0.94% |
| `org.eclipse.rdf4j.sail.lmdb.ValueResolutionScope$RevisionKey` | 81.408 | 0.85% |
| `org.eclipse.rdf4j.http.client.QueryExecutionContext$$Lambda.0x000000006f248d40` | 57.936 | 0.61% |
| `org.eclipse.rdf4j.sail.lmdb.model.LmdbLiteral` | 57.933 | 0.61% |
| `org.eclipse.rdf4j.model.Value[]` | 46.436 | 0.49% |

## Sampled allocation by top frame

| Method (JVM descriptor retained) | Weight, MiB | Selected share |
| --- | ---: | ---: |
| `org.eclipse.rdf4j.sail.lmdb.evaluation.LmdbNativePackedFtree$Planner.lambda$plan$0(Ljava/lang/Long;)Ljava/util/List;` | 2,489.993 | 26.02% |
| `java.lang.invoke.DirectMethodHandle.allocateInstance(Ljava/lang/Object;)Ljava/lang/Object;` | 1,013.802 | 10.59% |
| `org.eclipse.rdf4j.sail.lmdb.evaluation.LmdbNativePackedFtree$Planner$OrderSearch.recurse(IJ[I[I[I[DDI)V` | 683.477 | 7.14% |
| `org.eclipse.rdf4j.sail.lmdb.ValueStore.positionedRevision(Lorg/eclipse/rdf4j/sail/lmdb/ValueStoreRevision;I)Lorg/eclipse/rdf4j/sail/lmdb/ValueStoreRevision;` | 674.682 | 7.05% |
| `org.eclipse.rdf4j.sail.lmdb.evaluation.LmdbNativePackedFtree$Planner.orient(Lorg/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativePackedFtree$EdgePlan;II)Lorg/eclipse/rdf4j/sail/lmdb/evaluation/LmdbNativePackedFtree$EdgePlan;` | 664.248 | 6.94% |
| `java.util.LinkedHashMap.newNode(ILjava/lang/Object;Ljava/lang/Object;Ljava/util/HashMap$Node;)Ljava/util/HashMap$Node;` | 391.453 | 4.09% |
| `java.util.ArrayDeque.<init>()V` | 390.858 | 4.08% |
| `java.util.Arrays.copyOfRange([BII)[B` | 330.039 | 3.45% |
| `java.util.LinkedHashMap.sequencedEntrySet()Ljava/util/SequencedSet;` | 248.173 | 2.59% |
| `org.eclipse.rdf4j.query.MutableBindingSet.addBinding(Ljava/lang/String;Lorg/eclipse/rdf4j/model/Value;)V` | 247.701 | 2.59% |
| `java.util.IdentityHashMap.init(I)V` | 206.231 | 2.15% |
| `org.eclipse.rdf4j.sail.lmdb.model.LmdbLiteral.setInternalID(JLorg/eclipse/rdf4j/sail/lmdb/ValueStoreRevision;)V` | 193.781 | 2.02% |
| `java.util.HashMap.resize()[Ljava/util/HashMap$Node;` | 191.281 | 2.00% |
| `java.lang.ThreadLocal$ThreadLocalMap.set(Ljava/lang/ThreadLocal;Ljava/lang/Object;)V` | 182.734 | 1.91% |
| `org.eclipse.rdf4j.sail.lmdb.RepositoryValueIteration.prepareNext()Z` | 174.299 | 1.82% |
| `org.eclipse.rdf4j.query.impl.MapBindingSet.<init>(I)V` | 157.304 | 1.64% |
| `java.math.BigInteger.smallToString(ILjava/lang/StringBuilder;I)V` | 151.258 | 1.58% |
| `java.lang.AbstractStringBuilder.<init>(Ljava/lang/String;)V` | 141.339 | 1.48% |
| `org.eclipse.rdf4j.sail.lmdb.evaluation.LmdbNativePackedFtree$Planner$OrderSearch.consider([I[I[DDI)V` | 137.716 | 1.44% |
| `org.eclipse.rdf4j.sail.lmdb.evaluation.LmdbNativeCostVector.<init>([D[D[DZ)V` | 112.366 | 1.17% |
| `org.eclipse.rdf4j.sail.lmdb.RepositoryValueIteration.resolveWaves(Ljava/util/List;)V` | 105.379 | 1.10% |
| `org.eclipse.rdf4j.sail.lmdb.ValueResolutionScope.revision(Lorg/eclipse/rdf4j/sail/lmdb/ValueStoreRevision$Lazy;)Lorg/eclipse/rdf4j/sail/lmdb/ValueStoreRevision$Lazy;` | 81.408 | 0.85% |
| `java.util.ArrayList.grow(I)[Ljava/lang/Object;` | 80.409 | 0.84% |
| `org.eclipse.rdf4j.sail.lmdb.evaluation.NativeProjectedBindingSet.<init>(Lorg/eclipse/rdf4j/sail/lmdb/evaluation/NativeLmdbQuerySource;Lorg/eclipse/rdf4j/sail/lmdb/evaluation/NativeProjectedBindingSet$ProjectionLayout;[J)V` | 67.242 | 0.70% |
| `org.eclipse.rdf4j.sail.lmdb.ValueStore.createLazyValue(JLorg/eclipse/rdf4j/sail/lmdb/ValueStoreRevision;ZLorg/eclipse/rdf4j/sail/lmdb/ValueResolutionScope;Lorg/eclipse/rdf4j/sail/lmdb/ValueStore$ValueCacheEpoch;ZILorg/eclipse/rdf4j/sail/lmdb/ValueStore$CanonicalValueEntry;)Lorg/eclipse/rdf4j/sail/lmdb/model/LmdbValue;` | 57.933 | 0.61% |

## Sampled allocation inclusive attribution

| Method (JVM descriptor retained) | Weight, MiB | Selected share |
| --- | ---: | ---: |
| `java.lang.Thread.run()V` | 9,569.705 | 100.00% |
| `java.lang.Thread.runWith(Ljava/lang/Object;Ljava/lang/Runnable;)V` | 9,569.705 | 100.00% |
| `java.lang.invoke.DirectMethodHandle$Holder.invokeSpecial(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;` | 9,569.705 | 100.00% |
| `java.lang.invoke.LambdaForm$MH.0x000000006f05d400.invoke(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;` | 9,569.705 | 100.00% |
| `java.lang.invoke.LambdaForm$MH.0x000000006f05d800.invokeExact_MT(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;` | 9,569.705 | 100.00% |
| `java.lang.reflect.Method.invoke(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;` | 9,569.705 | 100.00% |
| `java.util.concurrent.Executors$RunnableAdapter.call()Ljava/lang/Object;` | 9,569.705 | 100.00% |
| `java.util.concurrent.FutureTask.run()V` | 9,569.705 | 100.00% |
| `java.util.concurrent.ThreadPoolExecutor$Worker.run()V` | 9,569.705 | 100.00% |
| `java.util.concurrent.ThreadPoolExecutor.runWorker(Ljava/util/concurrent/ThreadPoolExecutor$Worker;)V` | 9,569.705 | 100.00% |
| `jdk.internal.reflect.DirectMethodHandleAccessor.invoke(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;` | 9,569.705 | 100.00% |
| `jdk.internal.reflect.DirectMethodHandleAccessor.invokeImpl(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;` | 9,569.705 | 100.00% |
| `org.eclipse.rdf4j.query.resultio.binary.benchmark.BinaryWriterBenchmark.serialize(Lorg/openjdk/jmh/infra/Blackhole;)I` | 9,569.705 | 100.00% |
| `org.eclipse.rdf4j.query.resultio.binary.benchmark.BinaryWriterBenchmark.writeDocument()V` | 9,569.705 | 100.00% |
| `org.eclipse.rdf4j.query.resultio.binary.benchmark.jmh_generated.BinaryWriterBenchmark_serialize_jmhTest.serialize_AverageTime(Lorg/openjdk/jmh/runner/InfraControl;Lorg/openjdk/jmh/infra/ThreadParams;)Lorg/openjdk/jmh/results/BenchmarkTaskResult;` | 9,569.705 | 100.00% |
| `org.eclipse.rdf4j.query.resultio.binary.benchmark.jmh_generated.BinaryWriterBenchmark_serialize_jmhTest.serialize_avgt_jmhStub(Lorg/openjdk/jmh/runner/InfraControl;Lorg/openjdk/jmh/results/RawResults;Lorg/openjdk/jmh/infra/BenchmarkParams;Lorg/openjdk/jmh/infra/IterationParams;Lorg/openjdk/jmh/infra/ThreadParams;Lorg/openjdk/jmh/infra/Blackhole;Lorg/openjdk/jmh/infra/Control;ILorg/eclipse/rdf4j/query/resultio/binary/benchmark/jmh_generated/BinaryWriterBenchmark_jmhType;)V` | 9,569.705 | 100.00% |
| `org.openjdk.jmh.runner.BenchmarkHandler$BenchmarkTask.call()Ljava/lang/Object;` | 9,569.705 | 100.00% |
| `org.openjdk.jmh.runner.BenchmarkHandler$BenchmarkTask.call()Lorg/openjdk/jmh/results/BenchmarkTaskResult;` | 9,569.705 | 100.00% |
| `org.eclipse.rdf4j.common.iteration.IterationWrapper.hasNext()Z` | 9,189.227 | 96.02% |
| `org.eclipse.rdf4j.sail.helpers.CleanerIteration.hasNext()Z` | 9,189.227 | 96.02% |
| `org.eclipse.rdf4j.sail.helpers.SailBaseIteration.hasNext()Z` | 9,189.227 | 96.02% |
| `org.eclipse.rdf4j.sail.lmdb.RepositoryValueIteration.hasNext()Z` | 9,189.227 | 96.02% |
| `org.eclipse.rdf4j.sail.lmdb.RepositoryValueIteration.prepareNext()Z` | 9,189.227 | 96.02% |
| `org.eclipse.rdf4j.sail.lmdb.LmdbStoreConnection$ReplayableIteration.trackedCall(Lorg/eclipse/rdf4j/sail/helpers/AbstractSailConnection$ReadOperation;)Ljava/lang/Object;` | 5,808.094 | 60.69% |
| `org.eclipse.rdf4j.sail.lmdb.LmdbStoreConnection.executeTrackedRead(Lorg/eclipse/rdf4j/sail/helpers/AbstractSailConnection$ReadOperation;Lorg/eclipse/rdf4j/sail/lmdb/LmdbStoreConnection$ReplayableIteration;)Ljava/lang/Object;` | 5,808.094 | 60.69% |

## Interpretation

- CPU percentages are fractions of selected execution samples, not wall-clock time.
- Allocation weight estimates bytes represented by samples; sample counts are not object counts.
- Inclusive methods are deduplicated per stack; their percentages overlap and need not sum to 100%.
- Self means the top recorded Java frame, not an exact native instruction or allocation count.
- Missing-stack selected events remain in denominators; method tables omit those events.
- The default selection covers benchmark worker threads and writer stacks, not arbitrary asynchronous query workers.
- All matching events in the input are included, including any recorded setup or warmup.
- Print JFR with --stack-depth 128; the default five-frame display can hide callers even when truncated=false.
