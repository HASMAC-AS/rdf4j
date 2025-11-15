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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties used by the RDF4J Spring Boot launcher.
 */
@ConfigurationProperties(prefix = "rdf4j.boot")
public class Rdf4jServerBootProperties {

	private Path home = Paths.get("target", "rdf4j-home");

	private List<Webapp> webapps = defaultWebapps();

	public Path getHome() {
		return home;
	}

	public void setHome(Path home) {
		this.home = home;
	}

	public List<Webapp> getWebapps() {
		return webapps;
	}

	public void setWebapps(List<Webapp> webapps) {
		this.webapps = webapps;
	}

	private static List<Webapp> defaultWebapps() {
		List<Webapp> defaults = new ArrayList<>();
		defaults.add(new Webapp("/rdf4j-server", "classpath:webapps/rdf4j-server.war"));
		defaults.add(new Webapp("/rdf4j-workbench", "classpath:webapps/rdf4j-workbench.war"));
		return defaults;
	}

	public static class Webapp {

		private String contextPath;

		private String location;

		public Webapp() {
		}

		public Webapp(String contextPath, String location) {
			this.contextPath = contextPath;
			this.location = location;
		}

		public String getContextPath() {
			return contextPath;
		}

		public void setContextPath(String contextPath) {
			this.contextPath = contextPath;
		}

		public String getLocation() {
			return location;
		}

		public void setLocation(String location) {
			this.location = location;
		}
	}
}
