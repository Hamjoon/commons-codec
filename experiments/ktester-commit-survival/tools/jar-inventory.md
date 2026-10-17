# Supplied jar inventory

All values: `python3 experiments/ktester-commit-survival/tools/inventory_classpath.py --kt /Users/donggi/_projects/experiment-projects/KTester`. Jars are read in place; no supplied jar is copied into the public record. All embedded pom.properties coordinates and full manifests are preserved in jar-inventory.json. For shaded jars, only the primary artifact is requested; embedded dependencies are not separately added.

| Source jar | Bytes | Primary coordinate | Coordinate evidence |
|---|---:|---|---|
| data/projects/commons-codec/libs/apiguardian-api-1.1.2.jar | 6806 | org.apiguardian:apiguardian-api:1.1.2 | NO pom.properties: coordinate inferred from jar filename and manifest; verify against Central bytes |
| data/projects/commons-codec/libs/byte-buddy-1.14.1.jar | 4184899 | net.bytebuddy:byte-buddy:1.14.1 | pom.properties |
| data/projects/commons-codec/libs/byte-buddy-agent-1.14.1.jar | 256702 | net.bytebuddy:byte-buddy-agent:1.14.1 | pom.properties |
| data/projects/commons-codec/libs/commons-io-2.16.1.jar | 508826 | commons-io:commons-io:2.16.1 | pom.properties |
| data/projects/commons-codec/libs/commons-lang3-3.14.0.jar | 657952 | org.apache.commons:commons-lang3:3.14.0 | pom.properties |
| data/projects/commons-codec/libs/hamcrest-2.2.jar | 123360 | org.hamcrest:hamcrest:2.2 | NO pom.properties: coordinate inferred from jar filename and manifest; verify against Central bytes |
| data/projects/commons-codec/libs/junit-jupiter-5.11.4.jar | 6367 | org.junit.jupiter:junit-jupiter:5.11.4 | NO pom.properties: coordinate inferred from jar filename and manifest; verify against Central bytes |
| data/projects/commons-codec/libs/junit-jupiter-api-5.10.2.jar | 210956 | org.junit.jupiter:junit-jupiter-api:5.10.2 | NO pom.properties: coordinate inferred from jar filename and manifest; verify against Central bytes |
| data/projects/commons-codec/libs/junit-jupiter-engine-5.10.2.jar | 244690 | org.junit.jupiter:junit-jupiter-engine:5.10.2 | NO pom.properties: coordinate inferred from jar filename and manifest; verify against Central bytes |
| data/projects/commons-codec/libs/junit-jupiter-params-5.10.2.jar | 586027 | org.junit.jupiter:junit-jupiter-params:5.10.2 | NO pom.properties: coordinate inferred from jar filename and manifest; verify against Central bytes |
| data/projects/commons-codec/libs/junit-platform-commons-1.10.2.jar | 106232 | org.junit.platform:junit-platform-commons:1.10.2 | NO pom.properties: coordinate inferred from jar filename and manifest; verify against Central bytes |
| code/dependencies/junit-platform-console-standalone-1.9.3.jar | 2614186 | org.junit.platform:junit-platform-console-standalone:1.9.3 | NO pom.properties: coordinate inferred from jar filename and manifest; verify against Central bytes |
| data/projects/commons-codec/libs/junit-platform-engine-1.10.2.jar | 204821 | org.junit.platform:junit-platform-engine:1.10.2 | NO pom.properties: coordinate inferred from jar filename and manifest; verify against Central bytes |
| data/projects/commons-codec/libs/mockito-core-5.2.0.jar | 687422 | org.mockito:mockito-core:5.2.0 | NO pom.properties: coordinate inferred from jar filename and manifest; verify against Central bytes |
| data/projects/commons-codec/libs/mockito-inline-5.2.0.jar | 1501 | org.mockito:mockito-inline:5.2.0 | NO pom.properties: coordinate inferred from jar filename and manifest; verify against Central bytes |
| data/projects/commons-codec/libs/mockito-junit-jupiter-5.2.0.jar | 5472 | org.mockito:mockito-junit-jupiter:5.2.0 | NO pom.properties: coordinate inferred from jar filename and manifest; verify against Central bytes |
| data/projects/commons-codec/libs/objenesis-3.3.jar | 49423 | org.objenesis:objenesis:3.3 | NO pom.properties: coordinate inferred from jar filename and manifest; verify against Central bytes |
| data/projects/commons-codec/libs/opentest4j-1.3.0.jar | 14304 | org.opentest4j:opentest4j:1.3.0 | NO pom.properties: coordinate inferred from jar filename and manifest; verify against Central bytes |
| code/dependencies/evosuite-1.2.0.jar | 19766471 | org.evosuite:evosuite-master:1.2.0 | primary embedded pom.properties; shaded dependency coordinates recorded separately |
| code/dependencies/jacocoagent.jar | 303380 | org.jacoco:org.jacoco.agent.rt:0.8.13-SNAPSHOT | primary embedded pom.properties; shaded dependency coordinates recorded separately |
| code/dependencies/jacococli.jar | 600653 | org.jacoco:org.jacoco.cli:0.8.13-SNAPSHOT | primary embedded pom.properties; shaded dependency coordinates recorded separately |

JaCoCo agent metadata names the internal org.jacoco.agent.rt artifact; a published runtime classifier or replacement release must not silently be substituted. JaCoCo CLI is the nodeps shaded classifier. The supplied snapshot versions are preserved verbatim, pending actual repository resolution.
