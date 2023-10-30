/*
 * Copyright The Cryostat Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.cryostat.jfr.datasource.events;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.openjdk.jmc.common.item.IAccessorKey;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemIterable;
import org.openjdk.jmc.common.item.IMemberAccessor;
import org.openjdk.jmc.common.item.IType;
import org.openjdk.jmc.common.item.ItemFilters;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.common.unit.IRange;
import org.openjdk.jmc.common.unit.IUnit;
import org.openjdk.jmc.common.unit.QuantityConversionException;
import org.openjdk.jmc.common.unit.QuantityRange;
import org.openjdk.jmc.common.unit.UnitLookup;
import org.openjdk.jmc.flightrecorder.CouldNotLoadRecordingException;
import org.openjdk.jmc.flightrecorder.JfrAttributes;
import org.openjdk.jmc.flightrecorder.JfrLoaderToolkit;

import io.cryostat.jfr.datasource.json.JsonUtils;
import io.cryostat.jfr.datasource.server.Query;
import io.cryostat.jfr.datasource.server.Search;
import io.cryostat.jfr.datasource.server.Target;
import io.cryostat.jfr.datasource.utils.ArgRunnable;

import com.fasterxml.jackson.databind.JsonMappingException;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class RecordingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecordingService.class);

    private IItemCollection events;

    public boolean eventsLoaded() {
        return events != null;
    }

    public String search(Search search) throws JsonMappingException {
        if (!eventsLoaded()) {
            return JsonUtils.EMPTY_ARRAY;
        }
        if (search.getTarget().isPresent()) {
            String target = search.getTarget().get();
            return target.equals("*") ? getEventTypes() : getTargetValues(target);
        } else {
            throw new JsonMappingException(null, "missing target field in json body");
        }
    }

    public String getEventTypes() throws JsonMappingException {
        JsonArray json = new JsonArray();
        Iterator<IItemIterable> i = events.iterator();
        while (i.hasNext()) {
            try {
                IItemIterable item = i.next();
                if (item.hasItems()) {
                    IType<IItem> type = item.getType();
                    for (IAccessorKey<?> attribute : type.getAccessorKeys().keySet()) {
                        if (attribute
                                        .getIdentifier()
                                        .contains(JfrAttributes.EVENT_TYPE.getIdentifier())
                                || attribute
                                        .getIdentifier()
                                        .contains(JfrAttributes.START_TIME.getIdentifier())
                                || attribute
                                        .getIdentifier()
                                        .contains(JfrAttributes.END_TIME.getIdentifier())) {
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

    public String getTargetValues(String target) {
        if (target.isBlank()) {
            return JsonUtils.EMPTY_ARRAY;
        }

        JsonArray json = new JsonArray();
        IItemCollection filteredEvents = filterEvents(target);
        String targetField = target.substring(target.lastIndexOf(".") + 1);

        // Should be only 0 or 1 iterator as filtered by name
        for (IItemIterable itemIterable : filteredEvents) {
            IType<IItem> type = itemIterable.getType();
            IMemberAccessor<?, IItem> accessor = null;
            for (IAccessorKey<?> attribute : type.getAccessorKeys().keySet()) {
                if (targetField.equals(attribute.getIdentifier())) {
                    accessor = type.getAccessor(attribute);
                    break;
                }
            }

            if (accessor != null) {
                for (IItem item : itemIterable) {
                    json.add(accessor.getMember(item).toString());
                }
            }
        }
        return json.toString();
    }

    public JsonObject getDuration() {
        final JsonObject targetObject = new JsonObject();

        JsonArray columns = new JsonArray();
        JsonObject targetCol = new JsonObject();
        targetCol.put("text", "Duration");
        targetCol.put("type", "number");
        columns.add(targetCol);

        JsonArray rows = new JsonArray();

        targetObject.put("rows", rows);
        targetObject.put("columns", columns);

        long startTime = Long.MAX_VALUE;
        long stopTime = 0;

        for (IItemIterable itemIterable : events) {
            IType<IItem> type = itemIterable.getType();
            IMemberAccessor<IQuantity, IItem> startTimeAccessor =
                    JfrAttributes.START_TIME.getAccessor(type);
            IMemberAccessor<IQuantity, IItem> endTimeAccessor =
                    JfrAttributes.END_TIME.getAccessor(type);
            for (IItem item : itemIterable) {
                try {
                    long eventStartTime =
                            startTimeAccessor.getMember(item).longValueIn(UnitLookup.EPOCH_MS);

                    startTime = Math.min(eventStartTime, startTime);
                    stopTime = Math.max(eventStartTime, stopTime);
                    if (endTimeAccessor != null) {
                        long eventEndTime =
                                endTimeAccessor.getMember(item).longValueIn(UnitLookup.EPOCH_MS);
                        stopTime = Math.max(eventEndTime, stopTime);
                    }
                } catch (QuantityConversionException e) {
                    // Do nothing
                }
            }
        }

        JsonArray row = new JsonArray();
        LOGGER.info("Start time: {}", startTime);
        LOGGER.info("Stop time: {}", stopTime);

        row.add(Long.valueOf(Math.max(stopTime - startTime, 0)));
        rows.add(row);

        return targetObject;
    }

    public JsonObject getStartTime() {
        final JsonObject targetObject = new JsonObject();

        JsonArray columns = new JsonArray();
        JsonObject targetCol = new JsonObject();
        targetCol.put("text", "Start Time");
        targetCol.put("type", "number");
        columns.add(targetCol);

        JsonArray rows = new JsonArray();

        targetObject.put("rows", rows);
        targetObject.put("columns", columns);

        long startTime = Long.MAX_VALUE;

        for (IItemIterable itemIterable : events) {
            IType<IItem> type = itemIterable.getType();
            IMemberAccessor<IQuantity, IItem> startTimeAccessor =
                    JfrAttributes.START_TIME.getAccessor(type);
            for (IItem item : itemIterable) {
                try {
                    long markTime =
                            startTimeAccessor.getMember(item).longValueIn(UnitLookup.EPOCH_MS);

                    startTime = Math.min(markTime, startTime);
                } catch (QuantityConversionException e) {
                    // Do nothing
                }
            }
        }

        JsonArray row = new JsonArray();
        row.add(Long.valueOf(startTime));
        rows.add(row);

        return targetObject;
    }

    public String query(Query query) {
        try {
            if (events == null) {
                return JsonUtils.EMPTY_ARRAY;
            }
            JsonArray responseJson = new JsonArray();
            query.applyTargets(
                    (t) -> {
                        String type = t.getType();
                        LOGGER.info(type);
                        if (type.equals("timeserie")) {
                            for (JsonObject obj :
                                    this.getTimeseries(t, query.getFrom(), query.getTo())) {
                                responseJson.add(obj);
                            }
                        } else if (type.equals("table")) {
                            responseJson.add(this.getTable(t, query.getFrom(), query.getTo()));
                        }
                    });
            return responseJson.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return JsonUtils.EMPTY_ARRAY;
        }
    }

    public List<JsonObject> getTimeseries(Target target, long from, long to) {
        final List<JsonObject> targetObjects = new ArrayList<>();

        final String targetIdentifier = target.getTargetIdentifier(); // eventName.field format
        final Map<String, Set<String>> targetOptions = target.getTargetOptions();

        // Field name of the target event
        String eventField = targetIdentifier.substring(targetIdentifier.lastIndexOf(".") + 1);

        // A set of target options that are specified as * (all)
        final Set<String> toUpdateOptions = new HashSet<>();

        // Populate response json array
        if (targetOptions.isEmpty()) {
            targetObjects.add(createEmptyTargetJson(targetIdentifier, Optional.empty()));
        } else {
            targetOptions.forEach(
                    (fieldName, valueSet) -> {
                        if (valueSet.contains(
                                "*")) { // Do not exact values. Add in each event iteration.
                            toUpdateOptions.add(fieldName);
                        } else {
                            valueSet.forEach(
                                    (val) -> {
                                        targetObjects.add(
                                                createEmptyTargetJson(val, Optional.of(fieldName)));
                                    });
                        }
                    });
        }

        applyFilterEvents(
                targetIdentifier,
                from,
                to,
                (filteredEvents) -> {
                    // Iterators for event types that match filters
                    // Note: There should be only 0 or 1 iterator as filter is defined as event
                    // name.
                    for (IItemIterable itemIterable : filteredEvents) {
                        IType<IItem> type = itemIterable.getType();
                        final Map<String, IMemberAccessor<?, IItem>> aMap = new HashMap<>();
                        for (IAccessorKey<?> attribute : type.getAccessorKeys().keySet()) {
                            if (eventField.equals(attribute.getIdentifier())) {
                                aMap.put(eventField, type.getAccessor(attribute));
                            } else if (targetOptions.get(attribute.getIdentifier()) != null) {
                                aMap.put(attribute.getIdentifier(), type.getAccessor(attribute));
                            }
                        }

                        if (!aMap.isEmpty() && aMap.get(eventField) != null) {
                            IMemberAccessor<?, IItem> targetAccessor = aMap.get(eventField);
                            for (IItem item : itemIterable) { // Iterate on each event
                                long startTime = 0;
                                JsonArray datapoint = new JsonArray(); // [y, x]

                                Object value = targetAccessor.getMember(item);
                                if (value instanceof IQuantity) {
                                    IQuantity quanity = (IQuantity) value;
                                    IUnit displayUnit = displayUnit(quanity.getUnit());
                                    datapoint.add(quanity.doubleValueIn(displayUnit));
                                } else {
                                    // Note: content can be JSON, which requires transformation in
                                    // Grafana.
                                    datapoint.add(value.toString());
                                }

                                try {
                                    IMemberAccessor<IQuantity, IItem> startTimeAccessor =
                                            JfrAttributes.START_TIME.getAccessor(type);
                                    startTime =
                                            startTimeAccessor
                                                    .getMember(item)
                                                    .longValueIn(UnitLookup.EPOCH_MS);
                                } catch (QuantityConversionException e) {
                                    // Do Nothing
                                }
                                datapoint.add(startTime);

                                if (targetOptions.isEmpty()) {
                                    targetObjects.get(0).getJsonArray("datapoints").add(datapoint);
                                } else {
                                    targetOptions.forEach(
                                            (fieldName, valueSet) -> {
                                                IMemberAccessor<?, IItem> accessor =
                                                        aMap.get(fieldName);
                                                if (accessor != null) {
                                                    String group =
                                                            accessor.getMember(item).toString();
                                                    boolean found = false;
                                                    for (JsonObject obj : targetObjects) {
                                                        // Must match option field name (i.e.
                                                        // category) and target name
                                                        String paramField =
                                                                obj.getJsonObject("meta")
                                                                        .getString("paramField");
                                                        if (paramField.equals(fieldName)
                                                                && obj.getString("target")
                                                                        .equals(group)) {
                                                            found = true;
                                                            obj.getJsonArray("datapoints")
                                                                    .add(datapoint);
                                                        }
                                                    }
                                                    if (toUpdateOptions.contains(fieldName)
                                                            && !found) {
                                                        JsonObject obj =
                                                                createEmptyTargetJson(
                                                                        group,
                                                                        Optional.of(fieldName));
                                                        obj.getJsonArray("datapoints")
                                                                .add(datapoint);
                                                        targetObjects.add(obj);
                                                    }
                                                }
                                            });
                                }
                            }
                        }
                    }
                });

        return targetObjects;
    }

    public JsonObject getTable(Target target, long from, long to) {
        final JsonObject targetObject = new JsonObject();
        final String targetIdentifier = target.getTargetIdentifier();

        // Special cases for duration and startTime
        if (targetIdentifier.equals(Target.durationTargetIdentifier)) {
            return getDuration();
        } else if (targetIdentifier.equals(Target.startTimeTargetIdentifier)) {
            return getStartTime();
        }

        String targetEventField = targetIdentifier.substring(targetIdentifier.lastIndexOf(".") + 1);

        targetObject.put("type", "table");

        JsonArray columns = new JsonArray();

        JsonObject timestampCol = new JsonObject();
        timestampCol.put("text", "Time");
        timestampCol.put("type", "time");
        columns.add(timestampCol);

        JsonObject targetCol = new JsonObject();
        targetCol.put("text", targetEventField);
        targetCol.put("type", "string"); // default string
        columns.add(targetCol);

        JsonArray rows = new JsonArray();

        targetObject.put("rows", rows);
        targetObject.put("columns", columns);

        applyFilterEvents(
                targetIdentifier,
                from,
                to,
                (filteredEvents) -> {
                    // Iterators for event types that match filters
                    // Note: There should be only 0 or 1 iterator as filter is defined as event
                    // name.
                    for (IItemIterable itemIterable : filteredEvents) {
                        IType<IItem> type = itemIterable.getType();
                        IMemberAccessor<?, IItem> accessor = null;
                        for (IAccessorKey<?> attribute : type.getAccessorKeys().keySet()) {
                            if (targetEventField.equals(attribute.getIdentifier())) {
                                accessor = type.getAccessor(attribute);
                                columns.forEach(
                                        (obj) -> { // Update targetField type
                                            JsonObject jsonObj = ((JsonObject) obj);
                                            String colName = jsonObj.getString("text");
                                            if (colName.equals(targetEventField)) {
                                                jsonObj.put(
                                                        "type",
                                                        getColumnType(
                                                                attribute
                                                                        .getContentType()
                                                                        .getIdentifier()));
                                            }
                                        });
                            }
                        }

                        if (accessor != null) {
                            for (IItem item : itemIterable) {
                                JsonArray datapoint = new JsonArray();
                                long startTime = 0;
                                try {
                                    IMemberAccessor<IQuantity, IItem> startTimeAccessor =
                                            JfrAttributes.START_TIME.getAccessor(type);
                                    startTime =
                                            startTimeAccessor
                                                    .getMember(item)
                                                    .longValueIn(UnitLookup.EPOCH_MS);
                                } catch (QuantityConversionException e) {
                                    // Do Nothing
                                }
                                datapoint.add(startTime);

                                Object value = accessor.getMember(item);
                                if (value instanceof IQuantity) {
                                    IQuantity quanity = (IQuantity) value;
                                    IUnit displayUnit = displayUnit(quanity.getUnit());
                                    datapoint.add(quanity.doubleValueIn(displayUnit));
                                } else {
                                    datapoint.add(value.toString());
                                }

                                rows.add(datapoint);
                            }
                        }
                    }
                });
        return targetObject;
    }

    public void applyFilterEvents(
            String targetIdentifier, long from, long to, ArgRunnable<IItemCollection> runnable) {
        runnable.run(filterEvents(targetIdentifier, from, to));
    }

    public IItemCollection filterEvents(String targetIdentifier, long from, long to) {
        String eventName = targetIdentifier.substring(0, targetIdentifier.lastIndexOf("."));
        IQuantity start = UnitLookup.EPOCH_MS.quantity(from);
        IQuantity end = UnitLookup.EPOCH_MS.quantity(to);
        IRange<IQuantity> range = QuantityRange.createWithEnd(start, end);
        IItemCollection filteredEvents =
                this.events.apply(
                        ItemFilters.and(
                                ItemFilters.type(eventName),
                                ItemFilters.rangeContainedIn(JfrAttributes.LIFETIME, range)));
        return filteredEvents;
    }

    public IItemCollection filterEvents(String targetIdentifier) {
        String eventName = targetIdentifier.substring(0, targetIdentifier.lastIndexOf("."));
        IItemCollection filteredEvents = this.events.apply(ItemFilters.type(eventName));
        return filteredEvents;
    }

    public JsonObject createEmptyTargetJson(String identifier, Optional<String> paramField) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.put("target", identifier);
        jsonObject.put("datapoints", new JsonArray());
        jsonObject.put("meta", new JsonObject());

        if (paramField.isPresent()) {
            jsonObject.getJsonObject("meta").put("paramField", paramField.get());
        }
        return jsonObject;
    }

    public String annotations() {
        if (events == null) {
            return JsonUtils.EMPTY_ARRAY;
        }
        // TODO: Implement annotation support
        // Reference: https://grafana.com/grafana/plugins/grafana-simple-json-datasource/
        return JsonUtils.EMPTY_ARRAY;
    }

    public IUnit displayUnit(IUnit originalUnit) {
        String unitIdentifier = originalUnit.getContentType().getIdentifier();
        switch (unitIdentifier) {
            case "memory":
                return UnitLookup.BYTE;
            case "timespan":
                return UnitLookup.MILLISECOND;
            case "frequency":
                return UnitLookup.HERTZ;
            default:
                return originalUnit;
        }
    }

    public String getColumnType(String unitIdentifier) {
        switch (unitIdentifier) {
            case "memory":
            case "timespan":
            case "frequency":
                return "number";
            default:
                return "string";
        }
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
            LOGGER.info("Loading file: {}", file.getAbsolutePath());
            this.events = JfrLoaderToolkit.loadEvents(file);
        } catch (CouldNotLoadRecordingException e) {
            LOGGER.error("Failed to read events from recording", e);
            throw new IOException("Failed to load JFR recording", e);
        }
    }
}
