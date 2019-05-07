package com.redhat.jfr.server;

import com.redhat.jfr.events.Recording;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.Router;

public class MainVerticle {
  public static void main(String[] args) {
    try {
      Recording jfr = new Recording(args[0]);

      Vertx instance = Vertx.vertx();
      HttpServer server = instance.createHttpServer();
      Router router = Router.router(instance);

      router.route().path("/").handler(routingContext -> {
        HttpServerResponse response = routingContext.response();
        response.end();
      });

      router.route().path("/search").handler(routingContext -> {
        HttpServerResponse response = routingContext.response();
        setHeaders(response);
        response.end(jfr.search());
      });

      router.route().path("/query").handler(routingContext -> {
        HttpServerResponse response = routingContext.response();
        setHeaders(response);
        response.end(jfr.query());
      });

      router.route().path("/annotations").handler(routingContext -> {
        HttpServerResponse response = routingContext.response();
        setHeaders(response);
        response.end(jfr.annotations());
      });

      System.out.println("Server listening on 0.0.0.0:8080");
      server.requestHandler(router).listen(8080);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private static void setHeaders(HttpServerResponse response) {
    response.putHeader("content-type", "application/json");
    response.putHeader("Access-Control-Allow-Origin", "*");
    response.putHeader("Access-Control-Allow-Methods", "POST");
    response.putHeader("Access-Control-Allow-Headers", "accept, content-type");
  }
}
