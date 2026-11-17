#!/usr/bin/env python3
"""Measure one fresh JVM without an external time executable; never wraps multiple children."""
import json
import resource
import subprocess
import sys
import time

start = time.monotonic()
result = subprocess.run(sys.argv[2:])
usage = resource.getrusage(resource.RUSAGE_CHILDREN)
with open(sys.argv[1], 'w') as output:
    json.dump(dict(exit_code=result.returncode, elapsed_s=time.monotonic() - start,
                   user_s=usage.ru_utime, system_s=usage.ru_stime,
                   max_rss_kib=usage.ru_maxrss, major_faults=usage.ru_majflt,
                   minor_faults=usage.ru_minflt, filesystem_inputs=usage.ru_inblock,
                   filesystem_outputs=usage.ru_oublock), output, indent=2)
sys.exit(result.returncode)
