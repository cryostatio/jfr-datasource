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
package io.cryostat.jfr.datasource.server;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.function.Consumer;

import io.cryostat.jfr.datasource.utils.InvalidQueryException;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public record Query(JsonObject query) {

    private static final DateTimeFormatter dateFormat = DateTimeFormatter.ISO_INSTANT;

    public JsonArray getTargets() {
        return this.query.getJsonArray("targets");
    }

    public void applyTargets(Consumer<Target> consumer) throws InvalidQueryException {
        JsonArray targets = this.query.getJsonArray("targets");
        for (int i = 0; i < targets.size(); i++) {
            JsonObject target = targets.getJsonObject(i);
            Target t = new Target(target.getString("target"), target.getString("type"));
            consumer.accept(t);
        }
    }

    public long getFrom() {
        TemporalAccessor accessor =
                dateFormat.parse(this.query.getJsonObject("range").getString("from"));
        Instant instant = Instant.from(accessor);
        return instant.toEpochMilli();
    }

    public long getTo() {
        TemporalAccessor accessor =
                dateFormat.parse(this.query.getJsonObject("range").getString("to"));
        Instant instant = Instant.from(accessor);
        return instant.toEpochMilli();
    }
}
