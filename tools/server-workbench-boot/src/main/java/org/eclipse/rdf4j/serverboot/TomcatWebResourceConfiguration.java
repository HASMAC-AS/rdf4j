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
package org.eclipse.rdf4j.serverboot;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.apache.catalina.webresources.DirResourceSet;
import org.apache.catalina.webresources.StandardRoot;
import org.apache.tomcat.util.scan.StandardJarScanFilter;
import org.apache.tomcat.util.scan.StandardJarScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

@Configuration
public class TomcatWebResourceConfiguration {

	private static final Logger LOGGER = LoggerFactory.getLogger(TomcatWebResourceConfiguration.class);

	private static final String WEB_INF_PATTERN = "classpath:/WEB-INF/**";

	@Bean
	public WebServerFactoryCustomizer<TomcatServletWebServerFactory> tomcatResourcesCustomizer() {
		return factory -> factory.addContextCustomizers(context -> {
			try {
				Path webInfRoot = Files.createTempDirectory("rdf4j-webinf");
				copyWebInfResources(webInfRoot);
				webInfRoot.toFile().deleteOnExit();
				StandardRoot resources = new StandardRoot(context);
				resources.addPreResources(new DirResourceSet(resources, "/WEB-INF", webInfRoot.toString(), "/"));
				context.setResources(resources);
				StandardJarScanner jarScanner = (StandardJarScanner) context.getJarScanner();
				StandardJarScanFilter filter = new StandardJarScanFilter();
				filter.setDefaultPluggabilityScan(false);
				jarScanner.setJarScanFilter(filter);
			} catch (IOException e) {
				throw new IllegalStateException("Unable to prepare WEB-INF resources", e);
			}
		});
	}

	private void copyWebInfResources(Path targetRoot) throws IOException {
		PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
		Resource[] resources = resolver.getResources(WEB_INF_PATTERN);
		for (Resource resource : resources) {
			if (!resource.isReadable() || resource.getFilename() == null) {
				continue;
			}
			String urlPath = resource.getURL().getPath();
			int idx = urlPath.indexOf("WEB-INF");
			if (idx < 0) {
				continue;
			}
			Path destination = targetRoot.resolve(urlPath.substring(idx + "WEB-INF".length() + 1));
			Path parent = destination.getParent();
			if (parent != null) {
				Files.createDirectories(parent);
			}
			try (InputStream in = resource.getInputStream()) {
				Files.copy(in, destination, StandardCopyOption.REPLACE_EXISTING);
			}
		}
		LOGGER.info("Copied WEB-INF resources to {}", targetRoot);
	}
}
