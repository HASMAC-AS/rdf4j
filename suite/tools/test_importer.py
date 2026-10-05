#!/usr/bin/env python3
import unittest
import import_corpus as c

class ImporterTests(unittest.TestCase):
    def test_cpp_undefined(self):self.assertEqual([[None,13],[3,6]],c.cpp_table('{{U,13},{3,6}}'))
    def test_cpp_empty(self):self.assertEqual([],c.cpp_table('IdTable{2, alloc}'))
    def test_cpp_wrapper(self):self.assertEqual([[3,6]],c.cpp_table('makeIdTableFromVector({{3,6}})'))
    def test_split_nesting(self):self.assertEqual(['{{1,2}}','{true}','2'],c.split_args('{{1,2}}, {true}, 2'))
    def test_balancing_string(self):self.assertEqual(13,c.end_block('{ x="}"; y; } junk',0))
    def test_jstring(self):self.assertEqual('a\nb',c.jstring('"a\\nb"'))
    def test_datatype_not_normalized(self):self.assertEqual('01',c.term_from_sparql('"01"^^xsd:integer')['value'])
    def test_rdf_term_iri(self):self.assertEqual({'type':'iri','value':'urn:x'},c.term_from_sparql('<urn:x>'))
    def test_mask_string(self):self.assertNotIn('SERVICE',c.mask('SELECT ("SERVICE" AS ?x) WHERE {}'))
    def test_mask_does_not_hide_service_keyword(self):self.assertIn('SERVICE',c.mask('SERVICE <http://example.org/> { ?s ?p ?o }'))
    def test_inline_empty_row(self):self.assertEqual({'type':'tuple','vars':['x'],'rows':[[None]]},c.inline(['x'],[[None]]))

if __name__=='__main__':unittest.main()
