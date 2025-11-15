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
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

class WebappDeployment {

	private final String contextPath;

	private final String location;

	private final Resource resource;

	private volatile Path cachedDocBase;

	private WebappDeployment(String contextPath, String location, Resource resource) {
		this.contextPath = contextPath;
		this.location = location;
		this.resource = resource;
	}

	static List<WebappDeployment> fromProperties(List<Rdf4jServerBootProperties.Webapp> webapps, ResourceLoader loader)
			throws IOException {
		List<WebappDeployment> deployments = new ArrayList<>();
		for (Rdf4jServerBootProperties.Webapp webapp : webapps) {
			String contextPath = normalizeContextPath(webapp.getContextPath());
			String location = Objects.requireNonNull(webapp.getLocation(), "Missing WAR location");
			Resource resource = loader.getResource(location);
			if (!resource.exists()) {
				throw new IOException("Configured web application does not exist: " + location);
			}
			deployments.add(new WebappDeployment(contextPath, location, resource));
		}
		return deployments;
	}

	private static String normalizeContextPath(String contextPath) {
		Objects.requireNonNull(contextPath, "Missing context path");
		String trimmed = contextPath.trim();
		if (trimmed.isEmpty()) {
			throw new IllegalArgumentException("Context path must not be empty");
		}
		return trimmed.startsWith("/") ? trimmed : "/" + trimmed;
	}

	String getContextPath() {
		return contextPath;
	}

	String getLocation() {
		return location;
	}

	Path resolveDocBase() throws IOException {
		Path existing = cachedDocBase;
		if (existing != null) {
			return existing;
		}
		synchronized (this) {
			if (cachedDocBase != null) {
				return cachedDocBase;
			}
			Path resolved = resolveResource();
			cachedDocBase = resolved;
			return resolved;
		}
	}

	private Path resolveResource() throws IOException {
		if (resource.isFile()) {
			Path file = resource.getFile().toPath();
			return file;
		}
		String suffix = resource.getFilename() != null && !resource.getFilename().isBlank()
				? "-" + resource.getFilename()
				: "";
		Path tempFile = Files.createTempFile("rdf4j-webapp", suffix.endsWith(".war") ? suffix : suffix + ".war");
		try (InputStream stream = resource.getInputStream()) {
			Files.copy(stream, tempFile, StandardCopyOption.REPLACE_EXISTING);
		}
		tempFile.toFile().deleteOnExit();
		return tempFile;
	}
}
