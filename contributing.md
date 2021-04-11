# Contributor Guide

## Build-related commands

This project uses [Maven](https://maven.apache.org/) for build automation.

Run from the project root directory:

&#x23; | Command | Description
--- | --- | ---
1 | `mvn verify -f benchmarks/pom.xml` | Build the **`benchmarks`** sub-project and run benchmarks.
1.1 | `mvn verify -f benchmarks/pom.xml -Dsandbox.benchmark.dryRun=true` | Dry run when benchmarking.
1.2 | `mvn verify -f benchmarks/pom.xml -Dtest=AtomicApiComparisonTest` | Run a specific benchmark.
2 | `mvn verify -f examples/pom.xml` | Build the **`examples`** sub-project and run unit tests.
2.1 | `mvn verify -f examples/pom.xml -P with-integration-tests` | Also run intergation tests.
4 | `mvn verify -f exercises/pom.xml` | Build the **`exercises`** sub-project and run unit tests.
5 | `mvn clean -f benchmarks/pom.xml ; mvn clean -f examples/pom.xml ; mvn clean -f exercises/pom.xml` | Delete files generated at build-time.
