package org.hasmac.sparql;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JournalJsonTest {
    @TempDir Path directory;
    @Test void diagnosticsWithUnpairedSurrogatesSurviveUtf8Journal() throws Exception {
        String message="high="+(char)0xd800+", low="+(char)0xdc00+", valid=𐐈, nul="+(char)0;
        var mapper=JournalJson.mapper();var node=mapper.createObjectNode().put("message",message);
        Path file=directory.resolve("event.jsonl");Files.writeString(file,mapper.writeValueAsString(node)+"\n",StandardCharsets.UTF_8);
        assertEquals(message,mapper.readTree(Files.readString(file)).get("message").asText());
    }
    @Test void identicalInvalidLiteralsFollowOriginalTermFastPath() {
        var v=org.eclipse.rdf4j.model.impl.SimpleValueFactory.getInstance();
        var t=v.createIRI("http://www.w3.org/2001/XMLSchema#integer");
        assertTrue(ExpressionOracle.sameValue(v.createLiteral("bad",t),v.createLiteral("bad",t)));
        assertFalse(ExpressionOracle.sameValue(v.createLiteral("bad",t),v.createLiteral("worse",t)));
    }
    @Test void tripleTermValueComparisonUsesNestedValueSemantics() {
        var v=org.eclipse.rdf4j.model.impl.SimpleValueFactory.getInstance();var p=v.createIRI("urn:p");var s=v.createIRI("urn:s");
        var a=v.createTripleTerm(s,p,v.createLiteral("01",v.createIRI("http://www.w3.org/2001/XMLSchema#integer")));
        var b=v.createTripleTerm(s,p,v.createLiteral("1.0",v.createIRI("http://www.w3.org/2001/XMLSchema#decimal")));
        assertTrue(ExpressionOracle.sameValue(a,b));assertNotEquals(a,b);
    }
}
