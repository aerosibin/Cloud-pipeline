package com.cloudpipeline.app.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * StatusResponse is the response model for the /api/status endpoint.
 * Contains runtime deployment information about the running instance.
 */
public class StatusResponse {

    @JsonProperty("status")
    private final String status;

    @JsonProperty("version")
    private final String version;

    @JsonProperty("environment")
    private final String environment;

    @JsonProperty("hostname")
    private final String hostname;

    public StatusResponse(String status, String version, String environment, String hostname) {
        this.status = status;
        this.version = version;
        this.environment = environment;
        this.hostname = hostname;
    }

    public String getStatus() {
        return status;
    }

    public String getVersion() {
        return version;
    }

    public String getEnvironment() {
        return environment;
    }

    public String getHostname() {
        return hostname;
    }
}
