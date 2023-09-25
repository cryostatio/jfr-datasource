/*
 * Copyright The Cryostat Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.cryostat.jfr.datasource.server;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import io.cryostat.jfr.datasource.events.RecordingService;
import io.cryostat.jfr.datasource.sys.FileSystemService;

import io.quarkus.vertx.web.ReactiveRoutes;
import io.quarkus.vertx.web.Route;
import io.quarkus.vertx.web.Route.HttpMethod;
import io.smallrye.common.annotation.Blocking;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.RoutingContext;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.openjdk.jmc.common.io.IOToolkit;
import org.openjdk.jmc.common.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Datasource {
    private static final Logger LOGGER = LoggerFactory.getLogger(Datasource.class);
    private static final String UNSET_FILE = "";
    private volatile String loadedFile = UNSET_FILE;

    @ConfigProperty(name = "quarkus.http.body.uploads-directory")
    String jfrDir;

    @ConfigProperty(name = "io.cryostat.jfr-datasource.memory-factor", defaultValue = "10")
    String memoryFactor;

    @ConfigProperty(name = "io.cryostat.jfr-datasource.timeout", defaultValue = "29000")
    String timeoutMs;

    @Inject RecordingService recordingService;

    @Inject FileSystemService fsService;

    @Route(path = "/", methods = HttpMethod.GET)
    void root(RoutingContext context) {
        HttpServerResponse response = context.response();
        response.end();
    }

    @Route(
            path = "/search",
            methods = HttpMethod.POST,
            produces = {ReactiveRoutes.APPLICATION_JSON})
    void search(RoutingContext context) {
        HttpServerResponse response = context.response();
        JsonObject body = context.body().asJsonObject();
        try {
            if (body != null && !body.isEmpty()) {
                LOGGER.info(body.toString());
                response.end(recordingService.search(new Search(body)));
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        response.setStatusCode(400).end("Error: invalid search body");
    }

    @Route(
            path = "/query",
            methods = HttpMethod.POST,
            produces = {ReactiveRoutes.APPLICATION_JSON})
    void query(RoutingContext context) {
        HttpServerResponse response = context.response();
        try {
            JsonObject body = context.body().asJsonObject();
            if (body != null && !body.isEmpty()) {
                LOGGER.info(body.toString());
                Query query = new Query(body);
                response.end(recordingService.query(query));
                return;
            }
        } catch (Exception e) {
        }

        response.setStatusCode(400);
        response.end("Error: invalid query body");
    }

    @Route(
            path = "/annotations",
            methods = HttpMethod.POST,
            produces = {"text/plain"})
    void annotations(RoutingContext context) {
        HttpServerResponse response = context.response();
        response.end(recordingService.annotations());
    }

    @Route(
            path = "/set",
            methods = HttpMethod.POST,
            produces = {"text/plain"})
    @Blocking
    void set(RoutingContext context) {
        HttpServerResponse response = context.response();

        String file = context.body().asString();
        String filePath = jfrDir + File.separator + file;

        setFile(filePath, file, response, new StringBuilder());
    }

    @Route(
            path = "/upload",
            methods = HttpMethod.POST,
            produces = {"text/plain"})
    @Blocking
    void upload(RoutingContext context) throws IOException {
        HttpServerResponse response = context.response();

        final StringBuilder responseBuilder = new StringBuilder();

        boolean overwrite = Boolean.parseBoolean(extractQueryParam(context, "overwrite", "false"));
        uploadFiles(context.fileUploads(), responseBuilder, overwrite, response);
        response.end(responseBuilder.toString());
    }

    @Route(
            path = "/load",
            methods = HttpMethod.POST,
            produces = {"text/plain"})
    @Blocking
    void load(RoutingContext context) throws IOException {
        HttpServerResponse response = context.response();

        final StringBuilder responseBuilder = new StringBuilder();

        boolean overwrite = Boolean.parseBoolean(extractQueryParam(context, "overwrite", "false"));
        String lastFile = uploadFiles(context.fileUploads(), responseBuilder, overwrite, response);
        String filePath = jfrDir + File.separator + lastFile;

        setFile(filePath, lastFile, response, responseBuilder);
    }

    @Route(
            path = "/list",
            methods = HttpMethod.GET,
            produces = {"text/plain"})
    void list(RoutingContext context) {
        HttpServerResponse response = context.response();

        try {
            StringBuilder responseBuilder = new StringBuilder();
            for (String filename : listFiles()) {
                if (filename.equals(loadedFile)) {
                    filename = String.format("**%s**", filename);
                }
                responseBuilder.append(filename);
                responseBuilder.append(System.lineSeparator());
            }
            response.end(responseBuilder.toString());
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
            response.setStatusCode(500).end();
        }
    }

    @Route(
            path = "/current",
            methods = HttpMethod.GET,
            produces = {"text/plain"})
    void current(RoutingContext context) {
        HttpServerResponse response = context.response();

        LOGGER.info("Current: " + loadedFile);
        response.end(loadedFile + System.lineSeparator());
    }

    @Route(
            path = "/delete_all",
            methods = HttpMethod.DELETE,
            produces = {"text/plain"})
    @Blocking
    void deleteAll(RoutingContext context) {
        HttpServerResponse response = context.response();

        final StringBuilder stringBuilder = new StringBuilder();
        try {
            List<String> deletedFiles = deleteAllFiles();
            for (String deletedFile : deletedFiles) {
                stringBuilder.append("Deleted: " + deletedFile);
                stringBuilder.append(System.lineSeparator());
            }
            setLoadedFile(UNSET_FILE);
            response.end(stringBuilder.toString());
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
            response.setStatusCode(500).end();
        }
    }

    @Route(
            path = "/delete",
            methods = HttpMethod.DELETE,
            produces = {"text/plain"})
    @Blocking
    void delete(RoutingContext context) {
        HttpServerResponse response = context.response();

        String fileName = context.body().asString();
        if (fileName == null || fileName.isEmpty()) {
            response.setStatusCode(400);
        } else {
            try {
                deleteFile(fileName);
                if (fileName.equals(loadedFile)) {
                    setLoadedFile(UNSET_FILE);
                }
                response.setStatusCode(204);
            } catch (FileNotFoundException e) {
                LOGGER.error(e.getMessage(), e);
                response.setStatusCode(404);
            } catch (IOException e) {
                LOGGER.error(e.getMessage(), e);
                response.setStatusCode(500);
            }
        }
        response.end();
    }

    private List<String> listFiles() throws IOException {
        final List<String> files = new ArrayList<>();
        Path dir = fsService.pathOf(jfrDir);
        if (fsService.exists(dir) && fsService.isDirectory(dir)) {
            for (Path f : fsService.list(dir)) {
                if (fsService.isRegularFile(f)) {
                    files.add(f.getFileName().toString());
                }
            }
        }
        return files;
    }

    private String uploadFiles(
            List<FileUpload> uploads, StringBuilder responseBuilder, boolean overwrite, HttpServerResponse response) throws IOException {
        String lastFile = "";
        for (FileUpload fileUpload : uploads) {
            Pair<String, Path> pairHelper = uploadHelper(fileUpload, response);
            String uploadedFile = pairHelper.left;
            Path source = pairHelper.right;
            lastFile = uploadedFile;
            Path dest = source.resolveSibling(fileUpload.fileName());

            if (fsService.exists(dest)) {
                if (overwrite) {
                    LOGGER.info(fileUpload.fileName() + " exists and will be overwritten.");
                } else {
                    int attempts = 0;
                    while (fsService.exists(dest) && attempts < 10) {
                        dest =
                                source.resolveSibling(
                                        UUID.randomUUID().toString() + '-' + fileUpload.fileName());
                        attempts++;
                    }
                }
            }

            try {
                if (overwrite) {
                    fsService.move(source, dest, StandardCopyOption.REPLACE_EXISTING);
                } else {
                    fsService.move(source, dest);
                }
                logUploadedFile(dest.getFileName().toString(), responseBuilder);
                lastFile = dest.getFileName().toString();
            } catch (IOException e) {
                logUploadedFile(uploadedFile, responseBuilder);
            }
        }

        return lastFile;
    }

    private Pair<String, Path> uploadHelper(FileUpload fileUpload, HttpServerResponse response) throws IOException {
        Path source = fsService.pathOf(fileUpload.uploadedFileName());
        long timeout = TimeUnit.MILLISECONDS.toNanos(Long.parseLong(timeoutMs));
        String uploadedFile = source.getFileName().toString();
        long start = System.nanoTime();
        long now = start;
        long elapsed = 0;

        LOGGER.info("Received request for %s (%d bytes)", fileUpload.fileName(), fileUpload.size());

        if (IOToolkit.isCompressedFile(source.toFile())) {
            source = decompress(source);
            now = System.nanoTime();
            elapsed = now - start;
            LOGGER.info(
                    "%s was compressed. Decompressed size: %d bytes. Decompression took %dms",
                    fileUpload.fileName(),
                    source.toFile().length(),
                    TimeUnit.NANOSECONDS.toMillis(elapsed));
        }

        Runtime runtime = Runtime.getRuntime();
        System.gc();
        long availableMemory = runtime.maxMemory() - runtime.totalMemory() + runtime.freeMemory();
        long maxHandleableSize = availableMemory / Long.parseLong(memoryFactor);
        if (source.toFile().length() > maxHandleableSize) {
            response.setStatusCode(413);
            response.end();
        }

        now = System.nanoTime();
        elapsed = now - start;
        if (elapsed > timeout) {
            response.setStatusCode(504);
            response.end();
        }
        return new Pair<>(uploadedFile, source);
    }

    private Path decompress(Path source) throws IOException {
        Path tmp = Files.createTempFile(null, null);
        try (var stream = IOToolkit.openUncompressedStream(source.toFile())) {
            Files.copy(stream, tmp, StandardCopyOption.REPLACE_EXISTING);
            return tmp;
        } finally {
            Files.deleteIfExists(source);
        }
    }

    private void logUploadedFile(String file, StringBuilder responseBuilder) {
        responseBuilder.append("Uploaded: " + file);
        responseBuilder.append(System.lineSeparator());
        LOGGER.info("Uploaded: " + file);
    }

    private void setLoadedFile(String filename) {
        this.loadedFile = filename;
    }

    private void setFile(
            String absolutePath,
            String filename,
            HttpServerResponse response,
            StringBuilder responseBuilder) {
        try {
            recordingService.loadEvents(absolutePath);
            responseBuilder.append("Set: " + filename);
            responseBuilder.append(System.lineSeparator());
            setLoadedFile(filename);
            response.end(responseBuilder.toString());
        } catch (IOException e) {
            response.setStatusCode(404);
            response.end();
        }
    }

    private List<String> deleteAllFiles() throws IOException {
        final List<String> deleteFiles = new ArrayList<>();
        Path dir = fsService.pathOf(jfrDir);
        if (fsService.exists(dir) && fsService.isDirectory(dir)) {
            for (Path f : fsService.list(dir)) {
                if (fsService.isRegularFile(f)) {
                    fsService.delete(f);
                    deleteFiles.add(f.getFileName().toString());
                    LOGGER.info("Deleted: " + f.getFileSystem().toString());
                }
            }
            setLoadedFile(UNSET_FILE);
        }
        return deleteFiles;
    }

    private void deleteFile(String filename) throws IOException {
        Path dir = fsService.pathOf(jfrDir);

        if (fsService.exists(dir) && fsService.isDirectory(dir)) {
            if (fsService.deleteIfExists(
                    fsService.pathOf(dir.toAbsolutePath().toString(), filename))) {
                LOGGER.info("Deleted: " + filename);
                if (filename.equals(loadedFile)) {
                    setLoadedFile(UNSET_FILE);
                }
            } else {
                throw new FileNotFoundException(filename + " does not exist");
            }
        } else {
            throw new FileNotFoundException(filename + " does not exist");
        }
    }

    private String extractQueryParam(RoutingContext context, String name, String defaultValue) {
        final MultiMap queries = context.queryParams();
        final String val = queries.get(name);
        return val != null ? val : defaultValue;
    }
}
