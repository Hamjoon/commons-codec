# Fixed commit range

Every number below is produced by `python3 experiments/ktester-commit-survival/tools/check_commits.py --kt /Users/donggi/_projects/experiment-projects/KTester` from the Stage 0 CSV and this repository’s git objects. Input digest and observed upstream/master SHA are preserved in commits-check.json. The script verifies BASE occurs in `git rev-list --first-parent upstream/master`, and for every row checks `git rev-parse <sha>^1` against the preceding SHA (starting at BASE). Scope uses `git diff-tree --no-commit-id --name-only -r <parent> <sha>`.

Verified 539 selected links; additionally checked all 830 source-row links. The selected range contains 187 src/main-changing commits and 50 scope-changing commits. Its last SHA is f9808d1f01d3a4e08c0c46283ef447ee0af6e7d8 (2026-01-16T06:56:54-05:00). The full source list contains 77 scope-changing commits.

| Scope-changing milestone | First-parent index | Date | SHA |
|---:|---:|---|---|
| 25 | 363 | 2025-07-19T12:41:12-04:00 | ca27bd30b6170561786d421eb48f1535dc41336f |
| 50 | 539 | 2026-01-16T06:56:54-05:00 | f9808d1f01d3a4e08c0c46283ef447ee0af6e7d8 |
| 75 | 799 | 2026-09-19T06:09:25-07:00 | 54b7ad9d0406800cd54b213ea67a24311a32d2bf |
| 100 | not reached |  |  |

Release tags pointing directly onto the selected first-parent line (same verification invocation):

- rel/commons-codec-1.17.1: index 51, 965109705c5236b05011e1c45f47d991abfa521e, 2024-07-12T16:18:11Z.

The range was extended by the Part 1 addendum to the selected endpoint. These scope-change counts are facts for later review, not an instruction to extend it.
