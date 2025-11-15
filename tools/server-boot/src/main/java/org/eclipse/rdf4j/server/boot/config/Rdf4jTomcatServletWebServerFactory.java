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
import java.nio.file.Path;
import java.util.List;

import org.apache.catalina.Context;
import org.apache.catalina.Host;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.startup.Tomcat.FixContextListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.embedded.tomcat.TomcatWebServer;
import org.springframework.lang.NonNull;

/**
 * Servlet container factory that deploys the legacy RDF4J web applications alongside the Spring Boot context.
 */
class Rdf4jTomcatServletWebServerFactory extends TomcatServletWebServerFactory {

	private static final Logger logger = LoggerFactory.getLogger(Rdf4jTomcatServletWebServerFactory.class);

	private final List<WebappDeployment> deployments;

	Rdf4jTomcatServletWebServerFactory(List<WebappDeployment> deployments) {
		this.deployments = deployments;
	}

	@Override
	protected TomcatWebServer getTomcatWebServer(@NonNull Tomcat tomcat) {
		deployWebApplications(tomcat);
		return super.getTomcatWebServer(tomcat);
	}

	private void deployWebApplications(Tomcat tomcat) {
		Host host = tomcat.getHost();
		ClassLoader parentLoader = getClass().getClassLoader();
		for (WebappDeployment deployment : deployments) {
			try {
				Path docBase = deployment.resolveDocBase();
				Context context = tomcat.addWebapp(host, deployment.getContextPath(), docBase.toString());
				context.setParentClassLoader(parentLoader);
				context.setDocBase(docBase.toString());
				context.addLifecycleListener(new FixContextListener());
				logger.info("Deployed context {} from {}", deployment.getContextPath(), deployment.getLocation());
			} catch (IOException ex) {
				throw new IllegalStateException("Unable to deploy web application " + deployment.getContextPath(), ex);
			}
		}
	}
}
