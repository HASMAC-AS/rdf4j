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
package org.eclipse.rdf4j.sparqlbuilder.constraint;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.rdf4j.sparqlbuilder.util.QueryAssert.assertSparqlEquals;

import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.sparqlbuilder.constraint.Operand;
import org.eclipse.rdf4j.sparqlbuilder.constraint.SparqlFunction;
import org.eclipse.rdf4j.sparqlbuilder.core.SparqlBuilder;
import org.eclipse.rdf4j.sparqlbuilder.core.Variable;
import org.eclipse.rdf4j.sparqlbuilder.core.query.Queries;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPattern;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPatterns;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.TriplePattern;
import org.eclipse.rdf4j.sparqlbuilder.rdf.Iri;
import org.eclipse.rdf4j.sparqlbuilder.rdf.Rdf;
import org.junit.jupiter.api.Test;

class ExpressionsTest {

	private static final Iri EX_AGE = Rdf.iri("http://example.com/ns#age");
	private static final Iri EX_FRIEND = Rdf.iri("http://example.com/ns#friend");
	private static final Iri EX_NAME = Rdf.iri("http://example.com/ns#name");
	private static final Iri EX_NICKNAME = Rdf.iri("http://example.com/ns#nickname");
	private static final Iri EX_MEASURE = Rdf.iri("http://example.com/ns#measure");
	private static final Iri EX_NORMALIZE = Rdf.iri("http://example.com/function/normalize");
	private static final Iri EX_SCALE = Rdf.iri("http://example.com/function/scale");

	@Test
	void exprAr01_arithmeticPrecedenceAndParentheses() {
		Variable age = SparqlBuilder.var("age");
		Variable s = SparqlBuilder.var("s");

		TriplePattern pattern = GraphPatterns.tp(s, EX_AGE, age);

		var query = Queries.SELECT()
				.select(
						SparqlBuilder.as(
								Expressions.add(age, Rdf.literalOf(2)),
								SparqlBuilder.var("plusTwo")),
						SparqlBuilder.as(
								Expressions.divide(
										Expressions.subtract(age, Rdf.literalOf(1)),
										Expressions.multiply(Rdf.literalOf(3), Rdf.literalOf(2))),
								SparqlBuilder.var("normalized")))
				.where(pattern.filter(Expressions.gt(Expressions.add(age, Rdf.literalOf(2)), Rdf.literalOf(21))));

		assertSparqlEquals(
				"SELECT ( ( ?age + 2 ) AS ?plusTwo ) ( ( ( ?age - 1 ) / ( 3 * 2 ) ) AS ?normalized ) WHERE { ?s <http://example.com/ns#age> ?age . FILTER ( ( ?age + 2 ) > 21 ) }",
				query.getQueryString());
	}

	@Test
	void exprBo01_booleanConnectivesAndNegation() {
		Variable s = SparqlBuilder.var("s");
		Variable friend = SparqlBuilder.var("friend");
		Variable name = SparqlBuilder.var("name");

		GraphPattern base = GraphPatterns.tp(s, RDF.TYPE, Rdf.iri("http://example.com/ns#Person"));

		GraphPattern optionalFriend = GraphPatterns.optional(
				GraphPatterns.tp(s, EX_FRIEND, friend)
						.and(GraphPatterns.tp(friend, FOAF.NAME, name))
						.filter(
								Expressions.or(
										Expressions.isBlank(friend),
										Expressions.not(Expressions.bound(name)))));

		var query = Queries.SELECT(s)
				.where(base, optionalFriend)
				.having(Expressions.gte(Expressions.count(friend), Rdf.literalOf(1)))
				.groupBy(s);

		assertSparqlEquals(
				"SELECT ?s WHERE { ?s <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://example.com/ns#Person> . OPTIONAL { ?s <http://example.com/ns#friend> ?friend . ?friend <http://xmlns.com/foaf/0.1/name> ?name . FILTER ( ( isBLANK( ?friend ) || !( BOUND( ?name ) ) ) ) } } GROUP BY ?s HAVING ( COUNT( ?friend ) >= 1 )",
				query.getQueryString());
	}

	@Test
	void exprCmp01_comparisonsAndInClauses() {
		Variable s = SparqlBuilder.var("s");
		Variable name = SparqlBuilder.var("name");

		GraphPattern pattern = GraphPatterns.tp(s, FOAF.NAME, name);

		var query = Queries.SELECT(s)
				.where(pattern.filter(Expressions.and(
						Expressions.regex(name, "^A", "i"),
						Expressions.in(name, Rdf.literalOf("Alice"), Rdf.literalOf("Alex")),
						Expressions.notIn(name, Rdf.literalOf("Aaron"), Rdf.literalOf("Avery")))))
				.orderBy(SparqlBuilder.asc(name));

		assertSparqlEquals(
				"SELECT ?s WHERE { ?s <http://xmlns.com/foaf/0.1/name> ?name . FILTER ( ( REGEX( ?name, \"^A\", \"i\" ) && ?name IN ( \"Alice\", \"Alex\" ) && ?name NOT IN ( \"Aaron\", \"Avery\" ) ) ) } ORDER BY ASC( ?name )",
				query.getQueryString());
	}

	@Test
	void exprAgg01_aggregatesWithAliasesAndSeparator() {
		Variable s = SparqlBuilder.var("s");
		Variable label = SparqlBuilder.var("label");
		Variable concatenated = SparqlBuilder.var("labels");

		GraphPattern pattern = GraphPatterns.tp(s, Rdf.iri("http://example.com/ns#label"), label);

		var query = Queries.SELECT(
				s,
				SparqlBuilder.as(Expressions.count(label), SparqlBuilder.var("labelCount")),
				SparqlBuilder.as(Expressions.group_concat(", ", label), concatenated))
				.where(pattern)
				.groupBy(s)
				.having(Expressions.gt(Expressions.count(label), Rdf.literalOf(1)));

		String expected = "SELECT ?s ( COUNT( ?label ) AS ?labelCount ) ( GROUP_CONCAT( ?label ; SEPARATOR = \", \" ) AS ?labels ) WHERE { ?s <http://example.com/ns#label> ?label . } GROUP BY ?s HAVING ( COUNT( ?label ) > 1 )";

		assertSparqlEquals(expected, query.getQueryString());
	}

	@Test
	void exprBind01_multipleBindOrderingPreserved() {
		Variable s = SparqlBuilder.var("s");
		Variable birthYear = SparqlBuilder.var("birthYear");
		Variable birthDate = SparqlBuilder.var("birthDate");

		GraphPattern base = GraphPatterns.tp(s, Rdf.iri("http://example.com/ns#birthDate"), birthDate);

		GraphPattern binds = GraphPatterns.and(
				Expressions.bind(Expressions.function(SparqlFunction.YEAR, birthDate), birthYear),
				Expressions.bind(
						Expressions.concat(
								Rdf.literalOf("Born in "),
								Expressions.str(birthYear)),
						SparqlBuilder.var("description")));

		var query = Queries.SELECT(s, birthYear)
				.where(base, binds)
				.orderBy(SparqlBuilder.desc(birthYear));

		assertSparqlEquals(
				"SELECT ?s ?birthYear WHERE { ?s <http://example.com/ns#birthDate> ?birthDate . { BIND( YEAR( ?birthDate ) AS ?birthYear ) BIND( CONCAT( \"Born in \", STR( ?birthYear ) ) AS ?description ) } } ORDER BY DESC( ?birthYear )",
				query.getQueryString());
	}

	@Test
	void exprCoa01_coalesceAndDatatypeHelpers() {
		Variable s = SparqlBuilder.var("s");
		Variable name = SparqlBuilder.var("name");
		Variable nickname = SparqlBuilder.var("nickname");
		Variable display = SparqlBuilder.var("display");
		Variable datatype = SparqlBuilder.var("datatype");

		GraphPattern base = GraphPatterns.tp(s, EX_NAME, name)
				.and(GraphPatterns.optional(GraphPatterns.tp(s, EX_NICKNAME, nickname)));

		GraphPattern binds = GraphPatterns.and(
				Expressions.bind(
						Expressions.coalesce(nickname, Expressions.str(name), Rdf.literalOf("Unknown")),
						display),
				Expressions.bind(Expressions.datatype(name), datatype));

		var query = Queries.SELECT(s, display, datatype)
				.where(base, binds)
				.orderBy(SparqlBuilder.asc(display));

		String expected = "SELECT ?s ?display ?datatype WHERE { { ?s <http://example.com/ns#name> ?name . OPTIONAL { ?s <http://example.com/ns#nickname> ?nickname . } } { BIND( COALESCE( ?nickname, STR( ?name ), \"Unknown\" ) AS ?display ) BIND( DATATYPE( ?name ) AS ?datatype ) } } ORDER BY ASC( ?display )";

		assertSparqlEquals(expected, query.getQueryString());
	}

	@Test
	void exprCus01_customFunctionsWithMultipleArguments() {
		Variable s = SparqlBuilder.var("s");
		Variable measurement = SparqlBuilder.var("measurement");
		Variable normalized = SparqlBuilder.var("normalized");
		Variable scaled = SparqlBuilder.var("scaled");

		GraphPattern base = GraphPatterns.tp(s, EX_MEASURE, measurement);

		GraphPattern binds = GraphPatterns.and(
				Expressions.bind(
						Expressions.custom(EX_NORMALIZE, measurement, Rdf.literalOf("metric")),
						normalized),
				Expressions.bind(Expressions.custom(EX_SCALE, measurement), scaled));

		var query = Queries.SELECT(s, normalized, scaled)
				.where(base, binds);

		String expected = "SELECT ?s ?normalized ?scaled WHERE { ?s <http://example.com/ns#measure> ?measurement . { BIND( <http://example.com/function/normalize>( ?measurement, \"metric\" ) AS ?normalized ) BIND( <http://example.com/function/scale>( ?measurement ) AS ?scaled ) } }";

		assertSparqlEquals(expected, query.getQueryString());
	}

	@Test
	void exprNeg01_inRequiresAtLeastOneOption() {
		Variable name = SparqlBuilder.var("name");

		assertThatThrownBy(() -> Expressions.in((Operand) name))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("at least one option");

		assertThatThrownBy(() -> Expressions.notIn((Operand) name))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("at least one option");
	}
}
