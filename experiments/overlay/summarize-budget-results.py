#!/usr/bin/env python3
"""Extract warmup and measured query evidence without treating either as a statistical claim."""
import csv
import pathlib
import re
root = pathlib.Path(__file__).parent
fields = ['log', 'run', 'rows', 'checksum', 'millis', 'threadAllocated', 'compiledBinds', 'distinctPrimitive', 'distinctObjects', 'memoProbes', 'memoHits', 'admitted', 'migrated', 'spillCreated', 'spillDeleted', 'usedBefore', 'usedAfter', 'processPeak', 'globalLimit', 'perQueryLimit']
paths = sorted(set(root.glob('budget-comparison-[1-4]-*.log')) | set(root.glob('budget-spill-million.log')) | set(root.glob('budget-eight-million-*.log')) | set(root.glob('wide-budget-query-smoke.log')))
with (root / 'budget-measurements.csv').open('w', newline='') as stream:
    writer = csv.DictWriter(stream, fieldnames=fields)
    writer.writeheader()
    for path in paths:
        budget = None
        for line in path.read_text().splitlines():
            if line.startswith('budget admitted='):
                budget = dict(re.findall(r'(\w+)=(-?\d+(?:\.\d+)?)', line))
            elif line.startswith('run=') and budget is not None:
                row = dict(re.findall(r'(\w+)=(-?\d+(?:\.\d+)?)', line))
                row.update(budget)
                row['log'] = path.name
                writer.writerow({field: row.get(field, '') for field in fields})
                budget = None
