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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;

import com.linecorp.armeria.client.Endpoint;
import com.linecorp.armeria.client.consul.ConsulTestBase;
import com.linecorp.armeria.common.QueryParams;
import com.linecorp.armeria.internal.consul.AgentServiceClient.Service;

class AgentServiceClientTest extends ConsulTestBase {

    @Test
    void serviceCountShouldBeChangedAfterRegisterOrDeregister() throws JsonProcessingException {
        // register services
        final QueryParams params = QueryParams.of("replace-existing-checks", 1);
        final AgentServiceClient client = AgentServiceClient.of(client());
        for (Endpoint endpoint : sampleEndpoints) {
            client.register(serviceName, endpoint.host(), endpoint.port(), params).join();
        }

        // get service nodes and count it
        final Map<String, Service> services = client.services(serviceName, QueryParams.of()).join();
        assertThat(services.size()).as("registered services after register")
                                   .isEqualTo(sampleEndpoints.size());

        // loop that deregister a service and count service nodes
        final AtomicInteger expectedSize = new AtomicInteger(services.size());
        services.forEach((id, service) -> {
            client.deregister(service.id);
            assertThat(client.services(serviceName, QueryParams.of()).join().size())
                    .as("service count after deregister")
                    .isEqualTo(expectedSize.decrementAndGet());
        });
    }

    /**
     * Does not check health of services at this time.
     */
    @Test
    void testAgentServiceClientWithCheck() throws JsonProcessingException {
        // register services with check
        final QueryParams params = QueryParams.of("replace-existing-checks", 1);
        final AgentServiceClient client = AgentServiceClient.of(client());
        for (Endpoint endpoint : sampleEndpoints) {
            final Check check = new Check();
            check.setHttp("http://" + endpoint.host() + ':' + endpoint.port() + checkPath);
            check.setMethod(checkMethod);
            client.register(serviceName, endpoint.host(), endpoint.port(), check, params).join();
        }

        // get service nodes and count it
        final Map<String, Service> services = client.services(serviceName, QueryParams.of()).join();
        assertThat(services.size()).as("registered services after register")
                                   .isEqualTo(sampleEndpoints.size());

        // loop that deregister a service and count service nodes
        final AtomicInteger expectedSize = new AtomicInteger(services.size());
        services.forEach((id, service) -> {
            client.deregister(service.id);
            assertThat(client.services(serviceName, QueryParams.of()).join().size())
                    .as("service count after deregister")
                    .isEqualTo(expectedSize.decrementAndGet());
        });
    }
}
