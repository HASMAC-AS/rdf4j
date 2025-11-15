/*******************************************************************************
 * Copyright (c) 2025 Eclipse RDF4J contributors, Aduna, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.serverboot;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppDataDirectoryConfiguration {

	private static final Logger LOGGER = LoggerFactory.getLogger(AppDataDirectoryConfiguration.class);

	static final String APPDATA_PROPERTY = "org.eclipse.rdf4j.appdata.basedir";

	@PostConstruct
	public void ensureAppDataDirectory() throws IOException {
		if (System.getProperty(APPDATA_PROPERTY) == null) {
			Path defaultPath = Paths.get(System.getProperty("java.io.tmpdir"), "rdf4j-appdata");
			Files.createDirectories(defaultPath);
			System.setProperty(APPDATA_PROPERTY, defaultPath.toAbsolutePath().toString());
			LOGGER.info("Using temporary RDF4J app data directory at {}", defaultPath);
		}
	}
}
