/*******************************************************************************
 * Copyright (c) 2024 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.sail.nativerdf;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.rdf4j.sail.nativerdf.TxnStatusFile.TxnStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class TxnStatusFileTest {

	@TempDir
	File tempFolder;

	@Test
	public void statusTransitionsReusePrecreatedFiles() throws Exception {
		TxnStatusFile txnStatusFile = new TxnStatusFile(tempFolder);

		Path statusFile = tempFolder.toPath().resolve(TxnStatusFile.FILE_NAME);

		for (TxnStatus status : TxnStatus.values()) {
			Path variant = variantPath(status);
			assertTrue(Files.exists(variant), "Missing status file for " + status);
			if (status == TxnStatus.NONE) {
				assertEquals(0, Files.size(variant), "NONE status file should be empty");
			} else {
				assertArrayEquals(status.getOnDisk(), Files.readAllBytes(variant));
			}
		}

		txnStatusFile.setTxnStatus(TxnStatus.ACTIVE);

		assertFalse(Files.exists(variantPath(TxnStatus.ACTIVE)));
		assertArrayEquals(TxnStatus.ACTIVE.getOnDisk(), Files.readAllBytes(statusFile));

		txnStatusFile.setTxnStatus(TxnStatus.COMMITTING);

		assertTrue(Files.exists(variantPath(TxnStatus.ACTIVE)));
		assertFalse(Files.exists(variantPath(TxnStatus.COMMITTING)));
		assertArrayEquals(TxnStatus.COMMITTING.getOnDisk(), Files.readAllBytes(statusFile));

		txnStatusFile.close();
	}

	private Path variantPath(TxnStatus status) {
		return tempFolder.toPath().resolve(TxnStatusFile.FILE_NAME + "." + status.name());
	}
}
