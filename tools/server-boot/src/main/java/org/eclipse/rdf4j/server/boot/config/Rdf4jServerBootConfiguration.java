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
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;

/**
 * Boot configuration that prepares embedded Tomcat with the legacy RDF4J web applications.
 */
@Configuration
@EnableConfigurationProperties(Rdf4jServerBootProperties.class)
public class Rdf4jServerBootConfiguration {

	private static final Logger logger = LoggerFactory.getLogger(Rdf4jServerBootConfiguration.class);

	@Bean
	public TomcatServletWebServerFactory tomcatServletWebServerFactory(ResourceLoader resourceLoader,
			Rdf4jServerBootProperties properties) throws IOException {
		List<WebappDeployment> deployments = WebappDeployment.fromProperties(properties.getWebapps(), resourceLoader);
		return new Rdf4jTomcatServletWebServerFactory(deployments);
	}

	@Bean
	public InitializingBean rdf4jHomeInitializer(Rdf4jServerBootProperties properties) {
		return new Rdf4jHomeInitializer(properties.getHome(), logger);
	}
}
