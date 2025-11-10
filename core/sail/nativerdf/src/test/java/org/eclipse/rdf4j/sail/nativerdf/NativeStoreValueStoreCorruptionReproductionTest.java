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
package org.eclipse.rdf4j.sail.nativerdf;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Isolated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reproduces a corrupt ValueStore by tampering with the values.dat type byte and verifies that reading statements fails
 * when soft-fail is disabled.
 */
@Isolated
public class NativeStoreValueStoreCorruptionReproductionTest {

	private static final Logger logger = LoggerFactory.getLogger(NativeStoreValueStoreCorruptionReproductionTest.class);
	private static final String REPOSITORY_DEBUG_PROPERTY = "org.eclipse.rdf4j.repository.debug";
	private static final long CORRUPTION_OFFSET = 174;

	@TempDir
	File tempFolder;

	private SailRepository repo;

	private File dataDir;

	private final ValueFactory F = SimpleValueFactory.getInstance();

	@BeforeEach
	public void setup() {
		dataDir = new File(tempFolder, "dbmodel");
		dataDir.mkdir();
		repo = new SailRepository(new NativeStore(dataDir, "spoc,posc"));
		repo.init();
		logger.info("Initialized NativeStore in {}", dataDir.getAbsolutePath());

		// Insert the same base dataset used by NativeSailStoreCorruptionTest to ensure stable file layout
		IRI CTX_1 = F.createIRI("urn:one");
		IRI CTX_2 = F.createIRI("urn:two");

		Statement S0 = F.createStatement(F.createIRI("http://example.org/a0"), RDFS.LABEL, F.createLiteral("zero"));
		Statement S1 = F.createStatement(F.createIRI("http://example.org/b1"), RDFS.LABEL, F.createLiteral("one"));
		Statement S2 = F.createStatement(F.createIRI("http://example.org/c2"), RDFS.LABEL, F.createLiteral("two"));
		Statement S3 = F.createStatement(Values.bnode(), RDF.TYPE, Values.bnode());
		Statement S4 = F.createStatement(F.createIRI("http://example.org/c2"), RDFS.LABEL,
				F.createLiteral("two", "en"));
		Statement S5 = F.createStatement(F.createIRI("http://example.org/c2"), RDFS.LABEL, F.createLiteral(1.2));

		try (RepositoryConnection conn = repo.getConnection()) {
			conn.add(S0);
			conn.add(S1, CTX_1);
			conn.add(S2, CTX_2);
			conn.add(S2, CTX_2);
			conn.add(S3, CTX_2);
			conn.add(S4, CTX_2);
			conn.add(S5, CTX_2);
		}

		File valuesFile = new File(dataDir, "values.dat");
		logger.info("After dataset load values.dat exists={} length={} bytes", valuesFile.exists(), valuesFile.length());
	}

	@AfterEach
	public void tearDown() {
		repo.shutDown();
		NativeStore.SOFT_FAIL_ON_CORRUPT_DATA_AND_REPAIR_INDEXES = false;
	}

	@Test
	public void corruptValuesDatInvalidTypeShouldBreakReads() throws IOException {
		String previousDebugProperty = System.getProperty(REPOSITORY_DEBUG_PROPERTY);
		System.setProperty(REPOSITORY_DEBUG_PROPERTY, "true");
		logger.info("Enabled '{}' system property for extra diagnostics (previous value: {})",
				REPOSITORY_DEBUG_PROPERTY, previousDebugProperty);

		// Disable soft-fail to surface corruption as an exception
		NativeStore.SOFT_FAIL_ON_CORRUPT_DATA_AND_REPAIR_INDEXES = false;

		// Close repo to release files for mutation
		File valuesFile = new File(dataDir, "values.dat");
		logger.info("Preparing to corrupt {} (exists={} length={} bytes)", valuesFile.getAbsolutePath(),
				valuesFile.exists(), valuesFile.length());
		repo.shutDown();
		logger.info("Repository shut down to allow direct file mutation");

		// Flip a byte in values.dat at a position that maps to a value type marker
		// This offset mirrors NativeSailStoreCorruptionTest.testCorruptValuesDatFileInvalidTypeError
		byte originalByte = peekByte(valuesFile, CORRUPTION_OFFSET);
		logger.info("Original byte at offset {}: {} (unsigned={})", CORRUPTION_OFFSET, formatByte(originalByte),
				Byte.toUnsignedInt(originalByte));
		overwriteByte(valuesFile, CORRUPTION_OFFSET, 0x0);
		byte mutatedByte = peekByte(valuesFile, CORRUPTION_OFFSET);
		logger.info("Mutated byte at offset {}: {} (unsigned={})", CORRUPTION_OFFSET, formatByte(mutatedByte),
				Byte.toUnsignedInt(mutatedByte));

		// Reopen; attempting to read statements should now throw a RepositoryException
		repo.init();
		logger.info("Repository re-initialized after corruption; starting statement scan");
		try (RepositoryConnection conn = repo.getConnection()) {
			RepositoryException repositoryException = assertThrows(RepositoryException.class, () -> {
				logger.info("Requesting statements with explicit iteration to surface corruption");
				conn.getStatements(null, null, null, false).forEachRemaining(s -> {
					// Force materialization of all statements
				});
			});
			logger.info("Captured RepositoryException message='{}' type={} cause={}", repositoryException.getMessage(),
					repositoryException.getClass().getName(),
					repositoryException.getCause() != null ? repositoryException.getCause().getClass().getName()
							: "null");
		} finally {
			if (previousDebugProperty == null) {
				System.clearProperty(REPOSITORY_DEBUG_PROPERTY);
				logger.info("Cleared '{}' system property", REPOSITORY_DEBUG_PROPERTY);
			} else {
				System.setProperty(REPOSITORY_DEBUG_PROPERTY, previousDebugProperty);
				logger.info("Restored '{}' system property to {}", REPOSITORY_DEBUG_PROPERTY, previousDebugProperty);
			}
		}
	}

	private static void overwriteByte(File file, long pos, int newVal) throws IOException {
		try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
			long fileLength = raf.length();
			if (pos >= fileLength) {
				throw new IOException(
						"Attempt to write outside the existing file bounds: " + pos + " >= " + fileLength);
			}
			raf.seek(pos);
			raf.writeByte(newVal);
		}
	}

	private static byte peekByte(File file, long pos) throws IOException {
		try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
			long fileLength = raf.length();
			if (pos >= fileLength) {
				throw new IOException(
						"Attempt to read outside the existing file bounds: " + pos + " >= " + fileLength);
			}
			raf.seek(pos);
			return raf.readByte();
		}
	}

	private static String formatByte(byte value) {
		return String.format("0x%02X", Byte.toUnsignedInt(value));
	}
}
