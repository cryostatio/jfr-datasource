# jfr-datasource

[![Quay Repository](https://quay.io/repository/cryostat/jfr-datasource/status "Quay Repository")](https://quay.io/repository/cryostat/jfr-datasource)

![Build Status](https://github.com/cryostatio/jfr-datasource/actions/workflows/ci.yaml/badge.svg)

This demonstrates how a simple json data source can be used in Grafana to read the events of a JFR file.

## Usage

### Dependencies

For native image support, graalvm 20.3.0 (Java 11 version) is needed with path to it's directory set via environment variable `GRAALVM_HOME`. This can be downloaded from:
```
https://github.com/graalvm/graalvm-ce-builds/releases
```

After downloading, run
```
/path/to/graal-install/bin/gu install native-image
```

`libz.a` is also required to complete native image builds. On Fedora, `dnf install zlib-devel`.

For containers, podman is needed. Installation instructions are here:
```
https://podman.io/getting-started/installation.html
```

### Build and run locally

This project uses [Quarkus](https://quarkus.io), which can produce a JAR to run in a JVM (JDK 11+), or an executable native image.

To build a JAR:
```
mvn clean verify
```
To build a native image instead:
```
mvn -Pnative clean verify
```
Native image builds may use more than 4G of RAM to complete.

To build a native image within a container, for a consistent environment:
```
mvn -Pnative -Dquarkus.native.container-build=true -Dquarkus.native.container-runtime=podman \
-Dquarkus.native.builder-image=quay.io/quarkus/ubi-quarkus-mandrel:20.3.1.2-Final-java11 \
clean verify
```

#### Run the server

If you built a JAR:
```
java -jar target/quarkus-app/quarkus-run.jar
```
If you built a native image:
```
./target/jfr-datasource-2.0.0-SNAPSHOT-runner
```

### Run Grafana

- Install SimpleJson data source if not already installed via
```
grafana-cli --pluginsDir <path-to-your-plugins-directory> plugins install grafana-simple-json-datasource
```
- Add a SimpleJson data source
- Set the URL to the jfr-datasource (default: `http://localhost:8080`)
- Create a panel that pulls from the data source and plots a timeseries

### Building a container image

This project comes with a Dockerfile to produce a container image with the native image result.

```
podman build -f src/main/docker/Dockerfile.native -t quay.io/cryostat/jfr-datasource .
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

Expects a JFR file upload. Used to upload a JFR file to the server. Responds with the uploaded filename.

The webserver sets a default maximum file upload size of 10GB
(`application.properties`: `quarkus.http.limits.max-body-size=10G`).
This can be overridden on a deployed instance by setting the environment variable
`QUARKUS_HTTP_LIMITS_MAX_BODY_SIZE` and restarting the instance.

CURL Example
```
$ curl -F "file=@/home/user/some-file.jfr" "localhost:8080/upload"
```

It is also possible to bypass this webserver HTTP body size limit by copying
the large JFR file directly into the webserver's filesystem storage location.
This location is defined as
(`application.properties`: `quarkus.http.body.uploads-directory=${java.io.tmpdir}${file.separator}jfr-file-uploads`),
or `/tmp/jfr-file-uploads`. The following example assumes that the logged in
user has sufficient permissions and that the `jfr-datasource` container has
an OpenShift Service and Route exposing it to traffic from outside the cluster.

`oc` and CURL Example
```
$ oc cp my-large-file.jfr cryostat-sample-79cc897c8-smcrg:/tmp/jfr-file-uploads/my-large-file.jfr -c cryostat-sample-jfr-datasource
$ curl -X POST --data "my-large-file.jfr" "https://cryostat-sample-jfr-datasource-myproject.apps-crc.testing/set"
```

#### POST /set

Sets a JFR file for querying requests. Expects file name specified via POST body.

CURL Example
```
$ curl -X POST --data "some-file" "localhost:8080/set"
```

#### POST /load

Expects a JFR file upload. Performs `Upload` and `Set` in sequence. Responds with the uploaded and selected filename.

The webserver sets a default maximum file upload size. If the file to be
uploaded exceeds this size then either the limit can be raised or the `/load`
operation can be decomposed into two steps and the size limit worked around.
See the documentation for `POST /upload` for further detail.

CURL Example
```
$ curl -F "file=@/home/user/some-file.jfr" "localhost:8080/load"
```

#### GET /list

Lists files available for `Set`

CURL Example
```
$ curl "localhost:8080/list"
```

#### DELETE /delete

Deletes an individual JFR file. Expects file name specified via DELETE body.

CURL Example
```
$ curl -X DELETE --data "some-file" "localhost:8080/delete"
```

#### DELETE /delete_all

Delete all JFR files.

CURL Example
```
$ curl -X DELETE "localhost:8080/delete_all"
```

### Query Endpoints

These endpoints match those used by the Grafana Simple JSON datasource.

#### GET /search

Responds with a JSON array containing the selectable query elements.

CURL Example
```
$ curl "localhost:8080/search"
```

#### POST /query

Responds with a JSON array containing elements for a query. The query body format matches that of the Grafana Simple JSON datasource.

CURL Example
```
$ curl -X POST --data "Query Body" "localhost:8080/query"
```

## Supported JFR Events

This is a list of event attributes which work 'out-of-the-box' with this datasource. These are generally speaking any numerical timeseries-like event.

```
jdk.ActiveRecording.endTime
jdk.ActiveRecording.recordingDuration
jdk.ActiveRecording.recordingStart

jdk.BiasedLockClassRevocation.duration
jdk.BiasedLockRevocation.duration
jdk.BiasedLockSelfRevocation.duration

jdk.ClassLoaderStatistics.anonymousBlockSize
jdk.ClassLoaderStatistics.anonymousChunkSize
jdk.ClassLoaderStatistics.anonymousClassCount
jdk.ClassLoaderStatistics.blockSize
jdk.ClassLoaderStatistics.chunkSize
jdk.ClassLoaderStatistics.classCount

jdk.ClassLoadingStatistics.loadedClassCount

jdk.CodeCacheConfiguration.expansionSize
jdk.CodeCacheConfiguration.initialSize
jdk.CodeCacheConfiguration.minBlockLength
jdk.CodeCacheConfiguration.nonNMethodSize
jdk.CodeCacheConfiguration.nonProfiledSize
jdk.CodeCacheConfiguration.profiledSize
jdk.CodeCacheConfiguration.reservedSize

jdk.CodeSweeperStatistics.methodReclaimedCount
jdk.CodeSweeperStatistics.peakFractionTime
jdk.CodeSweeperStatistics.peakSweepTime
jdk.CodeSweeperStatistics.sweepCount
jdk.CodeSweeperStatistics.totalSweepTime

jdk.CompilerConfiguration.threadCount

jdk.CompilerStatistics.compileCount
jdk.CompilerStatistics.bailoutCount
jdk.CompilerStatistics.invalidatedCount
jdk.CompilerStatistics.osrCompileCount
jdk.CompilerStatistics.standardCompileCount
jdk.CompilerStatistics.osrBytesCompiled
jdk.CompilerStatistics.standardBytesCompiled
jdk.CompilerStatistics.nmetodsSize
jdk.CompilerStatistics.nmetodCodeSize
jdk.CompilerStatistics.peakTimeSpent
jdk.CompilerStatistics.totalTimeSpent

jdk.CPUInformation.sockets
jdk.CPUInformation.cores
jdk.CPUInformation.hwThreads

jdk.CPULoad.jvmSystem
jdk.CPULoad.jvmUser
jdk.CPULoad.machineTotal

jdk.CPUTimestampCounter.osFrequency
jdk.CPUTimestampCounter.fastTimeFrequency

jdk.DataLoss.amount
jdk.DataLoss.total

jdk.ExceptionStatistics.throwables

jdk.GCConfiguration
jdk.GCHeapConfiguration
jdk.GCSurvivorConfiguration
jdk.GCTLABConfiguration

jdk.G1EvacuationOldStatistics
jdk.G1EvacuationYoungStatistics

jdk.JavaThreadStatistics.accumulatedCount
jdk.JavaThreadStatistics.activeCount
jdk.JavaThreadStatistics.daemonCount
jdk.JavaThreadStatistics.peakCount

jdk.YoungGenerationConfiguration


jdk.MetaspaceGCThreshold

jdk.JVMInformation

jdk.PhysicalMemory

jdk.ThreadContextSwitchRate
```

### Unsupported JFR Events

This is a list of events which have no attributes that work out-of-the-box or no relevant attributes when visualized in Grafana.

```
jdk.ActiveSetting
jdk.AllocationRequiringGC
jdk.BooleanFlag
jdk.BooleanFlagChanged
jdk.ClassDefine
jdk.ClassLoad
jdk.ClassUnload
jdk.CodeCacheFull
jdk.CodeCacheStatistics
jdk.CodeSweeperConfiguration
** jdk.Compilation
jdk.CompilationFailure
** jdk.CompilerInlining
jdk.CompilerPhase
jdk.ConcurrentModeFailure
jdk.DoubleFlag
jdk.DoubleFlagChanged
```
