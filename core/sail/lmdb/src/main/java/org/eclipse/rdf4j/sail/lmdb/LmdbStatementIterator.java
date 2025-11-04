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

import java.io.IOException;
import java.util.NoSuchElementException;

import org.eclipse.rdf4j.common.iteration.AbstractCloseableIteration;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.sail.SailException;

/**
 * A statement iterator that wraps a RecordIterator containing statement records and translates these records to
 * {@link Statement} objects.
 */
class LmdbStatementIterator extends AbstractCloseableIteration<Statement> {

	private static final int DEFAULT_BATCH_SIZE = 16;

	/*-----------*
	 * Variables *
	 *-----------*/

	private final RecordIterator recordIt;

	private final ValueStore valueStore;
	private Statement nextElement;
	private final Statement[] statementBatch;
	private final long[] quadBatch;
	private final Value[] valueBatch;
	private int batchIndex;
	private int batchCount;

	/*--------------*
	 * Constructors *
	 *--------------*/

	/**
	 * Creates a new LmdbStatementIterator.
	 */
	public LmdbStatementIterator(RecordIterator recordIt, ValueStore valueStore) {
		this.recordIt = recordIt;
		this.valueStore = valueStore;
		this.statementBatch = new Statement[DEFAULT_BATCH_SIZE];
		this.quadBatch = new long[DEFAULT_BATCH_SIZE * 4];
		this.valueBatch = new Value[DEFAULT_BATCH_SIZE * 4];
	}

	/*---------*
	 * Methods *
	 *---------*/

	public Statement getNextElement() throws SailException {
		try {
			if (batchIndex >= batchCount) {
				batchCount = fillStatementBatch(statementBatch, 0, statementBatch.length);
				batchIndex = 0;
				if (batchCount <= 0) {
					return null;
				}
			}
			Statement result = statementBatch[batchIndex];
			statementBatch[batchIndex] = null;
			batchIndex++;
			return result;
		} catch (IOException e) {
			throw causeIOException(e);
		}
	}

	@Override
	protected void handleClose() throws SailException {
		recordIt.close();
	}

	private SailException causeIOException(IOException e) {
		return new SailException(e);
	}

	@Override
	public final boolean hasNext() {
		if (isClosed()) {
			return false;
		}

		return lookAhead() != null;
	}

	@Override
	public final Statement next() {
		if (isClosed()) {
			throw new NoSuchElementException("The iteration has been closed.");
		}
		Statement result = lookAhead();

		if (result != null) {
			nextElement = null;
			return result;
		} else {
			throw new NoSuchElementException();
		}
	}

	/**
	 * Fetches the next element if it hasn't been fetched yet and stores it in {@link #nextElement}.
	 *
	 * @return The next element, or null if there are no more results.
	 */
	private Statement lookAhead() {
		if (nextElement == null) {
			nextElement = getNextElement();

			if (nextElement == null) {
				close();
			}
		}
		return nextElement;
	}

	/**
	 * Throws an {@link UnsupportedOperationException}.
	 */
	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}

	private int fillStatementBatch(Statement[] statements, int offset, int maxStatements) throws IOException {
		if (maxStatements <= 0) {
			return 0;
		}

		if (offset < 0 || offset > statements.length) {
			throw new IllegalArgumentException("Offset outside of statements array");
		}

		int capacity = Math.min(maxStatements, quadBatch.length / 4);
		capacity = Math.min(capacity, statements.length - offset);
		if (capacity <= 0) {
			return 0;
		}

		int count = recordIt.fillBatch(quadBatch, 0, capacity);
		if (count <= 0) {
			return count;
		}

		int resolved = valueStore.bulkGetLazyValues(quadBatch, 0, count * 4, valueBatch);
		if (resolved != count * 4) {
			throw new IOException("Failed to resolve all values for the requested statements batch");
		}

		for (int i = 0; i < count; i++) {
			int base = i * 4;
			Resource subj = (Resource) valueBatch[base];
			IRI pred = (IRI) valueBatch[base + 1];
			Value obj = valueBatch[base + 2];
			Resource context = null;
			long contextId = quadBatch[base + 3];
			if (contextId != 0) {
				context = (Resource) valueBatch[base + 3];
			}
			statements[offset + i] = valueStore.createStatement(subj, pred, obj, context);

			valueBatch[base] = null;
			valueBatch[base + 1] = null;
			valueBatch[base + 2] = null;
			valueBatch[base + 3] = null;
		}
		return count;
	}
}
