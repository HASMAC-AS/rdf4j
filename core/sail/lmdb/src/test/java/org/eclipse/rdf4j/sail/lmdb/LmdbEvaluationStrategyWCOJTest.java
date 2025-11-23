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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.EmptyIteration;
import org.eclipse.rdf4j.common.order.StatementOrder;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Triple;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategy;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryEvaluationStep;
import org.eclipse.rdf4j.query.algebra.evaluation.TripleSource;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.EvaluationStatistics;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.QueryEvaluationContext;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.base.SailDataset;
import org.eclipse.rdf4j.sail.base.SailDatasetTripleSource;
import org.eclipse.rdf4j.sail.lmdb.LmdbDatasetProvider;
import org.eclipse.rdf4j.sail.lmdb.LmdbDatasetSnapshot;
import org.eclipse.rdf4j.sail.lmdb.LmdbWCOJStep;
import org.eclipse.rdf4j.sail.lmdb.TxnManager;
import org.eclipse.rdf4j.sail.lmdb.ValueStore;
import org.eclipse.rdf4j.sail.lmdb.lftj.LmdbEvaluationStrategy;
import org.eclipse.rdf4j.sail.lmdb.lftj.LmdbWCOJ;
import org.eclipse.rdf4j.sail.lmdb.lftj.QuadKeyOrder;
import org.junit.jupiter.api.Test;

class LmdbEvaluationStrategyWCOJTest {

	private final ValueFactory vf = SimpleValueFactory.getInstance();

	@Test
	void precompileReturnsWcojStep() {
		TripleSource tripleSource = new StubTripleSource();
		EvaluationStrategy strategy = new LmdbEvaluationStrategy(tripleSource, null, null, 0,
				new EvaluationStatistics(), false);

		TupleExpr expr = new LmdbWCOJ(
				List.of(new StatementPattern(new Var("s"), new Var("p"), new Var("o")),
						new StatementPattern(new Var("o"), new Var("p2"), new Var("x")),
						new StatementPattern(new Var("x"), new Var("p3"), new Var("s"))));

		QueryEvaluationStep step = strategy.precompile(expr,
				new QueryEvaluationContext.Minimal((Literal) null,
						(Dataset) null,
						(Comparator<Value>) null));

		assertEquals(LmdbWCOJStep.class.getSimpleName(), step.getClass().getSimpleName());
	}

	@Test
	void datasetIntrospectionUnwrapsDelegatedUnion() throws Exception {
		SailDataset lmdbDataset = new StubLmdbDataset();
		SailDataset delegated = new DelegatingStubDataset(lmdbDataset);
		SailDataset union = createUnion(delegated, new NonLmdbDataset());

		TripleSource tripleSource = new SailDatasetTripleSource(vf, union);
		EvaluationStrategy strategy = new LmdbEvaluationStrategy(tripleSource, null, null, 0,
				new EvaluationStatistics(), false);

		TupleExpr expr = new LmdbWCOJ(
				List.of(new StatementPattern(new Var("s"), new Var("p"), new Var("o")),
						new StatementPattern(new Var("o"), new Var("p2"), new Var("x"))));

		QueryEvaluationStep step = strategy.precompile(expr,
				new QueryEvaluationContext.Minimal((Literal) null,
						(Dataset) null,
						(Comparator<Value>) null));

		assertEquals(LmdbWCOJStep.class.getSimpleName(), step.getClass().getSimpleName(),
				"Delegated union datasets should still expose LMDB snapshot for WCOJ");
	}

	private static final class StubTripleSource implements TripleSource, LmdbDatasetProvider {

		private final ValueFactory vf = SimpleValueFactory.getInstance();

		@Override
		public CloseableIteration<? extends Statement> getStatements(
				Resource subj, IRI pred,
				Value obj, Resource... contexts) {
			return new EmptyIteration<>();
		}

		@Override
		public CloseableIteration<? extends Statement> getStatements(
				StatementOrder order, Resource subj,
				IRI pred, Value obj,
				Resource... contexts) {
			return new EmptyIteration<>();
		}

		@Override
		public Set<StatementOrder> getSupportedOrders(
				Resource subj, IRI pred,
				Value obj, Resource... contexts) {
			return Collections.emptySet();
		}

		@Override
		public Comparator<Value> getComparator() {
			return null;
		}

		@Override
		public ValueFactory getValueFactory() {
			return vf;
		}

		@Override
		public LmdbDatasetSnapshot getLmdbDatasetSnapshot() {
			return new LmdbDatasetSnapshot() {
				@Override
				public TxnManager.Txn getTxn() {
					return null;
				}

				@Override
				public Map<QuadKeyOrder, Integer> indexHandles() {
					return Collections.emptyMap();
				}

				@Override
				public ValueStore valueStore() {
					return null;
				}

				@Override
				public boolean isExplicit() {
					return true;
				}
			};
		}
	}

	private static final class StubLmdbDataset implements SailDataset, LmdbDatasetSnapshot {

		@Override
		public TxnManager.Txn getTxn() {
			return null;
		}

		@Override
		public Map<QuadKeyOrder, Integer> indexHandles() {
			return Map.of();
		}

		@Override
		public ValueStore valueStore() {
			return null;
		}

		@Override
		public boolean isExplicit() {
			return true;
		}

		@Override
		public void close() {
		}

		@Override
		public CloseableIteration<? extends Namespace> getNamespaces() {
			return new EmptyIteration<>();
		}

		@Override
		public String getNamespace(String prefix) {
			return null;
		}

		@Override
		public CloseableIteration<? extends Resource> getContextIDs() {
			return new EmptyIteration<>();
		}

		@Override
		public CloseableIteration<? extends Statement> getStatements(Resource subj, IRI pred, Value obj,
				Resource... contexts) {
			return new EmptyIteration<>();
		}

		@Override
		public CloseableIteration<? extends Triple> getTriples(Resource subj, IRI pred, Value obj) {
			return new EmptyIteration<>();
		}

		@Override
		public CloseableIteration<? extends Statement> getStatements(StatementOrder statementOrder, Resource subj,
				IRI pred, Value obj, Resource... contexts) {
			return new EmptyIteration<>();
		}

		@Override
		public Set<StatementOrder> getSupportedOrders(Resource subj, IRI pred, Value obj, Resource... contexts) {
			return Collections.emptySet();
		}

		@Override
		public Comparator<Value> getComparator() {
			return null;
		}
	}

	private static final class DelegatingStubDataset implements SailDataset {

		private final SailDataset delegate;

		private DelegatingStubDataset(SailDataset delegate) {
			this.delegate = delegate;
		}

		@Override
		public void close() throws SailException {
			delegate.close();
		}

		@Override
		public CloseableIteration<? extends Namespace> getNamespaces() throws SailException {
			return delegate.getNamespaces();
		}

		@Override
		public String getNamespace(String prefix) throws SailException {
			return delegate.getNamespace(prefix);
		}

		@Override
		public CloseableIteration<? extends Resource> getContextIDs() throws SailException {
			return delegate.getContextIDs();
		}

		@Override
		public CloseableIteration<? extends Statement> getStatements(Resource subj, IRI pred, Value obj,
				Resource... contexts) throws SailException {
			return delegate.getStatements(subj, pred, obj, contexts);
		}

		@Override
		public CloseableIteration<? extends Statement> getStatements(StatementOrder statementOrder, Resource subj,
				IRI pred, Value obj, Resource... contexts) throws SailException {
			return delegate.getStatements(statementOrder, subj, pred, obj, contexts);
		}

		@Override
		public CloseableIteration<? extends Triple> getTriples(Resource subj, IRI pred, Value obj)
				throws SailException {
			return delegate.getTriples(subj, pred, obj);
		}

		@Override
		public Set<StatementOrder> getSupportedOrders(Resource subj, IRI pred, Value obj, Resource... contexts) {
			return delegate.getSupportedOrders(subj, pred, obj, contexts);
		}

		@Override
		public Comparator<Value> getComparator() {
			return delegate.getComparator();
		}
	}

	private static final class NonLmdbDataset implements SailDataset {

		@Override
		public void close() {
		}

		@Override
		public CloseableIteration<? extends Namespace> getNamespaces() {
			return new EmptyIteration<>();
		}

		@Override
		public String getNamespace(String prefix) {
			return null;
		}

		@Override
		public CloseableIteration<? extends Resource> getContextIDs() {
			return new EmptyIteration<>();
		}

		@Override
		public CloseableIteration<? extends Statement> getStatements(Resource subj, IRI pred, Value obj,
				Resource... contexts) {
			return new EmptyIteration<>();
		}

		@Override
		public CloseableIteration<? extends Statement> getStatements(StatementOrder statementOrder, Resource subj,
				IRI pred, Value obj, Resource... contexts) {
			return new EmptyIteration<>();
		}

		@Override
		public CloseableIteration<? extends Triple> getTriples(Resource subj, IRI pred, Value obj) {
			return new EmptyIteration<>();
		}

		@Override
		public Set<StatementOrder> getSupportedOrders(Resource subj, IRI pred, Value obj, Resource... contexts) {
			return Collections.emptySet();
		}

		@Override
		public Comparator<Value> getComparator() {
			return null;
		}
	}

	private SailDataset createUnion(SailDataset dataset1, SailDataset dataset2) throws Exception {
		Class<?> unionClass = Class.forName("org.eclipse.rdf4j.sail.base.UnionSailDataset");
		Method getInstance = unionClass.getDeclaredMethod("getInstance", SailDataset.class, SailDataset.class);
		getInstance.setAccessible(true);
		return (SailDataset) getInstance.invoke(null, dataset1, dataset2);
	}
}
