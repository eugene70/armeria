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

import java.util.Map;

import javax.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public final class Check {

    @Nullable
    @JsonProperty("Node")
    private String node;

    @Nullable
    @JsonProperty("CheckID")
    private String checkId;

    @Nullable
    @JsonProperty("Name")
    private String name;

    @Nullable
    @JsonProperty("ID")
    private String id;

    @Nullable
    @JsonProperty("Interval")
    private String interval = "10s";

    @Nullable
    @JsonProperty("Notes")
    private String notes;

    @Nullable
    @JsonProperty("DeregisterCriticalServiceAfter")
    private String deregisterCriticalServiceAfter;

    @Nullable
    @JsonProperty("Args")
    private String[] args;

    @Nullable
    @JsonProperty("AliasNode")
    private String[] aliasNode;

    @Nullable
    @JsonProperty("DockerContainerID")
    private String dockerContainerID;

    @Nullable
    @JsonProperty("GRPC")
    private String grpc;

    @JsonProperty("GRPCUseTLS")
    private boolean grpcUseTls;

    @Nullable
    @JsonProperty("Shell")
    private String shell;

    @Nullable
    @JsonProperty("HTTP")
    private String http;

    @Nullable
    @JsonProperty("Method")
    private String method;

    @Nullable
    @JsonProperty("Header")
    private Map<String, String[]> header;

    @Nullable
    @JsonProperty("Timeout")
    private String timeout = "10s";

    @JsonProperty("OutputMaxSize")
    private int outputMaxSize = 4096;

    @JsonProperty("TLSSkipVerify")
    private boolean tlsSkipVerify;

    @Nullable
    @JsonProperty("TCP")
    private String tcp;

    @Nullable
    @JsonProperty("TTL")
    private String ttl;

    @Nullable
    @JsonProperty("ServiceID")
    private String serviceId;

    @Nullable
    @JsonProperty("Status")
    private String status;

    /**
     * Getter for node.
     */
    @Nullable
    public String getNode() {
        return node;
    }

    /**
     * Setter for node.
     */
    public void setNode(@Nullable String node) {
        this.node = node;
    }

    /**
     * Getter for checkId.
     */
    @Nullable
    public String getCheckId() {
        return checkId;
    }

    /**
     * Getter for checkId.
     */
    public void setCheckId(@Nullable String checkId) {
        this.checkId = checkId;
    }

    /**
     * Getter for name.
     */
    @Nullable
    public String getName() {
        return name;
    }

    /**
     * Setter for name.
     */
    public void setName(@Nullable String name) {
        this.name = name;
    }

    /**
     * Getter for id.
     */
    @Nullable
    public String getId() {
        return id;
    }

    /**
     * Setter for id.
     */
    public void setId(@Nullable String id) {
        this.id = id;
    }

    /**
     * Getter for interval.
     */
    @Nullable
    public String getInterval() {
        return interval;
    }

    /**
     * Setter for interval.
     */
    public void setInterval(@Nullable String interval) {
        this.interval = interval;
    }

    /**
     * Getter for notes.
     */
    @Nullable
    public String getNotes() {
        return notes;
    }

    /**
     * Setter for notes.
     */
    public void setNotes(@Nullable String notes) {
        this.notes = notes;
    }

    /**
     * Getter for deregisterCriticalServiceAfter.
     */
    @Nullable
    public String getDeregisterCriticalServiceAfter() {
        return deregisterCriticalServiceAfter;
    }

    /**
     * Setter for deregisterCriticalServiceAfter.
     */
    public void setDeregisterCriticalServiceAfter(@Nullable String deregisterCriticalServiceAfter) {
        this.deregisterCriticalServiceAfter = deregisterCriticalServiceAfter;
    }

    /**
     * Getter for args.
     */
    @Nullable
    public String[] getArgs() {
        return args;
    }

    /**
     * Setter for args.
     */
    public void setArgs(@Nullable String[] args) {
        this.args = args;
    }

    /**
     * Getter for aliasNode.
     */
    @Nullable
    public String[] getAliasNode() {
        return aliasNode;
    }

    /**
     * Setter for aliasNode.
     */
    public void setAliasNode(@Nullable String[] aliasNode) {
        this.aliasNode = aliasNode;
    }

    /**
     * Getter for dockerContainerID.
     */
    @Nullable
    public String getDockerContainerID() {
        return dockerContainerID;
    }

    /**
     * Setter for dockerContainerID.
     */
    public void setDockerContainerID(@Nullable String dockerContainerID) {
        this.dockerContainerID = dockerContainerID;
    }

    /**
     * Getter for grpc.
     */
    @Nullable
    public String getGrpc() {
        return grpc;
    }

    /**
     * Setter for grpc.
     */
    public void setGrpc(@Nullable String grpc) {
        this.grpc = grpc;
    }

    /**
     * Getter for grpcUseTls.
     */
    public boolean isGrpcUseTls() {
        return grpcUseTls;
    }

    /**
     * Setter for grpcUseTls.
     */
    public void setGrpcUseTls(boolean grpcUseTls) {
        this.grpcUseTls = grpcUseTls;
    }

    /**
     * Getter for shell.
     */
    @Nullable
    public String getShell() {
        return shell;
    }

    /**
     * Setter for shell.
     */
    public void setShell(@Nullable String shell) {
        this.shell = shell;
    }

    /**
     * Getter for http.
     */
    @Nullable
    public String getHttp() {
        return http;
    }

    /**
     * Setter for http.
     */
    public void setHttp(@Nullable String http) {
        this.http = http;
    }

    /**
     * Getter for method.
     */
    @Nullable
    public String getMethod() {
        return method;
    }

    /**
     * Setter for method.
     */
    public void setMethod(@Nullable String method) {
        this.method = method;
    }

    /**
     * Getter for header.
     */
    @Nullable
    public Map<String, String[]> getHeader() {
        return header;
    }

    /**
     * Setter for header.
     */
    public void setHeader(@Nullable Map<String, String[]> header) {
        this.header = header;
    }

    /**
     * Getter for timeout.
     */
    @Nullable
    public String getTimeout() {
        return timeout;
    }

    /**
     * Setter for timeout.
     */
    public void setTimeout(@Nullable String timeout) {
        this.timeout = timeout;
    }

    /**
     * Getter for outputMaxSize.
     */
    public int getOutputMaxSize() {
        return outputMaxSize;
    }

    /**
     * Setter for outputMaxSize.
     */
    public void setOutputMaxSize(int outputMaxSize) {
        this.outputMaxSize = outputMaxSize;
    }

    /**
     * Getter for tlsSkipVerify.
     */
    public boolean isTlsSkipVerify() {
        return tlsSkipVerify;
    }

    /**
     * Setter for tlsSkipVerify.
     */
    public void setTlsSkipVerify(boolean tlsSkipVerify) {
        this.tlsSkipVerify = tlsSkipVerify;
    }

    /**
     * Getter for tcp.
     */
    @Nullable
    public String getTcp() {
        return tcp;
    }

    /**
     * Setter for tcp.
     */
    public void setTcp(@Nullable String tcp) {
        this.tcp = tcp;
    }

    /**
     * Getter for ttl.
     */
    @Nullable
    public String getTtl() {
        return ttl;
    }

    /**
     * Setter for ttl.
     */
    public void setTtl(@Nullable String ttl) {
        this.ttl = ttl;
    }

    /**
     * Getter for serviceId.
     */
    @Nullable
    public String getServiceId() {
        return serviceId;
    }

    /**
     * Setter for serviceId.
     */
    public void setServiceId(@Nullable String serviceId) {
        this.serviceId = serviceId;
    }

    /**
     * Getter for status.
     */
    @Nullable
    public String getStatus() {
        return status;
    }

    /**
     * Setter for status.
     */
    public void setStatus(@Nullable String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                          .omitNullValues()
                          .add("node", node)
                          .add("checkId", checkId)
                          .add("name", name)
                          .add("id", id)
                          .add("interval", interval)
                          .add("notes", notes)
                          .add("deregisterCriticalServiceAfter",
                               deregisterCriticalServiceAfter)
                          .add("args", args)
                          .add("aliasNode", aliasNode)
                          .add("dockerContainerID", dockerContainerID)
                          .add("grpc", grpc)
                          .add("grpcUseTls", grpcUseTls)
                          .add("shell", shell)
                          .add("http", http)
                          .add("method", method)
                          .add("header", header)
                          .add("timeout", timeout)
                          .add("outputMaxSize", outputMaxSize)
                          .add("tlsSkipVerify", tlsSkipVerify)
                          .add("tcp", tcp)
                          .add("ttl", ttl)
                          .add("serviceId", serviceId)
                          .add("status", status)
                          .toString();
    }
}
