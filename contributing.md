# Contributor Guide

## Build-related commands

This project uses [Maven](https://maven.apache.org/) for build automation.

Run from the project root directory:

&#x23; | Command | Description
--- | --- | ---
0 | `mvn verify -f benchmarks/pom.xml -DskipTests -DskipITs ; mvn verify -f examples/pom.xml -DskipTests -DskipITs ; mvn verify -f exercises/pom.xml -DskipTests -DskipITs` | Validate the project structure, check style, compilation etc., without running tests.
0.1 | `mvn clean -f benchmarks/pom.xml ; mvn clean -f examples/pom.xml ; mvn clean -f exercises/pom.xml` | Delete files generated at build-time.
1 | `mvn verify -f benchmarks/pom.xml` | Build the **`benchmarks`** sub-project and run benchmarks.
1.1 | `mvn verify -f benchmarks/pom.xml -Dsandbox.benchmark.dryRun=true` | Dry run when benchmarking.
1.2 | `mvn verify -f benchmarks/pom.xml -Dtest=AtomicApiComparisonBenchmark` | Run a specific benchmark. See https://maven.apache.org/surefire/maven-surefire-plugin/examples/single-test.html for more details.
2 | `mvn verify -f examples/pom.xml` | Build the **`examples`** sub-project and run unit tests.
2.1 | `mvn verify -f examples/pom.xml -P default,with-it-tests` | Also run intergation tests.
2.2 | `mvn verify -f examples/pom.xml -P default,with-it-tests -Dit.test=stincmale.sandbox.examples.brokentimestamps.JdbcTimestampItTest` | Run a specific integration test. See https://maven.apache.org/surefire/maven-failsafe-plugin/examples/single-test.html for more details.
3 | `mvn verify -f exercises/pom.xml` | Build the **`exercises`** sub-project and run unit tests.
