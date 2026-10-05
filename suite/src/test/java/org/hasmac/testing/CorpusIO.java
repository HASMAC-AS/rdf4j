package org.hasmac.testing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.*;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.*;
import org.eclipse.rdf4j.query.resultio.QueryResultIO;
import org.eclipse.rdf4j.rio.*;
import org.eclipse.rdf4j.rio.helpers.BasicParserSettings;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;

final class CorpusIO {
    static final ObjectMapper JSON = new ObjectMapper();
    static final ValueFactory VF = SimpleValueFactory.getInstance();
    static final Path ROOT = Path.of(System.getProperty("corpus.root", ".")).toAbsolutePath().normalize();
    private static final String RS = "http://www.w3.org/2001/sw/DataAccess/tests/result-set#";
    private static final Set<String> VERIFIED = new HashSet<>();

    record Table(List<String> variables, List<Map<String, Value>> rows) {
        Table { variables = List.copyOf(variables); rows = List.copyOf(rows); }
    }

    static Path file(JsonNode asset) throws Exception {
        if (asset == null || !asset.hasNonNull("path")) throw new IllegalArgumentException("Missing source asset");
        Path p = ROOT.resolve(asset.get("path").asText()).normalize();
        if (!p.startsWith(ROOT) || !Files.isRegularFile(p)) throw new IllegalArgumentException("Missing/unsafe asset: " + p);
        String expected = asset.path("sha256").asText();
        if (!expected.isEmpty() && VERIFIED.add(p + ":" + expected)) {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            try (InputStream in = Files.newInputStream(p)) {
                byte[] buffer = new byte[65536]; int n;
                while ((n = in.read(buffer)) != -1) md.update(buffer, 0, n);
            }
            if (!HexFormat.of().formatHex(md.digest()).equals(expected)) {
                VERIFIED.remove(p + ":" + expected);
                throw new IllegalStateException("Source asset hash mismatch: " + p);
            }
        }
        return p;
    }

    static RDFFormat rdfFormat(JsonNode asset) {
        if ("Turtle".equals(asset.path("format").asText())) return RDFFormat.TURTLE;
        String path = asset.path("path").asText();
        if (path.endsWith(".n3")) return RDFFormat.TURTLE;
        return Rio.getParserFormatForFileName(path).orElseThrow(() -> new IllegalArgumentException("Unknown RDF format: " + path));
    }

    static void configure(ParserConfig config) {
        config.set(BasicParserSettings.VERIFY_DATATYPE_VALUES, false);
        config.set(BasicParserSettings.NORMALIZE_DATATYPE_VALUES, false);
        config.set(BasicParserSettings.FAIL_ON_UNKNOWN_DATATYPES, false);
    }

    static Model model(JsonNode asset) throws Exception {
        Model out = new LinkedHashModel();
        RDFParser parser = Rio.createParser(rdfFormat(asset));
        configure(parser.getParserConfig());
        parser.setRDFHandler(new StatementCollector(out));
        try (InputStream in = Files.newInputStream(file(asset))) { parser.parse(in, asset.path("uri").asText()); }
        return out;
    }

    static Value term(JsonNode node) {
        if (node == null || node.isNull()) return null;
        String text = node.path("value").asText();
        return switch (node.path("type").asText()) {
            case "iri" -> VF.createIRI(text);
            case "bnode" -> VF.createBNode(text);
            case "literal" -> node.hasNonNull("language") ? VF.createLiteral(text, node.get("language").asText())
                : VF.createLiteral(text, VF.createIRI(node.path("datatype").asText("http://www.w3.org/2001/XMLSchema#string")));
            default -> throw new IllegalArgumentException("Unknown inline term: " + node);
        };
    }

    static Table consume(TupleQueryResult result, int cap) {
        List<String> vars = result.getBindingNames();
        List<Map<String, Value>> rows = new ArrayList<>();
        while (result.hasNext() && rows.size() < cap) rows.add(copy(result.next()));
        return new Table(vars, rows);
    }

    private static Map<String, Value> copy(BindingSet bindings) {
        Map<String, Value> row = new LinkedHashMap<>();
        for (Binding binding : bindings) row.put(binding.getName(), binding.getValue());
        return Collections.unmodifiableMap(row);
    }

    static Table expectedTable(JsonNode spec) throws Exception {
        if ("tuple".equals(spec.path("type").asText())) {
            List<String> vars = new ArrayList<>(); spec.path("vars").forEach(x -> vars.add(x.asText()));
            List<Map<String, Value>> rows = new ArrayList<>();
            for (JsonNode row : spec.path("rows")) {
                if (row.size() != vars.size()) throw new IllegalArgumentException("Inline expected row width mismatch");
                Map<String, Value> mapped = new LinkedHashMap<>();
                for (int i = 0; i < vars.size(); i++) { Value value = term(row.get(i)); if (value != null) mapped.put(vars.get(i), value); }
                rows.add(mapped);
            }
            return new Table(vars, rows);
        }
        Path p = file(spec);
        var format = QueryResultIO.getParserFormatForFileName(p.toString());
        if (format.isPresent()) {
            List<String> variables = new ArrayList<>();
            List<Map<String,Value>> rows = new ArrayList<>();
            // Synchronous handler avoids per-file worker threads and supports RDF4J 6's API.
            TupleQueryResultHandler handler = new AbstractTupleQueryResultHandler() {
                @Override public void startQueryResult(List<String> names) { variables.addAll(names); }
                @Override public void handleSolution(BindingSet bindings) { rows.add(copy(bindings)); }
            };
            try (InputStream in = Files.newInputStream(p)) { QueryResultIO.parseTuple(in, format.get(), handler, VF); }
            return new Table(variables, rows);
        }
        Model model = model(spec);
        List<Resource> roots = model.filter(null, RDF.TYPE, iri("ResultSet")).subjects().stream().toList();
        if (roots.size() != 1) throw new IllegalArgumentException("Expected one RDF result-set root in " + p);
        Resource root = roots.getFirst();
        List<String> vars = model.filter(root, iri("resultVariable"), null).objects().stream().map(Value::stringValue).sorted().toList();
        List<Resource> solutions = model.filter(root, iri("solution"), null).objects().stream().map(x -> (Resource) x).toList();
        boolean indexed = solutions.stream().anyMatch(s -> !model.filter(s, iri("index"), null).isEmpty());
        if (indexed) solutions = solutions.stream().sorted(Comparator.comparingInt(s -> {
            Value v = one(model, s, iri("index"));
            if (!(v instanceof Literal l)) throw new IllegalArgumentException("Partially indexed expected results");
            return l.intValue();
        })).toList();
        List<Map<String, Value>> rows = new ArrayList<>();
        for (Resource solution : solutions) {
            Map<String, Value> row = new LinkedHashMap<>();
            for (Value binding : model.filter(solution, iri("binding"), null).objects()) {
                Value var = one(model, (Resource) binding, iri("variable"));
                Value value = one(model, (Resource) binding, iri("value"));
                if (var == null || value == null) throw new IllegalArgumentException("Malformed expected binding in " + p);
                if (row.put(var.stringValue(), value) != null) throw new IllegalArgumentException("Duplicate expected binding");
            }
            rows.add(row);
        }
        return new Table(vars, rows);
    }

    static boolean expectedBoolean(JsonNode spec) throws Exception {
        if ("boolean".equals(spec.path("type").asText())) return spec.path("value").asBoolean();
        Path p = file(spec);
        var format = QueryResultIO.getBooleanParserFormatForFileName(p.toString());
        if (format.isPresent()) try (InputStream in = Files.newInputStream(p)) { return QueryResultIO.parseBoolean(in, format.get()); }
        Model m = model(spec); Set<Value> values = m.filter(null, iri("boolean"), null).objects();
        if (values.size() != 1 || !(values.iterator().next() instanceof Literal)) throw new IllegalArgumentException("Missing ASK oracle");
        return ((Literal) values.iterator().next()).booleanValue();
    }

    private static IRI iri(String local) { return VF.createIRI(RS, local); }
    private static Value one(Model m, Resource s, IRI p) {
        Set<Value> values = m.filter(s, p, null).objects();
        if (values.size() > 1) throw new IllegalArgumentException("Expected one value for " + p);
        return values.isEmpty() ? null : values.iterator().next();
    }

    /** Resolve FROM only into the checked-out fork trees; never dereference the network. */
    static JsonNode localDatasetAsset(String uri) throws Exception {
        JsonNode sources = JSON.readTree(ROOT.resolve("sources.json").toFile());
        for (String name : List.of("jena", "qlever")) {
            JsonNode source = sources.get(name);
            String prefix = "https://raw.githubusercontent.com/" + source.get("repository").asText() + "/" + source.get("revision").asText() + "/";
            if (!uri.startsWith(prefix)) continue;
            String path = URI.create(uri).getPath().substring(URI.create(prefix).getPath().length());
            Path local = ROOT.resolve("vendor/" + name).resolve(path).normalize();
            if (!local.startsWith(ROOT.resolve("vendor/" + name)) || !Files.isRegularFile(local)) break;
            var object = JSON.createObjectNode(); object.put("path", ROOT.relativize(local).toString()); object.put("uri", uri);
            return object;
        }
        throw new IllegalArgumentException("FROM requires an unmaterialized external document: " + uri);
    }

    static String brief(Table table) { return table.variables + " (" + table.rows.size() + " rows) " + table.rows.stream().limit(12).toList(); }
}
