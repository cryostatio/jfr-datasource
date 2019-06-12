# jfr-datasource

This demonstrates how a simple json data source can be used in Grafana to read the events of a JFR file

## Usage

### Dependencies

This project depends on JMC Core libraries which are acquired from the sonatype repositories. They can also be built from the 'core' maven project at:
```
http://hg.openjdk.java.net/jmc/jmc
```

### Build

This project uses [Quarkus](https://quarkus.io), which can produce a JAR to run in a JVM, or an executable native image.

To build a JAR:
```
mvn clean verify
```
To build a native image instead:
```
mvn -Pnative clean verify
```
Native image builds may use more than 4G of RAM to complete.

### Run the server, targeting a jfr file

If you built a JAR:
```
java -jar ./server/target/server-1.0.0-SNAPSHOT-runner.jar
```
If you built a native image:
```
./server/target/server-1.0.0-SNAPSHOT-runner
```

By default, the server will load files from the directory `file-upload`. To change this, set the System Property `jfrDir`. For example:
```
java -DjfrDir="/some/path" ./server/target/server-1.0.0-SNAPSHOT-runner.jar
./server/target/server-1.0.0-SNAPSHOT-runner -DjfrDir="/some/path"
```

### Run Grafana

- Install SimpleJson data source if not already installed via
```
grafana-cli --pluginsDir <path-to-your-plugins-directory> plugins install grafana-simple-json-datasource
```
- Add a SimpleJson data source
- Set the URL to the jfr-datasource (default: `http://localhost:8080`)
- Create a panel that pulls from the data source and plots a timeseries


### Run on OpenShift

Build the builder image
```
oc new-build https://github.com/jiekang/jfr-datasource.git --context-dir=docker/builder --name jfr-datasource-builder
```

Deploy the datasource using the builder image
```
oc new-app -i jfr-datasource-builder:latest~https://github.com/jiekang/jfr-datasource.git --name=jfr-datasource
```

Expose the datasource
```
oc expose svc/jfr-datasource
```

## API

### JFR Endpoints

#### GET /

Responds with 200 OK. Used to verify server is available.

CURL Example
```
$ curl "localhost:8080/"
```

#### POST /upload

Expects a file upload. Used to upload a JFR file to the server. Responds with the uploaded filename

CURL Example
```
$ curl -F "file=@/home/user/some-file.jfr" "localhost:8080/upload"
```

#### POST /set

Sets a JFR file for querying requests. Expects files to match with the uploaded filename (generally a hash)

CURL Example
```
$ curl -X POST --data "some-hash" "localhost:8080/set"
```

#### POST /load

Expects a file upload. Uploads and Sets a JFR file for querying requests. Responds with the uploaded and selected filename.

CURL Example
```
$ curl -F "file=@/home/user/some-file.jfr" "localhost:8080/load"
```

### Query Endpoints

These endpoints match those required used by the Grafana Simple JSON datasource

#### GET /search

Responds with a JSON array containing the selectable query elements

CURL Example
```
$ curl "localhost:8080/search"
```


#### POST /query

Responds with a JSON array containing elements for a query. The query body format matches that of the Grafana Simple JSON datasource

CURL Example
```
$ curl -X POST --data "Query Body" "localhost:8080/query"
```