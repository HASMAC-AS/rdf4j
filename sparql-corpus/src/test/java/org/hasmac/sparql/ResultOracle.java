package org.hasmac.sparql;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.*;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.BindingSet;

/** Whole-relation equality: bags, unbound cells and globally consistent blank nodes. */
final class ResultOracle {
    private static final ValueFactory VF = SimpleValueFactory.getInstance();
    private static final IRI ROW = VF.createIRI("urn:corpus:oracle:Row");
    private static final IRI INDEX = VF.createIRI("urn:corpus:oracle:index");
    private static final IRI TRIPLE = VF.createIRI("urn:corpus:oracle:Triple");
    private ResultOracle() {}

    static Map<String,Value> row(BindingSet bindings) {
        Map<String,Value> row = new TreeMap<>();
        for (var binding : bindings) row.put(binding.getName(), binding.getValue());
        return Map.copyOf(row);
    }
    static boolean blank(Value v) {
        return v instanceof BNode || v instanceof TripleTerm t && (blank(t.getSubject()) || blank(t.getObject()));
    }
    static Map<Map<String,Value>,Integer> bag(List<Map<String,Value>> rows) {
        Map<Map<String,Value>,Integer> counts = new HashMap<>();
        for (var row : rows) counts.merge(row,1,Integer::sum);
        return counts;
    }
    static void tuples(List<String> expectedVars, List<Map<String,Value>> expected,
                       List<String> actualVars, List<Map<String,Value>> actual, boolean ordered) {
        assertEquals(new TreeSet<>(expectedVars),new TreeSet<>(actualVars),"projection variables");
        assertEquals(expected.size(),actual.size(),"solution cardinality (duplicates count)");
        boolean hasBlank = expected.stream().flatMap(r -> r.values().stream()).anyMatch(ResultOracle::blank)
                || actual.stream().flatMap(r -> r.values().stream()).anyMatch(ResultOracle::blank);
        if (!hasBlank) {
            if (ordered) assertEquals(expected,actual,"ordered solution sequence");
            else assertEquals(bag(expected),bag(actual),"solution multiset");
        } else {
            assertTrue(Models.isomorphic(relation(expected,ordered,"e"),relation(actual,ordered,"a")),
                    () -> "No consistent blank-node bijection for the complete result relation. Expected="
                            + expected.subList(0,Math.min(20,expected.size())) + "; actual="
                            + actual.subList(0,Math.min(20,actual.size())));
        }
    }
    static Model relation(List<Map<String,Value>> rows, boolean ordered, String prefix) {
        Model model = new LinkedHashModel();
        Map<BNode,BNode> bnodes = new HashMap<>();
        Map<TripleTerm,BNode> triples = new HashMap<>();
        for (int i=0;i<rows.size();i++) {
            BNode row = VF.createBNode(prefix+"-row-"+i);
            model.add(row,RDF.TYPE,ROW);
            if (ordered) model.add(row,INDEX,VF.createLiteral(i));
            for (var entry : rows.get(i).entrySet()) {
                IRI variable = VF.createIRI("urn:corpus:oracle:var:" + Base64.getUrlEncoder().withoutPadding()
                        .encodeToString(entry.getKey().getBytes(StandardCharsets.UTF_8)));
                model.add(row,variable,encode(entry.getValue(),model,bnodes,triples,prefix));
            }
        }
        return model;
    }
    private static Value encode(Value value, Model model, Map<BNode,BNode> bnodes,
                                Map<TripleTerm,BNode> triples, String prefix) {
        if (value instanceof BNode b) return bnodes.computeIfAbsent(b, k -> VF.createBNode(prefix+"-value-"+bnodes.size()));
        if (value instanceof TripleTerm t) {
            BNode existing=triples.get(t);
            if (existing!=null) return existing;
            BNode node=VF.createBNode(prefix+"-triple-"+triples.size()); triples.put(t,node);
            model.add(node,RDF.TYPE,TRIPLE);
            model.add(node,RDF.SUBJECT,encode(t.getSubject(),model,bnodes,triples,prefix));
            model.add(node,RDF.PREDICATE,t.getPredicate());
            model.add(node,RDF.OBJECT,encode(t.getObject(),model,bnodes,triples,prefix));
            return node;
        }
        return value;
    }
}
