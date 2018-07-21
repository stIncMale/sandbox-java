# sandbox
<p align="right">
<a href="http://www.oracle.com/technetwork/java/javase/overview/index.html"><img src="https://img.shields.io/badge/Java-9+-blue.svg" alt="Java requirement"></a>
</p>

## About
A repository for examples (for [Tech Blog](https://sites.google.com/site/aboutmale/techblog)), temporal, test and research Java projects.

## Usage
Run the following command in order to install common Maven POM artifacts your local Maven repository  
```
mvn clean install -f root.xml && mvn clean install -f version.xml && mvn clean install -f build.xml
```

Use the following command from inside any sub-project (e.g. `exercises`, or `examples`) in order to build it or run tests/benchmarks 
```
mvn clean package` or `mvn clean test
``` 

---

All content is licensed under [![WTFPL logo](http://www.wtfpl.net/wp-content/uploads/2012/12/wtfpl-badge-2.png)](http://www.wtfpl.net/), except where another license is explicitly specified.
