package org.hasmac.testing;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.hasmac.testing.CorpusIO.Table;
import org.junit.jupiter.api.Test;

final class ResultOracleTest {
    private static final ValueFactory V = CorpusIO.VF;
    @SafeVarargs private static Table table(Map<String,Value>... rows) { return new Table(List.of("x", "y"), List.of(rows)); }
    private static Map<String,Value> r(Value x, Value y) { Map<String,Value> m = new HashMap<>(); if (x != null) m.put("x",x); if (y != null) m.put("y",y); return m; }
    private static void compare(Table a, Table b) { ResultOracle.compare(a,b,false,List.of(),false); }
    @Test void duplicateRowsAreNotSets() { var row=r(V.createLiteral(1),null); assertThrows(AssertionError.class,()->compare(table(row,row),table(row))); }
    @Test void duplicateDistributionMatters() { var a=r(V.createLiteral(1),null);var b=r(V.createLiteral(2),null);assertThrows(AssertionError.class,()->compare(table(a,a,b),table(a,b,b))); }
    @Test void unorderedRowsMayMove() { var a=r(V.createLiteral(1),null);var b=r(V.createLiteral(2),null);compare(table(a,b,a),table(b,a,a)); }
    @Test void emptyBindingsAreRows() { assertThrows(AssertionError.class,()->compare(table(r(null,null)),table())); }
    @Test void unboundIsNotEmptyString() { assertThrows(AssertionError.class,()->compare(table(r(null,null)),table(r(V.createLiteral(""),null)))); }
    @Test void datatypeMatters() { assertThrows(AssertionError.class,()->compare(table(r(V.createLiteral("1",XSD.INTEGER),null)),table(r(V.createLiteral("1",XSD.STRING),null)))); }
    @Test void lexicalFormMatters() { assertThrows(AssertionError.class,()->compare(table(r(V.createLiteral("01",XSD.INTEGER),null)),table(r(V.createLiteral("1",XSD.INTEGER),null)))); }
    @Test void blankLabelsMayChange() { compare(table(r(V.createBNode("a"),null)),table(r(V.createBNode("z"),null))); }
    @Test void blankIdentitySpansRows() { var a=V.createBNode("a");var z=V.createBNode("z");compare(table(r(a,V.createLiteral(1)),r(a,V.createLiteral(2))),table(r(z,V.createLiteral(2)),r(z,V.createLiteral(1)))); }
    @Test void inconsistentBlankIdentityFails() { var a=V.createBNode("a");assertThrows(AssertionError.class,()->compare(table(r(a,V.createLiteral(1)),r(a,V.createLiteral(2))),table(r(V.createBNode("z"),V.createLiteral(1)),r(V.createBNode("w"),V.createLiteral(2))))); }
    @Test void blankMappingMustBeInjective() { assertThrows(AssertionError.class,()->compare(table(r(V.createBNode("a"),V.createBNode("b"))),table(r(V.createBNode("z"),V.createBNode("z"))))); }
    @Test void orderedResultsRejectSwapping() { var a=r(V.createLiteral(1),null);var b=r(V.createLiteral(2),null);assertThrows(AssertionError.class,()->ResultOracle.compare(table(a,b),table(b,a),true,List.of(),false)); }
    @Test void orderedTiesMayPermute() { var a=r(V.createLiteral(1),V.createLiteral("a"));var b=r(V.createLiteral(1),V.createLiteral("b"));ResultOracle.compare(table(a,b),table(b,a),true,List.of("x"),false); }
    @Test void blankIdentitySpansTieGroups() { var a=V.createBNode("a");var z=V.createBNode("z");var w=V.createBNode("w");assertThrows(AssertionError.class,()->ResultOracle.compare(table(r(V.createLiteral(1),a),r(V.createLiteral(2),a)),table(r(V.createLiteral(1),z),r(V.createLiteral(2),w)),true,List.of("x"),false)); }
    @Test void reducedAcceptsOneToOriginalCount() {var row=r(V.createLiteral(1),null);ResultOracle.compare(table(row,row,row),table(row,row),false,List.of(),true);}
    @Test void reducedRejectsExcessCount() {var row=r(V.createLiteral(1),null);assertThrows(AssertionError.class,()->ResultOracle.compare(table(row),table(row,row),false,List.of(),true));}
    @Test void reducedRejectsMissingValue() {var row=r(V.createLiteral(1),null);assertThrows(AssertionError.class,()->ResultOracle.compare(table(row),table(),false,List.of(),true));}
    @Test void tripleTermBlankIdentity() {IRI p=V.createIRI("urn:p");var a=V.createBNode("a");var b=V.createBNode("b");compare(table(r(V.createTripleTerm(a,p,V.createLiteral(1)),a)),table(r(V.createTripleTerm(b,p,V.createLiteral(1)),b)));}
    @Test void headerOnlyVariableIsRetained() {assertThrows(AssertionError.class,()->compare(new Table(List.of("x"),List.of(Map.of())),new Table(List.of("y"),List.of(Map.of()))));}
    @Test void ignoresSubqueryOrder() {assertFalse(CorpusTest.hasTopOrder("SELECT * WHERE { { SELECT * WHERE {} ORDER BY ?x } }"));}
    @Test void recognizesTopOrder() {assertTrue(CorpusTest.hasTopOrder("SELECT ?x WHERE {} ORDER BY ?x"));}
    @Test void ignoresOrderInsideLiteralAndComment() {assertFalse(CorpusTest.hasTopOrder("SELECT ('ORDER BY ?x' AS ?x) WHERE {} # ORDER BY ?x"));}
}
