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
package org.eclipse.rdf4j.repository.manager;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.eclipse.rdf4j.http.protocol.Protocol;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultFormat;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.config.RepositoryConfig;
import org.eclipse.rdf4j.repository.config.RepositoryConfigSchema;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

/**
 * Unit tests for {@link RemoteRepositoryManager}
 *
 * @author Jeen Broekstra
 */
public class RemoteRepositoryManagerTest extends RepositoryManagerTest {

	private WireMockServer server;

	@BeforeEach
	public void setUp() {
		server = new WireMockServer(WireMockConfiguration.options().dynamicPort().templatingEnabled(false));
		server.start();
		configureFor("localhost", server.port());
		subject = new RemoteRepositoryManager("http://localhost:" + server.port() + "/rdf4j-server");
	}

	@AfterEach
	public void tearDown() {
		if (server != null) {
			server.stop();
		}
	}

	@Test
	public void testAddRepositoryConfig() throws Exception {
		stubFor(get(urlEqualTo("/rdf4j-server/protocol"))
				.willReturn(aResponse().withBody(Protocol.VERSION)));
		stubFor(put(urlEqualTo("/rdf4j-server/repositories/test"))
				.willReturn(aResponse().withStatus(204)));
		stubFor(get(urlEqualTo("/rdf4j-server/repositories"))
				.willReturn(aResponse()
						.withHeader("Content-Type", TupleQueryResultFormat.SPARQL.getDefaultMIMEType())
						.withBody(readFileToString("repository-list-response.srx"))));

		RepositoryConfig config = new RepositoryConfig("test");

		subject.addRepositoryConfig(config);

		verify(1, putRequestedFor(urlEqualTo("/rdf4j-server/repositories/test"))
				.withHeader("Content-Type", equalTo("application/x-binary-rdf")));
	}

	@Test
	public void testAddRepositoryConfigExisting() throws Exception {
		stubFor(get(urlEqualTo("/rdf4j-server/protocol"))
				.willReturn(aResponse().withBody(Protocol.VERSION)));
		stubFor(post(urlEqualTo("/rdf4j-server/repositories/mem-rdf/config"))
				.willReturn(aResponse().withStatus(204)));
		stubFor(get(urlEqualTo("/rdf4j-server/repositories"))
				.willReturn(aResponse()
						.withHeader("Content-Type", TupleQueryResultFormat.SPARQL.getDefaultMIMEType())
						.withBody(readFileToString("repository-list-response.srx"))));

		RepositoryConfig config = new RepositoryConfig("mem-rdf"); // this repo already exists

		subject.addRepositoryConfig(config);

		verify(1, postRequestedFor(urlEqualTo("/rdf4j-server/repositories/mem-rdf/config"))
				.withHeader("Content-Type", equalTo("application/x-binary-rdf")));
	}

	@Test
	public void testGetRepositoryConfig() {
		stubFor(get(urlEqualTo("/rdf4j-server/protocol"))
				.willReturn(aResponse().withBody(Protocol.VERSION)));
		stubFor(get(urlEqualTo("/rdf4j-server/repositories/test/config"))
				.willReturn(aResponse()
						.withHeader("Content-Type", RDFFormat.NTRIPLES.getDefaultMIMEType())
						.withBody("_:node1 <" + RepositoryConfigSchema.REPOSITORYID + "> \"test\" . ")));

		subject.getRepositoryConfig("test");

		verify(1, getRequestedFor(urlEqualTo("/rdf4j-server/repositories/test/config")));
	}

	@Test
	public void testAddRepositoryConfigLegacy() throws Exception {
		stubFor(get(urlEqualTo("/rdf4j-server/protocol"))
				.willReturn(aResponse().withBody("8")));
		stubFor(post(urlEqualTo("/rdf4j-server/repositories/SYSTEM/statements"))
				.willReturn(aResponse().withStatus(204)));
		stubFor(get(urlEqualTo("/rdf4j-server/repositories"))
				.willReturn(aResponse()
						.withHeader("Content-Type", TupleQueryResultFormat.SPARQL.getDefaultMIMEType())
						.withBody(readFileToString("repository-list-response.srx"))));

		RepositoryConfig config = new RepositoryConfig("test");

		assertThrows(RepositoryException.class, () -> subject.addRepositoryConfig(config));
	}

	private String readFileToString(String fileName) throws IOException {
		return IOUtils.resourceToString("__files/" + fileName, StandardCharsets.UTF_8, getClass().getClassLoader());
	}
}
