# Contributor Guide

## Build-related commands

This project uses [Maven](https://maven.apache.org/) for build automation.

Run from the project root directory:

| &#x23; | Command                                                                                                                                                                 | Description                                                                                                                                 |
|--------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------|
| 0      | `mvn verify -f benchmarks/pom.xml -DskipTests -DskipITs ; mvn verify -f examples/pom.xml -DskipTests -DskipITs ; mvn verify -f exercises/pom.xml -DskipTests -DskipITs` | Validates the project structure, checks style, compilation etc., without running tests.                                                     |
| 0.1    | `mvn clean -f benchmarks/pom.xml ; mvn clean -f examples/pom.xml ; mvn clean -f exercises/pom.xml`                                                                      | Deletes files generated at build-time.                                                                                                      |
| 1      | `mvn verify -f benchmarks/pom.xml`                                                                                                                                      | Builds the **`benchmarks`** sub-project and runs benchmarks.                                                                                |
| 1.1    | `mvn verify -f benchmarks/pom.xml -Dsandbox.benchmark.dryRun=true`                                                                                                      | Runs benchmarks in dry run mode.                                                                                                            |
| 1.2    | `mvn verify -f benchmarks/pom.xml -Dtest=AtomicApiComparisonBench`                                                                                                      | Runs a specific benchmark. See <https://maven.apache.org/surefire/maven-surefire-plugin/examples/single-test.html> for more details.        |
| 2      | `mvn verify -f examples/pom.xml`                                                                                                                                        | Builds the **`examples`** sub-project and runs unit tests.                                                                                  |
| 2.1    | `mvn verify -f examples/pom.xml -P default,with-it-tests`                                                                                                               | Also runs intergation tests.                                                                                                                |
| 2.2    | `mvn verify -f examples/pom.xml -P default,with-it-tests -Dit.test=stincmale.sandbox.examples.brokentimestamps.JdbcTimestampItTest`                                     | Runs a specific integration test. See <https://maven.apache.org/surefire/maven-failsafe-plugin/examples/single-test.html> for more details. |
| 3      | `mvn verify -f exercises/pom.xml`                                                                                                                                       | Builds the **`exercises`** sub-project and runs unit tests.                                                                                 |
