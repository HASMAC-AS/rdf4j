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
package org.eclipse.rdf4j.sail.elasticsearchstore.compliance;

import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.elasticsearchstore.ElasticsearchStore;
import org.eclipse.rdf4j.sail.elasticsearchstore.ElasticsearchTestContainer;
import org.eclipse.rdf4j.sail.elasticsearchstore.SingletonClientProvider;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Disabled;

public class ElasticsearchStoreRepositoryIT extends ElasticsearchTestContainer {

	private static SingletonClientProvider clientPool;

	@AfterAll
	public static void afterClass() throws Exception {
		if (clientPool != null) {
			clientPool.close();
		}
	}

	@Override
	protected Repository createRepository() {
		clientPool = new SingletonClientProvider("localhost", transportPort, CLUSTER_NAME);
		return new SailRepository(new ElasticsearchStore(clientPool, "index1"));
	}

	@Disabled
	@Override
	public void testShutdownFollowedByInit() {
		// ignore test
	}
}
