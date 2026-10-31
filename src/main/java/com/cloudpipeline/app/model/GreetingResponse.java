package com.cloudpipeline.app.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * GreetingResponse is the response model for the /api/greet endpoint.
 * Contains a greeting message and the UTC timestamp of the request.
 */
public class GreetingResponse {

    @JsonProperty("message")
    private final String message;

    @JsonProperty("timestamp")
    private final String timestamp;

    public GreetingResponse(String message, String timestamp) {
        this.message = message;
        this.timestamp = timestamp;
    }

    public String getMessage() {
        return message;
    }

    public String getTimestamp() {
        return timestamp;
    }
}
