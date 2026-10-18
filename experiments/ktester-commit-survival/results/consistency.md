# Consistency checks

- Survival is non-increasing for both groups: passed.
- Index 0 equals the frozen denominators (143 and 290): passed.
- Every Phase A raw file parsed: 1078 of 1078.
- Build-fail commits: 3; indices: [330, 396, 397].
- Fail/error method re-runs: 603; passed on retry (flaky=yes): 0.

## Coverage consistency

- Production-changing commits checked: 187; coverage commits: 263.
- Every required c and c−1 has XML: False; unavailable build-fail records: [{'index': 397, 'group': 'dev'}, {'index': 397, 'group': 'llm'}].
- Both groups have identical executable line sets for every reported production file at each jointly built commit: 262 passed.
- All codec-package XML line records and committed line CSVs agree exactly: passed.
- Scope line projections equal the original five-file diff definition at every production-changing commit: passed.
- Each group’s single all-lines exposure table accounts for all 187 production-changing commits, including explicitly unavailable cases: passed.
