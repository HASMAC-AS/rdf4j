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
package org.eclipse.rdf4j.sparqlbuilder.core.query;

import static org.eclipse.rdf4j.sparqlbuilder.util.QueryAssert.assertSparqlEquals;

import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.sparqlbuilder.core.SparqlBuilder;
import org.eclipse.rdf4j.sparqlbuilder.graphpattern.GraphPatterns;
import org.eclipse.rdf4j.sparqlbuilder.rdf.Iri;
import org.eclipse.rdf4j.sparqlbuilder.rdf.Rdf;
import org.eclipse.rdf4j.sparqlbuilder.rdf.RdfLiteral;
import org.junit.jupiter.api.Test;

class UpdateQueriesTest {

	private static final Iri SUBJECT = Rdf.iri("http://example.com/ns#subject");
	private static final Iri PREDICATE = Rdf.iri("http://example.com/ns#predicate");
	private static final RdfLiteral OBJECT = Rdf.literalOf("value");
	private static final Iri GRAPH = Rdf.iri("http://example.com/graph");

	@Test
	void updData01_insertDataSingleTriple() {
		InsertDataQuery update = Queries.INSERT_DATA()
				.insertData(GraphPatterns.tp(SUBJECT, PREDICATE, OBJECT));

		assertSparqlEquals(
				"INSERT DATA { <http://example.com/ns#subject> <http://example.com/ns#predicate> \"value\" . }",
				update.getQueryString());
	}

	@Test
	void updData02_insertDataIntoNamedGraph() {
		InsertDataQuery update = Queries.INSERT_DATA()
				.into(GRAPH)
				.insertData(GraphPatterns.tp(SUBJECT, RDF.TYPE, Rdf.iri("http://example.com/ns#Class")));

		assertSparqlEquals(
				"INSERT DATA { GRAPH <http://example.com/graph> { <http://example.com/ns#subject> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://example.com/ns#Class> . } }",
				update.getQueryString());
	}

	@Test
	void updData03_deleteDataMultipleTriples() {
		DeleteDataQuery update = Queries.DELETE_DATA()
				.deleteData(GraphPatterns.tp(SUBJECT, PREDICATE, OBJECT))
				.deleteData(GraphPatterns.tp(SUBJECT, RDFS.LABEL, Rdf.literalOf("obsolete")));

		assertSparqlEquals(
				"DELETE DATA { <http://example.com/ns#subject> <http://example.com/ns#predicate> \"value\" . <http://example.com/ns#subject> <http://www.w3.org/2000/01/rdf-schema#label> \"obsolete\" . }",
				update.getQueryString());
	}

	@Test
	void updMod01_insertWhereClause() {
		ModifyQuery update = Queries.MODIFY()
				.insert(GraphPatterns.tp(SparqlBuilder.var("s"), PREDICATE, Rdf.literalOf("new")))
				.where(GraphPatterns.tp(SparqlBuilder.var("s"), PREDICATE, SparqlBuilder.var("o")));

		assertSparqlEquals(
				"INSERT { ?s <http://example.com/ns#predicate> \"new\" . } WHERE { ?s <http://example.com/ns#predicate> ?o . }",
				update.getQueryString());
	}

	@Test
	void updLoad01_loadIntoNamedGraph() {
		LoadQuery update = Queries.LOAD()
				.from(Rdf.iri("http://example.com/source"))
				.to(GRAPH)
				.silent();

		assertSparqlEquals(
				"LOAD SILENT <http://example.com/source> INTO GRAPH <http://example.com/graph>",
				update.getQueryString());
	}

	@Test
	void updClear01_clearNamedGraph() {
		ClearQuery update = Queries.CLEAR();
		update.graph(GRAPH);
		update.silent();

		assertSparqlEquals("CLEAR SILENT GRAPH <http://example.com/graph>", update.getQueryString());
	}

	@Test
	void updCopy01_copyGraph() {
		CopyQuery update = Queries.COPY();
		update.from(GRAPH);
		update.to(Rdf.iri("http://example.com/other"));
		update.silent();

		assertSparqlEquals(
				"COPY SILENT GRAPH <http://example.com/graph> TO GRAPH <http://example.com/other>",
				update.getQueryString());
	}

	@Test
	void updMod02_deleteInsertWhereWithDataset() {
		ModifyQuery update = Queries.MODIFY()
				.with(Rdf.iri("http://example.com/default"))
				.delete(GraphPatterns.tp(SparqlBuilder.var("person"), PREDICATE, SparqlBuilder.var("value")))
				.insert(GraphPatterns.tp(SparqlBuilder.var("person"), PREDICATE, Rdf.literalOf(42)))
				.where(GraphPatterns.tp(SparqlBuilder.var("person"), RDF.TYPE, Rdf.iri("http://example.com/ns#Person")))
				.using(Rdf.iri("http://example.com/dataset"))
				.usingNamed(Rdf.iri("http://example.com/named"));

		assertSparqlEquals(
				"WITH <http://example.com/default> DELETE { ?person <http://example.com/ns#predicate> ?value . } INSERT { ?person <http://example.com/ns#predicate> 42 . } USING <http://example.com/dataset> USING NAMED <http://example.com/named> WHERE { ?person <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://example.com/ns#Person> . }",
				update.getQueryString());
	}

	@Test
	void updMod03_insertWithUsingClause() {
		ModifyQuery update = Queries.MODIFY()
				.insert(GraphPatterns.tp(SparqlBuilder.var("s"), Rdf.iri("http://example.com/ns#age"),
						Rdf.literalOf(21)))
				.where(GraphPatterns.tp(SparqlBuilder.var("s"), Rdf.iri("http://example.com/ns#age"),
						SparqlBuilder.var("age")))
				.using(Rdf.iri("http://example.com/agedata"));

		assertSparqlEquals(
				"INSERT { ?s <http://example.com/ns#age> 21 . } USING <http://example.com/agedata> WHERE { ?s <http://example.com/ns#age> ?age . }",
				update.getQueryString());
	}
}
