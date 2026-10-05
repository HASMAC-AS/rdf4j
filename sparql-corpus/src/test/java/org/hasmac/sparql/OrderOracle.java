package org.hasmac.sparql;

import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.util.*;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import org.eclipse.rdf4j.model.*;

/** SPARQL 1.1 partial ordering, independent of the query engine's comparator. */
final class OrderOracle {
    private static final String XSD="http://www.w3.org/2001/XMLSchema#";
    private static final Set<String> NUMERIC=Set.of("integer","decimal","nonPositiveInteger","negativeInteger","long","int","short","byte","nonNegativeInteger","unsignedLong","unsignedInt","unsignedShort","unsignedByte","positiveInteger","float","double");
    private static final ThreadLocal<DatatypeFactory> DATES=ThreadLocal.withInitial(()->{
        try{return DatatypeFactory.newInstance();}catch(Exception e){throw new IllegalStateException(e);}
    });
    private OrderOracle() {}
    static void verify(List<String> variables,List<Map<String,Value>> rows,JsonNode keys) {
        for(JsonNode key:keys) CorpusTest.prerequisite(variables.contains(key.path("variable").asText()),"ORDER BY key is not projected: "+key);
        long pairs=(long)rows.size()*(rows.size()-1)/2;
        CorpusTest.prerequisite(pairs<=Long.getLong("suite.maxOrderPairs",10_000_000L),"ORDER BY verification exceeds suite.maxOrderPairs; no unverified order is accepted");
        // Incomparability is not transitive. Adjacent comparisons alone can miss
        // a reversed comparable pair separated by an incomparable language literal.
        for(int i=0;i<rows.size();i++) for(int j=i+1;j<rows.size();j++) {
            Integer comparison=compareRows(rows.get(i),rows.get(j),keys);
            if(comparison!=null && comparison>0) fail("ORDER BY violation between rows "+i+" and "+j+": "+rows.get(i)+" before "+rows.get(j));
        }
    }
    static Integer compareRows(Map<String,Value> left,Map<String,Value> right,JsonNode keys) {
        for(JsonNode key:keys) {
            String variable=key.path("variable").asText();Value a=left.get(variable),b=right.get(variable);
            if(Objects.equals(a,b)) continue;
            Integer comparison=values(a,b);
            // Only identical RDF terms, not merely numerically equal values,
            // require application of the next ordering key (SPARQL 1.1 section 15.1).
            if(comparison==null || comparison==0) return null;
            return key.path("ascending").asBoolean(true)?comparison:-comparison;
        }
        return 0;
    }
    static int rank(Value v){return v==null?0:v instanceof BNode?1:v instanceof IRI?2:v instanceof Literal?3:4;}
    static String local(Literal literal){String iri=literal.getDatatype().stringValue();return iri.startsWith(XSD)?iri.substring(XSD.length()):"";}
    static Integer values(Value left,Value right) {
        if(Objects.equals(left,right)) return 0;
        CorpusTest.prerequisite(!(left instanceof TripleTerm || right instanceof TripleTerm),"RDF 1.2 triple-term ordering needs a separate oracle");
        int a=rank(left),b=rank(right);if(a!=b) return Integer.compare(a,b);
        if(left instanceof BNode) return null;
        if(left instanceof IRI) return codepoints(left.stringValue(),right.stringValue());
        if(!(left instanceof Literal l) || !(right instanceof Literal r)) return null;
        if(l.getLanguage().isPresent() || r.getLanguage().isPresent()) return null;
        String lt=local(l),rt=local(r);
        try {
            if(NUMERIC.contains(lt) && NUMERIC.contains(rt)) {
                if(lt.equals("double") || rt.equals("double")) {
                    double x=floating(l.getLabel()),y=floating(r.getLabel());
                    if(Double.isNaN(x)||Double.isNaN(y)) return null;
                    return x==y?0:x<y?-1:1;
                }
                if(lt.equals("float") || rt.equals("float")) {
                    float x=(float)floating(l.getLabel()),y=(float)floating(r.getLabel());
                    if(Float.isNaN(x)||Float.isNaN(y)) return null;
                    return x==y?0:x<y?-1:1;
                }
                return Integer.signum(new BigDecimal(l.getLabel()).compareTo(new BigDecimal(r.getLabel())));
            }
            if(lt.equals("string") && rt.equals("string")) return codepoints(l.getLabel(),r.getLabel());
            if(lt.equals("boolean") && rt.equals("boolean")) return Boolean.compare(bool(l.getLabel()),bool(r.getLabel()));
            if(lt.equals("dateTime") && rt.equals("dateTime")) {
                int result=DATES.get().newXMLGregorianCalendar(l.getLabel()).compare(DATES.get().newXMLGregorianCalendar(r.getLabel()));
                return result==DatatypeConstants.INDETERMINATE?null:Integer.valueOf(result);
            }
        } catch(IllegalArgumentException invalidLexicalForm){return null;}
        return null;
    }
    static double floating(String text){return switch(text){case "INF","+INF"->Double.POSITIVE_INFINITY;case "-INF"->Double.NEGATIVE_INFINITY;default->Double.parseDouble(text);};}
    static boolean bool(String text){return switch(text){case "1","true"->true;case "0","false"->false;default->throw new IllegalArgumentException("invalid boolean");};}
    static int codepoints(String a,String b){
        int i=0,j=0;
        while(i<a.length() && j<b.length()){int x=a.codePointAt(i),y=b.codePointAt(j);if(x!=y)return Integer.compare(x,y);i+=Character.charCount(x);j+=Character.charCount(y);}
        return Integer.compare(a.length()-i,b.length()-j);
    }
}
