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

import java.util.List;
import java.util.concurrent.CompletableFuture;

import javax.annotation.Nullable;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.linecorp.armeria.client.Endpoint;
import com.linecorp.armeria.client.WebClient;
import com.linecorp.armeria.client.WebClientBuilder;
import com.linecorp.armeria.common.QueryParams;

/**
 * The Consul Client for accessing to consul agent API server.
 */
public final class ConsulClient {
    /**
     * Default Consul API SCHEME.
     */
    public static final String DEFAULT_SCHEME = "http";

    /**
     * Default Consul HTTP API HOST.
     */
    public static final String DEFAULT_HTTP_HOST = "localhost";

    /**
     * Default Consul HTTP API PORT.
     */
    public static final int DEFAULT_HTTP_PORT = 8500;

    /**
     * Default Consul API version.
     */
    public static final String DEFAULT_API_VERSION = "v1";

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final WebClient webClient;

    ConsulClient(String uri) {
        this(uri, null);
    }

    ConsulClient(String uri, @Nullable String token) {
        final WebClientBuilder builder = WebClient.builder(uri);
        if (token != null) {
            // TODO test with token
            builder.addHttpHeader("X-Consul-Token", token);
        }
        webClient = builder.build();
    }

    /**
     * Gets a object mapper.
     * @return Jackson ObjectMapper
     */
    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    /**
     * Registers a service to Consul Agent without service ID.
     *
     * @param serviceName service name
     * @param endpoint servicing endpoint
     * @param check a check for health checking
     * @return CompletableFuture with registered service ID(auto-generated)
     */
    public CompletableFuture<String> register(String serviceName, Endpoint endpoint, Check check)
            throws JsonProcessingException {
        return AgentServiceClient.of(this)
                                 .register(serviceName, endpoint.host(), endpoint.port(), check,
                                           QueryParams.of());
    }

    /**
     * Registers a service to Consul Agent with service ID.
     *
     * @param serviceId a service ID that identifying a service
     * @param serviceName a service name to register
     * @param endpoint an endpoint of service to register
     * @param check a check for the service
     * @return CompletableFuture with registered service ID
     */
    public CompletableFuture<String> register(String serviceId, String serviceName, Endpoint endpoint,
                                              Check check) throws JsonProcessingException {
        return AgentServiceClient.of(this)
                                 .register(serviceId, serviceName, endpoint.host(), endpoint.port(), check,
                                           QueryParams.of());
    }

    /**
     * De-registers a service to Consul Agent.
     *
     * @param serviceId a service ID that identifying a service
     */
    public CompletableFuture<Void> deregister(String serviceId) {
        return AgentServiceClient.of(this).deregister(serviceId);
    }

    /**
     * Get registered endpoints with service name from consul agent.
     */
    public CompletableFuture<List<Endpoint>> endpoints(String serviceName) {
        return CatalogClient.of(this)
                            .endpoints(serviceName, QueryParams.of());
    }

    /**
     * Get registered endpoints with service name from consul agent.
     */
    public CompletableFuture<List<Endpoint>> healthyEndpoints(String serviceName) {
        return HealthClient.of(this).healthyEndpoints(serviceName);
    }

    /**
     * Gets a {@code WebClient} for accessing to consul server.
     */
    public WebClient consulWebClient() {
        return webClient;
    }

    /**
     * Gets a URL of consul agent.
     */
    public String url() {
        return webClient.uri().toString();
    }

    /**
     * Returns a new {@link Builder}.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * The builder class to build {@link ConsulClient}.
     */
    public static final class Builder {
        @Nullable
        private String uri;
        @Nullable
        private String scheme;
        @Nullable
        private String host;
        private int port;
        @Nullable
        private String version;
        @Nullable
        private String token;

        private Builder() {
        }

        /**
         * Sets the {@code uri} of the client.
         */
        public Builder url(String uri) {
            this.uri = uri;
            return this;
        }

        /**
         * Sets the {@code scheme} of the client.
         */
        public Builder scheme(String scheme) {
            this.scheme = scheme;
            return this;
        }

        /**
         * Sets the {@code host} of the client.
         */
        public Builder host(String host) {
            this.host = host;
            return this;
        }

        /**
         * Sets the {@code port} of the client.
         */
        public Builder port(int port) {
            this.port = port;
            return this;
        }

        /**
         * Sets the {@code version} of the client.
         */
        public Builder version(String version) {
            this.version = version;
            return this;
        }

        /**
         * Sets the {@code token} of the client.
         */
        public Builder token(String token) {
            this.token = token;
            return this;
        }

        /**
         * Returns a newly-created Consul client based on the properties of this builder.
         *
         * @throws IllegalArgumentException if uri and other properties conflict.
         */
        public ConsulClient build() {
            if (uri != null) {
                if (scheme != null || host != null || port != 0 || version != null) {
                    throw new IllegalArgumentException("uri can not initialize with others");
                }
                if (token == null) {
                    new ConsulClient(uri);
                }
                return new ConsulClient(uri, token);
            }
            final StringBuilder sb = new StringBuilder();
            sb.append(scheme == null ? DEFAULT_SCHEME : scheme)
              .append("://")
              .append(host == null ? DEFAULT_HTTP_HOST : host)
              .append(':')
              .append(port == 0 ? DEFAULT_HTTP_PORT : port)
              .append('/')
              .append(version == null ? DEFAULT_API_VERSION : version);
            if (token == null) {
                new ConsulClient(sb.toString());
            }
            return new ConsulClient(sb.toString(), token);
        }
    }
}
