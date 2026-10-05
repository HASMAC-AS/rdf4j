import importlib.util, json, tempfile, unittest
from pathlib import Path
spec=importlib.util.spec_from_file_location('build_corpus',Path(__file__).resolve().parents[1]/'build_corpus.py')
m=importlib.util.module_from_spec(spec); spec.loader.exec_module(m)

class ImportTest(unittest.TestCase):
    def test_nested_arguments(self): self.assertEqual(m.split_args('{{1,2}}, f(1,2), {true,false}, 1'),['{{1,2}}','f(1,2)','{true,false}','1'])
    def test_strings_are_not_split(self): self.assertEqual(m.split_args('"a,b", {3}'),['"a,b"','{3}'])
    def test_undefined_values(self): self.assertEqual(m.cpp_array('{{U,3},{4,U}}'),[[None,3],[4,None]])
    def test_boolean_array(self): self.assertEqual(m.cpp_array('{true,false,true}'),[True,False,True])
    def test_empty_table(self): self.assertEqual(m.table('IdTable{2, alloc}'),([],2))
    def test_table_factory(self): self.assertEqual(m.table('makeIdTableFromVector({{3,6},{4,7}})'),([[3,6],[4,7]],2))
    def test_ragged_table_rejected(self):
        with self.assertRaises(ValueError): m.table('{{3},{4,5}}')
    def test_values_preserve_duplicates(self): self.assertEqual(m.values('l',[[3],[3]],1).count('<urn:qlever:vid:3>'),2)
    def test_values_undefined(self): self.assertIn('UNDEF',m.values('l',[[None,3]],2))
    def test_braces_in_literals(self):
        s='String s="}"; /* } */ if(true) { f(); } }tail'; self.assertEqual(s[m.base.block_end(s,0):],'tail')
    def test_cpp_raw_string(self):
        s='auto x=R"x( } )x"; }tail'; self.assertEqual(s[m.base.block_end(s,0):],'tail')
    def test_expression_error_keeps_one_unbound_row(self):
        with tempfile.TemporaryDirectory(dir=m.base.ROOT) as d:
            old=m.base.VENDOR; m.base.VENDOR=Path(d); p=Path(d)/'jena/TestExpressions2.java'; p.parent.mkdir(); p.write_text('test')
            try:
                c=m.base.expression_case(p,'error','assertThrows(ExprEvalException.class, ()->eval("1/0"));',1)
                self.assertEqual(c['expected']['rows'],[{}]); self.assertIn('IF((1/0)',c['query'])
            finally: m.base.VENDOR=old
    def test_exact_literal_not_normalized(self):
        lit=m.base.Literal('01',datatype=m.base.URIRef(m.base.XSD+'integer'),normalize=False)
        self.assertEqual(m.base.term(lit)['value'],'01')
    def test_boolean_manifest_result(self):
        self.assertEqual(m.read_expected(m.base.Literal(True)),{'kind':'boolean','value':True})

if __name__=='__main__': unittest.main()
