package io.academicmonitor.shared.health;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class HealthControllerTest {

    @Test
    void returnsSystemHealth() throws Exception {
        SystemHealthService healthService = () -> SystemHealthResponse.from(List.of(
                ServiceHealth.up("backend", "Spring Boot API is running"),
                ServiceHealth.up("database", "PostgreSQL is reachable"),
                ServiceHealth.up("ai", "Ollama is reachable")));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new HealthController(healthService))
                .build();

        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.services[0].name").value("backend"))
                .andExpect(jsonPath("$.services[1].name").value("database"))
                .andExpect(jsonPath("$.services[2].name").value("ai"));
    }
}
