package org.hasmac.sparql;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.eclipse.rdf4j.model.*;

/** mf:LaxCardinality: each solution occurs between one and its source upper bound. */
final class ReducedOracle {
    private ReducedOracle() {}
    static void tuples(List<String> expectedVars,List<Map<String,Value>> expected,List<String> actualVars,List<Map<String,Value>> actual) {
        assertEquals(new TreeSet<>(expectedVars),new TreeSet<>(actualVars),"REDUCED projection");
        var e=ResultOracle.bag(expected); var a=ResultOracle.bag(actual);
        assertEquals(e.size(),a.size(),"REDUCED distinct solution count");
        boolean blank=expected.stream().flatMap(r->r.values().stream()).anyMatch(ResultOracle::blank)
                || actual.stream().flatMap(r->r.values().stream()).anyMatch(ResultOracle::blank);
        if(!blank) {
            assertEquals(e.keySet(),a.keySet(),"REDUCED solution support");
            a.forEach((row,count)->assertTrue(count>=1 && count<=e.get(row),"REDUCED multiplicity outside [1,source count]")); return;
        }
        List<Map<String,Value>> left=new ArrayList<>(e.keySet()),right=new ArrayList<>(a.keySet());
        List<int[]> candidates=new ArrayList<>();
        for(var row:right) {
            int[] c=java.util.stream.IntStream.range(0,left.size()).filter(i -> e.get(left.get(i))>=a.get(row) && left.get(i).keySet().equals(row.keySet())).toArray();
            assertTrue(c.length>0,"REDUCED no cardinality-compatible row"); candidates.add(c);
        }
        Integer[] order=new Integer[right.size()]; for(int i=0;i<order.length;i++) order[i]=i;
        Arrays.sort(order,Comparator.comparingInt(i->candidates.get(i).length));
        Search search=new Search(left,right,candidates,order);
        assertTrue(search.visit(0),"REDUCED has no globally consistent blank-node bijection satisfying cardinality intervals");
    }
    static final class Search {
        final List<Map<String,Value>> e,a; final List<int[]> candidates; final Integer[] order;
        final boolean[] used; final Map<BNode,BNode> forward=new HashMap<>(),reverse=new HashMap<>();
        final ArrayList<BNode> trail=new ArrayList<>(); long attempts;
        Search(List<Map<String,Value>> e,List<Map<String,Value>> a,List<int[]> candidates,Integer[] order) {this.e=e;this.a=a;this.candidates=candidates;this.order=order;used=new boolean[e.size()];}
        boolean visit(int depth) {
            if(++attempts>2_000_000) throw new IllegalStateException("REDUCED oracle search budget exhausted: inconclusive, not an engine mismatch");
            if(depth==order.length) return true;
            int idx=order[depth]; var actual=a.get(idx);
            for(int candidate:candidates.get(idx)) if(!used[candidate]) {
                int mark=trail.size(); boolean match=true;
                for(var entry:e.get(candidate).entrySet()) if(!unify(entry.getValue(),actual.get(entry.getKey()))) {match=false;break;}
                if(match) { used[candidate]=true; if(visit(depth+1)) return true; used[candidate]=false; }
                while(trail.size()>mark) {BNode node=trail.removeLast(); reverse.remove(forward.remove(node));}
            }
            return false;
        }
        boolean unify(Value expected,Value actual) {
            if(expected instanceof BNode b) {
                if(!(actual instanceof BNode other)) return false;
                BNode mapped=forward.get(b); if(mapped!=null) return mapped.equals(other);
                if(reverse.containsKey(other)) return false;
                forward.put(b,other);reverse.put(other,b);trail.add(b);return true;
            }
            if(expected instanceof TripleTerm t) return actual instanceof TripleTerm other && unify(t.getSubject(),other.getSubject()) && t.getPredicate().equals(other.getPredicate()) && unify(t.getObject(),other.getObject());
            return expected.equals(actual);
        }
    }
}
