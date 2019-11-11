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

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;

import com.linecorp.armeria.client.Endpoint;
import com.linecorp.armeria.client.consul.ConsulTestBase;
import com.linecorp.armeria.common.QueryParams;
import com.linecorp.armeria.server.Server;

class CachedConsulClientTest extends ConsulTestBase {

    private static final AgentServiceClient serviceClient = AgentServiceClient.of(client());
    @Nullable
    private static Endpoint serviceEndpoint;
    @Nullable
    private static Server server;
    @Nullable
    private static String checkUrl;

    @BeforeAll
    static void init() {
        sampleEndpoints.stream().findAny().ifPresent(endpoint -> {
            serviceEndpoint = endpoint;
            server = Server.builder()
                           .http(endpoint.port())
                           .service("/", new EchoService())
                           .build();
            checkUrl = "http://" + endpoint.host() + ':' + endpoint.port() + checkPath;
            final Check check = new Check();
            check.setName(checkName);
            check.setHttp(checkUrl);
            check.setMethod(checkMethod);
            check.setInterval("1s");
            check.setStatus("critical"); // sets initial state
            try {
                serviceClient.register(
                        serviceName, serviceEndpoint.host(), serviceEndpoint.port(), check,
                        QueryParams.of());
            } catch (JsonProcessingException e) {
                fail(e.getMessage());
            }
        });
    }

    @AfterAll
    static void destroy() {
        if (server != null) {
            server.close();
        }
    }

    @Test
    void whenCallGetFromCacheBeforeStartThenRaiseException() {
        try (CachedConsulClient<List<Endpoint>> cached =
                     new CachedConsulClient<>(
                             () -> HealthClient.of(client())
                                               .healthyEndpoints(serviceName).join()
                     )
        ) {
            assertThrows(IllegalStateException.class, cached::get);
        }
    }

    @Test
    void testCachedHealthServiceList() {
        try (CachedConsulClient<List<Endpoint>> cached =
                     new CachedConsulClient<>(
                             () -> HealthClient.of(client())
                                               .healthyEndpoints(serviceName).join()
                     )
        ) {
            Objects.requireNonNull(server);
            Objects.requireNonNull(cached);
            cached.delayMillis(1_000).start();
            await().atMost(5, TimeUnit.SECONDS)
                   .until(() -> cached.get().isEmpty());
            server.start();
            await().atMost(5, TimeUnit.SECONDS)
                   .until(() -> cached.get().size() == 1);
        }
    }
}
