/*
 * Copyright The Cryostat Authors
 *
 * The Universal Permissive License (UPL), Version 1.0
 *
 * Subject to the condition set forth below, permission is hereby granted to any
 * person obtaining a copy of this software, associated documentation and/or data
 * (collectively the "Software"), free of charge and under any and all copyright
 * rights in the Software, and any and all patent rights owned or freely
 * licensable by each licensor hereunder covering either (i) the unmodified
 * Software as contributed to or provided by such licensor, or (ii) the Larger
 * Works (as defined below), to deal in both
 *
 * (a) the Software, and
 * (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
 * one is included with the Software (each a "Larger Work" to which the Software
 * is contributed by such licensors),
 *
 * without restriction, including without limitation the rights to copy, create
 * derivative works of, display, perform, and distribute the Software and make,
 * use, sell, offer for sale, import, export, have made, and have sold the
 * Software and the Larger Work(s), and to sublicense the foregoing rights on
 * either these or other terms.
 *
 * This license is subject to the following condition:
 * The above copyright notice and either this complete permission notice or at
 * a minimum a reference to the UPL must be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package io.cryostat.jfr.datasource.server;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;

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
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Datasource {
    private static final Logger LOGGER = LoggerFactory.getLogger(Datasource.class);
    private static final String UNSET_FILE = "";
    private volatile String loadedFile = UNSET_FILE;

    @ConfigProperty(name = "quarkus.http.body.uploads-directory")
    String jfrDir;

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
        JsonObject body = context.getBodyAsJson();
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
            JsonObject body = context.getBodyAsJson();
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

        String file = context.getBodyAsString();
        String filePath = jfrDir + File.separator + file;

        setFile(filePath, file, response, new StringBuilder());
    }

    @Route(
            path = "/upload",
            methods = HttpMethod.POST,
            produces = {"text/plain"})
    @Blocking
    void upload(RoutingContext context) {
        HttpServerResponse response = context.response();

        final StringBuilder responseBuilder = new StringBuilder();

        boolean overwrite = Boolean.valueOf(extractQueryParam(context, "overwrite", "false"));
        uploadFiles(context.fileUploads(), responseBuilder, overwrite);
        response.end(responseBuilder.toString());
    }

    @Route(
            path = "/load",
            methods = HttpMethod.POST,
            produces = {"text/plain"})
    @Blocking
    void load(RoutingContext context) {
        HttpServerResponse response = context.response();

        final StringBuilder responseBuilder = new StringBuilder();

        boolean overwrite = Boolean.valueOf(extractQueryParam(context, "overwrite", "false"));
        String lastFile = uploadFiles(context.fileUploads(), responseBuilder, overwrite);
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

        String fileName = context.getBodyAsString();
        if (fileName == null || fileName.isEmpty()) {
            response.setStatusCode(400);
        } else {
            try {
                deleteFile(fileName);
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
            Set<FileUpload> uploads, StringBuilder responseBuilder, boolean overwrite) {
        String lastFile = "";
        for (FileUpload fileUpload : uploads) {
            Path source = fsService.pathOf(fileUpload.uploadedFileName());
            String uploadedFile = source.getFileName().toString();
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
