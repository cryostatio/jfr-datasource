/*
 * Copyright The Cryostat Authors
 *
 * The Universal Permissive License (UPL), Version 1.0
 *
 * Subject to the condition set forth below, permission is hereby granted to any
 * person obtaining a copy of this software, associated documentation and/or data
 * (collectively the "Software"), free of charge and under any and all copyright
 * rights in the Software, and any and all patent rights owned or freely
 * licensable by each licensor hereunder covering either (i) the unmodified
 * Software as contributed to or provided by such licensor, or (ii) the Larger
 * Works (as defined below), to deal in both
 *
 * (a) the Software, and
 * (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
 * one is included with the Software (each a "Larger Work" to which the Software
 * is contributed by such licensors),
 *
 * without restriction, including without limitation the rights to copy, create
 * derivative works of, display, perform, and distribute the Software and make,
 * use, sell, offer for sale, import, export, have made, and have sold the
 * Software and the Larger Work(s), and to sublicense the foregoing rights on
 * either these or other terms.
 *
 * This license is subject to the following condition:
 * The above copyright notice and either this complete permission notice or at
 * a minimum a reference to the UPL must be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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
