import importlib.util,unittest
from pathlib import Path
spec=importlib.util.spec_from_file_location('order_policy',Path(__file__).resolve().parents[1]/'order_policy.py')
m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)
class OrderPolicyTest(unittest.TestCase):
    def test_plain(self):self.assertEqual(m.extract('SELECT ?x WHERE {} ORDER BY ?x')[0],[{'variable':'x','ascending':True}])
    def test_mixed(self):self.assertEqual(m.extract('SELECT * {} ORDER BY DESC(?x) ASC($y) LIMIT 3')[0],[{'variable':'x','ascending':False},{'variable':'y','ascending':True}])
    def test_subquery_is_not_outer_order(self):self.assertEqual(m.extract('SELECT * { { SELECT ?x {} ORDER BY ?x } }'),(None,None))
    def test_comment_not_order(self):self.assertEqual(m.extract('SELECT * {} # ORDER BY ?x'),(None,None))
    def test_literal_not_order(self):self.assertEqual(m.extract('SELECT ("ORDER BY ?x" AS ?x) {}'),(None,None))
    def test_iri_not_order(self):self.assertEqual(m.extract('PREFIX order: <urn:ORDER_BY> SELECT * {}'),(None,None))
    def test_expression_requires_oracle(self):self.assertIsNotNone(m.extract('SELECT ?x {} ORDER BY STR(?x)')[1])
    def test_missing_projection_blocked(self):
        c={'kind':'evaluation','query':'SELECT ?x {} ORDER BY ?y','expected':{'kind':'tuple','vars':['x']}}
        reasons=[];m.apply(c,lambda _,r:reasons.append(r));self.assertEqual(len(reasons),1)
    def test_indexed_sequence_is_replaced(self):
        c={'kind':'evaluation','query':'SELECT ?x {} ORDER BY ?x','expected':{'kind':'tuple','vars':['x'],'ordered':True}}
        m.apply(c,lambda _,r:self.fail(r));self.assertFalse(c['expected']['ordered']);self.assertEqual(c['orderBy'][0]['variable'],'x')
    def test_literal_braces_do_not_expose_subquery(self):self.assertEqual(m.extract('SELECT * { BIND("}" AS ?x) { SELECT * {} ORDER BY ?y }}'),(None,None))
if __name__=='__main__':unittest.main()
