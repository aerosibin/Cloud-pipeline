package com.cloudpipeline.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main entry point for the Cloud Pipeline Demo Application.
 * This Spring Boot application demonstrates an end-to-end CI/CD pipeline
 * using GitHub Actions, Docker, AWS EKS, and Kubernetes.
 */
@SpringBootApplication
public class CloudPipelineApplication {

    public static void main(String[] args) {
        SpringApplication.run(CloudPipelineApplication.class, args);
    }
}
