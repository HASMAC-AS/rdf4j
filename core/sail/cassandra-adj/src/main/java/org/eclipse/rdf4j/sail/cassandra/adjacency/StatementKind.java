package org.eclipse.rdf4j.sail.cassandra.adjacency;

/**
 * Statement classification outcome used to route data to Cassandra namespaces.
 */
public enum StatementKind {
	NODE_TYPE,
	NODE_PROPERTY,
	EDGE
}
