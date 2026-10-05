package org.hasmac.sparql;

import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.JsonNode;
import java.nio.file.*;
import java.util.*;
import org.eclipse.rdf4j.model.*;
import org.junit.jupiter.api.Test;

/** Synthetic comparator calibration, not imported query-correctness coverage. */
class JenaComparatorCalibrationTest {
    @Test void originalJenaValueAndDatatypeComparatorMatchesIndependentOracle() throws Exception {
        Path file=CorpusTest.CORPUS.resolve("jena-comparator-calibration.json");
        assertTrue(Files.isRegularFile(file),"Missing source-generated comparator matrix; rebuild the captured corpus");
        JsonNode matrix=CorpusTest.JSON.readTree(file.toFile());
        assertEquals(676,matrix.size(),"complete 26 by 26 calibration matrix");
        List<String> differences=new ArrayList<>();
        for(JsonNode pair:matrix) {
            Value a=ExpressionOracle.value(pair.get("left")),b=ExpressionOracle.value(pair.get("right"));
            boolean actual=ExpressionOracle.sameValue(a,b);
            if(a instanceof Literal x && b instanceof Literal y) actual&=x.getDatatype().equals(y.getDatatype());
            if(pair.get("accepts").asBoolean()!=actual)differences.add(pair.get("display").asText()+": Jena="+pair.get("accepts")+", oracle="+actual+", sourceException="+pair.path("sourceException"));
        }
        assertTrue(differences.isEmpty(),()->"Comparator policy mismatches (not RDF4J engine bugs):\n"+String.join("\n",differences));
    }
}
