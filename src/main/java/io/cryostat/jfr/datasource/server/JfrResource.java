package io.cryostat.jfr.datasource.server;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;

import io.cryostat.jfr.datasource.events.RecordingService;

import io.quarkus.vertx.web.Route;
import io.smallrye.common.annotation.Blocking;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.RoutingContext;
import org.eclipse.microprofile.config.inject.ConfigProperty;

public class JfrResource {
    private static final Logger LOGGER = LoggerFactory.getLogger(RecordingService.class);

    @ConfigProperty(name = "quarkus.http.body.uploads-directory")
    String jfrDir;

    @Inject RecordingService service;

    @Route(path = "/")
    void root(RoutingContext context) {
        HttpServerResponse response = context.response();
        response.end();
    }

    @Route(path = "/search")
    void search(RoutingContext context) {
        HttpServerResponse response = context.response();
        setHeaders(response);
        response.end(service.search());
    }

    @Route(path = "/query")
    void query(RoutingContext context) {
        HttpServerResponse response = context.response();
        setHeaders(response);
        try {
            JsonObject body = context.getBodyAsJson();
            if (body != null && !body.isEmpty()) {
                LOGGER.info(body);
                Query query = new Query(body);
                response.end(service.query(query));
                return;
            }
        } catch (Exception e) {
        }

        response.setStatusCode(400);
        response.end("Error: invalid query body");
    }

    @Route(path = "/annotations")
    void annotations(RoutingContext context) {
        HttpServerResponse response = context.response();
        setHeaders(response);
        response.end(service.annotations());
    }

    @Route(path = "/set")
    @Blocking
    void set(RoutingContext context) {
        HttpServerResponse response = context.response();
        setHeaders(response);

        String file = context.getBodyAsString();
        String filePath = jfrDir + File.separator + file;

        setFile(filePath, file, response, new StringBuilder());
    }

    @Route(path = "/upload")
    @Blocking
    void upload(RoutingContext context) {
        HttpServerResponse response = context.response();
        setHeaders(response);

        final StringBuilder responseBuilder = new StringBuilder();

        uploadFiles(context.fileUploads(), responseBuilder);
        response.end(responseBuilder.toString());
    }

    @Route(path = "/load")
    @Blocking
    void load(RoutingContext context) {
        HttpServerResponse response = context.response();
        setHeaders(response);

        final StringBuilder responseBuilder = new StringBuilder();

        String lastFile = uploadFiles(context.fileUploads(), responseBuilder);
        String filePath = jfrDir + File.separator + lastFile;

        setFile(filePath, lastFile, response, responseBuilder);
    }

    @Route(path = "/list")
    void list(RoutingContext context) {
        HttpServerResponse response = context.response();
        setHeaders(response);

        File dir = new File(jfrDir);
        StringBuilder responseBuilder = new StringBuilder();
        if (dir.exists() && dir.isDirectory()) {
            for (File f : dir.listFiles()) {
                if (f.isFile()) {
                    responseBuilder.append(f.getName());
                    responseBuilder.append(System.lineSeparator());
                }
            }
        }

        response.end(responseBuilder.toString());
    }

    @Route(path = "/delete_all", methods = HttpMethod.DELETE)
    @Blocking
    void deleteAll(RoutingContext context) {
        HttpServerResponse response = context.response();
        setHeaders(response);

        final StringBuilder stringBuilder = new StringBuilder();
        List<String> deletedFiles = deleteAllFiles();

        for (String deletedFile : deletedFiles) {
            stringBuilder.append(deletedFile);
            stringBuilder.append(System.lineSeparator());
        }

        response.end(stringBuilder.toString());
    }

    @Route(path = "/delete", methods = HttpMethod.DELETE)
    @Blocking
    void delete(RoutingContext context) {
        HttpServerResponse response = context.response();
        setHeaders(response);

        String fileName = context.getBodyAsString();
        deleteFile(fileName);

        response.end(stringBuilder.toString());
    }

    private String uploadFiles(Set<FileUpload> uploads, StringBuilder responseBuilder) {
        String lastFile = "";
        for (FileUpload fileUpload : uploads) {
            Path source = Paths.get(fileUpload.uploadedFileName());
            String uploadedFile = source.getFileName().toString();
            lastFile = uploadedFile;

            Path dest = source.resolveSibling(fileUpload.fileName());

            if (Files.exists(dest)) {
                int attempts = 0;
                while (Files.exists(dest) && attempts < 10) {
                    dest =
                            source.resolveSibling(
                                    UUID.randomUUID().toString() + '-' + fileUpload.fileName());
                    attempts++;
                }
            }
            try {
                Files.move(source, dest);
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

    private void setFile(
            String absolutePath,
            String filename,
            HttpServerResponse response,
            StringBuilder responseBuilder) {
        try {
            service.loadEvents(absolutePath);
            responseBuilder.append("Set: " + filename);
            responseBuilder.append(System.lineSeparator());
            response.end(responseBuilder.toString());
        } catch (IOException e) {
            response.setStatusCode(404);
            response.end();
        }
    }

    private List<String> deleteAllFiles() {
        File dir = new File(jfrDir);
        final List<String> deleteFiles = new ArrayList<>();
        if (dir.exists() && dir.isDirectory()) {
            for (File f : dir.listFiles()) {
                if (f.isFile()) {
                    try {
                        Files.delete(f.toPath());
                        deleteFiles.add(f.getName());
                        LOGGER.info("Deleted: " + f.getName());
                    } catch (IOException e) {
                    }
                }
            }
        }
        return deleteFiles;
    }

    private void deleteFile(String filename) {
        File dir = new File(jfrDir);

        if (dir.exists() && dir.isDirectory()) {
            try {
                Files.deleteIfExists(Paths.get(dir.getAbsolutePath(), filename));
                LOGGER.info("Deleted: " + filename);
            } catch (IOException e) {
            }
        }
    }

    private void setHeaders(HttpServerResponse response) {
        response.putHeader("content-type", "application/json");
        response.putHeader("Access-Control-Allow-Origin", "*");
        response.putHeader("Access-Control-Allow-Methods", "POST");
        response.putHeader("Access-Control-Allow-Headers", "accept, content-type");
    }
}
