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
package org.eclipse.rdf4j.sail.helpers;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.channels.FileChannel;
import java.util.Arrays;

import org.eclipse.rdf4j.common.concurrent.locks.Lock;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Ensures the lock returned by {@link DirectoryLockManager#tryLock()} retains only {@link FileChannel}-based state,
 * guaranteeing that the manager no longer depends on {@link java.io.RandomAccessFile}.
 */
public class DirectoryLockManagerFileChannelTest {

	@TempDir
	File tempDir;

	@Test
	public void lockCapturesFileChannelNotRandomAccessFile() {
		DirectoryLockManager manager = new DirectoryLockManager(tempDir);
		Lock lock = manager.tryLock();

		assertThat(lock).as("DirectoryLockManager should acquire a lock").isNotNull();

		Field[] fields = lock.getClass().getDeclaredFields();

		boolean hasRandomAccessFileField = Arrays.stream(fields)
				.anyMatch(field -> field.getType().getName().equals("java.io.RandomAccessFile"));
		boolean hasFileChannelField = Arrays.stream(fields)
				.anyMatch(field -> FileChannel.class.isAssignableFrom(field.getType()));

		try {
			assertThat(hasRandomAccessFileField)
					.as("Lock implementation should avoid capturing RandomAccessFile")
					.isFalse();
			assertThat(hasFileChannelField)
					.as("Lock implementation should capture FileChannel state for cleanup")
					.isTrue();
		} finally {
			if (lock != null) {
				lock.release();
			}
		}
	}
}
