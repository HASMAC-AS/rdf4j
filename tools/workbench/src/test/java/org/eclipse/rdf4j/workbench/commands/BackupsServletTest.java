/*******************************************************************************
 * Copyright (c) 2026 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.workbench.commands;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.eclipse.rdf4j.repository.http.HTTPRepository;
import org.eclipse.rdf4j.workbench.exceptions.BadRequestException;
import org.eclipse.rdf4j.workbench.util.WorkbenchRequest;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class BackupsServletTest {
	@Test
	void browserNavigationServesRegisteredBackupsPage() throws Exception {
		BackupsServlet servlet = new BackupsServlet();
		servlet.setRepository(mock(HTTPRepository.class));
		servlet.setRepositoryManager(mock(org.eclipse.rdf4j.repository.manager.RepositoryManager.class));
		var info = new org.eclipse.rdf4j.repository.manager.RepositoryInfo();
		info.setId("source");
		servlet.setRepositoryInfo(info);
		servlet.init(new org.eclipse.rdf4j.workbench.support.TestServletConfig("backups", Map.of()));
		try {
			MockHttpServletRequest request = new MockHttpServletRequest("GET",
					"/rdf4j-workbench/repositories/source/backups");
			request.setContextPath("/rdf4j-workbench");
			request.addHeader("Accept", "text/html");
			MockHttpServletResponse response = new MockHttpServletResponse();
			servlet.service(request, response);
			assertThat(response.getContentAsString()).contains("Backups", "data-workbench-view=\"backups\"");
		} finally {
			servlet.destroy();
		}
	}

	@Test
	void statusUsesConfiguredRepositorySession() throws Exception {
		HTTPRepository repository = mock(HTTPRepository.class);
		when(repository.manageBackups(null, null, null)).thenReturn("{\"supported\":true}");
		BackupsServlet servlet = new BackupsServlet();
		servlet.setRepository(repository);
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/backups");
		request.setParameter("action", "status");
		MockHttpServletResponse response = new MockHttpServletResponse();
		servlet.service(new WorkbenchRequest(repository, request, Map.of()), response);
		assertThat(response.getContentAsString()).isEqualTo("{\"supported\":true}");
		assertThat(response.getContentType()).startsWith("application/json");
	}

	@Test
	void mutationsRequirePostAndRejectUnknownActions() throws Exception {
		HTTPRepository repository = mock(HTTPRepository.class);
		BackupsServlet servlet = new BackupsServlet();
		servlet.setRepository(repository);
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/backups");
		request.setParameter("action", "backup");
		assertThatThrownBy(() -> servlet.service(new WorkbenchRequest(repository, request, Map.of()),
				new MockHttpServletResponse()))
						.isInstanceOf(BadRequestException.class);
		request.setMethod("POST");
		request.setParameter("action", "delete");
		assertThatThrownBy(() -> servlet.doPost(new WorkbenchRequest(repository, request, Map.of()),
				new MockHttpServletResponse()))
						.isInstanceOf(BadRequestException.class);
	}

	@Test
	void restoreRelaysOnlyIdsAndAcceptedStatus() throws Exception {
		HTTPRepository repository = mock(HTTPRepository.class);
		when(repository.manageBackups("restore", "b1", "new-repo")).thenReturn("{\"state\":\"RUNNING\"}");
		BackupsServlet servlet = new BackupsServlet();
		servlet.setRepository(repository);
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/backups");
		request.setParameter("action", "restore");
		request.setParameter("backupId", "b1");
		request.setParameter("targetRepositoryId", "new-repo");
		request.setParameter("path", "/ignored");
		MockHttpServletResponse response = new MockHttpServletResponse();
		servlet.doPost(new WorkbenchRequest(repository, request, Map.of()), response);
		assertThat(response.getStatus()).isEqualTo(202);
		verify(repository).manageBackups("restore", "b1", "new-repo");
	}
}
