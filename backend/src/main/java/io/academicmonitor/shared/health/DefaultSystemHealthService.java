package io.academicmonitor.shared.health;

import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
class DefaultSystemHealthService implements SystemHealthService {

    private final JdbcTemplate jdbcTemplate;
    private final RestClient ollamaClient;

    DefaultSystemHealthService(
            JdbcTemplate jdbcTemplate,
            RestClient.Builder restClientBuilder,
            @Value("${app.ollama.base-url}") String ollamaBaseUrl) {
        this.jdbcTemplate = jdbcTemplate;
        this.ollamaClient = restClientBuilder.baseUrl(ollamaBaseUrl).build();
    }

    @Override
    public SystemHealthResponse current() {
        List<ServiceHealth> services = new ArrayList<>();
        services.add(ServiceHealth.up("backend", "Spring Boot API is running"));
        services.add(databaseStatus());
        services.add(aiStatus());
        return SystemHealthResponse.from(services);
    }

    private ServiceHealth databaseStatus() {
        try {
            Integer result = jdbcTemplate.queryForObject("select 1", Integer.class);
            if (Integer.valueOf(1).equals(result)) {
                return ServiceHealth.up("database", "PostgreSQL is reachable");
            }
            return ServiceHealth.degraded("database", "PostgreSQL returned an unexpected response");
        } catch (RuntimeException exception) {
            return ServiceHealth.degraded("database", "PostgreSQL is not reachable");
        }
    }

    private ServiceHealth aiStatus() {
        try {
            ollamaClient.get().uri("/api/tags").retrieve().toBodilessEntity();
            return ServiceHealth.up("ai", "Ollama is reachable");
        } catch (RuntimeException exception) {
            return ServiceHealth.degraded("ai", "Ollama is not reachable");
        }
    }
}
