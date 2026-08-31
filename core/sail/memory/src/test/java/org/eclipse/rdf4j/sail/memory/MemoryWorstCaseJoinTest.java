/*******************************************************************************
 * Copyright (c) 2024 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ******************************************************************************/
package org.eclipse.rdf4j.sail.memory;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.Query;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.explanation.Explanation;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.testsuite.sail.SailHelper;
import org.junit.jupiter.api.Test;

public class MemoryWorstCaseJoinTest {

	private final ValueFactory vf = SimpleValueFactory.getInstance();

	@Test
	public void leapfrogTrieJoinIsUsed() {
		MemoryStore store = new MemoryStore();
		try (SailRepository repository = new SailRepository(store)) {
			repository.init();
			try (RepositoryConnection connection = repository.getConnection()) {
				IRI friend = vf.createIRI("http://example.com/friend");
				IRI type = vf.createIRI("http://example.com/type");
				IRI person = vf.createIRI("http://example.com/Person");
				connection.add(vf.createIRI("http://example.com/alice"), type, person);
				connection.add(vf.createIRI("http://example.com/bob"), type, person);
				connection.add(vf.createIRI("http://example.com/alice"), friend,
						vf.createIRI("http://example.com/bob"));
				connection.add(vf.createIRI("http://example.com/alice"), friend,
						vf.createIRI("http://example.com/charlie"));
				connection.add(vf.createIRI("http://example.com/bob"), friend,
						vf.createIRI("http://example.com/alice"));

				Query query = connection.prepareTupleQuery("SELECT ?s ?o WHERE { ?s <" + type + "> <" + person
						+ "> . ?s <" + friend + "> ?o . }");
				Explanation explanation = query.explain(Explanation.Level.Executed);
				assertThat(explanation.toString()).contains("LeapfrogTrieJoin");

				List<BindingSet> results = new ArrayList<>();
				try (TupleQueryResult result = SailHelper.evaluateTupleQuery(connection, query)) {
					result.forEachRemaining(results::add);
				}

				assertThat(results).hasSize(3);
			}
		}
	}
}
