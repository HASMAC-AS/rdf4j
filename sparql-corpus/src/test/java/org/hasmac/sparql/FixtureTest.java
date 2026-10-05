package org.hasmac.sparql;
import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import org.junit.jupiter.api.Test;

class FixtureTest {
    static ObjectNode test(String query,String expected) throws Exception {
        ObjectNode c=CorpusTest.JSON.createObjectNode();c.put("status","ready");c.put("kind","evaluation");c.put("base","http://example/");c.put("query",query);c.put("querySha256",CorpusTest.hex(MessageDigest.getInstance("SHA-256").digest(query.getBytes(StandardCharsets.UTF_8))));c.putArray("fixtures");c.set("expected",CorpusTest.JSON.readTree(expected));return c;
    }
    static String count(int n) {return "{\"kind\":\"tuple\",\"vars\":[\"c\"],\"rows\":[{\"c\":{\"type\":\"literal\",\"value\":\""+n+"\",\"datatype\":\"http://www.w3.org/2001/XMLSchema#integer\"}}]}";}
    @Test void expressionErrorIsOneUnboundRow() throws Exception {CorpusTest.execute(test("SELECT ?x WHERE {BIND(1/0 AS ?x)}","{\"kind\":\"tuple\",\"vars\":[\"x\"],\"rows\":[{}]}"));}
    @Test void falseFilterProducesZeroRows() throws Exception {CorpusTest.execute(test("SELECT ?x WHERE {BIND(1 AS ?x) FILTER(false)}","{\"kind\":\"tuple\",\"vars\":[\"x\"],\"rows\":[]}"));}
    @Test void askTrue() throws Exception {CorpusTest.execute(test("ASK {}","{\"kind\":\"boolean\",\"value\":true}"));}
    @Test void askFalse() throws Exception {CorpusTest.execute(test("ASK {FILTER(false)}","{\"kind\":\"boolean\",\"value\":false}"));}
    @Test void negativeSyntaxOnlyAcceptsParseError() throws Exception {var c=test("SELECT WHERE","{}");c.put("kind","syntax-negative");CorpusTest.execute(c);}
    @Test void twoRunsHaveFreshRepositories() throws Exception {CorpusTest.execute(test("SELECT (COUNT(*) AS ?c) WHERE {?s ?p ?o}",count(0)));CorpusTest.execute(test("SELECT (COUNT(*) AS ?c) WHERE {?s ?p ?o}",count(0)));}
    @Test void namedGraphDoesNotLeakIntoDefault() throws Exception {
        Path dir=Files.createTempDirectory(CorpusTest.ROOT,"fixture-calibration-");
        try {
            Path d=dir.resolve("default.ttl"),n=dir.resolve("named.ttl");Files.writeString(d,"<urn:s> <urn:p> <urn:o> .");Files.writeString(n,"<urn:n> <urn:p> <urn:o> .");
            var c=test("SELECT (COUNT(*) AS ?c) WHERE {?s ?p ?o}",count(1));var f=c.withArray("fixtures");
            f.addObject().put("path",CorpusTest.ROOT.relativize(d).toString()).put("base","http://example/default.ttl").putNull("graph");
            f.addObject().put("path",CorpusTest.ROOT.relativize(n).toString()).put("base","http://example/named.ttl").put("graph","urn:g");
            CorpusTest.execute(c);
        } finally {try(var walk=Files.walk(dir)) {for(Path p:walk.sorted(Comparator.reverseOrder()).toList()) Files.delete(p);}}
    }
    @Test void trigPreservesDefaultAndNamedGraphs() throws Exception {
        Path dir=Files.createTempDirectory(CorpusTest.ROOT,"fixture-calibration-");
        try {
            Path d=dir.resolve("data.trig");Files.writeString(d,"<urn:s> <urn:p> <urn:o> . <urn:g> {<urn:n> <urn:p> <urn:o> .}");
            var c=test("SELECT (COUNT(*) AS ?c) WHERE {GRAPH <urn:g> {?s ?p ?o}}",count(1));
            c.withArray("fixtures").addObject().put("path",CorpusTest.ROOT.relativize(d).toString()).put("base","http://example/data.trig").putNull("graph");CorpusTest.execute(c);
        } finally {try(var walk=Files.walk(dir)) {for(Path p:walk.sorted(Comparator.reverseOrder()).toList()) Files.delete(p);}}
    }
    @Test void corruptQueryHashIsRejected() throws Exception {var c=test("ASK {}","{\"kind\":\"boolean\",\"value\":true}");c.put("querySha256","bad");assertThrows(AssertionError.class,()->CorpusTest.execute(c));}
}
