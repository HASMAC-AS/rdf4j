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
package org.eclipse.rdf4j.sail.shacl;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;

class TestStartLoggerExtensionIntegrationTest {

	@Test
	void extensionAutoDetectionPrintsStartAndSuccess() {
		PrintStream originalOut = System.out;
		ByteArrayOutputStream outputBuffer = new ByteArrayOutputStream();
		PrintStream capturingStream = new PrintStream(outputBuffer, true, StandardCharsets.UTF_8);

		System.setOut(capturingStream);
		try {
			LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
					.selectors(DiscoverySelectors.selectClass(SampleTest.class))
					.build();

			Launcher launcher = LauncherFactory.create();
			launcher.execute(request);
		} finally {
			System.setOut(originalOut);
		}

		capturingStream.flush();

		String output = outputBuffer.toString(StandardCharsets.UTF_8);

		assertTrue(output.contains("[TEST] Start"), () -> "Expected start log entry in output, but was: " + output);
		assertTrue(output.contains("[TEST] Success"), () -> "Expected success log entry in output, but was: " + output);
	}

	static class SampleTest {

		@Test
		void succeeds() {
			// no-op test
		}
	}
}
