package com.cloudpipeline.app.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for HomeController endpoints using MockMvc.
 */
@WebMvcTest(HomeController.class)
@ActiveProfiles("test")
class HomeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getRoot_shouldReturn200AndHtmlContent() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Cloud Pipeline Demo App")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("bootstrap")));
    }

    @Test
    void getRoot_shouldContainPipelineStages() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("GitHub Actions")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("AWS EKS")));
    }

    @Test
    void getHealth_shouldReturn200WithStatusUp() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.version").value("1.0.1"));
    }

    @Test
    void getInfo_shouldReturn200WithAppMetadata() throws Exception {
        mockMvc.perform(get("/info"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.app.name").value("Cloud Pipeline Demo App"))
                .andExpect(jsonPath("$.app.version").value("1.0.1"))
                .andExpect(jsonPath("$.runtime.javaVersion").isNotEmpty());
    }
}
