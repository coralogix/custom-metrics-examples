Custom Metrics w/ Java
======================

In order to run the sample application, make sure the following environment variables are set:

```
export CX_ENDPOINT=<custom metrics endpoint>
export CX_TOKEN=<send your data api key>
mvn clean compile assembly:single && java -jar target/otel-java-sdk-example-1.0-SNAPSHOT-jar-with-dependencies.jar
```