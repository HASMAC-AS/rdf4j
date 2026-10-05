package org.hasmac.testing;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.hasmac.testing.CorpusIO.Table;
import org.junit.jupiter.api.Test;

final class NativeAssertionOracleTest {
    private static void check(String assertion,String value,Value actual) {
        var expected=CorpusIO.JSON.createObjectNode();expected.put("type","native-assertion");expected.put("assertion",assertion);
        if(value==null)expected.putNull("value");else expected.put("value",value);
        NativeAssertionOracle.compare(expected,new Table(List.of("result"),List.of(Map.of("result",actual))));
    }
    @Test void integerSubtypeUsesNumericValue(){check("integer32","1",CorpusIO.VF.createLiteral("01",XSD.INT));}
    @Test void integerCategoryRejectsDecimal(){assertThrows(AssertionError.class,()->check("integer32","1",CorpusIO.VF.createLiteral("1.0",XSD.DECIMAL)));}
    @Test void doubleConversionIncludesDecimal(){check("numeric-double","4.0",CorpusIO.VF.createLiteral("4.00",XSD.DECIMAL));}
    @Test void decimalScaleIsPreserved(){assertThrows(AssertionError.class,()->check("decimal-scale","2.5",CorpusIO.VF.createLiteral("2.50",XSD.DECIMAL)));}
    @Test void stringCategoryRejectsLanguageLiteral(){assertThrows(AssertionError.class,()->check("string","abc",CorpusIO.VF.createLiteral("abc","en")));}
    @Test void arbitraryBoundTermAccepted(){check("bound",null,CorpusIO.VF.createBNode());}
    @Test void unsupportedAssertionFails(){assertThrows(IllegalArgumentException.class,()->check("invented","1",CorpusIO.VF.createLiteral(1)));}
    @Test void invalidUnicodeDiagnosticsRemainJson()throws Exception{
        var n=CorpusIO.JSON.createObjectNode();n.put("bad",String.valueOf((char)0xd800));
        String encoded=CorpusTest.asciiJson(CorpusIO.JSON.writeValueAsString(n));
        assertTrue(encoded.chars().allMatch(x->x<128));assertEquals(n,CorpusIO.JSON.readTree(encoded));
    }
}
