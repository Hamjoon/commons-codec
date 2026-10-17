# Fixed generated-test classpath and smoke commands

Status: **Step 3 incomplete; Part 1 stopped here under the repeated-failure rule.** Exact supplied EvoSuite and JaCoCo coordinates did not resolve from Maven Central. No alternate versions/repositories or supplied-jar copies were used to bypass that failure. Population generation and the persistent compose override were not started.

All numbers below come from tools/jar-inventory.json, results/classpath-fetch.json, and results/classpath-smoke.json. Measurements were made with these commands:

```sh
python3 experiments/ktester-commit-survival/tools/inventory_classpath.py --kt /Users/donggi/_projects/experiment-projects/KTester
# From KTester/docker:
docker compose run --rm -v /Users/donggi/_projects/experiment-projects/commons-codec:/repo ktester bash -c '/repo/experiments/ktester-commit-survival/tools/fetch-llm-classpath.sh'
docker compose run --rm -v /Users/donggi/_projects/experiment-projects/commons-codec:/repo ktester bash -c 'python /repo/experiments/ktester-commit-survival/tools/smoke_classpath.py'
```

Render this report with `python3 experiments/ktester-commit-survival/tools/report_part1.py`.

Requested 21 supplied jars; resolved 18; unresolved 3. Every resolved jar’s SHA-256 equals its supplied counterpart. Names, byte sizes, embedded Maven coordinates and manifests are recorded in jar-inventory.md/json. llm-classpath.txt lists one primary coordinate per jar; comments identify inferred coordinates where pom.properties is absent. Fetching uses the compose ktester-m2 volume at /root/.m2, online mode, and pinned maven-dependency-plugin 3.8.1. No jar is committed.

## Unresolved exact artifacts

| Supplied jar | Requested coordinate | Classifier | Failed attempts | Evidence |
|---|---|---|---:|---|
| evosuite-1.2.0.jar | org.evosuite:evosuite-master:1.2.0 | (none) | 2 | results/classpath-evosuite-1.2.0.jar-attempt2.txt |
| jacocoagent.jar | org.jacoco:org.jacoco.agent.rt:0.8.13-SNAPSHOT | (none) | 2 | results/classpath-jacocoagent.jar-attempt2.txt |
| jacococli.jar | org.jacoco:org.jacoco.cli:0.8.13-SNAPSHOT | nodeps | 2 | results/classpath-jacococli.jar-attempt2.txt |

Each Maven log reports that the requested artifact is absent from https://repo.maven.apache.org/maven2. JaCoCo agent pom.properties names the internal org.jacoco.agent.rt module, and both JaCoCo jars use snapshot versions. A published agent runtime classifier/release is not silently substituted. EvoSuite is inventoried because the instruction includes code/dependencies; it is not needed to execute this standalone JUnit smoke test.

## Independent compile and launcher smoke

The original Md5Crypt_md5Crypt_Test.java was selected because the Stage 4c coverage log records its successful compilation and execution: `rg -n "Md5Crypt_md5Crypt_Test" KTester/results/stage4c/commons-codec/stage4c-commons-codec-coverage.log`. Only an ignored working copy under work/smoke/ is used here; the authored test is not committed. Its hash is in results/classpath-smoke.json.

The smoke uses the resolved project libraries plus the console standalone jar. Runtime classpath order explicitly places project libraries before the console bundle, as KTester does; this avoids relying on wildcard order for overlapping JUnit classes. This verifies compilation and execution only. It does not satisfy the missing-artifact requirement or establish coverage tooling.

Working directory inside the container: `/repo/experiments/ktester-commit-survival`. Exact commands:

```sh
javac -cp 'work/t/target/classes:work/libs/*' -d work/smoke/classes work/smoke/Md5Crypt_md5Crypt_Test.java
java -cp work/t/target/classes:work/smoke/classes:work/libs/apiguardian-api-1.1.2.jar:work/libs/byte-buddy-1.14.1.jar:work/libs/byte-buddy-agent-1.14.1.jar:work/libs/commons-io-2.16.1.jar:work/libs/commons-lang3-3.14.0.jar:work/libs/hamcrest-2.2.jar:work/libs/junit-jupiter-5.11.4.jar:work/libs/junit-jupiter-api-5.10.2.jar:work/libs/junit-jupiter-engine-5.10.2.jar:work/libs/junit-jupiter-params-5.10.2.jar:work/libs/junit-platform-commons-1.10.2.jar:work/libs/junit-platform-engine-1.10.2.jar:work/libs/mockito-core-5.2.0.jar:work/libs/mockito-inline-5.2.0.jar:work/libs/mockito-junit-jupiter-5.2.0.jar:work/libs/objenesis-3.3.jar:work/libs/opentest4j-1.3.0.jar:work/libs/junit-platform-console-standalone-1.9.3.jar org.junit.platform.console.ConsoleLauncher --disable-banner --disable-ansi-colors --fail-if-no-tests --select-class org.apache.commons.codec.digest.Md5Crypt_md5Crypt_Test --details summary --reports-dir work/smoke/reports
```

Compile exit: 0 (0.422 s). Launcher exit: 1 (0.369 s). Launcher XML reports 14 tests, 2 failures, 0 errors, 0 skipped. Assertion failures are allowed for this smoke criterion; the launcher did execute the class. Full output is in results/classpath-smoke-compile.txt and results/classpath-smoke-launch.txt. Public logs normalize trailing whitespace; exact raw output is retained locally under ignored work/raw-logs/.

## Stop condition

The controlling Part 1 instructions say: “If a command fails twice for the same reason, record it and stop at the end of the current step with a handover rather than improvising around it.” Each unresolved artifact failed both bounded attempts. The independent smoke check finishes the current step’s usable evidence; the overall classpath remains blocked. Do not treat this status as permission to start Part 2.
