/*
 * Copyright 2020 LINE Corporation
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
package com.linecorp.armeria.server.consul;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import java.net.URI;
import java.time.Duration;

import javax.annotation.Nullable;

import com.linecorp.armeria.client.Endpoint;
import com.linecorp.armeria.common.HttpMethod;
import com.linecorp.armeria.common.SessionProtocol;
import com.linecorp.armeria.internal.consul.ConsulClientBuilder;
import com.linecorp.armeria.server.Server;

/**
 * Builds a new {@link ConsulUpdatingListener}, which registers the server to Consul cluster.
 * <h3>Examples</h3>
 * <pre>{@code
 * ConsulUpdatingListener listener = ConsulUpdatingListener.builder("myService")
 *                                                         .consulPort(8501")
 *                                                         .build();
 * ServerBuilder sb = Server.builder();
 * sb.serverListener(listener);
 * }</pre>
 */
public final class ConsulUpdatingListenerBuilder extends ConsulClientBuilder {

    private static final long DEFAULT_CHECK_INTERVAL_MILLIS = 10_000;
    private static final HttpMethod DEFAULT_CHECK_METHOD = HttpMethod.HEAD;
    private final String serviceName;

    @Nullable
    private Endpoint serviceEndpoint;
    @Nullable
    private URI checkUri;
    private String checkInterval = DEFAULT_CHECK_INTERVAL_MILLIS + "ms";
    private HttpMethod checkMethod = DEFAULT_CHECK_METHOD;

    /**
     * Creates a {@link ConsulUpdatingListenerBuilder} with a service name.
     *
     * @param serviceName the service name to register
     */
    ConsulUpdatingListenerBuilder(String serviceName) {
        this.serviceName = requireNonNull(serviceName, "serviceName");
        checkArgument(!this.serviceName.isEmpty(), "serviceName can't be empty");
    }

    /**
     * Sets URI for checking health by Consul agent.
     *
     * @param checkUri the URI for checking health of service
     */
    public ConsulUpdatingListenerBuilder checkUri(URI checkUri) {
        this.checkUri = requireNonNull(checkUri, "checkUri");
        return this;
    }

    /**
     * Sets URI for checking health by Consul agent.
     *
     * @param checkUri the URI for checking health of service
     */
    public ConsulUpdatingListenerBuilder checkUri(String checkUri) {
        requireNonNull(checkUri, "checkUri");
        checkArgument(!checkUri.isEmpty(), "checkUri can't be empty");
        return checkUri(URI.create(checkUri));
    }

    /**
     * Sets HTTP method for checking health by Consul agent.
     * If not set {@value DEFAULT_CHECK_METHOD} is used by default.
     *
     * <p>Note that the {@code checkMethod} should be configured with {@link #checkUri(String)}.
     * Otherwise, the {@link #build()} method will throw an {@link IllegalStateException}.
     *
     * @param checkMethod the {@link HttpMethod} for checking health of service
     */
    public ConsulUpdatingListenerBuilder checkMethod(HttpMethod checkMethod) {
        this.checkMethod = requireNonNull(checkMethod, "checkMethod");
        return this;
    }

    /**
     * Sets the specified {@link Duration} for checking health.
     * If not set {@value DEFAULT_CHECK_INTERVAL_MILLIS} milliseconds is used by default.
     *
     * <p>Note that the {@code checkInterval} should be configured with {@link #checkUri(URI)}.
     * Otherwise, the {@link #build()} method will throw an {@link IllegalStateException}.
     */
    public ConsulUpdatingListenerBuilder checkInterval(Duration checkInterval) {
        requireNonNull(checkInterval, "checkInterval");
        checkIntervalMillis(checkInterval.toMillis());
        return this;
    }

    /**
     * Sets the specified {@code checkIntervalMills} for checking health in milliseconds.
     * If not set {@value DEFAULT_CHECK_INTERVAL_MILLIS} is used by default.
     *
     * <p>Note that the {@code checkIntervalMillis} should be configured with {@link #checkUri(URI)}.
     * Otherwise, the {@link #build()} method will throws {@link IllegalStateException}.
     */
    public ConsulUpdatingListenerBuilder checkIntervalMillis(long checkIntervalMillis) {
        checkArgument(checkIntervalMillis > 0, "checkIntervalMillis should be positive");
        checkInterval = checkIntervalMillis + "ms";
        return this;
    }

    /**
     * Sets the {@link Endpoint} to register. If not set, the current host name is used by default.
     *
     * @param endpoint the {@link Endpoint} to register
     */
    public ConsulUpdatingListenerBuilder endpoint(Endpoint endpoint) {
        serviceEndpoint = requireNonNull(endpoint, "endpoint");
        return this;
    }

    @Override
    public ConsulUpdatingListenerBuilder consulUri(URI consulUri) {
        return (ConsulUpdatingListenerBuilder) super.consulUri(consulUri);
    }

    @Override
    public ConsulUpdatingListenerBuilder consulUri(String consulUri) {
        return (ConsulUpdatingListenerBuilder) super.consulUri(consulUri);
    }

    @Override
    public ConsulUpdatingListenerBuilder consulProtocol(SessionProtocol consulProtocol) {
        return (ConsulUpdatingListenerBuilder) super.consulProtocol(consulProtocol);
    }

    @Override
    public ConsulUpdatingListenerBuilder consulAddress(String consulAddress) {
        return (ConsulUpdatingListenerBuilder) super.consulAddress(consulAddress);
    }

    @Override
    public ConsulUpdatingListenerBuilder consulPort(int consulPort) {
        return (ConsulUpdatingListenerBuilder) super.consulPort(consulPort);
    }

    @Override
    public ConsulUpdatingListenerBuilder consulApiVersion(String consulApiVersion) {
        return (ConsulUpdatingListenerBuilder) super.consulApiVersion(consulApiVersion);
    }

    @Override
    public ConsulUpdatingListenerBuilder consulToken(String consulToken) {
        return (ConsulUpdatingListenerBuilder) super.consulToken(consulToken);
    }

    /**
     * Returns a newly-created {@link ConsulUpdatingListener} that registers the {@link Server} to
     * Consul when the {@link Server} starts.
     */
    public ConsulUpdatingListener build() {
        return new ConsulUpdatingListener(buildClient(), serviceName, serviceEndpoint, checkUri, checkMethod,
                                          checkInterval);
    }
}
