This is a JMH benchmark comparing techniques for generating distinct world ids
for the TechEmpower Framework Benchmarks Multi-query and Updates tests.

To build:

    mvn clean package

To run:

    java -jar target/benchmarks.jar

To run and profile the garbage collector:

    java -jar target/benchmarks.jar -prof gc
