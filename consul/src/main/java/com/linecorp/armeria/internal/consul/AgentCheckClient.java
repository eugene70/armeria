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

import static java.util.Objects.requireNonNull;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Consul API client to registers, de-registers a check and gets list of check.
 * {@code AgentCheckClient} is responsible for endpoint of Consul API:
 * {@code `/agent/check`}(https://www.consul.io/api/agent/check.html)
 */
final class AgentCheckClient {

    private final Logger logger = LoggerFactory.getLogger(AgentCheckClient.class);
    private final ConsulClient client;
    private final ObjectMapper mapper;

    /**
     * Builds an AgentCheckClient with a ConsulClient.
     */
    public static AgentCheckClient of(ConsulClient consulClient) {
        return new AgentCheckClient(consulClient);
    }

    private AgentCheckClient(ConsulClient client) {
        this.client = client;
        mapper = client.getObjectMapper();
    }

    /**
     * Retrieves checks by a check ID.
     *
     * @param checkId check ID for finding
     * @return a check if found
     */
    public CompletableFuture<Check> check(@Nullable String checkId) {
        final String serviceFilter = checkId == null ? "" : "filter=CheckID==" + checkId;
        return client.consulWebClient()
                     .get("/agent/checks?" + serviceFilter)
                     .aggregate()
                     .thenCompose(response -> {
                         final CompletableFuture<Check> future = new CompletableFuture<>();
                         try {
                             final Map<String, Check> checks = mapper.readValue(
                                     response.content().toStringUtf8(),
                                     mapper.getTypeFactory().constructMapType(
                                             Map.class, String.class, Check.class));
                             if (checks == null || checks.isEmpty()) {
                                 future.complete(null);
                             } else {
                                 future.complete(checks.values().stream().findFirst().orElse(null));
                             }
                         } catch (JsonProcessingException e) {
                             future.completeExceptionally(e);
                         }
                         return future;
                     });
    }

    /**
     * Get all checks of agent.
     *
     * @return a map of checks
     */
    public CompletableFuture<Map<String, Check>> checks() {
        return checks(null, null);
    }

    /**
     * Gets a map of checks from agent.
     * The map's key is concat
     */
    public CompletableFuture<Map<String, Check>> checks(
            @Nullable String serviceName, @Nullable ConsulParameters parameters) {
        final String serviceFilter = serviceName == null ? "" : "filter=ServiceName==" + serviceName + '&';
        return client.consulWebClient()
                     .get("/agent/checks?" + serviceFilter +
                          (parameters == null ? "" : parameters.toParameterString()))
                     .aggregate()
                     .thenApply(response -> {
                         try {
                             return mapper.readValue(response.content().toStringUtf8(),
                                                     mapper.getTypeFactory()
                                                           .constructMapType(
                                                                   Map.class, String.class, Check.class));
                         } catch (JsonProcessingException e) {
                             throw new RuntimeException(e);
                         }
                     });
    }

    /**
     * Registers a check into the agent.
     */
    public CompletableFuture<Void> register(Check check) {
        requireNonNull(check, "check");
        final String jsonBody;
        try {
            jsonBody = mapper.writeValueAsString(check);
        } catch (final JsonProcessingException e) {
            final CompletableFuture<Void> completableFuture = new CompletableFuture<>();
            completableFuture.completeExceptionally(e);
            return completableFuture;
        }
        logger.debug("Registers a check, payload: {}", jsonBody);

        return client.consulWebClient()
                     .put("/agent/check/register", jsonBody).aggregate().thenAccept(response -> {});
    }

    /**
     * De-registers a check from the agent.
     */
    public CompletableFuture<Void> deregister(String checkId) {
        requireNonNull(checkId, "checkId");
        return client.consulWebClient()
                     .put("/agent/check/deregister/" + checkId, "")
                     .aggregate()
                     .thenAccept(response -> {});
    }
}
