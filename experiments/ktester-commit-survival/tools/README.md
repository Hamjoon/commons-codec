# Generated-test classpath and coverage tools

The Part 1 addendum removes EvoSuite: it is a KTester-internal dependency and the generated smoke test compiled and ran without it. The supplied JaCoCo snapshot artifacts are replaced by the explicitly approved release artifacts. The original supplied-jar inventory remains in jar-inventory.json/md as provenance, not the current fetch plan.

Current configuration: classpath-artifacts.json and llm-classpath.txt. Artifacts with role=test are the only jars on compile/run classpaths. Artifacts with role=coverage are used solely via -javaagent or java -jar. Do not use work/libs/* for compilation now that this directory also contains coverage tools.

Measurement commands (from KTester/docker):

```sh
docker compose run --rm -v /Users/donggi/_projects/experiment-projects/commons-codec:/repo ktester bash -c '/repo/experiments/ktester-commit-survival/tools/fetch-llm-classpath.sh && python /repo/experiments/ktester-commit-survival/tools/coverage_smoke.py'
```

Every number below is rendered by `python3 experiments/ktester-commit-survival/tools/classpath_report.py` from results/classpath-fetch.json and results/coverage-smoke.json. The fetcher uses the existing shared Maven cache and Maven Central. Every fetched file has its byte size and SHA-256 recorded; test-runtime artifacts retain their original supplied digests.

All 20 of 20 approved artifacts are present: 18 test-runtime jars and 2 coverage tools.

| Role | Coordinate | Classifier | Local filename |
|---|---|---|---|
| test | org.apiguardian:apiguardian-api:1.1.2 |  | apiguardian-api-1.1.2.jar |
| test | net.bytebuddy:byte-buddy:1.14.1 |  | byte-buddy-1.14.1.jar |
| test | net.bytebuddy:byte-buddy-agent:1.14.1 |  | byte-buddy-agent-1.14.1.jar |
| test | commons-io:commons-io:2.16.1 |  | commons-io-2.16.1.jar |
| test | org.apache.commons:commons-lang3:3.14.0 |  | commons-lang3-3.14.0.jar |
| test | org.hamcrest:hamcrest:2.2 |  | hamcrest-2.2.jar |
| test | org.junit.jupiter:junit-jupiter:5.11.4 |  | junit-jupiter-5.11.4.jar |
| test | org.junit.jupiter:junit-jupiter-api:5.10.2 |  | junit-jupiter-api-5.10.2.jar |
| test | org.junit.jupiter:junit-jupiter-engine:5.10.2 |  | junit-jupiter-engine-5.10.2.jar |
| test | org.junit.jupiter:junit-jupiter-params:5.10.2 |  | junit-jupiter-params-5.10.2.jar |
| test | org.junit.platform:junit-platform-commons:1.10.2 |  | junit-platform-commons-1.10.2.jar |
| test | org.junit.platform:junit-platform-console-standalone:1.9.3 |  | junit-platform-console-standalone-1.9.3.jar |
| test | org.junit.platform:junit-platform-engine:1.10.2 |  | junit-platform-engine-1.10.2.jar |
| test | org.mockito:mockito-core:5.2.0 |  | mockito-core-5.2.0.jar |
| test | org.mockito:mockito-inline:5.2.0 |  | mockito-inline-5.2.0.jar |
| test | org.mockito:mockito-junit-jupiter:5.2.0 |  | mockito-junit-jupiter-5.2.0.jar |
| test | org.objenesis:objenesis:3.3 |  | objenesis-3.3.jar |
| test | org.opentest4j:opentest4j:1.3.0 |  | opentest4j-1.3.0.jar |
| coverage | org.jacoco:org.jacoco.agent:0.8.12 | runtime | org.jacoco.agent-0.8.12-runtime.jar |
| coverage | org.jacoco:org.jacoco.cli:0.8.12 | nodeps | org.jacoco.cli-0.8.12-nodeps.jar |

## Verified commands

Working directory inside the container: /repo/experiments/ktester-commit-survival. These exact commands compiled the original smoke class, ran it with the coverage agent, and emitted source-line XML:

```sh
javac -cp work/t/target/classes:work/libs/apiguardian-api-1.1.2.jar:work/libs/byte-buddy-1.14.1.jar:work/libs/byte-buddy-agent-1.14.1.jar:work/libs/commons-io-2.16.1.jar:work/libs/commons-lang3-3.14.0.jar:work/libs/hamcrest-2.2.jar:work/libs/junit-jupiter-5.11.4.jar:work/libs/junit-jupiter-api-5.10.2.jar:work/libs/junit-jupiter-engine-5.10.2.jar:work/libs/junit-jupiter-params-5.10.2.jar:work/libs/junit-platform-commons-1.10.2.jar:work/libs/junit-platform-engine-1.10.2.jar:work/libs/mockito-core-5.2.0.jar:work/libs/mockito-inline-5.2.0.jar:work/libs/mockito-junit-jupiter-5.2.0.jar:work/libs/objenesis-3.3.jar:work/libs/opentest4j-1.3.0.jar:work/libs/junit-platform-console-standalone-1.9.3.jar -d work/smoke/classes work/smoke/Md5Crypt_md5Crypt_Test.java
java '-javaagent:work/libs/org.jacoco.agent-0.8.12-runtime.jar=destfile=work/smoke/jacoco.exec,includes=org.apache.commons.codec.*' -cp work/t/target/classes:work/smoke/classes:work/libs/apiguardian-api-1.1.2.jar:work/libs/byte-buddy-1.14.1.jar:work/libs/byte-buddy-agent-1.14.1.jar:work/libs/commons-io-2.16.1.jar:work/libs/commons-lang3-3.14.0.jar:work/libs/hamcrest-2.2.jar:work/libs/junit-jupiter-5.11.4.jar:work/libs/junit-jupiter-api-5.10.2.jar:work/libs/junit-jupiter-engine-5.10.2.jar:work/libs/junit-jupiter-params-5.10.2.jar:work/libs/junit-platform-commons-1.10.2.jar:work/libs/junit-platform-engine-1.10.2.jar:work/libs/mockito-core-5.2.0.jar:work/libs/mockito-inline-5.2.0.jar:work/libs/mockito-junit-jupiter-5.2.0.jar:work/libs/objenesis-3.3.jar:work/libs/opentest4j-1.3.0.jar:work/libs/junit-platform-console-standalone-1.9.3.jar org.junit.platform.console.ConsoleLauncher --disable-banner --disable-ansi-colors --fail-if-no-tests --select-class org.apache.commons.codec.digest.Md5Crypt_md5Crypt_Test --details summary --reports-dir work/smoke/reports
java -jar work/libs/org.jacoco.cli-0.8.12-nodeps.jar report work/smoke/jacoco.exec --classfiles work/t/target/classes --sourcefiles work/t/src/main/java --xml work/smoke/jacoco.xml
```

Md5Crypt.java has 70 line entries; 57 covered lines and 275 covered instructions. The exact covered line numbers and all counters are in results/coverage-smoke.json. The XML is work/smoke/jacoco.xml; execution data is work/smoke/jacoco.exec. The compile and report commands exit successfully. The smoke class still has assertion failures, allowed by the smoke criterion; tests execute and produce non-zero coverage.

The original smoke source remains under ignored work/smoke/ only. No jar or authored generated test source is committed. Current coverage measurement does not rebuild the KTester project index or make model calls. Public logs normalize trailing whitespace; raw logs remain under ignored work/raw-logs/.

## First attempt

The initial exact-inventory fetch stopped after repeated failures resolving EvoSuite and the supplied JaCoCo snapshots. The first attempt is preserved in commit 05e849ed and its failure logs. The addendum explicitly resolves that stop through exclusion and released-version substitution; those old failure logs are historical evidence, not current unresolved dependencies.

## Part 3 runner and reproducible commands

Part 3 freezes its input hashes in `results/part3-input-sha256.json`. Developer compile units are resolved once from transitive same-package Java identifier/type references (comments and strings excluded) and recorded in `dev-compile-units.json`. The six units use the existing 18 test jars; no additional developer jar is needed. `Base64Test` references `BaseNCodecTest.getPresumableFreeMemory()`, so that helper class is compiled but its own tests are not selected. Frozen resources are included on the runtime classpath.

The runner copies each source into a private unit directory without changing bytes. It strips `r2-` only from the temporary Java filename. Production and test outputs are isolated; each worker owns `work/commits/wN/`, exported with `git archive`. `mvn -q -o -DskipTests -Drat.skip -Dcheckstyle.skip -Dspotbugs.skip -Dpmd.skip -Dmaven.javadoc.skip package` uses the Part 1 skip set. A cache-miss failure may warm Maven online on its second attempt; other build failures get a second offline attempt. Every attempt is recorded. Build-fail commits are excluded during aggregation and carry forward the preceding cumulative state.

Phase A executes all six developer classes and the frozen LLM denominator methods from all 46 contributing files. LLM methods excluded by Part 2 are not reintroduced into the measured set. Each file compiles individually. `MethodSelectors.java` reads compiled JUnit annotations and exact parameter types without initializing the class, distinguishing tests from private overloads and selecting parameterized methods without guessing source type names. Method identity is `(file, class, method)` so identically named classes from different rounds remain distinct. All invocations of a parameterized method must pass. A fail/error method is rerun once, in isolation; if that retry passes, its final state is pass with `flaky=true`. Otherwise its original failure state is retained and both outcomes recorded. A whole-class run timeout marks all selected denominator methods timeout. Skipped or unreported methods are absent. Every Java run is limited to 600 seconds with process-group termination.

Phase B selects only methods alive entering each commit (index 0 uses the full denominator). It appends coverage within each isolated group using the same released JaCoCo 0.8.12 tools as Part 1. A rejected method selector may fall back to the class, with the exact fallback recorded. The XML report includes all production classes, retaining executable but uncovered lines. An empty alive set produces zero-coverage XML without executing any tests. Step 0 verifies this case too.

The updated Part 3 addendum defines B using every `changes-src-main=yes` index and its predecessor. The frozen inventory has 187 such commits (50 change a scope file), yielding 263 distinct coverage indices including 0. This exceeds the addendum's estimate of 100/at most 200; the set formula governs. `prepare_coverage.py` records all `src/main/java` hunks, each file's scope/other group, and explicit rename/deletion status. Its independent five-file diff check verifies that scope projections retain the original definition. Phase B line CSVs include every source file in the codec package and subpackages.

The revised aggregation writes all/scope post-image columns and all-lines pre-image columns in `changed-lines.csv`. Table 3 has all-production rows and two totals rows (all files and five scope files); it has no separate table3b. The single all-lines Table 4 stays in the repository and is omitted from the handover report. `coverage-t.md` covers only the five scope files. All line counts and percentages are derived from the raw XML and validated against its line CSV.

`finish_part3.py` is a host-side continuation for the long-running measurement. It never starts or restarts Phase A. It waits for the existing Phase A completion record, verifies every raw record, creates the prescribed stage commits with the Codex trailer, derives survival, wires the expanded coverage extractor only after Phase A is committed, runs Phase B with eight workers, derives coverage, and renders the final ignored handover. It uses a process lock and an atomic stage ledger at `work/part3-continuation.json`. Before Aggregation 2 it re-reads the addendum and compares its digest with the reviewed version; a change stops continuation for inspection. It also stops on failed verification, worker failure, or unrelated staged changes, recording the reason in the ledger. It does not push. Do not launch manual stage commands while this continuation is active.

Host preflight: `python3 experiments/ktester-commit-survival/tools/finish_part3.py --kt /Users/donggi/_projects/experiment-projects/KTester --phase-a-container ktester-commit-survival-phase-a-8 --check`. Remove `--check` to continue the existing Phase A run. The Phase B container has the stable name `ktester-commit-survival-phase-b`, allowing a resumed continuation to wait for an already-running Phase B without duplicating workers. Stage logs are under KTester `evaluation/logs/commit-survival-part3-*.log`.

All commands below run inside the prescribed container, from `/repo/experiments/ktester-commit-survival`:

```sh
python tools/prepare_part3.py
python tools/run_commit.py --index 0 --group dev --phase check --worker w0
python tools/run_commit.py --index 0 --group llm --phase check --worker w0
python tools/check_runner.py
python tools/selftest_part3.py
# Commit Step 0 only after all denominator methods and coverage checks pass.
python tools/run_phase.py --phase a --workers 8 --start 1 --end 539
# Check/commit Phase A, then derive and commit survival tables.
python tools/aggregate_survival.py
python tools/prepare_coverage.py
python tools/run_phase.py --phase b --workers 8 --start 0 --end 539
# Check/commit Phase B, then derive and commit changed-line tables.
python tools/aggregate_coverage.py
```

Host wrapper: from KTester/docker, `docker compose -f compose.yml -f compose.commit-survival.yml run --rm ktester bash -c 'cd /repo/experiments/ktester-commit-survival && <command>'`. Do not use `bash -l`. Eight Python processes run interleaved indices after the user-authorized restart, each building and measuring both groups before moving to its next commit. No table is aggregated in a worker. Every completed group is atomically written to `results/raw/<index>-<group>.json` or `results/raw-coverage/<index>-<group>.json`. Resume skips only complete records with matching commit, frozen inputs, compatible runner hashes and selection. The sole Phase A compatibility exception is the explicitly recorded concurrency-only transition in `results/part3-worker-restart.json`; both old and resumed full hash maps must match exactly. Original raw records retain their actual runner hashes and bytes. Incomplete local group reports may be replaced on retry. More than 53 build-fail commits stops the phase; worker infrastructure exceptions are reported separately. Every phase invocation records its exact worker layout and wall time.

`run_commit.py` accepts `--index`, `--group dev|llm`, `--worker wN`, `--phase a|b|check|check-coverage`, optional `--selection <JSON array of frozen method ids>`, and `--coverage`. `--class-selection` is for explicit full-class smoke checks. Existing complete raw records are never overwritten. Failure messages, compile attempts, launcher commands and retry outcomes are retained. Logs/JUnit XML are local under ignored `work/phase-*/` and referenced by the public JSON.

Agent and report command shapes (absolute paths and exact argument arrays are in each raw record):

```sh
java -javaagent:work/libs/org.jacoco.agent-0.8.12-runtime.jar=destfile=work/phase-b/INDEX/GROUP/jacoco.exec,append=true,includes=org.apache.commons.codec.* -cp '<production>:<isolated-test-classes>:<frozen-resources>:<18-test-jars>' org.junit.platform.console.ConsoleLauncher --disable-banner --disable-ansi-colors --fail-if-no-tests --details summary --select-method 'CLASS#METHOD(PARAMETER_TYPES)' --reports-dir '<private-unit-report-dir>'
java -jar work/libs/org.jacoco.cli-0.8.12-nodeps.jar report work/phase-b/INDEX/GROUP/jacoco.exec --classfiles work/commits/wN/target/classes --sourcefiles work/commits/wN/src/main/java --xml results/coverage/INDEX-GROUP.xml
```

Scientific figures use Matplotlib isolated in ignored `work/python-libs`; resolved package versions are in `results/part3-plot-dependencies.json`. Reinstall in the container with `python -m pip install --target work/python-libs matplotlib` only if that local environment is missing. Python plotting libraries never enter the Java classpath. `selftest_part3.py` checks assertion/error classification, parameterized aggregation, missing/skipped states, process timeout, build carryover, cumulative/transient survival, alive-entering indexing, flaky history, and zero-context diff line ranges.

After final commits, render the ignored handover with `python tools/report_part3.py --kt /work` inside the container, or use the host KTester path when running on the host. Tables 1–3 and five-file baseline coverage are copied verbatim from scripted results; Table 4 remains repository-only. Both Table 3 totals rows, the actual B size, and Phase B wall time are included. Final Git hashes are read at render time. No push is performed.

The host reports 12 logical CPUs (`sysctl -n hw.ncpu`); Docker also exposes 12. At the user’s request, Phase A was restarted with eight workers to meet the Monday deadline, and Phase B was set to eight. The first invocation stopped after 68 complete commits (136 raw records); all four workers exited on SIGTERM. Its timing and layout remain in `results/phase-a-invocations/`. The interrupted latest marker was removed only after confirming the identical historical record, so the continuation waits for the resumed invocation. The continuation was also restarted with explicit user approval because its live process held the old hash validator and Phase B command in memory. No test, timeout, build flag, coverage rule, or scoring rule changed. `results/phase-a-running.json` records the current full layout.

`python3 tools/verify_worker_restart.py` (from the experiment folder) verifies preserved raw bytes and modification times, the new layout and provenance exception, and the first completed commits after the restart. It writes `results/part3-worker-restart-verification.json` once at least three new commits have both groups recorded. Live `phase-*-running.json` files are ignored because completed invocation files retain the final authoritative layout.
