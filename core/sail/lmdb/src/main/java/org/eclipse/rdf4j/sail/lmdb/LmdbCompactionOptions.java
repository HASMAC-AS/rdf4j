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

import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Options that control how an LMDB compaction run is executed.
 */
public final class LmdbCompactionOptions {

	private final Path destinationDirectory;
	private final Path temporaryDirectory;
	private final boolean verifyAfterCopy;
	private final boolean keepBackup;
	private final Consumer<LmdbCompactionProgress> progressListener;
	private final Consumer<LmdbCompactionMetrics> metricsConsumer;

	private LmdbCompactionOptions(Builder builder) {
		this.destinationDirectory = builder.destinationDirectory;
		this.temporaryDirectory = builder.temporaryDirectory;
		this.verifyAfterCopy = builder.verifyAfterCopy;
		this.keepBackup = builder.keepBackup;
		this.progressListener = builder.progressListener;
		this.metricsConsumer = builder.metricsConsumer;
	}

	public static Builder builder() {
		return new Builder();
	}

	public Optional<Path> destinationDirectory() {
		return Optional.ofNullable(destinationDirectory);
	}

	public Optional<Path> temporaryDirectory() {
		return Optional.ofNullable(temporaryDirectory);
	}

	public boolean verifyAfterCopy() {
		return verifyAfterCopy;
	}

	public boolean keepBackup() {
		return keepBackup;
	}

	public Consumer<LmdbCompactionProgress> progressListener() {
		return progressListener;
	}

	public Consumer<LmdbCompactionMetrics> metricsConsumer() {
		return metricsConsumer;
	}

	public static final class Builder {

		private Path destinationDirectory;
		private Path temporaryDirectory;
		private boolean verifyAfterCopy;
		private boolean keepBackup = true;
		private Consumer<LmdbCompactionProgress> progressListener = progress -> {
		};
		private Consumer<LmdbCompactionMetrics> metricsConsumer = metrics -> {
		};

		private Builder() {
		}

		public Builder destinationDirectory(Path destinationDirectory) {
			this.destinationDirectory = Objects.requireNonNull(destinationDirectory, "destinationDirectory");
			return this;
		}

		public Builder temporaryDirectory(Path temporaryDirectory) {
			this.temporaryDirectory = Objects.requireNonNull(temporaryDirectory, "temporaryDirectory");
			return this;
		}

		public Builder verifyAfterCopy(boolean verifyAfterCopy) {
			this.verifyAfterCopy = verifyAfterCopy;
			return this;
		}

		public Builder keepBackup(boolean keepBackup) {
			this.keepBackup = keepBackup;
			return this;
		}

		public Builder progressListener(Consumer<LmdbCompactionProgress> progressListener) {
			this.progressListener = Objects.requireNonNull(progressListener, "progressListener");
			return this;
		}

		public Builder metricsConsumer(Consumer<LmdbCompactionMetrics> metricsConsumer) {
			this.metricsConsumer = Objects.requireNonNull(metricsConsumer, "metricsConsumer");
			return this;
		}

		public LmdbCompactionOptions build() {
			return new LmdbCompactionOptions(this);
		}
	}
}
