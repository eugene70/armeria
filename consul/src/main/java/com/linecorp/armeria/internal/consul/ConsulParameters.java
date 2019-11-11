/*
 * Copyright 2019 LINE Corporation
 *
 * LINE Corporation licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package com.linecorp.armeria.internal.consul;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.collect.ImmutableMap;

/**
 * The Parameter class.
 */
final class ConsulParameters {

    private final ImmutableMap<String, Object> parameters;

    /**
     * BLANK instance.
     */
    public static final ConsulParameters BLANK = builder().build();

    private ConsulParameters(Map<String, Object> parameters) {
        this.parameters = ImmutableMap.copyOf(parameters);
    }

    /**
     * builder.
     *
     * @return the.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * toString implement.
     *
     * @return the...
     */
    @Override
    public String toString() {
        return parameters.toString();
    }

    /**
     * turn.
     *
     * @return the...
     */
    public String toParameterString() {
        final StringBuilder sb = new StringBuilder();
        for (Entry<String, Object> entry : parameters.entrySet()) {
            sb.append(entry.getKey())
              .append('=')
              .append(entry.getValue())
              .append('&');
        }
        if (sb.length() > 0) {
            sb.deleteCharAt(sb.length() - 1);
        }
        return sb.toString();
    }

    static class Builder {
        private final Map<String, Object> parameters = new HashMap<>();

        public Builder service(String service) {
            parameters.put("service", service);
            return this;
        }

        public Builder dc(String dc) {
            parameters.put("dc", dc);
            return this;
        }

        public Builder near(String near) {
            parameters.put("near", near);
            return this;
        }

        public Builder tag(String tag) {
            parameters.put("tag", tag);
            return this;
        }

        public Builder nodeMeta(String nodeMeta) {
            parameters.put("nodeMeta", nodeMeta);
            return this;
        }

        public Builder passing(boolean passing) {
            parameters.put("passing", passing);
            return this;
        }

        public Builder filter(String filter) {
            parameters.put("filter", filter);
            return this;
        }

        public Builder put(String key, Object value) {
            parameters.put(key, value);
            return this;
        }

        public ConsulParameters build() {
            return new ConsulParameters(parameters);
        }
    }
}
