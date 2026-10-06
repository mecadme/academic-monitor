package io.academicmonitor.shared.integration;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;

@SpringBootTest
@Transactional
public abstract class PostgresIntegrationTest {

    protected static final PostgreSQLContainer<?> POSTGRES;

    static {
        POSTGRES = new PostgreSQLContainer<>("postgres:18-alpine")
                .withDatabaseName("academic_monitor_test")
                .withUsername("test")
                .withPassword("test");

        POSTGRES.start();
    }

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);

        registry.add("spring.datasource.username", POSTGRES::getUsername);

        registry.add("spring.datasource.password", POSTGRES::getPassword);
        // Ephemeral fixture key; never used for a running development/production environment.
        byte[] secret = new byte[32];
        new java.security.SecureRandom().nextBytes(secret);
        String testSecret = java.util.Base64.getEncoder().encodeToString(secret);
        registry.add("app.auth.jwt-secret-base64", () -> testSecret);
    }
}
