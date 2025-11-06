/*******************************************************************************
 * Copyright (c) 2024 Contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.impl.wcoj;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.CloseableIteratorIteration;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.query.algebra.Join;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategy;
import org.eclipse.rdf4j.query.algebra.evaluation.EvaluationStrategyFactory;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryBindingSet;
import org.eclipse.rdf4j.query.algebra.evaluation.TripleSource;
import org.eclipse.rdf4j.query.algebra.evaluation.impl.EvaluationStatistics;
import org.eclipse.rdf4j.query.impl.EmptyBindingSet;
import org.junit.jupiter.api.Test;

class WcojEvaluationStrategyTest {

	private static final String FACTORY_CLASS = "org.eclipse.rdf4j.query.algebra.evaluation.impl.wcoj.WcojEvaluationStrategyFactory";

	private final ValueFactory vf = SimpleValueFactory.getInstance();

	@Test
	void triangleQueryEnumeratesSingleCycle() throws Exception {
		EvaluationStrategy strategy = instantiateStrategy();

		TupleExpr expr = triangleBGP();
		List<BindingSet> results = QueryResults.asList(strategy.evaluate(expr, EmptyBindingSet.getInstance()));

		assertThat(results).hasSize(1);
		BindingSet binding = results.get(0);
		assertThat(binding.getValue("x")).isEqualTo(iri("ex:a"));
		assertThat(binding.getValue("y")).isEqualTo(iri("ex:b"));
		assertThat(binding.getValue("z")).isEqualTo(iri("ex:c"));
	}

	@Test
	void respectsExistingBindings() throws Exception {
		EvaluationStrategy strategy = instantiateStrategy();
		TupleExpr expr = triangleBGP();
		QueryBindingSet incoming = new QueryBindingSet();
		incoming.addBinding("y", iri("ex:b"));

		List<BindingSet> results = QueryResults.asList(strategy.evaluate(expr, incoming));

		assertThat(results).hasSize(1);
		assertThat(results.get(0).getValue("x")).isEqualTo(iri("ex:a"));
	}

	@Test
	void singleStatementPatternUsesDefaultEvaluation() throws Exception {
		EvaluationStrategy strategy = instantiateStrategy();
		StatementPattern pattern = new StatementPattern(var("s"), constant("foaf:knows"), var("o"));

		List<BindingSet> results = QueryResults.asList(strategy.evaluate(pattern, EmptyBindingSet.getInstance()));

		assertThat(results).hasSize(3);
	}

	private EvaluationStrategy instantiateStrategy()
			throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException,
			IllegalAccessException {
		Class<?> factoryClass = Class.forName(FACTORY_CLASS);
		EvaluationStrategyFactory factory = (EvaluationStrategyFactory) factoryClass.getDeclaredConstructor()
				.newInstance();
		return factory.createEvaluationStrategy(null, new ModelTripleSource(dataset()), new EvaluationStatistics());
	}

	private TupleExpr triangleBGP() {
		StatementPattern first = new StatementPattern(var("x"), constant("foaf:knows"), var("y"));
		StatementPattern second = new StatementPattern(var("y"), constant("foaf:knows"), var("z"));
		StatementPattern third = new StatementPattern(var("z"), constant("foaf:knows"), var("x"));
		return new Join(new Join(first, second), third);
	}

	private Model dataset() {
		Model model = new LinkedHashModel();
		model.add(iri("ex:a"), iri("foaf:knows"), iri("ex:b"));
		model.add(iri("ex:b"), iri("foaf:knows"), iri("ex:c"));
		model.add(iri("ex:c"), iri("foaf:knows"), iri("ex:a"));
		return model;
	}

	private Var var(String name) {
		return new Var(name);
	}

	private Var constant(String iri) {
		return new Var(iri, iri(iri));
	}

	private IRI iri(String prefixed) {
		return vf.createIRI("http://example.com/" + prefixed.substring(prefixed.indexOf(':') + 1));
	}

	private static final class ModelTripleSource implements TripleSource {

		private final Model model;
		private final ValueFactory valueFactory = SimpleValueFactory.getInstance();

		private ModelTripleSource(Model model) {
			this.model = model;
		}

		@Override
		public ValueFactory getValueFactory() {
			return valueFactory;
		}

		@Override
		public CloseableIteration<? extends Statement> getStatements(
				Resource subj, IRI pred, Value obj, Resource... contexts) throws QueryEvaluationException {
			List<Statement> statements = new ArrayList<>();
			for (Statement st : model.filter(subj, pred, obj, contexts)) {
				statements.add(st);
			}
			return new CloseableIteratorIteration<>(statements.iterator());
		}
	}
}
