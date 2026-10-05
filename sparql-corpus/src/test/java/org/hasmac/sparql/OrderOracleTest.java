package org.hasmac.sparql;
import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.*;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.junit.jupiter.api.Test;
class OrderOracleTest {
    static final ValueFactory V=SimpleValueFactory.getInstance();
    static JsonNode keys(boolean asc)throws Exception{return CorpusTest.JSON.readTree("[{\"variable\":\"x\",\"ascending\":"+asc+"}]");}
    static Map<String,Value> row(Value v){return Map.of("x",v);}
    @Test void ascending()throws Exception{OrderOracle.verify(List.of("x"),List.of(row(V.createLiteral(1)),row(V.createLiteral(2))),keys(true));}
    @Test void descending()throws Exception{OrderOracle.verify(List.of("x"),List.of(row(V.createLiteral(2)),row(V.createLiteral(1))),keys(false));}
    @Test void reversedFails()throws Exception{assertThrows(AssertionError.class,()->OrderOracle.verify(List.of("x"),List.of(row(V.createLiteral(2)),row(V.createLiteral(1))),keys(true)));}
    @Test void tiesAllowed()throws Exception{OrderOracle.verify(List.of("x"),List.of(row(V.createLiteral(1)),row(V.createLiteral(1))),keys(true));}
    @Test void allPairsCatchIncomparableBridge()throws Exception{assertThrows(AssertionError.class,()->OrderOracle.verify(List.of("x"),List.of(row(V.createLiteral(2)),row(V.createLiteral("language","en")),row(V.createLiteral(1))),keys(true)));}
    @Test void incomparableLanguageLiteralsAllowed()throws Exception{OrderOracle.verify(List.of("x"),List.of(row(V.createLiteral("z","en")),row(V.createLiteral("a","en"))),keys(true));}
    @Test void distinctBlankNodesNotOrdered(){assertNull(OrderOracle.values(V.createBNode("b"),V.createBNode("a")));}
    @Test void zeroSignsCompareEqual(){assertEquals(Integer.valueOf(0),OrderOracle.values(V.createLiteral(-0.0),V.createLiteral(0.0)));}
    @Test void unicodeCodepointOrder(){assertTrue(OrderOracle.codepoints("\uE000","\uD800\uDC00")<0);}
    @Test void mixedNumericPromotion(){assertTrue(OrderOracle.values(V.createLiteral(2),V.createLiteral(3.0))<0);}
    @Test void unboundIsLowest(){assertTrue(OrderOracle.values(null,V.createBNode("b"))<0);}
    @Test void projectionChecked()throws Exception{assertThrows(org.opentest4j.TestAbortedException.class,()->OrderOracle.verify(List.of("y"),List.of(),keys(true)));}
    @Test void onlyIdenticalTermsRequireSecondaryKey()throws Exception{
        var keys=CorpusTest.JSON.readTree("[{\"variable\":\"x\"},{\"variable\":\"y\"}]");
        Map<String,Value> a=Map.of("x",V.createLiteral("01",V.createIRI("http://www.w3.org/2001/XMLSchema#integer")),"y",V.createLiteral(9));
        Map<String,Value> b=Map.of("x",V.createLiteral(1),"y",V.createLiteral(1));
        assertNull(OrderOracle.compareRows(a,b,keys));
    }
    @Test void sameTermUsesSecondaryKey()throws Exception{
        var keys=CorpusTest.JSON.readTree("[{\"variable\":\"x\"},{\"variable\":\"y\"}]");
        Map<String,Value> a=Map.of("x",V.createLiteral(1),"y",V.createLiteral(9)),b=Map.of("x",V.createLiteral(1),"y",V.createLiteral(1));
        assertTrue(OrderOracle.compareRows(a,b,keys)>0);
    }
}
