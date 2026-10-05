package org.hasmac.sparql;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.junit.jupiter.api.Test;
class ReducedOracleTest {
    static Map<String,Value> r(Value v){return Map.of("x",v);}
    static void check(List<Map<String,Value>> expected,List<Map<String,Value>> actual){ReducedOracle.tuples(List.of("x"),expected,List.of("x"),actual);}
    @Test void allowsSomeDuplicates(){var v=SimpleValueFactory.getInstance().createLiteral(1);check(List.of(r(v),r(v),r(v)),List.of(r(v),r(v)));}
    @Test void disallowsTooManyDuplicates(){var v=SimpleValueFactory.getInstance().createLiteral(1);assertThrows(AssertionError.class,()->check(List.of(r(v)),List.of(r(v),r(v))));}
    @Test void disallowsMissingSolution(){var v=SimpleValueFactory.getInstance().createLiteral(1);assertThrows(AssertionError.class,()->check(List.of(r(v)),List.of()));}
    @Test void preservesGlobalBlankNodeIdentity(){var f=SimpleValueFactory.getInstance();var a=f.createBNode("a");var b=f.createBNode("b");check(List.of(r(a),r(a)),List.of(r(b)));}
    @Test void doesNotCollapseDistinctBlankNodes(){var f=SimpleValueFactory.getInstance();var a=f.createBNode("a");var b=f.createBNode("b");assertThrows(AssertionError.class,()->check(List.of(r(a),r(b)),List.of(r(a),r(a))));}
}
