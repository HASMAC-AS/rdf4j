/*******************************************************************************
 * Copyright (c) 2024 Contributors and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.workbench.commands;

import static org.eclipse.rdf4j.workbench.base.TransformationServlet.CONTEXT;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilderFactory;

import org.eclipse.rdf4j.common.transaction.IsolationLevels;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.QueryResultHandlerException;
import org.eclipse.rdf4j.query.resultio.sparqlxml.SPARQLResultsXMLWriter;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.workbench.exceptions.BadRequestException;
import org.eclipse.rdf4j.workbench.util.TupleResultBuilder;
import org.eclipse.rdf4j.workbench.util.WorkbenchRequest;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

class AddServletTest {

	private static final String URL = "url";

	private final AddServlet servlet = new AddServlet();

	@Test
	void setsIsolationLevelForUploadedContent() throws RepositoryException, QueryResultHandlerException, IOException,
			BadRequestException {
		WorkbenchRequest request = mock(WorkbenchRequest.class);
		when(request.getParameter("baseURI")).thenReturn(null);
		when(request.getParameter("Content-Type")).thenReturn("application/rdf+xml");
		when(request.isParameterPresent(URL)).thenReturn(false);
		when(request.isParameterPresent(CONTEXT)).thenReturn(false);
		when(request.getContentParameter()).thenReturn(new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8)));
		when(request.getContentFileName()).thenReturn("data.rdf");
		when(request.getParameter("isolation-level")).thenReturn("NONE");

		HttpServletResponse response = mock(HttpServletResponse.class);

		Repository repository = mock(Repository.class);
		servlet.setRepository(repository);
		RepositoryConnection connection = mock(RepositoryConnection.class);
		when(repository.getConnection()).thenReturn(connection);

		servlet.doPost(request, response, "");

		verify(connection).setIsolationLevel(IsolationLevels.NONE);
	}

	@Test
	void serviceListsAllIsolationLevels() throws Exception {
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		TupleResultBuilder builder = new TupleResultBuilder(new SPARQLResultsXMLWriter(buffer),
				SimpleValueFactory.getInstance());

		servlet.service(builder, "/transformations");

		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		Document document = factory.newDocumentBuilder().parse(new ByteArrayInputStream(buffer.toByteArray()));
		NodeList bindingNodes = document.getElementsByTagNameNS("http://www.w3.org/2005/sparql-results#", "binding");
		Set<String> isolationLevels = new HashSet<>();
		for (int i = 0; i < bindingNodes.getLength(); i++) {
			Element binding = (Element) bindingNodes.item(i);
			if (!"available-isolation-level".equals(binding.getAttribute("name"))) {
				continue;
			}
			NodeList literalNodes = binding.getElementsByTagNameNS("http://www.w3.org/2005/sparql-results#", "literal");
			for (int j = 0; j < literalNodes.getLength(); j++) {
				String value = literalNodes.item(j).getTextContent().trim();
				int separator = value.indexOf(' ');
				if (separator >= 0) {
					value = value.substring(0, separator);
				}
				isolationLevels.add(value);
			}
		}

		String[] expected = Arrays.stream(IsolationLevels.values()).map(Enum::name).toArray(String[]::new);
		org.assertj.core.api.Assertions.assertThat(isolationLevels).containsExactlyInAnyOrder(expected);
	}
}
