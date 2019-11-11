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
package com.linecorp.armeria.client.consul;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;

import com.linecorp.armeria.client.Endpoint;
import com.linecorp.armeria.client.endpoint.DynamicEndpointGroup;
import com.linecorp.armeria.client.endpoint.EndpointGroup;
import com.linecorp.armeria.client.retry.Backoff;
import com.linecorp.armeria.internal.consul.ConsulClient;

/**
 * A Consul-based {@link EndpointGroup} implementation. This {@link EndpointGroup} retrieves the list of
 * {@link Endpoint}s from a Consul using {@link ConsulClient} and updates it when the status of the
 * service by polling.
 */
public final class ConsulEndpointGroup extends DynamicEndpointGroup {

    /**
     * Create a {@code ConsulEndpointGroup} with only service name. This {@code ConsulEndpointGroup} will
     * retrieves the list of {@link Endpoint}s from a local Consul agent(using default consul service port).
     */
    public static ConsulEndpointGroup of(String serviceName) {
        return builder(serviceName).build();
    }

    /**
     * Creates a {@code ConsulEndpointGroupBuilder} to build {@code ConsulEndpointGroup}.
     */
    public static ConsulEndpointGroupBuilder builder(String serviceName) {
        return new ConsulEndpointGroupBuilder(serviceName);
    }

    private final ConsulClient consulClient;
    private final String serviceName;
    private final ScheduledExecutorService executorService;
    private final Backoff retryBackoff;

    @Nullable
    private volatile ScheduledFuture<?> scheduledFuture;

    /**
     * Create a Consul-based {@link EndpointGroup}, endpoints will be retrieved by service name using
     * {@link ConsulClient}.
     *
     * @param consulClient   a consul client
     * @param serviceName a service name to retrieve
     * @param executorService a executorService
     * @param retryBackoff a retry backoff to check
     */
    ConsulEndpointGroup(ConsulClient consulClient, String serviceName, ScheduledExecutorService executorService,
                        Backoff retryBackoff) {
        this.consulClient = consulClient;
        this.serviceName = serviceName;
        this.executorService = executorService;
        this.retryBackoff = retryBackoff;

        consulClient.healthyEndpoints(serviceName)
                    .thenAccept(this::setEndpoints);
        whenReady().join();
        scheduledFuture = executorService.schedule(this::update,
                                                   nextDelayMillis(),
                                                   TimeUnit.MILLISECONDS);
    }

    private void update() {
        if (isClosing()) {
            return;
        }
        consulClient.healthyEndpoints(serviceName).thenAccept(this::setEndpoints);
        scheduledFuture = executorService.schedule(this::update,
                                                   nextDelayMillis(),
                                                   TimeUnit.MILLISECONDS);
    }

    private long nextDelayMillis() {
        final long delayMillis = retryBackoff.nextDelayMillis(1);
        if (delayMillis < 0) {
            throw new IllegalStateException("retryBackoff.nextDelayMillis(1) returned a negative value: " +
                                            delayMillis);
        }
        return delayMillis;
    }

    /**
     * Implements close method of {@link AutoCloseable}.
     */
    @Override
    protected void doCloseAsync(CompletableFuture<?> future) {
        final ScheduledFuture<?> scheduledFuture = this.scheduledFuture;
        if (scheduledFuture != null) {
            scheduledFuture.cancel(true);
        }
        future.complete(null);
    }
}
