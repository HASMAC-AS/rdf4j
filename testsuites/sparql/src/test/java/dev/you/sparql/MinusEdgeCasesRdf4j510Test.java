package dev.you.sparql;

import static org.junit.jupiter.api.Assertions.*;

import java.io.StringReader;
import java.util.*;

import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.*;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.jupiter.api.*;

/**
 * A battery of tests designed to catch wrong MINUS/correlation/graph-scope rewrites. Verified against RDF4J 5.1.0 APIs.
 * See RDF4J Javadocs for Repository/TupleQuery, etc. https://rdf4j.org/javadoc/5.1.0/ (Repository,
 * RepositoryConnection, TupleQuery).
 */
public class MinusEdgeCasesRdf4j510Test {

	private Repository repo;
	private RepositoryConnection cxn;

	private static final String BASE = "http://ex/";

	@BeforeEach
	void setup() {
		repo = new SailRepository(new MemoryStore());
		repo.init();
		cxn = repo.getConnection();
	}

	@AfterEach
	void teardown() {
		if (cxn != null)
			cxn.close();
		if (repo != null)
			repo.shutDown();
	}

	// ---------- helpers ----------

	private void load(String data, RDFFormat fmt) throws Exception {
		cxn.clear();
		cxn.add(new StringReader(data), BASE, fmt);
	}

	private Set<Map<String, String>> runSelect(String sparql) {
		TupleQuery tq = cxn.prepareTupleQuery(QueryLanguage.SPARQL, sparql);
		try (TupleQueryResult tqr = tq.evaluate()) {
			List<String> names = tqr.getBindingNames();
			Set<Map<String, String>> out = new LinkedHashSet<>();
			while (tqr.hasNext()) {
				BindingSet bs = tqr.next();
				Map<String, String> m = new LinkedHashMap<>();
				for (String n : names) {
					Value v = bs.getValue(n);
					if (v != null)
						m.put(n, v.stringValue());
				}
				out.add(m);
			}
			return out;
		}
	}

	private static Map<String, String> row(Object... kv) {
		if (kv.length % 2 != 0)
			throw new IllegalArgumentException("odd kv length");
		Map<String, String> m = new LinkedHashMap<>();
		for (int i = 0; i < kv.length; i += 2) {
			m.put(kv[i].toString(), kv[i + 1].toString());
		}
		return m;
	}

	private static String ex(String local) {
		return BASE + local;
	}

	// ---------- TESTS ----------

	@Test
	void minus_no_shared_vars_is_noop_select() throws Exception {
		String ttl = "@prefix : <http://ex/> .\n" +
				":a :p 1 . :a :q 1 . :b :p 1 .";
		load(ttl, RDFFormat.TURTLE);

		String q = "PREFIX : <http://ex/>\n" +
				"SELECT ?s WHERE { ?s :p 1 MINUS { ?x :q 1 } }";

		Set<Map<String, String>> got = runSelect(q);
		Set<Map<String, String>> exp = Set.of(
				row("s", ex("a")),
				row("s", ex("b"))
		);
		assertEquals(exp, got, "MINUS with disjoint var-sets must keep the LHS intact (§8.3).");
	}

	@Test
	void not_exists_contrast_to_minus_no_shared_vars() throws Exception {
		String ttl = "@prefix : <http://ex/> .\n" +
				":a :p 1 . :a :q 1 . :b :p 1 .";
		load(ttl, RDFFormat.TURTLE);

		String q = "PREFIX : <http://ex/>\n" +
				"SELECT ?s WHERE { ?s :p 1 FILTER NOT EXISTS { ?x :q 1 } }";

		Set<Map<String, String>> got = runSelect(q);
		assertTrue(got.isEmpty(), "NOT EXISTS is correlated and removes all rows when { ?x :q 1 } exists (§8.3).");
	}

	@Test
	void rhs_filter_referencing_outer_var_is_unbound_and_ignored() throws Exception {
		String ttl = "@prefix : <http://ex/> .\n" +
				":a :p 1 ; :q 1, 2 .\n" +
				":b :p 3 ; :q 4 .";
		load(ttl, RDFFormat.TURTLE);

		String q = "PREFIX : <http://ex/>\n" +
				"SELECT ?x ?n WHERE {\n" +
				"  ?x :p ?n .\n" +
				"  MINUS { ?x :q ?m . FILTER(?m = ?n) }  # ?n is unbound on RHS → filter errors → RHS empty\n" +
				"} ORDER BY ?x";

		Set<Map<String, String>> got = runSelect(q);
		Set<Map<String, String>> exp = Set.of(
				row("x", ex("a"), "n", "1"),
				row("x", ex("b"), "n", "3")
		);
		assertEquals(exp, got, "RHS filter sees no outer vars under MINUS; subtract nothing (§8.3).");
	}

	@Test
	void rhs_bind_of_outer_var_produces_unbound_then_overremoves_on_shared_subset() throws Exception {
		String ttl = "@prefix : <http://ex/> .\n" +
				":a :p 1 ; :q 1, 2 .\n" +
				":b :p 3 ; :q 4 .\n" +
				":c :p 7 .";
		load(ttl, RDFFormat.TURTLE);

		String q = "PREFIX : <http://ex/>\n" +
				"SELECT ?x WHERE {\n" +
				"  ?x :p ?n .\n" +
				"  MINUS { BIND(?n AS ?k) ?x :q ?k }   # ?n unbound in RHS; BIND keeps row with ?k unbound; ?x :q ?k binds any ?k; shared vars = {?x}\n"
				+
				"} ORDER BY ?x";

		Set<Map<String, String>> got = runSelect(q);
		Set<Map<String, String>> exp = Set.of(
				row("x", ex("c")) // only :c has no :q → survives
		);
		assertEquals(exp, got,
				"RHS BIND on unbound outer var must not correlate; shared-vars logic should remove :a,:b only.");
	}

	@Test
	void rhs_bind_creates_intentional_shared_var_equalities() throws Exception {
		String ttl = "@prefix : <http://ex/> .\n" +
				":e :p 10 ; :q 42 .\n" +
				":f :p 20 ; :q 20 .\n" +
				":g :p 30 ; :q 99 .";
		load(ttl, RDFFormat.TURTLE);

		String q = "PREFIX : <http://ex/>\n" +
				"SELECT ?x WHERE {\n" +
				"  ?x :p ?v .\n" +
				"  MINUS { ?x :q ?m . BIND(?m AS ?v) }  # removes only when q==p\n" +
				"} ORDER BY ?x";

		Set<Map<String, String>> got = runSelect(q);
		Set<Map<String, String>> exp = Set.of(row("x", ex("e")), row("x", ex("g")));
		assertEquals(exp, got, "Only :f should be removed (q==p). Early projection must NOT change shared vars.");
	}

	@Test
	void early_projection_should_not_change_minus_semantics() throws Exception {
		// Same data as previous test
		String ttl = "@prefix : <http://ex/> .\n" +
				":e :p 10 ; :q 42 .\n" +
				":f :p 20 ; :q 20 .\n" +
				":g :p 30 ; :q 99 .";
		load(ttl, RDFFormat.TURTLE);

		String q = "PREFIX : <http://ex/>\n" +
				"SELECT ?x WHERE { ?x :p ?v MINUS { ?x :q ?v } } ORDER BY ?x";

		Set<Map<String, String>> got = runSelect(q);
		Set<Map<String, String>> exp = Set.of(row("x", ex("e")), row("x", ex("g")));
		assertEquals(exp, got, "Pushing projection before MINUS would wrongly remove :e and :g; don't do that.");
	}

	@Test
	void subquery_pins_shared_var_against_optimizer() throws Exception {
		String ttl = "@prefix : <http://ex/> .\n" +
				":e :p 10 ; :q 42 .\n" +
				":f :p 20 ; :q 20 .\n" +
				":g :p 30 ; :q 99 .";
		load(ttl, RDFFormat.TURTLE);

		String q = "PREFIX : <http://ex/>\n" +
				"SELECT ?x WHERE {\n" +
				"  { SELECT ?x ?v WHERE { ?x :p ?v } }   # box the LHS\n" +
				"  MINUS { ?x :q ?v }\n" +
				"} ORDER BY ?x";

		Set<Map<String, String>> got = runSelect(q);
		Set<Map<String, String>> exp = Set.of(row("x", ex("e")), row("x", ex("g")));
		assertEquals(exp, got, "Subquery must preserve shared vars until MINUS.");
	}

	@Test
	void optional_inside_minus_only_removes_when_optional_matches() throws Exception {
		String ttl = "@prefix : <http://ex/> .\n" +
				":e :name \"Alice\" ; :formerName \"Alice\" .\n" +
				":f :name \"Carol\" .";
		load(ttl, RDFFormat.TURTLE);

		String q = "PREFIX : <http://ex/>\n" +
				"SELECT ?x WHERE {\n" +
				"  ?x :name ?n .\n" +
				"  MINUS { OPTIONAL { ?x :formerName ?n } }\n" +
				"} ORDER BY ?x";

		Set<Map<String, String>> got = runSelect(q);
		Set<Map<String, String>> exp = Set.of(row("x", ex("f")));
		assertEquals(exp, got, "OPTIONAL inside MINUS: only rows for which the OPTIONAL binds compatibly are removed.");
	}

	@Test
	void not_exists_over_optional_is_always_false_here() throws Exception {
		// Same data as previous test
		String ttl = "@prefix : <http://ex/> .\n" +
				":e :name \"Alice\" ; :formerName \"Alice\" .\n" +
				":f :name \"Carol\" .";
		load(ttl, RDFFormat.TURTLE);

		String q = "PREFIX : <http://ex/>\n" +
				"SELECT ?x WHERE {\n" +
				"  ?x :name ?n .\n" +
				"  FILTER NOT EXISTS { OPTIONAL { ?x :formerName ?n } }\n" +
				"}";

		Set<Map<String, String>> got = runSelect(q);
		assertTrue(got.isEmpty(),
				"Rewriting MINUS{OPTIONAL{…}} to NOT EXISTS { OPTIONAL{…} } is wrong: the inner group always yields at least the empty mapping.");
	}

	@Test
	void graph_isolation_same_g_on_both_sides_no_removal_when_values_differ() throws Exception {
		String trig = "@prefix : <http://ex/> .\n" +
				"GRAPH :g1 { :a :p 1 . }\n" +
				"GRAPH :g2 { :a :q 1 . :a :p 2 . }";
		load(trig, RDFFormat.TRIG);

		String q = "PREFIX : <http://ex/>\n" +
				"SELECT ?g ?x ?n WHERE {\n" +
				"  GRAPH ?g { ?x :p ?n }\n" +
				"  MINUS { GRAPH ?g { ?x :q ?n } }\n" +
				"} ORDER BY ?g ?x ?n";

		Set<Map<String, String>> got = runSelect(q);
		Set<Map<String, String>> exp = Set.of(
				row("g", ex("g1"), "x", ex("a"), "n", "1"),
				row("g", ex("g2"), "x", ex("a"), "n", "2")
		);
		assertEquals(exp, got, "Active graph must be respected on the RHS as well (§13.3).");
	}

	@Test
	void graph_isolation_removes_only_in_graph_where_match_exists() throws Exception {
		String trig = "@prefix : <http://ex/> .\n" +
				"GRAPH :g1 { :a :p 1 . }\n" +
				"GRAPH :g2 { :a :q 1 . :a :p 2 . :a :q 2 . }";
		load(trig, RDFFormat.TRIG);

		String q = "PREFIX : <http://ex/>\n" +
				"SELECT ?g ?x ?n WHERE {\n" +
				"  GRAPH ?g { ?x :p ?n }\n" +
				"  MINUS { GRAPH ?g { ?x :q ?n } }\n" +
				"} ORDER BY ?g";

		Set<Map<String, String>> got = runSelect(q);
		Set<Map<String, String>> exp = Set.of(
				row("g", ex("g1"), "x", ex("a"), "n", "1")
		);
		assertEquals(exp, got, "Only the :g2 row should be removed because :q 2 exists in :g2.");
	}

	@Test
	void minus_disjoint_varsets_is_noop_even_with_union_on_lhs() throws Exception {
		String ttl = "@prefix : <http://ex/> .\n" +
				":a :p 1 ; :q 1 .\n" +
				":b :p 1 .\n" +
				":c :p 2 .";
		load(ttl, RDFFormat.TURTLE);

		String q = "PREFIX : <http://ex/>\n" +
				"SELECT ?x WHERE {\n" +
				"  { ?x :p 1 } UNION { ?x :p 2 }\n" +
				"  MINUS { ?y :q 1 }\n" +
				"} ORDER BY ?x";

		Set<Map<String, String>> got = runSelect(q);
		Set<Map<String, String>> exp = Set.of(row("x", ex("a")), row("x", ex("b")), row("x", ex("c")));
		assertEquals(exp, got, "No shared vars → MINUS must be a no-op (§8.3).");
	}

	@Test
	void values_left_only_no_shared_vars_is_noop() throws Exception {
		String ttl = "@prefix : <http://ex/> . :a :q 1 .";
		load(ttl, RDFFormat.TURTLE);

		String q = "PREFIX : <http://ex/>\n" +
				"SELECT ?a WHERE { VALUES ?a { 1 2 } MINUS { ?x :q 1 } } ORDER BY ?a";

		Set<Map<String, String>> got = runSelect(q);
		Set<Map<String, String>> exp = Set.of(row("a", "1"), row("a", "2"));
		assertEquals(exp, got, "VALUES introduces no shared vars with RHS, so MINUS removes nothing.");
	}

	@Test
	void minus_shared_subset_only_subject_shared_removes_all_rows_for_that_subject() throws Exception {
		String ttl = "@prefix : <http://ex/> .\n" +
				":a :p 1 ; :q 99 .\n" +
				":b :p 2 .";
		load(ttl, RDFFormat.TURTLE);

		String q = "PREFIX : <http://ex/>\n" +
				"SELECT ?x ?n WHERE { ?x :p ?n MINUS { ?x :q ?m } } ORDER BY ?x";

		Set<Map<String, String>> got = runSelect(q);
		Set<Map<String, String>> exp = Set.of(row("x", ex("b"), "n", "2"));
		assertEquals(exp, got, "Since only ?x is shared, any :q for :a kills *all* its :p rows.");
	}

	@Test
	void rhs_subselect_order_by_limit_one_global_elimination() throws Exception {
		String ttl = "@prefix : <http://ex/> .\n" +
				":a :p 1 ; :q 1 .\n" +
				":b :p 2 ; :q 2 .\n" +
				":c :p 3 ; :q 3 .";
		load(ttl, RDFFormat.TURTLE);

		String q = "PREFIX : <http://ex/>\n" +
				"SELECT ?x WHERE {\n" +
				"  ?x :p ?n .\n" +
				"  MINUS {\n" +
				"    { SELECT ?x WHERE { ?x :q ?m } ORDER BY ?m LIMIT 1 }\n" +
				"  }\n" +
				"} ORDER BY ?x";

		Set<Map<String, String>> got = runSelect(q);
		// the RHS selects only the globally smallest ?m → ?x=:a → remove :a
		Set<Map<String, String>> exp = Set.of(row("x", ex("b")), row("x", ex("c")));
		assertEquals(exp, got, "Flattening/pushing ORDER BY/LIMIT across MINUS would change which row is removed.");
	}

	@Test
	void bnode_function_on_rhs_cannot_match_data_terms() throws Exception {
		// We'll avoid comparing bnode IDs by not selecting ?id at all.
		String ttl = "@prefix : <http://ex/> .\n" +
				"_:b1 a [] . _:b2 a [] .  # just to have bnodes around\n" +
				":k :p _:b1 . :l :p _:b2 .";
		load(ttl, RDFFormat.TURTLE);

		String q = "PREFIX : <http://ex/>\n" +
				"SELECT ?s WHERE { ?s :p ?id MINUS { BIND(BNODE() AS ?id) } } ORDER BY ?s";

		Set<Map<String, String>> got = runSelect(q);
		Set<Map<String, String>> exp = Set.of(row("s", ex("k")), row("s", ex("l")));
		assertEquals(exp, got,
				"BNODE() creates fresh, distinct bnodes – cannot match dataset objects, so MINUS is a no-op here.");
	}

	@Test
	void syntax_error_rebinding_in_rhs_must_fail_to_parse() {
		String q = "PREFIX : <http://ex/>\n" +
				"SELECT * WHERE {\n" +
				"  ?x :p ?v .\n" +
				"  MINUS { ?x :q ?v . BIND(1 AS ?v) }   # re-binding ?v inside same RHS group is illegal\n" +
				"}";

		assertThrows(MalformedQueryException.class, () -> cxn.prepareTupleQuery(QueryLanguage.SPARQL, q),
				"BIND target must not have been used earlier in the same group; parser should reject.");
	}
}
