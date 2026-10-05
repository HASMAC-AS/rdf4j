package org.hasmac.sparql;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;
import com.fasterxml.jackson.databind.*;
import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.*;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.query.*;
import org.eclipse.rdf4j.query.impl.SimpleDataset;
import org.eclipse.rdf4j.query.impl.TupleQueryResultBuilder;
import org.eclipse.rdf4j.query.parser.QueryParserUtil;
import org.eclipse.rdf4j.query.resultio.*;
import org.eclipse.rdf4j.repository.*;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.*;
import org.eclipse.rdf4j.rio.helpers.BasicParserSettings;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.jupiter.api.*;

public class CorpusTest {
    static final ObjectMapper JSON=new ObjectMapper();
    static final ValueFactory VF=SimpleValueFactory.getInstance();
    static final String VIRTUAL="https://corpus.invalid/";
    static final IRI DEFAULT=VF.createIRI("urn:corpus:fixture:default");
    static final Path CORPUS=Path.of(System.getProperty("corpus.dir","corpus")).toAbsolutePath().normalize();
    static final Path ROOT=CORPUS.getParent();
    static final int MAX_ROWS=Integer.getInteger("suite.maxRows",500_000);
    static final long MAX_FIXTURE_BYTES=Long.getLong("suite.maxFixtureBytes",20_000_000L);
    static final Path CASE_REPORT=ROOT.resolve("reports/per-case-results.jsonl");

    @TestFactory Stream<DynamicTest> importedCases() throws IOException {
        Path file=CORPUS.resolve("cases.json");
        assertTrue(Files.isRegularFile(file),"Missing corpus/cases.json; run python3 tools/rebuild.py --download");
        List<JsonNode> all=new ArrayList<>();JSON.readTree(file.toFile()).forEach(all::add);
        Pattern filter=Pattern.compile(System.getProperty("suite.filter",".*"));
        List<JsonNode> selected=all.stream().filter(c->filter.matcher(c.path("family").asText()+" / "+c.path("name").asText()+" "+c.path("id").asText()).find()).toList();
        assertFalse(selected.isEmpty(),"The filter matched no cases");
        Files.createDirectories(CASE_REPORT.getParent());Files.writeString(CASE_REPORT,"");
        System.out.println("CORPUS_DISCOVERY total="+all.size()+" selected="+selected.size());
        return selected.stream().map(c->DynamicTest.dynamicTest(c.path("id").asText()+" | "+c.path("family").asText()+" | "+c.path("name").asText(),()->measured(c)));
    }
    static void measured(JsonNode c) throws Throwable {
        long start=System.nanoTime();String status="passed";Throwable problem=null;
        try {execute(c);} catch(Throwable error) {
            problem=error;status=error instanceof org.opentest4j.TestAbortedException?"skipped":error instanceof AssertionError?"failed":"error";throw error;
        } finally {
            var row=JSON.createObjectNode();row.put("id",c.path("id").asText());row.put("name",c.path("name").asText());row.put("family",c.path("family").asText());row.put("status",status);row.put("seconds",(System.nanoTime()-start)/1e9);row.put("source",c.path("source").path("url").asText());
            if(problem!=null) {row.put("exception",problem.getClass().getName());row.put("message",problem.getMessage());}
            synchronized(CorpusTest.class) {Files.writeString(CASE_REPORT,JSON.writeValueAsString(row)+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);}
        }
    }
    static void prerequisite(boolean satisfied,String reason) {
        if(Boolean.getBoolean("suite.strict")) assertTrue(satisfied,reason);else assumeTrue(satisfied,reason);
    }
    static Repository newRepository() throws Exception {
        String factory=System.getProperty("repository.factory","");
        return factory.isBlank()?new SailRepository(new MemoryStore()):((RepositoryFactory)Class.forName(factory).getDeclaredConstructor().newInstance()).create();
    }
    static void execute(JsonNode c) throws Exception {
        prerequisite(c.path("status").asText().equals("ready"),"Blocked prerequisite: "+c.path("limitations"));
        for(JsonNode fixture:c.path("fixtures")) {Path f=assetPath(fixture);prerequisite(Files.size(f)<=MAX_FIXTURE_BYTES,"Fixture exceeds suite.maxFixtureBytes="+MAX_FIXTURE_BYTES+": "+f);}
        String query=c.path("query").asText();
        assertEquals(c.path("querySha256").asText(),hex(MessageDigest.getInstance("SHA-256").digest(query.getBytes(StandardCharsets.UTF_8))),"query content integrity");
        Repository repository=newRepository();
        try {
            repository.init();
            try(RepositoryConnection con=repository.getConnection()) {
                con.getParserConfig().set(BasicParserSettings.VERIFY_DATATYPE_VALUES,false);
                con.getParserConfig().set(BasicParserSettings.VERIFY_LANGUAGE_TAGS,false);
                String kind=c.path("kind").asText(),base=c.path("base").asText();
                if(kind.equals("syntax-negative")) {assertThrows(MalformedQueryException.class,()->con.prepareQuery(QueryLanguage.SPARQL,query,base));return;}
                Query prepared=con.prepareQuery(QueryLanguage.SPARQL,query,base);
                if(kind.equals("syntax-positive")) return;
                Set<IRI> names=new LinkedHashSet<>();con.begin();
                try {
                    for(JsonNode fixture:c.path("fixtures")) loadFixture(con,fixture,names);
                    // getDataset() only returns an API override. FROM/FROM NAMED live in the parsed query.
                    Dataset explicit=QueryParserUtil.parseQuery(QueryLanguage.SPARQL,query,base).getDataset();
                    if(explicit!=null) {
                        Set<IRI> requested=new LinkedHashSet<>(explicit.getDefaultGraphs());requested.addAll(explicit.getNamedGraphs());
                        for(IRI iri:requested) {
                            if(names.contains(iri)) continue;
                            prerequisite(iri.stringValue().startsWith(VIRTUAL),"FROM requires an unprovided external graph: "+iri);
                            Path file=virtualPath(iri.stringValue());prerequisite(Files.isRegularFile(file),"FROM fixture unavailable: "+iri);
                            prerequisite(Files.size(file)<=MAX_FIXTURE_BYTES,"FROM fixture exceeds suite.maxFixtureBytes");
                            try(InputStream in=Files.newInputStream(file)) {con.add(in,iri.stringValue(),format(file),iri);}names.add(iri);
                        }
                        prepared.setDataset(explicit);
                    } else {
                        SimpleDataset dataset=new SimpleDataset();dataset.addDefaultGraph(DEFAULT);
                        names.forEach(dataset::addNamedGraph);prepared.setDataset(dataset);
                    }
                    con.commit();
                } catch(Throwable error) {if(con.isActive()) con.rollback();throw error;}
                prepared.setIncludeInferred(false);prepared.setMaxExecutionTime(Integer.getInteger("suite.timeoutSeconds",15));
                JsonNode expected=c.path("expected");String eKind=expected.path("kind").asText();
                if(prepared instanceof BooleanQuery ask) {assertEquals("boolean",eKind,"upstream result kind");assertEquals(expected.path("value").asBoolean(),ask.evaluate());}
                else if(prepared instanceof TupleQuery tuple) {
                    List<Map<String,Value>> actual=new ArrayList<>();List<String> vars;boolean qlever=eKind.equals("qlever-checks");
                    try(TupleQueryResult result=tuple.evaluate()) {
                        vars=List.copyOf(result.getBindingNames());
                        while(result.hasNext() && (!qlever || actual.size()<5000)) {assertTrue(actual.size()<MAX_ROWS,"result exceeds suite.maxRows; no truncated result is accepted");actual.add(ResultOracle.row(result.next()));}
                    }
                    if(qlever) checkQlever(expected.path("checks"),vars,actual);
                    else {
                        ExpectedTuple gold=readTuple(expected);
                        if(c.path("laxCardinality").asBoolean(false)) ReducedOracle.tuples(gold.variables(),gold.rows(),vars,actual);
                        else ResultOracle.tuples(gold.variables(),gold.rows(),vars,actual,expected.path("ordered").asBoolean(false));
                        if(c.has("orderBy")) OrderOracle.verify(vars,actual,c.get("orderBy"));
                    }
                } else if(prepared instanceof GraphQuery graph) {
                    assertEquals("graph",eKind,"upstream result kind");Model gold=readModel(expected.path("asset")),actual=new LinkedHashModel();
                    try(GraphQueryResult result=graph.evaluate()) {while(result.hasNext()) {assertTrue(actual.size()<MAX_ROWS,"graph exceeds suite.maxRows");actual.add(result.next());}}
                    assertTrue(Models.isomorphic(gold,actual),"graph results are not isomorphic");
                } else fail("Unsupported query kind: "+prepared.getClass());
            }
        } finally {repository.shutDown();}
    }
    record ExpectedTuple(List<String> variables,List<Map<String,Value>> rows) {}
    static ExpectedTuple readTuple(JsonNode gold) throws Exception {
        if(gold.path("kind").asText().equals("tuple-file")) {
            Path path=assetPath(gold.path("asset"));var candidate=QueryResultIO.getParserFormatForFileName(path.toString());
            prerequisite(candidate.isPresent(),"Unsupported tuple format: "+path);QueryResultFormat fmt=candidate.orElseThrow();
            TupleQueryResultBuilder builder=new TupleQueryResultBuilder();
            TupleQueryResultParser parser=QueryResultIO.createTupleParser(fmt);parser.setQueryResultHandler(builder);
            try(InputStream in=Files.newInputStream(path)) {parser.parseQueryResult(in);}
            List<Map<String,Value>> rows=new ArrayList<>();List<String> variables;
            try(TupleQueryResult result=builder.getQueryResult()) {
                variables=List.copyOf(result.getBindingNames());while(result.hasNext()) rows.add(ResultOracle.row(result.next()));
            }
            return new ExpectedTuple(variables,rows);
        }
        assertEquals("tuple",gold.path("kind").asText(),"expected result format");
        List<String> variables=new ArrayList<>();gold.path("vars").forEach(x->variables.add(x.asText()));List<Map<String,Value>> rows=new ArrayList<>();
        for(JsonNode row:gold.path("rows")) {Map<String,Value> result=new TreeMap<>();row.fields().forEachRemaining(e->result.put(e.getKey(),value(e.getValue())));rows.add(Map.copyOf(result));}
        return new ExpectedTuple(variables,rows);
    }
    static Value value(JsonNode term) {
        String type=term.path("type").asText(),lexical=term.path("value").asText();
        return switch(type) {
            case "uri"->VF.createIRI(lexical);
            case "bnode"->VF.createBNode(lexical);
            case "literal","typed-literal"->term.has("xml:lang")?VF.createLiteral(lexical,term.get("xml:lang").asText()):VF.createLiteral(lexical,VF.createIRI(term.path("datatype").asText("http://www.w3.org/2001/XMLSchema#string")));
            default->throw new IllegalArgumentException("Unsupported result term: "+term);
        };
    }
    static Path virtualPath(String uri) {Path path=ROOT.resolve(URI.create(uri).getPath().substring(1)).normalize();if(!path.startsWith(ROOT)) throw new IllegalArgumentException("Path escapes source root: "+uri);return path;}
    static Path assetPath(JsonNode asset) throws Exception {
        prerequisite(asset.has("path"),"missing asset: "+asset);Path path=ROOT.resolve(asset.path("path").asText()).normalize();assertTrue(path.startsWith(ROOT),"asset path escapes root");prerequisite(Files.isRegularFile(path),"missing asset file: "+path);
        if(asset.has("sha256")) {MessageDigest hash=MessageDigest.getInstance("SHA-256");try(InputStream in=Files.newInputStream(path)) {byte[] buffer=new byte[65536];for(int n;(n=in.read(buffer))>=0;) hash.update(buffer,0,n);}assertEquals(asset.get("sha256").asText(),hex(hash.digest()),"asset SHA-256: "+path);}return path;
    }
    static String hex(byte[] data) {return HexFormat.of().formatHex(data);}
    static RDFFormat format(Path path) {if(path.toString().endsWith(".n3")) return RDFFormat.TURTLE;return Rio.getParserFormatForFileName(path.toString()).orElseThrow(()->new IllegalArgumentException("Unsupported RDF syntax: "+path));}
    static Model readModel(JsonNode asset) throws Exception {
        Path path=assetPath(asset);RDFParser parser=Rio.createParser(format(path));parser.getParserConfig().set(BasicParserSettings.VERIFY_DATATYPE_VALUES,false);parser.getParserConfig().set(BasicParserSettings.VERIFY_LANGUAGE_TAGS,false);
        Model model=new LinkedHashModel();parser.setRDFHandler(new org.eclipse.rdf4j.rio.helpers.StatementCollector(model));
        try(InputStream in=Files.newInputStream(path)) {parser.parse(in,asset.path("base").asText());}return model;
    }
    static void loadFixture(RepositoryConnection con,JsonNode fixture,Set<IRI> names) throws Exception {
        Path path=assetPath(fixture);String base=fixture.path("base").asText();
        if(!fixture.path("graph").isNull() && !fixture.path("graph").isMissingNode()) {IRI graph=VF.createIRI(fixture.get("graph").asText());names.add(graph);try(InputStream in=Files.newInputStream(path)) {con.add(in,base,format(path),graph);}}
        else if(format(path).supportsContexts()) {Model data=readModel(fixture);for(Statement st:data) {Resource context=st.getContext()==null?DEFAULT:st.getContext();if(st.getContext() instanceof IRI name) names.add(name);con.add(st.getSubject(),st.getPredicate(),st.getObject(),context);}}
        else {try(InputStream in=Files.newInputStream(path)) {con.add(in,base,format(path),DEFAULT);}}
    }
    static void checkQlever(JsonNode checks,List<String> vars,List<Map<String,Value>> rows) {
        for(JsonNode check:checks) check.fields().forEachRemaining(entry->{JsonNode gold=entry.getValue();switch(entry.getKey()) {
            case "num_rows"->assertEquals(gold.asInt(),rows.size(),"QLever num_rows");
            case "num_cols"->assertEquals(gold.asInt(),vars.size(),"QLever num_cols");
            case "selected"->{List<String> expected=new ArrayList<>();gold.forEach(x->expected.add(x.asText().replaceFirst("^\\?","")));assertEquals(expected,vars,"QLever selected variables");}
            case "res"->{assertEquals(gold.size(),rows.size(),"QLever full result cardinality");for(int i=0;i<gold.size();i++) assertTrue(qleverRow(gold.get(i),vars,rows.get(i)),"QLever res at row "+i+": "+rows.get(i));}
            case "contains_row"->assertTrue(rows.stream().anyMatch(row->qleverRow(gold,vars,row)),"QLever contains_row: "+gold);
            case "order_numeric"->{String name=gold.path("var").asText().replaceFirst("^\\?","");boolean ascending=gold.path("dir").asText().equalsIgnoreCase("asc");for(int i=1;i<rows.size();i++) {assertInstanceOf(Literal.class,rows.get(i-1).get(name));assertInstanceOf(Literal.class,rows.get(i).get(name));double a=((Literal)rows.get(i-1).get(name)).doubleValue(),b=((Literal)rows.get(i).get(name)).doubleValue();assertTrue(ascending?a<=b:a>=b,"QLever numeric order on "+name);}}
            default->fail("Unported QLever assertion: "+entry.getKey());
        }});
    }
    static boolean qleverRow(JsonNode gold,List<String> vars,Map<String,Value> row) {
        if(gold.size()!=vars.size()) return false;
        for(int i=0;i<gold.size();i++) {
            JsonNode cell=gold.get(i);if(cell.isNull()) continue;Value actual=row.get(vars.get(i));if(actual==null) return false;
            if(cell.isNumber()) {if(!(actual instanceof Literal literal)) return false;try {if(cell.isIntegralNumber()) {if(literal.integerValue().compareTo(cell.bigIntegerValue())!=0) return false;}else if(!(Math.abs(literal.doubleValue()-cell.doubleValue())<=0.1)) return false;}catch(RuntimeException error) {return false;}}
            else if(cell.isBoolean()) {if(!(actual instanceof Literal literal) || !literal.getLabel().equals(cell.asText())) return false;}
            else {String text=cell.asText();if(text.startsWith("<") && text.endsWith(">")) {if(!(actual instanceof IRI) || !actual.stringValue().equals(text.substring(1,text.length()-1))) return false;}else if(!(actual instanceof Literal literal) || !literal.getLabel().equals(text)) return false;}
        }
        return true;
    }
}
