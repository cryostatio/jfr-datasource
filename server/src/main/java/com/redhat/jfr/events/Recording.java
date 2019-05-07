package com.redhat.jfr.events;

import java.io.File;
import java.util.Iterator;

import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemIterable;
import org.openjdk.jmc.common.item.IType;
import org.openjdk.jmc.flightrecorder.JfrLoaderToolkit;

public class Recording {

  private String filename;
  private IItemCollection events;

  public Recording(String filename) {
    this.filename = filename;
    loadEvents();
  }

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
        IType<IItem> type = item.getType();
        json.append("\"" + type.getIdentifier() + "\"");
        json.append(",");
      } catch (Exception e) {
        return "[]";
      }
    }

    json.deleteCharAt(json.length() - 1);
    json.append("]");
    return json.toString();
  }

  public String query() {
    if (events == null) {
      return "[]";
    }
    return "[]";
  }

  public String annotations() {
    if (events == null) {
      return "[]";
    }
    return "[]";
  }

  private void loadEvents() {
    try {
      this.events = JfrLoaderToolkit.loadEvents(new File(this.filename));
    } catch (Exception e) {

    }
  }

}