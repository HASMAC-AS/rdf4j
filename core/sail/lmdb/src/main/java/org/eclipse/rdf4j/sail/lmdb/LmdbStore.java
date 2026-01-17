/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.lmdb;

import static org.eclipse.rdf4j.sail.lmdb.LmdbUtil.E;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.util.lmdb.LMDB.MDB_CP_COMPACT;
import static org.lwjgl.util.lmdb.LMDB.MDB_INVALID;
import static org.lwjgl.util.lmdb.LMDB.MDB_NOTLS;
import static org.lwjgl.util.lmdb.LMDB.MDB_RDONLY;
import static org.lwjgl.util.lmdb.LMDB.mdb_env_close;
import static org.lwjgl.util.lmdb.LMDB.mdb_env_copy2;
import static org.lwjgl.util.lmdb.LMDB.mdb_env_create;
import static org.lwjgl.util.lmdb.LMDB.mdb_env_info;
import static org.lwjgl.util.lmdb.LMDB.mdb_env_open;
import static org.lwjgl.util.lmdb.LMDB.mdb_env_set_mapsize;
import static org.lwjgl.util.lmdb.LMDB.mdb_env_set_maxdbs;
import static org.lwjgl.util.lmdb.LMDB.mdb_env_stat;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.eclipse.rdf4j.collection.factory.api.CollectionFactory;
import org.eclipse.rdf4j.collection.factory.mapdb.MapDb3CollectionFactory;
import org.eclipse.rdf4j.common.annotation.Experimental;
import org.eclipse.rdf4j.common.concurrent.locks.Lock;
import org.eclipse.rdf4j.common.concurrent.locks.LockManager;
import org.eclipse.rdf4j.common.io.MavenUtil;
import org.eclipse.rdf4j.common.transaction.IsolationLevel;
import org.eclipse.rdf4j.common.transaction.IsolationLevels;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategy;
import org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategyFactory;
import org.eclipse.rdf4j.query.algebra.evaluation.federation.FederatedServiceResolver;
import org.eclipse.rdf4j.query.algebra.evaluation.federation.FederatedServiceResolverClient;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.StrictEvaluationStrategyFactory;
import org.eclipse.rdf4j.repository.sparql.federation.SPARQLServiceResolver;
import org.eclipse.rdf4j.sail.InterruptedSailException;
import org.eclipse.rdf4j.sail.NotifyingSailConnection;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.base.SailSource;
import org.eclipse.rdf4j.sail.base.SailStore;
import org.eclipse.rdf4j.sail.base.SnapshotSailStore;
import org.eclipse.rdf4j.sail.helpers.AbstractNotifyingSail;
import org.eclipse.rdf4j.sail.helpers.DirectoryLockManager;
import org.eclipse.rdf4j.sail.lmdb.config.LmdbStoreConfig;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.lmdb.MDBEnvInfo;
import org.lwjgl.util.lmdb.MDBStat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A SAIL implementation using LMDB for storing and querying its data.
 *
 * @implNote the LMDB store is is in an experimental state: its existence, signature or behavior may change without
 *           warning from one release to the next.
 */
@Experimental
public class LmdbStore extends AbstractNotifyingSail implements FederatedServiceResolverClient {

	private static final Logger logger = LoggerFactory.getLogger(LmdbStore.class);

	/*-----------*
	 * Variables *
	 *-----------*/

	private static final String VERSION = MavenUtil.loadVersion("org.eclipse.rdf4j", "rdf4j-sail-lmdb", "devel");

	/**
	 * Specifies which triple indexes this lmdb store must use.
	 */
	private final LmdbStoreConfig config;

	private SailStore store;

	private LmdbSailStore backingStore;

	// used to decide if store is writable, is true if the store was writable during initialization
	private boolean isWritable;

	// indicates if a datadir is temporary (i.e. will be deleted on shutdown)
	private boolean isTmpDatadir = false;

	/**
	 * Data directory lock.
	 */
	private volatile Lock dirLock;

	private EvaluationStrategyFactory evalStratFactory;

	/**
	 * independent life cycle
	 */
	private FederatedServiceResolver serviceResolver;

	/**
	 * dependent life cycle
	 */
	private SPARQLServiceResolver dependentServiceResolver;

	/**
	 * Lock manager used to prevent concurrent {@link #getTransactionLock(IsolationLevel)} calls.
	 */
	private final ReentrantLock txnLockManager = new ReentrantLock();

	/**
	 * Holds locks for all isolated transactions.
	 */
	private final LockManager isolatedLockManager = new LockManager(debugEnabled());

	/**
	 * Holds locks for all {@link IsolationLevels#NONE} isolation transactions.
	 */
	private final LockManager disabledIsolationLockManager = new LockManager(debugEnabled());

	/*--------------*
	 * Constructors *
	 *--------------*/

	/**
	 * Creates a new LmdbStore with default settings.
	 */
	public LmdbStore() {
		this(new LmdbStoreConfig());
	}

	/**
	 * Creates a new LmdbStore.
	 */
	public LmdbStore(LmdbStoreConfig config) {
		super();
		this.config = config;
		setSupportedIsolationLevels(IsolationLevels.NONE, IsolationLevels.READ_COMMITTED, IsolationLevels.SNAPSHOT_READ,
				IsolationLevels.SNAPSHOT, IsolationLevels.SERIALIZABLE);
		setDefaultIsolationLevel(IsolationLevels.SNAPSHOT_READ);
		config.getDefaultQueryEvaluationMode().ifPresent(this::setDefaultQueryEvaluationMode);
		if (config.getIterationCacheSyncThreshold() > 0) {
			setIterationCacheSyncThreshold(config.getIterationCacheSyncThreshold());
		}
		EvaluationStrategyFactory evalStrategyFactory = config.getEvaluationStrategyFactory();
		if (evalStrategyFactory != null) {
			setEvaluationStrategyFactory(evalStrategyFactory);
		}
	}

	/**
	 * Creates a new LmdbStore with default settings.
	 */
	public LmdbStore(File dataDir) {
		this(dataDir, new LmdbStoreConfig());
	}

	public LmdbStore(File dataDir, LmdbStoreConfig config) {
		this(config);
		setDataDir(dataDir);
	}

	/*---------*
	 * Methods *
	 *---------*/

	@Override
	public void setDataDir(File dataDir) {
		super.setDataDir(dataDir);
		isTmpDatadir = (dataDir == null);
	}

	/**
	 * @return Returns the {@link EvaluationStrategy}.
	 */
	public synchronized EvaluationStrategyFactory getEvaluationStrategyFactory() {
		if (evalStratFactory == null) {
			evalStratFactory = new StrictEvaluationStrategyFactory(getFederatedServiceResolver());
		}
		evalStratFactory.setQuerySolutionCacheThreshold(getIterationCacheSyncThreshold());
		evalStratFactory.setTrackResultSize(isTrackResultSize());
		evalStratFactory.setCollectionFactory(getCollectionFactory());
		return evalStratFactory;
	}

	/**
	 * Sets the {@link EvaluationStrategy} to use.
	 */
	public synchronized void setEvaluationStrategyFactory(EvaluationStrategyFactory factory) {
		evalStratFactory = factory;
	}

	/**
	 * @return Returns the SERVICE resolver.
	 */
	public synchronized FederatedServiceResolver getFederatedServiceResolver() {
		if (serviceResolver == null) {
			if (dependentServiceResolver == null) {
				dependentServiceResolver = new SPARQLServiceResolver();
			}
			setFederatedServiceResolver(dependentServiceResolver);
		}
		return serviceResolver;
	}

	/**
	 * Overrides the {@link FederatedServiceResolver} used by this instance, but the given resolver is not shutDown when
	 * this instance is.
	 *
	 * @param resolver The SERVICE resolver to set.
	 */
	@Override
	public synchronized void setFederatedServiceResolver(FederatedServiceResolver resolver) {
		this.serviceResolver = resolver;
		if (resolver != null && evalStratFactory instanceof FederatedServiceResolverClient) {
			((FederatedServiceResolverClient) evalStratFactory).setFederatedServiceResolver(resolver);
		}
	}

	/**
	 * Initializes this LmdbStore.
	 *
	 * @throws SailException If this LmdbStore could not be initialized using the parameters that have been set.
	 */
	@Override
	protected void initializeInternal() throws SailException {
		logger.debug("Initializing LmdbStore...");

		// Check initialization parameters
		File dataDir = getDataDir();

		if (dataDir == null) {
			try {
				setDataDir(Files.createTempDirectory("rdf4j-lmdb-tmp").toFile());
				isTmpDatadir = true;
			} catch (IOException ioe) {
				throw new SailException("Temp data dir could not be created");
			}
			dataDir = getDataDir();
		} else if (!dataDir.exists()) {
			boolean success = dataDir.mkdirs();
			if (!success) {
				throw new SailException("Unable to create data directory: " + dataDir);
			}
		} else if (!dataDir.isDirectory()) {
			throw new SailException("The specified path does not denote a directory: " + dataDir);
		} else if (!dataDir.canRead()) {
			throw new SailException("Not allowed to read from the specified directory: " + dataDir);
		}

		// try to lock the directory or fail
		dirLock = new DirectoryLockManager(dataDir).lockOrFail();

		logger.debug("Data dir is " + dataDir);

		try {
			File versionFile = new File(dataDir, "lmdbrdf.ver");
			String version = versionFile.exists() ? FileUtils.readFileToString(versionFile, StandardCharsets.UTF_8)
					: null;
			if (!VERSION.equals(version) && upgradeStore(dataDir, version)) {
				FileUtils.writeStringToFile(versionFile, VERSION, StandardCharsets.UTF_8);
			}
			backingStore = new LmdbSailStore(dataDir, config);
			this.store = new SnapshotSailStore(backingStore, () -> new MemoryOverflowModel(false) {
				@Override
				protected LmdbSailStore createSailStore(File dataDir) throws IOException, SailException {
					// Model can't fit into memory, use another LmdbSailStore to store delta
					LmdbSailStore lmdbSailStore = new LmdbSailStore(dataDir, config);
					lmdbSailStore.enableMultiThreading = false;
					return lmdbSailStore;
				}
			}) {

				@Override
				public SailSource getExplicitSailSource() {
					if (isIsolationDisabled()) {
						// no isolation, use LmdbSailStore directly
						return backingStore.getExplicitSailSource();
					} else {
						return super.getExplicitSailSource();
					}
				}

				@Override
				public SailSource getInferredSailSource() {
					if (isIsolationDisabled()) {
						// no isolation, use LmdbSailStore directly
						return backingStore.getInferredSailSource();
					} else {
						return super.getInferredSailSource();
					}
				}
			};
		} catch (Throwable e) {
			// LmdbStore initialization failed, release any allocated files
			dirLock.release();

			throw new SailException(e);
		}

		isWritable = getDataDir().canWrite();

		logger.debug("LmdbStore initialized");
	}

	@Override
	protected void shutDownInternal() throws SailException {
		logger.debug("Shutting down LmdbStore...");

		try {
			store.close();
		} finally {
			dirLock.release();
			if (dependentServiceResolver != null) {
				dependentServiceResolver.shutDown();
			}
		}

		if (isTmpDatadir) {
			File dataDir = getDataDir();
			if (dataDir != null) {
				try {
					try (Stream<Path> walk = Files.walk(dataDir.toPath())) {
						walk
								.map(Path::toFile)
								.sorted(Comparator.reverseOrder()) // delete files before directory
								.forEach(File::delete);
					}

				} catch (IOException ioe) {
					logger.error("Could not delete temp file " + dataDir);
				}
			}
		}
		logger.debug("LmdbStore shut down");
	}

	@Override
	public void shutDown() throws SailException {
		super.shutDown();
		// edge case when re-initialize after shutdown
		if (isTmpDatadir) {
			setDataDir(null);
		}
	}

	@Override
	public boolean isWritable() {
		return isWritable;
	}

	@Override
	protected NotifyingSailConnection getConnectionInternal() throws SailException {
		return new LmdbStoreConnection(this);
	}

	@Override
	public ValueFactory getValueFactory() {
		return store.getValueFactory();
	}

	/**
	 * This call will block when {@link IsolationLevels#NONE} is provided when there are active transactions with a
	 * higher isolation and block when a higher isolation is provided when there are active transactions with
	 * {@link IsolationLevels#NONE} isolation. Store is either exclusively in {@link IsolationLevels#NONE} isolation
	 * with potentially zero or more transactions, or exclusively in higher isolation mode with potentially zero or more
	 * transactions.
	 *
	 * @param level indicating desired mode {@link IsolationLevels#NONE} or higher
	 * @return Lock used to prevent Store from switching isolation modes
	 * @throws SailException
	 */
	protected Lock getTransactionLock(IsolationLevel level) throws SailException {
		txnLockManager.lock();
		try {
			if (IsolationLevels.NONE.isCompatibleWith(level)) {
				// make sure no isolated transaction are active
				isolatedLockManager.waitForActiveLocks();
				// mark isolation as disabled
				return disabledIsolationLockManager.createLock(level.toString());
			} else {
				// make sure isolation is not disabled
				disabledIsolationLockManager.waitForActiveLocks();
				// mark isolated transaction as active
				return isolatedLockManager.createLock(level.toString());
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new InterruptedSailException(e);
		} finally {
			txnLockManager.unlock();
		}
	}

	/**
	 * Checks if any {@link IsolationLevels#NONE} isolation transactions are active.
	 *
	 * @return <code>true</code> if at least one transaction has direct access to the indexes
	 */
	boolean isIsolationDisabled() {
		return disabledIsolationLockManager.isActiveLock();
	}

	SailStore getSailStore() {
		return store;
	}

	LmdbSailStore getBackingStore() {
		return backingStore;
	}

	private boolean upgradeStore(File dataDir, String version) throws SailException {
		// nothing to do, just update version number
		return true;
	}

	@Override
	public Supplier<CollectionFactory> getCollectionFactory() {
		return () -> new MapDb3CollectionFactory(getIterationCacheSyncThreshold());
	}

	public LmdbStoreConfig getStoreConfig() {
		return config;
	}

	/**
	 * Performs an offline LMDB compaction by copying all environments to a staging directory and atomically swapping
	 * the compacted copy into place.
	 *
	 * @param options compaction options
	 * @return a report describing the outcome of the compaction run
	 * @throws IOException if the compaction fails
	 */
	public LmdbCompactionReport compact(LmdbCompactionOptions options) throws IOException {
		Objects.requireNonNull(options, "options");
		File dataDir = getDataDir();
		if (dataDir == null) {
			throw new IllegalStateException("Data directory is not configured");
		}
		if (store != null) {
			throw new IllegalStateException("LmdbStore must be shut down before running compaction");
		}
		Path sourceDir = dataDir.toPath();
		if (!Files.isDirectory(sourceDir)) {
			throw new IOException("LMDB data directory does not exist: " + sourceDir);
		}
		LmdbCompactionProgressListener progressListener = options.getProgressListener();
		progressListener.onProgress(LmdbCompactionProgress
				.of(LmdbCompactionProgress.Phase.VALIDATING, "Validating LMDB compaction prerequisites"));
		Path stagingDir = options.getDestinationDirectory();
		if (sourceDir.equals(stagingDir)) {
			throw new IllegalArgumentException("Destination directory must differ from the source data directory");
		}
		ensureDestinationIsEmpty(stagingDir);
		Path temporaryRoot = options.getTemporaryDirectory();
		Files.createDirectories(temporaryRoot);
		Path backupDir = uniqueChild(temporaryRoot, "lmdb-backup-" + options.getJobId());
		Instant started = Instant.now();
		long sizeBefore = directorySize(sourceDir);
		LmdbCompactionMetrics.Builder metricsBuilder = LmdbCompactionMetrics.builder()
				.fileSizeBeforeBytes(sizeBefore)
				.startedAt(started);
		DirectoryLockManager lockManager = new DirectoryLockManager(dataDir);
		Lock lock = null;
		boolean swapped = false;
		boolean verificationAttempted = false;
		boolean verificationSucceeded = false;
		List<String> warnings = new ArrayList<>();
		try {
			lock = lockManager.lockOrFail();
			copyStore(sourceDir, stagingDir, progressListener, metricsBuilder);
			if (options.isVerifyAfterCopy()) {
				verificationAttempted = true;
				progressListener.onProgress(LmdbCompactionProgress
						.of(LmdbCompactionProgress.Phase.VERIFYING, "Verifying staged LMDB environment"));
				try {
					verifyCompactedStore(stagingDir);
					verificationSucceeded = true;
				} catch (SailException e) {
					throw new IOException("Verification of staged LMDB environment failed", e);
				}
			} else {
				progressListener.onProgress(LmdbCompactionProgress
						.of(LmdbCompactionProgress.Phase.VERIFYING, "Verification skipped by configuration"));
				verificationSucceeded = true;
			}
			progressListener.onProgress(LmdbCompactionProgress
					.of(LmdbCompactionProgress.Phase.SWAPPING, "Swapping compacted environment into place"));
			swapDirectories(sourceDir, stagingDir, backupDir);
			swapped = true;
		} finally {
			if (lock != null) {
				lock.release();
			}
			if (!swapped) {
				safeDeleteQuietly(stagingDir);
				safeDeleteQuietly(backupDir);
			}
		}
		Instant completed = Instant.now();
		long sizeAfter = directorySize(sourceDir);
		metricsBuilder.fileSizeAfterBytes(sizeAfter)
				.copyDuration(Duration.between(started, completed))
				.completedAt(completed);
		LmdbCompactionMetrics metrics = metricsBuilder.build();
		options.getMetricsConsumer().accept(metrics);
		progressListener.onProgress(
				LmdbCompactionProgress.of(LmdbCompactionProgress.Phase.FINISHED, "LMDB compaction finished"));
		Path reportBackup = backupDir;
		if (!options.isKeepBackup()) {
			safeDeleteQuietly(backupDir);
			reportBackup = null;
		}
		return new LmdbCompactionReport(metrics, reportBackup, sourceDir, verificationAttempted, verificationSucceeded,
				warnings);
	}

	public Optional<LmdbCompactionReport> compactIfNeeded(LmdbCompactionOptions options, double fragmentationThreshold)
			throws IOException {
		Objects.requireNonNull(options, "options");
		double fragmentation = estimateFragmentationRatio();
		if (fragmentation >= fragmentationThreshold) {
			return Optional.of(compact(options));
		}
		return Optional.empty();
	}

	public double estimateFragmentationRatio() throws IOException {
		File dataDir = getDataDir();
		if (dataDir == null) {
			throw new IllegalStateException("Data directory is not configured");
		}
		Path sourceDir = dataDir.toPath();
		if (!Files.isDirectory(sourceDir)) {
			return 0.0;
		}
		List<Path> environments = collectEnvironmentDirectories(sourceDir);
		if (environments.isEmpty()) {
			return 0.0;
		}
		double total = 0.0;
		for (Path env : environments) {
			String label = sourceDir.relativize(env).toString();
			LmdbCompactionMetrics.EnvironmentMetrics metrics = readEnvironmentMetrics(env, label);
			total += metrics.getFreePageRatio();
		}
		return total / environments.size();
	}

	private void copyStore(Path sourceDir, Path stagingDir, LmdbCompactionProgressListener progressListener,
			LmdbCompactionMetrics.Builder metricsBuilder) throws IOException {
		progressListener.onProgress(LmdbCompactionProgress
				.of(LmdbCompactionProgress.Phase.COPYING_ENVIRONMENTS, "Copying LMDB environments"));
		List<Path> environments = collectEnvironmentDirectories(sourceDir);
		Set<Path> envSet = new HashSet<>(environments);
		Files.createDirectories(stagingDir);
		final int total = environments.size();
		final int[] counter = new int[] { 0 };
		Files.walkFileTree(sourceDir, new SimpleFileVisitor<>() {
			@Override
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
				if (dir.equals(sourceDir)) {
					return FileVisitResult.CONTINUE;
				}
				Path relative = sourceDir.relativize(dir);
				Path target = stagingDir.resolve(relative);
				if (envSet.contains(dir)) {
					int current = ++counter[0];
					String label = relative.toString();
					progressListener.onProgress(LmdbCompactionProgress.of(
							LmdbCompactionProgress.Phase.COPYING_ENVIRONMENTS, current, Math.max(total, 1),
							"Copying " + label));
					metricsBuilder.addEnvironmentBefore(readEnvironmentMetrics(dir, label));
					copyEnvironment(dir, target);
					metricsBuilder.addEnvironmentAfter(readEnvironmentMetrics(target, label));
					return FileVisitResult.SKIP_SUBTREE;
				}
				Files.createDirectories(target);
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				Path target = stagingDir.resolve(sourceDir.relativize(file));
				Files.createDirectories(target.getParent());
				Files.copy(file, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
				return FileVisitResult.CONTINUE;
			}
		});
	}

	private List<Path> collectEnvironmentDirectories(Path sourceDir) throws IOException {
		List<Path> result = new ArrayList<>();
		try (Stream<Path> stream = Files.walk(sourceDir)) {
			stream.filter(Files::isDirectory).forEach(dir -> {
				Path parent = dir.getParent();
				if (Files.exists(dir.resolve("data.mdb")) && !dir.equals(sourceDir)
						&& (parent == null || !Files.exists(parent.resolve("data.mdb")))) {
					result.add(dir);
				}
			});
		}
		result.sort(Comparator.naturalOrder());
		return result;
	}

	private void copyEnvironment(Path sourceEnv, Path targetEnv) throws IOException {
		safeDeleteQuietly(targetEnv);
		Files.createDirectories(targetEnv);
		try (MemoryStack stack = stackPush()) {
			PointerBuffer pp = stack.mallocPointer(1);
			E(mdb_env_create(pp));
			long env = pp.get(0);
			try {
				E(mdb_env_set_maxdbs(env, 64));
				int flags = MDB_NOTLS | MDB_RDONLY;
				E(mdb_env_open(env, sourceEnv.toAbsolutePath().toString(), flags, 0664));
				int rc = mdb_env_copy2(env, targetEnv.toAbsolutePath().toString(), MDB_CP_COMPACT);
				if (rc == MDB_INVALID) {
					E(mdb_env_copy2(env, targetEnv.toAbsolutePath().toString(), 0));
					shrinkEnvironment(targetEnv);
				} else {
					E(rc);
				}
			} finally {
				mdb_env_close(env);
			}
		}
	}

	private void shrinkEnvironment(Path envDir) throws IOException {
		try (MemoryStack stack = stackPush()) {
			PointerBuffer pp = stack.mallocPointer(1);
			E(mdb_env_create(pp));
			long env = pp.get(0);
			try {
				E(mdb_env_set_maxdbs(env, 64));
				E(mdb_env_open(env, envDir.toAbsolutePath().toString(), MDB_NOTLS, 0664));
				MDBStat stat = MDBStat.malloc(stack);
				E(mdb_env_stat(env, stat));
				MDBEnvInfo info = MDBEnvInfo.malloc(stack);
				mdb_env_info(env, info);
				long pageSize = stat.ms_psize();
				long usedPages = info.me_last_pgno() + 1;
				long targetSize = usedPages * pageSize;
				if (targetSize < info.me_mapsize()) {
					E(mdb_env_set_mapsize(env, targetSize));
				}
			} finally {
				mdb_env_close(env);
			}
		}
	}

	private LmdbCompactionMetrics.EnvironmentMetrics readEnvironmentMetrics(Path envDir, String label)
			throws IOException {
		try (MemoryStack stack = stackPush()) {
			PointerBuffer pp = stack.mallocPointer(1);
			E(mdb_env_create(pp));
			long env = pp.get(0);
			try {
				E(mdb_env_set_maxdbs(env, 64));
				E(mdb_env_open(env, envDir.toAbsolutePath().toString(), MDB_RDONLY | MDB_NOTLS, 0664));
				MDBStat stat = MDBStat.malloc(stack);
				E(mdb_env_stat(env, stat));
				MDBEnvInfo info = MDBEnvInfo.malloc(stack);
				mdb_env_info(env, info);
				long pageSize = stat.ms_psize();
				long totalPages = pageSize == 0 ? 0 : info.me_mapsize() / pageSize;
				return new LmdbCompactionMetrics.EnvironmentMetrics(label, info.me_mapsize(), pageSize,
						info.me_last_pgno(), totalPages, stat.ms_branch_pages(), stat.ms_leaf_pages(),
						stat.ms_overflow_pages());
			} finally {
				mdb_env_close(env);
			}
		}
	}

	private void verifyCompactedStore(Path stagingDir) throws SailException {
		LmdbStore verificationStore = new LmdbStore(stagingDir.toFile(), config);
		verificationStore.init();
		verificationStore.shutDown();
	}

	private void swapDirectories(Path sourceDir, Path stagingDir, Path backupDir) throws IOException {
		safeDeleteQuietly(backupDir);
		Files.createDirectories(backupDir.getParent());
		moveDirectory(sourceDir, backupDir);
		try {
			moveDirectory(stagingDir, sourceDir);
		} catch (IOException e) {
			moveDirectory(backupDir, sourceDir);
			throw e;
		}
	}

	private void moveDirectory(Path source, Path target) throws IOException {
		try {
			Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
		} catch (IOException e) {
			Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
		}
	}

	private void ensureDestinationIsEmpty(Path destination) throws IOException {
		if (Files.exists(destination)) {
			try (Stream<Path> stream = Files.list(destination)) {
				if (stream.findAny().isPresent()) {
					throw new IOException("Destination directory must be empty: " + destination);
				}
			}
		} else {
			Files.createDirectories(destination);
		}
	}

	private Path uniqueChild(Path parent, String prefix) throws IOException {
		Files.createDirectories(parent);
		Path candidate = parent.resolve(prefix);
		int counter = 0;
		while (Files.exists(candidate)) {
			counter++;
			candidate = parent.resolve(prefix + "-" + counter);
		}
		return candidate;
	}

	private void safeDeleteQuietly(Path path) {
		if (path == null) {
			return;
		}
		try {
			if (Files.notExists(path)) {
				return;
			}
			if (Files.isDirectory(path)) {
				FileUtils.deleteDirectory(path.toFile());
			} else {
				Files.deleteIfExists(path);
			}
		} catch (IOException e) {
			logger.warn("Failed to delete temporary directory {}", path, e);
		}
	}

	private long directorySize(Path root) throws IOException {
		try (Stream<Path> stream = Files.walk(root)) {
			return stream.filter(Files::isRegularFile).mapToLong(path -> {
				try {
					return Files.size(path);
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			}).sum();
		} catch (UncheckedIOException e) {
			throw e.getCause();
		}
	}

}
