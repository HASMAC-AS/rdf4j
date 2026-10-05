package org.hasmac.sparql;

import org.eclipse.rdf4j.repository.Repository;

/** Each call must return a fresh, uninitialized repository owned by the caller. */
public interface RepositoryFactory {
    Repository create();
}
