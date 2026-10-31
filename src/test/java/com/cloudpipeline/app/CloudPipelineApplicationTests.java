package com.cloudpipeline.app;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integration test that verifies the Spring application context loads successfully.
 */
@SpringBootTest
@ActiveProfiles("test")
class CloudPipelineApplicationTests {

    @Test
    void contextLoads() {
        // Verifies that the Spring Boot application context starts without errors.
    }
}
