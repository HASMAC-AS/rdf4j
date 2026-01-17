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

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Immutable metrics collected during an LMDB compaction run.
 */
public final class LmdbCompactionMetrics {

	private final long fileSizeBeforeBytes;
	private final long fileSizeAfterBytes;
	private final Duration copyDuration;
	private final Instant startedAt;
	private final Instant completedAt;
	private final List<EnvironmentMetrics> environmentsBefore;
	private final List<EnvironmentMetrics> environmentsAfter;

	private LmdbCompactionMetrics(Builder builder) {
		this.fileSizeBeforeBytes = builder.fileSizeBeforeBytes;
		this.fileSizeAfterBytes = builder.fileSizeAfterBytes;
		this.copyDuration = builder.copyDuration;
		this.startedAt = builder.startedAt;
		this.completedAt = builder.completedAt;
		this.environmentsBefore = Collections.unmodifiableList(new ArrayList<>(builder.environmentsBefore));
		this.environmentsAfter = Collections.unmodifiableList(new ArrayList<>(builder.environmentsAfter));
	}

	public static Builder builder() {
		return new Builder();
	}

	public long getFileSizeBeforeBytes() {
		return fileSizeBeforeBytes;
	}

	public long getFileSizeAfterBytes() {
		return fileSizeAfterBytes;
	}

	public Duration getCopyDuration() {
		return copyDuration;
	}

	public Instant getStartedAt() {
		return startedAt;
	}

	public Instant getCompletedAt() {
		return completedAt;
	}

	public List<EnvironmentMetrics> getEnvironmentsBefore() {
		return environmentsBefore;
	}

	public List<EnvironmentMetrics> getEnvironmentsAfter() {
		return environmentsAfter;
	}

	public double getTotalFreePageRatioBefore() {
		return environmentsBefore.stream().mapToDouble(EnvironmentMetrics::getFreePageRatio).average().orElse(0.0);
	}

	public double getTotalFreePageRatioAfter() {
		return environmentsAfter.stream().mapToDouble(EnvironmentMetrics::getFreePageRatio).average().orElse(0.0);
	}

	/**
	 * Builder for {@link LmdbCompactionMetrics}.
	 */
	public static final class Builder {
		private long fileSizeBeforeBytes;
		private long fileSizeAfterBytes;
		private Duration copyDuration = Duration.ZERO;
		private Instant startedAt = Instant.now();
		private Instant completedAt = Instant.now();
		private final List<EnvironmentMetrics> environmentsBefore = new ArrayList<>();
		private final List<EnvironmentMetrics> environmentsAfter = new ArrayList<>();

		private Builder() {
		}

		public Builder fileSizeBeforeBytes(long fileSizeBeforeBytes) {
			this.fileSizeBeforeBytes = fileSizeBeforeBytes;
			return this;
		}

		public Builder fileSizeAfterBytes(long fileSizeAfterBytes) {
			this.fileSizeAfterBytes = fileSizeAfterBytes;
			return this;
		}

		public Builder copyDuration(Duration copyDuration) {
			this.copyDuration = Objects.requireNonNull(copyDuration, "copyDuration");
			return this;
		}

		public Builder startedAt(Instant startedAt) {
			this.startedAt = Objects.requireNonNull(startedAt, "startedAt");
			return this;
		}

		public Builder completedAt(Instant completedAt) {
			this.completedAt = Objects.requireNonNull(completedAt, "completedAt");
			return this;
		}

		public Builder addEnvironmentBefore(EnvironmentMetrics metrics) {
			this.environmentsBefore.add(Objects.requireNonNull(metrics, "metrics"));
			return this;
		}

		public Builder addEnvironmentAfter(EnvironmentMetrics metrics) {
			this.environmentsAfter.add(Objects.requireNonNull(metrics, "metrics"));
			return this;
		}

		public LmdbCompactionMetrics build() {
			return new LmdbCompactionMetrics(this);
		}
	}

	/**
	 * Snapshot of a single LMDB environment before or after compaction.
	 */
	public static final class EnvironmentMetrics {
		private final String name;
		private final long mapSize;
		private final long pageSize;
		private final long lastPageNumber;
		private final long totalPages;
		private final long branchPages;
		private final long leafPages;
		private final long overflowPages;

		public EnvironmentMetrics(String name, long mapSize, long pageSize, long lastPageNumber, long totalPages,
				long branchPages, long leafPages, long overflowPages) {
			this.name = name;
			this.mapSize = mapSize;
			this.pageSize = pageSize;
			this.lastPageNumber = lastPageNumber;
			this.totalPages = totalPages;
			this.branchPages = branchPages;
			this.leafPages = leafPages;
			this.overflowPages = overflowPages;
		}

		public String getName() {
			return name;
		}

		public long getMapSize() {
			return mapSize;
		}

		public long getPageSize() {
			return pageSize;
		}

		public long getLastPageNumber() {
			return lastPageNumber;
		}

		public long getTotalPages() {
			return totalPages;
		}

		public long getBranchPages() {
			return branchPages;
		}

		public long getLeafPages() {
			return leafPages;
		}

		public long getOverflowPages() {
			return overflowPages;
		}

		public long getFreePages() {
			long used = Math.max(lastPageNumber, branchPages + leafPages + overflowPages);
			return Math.max(0, totalPages - used);
		}

		public double getFreePageRatio() {
			if (totalPages == 0) {
				return 0.0;
			}
			return (double) getFreePages() / (double) totalPages;
		}
	}
}
