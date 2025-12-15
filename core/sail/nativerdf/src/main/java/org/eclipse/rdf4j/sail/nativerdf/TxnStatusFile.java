/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.nativerdf;

import static java.nio.charset.StandardCharsets.US_ASCII;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.EnumMap;

import org.eclipse.rdf4j.common.io.NioFile;

/**
 * Writes transaction statuses to a file.
 */
class TxnStatusFile {

	boolean disabled = false;

	public void disable() {
		this.disabled = true;
		this.currentStatusCache = TxnStatus.NONE;
	}

	public enum TxnStatus {

		/**
		 * No active transaction. This occurs if no transaction has been started yet, or if all transactions have been
		 * committed or rolled back. An empty TxnStatus file also represents the NONE status.
		 */
		NONE(TxnStatus.NONE_BYTE),

		/**
		 * A transaction has been started, but was not yet committed or rolled back.
		 */
		ACTIVE(TxnStatus.ACTIVE_BYTE),

		/**
		 * A transaction is being committed.
		 */
		COMMITTING(TxnStatus.COMMITTING_BYTE),

		/**
		 * A transaction is being rolled back.
		 */
		ROLLING_BACK(TxnStatus.ROLLING_BACK_BYTE),

		/**
		 * The transaction status is unknown.
		 */
		UNKNOWN(TxnStatus.UNKNOWN_BYTE);

		private final byte[] onDisk;

		TxnStatus(byte onDisk) {
			this.onDisk = new byte[1];
			this.onDisk[0] = onDisk;
		}

		byte[] getOnDisk() {
			return onDisk;
		}

		private static final byte NONE_BYTE = (byte) 0b00000000;
		private static final byte OLD_NONE_BYTE = (byte) 0b00000001;

		private static final byte ACTIVE_BYTE = (byte) 0b00000010;
		private static final byte COMMITTING_BYTE = (byte) 0b00000100;
		private static final byte ROLLING_BACK_BYTE = (byte) 0b00001000;
		private static final byte UNKNOWN_BYTE = (byte) 0b00010000;

	}

	/**
	 * The name of the transaction status file.
	 */
	public static final String FILE_NAME = "txn-status";

	private final File statusFile;
	private final Path statusPath;
	private final EnumMap<TxnStatus, Path> variantPaths = new EnumMap<>(TxnStatus.class);
	private NioFile nioFile;
	private TxnStatus currentStatusCache;

	/**
	 * Creates a new transaction status file. New files are initialized with {@link TxnStatus#NONE}.
	 *
	 * @param dataDir The directory for the transaction status file.
	 * @throws IOException If the file did not yet exist and could not be written to.
	 */
	public TxnStatusFile(File dataDir) throws IOException {
		this.statusFile = new File(dataDir, FILE_NAME);
		this.statusPath = statusFile.toPath();

		Files.createDirectories(statusPath.getParent());

		for (TxnStatus status : TxnStatus.values()) {
			variantPaths.put(status, statusPath.resolveSibling(FILE_NAME + "." + status.name()));
		}

		nioFile = new NioFile(statusFile, "rwd");
		initializeVariantFiles();
	}

	public void close() throws IOException {
		nioFile.close();
	}

	/**
	 * Writes the specified transaction status to file.
	 *
	 * @param txnStatus The transaction status to write.
	 * @throws IOException If the transaction status could not be written to file.
	 */
	public synchronized void setTxnStatus(TxnStatus txnStatus) throws IOException {
		if (disabled) {
			return;
		}

		TxnStatus currentStatus = currentStatusCache != null ? currentStatusCache : getTxnStatus();

		if (currentStatus == txnStatus) {
			return;
		}

		Path currentVariant = variantPaths.get(currentStatus);
		Path newVariant = variantPaths.get(txnStatus);

		ensureVariantFile(newVariant, txnStatus);

		nioFile.close();

		boolean movedCurrent = false;
		try {
			move(statusPath, currentVariant);
			movedCurrent = true;
			move(newVariant, statusPath);
		} catch (IOException e) {
			if (movedCurrent) {
				try {
					move(currentVariant, statusPath);
				} catch (IOException suppressed) {
					e.addSuppressed(suppressed);
				}
			}
			throw e;
		} finally {
			nioFile = new NioFile(statusFile, "rwd");
		}

		currentStatusCache = txnStatus;
	}

	/**
	 * Reads the transaction status from file.
	 *
	 * @return The read transaction status, or {@link TxnStatus#UNKNOWN} when the file contains an unrecognized status
	 *         string.
	 * @throws IOException If the transaction status file could not be read.
	 */
	public synchronized TxnStatus getTxnStatus() throws IOException {
		if (disabled) {
			currentStatusCache = TxnStatus.NONE;
			return currentStatusCache;
		}
		byte[] bytes;
		try {
			bytes = nioFile.readBytes(0, 1);
		} catch (EOFException e) {
			// empty file = NONE status
			currentStatusCache = TxnStatus.NONE;
			return currentStatusCache;
		}

		TxnStatus status;

		switch (bytes[0]) {
		case TxnStatus.NONE_BYTE:
			status = TxnStatus.NONE;
			break;
		case TxnStatus.OLD_NONE_BYTE:
			status = TxnStatus.NONE;
			break;
		case TxnStatus.ACTIVE_BYTE:
			status = TxnStatus.ACTIVE;
			break;
		case TxnStatus.COMMITTING_BYTE:
			status = TxnStatus.COMMITTING;
			break;
		case TxnStatus.ROLLING_BACK_BYTE:
			status = TxnStatus.ROLLING_BACK;
			break;
		case TxnStatus.UNKNOWN_BYTE:
			status = TxnStatus.UNKNOWN;
			break;
		default:
			status = getTxnStatusDeprecated();
		}

		currentStatusCache = status;

		return status;

	}

	private TxnStatus getTxnStatusDeprecated() throws IOException {
		if (disabled) {
			return TxnStatus.NONE;
		}

		byte[] bytes = nioFile.readBytes(0, (int) nioFile.size());

		String s = new String(bytes, US_ASCII);
		try {
			return TxnStatus.valueOf(s);
		} catch (IllegalArgumentException e) {
			// use platform encoding for backwards compatibility with versions
			// older than 2.6.6:
			s = new String(bytes);
			try {
				return TxnStatus.valueOf(s);
			} catch (IllegalArgumentException e2) {
				return TxnStatus.UNKNOWN;
			}
		}
	}

	private void initializeVariantFiles() throws IOException {
		currentStatusCache = getTxnStatus();

		for (TxnStatus status : TxnStatus.values()) {
			Path variantPath = variantPaths.get(status);
			writeVariantFile(status, variantPath);
		}
	}

	private void ensureVariantFile(Path variant, TxnStatus status) throws IOException {
		if (Files.exists(variant)) {
			return;
		}

		writeVariantFile(status, variant);
	}

	private void writeVariantFile(TxnStatus status, Path variant) throws IOException {
		Files.createDirectories(variant.getParent());

		byte[] contents = status == TxnStatus.NONE ? new byte[0] : status.getOnDisk();

		Files.write(variant, contents, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
	}

	private void move(Path source, Path target) throws IOException {
		try {
			Files.move(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
		} catch (AtomicMoveNotSupportedException e) {
			Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
		}
	}
}
