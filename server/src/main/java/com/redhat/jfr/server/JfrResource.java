package com.redhat.jfr.server;

import java.io.File;
import java.io.IOException;

import javax.enterprise.event.Observes;
import javax.inject.Inject;

import com.redhat.jfr.events.RecordingService;

import io.quarkus.runtime.StartupEvent;
import io.quarkus.vertx.web.Route;
import io.vertx.core.http.HttpServerResponse;
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

        Query query = new Query(context.getBodyAsJson());
        response.end(service.query(query));
    }

    @Route(path = "/annotations")
    void annotations(RoutingContext context) {
        HttpServerResponse response = context.response();
        setHeaders(response);
        response.end(service.annotations());
    }

    @Route(path = "/load")
    void load(RoutingContext context) {
        HttpServerResponse response = context.response();
        setHeaders(response);

        String filename = (jfrDir != null || jfrDir != "") ? jfrDir + File.separator + context.getBodyAsString()
                : context.getBodyAsString();

        File f = new File(filename);
        if (f.exists() && f.isFile()) {
            try {
                service.loadEvents(filename);
            } catch (IOException e) {
                response.setStatusCode(404);
                response.end();
            }
        } else {
            response.setStatusCode(404);
            response.end();
        }

        response.end("Loaded: " + filename);
    }

    @Route(path = "/upload")
    void upload(RoutingContext context) {
        HttpServerResponse response = context.response();
        setHeaders(response);

        StringBuilder builder = new StringBuilder();
        for (FileUpload f : context.fileUploads()) {
            builder.append("Uploaded: " + f.uploadedFileName());
            builder.append(System.lineSeparator());
            LOGGER.info("Uploaded: " + f.uploadedFileName());
        }

        response.end(builder.toString());
    }

    private void setHeaders(HttpServerResponse response) {
        response.putHeader("content-type", "application/json");
        response.putHeader("Access-Control-Allow-Origin", "*");
        response.putHeader("Access-Control-Allow-Methods", "POST");
        response.putHeader("Access-Control-Allow-Headers", "accept, content-type");
    }
}
