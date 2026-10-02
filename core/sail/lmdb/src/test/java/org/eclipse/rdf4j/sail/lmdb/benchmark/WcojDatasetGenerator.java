/*******************************************************************************
 * Copyright (c) 2025 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/

package org.eclipse.rdf4j.sail.lmdb.benchmark;

import java.util.Objects;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.repository.RepositoryConnection;

/**
 * Deterministic data generator for producing cyclic join workloads that benefit worst-case optimal joins.
 */
public class WcojDatasetGenerator {

	private static final String CONNECTED_LOCAL_NAME = "connected";
	private static final String TAG_LOCAL_NAME = "tag";

	private final ValueFactory valueFactory;
	private final String seedToken;
	private final IRI connected;
	private final IRI tag;

	public WcojDatasetGenerator(ValueFactory valueFactory) {
		this(valueFactory, 24L);
	}

	public WcojDatasetGenerator(ValueFactory valueFactory, long seed) {
		this.valueFactory = Objects.requireNonNull(valueFactory, "valueFactory");
		this.seedToken = Long.toHexString(seed);
		this.connected = valueFactory.createIRI(WcojBenchmarkQueries.NAMESPACE, CONNECTED_LOCAL_NAME);
		this.tag = valueFactory.createIRI(WcojBenchmarkQueries.NAMESPACE, TAG_LOCAL_NAME);
	}

	public GenerationResult generate(RepositoryConnection connection, int triangleCount, int fanoutPerNode) {
		Objects.requireNonNull(connection, "connection");
		if (triangleCount <= 0) {
			throw new IllegalArgumentException("triangleCount must be positive");
		}
		if (fanoutPerNode < 0) {
			throw new IllegalArgumentException("fanoutPerNode may not be negative");
		}

		connection.begin();
		long statements = 0;
		for (int i = 0; i < triangleCount; i++) {
			IRI a = vertex("a", i);
			IRI b = vertex("b", i);
			IRI c = vertex("c", i);
			IRI d = vertex("d", i);

			statements += addEdge(connection, a, b);
			statements += addEdge(connection, b, c);
			statements += addEdge(connection, c, a);
			statements += addEdge(connection, c, d);
			statements += addEdge(connection, d, a);

			statements += attachFanout(connection, a, i, fanoutPerNode, "a");
			statements += attachFanout(connection, b, i, fanoutPerNode, "b");
			statements += attachFanout(connection, c, i, fanoutPerNode, "c");
			statements += attachFanout(connection, d, i, fanoutPerNode, "d");
		}
		connection.commit();
		return new GenerationResult(triangleCount, fanoutPerNode, statements);
	}

	private long attachFanout(RepositoryConnection connection, IRI node, int triangleId, int fanoutPerNode,
			String nodeLabel) {
		long additions = 0;
		for (int i = 0; i < fanoutPerNode; i++) {
			Literal detail = valueFactory.createLiteral(seedToken + "-t-" + triangleId + '-' + nodeLabel + '-' + i);
			connection.add(node, tag, detail);
			additions++;
		}
		return additions;
	}

	private long addEdge(RepositoryConnection connection, IRI from, IRI to) {
		connection.add(from, connected, to);
		return 1;
	}

	private IRI vertex(String prefix, int id) {
		return valueFactory.createIRI(WcojBenchmarkQueries.NAMESPACE, prefix + '-' + id);
	}

	public static final class GenerationResult {
		private final int triangleCount;
		private final int fanoutPerNode;
		private final long statementCount;

		private GenerationResult(int triangleCount, int fanoutPerNode, long statementCount) {
			this.triangleCount = triangleCount;
			this.fanoutPerNode = fanoutPerNode;
			this.statementCount = statementCount;
		}

		public int getTriangleCount() {
			return triangleCount;
		}

		public int getFanoutPerNode() {
			return fanoutPerNode;
		}

		public long getStatementCount() {
			return statementCount;
		}
	}
}
