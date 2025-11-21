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
import java.util.List;

import org.eclipse.rdf4j.common.iteration.Iterations;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryEvaluationStep;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.QueryEvaluationContext;
import org.eclipse.rdf4j.query.impl.EmptyBindingSet;
import org.eclipse.rdf4j.query.impl.MapBindingSet;
import org.eclipse.rdf4j.query.impl.SimpleDataset;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.NotifyingSailConnection;
import org.eclipse.rdf4j.sail.lmdb.LmdbEvaluationDataset;
import org.eclipse.rdf4j.sail.lmdb.LmdbSailStore;
import org.eclipse.rdf4j.sail.lmdb.RecordIterator;
import org.eclipse.rdf4j.sail.lmdb.TrieIndexManager;
import org.eclipse.rdf4j.sail.lmdb.TrieLevelCursor;
import org.eclipse.rdf4j.sail.lmdb.TxnManager;
import org.eclipse.rdf4j.sail.lmdb.ValueStore;
import org.eclipse.rdf4j.sail.lmdb.config.LmdbStoreConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LmdbWcojEvaluationStrategyTest {

	@TempDir
	File dataDir;

	private SailRepository repo;
	private LmdbStore store;

	@BeforeEach
	void setUp() {
		LmdbStoreConfig cfg = new LmdbStoreConfig("spoc,posc").setMaintainTrieIndexes(true).setUseWcojForBgp(true);
		store = new LmdbStore(dataDir, cfg);
		repo = new SailRepository(store);
		repo.init();
	}

	@AfterEach
	void tearDown() {
		if (repo != null) {
			repo.shutDown();
		}
	}

	@Test
	void evaluatesBgpViaTrieJoin() {
		IRI painter = repo.getValueFactory().createIRI("urn:painter");
		IRI painting = repo.getValueFactory().createIRI("urn:painting");
		IRI depicts = repo.getValueFactory().createIRI("urn:depicts");
		IRI museum = repo.getValueFactory().createIRI("urn:museum");

		try (RepositoryConnection conn = repo.getConnection()) {
			conn.add(painter, RDF.TYPE, RDFS.CLASS);
			conn.add(painting, RDF.TYPE, RDFS.CLASS);
			conn.add(painter, depicts, painting);
			conn.add(painting, depicts, museum);

			TupleQuery query = conn.prepareTupleQuery("SELECT ?s WHERE { ?s a <" + RDFS.CLASS.stringValue()
					+ "> ; <" + depicts.stringValue() + "> ?o . ?o <" + depicts.stringValue() + "> ?m }");
			List<BindingSet> results = query.evaluate().stream().collect(java.util.stream.Collectors.toList());

			assertThat(results).extracting(bs -> bs.getValue("s")).containsExactlyInAnyOrder(painter);
		}
	}

	@Test
	void usesTrieJoinWhenFallbackIsProvided() throws Exception {
		IRI painter = repo.getValueFactory().createIRI("urn:painter");
		IRI depicts = repo.getValueFactory().createIRI("urn:depicts");
		IRI painting = repo.getValueFactory().createIRI("urn:painting");
		IRI museum = repo.getValueFactory().createIRI("urn:museum");

		try (NotifyingSailConnection cxn = store.getConnection()) {
			cxn.begin();
			cxn.addStatement(painter, depicts, painting);
			cxn.addStatement(painting, depicts, museum);
			cxn.commit();
		}

		LmdbSailStore backingStore = store.getBackingStore();
		TrieIndexManager trieIndexManager = backingStore.getTrieIndexManager();
		TxnManager txnManager = backingStore.getTxnManager();
		ValueStore valueStore = backingStore.getValueStore();

		long painterId = valueStore.getId(painter);
		long depictsId = valueStore.getId(depicts);
		long paintingId = valueStore.getId(painting);

		List<Long> objects = new java.util.ArrayList<>();
		txnManager.doWith((stack, txn) -> {
			try (TrieLevelCursor l2 = trieIndexManager.openCursor("spoc", 2, true, txn)) {
				l2.openPrefix(painterId, depictsId);
				while (!l2.atEnd()) {
					objects.add(l2.key());
					if (!l2.next()) {
						break;
					}
				}
			}
			return null;
		});
		assertThat(objects).contains(paintingId);

		List<Long> subjects = new java.util.ArrayList<>();
		txnManager.doWith((stack, txn) -> {
			try (TrieLevelCursor l1 = trieIndexManager.openCursor("spoc", 1, true, txn)) {
				l1.openPrefix(true);
				while (!l1.atEnd()) {
					subjects.add(l1.key());
					if (!l1.next()) {
						break;
					}
				}
			}
			return null;
		});
		assertThat(subjects).contains(painterId, paintingId);

		StatementPattern p1 = new StatementPattern(new Var("s"), detached(depicts), new Var("o"));
		StatementPattern p2 = new StatementPattern(new Var("o"), detached(depicts), new Var("m"));
		List<StatementPattern> patterns = List.of(p1, p2);

		QueryEvaluationContext context = new QueryEvaluationContext.Minimal(null, repo.getValueFactory(), null);

		LmdbEvaluationDataset dataset = new LmdbEvaluationDataset() {
			@Override
			public RecordIterator getRecordIterator(StatementPattern pattern, BindingSet bindings) {
				throw new UnsupportedOperationException();
			}

			@Override
			public RecordIterator getRecordIterator(long[] binding, int subjIndex, int predIndex, int objIndex,
					int ctxIndex, long[] patternIds) {
				throw new UnsupportedOperationException();
			}

			@Override
			public ValueStore getValueStore() {
				return valueStore;
			}

			@Override
			public TrieIndexManager getTrieIndexManager() {
				return trieIndexManager;
			}

			@Override
			public TxnManager getTxnManager() {
				return txnManager;
			}
		};

		// Fallback that would return no results if used
		QueryEvaluationStep emptyFallback = bs -> new org.eclipse.rdf4j.common.iteration.EmptyIteration<>();

		assertThat(LmdbEvaluationStrategy.hasActiveConnectionChanges()).isFalse();

		LmdbWcojBGPQueryEvaluationStep step = new LmdbWcojBGPQueryEvaluationStep(patterns, context, dataset,
				trieIndexManager, txnManager, emptyFallback);

		List<BindingSet> results = org.eclipse.rdf4j.common.iteration.Iterations
				.asList(step.evaluate(org.eclipse.rdf4j.query.impl.EmptyBindingSet.getInstance()));

		assertThat(results).extracting(b -> b.getValue("s")).containsExactlyInAnyOrder(painter);
	}

	private Var detached(IRI iri) {
		return new Var("_const", iri);
	}

	@Test
	void respectsIncomingBindings() throws Exception {
		IRI painter = repo.getValueFactory().createIRI("urn:painter");
		IRI depicts = repo.getValueFactory().createIRI("urn:depicts");
		IRI painting = repo.getValueFactory().createIRI("urn:painting");
		IRI museum = repo.getValueFactory().createIRI("urn:museum");

		try (NotifyingSailConnection cxn = store.getConnection()) {
			cxn.begin();
			cxn.addStatement(painter, depicts, painting);
			cxn.addStatement(painting, depicts, museum);
			cxn.commit();
		}

		LmdbSailStore backingStore = store.getBackingStore();
		TrieIndexManager trieIndexManager = backingStore.getTrieIndexManager();
		TxnManager txnManager = backingStore.getTxnManager();
		ValueStore valueStore = backingStore.getValueStore();

		StatementPattern p1 = new StatementPattern(new Var("s"), detached(depicts), new Var("o"));
		StatementPattern p2 = new StatementPattern(new Var("o"), detached(depicts), new Var("m"));
		List<StatementPattern> patterns = List.of(p1, p2);

		QueryEvaluationContext context = new QueryEvaluationContext.Minimal(null, repo.getValueFactory(), null);

		LmdbEvaluationDataset dataset = new LmdbEvaluationDataset() {
			@Override
			public RecordIterator getRecordIterator(StatementPattern pattern, BindingSet bindings) {
				throw new UnsupportedOperationException();
			}

			@Override
			public RecordIterator getRecordIterator(long[] binding, int subjIndex, int predIndex, int objIndex,
					int ctxIndex, long[] patternIds) {
				throw new UnsupportedOperationException();
			}

			@Override
			public ValueStore getValueStore() {
				return valueStore;
			}

			@Override
			public TrieIndexManager getTrieIndexManager() {
				return trieIndexManager;
			}

			@Override
			public TxnManager getTxnManager() {
				return txnManager;
			}
		};

		MapBindingSet incoming = new MapBindingSet();
		incoming.addBinding("s", painting);

		LmdbWcojBGPQueryEvaluationStep step = new LmdbWcojBGPQueryEvaluationStep(patterns, context, dataset,
				trieIndexManager, txnManager, bs -> new org.eclipse.rdf4j.common.iteration.EmptyIteration<>());

		List<BindingSet> results = Iterations.asList(step.evaluate(incoming));

		assertThat(results).isEmpty();
	}

	@Test
	void ordersVarsBeforeTriePrefixes() throws Exception {
		// Recreate the store with only the spoc index so object cursors depend on a bound subject.
		if (repo != null) {
			repo.shutDown();
		}
		File spocOnlyDir = new File(dataDir, "spoc-only");
		spocOnlyDir.mkdirs();
		LmdbStoreConfig cfg = new LmdbStoreConfig("spoc").setMaintainTrieIndexes(true).setUseWcojForBgp(true);
		store = new LmdbStore(spocOnlyDir, cfg);
		repo = new SailRepository(store);
		repo.init();

		IRI p1 = repo.getValueFactory().createIRI("urn:p1");
		IRI p2 = repo.getValueFactory().createIRI("urn:p2");
		IRI s = repo.getValueFactory().createIRI("urn:s");
		IRI o = repo.getValueFactory().createIRI("urn:o");
		IRI x = repo.getValueFactory().createIRI("urn:x");

		try (NotifyingSailConnection cxn = store.getConnection()) {
			cxn.begin();
			cxn.addStatement(s, p1, o);
			cxn.addStatement(o, p2, x);
			cxn.commit();
		}

		LmdbSailStore backingStore = store.getBackingStore();
		TrieIndexManager trieIndexManager = backingStore.getTrieIndexManager();
		TxnManager txnManager = backingStore.getTxnManager();
		ValueStore valueStore = backingStore.getValueStore();

		StatementPattern pDecl1 = new StatementPattern(new Var("s"), detached(p1), new Var("o"));
		StatementPattern pDecl2 = new StatementPattern(new Var("o"), detached(p2), new Var("x"));
		List<StatementPattern> patterns = List.of(pDecl1, pDecl2);

		QueryEvaluationContext context = new QueryEvaluationContext.Minimal(null, repo.getValueFactory(), null);

		LmdbEvaluationDataset dataset = new LmdbEvaluationDataset() {
			@Override
			public RecordIterator getRecordIterator(StatementPattern pattern, BindingSet bindings) {
				throw new UnsupportedOperationException();
			}

			@Override
			public RecordIterator getRecordIterator(long[] binding, int subjIndex, int predIndex, int objIndex,
					int ctxIndex, long[] patternIds) {
				throw new UnsupportedOperationException();
			}

			@Override
			public ValueStore getValueStore() {
				return valueStore;
			}

			@Override
			public TrieIndexManager getTrieIndexManager() {
				return trieIndexManager;
			}

			@Override
			public TxnManager getTxnManager() {
				return txnManager;
			}
		};

		LmdbWcojBGPQueryEvaluationStep step = new LmdbWcojBGPQueryEvaluationStep(patterns, context, dataset,
				trieIndexManager, txnManager, bs -> new org.eclipse.rdf4j.common.iteration.EmptyIteration<>());

		List<BindingSet> results = Iterations.asList(step.evaluate(EmptyBindingSet.getInstance()));

		assertThat(results).extracting(b -> b.getValue("s")).containsExactly(s);
	}

	@Test
	void wcojHonorsDatasetContexts() {
		IRI p = repo.getValueFactory().createIRI("urn:p");
		IRI p2 = repo.getValueFactory().createIRI("urn:p2");
		IRI o = repo.getValueFactory().createIRI("urn:o");
		IRI o2 = repo.getValueFactory().createIRI("urn:o2");
		IRI o3 = repo.getValueFactory().createIRI("urn:o3");
		IRI s1 = repo.getValueFactory().createIRI("urn:s1");
		IRI s2 = repo.getValueFactory().createIRI("urn:s2");
		IRI g1 = repo.getValueFactory().createIRI("urn:g1");
		IRI g2 = repo.getValueFactory().createIRI("urn:g2");

		try (RepositoryConnection conn = repo.getConnection()) {
			conn.begin();
			conn.add(s1, p, o, g1);
			conn.add(s1, p2, o2, g1);
			conn.add(s2, p, o, g2);
			conn.add(s2, p2, o3, g2);
			conn.commit();

			SimpleDataset dataset = new SimpleDataset();
			dataset.addDefaultGraph(g1);

			String queryString = "SELECT ?s ?o2 WHERE { ?s <" + p.stringValue() + "> <" + o.stringValue()
					+ "> . ?s <" + p2.stringValue() + "> ?o2 }";
			TupleQuery query = conn.prepareTupleQuery(queryString);
			query.setDataset(dataset);

			List<BindingSet> results = query.evaluate().stream().collect(java.util.stream.Collectors.toList());

			assertThat(results).extracting(b -> b.getValue("s")).containsExactly(s1);
		}
	}
}
