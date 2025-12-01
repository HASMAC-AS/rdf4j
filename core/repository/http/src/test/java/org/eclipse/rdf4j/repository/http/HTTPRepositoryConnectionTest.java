/*******************************************************************************
 * Copyright (c) 2023 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.repository.http;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.InputStream;
import java.net.URL;

import org.eclipse.rdf4j.http.client.RDF4JProtocolSession;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.UnsupportedRDFormatException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

public class HTTPRepositoryConnectionTest {

	static WireMockServer server;
	static HTTPRepository testRepository;
	static RDF4JProtocolSession session;
	static String baseUrl;

	@BeforeAll
	static void configureMockServer() {
		server = new WireMockServer(WireMockConfiguration.options().dynamicPort().templatingEnabled(false));
		server.start();
		configureFor("localhost", server.port());
		baseUrl = "http://localhost:" + server.port();
		stubFor(get(urlEqualTo("/Socrates"))
				.willReturn(aResponse()
						.withHeader("Content-Type", RDFFormat.TURTLE.getDefaultMIMEType())
						.withBody("<http://example.org/Socrates> a <http://xmlns.com/foaf/0.1/Person> .")));
		stubFor(get(urlEqualTo("/Socrates.ttl"))
				.willReturn(aResponse()
						.withHeader("Content-Type", "application/octet-stream")
						.withBody("<http://example.org/Socrates> a <http://xmlns.com/foaf/0.1/Person> .")));
		stubFor(get(urlEqualTo("/Plato"))
				.willReturn(aResponse()
						.withHeader("Content-Type", "application/octet-stream")
						.withBody("<http://example.org/Socrates> a <http://xmlns.com/foaf/0.1/Person> .")));

		session = mock(RDF4JProtocolSession.class);
		testRepository = mock(HTTPRepository.class);
	}

	@AfterAll
	static void stopServer() {
		if (server != null) {
			server.stop();
		}
	}

	@Test
	public void testAddFromURL_FormatFromMimetype() throws Exception {
		URL url = new URL(baseUrl + "/Socrates");
		try (HTTPRepositoryConnection repoConn = new HTTPRepositoryConnection(testRepository, session)) {
			repoConn.add(url);
		}
		verify(session).upload(any(InputStream.class), eq(url.toExternalForm()), eq(RDFFormat.TURTLE), anyBoolean(),
				anyBoolean());
	}

	@Test
	public void testAddFromURL_FormatFromFilename() throws Exception {
		URL url = new URL(baseUrl + "/Socrates.ttl");
		try (HTTPRepositoryConnection repoConn = new HTTPRepositoryConnection(testRepository, session)) {
			repoConn.add(url);
		}
		verify(session).upload(any(InputStream.class), eq(url.toExternalForm()), eq(RDFFormat.TURTLE), anyBoolean(),
				anyBoolean());
	}

	@Test
	public void testAddFromURL_FormatUndetermined() throws Exception {
		URL url = new URL(baseUrl + "/Plato");
		try (HTTPRepositoryConnection repoConn = new HTTPRepositoryConnection(testRepository, session)) {
			assertThatExceptionOfType(UnsupportedRDFormatException.class).isThrownBy(() -> {
				repoConn.add(url);
			}).withMessageContaining("Could not find RDF format for URL: " + url.toExternalForm());
		}
	}

}
