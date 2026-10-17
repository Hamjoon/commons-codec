# Part 1 status after addendum 2

Part 1 remains incomplete at Step 4: DatasetPrepare produced 66 entries, but only 43 distinct IDs. 9 IDs collide across 32 methods. The second addendum requires stopping on collisions, so Step 5 was not entered. Raw output is retained without ID repairs or Java patches.

All 66 candidate methods are present with non-empty focal-method and class-code fields. All 5 included original focal entries match every author field up to CRLF/LF differences. The two private originals are excluded as authorized.

Evidence and exact invocation: population/population-validation.json and population/README.md. The latter lists every affected signature. The raw metadata and dataset output are tracked in both repositories as diagnostic evidence, not a generation-ready dataset.

This report is rendered by `python3 experiments/ktester-commit-survival/tools/report_part1.py` from results/commits-check.json, results/developer-baseline.json and population/population-validation.json.

Range: 539 verified first-parent commits, including 50 scope-changing commits. Developer denominator: 143, unchanged. The approved 20-artifact classpath and verified JaCoCo coverage mechanism remain unchanged (results/classpath-fetch.json and results/coverage-smoke.json).

No model calls, pushes, source/settings/index changes, or test generation occurred.

## First attempt

The first attempt stopped at Step 3 (commons-codec 05e849ed) after repeated Maven Central failures for EvoSuite 1.2.0 and JaCoCo 0.8.13-SNAPSHOT. Addendum 1 removed EvoSuite and selected released JaCoCo 0.8.12; all artifacts now resolve. It also extended the range to 539 commits.

## Second attempt

The next attempt stopped at Step 4 (commons-codec f2368151; KTester 2650707): the requirement to include original private focal methods conflicted with the non-private population rule. Addendum 2 explicitly excluded MurmurHash3.hash128x64Internal and Sha2Crypt.sha2Crypt and fixed membership at the enumerated 66 methods. That conflict is resolved. The historical enumeration JSON/driver retain their original stop record; population-validation.json records the current state.

## Current stop and remaining work

The unmodified packaged DatasetPrepare reused overload IDs. Its source updates the suffixed ID counter instead of the base counter in getMethodInfo; the observed collisions are recorded without repairing the output. No missing/empty entries or author-field mismatches were observed. The persistent compose override and final successful Part 1 completion remain pending under the explicit stop rule.

TODO (Gary): Decide how to resolve the DatasetPrepare ID collisions while keeping all 66 methods. The current instructions prohibit patching DatasetPrepare.java; no Java patch or output-ID repair was applied.
