# jfr-datasource

This demonstrates how a simple json data source can be used in Grafana to read the events of a JFR file

## Usage

### Build

This project uses [Quarkus](https://quarkus.io), which can produce a JAR to run in a JVM,
or an executable native image.

To build a JAR:
```
mvn clean verify
```
To build a native image instead:
```
mvn -Pnative clean verify
```

### Run the server, targeting a jfr file

If you built a JAR:
```
java -DjfrFile=<path-to-jfr> -jar ./server/target/server-1.0.0-SNAPSHOT-runner.jar
```
If you built a native image:
```
./server/target/server-1.0.0-SNAPSHOT-runner -DjfrFile=<path-to-jfr>
```

### Run Grafana

- Install SimpleJson data source if necessary via
```
grafana-cli --pluginsDir <path-to-your-plugins-directory> plugins install grafana-simple-json-datasource
```
- Add a SimpleJson data source
- Set the URL to: `http://localhost:8080`
- Create a panel that pulls from the data source and plots a timeseries
