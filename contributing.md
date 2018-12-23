# Contributor Guide
## Build-related commands
This project uses [Maven](https://maven.apache.org/) for build automation.

Run from the project root directory:

&#x23; | Command | Description
--- | --- | ---
1 | `mvn clean verify -f benchmarks/pom.xml` | Build `benchmarks` sub-project.
2 | `mvn clean verify -f examples/pom.xml` | Build `examples` sub-project.
3 | `mvn clean verify -f exercises/pom.xml` | Build `exercises` sub-project.
4 | `mvn clean verify -f benchmarks/pom.xml && mvn clean verify -f examples/pom.xml && mvn clean verify -f exercises/pom.xml` | Combines everything.
