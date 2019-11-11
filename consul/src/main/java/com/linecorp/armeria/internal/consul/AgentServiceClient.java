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

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.MoreObjects;

import com.linecorp.armeria.common.QueryParams;

/**
 * Consul API client to register, deregister service and get list of services.
 * {@code AgentServiceClient} is responsible for endpoint of Consul API:
 * {@code `/agent/service`}(https://www.consul.io/api/agent/service.html)
 *
 * <p>GET /agent/services
 * GET /agent/service/:service_id
 * PUT /agent/service/register
 * PUT /agent/service/deregister/:service_id
 */
final class AgentServiceClient {

    private final Logger logger = LoggerFactory.getLogger(AgentServiceClient.class);
    private final ConsulClient client;
    private final ObjectMapper mapper;

    /**
     * Builds an AgentServiceClient with a ConsulClient.
     */
    public static AgentServiceClient of(ConsulClient consulClient) {
        return new AgentServiceClient(consulClient);
    }

    private AgentServiceClient(ConsulClient client) {
        this.client = client;
        mapper = client.getObjectMapper();
    }

    /**
     * Gets a list of services from consul agent.
     */
    public CompletableFuture<Map<String, Service>> services(
            @Nullable String serviceName, QueryParams params) {
        final String serviceFilter = serviceName != null ? "filter=Service==" + serviceName + '&'
                                                         : "";
        return client.consulWebClient()
                     .get("/agent/services?" + serviceFilter + params.toQueryString())
                     .aggregate()
                     .thenApply(response -> {
                         try {
                             return mapper.readValue(response.content().toStringUtf8(),
                                                     mapper.getTypeFactory()
                                                           .constructMapType(
                                                                   Map.class, String.class, Service.class));
                         } catch (JsonProcessingException e) {
                             throw new RuntimeException(e);
                         }
                     });
    }

    /**
     * Registers a service.
     */
    public CompletableFuture<String> register(String serviceName, String address, int port,
                                              QueryParams params) throws JsonProcessingException {
        return register(serviceName, address, port, null, params);
    }

    /**
     * Registers a service with a health checking endpoint.
     *
     * @return returns a generated service ID.
     */
    public CompletableFuture<String> register(String serviceName, String address, int port,
                                              @Nullable Check check, QueryParams params)
            throws JsonProcessingException {
        final String serviceId = UUID.randomUUID().toString();
        return register(serviceId, serviceName, address, port, check, params);
    }

    /**
     * Registers a service into the consul agent.
     */
    public CompletableFuture<String> register(String serviceId, String serviceName, String address, int port,
                                              @Nullable Check check, QueryParams params)
            throws JsonProcessingException {
        final Service service = new Service();
        service.id = serviceId;
        service.name = serviceName;
        service.address = address;
        service.port = port;
        if (check != null) {
            service.check = check;
        }

        final String jsonBody = mapper.writeValueAsString(service);
        logger.debug("Registers a service, payload: {}", jsonBody);

        return client.consulWebClient()
                     .put("/agent/service/register?" + params.toQueryString(),
                              jsonBody)
                     .aggregate()
                     .thenApply(a -> serviceId);
    }

    /**
     * De-registers a service from the consul agent.
     */
    public CompletableFuture<Void> deregister(String serviceId) {
        requireNonNull(serviceId, "serviceId");
        return client.consulWebClient()
                     .put("/agent/service/deregister/" + serviceId, "")
                     .aggregate().thenAccept(response -> {});
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(Include.NON_NULL)
    public static class Service {

        @Nullable
        @JsonProperty("Service")
        String service;

        @Nullable
        @JsonProperty("Name")
        String name;

        @JsonProperty("ID")
        String id;

        @Nullable
        @JsonProperty("Tags")
        String[] tags;

        @Nullable
        @JsonProperty("Address")
        String address;

        @Nullable
        @JsonProperty("TaggedAddresses")
        Map<String, Object> taggedAddresses;

        @Nullable
        @JsonProperty("Meta")
        Map<String, String> meta;

        @JsonProperty("Port")
        int port;

        @Nullable
        @JsonProperty("Kind")
        String kind;

        @Nullable
        @JsonProperty("Proxy")
        Object proxy;

        @Nullable
        @JsonProperty("Connect")
        Object connect;

        @Nullable
        @JsonProperty("Check")
        Check check;

        @Nullable
        @JsonProperty("Checks")
        List<Check> checks;

        @JsonProperty("EnableTagOverride")
        boolean enableTagOverride;

        @Nullable
        @JsonProperty("Weights")
        Map<String, Object> weights;

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                              .add("service", service)
                              .add("name", name)
                              .add("id", id)
                              .add("tags", tags)
                              .add("address", address)
                              .add("taggedAddresses", taggedAddresses)
                              .add("meta", meta)
                              .add("port", port)
                              .add("kind", kind)
                              .add("proxy", proxy)
                              .add("connect", connect)
                              .add("check", check)
                              .add("checks", checks)
                              .add("enableTagOverride", enableTagOverride)
                              .add("weights", weights)
                              .toString();
        }
    }
}
