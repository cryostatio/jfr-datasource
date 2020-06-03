package com.redhat.jfr.events;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;

import com.redhat.jfr.json.JsonUtils;
import com.redhat.jfr.server.Query;

import org.openjdk.jmc.common.item.IAttribute;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemIterable;
import org.openjdk.jmc.common.item.IMemberAccessor;
import org.openjdk.jmc.common.item.IType;
import org.openjdk.jmc.common.item.ItemFilters;
import org.openjdk.jmc.common.item.ItemToolkit;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.IRange;
import org.openjdk.jmc.common.unit.QuantityConversionException;
import org.openjdk.jmc.common.unit.QuantityRange;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.flightrecorder.CouldNotLoadRecordingException;
import org.openjdk.jmc.flightrecorder.JfrAttributes;
import org.openjdk.jmc.flightrecorder.JfrLoaderToolkit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

@ApplicationScoped
public class RecordingService {

  private static final Logger LOGGER = LoggerFactory.getLogger(RecordingService.class);

  private IItemCollection events;

  public boolean eventsLoaded() {
    return events != null;
  }

  public String search() {
    if (events == null) {
      return JsonUtils.EMPTY_ARRAY;
    }
    JsonArray json = new JsonArray();
    Iterator<IItemIterable> i = events.iterator();
    while (i.hasNext()) {
      try {
        IItemIterable item = i.next();
        if (item.hasItems()) {
          IType<IItem> type = item.getType();
          List<IAttribute<?>> attributes = type.getAttributes();
          for (IAttribute<?> attribute : attributes) {
            if (attribute.getIdentifier().contains("eventType") || attribute.getIdentifier().contains("startTime")
                || attribute.getIdentifier().contains("endTime")) {
              continue;
            }
            String name = type.getIdentifier() + "." + attribute.getIdentifier();
            json.add(name);
          }
        }
      } catch (Exception e) {
        return JsonUtils.EMPTY_ARRAY;
      }
    }
    return json.toString();
  }

  public String query(Query query) {
    try {
      if (events == null) {
        return JsonUtils.EMPTY_ARRAY;
      }
      JsonArray json = new JsonArray();
      query.applyTargets((t) -> {
        JsonObject targetObject = new JsonObject();
        json.add(targetObject);
        String target = t.getTarget();
        String type = t.getType();
        if ("timeserie".equals(type)) {
          this.getTimeseries(query, target, targetObject);
        } else if ("table".equals(type)) {
          this.getTable(query, target, targetObject);
        }
      }

      );
      return json.toString();
    } catch (Exception e) {
      e.printStackTrace();
      return JsonUtils.EMPTY_ARRAY;
    }

  }

  public void getTimeseries(Query query, String target, JsonObject targetObject) {
    if (target.contains(".")) {
      targetObject.put("target", target);
      JsonArray datapointsArray = new JsonArray();
      targetObject.put("datapoints", datapointsArray);

      String eventName = target.substring(0, target.lastIndexOf("."));
      String eventField = target.substring(target.lastIndexOf(".") + 1);
      IQuantity start = UnitLookup.EPOCH_MS.quantity(query.getFrom());
      IQuantity end = UnitLookup.EPOCH_MS.quantity(query.getTo());
      IRange<IQuantity> range = QuantityRange.createWithEnd(start, end);
      IItemCollection filteredEvents = events.apply(ItemFilters.type(eventName))
          .apply(ItemFilters.rangeContainedIn(JfrAttributes.LIFETIME, range));

      if (filteredEvents.hasItems()) {
        for (IItemIterable itemIterable : filteredEvents) {
          IType<IItem> type = itemIterable.getType();
          List<IAttribute<?>> attributes = type.getAttributes();
          for (IAttribute<?> attribute : attributes) {
            if (eventField.equals(attribute.getIdentifier())) {
              IMemberAccessor<IQuantity, IItem> startTimeAccessor = JfrAttributes.START_TIME.getAccessor(type);
              IMemberAccessor<?, IItem> accessor = ItemToolkit.accessor(attribute);

              for (IItem item : itemIterable) {
                if (!(accessor.getMember(item) instanceof IQuantity)) {
                  return;
                }
                JsonArray datapoint = new JsonArray();
                datapoint.add(((IQuantity) accessor.getMember(item)).doubleValue());

                long startTime = 0;
                try {
                  startTime = startTimeAccessor.getMember(item).longValueIn(UnitLookup.EPOCH_MS);
                } catch (QuantityConversionException e) {
                  // Do Nothing
                }
                datapoint.add(startTime);
                datapointsArray.add(datapoint);
              }
            }
          }
        }
      }
    }
  }

  public void getTable(Query query, String target, JsonObject targetObject) {
    if (target.contains(".")) {
      JsonArray columns = new JsonArray();
      targetObject.put("columns", columns);

      JsonArray rows = new JsonArray();
      targetObject.put("rows", rows);

      targetObject.put("type", "table");

      JsonObject timestamp = new JsonObject();
      timestamp.put("text", "Time");
      timestamp.put("type", "time");
      columns.add(timestamp);


      String eventName = target.substring(0, target.lastIndexOf("."));
      String eventField = target.substring(target.lastIndexOf(".") + 1);

      JsonObject field = new JsonObject();
      field.put("text", eventField);
      field.put("type", "number");
      columns.add(field);

      IQuantity start = UnitLookup.EPOCH_MS.quantity(query.getFrom());
      IQuantity end = UnitLookup.EPOCH_MS.quantity(query.getTo());
      IRange<IQuantity> range = QuantityRange.createWithEnd(start, end);
      IItemCollection filteredEvents = events.apply(ItemFilters.type(eventName))
          .apply(ItemFilters.rangeContainedIn(JfrAttributes.LIFETIME, range));
          
      if (filteredEvents.hasItems()) {
        for (IItemIterable itemIterable : filteredEvents) {
          IType<IItem> type = itemIterable.getType();
          List<IAttribute<?>> attributes = type.getAttributes();
          for (IAttribute<?> attribute : attributes) {
            if (eventField.equals(attribute.getIdentifier())) {
              IMemberAccessor<IQuantity, IItem> startTimeAccessor = JfrAttributes.START_TIME.getAccessor(type);
              IMemberAccessor<?, IItem> accessor = ItemToolkit.accessor(attribute);

              for (IItem item : itemIterable) {
                if (!(accessor.getMember(item) instanceof IQuantity)) {
                  return;
                }
                JsonArray datapoint = new JsonArray();
                long startTime = 0;
                try {
                  startTime = startTimeAccessor.getMember(item).longValueIn(UnitLookup.EPOCH_MS);
                } catch (QuantityConversionException e) {
                  // Do Nothing
                }
                datapoint.add(startTime);
                datapoint.add(((IQuantity) accessor.getMember(item)).doubleValue());
                rows.add(datapoint);
              }
            }
          }
        }
      }
    }
  }

  public String annotations() {
    if (events == null) {
      return JsonUtils.EMPTY_ARRAY;
    }
    // TODO: Implement annotation support
    return JsonUtils.EMPTY_ARRAY;
  }

  public void loadEvents(String filename) throws IOException {
    if (filename == null || filename == "") {
      throw new IOException("Invalid JFR filename");
    }
    try {
      File file = new File(filename);
      if (!file.exists() || !file.isFile()) {
        throw new IOException("File not found");
      }
      LOGGER.info("Loading file: " + file.getAbsolutePath());
      this.events = JfrLoaderToolkit.loadEvents(file);
    } catch (CouldNotLoadRecordingException e) {
      LOGGER.error("Failed to read events from recording", e);
      throw new IOException("Failed to load JFR recording", e);
    }
  }

}