/*******************************************************************************
 * Copyright (c) 2021 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/

package org.eclipse.rdf4j.sail.shacl;

import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.AfterClass;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.io.StringReader;

/**
 * @author Håvard Ottestad
 */
public class TransactionValidationLimitTest {

	@AfterClass
	public static void afterClass() {
		GlobalValidationExecutionLogging.loggingEnabled = false;
	}

	@Test
	public void testFailoverToBulkValidationSingleConnection() throws Exception {

		SailRepository shaclRepository = Utils.getInitializedShaclRepository("shacl.ttl");

		((ShaclSail) shaclRepository.getSail()).setTransactionalValidationLimit(3);

		try (SailRepositoryConnection connection = shaclRepository.getConnection()) {

			connection.begin();
			connection.add(RDFS.CLASS, RDFS.LABEL, connection.getValueFactory().createLiteral("a"));
			connection.commit();



			connection.begin();
			connection.add(RDFS.RESOURCE, RDF.TYPE, RDFS.RESOURCE);
			connection.add(RDFS.RESOURCE, RDFS.LABEL, connection.getValueFactory().createLiteral("a"));
			connection.add(RDFS.CLASS, RDF.TYPE, RDFS.RESOURCE);
			connection.add(RDFS.CLASS, RDFS.LABEL, connection.getValueFactory().createLiteral("yay"));
			connection.add(RDFS.CLASS, RDFS.LABEL, connection.getValueFactory().createLiteral("yay2"));
			connection.commit();


		} finally {
			shaclRepository.shutDown();
		}

	}

	@Test
	public void testFailoverToBulkValidationNewConnection() throws Exception {

		SailRepository shaclRepository = Utils.getInitializedShaclRepository("shacl.ttl");

		((ShaclSail) shaclRepository.getSail()).setTransactionalValidationLimit(3);

		try (SailRepositoryConnection connection = shaclRepository.getConnection()) {

			connection.begin();
			connection.add(RDFS.CLASS, RDFS.LABEL, connection.getValueFactory().createLiteral("a"));
			connection.commit();
		}

		try (SailRepositoryConnection connection = shaclRepository.getConnection()) {

			connection.begin();
			connection.add(RDFS.RESOURCE, RDF.TYPE, RDFS.RESOURCE);
			connection.add(RDFS.RESOURCE, RDFS.LABEL, connection.getValueFactory().createLiteral("a"));
			connection.add(RDFS.CLASS, RDF.TYPE, RDFS.RESOURCE);
			connection.add(RDFS.CLASS, RDFS.LABEL, connection.getValueFactory().createLiteral("yay"));
			connection.add(RDFS.CLASS, RDFS.LABEL, connection.getValueFactory().createLiteral("yay2"));
			connection.commit();


		} finally {
			shaclRepository.shutDown();
		}

	}

	private void add(SailRepositoryConnection connection, String data) throws IOException {
		connection.begin();

		StringReader invalidSampleData = new StringReader(data);

		connection.add(invalidSampleData, "", RDFFormat.TURTLE);
		connection.commit();
	}

}
