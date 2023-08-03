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

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import io.cryostat.jfr.datasource.utils.InvalidQueryException;

public class Target {
    private final String targetIdentifier;
    private final String type;
    private final Map<String, Set<String>> targetOptions;

    public static final String durationTargetIdentifier = "events.custom.recordingDuration";
    public static final String startTimeTargetIdentifier = "events.custom.startTime";

    public enum ParamOperator { // TODO: Support inequality for numeric fields.
        EQUAL("=");

        private final String identifier;

        ParamOperator(String identifier) {
            this.identifier = identifier;
        }

        String getIdentifier() {
            return this.identifier;
        }
    }

    private static final String PARAM_SEPARATOR = "&";

    public Target(String target, String type) throws InvalidQueryException {
        this.targetIdentifier = parseTargetIdentifier(target);
        this.targetOptions = parseTargetOptions(target);
        this.type = type;
    }

    public String getTargetIdentifier() {
        return this.targetIdentifier;
    }

    public String getType() {
        return this.type;
    }

    public Map<String, Set<String>> getTargetOptions() {
        return this.targetOptions;
    }

    private String parseTargetIdentifier(String target) {
        final int idx = target.indexOf("?");
        return idx >= 0 ? target.substring(0, target.indexOf("?")) : target;
    }

    private Map<String, Set<String>> parseTargetOptions(String target)
            throws InvalidQueryException {
        target = target.replaceAll("\\\\", ""); // Remove escapes
        final Map<String, Set<String>> options = new HashMap<>();
        final int idx = target.indexOf("?");
        if (idx >= 0) {
            for (String option : target.substring(idx + 1).split(PARAM_SEPARATOR)) {
                int equalSignIndex = option.indexOf(ParamOperator.EQUAL.getIdentifier());

                if (equalSignIndex < 0) {
                    throw new InvalidQueryException(option);
                }

                String fieldName = option.substring(0, equalSignIndex);
                String[] fieldValues =
                        (equalSignIndex + 1 >= option.length())
                                ? new String[] {""}
                                : option.substring(equalSignIndex + 1).split(",");

                if (options.containsKey(fieldName)) {
                    options.get(fieldName).addAll(Arrays.asList(fieldValues));
                } else {
                    options.put(fieldName, new HashSet<String>(Arrays.asList(fieldValues)));
                }
            }
        }
        return options;
    }
}
