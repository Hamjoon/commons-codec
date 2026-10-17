# Part 1 final status

Part 1 is complete. The population has 66 entries and 66 unique IDs. Renumbering changes 23 IDs and preserves 43 IDs, including every previously unique ID. The compose override is verified with the existing Maven cache retained.

All 66 candidate methods have non-empty focal-method and class-code fields. Only id, test-class and test-path were changed; all other fields and entry order are preserved. All 5 included original focal entries still match every author field up to CRLF/LF differences. The two private originals are excluded as authorized.

Evidence: population/id-renumbering-validation.json, population/id-renumbering.csv, results/part1-validation.json and results/container-wiring.json. Use dataset_info.json for Part 2; dataset_info.raw.json retains the unmodified DatasetPrepare output. Historical population-enumeration.json and population-validation.json retain the earlier stop records.

This report is rendered by `python3 experiments/ktester-commit-survival/tools/report_part1.py` from results/commits-check.json, results/developer-baseline.json, population/id-renumbering-validation.json, results/part1-validation.json and results/container-wiring.json.

Range: 539 verified first-parent commits, including 50 scope-changing commits. Developer denominator: 143, unchanged. The approved 20-artifact classpath and verified JaCoCo coverage mechanism remain unchanged (results/classpath-fetch.json and results/coverage-smoke.json).

No model calls, pushes, source/settings/index changes, or test generation occurred.

## First attempt

The first attempt stopped at Step 3 (commons-codec 05e849ed) after repeated Maven Central failures for EvoSuite 1.2.0 and JaCoCo 0.8.13-SNAPSHOT. Addendum 1 removed EvoSuite and selected released JaCoCo 0.8.12; all artifacts now resolve. It also extended the range to 539 commits.

## Second attempt

The next attempt stopped at Step 4 (commons-codec f2368151; KTester 2650707): the requirement to include original private focal methods conflicted with the non-private population rule. Addendum 2 explicitly excluded MurmurHash3.hash128x64Internal and Sha2Crypt.sha2Crypt and fixed membership at 66 methods.

## Third attempt and resolution

Addendum 2 stopped at Step 4 (commons-codec 09be111e; KTester 86fe574): DatasetPrepare emitted 66 entries but only 43 distinct IDs, with nine colliding IDs affecting 32 methods. The raw extraction has no missing/empty entries or author-field mismatches. Addendum 3 authorized post-processing the naming fields while leaving the authors’ Java and all other fields untouched.

Gary clarified that every previously unique ID must be preserved: only entries with colliding raw IDs are sorted and renumbered. This clarification governs the ambiguous full-group sorting sentence in the addendum. The final mapping changes 23 IDs, preserves all previously unique entries, and yields 66 unique IDs. All three stops are resolved.

## Next part

No open Part 1 decisions remain. Stop here: Part 2 generation has not started and no LLM call was made. Use the renumbered dataset_info.json and the authors’ existing project index when Part 2 is authorized. The fixed model, prompts and settings remain unchanged.
