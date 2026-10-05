package org.hasmac.sparql;

import com.fasterxml.jackson.databind.node.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import org.junit.jupiter.api.*;

/** Exercises the actual corpus executor, not a separately reimplemented fixture loader. */
class FixtureDatasetTest {
    Path directory;
    @BeforeEach void setUp() throws Exception {
        Path target=CorpusTest.ROOT.resolve("target"); Files.createDirectories(target);
        directory=Files.createTempDirectory(target,"dataset-calibration-");
    }
    @AfterEach void tearDown() throws Exception {
        if(directory!=null) try(var paths=Files.walk(directory)) {
            for(Path p:paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(p);
        }
    }
    ObjectNode fixture(String name,String data,String graph) throws Exception {
        Path file=directory.resolve(name); Files.writeString(file,data,StandardCharsets.UTF_8);
        String rel=CorpusTest.ROOT.relativize(file).toString().replace('\\','/');
        ObjectNode a=CorpusTest.JSON.createObjectNode().put("path",rel).put("base",CorpusTest.VIRTUAL+rel)
                .put("sha256",hash(data));
        if(graph==null) a.putNull("graph"); else a.put("graph",graph);
        return a;
    }
    String iri(ObjectNode fixture) { return '<'+fixture.path("base").asText()+'>'; }
    static String hash(String text) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));
    }
    void run(String query,List<ObjectNode> fixtures,String[] vars,String[][] values) throws Exception {
        ObjectNode c=CorpusTest.JSON.createObjectNode().put("id","fixture-calibration")
                .put("name","fixture-calibration").put("status","ready").put("kind","evaluation")
                .put("base","http://base/").put("query",query).put("querySha256",hash(query));
        ArrayNode f=c.putArray("fixtures"); fixtures.forEach(f::add);
        ObjectNode expected=c.putObject("expected").put("kind","tuple");
        ArrayNode names=expected.putArray("vars"); for(String v:vars) names.add(v);
        ArrayNode rows=expected.putArray("rows");
        for(String[] row:values) {
            ObjectNode b=rows.addObject();
            for(int i=0;i<vars.length;i++) if(row[i]!=null) b.putObject(vars[i]).put("type","uri").put("value",row[i]);
        }
        CorpusTest.execute(c);
    }
    @Test void fromLoadsItsLocalDocument() throws Exception {
        ObjectNode data=fixture("from.ttl","<urn:s> <urn:p> <urn:o> .",null);
        run("SELECT ?s FROM "+iri(data)+" WHERE { ?s ?p ?o }",List.of(),new String[]{"s"},new String[][]{{"urn:s"}});
    }
    @Test void fromOverridesManifestDefaultGraph() throws Exception {
        ObjectNode selected=fixture("selected.ttl","<urn:selected> <urn:p> <urn:o> .",null);
        ObjectNode other=fixture("other.ttl","<urn:other> <urn:p> <urn:o> .",null);
        run("SELECT ?s FROM "+iri(selected)+" WHERE { ?s ?p ?o }",List.of(other),new String[]{"s"},new String[][]{{"urn:selected"}});
    }
    @Test void fromNamedDoesNotPopulateDefaultGraph() throws Exception {
        ObjectNode named=fixture("named.ttl","<urn:s> <urn:p> <urn:o> .",null);
        run("SELECT ?s FROM NAMED "+iri(named)+" WHERE { ?s ?p ?o }",List.of(),new String[]{"s"},new String[][]{});
    }
    @Test void fromNamedMakesTheNamedGraphQueryable() throws Exception {
        ObjectNode named=fixture("named.ttl","<urn:s> <urn:p> <urn:o> .",null);
        run("SELECT ?g ?s FROM NAMED "+iri(named)+" WHERE { GRAPH ?g { ?s ?p ?o } }",List.of(),new String[]{"g","s"},new String[][]{{named.path("base").asText(),"urn:s"}});
    }
    @Test void plainQueryDoesNotUnionNamedGraphsIntoDefault() throws Exception {
        ObjectNode ordinary=fixture("default.ttl","<urn:default> <urn:p> <urn:o> .",null);
        ObjectNode named=fixture("named.ttl","<urn:named> <urn:p> <urn:o> .","urn:g");
        run("SELECT ?s WHERE { ?s ?p ?o }",List.of(ordinary,named),new String[]{"s"},new String[][]{{"urn:default"}});
    }
    @Test void defaultGraphIsNotExposedByGraphVariable() throws Exception {
        ObjectNode ordinary=fixture("default.ttl","<urn:default> <urn:p> <urn:o> .",null);
        ObjectNode named=fixture("named.ttl","<urn:named> <urn:p> <urn:o> .","urn:g");
        run("SELECT ?g ?s WHERE { GRAPH ?g { ?s ?p ?o } }",List.of(ordinary,named),new String[]{"g","s"},new String[][]{{"urn:g","urn:named"}});
    }
    @Test void explicitDefaultOnlyHidesNamedGraphs() throws Exception {
        ObjectNode data=fixture("from.ttl","<urn:s> <urn:p> <urn:o> .",null);
        ObjectNode named=fixture("other.ttl","<urn:named> <urn:p> <urn:o> .","urn:g");
        run("SELECT ?g FROM "+iri(data)+" WHERE { GRAPH ?g { ?s ?p ?o } }",List.of(named),new String[]{"g"},new String[][]{});
    }
    @Test void explicitNamedDatasetDoesNotLeakOtherNamedFixtures() throws Exception {
        ObjectNode data=fixture("selected.ttl","<urn:selected> <urn:p> <urn:o> .",null);
        ObjectNode other=fixture("other.ttl","<urn:other> <urn:p> <urn:o> .","urn:otherGraph");
        run("SELECT ?s FROM NAMED "+iri(data)+" WHERE { GRAPH ?g { ?s ?p ?o } }",List.of(other),new String[]{"s"},new String[][]{{"urn:selected"}});
    }
    @Test void multipleFromClausesMergeTheirDocuments() throws Exception {
        ObjectNode a=fixture("a.ttl","<urn:a> <urn:p> <urn:o> .",null);
        ObjectNode b=fixture("b.ttl","<urn:b> <urn:p> <urn:o> .",null);
        run("SELECT ?s FROM "+iri(a)+" FROM "+iri(b)+" WHERE { ?s ?p ?o }",List.of(),new String[]{"s"},new String[][]{{"urn:a"},{"urn:b"}});
    }
    @Test void duplicateTriplesInFromMergeAreNotDuplicateSolutions() throws Exception {
        ObjectNode a=fixture("a.ttl","<urn:s> <urn:p> <urn:o> .",null);
        ObjectNode b=fixture("b.ttl","<urn:s> <urn:p> <urn:o> .",null);
        run("SELECT DISTINCT ?s FROM "+iri(a)+" FROM "+iri(b)+" WHERE { ?s ?p ?o }",List.of(),new String[]{"s"},new String[][]{{"urn:s"}});
    }
}
