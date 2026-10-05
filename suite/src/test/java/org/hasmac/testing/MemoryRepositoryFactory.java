package org.hasmac.testing;

import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.memory.MemoryStore;

public final class MemoryRepositoryFactory implements RepositoryFactory {
    @Override public Repository createRepository() { return new SailRepository(new MemoryStore()); }
}
