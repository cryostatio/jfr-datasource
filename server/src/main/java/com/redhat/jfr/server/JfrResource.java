package com.redhat.jfr.server;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import javax.enterprise.event.Observes;
import javax.inject.Inject;

import com.redhat.jfr.events.RecordingService;

import io.quarkus.runtime.StartupEvent;
import io.quarkus.vertx.web.Route;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.RoutingContext;

public class JfrResource {
    private static final Logger LOGGER = LoggerFactory.getLogger(RecordingService.class);
    private static final String JFR_PROPERTY = "jfrDir";
    private String jfrDir = "file-uploads";

    @Inject
    RecordingService service;

    void onStart(@Observes StartupEvent event) {
        String dir = System.getProperty(JFR_PROPERTY);
        if (dir != null && dir != "") {
            jfrDir = dir;
        }
        LOGGER.info("Set JFR Directory to: " + jfrDir);
    }

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
                Query query = new Query(context.getBodyAsJson());
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
    void set(RoutingContext context) {
        HttpServerResponse response = context.response();
        setHeaders(response);

        String filename = (jfrDir != null || jfrDir != "") ? jfrDir + File.separator + context.getBodyAsString()
                : context.getBodyAsString();

        try {
            service.loadEvents(filename);
            response.end("Loaded: " + filename);
        } catch (IOException e) {
            response.setStatusCode(404);
            response.end();
        }
    }

    @Route(path = "/upload")
    void upload(RoutingContext context) {
        HttpServerResponse response = context.response();
        setHeaders(response);

        StringBuilder responseBuilder = new StringBuilder();
        uploadFiles(context.fileUploads(), responseBuilder);

        response.end(responseBuilder.toString());
    }

    @Route(path = "/load")
    void load(RoutingContext context) {
        HttpServerResponse response = context.response();
        setHeaders(response);

        StringBuilder responseBuilder = new StringBuilder();
        String lastFile = uploadFiles(context.fileUploads(), responseBuilder);

        try {
            service.loadEvents(lastFile);
            responseBuilder.append("Loaded: " + lastFile);
            response.end(responseBuilder.toString());
        } catch (IOException e) {
            response.setStatusCode(404);
            response.end();
        }
    }

    @Route(path = "/list")
    void list(RoutingContext context) {
        HttpServerResponse response = context.response();
        setHeaders(response);

        File dir = new File(jfrDir);
        StringBuilder responseBuilder = new StringBuilder();
        for (File f : dir.listFiles()) {
            if (f.isFile()) {
                responseBuilder.append(f.getName());
                responseBuilder.append(System.lineSeparator());
            }
        }

        response.end(responseBuilder.toString());
    }

    private String uploadFiles(Set<FileUpload> uploads, StringBuilder responseBuilder) {
        String lastFile = "";
        for (FileUpload f : uploads) {
            responseBuilder.append("Uploaded: " + f.uploadedFileName());
            responseBuilder.append(System.lineSeparator());
            LOGGER.info("Uploaded: " + f.uploadedFileName());
            lastFile = f.uploadedFileName();
        }

        return lastFile;
    }

    private void setHeaders(HttpServerResponse response) {
        response.putHeader("content-type", "application/json");
        response.putHeader("Access-Control-Allow-Origin", "*");
        response.putHeader("Access-Control-Allow-Methods", "POST");
        response.putHeader("Access-Control-Allow-Headers", "accept, content-type");
    }
}
