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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.eclipse.rdf4j.sail.lmdb.config.LmdbStoreConfig;

/**
 * Command line entry point for LMDB maintenance operations.
 */
public final class LmdbMaintenanceCli {

	private LmdbMaintenanceCli() {
	}

	public static void main(String[] args) {
		try {
			new Runner(args).run();
		} catch (Exception e) {
			System.err.println("LMDB maintenance failed: " + e.getMessage());
			e.printStackTrace(System.err);
			System.exit(1);
		}
	}

	private static final class Runner {
		private final List<String> args;

		private Runner(String[] args) {
			this.args = List.of(args);
		}

		void run() throws IOException {
			if (args.contains("--help") || args.isEmpty()) {
				printUsage();
				return;
			}

			boolean compactRequested = false;
			boolean verify = false;
			boolean keepBackup = true;
			Double threshold = null;
			Path dataDir = null;
			Path destination = null;
			Path temporary = null;

			for (int i = 0; i < args.size(); i++) {
				String arg = args.get(i);
				switch (arg) {
				case "--compact":
					compactRequested = true;
					break;
				case "--dataDir":
					dataDir = Paths.get(requireValue(i++));
					break;
				case "--target":
					destination = Paths.get(requireValue(i++));
					break;
				case "--temporary":
					temporary = Paths.get(requireValue(i++));
					break;
				case "--threshold":
					String thresholdValue = requireValue(i++);
					threshold = Double.parseDouble(thresholdValue);
					break;
				case "--verify":
					verify = true;
					break;
				case "--no-backup":
					keepBackup = false;
					break;
				case "--keep-backup":
					keepBackup = true;
					break;
				default:
					if (arg.startsWith("--")) {
						throw new IllegalArgumentException("Unknown option " + arg);
					}
				}
			}

			if (!compactRequested) {
				printUsage();
				return;
			}
			if (dataDir == null) {
				throw new IllegalArgumentException("--dataDir must be provided");
			}
			if (!Files.isDirectory(dataDir)) {
				throw new IllegalArgumentException("Data directory " + dataDir + " does not exist");
			}

			if (destination == null) {
				destination = dataDir.resolveSibling(dataDir.getFileName() + "-compacted");
			}
			if (temporary == null) {
				Path parent = destination.getParent();
				temporary = parent != null ? parent : destination;
			}

			LmdbStoreConfig config = new LmdbStoreConfig();
			LmdbStore store = new LmdbStore(dataDir.toFile(), config);
			double fragmentation = store.estimateFragmentationRatio();
			System.out.printf(Locale.ROOT, "Detected fragmentation ratio: %.4f%n", fragmentation);

			if (threshold != null && fragmentation < threshold) {
				System.out.printf(Locale.ROOT,
						"Fragmentation %.4f below threshold %.4f; skipping compaction.%n",
						fragmentation, threshold);
				return;
			}

			LmdbCompactionOptions options = LmdbCompactionOptions.builder()
					.destinationDirectory(destination)
					.temporaryDirectory(temporary)
					.verifyAfterCopy(verify)
					.keepBackup(keepBackup)
					.build();

			LmdbCompactionReport report = store.compact(options);
			printReport(report);
		}

		private void printReport(LmdbCompactionReport report) {
			LmdbCompactionMetrics metrics = report.getMetrics();
			System.out.println("Compaction completed successfully.");
			System.out.printf(Locale.ROOT, "  Started:   %s%n", metrics.getStartedAt());
			System.out.printf(Locale.ROOT, "  Completed: %s%n", metrics.getCompletedAt());
			System.out.printf(Locale.ROOT, "  Duration:  %s%n", metrics.getCopyDuration());
			System.out.printf(Locale.ROOT, "  Size:      %s -> %s%n",
					formatSize(metrics.getFileSizeBeforeBytes()),
					formatSize(metrics.getFileSizeAfterBytes()));
			System.out.printf(Locale.ROOT, "  Free page ratio: %.4f -> %.4f%n",
					metrics.getTotalFreePageRatioBefore(), metrics.getTotalFreePageRatioAfter());
			report.getBackupDirectory().ifPresent(path -> System.out.println("  Backup:    " + path));
		}

		private String formatSize(long bytes) {
			final String[] units = { "B", "KiB", "MiB", "GiB", "TiB" };
			double size = bytes;
			int unit = 0;
			while (size >= 1024 && unit < units.length - 1) {
				size /= 1024;
				unit++;
			}
			return String.format(Locale.ROOT, "%.2f %s", size, units[unit]);
		}

		private String requireValue(int index) {
			if (index + 1 >= args.size()) {
				throw new IllegalArgumentException("Missing value for " + args.get(index));
			}
			return args.get(index + 1);
		}

		private void printUsage() {
			List<String> lines = new ArrayList<>();
			lines.add("Usage: rdf4j-lmdb-maintenance.sh --compact --dataDir <path> [options]");
			lines.add("Options:");
			lines.add("  --dataDir <path>          LMDB data directory");
			lines.add("  --compact                 Run compaction");
			lines.add("  --target <path>           Destination directory for staged copy");
			lines.add("  --temporary <path>        Temporary directory used during swap");
			lines.add("  --threshold <ratio>       Minimum fragmentation ratio required to compact");
			lines.add("  --verify                  Verify staged copy before swap");
			lines.add("  --no-backup               Remove backup once compaction succeeds");
			lines.add("  --keep-backup             Retain backup (default)");
			lines.add("  --help                    Show this help message");
			lines.forEach(System.out::println);
		}
	}
}
