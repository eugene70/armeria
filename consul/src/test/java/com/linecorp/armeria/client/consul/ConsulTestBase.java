/*
    Copyright 2019 LINE Corporation

    LINE Corporation licenses this file to you under the Apache License,
    version 2.0 (the "License"); you may not use this file except in compliance
    with the License. You may obtain a copy of the License at:

      https://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
    WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
    License for the specific language governing permissions and limitations
    under the License.
 */
package com.linecorp.armeria.client.consul;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import javax.annotation.Nullable;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import com.google.common.collect.ImmutableSet;
import com.pszymczyk.consul.ConsulProcess;
import com.pszymczyk.consul.ConsulStarterBuilder;

import com.linecorp.armeria.client.Endpoint;
import com.linecorp.armeria.common.AggregatedHttpRequest;
import com.linecorp.armeria.common.HttpRequest;
import com.linecorp.armeria.common.HttpResponse;
import com.linecorp.armeria.common.HttpStatus;
import com.linecorp.armeria.common.ResponseHeaders;
import com.linecorp.armeria.common.util.CompletionActions;
import com.linecorp.armeria.internal.consul.ConsulClient;
import com.linecorp.armeria.server.AbstractHttpService;
import com.linecorp.armeria.server.ServiceRequestContext;

public class ConsulTestBase {

    protected static final String serviceName = "testService";
    protected static final String checkName = "testCheck";
    protected static final String checkPath = "/";
    protected static final String checkMethod = "POST";
    protected static final Set<Endpoint> sampleEndpoints;

    static {
        final int[] ports = unusedPorts(3);
        sampleEndpoints = ImmutableSet.of(Endpoint.of("localhost", ports[0]).withWeight(2),
                                          Endpoint.of("127.0.0.1", ports[1]).withWeight(4),
                                          Endpoint.of("127.0.0.1", ports[2]).withWeight(2));
    }

    protected ConsulTestBase() {
    }

    @Nullable
    private static ConsulProcess consul;

    @Nullable
    private static ConsulClient consulClient;

    @BeforeAll
    public static void start() throws Throwable {
        // Initialize Consul embedded server for testing
        consul = ConsulStarterBuilder.consulStarter()
                                     .withConsulVersion("1.7.2")
                                     .build().start();
        // Initialize Consul client
        consulClient = ConsulClient.builder()
                                   .port(consul.getHttpPort())
                                   .build();
    }

    @AfterAll
    public static void stop() throws Throwable {
        if (consul != null) {
            consul.close();
            consul = null;
        }
        if (consulClient != null) {
            consulClient = null;
        }
    }

    protected static ConsulClient client() {
        if (consulClient == null) {
            throw new RuntimeException("consul client has not initialized");
        }
        return consulClient;
    }

    private static int[] unusedPorts(int numPorts) {
        final int[] ports = new int[numPorts];
        final Random random = ThreadLocalRandom.current();
        for (int i = 0; i < numPorts; i++) {
            for (;;) {
                final int candidatePort = random.nextInt(64512) + 1024;
                try (ServerSocket ss = new ServerSocket()) {
                    ss.bind(new InetSocketAddress("127.0.0.1", candidatePort));
                    ports[i] = candidatePort;
                    break;
                } catch (IOException e) {
                    // Port in use or unable to bind.
                    continue;
                }
            }
        }

        return ports;
    }

    public static class EchoService extends AbstractHttpService {
        @Override
        protected final HttpResponse doPost(ServiceRequestContext ctx, HttpRequest req) {
            return HttpResponse.from(req.aggregate()
                                        .thenApply(this::echo)
                                        .exceptionally(CompletionActions::log));
        }

        protected HttpResponse echo(AggregatedHttpRequest aReq) {
            return HttpResponse.of(ResponseHeaders.of(HttpStatus.OK), aReq.content());
        }
    }
}
