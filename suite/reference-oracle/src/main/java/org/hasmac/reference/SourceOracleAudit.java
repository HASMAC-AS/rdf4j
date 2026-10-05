package org.hasmac.reference;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.*;
import java.util.*;
import org.apache.jena.graph.Node;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.*;
import org.apache.jena.riot.resultset.ResultSetLang;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.ResultSetStream;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.engine.binding.BindingBuilder;
import org.apache.jena.sparql.engine.iterator.QueryIterPlainWrapper;
import org.apache.jena.sparql.resultset.ResultsCompare;
import org.apache.jena.sparql.resultset.SPARQLResult;
import org.apache.jena.sparql.util.NodeFactoryExtra;

/**
 * Re-check saved failed SELECT outputs with the actual pinned Jena comparator.
 * No SPARQL query is executed here and no upstream expected result is regenerated.
 * This audit does not modify original RDF4J/Jupiter outcomes.
 */
public final class SourceOracleAudit {
    private static final ObjectMapper JSON = new ObjectMapper();

    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("Usage: SourceOracleAudit <corpus project root>");
        Path root = Path.of(args[0]).toAbsolutePath().normalize();
        JsonNode cases = JSON.readTree(root.resolve("corpus/cases.json").toFile());
        List<Map<String,Object>> results = new ArrayList<>();
        Map<String,Integer> counts = new TreeMap<>();
        for (JsonNode c : cases) {
            if (!"jena".equals(c.path("project").asText()) || !"query".equals(c.path("kind").asText()) || !c.has("queryAsset") || !c.path("expected").has("path")) continue;
            Path actualFile = root.resolve("reports/actual-results/" + c.path("id").asText() + ".json");
            if (!Files.isRegularFile(actualFile)) continue;
            Map<String,Object> record = new LinkedHashMap<>();
            record.put("id", c.path("id").asText()); record.put("name", c.path("name").asText());
            record.put("source", c.path("source").path("url").asText());
            record.put("scope", "Jena default manifest comparison; suite-level overrides not assumed");
            String status;
            try {
                Query query = QueryFactory.create(c.path("query").asText(), c.path("base").asText(), Syntax.syntaxARQ);
                record.put("ordered", query.isOrdered()); record.put("reduced", query.isReduced());
                ResultSetRewindable expected = expected(root, c.get("expected"));
                ResultSetRewindable actual = actual(JSON.readTree(actualFile.toFile()));
                if (query.isReduced()) { expected = unique(expected); actual = unique(actual); }
                boolean byTerm = query.isOrdered() ? ResultsCompare.equalsByTermAndOrder(expected, actual) : ResultsCompare.equalsByTerm(expected, actual);
                expected.reset(); actual.reset();
                boolean byValue = query.isOrdered() ? ResultsCompare.equalsByValueAndOrder(expected, actual) : ResultsCompare.equalsByValue(expected, actual);
                record.put("matchesByTerm", byTerm); record.put("matchesByValue", byValue);
                status = byValue ? byTerm ? "matches-upstream-term-and-value" : "value-comparison-policy-difference" : "still-mismatches-upstream-default";
            } catch (Exception | AssertionError e) {
                status = "reference-audit-error"; record.put("error", e.toString());
            }
            record.put("status", status); results.add(record); counts.merge(status, 1, Integer::sum);
        }
        Path reports = root.resolve("reports"); Files.createDirectories(reports);
        JSON.writerWithDefaultPrettyPrinter().writeValue(reports.resolve("upstream-oracle-audit.json").toFile(), results);
        Map<String,Object> summary = new LinkedHashMap<>();
        summary.put("assessedFailedSelectOutputs", results.size()); summary.put("outcomes", counts);
        summary.put("comparatorRepository", "HASMAC-AS/jena"); summary.put("comparatorRevision", "df0d523eed42cd0b7c9ef11c74eeb0a63302a423");
        summary.put("comparator", "org.apache.jena.sparql.resultset.ResultsCompare");
        summary.put("queryEvaluationPerformed", false); summary.put("originalJunitOutcomesModified", false);
        summary.put("coverage", "Only saved failed manifest SELECT outputs; no claim to reassess unsaved errors, graph results, syntax tests or native helper assertions");
        JSON.writerWithDefaultPrettyPrinter().writeValue(reports.resolve("upstream-oracle-summary.json").toFile(), summary);
        System.out.println(JSON.writeValueAsString(summary));
    }

    private static ResultSetRewindable expected(Path root, JsonNode asset) {
        Path path = root.resolve(asset.path("path").asText()).normalize();
        if (!path.startsWith(root) || !Files.isRegularFile(path)) throw new IllegalArgumentException("Missing/unsafe expected asset: " + path);
        Lang lang = RDFLanguages.pathnameToLang(path.toString());
        if (ResultSetLang.isRegistered(lang)) {
            SPARQLResult result = ResultSetFactory.result(path.toString());
            if (!result.isResultSet()) throw new IllegalArgumentException("Expected SELECT result set: " + path);
            return ResultSetFactory.makeRewindable(result.getResultSet());
        }
        Model model = RDFParser.create().source(path.toString()).lang(lang).base(asset.path("uri").asText()).toModel();
        return ResultSetFactory.makeRewindable(model);
    }

    private static ResultSetRewindable actual(JsonNode document) {
        List<String> variables = new ArrayList<>(); document.path("variables").forEach(v -> variables.add(v.asText()));
        List<Binding> rows = new ArrayList<>();
        for (JsonNode row : document.path("rows")) {
            BindingBuilder builder = Binding.builder();
            var fields = row.fields();
            while (fields.hasNext()) {
                var field = fields.next();
                Node node = NodeFactoryExtra.parseNode(field.getValue().asText());
                builder.add(Var.alloc(field.getKey()), node);
            }
            rows.add(builder.build());
        }
        return result(variables, rows);
    }

    private static ResultSetRewindable unique(ResultSetRewindable input) {
        List<String> variables = List.copyOf(input.getResultVars());
        List<Binding> rows = new ArrayList<>(); Set<Binding> seen = new HashSet<>();
        while (input.hasNext()) { Binding b = input.nextBinding(); if (seen.add(b)) rows.add(b); }
        return result(variables, rows);
    }

    private static ResultSetRewindable result(List<String> variables, List<Binding> rows) {
        return ResultSetFactory.makeRewindable(ResultSetStream.create(variables, null, QueryIterPlainWrapper.create(rows.iterator())));
    }
}
