package org.eclipse.rdf4j.sail.replication.ratis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.sail.SailConnectionListener;

final class StatementDeltaCollector implements SailConnectionListener {
    static final class Snapshot {
        private final List<StatementChange> removals;
        private final List<StatementChange> additions;
        Snapshot(List<StatementChange> removals, List<StatementChange> additions) {
            this.removals = removals;
            this.additions = additions;
        }
        List<StatementChange> removals() { return removals; }
        List<StatementChange> additions() { return additions; }
        boolean isEmpty() { return removals.isEmpty() && additions.isEmpty(); }
    }

    private final Set<StatementChange> removals = new LinkedHashSet<>();
    private final Set<StatementChange> additions = new LinkedHashSet<>();

    @Override
    public synchronized void statementAdded(Statement statement, boolean inferred) {
        StatementChange change = new StatementChange(statement, inferred);
        if (!removals.remove(change)) {
            additions.add(change);
        }
    }

    @Override
    public synchronized void statementRemoved(Statement statement, boolean inferred) {
        StatementChange change = new StatementChange(statement, inferred);
        if (!additions.remove(change)) {
            removals.add(change);
        }
    }

    @Override
    @Deprecated
    public void statementAdded(Statement statement) {
        statementAdded(statement, false);
    }

    @Override
    @Deprecated
    public void statementRemoved(Statement statement) {
        statementRemoved(statement, false);
    }

    synchronized Snapshot snapshot() {
        return new Snapshot(
                Collections.unmodifiableList(new ArrayList<>(removals)),
                Collections.unmodifiableList(new ArrayList<>(additions)));
    }

    synchronized void reset() {
        removals.clear();
        additions.clear();
    }
}
