package org.eclipse.rdf4j.sail.cassandra.adjacency;

import java.util.List;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.sail.SailException;

/**
 * Abstraction over the Cassandra-backed graph store used by the Sail connection.
 */
public interface CassandraGraphStore {

	void applyMutations(List<Statement> adds, List<Statement> removes) throws SailException;

	CloseableIteration<? extends Statement> queryStatements(Resource subj, IRI pred, Value obj, Resource[] contexts,
			boolean includeInferred) throws SailException;
}
