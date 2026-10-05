import copy
import sys
import unittest
from pathlib import Path
sys.path.insert(0,str(Path(__file__).resolve().parents[1]))
import qlever_fixture as q

class QleverFixtureTest(unittest.TestCase):
    def test_relative_iri_is_rebased(self):
        self.assertEqual('<https://qlever-corpus.invalid/Albert_Einstein>',q.map_iri_cell('<Albert_Einstein>'))
    def test_absolute_iris_are_unchanged(self):
        self.assertEqual('<urn:example>',q.map_iri_cell('<urn:example>'))
    def test_unicode_is_not_percent_encoded(self):
        self.assertEqual('<https://qlever-corpus.invalid/Luís>',q.map_iri_cell('<Luís>'))
    def test_literals_numbers_and_wildcards_unchanged(self):
        for value in ['Albert_Einstein',1,0.2,True,None]:self.assertEqual(value,q.map_iri_cell(value))
    def test_source_checks_are_never_modified(self):
        checks=[{'res':[['<one>',None,3]]},{'contains_row':['<two>','literal']},{'num_rows':1}]
        before=copy.deepcopy(checks);mapped=q.adapt_checks(checks)
        self.assertEqual(before,checks)
        self.assertNotEqual(checks[0]['res'][0][0],mapped[0]['res'][0][0])
    def test_rebasing_is_idempotent(self):
        checks=[{'contains_row':['<one>',None]}]
        self.assertEqual(q.adapt_checks(checks),q.adapt_checks(q.adapt_checks(checks)))

if __name__=='__main__':unittest.main()
