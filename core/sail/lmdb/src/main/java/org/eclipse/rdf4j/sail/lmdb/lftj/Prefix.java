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
package org.eclipse.rdf4j.sail.lmdb.lftj;

import java.util.OptionalLong;

/**
 * Encapsulates bound quad components for a specific pattern prefix.
 */
public final class Prefix {
	private final OptionalLong subject;
	private final OptionalLong predicate;
	private final OptionalLong object;
	private final OptionalLong context;

	private Prefix(Builder builder) {
		this.subject = builder.subject;
		this.predicate = builder.predicate;
		this.object = builder.object;
		this.context = builder.context;
	}

	public static Builder builder() {
		return new Builder();
	}

	public boolean hasSubject() {
		return subject.isPresent();
	}

	public long subject() {
		return subject.orElseThrow();
	}

	public boolean hasPredicate() {
		return predicate.isPresent();
	}

	public long predicate() {
		return predicate.orElseThrow();
	}

	public boolean hasObject() {
		return object.isPresent();
	}

	public long object() {
		return object.orElseThrow();
	}

	public boolean hasContext() {
		return context.isPresent();
	}

	public long context() {
		return context.orElseThrow();
	}

	public static final class Builder {
		private OptionalLong subject = OptionalLong.empty();
		private OptionalLong predicate = OptionalLong.empty();
		private OptionalLong object = OptionalLong.empty();
		private OptionalLong context = OptionalLong.empty();

		public Builder subject(long value) {
			this.subject = OptionalLong.of(value);
			return this;
		}

		public Builder predicate(long value) {
			this.predicate = OptionalLong.of(value);
			return this;
		}

		public Builder object(long value) {
			this.object = OptionalLong.of(value);
			return this;
		}

		public Builder context(long value) {
			this.context = OptionalLong.of(value);
			return this;
		}

		public Prefix build() {
			return new Prefix(this);
		}
	}
}
