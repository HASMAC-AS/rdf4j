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

import java.io.Closeable;

/**
 * An iterator that iterates over records, for example those in a key-value database.
 */
interface RecordIterator extends Closeable {

	/**
	 * Returns the next record.
	 *
	 * @return A record that or <tt>null</tt> if all records have been returned.
	 */
	long[] next();

	/**
	 * Fills a caller-provided buffer with up to {@code maxQuads} records. The buffer stores quads packed as consecutive
	 * {@code long} values using the order {@code [subj, pred, obj, context]} per record.
	 *
	 * <p>
	 * The default implementation falls back to repeated calls to {@link #next()}.
	 * </p>
	 *
	 * @param quads      target buffer for decoded quads
	 * @param quadOffset offset (in {@code long} elements) into {@code quads} where the first quad should be written
	 * @param maxQuads   maximum number of quads to decode into {@code quads}
	 * @return the number of quads that were written to {@code quads}; {@code 0} indicates that no further records are
	 *         available
	 */
	default int fillBatch(long[] quads, int quadOffset, int maxQuads) {
		if (maxQuads <= 0) {
			return 0;
		}

		int remaining = quads.length - quadOffset;
		if (remaining < 4) {
			return 0;
		}

		int capacity = remaining / 4;
		if (capacity <= 0) {
			return 0;
		}

		int limit = Math.min(maxQuads, capacity);
		int loaded = 0;
		int offset = quadOffset;
		long[] record;
		while (loaded < limit && (record = next()) != null) {
			System.arraycopy(record, 0, quads, offset, record.length);
			offset += record.length;
			loaded++;
		}
		return loaded;
	}

	/**
	 * Closes the iterator, freeing any resources that it uses. Once closed, the iterator will not return any more
	 * records.
	 *
	 */
	@Override
	void close();
}
