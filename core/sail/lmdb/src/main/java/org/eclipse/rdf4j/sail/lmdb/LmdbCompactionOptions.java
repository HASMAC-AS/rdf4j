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
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Fluent options used to drive a single LMDB compaction run.
 */
public final class LmdbCompactionOptions {

	private final Path destinationDirectory;
	private final Path temporaryDirectory;
	private final boolean verifyAfterCopy;
	private final boolean keepBackup;
	private final LmdbCompactionProgressListener progressListener;
	private final Consumer<LmdbCompactionMetrics> metricsConsumer;
	private final Duration verificationTimeout;
	private final String jobId;

	private LmdbCompactionOptions(Builder builder) {
		this.destinationDirectory = Objects.requireNonNull(builder.destinationDirectory, "destinationDirectory");
		this.temporaryDirectory = Objects.requireNonNull(builder.temporaryDirectory, "temporaryDirectory");
		this.verifyAfterCopy = builder.verifyAfterCopy;
		this.keepBackup = builder.keepBackup;
		this.progressListener = builder.progressListener;
		this.metricsConsumer = builder.metricsConsumer;
		this.verificationTimeout = builder.verificationTimeout;
		this.jobId = builder.jobId != null ? builder.jobId : UUID.randomUUID().toString();
	}

	public static Builder builder() {
		return new Builder();
	}

	public Path getDestinationDirectory() {
		return destinationDirectory;
	}

	public Path getTemporaryDirectory() {
		return temporaryDirectory;
	}

	public boolean isVerifyAfterCopy() {
		return verifyAfterCopy;
	}

	public boolean isKeepBackup() {
		return keepBackup;
	}

	public LmdbCompactionProgressListener getProgressListener() {
		return progressListener;
	}

	public Consumer<LmdbCompactionMetrics> getMetricsConsumer() {
		return metricsConsumer;
	}

	public Duration getVerificationTimeout() {
		return verificationTimeout;
	}

	public String getJobId() {
		return jobId;
	}

	/** Builder for {@link LmdbCompactionOptions}. */
	public static final class Builder {
		private Path destinationDirectory;
		private Path temporaryDirectory;
		private boolean verifyAfterCopy;
		private boolean keepBackup = true;
		private LmdbCompactionProgressListener progressListener = progress -> {
		};
		private Consumer<LmdbCompactionMetrics> metricsConsumer = metrics -> {
		};
		private Duration verificationTimeout = Duration.ofMinutes(10);
		private String jobId;

		private Builder() {
		}

		public Builder destinationDirectory(Path destinationDirectory) {
			this.destinationDirectory = destinationDirectory;
			if (this.temporaryDirectory == null && destinationDirectory != null) {
				this.temporaryDirectory = destinationDirectory.getParent();
			}
			return this;
		}

		public Builder temporaryDirectory(Path temporaryDirectory) {
			this.temporaryDirectory = temporaryDirectory;
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

		public Builder progressListener(LmdbCompactionProgressListener progressListener) {
			this.progressListener = Objects.requireNonNull(progressListener, "progressListener");
			return this;
		}

		public Builder metricsConsumer(Consumer<LmdbCompactionMetrics> metricsConsumer) {
			this.metricsConsumer = Objects.requireNonNull(metricsConsumer, "metricsConsumer");
			return this;
		}

		public Builder verificationTimeout(Duration verificationTimeout) {
			this.verificationTimeout = Objects.requireNonNull(verificationTimeout, "verificationTimeout");
			return this;
		}

		public Builder jobId(String jobId) {
			this.jobId = jobId;
			return this;
		}

		public LmdbCompactionOptions build() {
			if (destinationDirectory == null) {
				throw new IllegalStateException("Destination directory must be provided");
			}
			if (temporaryDirectory == null) {
				throw new IllegalStateException("Temporary directory must be provided");
			}
			return new LmdbCompactionOptions(this);
		}
	}

	public Optional<String> getJobIdOptional() {
		return Optional.of(jobId);
	}
}
