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
package com.linecorp.armeria.client.consul;

import static java.util.Objects.requireNonNull;

import java.util.concurrent.ScheduledExecutorService;

import javax.annotation.Nullable;

import com.linecorp.armeria.client.retry.Backoff;
import com.linecorp.armeria.common.CommonPools;
import com.linecorp.armeria.internal.consul.ConsulClient;

/**
 * Builder class for {@link ConsulEndpointGroup}. It helps to build {@link ConsulEndpointGroup}.
 */
public final class ConsulEndpointGroupBuilder {

    private final String serviceName;
    @Nullable
    private String consulUrl;
    @Nullable
    private ConsulClient consulClient;
    private ScheduledExecutorService executorService = CommonPools.blockingTaskExecutor();
    private static final Backoff defaultHealthCheckBackoff = Backoff.fixed(3000).withJitter(0.2);
    private Backoff retryBackoff = defaultHealthCheckBackoff;
    @Nullable
    private String token;

    /**
     * Constructor of {@code ConsulEndpointGroupBuilder}.
     */
    ConsulEndpointGroupBuilder(String serviceName) {
        this.serviceName = requireNonNull(serviceName, "serviceName");
    }

    /**
     * Sets the {@code consulUrl}.
     */
    public ConsulEndpointGroupBuilder consulUrl(String consulUrl) {
        this.consulUrl = requireNonNull(consulUrl, "consulUrl");
        consulClient = null;
        return this;
    }

    /**
     * Sets the {@code consulClient}.
     */
    public ConsulEndpointGroupBuilder consulClient(ConsulClient consulClient) {
        this.consulClient = requireNonNull(consulClient, "consulClient");
        return this;
    }

    /**
     * Sets the {@code retryBackoff}.
     */
    public ConsulEndpointGroupBuilder retryBackoff(Backoff retryBackoff) {
        this.retryBackoff = requireNonNull(retryBackoff, "retryBackoff");
        return this;
    }

    /**
     * Sets the {@code executorService}.
     */
    public ConsulEndpointGroupBuilder executorService(ScheduledExecutorService executorService) {
        this.executorService = requireNonNull(executorService, "executorService");
        return this;
    }

    /**
     * Sets the {@code token} to access consul server.
     */
    public ConsulEndpointGroupBuilder token(String token) {
        this.token = requireNonNull(token, "token");
        return this;
    }

    /**
     * Builds a {@code ConsulEndpointGroup}.
     * @return a ConsulEndpointGroup
     */
    public ConsulEndpointGroup build() {
        if (consulClient == null) {
            final ConsulClient.Builder builder = ConsulClient.builder();
            if (consulUrl != null) {
                builder.url(consulUrl);
            }
            if (token != null) {
                builder.token(token);
            }
            consulClient = builder.build();
        }
        return new ConsulEndpointGroup(consulClient, serviceName, executorService, retryBackoff);
    }
}
