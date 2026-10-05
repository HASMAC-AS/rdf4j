import importlib.util
import json
import sys
import tempfile
import unittest
from pathlib import Path

TOOLS = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(TOOLS))
import native_capture as capture
import lazy_ports as lazy


class NativeCaptureTest(unittest.TestCase):
    def predicate(self, expression, name='test', line=12):
        event = {'callLine': 12}
        method = {'invocations': [{'name': name, 'line': line, 'arguments': ['"RAND()"', expression]}]}
        return capture.predicate_descriptor(event, method)

    def test_method_reference(self):
        self.assertEqual('isIRI', self.predicate('NodeValue::isIRI')['predicate'])

    def test_simple_lambda(self):
        self.assertEqual('isDateTime', self.predicate('n -> n.isDateTime()')['predicate'])

    def test_parenthesized_lambda(self):
        self.assertEqual('isDouble', self.predicate('( n ) -> n.isDouble()')['predicate'])

    def test_compound_predicate_is_not_weakened(self):
        self.assertIsNone(self.predicate('n -> n.isInteger() && n.getInteger().signum() > 0'))

    def test_wrong_call_line_is_not_guessed(self):
        self.assertIsNone(self.predicate('NodeValue::isIRI', line=13))

    def test_unknown_type_predicate_needs_adapter(self):
        self.assertIsNone(self.predicate('NodeValue::isGregorian'))

    def test_non_test_call_is_not_selected(self):
        self.assertIsNone(self.predicate('NodeValue::isIRI', name='helper'))

    def test_jsonl_preserves_unicode_and_empty_bindings(self):
        with tempfile.TemporaryDirectory() as directory:
            file = Path(directory) / 'events.jsonl'
            value = {'expression': '"𐐈"', 'expected': {'rows': [{}]}}
            file.write_text(json.dumps(value) + '\n\n')
            self.assertEqual([value], capture.read_lines(file))

    def test_malformed_jsonl_is_not_silently_dropped(self):
        with tempfile.TemporaryDirectory() as directory:
            file = Path(directory) / 'events.jsonl'
            file.write_text('{"broken":\n')
            with self.assertRaises(json.JSONDecodeError):
                capture.read_lines(file)

    def test_lazy_calibration(self):
        lazy.calibration()

    def test_typed_id_kinds_do_not_collapse(self):
        row = lazy.array('{{V(1), I(1), U, T}}')[0]
        self.assertNotEqual(row[0], row[1])
        self.assertEqual('uri', lazy.cell_term(row[0])['type'])
        self.assertEqual('literal', lazy.cell_term(row[1])['type'])
        self.assertIsNone(lazy.cell_term(row[2]))

    def test_minus_disjoint_bound_domains_retained(self):
        row = (None, '@vocab:1')
        self.assertEqual(1, lazy.reference_rows((row,), (('@vocab:2', '@vocab:3'),), 'minus', 2)[row])

    def test_optional_duplicate_rows_are_preserved(self):
        a, b = ('@vocab:1', '@vocab:2'), ('@vocab:1', '@vocab:3')
        result = lazy.reference_rows((a, a), (b, b), 'optional', 2)
        self.assertEqual(4, result[('@vocab:1', '@vocab:2', '@vocab:3')])

    def test_unknown_vector_mutation_rejected(self):
        text = 'std::vector<IdTable> a; a.resize(4); test(a);'
        with self.assertRaises(ValueError):
            lazy.Tables(text).sequence('a', text.index('test('))

    def test_vector_loop_requires_expansion(self):
        text = 'std::vector<IdTable> a; for(int i=0;i<2;i++){a.emplace_back(1, makeAllocator());} test(a);'
        with self.assertRaises(ValueError):
            lazy.Tables(text).sequence('a', text.index('test('))


if __name__ == '__main__':
    unittest.main()
