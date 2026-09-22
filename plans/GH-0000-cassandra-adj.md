# Cassandra adjacency-list Sail for RDF4J

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` must be kept up to date as work proceeds. It follows the repository guidance in PLANS.md.

## Purpose / Big Picture

Deliver a new RDF4J Sail backed by Apache Cassandra using an adjacency-list layout. After completion, a developer can configure a `CassandraAdjacencySail`, load RDF statements into Cassandra, and query them via RDF4J APIs. They will see data persisted in Cassandra tables and retrievable through RDF4J `getStatements` and SPARQL queries using the new module.

## Progress

- [x] (2025-02-06 00:00Z) Drafted initial ExecPlan outlining module creation, data model, and testing strategy.
- [x] (2025-02-07 00:30Z) Implemented module scaffolding (`core/sail/cassandra-adj` POM) and added initial failing tests for utilities and Sail wiring.
- [x] (2025-02-07 02:00Z) Implemented ValueEncoding utilities, StatementClassifier, and namespace helpers to satisfy unit expectations.
- [x] (2025-02-07 02:15Z) Added Cassandra graph/session interfaces and minimal CassandraAdjacencySail plus connection buffering logic to make Sail tests runnable with in-memory store.
- [ ] Finalize documentation, schema DDL, and example configuration files.
- [ ] Run formatting/linting and commit changes (formatting and module tests now pass; commit pending).

## Surprises & Discoveries

- Observation: Pending.
  Evidence: Pending.

## Decision Log

- Decision: Use mockable interfaces for Cassandra access to allow unit tests without real Cassandra. Rationale: Keeps tests lightweight and runnable in CI. Date/Author: 2025-02-06 / AI Agent.

## Outcomes & Retrospective

Pending completion. Will summarize functioning Sail, test coverage, and remaining gaps.

## Context and Orientation

The repository root hosts multiple modules; we will add a new Maven module `rdf4j-sail-cassandra-adj` under `storage/` or a sibling location aligned with other Sail implementations. Key existing patterns include `storage/sail/elasticsearch-store` and `storage/sail/lmdb` for reference. Tests live under `testsuites/` and module-specific `src/test/java` directories. PLANS.md governs this ExecPlan.

## Plan of Work

1. Create a new Maven module `storage/cassandra-adj` (artifact `rdf4j-sail-cassandra-adj`) with dependencies on RDF4J Sail APIs and the DataStax Java driver. Provide module POM and parent references in root `pom.xml` and relevant aggregator POMs.
2. Introduce configuration classes (`CassandraSailConfig`, `CassandraSailFactory`) supporting contact points, keyspace, and namespace mapping file paths.
3. Implement utility classes:
   - `ValueEncoding` to map RDF4J `Value` objects to stable string identifiers and literals to serialized forms.
   - `StatementClassifier` plus supporting `GraphNamespaceId` and `NamespaceManager` abstractions for namespace resolution.
4. Create storage abstractions:
   - `CassandraSessionManager` interface with a default implementation stub that can be mocked in tests.
   - `CassandraGraphStore` interface and a default implementation skeleton that will later integrate with Cassandra tables (initially mocked or in-memory to satisfy tests).
5. Implement Sail layer:
   - `CassandraAdjacencySail` extending `NotifyingSailBase` with configuration loading and connection provisioning.
   - `CassandraAdjacencySailConnection` extending `NotifyingSailConnectionBase` with buffered mutations and delegation to `CassandraGraphStore` for reads/writes.
6. Add schema DDL and example configuration resources under the module (e.g., `src/main/resources/cassandra-adj/schema.cql`, `namespace-example.yaml`).
7. Testing (TDD):
   - Start with unit tests for `ValueEncoding` and `StatementClassifier` validating round-trips and classification rules.
   - Add Sail-level tests using an in-memory `CassandraGraphStore` mock to ensure add/query/remove flows produce expected statements.
   - Ensure tests fail before implementation, then implement to make them pass.
8. Documentation: add README in the module describing configuration, schema creation, and limitations.
9. Validation: run module tests via Maven (`mvn -pl storage/cassandra-adj -DskipITs=false test`) and ensure lint/format where applicable.

## Concrete Steps

- From repository root, create module directories and POM scaffolding. Update parent POMs to include the new module.
- Add placeholder interfaces and stub implementations; compile to ensure module structure is sound.
- Write unit tests that initially fail due to missing logic (ValueEncoding and StatementClassifier behaviors, Sail add/query paths using a fake store).
- Implement utilities and Sail classes incrementally until tests pass.
- Add schema DDL and example configuration resources.
- Run `mvn -pl storage/cassandra-adj test` and address any failures.

## Validation and Acceptance

Acceptance will be demonstrated by running the new module tests. Before implementation, tests should fail because classes are unimplemented. After coding, the same test command (`mvn -pl storage/cassandra-adj test`) should pass, showing that the Sail can encode values, classify statements, and perform basic add/query operations through the mocked store.

## Idempotence and Recovery

All steps are additive. Re-running Maven tests is safe. If module wiring fails, fix POM references and rerun tests. No destructive database operations are executed in tests because Cassandra interactions are mocked.

## Artifacts and Notes

Expect new files under `core/sail/cassandra-adj/src/main/java/` and `src/test/java/`, plus resources for schema and configuration examples. POM updates integrate the module into the build.

## Interfaces and Dependencies

Define interfaces in `core/sail/cassandra-adj/src/main/java/org/eclipse/rdf4j/sail/cassandra/adjacency/`:

    public interface CassandraGraphStore {
        void applyMutations(List<Statement> adds, List<Statement> removes) throws SailException;
        CloseableIteration<? extends Statement> queryStatements(Resource subj, IRI pred, Value obj, Resource[] contexts, boolean includeInferred) throws SailException;
    }

    public interface CassandraSessionManager extends AutoCloseable {
        CqlSession getSession(GraphNamespaceId namespace);
        @Override void close();
    }

Utility classes:

    public final class ValueEncoding { ... }
    public final class StatementClassifier { ... }
    public final class NamespaceManager { ... }

Sail classes:

    public class CassandraAdjacencySail extends NotifyingSailBase { ... }
    public class CassandraAdjacencySailConnection extends NotifyingSailConnectionBase { ... }

These must compile and satisfy the unit tests without requiring a live Cassandra instance.
