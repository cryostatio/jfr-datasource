package com.redhat.jfr.server;

import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Date;
import java.util.Map;

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

  public void applyTargets(ArgRunnable<String> runnable) {
    JsonArray targets = this.query.getJsonArray("targets");
    for (int i = 0; i < targets.size(); i++) {
      JsonObject target = targets.getJsonObject(i);
      runnable.run(target.getString("target"));
    }
  }

  public TemporalAccessor getFrom() {
    TemporalAccessor accessor = dateFormat.parse(this.query.getJsonObject("range").getString("from"));
    return accessor;
  }

  public TemporalAccessor getTo() {
    TemporalAccessor accessor = dateFormat.parse(this.query.getJsonObject("range").getString("to"));
    return accessor;
  }

}