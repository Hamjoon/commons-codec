# Part 1 status after addendum

The classpath and commit-range decisions are implemented. Part 1 stops during Step 4 because the fixed population requirements conflict: private methods are excluded, but the original scoped dataset contains private focal methods that are required to be included. No DatasetPrepare run or persistent compose override was performed after this stop.

All numbers: `python3 experiments/ktester-commit-survival/tools/report_part1.py`, reading results/commits-check.json, results/developer-baseline.json, results/classpath-fetch.json, results/coverage-smoke.json and population/population-enumeration.json. Their measurement invocations are recorded in the corresponding reports.

- Range: 539 first-parent commits; 187 change src/main; 50 change scoped source files.
- Developer denominator unchanged: 143 methods pass all runs, from 144 scoped methods. The skipped Base64Test.testCodec265 remains excluded.
- Current artifact set: 20/20 resolved with recorded SHA-256. EvoSuite removed; coverage uses released JaCoCo. The XML has 70 Md5Crypt.java line entries and 57 covered lines.
- Eligible population: 66 methods in 5 classes, within the required bound. Original scoped focal methods missing under the non-private rule: 2.
- Overload groups enumerated: 12. Generated-ID uniqueness and original-entry round trips were not run because the earlier fixed-decision conflict requires stopping.

## Conflicting original focal methods

- `org.apache.commons.codec.digest.MurmurHash3.hash128x64Internal(byte[], int, int, long)`: private at upstream line 515 (Spoon evidence in population-enumeration.json).
- `org.apache.commons.codec.digest.Sha2Crypt.sha2Crypt(byte[], String, String, int, String)`: private at upstream line 170 (Spoon evidence in population-enumeration.json).

The candidates and enumeration JSON are committed as diagnostic evidence in both repositories. No dataset_meta.json or dataset_info.json exists for this population. No source/settings/index changes, model calls, generated tests, or pushes occurred. Temporary compose run -v mounts preserve the original cache and mounts; Step 5 persistent wiring was not reached.

## First attempt

The initial run stopped at Step 3 after repeated Maven Central failures for EvoSuite and JaCoCo snapshots (commit 05e849ed). The user addendum excluded EvoSuite, selected released coverage jars, and extended the range. Those original blockers are resolved.

TODO (Gary): Reconcile the requirement to include the original private focal methods with the non-private population rule. DatasetPrepare itself also rejects private methods; do not silently patch it or drop required methods. Then resume metadata generation, overload-ID validation and Step 5 wiring.
