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
package org.eclipse.rdf4j.sail.lmdb;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class KnowsCliqueDataGeneratorTest {

	private SailRepository repo;

	@BeforeEach
	void setup() {
		repo = new SailRepository(new MemoryStore());
		repo.init();
	}

	@AfterEach
	void tearDown() {
		if (repo != null) {
			repo.shutDown();
		}
	}

	@Test
	void generatesExpectedEdgesAndTypes() {
		try (SailRepositoryConnection cxn = repo.getConnection()) {
			KnowsCliqueDataGenerator.Config config = new KnowsCliqueDataGenerator.Config(cxn.getValueFactory())
					.cliqueCount(2)
					.cliqueSize(3)
					.bidirectional(true)
					.startIndex(0);

			KnowsCliqueDataGenerator generator = KnowsCliqueDataGenerator.withConfig(config);
			List<Statement> statements = generator.generate();
			assertThat(statements).isNotEmpty();

			// two cliques * 3 persons = 6 type statements if enabled
			long typeCount = statements.stream().filter(st -> st.getPredicate().equals(RDF.TYPE)).count();
			assertThat(typeCount).isEqualTo(6);

			// For each clique of 3, bidirectional edges: 3 * (3-1) = 6 per clique => 12 total
			long knowsCount = statements.stream().filter(st -> st.getPredicate().equals(FOAF.KNOWS)).count();
			assertThat(knowsCount).isEqualTo(12);

			generator.addTo(cxn);
			assertThat(cxn.size()).isEqualTo(knowsCount + typeCount);
		}
	}
}
