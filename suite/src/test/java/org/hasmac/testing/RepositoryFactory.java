package org.hasmac.testing;

import org.eclipse.rdf4j.repository.Repository;

/** Supply a new, empty repository. The harness initializes and closes it. */
public interface RepositoryFactory {
    Repository createRepository();
}
