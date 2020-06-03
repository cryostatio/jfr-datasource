package com.redhat.jfr.server;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;

import com.redhat.jfr.events.RecordingService;

import io.quarkus.vertx.web.Route;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.RoutingContext;

public class JfrResource {
  private static final Logger LOGGER = LoggerFactory.getLogger(RecordingService.class);

  private static final String jfrDir = "file-uploads";

  @Inject
  RecordingService service;

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
  void set(RoutingContext context) {
    HttpServerResponse response = context.response();
    setHeaders(response);

    String file = context.getBodyAsString();
    String filePath = jfrDir + File.separator + file;

    setFile(filePath, file, response, new StringBuilder());
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
          dest = source.resolveSibling(UUID.randomUUID().toString() + '-' + fileUpload.fileName());
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

  private void setFile(String absolutePath, String filename, HttpServerResponse response,
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

  private void setHeaders(HttpServerResponse response) {
    response.putHeader("content-type", "application/json");
    response.putHeader("Access-Control-Allow-Origin", "*");
    response.putHeader("Access-Control-Allow-Methods", "POST");
    response.putHeader("Access-Control-Allow-Headers", "accept, content-type");
  }
}
