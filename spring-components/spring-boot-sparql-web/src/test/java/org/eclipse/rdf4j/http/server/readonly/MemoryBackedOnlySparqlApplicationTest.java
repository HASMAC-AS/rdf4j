/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.http.server.readonly;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.resultio.BooleanQueryResultFormat;
import org.eclipse.rdf4j.query.resultio.QueryResultIO;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultFormat;
import org.eclipse.rdf4j.query.resultio.helpers.QueryResultCollector;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

@SpringBootTest(webEnvironment = WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Import(MemoryBackedOnlySparqlApplicationTestConfig.class)
public class MemoryBackedOnlySparqlApplicationTest {
	@Autowired
	private RestTemplate restTemplate;
	@Autowired
	private QueryResponder queryResponder;

	@Test
	public void contextLoads() {
		assertThat(queryResponder).isNotNull();
	}

	@Test
	public void testAskQuery() throws Exception {
		HttpHeaders headers = new HttpHeaders();
		headers.set(HttpHeaders.ACCEPT, BooleanQueryResultFormat.TEXT.getDefaultMIMEType());
		ResponseEntity<String> result = restTemplate.exchange("/sparql?query={query}", HttpMethod.GET,
				new HttpEntity<>(headers), String.class, "ASK { ?s ?p ?o }");

		assertThat(result.getBody()).contains("true");
	}

	@Test
	public void testSelectQuery() throws Exception {
		HttpHeaders headers = new HttpHeaders();
		headers.set(HttpHeaders.ACCEPT, TupleQueryResultFormat.JSON.getDefaultMIMEType());
		ResponseEntity<String> result = restTemplate.exchange("/sparql?query={query}", HttpMethod.GET,
				new HttpEntity<>(headers), String.class, "SELECT * WHERE { ?s ?p ?o }");

		assertThat(result.getBody())
				.contains("http://www.w3.org/1999/02/22-rdf-syntax-ns#Bag");
	}

	@Test
	public void testSPARQLRepository() throws Exception {
		String query = "SELECT * WHERE { ?s ?p ?o }";
		HttpHeaders headers = new HttpHeaders();
		headers.set(HttpHeaders.ACCEPT, TupleQueryResultFormat.JSON.getDefaultMIMEType());
		ResponseEntity<String> result = restTemplate.exchange("/sparql?query={query}", HttpMethod.GET,
				new HttpEntity<>(headers), String.class, query);

		QueryResultCollector collector = new QueryResultCollector();
		try (ByteArrayInputStream in = new ByteArrayInputStream(
				result.getBody().getBytes(StandardCharsets.UTF_8))) {
			QueryResultIO.parseTuple(in, TupleQueryResultFormat.JSON, collector, SimpleValueFactory.getInstance());
		}
		assertThat(collector.getBindingSets()).isNotEmpty();
		collector.getBindingSets().forEach(bindingSet -> assertNotNull(bindingSet));
	}

}
