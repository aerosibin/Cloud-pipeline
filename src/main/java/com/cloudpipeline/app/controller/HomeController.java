package com.cloudpipeline.app.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.InetAddress;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * HomeController handles the root and system-level endpoints.
 * Provides a human-readable HTML landing page, a health check, and app info.
 */
@RestController
public class HomeController {

    @Value("${info.app.name:Cloud Pipeline Demo App}")
    private String appName;

    @Value("${info.app.version:1.0.0}")
    private String appVersion;

    @Value("${info.app.description:End-to-End CI/CD Pipeline Demo Application}")
    private String appDescription;

    @Value("${spring.profiles.active:default}")
    private String activeProfile;

    /**
     * GET / — Returns a visually styled Bootstrap HTML landing page.
     */
    @GetMapping(value = "/", produces = "text/html")
    public String home() {
        String hostname = "unknown";
        try {
            hostname = InetAddress.getLocalHost().getHostName();
        } catch (Exception ignored) {
        }

        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8"/>
                    <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
                    <title>Cloud Pipeline Demo App</title>
                    <link rel="stylesheet"
                          href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css"
                          integrity="sha384-T3c6CoIi6uLrA9TneNEoa7RxnatzjcDSCmG1MXxSR1GAsXEV/Dwwykc2MPK8M2HN"
                          crossorigin="anonymous"/>
                    <style>
                        body { background: linear-gradient(135deg, #0f2027, #203a43, #2c5364); min-height: 100vh; color: #f8f9fa; }
                        .hero { padding: 60px 0 40px; }
                        .card { background: rgba(255,255,255,0.07); border: 1px solid rgba(255,255,255,0.15); backdrop-filter: blur(10px); }
                        .card-header { background: rgba(255,255,255,0.1); border-bottom: 1px solid rgba(255,255,255,0.15); }
                        .badge-healthy { background: #28a745; font-size: 0.9rem; padding: 6px 14px; border-radius: 50px; }
                        .pipeline-step { display: inline-block; padding: 8px 18px; margin: 4px; border-radius: 25px; font-size: 0.85rem; font-weight: 600; }
                        .step-code    { background: #6f42c1; }
                        .step-test    { background: #fd7e14; }
                        .step-build   { background: #0d6efd; }
                        .step-docker  { background: #0dcaf0; color: #000; }
                        .step-k8s     { background: #198754; }
                        .table { color: #f8f9fa; }
                        .table th { color: #adb5bd; font-weight: 600; text-transform: uppercase; font-size: 0.78rem; letter-spacing: .05em; }
                        .endpoint-badge { font-size: 0.75rem; font-weight: 700; padding: 3px 10px; border-radius: 4px; }
                        .footer { border-top: 1px solid rgba(255,255,255,0.1); padding: 20px 0; margin-top: 40px; color: #adb5bd; font-size: 0.85rem; }
                    </style>
                </head>
                <body>
                <div class="container">
                    <!-- Hero -->
                    <div class="hero text-center">
                        <h1 class="display-4 fw-bold mb-2">&#9729;&#65039; Cloud Pipeline Demo App</h1>
                        <p class="lead text-secondary mb-4">""" + appDescription + """
                </p>
                        <span class="badge badge-healthy me-2">&#10003; Healthy</span>
                        <span class="badge bg-secondary">v""" + appVersion + """
                </span>
                    </div>

                    <!-- Info Cards Row -->
                    <div class="row g-3 mb-4">
                        <div class="col-md-4">
                            <div class="card h-100">
                                <div class="card-header fw-semibold">&#128187; Application Info</div>
                                <div class="card-body">
                                    <table class="table table-sm mb-0">
                                        <tr><th>Name</th><td>""" + appName + """
                </td></tr>
                                        <tr><th>Version</th><td>""" + appVersion + """
                </td></tr>
                                        <tr><th>Profile</th><td><code>""" + activeProfile + """
                </code></td></tr>
                                        <tr><th>Hostname</th><td><code>""" + hostname + """
                </code></td></tr>
                                    </table>
                                </div>
                            </div>
                        </div>
                        <div class="col-md-4">
                            <div class="card h-100">
                                <div class="card-header fw-semibold">&#9881;&#65039; CI/CD Stack</div>
                                <div class="card-body">
                                    <ul class="list-unstyled mb-0">
                                        <li class="mb-2">&#128640; <strong>CI/CD:</strong> GitHub Actions</li>
                                        <li class="mb-2">&#128679; <strong>Build:</strong> Maven 3.9 + JDK 21</li>
                                        <li class="mb-2">&#128025; <strong>Registry:</strong> Docker Hub</li>
                                        <li class="mb-2">&#9749; <strong>Runtime:</strong> AWS EKS (Kubernetes)</li>
                                        <li class="mb-2">&#128736;&#65039; <strong>Config:</strong> Ansible</li>
                                    </ul>
                                </div>
                            </div>
                        </div>
                        <div class="col-md-4">
                            <div class="card h-100">
                                <div class="card-header fw-semibold">&#127381; Features</div>
                                <div class="card-body">
                                    <ul class="list-unstyled mb-0">
                                        <li class="mb-1">&#10003; Multi-stage Docker builds</li>
                                        <li class="mb-1">&#10003; Automated unit testing</li>
                                        <li class="mb-1">&#10003; Image versioning with git SHA</li>
                                        <li class="mb-1">&#10003; Zero-downtime rolling deployments</li>
                                        <li class="mb-1">&#10003; Horizontal Pod Autoscaling</li>
                                        <li class="mb-1">&#10003; Spring Boot Actuator health checks</li>
                                        <li class="mb-1">&#10003; Ingress with TLS termination</li>
                                    </ul>
                                </div>
                            </div>
                        </div>
                    </div>

                    <!-- Pipeline Stages -->
                    <div class="card mb-4">
                        <div class="card-header fw-semibold">&#128260; Pipeline Stages</div>
                        <div class="card-body text-center py-3">
                            <span class="pipeline-step step-code">1. Code Push</span>
                            <span class="text-secondary mx-1">&#8594;</span>
                            <span class="pipeline-step step-test">2. Unit Tests</span>
                            <span class="text-secondary mx-1">&#8594;</span>
                            <span class="pipeline-step step-build">3. Maven Build</span>
                            <span class="text-secondary mx-1">&#8594;</span>
                            <span class="pipeline-step step-docker">4. Docker Push</span>
                            <span class="text-secondary mx-1">&#8594;</span>
                            <span class="pipeline-step step-k8s">5. EKS Deploy</span>
                        </div>
                    </div>

                    <!-- API Endpoints -->
                    <div class="card mb-4">
                        <div class="card-header fw-semibold">&#128203; API Endpoints</div>
                        <div class="card-body p-0">
                            <table class="table table-hover mb-0" style="border-color: rgba(255,255,255,0.1)">
                                <thead>
                                    <tr>
                                        <th class="ps-3">Method</th>
                                        <th>Endpoint</th>
                                        <th>Description</th>
                                        <th class="pe-3">Response</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <tr>
                                        <td class="ps-3"><span class="badge bg-success endpoint-badge">GET</span></td>
                                        <td><code>/</code></td>
                                        <td>Landing page (this page)</td>
                                        <td>HTML</td>
                                    </tr>
                                    <tr>
                                        <td class="ps-3"><span class="badge bg-success endpoint-badge">GET</span></td>
                                        <td><code>/health</code></td>
                                        <td>Application health check</td>
                                        <td>JSON</td>
                                    </tr>
                                    <tr>
                                        <td class="ps-3"><span class="badge bg-success endpoint-badge">GET</span></td>
                                        <td><code>/info</code></td>
                                        <td>Application metadata</td>
                                        <td>JSON</td>
                                    </tr>
                                    <tr>
                                        <td class="ps-3"><span class="badge bg-success endpoint-badge">GET</span></td>
                                        <td><code>/api/greet?name=World</code></td>
                                        <td>Greeting with timestamp</td>
                                        <td>JSON</td>
                                    </tr>
                                    <tr>
                                        <td class="ps-3"><span class="badge bg-success endpoint-badge">GET</span></td>
                                        <td><code>/api/status</code></td>
                                        <td>Deployment status info</td>
                                        <td>JSON</td>
                                    </tr>
                                    <tr>
                                        <td class="ps-3"><span class="badge bg-info endpoint-badge text-dark">GET</span></td>
                                        <td><code>/actuator/health</code></td>
                                        <td>Spring Boot Actuator health</td>
                                        <td>JSON</td>
                                    </tr>
                                </tbody>
                            </table>
                        </div>
                    </div>

                    <div class="footer text-center">
                        <p class="mb-0">Cloud Pipeline Demo App &copy; 2024 &mdash; Built with Spring Boot 3.2, Deployed on AWS EKS</p>
                    </div>
                </div>
                <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/js/bootstrap.bundle.min.js"
                        integrity="sha384-C6RzsynM9kWDrMNeT87bh95OGNyZPhcTNXj1NW7RuBCsyN/o0jlpcV8Qyq46cDfL"
                        crossorigin="anonymous"></script>
                </body>
                </html>
                """;
    }

    /**
     * GET /health — Returns a simple JSON health status.
     */
    @GetMapping(value = "/health", produces = "application/json")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "UP");
        response.put("version", appVersion);
        response.put("application", appName);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /info — Returns JSON with application metadata.
     */
    @GetMapping(value = "/info", produces = "application/json")
    public ResponseEntity<Map<String, Object>> info() {
        String hostname = "unknown";
        try {
            hostname = InetAddress.getLocalHost().getHostName();
        } catch (Exception ignored) {
        }

        Map<String, Object> app = new LinkedHashMap<>();
        app.put("name", appName);
        app.put("version", appVersion);
        app.put("description", appDescription);

        Map<String, Object> runtime = new LinkedHashMap<>();
        runtime.put("hostname", hostname);
        runtime.put("profile", activeProfile);
        runtime.put("javaVersion", System.getProperty("java.version"));

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("app", app);
        response.put("runtime", runtime);
        return ResponseEntity.ok(response);
    }
}
