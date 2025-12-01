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
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import org.eclipse.rdf4j.http.protocol.Protocol;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQueryResultHandler;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultFormat;
import org.eclipse.rdf4j.query.resultio.sparqljson.SPARQLStarResultsJSONWriter;
import org.eclipse.rdf4j.query.resultio.sparqlxml.SPARQLStarResultsXMLWriter;
import org.eclipse.rdf4j.rio.RDFFormat;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HeaderIterator;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.ProtocolVersion;
import org.apache.http.StatusLine;
import org.apache.http.params.HttpParams;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.stubbing.Scenario;

/**
 * Unit tests for {@link SPARQLProtocolSession}
 *
 * @author Jeen Broekstra
 */
public class SPARQLProtocolSessionTest {
	WireMockServer server;

	SPARQLProtocolSession sparqlSession;

	String serverURL;
	String repositoryID = "test";

	SPARQLProtocolSession createProtocolSession() {
		SPARQLProtocolSession session = new SharedHttpClientSessionManager().createRDF4JProtocolSession(serverURL);
		session.setQueryURL(Protocol.getRepositoryLocation(serverURL, repositoryID));
		session.setUpdateURL(
				Protocol.getStatementsLocation(Protocol.getRepositoryLocation(serverURL, repositoryID)));
		return session;
	}

	@BeforeEach
	public void setUp() {
		server = new WireMockServer(WireMockConfiguration.options().dynamicPort().templatingEnabled(false));
		server.start();
		WireMock.configureFor("localhost", server.port());
		serverURL = "http://localhost:" + server.port() + "/rdf4j-server";
		sparqlSession = createProtocolSession();
	}

	@AfterEach
	public void tearDown() {
		if (server != null) {
			server.stop();
		}
	}

	@Test
	public void testConnectionTimeoutRetry() throws Exception {
		server.stubFor(post(urlEqualTo("/rdf4j-server/repositories/test"))
				.inScenario("timeout-retry")
				.whenScenarioStateIs(Scenario.STARTED)
				.willReturn(aResponse().withStatus(408).withFault(Fault.CONNECTION_RESET_BY_PEER))
				.willSetStateTo("retry"));
		server.stubFor(post(urlEqualTo("/rdf4j-server/repositories/test"))
				.inScenario("timeout-retry")
				.whenScenarioStateIs("retry")
				.willReturn(aResponse()
						.withBody(readFileToString("repository-list.xml"))
						.withHeader("Content-Type", TupleQueryResultFormat.SPARQL.getDefaultMIMEType())));

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		TupleQueryResultHandler handler = Mockito.spy(new SPARQLStarResultsJSONWriter(out));
		// We only send the query once, internally the retry handler makes sure the first 408 response causes
		// a retry. From user perspective it just looks like everything went fine, the closed connection is gracefully
		// refreshed.
		sparqlSession.sendTupleQuery(QueryLanguage.SPARQL, "SELECT * WHERE { ?s ?p ?o}", null, null, true, -1, handler);
		assertThat(out.toString()).startsWith("{");
	}

	@Test
	public void testConnectionPoolTimeoutRetry() throws Exception {
		server.stubFor(post(urlEqualTo("/rdf4j-server/repositories/test"))
				.inScenario("pool-timeout")
				.whenScenarioStateIs(Scenario.STARTED)
				.willReturn(successTupleList())
				.willSetStateTo("second-success"));
		server.stubFor(post(urlEqualTo("/rdf4j-server/repositories/test"))
				.inScenario("pool-timeout")
				.whenScenarioStateIs("second-success")
				.willReturn(successTupleList())
				.willSetStateTo("first-fault"));
		server.stubFor(post(urlEqualTo("/rdf4j-server/repositories/test"))
				.inScenario("pool-timeout")
				.whenScenarioStateIs("first-fault")
				.willReturn(aResponse().withStatus(408))
				.willSetStateTo("second-fault"));
		server.stubFor(post(urlEqualTo("/rdf4j-server/repositories/test"))
				.inScenario("pool-timeout")
				.whenScenarioStateIs("second-fault")
				.willReturn(aResponse().withStatus(408))
				.willSetStateTo("final-success"));
		server.stubFor(post(urlEqualTo("/rdf4j-server/repositories/test"))
				.inScenario("pool-timeout")
				.whenScenarioStateIs("final-success")
				.willReturn(successTupleList()));

		// First fill the pool with 2 connections
		ByteArrayOutputStream out1 = new ByteArrayOutputStream();
		TupleQueryResultHandler handler1 = Mockito.spy(new SPARQLStarResultsJSONWriter(out1));
		sparqlSession.sendTupleQuery(QueryLanguage.SPARQL, "SELECT * WHERE { ?s ?p ?o}", null, null, true, -1,
				handler1);
		ByteArrayOutputStream out2 = new ByteArrayOutputStream();
		TupleQueryResultHandler handler2 = Mockito.spy(new SPARQLStarResultsJSONWriter(out2));
		sparqlSession.sendTupleQuery(QueryLanguage.SPARQL, "SELECT * WHERE { ?s ?p ?o}", null, null, true, -1,
				handler2);
		assertThat(out1.toString()).startsWith("{");
		assertThat(out2.toString()).startsWith("{");

		// When trying another `sendTupleQuery` the 2 pooled connections fail with a 408. Both are cleaned up
		// and finally a fresh connection is opened and goes through successfully
		ByteArrayOutputStream out3 = new ByteArrayOutputStream();
		TupleQueryResultHandler handler3 = Mockito.spy(new SPARQLStarResultsJSONWriter(out3));
		sparqlSession.sendTupleQuery(QueryLanguage.SPARQL, "SELECT * WHERE { ?s ?p ?o}", null, null, true, -1,
				handler3);
		assertThat(out3.toString()).startsWith("{");
	}

	@Test
	public void testTupleQuery_NoPassthrough() throws Exception {
		server.stubFor(post(urlEqualTo("/rdf4j-server/repositories/test"))
				.willReturn(successTupleList()));

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		TupleQueryResultHandler handler = Mockito.spy(new SPARQLStarResultsJSONWriter(out));
		sparqlSession.sendTupleQuery(QueryLanguage.SPARQL, "SELECT * WHERE { ?s ?p ?o}", null, null, true, -1, handler);

		// If not passed through, the QueryResultWriter methods should have been invoked
		verify(handler, times(1)).startQueryResult(anyList());

		// check that the OutputStream received content in JSON format
		assertThat(out.toString()).startsWith("{");
	}

	@Test
	public void testTupleQuery_Passthrough() throws Exception {
		server.stubFor(post(urlEqualTo("/rdf4j-server/repositories/test"))
				.willReturn(successTupleList()));

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		SPARQLStarResultsXMLWriter handler = Mockito.spy(new SPARQLStarResultsXMLWriter(out));
		sparqlSession.sendTupleQuery(QueryLanguage.SPARQL, "SELECT * WHERE { ?s ?p ?o}", null, null, true, -1, handler);

		// SPARQL-star XML sink should accept SPARQL/XML data and pass directly to OutputStream
		verify(handler, never()).startQueryResult(anyList());

		// check that the OutputStream received content in XML format
		assertThat(out.toString()).startsWith("<");
	}

	@Test
	public void testTupleQuery_Passthrough_ConfiguredFalse() throws Exception {
		server.stubFor(post(urlEqualTo("/rdf4j-server/repositories/test"))
				.willReturn(successTupleList()));

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		SPARQLStarResultsXMLWriter handler = Mockito.spy(new SPARQLStarResultsXMLWriter(out));
		sparqlSession.setPassThroughEnabled(false);
		sparqlSession.sendTupleQuery(QueryLanguage.SPARQL, "SELECT * WHERE { ?s ?p ?o}", null, null, true, -1, handler);

		// If not passed through, the QueryResultWriter methods should have been invoked
		verify(handler, times(1)).startQueryResult(anyList());

		// check that the OutputStream received content in XML format
		assertThat(out.toString()).startsWith("<");
	}

	@Test
	public void getContentTypeSerialisationTest() {
		{
			HttpResponse httpResponse = withContentType("application/shacl-validation-report+n-quads");
			RDFFormat format = SPARQLProtocolSession.getContentTypeSerialisation(httpResponse);

			assertThat(format).isEqualTo(RDFFormat.NQUADS);
		}

		{
			HttpResponse httpResponse = withContentType("application/shacl-validation-report+ld+json");
			RDFFormat format = SPARQLProtocolSession.getContentTypeSerialisation(httpResponse);

			assertThat(format).isEqualTo(RDFFormat.JSONLD);
		}

		{
			HttpResponse httpResponse = withContentType("text/shacl-validation-report+turtle");
			RDFFormat format = SPARQLProtocolSession.getContentTypeSerialisation(httpResponse);

			assertThat(format).isEqualTo(RDFFormat.TURTLE);
		}
	}

	/* private methods */

	private HttpResponse withContentType(String contentType) {
		Header header = new Header() {
			@Override
			public String getName() {
				return null;
			}

			@Override
			public String getValue() {
				return null;
			}

			@Override
			public HeaderElement[] getElements() throws ParseException {

				HeaderElement[] elements = { new HeaderElement() {
					@Override
					public String getName() {
						return contentType;
					}

					@Override
					public String getValue() {
						return null;
					}

					@Override
					public NameValuePair[] getParameters() {
						return new NameValuePair[0];
					}

					@Override
					public NameValuePair getParameterByName(String name) {
						return null;
					}

					@Override
					public int getParameterCount() {
						return 0;
					}

					@Override
					public NameValuePair getParameter(int index) {
						return null;
					}
				} };
				return elements;
			}
		};

		return new HttpResponse() {
			@Override
			public ProtocolVersion getProtocolVersion() {
				return null;
			}

			@Override
			public boolean containsHeader(String name) {
				return false;
			}

			@Override
			public Header[] getHeaders(String name) {
				Header[] headers = { header };
				return headers;
			}

			@Override
			public Header getFirstHeader(String name) {
				return null;
			}

			@Override
			public Header getLastHeader(String name) {
				return null;
			}

			@Override
			public Header[] getAllHeaders() {
				return new Header[0];
			}

			@Override
			public void addHeader(Header header1) {

			}

			@Override
			public void addHeader(String name, String value) {

			}

			@Override
			public void setHeader(Header header1) {

			}

			@Override
			public void setHeader(String name, String value) {

			}

			@Override
			public void setHeaders(Header[] headers) {

			}

			@Override
			public void removeHeader(Header header1) {

			}

			@Override
			public void removeHeaders(String name) {

			}

			@Override
			public HeaderIterator headerIterator() {
				return null;
			}

			@Override
			public HeaderIterator headerIterator(String name) {
				return null;
			}

			@Override
			public HttpParams getParams() {
				return null;
			}

			@Override
			public void setParams(HttpParams params) {

			}

			@Override
			public StatusLine getStatusLine() {
				return null;
			}

			@Override
			public void setStatusLine(StatusLine statusline) {

			}

			@Override
			public void setStatusLine(ProtocolVersion ver, int code) {

			}

			@Override
			public void setStatusLine(ProtocolVersion ver, int code, String reason) {

			}

			@Override
			public void setStatusCode(int code) throws IllegalStateException {

			}

			@Override
			public void setReasonPhrase(String reason) throws IllegalStateException {

			}

			@Override
			public HttpEntity getEntity() {
				return null;
			}

			@Override
			public void setEntity(HttpEntity entity) {

			}

			@Override
			public Locale getLocale() {
				return null;
			}

			@Override
			public void setLocale(Locale loc) {

			}
		};
	}

	protected com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder successTupleList() throws IOException {
		return aResponse()
				.withBody(readFileToString("repository-list.xml"))
				.withHeader("Content-Type", TupleQueryResultFormat.SPARQL.getDefaultMIMEType());
	}

	protected String readFileToString(String fileName) throws IOException {
		return IOUtils.resourceToString("__files/" + fileName, StandardCharsets.UTF_8, getClass().getClassLoader());
	}
}
