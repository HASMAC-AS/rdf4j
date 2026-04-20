/*******************************************************************************
 * Copyright (c) 2025 Eclipse RDF4J contributors.
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
import static org.eclipse.rdf4j.sail.lmdb.LmdbUtil.readTransaction;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.util.lmdb.LMDB.MDB_CP_COMPACT;
import static org.lwjgl.util.lmdb.LMDB.MDB_NOTFOUND;
import static org.lwjgl.util.lmdb.LMDB.MDB_NOTLS;
import static org.lwjgl.util.lmdb.LMDB.MDB_RDONLY;
import static org.lwjgl.util.lmdb.LMDB.mdb_dbi_open;
import static org.lwjgl.util.lmdb.LMDB.mdb_env_close;
import static org.lwjgl.util.lmdb.LMDB.mdb_env_copy2;
import static org.lwjgl.util.lmdb.LMDB.mdb_env_create;
import static org.lwjgl.util.lmdb.LMDB.mdb_env_open;
import static org.lwjgl.util.lmdb.LMDB.mdb_env_set_maxdbs;
import static org.lwjgl.util.lmdb.LMDB.mdb_env_set_maxreaders;
import static org.lwjgl.util.lmdb.LMDB.mdb_stat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.IntBuffer;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.sail.lmdb.LmdbCompactionProgress.Stage;
import org.eclipse.rdf4j.sail.lmdb.config.LmdbStoreConfig;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.lmdb.MDBStat;

final class LmdbCompactor {

	private static final String VALUES_DIRECTORY = "values";
	private static final String TRIPLES_DIRECTORY = "triples";
	private static final String TRIPLE_PROPERTIES_FILE = "triples.prop";
	private static final String INDEXES_KEY = "triple-indexes";
	private static final DateTimeFormatter STAGING_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
			.withLocale(Locale.ROOT)
			.withZone(ZoneOffset.UTC);

	private final Path dataDir;
	private final LmdbStoreConfig config;
	private final LmdbCompactionOptions options;
	private final Consumer<LmdbCompactionProgress> progressListener;

	LmdbCompactor(Path dataDir, LmdbStoreConfig config, LmdbCompactionOptions options) {
		this.dataDir = Objects.requireNonNull(dataDir, "dataDir");
		this.config = Objects.requireNonNull(config, "config");
		this.options = Objects.requireNonNull(options, "options");
		this.progressListener = options.progressListener();
	}

	LmdbCompactionResult run() throws IOException {
		if (!Files.isDirectory(dataDir)) {
			throw new IOException("LMDB data directory does not exist: " + dataDir);
		}

		progressListener.accept(new LmdbCompactionProgress(Stage.PREPARE, "preparing staging directory", 0));
		Path stagingRoot = prepareStagingDirectory();
		copyStaticFiles(stagingRoot);

		long valuesSizeBefore = sizeOfDataFile(dataDir.resolve(VALUES_DIRECTORY));
		long triplesSizeBefore = sizeOfDataFile(dataDir.resolve(TRIPLES_DIRECTORY));

		Instant start = Instant.now();
		List<LmdbCompactionMetrics.DatabaseStats> stats = new ArrayList<>();

		progressListener.accept(new LmdbCompactionProgress(Stage.COPY_VALUES, VALUES_DIRECTORY, 0));
		EnvironmentStats valuesStats = compactValues(stagingRoot);
		stats.add(valuesStats.toDatabaseStats());

		progressListener.accept(new LmdbCompactionProgress(Stage.COPY_TRIPLES, TRIPLES_DIRECTORY, 0.5));
		EnvironmentStats triplesStats = compactTriples(stagingRoot);
		stats.add(triplesStats.toDatabaseStats());

		Duration duration = Duration.between(start, Instant.now());

		long valuesSizeAfter = sizeOfDataFile(stagingRoot.resolve(VALUES_DIRECTORY));
		long triplesSizeAfter = sizeOfDataFile(stagingRoot.resolve(TRIPLES_DIRECTORY));

		LmdbCompactionMetrics metrics = new LmdbCompactionMetrics(duration, stats);
		options.metricsConsumer().accept(metrics);

		progressListener.accept(new LmdbCompactionProgress(Stage.SWAP, "swapping directories", 0.75));
		Path backupDir = swapWithOriginal(stagingRoot);

		long bytesBefore = safeAdd(valuesSizeBefore, triplesSizeBefore);
		long bytesAfter = safeAdd(valuesSizeAfter, triplesSizeAfter);

		progressListener.accept(new LmdbCompactionProgress(Stage.COMPLETE, "complete", 1));

		return new LmdbCompactionResult(bytesBefore, bytesAfter, dataDir, backupDir, metrics);
	}

	private EnvironmentStats compactValues(Path stagingRoot) throws IOException {
		Path source = dataDir.resolve(VALUES_DIRECTORY);
		if (!Files.isDirectory(source)) {
			return EnvironmentStats.empty(VALUES_DIRECTORY);
		}
		Path target = stagingRoot.resolve(VALUES_DIRECTORY);
		Files.createDirectories(target);

		List<String> dbNames = Arrays.asList(null, "unused_ids", "free_ids", "ref_counts");
		EnvironmentStats stats = copyEnvironment(VALUES_DIRECTORY, source, target, 6, dbNames, null);
		return stats;
	}

	private EnvironmentStats compactTriples(Path stagingRoot) throws IOException {
		Path source = dataDir.resolve(TRIPLES_DIRECTORY);
		if (!Files.isDirectory(source)) {
			return EnvironmentStats.empty(TRIPLES_DIRECTORY);
		}
		Path target = stagingRoot.resolve(TRIPLES_DIRECTORY);
		Files.createDirectories(target);

		TripleLayout layout = loadTripleLayout();
		EnvironmentStats stats = copyEnvironment(TRIPLES_DIRECTORY, source, target, layout.maxDatabases(),
				layout.databaseNames(), layout.primaryDatabase());
		return stats;
	}

	private EnvironmentStats copyEnvironment(String name, Path source, Path destination, int maxDbs,
			List<String> dbNames, String primaryDb) throws IOException {
		Map<String, StatValues> beforeStats = collectStats(source, maxDbs, dbNames);
		performCopy(source, destination, maxDbs);
		Map<String, StatValues> afterStats = collectStats(destination, maxDbs, dbNames);

		if (options.verifyAfterCopy()) {
			progressListener.accept(new LmdbCompactionProgress(Stage.VERIFY, name, 0.9));
			verifyStats(name, beforeStats, afterStats);
		}

		String primaryKey = normalizeDbName(primaryDb);
		StatValues before = beforeStats.getOrDefault(primaryKey, StatValues.EMPTY);
		StatValues after = afterStats.getOrDefault(primaryKey, StatValues.EMPTY);
		long pagesBefore = beforeStats.values()
				.stream()
				.mapToLong(StatValues::totalPages)
				.sum();
		long pagesAfter = afterStats.values()
				.stream()
				.mapToLong(StatValues::totalPages)
				.sum();

		return new EnvironmentStats(name, before.entries(), after.entries(), pagesBefore, pagesAfter);
	}

	private void verifyStats(String envName, Map<String, StatValues> before, Map<String, StatValues> after)
			throws IOException {
		for (Map.Entry<String, StatValues> entry : before.entrySet()) {
			StatValues other = after.get(entry.getKey());
			if (other == null) {
				throw new IOException("Database '" + entry.getKey() + "' missing after compaction for " + envName);
			}
			if (entry.getValue().entries() != other.entries()) {
				throw new IOException("Entry count mismatch for database '" + entry.getKey() + "' in environment "
						+ envName);
			}
		}
	}

	private void performCopy(Path source, Path destination, int maxDbs) throws IOException {
		try (MemoryStack stack = stackPush()) {
			PointerBuffer ptr = stack.mallocPointer(1);
			E(mdb_env_create(ptr));
			long env = ptr.get(0);
			try {
				E(mdb_env_set_maxdbs(env, maxDbs));
				E(mdb_env_set_maxreaders(env, 512));
				int flags = MDB_RDONLY | MDB_NOTLS;
				E(mdb_env_open(env, source.toString(), flags, 0664));
				E(mdb_env_copy2(env, destination.toString(), MDB_CP_COMPACT));
			} finally {
				mdb_env_close(env);
			}
		}
	}

	private Map<String, StatValues> collectStats(Path envPath, int maxDbs, List<String> dbNames) throws IOException {
		if (!Files.isDirectory(envPath)) {
			return Collections.emptyMap();
		}
		Map<String, StatValues> result = new LinkedHashMap<>();
		long env;
		try (MemoryStack stack = stackPush()) {
			PointerBuffer ptr = stack.mallocPointer(1);
			E(mdb_env_create(ptr));
			env = ptr.get(0);
			try {
				E(mdb_env_set_maxdbs(env, maxDbs));
				E(mdb_env_set_maxreaders(env, 512));
				int flags = MDB_RDONLY | MDB_NOTLS;
				E(mdb_env_open(env, envPath.toString(), flags, 0664));
			} catch (IOException e) {
				mdb_env_close(env);
				throw e;
			}
		}

		try {
			readTransaction(env, (stack, txn) -> {
				for (String dbName : dbNames) {
					IntBuffer ip = stack.mallocInt(1);
					int rc = mdb_dbi_open(txn, dbName, 0, ip);
					if (rc == MDB_NOTFOUND) {
						continue;
					}
					E(rc);
					MDBStat stat = MDBStat.malloc(stack);
					mdb_stat(txn, ip.get(0), stat);
					result.put(normalizeDbName(dbName), new StatValues(stat.ms_entries(),
							stat.ms_branch_pages(), stat.ms_leaf_pages(), stat.ms_overflow_pages()));
				}
				return null;
			});
		} finally {
			mdb_env_close(env);
		}
		return result;
	}

	private TripleLayout loadTripleLayout() throws IOException {
		Path props = dataDir.resolve(TRIPLES_DIRECTORY).resolve(TRIPLE_PROPERTIES_FILE);
		if (!Files.exists(props)) {
			return TripleLayout.fromConfig(config);
		}
		Properties properties = new Properties();
		try (InputStream in = Files.newInputStream(props)) {
			properties.load(in);
		}
		String indexSpec = properties.getProperty(INDEXES_KEY);
		if (indexSpec == null || indexSpec.isBlank()) {
			return TripleLayout.fromConfig(config);
		}
		return TripleLayout.fromIndexString(indexSpec);
	}

	private Path prepareStagingDirectory() throws IOException {
		Path staging = options.destinationDirectory().orElseGet(() -> {
			Path base = options.temporaryDirectory().orElse(dataDir.getParent());
			String candidateName = dataDir.getFileName() + "-compact-" + STAGING_FORMAT.format(Instant.now());
			Path candidate = base.resolve(candidateName);
			AtomicInteger counter = new AtomicInteger();
			while (Files.exists(candidate)) {
				candidate = base.resolve(candidateName + "-" + counter.incrementAndGet());
			}
			return candidate;
		});

		if (Files.exists(staging)) {
			if (!Files.isDirectory(staging)) {
				throw new IOException("Destination path is not a directory: " + staging);
			}
			try (var stream = Files.list(staging)) {
				if (stream.findAny().isPresent()) {
					throw new IOException("Destination directory must be empty: " + staging);
				}
			}
		} else {
			Files.createDirectories(staging);
		}

		if (staging.startsWith(dataDir)) {
			throw new IOException("Destination directory may not be inside the LMDB data directory: " + staging);
		}
		return staging;
	}

	private void copyStaticFiles(Path stagingRoot) throws IOException {
		Set<String> envDirectories = Set.of(VALUES_DIRECTORY, TRIPLES_DIRECTORY);
		try (var stream = Files.list(dataDir)) {
			for (Path entry : stream.collect(Collectors.toList())) {
				String name = entry.getFileName().toString();
				if (envDirectories.contains(name)) {
					Files.createDirectories(stagingRoot.resolve(name));
					continue;
				}
				Path target = stagingRoot.resolve(name);
				if (Files.isDirectory(entry)) {
					copyDirectory(entry, target);
				} else {
					Files.copy(entry, target, StandardCopyOption.COPY_ATTRIBUTES,
							StandardCopyOption.REPLACE_EXISTING);
				}
			}
		}
	}

	private void copyDirectory(Path source, Path target) throws IOException {
		Files.walkFileTree(source, new SimpleFileVisitor<>() {
			@Override
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
				Path relative = source.relativize(dir);
				Files.createDirectories(target.resolve(relative));
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				Path relative = source.relativize(file);
				Files.copy(file, target.resolve(relative), StandardCopyOption.COPY_ATTRIBUTES,
						StandardCopyOption.REPLACE_EXISTING);
				return FileVisitResult.CONTINUE;
			}
		});
	}

	private Path swapWithOriginal(Path stagingRoot) throws IOException {
		Path backup = null;
		if (options.keepBackup()) {
			backup = createBackupPath();
			moveDirectory(dataDir, backup);
		} else {
			Path tempBackup = createBackupPath();
			moveDirectory(dataDir, tempBackup);
			deleteDirectory(tempBackup);
		}
		moveDirectory(stagingRoot, dataDir);
		return backup;
	}

	private Path createBackupPath() throws IOException {
		String baseName = dataDir.getFileName() + ".bak";
		Path candidate = dataDir.resolveSibling(baseName);
		int index = 1;
		while (Files.exists(candidate)) {
			candidate = dataDir.resolveSibling(baseName + "-" + index++);
		}
		return candidate;
	}

	private void moveDirectory(Path source, Path target) throws IOException {
		try {
			Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
		} catch (AtomicMoveNotSupportedException e) {
			Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
		}
	}

	private void deleteDirectory(Path path) throws IOException {
		if (!Files.exists(path)) {
			return;
		}
		Files.walkFileTree(path, new SimpleFileVisitor<>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				Files.delete(file);
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
				Files.delete(dir);
				return FileVisitResult.CONTINUE;
			}
		});
	}

	private long sizeOfDataFile(Path envDir) throws IOException {
		Path dataFile = envDir.resolve("data.mdb");
		if (Files.exists(dataFile)) {
			return Files.size(dataFile);
		}
		return 0L;
	}

	private static long safeAdd(long left, long right) {
		long result = left + right;
		if (((left ^ result) & (right ^ result)) < 0) {
			return Long.MAX_VALUE;
		}
		return result;
	}

	private static String normalizeDbName(String name) {
		return name == null ? "(main)" : name;
	}

	private static final class StatValues {

		static final StatValues EMPTY = new StatValues(0, 0, 0, 0);

		private final long entries;
		private final long branchPages;
		private final long leafPages;
		private final long overflowPages;

		StatValues(long entries, long branchPages, long leafPages, long overflowPages) {
			this.entries = entries;
			this.branchPages = branchPages;
			this.leafPages = leafPages;
			this.overflowPages = overflowPages;
		}

		long entries() {
			return entries;
		}

		long totalPages() {
			return branchPages + leafPages + overflowPages;
		}
	}

	private static final class EnvironmentStats {
		private final String name;
		private final long entriesBefore;
		private final long entriesAfter;
		private final long pagesBefore;
		private final long pagesAfter;

		private EnvironmentStats(String name, long entriesBefore, long entriesAfter, long pagesBefore,
				long pagesAfter) {
			this.name = name;
			this.entriesBefore = entriesBefore;
			this.entriesAfter = entriesAfter;
			this.pagesBefore = pagesBefore;
			this.pagesAfter = pagesAfter;
		}

		static EnvironmentStats empty(String name) {
			return new EnvironmentStats(name, 0, 0, 0, 0);
		}

		LmdbCompactionMetrics.DatabaseStats toDatabaseStats() {
			return new LmdbCompactionMetrics.DatabaseStats(name, entriesBefore, entriesAfter, pagesBefore,
					pagesAfter);
		}
	}

	private static final class TripleLayout {

		private final List<String> indexes;

		private TripleLayout(List<String> indexes) {
			this.indexes = List.copyOf(indexes);
		}

		static TripleLayout fromConfig(LmdbStoreConfig config) {
			String indexSpec = config.getTripleIndexes();
			if (indexSpec == null || indexSpec.isBlank()) {
				return defaultLayout();
			}
			return fromIndexString(indexSpec);
		}

		static TripleLayout defaultLayout() {
			return new TripleLayout(List.of("spoc", "posc", "ospc", "cspo", "cpso", "pocs"));
		}

		static TripleLayout fromIndexString(String indexString) {
			String[] parts = indexString.split("[,\\s]+");
			List<String> indexes = new ArrayList<>();
			for (String part : parts) {
				if (!part.isBlank()) {
					indexes.add(part.trim().toLowerCase(Locale.ROOT));
				}
			}
			if (indexes.isEmpty()) {
				return defaultLayout();
			}
			return new TripleLayout(indexes);
		}

		int maxDatabases() {
			// contexts + explicit/inferred per index + main
			return 2 * indexes.size() + 2;
		}

		List<String> databaseNames() {
			Set<String> names = new LinkedHashSet<>();
			names.add(null);
			names.add("contexts");
			for (String index : indexes) {
				names.add(index);
				names.add(index + "-inf");
			}
			return new ArrayList<>(names);
		}

		String primaryDatabase() {
			return indexes.isEmpty() ? null : indexes.get(0);
		}
	}
}
