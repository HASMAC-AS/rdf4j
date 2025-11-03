package org.eclipse.rdf4j.sail.lmdb;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.lmdb.config.LmdbStoreConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Integration test for LMDB compaction.
 */
public class LmdbCompactionIT {

	@TempDir
	File tempDir;

	private File dataDir;
	private LmdbStoreConfig config;

	@BeforeEach
	void setUp() {
		dataDir = new File(tempDir, "store");
		config = new LmdbStoreConfig("spoc,posc");
	}

	@Test
	void compactShrinksStoreAndPreservesData() throws Exception {
		List<Statement> retainedStatements = new ArrayList<>();
		createFragmentedStore(retainedStatements);

		long sizeBefore = directorySize(dataDir.toPath());
		Path destinationDir = tempDir.toPath().resolve("compacted");
		Files.createDirectories(destinationDir);
		Path temporaryDir = tempDir.toPath().resolve("temp-swap");
		Files.createDirectories(temporaryDir);

		Class<?> optionsClass;
		try {
			optionsClass = Class.forName("org.eclipse.rdf4j.sail.lmdb.LmdbCompactionOptions");
		} catch (ClassNotFoundException e) {
			fail("LMDB compaction options should be available", e);
			return;
		}

		Object optionsBuilder = invokeStatic(optionsClass, "builder");

		optionsBuilder = invoke(optionsBuilder, "destinationDirectory", destinationDir);
		optionsBuilder = invoke(optionsBuilder, "temporaryDirectory", temporaryDir);
		optionsBuilder = invoke(optionsBuilder, "verifyAfterCopy", true);

		Class<?> progressListenerClass = Class
				.forName("org.eclipse.rdf4j.sail.lmdb.LmdbCompactionProgressListener");
		CopyOnWriteArrayList<Object> progressEvents = new CopyOnWriteArrayList<>();
		Object progressListenerProxy = Proxy.newProxyInstance(progressListenerClass.getClassLoader(),
				new Class[] { progressListenerClass }, new ProgressInvocationHandler(progressEvents));
		optionsBuilder = invoke(optionsBuilder, "progressListener", progressListenerProxy);

		AtomicReference<Object> metricsFromCallback = new AtomicReference<>();
		optionsBuilder = invoke(optionsBuilder, "metricsConsumer", (Consumer<Object>) metricsFromCallback::set);

		Object options = invoke(optionsBuilder, "build");

		LmdbStore store = new LmdbStore(dataDir, config);
		Method compactMethod = LmdbStore.class.getMethod("compact", optionsClass);
		Object report = compactMethod.invoke(store, options);

		Class<?> reportClass = Class.forName("org.eclipse.rdf4j.sail.lmdb.LmdbCompactionReport");
		assertThat(reportClass.isInstance(report)).isTrue();

		Method metricsGetter = reportClass.getMethod("getMetrics");
		Object metrics = metricsGetter.invoke(report);
		assertThat(metrics).isNotNull();

		Class<?> metricsClass = Class.forName("org.eclipse.rdf4j.sail.lmdb.LmdbCompactionMetrics");
		assertThat(metricsClass.isInstance(metrics)).isTrue();

		long sizeAfter = directorySize(dataDir.toPath());
		Method beforeGetter = metricsClass.getMethod("getFileSizeBeforeBytes");
		Method afterGetter = metricsClass.getMethod("getFileSizeAfterBytes");
		long reportedBefore = ((Number) beforeGetter.invoke(metrics)).longValue();
		long reportedAfter = ((Number) afterGetter.invoke(metrics)).longValue();

		assertThat(reportedBefore).isEqualTo(sizeBefore);
		assertThat(reportedAfter).isEqualTo(sizeAfter);
		assertThat(sizeAfter).isLessThan(sizeBefore);

		assertThat(metricsFromCallback.get()).isSameAs(metrics);
		assertThat(progressEvents).isNotEmpty();

		verifyDataPreserved(retainedStatements);
	}

	private void createFragmentedStore(List<Statement> retainedStatements) throws IOException {
		SailRepository repo = new SailRepository(new LmdbStore(dataDir, config));
		repo.init();

		ValueFactory vf = repo.getValueFactory();
		List<Statement> inserted = new ArrayList<>();
		try (RepositoryConnection conn = repo.getConnection()) {
			conn.begin();
			for (int i = 0; i < 2000; i++) {
				IRI subject = vf.createIRI("urn:s:" + (i % 200));
				IRI predicate = vf.createIRI("urn:p:" + (i % 10));
				Statement st = vf.createStatement(subject, predicate, vf.createLiteral("value-" + i));
				conn.add(st);
				inserted.add(st);
			}
			conn.commit();

			conn.begin();
			for (int i = 0; i < inserted.size(); i++) {
				Statement st = inserted.get(i);
				if (i % 3 == 0) {
					conn.remove(st);
				} else {
					retainedStatements.add(st);
				}
			}
			conn.commit();
		}

		repo.shutDown();
	}

	private void verifyDataPreserved(List<Statement> retainedStatements) {
		SailRepository repo = new SailRepository(new LmdbStore(dataDir, config));
		repo.init();
		try (RepositoryConnection conn = repo.getConnection()) {
			Set<Statement> statements = conn.getStatements(null, null, null, false)
					.stream()
					.collect(Collectors.toSet());
			assertThat(statements).containsExactlyInAnyOrderElementsOf(retainedStatements);
		}
		repo.shutDown();
	}

	private static long directorySize(Path root) throws IOException {
		try (var stream = Files.walk(root)) {
			return stream.filter(Files::isRegularFile).mapToLong(path -> {
				try {
					return Files.size(path);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}).sum();
		}
	}

	private static Object invokeStatic(Class<?> type, String method) {
		try {
			return type.getMethod(method).invoke(null);
		} catch (ReflectiveOperationException e) {
			throw new AssertionError("Failed to invoke static method " + method + " on " + type, e);
		}
	}

	private static Object invoke(Object target, String method, Object... args) {
		try {
			Class<?>[] parameterTypes = new Class<?>[args.length];
			for (int i = 0; i < args.length; i++) {
				Object arg = args[i];
				parameterTypes[i] = arg == null ? Object.class : arg.getClass();
			}
			Method m = findMethod(target.getClass(), method, parameterTypes);
			if (m == null) {
				throw new AssertionError("Method " + method + " not found on " + target.getClass());
			}
			return m.invoke(target, args);
		} catch (ReflectiveOperationException e) {
			throw new AssertionError("Failed to invoke method " + method + " on " + target.getClass(), e);
		}
	}

	private static Method findMethod(Class<?> clazz, String name, Class<?>[] parameterTypes) {
		for (Method method : clazz.getMethods()) {
			if (!method.getName().equals(name)) {
				continue;
			}
			if (method.getParameterCount() != parameterTypes.length) {
				continue;
			}
			return method;
		}
		return null;
	}

	private static final class ProgressInvocationHandler implements InvocationHandler {
		private final CopyOnWriteArrayList<Object> events;

		ProgressInvocationHandler(CopyOnWriteArrayList<Object> events) {
			this.events = events;
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) {
			if (args != null && args.length == 1) {
				events.add(args[0]);
			}
			return null;
		}
	}
}
