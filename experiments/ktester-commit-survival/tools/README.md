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
