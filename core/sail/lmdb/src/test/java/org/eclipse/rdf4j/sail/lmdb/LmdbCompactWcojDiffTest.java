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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.lmdb.LmdbStoreConnection;
import org.eclipse.rdf4j.sail.lmdb.config.LmdbStoreConfig;
import org.eclipse.rdf4j.sail.lmdb.model.LmdbValue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LmdbCompactWcojDiffTest {

	@TempDir
	File tempDir;

	private List<String> perms;

	@BeforeEach
	void setup() {
		perms = List.of("spoc", "posc");
	}

	@AfterEach
	void tearDown() {
		// repositories closed in helper
	}

	@Test
	void compactBackendMatchesLegacyWithExplicitAndInferred() throws Exception {
		// explicit clique plus inferred extra edge to exercise union handling
		ValueFactory vf = org.eclipse.rdf4j.model.impl.SimpleValueFactory.getInstance();
		KnowsCliqueDataGenerator.Config cfg = new KnowsCliqueDataGenerator.Config(vf).cliqueCount(1)
				.cliqueSize(3)
				.bidirectional(true)
				.includeTypeAssertion(false);
		List<Statement> explicit = KnowsCliqueDataGenerator.withConfig(cfg).generate();

		List<Statement> inferred = new ArrayList<>();
		Statement inferredEdge = vf.createStatement(vf.createIRI("urn:i0"), FOAF.KNOWS, vf.createIRI("urn:i1"));
		Statement inferredEdge2 = vf.createStatement(vf.createIRI("urn:i1"), FOAF.KNOWS, vf.createIRI("urn:i2"));
		inferred.add(inferredEdge);
		inferred.add(inferredEdge2);

		List<BindingSet> legacy = runQueryWithBackend(new File(tempDir, "legacy"), false, explicit, inferred);
		List<BindingSet> compact = runQueryWithBackend(new File(tempDir, "compact"), true, explicit, inferred);

		assertThat(compact).containsExactlyInAnyOrderElementsOf(legacy);
	}

	private List<BindingSet> runQueryWithBackend(File dir, boolean useCompact, List<Statement> explicit,
			List<Statement> inferred) throws Exception {
		LmdbStoreConfig cfg = new LmdbStoreConfig("spoc,posc").setMaintainTrieIndexes(true)
				.setUseWcojForBgp(true)
				.setUseCompactTrie(useCompact);
		LmdbStore store = new LmdbStore(dir, cfg);
		store.init();
		// load data via raw Sail connection to control explicit/inferred placement
		try (LmdbStoreConnection cxn = (LmdbStoreConnection) store.getConnection()) {
			cxn.begin();
			for (Statement st : explicit) {
				cxn.addStatement(st.getSubject(), st.getPredicate(), st.getObject(), st.getContext());
			}
			for (Statement st : inferred) {
				cxn.addInferredStatement(st.getSubject(), st.getPredicate(), st.getObject(), st.getContext());
			}
			cxn.commit();
		}

		if (useCompact) {
			buildCompactTries(store, explicit, inferred);
		}

		SailRepository repo = new SailRepository(store);
		repo.init();
		try (RepositoryConnection conn = repo.getConnection()) {
			TupleQuery tq = conn
					.prepareTupleQuery("SELECT ?a ?b ?c WHERE { ?a <" + FOAF.KNOWS.stringValue() + "> ?b . "
							+ "?b <" + FOAF.KNOWS.stringValue() + "> ?c }");
			List<BindingSet> raw = tq.evaluate().stream().collect(java.util.stream.Collectors.toList());
			return detach(raw);
		} finally {
			repo.shutDown();
		}
	}

	private void buildCompactTries(LmdbStore store, List<Statement> explicit, List<Statement> inferred)
			throws Exception {
		LmdbSailStore backing = store.getBackingStore();
		ValueStore valueStore = backing.getValueStore();
		List<IdQuad> explicitIds = toIdQuads(explicit, valueStore);
		List<IdQuad> inferredIds = toIdQuads(inferred, valueStore);

		for (String perm : perms) {
			TrieIndexManager.IndexOrder order = new TrieIndexManager.IndexOrder(perm);
			java.nio.file.Path base = store.getDataDir().toPath();
			CompactTrieWriter.write(explicitIds, order, base.resolve("compact_" + perm + "_exp.bin"));
			CompactTrieWriter.write(inferredIds, order, base.resolve("compact_" + perm + "_inf.bin"));
		}
	}

	private List<IdQuad> toIdQuads(List<Statement> stmts, ValueStore valueStore) throws Exception {
		List<IdQuad> out = new ArrayList<>(stmts.size());
		for (Statement st : stmts) {
			long s = valueStore.getId(st.getSubject());
			long p = valueStore.getId(st.getPredicate());
			long o = valueStore.getId(st.getObject());
			long c = st.getContext() == null ? 0L : valueStore.getId(st.getContext());
			if (s == LmdbValue.UNKNOWN_ID || p == LmdbValue.UNKNOWN_ID || o == LmdbValue.UNKNOWN_ID) {
				continue;
			}
			out.add(new IdQuad(s, p, o, c));
		}
		return out;
	}

	private List<BindingSet> detach(List<BindingSet> raw) {
		ValueFactory vf = org.eclipse.rdf4j.model.impl.SimpleValueFactory.getInstance();
		List<BindingSet> out = new ArrayList<>(raw.size());
		for (BindingSet bs : raw) {
			org.eclipse.rdf4j.query.impl.MapBindingSet copy = new org.eclipse.rdf4j.query.impl.MapBindingSet();
			for (String name : bs.getBindingNames()) {
				copy.addBinding(name, cloneValue(vf, bs.getValue(name)));
			}
			out.add(copy);
		}
		return out;
	}

	private org.eclipse.rdf4j.model.Value cloneValue(ValueFactory vf, org.eclipse.rdf4j.model.Value v) {
		if (v == null) {
			return null;
		}
		if (v instanceof org.eclipse.rdf4j.model.IRI) {
			return vf.createIRI(v.stringValue());
		}
		if (v instanceof org.eclipse.rdf4j.model.BNode) {
			return vf.createBNode(((org.eclipse.rdf4j.model.BNode) v).getID());
		}
		if (v instanceof org.eclipse.rdf4j.model.Literal) {
			org.eclipse.rdf4j.model.Literal lit = (org.eclipse.rdf4j.model.Literal) v;
			if (lit.getLanguage().isPresent()) {
				return vf.createLiteral(lit.getLabel(), lit.getLanguage().get());
			}
			return vf.createLiteral(lit.getLabel(), lit.getDatatype());
		}
		if (v instanceof org.eclipse.rdf4j.model.Triple) {
			org.eclipse.rdf4j.model.Triple t = (org.eclipse.rdf4j.model.Triple) v;
			return vf.createTriple((org.eclipse.rdf4j.model.Resource) cloneValue(vf, t.getSubject()),
					(org.eclipse.rdf4j.model.IRI) cloneValue(vf, t.getPredicate()),
					cloneValue(vf, t.getObject()));
		}
		return v;
	}
}
