package org.hasmac.testing;

import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.hasmac.testing.CorpusIO.Table;

/** Mirrors the reviewed Jena TestExpressions helper predicates, not the query evaluator. */
final class NativeAssertionOracle {
    private static final Set<String> INTEGERS = Set.of("integer", "long", "int", "short", "byte", "nonPositiveInteger", "negativeInteger", "nonNegativeInteger", "positiveInteger", "unsignedLong", "unsignedInt", "unsignedShort", "unsignedByte");

    static void compare(JsonNode expected, Table actual) {
        String variable = expected.path("variable").asText("result");
        assertEquals(List.of(variable), actual.variables(), "Native assertion projection");
        assertEquals(1, actual.rows().size(), "Native expression has one solution");
        Value value = actual.rows().getFirst().get(variable);
        assertNotNull(value, "Native expression must evaluate successfully: " + expected);
        String kind = expected.path("assertion").asText();
        JsonNode reference = expected.get("value");
        if ("bound".equals(kind)) return;
        if ("iri".equals(kind)) {
            assertInstanceOf(IRI.class, value); assertEquals(reference.asText(), value.stringValue()); return;
        }
        Literal literal = assertInstanceOf(Literal.class, value);
        String local = literal.getDatatype().getLocalName();
        boolean integer = literal.getDatatype().getNamespace().equals(XSD.NAMESPACE) && INTEGERS.contains(local);
        switch (kind) {
            case "boolean" -> { assertEquals(XSD.BOOLEAN, literal.getDatatype()); assertEquals(reference.asBoolean(), literal.booleanValue()); }
            case "string" -> { assertEquals(XSD.STRING, literal.getDatatype()); if (reference != null && !reference.isNull()) assertEquals(reference.asText(), literal.getLabel()); }
            case "integer" -> { assertTrue(integer, "Jena isInteger category"); assertEquals(new BigInteger(reference.asText()), literal.integerValue()); }
            case "integer32" -> { assertTrue(integer, "Jena isInteger category"); assertEquals(new BigInteger(reference.asText()).intValue(), literal.integerValue().intValue()); }
            case "integer64" -> { assertTrue(integer, "Jena isInteger category"); assertEquals(new BigInteger(reference.asText()).longValue(), literal.integerValue().longValue()); }
            case "decimal-scale" -> { assertTrue(integer || literal.getDatatype().equals(XSD.DECIMAL), "Jena isDecimal category"); assertEquals(new BigDecimal(reference.asText()), literal.decimalValue()); }
            case "numeric-double" -> {
                assertTrue(integer || Set.of(XSD.DECIMAL, XSD.FLOAT, XSD.DOUBLE).contains(literal.getDatatype()), "Jena isDouble conversion category");
                assertEquals(Double.parseDouble(reference.asText()), literal.doubleValue(), 0.0);
            }
            default -> throw new IllegalArgumentException("Unknown native helper assertion: " + kind);
        }
    }
}
