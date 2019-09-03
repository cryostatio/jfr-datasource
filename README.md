# jfr-datasource

This demonstrates how a simple json data source can be used in Grafana to read the events of a JFR file

## Usage

### Dependencies

This project depends on JMC Core libraries which are acquired from the sonatype repositories. They can also be built from the 'core' maven project at:
```
http://hg.openjdk.java.net/jmc/jmc
```

### Build and run locally

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

#### Run the server

If you built a JAR:
```
java -jar ./server/target/server-1.0.0-SNAPSHOT-runner.jar
```
If you built a native image:
```
./server/target/server-1.0.0-SNAPSHOT-runner
```

### Run Grafana

- Install SimpleJson data source if not already installed via
```
grafana-cli --pluginsDir <path-to-your-plugins-directory> plugins install grafana-simple-json-datasource
```
- Add a SimpleJson data source
- Set the URL to the jfr-datasource (default: `http://localhost:8080`)
- Create a panel that pulls from the data source and plots a timeseries


### Build and run via S2I

This project has support for building a runtime image via S2I

Build the builder and runtime images
```
pushd docker/builder
docker build . -t jfr-datasource-builder
popd
pushd docker/runtime
docker build . -t jfr-datasource-runtime
popd
```

#### Run the S2I build
```
s2i build https://github.com/rh-jmc-team/jfr-datasource jfr-datasource-builder jfr-datasource --runtime-image jfr-datasource-runtime --runtime-artifact /home/quarkus/application:.
```

Run the image
```
docker run --rm -it -p 8080:8080 jfr-datasource
```

### Run on OpenShift

Build the builder image
```
oc new-build https://github.com/rh-jmc-team/jfr-datasource.git --context-dir=docker/builder --name jfr-datasource-builder
```

Deploy the datasource using the builder image
```
oc new-app -i jfr-datasource-builder:latest~https://github.com/rh-jmc-team/jfr-datasource.git --name=jfr-datasource
```

Expose the datasource
```
oc expose svc/jfr-datasource
```

## API

### JFR Endpoints

#### GET /

Responds with 200 OK. Used to verify server is available

CURL Example
```
$ curl "localhost:8080/"
```

#### POST /upload

Expects a JFR file upload. Used to upload a JFR file to the server. Responds with the uploaded filename

CURL Example
```
$ curl -F "file=@/home/user/some-file.jfr" "localhost:8080/upload"
```

#### POST /set

Sets a JFR file for querying requests. Expects file name specified via POST body

CURL Example
```
$ curl -X POST --data "some-file" "localhost:8080/set"
```

#### POST /load

Expects a JFR file upload. Performs `Upload` and `Set` in sequence. Responds with the uploaded and selected filename.

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

### Query Endpoints

These endpoints match those used by the Grafana Simple JSON datasource

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

This is a list of events which have no attributes that work out-of-the-box or no relevant attributes when visualized in Grafana

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
