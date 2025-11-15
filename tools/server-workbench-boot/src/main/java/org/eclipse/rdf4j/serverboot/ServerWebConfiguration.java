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

import org.eclipse.rdf4j.common.webapp.filters.PathFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;
import org.tuckey.web.filters.urlrewrite.UrlRewriteFilter;

import com.github.ziplet.filter.compression.CompressingFilter;

@Configuration
@ImportResource({
		"classpath:/WEB-INF/common-webapp-servlet.xml",
		"classpath:/WEB-INF/common-webapp-system-servlet.xml",
		"classpath:/WEB-INF/rdf4j-http-server-servlet.xml" })
public class ServerWebConfiguration {

	@Bean
	public ServletRegistrationBean<DispatcherServlet> rdf4jServerServlet(WebApplicationContext parentContext) {
		DispatcherServlet dispatcherServlet = new DispatcherServlet(parentContext);
		ServletRegistrationBean<DispatcherServlet> registration = new ServletRegistrationBean<>(dispatcherServlet,
				"/rdf4j-server/*",
				"/protocol/*",
				"/repositories/*",
				"*.view",
				"*.form");
		registration.setName("rdf4j-http-server");
		registration.setLoadOnStartup(100);
		return registration;
	}

	@Bean
	public FilterRegistrationBean<Rdf4jServerForwardFilter> rdf4jServerForwardFilter() {
		FilterRegistrationBean<Rdf4jServerForwardFilter> registration = new FilterRegistrationBean<>(
				new Rdf4jServerForwardFilter());
		registration.setName("Rdf4jServerForwardFilter");
		registration.addUrlPatterns("/rdf4j-server/*");
		registration.setOrder(0);
		return registration;
	}

	@Bean
	public FilterRegistrationBean<CompressingFilter> compressingFilter() {
		FilterRegistrationBean<CompressingFilter> registration = new FilterRegistrationBean<>(new CompressingFilter());
		registration.setName("CompressingFilter");
		registration.addInitParameter("excludeContentTypes",
				"application/x-binary-rdf,application/x-binary-rdf-results-table");
		registration.addUrlPatterns(
				"/rdf4j-server/*",
				"/protocol/*",
				"/repositories/*",
				"*.view",
				"*.form");
		registration.setOrder(1);
		return registration;
	}

	@Bean
	public FilterRegistrationBean<PathFilter> pathFilter() {
		FilterRegistrationBean<PathFilter> registration = new FilterRegistrationBean<>(new PathFilter());
		registration.setName("PathFilter");
		registration.addUrlPatterns("*.css", "/rdf4j-server/styles/*");
		registration.setOrder(2);
		return registration;
	}

	@Bean
	public FilterRegistrationBean<UrlRewriteFilter> urlRewriteFilter() {
		FilterRegistrationBean<UrlRewriteFilter> registration = new FilterRegistrationBean<>(new UrlRewriteFilter());
		registration.setName("UrlRewriteFilter");
		registration.addInitParameter("logLevel", "commons");
		registration.addInitParameter("statusEnabled", "false");
		registration.addUrlPatterns("/rdf4j-server", "/rdf4j-server/", "/rdf4j-server/overview.view");
		registration.setOrder(3);
		return registration;
	}

}
