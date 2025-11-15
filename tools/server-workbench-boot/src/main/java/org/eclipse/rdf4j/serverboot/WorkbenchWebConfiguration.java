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

import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.rdf4j.workbench.proxy.CacheFilter;
import org.eclipse.rdf4j.workbench.proxy.CookieCacheControlFilter;
import org.eclipse.rdf4j.workbench.proxy.RedirectFilter;
import org.eclipse.rdf4j.workbench.proxy.WorkbenchGateway;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WorkbenchWebConfiguration {

	@Bean
	public ServletRegistrationBean<WorkbenchGateway> workbenchServlet() {
		WorkbenchGateway gateway = new WorkbenchGateway();
		ServletRegistrationBean<WorkbenchGateway> registration = new ServletRegistrationBean<>(gateway,
				"/rdf4j-workbench/repositories/*");
		registration.setName("workbench");
		Map<String, String> initParams = new LinkedHashMap<>();
		initParams.put("transformations", "/transformations");
		initParams.put("default-server", "/rdf4j-server");
		initParams.put("accepted-server-prefixes", "file: http: https:");
		initParams.put("change-server-path", "/NONE/server");
		initParams.put("cookie-max-age", "2592000");
		initParams.put("no-repository-id", "NONE");
		initParams.put("default-path", "/NONE/repositories");
		initParams.put("default-command", "/summary");
		initParams.put("default-limit", "100");
		initParams.put("default-queryLn", "SPARQL");
		initParams.put("default-infer", "true");
		initParams.put("default-Accept", "application/rdf+xml");
		initParams.put("default-Content-Type", "application/rdf+xml");
		initParams.put("/summary", "org.eclipse.rdf4j.workbench.commands.SummaryServlet");
		initParams.put("/info", "org.eclipse.rdf4j.workbench.commands.InfoServlet");
		initParams.put("/information", "org.eclipse.rdf4j.workbench.commands.InformationServlet");
		initParams.put("/repositories", "org.eclipse.rdf4j.workbench.commands.RepositoriesServlet");
		initParams.put("/create", "org.eclipse.rdf4j.workbench.commands.CreateServlet");
		initParams.put("/delete", "org.eclipse.rdf4j.workbench.commands.DeleteServlet");
		initParams.put("/namespaces", "org.eclipse.rdf4j.workbench.commands.NamespacesServlet");
		initParams.put("/contexts", "org.eclipse.rdf4j.workbench.commands.ContextsServlet");
		initParams.put("/types", "org.eclipse.rdf4j.workbench.commands.TypesServlet");
		initParams.put("/explore", "org.eclipse.rdf4j.workbench.commands.ExploreServlet");
		initParams.put("/query", "org.eclipse.rdf4j.workbench.commands.QueryServlet");
		initParams.put("/saved-queries", "org.eclipse.rdf4j.workbench.commands.SavedQueriesServlet");
		initParams.put("/export", "org.eclipse.rdf4j.workbench.commands.ExportServlet");
		initParams.put("/add", "org.eclipse.rdf4j.workbench.commands.AddServlet");
		initParams.put("/remove", "org.eclipse.rdf4j.workbench.commands.RemoveServlet");
		initParams.put("/clear", "org.eclipse.rdf4j.workbench.commands.ClearServlet");
		initParams.put("/update", "org.eclipse.rdf4j.workbench.commands.UpdateServlet");
		registration.setInitParameters(initParams);
		registration.setLoadOnStartup(1);
		return registration;
	}

	@Bean
	public FilterRegistrationBean<RedirectFilter> redirectFilter() {
		FilterRegistrationBean<RedirectFilter> registration = new FilterRegistrationBean<>(new RedirectFilter());
		registration.setName("redirect");
		registration.addUrlPatterns("/rdf4j-workbench/");
		return registration;
	}

	@Bean
	public FilterRegistrationBean<CookieCacheControlFilter> cookieCacheControlFilter() {
		FilterRegistrationBean<CookieCacheControlFilter> registration = new FilterRegistrationBean<>(
				new CookieCacheControlFilter());
		registration.setName("cache");
		registration.addUrlPatterns("/rdf4j-workbench/repositories/*");
		return registration;
	}

	@Bean
	public FilterRegistrationBean<CacheFilter> cacheFilter() {
		FilterRegistrationBean<CacheFilter> registration = new FilterRegistrationBean<>(new CacheFilter());
		registration.setName("CacheFilter");
		registration.addUrlPatterns("/rdf4j-workbench/*");
		registration.addInitParameter("Cache-Control", "600");
		return registration;
	}
}
