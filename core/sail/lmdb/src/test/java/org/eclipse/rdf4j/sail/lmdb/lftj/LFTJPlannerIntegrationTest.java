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
package org.eclipse.rdf4j.sail.lmdb.lftj;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.Set;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.explanation.Explanation;
import org.eclipse.rdf4j.query.explanation.GenericPlanNode;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.lmdb.LmdbStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LFTJPlannerIntegrationTest {

	@TempDir
	File tempDir;

	private SailRepository repository;

	private final ValueFactory vf = SimpleValueFactory.getInstance();

	@BeforeEach
	void setup() {
		repository = new SailRepository(new LmdbStore(tempDir));
		repository.init();
	}

	@AfterEach
	void cleanup() {
		repository.shutDown();
	}

	@Test
	void usesWcojForTriangleQueries() {
		IRI knows = vf.createIRI("urn:knows");
		IRI likes = vf.createIRI("urn:likes");
		Resource alice = vf.createIRI("urn:alice");
		Resource bob = vf.createIRI("urn:bob");
		Resource carol = vf.createIRI("urn:carol");

		try (RepositoryConnection conn = repository.getConnection()) {
			conn.add(alice, knows, bob);
			conn.add(bob, likes, carol);
			conn.add(carol, knows, alice);

			String query = "SELECT ?a ?b ?c WHERE { ?a <urn:knows> ?b . ?b <urn:likes> ?c . ?c <urn:knows> ?a . }";
			TupleQuery tupleQuery = conn.prepareTupleQuery(query);

			try (TupleQueryResult result = tupleQuery.evaluate()) {
				assertThat(result.hasNext()).isTrue();
				BindingSet bindingSet = result.next();
				assertThat(bindingSet.getBindingNames()).containsExactlyInAnyOrder("a", "b", "c");
				assertThat(bindingSet.getValue("a")).isEqualTo(alice);
				assertThat(bindingSet.getValue("b")).isEqualTo(bob);
				assertThat(bindingSet.getValue("c")).isEqualTo(carol);
				assertThat(result.hasNext()).isFalse();
			}

			Explanation explanation = tupleQuery.explain(Explanation.Level.Optimized);
			GenericPlanNode root = explanation.toGenericPlanNode();
			assertThat(containsNodeType(root, "LmdbWCOJ")).isTrue();
		}
	}

	private boolean containsNodeType(GenericPlanNode node, String expected) {
		if (expected.equals(node.getType())) {
			return true;
		}
		Set<GenericPlanNode> children = node.getPlans() == null ? Set.of() : Set.copyOf(node.getPlans());
		return children.stream().anyMatch(child -> containsNodeType(child, expected));
	}
}
