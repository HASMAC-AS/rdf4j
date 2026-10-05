#!/usr/bin/env python3
"""Append reviewed QLever OPTIONAL cases from literal upstream assertions.

Unsupported C++ expressions are rejected, never evaluated or replaced by an
engine-generated expected result. A pinned source hash bounds this adapter.
"""
from __future__ import annotations
import bisect
import collections
import json
import re
from dataclasses import dataclass
import build_corpus as builder

base = builder.base
SOURCE_BLOB = '023a7760457c74de86b129677e09fbc8388d0c12'
COHORT = {
    'singleColumnRightIsEmpty': 1, 'singleColumnLeftIsEmpty': 1,
    'singleColumnPreexistingNulloptsLeft': 1,
    'singleColumnPreexistingNulloptsRight': 1,
    'singleColumnPreexistingNulloptsBoth': 1,
    'twoColumnsPreexistingUndefLeft': 2, 'twoColumnsPreexistingUndefRight': 1,
    'twoColumnsPreexistingUndefBoth': 2, 'multipleColumnsNoUndef': 2,
    'specialOptionalJoinTwoColumns': 2,
}
LEX = re.compile(r'//[^\n]*|/\*[\s\S]*?\*/|"(?:\\.|[^"\\])*"|\'(?:\\.|[^\'\\])*\'|[{}]')
DECL = re.compile(r'\b(auto|IdTable|JoinColumns)\s+(\w+)\s*(=|\{|\()')
TEST = re.compile(r'\bTEST(?:_F|_P)?\s*\(\s*OptionalJoin\s*,\s*(\w+)\s*\)\s*\{')

@dataclass(frozen=True)
class Table:
    rows: tuple[tuple[int | None, ...], ...]
    width: int

@dataclass(frozen=True)
class Declaration:
    name: str
    kind: str
    expression: str
    offset: int
    end: int
    scope: tuple[int, ...]


def mask_comments(text):
    return LEX.sub(lambda m: ''.join('\n' if c == '\n' else ' ' for c in m.group())
                   if m.group().startswith(('//', '/*')) else m.group(), text)


def scope_index(text):
    positions, scopes, stack, serial = [0], [()], [], 0
    for match in LEX.finditer(text):
        if match.group() == '{':
            serial += 1
            stack.append(serial)
        elif match.group() == '}':
            if not stack:
                raise ValueError('Unbalanced C++ scope')
            stack.pop()
        else:
            continue
        positions.append(match.end())
        scopes.append(tuple(stack))
    if stack:
        raise ValueError('Unclosed C++ scope')
    return positions, scopes


def scope_at(index, offset):
    positions, scopes = index
    return scopes[bisect.bisect_right(positions, offset) - 1]


def expression_end(text, start):
    depth, quote, escape = 0, None, False
    for i in range(start, len(text)):
        c = text[i]
        if quote:
            if escape: escape = False
            elif c == '\\': escape = True
            elif c == quote: quote = None
        elif c in {'"', "'"}: quote = c
        elif c in '({[': depth += 1
        elif c in ')}]':
            depth -= 1
            if depth < 0: raise ValueError('Unbalanced constant initializer')
        elif c == ';' and depth == 0: return i
    raise ValueError('Unterminated constant declaration')


def constant_array(expression):
    # V is the source file's alias for VocabId; bare integer cells have the same
    # identity semantics. No integer-valued Id aliases or arbitrary calls are accepted.
    text = re.sub(r'\bV\(\s*(\d+)\s*\)', r'\1', expression.strip())
    text = re.sub(r'\bU\b', 'null', text).replace('{', '[').replace('}', ']')
    return json.loads(re.sub(r',\s*]', ']', text))


class Constants:
    def __init__(self, text):
        self.text, self.index, self.declarations = text, scope_index(text), []
        for match in DECL.finditer(text):
            kind, name, operator = match.groups()
            start = match.end() if operator == '=' else match.end() - 1
            end = expression_end(text, start)
            self.declarations.append(Declaration(name, kind, text[start:end].strip(),
                match.start(), end, scope_at(self.index, match.start())))

    def visible(self, name, before):
        scope = scope_at(self.index, before)
        choices = [d for d in self.declarations if d.name == name and d.end < before
                   and scope[:len(d.scope)] == d.scope]
        if not choices: raise ValueError('No visible constant declaration for ' + name)
        return max(choices, key=lambda d: d.offset)

    def resolve(self, name, before, seen=()):
        if name in seen: raise ValueError('Cyclic constant alias: ' + name)
        d = self.visible(name, before)
        # A declaration or mutation of an inner shadow is not a mutation of d.
        pattern = r'\b' + re.escape(name) + r'\s*(?:\.(?!clone\b)|\[|=(?!=)|\+\+|--)'
        for mutation in re.finditer(pattern, self.text[d.end + 1:before]):
            at = d.end + 1 + mutation.start()
            if any(x.name == name and x.offset <= at <= x.end for x in self.declarations):
                continue
            if self.visible(name, at) == d:
                raise ValueError('Potential mutation of ' + name + ' before assertion')
        expression = d.expression
        if d.kind == 'JoinColumns': return constant_array(expression)
        if d.kind == 'IdTable' and expression[:1] in {'{', '('}:
            inside = expression[1:-1].strip()
            if inside.startswith('makeIdTableFromVector'): expression = inside
            elif re.fullmatch(r'\d+\s*,\s*makeAllocator\(\s*\)', inside):
                return Table((), int(inside.split(',', 1)[0]))
            else: raise ValueError('Unreviewed IdTable initializer: ' + expression)
        return self.table(expression, d.offset, seen + (name,))

    def table(self, expression, before, seen=()):
        expression = expression.strip()
        if re.fullmatch(r'\w+', expression):
            result = self.resolve(expression, before, seen)
            if not isinstance(result, Table): raise ValueError('Expected IdTable: ' + expression)
            return result
        match = re.fullmatch(r'makeIdTableFromVector\s*\(([\s\S]*)\)', expression)
        if not match: raise ValueError('Unreviewed table expression: ' + expression)
        arguments = builder.split_args(match.group(1))
        if len(arguments) != 1: raise ValueError('Non-default Id conversion requires a separate adapter')
        data = constant_array(arguments[0])
        if not isinstance(data, list) or not data: raise ValueError('Empty literal table has no width')
        width = len(data[0])
        if any(not isinstance(row, list) or len(row) != width for row in data): raise ValueError('Ragged table')
        if any(v is not None and type(v) is not int for row in data for v in row):
            raise ValueError('Not a vocabulary identifier / UNDEF table')
        return Table(tuple(tuple(row) for row in data), width)

    def columns(self, expression, before):
        expression = expression.strip()
        value = self.resolve(expression, before) if re.fullmatch(r'\w+', expression) else constant_array(expression)
        if not isinstance(value, list) or any(len(pair) != 2 or any(type(i) is not int for i in pair) for pair in value):
            raise ValueError('Invalid join-column constant')
        return value


def literal_join(left, right, columns):
    """Independent calibration only; returned rows never become the gold result."""
    extras = [i for i in range(right.width) if i not in {b for _, b in columns}]
    rows = []
    for lrow in left.rows:
        matched = False
        for rrow in right.rows:
            if all(lrow[a] is None or rrow[b] is None or lrow[a] == rrow[b] for a, b in columns):
                joined = list(lrow)
                for a, b in columns:
                    if joined[a] is None: joined[a] = rrow[b]
                rows.append(tuple(joined + [rrow[i] for i in extras]))
                matched = True
        if not matched: rows.append(tuple(lrow) + (None,) * len(extras))
    return collections.Counter(rows)


def values(names, table):
    rows = ['(' + ' '.join('UNDEF' if v is None else '<urn:qlever:vid:' + str(v) + '>' for v in row) + ')' for row in table.rows]
    return 'VALUES (' + ' '.join('?' + name for name in names) + ') { ' + ' '.join(rows) + ' }'


def extract(text, path):
    masked, records, counts = mask_comments(text), [], collections.Counter()
    for method in TEST.finditer(masked):
        name = method.group(1)
        if name not in COHORT: continue
        end = base.block_end(masked, method.end())
        body = masked[method.end():end - 1]
        constants = Constants(body)
        for ordinal, call in enumerate(re.finditer(r'\btestOptionalJoin\s*\(', body), 1):
            stop = builder.close_paren(body, call.end())
            args = builder.split_args(body[call.end():stop])
            if len(args) != 4: raise ValueError('Unexpected OptionalJoin helper signature')
            left, right = constants.table(args[0], call.start()), constants.table(args[1], call.start())
            columns, gold = constants.columns(args[2], call.start()), constants.table(args[3], call.start())
            if len({a for a, _ in columns}) != len(columns) or len({b for _, b in columns}) != len(columns):
                raise ValueError('Repeated join-column pair needs an explicit adapter')
            if any(not 0 <= a < left.width or not 0 <= b < right.width for a, b in columns):
                raise ValueError('Join column out of range')
            extra = [i for i in range(right.width) if i not in {b for _, b in columns}]
            if gold.width != left.width + len(extra): raise ValueError('Unexpected result width')
            if literal_join(left, right, columns) != collections.Counter(gold.rows):
                raise ValueError('Source assertion / decoding discrepancy: OptionalJoin.' + name)
            left_names, right_names = ['l' + str(i) for i in range(left.width)], ['r' + str(i) for i in range(right.width)]
            for a, b in columns: right_names[b] = left_names[a]
            projected = left_names + [right_names[i] for i in extra]
            query = 'SELECT ' + ' '.join('?' + n for n in projected) + ' WHERE {\n  ' + values(left_names, left) + '\n  OPTIONAL { ' + values(right_names, right) + ' }\n}'
            expected = [{projected[i]: {'type': 'uri', 'value': 'urn:qlever:vid:' + str(v)} for i, v in enumerate(row) if v is not None} for row in gold.rows]
            absolute = method.end() + call.start()
            case = builder.native_case(path, 'OptionalJoin.' + name + '.' + str(ordinal).zfill(2),
                text.count('\n', 0, absolute) + 1, query, projected, expected,
                text[absolute:method.end() + stop + 1],
                ['Prerequisite: a fresh empty repository. Both input relations are supplied verbatim as VALUES.',
                 'Vocabulary IDs are distinct IRIs; U is an unbound binding. Original expected rows and multiplicities are retained.',
                 'This ports result semantics, not native row ordering, lazy chunk boundaries, allocator state, or physical join selection.'])
            case['family'], case['nativeMethod'] = 'qlever-native-optional', 'OptionalJoin.' + name
            case['nativeInputs'] = {'left': {'width': left.width, 'rows': left.rows}, 'right': {'width': right.width, 'rows': right.rows},
                                    'joinColumns': columns, 'expectedWidth': gold.width, 'expectedRows': gold.rows}
            case['querySha256'] = base.digest(query.encode())
            records.append(case)
            counts[name] += 1
    if dict(counts) != COHORT: raise ValueError('Reviewed OPTIONAL cohort changed: ' + json.dumps(dict(counts), sort_keys=True))
    return records


def calibration():
    text = 'auto a=makeIdTableFromVector({{U},{1},}); { auto a=makeIdTableFromVector({{2}}); testOptionalJoin(a,a,{{0,0}},a); } /* auto a=bad; */ testOptionalJoin(a,a,{{0,0}},a);'
    masked = mask_comments(text)
    constants, calls = Constants(masked), list(re.finditer('testOptionalJoin', masked))
    assert constants.table('a', calls[0].start()).rows == ((2,),)
    assert constants.table('a', calls[1].start()).rows == ((None,), (1,))
    assert literal_join(Table(((None,), (1,)), 1), Table(((2,), (2,)), 1), [[0, 0]]) == collections.Counter({(2,): 2, (1,): 1})
    assert literal_join(Table(((None,),), 1), Table((), 1), [[0, 0]]) == collections.Counter({(None,): 1})
    changed = 'auto a=makeIdTableFromVector({{1}}); a.resize(0); testOptionalJoin(a,a,{{0,0}},a);'
    try: Constants(changed).table('a', changed.index('testOptionalJoin'))
    except ValueError: pass
    else: raise AssertionError('Mutation guard accepted changed source')


def main():
    calibration()
    path = base.VENDOR / 'qlever/test/engine/OptionalJoinTest.cpp'
    source = base.source(path)
    if source['gitBlobSha1'] != SOURCE_BLOB: raise ValueError('Unreviewed OptionalJoin source hash: ' + source['gitBlobSha1'])
    additions = extract(path.read_text(), path)
    corpus = [c for c in json.loads((base.OUT / 'cases.json').read_text()) if c['family'] != 'qlever-native-optional'] + additions
    corpus.sort(key=lambda c: (c['family'], c['source']['path'], c['name'], c['id']))
    if len({c['id'] for c in corpus}) != len(corpus): raise ValueError('Duplicate case IDs')
    base.cases = corpus
    inventory = json.loads((base.OUT / 'native-inventory.json').read_text())
    for n in inventory:
        ids = [c['id'] for c in additions if n['project'] == 'qlever' and n['name'] == c['nativeMethod'] and n['path'].endswith('test/engine/OptionalJoinTest.cpp')]
        if ids: n['disposition'], n['caseIds'] = 'adapted-query', ids
    (base.OUT / 'cases.json').write_text(json.dumps(corpus, ensure_ascii=False, indent=2) + '\n')
    (base.OUT / 'native-inventory.json').write_text(json.dumps(inventory, indent=2) + '\n')
    (base.OUT / 'optional-port-audit.json').write_text(json.dumps({'source': source, 'methods': COHORT, 'cases': len(additions), 'calibration': 'passed; source expectations were not rewritten'}, indent=2) + '\n')
    stats = json.loads((base.OUT / 'coverage.json').read_text())
    stats.update({'cases': len(corpus), 'status': dict(collections.Counter(c['status'] for c in corpus)), 'families': dict(collections.Counter(c['family'] for c in corpus)),
                  'optionalNativeCases': len(additions), 'nativeAdapted': sum(n['disposition'] == 'adapted-query' for n in inventory)})
    (base.OUT / 'coverage.json').write_text(json.dumps(stats, indent=2) + '\n')
    base.document()
    print('OPTIONAL_PORT_COVERAGE ' + json.dumps(stats), flush=True)
    print('IMPORT_ERRORS ' + (base.OUT / 'import-errors.json').read_text(), flush=True)

if __name__ == '__main__': main()
