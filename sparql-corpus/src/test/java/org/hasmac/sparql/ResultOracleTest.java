package org.hasmac.sparql;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.junit.jupiter.api.Test;

class ResultOracleTest {
    static final ValueFactory V=SimpleValueFactory.getInstance();
    static Map<String,Value> r(String name,Value value) { return Map.of(name,value); }
    static void compare(List<Map<String,Value>> a,List<Map<String,Value>> b,boolean ordered) {
        ResultOracle.tuples(List.of("x"),a,List.of("x"),b,ordered);
    }
    @Test void bagsIgnoreOrder() { compare(List.of(r("x",V.createLiteral(1)),r("x",V.createLiteral(2))),List.of(r("x",V.createLiteral(2)),r("x",V.createLiteral(1))),false); }
    @Test void sequencesRequireOrder() { assertThrows(AssertionError.class,() -> compare(List.of(r("x",V.createLiteral(1)),r("x",V.createLiteral(2))),List.of(r("x",V.createLiteral(2)),r("x",V.createLiteral(1))),true)); }
    @Test void duplicatesAreNotSets() { assertThrows(AssertionError.class,() -> compare(List.of(r("x",V.createLiteral(1)),r("x",V.createLiteral(1))),List.of(r("x",V.createLiteral(1)),r("x",V.createLiteral(2))),false)); }
    @Test void unboundIsNotEmptyString() { assertThrows(AssertionError.class,() -> compare(List.of(Map.of()),List.of(r("x",V.createLiteral(""))),false)); }
    @Test void emptyMappingDiffersFromEmptySequence() { assertThrows(AssertionError.class,() -> compare(List.of(Map.of()),List.of(),false)); }
    @Test void multipleEmptyMappingsPreserved() { compare(List.of(Map.of(),Map.of()),List.of(Map.of(),Map.of()),false); }
    @Test void blankNodeNamesMayChange() { compare(List.of(r("x",V.createBNode("a"))),List.of(r("x",V.createBNode("z"))),false); }
    @Test void repeatedBlankNodeMustStaySame() { assertThrows(AssertionError.class,() -> compare(List.of(r("x",V.createBNode("a")),r("x",V.createBNode("a"))),List.of(r("x",V.createBNode("z")),r("x",V.createBNode("y"))),false)); }
    @Test void distinctBlankNodesCannotCollapse() { assertThrows(AssertionError.class,() -> compare(List.of(r("x",V.createBNode("a")),r("x",V.createBNode("b"))),List.of(r("x",V.createBNode("z")),r("x",V.createBNode("z"))),false)); }
    @Test void blankNodeMappingSpansRows() {
        List<String> vars=List.of("x","y");
        var a=List.of(Map.of("x",(Value)V.createBNode("a"),"y",V.createLiteral(1)),Map.of("x",(Value)V.createBNode("a"),"y",V.createLiteral(2)));
        var b=List.of(Map.of("x",(Value)V.createBNode("z"),"y",V.createLiteral(2)),Map.of("x",(Value)V.createBNode("z"),"y",V.createLiteral(1)));
        ResultOracle.tuples(vars,a,vars,b,false);
    }
    @Test void iriAndStringDiffer() { assertThrows(AssertionError.class,() -> compare(List.of(r("x",V.createIRI("urn:x"))),List.of(r("x",V.createLiteral("urn:x"))),false)); }
    @Test void numericLexicalFormsAreNotNormalized() { IRI integer=V.createIRI("http://www.w3.org/2001/XMLSchema#integer"); assertThrows(AssertionError.class,() -> compare(List.of(r("x",V.createLiteral("01",integer))),List.of(r("x",V.createLiteral("1",integer))),false)); }
    @Test void datatypeIsPreserved() { assertThrows(AssertionError.class,() -> compare(List.of(r("x",V.createLiteral(1))),List.of(r("x",V.createLiteral("1"))),false)); }
    @Test void languageIsPreserved() { assertThrows(AssertionError.class,() -> compare(List.of(r("x",V.createLiteral("a","en"))),List.of(r("x",V.createLiteral("a","fr"))),false)); }
    @Test void tripleTermsPreserveSharedBlankNodes() {
        var p=V.createIRI("urn:p"); var a=V.createBNode("a"); var b=V.createBNode("b");
        compare(List.of(r("x",V.createTripleTerm(a,p,a))),List.of(r("x",V.createTripleTerm(b,p,b))),false);
    }
    @Test void tripleTermsDoNotCollapseBlankNodes() {
        var p=V.createIRI("urn:p"); var a=V.createBNode("a"); var b=V.createBNode("b");
        assertThrows(AssertionError.class,() -> compare(List.of(r("x",V.createTripleTerm(a,p,a))),List.of(r("x",V.createTripleTerm(a,p,b))),false));
    }
}
