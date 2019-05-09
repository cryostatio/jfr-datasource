# jfr-datasource

This demonstrates how a simple json data source can be used in Grafana to read the events of a JFR file

## Usage

### Build

```
mvn clean verify
```

### Run the server, targeting a jfr file

```
java -jar ./server/target/server-1.0.0-SNAPSHOT-fat.jar <path-to-jfr>
```

### Run Grafana

- Add a SimpleJson data source
- Set the URL to: `http://localhost:8080`
- Create a panel that pulls from the data source and plots a timeseries
