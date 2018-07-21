## About
Contains performance benchmarks.

## Usage
Run the following command in order to run all benchmarks
```
mvn clean test -Dstincmale.sandbox.benchmarks.dryRun=false
```

Run the following command in order to run a specific benchmark
```
mvn clean test -Dtest=RemainderPerformanceTest -Dstincmale.sandbox.benchmarks.dryRun=false
```