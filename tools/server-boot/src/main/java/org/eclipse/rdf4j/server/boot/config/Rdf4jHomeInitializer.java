/*******************************************************************************
 * Copyright (c) 2025 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ******************************************************************************/
package org.eclipse.rdf4j.server.boot.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.springframework.beans.factory.InitializingBean;

class Rdf4jHomeInitializer implements InitializingBean {

	static final String RDF4J_HOME_PROPERTY = "org.eclipse.rdf4j.appdata.basedir";

	private final Path configuredHome;

	private final Logger logger;

	Rdf4jHomeInitializer(Path configuredHome, Logger logger) {
		this.configuredHome = configuredHome;
		this.logger = logger;
	}

	@Override
	public void afterPropertiesSet() throws IOException {
		String existingHome = System.getProperty(RDF4J_HOME_PROPERTY);
		if (existingHome != null && !existingHome.isBlank()) {
			initialize(Path.of(existingHome));
			return;
		}
		Path target = configuredHome.toAbsolutePath();
		initialize(target);
		System.setProperty(RDF4J_HOME_PROPERTY, target.toString());
	}

	private void initialize(Path directory) throws IOException {
		Files.createDirectories(directory);
		logger.info("Using RDF4J data directory at {}", directory.toAbsolutePath());
	}
}
