# Round 2 exploratory candidate results

These four short JMH screens informed candidate selection. **They are not the
final comparison of published and promoted production code.** Each screen is
reported independently; do not subtract or divide scores from different
screens. The shared Chunked candidate, for example, measured 26.96 µs/row for
ASCII in the first screen and 23.62 µs/row in the later bounded screen.

All JSON files report JMH 1.37, OpenJDK 25.0.5, one thread, one fork, two
one-second warmup iterations, and three one-second measurement iterations.
Many reported confidence intervals are very wide. Tables show the measured
point estimates, rounded for readability; the linked JSON files retain the
iteration samples and errors. These are controlled writer workloads, not
end-to-end query or production HTTP measurements.

The large, record, and bounded screens use 4,096-row documents with the real
heartbeat payload wrapper, stable namespaces, and preconstructed inputs.
Their JMH operation is normalized to one row. The small screen instead uses
memory output and reports **one complete document**, including a new writer,
per operation. Allocation comes from `gc.alloc.rate.norm`: it includes other
measured writer/model/sink activity, not just UTF-8 arrays.

## 1. Large-string alternatives

Source: [screen-large.json](screen-large.json), all 24 cases. “Published” is
`741b938`; validated bytes checks surrogate validity then calls JDK
`String.getBytes(UTF_8)`. Chunked validates/counts then manually encodes into
bounded scratch; exact bytes validates/counts then manually encodes into one
exact-sized array. All retain the published short-string path.

Each long workload replaces one of eight columns with a varying long literal.
ASCII, Latin-1, CJK, emoji, and sparse fixtures contain 8,192 UTF-16 units;
mixed Unicode contains 8,870. Every label has a four-character ASCII row
prefix. Sparse Unicode ends with one CJK character. Other columns also differ
between the mixed-Unicode workload and the remaining long workloads, so
compare implementations within a workload.

**Time, µs/row**

| Workload | Published | Validated bytes | Chunked | Exact bytes |
| --- | ---: | ---: | ---: | ---: |
| ASCII | 32.00 | 16.03 | 26.96 | 23.45 |
| Mixed Unicode | 57.62 | 37.26 | 49.54 | 38.66 |
| Latin-1 | 47.28 | 32.94 | 32.00 | 39.34 |
| CJK | 63.31 | 29.41 | 38.56 | 31.75 |
| Emoji | 59.20 | 33.71 | 27.52 | 26.27 |
| Sparse Unicode | 48.62 | 13.43 | 28.39 | 35.02 |

**Allocated bytes/row**

| Workload | Published | Validated bytes | Chunked | Exact bytes |
| --- | ---: | ---: | ---: | ---: |
| ASCII | 9,505.6 | 8,569.5 | 361.5 | 8,569.5 |
| Mixed Unicode | 29,889.7 | 41,385.6 | 409.7 | 14,753.6 |
| Latin-1 | 27,601.7 | 33,161.6 | 361.6 | 16,761.6 |
| CJK | 63,721.8 | 49,537.5 | 361.6 | 24,945.6 |
| Emoji | 27,601.7 | 41,353.6 | 361.5 | 16,761.5 |
| Sparse Unicode | 9,505.7 | 33,169.4 | 361.5 | 8,577.6 |

Chunked's point estimates improved on Published in every case and eliminated
output-sized encoding arrays. Validated bytes was particularly fast for ASCII
and sparse Unicode, but retained large allocations; exact bytes reduced that
allocation to one payload array. This screen justified exploring a faster
bounded loop rather than selecting only the fastest ASCII result.

**JDK allocation nuance:** the Latin-1 and CJK fixtures are not entirely dense
non-ASCII text. Their four ASCII prefix characters mean the JDK's `2*N`
Latin-1 or `3*N` UTF-16 allocation does not fill exactly, so `getBytes` trims
into a second, nearly full payload array. For Latin-1, those capacities are
16,384 and 16,380 bytes; for CJK, 24,576 and 24,568. Array headers/alignment and
other row allocations explain the remaining difference from the measured
33,161.6 and 49,537.5 B/row. The one-array estimates for all-dense strings in
[the source analysis](large-string-analysis.md) therefore do not describe
these prefixed fixtures.

## 2. Complete-record staging and adaptive scratch

Source: [screen-records.json](screen-records.json), all 24 cases.
Full record stages complete records into one bulk output call. Prefix record
also continues after the already-copied ASCII prefix rather than restarting
Unicode encoding. Adaptive record adds a 132-byte initial scratch array with
bounded growth to 4,100 bytes.

**Time, µs/row**

| Workload | Published | Full record | Prefix record | Adaptive record |
| --- | ---: | ---: | ---: | ---: |
| ASCII | 2.90 | 1.32 | 1.26 | 1.21 |
| Unicode | 2.41 | 1.29 | 1.11 | 1.13 |
| Distinct LMDB | 2.27 | 1.07 | 1.16 | 1.03 |
| Wide (32 columns) | 9.94 | 6.45 | 6.72 | 6.55 |
| Medium ASCII | 5.30 | 3.31 | 3.41 | 2.84 |
| Medium Unicode | 5.03 | 3.82 | 3.32 | 3.45 |

Record staging had lower point estimates than Published for every workload.
The smaller distinctions among staging variants were mixed and noisy:
Adaptive was not fastest for wide or medium-Unicode results. Their allocation
was broadly similar at this document size: about 360–361 B/row for ASCII and
medium ASCII, 408–410 for Unicode, 1,441 for wide, and 0.36–1.34 for distinct
LMDB wrappers. Constructor savings are heavily amortized over 4,096 rows.

## 3. Small documents

Source: [screen-small.json](screen-small.json), all nine cases. These are
memory-sink results; they must not be compared numerically with the heartbeat
row-normalized tables.

**Time, µs/document**

| Workload | Published | Full record | Adaptive record |
| --- | ---: | ---: | ---: |
| 0 rows | 1.34 | 1.29 | 0.62 |
| 1 rows | 3.07 | 2.39 | 1.86 |
| 8 rows | 12.26 | 7.44 | 8.38 |

**Allocated bytes/document**

| Workload | Published | Full record | Adaptive record |
| --- | ---: | ---: | ---: |
| 0 rows | 5,000.0 | 5,000.0 | 1,032.0 |
| 1 rows | 5,792.0 | 5,792.0 | 1,824.0 |
| 8 rows | 8,256.1 | 8,208.1 | 4,240.1 |

Adaptive scratch removed 3,968 B/document relative to Full record in each
case, matching the smaller initial array. It was faster at zero and one row,
but its eight-row point estimate was slower than Full record. Choosing
adaptive allocation therefore trades some possible growth/branch overhead
for a substantially smaller small-document allocation footprint.

## 4. Bounded loops and combined candidate

Source: [screen-bounded.json](screen-bounded.json), all 18 cases. Bounded
chunked moves capacity decisions outside inner encoding loops and has a
separate ASCII copy loop. Combined adds complete-record/prefix staging and
adaptive scratch, growing scratch to 4,100 bytes before a large-string chunk
write. This prevents an adaptive writer from issuing 128-byte long-string
chunks.

**Time, µs/row**

| Workload | Chunked | Bounded chunked | Combined |
| --- | ---: | ---: | ---: |
| ASCII | 23.62 | 28.94 | 21.35 |
| Mixed Unicode | 54.80 | 37.12 | 34.74 |
| Latin-1 | 31.01 | 31.79 | 30.15 |
| CJK | 34.01 | 32.35 | 32.71 |
| Emoji | 28.56 | 28.97 | 28.66 |
| Sparse Unicode | 29.18 | 29.89 | 31.58 |

**Allocated bytes/row**

| Workload | Chunked | Bounded chunked | Combined |
| --- | ---: | ---: | ---: |
| ASCII | 361.5 | 361.5 | 361.5 |
| Mixed Unicode | 409.7 | 409.6 | 409.6 |
| Latin-1 | 361.6 | 361.6 | 361.6 |
| CJK | 361.6 | 361.6 | 361.6 |
| Emoji | 361.5 | 361.5 | 361.6 |
| Sparse Unicode | 361.5 | 361.5 | 361.6 |

The unfavorable standalone ASCII result is material: Bounded chunked measured
28.94 versus Chunked's 23.62 µs/row, approximately 22.5% slower in this screen.
Its three iterations ranged from 26.51–32.01 µs/row, versus 22.25–24.81 for
Chunked; the short-run confidence intervals are nevertheless wide. Bounded
chunked also had slightly worse Latin-1, emoji, and sparse point estimates.
Its main positive signal was mixed Unicode, 37.12 versus 54.80 µs/row, while
retaining the low allocation of chunking.

Combined improved the ASCII, mixed-Unicode, Latin-1, and CJK point estimates
relative to Chunked in this same screen. It did not win every case: sparse
Unicode was 31.58 versus 29.18 µs/row, and emoji was effectively tied. Its
selection rests on the combined design's record-write reduction, bounded
large-string allocation, stronger mixed-Unicode result, and small-document
memory savings—not a claim that every isolated change wins every workload.

The promoted combination requires a separate, longer published-versus-current
run for final improvement claims. These exploratory screens establish why the
candidate was worth that validation and expose the cases that remain worth
watching.
