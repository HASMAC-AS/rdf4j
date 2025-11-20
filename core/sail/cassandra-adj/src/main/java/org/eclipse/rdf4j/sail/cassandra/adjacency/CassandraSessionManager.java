package org.eclipse.rdf4j.sail.cassandra.adjacency;

import com.datastax.oss.driver.api.core.CqlSession;

/**
 * Manages Cassandra driver sessions per namespace or cluster configuration.
 */
public interface CassandraSessionManager extends AutoCloseable {

	CqlSession getSession(GraphNamespaceId namespaceId);

	@Override
	void close();
}
