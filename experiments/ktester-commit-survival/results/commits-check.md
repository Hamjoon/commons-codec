# Fixed commit range

Every number below is produced by `python3 experiments/ktester-commit-survival/tools/check_commits.py --kt /Users/donggi/_projects/experiment-projects/KTester` from the Stage 0 CSV and this repository’s git objects. Input digest and observed upstream/master SHA are preserved in commits-check.json. The script verifies BASE occurs in `git rev-list --first-parent upstream/master`, and for every row checks `git rev-parse <sha>^1` against the preceding SHA (starting at BASE). Scope uses `git diff-tree --no-commit-id --name-only -r <parent> <sha>`.

Verified 338 selected links; additionally checked all 830 source-row links. The selected range contains 100 src/main-changing commits and 21 scope-changing commits. Its last SHA is 1d718d8515091f49fe9c092455b17bacf3085809 (2025-07-18T21:51:03-04:00). The full source list contains 77 scope-changing commits.

| Scope-changing milestone | First-parent index | Date | SHA |
|---:|---:|---|---|
| 25 | 363 | 2025-07-19T12:41:12-04:00 | ca27bd30b6170561786d421eb48f1535dc41336f |
| 50 | 539 | 2026-01-16T06:56:54-05:00 | f9808d1f01d3a4e08c0c46283ef447ee0af6e7d8 |
| 75 | 799 | 2026-09-19T06:09:25-07:00 | 54b7ad9d0406800cd54b213ea67a24311a32d2bf |
| 100 | not reached |  |  |

The prescribed range is unchanged. These scope-change counts are facts for later review, not an instruction to extend it.
