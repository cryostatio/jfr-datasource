package io.cryostat.jfr.datasource.server;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;

import io.cryostat.jfr.datasource.events.RecordingService;
import io.cryostat.jfr.datasource.sys.FileSystemService;

import io.quarkus.vertx.web.Route;
import io.quarkus.vertx.web.Route.HttpMethod;
import io.smallrye.common.annotation.Blocking;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.RoutingContext;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JfrResource {
    private static final Logger LOGGER = LoggerFactory.getLogger(JfrResource.class);
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

    @Route(path = "/search", methods = HttpMethod.GET)
    void search(RoutingContext context) {
        HttpServerResponse response = context.response();
        setHeaders(response, "application/json", "GET");
        response.end(recordingService.search());
    }

    @Route(path = "/query", methods = HttpMethod.POST)
    void query(RoutingContext context) {
        HttpServerResponse response = context.response();
        setHeaders(response, "application/json", "POST");
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

    @Route(path = "/annotations", methods = HttpMethod.GET)
    void annotations(RoutingContext context) {
        HttpServerResponse response = context.response();
        setHeaders(response, "text/plain", "GET");
        response.end(recordingService.annotations());
    }

    @Route(path = "/set", methods = HttpMethod.POST)
    @Blocking
    void set(RoutingContext context) {
        HttpServerResponse response = context.response();
        setHeaders(response, "text/plain", "POST");

        String file = context.getBodyAsString();
        String filePath = jfrDir + File.separator + file;

        setFile(filePath, file, response, new StringBuilder());
    }

    @Route(path = "/upload", methods = HttpMethod.POST)
    @Blocking
    void upload(RoutingContext context) {
        HttpServerResponse response = context.response();
        setHeaders(response, "text/plain", "POST");

        final StringBuilder responseBuilder = new StringBuilder();

        uploadFiles(context.fileUploads(), responseBuilder);
        response.end(responseBuilder.toString());
    }

    @Route(path = "/load", methods = HttpMethod.POST)
    @Blocking
    void load(RoutingContext context) {
        HttpServerResponse response = context.response();
        setHeaders(response, "text/plain", "POST");

        final StringBuilder responseBuilder = new StringBuilder();

        String lastFile = uploadFiles(context.fileUploads(), responseBuilder);
        String filePath = jfrDir + File.separator + lastFile;

        setFile(filePath, lastFile, response, responseBuilder);
    }

    @Route(path = "/list", methods = HttpMethod.GET)
    void list(RoutingContext context) {
        HttpServerResponse response = context.response();
        setHeaders(response, "text/plain", "GET");

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

    @Route(path = "/current", methods = HttpMethod.GET)
    void current(RoutingContext context) {
        HttpServerResponse response = context.response();
        setHeaders(response, "text/plain", "GET");

        LOGGER.info("Current: " + loadedFile);
        response.end(loadedFile + System.lineSeparator());
    }

    @Route(path = "/delete_all", methods = HttpMethod.DELETE)
    @Blocking
    void deleteAll(RoutingContext context) {
        HttpServerResponse response = context.response();
        setHeaders(response, "text/plain", "DELETE");

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

    @Route(path = "/delete", methods = HttpMethod.DELETE)
    @Blocking
    void delete(RoutingContext context) {
        HttpServerResponse response = context.response();
        setHeaders(response, "text/plain", "DELETE");

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

    private String uploadFiles(Set<FileUpload> uploads, StringBuilder responseBuilder) {
        String lastFile = "";
        for (FileUpload fileUpload : uploads) {
            Path source = fsService.pathOf(fileUpload.uploadedFileName());
            String uploadedFile = source.getFileName().toString();
            lastFile = uploadedFile;

            Path dest = source.resolveSibling(fileUpload.fileName());

            if (fsService.exists(dest)) {
                int attempts = 0;
                while (fsService.exists(dest) && attempts < 10) {
                    dest =
                            source.resolveSibling(
                                    UUID.randomUUID().toString() + '-' + fileUpload.fileName());
                    attempts++;
                }
            }
            try {
                fsService.move(source, dest);
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

    private void setHeaders(HttpServerResponse response, String contentType, String allowedMethod) {
        response.putHeader("content-type", contentType);
        response.putHeader("Access-Control-Allow-Methods", allowedMethod);
        response.putHeader("Access-Control-Allow-Origin", "*");
        response.putHeader("Access-Control-Allow-Headers", "accept, content-type");
    }
}
