package com.jobplatform;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "app.jwt.secret=test-secret-that-is-at-least-thirty-two-bytes")
class BackendReliabilityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void jobsArePagedAndSearchRunsAgainstTheDatabase() throws Exception {
        mockMvc.perform(get("/api/jobs").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(5)))
                .andExpect(jsonPath("$.totalElements").value(19))
                .andExpect(jsonPath("$.totalPages").value(4));

        mockMvc.perform(get("/api/jobs/search")
                        .param("keyword", "Java Backend")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].title").value("Java Backend Engineer"));

        mockMvc.perform(get("/api/jobs/search").param("sort", "salary").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Product Designer"));
    }

    @Test
    void jobDetailsAndHealthRemainPubliclyAvailable() throws Exception {
        mockMvc.perform(get("/api/jobs/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.createdAt").exists());

        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));

        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void paginationLimitsAndSortOptionsAreValidated() throws Exception {
        mockMvc.perform(get("/api/jobs").param("size", "101"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/jobs").param("sort", "not-a-supported-sort"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void localDemoProfilesAreAvailableForTheLoginExperience() throws Exception {
        mockMvc.perform(get("/api/demo/profiles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].role").value("CANDIDATE"))
                .andExpect(jsonPath("$[1].role").value("RECRUITER"));
    }

    @Test
    void corsAllowsOnlyConfiguredLocalOrigin() throws Exception {
        mockMvc.perform(options("/api/jobs/search")
                        .header("Origin", "http://localhost:8080")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:8080"));

        mockMvc.perform(options("/api/jobs/search")
                        .header("Origin", "https://untrusted.example")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden());
    }
}
