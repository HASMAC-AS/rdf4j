from pathlib import Path
import hashlib
path = Path("bootstrap/collection-source.patch.gz.b64")
text = path.read_text()
expected = "06b5ff87e241ff4b9859bb05c9efa057b89211dbc9bbc9d9bbf9fe663e4124fe"
assert hashlib.sha256(text.encode()).hexdigest() == expected
repairs = [(3101, 3104, 'Pro', ''), (3469, 3471, 'rA', ''), (4888, 4889, 'G', ''), (5838, 5839, 'G', '')]
for start, end, old, new in reversed(repairs):
    assert text[start:end] == old
    text = text[:start] + new + text[end:]
assert hashlib.sha256(text.encode()).hexdigest() == "ff47df5c11ecb0156d3845775a589194af3c1965e0ce29cc730cad9774bf18e4"
path.write_text(text)
