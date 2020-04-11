package org.eclipse.rdf4j.sail.memory;

import org.apache.commons.io.IOUtils;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.repository.sail.SailRepositoryConnection;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class TempTest {

	private static String query1;
	private static String query2;
	private static String query3;
	private static String query4;
	private static Model parse;

	static {
		try {
//			query1 = IOUtils.toString(getResourceAsStream("tempTest/query1.qr"), StandardCharsets.UTF_8);
			query2 = IOUtils.toString(getResourceAsStream("tempTest/query2.qr"), StandardCharsets.UTF_8);
			query3 = IOUtils.toString(getResourceAsStream("tempTest/query3.qr"), StandardCharsets.UTF_8);
			query4 = IOUtils.toString(getResourceAsStream("tempTest/query4.qr"), StandardCharsets.UTF_8);
			parse = Rio.parse(getResourceAsStream("tempTest/bsbm-100.ttl.txt"), "", RDFFormat.TURTLE);

		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	public void tempTest() {

		MemoryStore sail = new MemoryStore();
		SailRepository sailRepository = new SailRepository(sail);

		try (SailRepositoryConnection connection = sailRepository.getConnection()) {
			connection.add(parse);

			try (TupleQueryResult evaluate = connection.prepareTupleQuery(query3).evaluate()) {

				System.out.println();

				long i = 0;
				while (evaluate.hasNext()) {
					i++;
					evaluate.next();
				}

				System.out.println();
			}
		}

	}

	private static InputStream getResourceAsStream(String name) {
		return TempTest.class.getClassLoader().getResourceAsStream(name);
	}

}
