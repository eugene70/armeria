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

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;

import com.linecorp.armeria.client.Endpoint;
import com.linecorp.armeria.client.consul.ConsulTestBase;
import com.linecorp.armeria.common.QueryParams;
import com.linecorp.armeria.internal.consul.AgentServiceClient.Service;
import com.linecorp.armeria.server.Server;

class AgentCheckClientTest extends ConsulTestBase {

    private static final AgentServiceClient serviceClient = AgentServiceClient.of(client());
    private static final AgentCheckClient checkClient = AgentCheckClient.of(client());
    private static Endpoint serviceEndpoint;
    private static Server server;
    private static String checkUrl;

    @BeforeAll
    static void init() {
        sampleEndpoints.stream().findAny().ifPresent(endpoint -> {
            serviceEndpoint = endpoint;
            server = Server.builder()
                           .http(endpoint.port())
                           .service("/", new EchoService())
                           .build();
            // starts a sample server
            server.start().join();
            checkUrl = "http://" + endpoint.host() + ':' + endpoint.port() + checkPath;
        });
    }

    @AfterAll
    static void destroy() {
        if (server != null) {
            server.close();
        }
    }

    /**
     * Test check for node(service not specified). Test below processes.
     *
     * <p><ul>1) registers a check for node</ul>
     * <ul>2) confirm the check is registered</ul>
     * <ul>3) confirm status of the check is healthy</ul>
     * <ul>4) de-registers the check</ul>
     * <ul>5) confirm the check is de-registered</ul>
     */
    @Test
    void testCheckRegistrationAndHealth() throws InterruptedException, JsonProcessingException {
        // 1) Registers a check for node.
        // When service is unspecified in a check, the check acts for 'consul' the default service of a node.
        final Check check = new Check();
        check.setName(checkName);
        check.setHttp(checkUrl);
        check.setMethod(checkMethod);
        check.setInterval("1s");
        check.setStatus("critical"); // sets initial state
        checkClient.register(check);

        Thread.sleep(1500);

        // 2) Confirm registration of the check
        Check gotCheck = checkClient.check(checkName).join();
        assertThat(gotCheck)
                .as("the check after register")
                .isNotNull();

        // 3) Confirm healthy status of the check
        assertThat(gotCheck.getStatus())
                .as("getStatus")
                .isEqualTo("passing");

        // 4) De-registers the check
        checkClient.deregister(checkName);

        // 5) confirm the check was de-registered
        final Map<String, Check> checks = checkClient.checks().join();
        gotCheck = checks.get(checkName);
        assertThat(gotCheck)
                .as("the check after deregister")
                .isNull();
    }

    /**
     * Test check for node(service specified).
     */
    @Test
    void testServiceCheck() throws InterruptedException, JsonProcessingException {

        final String serviceId = serviceName + "_for_test_check";
        final QueryParams params = QueryParams.of("replace-existing-checks", 1);
        final Check check = new Check();
        check.setId(checkName + "_test_test");
        check.setName(checkName);
        check.setHttp(checkUrl);
        check.setMethod(checkMethod);
        check.setInterval("1s");
        check.setStatus("critical"); // sets initial state
        //check.setServiceId(serviceId);
        checkClient.register(check);
        // Using '/agent/service' API to register a service with a check

        check.setId(null); // no needs ID
        serviceClient.register(
                serviceId, serviceName, serviceEndpoint.host(), serviceEndpoint.port(), check, params);

        Thread.sleep(1_500);

        // Get service nodes and count it.
        final Map<String, Service> services =
                serviceClient.services(serviceName, QueryParams.of()).join();
        assertThat(services.size()).isEqualTo(1);

        final Map<?, Check> checkMap = checkClient.checks().join();
        assertThat(checkMap.size()).isGreaterThan(0);

        // The key of the check seems like it depends on the Consul version.
        final Check gotCheck = checkMap.get("service:" + serviceId);
        assertThat(gotCheck).isNotNull();

        assertThat(gotCheck.getStatus()).isEqualTo("passing");

        // loop that deregister a service and count service nodes
        final AtomicInteger size = new AtomicInteger(services.size());
        services.forEach((id, service) -> {
            serviceClient.deregister(service.id);
            assertThat(serviceClient.services(serviceName, QueryParams.of()).join().size())
                    .isEqualTo(size.decrementAndGet());
        });
    }
}
