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
package com.linecorp.armeria.server.consul;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;

import com.linecorp.armeria.client.Endpoint;
import com.linecorp.armeria.client.consul.ConsulTestBase;
import com.linecorp.armeria.common.AggregatedHttpResponse;
import com.linecorp.armeria.common.HttpStatus;
import com.linecorp.armeria.server.Server;
import com.linecorp.armeria.server.ServerListener;

public class ConsulUpdatingListenerTest extends ConsulTestBase {

    @Nullable
    static List<Server> servers;

    @BeforeAll
    public static void startServers() throws JsonProcessingException {
        servers = new ArrayList<>();

        for (Endpoint endpoint : sampleEndpoints) {
            final Server server = Server.builder()
                                        .http(endpoint.port())
                                        .service("/", new EchoService())
                                        .build();
            final ServerListener listener =
                    new ConsulUpdatingListenerBuilder(serviceName).url(client().url())
                                                                  .endpoint(endpoint)
                                                                  .checkUrl("http://" + endpoint.host() +
                                                                            ':' + endpoint.port() + checkPath)
                                                                  .checkMethod("POST")
                                                                  .checkInterval("1s")
                                                                  .build();
            server.addListener(listener);
            server.start().join();
            servers.add(server);
        }
    }

    @AfterAll
    public static void stopServers() throws Exception {
        assert servers != null;
        servers.forEach(Server::close);
    }

    @Test
    public void shouldStartConsul() throws Throwable {
        await().atMost(30, TimeUnit.SECONDS).until(() -> {
            final AggregatedHttpResponse response = client().consulWebClient()
                                                            .get("/agent/self").aggregate().join();
            return response.status() == HttpStatus.OK;
        });
    }

    @Test
    public void testBuild() {
        assertThat(new ConsulUpdatingListenerBuilder(serviceName).build()).isNotNull();
        assertThat(new ConsulUpdatingListenerBuilder(serviceName).url("http://localhost:8080")
                                                                 .build()).isNotNull();
    }

    @Test
    public void testEndpointsCountOfListeningServiceWithAServerStopAndStart() {
        // Checks sample endpoints created when initialized.
        await().atMost(10, TimeUnit.SECONDS)
               .until(() -> client().endpoints(serviceName).join().size() == sampleEndpoints.size());

        // When we close one server then the listener deregister it automatically from consul agent.
        assert servers != null;
        servers.get(0).stop().join();
        await().atMost(10, TimeUnit.SECONDS)
               .until(() -> client().endpoints(serviceName).join().size() == sampleEndpoints.size() - 1);

        // Endpoints increased after service restart.
        servers.get(0).start().join();
        await().atMost(10, TimeUnit.SECONDS)
               .until(() -> client().endpoints(serviceName).join().size() == sampleEndpoints.size());
    }
}
