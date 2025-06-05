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
import java.net.InetAddress;
import java.time.Duration;
import java.util.UUID;

import org.apache.http.HttpHost;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.get.GetIndexRequest;
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers(disabledWithoutDocker = true)
public abstract class ElasticsearchTestContainer {

	private static final DockerImageName ES_IMAGE = DockerImageName
			.parse("docker.elastic.co/elasticsearch/elasticsearch:7.15.2")
			.asCompatibleSubstituteFor("docker.elastic.co/elasticsearch/elasticsearch");

	private static final String CLUSTER_NAME = "es-it-" + UUID.randomUUID();

	@Container
	protected static final ElasticsearchContainer ES = new ElasticsearchContainer(ES_IMAGE)
			.withEnv("cluster.name", CLUSTER_NAME)
			.withReuse(true)
			.withStartupTimeout(Duration.ofMinutes(2));

	protected static RestHighLevelClient client;

	protected static String hostname;
	protected static int transportPort;
	protected static String clusterName;

	@BeforeAll
	static void startContainer() {
		hostname = ES.getHost();
		transportPort = ES.getMappedPort(9300);
		clusterName = CLUSTER_NAME;
		client = new RestHighLevelClient(RestClient.builder(HttpHost.create(ES.getHttpHostAddress())));
	}

	@AfterEach
	void cleanIndices() throws Exception {
		client.indices().refresh(new RefreshRequest("*"), RequestOptions.DEFAULT);
		try (PreBuiltTransportClient transportClient = new PreBuiltTransportClient(
				Settings.builder().put("cluster.name", clusterName).build())) {
			transportClient.addTransportAddress(
					new TransportAddress(InetAddress.getByName(hostname), transportPort));
			String[] indices = transportClient.admin()
					.indices()
					.getIndex(new GetIndexRequest())
					.actionGet()
					.getIndices();
			for (String index : indices) {
				if (!index.equals(".geoip_databases")) {
					client.indices().delete(new DeleteIndexRequest(index), RequestOptions.DEFAULT);
				}
			}
		}
	}

	@AfterAll
	static void stopContainer() throws IOException {
		if (client != null) {
			client.close();
		}
	}
}
