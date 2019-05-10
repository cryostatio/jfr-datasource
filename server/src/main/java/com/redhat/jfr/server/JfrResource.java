package com.redhat.jfr.server;

import javax.inject.Inject;

import com.redhat.jfr.events.RecordingService;

import io.quarkus.vertx.web.Route;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;

public class JfrResource {

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

        Query query = new Query(context.getBodyAsJson());
        response.end(service.query(query));
    }

    @Route(path = "/annotations")
    void annotations(RoutingContext context) {
        HttpServerResponse response = context.response();
        setHeaders(response);
        response.end(service.annotations());
    }

    private void setHeaders(HttpServerResponse response) {
        response.putHeader("content-type", "application/json");
        response.putHeader("Access-Control-Allow-Origin", "*");
        response.putHeader("Access-Control-Allow-Methods", "POST");
        response.putHeader("Access-Control-Allow-Headers", "accept, content-type");
    }
}
