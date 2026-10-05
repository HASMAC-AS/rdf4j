package org.hasmac.testing;

import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.ibm.icu.text.Collator;
import com.ibm.icu.util.ULocale;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.vocabulary.RDF4J;
import org.eclipse.rdf4j.query.*;
import org.eclipse.rdf4j.query.algebra.Order;
import org.eclipse.rdf4j.query.algebra.Var;
import org.eclipse.rdf4j.query.algebra.helpers.AbstractQueryModelVisitor;
import org.eclipse.rdf4j.query.impl.SimpleDataset;
import org.eclipse.rdf4j.query.parser.ParsedQuery;
import org.eclipse.rdf4j.query.parser.QueryParserUtil;
import org.eclipse.rdf4j.repository.*;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.helpers.NTriplesUtil;
import org.hasmac.testing.CorpusIO.Table;
import org.junit.jupiter.api.*;
import org.opentest4j.TestAbortedException;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public final class CorpusTest {
    private final LinkedHashMap<String, Holder> pool = new LinkedHashMap<>(16, .75f, true);
    private final int poolSize = Math.max(0, Integer.getInteger("suite.fixtureCacheSize", 2));
    private final RepositoryFactory factory;
    private Repository syntaxRepository;
    private final Path journal = CorpusIO.ROOT.resolve("reports/case-results.ndjson");

    public CorpusTest() throws Exception {
        String type = System.getProperty("repository.factory", MemoryRepositoryFactory.class.getName());
        factory = (RepositoryFactory) Class.forName(type).getDeclaredConstructor().newInstance();
    }

    private record Holder(Repository repository, Dataset dataset) implements AutoCloseable {
        @Override public void close() { repository.shutDown(); }
    }

    @TestFactory
    Stream<DynamicTest> queryCases() throws Exception {
        JsonNode data = CorpusIO.JSON.readTree(CorpusIO.ROOT.resolve("corpus/cases.json").toFile());
        assertTrue(data.isArray() && !data.isEmpty(), "Importer must produce non-empty cases.json");
        Files.createDirectories(journal.getParent()); Files.writeString(journal, "");
        Pattern filter = Pattern.compile(System.getProperty("suite.filter", ".*"));
        List<JsonNode> selected = new ArrayList<>();
        data.forEach(c -> { if (filter.matcher(c.path("id").asText() + " " + c.path("name").asText()).find()) selected.add(c); });
        assertFalse(selected.isEmpty(), "suite.filter matched zero cases");
        return selected.stream().map(c -> DynamicTest.dynamicTest(c.path("id").asText() + " " + c.path("name").asText(), URI.create(c.path("source").path("url").asText()), () -> run(c)));
    }

    private void run(JsonNode c) throws Throwable {
        long start = System.nanoTime(); String status = "passed", detail = "";
        try { execute(c); }
        catch (TestAbortedException e) { status = "skipped"; detail = e.toString(); throw e; }
        catch (Throwable e) { status = e instanceof AssertionError ? "failed" : "error"; detail = e.toString(); throw e; }
        finally {
            var item = CorpusIO.JSON.createObjectNode(); item.put("id", c.path("id").asText()); item.put("name", c.path("name").asText());
            item.put("status", status); item.put("seconds", (System.nanoTime() - start) / 1_000_000_000.0); item.put("detail", detail);
            try { Files.writeString(journal, item + "\n", StandardOpenOption.APPEND); }
            catch (Exception e) { System.err.println("Cannot record case outcome: " + e); }
        }
    }

    private void execute(JsonNode c) throws Exception {
        if (!"ready".equals(c.path("status").asText())) {
            String reason = c.path("blockedReason").asText();
            if (Boolean.getBoolean("suite.strict")) fail("Unported prerequisite: " + reason);
            Assumptions.assumeTrue(false, reason);
        }
        String text = c.path("query").asText(), base = c.path("base").asText(), kind = c.path("kind").asText();
        if (!"query".equals(kind)) {
            if (syntaxRepository == null) { syntaxRepository = factory.createRepository(); syntaxRepository.init(); }
            try (RepositoryConnection connection = syntaxRepository.getConnection()) {
                if ("negative-syntax".equals(kind)) assertThrows(MalformedQueryException.class, () -> connection.prepareQuery(QueryLanguage.SPARQL, text, base));
                else if ("positive-syntax".equals(kind)) assertNotNull(connection.prepareQuery(QueryLanguage.SPARQL, text, base));
                else throw new IllegalArgumentException("Unknown test kind: " + kind);
            }
            return;
        }
        ParsedQuery parsed = QueryParserUtil.parseQuery(QueryLanguage.SPARQL, text, base);
        Holder holder = repository(c, parsed.getDataset());
        try (RepositoryConnection connection = holder.repository.getConnection()) {
            Query query = connection.prepareQuery(QueryLanguage.SPARQL, text, base);
            query.setDataset(holder.dataset); query.setIncludeInferred(false);
            query.setMaxExecutionTime(Integer.getInteger("suite.timeoutSeconds", 20));
            JsonNode expected = c.get("expected");
            if (query instanceof TupleQuery tuple) {
                boolean yaml = c.has("qleverChecks");
                try (TupleQueryResult result = tuple.evaluate()) {
                    int cap = yaml ? 5000 : Integer.getInteger("suite.maxRows", 1000000);
                    Table actual = CorpusIO.consume(result, cap);
                    if (!yaml && result.hasNext()) throw new IllegalStateException("Query exceeded explicit suite.maxRows guard; no truncated comparison performed");
                    try {
                        if (yaml) qleverChecks(c, actual);
                        else {
                            Table reference = CorpusIO.expectedTable(expected);
                            boolean ordered = c.hasNonNull("ordered") ? c.get("ordered").asBoolean() : hasTopOrder(text);
                            List<String> keys = ordered ? tieKeys(parsed, reference.variables()) : List.of();
                            ResultOracle.compare(reference, actual, ordered, keys, c.path("lax").asBoolean());
                        }
                    } catch (AssertionError | RuntimeException failure) { saveActual(c, actual); throw failure; }
                }
            } else if (query instanceof BooleanQuery ask) {
                assertEquals(CorpusIO.expectedBoolean(expected), ask.evaluate(), "ASK result");
            } else if (query instanceof GraphQuery graphQuery) {
                Model actual = new LinkedHashModel();
                try (GraphQueryResult result = graphQuery.evaluate()) { while (result.hasNext()) actual.add(result.next()); }
                Model reference = CorpusIO.model(expected);
                assertTrue(Models.isomorphic(reference, actual), "Graph mismatch: expected " + reference.size() + " statements, got " + actual.size());
            } else throw new IllegalArgumentException("Unsupported prepared query type: " + query.getClass());
        } finally { if (poolSize == 0) holder.close(); }
    }

    private Holder repository(JsonNode c, Dataset requested) throws Exception {
        String key = c.path("defaults") + "\n" + c.path("named") + "\n" + requested;
        Holder existing = pool.get(key); if (existing != null) return existing;
        Repository repository = factory.createRepository(); repository.init();
        try {
            SimpleDataset natural = new SimpleDataset(); natural.addDefaultGraph(RDF4J.NIL);
            Set<IRI> supplied = new HashSet<>();
            try (RepositoryConnection connection = repository.getConnection()) {
                CorpusIO.configure(connection.getParserConfig()); connection.begin();
                for (JsonNode a : c.path("defaults")) load(connection, a, null, true);
                for (JsonNode graph : c.path("named")) {
                    IRI name = CorpusIO.VF.createIRI(graph.path("graph").asText());
                    supplied.add(name); load(connection, graph.get("asset"), name, false); natural.addNamedGraph(name);
                }
                try (RepositoryResult<Resource> contexts = connection.getContextIDs()) {
                    while (contexts.hasNext()) if (contexts.next() instanceof IRI iri) { supplied.add(iri); natural.addNamedGraph(iri); }
                }
                if (requested != null) {
                    Set<IRI> documents = new LinkedHashSet<>(requested.getDefaultGraphs()); documents.addAll(requested.getNamedGraphs());
                    for (IRI document : documents) {
                        if (document.equals(RDF4J.NIL) || supplied.contains(document)) continue;
                        load(connection, CorpusIO.localDatasetAsset(document.stringValue()), document, false); supplied.add(document);
                    }
                }
                connection.commit();
            }
            Holder holder = new Holder(repository, requested == null ? natural : requested);
            if (poolSize > 0) {
                pool.put(key, holder);
                while (pool.size() > poolSize) {
                    var iterator = pool.entrySet().iterator(); var oldest = iterator.next(); iterator.remove(); oldest.getValue().close();
                }
            }
            return holder;
        } catch (Exception | Error e) { repository.shutDown(); throw e; }
    }

    private static void load(RepositoryConnection connection, JsonNode asset, IRI context, boolean preserveDataset) throws Exception {
        RDFFormat format = CorpusIO.rdfFormat(asset);
        try (InputStream in = Files.newInputStream(CorpusIO.file(asset))) {
            if (preserveDataset && format.supportsContexts()) connection.add(in, asset.path("uri").asText(), format);
            else connection.add(in, asset.path("uri").asText(), format, (Resource) context);
        }
    }

    private static List<String> tieKeys(ParsedQuery parsed, List<String> projected) {
        List<Order> orders = new ArrayList<>();
        parsed.getTupleExpr().visit(new AbstractQueryModelVisitor<RuntimeException>() {
            @Override public void meet(Order order) { if (orders.isEmpty()) orders.add(order); }
        });
        if (orders.isEmpty()) return List.of();
        List<String> keys = new ArrayList<>();
        for (var elem : orders.getFirst().getElements()) {
            if (!(elem.getExpr() instanceof Var var) || !projected.contains(var.getName())) return List.of();
            keys.add(var.getName());
        }
        return keys;
    }

    static boolean hasTopOrder(String q) {
        int braces = 0;
        for (int i = 0; i < q.length();) {
            char ch = q.charAt(i);
            if (ch == '#') { int j = q.indexOf('\n', i); i = j < 0 ? q.length() : j + 1; continue; }
            if (ch == '\'' || ch == '"') {
                boolean triple = i + 2 < q.length() && q.charAt(i + 1) == ch && q.charAt(i + 2) == ch;
                int width = triple ? 3 : 1; i += width;
                while (i < q.length()) {
                    if (q.charAt(i) == '\\') { i += 2; continue; }
                    if (q.charAt(i) == ch && (!triple || i + 2 < q.length() && q.charAt(i + 1) == ch && q.charAt(i + 2) == ch)) { i += width; break; }
                    i++;
                }
                continue;
            }
            if (ch == '<') { int j = q.indexOf('>', i + 1); if (j > i && !q.substring(i, j).chars().anyMatch(Character::isWhitespace)) { i = j + 1; continue; } }
            if (ch == '{') braces++; else if (ch == '}') braces--;
            if (braces == 0 && (i == 0 || !Character.isLetterOrDigit(q.charAt(i - 1))) && q.regionMatches(true, i, "ORDER", 0, 5)) {
                int j = i + 5; while (j < q.length() && Character.isWhitespace(q.charAt(j))) j++;
                if (j > i + 5 && q.regionMatches(true, j, "BY", 0, 2)) return true;
            }
            i++;
        }
        return false;
    }

    private static void qleverChecks(JsonNode c, Table actual) {
        for (JsonNode check : c.path("qleverChecks")) {
            var fields = check.fields();
            while (fields.hasNext()) {
                var entry = fields.next(); JsonNode value = entry.getValue();
                switch (entry.getKey()) {
                    case "num_rows" -> assertEquals(value.asInt(), actual.rows().size(), "QLever returned-row count");
                    case "num_cols" -> { if (!actual.rows().isEmpty()) assertEquals(value.asInt(), actual.variables().size(), "QLever column count"); }
                    case "selected" -> {
                        List<String> selected = new ArrayList<>(); value.forEach(v -> selected.add(v.asText().replaceFirst("^\\?", "")));
                        assertEquals(selected, actual.variables(), "QLever selected header");
                    }
                    case "res" -> {
                        assertEquals(value.size(), actual.rows().size(), "QLever expected rows");
                        for (int i = 0; i < value.size(); i++) assertTrue(matches(value.get(i), actual.rows().get(i), actual.variables(), c.path("base").asText()), "QLever row " + i + ": " + actual.rows().get(i));
                    }
                    case "contains_row" -> assertTrue(actual.rows().stream().anyMatch(r -> matches(value, r, actual.variables(), c.path("base").asText())), "QLever missing row " + value);
                    case "order_numeric", "order_string" -> {
                        String var = value.path("var").asText().replaceFirst("^\\?", "");
                        boolean ascending = "asc".equalsIgnoreCase(value.path("dir").asText());
                        Collator collator = Collator.getInstance(new ULocale("de_DE"));
                        for (int i = 1; i < actual.rows().size(); i++) {
                            Value before = actual.rows().get(i - 1).get(var), after = actual.rows().get(i).get(var);
                            assertNotNull(before); assertNotNull(after);
                            int cmp = entry.getKey().equals("order_numeric") ? Double.compare(((Literal) before).doubleValue(), ((Literal) after).doubleValue()) : collator.compare(NTriplesUtil.toNTriplesString(before), NTriplesUtil.toNTriplesString(after));
                            assertTrue(ascending ? cmp <= 0 : cmp >= 0, "QLever ordering at row " + i);
                        }
                    }
                    default -> throw new IllegalArgumentException("Unknown QLever assertion: " + entry.getKey());
                }
            }
        }
    }

    private static boolean matches(JsonNode expected, Map<String,Value> actual, List<String> vars, String base) {
        if (!expected.isArray() || expected.size() != vars.size()) return false;
        for (int i = 0; i < vars.size(); i++) {
            JsonNode gold = expected.get(i); if (gold.isNull()) continue;
            Value value = actual.get(vars.get(i)); if (value == null) return false;
            try {
                if (gold.isTextual() && gold.asText().startsWith("<")) {
                    String iri = gold.asText(); if (!iri.endsWith(">")) return false;
                    if (!(value instanceof IRI) || !URI.create(base).resolve(iri.substring(1, iri.length() - 1)).toString().equals(value.stringValue())) return false;
                } else if (!(value instanceof Literal literal)) return false;
                else if (gold.isBoolean()) { if (!literal.getLabel().equals(gold.asBoolean() ? "true" : "false")) return false; }
                else if (gold.isIntegralNumber()) { if (!literal.integerValue().equals(gold.bigIntegerValue())) return false; }
                else if (gold.isFloatingPointNumber()) { if (!(Math.abs(literal.doubleValue() - gold.asDouble()) <= .1)) return false; }
                else if (!literal.getLabel().equals(gold.asText())) return false;
            } catch (RuntimeException e) { return false; }
        }
        return true;
    }

    private static void saveActual(JsonNode c, Table table) throws Exception {
        Path dir = CorpusIO.ROOT.resolve("reports/actual-results"); Files.createDirectories(dir);
        List<Map<String,String>> rows = new ArrayList<>();
        for (var row : table.rows()) { Map<String,String> out = new LinkedHashMap<>(); row.forEach((k,v) -> out.put(k, NTriplesUtil.toNTriplesString(v))); rows.add(out); }
        CorpusIO.JSON.writerWithDefaultPrettyPrinter().writeValue(dir.resolve(c.path("id").asText() + ".json").toFile(), Map.of("variables", table.variables(), "rows", rows));
    }

    @AfterAll void closeRepositories() {
        for (Holder holder : pool.values()) holder.close(); pool.clear();
        if (syntaxRepository != null) syntaxRepository.shutDown();
    }
}
