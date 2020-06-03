package com.redhat.jfr.server;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;

import com.redhat.jfr.utils.ArgRunnable;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class Query {

  DateTimeFormatter dateFormat = DateTimeFormatter.ISO_INSTANT;

  private JsonObject query;

  public Query(JsonObject query) {
    this.query = query;
  }

  public JsonArray getTargets() {
    return this.query.getJsonArray("targets");
  }

  public void applyTargets(ArgRunnable<Target> runnable) {
    JsonArray targets = this.query.getJsonArray("targets");
    for (int i = 0; i < targets.size(); i++) {
      JsonObject target = targets.getJsonObject(i);
      Target t = new Target(target.getString("target"), target.getString("type"));
      runnable.run(t);
    }
  }

  public long getFrom() {
    TemporalAccessor accessor = dateFormat.parse(this.query.getJsonObject("range").getString("from"));
    Instant instant = Instant.from(accessor);
    return instant.toEpochMilli();
  }

  public long getTo() {
    TemporalAccessor accessor = dateFormat.parse(this.query.getJsonObject("range").getString("to"));
    Instant instant = Instant.from(accessor);
    return instant.toEpochMilli();
  }

}