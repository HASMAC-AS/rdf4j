package org.eclipse.rdf4j.sail.cassandra.adjacency;

import java.util.Objects;

import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.sail.NotifyingSailConnection;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.helpers.AbstractNotifyingSail;

/**
 * Minimal NotifyingSail implementation that delegates storage to a CassandraGraphStore.
 */
public class CassandraAdjacencySail extends AbstractNotifyingSail {

	private final CassandraGraphStore graphStore;
	private final NamespaceManager namespaceManager;
	private final ValueFactory valueFactory = SimpleValueFactory.getInstance();

	public CassandraAdjacencySail(CassandraGraphStore graphStore, NamespaceManager namespaceManager) {
		this.graphStore = Objects.requireNonNull(graphStore, "graphStore");
		this.namespaceManager = Objects.requireNonNull(namespaceManager, "namespaceManager");
	}

	public void initialize() throws SailException {
		init();
	}

	@Override
	protected NotifyingSailConnection getConnectionInternal() throws SailException {
		return new CassandraAdjacencySailConnection(this, graphStore, namespaceManager);
	}

	@Override
	protected void shutDownInternal() throws SailException {
		// nothing to do for the in-memory scaffolding
	}

	@Override
	public ValueFactory getValueFactory() {
		return valueFactory;
	}

	@Override
	public boolean isWritable() throws SailException {
		return true;
	}
}
