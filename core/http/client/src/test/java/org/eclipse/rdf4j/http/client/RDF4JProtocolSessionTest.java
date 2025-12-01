/*******************************************************************************
 * Copyright (c) 2019 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.http.client;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;

import org.eclipse.rdf4j.http.protocol.Protocol;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultFormat;
import org.eclipse.rdf4j.repository.config.RepositoryConfig;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;

import org.junit.jupiter.api.Test;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.stubbing.Scenario;

/**
 * Unit tests for {@link RDF4JProtocolSession}
 *
 * @author Jeen Broekstra
 */
public class RDF4JProtocolSessionTest extends SPARQLProtocolSessionTest {

	private final String testHeader = "X-testing-header";
	private final String testValue = "foobar";

	RDF4JProtocolSession getRDF4JSession() {
		return (RDF4JProtocolSession) sparqlSession;
	}

	@Override
	RDF4JProtocolSession createProtocolSession() {
		RDF4JProtocolSession session = new SharedHttpClientSessionManager().createRDF4JProtocolSession(serverURL);
		session.setRepository(Protocol.getRepositoryLocation(serverURL, repositoryID));
		HashMap<String, String> additionalHeaders = new HashMap<>();
		additionalHeaders.put(testHeader, testValue);
		session.setAdditionalHttpHeaders(additionalHeaders);
		return session;
	}

	@Test
	public void testCreateRepositoryExecutesPut() throws Exception {
		stubFor(put(urlEqualTo("/rdf4j-server/repositories/test")).willReturn(aResponse()));
		RepositoryConfig config = new RepositoryConfig("test");
		getRDF4JSession().createRepository(config);
		verify(1, putRequestedFor(urlEqualTo("/rdf4j-server/repositories/test"))
				.withHeader(testHeader, WireMock.equalTo(testValue)));
	}

	@Test
	public void testCreateRepositoryFollowsRedirectOnPut() throws Exception {
		String originalPath = "/rdf4j-server/repositories/test";
		String redirectedPath = "/https/rdf4j-server/repositories/test";
		String redirectLocation = "http://localhost:" + server.port() + redirectedPath;

		stubFor(put(urlEqualTo(originalPath))
				.inScenario("redirect-put")
				.whenScenarioStateIs(Scenario.STARTED)
				.willReturn(aResponse()
						.withStatus(301)
						.withHeader("Location", redirectLocation))
				.willSetStateTo("redirected"));

		stubFor(put(urlEqualTo(redirectedPath))
				.inScenario("redirect-put")
				.whenScenarioStateIs("redirected")
				.willReturn(aResponse()));

		RepositoryConfig config = new RepositoryConfig("test");
		getRDF4JSession().createRepository(config);

		verify(1, putRequestedFor(urlEqualTo(originalPath))
				.withHeader(testHeader, WireMock.equalTo(testValue)));
		verify(1, putRequestedFor(urlEqualTo(redirectedPath))
				.withHeader(testHeader, WireMock.equalTo(testValue)));
	}

	@Test
	public void testUpdateRepositoryExecutesPost() throws Exception {
		stubFor(post(urlEqualTo("/rdf4j-server/repositories/test/config"))
				.willReturn(aResponse().withStatus(204)));
		getRDF4JSession().updateRepository(new RepositoryConfig("test"));
		verify(1, postRequestedFor(urlEqualTo("/rdf4j-server/repositories/test/config")));
	}

	@Test
	public void testSize() throws Exception {
		stubFor(get(urlEqualTo("/rdf4j-server/repositories/test/size"))
				.willReturn(aResponse().withBody("10")));

		long size = getRDF4JSession().size();

		assertThat(size).isEqualTo(10);
	}

	@Test
	public void testGetRepositoryConfig() throws Exception {
		stubFor(get(urlEqualTo("/rdf4j-server/repositories/test/config"))
				.willReturn(aResponse()
						.withHeader("Content-Type", RDFFormat.NTRIPLES.getDefaultMIMEType())
						.withBody("_:node1 <http://www.openrdf.org/config/repository#repositoryID> \"test\" . ")));

		StatementCollector collector = new StatementCollector();
		getRDF4JSession().getRepositoryConfig(collector);

		assertThat(collector.getStatements()).isNotEmpty();
	}

	@Test
	public void testRepositoryList() throws Exception {
		stubFor(get(urlEqualTo("/rdf4j-server/repositories"))
				.willReturn(aResponse()
						.withHeader("Content-Type", TupleQueryResultFormat.SPARQL.getDefaultMIMEType())
						.withBody(readFileToString("repository-list.xml"))));

		TupleQueryResult result = getRDF4JSession().getRepositoryList();
		assertThat((Object) result).isNotNull();
		result.close();

		verify(1, getRequestedFor(urlEqualTo("/rdf4j-server/repositories")));
	}

	@Test
	public void testClose() throws Exception {
		stubFor(post(urlEqualTo("/rdf4j-server/repositories/test/transactions"))
				.willReturn(aResponse()
						.withStatus(201)
						.withHeader("Location",
								"http://localhost:" + server.port()
										+ "/rdf4j-server/repositories/test/transactions/31337")
						.withBody("transaction created")));
		stubFor(delete(urlPathEqualTo("/rdf4j-server/repositories/test/transactions/31337"))
				.willReturn(aResponse().withStatus(204)));

		getRDF4JSession().beginTransaction();
		getRDF4JSession().rollbackTransaction();
		getRDF4JSession().close();

		verify(1, deleteRequestedFor(urlPathEqualTo("/rdf4j-server/repositories/test/transactions/31337")));
	}

	@Override
	protected com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder successTupleList()
			throws java.io.IOException {
		return super.successTupleList().withHeader(testHeader, testValue);
	}

	@Override
	protected String readFileToString(String fileName) throws java.io.IOException {
		return super.readFileToString(fileName);
	}
}
