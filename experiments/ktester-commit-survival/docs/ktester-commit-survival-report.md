# Do frozen tests see what changes? KTester tests on Commons Codec at commit granularity, 2024 to 2026

## Question

We repeat the commit-level survival measurement of the last two weeks with a third generator, KTester (Li et al., ICSE 2026), on Apache Commons Codec, and add the measurement requested this week: at every commit that changes production code, how many of the changed lines are executed by the developer tests and by the LLM tests that are still passing at that point. Survival keeps its fixed denominator; changed-line coverage is reported as a separate table beside it.

## Setup

- **Project and range.** Commons Codec, starting commit *t* = `cd16988` (2024-05-16), the commit the KTester authors' project index was built from. Points are the 539 consecutive first-parent commits on `master` after *t*, up to 2026-01-16. Three commits do not build (330, 396, 397); they carry the previous state and are excluded from evaluation. Release 1.17.1 is commit 51. Five classes are under test: `Base32`, `Base64`, `Md5Crypt`, `MurmurHash3`, `Sha2Crypt`.
- **Tests, frozen at *t*.** Developer: the 143 test methods of the six test classes written for the five classes, all passing three runs at *t*. LLM: KTester with `gpt-oss-120b`, prompts and repair loop as released, run twice over the 52 public non-deprecated methods of the five classes; 104 files produced, 47 compiled, 403 methods listed, 293 passing three runs, 3 duplicate bodies removed, 290 methods in the denominator. No test was edited.
- **Survival.** Cumulative over commits as before: a method survives at commit *c* if it passed at every evaluated commit up to *c*; denominators fixed at 143 and 290; a lost method is classified by its state at the first failing commit (compile failure, runtime exception, timeout, assertion failure, not reported).
- **Changed-line coverage (new).** For each of the 187 commits that change `src/main`, the lines added or modified by the commit that JaCoCo reports as executable at *c*, over all production files. Each group's methods still surviving when entering *c* are run at *c* under JaCoCo, and a changed line counts as covered if the group executed it. The table gives the count over all production files and, in parentheses, the part inside the five classes. Both test sets were selected for the five classes, so the five-class ratio shows how much of the change to the code under test the tests executed, and the all-files ratio shows how much of the project's change a test set frozen at *t* sees at all. Coverage of the five classes at *t* by the full denominators is reported as before for reference.

## Results

**Table 1. Surviving tests at every commit where a test was lost, and at the release tag** (count and percent of the fixed denominator; full 540-row series in the repository).

| # | Commit | Date | Change | Dev (143) | LLM (290) |
|---:|---|---|---|---:|---:|
| 0 | cd16988 (*t*) | 2024-05-16 | | 143 (100%) | 290 (100%) |
| 51 | **1.17.1** | 2024-07-12 | | 143 (100%) | 290 (100%) |
| 436 | 68199f1 | 2025-10-12 | Internal refactoring (`Base32`, `BaseNCodec` builder) | 142 (99.3%) | 289 (99.7%) |
| 438 | 937df54 | 2025-10-12 | Internal refactoring (line separator handling) | 142 (99.3%) | 287 (99.0%) |
| 444 | 42de020 | 2025-10-12 | Internal refactoring (`Base16`, `BaseNCodec`) | 142 (99.3%) | 286 (98.6%) |
| 445 | f9f750a | 2025-10-18 | `BaseNCodec.AbstractBuilder.setDecodeTable(byte[])` added, subclasses refactored | 142 (99.3%) | 285 (98.3%) |
| 539 | f9808d1 | 2026-01-16 | (end of range) | **142 (99.3%)** | **285 (98.3%)** |

**Table 2. Commits that stopped tests** (all four; no other commit stopped a test).

| # | Commit | Dev lost | LLM lost | Failure kind |
|---:|---|---:|---:|---|
| 436 | 68199f1 | 1 | 1 | assertion failure 2 (`Base32` constructor with null line separator no longer throws) |
| 438 | 937df54 | 0 | 2 | assertion failure 2 (empty line separator stored as null) |
| 444 | 42de020 | 0 | 1 | runtime exception 1 (`encodeTable` null check) |
| 445 | f9f750a | 0 | 1 | runtime exception 1 (`decodeTable` null check) |

**Table 3. Changed-line coverage at the commits that change production code** (lines: executable changed lines over all production files, in parentheses those in the five classes; covered: lines executed by the group's surviving tests at that commit; broken: tests of the group lost at that commit. The 23 commits where at least one line was covered are listed; the 59 others and the totals follow. The 105 production-changing commits with no executable changed line, such as Javadoc and formatting, are not rows.)

| # | Commit | Date | Change | Lines (five classes) | Dev covered | LLM covered | Dev broken | LLM broken |
|---:|---|---|---|---:|---:|---:|---:|---:|
| 1 | ad28b0f | 2024-05-16 | `Md5Crypt` throws on invalid prefix | 5 (5) | 3 (60.0%) | 3 (60.0%) | 0 | 0 |
| 65 | aaef8c5 | 2024-07-30 | Document protection needs | 1 (0) | 1 (100%) | 1 (100%) | 0 | 0 |
| 178 | 4d917fa | 2024-12-28 | Deprecate `Sha2Crypt` constructor | 2 (2) | 2 (100%) | 0 (0%) | 0 | 0 |
| 179 | 10fd9a4 | 2024-12-28 | Add missing Javadoc | 33 (4) | 4 (12.1%) | 0 (0%) | 0 | 0 |
| 196 | 9fb8ee2 | 2025-01-21 | Add `Base32.Builder.setHexDecodeTable` | 5 (5) | 2 (40.0%) | 2 (40.0%) | 0 | 0 |
| 260 | b4c7653 | 2025-03-22 | Base validation on standard table length | 1 (1) | 1 (100%) | 1 (100%) | 0 | 0 |
| 262 | 729bdc8 | 2025-03-22 | Rename private constants | 43 (41) | 41 (95.3%) | 41 (95.3%) | 0 | 0 |
| 264 | 6e5d0e0 | 2025-03-25 | Merge (`BaseNCodec`) | 16 (1) | 16 (100%) | 13 (81.2%) | 0 | 0 |
| 275 | e119f12 | 2025-05-03 | Normalize local variable naming | 9 (7) | 0 (0%) | 7 (77.8%) | 0 | 0 |
| 338 | 1d718d8 | 2025-07-18 | PMD fix in `Md5Crypt` | 1 (1) | 0 (0%) | 1 (100%) | 0 | 0 |
| 344 | 20df4c9 | 2025-07-19 | SpotBugs fix in `BaseNCodec` builder | 2 (1) | 2 (100%) | 2 (100%) | 0 | 0 |
| 345 | f49428a | 2025-07-19 | Defensive copy in `setLineSeparator` | 1 (0) | 1 (100%) | 0 (0%) | 0 | 0 |
| 348 | 46e8cdc | 2025-07-19 | Javadoc | 1 (1) | 1 (100%) | 0 (0%) | 0 | 0 |
| 369 | d6d4b82 | 2025-07-19 | Refactor duplicate code | 11 (6) | 8 (72.7%) | 5 (45.5%) | 0 | 0 |
| 416 | 0b9bd57 | 2025-09-27 | Javadoc | 1 (0) | 1 (100%) | 1 (100%) | 0 | 0 |
| 430 | 6ca5a08 | 2025-10-05 | SHAKE digests in `DigestUtils` | 18 (0) | 1 (5.6%) | 0 (0%) | 0 | 0 |
| 436 | 68199f1 | 2025-10-12 | Internal refactoring | 42 (25) | 36 (85.7%) | 38 (90.5%) | 1 | 1 |
| 438 | 937df54 | 2025-10-12 | Internal refactoring | 32 (31) | 31 (96.9%) | 31 (96.9%) | 0 | 2 |
| 444 | 42de020 | 2025-10-12 | Internal refactoring | 5 (0) | 2 (40.0%) | 2 (40.0%) | 0 | 1 |
| 445 | f9f750a | 2025-10-18 | `setDecodeTable` added, subclasses refactored | 54 (42) | 47 (87.0%) | 39 (72.2%) | 0 | 1 |
| 506 | d594dd2 | 2025-12-31 | Strict Standard and URL-safe `Base64` decoding methods | 29 (29) | 3 (10.3%) | 3 (10.3%) | 0 | 0 |
| 507 | 1351cb7 | 2025-12-31 | Sort members | 5 (5) | 5 (100%) | 5 (100%) | 0 | 0 |
| 508 | e620a5e | 2025-12-31 | Javadoc | 1 (1) | 1 (100%) | 1 (100%) | 0 | 0 |
| | 59 other commits | | all changes outside the five classes | 399 (0) | 0 (0%) | 0 (0%) | 0 | 0 |
| | **Total, all files** | | | **717 (208)** | **209 (29.1%)** | **196 (27.3%)** | 1 | 5 |
| | **Total, five classes only** | | | **208** | **162 (77.9%)** | **154 (74.0%)** | | |

**Table 4. Line coverage of the five classes at *t*** (full denominators executed; comparable to last week's Table 3).

| Class | Executable lines | Dev (143) | LLM (290) |
|---|---:|---:|---:|
| `Base32` | 185 | 182 (98.4%) | 162 (87.6%) |
| `Base64` | 179 | 175 (97.8%) | 164 (91.6%) |
| `Md5Crypt` | 70 | 63 (90.0%) | 68 (97.1%) |
| `MurmurHash3` | 262 | 261 (99.6%) | 104 (39.7%) |
| `Sha2Crypt` | 134 | 3 (2.2%) | 130 (97.0%) |
| Total | 830 | **684 (82.4%)** | **628 (75.7%)** |

The per-class differences follow from the test sets: `Sha2Crypt` has a single developer method (2.2%), and 13 deprecated `MurmurHash3` overloads were outside the generation population (LLM 39.7%).

## Observations

1. **Almost nothing broke, and what broke came from one refactoring.** Over 539 commits and 20 months the developer suite lost 1 of 143 methods and the LLM set lost 5 of 290, all at four commits in October 2025 that reworked the `BaseNCodec` builder (line separator and decode table handling); none of the six losses is a compile failure; 2 are runtime exceptions and 4 are assertion failures. The five classes were changed by 50 of the 539 commits, against 185 commits that lost 42% of developer tests and 32% of LLM tests in jsoup last week.
2. **Inside the five classes the two sets see the same changes, and every loss happened at a change they saw.** Of the 208 executable changed lines in the five classes, the developer tests executed 77.9% and the LLM tests 74.0%, and of the 23 commits that changed those lines the developer tests executed changed lines at 21 and the LLM tests at 18 (16 in common); at *t* the full sets cover 82.4% and 75.7% of the same files (Table 4). Each of the six lost tests failed at a commit whose changed lines the losing group had executed (Table 3, rows 436 to 445), and no test was lost at any of the 59 commits whose changed lines neither group executed.
3. **The all-files ratio is low because of files outside the five classes.** 509 of the 717 executable changed lines are in other files; the two sets executed 47 and 42 of them, almost all in `BaseNCodec`, the parent of `Base32` and `Base64`. The 59 commits with no covered line changed only such files, and the largest among them add code that did not exist at *t* (`CRC16`, stream builders), which no test set frozen at *t* can reach.

## Limitations

- 57 of the 104 generated files did not compile at *t*; 36 of them use the JUnit extension and Mockito annotations without importing them. Only methods passing three runs at *t* enter the denominator, so these failures reduce the LLM set but do not affect survival or coverage.
- Both sets target the five classes but not the same parts of them: generation covered the 52 public non-deprecated methods (14 deprecated and 2 private focal methods excluded by the tool) while the developer tests also exercise the deprecated ones, and the developer set for `Sha2Crypt` has a single method.
- Coverage was measured per group, not per test, so which test executed a changed line is not recorded.
- Coverage runs used a capped JVM heap, so one developer method that allocates about 2 GB did not run in those runs (the method aborts when the allocation fails); commit 397 (build failure) has no coverage.

## Repository

`Hamjoon/commons-codec`, branch `experiment/2026-10-week3-ktester-commit-survival`, directory `experiments/ktester-commit-survival/`.

- Commits: `results/commits.csv`.
- Survival: `results/<group>-survival.csv` (540 rows), `<group>-first-break.csv`, `table1-survival-by-event.md`, `table2-breaking-commits.md`, `figure1-survival-by-commit.png`.
- Changed-line coverage: `results/changed-lines.csv` (all 187 commits), `table3-changed-line-coverage.md` (all 82 rows), `coverage/<commit>-<group>-lines.csv`, `changes/<commit>.json`, `coverage-t.md`.
- Checks: `results/consistency.md`.
