package com.redhat.jfr.server;

import static org.openjdk.jmc.common.unit.UnitLookup.NUMBER;

import java.io.File;
import java.util.Map;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.Router;


import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemIterable;
import org.openjdk.jmc.common.item.IMemberAccessor;
import org.openjdk.jmc.common.item.IType;
import org.openjdk.jmc.common.item.ItemFilters;
import org.openjdk.jmc.common.IDescribable;
import org.openjdk.jmc.common.item.Attribute;
import org.openjdk.jmc.common.item.IAccessorKey;
import org.openjdk.jmc.common.item.IAttribute;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.QuantityConversionException;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.flightrecorder.JfrAttributes;
import org.openjdk.jmc.flightrecorder.JfrLoaderToolkit;

public class MainVerticle {

  public static final IAttribute<IQuantity> WORK_LEFT = Attribute.attr("workLeft", "MCCustomEventDemo$CustomEvent",
      NUMBER);

  public static void main(String[] args) {
    try {
      IItemCollection events = JfrLoaderToolkit.loadEvents(new File(args[0]));
      IItemCollection customEvents = events.apply(ItemFilters.type("MCCustomEventDemo$CustomEvent"));

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
        response.end("[\"workLeft\"]");
      });

      router.route().path("/query").handler(routingContext -> {
        HttpServerResponse response = routingContext.response();
        setHeaders(response);

        String eventsJson = "[]";
        try {
          eventsJson = filterEvents(0, 0, customEvents);
        } catch (Exception e) {
        }

        response.end(eventsJson);
      });

      router.route().path("/annotations").handler(routingContext -> {
        HttpServerResponse response = routingContext.response();
        setHeaders(response);
        response.end("[]");
      });

      server.requestHandler(router::accept);

      System.out.println("Server listening on 0.0.0.0:8080");
      server.listen(8080);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private static String filterEvents(long from, long to, IItemCollection events) throws QuantityConversionException {

    StringBuilder queryResponse = new StringBuilder();
    queryResponse.append("[");
    queryResponse.append("{");
    queryResponse.append("\"target\" : \"workLeft\"");
    queryResponse.append(",");
    queryResponse.append("\"datapoints\":");
    queryResponse.append("[");

    for (IItemIterable itemIterable : events) {
      IType<IItem> type = itemIterable.getType();
      IMemberAccessor<IQuantity, IItem> workAccessor = WORK_LEFT.getAccessor(type);
      IMemberAccessor<IQuantity, IItem> startTimeAccessor = JfrAttributes.START_TIME.getAccessor(type);

      for (IItem item : itemIterable) {
        queryResponse.append("[");
        queryResponse.append(workAccessor.getMember(item).longValue());
        queryResponse.append(",");
        queryResponse.append(startTimeAccessor.getMember(item).longValueIn(UnitLookup.EPOCH_MS));
        queryResponse.append("]");
        queryResponse.append(",");
      }
    }
    queryResponse.deleteCharAt(queryResponse.length() - 1);

    queryResponse.append("]");
    queryResponse.append("}");
    queryResponse.append("]");

    return queryResponse.toString();
  }

  private static void setHeaders(HttpServerResponse response) {
    response.putHeader("content-type", "application/json");
    response.putHeader("Access-Control-Allow-Origin", "*");
    response.putHeader("Access-Control-Allow-Methods", "POST");
    response.putHeader("Access-Control-Allow-Headers", "accept, content-type");
  }

  private static void printKeys(IType<IItem> type) {
    Map<IAccessorKey<?>, ? extends IDescribable> m = type.getAccessorKeys();
    for(Object key: m.keySet()) {
      System.out.println("KEY " + key);
    }
  }
}
