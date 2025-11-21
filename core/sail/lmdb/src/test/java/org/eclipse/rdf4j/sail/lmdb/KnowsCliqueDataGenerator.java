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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.repository.RepositoryConnection;

/**
 * Generates fully connected cliques of people linked by {@code foaf:knows}. Configurable for test data.
 */
public final class KnowsCliqueDataGenerator {

	public static final class Config {
		private final ValueFactory vf;
		private int cliqueSize = 3;
		private int cliqueCount = 1;
		private boolean bidirectional = true;
		private boolean includeTypeAssertion = true;
		private String personNamespace = "urn:person";
		private String cliqueLabelPrefix = "c";
		private String personLabelPrefix = "p";
		private long startIndex = 0;

		public Config(ValueFactory vf) {
			this.vf = Objects.requireNonNull(vf, "vf");
		}

		public Config cliqueSize(int size) {
			this.cliqueSize = size;
			return this;
		}

		public Config cliqueCount(int count) {
			this.cliqueCount = count;
			return this;
		}

		public Config bidirectional(boolean bidirectional) {
			this.bidirectional = bidirectional;
			return this;
		}

		public Config includeTypeAssertion(boolean includeTypeAssertion) {
			this.includeTypeAssertion = includeTypeAssertion;
			return this;
		}

		public Config personNamespace(String ns) {
			this.personNamespace = Objects.requireNonNull(ns, "personNamespace");
			return this;
		}

		public Config cliqueLabelPrefix(String prefix) {
			this.cliqueLabelPrefix = Objects.requireNonNull(prefix, "cliqueLabelPrefix");
			return this;
		}

		public Config personLabelPrefix(String prefix) {
			this.personLabelPrefix = Objects.requireNonNull(prefix, "personLabelPrefix");
			return this;
		}

		public Config startIndex(long startIndex) {
			this.startIndex = startIndex;
			return this;
		}

		Config copy() {
			Config c = new Config(vf);
			c.cliqueSize = cliqueSize;
			c.cliqueCount = cliqueCount;
			c.bidirectional = bidirectional;
			c.includeTypeAssertion = includeTypeAssertion;
			c.personNamespace = personNamespace;
			c.cliqueLabelPrefix = cliqueLabelPrefix;
			c.personLabelPrefix = personLabelPrefix;
			c.startIndex = startIndex;
			return c;
		}
	}

	private final Config config;

	private KnowsCliqueDataGenerator(Config config) {
		this.config = config.copy();
	}

	public static KnowsCliqueDataGenerator withConfig(Config config) {
		return new KnowsCliqueDataGenerator(config);
	}

	/**
	 * Generate statements for the configured cliques.
	 */
	public List<Statement> generate() {
		List<Statement> statements = new ArrayList<>();
		ValueFactory vf = config.vf;
		long globalIndex = config.startIndex;
		for (int c = 0; c < config.cliqueCount; c++) {
			List<IRI> people = new ArrayList<>(config.cliqueSize);
			for (int i = 0; i < config.cliqueSize; i++) {
				IRI person = vf.createIRI(config.personNamespace + ":" + config.cliqueLabelPrefix + c + "-"
						+ config.personLabelPrefix + (globalIndex++));
				people.add(person);
				if (config.includeTypeAssertion) {
					statements.add(vf.createStatement(person, RDF.TYPE, FOAF.PERSON));
				}
			}
			for (int i = 0; i < people.size(); i++) {
				for (int j = 0; j < people.size(); j++) {
					if (i == j) {
						continue;
					}
					if (!config.bidirectional && j <= i) {
						continue; // emit only one direction when not bidirectional
					}
					statements.add(vf.createStatement(people.get(i), FOAF.KNOWS, people.get(j)));
				}
			}
		}
		return statements;
	}

	/**
	 * Convenience helper to write directly into a repository connection.
	 */
	public void addTo(RepositoryConnection connection) {
		for (Statement st : generate()) {
			connection.add(st);
		}
	}
}
