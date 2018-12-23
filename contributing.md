# Contributor Guide
## Build-related commands
This project uses [Maven](https://maven.apache.org/) for build automation.

Run from the project root directory:

&#x23; | Command | Description
--- | --- | ---
1 | `mvn clean verify -f benchmarks/pom.xml -Dstincmale.sandbox.benchmarks.dryRun=false` | Build `benchmarks` sub-project and run all benchmarks. Consider using `-Dstincmale.ratmex.performance.dryRun=true` for dry runs. Take a look at `JmhOptions` to see/modify settings for performance tests.
2 | `mvn clean verify -f benchmarks/pom.xml -Dtest=RemainderPerformanceTest -Dstincmale.sandbox.benchmarks.dryRun=false` | Build `benchmarks` sub-project and run a specific benchmark.
3 | `mvn clean verify -f examples/pom.xml` | Build `examples` sub-project.
4 | `mvn clean verify -f exercises/pom.xml` | Build `exercises` sub-project.
5 | `mvn clean verify -f benchmarks/pom.xml -Dstincmale.sandbox.benchmarks.dryRun=false && mvn clean verify -f examples/pom.xml && mvn clean verify -f exercises/pom.xml` | Combines 1, 3, 4.
