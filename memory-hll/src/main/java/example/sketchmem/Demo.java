/*****************************************************************************
 * Copyright (c) 2024 Eclipse RDF4J contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *****************************************************************************/
package example.sketchmem;

import java.io.StringReader;

import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;

/** Simple demo loading a tiny dataset and executing a query. */
public class Demo {
	public static void main(String[] args) throws Exception {
		SketchRegistry reg = new SketchRegistry();
		Repository repo = new SailRepository(new SketchAwareMemoryStore(reg));
		repo.init();

		String data = "@prefix foaf: <http://xmlns.com/foaf/0.1/> .\n"
				+ "_:a foaf:name \"Alice\" .\n"
				+ "_:b foaf:name \"Bob\" .\n"
				+ "_:c foaf:name \"Charlie\" .\n";

		try (var conn = repo.getConnection()) {
			conn.add(new StringReader(data), "", RDFFormat.TURTLE);
			TupleQuery tq = conn.prepareTupleQuery("SELECT ?s WHERE { ?s foaf:name ?n } ");
			try (TupleQueryResult res = tq.evaluate()) {
				while (res.hasNext()) {
					System.out.println(res.next());
				}
			}
		}
		repo.shutDown();
	}
}
