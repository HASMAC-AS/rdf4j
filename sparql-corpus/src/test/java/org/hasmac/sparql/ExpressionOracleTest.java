package org.hasmac.sparql;

import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.*;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.junit.jupiter.api.Test;

class ExpressionOracleTest {
    private static final ObjectMapper J=new ObjectMapper();private static final ValueFactory V=SimpleValueFactory.getInstance();
    private static final String X="http://www.w3.org/2001/XMLSchema#";
    private static Literal l(String label,String datatype){return V.createLiteral(label,V.createIRI(X+datatype));}
    private static ObjectNode expected(String mode,String label,String datatype){ObjectNode e=J.createObjectNode().put("mode",mode);e.putObject("term").put("type","literal").put("value",label).put("datatype",X+datatype);return e;}
    private static void check(JsonNode e,Value a){ExpressionOracle.verify(e,List.of("result"),List.of(Map.of("result",a)));}
    @Test void exactTermRetainsLexicalForm(){assertThrows(AssertionError.class,()->check(expected("term","01","integer"),l("1","integer")));}
    @Test void valueDatatypeAllowsNumericLexicalVariants(){check(expected("value-datatype","01","integer"),l("1","integer"));}
    @Test void valueDatatypeRejectsDifferentDatatype(){assertThrows(AssertionError.class,()->check(expected("value-datatype","1","integer"),l("1.0","decimal")));}
    @Test void negativeZeroHasSameNumericValue(){check(expected("value-datatype","-0.0","double"),l("0.0","double"));}
    @Test void nanIsNotNumericEquality(){assertThrows(AssertionError.class,()->check(expected("value-datatype","NaN","double"),l("NaN","double")));}
    @Test void exactNanWorks(){check(expected("term","NaN","double"),l("NaN","double"));}
    @Test void toleranceRetainsOriginalBound(){ObjectNode e=J.createObjectNode().put("mode","double-tolerance").put("number","1.0").put("delta","0.1");check(e,l("1.05","double"));assertThrows(AssertionError.class,()->check(e,l("1.2","double")));}
    @Test void toleranceRequiresDouble(){ObjectNode e=J.createObjectNode().put("mode","double-tolerance").put("number","1.0").put("delta","0.1");assertThrows(AssertionError.class,()->check(e,l("1.0","decimal")));}
    @Test void originalInfinityCheckDoesNotCheckSign(){ObjectNode e=J.createObjectNode().put("mode","double-tolerance").put("number","Infinity").put("delta","0");check(e,l("-INF","double"));}
    @Test void doubleNodeRetainsEarlyCrossDatatypeEquality(){ObjectNode e=expected("double-node","1","integer").put("number","1.0").put("delta","0.001");check(e,l("1.0","decimal"));}
    @Test void booleanValueComparison(){check(expected("value-datatype","1","boolean"),l("true","boolean"));}
    @Test void dateTimeTimezoneValueComparison(){check(expected("value-datatype","2000-01-01T01:00:00+01:00","dateTime"),l("2000-01-01T00:00:00Z","dateTime"));}
    @Test void unknownDatatypeDoesNotNormalize(){assertThrows(AssertionError.class,()->check(expected("value-datatype","01","custom"),l("1","custom")));}
    @Test void expressionErrorIsOneUnboundMapping(){ObjectNode e=J.createObjectNode().put("mode","unbound");ExpressionOracle.verify(e,List.of("result"),List.of(Map.of()));assertThrows(AssertionError.class,()->ExpressionOracle.verify(e,List.of("result"),List.of()));}
    @Test void propertyOracleRejectsWrongKind(){ObjectNode e=J.createObjectNode().put("mode","predicate").put("predicate","isIRI");check(e,V.createIRI("urn:x"));assertThrows(AssertionError.class,()->check(e,V.createLiteral("urn:x")));}
    @Test void decimalCategoryIncludesIntegers(){assertTrue(ExpressionOracle.predicate("isDecimal",l("1","integer")));assertFalse(ExpressionOracle.predicate("isDecimal",l("1","double")));}
    @Test void directedLiteralPreservesDirection(){ObjectNode t=J.createObjectNode().put("type","literal").put("value","hello").put("xml:lang","en").put("direction","ltr");Literal v=(Literal)ExpressionOracle.value(t);assertEquals(Literal.BaseDirection.LTR,v.getBaseDirection());}
    @Test void nestedTripleTermMaterializes(){ObjectNode t=J.createObjectNode().put("type","triple");ObjectNode fields=t.putObject("value");fields.putObject("subject").put("type","uri").put("value","urn:s");fields.putObject("predicate").put("type","uri").put("value","urn:p");fields.putObject("object").put("type","literal").put("value","x");assertInstanceOf(TripleTerm.class,ExpressionOracle.value(t));}
    @Test void projectionMismatchFails(){assertThrows(AssertionError.class,()->ExpressionOracle.verify(expected("term","1","integer"),List.of("x"),List.of(Map.of("result",l("1","integer")))));}
    @Test void duplicateInputMappingsAreNotAccepted(){assertThrows(AssertionError.class,()->ExpressionOracle.verify(expected("term","1","integer"),List.of("result"),List.of(Map.of("result",l("1","integer")),Map.of("result",l("1","integer")))));}
}
