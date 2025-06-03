/*****************************************************************************
 * Copyright (c) 2025 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *****************************************************************************/
package org.eclipse.rdf4j.sail.elasticsearchstore;

import java.io.IOException;
import java.time.Duration;
import java.util.UUID;

import org.apache.http.HttpHost;
import org.eclipse.rdf4j.testsuite.repository.RepositoryTest;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers(disabledWithoutDocker = true)
public abstract class ElasticsearchTestContainer extends RepositoryTest {

	protected static final String CLUSTER_NAME = "es-it-" + UUID.randomUUID();

	private static final DockerImageName ES_IMAGE = DockerImageName
			.parse("docker.elastic.co/elasticsearch/elasticsearch:8.14.0");

	@Container
	protected static final ElasticsearchContainer ES = new ElasticsearchContainer(ES_IMAGE)
			.withEnv("cluster.name", CLUSTER_NAME)
			.withReuse(true)
			.withStartupTimeout(Duration.ofMinutes(2));

	protected static RestHighLevelClient client;
	protected static int transportPort;

	@BeforeAll
	static void startContainer() {
		client = new RestHighLevelClient(RestClient.builder(HttpHost.create(ES.getHttpHostAddress())));
		transportPort = ES.getMappedPort(9300);
		System.setProperty("es.host", ES.getHttpHostAddress());
	}

	@AfterAll
	static void stopContainer() throws IOException {
		client.close();
	}

	protected String getHttpHostAddress() {
		return ES.getHttpHostAddress();
	}
}
