package org.hasmac.testing;

import static org.junit.jupiter.api.Assertions.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.hasmac.testing.CorpusIO.Table;

/** Independent duplicate-sensitive relational encoding, not RDF4J query evaluation. */
final class ResultOracle {
    private static final ValueFactory VF = CorpusIO.VF;
    private static final String NS = "urn:hasmac:test-oracle:";

    static void compare(Table expected, Table actual, boolean ordered, List<String> tieKeys, boolean lax) {
        assertEquals(new HashSet<>(expected.variables()), new HashSet<>(actual.variables()), "Projected variable names");
        String context = "Expected " + CorpusIO.brief(expected) + " but got " + CorpusIO.brief(actual);
        boolean blanks = expected.rows().stream().anyMatch(ResultOracle::hasBlank) || actual.rows().stream().anyMatch(ResultOracle::hasBlank);
        if (lax) {
            if (blanks) throw new UnsupportedOperationException("Blank-node-aware REDUCED cardinality oracle not yet implemented");
            Map<Map<String,Value>,Integer> e = counts(expected.rows()), a = counts(actual.rows());
            assertEquals(e.keySet(), a.keySet(), context);
            for (var entry : a.entrySet()) assertTrue(entry.getValue() >= 1 && entry.getValue() <= e.get(entry.getKey()), context);
            if (ordered) {
                List<Map<String,Value>> compressed = new ArrayList<>();
                for (var row : expected.rows()) if (compressed.isEmpty() || !compressed.getLast().equals(row)) compressed.add(row);
                List<Map<String,Value>> actualCompressed = new ArrayList<>();
                for (var row : actual.rows()) if (actualCompressed.isEmpty() || !actualCompressed.getLast().equals(row)) actualCompressed.add(row);
                assertEquals(compressed, actualCompressed, "REDUCED order: " + context);
            }
            return;
        }
        assertEquals(expected.rows().size(), actual.rows().size(), context);
        int[] eg = groups(expected, ordered, tieKeys), ag = groups(actual, ordered, tieKeys);
        if (!blanks) {
            if (!ordered) assertEquals(counts(expected.rows()), counts(actual.rows()), context);
            else if (tieKeys.isEmpty()) assertEquals(expected.rows(), actual.rows(), context);
            else {
                assertArrayEquals(eg, ag, "Order tie group sizes: " + context);
                int start = 0;
                while (start < eg.length) {
                    int end = start + 1; while (end < eg.length && eg[end] == eg[start]) end++;
                    assertEquals(counts(expected.rows().subList(start, end)), counts(actual.rows().subList(start, end)), "Order tie group: " + context);
                    start = end;
                }
            }
        } else {
            assertTrue(Models.isomorphic(encode(expected, eg, ordered), encode(actual, ag, ordered)), "Blank-node mapping / duplicate / ordering mismatch. " + context);
        }
    }

    static boolean hasBlank(Map<String,Value> row) { return row.values().stream().anyMatch(ResultOracle::hasBlank); }
    static boolean hasBlank(Value v) {
        if (v instanceof BNode) return true;
        if (v instanceof TripleTerm t) return hasBlank(t.getSubject()) || hasBlank(t.getObject());
        return false;
    }

    private static Map<Map<String,Value>,Integer> counts(List<Map<String,Value>> rows) {
        Map<Map<String,Value>,Integer> out = new HashMap<>();
        for (var row : rows) out.merge(row, 1, Integer::sum);
        return out;
    }

    private static int[] groups(Table table, boolean ordered, List<String> keys) {
        int[] groups = new int[table.rows().size()];
        if (!ordered) return groups;
        for (int i = 1; i < groups.length; i++) {
            boolean equal = !keys.isEmpty();
            for (String key : keys) if (!Objects.equals(table.rows().get(i - 1).get(key), table.rows().get(i).get(key))) { equal = false; break; }
            groups[i] = groups[i - 1] + (equal ? 0 : 1);
        }
        return groups;
    }

    private static Model encode(Table table, int[] groups, boolean ordered) {
        Model graph = new LinkedHashModel();
        Map<Value,Resource> nodes = new HashMap<>();
        IRI root = VF.createIRI(NS + "root"), member = VF.createIRI(NS + "member"), position = VF.createIRI(NS + "position");
        for (int i = 0; i < table.rows().size(); i++) {
            BNode row = VF.createBNode(); graph.add(root, member, row);
            graph.add(row, RDF.TYPE, VF.createIRI(NS + "solution"));
            if (ordered) graph.add(row, position, VF.createLiteral(groups[i]));
            for (var entry : table.rows().get(i).entrySet()) {
                IRI predicate = VF.createIRI(NS + "variable/" + URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8));
                graph.add(row, predicate, encodeTerm(entry.getValue(), graph, nodes));
            }
        }
        return graph;
    }

    private static Value encodeTerm(Value term, Model graph, Map<Value,Resource> nodes) {
        if (term instanceof BNode) return nodes.computeIfAbsent(term, ignored -> VF.createBNode());
        if (term instanceof TripleTerm triple) {
            Resource existing = nodes.get(term); if (existing != null) return existing;
            BNode node = VF.createBNode(); nodes.put(term, node);
            graph.add(node, RDF.TYPE, VF.createIRI(NS + "triple-term"));
            graph.add(node, VF.createIRI(NS + "subject"), encodeTerm(triple.getSubject(), graph, nodes));
            graph.add(node, VF.createIRI(NS + "predicate"), triple.getPredicate());
            graph.add(node, VF.createIRI(NS + "object"), encodeTerm(triple.getObject(), graph, nodes));
            return node;
        }
        return term;
    }
}
