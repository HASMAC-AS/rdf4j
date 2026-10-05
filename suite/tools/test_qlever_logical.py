import unittest
import import_corpus as c
from qlever_logical import Resolver,Scalar,UNDEF,literal,expand,XSD

class QleverLogicalTests(unittest.TestCase):
    def resolve(self,code,expression):
        r=Resolver(code,c);return r.resolve(expression,len(code),(0,))
    def test_const_vector(self):
        code='{ constexpr auto t=B(true); constexpr auto f=B(false); V<Id> v{{f,t,U}, alloc};'
        values=self.resolve(code,'v')
        self.assertEqual(['false','true',None],[x.term['value'] if x.term else None for x in values])
    def test_scalar_broadcast(self):
        expected,a,b=expand([literal('true','boolean'),literal('false','boolean')],literal('1','integer'),[UNDEF,UNDEF])
        self.assertEqual(2,len(a));self.assertEqual(a[0],a[1]);self.assertEqual(2,len(expected))
    def test_unknown_vocab_rejected(self):
        with self.assertRaises(ValueError):self.resolve('{','Voc(4)')
    def test_mutated_vector_rejected(self):
        with self.assertRaises(ValueError):self.resolve('{ V<Id> v{{B(true)}, alloc}; v.clear();','v')
    def test_nan_is_double(self):
        x=self.resolve('{','D(std::numeric_limits<double>::quiet_NaN())')
        self.assertEqual('NaN',x.term['value']);self.assertEqual(XSD+'double',x.term['datatype'])
    def test_string_empty(self):self.assertEqual('',self.resolve('{','IdOrLocalVocabEntry(lit(""))').term['value'])
    def test_logical_expected_category(self):
        with self.assertRaises(ValueError):expand(literal('1','integer'),UNDEF,UNDEF)
    def test_vector_lengths(self):
        with self.assertRaises(ValueError):expand([UNDEF],[UNDEF,UNDEF],[UNDEF])
    def test_aliases(self):
        self.assertEqual('true',self.resolve('{ auto x=B(true); auto y=x;','y').term['value'])

if __name__=='__main__':unittest.main()
