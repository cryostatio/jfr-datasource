package com.redhat.jfr.events;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;

import com.redhat.jfr.server.Query;

import io.quarkus.runtime.StartupEvent;

import org.openjdk.jmc.common.item.IAttribute;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemIterable;
import org.openjdk.jmc.common.item.IMemberAccessor;
import org.openjdk.jmc.common.item.IType;
import org.openjdk.jmc.common.item.ItemFilters;
import org.openjdk.jmc.common.item.ItemToolkit;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.QuantityConversionException;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.flightrecorder.CouldNotLoadRecordingException;
import org.openjdk.jmc.flightrecorder.JfrAttributes;
import org.openjdk.jmc.flightrecorder.JfrLoaderToolkit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class RecordingService {

  private static final Logger LOGGER = LoggerFactory.getLogger(RecordingService.class);

  private IItemCollection events;

  public String search() {
    if (events == null) {
      return "[]";
    }
    StringBuilder json = new StringBuilder();
    json.append("[");
    Iterator<IItemIterable> i = events.iterator();
    while (i.hasNext()) {
      try {
        IItemIterable item = i.next();
        if (item.hasItems()) {
          IType<IItem> type = item.getType();
          List<IAttribute<?>> attributes = type.getAttributes();
          for (IAttribute<?> attribute : attributes) {
            json.append("\"" + type.getIdentifier() + "." + attribute.getIdentifier() + "\"");
            json.append(",");
          }
        }
      } catch (Exception e) {
        return "[]";
      }
    }

    json.deleteCharAt(json.length() - 1);
    json.append("]");
    return json.toString();
  }

  public String query(Query query) {
    try {
      if (events == null) {
        return "[]";
      }
      StringBuilder json = new StringBuilder();
      json.append("[");
      query.applyTargets((target) -> {
        json.append("{");
        json.append("\"target\"");
        json.append(":");
        json.append("\"" + target + "\"");
        json.append(",");
        json.append("\"datapoints\"");
        json.append(":");
        json.append("[");

        String eventName = target.substring(0, target.lastIndexOf("."));
        String eventField = target.substring(target.lastIndexOf(".") + 1);

        IItemCollection filteredEvents = events.apply(ItemFilters.type(eventName));

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
                  json.append("[");
                  json.append(((IQuantity) accessor.getMember(item)).doubleValue());
                  json.append(",");
                  try {
                    json.append(startTimeAccessor.getMember(item).longValueIn(UnitLookup.EPOCH_MS));
                  } catch (QuantityConversionException e) {
                    json.append(0);
                  }
                  json.append("]");
                  json.append(",");
                }
                json.deleteCharAt(json.length() - 1);
              }
            }
          }
        }

        json.append("]");
        json.append("}");
        json.append(",");
      });
      json.deleteCharAt(json.length() - 1);
      json.append("]");
      return json.toString();
    } catch (Exception e) {
      e.printStackTrace();
      return "[]";
    }
  }

  public String annotations() {
    if (events == null) {
      return "[]";
    }
    return "[]";
  }

  public void loadEvents(String filename) throws IOException {
    if (filename == null || filename == "") {
      throw new IOException("Invalid JFR filename");
    }
    try {
      File file = new File(filename);
      LOGGER.info("Loading file: " + file.getAbsolutePath());
      this.events = JfrLoaderToolkit.loadEvents(file);
    } catch (CouldNotLoadRecordingException e) {
      LOGGER.error("Failed to read events from recording", e);
      throw new IOException("Failed to load JFR recording", e);
    }
  }

}