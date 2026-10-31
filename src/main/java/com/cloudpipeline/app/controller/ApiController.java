package com.cloudpipeline.app.controller;

import com.cloudpipeline.app.model.GreetingResponse;
import com.cloudpipeline.app.model.StatusResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.InetAddress;
import java.time.Instant;

/**
 * ApiController exposes REST API endpoints under the /api path.
 * Provides greeting and deployment status functionality.
 */
@RestController
@RequestMapping("/api")
public class ApiController {

    @Value("${info.app.version:1.0.0}")
    private String appVersion;

    @Value("${spring.profiles.active:default}")
    private String activeProfile;

    /**
     * GET /api/greet?name=World
     * Returns a personalised greeting with the current UTC timestamp.
     *
     * @param name the name to greet (defaults to "World")
     * @return GreetingResponse containing message and ISO-8601 timestamp
     */
    @GetMapping("/greet")
    public ResponseEntity<GreetingResponse> greet(
            @RequestParam(name = "name", defaultValue = "World") String name) {

        String message = String.format("Hello Welcome, %s!", name);
        GreetingResponse response = new GreetingResponse(message, Instant.now().toString());
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/status
     * Returns current deployment status information including hostname, version, and environment.
     *
     * @return StatusResponse with deployment details
     */
    @GetMapping("/status")
    public ResponseEntity<StatusResponse> status() {
        String hostname = "unknown";
        try {
            hostname = InetAddress.getLocalHost().getHostName();
        } catch (Exception ignored) {
        }

        StatusResponse response = new StatusResponse(
                "RUNNING",
                appVersion,
                activeProfile,
                hostname
        );
        return ResponseEntity.ok(response);
    }
}
