package com.example.rdf4jminus;

import static org.junit.jupiter.api.Assertions.*;

import java.io.StringReader;
import java.util.*;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * SPARQL MINUS scoping & nondeterminism tests (RDF4J 5.1.0).
 *
 * Dataset:
 *
 * @prefix : <http://ex/> . :a :p 1 . :a :q 10 . :a :r 100 . :b :p 2 . :b :q 20 . :b :r 200 . :c :p 3 . :c :r 300 . :d
 *         :q 40 . :d :r 400 . :e :p 5 . :e :q 50 .
 *
 *         Notes about expectations vs. spec: - Disjoint-variable MINUS has no effect (spec). This applies to T1, T4,
 *         T5, T8, T9, and the 2nd MINUS in T11. - When a right-hand BIND overwrites a shared var (T2, T7),
 *         compatibility is checked on the final right-hand bindings, so the MINUS should not remove left rows if the
 *         overwritten value disagrees. - T6 uses RAND(); we check statistical behavior across multiple runs.
 */
public class SparqlMinusScopingTests {

	private static final String NS = "http://ex/";
	private static final String PREFIX = "PREFIX : <http://ex/>\n";

	private static SailRepository REPO;

	private static final String TTL = String.join("\n",
			"@prefix : <http://ex/> .",
			":a :p 1 .   :a :q 10 .  :a :r 100 .",
			":b :p 2 .   :b :q 20 .  :b :r 200 .",
			":c :p 3 .                 :c :r 300 .",
			":d :q 40 .  :d :r 400 .",
			":e :p 5 .   :e :q 50 ."
	);

	@BeforeAll
	static void setup() throws Exception {
		REPO = new SailRepository(new MemoryStore());
		REPO.init();
		try (RepositoryConnection conn = REPO.getConnection()) {
			conn.add(new StringReader(TTL), "", RDFFormat.TURTLE);
		}
	}

	@AfterAll
	static void teardown() {
		if (REPO != null) {
			REPO.shutDown();
		}
	}

	// ---------- Helpers

	private static List<BindingSet> select(String body) {
		String sparql = PREFIX + body;
		try (RepositoryConnection conn = REPO.getConnection()) {
			TupleQuery q = conn.prepareTupleQuery(sparql);
			try (TupleQueryResult r = q.evaluate()) {
				return QueryResults.asList(r);
			}
		}
	}

	private static Set<String> names(List<BindingSet> rows, String var) {
		return rows.stream()
				.map(bs -> bs.getValue(var))
				.filter(Objects::nonNull)
				.map(SparqlMinusScopingTests::name)
				.collect(Collectors.toCollection(LinkedHashSet::new));
	}

	private static String name(Value v) {
		if (v instanceof IRI) {
			IRI iri = (IRI) v;
			return iri.getLocalName(); // ex:a -> "a"
		}
		return v.stringValue();
	}

	private static Set<String> setOf(String... items) {
		return new LinkedHashSet<>(Arrays.asList(items));
	}

	// ---------- T1

	@Test
	void T1_bindCreatesFreshVarInRight_NoOverlap_NoEffect() {
		List<BindingSet> rows = select(
				"SELECT ?s ?pVal WHERE {\n" +
						"  ?s :p ?pVal .\n" +
						"  MINUS { ?x :q ?qVal . BIND(?qVal*2 AS ?fresh) }\n" +
						"}"
		);
		assertEquals(4, rows.size());
		assertEquals(setOf("a", "b", "c", "e"), names(rows, "s"));
	}

	// ---------- T2

	@Test
	void T2_overwriteSharedNameOnRight_FinalBindingsUsed_ThusNoRemoval() {
		List<BindingSet> rows = select(
				"SELECT ?s ?qVal WHERE {\n" +
						"  ?s :q ?qVal .\n" +
						"  MINUS { ?t :q ?qVal . BIND(?qVal+1 AS ?qVal) }\n" +
						"}"
		);
		// Spec-conformant: right overwrites ?qVal to (+1), so no final-compatible mapping remains.
		assertEquals(4, rows.size());
		assertEquals(setOf("a", "b", "d", "e"), names(rows, "s"));
	}

	// ---------- T3

	@Test
	void T3_bindBeforeUseIntroducesOverlap_EverythingRemoved() {
		List<BindingSet> rows = select(
				"SELECT ?s ?qVal WHERE {\n" +
						"  ?s :q ?qVal .\n" +
						"  MINUS { ?t :q ?x . BIND(?x AS ?qVal) }\n" +
						"}"
		);
		assertTrue(rows.isEmpty(), "All ?qVal values appear on the right after BIND, so MINUS removes all left rows");
	}

	// ---------- T4

	@Test
	void T4_renamedVarsInsideRight_NoTrueOverlap_NoEffect() {
		List<BindingSet> rows = select(
				"SELECT ?s ?pVal WHERE {\n" +
						"  ?s :p ?pVal .\n" +
						"  MINUS { ?s2 :p ?pVal2 . BIND(?pVal2 AS ?pVal_tmp) }\n" +
						"}"
		);
		assertEquals(4, rows.size());
		assertEquals(setOf("a", "b", "c", "e"), names(rows, "s"));
	}

	// ---------- T5

	@Test
	void T5_randInsideDisjointRight_MinusHasNoEffect() {
		List<BindingSet> rows = select(
				"SELECT ?s WHERE {\n" +
						"  ?s :p ?v .\n" +
						"  MINUS { ?x :q ?w . FILTER(RAND() < 2) }\n" + // disjoint vars -> MINUS no effect by spec
						"}"
		);
		assertEquals(4, rows.size(), "Disjoint-variable MINUS must not remove any rows, regardless of RAND()");
		assertEquals(setOf("a", "b", "c", "e"), names(rows, "s"));
	}

	// ---------- T6

	@Test
	void T6_randPerSolution_RemovesAboutHalfOnAverage() {
		final int runs = 200; // statistical check
		int totalKept = 0;

		for (int i = 0; i < runs; i++) {
			List<BindingSet> rows = select(
					"SELECT ?s WHERE {\n" +
							"  ?s :q ?q .\n" +
							"  MINUS { ?s :q ?q . FILTER(RAND() < 0.5) }\n" +
							"}"
			);
			totalKept += rows.size();
		}

		// Left side has 4 ?s with :q (a,b,d,e). Expect ~2 kept on average.
		double meanKept = (double) totalKept / runs;
		double fraction = meanKept / 4.0;

		assertTrue(fraction >= 0.35 && fraction <= 0.65,
				"Expected mean kept fraction ~0.5, got " + fraction + " (mean kept=" + meanKept + ")");
	}

	// ---------- T7

	@Test
	void T7_overwriteSharedVarAfterOverlapIsEstablished_FinalBindingsControl_NoRemoval() {
		List<BindingSet> rows = select(
				"SELECT ?s ?r WHERE {\n" +
						"  ?s :r ?r .\n" +
						"  MINUS {\n" +
						"    ?s :r ?r .\n" +
						"    BIND(-1 AS ?r)\n" + // overwrites ?r; final right-hand ?r != left ?r -> incompatible
						"  }\n" +
						"}"
		);
		assertEquals(4, rows.size());
		assertEquals(setOf("a", "b", "c", "d"), names(rows, "s"));
	}

	// ---------- T8

	@Test
	void T8_projectionExprOnLeftDoesNotAffectMinusOverlap_NoEffect() {
		List<BindingSet> rows = select(
				"SELECT ?s ?pVal (STR(?s) AS ?z) WHERE {\n" +
						"  ?s :p ?pVal .\n" +
						"  MINUS { ?x :q ?q . BIND(STR(?x) AS ?z) }\n" + // ?z only exists on the right (left ?z is
																			// projection-time)
						"}"
		);
		assertEquals(4, rows.size());
		assertEquals(setOf("a", "b", "c", "e"), names(rows, "s"));
	}

	// ---------- T9

	@Test
	void T9_projectionBeforeMinus_NoSharedVarsAfterSubselect_NoEffect() {
		List<BindingSet> rows = select(
				"SELECT ?s WHERE {\n" +
						"  { SELECT ?s WHERE { ?s :p ?v } }\n" +
						"  MINUS { ?x :p ?v }\n" + // ?v not projected to the outer level; disjoint wrt left (?s)
						"}"
		);
		assertEquals(4, rows.size());
		assertEquals(setOf("a", "b", "c", "e"), names(rows, "s"));
	}

	// ---------- T10

	@Test
	void T10_minusVsNotExists_WithThisDataTheyCoincide() {
		List<BindingSet> minusRows = select(
				"SELECT ?s WHERE {\n" +
						"  ?s :p ?v .\n" +
						"  MINUS { ?s :q ?w }\n" +
						"}"
		);
		assertEquals(setOf("c"), names(minusRows, "s"));

		List<BindingSet> notExistsRows = select(
				"SELECT ?s WHERE {\n" +
						"  ?s :p ?v .\n" +
						"  FILTER NOT EXISTS { ?s :q ?w }\n" +
						"}"
		);
		assertEquals(setOf("c"), names(notExistsRows, "s"));
	}

	// ---------- T11

	@Test
	void T11_multipleMinus_sharedThenIndependent_onlyFirstMatters() {
		List<BindingSet> rows = select(
				"SELECT ?s WHERE {\n" +
						"  ?s :p ?v .\n" +
						"  MINUS { ?s :q ?w }   # removes a, b, e\n" +
						"  MINUS { ?x :r ?r }   # no shared vars -> no further effect\n" +
						"}"
		);
		assertEquals(setOf("c"), names(rows, "s"));
	}

	// ---------- T12

	@Test
	void T12_minusInsideOptional_affectsOnlyOptionalGroup() {
		List<BindingSet> rows = select(
				"SELECT ?s ?maybe WHERE {\n" +
						"  ?s :p ?v .\n" +
						"  OPTIONAL {\n" +
						"    BIND(1 AS ?maybe)\n" +
						"    MINUS { ?s :q ?w }\n" +
						"  }\n" +
						"}"
		);

		// Build subject -> hasMaybe mapping
		Map<String, Boolean> hasMaybe = new LinkedHashMap<>();
		for (BindingSet bs : rows) {
			String s = name(bs.getValue("s"));
			boolean bound = bs.hasBinding("maybe");
			hasMaybe.put(s, bound);
		}

		// With the dataset, only :c lacks :q, so OPTIONAL survives only for c.
		assertEquals(4, rows.size());
		assertEquals(Boolean.FALSE, hasMaybe.get("a"));
		assertEquals(Boolean.FALSE, hasMaybe.get("b"));
		assertEquals(Boolean.TRUE, hasMaybe.get("c"));
		assertEquals(Boolean.FALSE, hasMaybe.get("e"));
	}
}
