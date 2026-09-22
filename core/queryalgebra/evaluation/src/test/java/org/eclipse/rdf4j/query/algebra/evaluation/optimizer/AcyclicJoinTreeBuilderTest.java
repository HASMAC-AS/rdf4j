/*******************************************************************************
 * Copyright (c) 2025 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ******************************************************************************/
package org.eclipse.rdf4j.query.algebra.evaluation.optimizer;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.algebra.Filter;
import org.eclipse.rdf4j.query.algebra.Join;
import org.eclipse.rdf4j.query.algebra.StatementPattern;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.query.algebra.Var;
import org.junit.jupiter.api.Test;

class AcyclicJoinTreeBuilderTest {

	private final ValueFactory vf = SimpleValueFactory.getInstance();

	@Test
	void buildsJoinTreeForAcyclicBgp() {
		StatementPattern first = statement("a", iri("p1"), "b");
		StatementPattern second = statement("b", iri("p2"), "c");
		StatementPattern third = statement("c", iri("p3"), "d");

		TupleExpr bgp = join(first, join(second, third));

		Optional<JoinTree> result = AcyclicJoinTreeBuilder.build(bgp);

		assertThat(result).isPresent();
		JoinTree tree = result.get();

		JoinTreeNode root = tree.getRoot();
		assertThat(root.getStatementPattern()).isEqualTo(second);
		assertThat(root.getJoinVariables()).isEmpty();
		assertThat(root.getChildren()).hasSize(2);
		assertThat(root.getChildren())
				.anySatisfy(node -> {
					assertThat(node.getStatementPattern()).isEqualTo(first);
					assertThat(node.getJoinVariables()).containsExactly("b");
				})
				.anySatisfy(node -> {
					assertThat(node.getStatementPattern()).isEqualTo(third);
					assertThat(node.getJoinVariables()).containsExactly("c");
				});
	}

	@Test
	void returnsEmptyForCyclicBgp() {
		StatementPattern first = statement("a", iri("p1"), "b");
		StatementPattern second = statement("b", iri("p2"), "c");
		StatementPattern third = statement("c", iri("p3"), "a");

		TupleExpr cycle = join(first, join(second, third));

		assertThat(AcyclicJoinTreeBuilder.build(cycle)).isEmpty();
	}

	@Test
	void buildsJoinTreeForDisconnectedBgp() {
		StatementPattern first = statement("a", iri("p1"), "b");
		StatementPattern second = statement("c", iri("p2"), "d");
		StatementPattern third = statement("e", iri("p3"), "f");

		TupleExpr disconnected = join(first, join(second, third));

		Optional<JoinTree> result = AcyclicJoinTreeBuilder.build(disconnected);

		assertThat(result).isPresent();
		JoinTreeNode root = result.get().getRoot();
		assertThat(root.getJoinVariables()).isEmpty();
		assertThat(root.getChildren()).hasSize(2);
		root.getChildren().forEach(child -> assertThat(child.getJoinVariables()).isEmpty());
	}

	@Test
	void buildsJoinTreeWhenFiltersArePresent() {
		StatementPattern first = statement("a", iri("p1"), "b");
		StatementPattern second = statement("b", iri("p2"), "c");

		TupleExpr filtered = new Filter(join(first, second), new Var("b"));

		Optional<JoinTree> result = AcyclicJoinTreeBuilder.build(filtered);

		assertThat(result).isPresent();
		JoinTreeNode root = result.get().getRoot();
		assertThat(root.getStatementPattern()).isEqualTo(second);
		assertThat(root.getChildren()).singleElement()
				.satisfies(node -> {
					assertThat(node.getStatementPattern()).isEqualTo(first);
					assertThat(node.getJoinVariables()).containsExactly("b");
				});
	}

	private TupleExpr join(TupleExpr left, TupleExpr right) {
		return new Join(left, right);
	}

	private StatementPattern statement(String subj, IRI pred, String obj) {
		return new StatementPattern(new Var(subj), new Var(pred.stringValue(), pred), new Var(obj));
	}

	private IRI iri(String localName) {
		return vf.createIRI("http://example.org/", localName);
	}
}
