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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import javax.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;

import com.linecorp.armeria.client.Endpoint;
import com.linecorp.armeria.common.QueryParams;

/**
 * Consul's catalog API client to registers a service, to deregisters a service and to gets list of services.
 * {@code CatalogClient} is responsible for endpoint of Consul API:
 * {@code `/catalog`}(https://www.consul.io/api/catalog.html)
 *
 * <p>Consul provides below catalog APIs, but we build interface not for all in this time.
 * <li> PUT /catalog/register - Register Entity(nodes, services and checks) </li>
 * <li> PUT /catalog/deregister - Deregister Entity </li>
 * <li> GET /catalog/datacenters - List Datacenters </li>
 * <li> GET /catalog/nodes - List Nodes </li>
 * <li> GET /catalog/services - List Services </li>
 * <li> GET /catalog/service/:service - List Nodes for Service * </li>
 * <li> GET /catalog/connect/:service - List Nodes for Connect-capable Service * </li>
 * <li> GET /catalog/node/:node - List Services for Node </li>
 */
final class CatalogClient {

    private final ConsulClient client;
    private final ObjectMapper mapper;

    /**
     * Builds CatalogClient with a ConsulClient.
     */
    public static CatalogClient of(ConsulClient consulClient) {
        return new CatalogClient(consulClient);
    }

    private CatalogClient(ConsulClient client) {
        this.client = client;
        mapper = client.getObjectMapper();
    }

    /**
     * Gets list of services.
     */
    public CompletableFuture<Map<String, Service>> services(String serviceName, QueryParams params) {
        requireNonNull(serviceName, "serviceName");
        final String serviceFilter = "filter=Service==" + serviceName + '&';
        return client.consulWebClient()
                     .get("/catalog/services?" + serviceFilter + params.toQueryString())
                     .aggregate().thenApply(response -> {
                         final Map<String, Service> serviceMap;
                         try {
                             serviceMap = mapper.readValue(
                                     response.content().toStringUtf8(),
                                     mapper.getTypeFactory().constructMapType(
                                             Map.class, String.class, Service.class));
                         } catch (JsonProcessingException e) {
                             throw new RuntimeException(e);
                         }
                         return serviceMap;
                     });
    }

    /**
     * Gets endpoint list with service name.
     */
    public CompletableFuture<List<Endpoint>> endpoints(String serviceName, QueryParams params) {
        requireNonNull(serviceName, "serviceName");
        return service(serviceName, params)
                .thenApply(nodes -> {
                    final List<Endpoint> endpoints = new ArrayList<>(nodes.size());
                    nodes.forEach(node -> {
                        final String host = Strings.isNullOrEmpty(node.serviceAddress) ?
                                            Strings.isNullOrEmpty(node.address) ? "127.0.0.1"
                                                                                : node.address
                                                                                       : node.serviceAddress;
                        endpoints.add(Endpoint.of(host, node.servicePort));
                    });
                    return endpoints;
                });
    }

    /**
     * Gets node list with service name.
     */
    public CompletableFuture<List<Node>> service(String serviceName, QueryParams params) {
        requireNonNull(serviceName, "serviceName");

        return client.consulWebClient()
                     .get("/catalog/service/" + serviceName + '?' + params.toQueryString())
                     .aggregate()
                     .thenApply(response -> {
                         try {
                             return mapper.readValue(response.content().toStringUtf8(),
                                                     mapper.getTypeFactory()
                                                           .constructCollectionType(List.class, Node.class));
                         } catch (JsonProcessingException e) {
                             throw new RuntimeException(e);
                         }
                    });
    }

    /**
     * Gets connect-capable service list by service name.
     */
    public CompletableFuture<List<Node>> connect(String serviceName, QueryParams params) {
        requireNonNull(serviceName, "serviceName");
        return client.consulWebClient()
                     .get("/catalog/connect/" + serviceName + '?' + params.toQueryString())
                     .aggregate()
                     .thenApply(response -> {
                         try {
                             return mapper.readValue(response.content().toStringUtf8(),
                                                     mapper.getTypeFactory().constructCollectionType(
                                                             List.class, Node.class));
                         } catch (JsonProcessingException e) {
                             throw new RuntimeException(e);
                         }
                     });
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(Include.NON_NULL)
    public abstract static class CatalogRegistration {

        @JsonProperty("ID")
        Optional<String> id;

        @JsonProperty("Datacenter")
        Optional<String> datacenter;

        @JsonProperty("Node")
        String node;

        @JsonProperty("Address")
        String address;

        @JsonProperty("NodeMeta")
        Map<String, String> nodeMeta;

        @JsonProperty("TaggedAddresses")
        Optional<Object> taggedAddresses;

        @JsonProperty("Service")
        Optional<Service> service;

        @JsonProperty("Check")
        Optional<Object> check;

        @JsonProperty("WriteRequest")
        Optional<Object> writeRequest;

        @JsonProperty("SkipNodeUpdate")
        Optional<Boolean> skipNodeUpdate;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Service {
        @JsonProperty("Name")
        String name;

        @JsonProperty("ID")
        String id;

        @JsonProperty("Tags")
        String[] tags;

        @JsonProperty("Address")
        String address;

        @JsonProperty("TaggedAddresses")
        Map<String, Object> taggedAddresses;

        @JsonProperty("Meta")
        Map<String, String> meta;

        @JsonProperty("Port")
        int port;

        @JsonProperty("Kind")
        String kind;

        @JsonProperty("Proxy")
        Object proxy;

        @JsonProperty("Connect")
        Object connect;

        @JsonProperty("Check")
        Object check;

        @JsonProperty("Checks")
        Object checks;

        @JsonProperty("EnableTagOverride")
        boolean enableTagOverride;

        @JsonProperty("Weights")
        Map<String, Object> weights;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Node {
        @Nullable
        @JsonProperty("ID")
        String id;

        @Nullable
        @JsonProperty("Node")
        String node;

        @Nullable
        @JsonProperty("Address")
        String address;

        @Nullable
        @JsonProperty("Datacenter")
        String datacenter;

        @Nullable
        @JsonProperty("TaggedAddresses")
        Map<String, String> taggedAddresses;

        @Nullable
        @JsonProperty("NodeMeta")
        Map<String, Object> nodeMeta;

        @JsonProperty("CreateIndex")
        int createIndex;

        @JsonProperty("ModifyIndex")
        int modifyIndex;

        @JsonProperty("ServiceAddress")
        String serviceAddress;

        @JsonProperty("ServiceEnableTagOverride")
        boolean serviceEnableTagOverride;

        @Nullable
        @JsonProperty("ServiceID")
        String serviceId;

        @Nullable
        @JsonProperty("ServiceName")
        String serviceName;

        @JsonProperty("ServicePort")
        int servicePort;

        @Nullable
        @JsonProperty("ServiceMeta")
        Map<String, Object> serviceMeta;

        @Nullable
        @JsonProperty("ServiceTaggedAddresses")
        Map<String, Object> serviceTaggedAddresses;

        @Nullable
        @JsonProperty("ServiceTags")
        String[] serviceTags;

        @Nullable
        @JsonProperty("ServiceProxy")
        Map<String, Object> serviceProxy;

        @Nullable
        @JsonProperty("ServiceConnect")
        Map<String, Object> serviceConnect;

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                              .add("id", id)
                              .add("node", node)
                              .add("address", address)
                              .add("datacenter", datacenter)
                              .add("taggedAddresses", taggedAddresses)
                              .add("nodeMeta", nodeMeta)
                              .add("createIndex", createIndex)
                              .add("modifyIndex", modifyIndex)
                              .add("serviceAddress", serviceAddress)
                              .add("serviceEnableTagOverride", serviceEnableTagOverride)
                              .add("serviceId", serviceId)
                              .add("serviceName", serviceName)
                              .add("servicePort", servicePort)
                              .add("serviceMeta", serviceMeta)
                              .add("serviceTaggedAddresses", serviceTaggedAddresses)
                              .add("serviceTags", serviceTags)
                              .add("serviceProxy", serviceProxy)
                              .add("serviceConnect", serviceConnect)
                              .toString();
        }
    }
}
