# Contributor Guide
## Build-related commands
This project uses [Maven](https://maven.apache.org/) for build automation.

Run from the project root directory:

&#x23; | Command | Description
--- | --- | ---
1 | `mvn clean test -f benchmarks/pom.xml` | Build **`benchmarks`** sub-project and run all tests. Consider using `-Dsandbox.dryRun=true` for dry runs.
2 | `mvn clean test -f benchmarks/pom.xml -Dtest=AtomicApiComparisonTest` | Similar to 1, but runs a specific test.
3 | `mvn clean verify -f examples/pom.xml` | Build **`examples`** sub-project.
4 | `mvn clean verify -f exercises/pom.xml` | Build **`exercises`** sub-project.
5 | `mvn clean test -f benchmarks/pom.xml && mvn clean verify -f examples/pom.xml && mvn clean verify -f exercises/pom.xml` | Combines 1, 3, 4.
