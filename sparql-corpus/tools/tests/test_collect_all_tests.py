import importlib.util
import json
import tempfile
import unittest
from pathlib import Path

PATH = Path(__file__).resolve().parents[1] / "collect_all_tests.py"
spec = importlib.util.spec_from_file_location("collect_all_tests", PATH)
collector = importlib.util.module_from_spec(spec)
spec.loader.exec_module(collector)


class DeclarationTests(unittest.TestCase):
    def test_comments_and_strings_are_not_tests(self):
        source = '''// @Test void phantom() {}
/* @Test void phantom2() {} */
class T { String s = "@Test void phantom3() {}";
@Test public void actual() { String braces = "}}"; if (true) {} }
}'''
        records = collector.native_declarations(source, ".java")
        self.assertEqual(["actual"], [r["name"] for r in records])
        self.assertIn('if (true) {} }', source[records[0]["startOffset"]:records[0]["endOffset"]])

    def test_parameterized_annotation_array(self):
        source = '''@ParameterizedTest
@ValueSource(strings = {"a", "b"})
void parameter(String value) { assertNotNull(value); }'''
        records = collector.native_declarations(source, ".java")
        self.assertEqual("parameter", records[0]["name"])
        self.assertEqual("ParameterizedTest", records[0]["declarationKind"])

    def test_factory(self):
        source = '@TestFactory Stream<DynamicTest> generated() { return Stream.empty(); }'
        records = collector.native_declarations(source, ".java")
        self.assertEqual("generated", records[0]["name"])
        self.assertEqual("TestFactory", records[0]["declarationKind"])

    def test_repeated_qualified_annotation(self):
        source = '@org.junit.jupiter.api.RepeatedTest(5) void runAgain() { verify(); }'
        records = collector.native_declarations(source, ".java")
        self.assertEqual("runAgain", records[0]["name"])
        self.assertEqual("RepeatedTest", records[0]["declarationKind"])

    def test_junit3_candidates_are_not_asserted_to_be_runtime_tests(self):
        records = collector.native_declarations('public void testOld() {}\nvoid helper() {}', '.java')
        self.assertEqual(1, len(records))
        self.assertEqual("JUnit3-name-candidate", records[0]["declarationKind"])
        self.assertFalse(records[0]["declarationResolved"])

    def test_no_double_count_annotated_test_prefix(self):
        records = collector.native_declarations('@Test public void testNew() {}', '.java')
        self.assertEqual(1, len(records))
        self.assertEqual("Test", records[0]["declarationKind"])

    def test_cpp_parameterized_and_typed(self):
        source = '''TEST_P(Fixture, Case) { EXPECT_TRUE(true); }
TYPED_TEST(Types, Case2) { EXPECT_EQ(1, 1); }
// TEST(Phantom, Nope) {}
'''
        records = collector.native_declarations(source, '.cpp')
        self.assertEqual(["Fixture.Case", "Types.Case2"], [r["name"] for r in records])

    def test_cpp_raw_string_does_not_close_body(self):
        source = 'TEST(X, Y) { auto s = R"tag(}} TEST(Fake, Bad) {})tag"; check(s); }'
        records = collector.native_declarations(source, '.cpp')
        self.assertEqual(1, len(records))
        self.assertEqual(len(source), records[0]["endOffset"])

    def test_java_text_block_does_not_close_body(self):
        source = '@Test void query() { String q = """\n} @Test void fake() {}\n"""; check(q); }'
        records = collector.native_declarations(source, '.java')
        self.assertEqual(["query"], [r["name"] for r in records])
        self.assertEqual(len(source), records[0]["endOffset"])

    def test_offsets_and_lines(self):
        source = '\n@Test\nvoid sample() {\n call();\n}\n'
        record = collector.native_declarations(source, '.java')[0]
        self.assertEqual(2, record["line"])
        self.assertEqual(5, record["endLine"])
        body = source[record["startOffset"]:record["endOffset"]]
        self.assertEqual(record["sourceTextSha256"], collector.sha(body.encode()))

    def test_mask_preserves_offsets_and_newlines(self):
        source = '"hello\\\"world"\n/* hi\nbye */\nR"(}})"'
        masked = collector.mask_code(source)
        self.assertEqual(len(source), len(masked))
        self.assertEqual([i for i, c in enumerate(source) if c == '\n'],
                         [i for i, c in enumerate(masked) if c == '\n'])

    def test_path_escape_rejected(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            with self.assertRaises(ValueError):
                collector.safe_path(root, '../outside')
            self.assertEqual(root / 'inside', collector.safe_path(root, 'inside'))

    def test_lexical_literals_not_normalized(self):
        graph = collector.Graph().parse(data='@prefix xsd: <http://www.w3.org/2001/XMLSchema#> . <urn:s> <urn:p> "01"^^xsd:integer .', format='turtle')
        self.assertEqual('01', str(next(graph.objects())))

    def test_deterministic_identifier(self):
        self.assertEqual(collector.identifier('test'), collector.identifier('test'))
        self.assertNotEqual(collector.identifier('test'), collector.identifier('test2'))


class ManifestTests(unittest.TestCase):
    def test_duplicate_entries_do_not_shift_existing_case_ids(self):
        graph = collector.Graph().parse(data='''@prefix mf: <http://www.w3.org/2001/sw/DataAccess/tests/test-manifest#> .
        <urn:manifest> mf:entries (<urn:a> <urn:a> <urn:b>) .
        <urn:a> a mf:QueryEvaluationTest .
        <urn:b> a mf:QueryEvaluationTest .
        <urn:c> a mf:UpdateEvaluationTest .''', format="turtle")
        self.assertEqual(["urn:a", "urn:b", "urn:c"], [str(x) for x in collector.manifest_entries(graph)])

    def test_vocabulary_status_is_not_a_test(self):
        graph = collector.Graph().parse(data='''@prefix mf: <http://www.w3.org/2001/sw/DataAccess/tests/test-manifest#> .
        <urn:approved> a mf:TestStatus .''', format="turtle")
        self.assertEqual([], collector.manifest_entries(graph))


if __name__ == '__main__':
    unittest.main()
