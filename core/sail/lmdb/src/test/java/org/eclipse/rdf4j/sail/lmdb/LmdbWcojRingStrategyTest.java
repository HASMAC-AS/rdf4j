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
import java.util.stream.Collectors;

import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.impl.MapBindingSet;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.lmdb.config.LmdbStoreConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LmdbWcojRingStrategyTest {

	@TempDir
	File tempDir;

	private String previousStrategy;
	private String previousTracking;

	@BeforeEach
	void setupProps() {
		previousStrategy = System.getProperty("rdf4j.lmdb.wcoj.strategy");
		previousTracking = System.getProperty("rdf4j.lmdb.wcoj.trackPartial");
	}

	@AfterEach
	void restoreProps() {
		if (previousStrategy == null) {
			System.clearProperty("rdf4j.lmdb.wcoj.strategy");
		} else {
			System.setProperty("rdf4j.lmdb.wcoj.strategy", previousStrategy);
		}
		if (previousTracking == null) {
			System.clearProperty("rdf4j.lmdb.wcoj.trackPartial");
		} else {
			System.setProperty("rdf4j.lmdb.wcoj.trackPartial", previousTracking);
		}
	}

	@Test
	void ringAndLkjMatchResultsAndRingDoesNotExploreMore() throws Exception {
		// moderately sized clique creates multiple cyclic joins
		QueryOutcome ltj = runQueryWithStrategy("ltj", cliqueConfig(true), "ltj-cyclic");
		QueryOutcome ring = runQueryWithStrategy("ring", cliqueConfig(true), "ring-cyclic");

		assertThat(ring.results).containsExactlyInAnyOrderElementsOf(ltj.results);
		assertThat(ring.partialBindings).isLessThanOrEqualTo(ltj.partialBindings);
	}

	@Test
	void defaultUsesRingOnCyclicAndLtjOnAcyclic() throws Exception {
		System.clearProperty("rdf4j.lmdb.wcoj.strategy");
		System.setProperty("rdf4j.lmdb.wcoj.trackPartial", "true");

		QueryOutcome defaultCyclic = runQueryWithStrategy(null, cliqueConfig(true), "auto-cyclic");
		assertThat(defaultCyclic.strategy).isEqualTo(LmdbWcojBGPQueryEvaluationStep.WcojStrategy.RING);

		QueryOutcome acyclic = runAcyclicQueryWithStrategy(null);
		assertThat(acyclic.strategy).isEqualTo(LmdbWcojBGPQueryEvaluationStep.WcojStrategy.LTJ);
	}

	@Test
	void ringPrunesFailedCyclesInDirectionalClique() throws Exception {
		KnowsCliqueDataGenerator.Config cfg = cliqueConfig(false).cliqueSize(7);

		QueryOutcome ltj = runQueryWithStrategy("ltj", cfg, "ltj-dir");
		QueryOutcome ring = runQueryWithStrategy("ring", cfg, "ring-dir");

		assertThat(ring.results).isEmpty();
		assertThat(ltj.results).isEmpty();
		assertThat(ring.framesPushed).isLessThan(ltj.framesPushed);
	}

	private KnowsCliqueDataGenerator.Config cliqueConfig(boolean bidirectional) {
		ValueFactory vf = org.eclipse.rdf4j.model.impl.SimpleValueFactory.getInstance();
		return new KnowsCliqueDataGenerator.Config(vf).cliqueCount(1)
				.cliqueSize(4)
				.bidirectional(bidirectional)
				.includeTypeAssertion(false);
	}

	private QueryOutcome runQueryWithStrategy(String strategy, KnowsCliqueDataGenerator.Config cliqueConfig,
			String label) throws Exception {
		if (strategy == null) {
			System.clearProperty("rdf4j.lmdb.wcoj.strategy");
		} else {
			System.setProperty("rdf4j.lmdb.wcoj.strategy", strategy);
		}
		System.setProperty("rdf4j.lmdb.wcoj.trackPartial", "true");

		LmdbStoreConfig cfg = new LmdbStoreConfig("spoc,posc").setMaintainTrieIndexes(true).setUseWcojForBgp(true);
		File dir = new File(tempDir, "store-" + (label == null ? "default" : label));
		LmdbStore store = new LmdbStore(dir, cfg);
		store.init();

		List<Statement> stmts = KnowsCliqueDataGenerator.withConfig(cliqueConfig).generate();

		SailRepository repo = new SailRepository(store);
		repo.init();
		try (RepositoryConnection conn = repo.getConnection()) {
			conn.add(stmts);
			TupleQuery tq = conn
					.prepareTupleQuery("SELECT ?a ?b ?c WHERE { ?a <" + FOAF.KNOWS.stringValue() + "> ?b . "
							+ "?b <" + FOAF.KNOWS.stringValue() + "> ?c . "
							+ "?c <" + FOAF.KNOWS.stringValue() + "> ?a }");
			List<BindingSet> results = detach(tq.evaluate().stream().collect(Collectors.toList()));
			LmdbWcojBGPQueryEvaluationStep.Metrics metrics = LmdbWcojBGPQueryEvaluationStep.pollLastMetrics();
			long partial = metrics == null ? -1 : metrics.getPartialBindings();
			LmdbWcojBGPQueryEvaluationStep.WcojStrategy strat = metrics == null ? null : metrics.getStrategy();
			long frames = metrics == null ? -1 : metrics.getFramesPushed();
			return new QueryOutcome(results, partial, strat, frames);
		} finally {
			repo.shutDown();
			store.shutDown();
		}
	}

	private QueryOutcome runAcyclicQueryWithStrategy(String strategy) throws Exception {
		if (strategy == null) {
			System.clearProperty("rdf4j.lmdb.wcoj.strategy");
		} else {
			System.setProperty("rdf4j.lmdb.wcoj.strategy", strategy);
		}
		System.setProperty("rdf4j.lmdb.wcoj.trackPartial", "true");

		LmdbStoreConfig cfg = new LmdbStoreConfig("spoc,posc").setMaintainTrieIndexes(true).setUseWcojForBgp(true);
		File dir = new File(tempDir, "store-acyclic-" + (strategy == null ? "default" : strategy));
		LmdbStore store = new LmdbStore(dir, cfg);
		store.init();

		ValueFactory vf = org.eclipse.rdf4j.model.impl.SimpleValueFactory.getInstance();
		List<Statement> stmts = new ArrayList<>();
		org.eclipse.rdf4j.model.IRI a = vf.createIRI("urn:a");
		org.eclipse.rdf4j.model.IRI b = vf.createIRI("urn:b");
		org.eclipse.rdf4j.model.IRI c = vf.createIRI("urn:c");
		stmts.add(vf.createStatement(a, FOAF.KNOWS, b));
		stmts.add(vf.createStatement(b, FOAF.KNOWS, c));

		SailRepository repo = new SailRepository(store);
		repo.init();
		try (RepositoryConnection conn = repo.getConnection()) {
			conn.add(stmts);
			TupleQuery tq = conn
					.prepareTupleQuery("SELECT ?a ?b ?c WHERE { ?a <" + FOAF.KNOWS.stringValue() + "> ?b . "
							+ "?b <" + FOAF.KNOWS.stringValue() + "> ?c }");
			List<BindingSet> results = detach(tq.evaluate().stream().collect(Collectors.toList()));
			LmdbWcojBGPQueryEvaluationStep.Metrics metrics = LmdbWcojBGPQueryEvaluationStep.pollLastMetrics();
			long partial = metrics == null ? -1 : metrics.getPartialBindings();
			LmdbWcojBGPQueryEvaluationStep.WcojStrategy strat = metrics == null ? null : metrics.getStrategy();
			long frames = metrics == null ? -1 : metrics.getFramesPushed();
			return new QueryOutcome(results, partial, strat, frames);
		} finally {
			repo.shutDown();
			store.shutDown();
		}
	}

	private static final class QueryOutcome {
		final List<BindingSet> results;
		final long partialBindings;
		final LmdbWcojBGPQueryEvaluationStep.WcojStrategy strategy;
		final long framesPushed;

		QueryOutcome(List<BindingSet> results, long partialBindings,
				LmdbWcojBGPQueryEvaluationStep.WcojStrategy strategy, long framesPushed) {
			this.results = results;
			this.partialBindings = partialBindings;
			this.strategy = strategy;
			this.framesPushed = framesPushed;
		}
	}

	private List<BindingSet> detach(List<BindingSet> raw) {
		ValueFactory vf = org.eclipse.rdf4j.model.impl.SimpleValueFactory.getInstance();
		List<BindingSet> out = new ArrayList<>(raw.size());
		for (BindingSet bs : raw) {
			MapBindingSet copy = new MapBindingSet();
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
					(org.eclipse.rdf4j.model.IRI) cloneValue(vf, t.getPredicate()), cloneValue(vf, t.getObject()));
		}
		return v;
	}
}
