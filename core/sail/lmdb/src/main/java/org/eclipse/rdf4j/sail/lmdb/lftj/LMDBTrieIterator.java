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

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.util.lmdb.LMDB.MDB_NEXT;
import static org.lwjgl.util.lmdb.LMDB.MDB_NOTFOUND;
import static org.lwjgl.util.lmdb.LMDB.MDB_SET_RANGE;
import static org.lwjgl.util.lmdb.LMDB.MDB_SUCCESS;
import static org.lwjgl.util.lmdb.LMDB.mdb_cursor_close;
import static org.lwjgl.util.lmdb.LMDB.mdb_cursor_get;
import static org.lwjgl.util.lmdb.LMDB.mdb_cursor_open;
import static org.lwjgl.util.lmdb.LMDB.mdb_strerror;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Objects;

import org.eclipse.rdf4j.sail.lmdb.Varint;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.lmdb.MDBVal;

/**
 * LMDB-backed implementation of {@link TrieIterator} for quad indexes.
 */
public class LMDBTrieIterator implements CloseableTrieIterator, QuadKeyEncoding.QuadKeySink {

	private static final int MAX_ENCODED_KEY_LENGTH = Varint
			.calcListLengthUnsigned(Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE);

	private final long cursor;

	private final int dbi;

	private final QuadKeyOrder order;

	private final Slot role;

	private final MDBVal keyVal = MDBVal.malloc();

	private final MDBVal dataVal = MDBVal.malloc();

	private final ByteBuffer seekKeyBuffer = ByteBuffer.allocateDirect(MAX_ENCODED_KEY_LENGTH);

	private boolean end = true;

	private Prefix prefix = Prefix.builder().build();

	private long currentS;

	private long currentP;

	private long currentO;

	private long currentC;

	public LMDBTrieIterator(long txn, int dbi, QuadKeyOrder order, Slot role) throws IOException {
		this.dbi = dbi;
		this.order = Objects.requireNonNull(order, "order");
		this.role = Objects.requireNonNull(role, "role");
		try (MemoryStack stack = stackPush()) {
			PointerBuffer cursorPtr = stack.mallocPointer(1);
			assertSuccess(mdb_cursor_open(txn, dbi, cursorPtr));
			this.cursor = cursorPtr.get(0);
		}
	}

	@Override
	public void open(Prefix prefix) {
		Objects.requireNonNull(prefix, "prefix");
		this.prefix = prefix;
		end = false;
		QuadKey startKey = QuadKeyEncoding.minimalKeyForPrefix(prefix);
		positionCursor(startKey.s(), startKey.p(), startKey.o(), startKey.c());
	}

	@Override
	public boolean atEnd() {
		return end;
	}

	@Override
	public long key() {
		if (end) {
			throw new IllegalStateException("Iterator is at end");
		}
		return currentValue();
	}

	@Override
	public void next() {
		if (end) {
			return;
		}

		long currentValue = currentValue();
		while (true) {
			int rc = mdb_cursor_get(cursor, keyVal, dataVal, MDB_NEXT);
			if (rc == MDB_NOTFOUND) {
				end = true;
				return;
			}
			assertSuccess(rc);
			loadCurrentKey();
			if (!matchesPrefix()) {
				end = true;
				return;
			}
			long newValue = currentValue();
			if (newValue != currentValue) {
				return;
			}
		}
	}

	@Override
	public void seek(long value) {
		if (end) {
			return;
		}
		if (value <= currentValue()) {
			return;
		}
		long s = prefix.hasSubject() ? prefix.subject() : QuadKeyEncoding.MIN_TERM_ID;
		long p = prefix.hasPredicate() ? prefix.predicate() : QuadKeyEncoding.MIN_TERM_ID;
		long o = prefix.hasObject() ? prefix.object() : QuadKeyEncoding.MIN_TERM_ID;
		long c = prefix.hasContext() ? prefix.context() : QuadKeyEncoding.MIN_TERM_ID;
		switch (role) {
		case S:
			s = value;
			break;
		case P:
			p = value;
			break;
		case O:
			o = value;
			break;
		case C:
			c = value;
			break;
		default:
			throw new IllegalStateException("Unexpected slot: " + role);
		}
		positionCursor(s, p, o, c);
	}

	@Override
	public void close() {
		mdb_cursor_close(cursor);
	}

	public Slot slot() {
		return role;
	}

	@Override
	public int slotDbi() {
		return dbi;
	}

	@Override
	public QuadKeyOrder slotOrder() {
		return order;
	}

	private void positionCursor(long s, long p, long o, long c) {
		seekKeyBuffer.clear();
		int encodedLength = QuadKeyEncoding.encodeFieldsInto(s, p, o, c, order, seekKeyBuffer);
		seekKeyBuffer.flip();
		keyVal.mv_data(seekKeyBuffer);
		keyVal.mv_size(encodedLength);
		dataVal.mv_data((ByteBuffer) null);
		dataVal.mv_size(0);
		int rc = mdb_cursor_get(cursor, keyVal, dataVal, MDB_SET_RANGE);
		while (true) {
			if (rc == MDB_NOTFOUND) {
				end = true;
				return;
			}
			assertSuccess(rc);
			loadCurrentKey();
			if (matchesPrefix()) {
				end = false;
				return;
			}
			rc = mdb_cursor_get(cursor, keyVal, dataVal, MDB_NEXT);
		}
	}

	private void loadCurrentKey() {
		ByteBuffer buffer = keyVal.mv_data();
		int len = (int) keyVal.mv_size();
		buffer.limit(len);
		buffer.position(0);
		QuadKeyEncoding.decodeInto(buffer, order, this);
	}

	private void assertSuccess(int rc) {
		if (rc != MDB_SUCCESS) {
			throw new IllegalStateException(mdb_strerror(rc));
		}
	}

	private boolean matchesPrefix() {
		if (prefix.hasSubject() && currentS != prefix.subject()) {
			return false;
		}
		if (prefix.hasPredicate() && currentP != prefix.predicate()) {
			return false;
		}
		if (prefix.hasObject() && currentO != prefix.object()) {
			return false;
		}
		if (prefix.hasContext() && currentC != prefix.context()) {
			return false;
		}
		return true;
	}

	private long currentValue() {
		switch (role) {
		case S:
			return currentS;
		case P:
			return currentP;
		case O:
			return currentO;
		case C:
			return currentC;
		default:
			throw new IllegalStateException("Unexpected slot: " + role);
		}
	}

	@Override
	public void set(long s, long p, long o, long c) {
		this.currentS = s;
		this.currentP = p;
		this.currentO = o;
		this.currentC = c;
	}
}
