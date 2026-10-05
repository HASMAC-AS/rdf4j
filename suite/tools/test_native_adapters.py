import unittest
import native_adapters as a

class NativeAdapterTests(unittest.TestCase):
    def test_comments_preserve_offsets(self):
        source='// @Test void fake() {}\n@Test void real() { String x="@Test"; }'
        masked=a.mask_source(source)
        self.assertEqual(len(source),len(masked));self.assertEqual(1,masked.count('@Test'));self.assertEqual(source.count('\n'),masked.count('\n'))
    def test_cpp_raw_string(self):self.assertNotIn('TEST(',a.mask_source('R"tag(TEST(fake, no){})tag"'))
    def test_comment_markers_inside_strings(self):self.assertIn('y',a.mask_source('String x="https://example/*"; int y=1;'))
    def test_boolean_constants(self):self.assertTrue(a.java_value('(2 < 3) && (3 < 4)'))
    def test_long_arithmetic(self):self.assertEqual(4111222334678,a.java_value('1234 + 4111222333444L'))
    def test_no_arbitrary_evaluation(self):
        with self.assertRaises(ValueError):a.java_value('__import__("os").system("false")')
    def test_string_concat_namespace(self):self.assertEqual('xhttp://www.w3.org/2001/XMLSchema#integer',a.string_value('"x"+XSDDatatype.XSDinteger.getURI()',{}))
    def test_string_constant(self):self.assertEqual('a = b',a.string_value('first+" = "+second',{'first':'a','second':'b'}))
    def test_call_balance_with_parenthesis_in_string(self):self.assertEqual('"f())", true',list(a.calls('testBoolean("f())", true);',{'testBoolean'}))[0][3])
    def test_does_not_read_commented_helper(self):self.assertEqual([],list(a.calls('// testBoolean("x", true);',{'testBoolean'})))
    def test_scope(self):self.assertEqual((0,2),a.scope('{ { x; } y; }',5))

if __name__=='__main__':unittest.main()
