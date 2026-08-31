package org.eclipse.rdf4j.sail.cassandra.adjacency;

import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.common.iteration.CloseableIteratorIteration;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.testcontainers.containers.CassandraContainer;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;

/**
 * Minimal Cassandra-backed graph store used for integration testing with a live Cassandra instance.
 */
class CassandraTestGraphStore implements CassandraGraphStore, AutoCloseable {

	private static final String TABLE = "triples";

	private final CqlSession session;
	private final String keyspace;
	private final ValueFactory valueFactory;

	private final PreparedStatement insertStatement;
	private final PreparedStatement deleteStatement;

	private CassandraTestGraphStore(CqlSession session, String keyspace, ValueFactory valueFactory) {
		this.session = session;
		this.keyspace = keyspace;
		this.valueFactory = valueFactory;

		this.insertStatement = session.prepare(
				"INSERT INTO " + keyspace + "." + TABLE
						+ " (subject, predicate, object, object_type, datatype, lang, context)"
						+ " VALUES (?, ?, ?, ?, ?, ?, ?)");
		this.deleteStatement = session.prepare(
				"DELETE FROM " + keyspace + "." + TABLE
						+ " WHERE subject = ? AND predicate = ? AND object = ? AND context = ?");
	}

	static CassandraTestGraphStore create(CassandraContainer<?> container, String keyspace, ValueFactory valueFactory) {
		Objects.requireNonNull(container, "container");
		Objects.requireNonNull(keyspace, "keyspace");

		InetSocketAddress contactPoint = new InetSocketAddress(container.getHost(), container.getMappedPort(9042));

		CqlSession session = CqlSession.builder()
				.addContactPoint(contactPoint)
				.withLocalDatacenter(container.getLocalDatacenter())
				.build();

		session.execute("CREATE KEYSPACE IF NOT EXISTS " + keyspace
				+ " WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1}");
		session.execute("CREATE TABLE IF NOT EXISTS " + keyspace + "." + TABLE + " ("
				+ "subject text, "
				+ "predicate text, "
				+ "object text, "
				+ "object_type text, "
				+ "datatype text, "
				+ "lang text, "
				+ "context text, "
				+ "PRIMARY KEY ((subject), predicate, object, context))");

		return new CassandraTestGraphStore(session, keyspace,
				valueFactory == null ? SimpleValueFactory.getInstance() : valueFactory);
	}

	long fetchTripleCount() {
		ResultSet rs = session.execute("SELECT count(*) FROM " + keyspace + "." + TABLE);
		Row row = rs.one();
		return row == null ? 0L : row.getLong(0);
	}

	@Override
	public void applyMutations(List<Statement> adds, List<Statement> removes) {
		Objects.requireNonNull(adds, "adds");
		Objects.requireNonNull(removes, "removes");

		for (Statement st : removes) {
			EncodedTriple encoded = encodeStatement(st);
			session.execute(deleteStatement.bind(encoded.subject, encoded.predicate, encoded.object,
					encoded.context));
		}

		for (Statement st : adds) {
			EncodedTriple encoded = encodeStatement(st);
			session.execute(insertStatement.bind(encoded.subject, encoded.predicate, encoded.object,
					encoded.objectType, encoded.datatype, encoded.lang, encoded.context));
		}
	}

	@Override
	public CloseableIteration<? extends Statement> queryStatements(Resource subj, IRI pred, Value obj,
			Resource[] contexts, boolean includeInferred) {
		Set<Statement> statements = new HashSet<>();

		SimpleStatement statement;
		if (subj != null) {
			statement = SimpleStatement.builder(
					"SELECT subject, predicate, object, object_type, datatype, lang, context FROM " + keyspace
							+ "." + TABLE + " WHERE subject = ?")
					.addPositionalValue(ValueEncoding.encodeResource(subj))
					.build();
		} else {
			statement = SimpleStatement.builder(
					"SELECT subject, predicate, object, object_type, datatype, lang, context FROM " + keyspace
							+ "." + TABLE)
					.build();
		}

		ResultSet rs = session.execute(statement);
		for (Row row : rs) {
			Statement decoded = decodeRow(row);
			if (matches(decoded, subj, pred, obj, contexts)) {
				statements.add(decoded);
			}
		}

		return new CloseableIteratorIteration<>(statements.iterator());
	}

	@Override
	public void close() {
		session.close();
	}

	private Statement decodeRow(Row row) {
		Resource subject = ValueEncoding.decodeResource(row.getString("subject"), valueFactory);
		IRI predicate = valueFactory.createIRI(row.getString("predicate"));
		String objectType = row.getString("object_type");
		Value object;
		if ("LITERAL".equals(objectType)) {
			ValueEncoding.LiteralFields fields = new ValueEncoding.LiteralFields(row.getString("object"),
					row.getString("datatype"),
					row.isNull("lang") ? null : java.util.Optional.ofNullable(row.getString("lang")));
			object = ValueEncoding.decodeLiteral(fields, valueFactory);
		} else {
			object = ValueEncoding.decodeResource(row.getString("object"), valueFactory);
		}
		Resource context = ValueEncoding.decodeContext(row.getString("context"), valueFactory);
		return valueFactory.createStatement(subject, predicate, object, context);
	}

	private boolean matches(Statement st, Resource subj, IRI pred, Value obj, Resource[] contexts) {
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

	private EncodedTriple encodeStatement(Statement st) {
		String subject = ValueEncoding.encodeResource(st.getSubject());
		String predicate = st.getPredicate().stringValue();
		String context = ValueEncoding.encodeContext(st.getContext());

		Value object = st.getObject();
		if (object instanceof Literal) {
			ValueEncoding.LiteralFields fields = ValueEncoding.encodeLiteral((Literal) object);
			return new EncodedTriple(subject, predicate, fields.lexical(), "LITERAL", fields.datatype(),
					fields.language().orElse(null), context);
		}

		return new EncodedTriple(subject, predicate, ValueEncoding.encodeResource((Resource) object), "RESOURCE", null,
				null, context);
	}

	private static final class EncodedTriple {
		private final String subject;
		private final String predicate;
		private final String object;
		private final String objectType;
		private final String datatype;
		private final String lang;
		private final String context;

		EncodedTriple(String subject, String predicate, String object, String objectType, String datatype, String lang,
				String context) {
			this.subject = subject;
			this.predicate = predicate;
			this.object = object;
			this.objectType = objectType;
			this.datatype = datatype;
			this.lang = lang;
			this.context = context;
		}
	}
}
