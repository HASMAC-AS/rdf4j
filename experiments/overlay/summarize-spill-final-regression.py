#!/usr/bin/env python3
"""Summarize isolated shard reports, checking coverage against the exact selection."""
from pathlib import Path
import csv
import sys
import xml.etree.ElementTree as ET

root = Path(sys.argv[1]) if len(sys.argv) > 1 else Path(__file__).resolve().parent / 'spill-final-regression'
fields = ['shard', 'exit', 'selected_classes', 'reported_classes', 'tests', 'failures', 'errors', 'skipped', 'missing']
rows = []
for selection in sorted(root.glob('shard-*.selection')):
    stem = selection.with_suffix('')
    exit_file = stem.with_suffix('.exit')
    if not exit_file.exists():
        continue
    expected = set(selection.read_text().split(','))
    found = set()
    counts = {x: 0 for x in ['tests', 'failures', 'errors', 'skipped']}
    for report in Path(str(stem) + '-reports').glob('TEST-*.xml'):
        suite = ET.parse(report).getroot()
        if suite.attrib['name'] not in expected:
            continue
        found.add(suite.attrib['name'])
        for key in counts:
            counts[key] += int(suite.attrib.get(key, 0))
    rows.append(dict(shard=stem.name, exit=int(exit_file.read_text()), selected_classes=len(expected),
                     reported_classes=len(found), missing=';'.join(sorted(expected - found)), **counts))
with (root / 'summary.csv').open('w') as output:
    writer = csv.DictWriter(output, fieldnames=fields)
    writer.writeheader()
    writer.writerows(rows)
print((root / 'summary.csv').read_text())
print('Totals:', {key: sum(row[key] for row in rows) for key in fields[2:-1]})
