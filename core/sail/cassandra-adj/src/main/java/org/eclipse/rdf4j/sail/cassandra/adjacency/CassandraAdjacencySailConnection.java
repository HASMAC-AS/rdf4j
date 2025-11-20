package org.eclipse.rdf4j.sail.cassandra.adjacency;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.CloseableIteratorIteration;
import org.eclipse.rdf4j.common.iteration.EmptyIteration;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleNamespace;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.Dataset;
import org.eclipse.rdf4j.query.algebra.TupleExpr;
import org.eclipse.rdf4j.sail.SailException;
import org.eclipse.rdf4j.sail.helpers.AbstractNotifyingSailConnection;

/**
 * Connection implementation that buffers mutations and delegates to a CassandraGraphStore.
 */
public class CassandraAdjacencySailConnection extends AbstractNotifyingSailConnection {

	private final CassandraGraphStore graphStore;
	private final NamespaceManager namespaceManager;
	private final Map<String, String> namespaces = new ConcurrentHashMap<>();
	private final List<Statement> pendingAdd = new ArrayList<>();
	private final List<Statement> pendingRemove = new ArrayList<>();

	public CassandraAdjacencySailConnection(CassandraAdjacencySail sail, CassandraGraphStore graphStore,
			NamespaceManager namespaceManager) {
		super(sail);
		this.graphStore = Objects.requireNonNull(graphStore, "graphStore");
		this.namespaceManager = Objects.requireNonNull(namespaceManager, "namespaceManager");
	}

	@Override
	protected void startTransactionInternal() throws SailException {
		pendingAdd.clear();
		pendingRemove.clear();
	}

	@Override
	protected void commitInternal() throws SailException {
		graphStore.applyMutations(new ArrayList<>(pendingAdd), new ArrayList<>(pendingRemove));

		if (hasConnectionListeners()) {
			pendingAdd.forEach(st -> notifyStatementAdded(st, false));
			pendingRemove.forEach(st -> notifyStatementRemoved(st, false));
		}

		pendingAdd.clear();
		pendingRemove.clear();
	}

	@Override
	protected void rollbackInternal() throws SailException {
		pendingAdd.clear();
		pendingRemove.clear();
	}

	@Override
	protected void closeInternal() throws SailException {
		pendingAdd.clear();
		pendingRemove.clear();
	}

	@Override
	protected CloseableIteration<? extends BindingSet> evaluateInternal(TupleExpr tupleExpr, Dataset dataset,
			BindingSet bindings, boolean includeInferred) throws SailException {
		return new EmptyIteration<>();
	}

	@Override
	protected CloseableIteration<? extends Resource> getContextIDsInternal() throws SailException {
		Set<Resource> contexts = new HashSet<>();
		try (CloseableIteration<? extends Statement> iter = graphStore.queryStatements(null, null, null,
				new Resource[0],
				false)) {
			while (iter.hasNext()) {
				Statement st = iter.next();
				if (st.getContext() != null) {
					contexts.add(st.getContext());
				}
			}
		}
		return new CloseableIteratorIteration<>(contexts.iterator());
	}

	@Override
	protected CloseableIteration<? extends Statement> getStatementsInternal(Resource subj, IRI pred, Value obj,
			boolean includeInferred, Resource... contexts) throws SailException {
		Set<Statement> results = new HashSet<>();

		try (CloseableIteration<? extends Statement> iter = graphStore.queryStatements(subj, pred, obj, contexts,
				includeInferred)) {
			while (iter.hasNext()) {
				results.add(iter.next());
			}
		}

		for (Statement st : pendingAdd) {
			if (matchesFilters(st, subj, pred, obj, contexts)) {
				results.add(st);
			}
		}

		for (Statement st : pendingRemove) {
			if (matchesFilters(st, subj, pred, obj, contexts)) {
				results.remove(st);
			}
		}

		return new CloseableIteratorIteration<>(results.iterator());
	}

	@Override
	protected long sizeInternal(Resource... contexts) throws SailException {
		long count = 0;
		try (CloseableIteration<? extends Statement> iter = getStatementsInternal(null, null, null, false, contexts)) {
			while (iter.hasNext()) {
				iter.next();
				count++;
			}
		}
		return count;
	}

	@Override
	protected void addStatementInternal(Resource subj, IRI pred, Value obj, Resource... contexts) throws SailException {
		List<Statement> statementsToAdd = createStatements(subj, pred, obj, contexts);
		pendingAdd.addAll(statementsToAdd);
		pendingRemove.removeAll(statementsToAdd);
	}

	@Override
	protected void removeStatementsInternal(Resource subj, IRI pred, Value obj, Resource... contexts)
			throws SailException {
		Set<Statement> statements = new HashSet<>();

		try (CloseableIteration<? extends Statement> iter = graphStore.queryStatements(subj, pred, obj, contexts,
				false)) {
			while (iter.hasNext()) {
				statements.add(iter.next());
			}
		}

		statements.addAll(pendingAdd.stream()
				.filter(st -> matchesFilters(st, subj, pred, obj, contexts))
				.collect(Collectors.toSet()));

		pendingAdd.removeIf(statements::contains);
		pendingRemove.addAll(statements);
	}

	@Override
	protected void clearInternal(Resource... contexts) throws SailException {
		removeStatementsInternal(null, null, null, contexts);
	}

	@Override
	protected CloseableIteration<? extends Namespace> getNamespacesInternal() {
		List<Namespace> list = namespaces.entrySet()
				.stream()
				.map(e -> new SimpleNamespace(e.getKey(), e.getValue()))
				.collect(Collectors.toList());
		return new CloseableIteratorIteration<>(list.iterator());
	}

	@Override
	protected String getNamespaceInternal(String prefix) throws SailException {
		return namespaces.get(prefix);
	}

	@Override
	protected void setNamespaceInternal(String prefix, String name) throws SailException {
		namespaces.put(prefix, name);
	}

	@Override
	protected void removeNamespaceInternal(String prefix) throws SailException {
		namespaces.remove(prefix);
	}

	@Override
	protected void clearNamespacesInternal() throws SailException {
		namespaces.clear();
	}

	private List<Statement> createStatements(Resource subj, IRI pred, Value obj, Resource... contexts) {
		List<Statement> statements = new ArrayList<>();
		if (contexts == null || contexts.length == 0) {
			statements.add(SimpleValueFactory.getInstance().createStatement(subj, pred, obj));
		} else {
			for (Resource ctx : contexts) {
				if (ctx == null) {
					statements.add(SimpleValueFactory.getInstance().createStatement(subj, pred, obj));
				} else {
					statements.add(SimpleValueFactory.getInstance().createStatement(subj, pred, obj, ctx));
				}
			}
		}
		return statements;
	}

	private boolean matchesFilters(Statement st, Resource subj, IRI pred, Value obj, Resource[] contexts) {
		if (subj != null && !subj.equals(st.getSubject())) {
			return false;
		}
		if (pred != null && !pred.equals(st.getPredicate())) {
			return false;
		}
		if (obj != null && !obj.equals(st.getObject())) {
			return false;
		}
		if (contexts == null || contexts.length == 0) {
			return true;
		}
		Resource ctx = st.getContext();
		for (Resource filter : contexts) {
			if (filter == null && ctx == null) {
				return true;
			}
			if (filter != null && filter.equals(ctx)) {
				return true;
			}
		}
		return false;
	}
}
