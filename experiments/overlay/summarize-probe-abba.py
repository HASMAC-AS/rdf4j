#!/usr/bin/env python3
import csv
import json
import pathlib
import re

root = pathlib.Path(__file__).resolve().parent
fields = ['run', 'reuse', 'query_s', 'elapsed_s', 'user_s', 'system_s', 'max_rss_kib',
          'major_faults', 'filesystem_inputs', 'filesystem_outputs', 'gc_pause_count',
          'gc_pause_ms', 'gc_max_pause_ms', 'reused_slots', 'exit_code']
with (root / 'probe-abba-summary.csv').open('w') as output:
    writer = csv.DictWriter(output, fieldnames=fields)
    writer.writeheader()
    for run, reuse in enumerate(['false', 'true', 'true', 'false'], 1):
        stem = root / f'probe-abba-{run}-{reuse}'
        if not stem.with_suffix('.json').exists():
            continue
        row = json.loads(stem.with_suffix('.json').read_text())
        row.pop('minor_faults')
        log = stem.with_suffix('.log').read_text()
        gc = stem.with_suffix('.gc.log').read_text()
        pauses = [float(x) for x in re.findall(r'Pause[^\n]* ([0-9.]+)ms', gc)]
        row.update(run=run, reuse=reuse,
                   query_s=float(re.search(r'run=0 .*?millis=([0-9.]+)', log)[1]) / 1000,
                   reused_slots=int(re.search(r'membershipMissSlotsReused=(\d+)', log)[1]),
                   gc_pause_count=len(pauses), gc_pause_ms=sum(pauses),
                   gc_max_pause_ms=max(pauses, default=0))
        writer.writerow(row)
print((root / 'probe-abba-summary.csv').read_text())
