package org.hasmac.sparql;

import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.*;
import javax.xml.datatype.*;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

/** Source-specific expression assertion modes; no SPARQL engine is used as the comparator. */
final class ExpressionOracle {
    private static final ValueFactory VF=SimpleValueFactory.getInstance();
    private static final String XSD="http://www.w3.org/2001/XMLSchema#";
    private static final DatatypeFactory XML=DatatypeFactory.newDefaultInstance();
    private static final Set<String> INTEGERS=Set.of("integer","long","int","short","byte","nonPositiveInteger","negativeInteger","nonNegativeInteger","positiveInteger","unsignedLong","unsignedInt","unsignedShort","unsignedByte");
    private static final Set<String> CALENDARS=Set.of("dateTime","dateTimeStamp","date","time","gYear","gYearMonth","gMonth","gMonthDay","gDay");
    private static final Set<String> FLOATING=Set.of("double","float");
    private static final Set<String> DURATIONS=Set.of("duration","dayTimeDuration","yearMonthDuration");
    private static final Set<String> DATETIMES=Set.of("dateTime","dateTimeStamp");
    private ExpressionOracle() {}
    static Value value(JsonNode term) {
        String type=term.path("type").asText(),lexical=term.path("value").asText();
        return switch(type) {
            case "uri"->VF.createIRI(lexical);
            case "bnode"->VF.createBNode(lexical);
            case "literal","typed-literal"->{
                if(term.has("xml:lang")) {
                    String direction=term.path("direction").asText("").toLowerCase(Locale.ROOT).replace("--","");
                    Literal.BaseDirection dir=direction.isEmpty()?Literal.BaseDirection.NONE:Literal.BaseDirection.valueOf(direction.toUpperCase(Locale.ROOT));
                    yield VF.createLiteral(lexical,term.get("xml:lang").asText(),dir);
                }
                yield VF.createLiteral(lexical,VF.createIRI(term.path("datatype").asText(XSD+"string")));
            }
            case "triple"->{JsonNode t=term.get("value");yield VF.createTripleTerm((Resource)value(t.get("subject")),(IRI)value(t.get("predicate")),value(t.get("object")));}
            default->throw new IllegalArgumentException("Unsupported captured expected term: "+term);
        };
    }
    static String datatype(Value value) {
        if(!(value instanceof Literal l))return "";
        String iri=l.getDatatype().stringValue();return iri.startsWith(XSD)?iri.substring(XSD.length()):iri;
    }
    static boolean integer(Value v) {return v instanceof Literal&&INTEGERS.contains(datatype(v));}
    static boolean decimal(Value v) {return integer(v)||datatype(v).equals("decimal");}
    static boolean numeric(Value v) {return decimal(v)||FLOATING.contains(datatype(v));}
    static boolean sameNumeric(Literal a,Literal b) {
        // Preserve the pinned Jena XSDFuncOp.compareNumeric implementation,
        // including its signed-zero distinction. This is not a generic SPARQL equality policy.
        if(datatype(a).equals("double")||datatype(b).equals("double"))return Double.compare(a.doubleValue(),b.doubleValue())==0;
        if(datatype(a).equals("float")||datatype(b).equals("float"))return Float.compare(a.floatValue(),b.floatValue())==0;
        return a.decimalValue().compareTo(b.decimalValue())==0;
    }
    static boolean sameValue(Value a,Value b) {
        // Match Jena NVCompare's exact-term fast path, except floating NaN.
        // Identical ill-typed RDF literals are still identical terms.
        if(a.equals(b)) {
            if(a instanceof Literal l && FLOATING.contains(datatype(a))) {
                try {return !Double.isNaN(l.doubleValue());}catch(IllegalArgumentException error){return true;}
            }
            return true;
        }
        if(numeric(a)&&numeric(b)) {
            try{return sameNumeric((Literal)a,(Literal)b);}catch(IllegalArgumentException ex){return false;}
        }
        if(a instanceof TripleTerm x && b instanceof TripleTerm y)
            return sameValue(x.getSubject(),y.getSubject())&&sameValue(x.getPredicate(),y.getPredicate())&&sameValue(x.getObject(),y.getObject());
        if(!(a instanceof Literal x)||!(b instanceof Literal y)||!x.getDatatype().equals(y.getDatatype()))return false;
        if(!x.getLanguage().equals(y.getLanguage())||x.getBaseDirection()!=y.getBaseDirection())return false;
        String dt=datatype(a);
        try {
            if(dt.equals("boolean"))return x.booleanValue()==y.booleanValue();
            if(CALENDARS.contains(dt))return XML.newXMLGregorianCalendar(x.getLabel()).compare(XML.newXMLGregorianCalendar(y.getLabel()))==DatatypeConstants.EQUAL;
            if(DURATIONS.contains(dt))return XML.newDuration(x.getLabel()).equals(XML.newDuration(y.getLabel()));
        }catch(IllegalArgumentException ex){return false;}
        return false;
    }
    static void verify(JsonNode expected,List<String> variables,List<Map<String,Value>> rows) {
        assertEquals(Set.of("result"),new HashSet<>(variables),"captured expression projection");
        assertEquals(1,rows.size(),"expression evaluation preserves its single input mapping");
        Value actual=rows.getFirst().get("result");String mode=expected.path("mode").asText();
        if(mode.equals("unbound")){assertNull(actual,"source expects expression error/unbound result");return;}
        assertNotNull(actual,"source expects a bound expression result");
        switch(mode) {
            case "term"->assertEquals(value(expected.get("term")),actual,"original exact RDF-term assertion");
            case "value-datatype"->{
                Value gold=value(expected.get("term"));
                if(gold instanceof Literal l)assertEquals(l.getDatatype(),assertInstanceOf(Literal.class,actual).getDatatype(),"original datatype assertion");
                assertTrue(sameValue(gold,actual),()->"Original same-value assertion: expected "+gold+"; actual "+actual);
            }
            case "double-tolerance","double-node"->{
                if(mode.equals("double-node")&&sameValue(value(expected.get("term")),actual))return;
                Literal number=assertInstanceOf(Literal.class,actual);assertEquals("double",datatype(number),"original isDouble assertion");
                double gold=Double.parseDouble(expected.get("number").asText()),delta=Double.parseDouble(expected.get("delta").asText()),numberValue=number.doubleValue();
                assertTrue(delta>=0&&!Double.isNaN(delta),"invalid source delta");
                if(Double.isNaN(gold))assertTrue(Double.isNaN(numberValue),"original expected NaN");
                else if(Double.isInfinite(gold))assertTrue(Double.isInfinite(numberValue),"original helper accepts either infinity sign");
                else assertTrue(Math.abs(numberValue-gold)<=delta,()->"Original delta "+delta+": expected "+gold+"; actual "+numberValue);
            }
            case "predicate"->assertTrue(predicate(expected.get("predicate").asText(),actual),()->"Original predicate "+expected.get("predicate")+" rejected "+actual);
            default->fail("Unimplemented expression oracle mode: "+mode);
        }
    }
    static boolean predicate(String name,Value value) {
        return switch(name) {
            case "isIRI"->value instanceof IRI;case "isBlank"->value instanceof BNode;case "isLiteral"->value instanceof Literal;
            case "isString"->datatype(value).equals("string");case "isBoolean"->datatype(value).equals("boolean");
            case "isInteger"->integer(value);case "isDecimal"->decimal(value);case "isDouble"->datatype(value).equals("double");
            case "isFloat"->datatype(value).equals("float");case "isNumber"->numeric(value);
            case "isDateTime"->DATETIMES.contains(datatype(value));
            case "isDayTimeDuration"->datatype(value).equals("dayTimeDuration");case "isYearMonthDuration"->datatype(value).equals("yearMonthDuration");
            case "isDuration"->DURATIONS.contains(datatype(value));
            default->throw new IllegalArgumentException("Unimplemented source predicate: "+name);
        };
    }
}
